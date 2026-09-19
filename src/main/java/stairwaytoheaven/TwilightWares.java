package stairwaytoheaven;

import java.awt.Color;

import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.SettlerPersonalityRegistry;
import necesse.engine.registries.SettlerRegistry;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.armorItem.BootsArmorItem;
import necesse.inventory.item.armorItem.ChestArmorItem;
import necesse.inventory.item.armorItem.HelmetArmorItem;
import necesse.level.gameObject.LargePaintingObject;
import necesse.level.gameObject.PaintingObject;
import necesse.level.gameObject.TableDecorationObject;
import necesse.level.gameObject.furniture.BedObject;
import necesse.level.gameObject.furniture.CandelabraObject;
import necesse.level.gameObject.furniture.ChairObject;
import necesse.level.gameObject.furniture.ClockObject;
import necesse.level.gameObject.happinessObject.SarcophagusObject;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementVisitorOdds;
import necesse.level.maps.levelData.settlementData.SettlementVisitorSpawner;
import necesse.level.maps.levelData.settlementData.settler.personalities.SimplePersonalityFilter;
import stairwaytoheaven.mobs.TwilightMerchantHumanMob;
import stairwaytoheaven.objects.SkyDecoObject;
import stairwaytoheaven.settlement.TwilightMerchantPersonality;
import stairwaytoheaven.settlement.TwilightMerchantSettler;

/**
 * The Twilight Merchant and everything on his shelf: eleven cosmetic outfits
 * (head, chest with arms, boots — armor 0) and ten pieces of gloomy furniture.
 * None of it has a recipe; he is the only way to get it.
 *
 * <p>He visits settlements through vanilla's own visitor lottery
 * ({@link ServerSettlementData#visitorOdds}), the same list the Exotic
 * Merchant, Pawnbroker and Animal Merchant draw from.
 */
public final class TwilightWares {

    /** Outfit IDs; each registers {@code <id>head}, {@code <id>chest}, {@code <id>boots}. */
    public static final String[] OUTFITS = {
            "twilightsuit", "skeleton", "grinclown", "hockeyslasher", "dreamstalker", "screamrobe",
            "pincushion", "widowgown", "stitchedmonster", "pumpkinscarecrow", "hauntedpuppet"
    };

    /** Object IDs of the furniture he sells. */
    public static final String[] DECOR = {
            "coffinbed", "hauntedclock", "hangingtree", "skullcandelabra",
            "thingbox", "twilightcauldron", "sandwormtombstone", "electricchair",
            "walleye", "hauntedwallclock", "magicmirror", "shrunkenheads", "twilightsarcophagus",
            "hauntedscarecrow"
    };

    /**
     * The two framed 1-tile paintings of the first batch. They are no longer
     * sold, but stay registered while their sheet ships so that pieces already
     * hanging in a world keep loading.
     */
    private static final String[] RETIRED_PAINTINGS = {"eyepainting", "shrunkenheadtrophy"};

    /** Free-form wall pieces on vanilla's 2-tile large painting frame (64x256 sheet). */
    private static final String[] LARGE_WALL_PIECES = {"walleye", "hauntedwallclock", "magicmirror", "shrunkenheads"};

    private static final String[] CATEGORY = {"objects", "furniture", "twilight"};
    private static final Color MAP_COFFIN = new Color(52, 36, 44);
    private static final Color MAP_BONE = new Color(214, 206, 180);
    private static final Color MAP_SLIME = new Color(120, 200, 60);
    private static final Color MAP_PUMPKIN = new Color(214, 106, 32);

    /**
     * The outfits and furniture whose sheets are actually in the jar. The
     * wares arrive in batches as they are drawn; an entry without its sheet is
     * neither registered nor sold, so the merchant never shelves a missing
     * texture. IDs only ever get added, so a later batch is save-compatible.
     */
    public static final java.util.List<String> AVAILABLE_OUTFITS = new java.util.ArrayList<>();
    public static final java.util.List<String> AVAILABLE_DECOR = new java.util.ArrayList<>();

    private TwilightWares() {
    }

    /**
     * The mod jar keeps its textures under {@code resources/}, not at the jar
     * root. Checking the bare path found nothing, so on 2026-09-16 the merchant
     * arrived with no wares and without his suit; both forms are accepted so
     * the check also holds when resources sit at the root (IDE runs).
     */
    static boolean inJar(String path) {
        ClassLoader loader = TwilightWares.class.getClassLoader();
        return loader.getResource("resources/" + path) != null || loader.getResource(path) != null;
    }

    public static boolean outfitDrawn(String outfit) {
        return inJar("player/armor/" + outfit + "head.png") && inJar("player/armor/" + outfit + "chest.png")
                && inJar("player/armor/" + outfit + "arms_left.png") && inJar("player/armor/" + outfit + "boots.png");
    }

    private static boolean decorDrawn(String id) {
        return inJar("objects/" + id + ".png") || inJar("objects/paintings/" + id + ".png");
    }

    public static void register() {
        ItemCategory.createCategory("E-E-Q", "objects", "furniture", "twilight");
        ItemCategory.craftingManager.createCategory("E-B-Q", "objects", "furniture", "twilight");

        for (String outfit : OUTFITS) {
            if (outfitDrawn(outfit)) {
                AVAILABLE_OUTFITS.add(outfit);
            }
        }
        for (String decor : DECOR) {
            if (decorDrawn(decor)) {
                AVAILABLE_DECOR.add(decor);
            }
        }

        for (String outfit : AVAILABLE_OUTFITS) {
            ItemRegistry.registerItem(outfit + "head", new Head(outfit + "head"), 0.0F, true);
            ItemRegistry.registerItem(outfit + "chest", new Chest(outfit), 0.0F, true);
            ItemRegistry.registerItem(outfit + "boots", new Boots(outfit + "boots"), 0.0F, true);
        }

        // A real BedObject, so a settler can be assigned to the coffin.
        if (AVAILABLE_DECOR.contains("coffinbed")) {
            BedObject.registerBed("coffinbed", "coffinbed", MAP_COFFIN, 100.0F, CATEGORY);
        }
        if (AVAILABLE_DECOR.contains("hauntedclock")) {
            ObjectRegistry.registerObject("hauntedclock", new ClockObject("hauntedclock", MAP_COFFIN, CATEGORY), 10.0F, true);
        }
        if (AVAILABLE_DECOR.contains("skullcandelabra")) {
            ObjectRegistry.registerObject("skullcandelabra",
                    new CandelabraObject("skullcandelabra", MAP_BONE, 50.0F, 0.12F, CATEGORY), 10.0F, true);
        }
        if (AVAILABLE_DECOR.contains("electricchair")) {
            ObjectRegistry.registerObject("electricchair", new ChairObject("electricchair", MAP_COFFIN, CATEGORY), 5.0F, true);
        }
        if (AVAILABLE_DECOR.contains("thingbox")) {
            ObjectRegistry.registerObject("thingbox", new TableDecorationObject("thingbox", MAP_COFFIN, 16, 14), 20.0F, true);
        }
        // The merchant's own sarcophagus on vanilla's SarcophagusObject: one
        // 32 px column per rotation in objects/<id>.png, standing on its tile.
        if (AVAILABLE_DECOR.contains("twilightsarcophagus")) {
            ObjectRegistry.registerObject("twilightsarcophagus",
                    new SarcophagusObject("twilightsarcophagus", MAP_COFFIN, CATEGORY), 50.0F, true);
        }
        // PaintingObject reads objects/paintings/<id>.png (32x128, one 32x32
        // cell per wall direction) and hangs on WALL_DECOR.
        for (String retired : RETIRED_PAINTINGS) {
            if (decorDrawn(retired)) {
                ObjectRegistry.registerObject(retired, new PaintingObject(Item.Rarity.RARE), 20.0F, true);
            }
        }
        // The free-form wall pieces use vanilla's large painting frame: two
        // tiles wide, registered as <id> and its far half <id>2. The sheet is
        // an unframed shape with transparent surroundings.
        for (String piece : LARGE_WALL_PIECES) {
            if (AVAILABLE_DECOR.contains(piece)) {
                LargePaintingObject.registerLargePainting(piece, Item.Rarity.EPIC, 20.0F, true, true);
            }
        }
        if (AVAILABLE_DECOR.contains("hangingtree")) {
            ObjectRegistry.registerObject("hangingtree", new SkyDecoObject("hangingtree", 128, MAP_COFFIN, null, CATEGORY), 20.0F, true);
        }
        // Not "witchcauldron": vanilla already registers that ID, and the
        // duplicate stopped the whole mod from loading.
        if (AVAILABLE_DECOR.contains("twilightcauldron")) {
            ObjectRegistry.registerObject("twilightcauldron", new SkyDecoObject("twilightcauldron", 64, MAP_SLIME, null, CATEGORY), 20.0F, true);
        }
        // The scarecrow for the fields: one tile, and its eyes keep burning.
        // The light is GameObject's own (lightLevel/lightHue/lightSat through
        // SkyDecoObject.setLight): 100 is vanilla's CandlePedestalObject, a
        // glow around the post rather than a torch's 150, and hue 25 at 0.9
        // saturation is pumpkin orange. No collision, like the hanging tree —
        // a scarecrow that blocks the tile would fight the farm it stands in.
        if (AVAILABLE_DECOR.contains("hauntedscarecrow")) {
            ObjectRegistry.registerObject("hauntedscarecrow",
                    new SkyDecoObject("hauntedscarecrow", 32, MAP_PUMPKIN, null, CATEGORY)
                            .setLight(100, 25.0F, 0.9F), 20.0F, true);
        }
        if (AVAILABLE_DECOR.contains("sandwormtombstone")) {
            ObjectRegistry.registerObject("sandwormtombstone",
                    new SkyDecoObject("sandwormtombstone", 32, MAP_BONE, null, CATEGORY), 20.0F, true);
        }

        // Only the merchant himself may roll this personality, as vanilla
        // whitelists "exoticmerchant" to the Exotic Merchant.
        SettlerPersonalityRegistry.registerSettlerPersonality("twilightmerchant", TwilightMerchantPersonality.class,
                new SimplePersonalityFilter(100).makeSettlerStringIDsWhitelist().filterSettlerStringID("twilightmerchant"),
                false);
        MobRegistry.registerMob("twilightmerchanthuman", TwilightMerchantHumanMob.class, false);
        SettlerRegistry.registerSettler("twilightmerchant", new TwilightMerchantSettler());
        ServerSettlementData.visitorOdds.add(new SettlementVisitorOdds("twilightmerchant") {
            @Override
            public boolean canSpawn(ServerSettlementData data) {
                return true;
            }

            @Override
            public int getTickets(ServerSettlementData data) {
                // A little rarer than the Exotic Merchant's 100. Vanilla drops a
                // repeat visitor to 25 through lastVisitorIdentifier, which is
                // private to ServerSettlementData, so there is no repeat damping.
                return 60;
            }

            @Override
            public SettlementVisitorSpawner getNewVisitorSpawner(ServerSettlementData data) {
                return new SettlementVisitorSpawner(this,
                        (HumanMob) MobRegistry.getMob("twilightmerchanthuman", data.getLevel()));
            }
        });
    }

    static class Head extends HelmetArmorItem {
        Head(String id) {
            super(0, null, 0, Item.Rarity.EPIC, id, null);
            this.hairDrawOptions = ArmorItem.HairDrawMode.NO_HAIR;
        }
    }

    static class Chest extends ChestArmorItem {
        Chest(String outfit) {
            super(0, 0, Item.Rarity.EPIC, outfit + "chest", outfit + "arms", null);
        }
    }

    static class Boots extends BootsArmorItem {
        Boots(String id) {
            super(0, 0, Item.Rarity.EPIC, id, null);
        }
    }
}
