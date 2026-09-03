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
     * The registry ID, so the Séance Circle cannot mistype it.
     *
     * <p>It is NOT what {@code SkyMobs.register} passes to
     * {@code MobRegistry.registerMob} — that line spells the string out,
     * because {@code tools/locale_audit.py} matches the registration with a
     * regex and an ID arriving through a constant would register a mob whose
     * two locale keys nothing checks. The two must agree, and the registration
     * says so at its own call site.
     */
    public static final String STRING_ID = "ghostguide";

    /** Per-player cooldown key and length for the printed price list. */
    private static final String QUOTE_COOLDOWN = "swhghostguidequote";
    private static final long QUOTE_COOLDOWN_MS = 60_000L;

    /**
     * What he will take, and what he gives back.
     *
     * <h3>Where the numbers come from</h3>
     * Every row is priced in broker value, the same currency the mod's own
     * recipes are checked in ({@code SkyArsenal.registerItems} explains the
     * method): {@code spiritsteelbar} 55.0F and {@code veilessence} 34.0F are
     * this mod's ({@code GhostRealm.registerItems}, {@code SkyItems.register}),
     * {@code ectoplasm} is vanilla's at 12.0F, and vanilla's own FOOD_FINE
     * dishes run 5.0F-14.0F (ItemRegistry.java:1427-1435 — steak 5, roastedpork
     * 12, roastedrabbitleg 14, VERIFIED [jar]). Each trade hands the player
     * back a little LESS than they paid, which is what a shop is:
     *
     * <pre>
     *   8 spiritsteelbar   440  ->  Spiritsteel Reaver  430
     *  12 veilessence      408  ->  Gravewind Bow       400
     *  20 ectoplasm        240  ->  4 spiritsteelbar    220
     *   8 fine dishes   40-112  ->  2 veilessence        68
     * </pre>
     *
     * The two material rows exist so that neither weapon is behind a resource
     * the player might not have found: raw ectoplasm buys the bars the sword
     * costs, and a cook who never fights buys the essence the bow costs.
     */
    private static final Trade[] TRADES = {
            new Trade("spiritsteelbar", 8, "spiritsteelreaver", 1),
            new Trade("veilessence", 12, "gravewindbow", 1),
            new Trade("ectoplasm", 20, "spiritsteelbar", 4),
            new Trade(null, 8, "veilessence", 2),
    };

    /** One row of the price list. A null {@link #payID} means "any fine dish". */
    private static final class Trade {
        final String payID;
        final int payCount;
        final String giveID;
        final int giveCount;

        Trade(String payID, int payCount, String giveID, int giveCount) {
            this.payID = payID;
            this.payCount = payCount;
            this.giveID = giveID;
            this.giveCount = giveCount;
        }

        /** The name shown in the price list. */
        GameMessage payName() {
            // Vanilla's own "Fine food" string, so the food row needs no key of
            // ours: FoodQuality.displayName is what the settlement screens
            // already print (Settler.java:51, VERIFIED [jar]).
            return this.payID == null
                    ? Settler.FOOD_FINE.displayName
                    : ItemRegistry.getLocalization(this.payID);
        }

        /** Does the item the player is holding pay for this row? */
        boolean accepts(InventoryItem held) {
            if (held == null || held.item == null) {
                return false;
            }
            if (this.payID != null) {
                return this.payID.equals(held.item.getStringID());
            }
            // FOOD_FINE is a FoodQuality, not an item list, so the test is the
            // one field vanilla itself keys the quality off:
            // FoodConsumableItem.quality (FoodConsumableItem.java:44).
            return held.item instanceof FoodConsumableItem
                    && ((FoodConsumableItem) held.item).quality == Settler.FOOD_FINE;
        }
    }

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
     * The fork: the unlock once, the trade every time after.
     *
     * <p>Everything below is server-side and per client, which is the whole
     * multiplayer answer — two players talking to the same guide each get their
     * own first conversation, because the ledger is keyed on
     * {@code ServerClient.authentication} and not on this mob.
     */
    @Override
    public void interact(PlayerMob player) {
        if (!this.isServer() || !player.isServerClient()) {
            super.interact(player);
            return;
        }
        ServerClient client = player.getServerClient();
        Server server = client.getServer();
        VeilWorldData veil = VeilWorldData.get(server);

        // --- 1. THE UNLOCK -------------------------------------------------
        if (veil != null && veil.grantMark(client.authentication)) {
            // grantMark returns true only when the Mark was actually new, so
            // this branch is the FIRST conversation and nothing else.
            this.bubble("ghostguideunlock1");
            client.sendChatMessage(new LocalMessage("misc", "ghostguideunlock2"));
            return;
        }

        // --- 2. THE TRADE --------------------------------------------------
        InventoryItem held = player.getSelectedItem();
        for (Trade trade : TRADES) {
            if (!trade.accepts(held)) {
                continue;
            }
            if (this.settle(client, player, trade)) {
                // Traded. The dialogue window is deliberately NOT opened on top
                // of it -- the exchange was the conversation.
                return;
            }
            // Right kind of thing, not enough of it. Say the shortfall, then
            // fall through to the small talk so the player is not locked out of
            // his window by holding three ectoplasm.
            client.sendChatMessage(new LocalMessage("misc", "ghostguideshort",
                    "count", String.valueOf(trade.payCount),
                    "pay", trade.payName()));
            break;
        }

        // Nothing he wants in hand (or not enough of it): print the list, then
        // let vanilla's own dialogue window open on his small talk.
        this.quote(client, player);
        super.interact(player);
    }

    /**
     * Takes the payment and hands over the goods, or answers false when the
     * player is short.
     *
     * <p>The payment is counted and removed across the whole main inventory
     * rather than out of the held stack alone — holding one Veil Essence and
     * owning twelve is still twelve — which is the same shape
     * {@code EleanorMob.interact} uses for her pass-on.
     */
    private boolean settle(ServerClient client, PlayerMob player, Trade trade) {
        Level level = player.getLevel();
        if (trade.payID != null) {
            Item pay = ItemRegistry.getItem(trade.payID);
            if (player.getInv().main.getAmount(level, player, pay, "ghostguide") < trade.payCount) {
                return false;
            }
            player.getInv().main.removeItems(level, player, pay, trade.payCount, "ghostguide");
        } else {
            // The food row is a CATEGORY, so it is counted and removed dish by
            // dish: a player may be paying with three different roasts.
            if (countFineFood(player) < trade.payCount) {
                return false;
            }
            if (!removeFineFood(player, level, trade.payCount)) {
                return false;
            }
        }
        this.bubble("ghostguidetrade");
        give(client, trade.giveID, trade.giveCount);
        return true;
    }

    /** How many FOOD_FINE dishes the player owns, across the main inventory. */
    private static int countFineFood(PlayerMob player) {
        int total = 0;
        for (int slot = 0; slot < player.getInv().main.getSize(); slot++) {
            InventoryItem item = player.getInv().main.getItem(slot);
            if (isFineFood(item)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    /** The item TYPE of the first fine dish in the inventory, or null. */
    private static Item firstFineFoodType(PlayerMob player) {
        for (int slot = 0; slot < player.getInv().main.getSize(); slot++) {
            InventoryItem item = player.getInv().main.getItem(slot);
            if (isFineFood(item)) {
                return item.item;
            }
        }
        return null;
    }

    /**
     * Removes {@code count} dishes, of whatever kinds the player happens to be
     * carrying.
     *
     * <p>It works one TYPE at a time rather than one slot at a time, and that
     * is deliberate. {@code Inventory.removeItems(item, amount)} takes the
     * amount from wherever in the inventory it finds it, so a loop that walked
     * slots and assumed its own walk survived each removal could empty a slot
     * it had already counted, run out of slots with dishes still owed, and
     * return false having ALREADY taken the player's food. Asking for a whole
     * type at a time and subtracting what the inventory says it actually
     * removed cannot drift; each pass either finishes the bill or removes every
     * dish of one kind, so the number of passes is bounded by the number of
     * kinds, and the loop bound is a belt on top of that brace.
     */
    private static boolean removeFineFood(PlayerMob player, Level level, int count) {
        if (countFineFood(player) < count) {
            return false;
        }
        int remaining = count;
        for (int pass = 0; pass < player.getInv().main.getSize() && remaining > 0; pass++) {
            Item type = firstFineFoodType(player);
            if (type == null) {
                break;
            }
            int removed = player.getInv().main.removeItems(level, player, type, remaining, "ghostguide");
            if (removed <= 0) {
                break;
            }
            remaining -= removed;
        }
        return remaining <= 0;
    }

    private static boolean isFineFood(InventoryItem item) {
        return item != null && item.item instanceof FoodConsumableItem
                && ((FoodConsumableItem) item.item).quality == Settler.FOOD_FINE;
    }

    /**
     * Prints the whole price list into the player's chat, at most once a minute
     * per player.
     *
     * <p>The cooldown matters because the list is five lines and he is also an
     * ordinary talker: without it, two conversations in a row would bury the
     * rest of the chat. {@code Mob.startGenericCooldown} is the same per-player
     * timer {@code VeilWorldData} uses to stop the fog warning repeating, and it
     * lives on the PLAYER, so two players each get their own.
     */
    private void quote(ServerClient client, PlayerMob player) {
        if (player.isOnGenericCooldown(QUOTE_COOLDOWN)) {
            return;
        }
        player.startGenericCooldown(QUOTE_COOLDOWN, QUOTE_COOLDOWN_MS);
        client.sendChatMessage(new LocalMessage("misc", "ghostguideoffer"));
        for (Trade trade : TRADES) {
            client.sendChatMessage(new LocalMessage("misc", "ghostguidewares",
                    "count", String.valueOf(trade.payCount),
                    "pay", trade.payName(),
                    "amount", String.valueOf(trade.giveCount),
                    "get", ItemRegistry.getLocalization(trade.giveID)));
        }
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

    /** Hand-off; anything that does not fit drops at the player's feet. */
    private static void give(ServerClient client, String itemStringID, int amount) {
        PlayerMob player = client.playerMob;
        Level level = player.getLevel();
        InventoryItem item = new InventoryItem(itemStringID, amount);
        boolean added = player.getInv().main.addItem(level, player, item, "ghostguide", null);
        if (!added && item.getAmount() > 0) {
            level.entityManager.pickups.add(
                    new ItemPickupEntity(level, item, player.x, player.y, 0.0F, 0.0F));
        }
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
