package stairwaytoheaven.mobs;

import java.util.ArrayList;
import java.util.Collections;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.SettlerPersonalityRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.inventory.InventoryItem;
import stairwaytoheaven.TwilightWares;

/**
 * The Twilight Merchant (Zwielichtiger Haendler) — a travelling visitor built
 * on the same frame as vanilla's {@code ExoticMerchantHumanMob}: a
 * {@link HumanShop} that answers {@code isVisitorShop()} and seeds its stock on
 * its own unique ID while visiting, so every visit rolls a different shelf.
 *
 * <p>Everything he sells is the mod's own: {@link TwilightWares#OUTFITS} and
 * {@link TwilightWares#DECOR}. Per visit he brings {@link #OUTFITS_PER_VISIT}
 * whole outfits and {@link #DECOR_PER_VISIT} pieces of furniture, chosen once
 * into the shop blackboard exactly like vanilla's {@code decidedCosmetic}.
 */
public class TwilightMerchantHumanMob extends HumanShop {

    public static final int OUTFITS_PER_VISIT = 3;
    public static final int DECOR_PER_VISIT = 5;

    public TwilightMerchantHumanMob() {
        super(500, 200, "twilightmerchant");
        this.attackCooldown = 500;
        this.attackAnimTime = 500;
        this.setSwimSpeed(1.0F);
        this.equipmentInventory.setItem(6, new InventoryItem("ironsword"));

        for (String outfit : TwilightWares.AVAILABLE_OUTFITS) {
            SellingShopItem.ShopItemRequirement brought = picked("decidedOutfits", TwilightWares.AVAILABLE_OUTFITS,
                    OUTFITS_PER_VISIT, outfit);
            this.shop.addSellingItem(outfit + "head", new SellingShopItem()).setRandomPrice(300, 500).addRequirement(brought);
            this.shop.addSellingItem(outfit + "chest", new SellingShopItem()).setRandomPrice(350, 600).addRequirement(brought);
            this.shop.addSellingItem(outfit + "boots", new SellingShopItem()).setRandomPrice(250, 450).addRequirement(brought);
        }
        for (String decor : TwilightWares.AVAILABLE_DECOR) {
            this.shop.addSellingItem(decor, new SellingShopItem()).setRandomPrice(400, 900)
                    .addRequirement(picked("decidedDecor", TwilightWares.AVAILABLE_DECOR, DECOR_PER_VISIT, decor));
        }
    }

    /**
     * True when {@code id} is among the {@code count} entries drawn from
     * {@code pool} for this shop roll. The draw happens on the first call and
     * is kept in the blackboard, so all entries agree on one shelf.
     */
    private static SellingShopItem.ShopItemRequirement picked(String key, java.util.List<String> pool, int count, String id) {
        return (GameRandom random, ServerClient client, HumanShop mob, GameBlackboard blackboard) -> {
            String decided = blackboard.getString(key);
            if (decided == null) {
                ArrayList<String> shuffled = new ArrayList<>(pool);
                Collections.shuffle(shuffled, random);
                decided = "," + String.join(",", shuffled.subList(0, Math.min(count, shuffled.size()))) + ",";
                blackboard.set(key, decided);
            }
            return decided.contains("," + id + ",");
        };
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
