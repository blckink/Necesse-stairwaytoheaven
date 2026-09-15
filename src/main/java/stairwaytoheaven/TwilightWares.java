package stairwaytoheaven;

import java.awt.Color;

import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.SettlerRegistry;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.armorItem.BootsArmorItem;
import necesse.inventory.item.armorItem.ChestArmorItem;
import necesse.inventory.item.armorItem.HelmetArmorItem;
import necesse.level.gameObject.PaintingObject;
import necesse.level.gameObject.TableDecorationObject;
import necesse.level.gameObject.furniture.BedObject;
import necesse.level.gameObject.furniture.CandelabraObject;
import necesse.level.gameObject.furniture.ChairObject;
import necesse.level.gameObject.furniture.ClockObject;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementVisitorOdds;
import necesse.level.maps.levelData.settlementData.SettlementVisitorSpawner;
import stairwaytoheaven.mobs.TwilightMerchantHumanMob;
import stairwaytoheaven.objects.SkyDecoObject;
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
            "coffinbed", "eyepainting", "hauntedclock", "hangingtree", "skullcandelabra",
            "thingbox", "shrunkenheadtrophy", "witchcauldron", "sandwormtombstone", "electricchair"
    };

    private static final String[] CATEGORY = {"objects", "furniture", "twilight"};
    private static final Color MAP_COFFIN = new Color(52, 36, 44);
    private static final Color MAP_BONE = new Color(214, 206, 180);
    private static final Color MAP_SLIME = new Color(120, 200, 60);

    private TwilightWares() {
    }

    public static void register() {
        ItemCategory.createCategory("E-E-Q", "objects", "furniture", "twilight");
        ItemCategory.craftingManager.createCategory("E-B-Q", "objects", "furniture", "twilight");

        for (String outfit : OUTFITS) {
            ItemRegistry.registerItem(outfit + "head", new Head(outfit + "head"), 0.0F, true);
            ItemRegistry.registerItem(outfit + "chest", new Chest(outfit), 0.0F, true);
            ItemRegistry.registerItem(outfit + "boots", new Boots(outfit + "boots"), 0.0F, true);
        }

        // A real BedObject, so a settler can be assigned to the coffin.
        BedObject.registerBed("coffinbed", "coffinbed", MAP_COFFIN, 100.0F, CATEGORY);
        ObjectRegistry.registerObject("hauntedclock", new ClockObject("hauntedclock", MAP_COFFIN, CATEGORY), 10.0F, true);
        ObjectRegistry.registerObject("skullcandelabra",
                new CandelabraObject("skullcandelabra", MAP_BONE, 50.0F, 0.12F, CATEGORY), 10.0F, true);
        ObjectRegistry.registerObject("electricchair", new ChairObject("electricchair", MAP_COFFIN, CATEGORY), 5.0F, true);
        ObjectRegistry.registerObject("thingbox", new TableDecorationObject("thingbox", MAP_COFFIN, 16, 14), 20.0F, true);
        // PaintingObject reads objects/paintings/<id>.png and hangs on WALL_DECOR.
        ObjectRegistry.registerObject("eyepainting", new PaintingObject(Item.Rarity.RARE), 20.0F, true);
        ObjectRegistry.registerObject("shrunkenheadtrophy", new PaintingObject(Item.Rarity.RARE), 20.0F, true);
        ObjectRegistry.registerObject("hangingtree", new SkyDecoObject("hangingtree", 128, MAP_COFFIN, null, CATEGORY), 20.0F, true);
        ObjectRegistry.registerObject("witchcauldron", new SkyDecoObject("witchcauldron", 64, MAP_SLIME, null, CATEGORY), 20.0F, true);
        ObjectRegistry.registerObject("sandwormtombstone",
                new SkyDecoObject("sandwormtombstone", 32, MAP_BONE, null, CATEGORY), 20.0F, true);

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
