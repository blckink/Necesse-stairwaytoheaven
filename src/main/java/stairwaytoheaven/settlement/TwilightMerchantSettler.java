package stairwaytoheaven.settlement;

import necesse.gfx.HumanLook;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.inventory.InventoryItem;
import necesse.level.maps.levelData.settlementData.settler.Settler;

/**
 * Settlement type of the Twilight Merchant, after vanilla's
 * {@code ExoticMerchantSettler}: not part of a complete host, and dressed in
 * his own striped suit, which he also sells.
 */
public class TwilightMerchantSettler extends Settler {

    public TwilightMerchantSettler() {
        super("twilightmerchanthuman");
        this.isPartOfCompleteHost = false;
    }

    @Override
    public void setDefaultArmor(HumanDrawOptions drawOptions, int settlerSeed, HumanLook look, boolean customLook) {
        drawOptions.helmet(new InventoryItem("twilightsuithead"));
        drawOptions.chestplate(new InventoryItem("twilightsuitchest"));
        drawOptions.boots(new InventoryItem("twilightsuitboots"));
    }
}
