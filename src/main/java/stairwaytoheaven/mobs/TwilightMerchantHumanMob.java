package stairwaytoheaven.mobs;

import java.util.ArrayList;
import java.util.Collections;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.SettlerPersonalityRegistry;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.inventory.InventoryItem;
import stairwaytoheaven.TwilightWares;

/**
 * The Twilight Merchant (Zwielichtiger Haendler) — a travelling visitor built
 * on the same frame as vanilla's {@code ExoticMerchantHumanMob}: a
 * {@link HumanShop} that answers {@code isVisitorShop()} and seeds its stock on
 * its own unique ID while visiting, so every visit rolls different prices.
 *
 * <p>Everything he sells is the mod's own: all of
 * {@link TwilightWares#AVAILABLE_OUTFITS} and {@link TwilightWares#AVAILABLE_DECOR},
 * on every visit.
 */
public class TwilightMerchantHumanMob extends HumanShop {

    public TwilightMerchantHumanMob() {
        super(500, 200, "twilightmerchant");
        this.attackCooldown = 500;
        this.attackAnimTime = 500;
        this.setSwimSpeed(1.0F);
        this.equipmentInventory.setItem(6, new InventoryItem("ironsword"));

        // He brings his whole shelf on every visit: every outfit and every
        // piece of furniture whose sheet shipped. Only the prices roll.
        for (String outfit : TwilightWares.AVAILABLE_OUTFITS) {
            this.shop.addSellingItem(outfit + "head", new SellingShopItem()).setRandomPrice(300, 500);
            this.shop.addSellingItem(outfit + "chest", new SellingShopItem()).setRandomPrice(350, 600);
            this.shop.addSellingItem(outfit + "boots", new SellingShopItem()).setRandomPrice(250, 450);
        }
        for (String decor : TwilightWares.AVAILABLE_DECOR) {
            this.shop.addSellingItem(decor, new SellingShopItem()).setRandomPrice(400, 900);
        }
    }

    @Override
    protected void setupPersonalities() {
        this.personalities = new ArrayList<>(Collections.singletonList(
                SettlerPersonalityRegistry.getNewSettlerPersonality("twilightmerchant", (HumanMob) this)));
    }

    @Override
    protected ArrayList<GameMessage> getMessages(ServerClient client) {
        return this.getLocalMessages("misc", "twilightmerchanttalk", 5);
    }

    @Override
    public long getShopSeed() {
        if (this.isVisitor()) {
            return this.getUniqueID();
        }
        return super.getShopSeed();
    }

    @Override
    public boolean isVisitorShop() {
        return true;
    }
}
