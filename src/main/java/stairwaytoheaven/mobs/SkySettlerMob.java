package stairwaytoheaven.mobs;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.HumanAI;
import necesse.entity.mobs.ai.behaviourTree.util.AIMover;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.gfx.HumanGender;
import necesse.gfx.HumanLook;
import necesse.gfx.GameHair;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.inventory.InventoryItem;

/**
 * Shared base for the Skyreach's hireable residents.
 *
 * WHY THIS EXISTS AT ALL. Before this class the mod registered exactly ONE
 * settler — the Warden — and one HumanShop. The player's question was why a
 * settler with one of our professions had never turned up in their town, and
 * the answer was that none existed: the three "professions" are workstations
 * for vanilla settlers, not settler types, and the designed cast lived only in
 * `docs/design/chapter-01-skyreach-cast.md`.
 *
 * WHAT A PROFESSION IS, read out of the game rather than assumed. Necesse ships
 * eighteen of them through {@code SettlerRegistry} — angler, stylist, mage,
 * explorer, miner and the rest — and each one is a {@link HumanShop} subclass
 * whose identity is its SHOP: what it sells, what it buys, and prices that move
 * with its happiness. That is the whole mechanism, and it is what this base
 * gives its three subclasses.
 *
 * APPEARANCE COSTS NO NEW ART. Vanilla dresses a settler by putting real
 * clothing ITEMS on a plain human body — the Elder is a human in `elderhat`,
 * `eldershirt` and `eldershoes` — which `WardenIdentity` already follows. Each
 * resident here picks its wardrobe from vanilla's own, verified present in the
 * ItemRegistry, so three new people cost three 32px icons rather than three
 * human sheets.
 */
public abstract class SkySettlerMob extends HumanShop {

    protected SkySettlerMob(String settlerStringID) {
        // Third argument is the SettlerRegistry key, NOT a free-form name.
        // Getting it wrong makes getSettler() null and silently disables
        // recruitment — the bug that cost the Warden three player-visible
        // faults (see DESIGN_DECISIONS.md).
        super(400, 400, settlerStringID);
        this.canDespawn = false;
    }

    /** The Miner's brain: mills about, and works like any settler once hired. */
    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(this, new HumanAI<>(320, true, false, 25000),
                new AIMover(HumanMob.humanPathIterations));
    }

    // --- professions ----------------------------------------------------
    //
    // WHAT A PROFESSION IS IN THE JOB SYSTEM, read out of 1.3.2.
    //
    // JobTypeRegistry.registerCore (JobTypeRegistry.java:20-32) registers each
    // job type as `new JobType(canChangePriority, defaultDisabledBySettler,
    // name, tip)`. The SECOND flag is the whole profession mechanism:
    //
    //   hauling, crafting, forestry, farming   -> false: EVERY settler does them
    //   fertilize, husbandry, fishing, hunting,
    //   tradingmission                         -> true: nobody does them...
    //
    // ...unless their mob turns the flag off for itself. JobTypeHandler's
    // constructor copies `type.defaultDisabledBySettler` into a per-mob
    // TypePriority (JobTypeHandler.java:132-135), and JobTypeHandler.streamJobs
    // drops every job whose TypePriority is `disabledBySettler`
    // (JobTypeHandler.java:88-91). So vanilla's professions are literally one
    // line each in the mob's constructor:
    //
    //   FarmerHumanMob:       getPriority("fertilize").disabledBySettler = false
    //   AnimalKeeperHumanMob: getPriority("husbandry").disabledBySettler = false
    //   AnglerHumanMob:       getPriority("fishing").disabledBySettler   = false
    //   HunterHumanMob:       getPriority("hunting").disabledBySettler   = false
    //   TraderHumanMob:       getPriority("tradingmission")...           = false
    //
    // and the opposite direction is a profession too: GuardHumanMob switches
    // crafting, forestry and farming OFF (GuardHumanMob.java:31-34) because a
    // guard guards. That is the pair of verbs below.
    //
    // The job handlers these touch are created inside the HumanMob constructor
    // (`LevelJobRegistry.addHandlers`, HumanMob.java:514), so a subclass
    // constructor is the correct and only place to call them - which is exactly
    // where vanilla calls them. TypePriority.loadSaveData restores `priority`
    // and `disabledByPlayer` and never `disabledBySettler`
    // (JobTypeHandler.java:137-140), so what is set here survives every load.

    /**
     * Grant this settler a job vanilla withholds from settlers by default -
     * i.e. give them a specialist profession.
     *
     * @param jobType one of {@code fertilize}, {@code husbandry},
     *                {@code fishing}, {@code hunting}, {@code tradingmission}
     */
    protected final void enableProfession(String jobType) {
        JobTypeHandler.TypePriority priority = this.jobTypeHandler.getPriority(jobType);
        if (priority != null) {
            priority.disabledBySettler = false;
        }
    }

    /**
     * Take a job away that vanilla grants to every settler, because this
     * character would not do it. The Guard's own move.
     *
     * @param jobType one of {@code hauling}, {@code crafting}, {@code forestry},
     *                {@code farming}
     */
    protected final void refuseJob(String jobType) {
        JobTypeHandler.TypePriority priority = this.jobTypeHandler.getPriority(jobType);
        if (priority != null) {
            priority.disabledBySettler = true;
        }
    }

    /**
     * Speech bubble over their head, seen by everyone nearby.
     *
     * <p>This is the mod's ONLY way for a person to say something — the chat
     * log is gone (the player: <i>"und keine chat nachrichten! generell.. die
     * sind total kacke lesbar"</i>). Two rules come with it, both from
     * {@code ChatBubbleText} (VERIFIED [jar]):
     *
     * <ul>
     *   <li>{@code init} (ChatBubbleText.java:67-76) removes any bubble the
     *       same mob already has, so <b>only the last call in a method is
     *       ever seen</b>. Say one thing, or build one message.</li>
     *   <li>the text wraps at {@code maxWidth} 200px and honours {@code \n}
     *       (FairType.java:262), so a line that needs a break can have one —
     *       but a paragraph in a bubble is unreadable and belongs nowhere.</li>
     * </ul>
     */
    protected void bubble(String miscKey) {
        this.bubble(new LocalMessage("misc", miscKey));
    }

    /**
     * The same bubble, for a line that carries replacements — a count, a name,
     * a position. The message is sent unresolved so every player reads it in
     * their own language.
     */
    protected void bubble(necesse.engine.localization.message.GameMessage message) {
        if (message == null || this.getLevel() == null || this.getLevel().getServer() == null) {
            return;
        }
        this.getLevel().getServer().network.sendToClientsWithEntity(
                new necesse.engine.network.packet.PacketMobChat(
                        this.getUniqueID(), message), this);
    }

    /** A fixed face, so the same person is the same person in every world. */
    protected abstract int lookSeed();

    protected abstract HumanGender gender();

    protected abstract Color shirtColor();

    protected abstract Color shoesColor();

    /** helmet / chestplate / boots item string IDs, or null to leave bare. */
    protected abstract String[] wardrobe();

    /** Coins asked at vanilla's own recruit page. */
    protected abstract int recruitCost();

    /** The `misc.<key>N` prefix for this resident's small talk. */
    protected abstract String talkKey();

    @Override
    public void randomizeLook(HumanLook look, HumanGender gender, GameRandom random) {
        GameRandom fixed = new GameRandom(this.lookSeed());
        HumanGender g = this.gender();
        look.randomizeLook(fixed, true, g, true, true, true, true);
        look.setHairColor(GameHair.getRandomHairColorAtSpecificWeight(fixed, 140));
        look.setShirtColor(this.shirtColor());
        look.setShoesColor(this.shoesColor());
        this.gender = g;
    }

    @Override
    public void setDefaultArmor(HumanDrawOptions drawOptions) {
        String[] w = this.wardrobe();
        if (w == null) {
            return;
        }
        if (w.length > 0 && w[0] != null) {
            drawOptions.helmet(new InventoryItem(w[0]));
        }
        if (w.length > 1 && w[1] != null) {
            drawOptions.chestplate(new InventoryItem(w[1]));
        }
        if (w.length > 2 && w[2] != null) {
            drawOptions.boots(new InventoryItem(w[2]));
        }
    }

    /**
     * The price, stated by vanilla's own recruit page.
     *
     * This IS the payment mechanism: {@code ShopContainer.payForRecruit} takes
     * the coins server-side on the button press. Never hand-roll a payment in
     * {@code interact()} — that is exactly what produced the Warden's
     * "coins taken by talking" bug.
     */
    @Override
    public List<InventoryItem> getRecruitItems(ServerClient client) {
        return Collections.singletonList(new InventoryItem("coin", this.recruitCost()));
    }

    /** Open on the recruit page until they have actually moved in. */
    @Override
    public boolean startInRecruitForm(ServerClient client) {
        return !this.isSettler();
    }

    /**
     * Their own small talk. Without this override HumanMob falls back to
     * {@code mobmsg.humantalk1..5} and a Skywatch courier greets the player
     * with "I often think about the big questions in life" — which is what a
     * playtester screenshotted on the Warden.
     */
    @Override
    protected ArrayList<GameMessage> getMessages(ServerClient client) {
        return getLocalMessages("misc", this.talkKey(), 4);
    }

    @Override
    public GameMessage getDialogueIntroMessage(ServerClient client) {
        return this.isSettler() ? super.getDialogueIntroMessage(client)
                : new LocalMessage("misc", this.talkKey() + "pitch");
    }

    /** Unique story residents: no stray mob may kill one. */
    @Override
    public boolean canTakeDamage() {
        return false;
    }
}
