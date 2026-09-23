package stairwaytoheaven.settlement;

import necesse.gfx.HumanLook;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.InventoryItem;
import necesse.level.maps.levelData.settlementData.settler.Settler;
import stairwaytoheaven.TwilightWares;

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

    /**
     * Vanilla's Exotic Merchant face, the visitor he is built after. Settler's
     * own loadTextures looks for mobs/icons/twilightmerchanthuman, then
     * mobs/icons/twilightmerchant, then settlers/twilightmerchant
     * (Settler.java:232-289, decompiled 1.3.2); none of them ships, so the
     * last one handed back the ERR tile. Row in docs/VANILLA_ASSET_MAP.md.
     */
    @Override
    public void loadTextures() {
        this.texture = GameTexture.fromFile("mobs/icons/exoticmerchanthuman");
    }

    @Override
    public void setDefaultArmor(HumanDrawOptions drawOptions, int settlerSeed, HumanLook look, boolean customLook) {
        // Until his own suit is drawn he visits in his plain body rather than
        // in an item that is not registered.
        if (!TwilightWares.AVAILABLE_OUTFITS.contains("twilightsuit")) {
            return;
        }
        drawOptions.helmet(new InventoryItem("twilightsuithead"));
        drawOptions.chestplate(new InventoryItem("twilightsuitchest"));
        drawOptions.boots(new InventoryItem("twilightsuitboots"));
    }
}
