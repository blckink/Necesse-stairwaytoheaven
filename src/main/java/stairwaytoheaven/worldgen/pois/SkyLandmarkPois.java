package stairwaytoheaven.worldgen.pois;

import java.awt.Point;

import necesse.engine.network.server.Server;
import necesse.engine.util.GameRandom;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.quest.SkywatchQuestData;
import stairwaytoheaven.quest.SkywatchWorldData;
import stairwaytoheaven.worldgen.SkyNoise;
import stairwaytoheaven.worldgen.SkyOrigin;
import stairwaytoheaven.worldgen.SkyTerrainPainter;

/**
 * §0.6's third rarity: the places there is exactly ONE of in a world.
 *
 * <h2>Why they are not lattice kinds</h2>
 * {@code chapter-01-skyreach-pois.md} §0.6 names three mechanisms and the mod
 * had only ever built one. A kind in {@link RealmPoiWorldPreset#REALM_KINDS} is
 * §0.6's <b>common</b> row by definition — "one designed place per 72x72-tile
 * cell, two cells out of three ... every few minutes of walking" — and the
 * Skyway Toll-House had been running as one since 2026-09-09. Its whole point
 * is that the loot is a person: a world with four toll-houses has four Magpies
 * standing in four ledger rooms, or (once the settler claim refuses the second)
 * three empty ones. §0.6's own words for these three are <i>"a seed-derived
 * site in a stated distance band from {@code SkyOrigin}, stamped lazily exactly
 * the way {@code SkyLevel.ensureWardenSpire} stamps the Spire, and recorded in
 * world data so it is never re-stamped"</i>, and that is what this class is.
 *
 * <h2>Where the "already stamped" record lives, and why</h2>
 * In {@link SkywatchQuestData} — {@code LevelData} on the Skyreach, next to
 * {@code spirePlaced} — and NOT in {@link SkywatchWorldData}, which is what the
 * word "world data" would suggest. The reason is the one
 * {@code SkywatchWorldData}'s own header gives: bumping
 * {@code SkyRegistry.WORLD_GENERATION} starts a FRESH Skyreach, and a
 * world-scoped "already stamped" would mean that new sky never gets a
 * toll-house, a cellar or a range at all, in any world that had ever ascended.
 * The spire has exactly this shape and is exactly this kind of record.
 *
 * <p>The SETTLER is the other half and it is world-scoped, in
 * {@code SkywatchWorldData.residentsClaimed}, because a recruited Magpie is
 * asleep in a settlement on the surface — a level this class will never see.
 * The two halves are checked independently on every call, so a
 * {@code /swhreset} that removes the people leaves the buildings alone and
 * puts the people back.
 */
public final class SkyLandmarkPois {

    private SkyLandmarkPois() {
    }

    /**
     * The three, with the band each is looked for in and the biome it belongs
     * to. Order is the order they are stamped in, which is also the order the
     * census prints.
     */
    public static final int[] KINDS = {
            RealmPoiPresets.SKY_TOLL_HOUSE,
            RealmPoiPresets.SKY_GRANGE_CELLAR,
            RealmPoiPresets.SKY_TEST_RANGE,
    };

    /** The biome a landmark's centre wants, as {@link #biomeOf} names them. */
    public static final int BIOME_STORMVEIL = 0, BIOME_SKYWAY = 1,
            BIOME_DRIFTLANDS = 2, BIOME_AURORA = 3;

    private static final String[] BIOME_NAMES = {"stormveil", "skyway", "driftlands", "aurora"};

    /**
     * Where each one is looked for: {min radius, max radius} in tiles from
     * {@link SkyOrigin}, and the biome §2.12-§2.14 puts it in.
     *
     * <p>The bands are the dossier's power order (§0.7) read as distance, which
     * is the only ordering the Skyreach has: the toll-house is "any" and sits
     * just outside the spire's own 100-tile clear ring and inside
     * {@code SkyOrigin.CORE_RADIUS}; the cellar is a step further out; the
     * range is the Stormveil, which is the far half of the band anyway. All
     * three stay well inside {@code RealmDepth}'s Skyreach reach so a landmark
     * can never land in Eden.
     */
    private static final int[][] BANDS = {
            {180, 700},   // toll-house: on the passages, close enough to be first
            {400, 1100},  // grange cellar
            {700, 1700},  // test range
    };

    private static final int[] BIOMES = {BIOME_SKYWAY, BIOME_DRIFTLANDS, BIOME_STORMVEIL};

    /** How the site search ended, for the census to print. */
    public static final int TIER_BIOME = 0, TIER_ANY_LAND = 1, TIER_FALLBACK = 2;

    /** Human-readable {@link #TIER_BIOME} names. */
    public static final String[] TIER_NAMES = {"biome", "anyland", "fallback"};

    /** Which settler each landmark seats, by {@link #KINDS} index; "" for none. */
    private static final String[] SETTLERS = {
            stairwaytoheaven.settlement.SkySettlers.MAGPIE,
            stairwaytoheaven.settlement.SkySettlers.HALDA,
            stairwaytoheaven.settlement.SkySettlers.OSSIAN,
    };

    /** Where in each plan that settler stands (§2.12 (18,5), §2.13 (12,10), §2.14 (20,6)). */
    private static final int[][] SETTLER_AT = {{18, 5}, {12, 10}, {20, 6}};

    /**
     * The one enemy each landmark keeps, by {@link #KINDS} index.
     *
     * <p>{@code docs/design/chapter-01-skyreach-cast.md} §2: <i>"None of these
     * spawn on the open map. Each lives in one structure, and each is the
     * reason its POI is a fight instead of a container."</i> This array is the
     * whole of "does not spawn on the open map" — none of the four new mobs is
     * on any biome spawn table, so this is the only thing in the game that
     * places them.
     */
    private static final String[] GUARDS = {"tollwright", "sourvatbloom", "prototypenine"};

    /**
     * Where each guard stands: §2.12 <i>"The Tollwright stands at (16,12), on
     * the vault floor directly in front of the Lockbox"</i>, §2.13 <i>"The
     * Sourvat Bloom sits at (7,8), between the four vats"</i>, §2.14
     * <i>"Prototype Nine lies at (9,15) in the largest crater"</i>.
     */
    private static final int[][] GUARD_AT = {{16, 12}, {7, 8}, {9, 15}};

    /**
     * The unique rewards of {@code chapter-01-skyreach-cast.md} §3 that lie in
     * a container rather than in a mob, as
     * {@code {tileX, tileY, min, max}} plus the item ID.
     *
     * <p>Five of the eight are here; the other three are recruit keys and are
     * carried by the guards above ({@code TollwrightMob} is the exception — its
     * key is the Lockbox on the stand it is standing in front of, which is what
     * §2.12's object table says). Every tile is read straight off the object
     * table of its section:
     *
     * <ul>
     *   <li>§2.12 (16,11) {@code skywatchdisplay} — <i>"the Bonded
     *       Lockbox"</i>; (16,6) — <i>"the Skyway Writ"</i>; (19,6)
     *       {@code skywatchcabinet} takes the Ledger of Undelivered Post,
     *       because §2.12 draws the Ledger as the {@code skywatchtome} ON
     *       Magpie's desk and a table decoration is scenery, not an item a
     *       player can pick up.</li>
     *   <li>§2.13 (12,6) the {@code barrel} on its one chequer tile —
     *       <i>"the Warden's Round"</i>; (13,10) {@code skywatchcabinet} takes
     *       the Skywatch Signet. §3 puts the Signet in the spire archive
     *       "when the household is whole", which is a story gate nothing has
     *       built and a building every existing save already stamped; the last
     *       of the Skywatch's household keeping the household's signet in the
     *       cell she never left is the reachable version of the same sentence.
     *       Recorded in {@code state/decisions.json}.</li>
     *   <li>§2.14 (19,6) {@code skywatchdisplay} — drawn as the Storm Lens Core
     *       "after the fight", but the Core comes out of Prototype Nine, so the
     *       stand carries the other half of what §2.14 promises: <i>"the
     *       prototype cache of Aetherwright's Casings"</i>.</li>
     * </ul>
     */
    private static final Reward[][] REWARDS = {
            {new Reward(16, 11, "bondedlockbox", 1, 1),
             new Reward(16, 6, "skywaywrit", 1, 1),
             new Reward(19, 6, "postledger", 1, 1)},
            {new Reward(12, 6, "wardensround", 1, 1),
             new Reward(13, 10, "skywatchsignet", 1, 1)},
            {new Reward(19, 6, "aetherwrightcasing", 2, 4)},
    };

    /** One reward, and the container tile of its own plan that holds it. */
    private static final class Reward {
        final int x;
        final int y;
        final String itemID;
        final int min;
        final int max;

        Reward(int x, int y, String itemID, int min, int max) {
            this.x = x;
            this.y = y;
            this.itemID = itemID;
            this.min = min;
            this.max = max;
        }

        LootTable table() {
            return new LootTable(this.min == this.max
                    ? new LootItem(this.itemID, this.min)
                    : LootItem.between(this.itemID, this.min, this.max));
        }
    }

    /** The index in {@link #KINDS} of a kind, or -1 if it is a lattice kind. */
    public static int indexOf(int kind) {
        for (int i = 0; i < KINDS.length; i++) {
            if (KINDS[i] == kind) {
                return i;
            }
        }
        return -1;
    }

    /** Which settler a landmark seats, or {@code ""}. */
    public static String settlerOf(int index) {
        return SETTLERS[index];
    }

    /**
     * The recruit key each guard carries in its own loot table, or {@code ""}.
     *
     * <p>Two of §3's eight rewards are boss loot rather than container loot —
     * the Mother comes out of the Sourvat Bloom and the Storm Lens Core out of
     * Prototype Nine — and both are guaranteed drops, because they are Halda's
     * and Vane's recruit keys and a failed roll would be a world in which they
     * can never be hired. The Toll-House's key is on a display stand instead
     * (§2.12), so its entry is empty. The census rolls these to prove it.
     */
    private static final String[] GUARD_KEYS = {"", "themother", "stormlenscore"};

    /** Which enemy a landmark keeps. */
    public static String guardOf(int index) {
        return GUARDS[index];
    }

    /** The recruit key that guard drops, or {@code ""} if its place holds it. */
    public static String guardKeyOf(int index) {
        return GUARD_KEYS[index];
    }

    /** The tile that guard stands on, once the landmark's corner is known. */
    public static Point guardTile(int index, Point corner) {
        return new Point(corner.x + GUARD_AT[index][0], corner.y + GUARD_AT[index][1]);
    }

    /** How many unique rewards a landmark's containers carry. */
    public static int rewardCount(int index) {
        return REWARDS[index].length;
    }

    /** The item ID of one of them. */
    public static String rewardItem(int index, int slot) {
        return REWARDS[index][slot].itemID;
    }

    /** The container tile it lies in, once the landmark's corner is known. */
    public static Point rewardTile(int index, int slot, Point corner) {
        return new Point(corner.x + REWARDS[index][slot].x, corner.y + REWARDS[index][slot].y);
    }

    /** The tile that settler stands on, once the landmark's corner is known. */
    public static Point settlerTile(int index, Point corner) {
        return new Point(corner.x + SETTLER_AT[index][0], corner.y + SETTLER_AT[index][1]);
    }

    /** What a site search found: the top-left corner and which tier answered. */
    public static final class Site {
        public final int x;
        public final int y;
        public final int tier;

        Site(int x, int y, int tier) {
            this.x = x;
            this.y = y;
            this.tier = tier;
        }

        public String tierName() {
            return TIER_NAMES[this.tier];
        }
    }

    /** The band a landmark is looked for in, as {@code "min-max"}. */
    public static String bandOf(int index) {
        return BANDS[index][0] + "-" + BANDS[index][1];
    }

    /** The biome a landmark wants, by name. */
    public static String biomeNameOf(int index) {
        return BIOME_NAMES[BIOMES[index]];
    }

    /**
     * Which of the four sky biomes a tile is in, in
     * {@link SkyTerrainPainter}'s own thresholds. Read off the same fbm the
     * painter reads, so this cannot drift from the ground it describes.
     */
    public static int biomeOf(int seed, int x, int y) {
        float biome = SkyNoise.fbm(seed + SkyTerrainPainter.SALT_BIOME, x, y,
                SkyTerrainPainter.BIOME_SCALE, 2);
        if (biome < SkyTerrainPainter.STORMVEIL_BELOW) return BIOME_STORMVEIL;
        if (biome < SkyTerrainPainter.SKYWAY_BELOW) return BIOME_SKYWAY;
        if (biome <= SkyTerrainPainter.AURORA_ABOVE) return BIOME_DRIFTLANDS;
        return BIOME_AURORA;
    }

    /**
     * The site one landmark stands on in this world: a pure function of the
     * seed, so the census can recompute it without reading a save and a repair
     * pass can never move a building that is already standing.
     *
     * <p>The sweep is {@code SkyLevel.findLairSite}'s, which is the shape this
     * codebase already uses to answer "the first ground of the right kind,
     * outward from the origin": rings of radius from the band's inner edge,
     * twenty angles each, the whole ring turned by a seed-derived offset so two
     * worlds do not put their toll-house on the same bearing. A ring is
     * accepted when all nine footprint samples are land AND the centre is in
     * the right biome; if the whole band offers no such ring the same sweep is
     * run again asking only for land, and the tier says which answered. The
     * hard fallback is the band's inner edge due east, which is never reached
     * on a world with any land in it — but a place that cannot be found is
     * worse than a place in the wrong biome, and both are better than a
     * settler nobody can reach.
     */
    public static Site site(int seed, int index) {
        Snapshot cached = CACHE;
        if (cached != null && cached.seed == seed) {
            return cached.sites[index];
        }
        Site[] fresh = new Site[KINDS.length];
        for (int i = 0; i < fresh.length; i++) {
            fresh[i] = search(seed, i);
        }
        CACHE = new Snapshot(seed, fresh);
        return fresh[index];
    }

    /** One world's three sites and the seed they belong to, published together. */
    private static final class Snapshot {
        final int seed;
        final Site[] sites;

        Snapshot(int seed, Site[] sites) {
            this.seed = seed;
            this.sites = sites;
        }
    }

    /**
     * The three sites of one world, remembered.
     *
     * <p>{@link RealmPoiWorldPreset#survey} asks for them once per preset
     * region and the census walks 169 of those, so the sweep below would run
     * five hundred times over the same seed for the same three answers. A
     * server holds one world; a re-derivation on a second seed simply replaces
     * the snapshot, and because {@link #search} is a pure function of the seed
     * a torn read can only ever hand back the right answer for the wrong world,
     * which the seed check then rejects. Seed and sites are published as one
     * object so the check and the answer can never come from different worlds.
     */
    private static volatile Snapshot CACHE;

    private static Site search(int seed, int index) {
        int kind = KINDS[index];
        int width = RealmPoiPresets.width(kind);
        int height = RealmPoiPresets.height(kind);
        int originX = SkyOrigin.originX(seed);
        int originY = SkyOrigin.originY(seed);
        double turn = SkyNoise.hash(seed + 0x1A4D + index, 0, 0) * Math.PI * 2;
        for (int pass = TIER_BIOME; pass <= TIER_ANY_LAND; pass++) {
            for (int radius = BANDS[index][0]; radius <= BANDS[index][1]; radius += 8) {
                for (int step = 0; step < 24; step++) {
                    double angle = turn + (step / 24.0 + (radius % 16) / 384.0) * Math.PI * 2;
                    int centreX = originX + (int) Math.round(Math.cos(angle) * radius);
                    int centreY = originY + (int) Math.round(Math.sin(angle) * radius);
                    if (pass == TIER_BIOME && biomeOf(seed, centreX, centreY) != BIOMES[index]) {
                        continue;
                    }
                    int x = centreX - width / 2;
                    int y = centreY - height / 2;
                    if (!allLand(seed, x, y, width, height)) {
                        continue;
                    }
                    return new Site(x, y, pass);
                }
            }
        }
        return new Site(originX + BANDS[index][0] - width / 2, originY - height / 2, TIER_FALLBACK);
    }

    /** The nine-sample ground test {@link RealmPoiWorldPreset} uses, on sky ground. */
    private static boolean allLand(int seed, int x, int y, int width, int height) {
        for (int sx = 0; sx <= 2; sx++) {
            for (int sy = 0; sy <= 2; sy++) {
                if (!skyLand(seed, x + sx * (width - 1) / 2, y + sy * (height - 1) / 2)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean skyLand(int seed, int x, int y) {
        long description = SkyTerrainPainter.describeTile(seed, x, y,
                SkyOrigin.originX(seed), SkyOrigin.originY(seed));
        return SkyTerrainPainter.descTile(description) != SkyRegistry.mistseaID;
    }

    /**
     * Stamps whichever of the three this world has not stamped yet, and seats
     * whichever settler this world has not seated yet.
     *
     * <p>Called from {@code SkyLevel.ensureWardenSpire}, i.e. on the first
     * ascent and on every {@code /skyreachstatus}. Server side only, like every
     * other placement in this mod: spawning a mob on both ends is how you get
     * two of somebody.
     *
     * <p>The two halves are deliberately independent. A building is a fact
     * about this Skyreach and is remembered in {@link SkywatchQuestData}; a
     * settler is a fact about the world and is claimed in
     * {@link SkywatchWorldData}. {@code /swhreset quests} removes the named
     * people and can clear their claims, and a single combined flag would then
     * leave three buildings standing with nobody in them and no way back.
     */
    public static void ensureAll(Level level) {
        if (level.isClient()) {
            return;
        }
        SkywatchQuestData quest = SkywatchQuestData.get(level);
        Server server = level.getServer();
        // Off the world entity rather than off SkyLevel, so this class does not
        // have to know which Level subclass it was handed.
        int seed = SkyOrigin.worldGenSeed(level.getWorldEntity());
        for (int index = 0; index < KINDS.length; index++) {
            int kind = KINDS[index];
            String key = RealmPoiPresets.key(kind);
            Site site = site(seed, index);
            int width = RealmPoiPresets.width(kind);
            int height = RealmPoiPresets.height(kind);
            if (!quest.landmarksStamped.contains(key)) {
                level.regionManager.ensureTilesAreLoaded(site.x - 1, site.y - 1,
                        site.x + width, site.y + height);
                // The same GameRandom seeding the lattice path uses, derived
                // from the world seed and the kind so the cache in a landmark
                // is rolled once and stays rolled.
                RealmPoiPresets.build(kind, new GameRandom(seed * 31L + kind))
                        .applyToLevel(level, site.x, site.y);
                quest.landmarksStamped.add(key);
            }
            // Three independent questions, each with its own record. See
            // SkywatchQuestData.landmarkGuards for why they are not one flag:
            // the buildings were stamped and deployed before the enemies and
            // the rewards existed, so a save that already holds the walls has
            // to be able to receive both afterwards.
            seatGuard(level, quest, index, site, width, height);
            placeRewards(level, quest, index, site, width, height, seed);
            seatSettler(level, server, index, site, width, height);
        }
    }

    /**
     * Puts a landmark's one enemy on the tile its plan draws it on.
     *
     * <p>{@code spawnTilePosition} is set AFTER the mob is added, and that
     * order is not cosmetic: VERIFIED [jar], {@code Mob.onLevelChanged}
     * (Mob.java:741) sets the field back to null, so anything written before
     * {@code addMob} is thrown away. {@code AshGolemMob.AshGolemAI} bases both
     * its target finder and its wanderer on that point
     * (AshGolemMob.java:225, :249), which is the whole of §2's <i>"owns a room
     * rather than chasing"</i> — without it the Tollwright wanders the world
     * instead of the vault.
     *
     * <p>{@code canDespawn = false}, like every other placed mob here: a boss
     * the player retreated from has to still be there on the way back.
     */
    private static void seatGuard(Level level, SkywatchQuestData quest, int index, Site site,
            int width, int height) {
        String key = RealmPoiPresets.key(KINDS[index]);
        if (quest.landmarkGuards.contains(key)) {
            return;
        }
        level.regionManager.ensureTilesAreLoaded(site.x - 1, site.y - 1,
                site.x + width, site.y + height);
        necesse.entity.mobs.Mob guard =
                necesse.engine.registries.MobRegistry.getMob(GUARDS[index], level);
        if (guard == null) {
            return;
        }
        int tileX = site.x + GUARD_AT[index][0];
        int tileY = site.y + GUARD_AT[index][1];
        guard.canDespawn = false;
        level.entityManager.addMob(guard, tileX * 32 + 16, tileY * 32 + 16);
        guard.setSpawnTilePosition(tileX, tileY);
        quest.landmarkGuards.add(key);
    }

    /**
     * Writes §3's unique rewards into the containers §2.12-§2.14 stamp empty.
     *
     * <p>This is {@code Preset.addInventory}'s body without the preset: the
     * same master-object resolution, the same {@code implementsOEInventory}
     * test and the same {@code LootTable.applyToLevel} call
     * (Preset.java:1717-1752). It cannot BE {@code addInventory}, because that
     * only ever runs while a preset is being applied — i.e. never again in a
     * world whose landmarks are already stamped, which is every save the
     * players are carrying.
     *
     * <p>The random is derived from the seed and the kind, like the one the
     * stamp itself is built with, so the Casing cache is rolled once and stays
     * rolled.
     */
    private static void placeRewards(Level level, SkywatchQuestData quest, int index, Site site,
            int width, int height, int seed) {
        String key = RealmPoiPresets.key(KINDS[index]);
        if (quest.landmarkLoot.contains(key)) {
            return;
        }
        level.regionManager.ensureTilesAreLoaded(site.x - 1, site.y - 1,
                site.x + width, site.y + height);
        GameRandom random = new GameRandom(seed * 31L + KINDS[index] + 0x5EED);
        boolean placedAll = true;
        for (Reward reward : REWARDS[index]) {
            if (!fill(level, site.x + reward.x, site.y + reward.y, reward.table(), random)) {
                placedAll = false;
            }
        }
        // Only claimed when every container really took its item. A half-filled
        // landmark left claimed is a Bonded Lockbox that exists nowhere, and
        // the player has no way to ask for it again.
        if (placedAll) {
            quest.landmarkLoot.add(key);
        }
    }

    /** One container. False when there is nothing at that tile to put an item in. */
    private static boolean fill(Level level, int tileX, int tileY, LootTable table,
            GameRandom random) {
        necesse.level.maps.LevelObject at = level.getLevelObject(tileX, tileY);
        if (at == null) {
            return false;
        }
        necesse.level.maps.LevelObject master = at.getMasterLevelObject().orElse(null);
        int x = master == null ? tileX : master.tileX;
        int y = master == null ? tileY : master.tileY;
        necesse.entity.objectEntity.ObjectEntity entity =
                level.entityManager.getObjectEntity(x, y);
        if (entity == null || !entity.implementsOEInventory()) {
            return false;
        }
        table.applyToLevel(random,
                level.buffManager.getModifier(necesse.level.maps.levelBuffManager.LevelModifiers.LOOT),
                level, x, y, level);
        return true;
    }

    /**
     * Puts a landmark's one settler on the tile its plan gives them, unless
     * this world already has that person somewhere.
     *
     * <p>{@code canDespawn = false}, like every other placed person: a recruit
     * the player walked past has to still be there on the way back.
     */
    private static void seatSettler(Level level, Server server, int index, Site site,
            int width, int height) {
        String who = SETTLERS[index];
        if (who.isEmpty() || server == null) {
            return;
        }
        if (SkywatchWorldData.residentClaimed(server, who)) {
            return;
        }
        Point at = settlerTile(index, new Point(site.x, site.y));
        level.regionManager.ensureTilesAreLoaded(site.x - 1, site.y - 1,
                site.x + width, site.y + height);
        necesse.entity.mobs.Mob mob = necesse.engine.registries.MobRegistry.getMob(who, level);
        if (mob == null) {
            return;
        }
        mob.canDespawn = false;
        level.entityManager.addMob(mob, at.x * 32 + 16, at.y * 32 + 16);
        SkywatchWorldData.claimResident(server, who);
    }
}
