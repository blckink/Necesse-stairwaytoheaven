package stairwaytoheaven.realms.crooked;

import java.awt.Color;

import necesse.engine.registries.ObjectRegistry;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.gameObject.RandomCrateObject;
import necesse.level.maps.Level;
import stairwaytoheaven.objects.SkyDecoObject;

/**
 * Everything that stands on the ground in Crooked Beyond: the flora of §13, the
 * decoration of §13/A3.6, and the realm's one container.
 *
 * <h2>Why they are all {@link SkyDecoObject}</h2>
 * That class takes the TEXTURE NAME as constructor argument 0, deliberately
 * decoupled from the registered string ID — the mod's own
 * {@code SkyfallShardObject} already registers {@code "skyfallshard"} and draws
 * {@code objects/starfall.png}. That decoupling is the whole reason this realm
 * can be built: every sheet below is one the game or the mod already owns, named
 * by literal path, and no PNG had to be drawn for any of it
 * ({@code WORLD_DESIGN.md} A4.3).
 *
 * <p>The alternative — {@code TreeObject}, {@code GrassObject},
 * {@code RockObject} — was considered and rejected for this pass. All three also
 * take a decoupled texture name, but each additionally owns an ITEM
 * ({@code TreeObject}'s sapling and log, {@code RockObject}'s stone) whose icon
 * the engine builds from files this mod does not have, and inventing borrowed
 * icons for a family nobody can craft yet buys nothing. What the archetypes are
 * really for — replanting, settler jobs, ore masks — belongs with the Reality
 * Stitcher, which is §13's crafting station and is deferred with the rest of the
 * realm's crafting.
 *
 * <h2>Nothing here is obtainable, and that is the design</h2>
 * Every registration below passes {@code obtainable = false}, the way vanilla
 * registers {@code wildmushroom} and this mod registers {@code ashbones}. A
 * Crooked prop is a HARVEST NODE: you break it for a material and the material
 * is the reward. Nothing in this realm is furniture you pick up and carry home,
 * because the furniture of §13 ("morphing furniture, warped building materials")
 * is Reality Stitcher output and the Stitcher is not in this pass. Saying that
 * out loud is better than shipping a dozen placeable props with vanilla icons
 * that the player would then find in a menu with no recipe.
 *
 * <h2>Tool behaviour</h2>
 * {@code docs/IMPLEMENTATION_RULES.md} §4: do not default custom objects to the
 * pickaxe. Woody things take the AXE, soft things take {@code ToolType.ALL} the
 * way vanilla's soft flora does, and the one mineral formation takes the
 * PICKAXE. Each choice is stated at its registration.
 */
public final class CrookedObjects {

    private static final String[] CATEGORY = {"objects", "decorations"};

    private CrookedObjects() {
    }

    static void register() {
        // ===== The Spiral Fields: what grows =====

        // Spiral Tree -- §13's own name. Borrowed sheet: vanilla
        // objects/burnedbush.png (128x64, two 64-wide variants), a black tangle
        // of branches with no leaves on it. AXE, because it is woody: vanilla
        // treats every trunk and every woody shrub that way and so does the
        // mod's own Dead Tree. 40 HP -- between the Dead Tree's default and a
        // vanilla trunk, so felling one is a few swings rather than a chore.
        CrookedRealm.spiralTreeID = ObjectRegistry.registerObject("spiraltree",
                new SkyDecoObject("burnedbush", 64, new Color(38, 30, 44), null, CATEGORY) {
                    @Override
                    public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
                        return SPIRAL_TREE_LOOT;
                    }
                }.setTool(ToolType.AXE).setObjectHealth(40),
                0.0F, false);

        // Eyeball Shrub -- §13's own name, and the one borrowed sheet that
        // needed no interpretation at all. Vanilla objects/voidtrap.png
        // (160x64, five 32-wide variants): violet rings with an eye in the
        // middle of each. ALL + 1 HP, the mod's audited pattern for soft flora
        // (see the Gloomshroom, where the GameObject pickaxe default was wrong).
        CrookedRealm.eyeballShrubID = ObjectRegistry.registerObject("eyeballshrub",
                new SkyDecoObject("voidtrap", 32, new Color(150, 60, 180), null, CATEGORY) {
                    @Override
                    public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
                        return EYEBALL_SHRUB_LOOT;
                    }
                }.setTool(ToolType.ALL).setObjectHealth(1),
                0.0F, false);

        // Screaming Flower -- §13's own name. Borrowed sheet: vanilla
        // objects/glowcoral.png (128x32, four 32-wide variants), red spiky
        // fronds. Soft flora: ALL, 1 HP.
        // It glows faintly red; the Fields are otherwise all violet.
        CrookedRealm.screamingFlowerID = ObjectRegistry.registerObject("screamingflower",
                new SkyDecoObject("glowcoral", 32, new Color(196, 58, 62), null, CATEGORY) {
                    @Override
                    public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
                        return SCREAMING_FLOWER_LOOT;
                    }
                }.setTool(ToolType.ALL).setObjectHealth(1)
                        .setLight(55, 0.02F, 0.55F),
                0.0F, false);

        // Striped Mushroom -- §13's own name. Borrowed sheet: vanilla
        // objects/mushroom.png (224x64, seven 32-wide growth variants). Soft
        // flora: ALL, 1 HP.
        CrookedRealm.stripedMushroomID = ObjectRegistry.registerObject("stripedmushroom",
                new SkyDecoObject("mushroom", 32, new Color(168, 140, 108), null, CATEGORY) {
                    @Override
                    public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
                        return STRIPED_MUSHROOM_LOOT;
                    }
                }.setTool(ToolType.ALL).setObjectHealth(1),
                0.0F, false);

        // Bent Grass -- §13's own name. Borrowed sheet: vanilla
        // objects/witheredgrass.png (256x32, eight 32-wide variants), dry stalks
        // leaning every way but up. Drops nothing: it is the realm's carpet, the
        // way the Skyreach's meadow grasses are, and a carpet that pays would
        // make the whole realm a farm.
        CrookedRealm.bentGrassID = ObjectRegistry.registerObject("bentgrass",
                new SkyDecoObject("witheredgrass", 32, new Color(122, 106, 78), null, CATEGORY)
                        .setTool(ToolType.ALL).setObjectHealth(1),
                0.0F, false);

        // ===== The built ground: what somebody left =====

        // Bent Lantern -- A3.6's "lanterns growing out of plants" and "bent
        // lanterns". Borrowed sheet: vanilla objects/voidflame.png (128x64, four
        // 32-wide variants), a magenta flame with no lamp under it. It is the
        // realm's only natural light source, which is what makes the
        // Checkerworks navigable after dark.
        CrookedRealm.bentLanternID = ObjectRegistry.registerObject("bentlantern",
                new SkyDecoObject("voidflame", 32, new Color(214, 74, 190), null, CATEGORY)
                        .setTool(ToolType.ALL).setObjectHealth(20)
                        .setLight(140, 0.86F, 0.70F),
                0.0F, false);

        // Crooked Clock -- §13's "crooked clocks". Borrowed sheet: vanilla
        // objects/boneclock.png (128x64, four 32-wide rotations, drawn here as
        // four variants so a row of them faces different ways). Bone rather than
        // wood, so ALL rather than AXE, and 30 HP like a piece of furniture.
        CrookedRealm.crookedClockID = ObjectRegistry.registerObject("crookedclock",
                new SkyDecoObject("boneclock", 32, new Color(206, 198, 168), null, CATEGORY) {
                    @Override
                    public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
                        return BUILT_THING_LOOT;
                    }
                }.setTool(ToolType.ALL).setObjectHealth(30),
                0.0F, false);

        // Long Chair -- §13's "absurdly long chairs". Borrowed sheet: vanilla
        // objects/bonechair.png (128x64, four 32-wide rotations). It is NOT a
        // ChairObject and cannot be sat on, which is deliberate rather than an
        // oversight: a sittable chair is furniture the player expects to be able
        // to pick up and place, and this realm ships no placeable furniture (see
        // the class comment). The Long Table preset lines them up so the joke
        // still lands.
        CrookedRealm.longChairID = ObjectRegistry.registerObject("longchair",
                new SkyDecoObject("bonechair", 32, new Color(200, 192, 162), null, CATEGORY) {
                    @Override
                    public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
                        return BUILT_THING_LOOT;
                    }
                }.setTool(ToolType.ALL).setObjectHealth(30),
                0.0F, false);

        // Ground Window -- A3.6's "windows in the ground", which is the single
        // image in that section that most needs no explanation. Borrowed sheet:
        // vanilla objects/voidcube.png (32x48), a violet cube with a swirl
        // inside it, drawn flat on the floor. Glass: ALL, low health.
        CrookedRealm.groundWindowID = ObjectRegistry.registerObject("groundwindow",
                new SkyDecoObject("voidcube", 32, new Color(126, 66, 168), null, CATEGORY) {
                    @Override
                    public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
                        return BUILT_THING_LOOT;
                    }
                }.setTool(ToolType.ALL).setObjectHealth(12)
                        .setLight(45, 0.78F, 0.55F),
                0.0F, false);

        // Teeth-Rock -- A3.6's "a rock that reads as a clenched mouth".
        // Borrowed sheet: vanilla objects/smallrunestone.png (192x64, six
        // 32-wide variants), pale violet stubs standing out of the ground.
        // PICKAXE and 80 HP, because it is the one mineral formation here and
        // IMPLEMENTATION_RULES §4 puts rock and mineral on the pickaxe.
        CrookedRealm.teethRockID = ObjectRegistry.registerObject("teethrock",
                new SkyDecoObject("smallrunestone", 32, new Color(150, 140, 190), null, CATEGORY) {
                    @Override
                    public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
                        return TEETH_ROCK_LOOT;
                    }
                }.setTool(ToolType.PICKAXE).setObjectHealth(80),
                0.0F, false);

        // ===== The realm's container =====

        // RandomCrateObject asks Level.getCrateLootTable -> Biome
        // .getCrateLootTable, which all three Crooked biomes answer with their
        // own cargo -- so opening one tells the player which band they are
        // standing in. It reuses the mod's own skycrate sheet (there is no
        // objects/crookedcrate.png and none will be drawn in this pass):
        // RandomBreakObject.loadTextures resolves the debris sheet itself as
        // "objects/<texturePath>debris", so naming the texture "skycrate"
        // wires both halves at once.
        CrookedRealm.crookedCrateID = ObjectRegistry.registerObject("crookedcrate",
                new RandomCrateObject("skycrate"), 0.0F, false);

        allowShore("spiraltree", "eyeballshrub", "screamingflower", "stripedmushroom",
                "bentgrass", "bentlantern", "crookedclock", "longchair", "groundwindow",
                "teethrock", "crookedcrate");
    }

    // ---- loot ---------------------------------------------------------------

    /**
     * Oddwood off a Spiral Tree, with resin bleeding out of the cut.
     *
     * <p>Amounts already carry the realm's drop value
     * ({@code SkyMobTiers.CROOKED_DROP_VALUE} = 2.5, the measured tier-10 loot
     * figure): an ordinary level has no {@code LevelModifiers.LOOT} on it, so
     * the multiplier has to be written into the table rather than applied by the
     * engine.
     */
    static final LootTable SPIRAL_TREE_LOOT = new LootTable(
            LootItem.between("oddwood", 3, 6),
            ChanceLootItem.between(0.35F, "warpresin", 1, 2));

    /** An Eyeball Shrub is where Eye Seeds come from. That is the whole plant. */
    static final LootTable EYEBALL_SHRUB_LOOT = new LootTable(
            LootItem.between("eyeseed", 1, 2));

    /** A Screaming Flower bleeds resin when you cut it, and screams. */
    static final LootTable SCREAMING_FLOWER_LOOT = new LootTable(
            LootItem.between("warpresin", 1, 3));

    /** Striped Mushrooms are fibre, not food — nothing in this realm is food. */
    static final LootTable STRIPED_MUSHROOM_LOOT = new LootTable(
            LootItem.between("strangefabric", 1, 2),
            ChanceLootItem.between(0.20F, "warpresin", 1, 1));

    /**
     * Anything the realm's absent architect made, broken up.
     *
     * <p>Shared by the clock, the chair and the ground window on purpose: they
     * are the same material seen three ways, and giving each its own table would
     * be three rows of the ledger saying the same thing.
     */
    static final LootTable BUILT_THING_LOOT = new LootTable(
            LootItem.between("strangefabric", 1, 3),
            ChanceLootItem.between(0.25F, "realityshard", 1, 1));

    /**
     * The one node that gives Reality Shards reliably, and the reason a player
     * carries a pickaxe into this realm at all.
     */
    static final LootTable TEETH_ROCK_LOOT = new LootTable(
            LootItem.between("realityshard", 1, 2),
            ChanceLootItem.between(0.30F, "strangefabric", 1, 2));

    /**
     * Sky islands are small and so are these; right after region generation the
     * liquid height map is still settling, so {@code Level.isShore} reports true
     * across a fresh landmass and {@code Region.checkGenerationValid} would sweep
     * every prop away. The Skyreach hit this exactly and answered it the same
     * way (verified through its own status diagnostics).
     */
    private static void allowShore(String... objectStringIDs) {
        for (String stringID : objectStringIDs) {
            ObjectRegistry.getObject(ObjectRegistry.getObjectID(stringID)).canPlaceOnShore = true;
        }
    }
}
