package stairwaytoheaven.realms.crooked;

import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.registries.WorldPresetRegistry;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import stairwaytoheaven.SkyRegistry;

/**
 * Crooked Beyond — Tier 4 of {@code docs/WORLD_DESIGN.md}, and everything it
 * registers, in one place.
 *
 * <h2>What this realm is</h2>
 * The player's one line for it: <b>"reality no longer works properly."</b> §13
 * describes the look — black-and-white stripes, neon green, violet, red, cyan,
 * chequerboard, spirals — and A3.6 supplies the reason, which is what makes it
 * more than a texture pack: <i>"After death it is not only the landscape that
 * decays. The rules of the world decay."</i>
 *
 * <h2>Where it came from</h2>
 * It already existed as an edge. The Skyreach's {@code outlands} biome has been
 * painting striped ground, dead trees and crystal massifs into the far sky since
 * the Outlands shipped, gated by distance from the spire, and its three mobs —
 * the two Crooked Golems and the Crooked Armadillo — have worn Crooked art since
 * the pass that gave them their sheets. That was a foreshadowing edge, not a
 * realm. <b>It is kept, and it is now the RIM:</b> the same ground, the same
 * cast, one walk short of the place itself — and the portal lattice it already
 * generated, which stood rings the player could not use, now opens the door.
 *
 * <h2>Wiring</h2>
 * <pre>
 *   StairwayToHeavenMod.init()          -&gt; CrookedRealm.register()
 *                                          (which calls registerBiomes/registerTiles)
 *   StairwayToHeavenMod.initResources() -&gt; CrookedRealm.loadTextures()
 * </pre>
 * The split is where it is because {@code GlobalData.loadAll} closes
 * {@code ItemRegistry}, {@code MobRegistry}, {@code TileRegistry},
 * {@code ObjectRegistry} and {@code BiomeRegistry} immediately after the
 * {@code init()} loop, and {@code initResources()} runs on the client only — a
 * dedicated server never calls it, which is why every {@code GameTexture} field
 * in this package is null-guarded at its draw site.
 *
 * <h2>String IDs are literals at every registration, on purpose</h2>
 * {@code tools/locale_audit.py} finds registered IDs by matching a quoted first
 * argument to the registry calls, and {@code tools/content_ledger.py} reuses that
 * scan. An ID hidden behind a constant is an ID neither gate can name-check. The
 * {@code public static final String} constants below exist only for code in
 * other classes that has to refer to these IDs.
 *
 * <h2>Deferred, and named rather than quietly missing</h2>
 * <ul>
 * <li><b>The Doorman (§15)</b> and <b>The Architect (§16)</b> — an NPC pass and a
 *     boss chapter, both out of scope here by instruction. The Long Table POI is
 *     the Architect's arena, built ahead of him.</li>
 * <li><b>The Reality Stitcher (§13)</b> — the realm's crafting station, and with
 *     it every placeable Crooked wall, floor and piece of furniture. Nothing here
 *     is obtainable for that reason; see {@link CrookedObjects}.</li>
 * <li><b>Long-Legged Chicken and Crooked Goat (§14)</b> — the two tameables.
 *     Reasoned out in {@code docs/realms/crooked.md}: on borrowed art and with
 *     load-time recolouring ruled out, a "Long-Legged Chicken" would be a
 *     chicken, and {@code AGENTS.md}'s standard for a tameable is a full vanilla
 *     husbandry path with feed, breeding and a tooltip that says what the animal
 *     is for.</li>
 * </ul>
 */
public final class CrookedRealm {

    private CrookedRealm() {
    }

    // ===== IDs other code refers to =====

    public static final String STRIPE_BEETLE = "stripebeetle";
    public static final String DOOR_MIMIC = "doormimic";
    public static final String TONGUE_PLANT = "tongueplant";

    // ===== Biomes =====

    public static StripedWasteBiome stripedWaste;
    public static SpiralFieldsBiome spiralFields;
    public static CheckerworksBiome checkerworks;

    // ===== Tiles =====

    public static CrookedStripeTile crookedStripeTile;
    public static SpiralSoilTile spiralSoilTile;
    public static CheckerStoneTile checkerStoneTile;

    public static int crookedStripeID;
    public static int spiralSoilID;
    public static int violetMudID;
    public static int checkerStoneID;
    public static int wrongWayID;
    public static int spillID;

    // ===== Objects =====

    public static int spiralTreeID;
    public static int eyeballShrubID;
    public static int screamingFlowerID;
    public static int stripedMushroomID;
    public static int bentGrassID;
    public static int bentLanternID;
    public static int crookedClockID;
    public static int longChairID;
    public static int groundWindowID;
    public static int teethRockID;
    public static int crookedCrateID;

    // ===== Items =====

    public static int oddwoodID;
    public static int warpResinID;
    public static int realityShardID;
    public static int eyeSeedID;
    public static int strangeFabricID;
    public static int stripedShellID;

    /**
     * Everything the realm puts into the registries, in the order the registries
     * require: tiles and biomes first (worldgen refers to them by ID), then
     * objects, then items, then mobs.
     */
    public static void register() {
        registerBiomes();
        registerTiles();
        registerObjects();
        registerItems();
        registerMobs();
        registerDoors();
        WorldPresetRegistry.registerPreset("swh_crookedrealm", new CrookedWorldPreset());
    }

    /**
     * The three bands.
     *
     * <p>{@code countInStats = false}, the flag vanilla uses for every biome that
     * is not a surface island: these are painted into a dimension's own biome
     * layer by {@link CrookedTerrainPainter} and must never turn up in the
     * world's surface-biome statistics or in an island generator's draw.
     */
    public static void registerBiomes() {
        stripedWaste = BiomeRegistry.registerBiome("stripedwaste", new StripedWasteBiome(), false);
        spiralFields = BiomeRegistry.registerBiome("spiralfields", new SpiralFieldsBiome(), false);
        checkerworks = BiomeRegistry.registerBiome("checkerworks", new CheckerworksBiome(), false);
    }

    /**
     * The six grounds.
     *
     * <p>All six are registered {@code brokerValue = 0.0F} and
     * <b>{@code obtainable = false}</b>, which is how vanilla registers
     * {@code spidernesttile} and {@code ascendedvoidtile} and how this mod
     * registers {@code beetlefreaktile}: the tile is placed by worldgen and by
     * presets, never carried in an inventory. Nothing in this realm is a floor
     * the player crafts — that is Reality Stitcher content and it is deferred —
     * so registering any of them obtainable would put six unnamed tiles in a
     * building menu with no recipe behind them.
     *
     * <p>The trailing {@code (false, false, true)} arguments mirror the mod's own
     * {@code beetlefreaktile} registration: not obtainable, not obtainable in
     * creative, and on the tile layer terrain uses.
     */
    public static void registerTiles() {
        crookedStripeTile = new CrookedStripeTile();
        spiralSoilTile = new SpiralSoilTile();
        checkerStoneTile = new CheckerStoneTile();

        crookedStripeID = TileRegistry.registerTile("crookedstripetile",
                crookedStripeTile, 0.0F, false, false, true);
        spiralSoilID = TileRegistry.registerTile("spiralsoiltile",
                spiralSoilTile, 0.0F, false, false, true);
        violetMudID = TileRegistry.registerTile("violetmudtile",
                new VioletMudTile(), 0.0F, false, false, true);
        checkerStoneID = TileRegistry.registerTile("checkerstonetile",
                checkerStoneTile, 0.0F, false, false, true);
        wrongWayID = TileRegistry.registerTile("wrongwaytile",
                new WrongWayTile(), 0.0F, false, false, true);
        // The liquid, like the mod's other two seas: broker 0, not obtainable.
        spillID = TileRegistry.registerTile("spilltile", new SpillTile(), 0.0F, false);
    }

    private static void registerObjects() {
        CrookedObjects.register();
    }

    /**
     * The realm's resource line — §13's <i>"Oddwood · Warp Resin · Reality Shard
     * · Eye Seed · Strange Fabric"</i>, plus the Striped Shell §14 hangs on the
     * Stripe Beetle.
     *
     * <h2>Every one has a sink, and that is the design</h2>
     * {@code WORLD_DESIGN.md} A4.5, in the player's words: a resource feels
     * scarce when <i>something always wants it</i>, and <i>"a material with no
     * consumer is either loot or clutter"</i>. The consumers here are the
     * Reality Stitcher's, and the Stitcher is deferred — so this pass is
     * deliberately honest about the state of the economy rather than inventing
     * placeholder recipes at somebody else's workstation:
     *
     * <ul>
     * <li><b>Today</b> all six are loot with a broker value, i.e. they sell.
     *     Magpie buys sky salvage in quantity and the realm's drop value is
     *     x2.5, so a run into Crooked Beyond already pays.</li>
     * <li><b>Next</b> the Stitcher consumes Reality Shard + Spiritsteel + Soul
     *     Thread to exist, and then eats Oddwood, Warp Resin and Strange Fabric
     *     for the warped building set, the morphing furniture and the Pocket
     *     Door. Eye Seed is the Long-Legged Chicken's feed the day that animal
     *     ships (§14), and Striped Shell is its shell armour.</li>
     * </ul>
     *
     * <p>Writing that down here rather than in a design doc is deliberate: the
     * next agent to open this file is the one who will add the recipes.
     *
     * <h2>Icons</h2>
     * Every one draws a VANILLA item icon by literal path through
     * {@link CrookedMatItem} — no recolouring, no new PNG. Each choice is stated
     * at its registration and repeated in {@code docs/realms/crooked.md} and
     * {@code docs/VANILLA_ASSET_MAP.md}.
     *
     * <h2>Broker values</h2>
     * Read against the mod's own ladder rather than invented: the Skyreach's
     * Windsilk is 6, its Aetherium Ore 12, its Storm Shard 25, and the Veil's
     * Veil Essence 30. Crooked Beyond is two rungs past the Veil, so its common
     * materials sit at 30-40 and its rare one at 90 — above everything the mod
     * has and below nothing, because there is nothing above it yet.
     */
    private static void registerItems() {
        // Oddwood -- vanilla items/deadwoodlog.png, a dark barkless log. The
        // realm's common material and what a Spiral Tree is made of.
        oddwoodID = ItemRegistry.registerItem("oddwood",
                new CrookedMatItem("deadwoodlog", 999, Item.Rarity.UNCOMMON), 30.0F, true);
        // Warp Resin -- vanilla items/bioessence.png, a neon-green blob. §13's
        // palette calls for neon green and this is the game's own.
        warpResinID = ItemRegistry.registerItem("warpresin",
                new CrookedMatItem("bioessence", 999, Item.Rarity.UNCOMMON), 34.0F, true);
        // Strange Fabric -- vanilla items/clothscraps.png, torn cloth. What the
        // realm's furniture turns out to have been made of.
        strangeFabricID = ItemRegistry.registerItem("strangefabric",
                new CrookedMatItem("clothscraps", 999, Item.Rarity.UNCOMMON), 36.0F, true);
        // Eye Seed -- vanilla items/crystalessence.png, three coloured shards
        // that read as seeds in a palm. What an Eyeball Shrub drops.
        eyeSeedID = ItemRegistry.registerItem("eyeseed",
                new CrookedMatItem("crystalessence", 999, Item.Rarity.RARE), 45.0F, true);
        // Striped Shell -- vanilla items/crystallizedskull.png, a pink plated
        // carapace. What comes off a netted Stripe Beetle.
        stripedShellID = ItemRegistry.registerItem("stripedshell",
                new CrookedMatItem("crystallizedskull", 999, Item.Rarity.RARE), 55.0F, true);
        // Reality Shard -- vanilla items/ascendedshard.png, a magenta-and-cyan
        // splinter. The realm's rare material and §13's own name for it; the
        // Reality Stitcher is built out of these.
        realityShardID = ItemRegistry.registerItem("realityshard",
                new CrookedMatItem("ascendedshard", 999, Item.Rarity.EPIC), 90.0F, true);
    }

    /**
     * The realm's three new bodies.
     *
     * <p>Kill statistics ON for the two hostiles, the way the mod registers
     * every enemy, and OFF for the critter, the way vanilla registers its own —
     * nobody wants a scoreboard for netting beetles.
     *
     * <p>The three Crooked mobs that already existed —
     * {@code crookedgolem}, {@code rarecrookedgolem}, {@code crookedarmadillo} —
     * are NOT re-registered here. They are registered once, in {@code SkyMobs},
     * because they still stand on the Outlands rim as well as in the realm; this
     * pass only moves their numbers onto the realm's row.
     */
    private static void registerMobs() {
        MobRegistry.registerMob("doormimic", DoorMimicMob.class, true);
        MobRegistry.registerMob("tongueplant", TonguePlantMob.class, true);
        MobRegistry.registerMob("stripebeetle", StripeBeetleMob.class, false);
    }

    /**
     * The doorway pair.
     *
     * <p>Registered unobtainable, exactly like the Veil's rift pair: a door into
     * a tier-10 realm is opened at a place, not carried in a backpack. The
     * down-side is created by using a Seance Circle inside the Beetle Outlands
     * ({@link stairwaytoheaven.objects.SeanceCircleObject}); the up-side is
     * placed automatically at the far end on first arrival.
     */
    private static void registerDoors() {
        SkyRegistry.crookedDoorDownID = ObjectRegistry.registerObject("crookeddoordown",
                new CrookedDoorObject(), 0.0F, false);
        SkyRegistry.crookedDoorUpID = ObjectRegistry.registerObject("crookeddoorup",
                new CrookedSideDoorObject(), 0.0F, false);
        ((CrookedDoorObject) ObjectRegistry.getObject(SkyRegistry.crookedDoorDownID))
                .ladderUpObjectID = SkyRegistry.crookedDoorUpID;
    }

    /**
     * Client-side texture loading. Never called on a dedicated server.
     *
     * <p>One entry, because everything else in this realm either draws a vanilla
     * sheet through the vanilla class it inherits from ({@code doormimic},
     * {@code tongueplant}) or names its sheet as a constructor argument that the
     * object's own {@code loadTextures} resolves ({@link CrookedObjects},
     * {@link CrookedGroundTile}). Only the Stripe Beetle needs a static field,
     * because {@code CritterMob} has no per-instance texture hook — the same
     * reason {@code SkyCritterMob} has four of them.
     */
    public static void loadTextures() {
        StripeBeetleMob.texture = GameTexture.fromFile("mobs/scorpion");
    }
}
