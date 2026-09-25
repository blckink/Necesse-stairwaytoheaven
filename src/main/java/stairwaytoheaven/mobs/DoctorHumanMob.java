package stairwaytoheaven.mobs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MobHealthChangeEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.util.AIMover;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanShop.BuyingShopItem;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.inventory.InventoryItem;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.settler.dialogues.SettlerDialogue;
import stairwaytoheaven.settlement.DoctorHealDialogue;
import stairwaytoheaven.settlement.SkyDoctor;

/**
 * The Doctor — the person behind the profession.
 *
 * <h2>The shop: the good stuff, at vanilla's own exchange rate</h2>
 *
 * Eleven consumables, every one of them a vanilla item the player could already
 * craft — six potions and five cooked dishes, all of which carry real
 * {@code BuffModifiers} rather than just filling a hunger bar. Nothing here is
 * new content; the Doctor is a shortcut past the Alchemy Table and the Cooking
 * Pot when you are about to go somewhere unpleasant, which is what "sells
 * steroids" means.
 *
 * <h2>Where the prices come from</h2>
 *
 * Not invented. Vanilla's own Alchemist prices its buff potions at exactly
 * <b>min = 2x the item's broker value, max = 6x, step = 1x</b> — read out of the
 * 1.3.3 jar: {@code speedpotion} is registered at broker value 10 and sold at
 * {@code setStaticPriceBasedOnHappiness(20, 60, 10)}; {@code healthregenpotion}
 * and {@code attackspeedpotion} the same; {@code healthpotion} is value 5 at
 * (5, 25, 5). Every line below applies that rule to the item's own registered
 * value, so a Greater Battle Potion (value 50) lands at 100-300 and a plate of
 * Spaghetti Bolognese (value 30) at 60-180. That is {@code docs/BALANCE.md}'s
 * rule — calibrate against the vanilla thing of the same class — applied to a
 * shop instead of a weapon.
 *
 * <table>
 * <tr><th>item</th><th>broker value</th><th>sold at</th><th>what it does</th></tr>
 * <tr><td>superiorhealthpotion</td><td>10</td><td>20-60</td><td>the big heal</td></tr>
 * <tr><td>greaterspeedpotion</td><td>20</td><td>40-120</td><td>movement</td></tr>
 * <tr><td>greaterattackspeedpotion</td><td>20</td><td>40-120</td><td>attack speed</td></tr>
 * <tr><td>greaterhealthregenpotion</td><td>20</td><td>40-120</td><td>regeneration</td></tr>
 * <tr><td>greaterresistancepotion</td><td>30</td><td>60-180</td><td>damage taken</td></tr>
 * <tr><td>greaterbattlepotion</td><td>50</td><td>100-300</td><td>damage dealt</td></tr>
 * <tr><td>porktenderloin</td><td>35</td><td>70-210</td><td>+80 max health, combat regen</td></tr>
 * <tr><td>deepfriedchicken</td><td>35</td><td>70-210</td><td>speed, resilience, attack speed</td></tr>
 * <tr><td>sushirolls</td><td>32</td><td>64-192</td><td>damage, attack speed, max health</td></tr>
 * <tr><td>spaghettibolognese</td><td>30</td><td>60-180</td><td>damage, combat regen, max health</td></tr>
 * <tr><td>beefgoulash</td><td>30</td><td>60-180</td><td>+20% crit, attack speed</td></tr>
 * </table>
 *
 * <p>Stock counts are deliberately small — 10 and 5 rather than the Alchemist's
 * 50 — so the Doctor supplements a kitchen rather than replacing one. The five
 * dishes spoil (vanilla gives every gourmet meal {@code spoilDuration(120)}),
 * which is the other half of that: they are for the trip you are leaving on,
 * not for a chest.
 *
 * <h2>In the field</h2>
 *
 * A settled Doctor runs to any downed settler of his settlement and revives
 * them for free ({@link DoctorReviveAINode}), and while a fight is on he heals
 * the most hurt ally near him every three seconds ({@link #serverTick}).
 */
public class DoctorHumanMob extends HumanShop {

    public DoctorHumanMob() {
        // (health, speed, settlerStringID). The third argument is the
        // SettlerRegistry key, NOT the mob's own ID -- vanilla's Stylist passes
        // "stylist" while its mob is "stylisthuman"; ours is "doctor" against
        // mob "doctorhuman".
        super(500, 200, "doctor");
        this.attackCooldown = 500;
        this.attackAnimTime = 500;

        // --- potions: the Greater tier, i.e. the ones worth the trip --------
        this.shop.addSellingItem("superiorhealthpotion", new SellingShopItem(10, 2))
                .setStaticPriceBasedOnHappiness(20, 60, 10);
        this.shop.addSellingItem("greaterspeedpotion", new SellingShopItem(10, 2))
                .setStaticPriceBasedOnHappiness(40, 120, 20);
        this.shop.addSellingItem("greaterattackspeedpotion", new SellingShopItem(10, 2))
                .setStaticPriceBasedOnHappiness(40, 120, 20);
        this.shop.addSellingItem("greaterhealthregenpotion", new SellingShopItem(10, 2))
                .setStaticPriceBasedOnHappiness(40, 120, 20);
        this.shop.addSellingItem("greaterresistancepotion", new SellingShopItem(10, 2))
                .setStaticPriceBasedOnHappiness(60, 180, 30);
        this.shop.addSellingItem("greaterbattlepotion", new SellingShopItem(10, 2))
                .setStaticPriceBasedOnHappiness(100, 300, 50);

        // --- and the five dishes, which in this game are buffs with a plate --
        this.shop.addSellingItem("porktenderloin", new SellingShopItem(5, 1))
                .setStaticPriceBasedOnHappiness(70, 210, 35);
        this.shop.addSellingItem("deepfriedchicken", new SellingShopItem(5, 1))
                .setStaticPriceBasedOnHappiness(70, 210, 35);
        this.shop.addSellingItem("sushirolls", new SellingShopItem(5, 1))
                .setStaticPriceBasedOnHappiness(64, 192, 32);
        this.shop.addSellingItem("spaghettibolognese", new SellingShopItem(5, 1))
                .setStaticPriceBasedOnHappiness(60, 180, 30);
        this.shop.addSellingItem("beefgoulash", new SellingShopItem(5, 1))
                .setStaticPriceBasedOnHappiness(60, 180, 30);

        // --- what a doctor takes off your hands -----------------------------
        // The two alchemy reagents vanilla's own Alchemist buys, at its own
        // prices, so a player with a herb patch has the same outlet either way.
        this.shop.addBuyingItem("firemone", new BuyingShopItem())
                .setPriceBasedOnHappiness(12, 3, 3);
        this.shop.addBuyingItem("iceblossom", new BuyingShopItem())
                .setPriceBasedOnHappiness(12, 3, 3);

        // --- the cure in a bottle (Dorian concept, E5) -----------------------
        // 250 against his on-the-spot 100: it saves the walk, so it costs more,
        // which is what the concept asked for. Always on the shelf — the fever
        // is not the only reason to want one ready.
        this.shop.addSellingItem("bloodfevertincture", new SellingShopItem(5, 1))
                .setStaticPrice(250, 250);
    }

    // --- the field medic: revive the fallen, heal the fighting ---------------

    /** Ticks between two heals (20 ticks = one second). */
    public static final int HEAL_INTERVAL = 60;

    /** How far the heal reaches, in pixels (32 = one tile). */
    private static final int HEAL_RANGE = 10 * 32;

    /**
     * One heal: a tenth of the patient's max health, at least 20. Every three
     * seconds that is a little under a Health Potion (+50 at vanilla value 5)
     * per ten seconds on a 500-health settler — a medic, not a second potion
     * belt, and only while the fight lasts.
     */
    private static final float HEAL_FRACTION = 0.1F;
    private static final int HEAL_MIN = 20;

    private int healCooldown;

    /**
     * Vanilla's settler brain plus the revive node ({@link DoctorAI}).
     * {@code HumanMob.init} installs the plain {@code HumanAI}; this swaps it
     * right after, the same way {@code SkySettlerMob.installBrain} does.
     */
    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(this, new DoctorAI<>(320, true, false, 25000),
                new AIMover(HumanMob.humanPathIterations));
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (--this.healCooldown > 0) {
            return;
        }
        this.healCooldown = HEAL_INTERVAL;
        if (!this.isSettler() || this.isDowned()) {
            return;
        }
        Mob patient = this.findHurtAlly();
        if (patient != null) {
            int amount = Math.max(HEAL_MIN, (int) (patient.getMaxHealth() * HEAL_FRACTION));
            amount = Math.min(amount, patient.getMaxHealth() - patient.getHealth());
            // Vanilla's own Health Potion heals through this event, which is
            // what puts the green number over the patient on every client.
            this.getLevel().entityManager.events.add(new MobHealthChangeEvent(patient, amount));
        }
    }

    /**
     * The most hurt ally within {@link #HEAL_RANGE} who is in a fight — this
     * settlement's settlers and its players. Not "same team": a settlement
     * without a player team puts its settlers on vanilla's shared team -10,
     * which every other teamless settler and villager is on as well.
     */
    private Mob findHurtAlly() {
        ServerSettlementData data = this.getSettlerSettlementServerData();
        if (data == null || this.getLevel() == null) {
            return null;
        }
        int ours = this.getSettlementUniqueID();
        Stream<Mob> settlers = this.getLevel().entityManager.mobs
                .streamInRegionsInRange(this.x, this.y, HEAL_RANGE)
                .filter(m -> m instanceof HumanMob && ((HumanMob) m).getSettlementUniqueID() == ours);
        Stream<Mob> players = data.networkData.streamTeamMembers()
                .map(c -> (Mob) c.playerMob)
                .filter(p -> p != null && p.isSamePlace(this));
        return Stream.concat(settlers, players)
                .filter(m -> !m.removed() && m.getHealth() > 0 && m.getHealth() < m.getMaxHealth())
                .filter(m -> m.isInCombat() || this.isInCombat())
                .filter(m -> this.getDistance(m) <= HEAL_RANGE)
                .min(Comparator.comparingDouble(m -> m.getHealth() / (double) m.getMaxHealth()))
                .orElse(null);
    }

    /**
     * Vanilla's own recruit page takes this; never hand-roll a payment in
     * {@code interact()}. Priced beside the Therapist: both are professions a
     * settlement wants early, and neither is a boss reward.
     */
    @Override
    public List<InventoryItem> getRecruitItems(ServerClient client) {
        GameRandom random = new GameRandom((long) this.getSettlerSeed() * 367L);
        return Collections.singletonList(
                new InventoryItem("coin", random.getIntBetween(400, 700)));
    }

    @Override
    protected ArrayList<GameMessage> getMessages(ServerClient client) {
        // Six and seven are about the Nightbound (concept 3.5): a player hears
        // of the cure before the day they need it.
        return getLocalMessages("mobmsg", "doctortalk", 7);
    }

    /**
     * The one extra line in the talk menu, beside vanilla's own.
     *
     * <p>{@code clientHasAccess} is vanilla's "this player owns the settlement".
     * A Doctor who has not moved in yet has no practice to treat you in, and a
     * visiting player is not a patient — the same gate the Therapist's two
     * services use.
     */
    @Override
    public ArrayList<SettlerDialogue> getDialogues(ServerClient client, ServerSettlementData data,
                                                   boolean clientHasAccess) {
        ArrayList<SettlerDialogue> out = super.getDialogues(client, data, clientHasAccess);
        if (clientHasAccess && this.isSettler()) {
            out.add(new DoctorHealDialogue(this, SkyDoctor.HEAL_PRICE));
        }
        return out;
    }
}
