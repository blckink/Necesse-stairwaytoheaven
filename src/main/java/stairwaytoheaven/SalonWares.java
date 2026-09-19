package stairwaytoheaven;

import java.awt.Color;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.HappinessObject;
import necesse.level.gameObject.PaintingObject;
import necesse.level.gameObject.TableDecorationObject;
import necesse.level.gameObject.furniture.ChairObject;
import stairwaytoheaven.mobs.SalonStylistMob;
import stairwaytoheaven.objects.SkyDecoObject;
import stairwaytoheaven.objects.SkyWallLightObject;

/**
 * The salon fit-out the Stylist sells — a shop that matches her trade, the way
 * the Farmer sells seeds and the Angler sells bait.
 *
 * <p>Six pieces, each on the vanilla frame that already gets its geometry
 * right, so nothing here re-implements placement or rotation:
 *
 * <table>
 *   <tr><th>ID</th><th>frame</th><th>sheet</th></tr>
 *   <tr><td>{@code salonchair}</td><td>{@code ChairObject}</td><td>{@code objects/salonchair.png}, 128xH — four 32-wide columns, one per facing, column 0 = seen from the back</td></tr>
 *   <tr><td>{@code salonmirror}</td><td>{@code PaintingObject}</td><td>{@code objects/paintings/salonmirror.png}, 32x128 — one 32x32 cell per wall direction, side views narrow</td></tr>
 *   <tr><td>{@code salonsign}</td><td>{@code WallTorchObject}</td><td>{@code objects/salonsign.png}, 64x128 — lit/unlit column x four attach orientations</td></tr>
 *   <tr><td>{@code saloncashregister}</td><td>{@code TableDecorationObject}</td><td>{@code objects/saloncashregister.png}, 128xH — four 32-wide columns, one per facing</td></tr>
 *   <tr><td>{@code salonproducts}</td><td>{@code TableDecorationObject}</td><td>{@code objects/salonproducts.png}, 128xH</td></tr>
 *   <tr><td>{@code salonbarberpole}</td><td>{@code SkyDecoObject}</td><td>{@code objects/salonbarberpole.png}, 32xH, bottom-anchored, glows</td></tr>
 * </table>
 *
 * <p>Every piece is a {@link HappinessObject}. That is vanilla's own mechanism
 * for "this furniture makes settlers happier" — the same interface behind the
 * Sheep Chair and the Wooden Duck — and it pays out per room, so a settler
 * living over the salon gets the bonus the user asked for. All six are
 * {@code RARE} or better, which is 40–50 happiness each through
 * {@code HappinessObject.getDefaultHappinessBonus}.
 *
 * <p>Like {@link TwilightWares}, a piece whose sheet is not in the jar is
 * neither registered nor sold, so a half-drawn batch never shelves a missing
 * texture and the build stays green while the art is still being drawn.
 */
public final class SalonWares {

    /** One shelf entry: the object ID and what she wants for it. */
    public static final class Ware {
        public final String id;
        public final int bestPrice;
        public final int worstPrice;
        public final int priceRange;

        Ware(String id, int bestPrice, int worstPrice, int priceRange) {
            this.id = id;
            this.bestPrice = bestPrice;
            this.worstPrice = worstPrice;
            this.priceRange = priceRange;
        }
    }

    /**
     * The shelf in the order it should read: the two pieces that make a salon
     * a salon first, then the counter, then the decoration.
     */
    private static final Ware[] SHELF = {
            new Ware("salonchair", 900, 1400, 100),
            new Ware("salonmirror", 700, 1100, 80),
            new Ware("salonsign", 600, 950, 80),
            new Ware("saloncashregister", 800, 1250, 90),
            new Ware("salonproducts", 250, 400, 40),
            new Ware("salonbarberpole", 450, 700, 60),
    };

    private static final String[] CATEGORY = {"objects", "furniture", "salon"};
    private static final Color MAP_CHAIR = new Color(58, 44, 66);
    private static final Color MAP_MIRROR = new Color(198, 214, 222);
    private static final Color MAP_BRASS = new Color(196, 152, 68);
    private static final Color MAP_POLE = new Color(206, 60, 60);

    /** The pieces whose sheets actually shipped, in {@link #SHELF} order. */
    private static final List<Ware> AVAILABLE = new ArrayList<>();

    private SalonWares() {
    }

    public static List<Ware> available() {
        return AVAILABLE;
    }

    public static void register() {
        ItemCategory.createCategory("E-E-R", CATEGORY);
        ItemCategory.craftingManager.createCategory("E-B-R", CATEGORY);

        for (Ware ware : SHELF) {
            if (drawn(ware.id)) {
                AVAILABLE.add(ware);
            }
        }

        // The chair: vanilla's own ChairObject, so a settler can actually sit
        // in it and the four rotations, the sit offsets and the
        // draw-behind-the-user rule all come from the game.
        if (has("salonchair")) {
            ObjectRegistry.registerObject("salonchair",
                    new SalonChairObject("salonchair", MAP_CHAIR, CATEGORY), 30.0F, true);
        }
        // The mirror hangs on the wall, not through it: PaintingObject is the
        // frame vanilla's own wall pieces use, on WALL_DECOR, with one cell per
        // wall direction so the side views are narrow by construction.
        if (has("salonmirror")) {
            ObjectRegistry.registerObject("salonmirror",
                    new SalonMirrorObject(), 30.0F, true);
        }
        // The shop sign attaches the way a lamp does, because it is one:
        // WallTorchObject carries the attach/orientation/wiring logic whole.
        if (has("salonsign")) {
            ObjectRegistry.registerObject("salonsign",
                    new SalonSignObject(), 30.0F, true);
        }
        if (has("saloncashregister")) {
            ObjectRegistry.registerObject("saloncashregister",
                    new SalonTableObject("saloncashregister", MAP_BRASS, 18, 14), 30.0F, true);
        }
        if (has("salonproducts")) {
            ObjectRegistry.registerObject("salonproducts",
                    new SalonTableObject("salonproducts", MAP_MIRROR, 16, 12), 20.0F, true);
        }
        if (has("salonbarberpole")) {
            ObjectRegistry.registerObject("salonbarberpole",
                    new SalonPoleObject(), 25.0F, true);
        }

        replaceVanillaStylist();
    }

    private static boolean has(String id) {
        return AVAILABLE.stream().anyMatch(w -> w.id.equals(id));
    }

    /** Both sheet homes, since the mirror sits under {@code objects/paintings/}. */
    private static boolean drawn(String id) {
        return TwilightWares.inJar("objects/" + id + ".png")
                || TwilightWares.inJar("objects/paintings/" + id + ".png");
    }

    /**
     * Puts {@link SalonStylistMob} behind the unchanged mob string ID
     * {@code stylisthuman}.
     *
     * <h2>Why this is done by hand</h2>
     *
     * The shop has to be filled before {@code HumanShop.init()} closes the shop
     * registries, which means it has to happen in the mob's constructor, which
     * means the Stylist has to be our class. {@code MobRegistry} has no public
     * {@code replaceMob} the way {@code ItemRegistry}, {@code ObjectRegistry}
     * and {@code SettlerRegistry} have theirs, so the replacement goes through
     * {@code GameRegistry.replaceObj} by reflection. The registry is still open
     * here — this runs from {@code init()}.
     *
     * <h2>Why not a new mob ID</h2>
     *
     * {@code SettlerRegistry.replaceSettler} would be public API, but vanilla
     * names this mob by string in five places
     * ({@code PirateVillageBossPreset}, {@code VillageHouse5Preset},
     * {@code StylistSettler}, {@code FreeStylistJournalChallenge},
     * {@code RescueSettlerRewardPerk}) and only three of them go through the
     * Settler. Keeping the string means all five keep working and stylists in
     * existing worlds load as salon owners. No vanilla code looks this mob up
     * by {@code StylistHumanMob.class}, so the subclass breaks no lookup.
     *
     * <h2>The one flag that must change</h2>
     *
     * {@code createSpawnItem} is passed {@code false}. Vanilla registered the
     * Stylist with it {@code true}, and {@code MobRegistry.onRegister} runs
     * again on a replace — a second pass would try to register
     * {@code stylisthumanspawnitem} twice and take the whole mod down. The
     * spawn item from the first registration stays mapped to the same mob ID.
     *
     * <p>Failure here is logged and swallowed: a Stylist without a salon is a
     * worse game, a mod that refuses to load is no game at all.
     */
    private static void replaceVanillaStylist() {
        try {
            Class<?> elementClass = Class.forName("necesse.engine.registries.MobRegistry$MobRegistryElement");
            Constructor<?> ctor = elementClass.getDeclaredConstructor(Class.class, boolean.class, boolean.class,
                    boolean.class, necesse.engine.localization.message.GameMessage.class,
                    necesse.engine.localization.message.GameMessage.class);
            ctor.setAccessible(true);
            Object element = ctor.newInstance(SalonStylistMob.class, true, false, false,
                    new LocalMessage("mob", "stylisthuman"), null);

            Class<?> registryClass = Class.forName("necesse.engine.registries.GameRegistry");
            Method replaceObj = registryClass.getDeclaredMethod("replaceObj", String.class,
                    Class.forName("necesse.engine.registries.IDDataContainer"));
            replaceObj.setAccessible(true);
            replaceObj.invoke(MobRegistry.instance, "stylisthuman", element);
        } catch (ReflectiveOperationException | RuntimeException e) {
            System.err.println("[stairwaytoheaven] Could not give the Stylist her salon shop: " + e);
            e.printStackTrace();
        }
    }

    /** Vanilla's SheepChairObject, with our texture. */
    static class SalonChairObject extends ChairObject implements HappinessObject {
        SalonChairObject(String textureName, Color mapColor, String... category) {
            super(textureName, ToolType.ALL, mapColor, category);
            this.rarity = Item.Rarity.RARE;
        }

        @Override
        public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
            ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
            tooltips.add(this.getHappinessObjectTooltip());
            return tooltips;
        }
    }

    /**
     * The lit mirror. The light is GameObject's own, at the level of a candle
     * pedestal rather than a torch — bulbs around a mirror, not a fire — and
     * nearly unsaturated, so it reads as white salon light.
     */
    static class SalonMirrorObject extends PaintingObject implements HappinessObject {
        SalonMirrorObject() {
            super(Item.Rarity.EPIC);
            this.lightLevel = 100;
            this.lightHue = 45.0F;
            this.lightSat = 0.12F;
            this.roomProperties.add("lights");
        }

        @Override
        public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
            ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
            tooltips.add(this.getHappinessObjectTooltip());
            return tooltips;
        }
    }

    /** The shop sign outside: a wall lamp that says what the shop is. */
    static class SalonSignObject extends SkyWallLightObject implements HappinessObject {
        SalonSignObject() {
            super("salonsign", 130, 320.0F, 0.45F);
            this.rarity = Item.Rarity.RARE;
            this.setItemCategory(CATEGORY);
            this.setCraftingCategory(CATEGORY);
        }

        @Override
        public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
            ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
            tooltips.add(this.getHappinessObjectTooltip());
            return tooltips;
        }
    }

    /** Vanilla's WoodenDuckObject shape: stands on a table, four facings. */
    static class SalonTableObject extends TableDecorationObject implements HappinessObject {
        SalonTableObject(String textureName, Color mapColor, int width, int height) {
            super(textureName, ToolType.ALL, mapColor, width, height, 0, 0);
            this.rarity = Item.Rarity.RARE;
        }

        @Override
        public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
            ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
            tooltips.add(this.getHappinessObjectTooltip());
            return tooltips;
        }
    }

    /** The barber pole by the door — one tile, no collision, its own glow. */
    static class SalonPoleObject extends SkyDecoObject implements HappinessObject {
        SalonPoleObject() {
            super("salonbarberpole", 32, MAP_POLE, null, CATEGORY);
            this.rarity = Item.Rarity.RARE;
            this.setLight(90, 0.0F, 0.35F);
        }

        @Override
        public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
            ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
            tooltips.add(this.getHappinessObjectTooltip());
            return tooltips;
        }
    }
}
