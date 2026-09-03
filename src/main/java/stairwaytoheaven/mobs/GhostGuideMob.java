package stairwaytoheaven.mobs;

import java.awt.Color;
import java.util.ArrayList;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketMobChat;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.HumanAI;
import necesse.entity.mobs.ai.behaviourTree.util.AIMover;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanShop.BuyingShopItem;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.gfx.HumanGender;
import necesse.gfx.HumanLook;
import necesse.gfx.GameHair;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.placeableItem.consumableItem.food.FoodConsumableItem;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.CachedSettlementData;
import necesse.level.maps.levelData.settlementData.settler.Settler;
import stairwaytoheaven.veil.VeilWorldData;

/**
 * The Ghost Guide — what answers the Séance Circle.
 *
 * <p>{@code docs/FOGKEY_AND_BOSSPORTALS.md} A3, in full:
 *
 * <ol>
 *   <li><b>First use — he unlocks you.</b> From then on the Ghost band's Soul
 *       Exposure does not apply to that player.</li>
 *   <li><b>Every use after — he trades.</b> He sells ghost weapons and he does
 *       not take coins: he takes Ghost-region valuables or high-quality cooked
 *       food the player made.</li>
 * </ol>
 *
 * <h2>1. The unlock is the Veil Mark, and it already existed</h2>
 *
 * "The Ghost band's Soul Exposure does not apply to that player" is exactly
 * what {@code VeilWorldData.hasMark} already answers, once a second, for every
 * player in the fog. So the first conversation calls
 * {@link VeilWorldData#grantMark} and nothing else — no second store, no second
 * code path, and the {@code /veilmark} admin command keeps working as the way
 * to test the gate from both sides.
 *
 * <h2>2. A coinless shop is NOT expressible in vanilla's shop API</h2>
 *
 * <b>VERIFIED [jar].</b> {@code HumanShop}'s shop is a {@code ShopManager} of
 * {@code SellingShopItem}s whose price is an {@code IntRange} of coins, and the
 * currency is not a parameter anywhere in the chain — it is hard-coded twice,
 * in the two methods that decide whether a trade may happen and then take the
 * payment:
 *
 * <pre>
 *   NetworkSellingShopItem.canAffordCost  (:82)  Item i = ItemRegistry.getItem("coin");
 *   NetworkSellingShopItem.consumeCost    (:108) Item i = ItemRegistry.getItem("coin");
 * </pre>
 *
 * There is no hook between {@code addSellingItem} and those two lines, so a
 * shop row that costs ectoplasm cannot be built out of the shop API. Making one
 * would mean replacing {@code ShopContainer} — a whole container, its packets
 * and its UI — for four trades.
 *
 * <p>So the honest alternative is the one the mod already uses twice for
 * exactly this shape of exchange: <b>hold the payment in the selected slot and
 * talk</b>. {@code SpireCatMob.interact} coaxes a cat home with a Cloudpuff
 * Treat that way and {@code EleanorMob.interact} takes twelve Veil Essence that
 * way, and both were written because holding a thing in your hand is what makes
 * a trade a choice rather than an inventory audit. His wares are printed into
 * chat whenever he is talked to with nothing he wants in hand, so the price
 * list is never hidden.
 *
 * <h2>He is summoned, not hired</h2>
 *
 * The settler key is {@code null}, which is vanilla's own way of writing a
 * {@code HumanShop} that is not a settler — {@code FriendlyWitchHumanMob}
 * passes {@code null} for the same reason (FriendlyWitchHumanMob.java:36,
 * VERIFIED [jar]). With no settler behind him {@code getRecruitItems} stays
 * {@code null} and the recruit page has nothing to charge, and
 * {@link #isValidRecruitment} refuses outright so no settlement can list him.
 */
public class GhostGuideMob extends HumanShop {

    /**
     * The registry id. {@code SeanceCircleObject} summons him by it, so it lives
     * here rather than being spelled twice.
     */
    public static final String STRING_ID = "ghostguide";

    public GhostGuideMob() {
        // null settler key: vanilla's own shape for a HumanShop nobody can hire
        // (FriendlyWitchHumanMob.java:36). The two health numbers are the
        // non-settler / settler pair; SkySettlerMob uses 400/400 and he is not
        // meant to be tougher or softer than the mod's other talkers.
        super(400, 400, null);
        // Summoned by hand and expected to still be there tomorrow. A guide who
        // despawned between conversations would make the chalk a consumable
        // with a timer on it, which A2 does not ask for.
        this.canDespawn = false;

        // --- an ordinary coin shop, at endgame prices ------------------------
        //
        // He used to barter: hold spiritsteel or fine food, talk, and he swapped
        // it. That was built because vanilla's shop API cannot price a row in
        // anything but coins -- NetworkSellingShopItem.canAffordCost:82 and
        // consumeCost:108 both hard-code ItemRegistry.getItem("coin")
        // (VERIFIED [jar]). The player ruled on it: "Auf garkeinen fall! dann
        // lieber hohe münzbeträge und normaler shop." So: a normal shop.
        //
        // The loop the barter existed to close is kept, and closed the ordinary
        // way instead -- he BUYS what the Ghost Realm drops, well above broker,
        // so ectoplasm and essence still turn into ghost weapons. It just goes
        // through the player's purse on the way.
        //
        // Prices are anchored on Caspern, the mod's other Ghost-realm smith,
        // whose ceiling is nightsteelveil at 2600-4800 behind a Reaper kill.
        // These two are the realm's signature weapons at Spiritsteel tier
        // (chest 34 / enchant 2400, docs/BALANCE.md), so they sit far above it
        // and near the Warden's own 30 000 recruit -- the most expensive thing
        // in the mod. Stock 1, restock 1: one a day, not a rack.
        this.shop.addSellingItem("spiritsteelreaver", new SellingShopItem(1, 1))
                .setStaticPriceBasedOnHappiness(14000, 24000, 1600);
        this.shop.addSellingItem("gravewindbow", new SellingShopItem(1, 1))
                .setStaticPriceBasedOnHappiness(12000, 20000, 1400);
        // The materials, so neither weapon is locked behind a drop the player
        // never happened to find. Broker values: spiritsteelbar 55.0F,
        // veilessence 34.0F (GhostRealm.registerItems, SkyItems.register); the
        // 3x-20x band over broker is vanilla's own (FriendlyWitchHumanMob:94).
        this.shop.addSellingItem("spiritsteelbar", new SellingShopItem(20, 4))
                .setStaticPriceBasedOnHappiness(400, 900, 90);
        this.shop.addSellingItem("veilessence", new SellingShopItem(30, 6))
                .setStaticPriceBasedOnHappiness(250, 560, 55);

        // --- and he buys the realm's own drops -------------------------------
        this.shop.addBuyingItem("ectoplasm", new BuyingShopItem())
                .setPriceBasedOnHappiness(60, 38, 10);
        this.shop.addBuyingItem("veilessence", new BuyingShopItem())
                .setPriceBasedOnHappiness(170, 108, 24);
        this.shop.addBuyingItem("spiritsteelbar", new BuyingShopItem())
                .setPriceBasedOnHappiness(275, 175, 38);
    }

    /** The same brain every other talker in this mod uses: he mills about. */
    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(this, new HumanAI<>(320, true, false, 25000),
                new AIMover(HumanMob.humanPathIterations));
    }

    /** Summoned, never hired: no settlement may list him. */
    @Override
    public boolean isValidRecruitment(CachedSettlementData settlement, ServerClient client) {
        return false;
    }

    /** ...and the dialogue window says why, instead of showing a blank offer. */
    @Override
    public GameMessage getRecruitError(ServerClient client) {
        return new LocalMessage("misc", "ghostguidenohire");
    }

    @Override
    public boolean startInRecruitForm(ServerClient client) {
        return false;
    }

    /**
     * He is a guide, not a target. Nothing in the design lets a stray Fen
     * Wraith end a player's only route through the fog — the same reason
     * {@code SkySettlerMob.canTakeDamage} is false for every named resident.
     */
    @Override
    public boolean canTakeDamage() {
        return false;
    }

    /**
     * First conversation unlocks the fog. Every one after opens his shop.
     *
     * <p>The unlock is {@link VeilWorldData#grantMark}, which is already keyed
     * on {@code ServerClient.authentication} and therefore already per player —
     * no second store, and no multiplayer race over who reached the fog first.
     * {@code grantMark} answers true only when the Mark was actually new, so
     * that branch IS the first conversation and cannot fire twice.
     *
     * <p>Everything after it is vanilla's: {@code super.interact} opens the
     * dialogue and the shop window built in the constructor.
     */
    @Override
    public void interact(PlayerMob player) {
        if (!this.isServer() || !player.isServerClient()) {
            super.interact(player);
            return;
        }
        ServerClient client = player.getServerClient();
        VeilWorldData veil = VeilWorldData.get(client.getServer());
        if (veil != null && veil.grantMark(client.authentication)) {
            // ONE bubble, and it has to carry the whole unlock. It used to be a
            // bubble plus a chat line ("misc.ghostguideunlock2") explaining
            // that Soul Exposure no longer touches the player; with the chat
            // log gone that sentence was folded into "misc.ghostguideunlock1"
            // rather than sent as a second bubble, because ChatBubbleText.init
            // (ChatBubbleText.java:67-76, VERIFIED [jar]) removes the bubble a
            // mob already has and the second would simply have deleted the
            // first.
            this.bubble("ghostguideunlock1");
            return;
        }
        super.interact(player);
    }

    /** Speech bubble over his head, seen by everyone nearby. */
    private void bubble(String miscKey) {
        Level level = this.getLevel();
        if (level == null || level.getServer() == null) {
            return;
        }
        level.getServer().network.sendToClientsWithEntity(
                new PacketMobChat(this.getUniqueID(), new LocalMessage("misc", miscKey)), this);
    }

    // ------------------------------------------------------------------
    // his voice and his face
    // ------------------------------------------------------------------

    @Override
    protected ArrayList<GameMessage> getMessages(ServerClient client) {
        return getLocalMessages("misc", "ghostguidetalk", 4);
    }

    @Override
    public GameMessage getDialogueIntroMessage(ServerClient client) {
        return new LocalMessage("misc", "ghostguideintro");
    }

    /**
     * A fixed face, so the guide the player met yesterday is the same person
     * today. Same technique and same reason as {@code SkySettlerMob}: a seeded
     * {@code HumanLook} plus real vanilla clothing items, so he costs no new
     * art at all.
     */
    @Override
    public void randomizeLook(HumanLook look, HumanGender gender, GameRandom random) {
        GameRandom fixed = new GameRandom(0x6805700);
        HumanGender g = HumanGender.NEUTRAL;
        look.randomizeLook(fixed, true, g, true, true, true, true);
        look.setHairColor(GameHair.getRandomHairColorAtSpecificWeight(fixed, 200));
        look.setShirtColor(new Color(168, 190, 196));
        look.setShoesColor(new Color(96, 108, 116));
        this.gender = g;
    }

    /**
     * Borrowed wardrobe, no recolouring — the rule the mod's other residents
     * follow. {@code snowhood} / {@code snowcloak} / {@code clothboots} are the
     * palest hooded silhouette vanilla's wardrobe has, and they are the same
     * three items {@code EleanorMob} already wears, so the Ghost Realm's two
     * dead read as the same kind of person. The borrow is listed in
     * {@code docs/VANILLA_ASSET_MAP.md} §1.3b.
     */
    @Override
    public void setDefaultArmor(HumanDrawOptions drawOptions) {
        drawOptions.helmet(new InventoryItem("snowhood"));
        drawOptions.chestplate(new InventoryItem("snowcloak"));
        drawOptions.boots(new InventoryItem("clothboots"));
    }
}
