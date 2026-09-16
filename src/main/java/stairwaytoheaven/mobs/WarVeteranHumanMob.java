package stairwaytoheaven.mobs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.HumanAI;
import necesse.entity.mobs.ai.behaviourTree.util.AIMover;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.inventory.InventoryItem;

/**
 * The War Veteran (Kriegsveteran) — a soldier profession, in the sense of
 * {@code ProfessionSettler}: any settlement can hold one, a second can turn
 * up if the first dies, and the player may send them away again. Not a
 * {@link SkySettlerMob} (that base is for the mod's fixed-identity named
 * residents with {@code canTakeDamage() == false}); this extends
 * {@link HumanShop} directly, the same as vanilla's own Guard and this mod's
 * Therapist/Doctor.
 *
 * <p>He carries a sword and fights on sight: {@code HumanAI}'s
 * {@code attackHostiles} flag (see {@code SkySettlerMob#attacksHostiles}
 * javadoc) is what makes vanilla settlers actively engage raiders rather than
 * wait to be hit, and is passed {@code true} here exactly as vanilla's own
 * Miner/Explorer/Hunter do. His shop sells the settlement's defense
 * structures for coins.
 */
public class WarVeteranHumanMob extends HumanShop {

    public WarVeteranHumanMob() {
        // (health, speed, settlerStringID). Third argument is the
        // SettlerRegistry key, NOT this mob's own ID.
        super(600, 220, "warveteran");
        this.attackCooldown = 500;
        this.attackAnimTime = 500;
        this.equipmentInventory.setItem(6, new InventoryItem("ironsword"));

        // The shop: defense structures for coins.
        this.shop.addSellingItem("veteranbarricade", new SellingShopItem())
                .setStaticPriceBasedOnHappiness(120, 220, 20);
        this.shop.addSellingItem("barbedwirefence", new SellingShopItem())
                .setStaticPriceBasedOnHappiness(90, 160, 15);
        this.shop.addSellingItem("veteranturret", new SellingShopItem())
                .setStaticPriceBasedOnHappiness(600, 950, 60);
        this.shop.addSellingItem("veterancatapult", new SellingShopItem())
                .setStaticPriceBasedOnHappiness(1400, 2200, 150);
    }

    /**
     * Patrols the settlement like any settler, and actively engages hostiles
     * on sight rather than waiting to be attacked first.
     */
    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(this, new HumanAI<>(320, true, false, 25000),
                new AIMover(HumanMob.humanPathIterations));
    }

    @Override
    protected ArrayList<GameMessage> getMessages(ServerClient client) {
        return getLocalMessages("mobmsg", "warveterantalk", 5);
    }

    @Override
    public List<InventoryItem> getRecruitItems(ServerClient client) {
        return Collections.singletonList(new InventoryItem("coin", 900));
    }
}
