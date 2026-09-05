package stairwaytoheaven;

import necesse.engine.commands.CommandsManager;
import necesse.engine.modLoader.annotations.ModEntry;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.LevelDataRegistry;
import necesse.engine.registries.LevelRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.network.server.Server;
import necesse.engine.world.WorldGenerator;
import necesse.level.maps.Level;
import stairwaytoheaven.biomes.AuroraShoalsBiome;
import stairwaytoheaven.biomes.DriftlandsBiome;
import stairwaytoheaven.biomes.SkywayBiome;
import stairwaytoheaven.biomes.StormveilBiome;
import stairwaytoheaven.biomes.GloomfenBiome;
import stairwaytoheaven.biomes.AshenReachBiome;
import stairwaytoheaven.commands.SkyreachStatusCommand;
import stairwaytoheaven.commands.VeilStatusCommand;
import stairwaytoheaven.level.SkyLevel;
import stairwaytoheaven.objects.SkySideStairwayObject;
import stairwaytoheaven.objects.SkywardStairwayObject;
import stairwaytoheaven.objects.SeanceCircleObject;
import stairwaytoheaven.objects.VeilRiftObject;
import stairwaytoheaven.objects.VeilSideRiftObject;
import stairwaytoheaven.quest.SkywatchQuestData;
import stairwaytoheaven.tiles.CloudturfTile;
import stairwaytoheaven.tiles.MistseaTile;
import stairwaytoheaven.tiles.SkystoneTile;
import stairwaytoheaven.tiles.StormslateTile;
import stairwaytoheaven.tiles.MurkmossTile;
import stairwaytoheaven.tiles.BlackpeatTile;
import stairwaytoheaven.tiles.AshsandTile;
import stairwaytoheaven.tiles.MurkwaterTile;

/**
 * Stairway to Heaven — adds the Skyreach, a persistent sky dimension one layer
 * above the surface, reached through a craftable stairway pair.
 *
 * Lifecycle (see docs/ARCHITECTURE.md):
 *  - init():          all registry entries (dimension, level, biomes, tiles,
 *                     objects, mobs, items) — registries close right after.
 *  - initResources(): client-side texture loading for mobs.
 *  - postInit():      recipes, loot hooks and the WorldGenerator that
 *                     instantiates the Skyreach (that registry closes after
 *                     postInit).
 */
@ModEntry
public class StairwayToHeavenMod {

    public void init() {
        registerDimension();
        registerBiomes();
        registerTiles();
        registerObjects();
        SkyBuildingSet.register();
        SkyFurnitureSet.register();
        SkyCloudmarbleSet.register();
        SkyMobs.register();
        SkyItems.register();
        stairwaytoheaven.realms.eden.EdenRealm.register();
        stairwaytoheaven.realms.steinfeld.SteinfeldRealm.register();
        stairwaytoheaven.realms.crooked.CrookedRealm.register();
        stairwaytoheaven.realms.ghost.GhostRealm.register();
        // Dense, road-connected settlements and civic POIs across all six
        // realm bands. They are one WorldPreset catalogue on the one plane,
        // not extra levels; placement shares vanilla's "villages" collision
        // board with every older structure.
        necesse.engine.registries.WorldPresetRegistry.registerPreset(
                stairwaytoheaven.worldgen.pois.RealmPoiWorldPreset.STRING_ID,
                new stairwaytoheaven.worldgen.pois.RealmPoiWorldPreset());
        stairwaytoheaven.arsenal.SkyArsenal.register();
        stairwaytoheaven.arsenal.SkyArsenal.registerItems();
        stairwaytoheaven.settlement.SkyProfessions.register();
        stairwaytoheaven.livestock.SkyLivestock.register();
        SkyBuildingSet.registerItems();
        stairwaytoheaven.settlement.SkyProfessions.registerItems();
        stairwaytoheaven.surface.SkySurface.register();
        stairwaytoheaven.surface.SkySurface.registerItems();
        LevelDataRegistry.registerLevelData(SkywatchQuestData.KEY, SkywatchQuestData.class);
        // World-scoped truth of "this world already has a Warden". Lives in
        // the world entity rather than the Skyreach level, so a generation
        // bump (which starts a fresh level) cannot make the world forget the
        // Warden the player already paid for. See SkywatchWorldData.
        necesse.engine.registries.WorldDataRegistry.registerWorldData(
                stairwaytoheaven.quest.SkywatchWorldData.KEY,
                stairwaytoheaven.quest.SkywatchWorldData.class);
        // The Crooked House, scattered through the Beetlefreak Hollows by
        // vanilla's own world-preset machinery. The Hollows are now a band of
        // the sky plane (WORLD_DESIGN §41.5) and SkyLevel.generateRegion
        // already brackets its painting with startPresetGenerationInRegion /
        // runPresetGenerationInRegion, so registering here is all that is left.
        necesse.engine.registries.WorldPresetRegistry.registerPreset("swh_crookedhouse",
                new stairwaytoheaven.worldgen.CrookedHouseWorldPreset());
        // HUD quest layer: real journal/sidebar quests mirroring the Warden
        // chain (see docs/research/quest-api.md). Items must already be
        // registered — the delivery quests reference them by stringID.
        necesse.engine.registries.QuestRegistry.registerQuest("swh_findspire", stairwaytoheaven.quest.FindSpireQuest.class);
        necesse.engine.registries.QuestRegistry.registerQuest("swh_recruitwarden", stairwaytoheaven.quest.RecruitWardenQuest.class);
        necesse.engine.registries.QuestRegistry.registerQuest("swh_beacon", stairwaytoheaven.quest.BeaconDeliveryQuest.class);
        necesse.engine.registries.QuestRegistry.registerQuest("swh_cats", stairwaytoheaven.quest.SpireCatsQuest.class);
        necesse.engine.registries.QuestRegistry.registerQuest("swh_anchor", stairwaytoheaven.quest.AnchorDeliveryQuest.class);
        // One short chain per new realm (Eden, Ghost, Crooked Beyond) — see
        // each quest class's own doc for the story and the reward.
        necesse.engine.registries.QuestRegistry.registerQuest("swh_edenreach", stairwaytoheaven.quest.EdenArrivalQuest.class);
        necesse.engine.registries.QuestRegistry.registerQuest("swh_edenplants", stairwaytoheaven.quest.EdenPlantsQuest.class);
        necesse.engine.registries.QuestRegistry.registerQuest("swh_eleanor", stairwaytoheaven.quest.EleanorQuest.class);
        necesse.engine.registries.QuestRegistry.registerQuest("swh_crookedarrival", stairwaytoheaven.quest.CrookedArrivalQuest.class);
        necesse.engine.registries.QuestRegistry.registerQuest("swh_crookeddoor", stairwaytoheaven.quest.CrookedDoorQuest.class);
        // One region key per realm with a boss portal — the Warden's line after
        // "The Warden's Call" (docs/FOGKEY_AND_BOSSPORTALS.md §B1). Each asks
        // for materials only its own realm drops and pays the buildable key
        // piece that unlocks that realm's Summoning Stones.
        necesse.engine.registries.QuestRegistry.registerQuest("swh_keyskyreach", stairwaytoheaven.quest.SkyreachKeyQuest.class);
        necesse.engine.registries.QuestRegistry.registerQuest("swh_keyeden", stairwaytoheaven.quest.EdenKeyQuest.class);
        necesse.engine.registries.QuestRegistry.registerQuest("swh_keysteinfeld", stairwaytoheaven.quest.SteinfeldKeyQuest.class);
        necesse.engine.registries.QuestRegistry.registerQuest("swh_keyghostrealm", stairwaytoheaven.quest.GhostKeyQuest.class);
        necesse.engine.registries.QuestRegistry.registerQuest("swh_keycrookedbeyond", stairwaytoheaven.quest.CrookedKeyQuest.class);

        // The realms' own resident side-chains. Each is handed out and turned
        // in by the person it belongs to, not by the Warden, and each pays that
        // person's recruit fee plus their realm's bar. docs/AREA_OVERVIEW.md
        // for the holes they close.
        necesse.engine.registries.QuestRegistry.registerQuest("swh_steinfeldvigil", stairwaytoheaven.quest.SteinfeldVigilQuest.class);
        necesse.engine.registries.QuestRegistry.registerQuest("swh_mortimerrites", stairwaytoheaven.quest.MortimerRitesQuest.class);
        necesse.engine.registries.QuestRegistry.registerQuest("swh_caspernforge", stairwaytoheaven.quest.CaspernForgeQuest.class);
        // World-map icons for the auto-placed markers (spire + return
        // stairway). Textures load client-side via GameResources; the
        // registration itself is texture-free and server-safe.
        necesse.engine.registries.MapIconRegistry.registerIcon("skyspire",
                new necesse.level.maps.mapData.TextureGameMapIcon("ui/mapicons/skyspire"));
        necesse.engine.registries.MapIconRegistry.registerIcon("skystairs",
                new necesse.level.maps.mapData.TextureGameMapIcon("ui/mapicons/skystairs"));
        necesse.engine.registries.MapIconRegistry.registerIcon("skycat",
                new necesse.level.maps.mapData.TextureGameMapIcon("ui/mapicons/skycat"));
        // The Veil's fog and its Soul Exposure debuff (WORLD_DESIGN §8), plus
        // the world record of who carries the Veil Mark (§9). One call, because
        // §42.4 asks for ONE gate mechanic rather than a second code path when
        // the Infernal Visa lands — see stairwaytoheaven.veil.VeilGate.
        stairwaytoheaven.veil.VeilGate.register();
    }

    /**
     * ONE modded level, and it is the whole world this mod adds.
     *
     * <p>{@code docs/PLAN_ONE_PLANE.md}: <i>"One level: skylevel / skyreach2.
     * Everything the player walks to is on it. Realms are BIOME WEIGHT BANDS
     * over RealmDepth.depthAt."</i> {@code edenlevel}, {@code steinfeldlevel},
     * {@code ghostlevel}, {@code crookedlevel} and {@code veillevel} were
     * registered here for one day and are gone: five dimensions is five sets of
     * hard borders, which is the exact opposite of {@code WORLD_DESIGN} §3's
     * overlapping weights. Their terrain, biomes, mobs, items, POIs, settlers
     * and quests all survive as bands of this level — see
     * {@code worldgen.SkyTerrainPainter.describeRealmTile}.
     *
     * <p>Vertical layout: deepcave(-2) &lt; cave(-1) &lt; surface(0) &lt;
     * skyreach(+1). Nothing of ours sits anywhere else any more.
     */
    private void registerDimension() {
        LevelIdentifier.IDENTIFIER_TO_DIMENSION.put(SkyRegistry.SKYREACH_IDENTIFIER.stringID, SkyRegistry.SKY_DIMENSION);
        LevelRegistry.registerLevel("skylevel", SkyLevel.class);
    }

    private void registerBiomes() {
        // countInStats=false: sub-biomes of the Skyreach never generate as
        // surface islands (same flag vanilla uses for its non-surface biomes)
        SkyRegistry.driftlands = BiomeRegistry.registerBiome("driftlands", new DriftlandsBiome(), false);
        SkyRegistry.stormveil = BiomeRegistry.registerBiome("stormveil", new StormveilBiome(), false);
        SkyRegistry.auroraShoals = BiomeRegistry.registerBiome("aurorashoals", new AuroraShoalsBiome(), false);
        SkyRegistry.skyway = BiomeRegistry.registerBiome("skyway", new SkywayBiome(), false);
        // The sky's wrong ground. Registered beside the other sky sub-biomes
        // because that is what it is now: SkyTerrainPainter paints it into the
        // Skyreach's own biome layer, gated by distance from the spire.
        SkyRegistry.outlands = BiomeRegistry.registerBiome("outlands",
                new stairwaytoheaven.biomes.OutlandsBiome(), false);
        SkyRegistry.gloomfen = BiomeRegistry.registerBiome("gloomfen", new GloomfenBiome(), false);
        SkyRegistry.ashenReach = BiomeRegistry.registerBiome("ashenreach", new AshenReachBiome(), false);
        SkyRegistry.beetlefreakHollow = BiomeRegistry.registerBiome("beetlefreakhollow",
                new stairwaytoheaven.biomes.BeetlefreakHollowBiome(), false);
    }

    private void registerTiles() {
        SkyRegistry.cloudturfTile = new CloudturfTile();
        SkyRegistry.auroraShoalTile = new stairwaytoheaven.tiles.AuroraShoalTile();
        SkyRegistry.skystoneTile = new SkystoneTile();
        SkyRegistry.stormslateTile = new StormslateTile();
        SkyRegistry.mistseaTile = new MistseaTile();

        // "...tile" suffix mirrors vanilla naming (rocktile vs. the "stone"
        // item) and keeps tile items from colliding with material items.
        SkyRegistry.cloudturfID = TileRegistry.registerTile("cloudturftile", SkyRegistry.cloudturfTile, 1.0F, true);
        SkyRegistry.auroraShoalID = TileRegistry.registerTile("aurorashoaltile", SkyRegistry.auroraShoalTile, 1.0F, true);
        SkyRegistry.skystoneTileID = TileRegistry.registerTile("skystonetile", SkyRegistry.skystoneTile, 1.0F, true);
        SkyRegistry.stormslateID = TileRegistry.registerTile("stormslatetile", SkyRegistry.stormslateTile, 1.0F, true);
        SkyCloudmarbleSet.registerTiles();
        SkyRegistry.mistseaID = TileRegistry.registerTile("mistseatile", SkyRegistry.mistseaTile, 0.0F, false);

        // Eden grass, registered exactly the way vanilla registers
        // overgrowngrasstile (the asset its supplied art names as source):
        // brokerValue 0 and not obtainable -- the tile is never carried, the
        // SEED is (overgrownedenseed, in SkyItems), and mining a patch gives
        // seed back at vanilla's 4%. VERIFIED [jar] TileRegistry line 166.
        SkyRegistry.overgrownEdenID = TileRegistry.registerTile("overgrownedentile",
                new stairwaytoheaven.tiles.OvergrownEdenTile(), 0.0F, false, false, true);

        SkyRegistry.murkmossTile = new MurkmossTile();
        SkyRegistry.murkmossID = TileRegistry.registerTile("murkmosstile", SkyRegistry.murkmossTile, 1.0F, true);
        SkyRegistry.blackpeatID = TileRegistry.registerTile("blackpeattile", new BlackpeatTile(), 1.0F, true);
        SkyRegistry.ashsandID = TileRegistry.registerTile("ashsandtile", new AshsandTile(), 1.0F, true);
        SkyRegistry.murkwaterID = TileRegistry.registerTile("murkwatertile", new MurkwaterTile(), 0.0F, false);
        // The Veil's maddest ground. Registered exactly the way vanilla
        // registers spidernesttile, the tile its artwork was drawn on:
        // brokerValue 0 and not obtainable, so it is placed by worldgen and
        // by presets rather than carried in an inventory.
        SkyRegistry.beetlefreakTile = new stairwaytoheaven.tiles.BeetlefreakTile();
        SkyRegistry.beetlefreakID = TileRegistry.registerTile("beetlefreaktile",
                SkyRegistry.beetlefreakTile, 0.0F, false, false, true);
    }

    private void registerObjects() {
        SkyRegistry.stairwayDown = new SkywardStairwayObject();
        SkyRegistry.stairwayUp = new SkySideStairwayObject();
        SkyRegistry.stairwayDownID = ObjectRegistry.registerObject("skystairwaydown", SkyRegistry.stairwayDown, 20.0F, true);
        SkyRegistry.stairwayUpID = ObjectRegistry.registerObject("skystairwayup", SkyRegistry.stairwayUp, 0.0F, false);
        SkyRegistry.stairwayDown.ladderUpObjectID = SkyRegistry.stairwayUpID;

        // The Veil's rift pair + the seance circle that opens it
        SkyRegistry.veilRiftDown = new VeilRiftObject();
        SkyRegistry.veilRiftUp = new VeilSideRiftObject();
        SkyRegistry.veilRiftDownID = ObjectRegistry.registerObject("veilriftdown", SkyRegistry.veilRiftDown, 0.0F, false);
        SkyRegistry.veilRiftUpID = ObjectRegistry.registerObject("veilriftup", SkyRegistry.veilRiftUp, 0.0F, false);
        SkyRegistry.veilRiftDown.ladderUpObjectID = SkyRegistry.veilRiftUpID;
        // The Seance Circle grows NO item of its own any more.
        // docs/FOGKEY_AND_BOSSPORTALS.md A2 makes the circle "placed from
        // ghostchalk; the chalk is consumed", so the chalk is the item and this
        // is only the thing it leaves on the ground. Registering it obtainable
        // would give the game a second, differently-named item that does the
        // same job, which is exactly the redundancy the chalk replaced.
        //
        // itemObtainable=false, itemCountInStats=false, and the trailing
        // "ghostchalk" is isObtainedByOtherItemStringIDs -- vanilla's own way
        // of telling the "how do I get this" UI which item makes it
        // (ObjectRegistry.java:2150-2175, VERIFIED [jar]). Breaking a placed
        // circle still returns the chalk; see SeanceCircleObject.getLootTable.
        SkyRegistry.seanceCircleID = ObjectRegistry.registerObject(
                "seancecircle", new SeanceCircleObject(), 15.0F, false, false, "ghostchalk");

        // One boss portal per realm, plus the per-mob scaling buff they use.
        // docs/FOGKEY_AND_BOSSPORTALS.md §B3-B5: unbreakable, inert until that
        // realm's key piece is built, and scattered by SkyLevel's worldgen.
        stairwaytoheaven.bosses.BossPortalObject.register();

        // ...and the five key pieces that wake them. Each wears its own realm's
        // portal sheet, so the thing you build at home and the thing you find
        // out there are one picture (docs/FOGKEY_AND_BOSSPORTALS.md §B1-B3).
        // Registered AFTER the portals so the two ID sets read in the same
        // order in every dump; nothing depends on the ordering.
        stairwaytoheaven.objects.RegionKeyObject.register();

        SkyObjects.register();
    }

    public void initResources() {
        SkyMobs.loadTextures();
        stairwaytoheaven.arsenal.SkyArsenal.loadTextures();
        stairwaytoheaven.livestock.SkyLivestock.loadTextures();
        stairwaytoheaven.realms.eden.EdenRealm.loadTextures();
        stairwaytoheaven.realms.steinfeld.SteinfeldRealm.loadTextures();
        stairwaytoheaven.realms.crooked.CrookedRealm.loadTextures();
        stairwaytoheaven.realms.ghost.GhostRealm.loadTextures();
        stairwaytoheaven.bosses.BossPortalObject.loadBorrowedSheets();
        stairwaytoheaven.objects.RegionKeyObject.loadBorrowedArt();
    }

    public void postInit() {
        SkyItems.registerRecipes();
        stairwaytoheaven.realms.eden.EdenRealm.registerRecipes();
        stairwaytoheaven.realms.ghost.GhostRealm.registerRecipes();
        stairwaytoheaven.livestock.SkyLivestock.registerItems();
        SkyBuildingSet.registerRecipes();
        SkyFurnitureSet.registerRecipes();
        stairwaytoheaven.settlement.SkyProfessions.registerRecipes();
        SkyCloudmarbleSet.registerRecipes();
        stairwaytoheaven.arsenal.SkyArsenal.registerRecipes();
        SkyBuildingSet.resolveWorldgenMaterials();
        registerWorldGenerator();
        CommandsManager.registerServerCommand(new SkyreachStatusCommand());
        CommandsManager.registerServerCommand(new VeilStatusCommand());
        CommandsManager.registerServerCommand(new stairwaytoheaven.commands.EdenStatusCommand());
        // /swhreset — replay an existing save, and retrofit ground generated by
        // an older build. docs/SAVE_COMPAT.md.
        CommandsManager.registerServerCommand(new stairwaytoheaven.commands.SwhResetCommand());
    }

    private void registerWorldGenerator() {
        WorldGenerator.registerGenerator(new WorldGenerator() {
            @Override
            public Level getNewLevel(LevelIdentifier levelIdentifier, Server server, GameBlackboard blackboard) {
                if (levelIdentifier.equals(SkyRegistry.SKYREACH_IDENTIFIER)) {
                    return new SkyLevel(levelIdentifier, 0, 0, server.world.worldEntity, blackboard.getInt("seed"));
                }
                return null;
            }
        });
    }

    public void dispose() {
    }
}
