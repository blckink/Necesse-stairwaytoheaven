package stairwaytoheaven.mobs;

import java.awt.Color;
import java.util.function.Consumer;

import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.util.AIMover;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.JobFinder;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.gfx.HumanGender;
import necesse.gfx.HumanLook;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.NetworkSettlementData;

/**
 * Dorian, the Nightbound — the settlement's vampire.
 *
 * <p>He is the first settler in this repository whose DAY is inverted: he
 * sleeps while the sun is up and works, hunts and walks the settlement at
 * night. Necesse has no "nocturnal" flag for that; the schedule is four
 * separate decisions in vanilla, and this class (with {@link VampireAI} and
 * {@link VampireSleepAINode}) turns each of them around:
 *
 * <ol>
 * <li><b>Sleeping</b> — {@code HumanSleepAINode.shouldSleep} returns
 *     {@code isNight()}. {@link VampireSleepAINode} returns the opposite, and
 *     {@link VampireAI} swaps it into the behaviour tree in place of vanilla's
 *     node.</li>
 * <li><b>Working</b> — {@code HumanMob.findJob} (HumanMob.java:3353) returns
 *     {@code null} at night for every settler. {@link #findJob} turns that
 *     around: nothing during the day, normal settler work after dark. The
 *     night branch cannot call {@code super} (super IS the night lock), so it
 *     re-walks vanilla's own guards and then the body of
 *     {@code EntityJobWorker.findJob} — twenty lines, all public API.</li>
 * <li><b>Standing around</b> — {@code HumanAI.wandererAINode.hideInside} sends
 *     an idle settler indoors at night. {@link VampireAI} reverses the
 *     predicate, or he would spend his shift hiding in the house.</li>
 * <li><b>Hunger</b> — {@code tickHunger} only drains while the sun is up
 *     (HumanMob.java:2203), so an inverted settler would starve in his sleep
 *     and never while awake. {@link #tickHunger} replaces it with
 *     {@link #bloodThirst}: he does not eat, he drinks.</li>
 * </ol>
 *
 * <p><b>The player's group is vanilla's own exception, not ours.</b>
 * {@code findJob}'s night lock already lets an adventure-party member through
 * ({@code adventureParty.isInAdventureParty()}), so "recruit him into the
 * party and he keeps going" is the shape the game itself offers. It is
 * mirrored here for the day: in the party he works through daylight, and
 * {@link VampireSleepAINode#canSleep} refuses the mid-expedition nap that
 * would otherwise send him home to bed at noon. Note that party membership
 * does NOT set command orders — {@code hasCommandOrders()} is
 * guardPoint/followMob/attackMob only — which is why both places have to ask
 * about the party separately.
 *
 * <p><b>Sunlight</b> costs him his speed rather than his life. Every settler
 * of this mod is immortal ({@code SkySettlerMob.canTakeDamage} is false, so
 * no stray mob can kill a named character), which rules out a sunburn that
 * ticks damage. What daylight takes instead is the thing that makes him worth
 * having: out in the sun he moves like anybody else, at night he is half again
 * as fast. The planned Daywalk quest removes exactly that penalty.
 */
public class VampireSettlerMob extends SkySettlerMob {

    /** How fast he is in the dark, against a normal settler. */
    private static final float NIGHT_SPEED = 1.45F;

    /** Real seconds of being awake that one full blood meter covers. */
    private static final float SECONDS_PER_FULL_THIRST = 1200.0F;

    /** 0 = starving, 1 = fed. Saved with the mob. */
    private float bloodThirst = 1.0F;

    /**
     * Settler ticks left before an empty vampire helps himself to a neighbour.
     * {@link #tickHunger} runs on the 50ms settler tick, so 3600 is three
     * minutes of standing around with nothing to drink.
     */
    private static final int BITE_INTERVAL = 3600;

    /** How often a drained animal gets up again as a Blood Thrall. */
    private static final float TURN_CHANCE = 1.0F / 6.0F;

    /** How close a resident has to be to be bitten, in pixels. */
    private static final int BITE_RANGE = 320;

    private int biteCooldown = BITE_INTERVAL;

    public VampireSettlerMob() {
        super("vampiresettler");

        // He hunts — vanilla withholds `hunting` from every settler but its own
        // Hunter — and he will not farm or chop: a settler who does everything
        // has no character (the Guard's own move, GuardHumanMob.java:31-34).
        // NOTE: the vanilla hunting job is fenced to the settlement bounds by
        // getJobRestrictZone, so it is the settlement's own pest control. The
        // hunt OUTSIDE the walls is VampireHuntAINode, not this profession.
        enableProfession("hunting");
        refuseJob("farming");
        refuseJob("forestry");

        // The Blood Bowl he drinks from (concept E3): his to sell, since he is
        // the one who wants it filled. 4 000 coins — the price of a sarcophagus
        // at Mortimer's, the other thing a town buys to keep him.
        this.shop.addSellingItem("bloodbowl",
                new necesse.entity.mobs.friendly.human.humanShop.SellingShopItem(2, 1))
                .setStaticPriceBasedOnHappiness(3200, 4800, 400);
    }

    /**
     * The same brain every settler of this mod runs, with the night turned
     * around. {@code SkySettlerMob.init} installs the vanilla {@code HumanAI};
     * this replaces it wholesale rather than patching it afterwards, because
     * {@link VampireAI} does its swapping in its own constructor.
     */
    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(this,
                new VampireAI<>(320, this.attacksHostiles(), false, 25000),
                new AIMover(HumanMob.humanPathIterations));
    }

    // --- the clock -------------------------------------------------------

    /** Night, as the world sees it. False on a client with no world entity. */
    public boolean isNightTime() {
        return this.getWorldEntity() != null && this.getWorldEntity().isNight();
    }

    /**
     * Whether he is on his feet: after dark, or because the player is keeping
     * him up (party member / standing orders).
     */
    public boolean isOnDuty() {
        return this.isNightTime()
                || this.adventureParty.isInAdventureParty()
                || this.hasCommandOrders();
    }

    /** Out in daylight — the state that costs him his speed. */
    public boolean isSunlit() {
        Level level = this.getLevel();
        return level != null && !level.isCave && !this.isNightTime()
                && level.isOutside(this.getTileX(), this.getTileY());
    }

    /** Fast in the dark, ordinary in the sun. */
    @Override
    public float getSpeed() {
        float base = super.getSpeed();
        return this.isSunlit() ? base : base * NIGHT_SPEED;
    }

    // --- blood instead of bread ------------------------------------------

    /** 0..1. What the hunt fills and the night drains. */
    public float getBloodThirst() {
        return this.bloodThirst;
    }

    /** The hunt's payout, and the player's blood vial. */
    public void feed(float amount) {
        this.bloodThirst = Math.min(1.0F, this.bloodThirst + amount);
    }

    /**
     * Vanilla's hunger clock, repurposed.
     *
     * <p>{@code HumanMob.tickHunger} drains hunger only while the sun is up,
     * which for a settler who sleeps through the day would mean starving in
     * bed and never going hungry on shift. Rather than invert the same meter —
     * the settlement UI would then ask the player to cook for a vampire — the
     * whole method is replaced: hunger stays where it is, and the meter that
     * actually moves is {@link #bloodThirst}, drained while he is awake.
     *
     * <p>The cadence is vanilla's: this is called on the settler tick, and the
     * 50-per-call arithmetic is copied from {@code tickHunger} so a full meter
     * lasts {@link #SECONDS_PER_FULL_THIRST} of waking time.
     */
    @Override
    public void tickHunger() {
        if (!this.isServer() || !this.isSettler()) {
            return;
        }
        if (!this.isOnDuty()) {
            return;
        }
        this.bloodThirst = Math.max(0.0F,
                this.bloodThirst - (float) (50.0 / (1000.0 * (double) SECONDS_PER_FULL_THIRST)));

        this.rollNight();
        if (this.bloodThirst < BOWL_THRESHOLD
                && stairwaytoheaven.settlement.BloodBowlObject.drinkNear(this.getLevel(),
                        this.getTileX(), this.getTileY(), BOWL_REACH_TILES)) {
            // Concept E3: the bowl, before anyone's neck.
            this.feed(0.5F);
            this.bubble("vampirebowl");
        }
        if (this.bloodThirst > 0.0F) {
            this.biteCooldown = BITE_INTERVAL;
        } else if (--this.biteCooldown <= 0) {
            this.biteCooldown = BITE_INTERVAL;
            this.biteNeighbour();
        }
    }

    /**
     * An empty vampire nobody has fed takes what he needs from the next bed
     * along.
     *
     * <p>This is the whole cost of keeping him: hunt with him, hand him a blood
     * vial, or let the settlement pay for it. What the victim gets is
     * {@link BloodFeverBuff} — pale, slow, unwell for a day — and not a second
     * vampire: replacing a settled resident's mob would break the save of
     * somebody the player has already built a room for.
     */
    private void biteNeighbour() {
        Level level = this.getLevel();
        if (level == null) {
            return;
        }
        HumanMob victim = level.entityManager.mobs.streamInRegionsInRange(this.x, this.y, BITE_RANGE)
                .filter(m -> m != this && m.isHuman && !m.removed())
                .map(m -> (HumanMob) m)
                .filter(HumanMob::isSettler)
                .filter(m -> m.buffManager.getBuff(BloodFeverBuff.ID) == null)
                .findFirst()
                .orElse(null);
        if (victim == null) {
            return;
        }
        victim.buffManager.addBuff(new necesse.entity.mobs.buffs.ActiveBuff(
                BloodFeverBuff.ID, victim, BloodFeverBuff.durationMs(), this), true);
        this.feed(0.5F);
        this.bubble("vampirebite");
        // Concept 3.4 / E2: the town is told, in the settlement's own
        // notification list — never in chat. It clears itself when the fever
        // does (BloodFeverNotification.isStillValid).
        this.tonightBitten.add(victim.getSettlerName());
        if (victim.levelSettler != null && victim.levelSettler.data != null
                && victim instanceof necesse.level.maps.levelData.settlementData.settler.SettlerMob) {
            victim.levelSettler.data.networkData.notifications.submitNotification(
                    stairwaytoheaven.settlement.BloodFeverNotification.ID,
                    (necesse.level.maps.levelData.settlementData.settler.SettlerMob) victim,
                    necesse.level.maps.levelData.settlementData.notifications
                            .SettlementNotificationSeverity.WARNING);
        }
    }

    /**
     * He does not eat.
     *
     * <p>This is NOT what stops the hunger meter — {@code tickHunger} never
     * consults it (it is read by {@code SettlerDietData} and
     * {@code HungrySettlementNotification} only, VERIFIED [jar]). What it does
     * is keep him off the settlement's food list, so a kitchen full of bread
     * is not reported as failing to feed him.
     */
    @Override
    public boolean doesEatFood() {
        return false;
    }

    // --- work, but after dark --------------------------------------------

    /**
     * The night lock, turned around.
     *
     * <p>Day branch: he has no jobs at all unless the player is keeping him up,
     * in which case {@code super} is safe — vanilla's own lock is inactive
     * during the day and every other guard it applies still should.
     *
     * <p>Night branch: {@code super} would refuse before doing anything else,
     * so it is rebuilt here. Everything below the time check is vanilla's,
     * in vanilla's order (HumanMob.java:3353-3388 and
     * {@code EntityJobWorker.findJob}), so a vampire on strike, in a raid, in a
     * disbanding settlement or hiding behaves exactly like anybody else.
     */
    @Override
    public JobSequence findJob(boolean ignoreRecreationJobs, Consumer<JobFinder> finderMod) {
        if (!this.isNightTime()) {
            if (!this.adventureParty.isInAdventureParty() && !this.hasCommandOrders()) {
                return null;
            }
            return super.findJob(ignoreRecreationJobs, finderMod);
        }
        return this.findJobIgnoringNight(ignoreRecreationJobs, finderMod);
    }

    /** {@link #findJob}'s night branch: vanilla's method without its clock. */
    private JobSequence findJobIgnoringNight(boolean ignoreRecreationJobs,
            Consumer<JobFinder> finderMod) {
        if (this.objectUser != null) {
            return null;
        }
        NetworkSettlementData settlement = this.getSettlerSettlementNetworkData();
        if (settlement != null
                && (settlement.isRaidActive() || settlement.isDisbanding() || !settlement.hasOwner())) {
            return null;
        }
        if (this.isHiding || this.isVisitor()) {
            return null;
        }
        if (!this.adventureParty.isInAdventureParty() && !this.hasCommandOrders()
                && this.attemptStartStrike(true)) {
            return null;
        }

        // EntityJobWorker.findJob's own body, which `super` can no longer be
        // asked for: `EntityJobWorker.super` is illegal once HumanMob has
        // overridden the default (JLS 15.12.3).
        JobTypeHandler handler = this.getJobTypeHandler();
        long currentTime = this.getMobWorker().getTime();
        if (handler.isOnGlobalCooldown(currentTime)) {
            return null;
        }
        JobFinder jobFinder = new JobFinder(this);
        if (finderMod != null) {
            finderMod.accept(jobFinder);
        }
        FoundJob<?> first = jobFinder.findJob(ignoreRecreationJobs);
        if (handler.resetPrioritizeNextJobIfFound) {
            handler.prioritizeNextJobID = -1;
        }
        JobSequence foundJob = null;
        if (first != null) {
            first.startCooldown(currentTime);
            handler.lastPerformedJobID = first.job.prioritizeForSameJobAgain() ? first.job.getID() : -1;
            foundJob = first.getSequence();
        } else {
            handler.lastPerformedJobID = -1;
            handler.prioritizeNextJobID = -1;
        }

        if (foundJob == null && this.getWorkInventory().isFull()) {
            this.submitFullInventoryNotification();
        } else {
            this.removeFullInventoryNotification();
        }
        return foundJob;
    }

    // --- the drain --------------------------------------------------------

    /**
     * Take an animal. Called by {@link VampireHuntAINode} once he has walked up
     * to his prey.
     *
     * <p>The kill is not dealt as damage: he is an immortal story settler with
     * no weapon of his own, and a drained animal is meant to leave a different
     * body than a butchered one. The animal is removed and the loot is written
     * here — a blood vial always, and only sometimes the meat, because an
     * animal he has emptied is worth less at the table.
     */
    public void drain(Mob prey) {
        Level level = this.getLevel();
        if (level == null || prey == null || prey.removed()) {
            return;
        }
        this.feed(0.34F);
        this.tonightDrained++;
        GameRandom random = GameRandom.globalRandom;
        float preyX = prey.x;
        float preyY = prey.y;
        boolean turns = random.getChance(TURN_CHANCE);
        if (!turns) {
            dropAt(level, prey, new InventoryItem("bloodvial", 1));
            if (random.getChance(0.4F)) {
                // "rawmeat" is not an item in 1.3.2 -- vanilla's raw meats are
                // rawmutton, rawpork and rawchickenleg (ItemRegistry.java:2015-
                // 2035, grouped under the "anyrawmeat" global ingredient) -- and
                // new InventoryItem(String) passes ItemRegistry.getItem's null
                // straight into Objects.requireNonNull (InventoryItem.java:61),
                // so four drains in ten threw instead of dropping meat.
                dropAt(level, prey, new InventoryItem("rawmutton", 1));
            }
        }
        prey.remove();

        if (turns) {
            // It does not stay down. The thrall is hostile and carries the
            // loot an ordinary animal never does — see BloodThrallMob.
            Mob thrall = necesse.engine.registries.MobRegistry.getMob("bloodthrall", level);
            if (thrall != null) {
                this.tonightThralls++;
                level.entityManager.addMob(thrall, preyX, preyY);
                this.bubble("vampireturned");
                return;
            }
        }
        this.bubble("vampiredrained");
    }

    private static void dropAt(Level level, Mob at, InventoryItem item) {
        level.entityManager.pickups.add(new ItemPickupEntity(level, item, at.x, at.y, 0.0F, 0.0F));
    }

    // --- persistence ------------------------------------------------------

    @Override
    public void addSaveData(SaveData save) {
        super.addSaveData(save);
        save.addFloat("bloodthirst", this.bloodThirst);
        save.addBoolean("nightrolled", this.wasNight);
        save.addInt("tonightdrained", this.tonightDrained);
        save.addInt("tonightthralls", this.tonightThralls);
        save.addStringArray("tonightbitten", this.tonightBitten.toArray(new String[0]));
        save.addInt("lastdrained", this.lastDrained);
        save.addInt("lastthralls", this.lastThralls);
        save.addSafeString("lastbitten", this.lastBitten);
    }

    @Override
    public void applyLoadData(LoadData save) {
        super.applyLoadData(save);
        this.bloodThirst = save.getFloat("bloodthirst", this.bloodThirst, false);
        this.wasNight = save.getBoolean("nightrolled", false, false);
        this.tonightDrained = save.getInt("tonightdrained", 0, false);
        this.tonightThralls = save.getInt("tonightthralls", 0, false);
        this.tonightBitten.clear();
        for (String name : save.getStringArray("tonightbitten", new String[0], false)) {
            if (name != null && !name.isEmpty()) {
                this.tonightBitten.add(name);
            }
        }
        this.lastDrained = save.getInt("lastdrained", 0, false);
        this.lastThralls = save.getInt("lastthralls", 0, false);
        this.lastBitten = save.getSafeString("lastbitten", "", false);
    }

    // --- what he can tell you (concept 3.2) -------------------------------

    /** Below this he goes to a Blood Bowl rather than wait for empty. */
    private static final float BOWL_THRESHOLD = 0.25F;
    /** How far he walks for a bowl: his settlement, give or take. */
    private static final int BOWL_REACH_TILES = 40;

    private boolean wasNight;
    private int tonightDrained;
    private int tonightThralls;
    private final java.util.ArrayList<String> tonightBitten = new java.util.ArrayList<>();
    private int lastDrained;
    private int lastThralls;
    private String lastBitten = "";

    /**
     * At every dusk, "tonight" becomes "last night" and starts again from
     * zero — so the report he gives by day is the night just gone. Called
     * from the settler tick, which only runs while he is a settler.
     */
    private void rollNight() {
        boolean night = this.isNightTime();
        if (night && !this.wasNight) {
            this.lastDrained = this.tonightDrained;
            this.lastThralls = this.tonightThralls;
            this.lastBitten = String.join(", ", this.tonightBitten);
            this.tonightDrained = 0;
            this.tonightThralls = 0;
            this.tonightBitten.clear();
        }
        this.wasNight = night;
    }

    /** 0 sated, 1 thirsty, 2 very thirsty, 3 he will bite tonight. */
    public int thirstStage() {
        if (this.bloodThirst > 0.66F) return 0;
        if (this.bloodThirst > 0.33F) return 1;
        if (this.bloodThirst > 0.0F) return 2;
        return 3;
    }

    /** The night being reported: the last finished one, or tonight's so far by night. */
    public int lastNightDrained() {
        return this.isNightTime() ? this.tonightDrained : this.lastDrained;
    }

    public int lastNightThralls() {
        return this.isNightTime() ? this.tonightThralls : this.lastThralls;
    }

    public String lastNightBitten() {
        return this.isNightTime() ? String.join(", ", this.tonightBitten) : this.lastBitten;
    }

    /** His dialogue page: thirst, a vial, last night (concept 3.2). */
    @Override
    public java.util.ArrayList<necesse.level.maps.levelData.settlementData.settler.dialogues.SettlerDialogue>
            getDialogues(necesse.engine.network.server.ServerClient client,
                    necesse.level.maps.levelData.settlementData.ServerSettlementData data,
                    boolean clientHasAccess) {
        java.util.ArrayList<necesse.level.maps.levelData.settlementData.settler.dialogues.SettlerDialogue> out =
                super.getDialogues(client, data, clientHasAccess);
        if (clientHasAccess && this.isSettler()) {
            out.add(new stairwaytoheaven.settlement.DorianDialogue(this));
        }
        return out;
    }

    // --- the face ---------------------------------------------------------

    /**
     * Pale, dark-haired, in black. {@code setSkin(0)} is the palest entry in
     * vanilla's skin list, which is the whole of "blass" — no new art, and the
     * settlement screen shows him with vanilla's own vampire face
     * ({@code mobs/icons/vampire}, borrowed by literal path in
     * {@code SkySettlers}).
     */
    @Override
    public void randomizeLook(HumanLook look, HumanGender gender, GameRandom random) {
        super.randomizeLook(look, gender, random);
        look.setSkin(0);
    }

    @Override protected int lookSeed() { return 0x5A3B11; }
    @Override protected HumanGender gender() { return HumanGender.MALE; }
    @Override protected Color shirtColor() { return new Color(24, 18, 26); }
    @Override protected Color shoesColor() { return new Color(18, 14, 18); }

    /**
     * The outfit: vanilla's black cloak over dress shoes, no hat — three item
     * IDs the game already owns, drawn on the ordinary settler body the way the
     * Elder wears his. Zero new pixels.
     */
    @Override protected String[] wardrobe() {
        return new String[]{null, "thiefscloak", "dressshoes"};
    }

    @Override protected int recruitCost() { return 3000; }
    @Override protected String talkKey() { return "vampiretalk"; }
}
