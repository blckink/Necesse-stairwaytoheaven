package stairwaytoheaven.realms.steinfeld;

import java.awt.Color;
import java.awt.Rectangle;

import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.registries.WorldPresetRegistry;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.gameObject.ColumnObject;
import necesse.level.gameObject.GravestoneObject;
import necesse.level.gameObject.RockObject;
import necesse.level.gameObject.StatueObject;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.objects.SkyDecoObject;
import stairwaytoheaven.realms.steinfeld.mobs.GraveCrowMob;
import stairwaytoheaven.realms.steinfeld.mobs.HollowAngelMob;
import stairwaytoheaven.realms.steinfeld.mobs.LostPilgrimMob;
import stairwaytoheaven.realms.steinfeld.mobs.StoneMournerMob;
import stairwaytoheaven.realms.steinfeld.tiles.AshGrassTile;
import stairwaytoheaven.realms.steinfeld.tiles.CrackedMarbleTile;
import stairwaytoheaven.realms.steinfeld.tiles.DeadSoilTile;
import stairwaytoheaven.realms.steinfeld.tiles.GraveSoilTile;
import stairwaytoheaven.realms.steinfeld.tiles.MistStoneTile;
import stairwaytoheaven.realms.steinfeld.tiles.PaleGrassTile;
import stairwaytoheaven.realms.steinfeld.tiles.WeatheredStoneTile;

/**
 * Steinfeld / The Quiet Reach — Tier 2 of {@code docs/WORLD_DESIGN.md} §7, and
 * everything it registers, in one place.
 *
 * <h2>What this realm is</h2>
 * A3.4's line for it: <b>"the place where the sky stops working properly."</b>
 * Near the gate the ground is still Eden's own green and the sky's own bright
 * stone; walk out and the grass pales, the stone dulls, the trees die and the
 * fog comes in. §7 gives the concrete gradient — pale grass, big slabs, dead
 * trees, gravestones — and {@link SteinfeldTerrainPainter} is the field that
 * computes it, one tile at a time, from a single depth value.
 *
 * <h2>Where the actual content lives</h2>
 * This class only REGISTERS. The generation itself is
 * {@link SteinfeldTerrainPainter} (the depth field, the three bands' ground
 * mix and prop scatter, the organic POI lattice), {@link SteinfeldPressure}
 * (where a hostile may appear), {@link SteinfeldSites} +
 * {@link SteinfeldWorldPreset} (the two hand-authored landmarks), and
 * {@link SteinfeldLevel} (the level class and its guard-pack placement). The
 * three biomes, the seven tiles and the four mobs already existed, complete,
 * before this class did; what was missing was the registry glue that turns
 * their string IDs into real game content and the SkyRegistry fields the rest
 * of the package already compiles against.
 *
 * <h2>Wiring</h2>
 * <pre>
 *   StairwayToHeavenMod.init()          -&gt; SteinfeldRealm.register()
 *                                          (registerBiomes/registerTiles/
 *                                           registerObjects/registerItems/
 *                                           registerMobs, in that order)
 *   StairwayToHeavenMod.initResources() -&gt; SteinfeldRealm.loadTextures()
 * </pre>
 * Tiles and biomes are registered before objects and items, the same order
 * {@code GhostRealm} and {@code CrookedRealm} use, because worldgen needs
 * their IDs and both registries close at the end of the {@code init()} loop.
 *
 * <h2>String IDs are literals at every registration, on purpose</h2>
 * {@code tools/locale_audit.py} finds registered IDs by matching a quoted
 * first argument to the registry calls, and {@code tools/content_ledger.py}
 * reuses that scan. An ID hidden behind a constant or a private wrapper is an
 * ID neither gate can name-check, so every call below spells its string out.
 *
 * <h2>No new art</h2>
 * Every sheet named below already exists — in this mod's own resources or in
 * the vanilla game's — and none is recoloured at load time. Which file stands
 * in for which piece of Steinfeld is recorded once, in
 * {@code docs/realms/steinfeld.md}, rather than repeated in every comment
 * here.
 *
 * <h2>Deferred, and named rather than quietly missing</h2>
 * <ul>
 * <li><b>The world-event ghosts (§7, A3.4)</b> — transparent, unattackable
 *     figures walking to a grave, a door or the map edge. {@link SteinfeldLevel}
 *     already explains why: it needs its own invulnerable,
 *     destination-seeking mob archetype, which is a feature in its own right
 *     and not one of this pass's four residents.</li>
 * <li><b>A player-facing entry object.</b> Exactly like the Garden of Eden —
 *     the dimension directly below this one on the same ladder — Steinfeld is
 *     registered as a dimension index and a level class and nothing walks the
 *     player there yet. Building a "Fallen Gate" for Steinfeld while Eden
 *     still has none would not make Steinfeld more reachable than the realm
 *     underneath it; both stay deferred together.</li>
 * <li><b>A crafting station and recipe economy.</b> Pale Stone, Grave Salt,
 *     Spirit Moss and Echo Shard are loot and mining drops today, exactly the
 *     state Crooked Beyond's own six materials shipped in. Spirit Moss and
 *     Echo Shard already have a real consumer — §9's séance quest — so
 *     nothing here is unspent by design; a Steinfeld-side recipe list is
 *     future work, not a hole.</li>
 * </ul>
 */
public final class SteinfeldRealm {

    private SteinfeldRealm() {
    }

    // ===== Category, shared by the two mineable rocks =====

    private static final String[] STONE_CATEGORY = {"objects", "landscaping", "rocksandores"};

    /**
     * Everything the realm puts into the registries, in the order the
     * registries require: biomes and tiles first (worldgen and the terrain
     * painter refer to them by ID), then objects, then items, then mobs.
     */
    public static void register() {
        registerBiomes();
        registerTiles();
        registerObjects();
        registerItems();
        registerMobs();
        WorldPresetRegistry.registerPreset("swh_steinfeldrealm", new SteinfeldWorldPreset());
    }

    /**
     * The three bands. {@code countInStats = false}, the flag vanilla uses for
     * every biome that is not a surface island: these are painted into
     * Steinfeld's own biome layer by {@link SteinfeldTerrainPainter} and must
     * never turn up in the world's surface-biome statistics.
     */
    public static void registerBiomes() {
        SkyRegistry.quietMeadow = BiomeRegistry.registerBiome("quietmeadow", new QuietMeadowBiome(), false);
        SkyRegistry.slabFields = BiomeRegistry.registerBiome("slabfields", new SlabFieldsBiome(), false);
        SkyRegistry.graveHeath = BiomeRegistry.registerBiome("graveheath", new GraveHeathBiome(), false);
    }

    /**
     * The seven grounds. All registered {@code brokerValue = 0.0F},
     * {@code obtainable = false}, {@code itemCountInStats = false},
     * {@code obtainableInCreative = true} — vanilla's own
     * {@code (false, false, true)} shorthand, mirrored from
     * {@code CrookedRealm.registerTiles}: the tile is placed by worldgen and by
     * presets, never carried in an inventory, but a world builder can still
     * pull it from the creative menu.
     */
    public static void registerTiles() {
        SkyRegistry.palegrassTile = new PaleGrassTile();
        SkyRegistry.palegrassID = TileRegistry.registerTile(
                "palegrasstile", SkyRegistry.palegrassTile, 0.0F, false, false, true);
        SkyRegistry.weatheredstoneID = TileRegistry.registerTile(
                "weatheredstonetile", new WeatheredStoneTile(), 0.0F, false, false, true);
        SkyRegistry.crackedmarbleID = TileRegistry.registerTile(
                "crackedmarbletile", new CrackedMarbleTile(), 0.0F, false, false, true);
        SkyRegistry.deadsoilID = TileRegistry.registerTile(
                "deadsoiltile", new DeadSoilTile(), 0.0F, false, false, true);
        SkyRegistry.ashgrassID = TileRegistry.registerTile(
                "ashgrasstile", new AshGrassTile(), 0.0F, false, false, true);
        SkyRegistry.miststoneID = TileRegistry.registerTile(
                "miststonetile", new MistStoneTile(), 0.0F, false, false, true);
        SkyRegistry.gravesoilID = TileRegistry.registerTile(
                "gravesoiltile", new GraveSoilTile(), 0.0F, false, false, true);
    }

    /**
     * Everything {@link SteinfeldTerrainPainter#objectIdOf} and the two
     * landmark presets point at, except the two IDs that are simply the
     * vanilla object they reuse (see the two aliases at the end of this
     * method) and {@code deadtreeID} / {@code overgrownEdenID}, both already
     * registered — the Veil's own dead tree and Eden's own grass — long
     * before this method runs.
     *
     * <h2>Flora</h2>
     * All five are {@link SteinfeldPlantObject} — vanilla's own
     * {@code GrassObject} archetype, see that class's header — registered
     * unobtainable with no recipe, so neither the world sprite nor an item
     * icon is ever asked for by a player and the density argument is the
     * only number that matters. Only Spirit Moss carries a real drop.
     *
     * <h2>The two rocks</h2>
     * Vanilla's own {@link RockObject}, exactly the shape
     * {@code SkyObjects.registerVeilObjects} uses for {@code veilrock}: a
     * texture argument, a map colour, the material it drops. Pale Stone Rock
     * reuses this mod's own {@code skystonerock} sheet — Pale Stone IS the
     * sky's own material, still lying about up here, the way
     * {@code WeatheredStoneTile}'s own header explains — and Grave Salt Rock
     * reuses {@code veilrock}, the Veil's own dark stone, on the same
     * "the Veil's flora may serve the later regions" licence
     * {@link stairwaytoheaven.realms.steinfeld.tiles.AshGrassTile} already
     * uses for its ground.
     *
     * <h2>Statues, column, slab</h2>
     * {@code mournerstatue} and {@code brokenangel} are vanilla's
     * {@link StatueObject} pointed at {@code mossymonkstatue} and this mod's
     * own {@code seraph} sheet (the same sheet {@code SkyCloudmarbleSet}'s
     * Seraph Statue already draws, with the identical
     * {@code (xOffset, spriteCount)} pair, since it is the same physical
     * file). {@code chapelcolumn} is vanilla's {@link ColumnObject} on
     * {@code cryptcolumn}. {@code heavenslab} is {@link SkyDecoObject} on this
     * mod's own {@code skywatchrubble} sheet, the same rubble the Skyreach's
     * own ruins already scatter. All four are given Steinfeld's own name
     * rather than left to show the borrowed sheet's vanilla or Skyreach
     * tooltip, because each one is named directly in §7 or A3.4's own
     * imagery.
     *
     * <h2>The two aliases</h2>
     * {@code gravefenceID} and {@code steinfeldgravestoneID} are NOT new
     * registrations. They are {@code ObjectRegistry.getObjectID} reads of
     * vanilla's own {@code cryptfence} and {@code cryptgravestone1} — exactly
     * the pattern {@code SkyObjects.register} already uses for
     * {@code stormCrystalID}/{@code auroraBloomID}, and available at this
     * point in {@code init()} for the same reason those are: vanilla's own
     * content registers before any mod's {@code init()} runs. A fence around
     * a grave plot reads correctly under vanilla's own "Crypt Fence" tooltip,
     * and a lone gravestone reads correctly under vanilla's own "Gravestone"
     * — and reusing {@link GravestoneObject} itself rather than wrapping it is
     * what makes {@code SteinfeldBiome}'s own crate-loot promise true:
     * <b>VERIFIED [jar]</b> {@code GravestoneObject.getLootTable} answers
     * {@code level.getCrateLootTable} for any stone the player did not place,
     * whether that stone is vanilla's own registration or a new one built on
     * the same class. Reusing the ID outright needed neither.
     */
    private static void registerObjects() {
        SkyRegistry.witheredtuftID = ObjectRegistry.registerObject("witheredtuft",
                new SteinfeldPlantObject("witheredgrass", 2, new Color(150, 140, 112), new LootTable()),
                0.0F, false);
        SkyRegistry.palereedID = ObjectRegistry.registerObject("palereed",
                new SteinfeldPlantObject("skyreeds", 2, new Color(182, 178, 142), new LootTable()),
                0.0F, false);
        SkyRegistry.widowflowerID = ObjectRegistry.registerObject("widowflower",
                new SteinfeldPlantObject("cragbloom", 1, new Color(112, 72, 122), new LootTable()),
                0.0F, false);
        SkyRegistry.deadheavenbloomID = ObjectRegistry.registerObject("deadheavenbloom",
                new SteinfeldPlantObject("aurorabloom", 2, new Color(182, 190, 200), new LootTable()),
                0.0F, false);
        SkyRegistry.ghostmushroomID = ObjectRegistry.registerObject("ghostmushroom",
                new SteinfeldPlantObject("gloomshroom", 2, new Color(112, 170, 152), new LootTable()),
                0.0F, false);
        SkyRegistry.spiritmosspatchID = ObjectRegistry.registerObject("spiritmosspatch",
                new SteinfeldPlantObject("staticmoss", 2, new Color(82, 108, 92),
                        new LootTable(LootItem.between("spiritmoss", 1, 2))),
                0.0F, false);

        SkyRegistry.palestonerockID = ObjectRegistry.registerObject("palestonerock",
                new RockObject("skystonerock", new Color(172, 178, 188), "palestone", STONE_CATEGORY),
                -1.0F, true);
        SkyRegistry.gravesaltrockID = ObjectRegistry.registerObject("gravesaltrock",
                new RockObject("veilrock", new Color(90, 96, 100), "gravesalt", STONE_CATEGORY),
                -1.0F, true);

        SkyRegistry.mournerstatueID = ObjectRegistry.registerObject("mournerstatue",
                new StatueObject("mossymonkstatue", 16, 4), 0.0F, false);
        SkyRegistry.brokenangelID = ObjectRegistry.registerObject("brokenangel",
                new StatueObject("seraph", 32, 1), 0.0F, false);
        SkyRegistry.chapelcolumnID = ObjectRegistry.registerObject("chapelcolumn",
                new ColumnObject("cryptcolumn", new Color(150, 156, 164), ToolType.PICKAXE), 0.0F, false);
        SkyRegistry.heavenslabID = ObjectRegistry.registerObject("heavenslab",
                new SkyDecoObject("skywatchrubble", 32, new Color(160, 166, 176),
                        new Rectangle(8, 12, 16, 20), "objects", "decorations"),
                0.0F, false);

        // Aliases -- see the header above for why these are not new registrations.
        SkyRegistry.gravefenceID = ObjectRegistry.getObjectID("cryptfence");
        SkyRegistry.steinfeldgravestoneID = ObjectRegistry.getObjectID("cryptgravestone1");
    }

    /**
     * The realm's four materials — §7's own list: Pale Stone (building),
     * Grave Salt (alchemy), Spirit Moss (later séance), Echo Shard (from
     * ghost apparitions). All four draw a vanilla icon, unrecoloured, by
     * literal path through {@link SteinfeldMatItem}; the choice is repeated
     * with its reasoning in {@code docs/realms/steinfeld.md}.
     *
     * <p>Broker values sit under Ghost Realm's own ladder — Bonewood 4,
     * Soul Thread 30, Spectral Ore 35 — the way Steinfeld's own tier (5,
     * against Ghost Realm's higher one) says they should: this realm's
     * materials are worth less than the realm one rung further up the
     * stairway, not more.
     */
    private static void registerItems() {
        // No SkyRegistry ID fields: exactly like GhostRealm.registerItems, nothing
        // elsewhere needs the numeric ID -- every reference to these four (the
        // three biomes' crate tables, the four mobs' loot tables, the two
        // landmark presets) is by their string ID, inside a LootTable.
        ItemRegistry.registerItem("palestone",
                new SteinfeldMatItem("cryptstone", 500, Item.Rarity.UNCOMMON)
                        .setItemCategory("materials"), 6.0F, true);
        ItemRegistry.registerItem("gravesalt",
                new SteinfeldMatItem("alchemyshard", 500, Item.Rarity.UNCOMMON)
                        .setItemCategory("materials"), 18.0F, true);
        ItemRegistry.registerItem("spiritmoss",
                new SteinfeldMatItem("phantomdust", 500, Item.Rarity.RARE)
                        .setItemCategory("materials"), 32.0F, true);
        ItemRegistry.registerItem("echoshard",
                new SteinfeldMatItem("pearlescentshard", 500, Item.Rarity.RARE)
                        .setItemCategory("materials"), 45.0F, true);
    }

    /**
     * The realm's four residents — §7's own list, one of each spawn-table
     * role (see {@link stairwaytoheaven.realms.steinfeld.mobs.SteinfeldTier}).
     * Kill statistics ON for all four, the way the mod registers every
     * hostile ({@code CrookedRealm.registerMobs}' own convention) — nothing
     * here is a critter.
     */
    private static void registerMobs() {
        MobRegistry.registerMob("lostpilgrim", LostPilgrimMob.class, true);
        MobRegistry.registerMob("stonemourner", StoneMournerMob.class, true);
        MobRegistry.registerMob("hollowangel", HollowAngelMob.class, true);
        MobRegistry.registerMob("gravecrow", GraveCrowMob.class, true);
    }

    /**
     * Client-side texture loading. Never called on a dedicated server.
     *
     * <p>Empty on purpose. Every class this realm registers through —
     * {@link RockObject}, {@link GravestoneObject}, {@link StatueObject},
     * {@link ColumnObject}, {@link SkyDecoObject}, and
     * {@link SteinfeldPlantObject}'s vanilla {@code GrassObject} base — loads
     * its own sheet from its own {@code loadTextures()} override the moment
     * the object registry closes; none of them needs a static field primed
     * from outside the way {@code CrookedRealm} has to prime
     * {@code StripeBeetleMob.texture} for a {@code CritterMob}, which has no
     * per-instance texture hook. Steinfeld's four mobs all subclass a real
     * vanilla hostile body (see each mob class's own header) and inherit that
     * body's texture handling unchanged, so there is nothing here to prime
     * either.
     */
    public static void loadTextures() {
    }
}
