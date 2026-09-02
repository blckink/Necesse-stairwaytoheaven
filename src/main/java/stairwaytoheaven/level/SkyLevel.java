package stairwaytoheaven.level;

import java.awt.Point;

import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.level.maps.BiomeGeneratorStackLevel;
import necesse.level.maps.regionSystem.Region;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.quest.SkywatchQuestData;
import stairwaytoheaven.worldgen.SkyNoise;
import stairwaytoheaven.worldgen.SkyOrigin;
import stairwaytoheaven.worldgen.SkyTerrainPainter;
import stairwaytoheaven.worldgen.WardenSpirePreset;

/**
 * The Skyreach: the persistent one-world dimension one layer above the surface
 * (dimension +1), mirroring how CaveLevel/DeepCaveLevel sit below it.
 *
 * Infinite, generated region-by-region from the world seed. Not a cave
 * ({@code isCave} stays false), so it follows the world's day/night ambient
 * light like the surface does.
 */
public class SkyLevel extends BiomeGeneratorStackLevel {

    /**
     * Required by LevelRegistry: the game reconstructs registered levels through
     * this exact constructor signature when loading a saved world (the seed is
     * restored afterwards via applyLoadData, same as vanilla cave levels).
     */
    public SkyLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
        super(identifier, width, height, worldEntity);
        this.setup();
    }

    /** Used on first generation, when the world generator supplies the seed. */
    public SkyLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity, int seed) {
        super(identifier, width, height, worldEntity, seed);
        this.setup();
    }

    private void setup() {
        this.isCave = false;
        this.baseBiome = SkyRegistry.driftlands;
    }

    @Override
    public void generateRegion(Region region) {
        super.generateRegion(region);
        this.startDirtyRegionTracking();
        int presetGenerationUniqueID = this.getWorldEntity().startPresetGenerationInRegion(region, this.seed);
        try {
            SkyTerrainPainter.paintRegion(region, this.getWorldGenSeed());
        } finally {
            this.getWorldEntity().runPresetGenerationInRegion(presetGenerationUniqueID, region, this.seed);
            this.removeDirtyRegion(region.regionX, region.regionY);
        }
    }

    @Override
    public void onRegionGenerated(Region region, boolean skipGenerateForced) {
        super.onRegionGenerated(region, skipGenerateForced);
        region.checkGenerationValid();
        placeLivestockHerds(region);
        placeResident(region);
        placeGuardPacks(region);
    }

    /**
     * Cloud Lambs are placed HERE, at generation, and not by a spawn table.
     *
     * WHY: {@code MobChance.spawnMob} calls {@code mob.isValidSpawnLocation},
     * and {@code Mob}'s own implementation is {@code return false}. Nothing in
     * SheepMob -> HusbandryMob -> FriendlyRopableMob -> AttackAnimMob overrides
     * it, so a sheep can never be placed by a spawn table at all. A critter
     * table that asks for one gets nothing, silently -- which it did here for
     * three releases, and is why the player never saw the animal. Measured at
     * the time: {@code /skyreachstatus} reported
     * `validSpawnLocation=INHERITS Mob's false accepted lit=0/6 dark=0/6`.
     *
     * Vanilla has the same constraint and solves it the same way: sheep, rams,
     * cows and bulls are placed by the island generator
     * ({@code ig.spawnMobHerds} in PlainsSurfaceLevel and friends), never by a
     * spawn table. Livestock is terrain, not weather.
     *
     * Deterministic from the level seed and the region coordinates, so the same
     * world always grows the same flocks, and persistent (canDespawn false) so
     * a flock the player walked past is still there when they come back.
     */
    /**
     * The three Skyreach residents, standing at the derelict workshop they
     * refused to leave.
     *
     * WHY THEY ARE PLACED AT ALL, and why here. The mod registered one settler
     * for four releases and the player's question was why a settler with one of
     * our professions had never turned up. Half the answer was that none
     * existed; this is the other half. A hireable NPC that worldgen never
     * places is a class file, not a character -- the same failure as the three
     * workstations, which were craftable, correct and referenced by worldgen
     * exactly zero times.
     *
     * They stand at a workshop because that is where a loom-keeper, a cellarer
     * and an archivist would be, and because the workshop is already the thing
     * that says "somebody worked here". Finding the station and finding the
     * person is one discovery, not two.
     *
     * Deterministic from the level seed and the region, persistent, and each
     * person exists at most once in a world -- they are individuals, not a
     * spawn table.
     */
    private void placeResident(Region region) {
        if (this.isClient()) {
            return;
        }
        long seed = (this.getWorldGenSeed() * 0x9E3779B97F4A7C15L)
                ^ ((long) region.regionX * 0x27D4EB2FL)
                ^ ((long) region.regionY * 0x165667B1L);
        GameRandom random = new GameRandom(seed);
        if (!random.getChance(RESIDENT_REGION_CHANCE)) {
            return;
        }
        String who = RESIDENTS[random.nextInt(RESIDENTS.length)];
        // One of each per world. The level's own entity list is the record:
        // asking it is cheaper than a world flag and cannot drift out of sync
        // with what is actually standing there.
        for (Mob existing : this.entityManager.mobs) {
            if (who.equals(existing.getStringID())) {
                return;
            }
        }
        for (int attempt = 0; attempt < 40; attempt++) {
            int tileX = region.tileXOffset + random.getIntBetween(2, region.tileWidth - 3);
            int tileY = region.tileYOffset + random.getIntBetween(2, region.tileHeight - 3);
            if (!this.isTileWithinBounds(tileX, tileY) || this.isSolidTile(tileX, tileY)) {
                continue;
            }
            if (this.getObjectID(tileX, tileY) != 0) {
                continue;
            }
            // Only beside a workshop: the station is the landmark that makes
            // the person findable rather than a figure alone in a field.
            if (!this.hasWorkstationNear(tileX, tileY)) {
                continue;
            }
            Mob mob = MobRegistry.getMob(who, this);
            if (mob == null) {
                return;
            }
            mob.canDespawn = false;
            this.entityManager.addMob(mob, tileX * 32 + 16, tileY * 32 + 16);
            return;
        }
    }

    /** Is one of the three settlement stations within three tiles? */
    private boolean hasWorkstationNear(int tileX, int tileY) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                int id = this.getObjectID(tileX + dx, tileY + dy);
                if (id != 0
                        && (id == stairwaytoheaven.settlement.SkyProfessions.windsilkLoomID
                        || id == stairwaytoheaven.settlement.SkyProfessions.aetherForgeID
                        || id == stairwaytoheaven.settlement.SkyProfessions.stormglassKilnID)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final String[] RESIDENTS = {
            "magpiesettler", "haldasettler", "ossiansettler",
    };

    /** Rare: a workshop with somebody still at it is the exception. */
    private static final float RESIDENT_REGION_CHANCE = 0.16F;

    private void placeLivestockHerds(Region region) {
        // The husbandry animals used to ride the CRITTER SPAWN TABLES,
        // which is a per-tile roll and put them on every corner of the sky.
        // Vanilla never does that with livestock: GenerationTools.spawnMobHerds
        // drops 25-50 sheep on a WHOLE ISLAND, in clumps of 2-6 within 5 tiles
        // of a point (PlainsSurfaceLevel:234). The player's own words:
        // "wertvolle Tiere nicht an jeder Ecke". So they are herds now, each on
        // its own ground and each rare, because each is worth something.
        placeHerd(region, stairwaytoheaven.livestock.SkyLivestock.NIMBUS_YAK,
                SkyRegistry.cloudturfID, YAK_REGION_CHANCE, 0x9E3779B1L);
        placeHerd(region, stairwaytoheaven.livestock.SkyLivestock.GLIMMERGOAT,
                SkyRegistry.auroraShoalID, GOAT_REGION_CHANCE, 0x165667B1L);
    }

    /**
     * One herd of one species, on one ground, at most once per region.
     *
     * Modelled on {@code GenerationTools.spawnMobHerds}: a herd is a clump
     * around a point, not a scatter. Persistent ({@code canDespawn = false}) so
     * a herd the player walked past is still there on return.
     */
    private void placeHerd(Region region, String mobID, int groundID, float chance, long salt) {
        if (this.isClient()) {
            return;
        }
        // Seed mixes the world seed with the region coordinates, so a flock
        // belongs to a place rather than to the order regions happen to load.
        long flockSeed = (this.getWorldGenSeed() * 0x9E3779B97F4A7C15L)
                ^ ((long) region.regionX * salt)
                ^ ((long) region.regionY * 0xC2B2AE3DL);
        GameRandom random = new GameRandom(flockSeed);
        if (!random.getChance(chance)) {
            return;
        }
        int originX = region.tileXOffset + random.getIntBetween(4, region.tileWidth - 5);
        int originY = region.tileYOffset + random.getIntBetween(4, region.tileHeight - 5);
        int wanted = random.getIntBetween(2, 5);
        int placed = 0;
        for (int attempt = 0; attempt < 24 && placed < wanted; attempt++) {
            int tileX = originX + random.getIntBetween(-4, 4);
            int tileY = originY + random.getIntBetween(-4, 4);
            if (!this.isTileWithinBounds(tileX, tileY) || this.isSolidTile(tileX, tileY)) {
                continue;
            }
            if (this.getTileID(tileX, tileY) != groundID || this.getObjectID(tileX, tileY) != 0) {
                continue;
            }
            Mob lamb = MobRegistry.getMob(mobID, this);
            if (lamb == null) {
                return;
            }
            lamb.canDespawn = false;
            this.entityManager.addMob(lamb, tileX * 32 + 16, tileY * 32 + 16);
            placed++;
        }
    }


    // ---- Guarded places ---------------------------------------------------

    /**
     * The packs that stand over the sky's loot.
     *
     * <p>The player, after finishing incursion 10: <i>"es nervt aber wenn die
     * alle 2 Sekunden ueberall angreifen ... sie sollen mal geballt kommen und
     * ein Gebiet z.b bewachen wo es loot gibt in anderen Ecken aber nicht
     * dauernd angeflogen kommen"</i>. {@link stairwaytoheaven.worldgen.SkyPressure}
     * is the half that makes the open ground quiet; this is the half that makes
     * arriving somewhere loud.
     *
     * <p>The two things the sky already builds that are worth guarding are the
     * aeronaut wreck (which carries the sky caches) and the Skywatch workshop
     * (which carries a station and a crate). The guards are found through the
     * same lattice the painter placed those with —
     * {@link SkyTerrainPainter#nearestSite} — so a pack cannot end up standing
     * where the loot is not.
     */
    private void placeGuardPacks(Region region) {
        if (this.isClient()) {
            return;
        }
        placePacksOf(region, SkyTerrainPainter.WRECK_CELL, SkyTerrainPainter.SALT_WRECK,
                SkyTerrainPainter.WRECK_CHANCE, 0x9E3779B1L);
        placePacksOf(region, SkyTerrainPainter.WORKSHOP_CELL, SkyTerrainPainter.SALT_WORKSHOP,
                SkyTerrainPainter.WORKSHOP_CHANCE, 0x85EBCA77L);
    }

    /**
     * Every site of one lattice whose pack reaches into this region.
     *
     * <p><b>Why the loop is over cells and not over tiles.</b> A region is
     * 16x16 tiles ({@code RegionManager.REGION_SIZE}) and a pack is spread over
     * a disc of radius {@link stairwaytoheaven.worldgen.SkyPressure#GUARD_RADIUS}
     * = 7, so a pack routinely straddles four regions. Scanning this region for
     * site centres would place a pack only when the centre happened to land
     * inside it and lose the rest; scanning the cells that could REACH this
     * region, deriving every member's position from the site seed, and placing
     * only the members whose tile falls inside it, puts each guard down exactly
     * once no matter which order the regions generate in.
     */
    private void placePacksOf(Region region, int cell, int salt, float chance, long saltMix) {
        int seed = this.getWorldGenSeed();
        int reach = (int) Math.ceil(stairwaytoheaven.worldgen.SkyPressure.GUARD_RADIUS) + 1;
        int minX = region.tileXOffset - reach;
        int minY = region.tileYOffset - reach;
        int maxX = region.tileXOffset + region.tileWidth + reach;
        int maxY = region.tileYOffset + region.tileHeight + reach;
        for (int cx = Math.floorDiv(minX, cell); cx <= Math.floorDiv(maxX, cell); cx++) {
            for (int cy = Math.floorDiv(minY, cell); cy <= Math.floorDiv(maxY, cell); cy++) {
                if (SkyNoise.hash(seed + salt, cx, cy) >= chance) {
                    continue;
                }
                int siteX = Math.round(cx * cell + SkyNoise.hash(seed + salt + 1, cx, cy) * cell);
                int siteY = Math.round(cy * cell + SkyNoise.hash(seed + salt + 2, cx, cy) * cell);
                if (siteX < minX || siteX > maxX || siteY < minY || siteY > maxY) {
                    continue;
                }
                placePackAt(region, siteX, siteY, saltMix);
            }
        }
    }

    /**
     * One pack, around one site.
     *
     * <p>Every member's tile is a pure function of the site position and the
     * member's index, so the pack is the same in every save and on every
     * client, and a member is placed by whichever region happens to contain
     * its tile. The eight-attempt search per member is what keeps a pack off
     * the Mistsea without moving the site: 61% of the sky is sea, so a fixed
     * offset would drown half of every pack.
     */
    private void placePackAt(Region region, int siteX, int siteY, long saltMix) {
        necesse.level.maps.biomes.Biome biome = this.getBiome(siteX, siteY);
        if (!(biome instanceof stairwaytoheaven.biomes.GuardedBiome)) {
            return;
        }
        stairwaytoheaven.biomes.GuardedBiome.Guard guard =
                ((stairwaytoheaven.biomes.GuardedBiome) biome).getGuard();
        if (guard == null) {
            return;
        }
        long packSeed = (this.getWorldGenSeed() * 0x9E3779B97F4A7C15L)
                ^ ((long) siteX * saltMix)
                ^ ((long) siteY * 0xC2B2AE3DL);
        GameRandom random = new GameRandom(packSeed);
        int size = random.getIntBetween(guard.minSize, guard.maxSize);
        float radius = stairwaytoheaven.worldgen.SkyPressure.GUARD_RADIUS;
        for (int i = 0; i < size; i++) {
            String who = guard.memberAt(i, random.nextFloat());
            // Anchors stand close in, rabble spreads to the edge of the ground
            // the pressure field marks as the site's own.
            float near = i < guard.anchors.length ? 3.0F : radius;
            int tileX = 0;
            int tileY = 0;
            boolean found = false;
            for (int attempt = 0; attempt < 8 && !found; attempt++) {
                tileX = siteX + random.getIntBetween(-(int) near, (int) near);
                tileY = siteY + random.getIntBetween(-(int) near, (int) near);
                found = this.isTileWithinBounds(tileX, tileY)
                        && !this.isSolidTile(tileX, tileY)
                        && this.getTileID(tileX, tileY) != SkyRegistry.mistseaID
                        && this.getObjectID(tileX, tileY) == 0;
            }
            if (!found) {
                continue;
            }
            // Only this region's share: the other members belong to the regions
            // their own tiles fall in, and will be placed when those generate.
            if (tileX < region.tileXOffset || tileX >= region.tileXOffset + region.tileWidth
                    || tileY < region.tileYOffset || tileY >= region.tileYOffset + region.tileHeight) {
                continue;
            }
            Mob mob = MobRegistry.getMob(who, this);
            if (mob == null) {
                continue;
            }
            // Persistent, exactly like the herds and the residents. VERIFIED
            // [jar]: EntityManager.tickMobSpawning counts only
            // (isHostile && canDespawn) against the spawn cap, so a placed
            // guard does not eat the ambient budget -- and the site is still
            // guarded when the player comes back for the crate they left.
            mob.canDespawn = false;
            this.entityManager.addMob(mob, tileX * 32 + 16, tileY * 32 + 16);
        }
    }

    /**
     * Both husbandry animals are rarer than the Cloud Lamb, because each is a
     * production animal rather than scenery: milk and fleece. One yak herd per
     * ~12 regions, goats per ~14 -- the goat is rarest because the Aurora
     * Shoals are the rarest ground.
     */
    private static final float YAK_REGION_CHANCE = 0.085F;
    private static final float GOAT_REGION_CHANCE = 0.070F;

    @Override
    public boolean canRain() {
        // Above the cloud ceiling. Storm weather is a roadmap feature (v0.3).
        return false;
    }

    private int structureHealCounter;

    @Override
    public void serverTick() {
        super.serverTick();
        // The spire is stamped on the FIRST ASCENT near the player's arrival
        // stairway (see ensureWardenSpire(anchor)); the tick only maintains an
        // already-placed spire: cat spawns + healing the quest beacon.
        SkywatchQuestData quest = SkywatchQuestData.get(this);
        if (!quest.spirePlaced) {
            return;
        }
        if (!quest.catsSpawned) {
            this.spawnSpireCats(quest);
        }
        if (++this.structureHealCounter >= 200) {
            this.structureHealCounter = 0;
            this.healQuestStructure(quest);
        }
    }

    /**
     * Restores the quest beacon if it went missing (older jars allowed mining
     * it, which dropped nothing and would soft-lock the chain), and makes sure
     * the cats' basket actually stands on the tile the quest data calls their
     * home. Only touches loaded regions — never forces region loads from the
     * tick.
     */
    private void healQuestStructure(SkywatchQuestData quest) {
        healBeacon(quest);
        healCatBasket(quest);
    }

    private void healBeacon(SkywatchQuestData quest) {
        if (!this.regionManager.isTileLoaded(quest.beaconX, quest.beaconY)) {
            return;
        }
        int current = this.getObjectID(quest.beaconX, quest.beaconY);
        if (current == SkyRegistry.wardenBeaconOffID || current == SkyRegistry.wardenBeaconOnID) {
            return;
        }
        int wanted = quest.stage >= 2 ? SkyRegistry.wardenBeaconOnID : SkyRegistry.wardenBeaconOffID;
        setQuestObject(quest.beaconX, quest.beaconY, wanted);
    }

    /**
     * The spire's cat basket.
     *
     * WardenSpirePreset reserves local (5,6) as BASKET_X/BASKET_Y and records it
     * in SkywatchQuestData, and SpireCatMob teleports a coaxed cat exactly
     * there — but the preset never placed anything on that tile, so "home" was
     * a bare floor square in a tower the player had already left. That is the
     * mechanical half of "Siggi gefunden und Snack gegeben aber danach nie
     * wieder gesehen": there was nothing at the destination to see.
     *
     * Healing it here rather than in the preset is deliberate: every world that
     * already stamped its spire gets the basket too, without re-stamping (and
     * the preset belongs to the worldgen agent). The basket is a FurnitureObject
     * with a 0x0 collision, so it is NOT solid — the cat stands on it the way a
     * pet sits on a pet bed, and the tile stays walkable.
     */
    private void healCatBasket(SkywatchQuestData quest) {
        // ONCE per world, unlike the beacon. The beacon is unbreakable and
        // healing it can never mint anything; the basket is ordinary furniture
        // with no recipe anywhere, so re-placing it on every empty tile would
        // turn a quest reward into a ten-second farm.
        if (quest.basketPlaced || SkyRegistry.catBasketID <= 0
                || !this.regionManager.isTileLoaded(quest.basketX, quest.basketY)) {
            return;
        }
        // Only ever fill an EMPTY tile: a player who put something of their own
        // in the tower keeps it (and the flag is set either way, so we do not
        // come back and argue about it every ten seconds).
        if (this.getObjectID(quest.basketX, quest.basketY) == 0) {
            setQuestObject(quest.basketX, quest.basketY, SkyRegistry.catBasketID);
        }
        quest.basketPlaced = true;
    }

    private void setQuestObject(int tileX, int tileY, int objectID) {
        this.setObject(tileX, tileY, objectID);
        if (this.getServer() != null) {
            this.getServer().network.sendToClientsWithTile(
                    new necesse.engine.network.packet.PacketChangeObject(this, 0, tileX, tileY, objectID),
                    this, tileX, tileY);
        }
    }

    /**
     * Lazily stamps the Warden's Spire and spawns the spire cats, exactly
     * once per world (persisted in SkywatchQuestData).
     *
     * v0.5: the spire is THE canonical sky origin (see {@link SkyOrigin}) —
     * every Stairway ascends to it, and the terrain painter guarantees a solid
     * Driftlands hub island around this exact position, so no site search is
     * needed anymore (the old player-anchored sweep is gone: stairway placement
     * on the surface must not influence sky geography, or players could
     * relocate the hub — and with it the whole difficulty gradient — by
     * placing stairways far from spawn).
     */
    public void ensureWardenSpire() {
        SkywatchQuestData quest = SkywatchQuestData.get(this);
        if (!quest.spirePlaced) {
            Point site = SkyOrigin.compute(this.getWorldGenSeed());
            int half = WardenSpirePreset.SIZE / 2;
            this.regionManager.ensureTilesAreLoaded(site.x - half - 2, site.y - half - 2,
                    site.x + half + 2, site.y + half + 2);
            new WardenSpirePreset().applyToLevelCentered(this, site.x, site.y);
            // applyToLevel runs the custom-apply hook, which sets spirePlaced
            // and the quest anchor points; guard against a silent failure so
            // we never re-stamp every tick.
            quest.spirePlaced = true;
        }
        if (!quest.catsSpawned && quest.spirePlaced) {
            this.spawnSpireCats(quest);
        }
        // Immediately, not in ten seconds' time: the spire's own regions are
        // loaded right now, and an arriving player should never see the tower
        // without its beacon or its cat basket.
        if (quest.spirePlaced) {
            this.healQuestStructure(quest);
        }
    }

    /** First land spot in the right sub-biome, sweeping outward from the spire. */
    private void spawnSpireCats(SkywatchQuestData quest) {
        int seed = this.getWorldGenSeed();
        Point blackLair = this.findLairSite(seed, quest, true);
        Point tabbyLair = this.findLairSite(seed, quest, false);
        quest.blackLairX = blackLair.x;
        quest.blackLairY = blackLair.y;
        quest.tabbyLairX = tabbyLair.x;
        quest.tabbyLairY = tabbyLair.y;

        this.regionManager.ensureTileIsLoaded(blackLair.x, blackLair.y);
        this.entityManager.addMob(MobRegistry.getMob("spirecatblack", this), blackLair.x * 32 + 16, blackLair.y * 32 + 16);
        this.regionManager.ensureTileIsLoaded(tabbyLair.x, tabbyLair.y);
        this.entityManager.addMob(MobRegistry.getMob("spirecattabby", this), tabbyLair.x * 32 + 16, tabbyLair.y * 32 + 16);
        quest.catsSpawned = true;
    }

    /** First land spot in the right sub-biome, sweeping outward from the spire. */
    private Point findLairSite(int seed, SkywatchQuestData quest, boolean stormveil) {
        for (int radius = 48; radius <= 600; radius += 8) {
            for (int angleStep = 0; angleStep < 20; angleStep++) {
                double angle = (angleStep / 20.0 + (stormveil ? 0.0 : 0.5) / 20.0 + (radius % 16) / 40.0) * Math.PI * 2;
                int x = quest.spireX + (int) Math.round(Math.cos(angle) * radius);
                int y = quest.spireY + (int) Math.round(Math.sin(angle) * radius);
                float island = SkyNoise.fbm(seed, x, y, SkyTerrainPainter.ISLAND_SCALE, 3);
                if (island <= SkyTerrainPainter.ISLAND_THRESHOLD + SkyTerrainPainter.ISLAND_RIM) {
                    continue;
                }
                float biome = SkyNoise.fbm(seed + SkyTerrainPainter.SALT_BIOME, x, y, SkyTerrainPainter.BIOME_SCALE, 2);
                boolean matches = stormveil
                        ? biome < SkyTerrainPainter.STORMVEIL_BELOW
                        : biome > SkyTerrainPainter.AURORA_ABOVE;
                if (matches) {
                    return new Point(x, y);
                }
            }
        }
        // Fallback: beside the spire, so the quest is never soft-locked
        return new Point(quest.spireX + (stormveil ? -3 : 3), quest.spireY + 3);
    }

    /**
     * Seed used by the terrain painter. The lazy level-creation path passes no
     * explicit seed (vanilla cave levels then fall back to the world's shared
     * generator stack), so we derive a per-world seed from the persisted world
     * seed string, salted so the sky never mirrors another layer's layout. An
     * explicit non-zero seed (tests, tools) takes precedence.
     *
     * The derivation itself lives in {@link SkyOrigin#worldGenSeed} so surface
     * code (stairway portals) computes identical sky positions.
     */
    public int getWorldGenSeed() {
        if (this.seed != 0) {
            return this.seed;
        }
        return SkyOrigin.worldGenSeed(this.getWorldEntity());
    }
}
