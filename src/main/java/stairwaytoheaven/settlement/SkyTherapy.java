package stairwaytoheaven.settlement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import necesse.engine.network.server.Server;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.PacketRegistry;
import necesse.engine.registries.SettlerDialogueRegistry;
import necesse.engine.registries.SettlerPersonalityRegistry;
import necesse.engine.registries.SettlerRegistry;
import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.settler.ActiveSettlerThoughtsManager;
import necesse.level.maps.levelData.settlementData.settler.personalities.SettlerPersonality;

/**
 * The Therapist — a new settler profession, and the two things one does.
 *
 * <h2>What a "profession" is here, and why this is one</h2>
 *
 * Vanilla's eighteen professions (stylist, miner, angler, …) are each exactly
 * two registrations: a {@link necesse.entity.mobs.friendly.human.humanShop.HumanShop}
 * subclass under {@code MobRegistry} and a
 * {@link necesse.level.maps.levelData.settlementData.settler.Settler} under
 * {@code SettlerRegistry} whose {@code mobStringID} names it. The settler type
 * puts tickets into the settlement's recruit draw
 * ({@code Settler.addNewRecruitSettler}), and that draw is the whole of
 * "turns up among all the others". {@link TherapistSettler} does the same with
 * the same weight vanilla gives the Blacksmith and the Miner (75) and, like
 * them, behind no story gate — so a Therapist is sprinkled through a world at
 * the same rate as the professions the player already knows.
 *
 * <h2>The two services, and where they live</h2>
 *
 * Both hang off the settler dialogue — the same menu that carries "show
 * equipment" — added in {@code TherapistHumanMob.getDialogues}:
 *
 * <ul>
 * <li>{@link TherapySlotsDialogue} — four slots, each holding one settler of
 *     the therapist's own settlement. While assigned, that settler carries the
 *     {@link TherapyThought} below.</li>
 * <li>{@link TraitTherapyDialogue} — pick any settler of the settlement and
 *     one trait they have; for 50 000 coins it is permanently traded for a
 *     random other one. The player does not get to choose what comes back.</li>
 * </ul>
 *
 * <h2>Why the trait swap sticks</h2>
 *
 * A settler's personalities look seed-derived — {@code HumanMob.setupPersonalities}
 * builds them from {@code settlerSeed} — but that is only the DEFAULT. Read out
 * of the jar (1.3.3, VERIFIED):
 *
 * <ul>
 * <li>{@code HumanMob.addSaveData} writes the list out by stringID, and
 *     {@code applyLoadData} reads it back AFTER {@code setSettlerSeed} has
 *     regenerated it — so the saved list wins on load;</li>
 * <li>{@code setupSpawnPacket}/{@code applySpawnPacket} carry the list
 *     explicitly (count + IDs), so what the server holds is what a client
 *     shows.</li>
 * </ul>
 *
 * Changing {@code mob.getPersonalities()} on the server is therefore permanent
 * and authoritative all by itself. The only gap is a client that already has
 * the mob loaded when the swap happens, and {@link PacketSettlerPersonalities}
 * closes exactly that gap. No method patching, no shadow bookkeeping, nothing
 * that a save without this mod would trip over.
 */
public final class SkyTherapy {

    private SkyTherapy() {
    }

    /** Mob string ID. Vanilla's own professions read {@code <job>human}. */
    public static final String MOB_ID = "therapisthuman";

    /** Settler string ID. Vanilla's own professions read {@code <job>}. */
    public static final String SETTLER_ID = "therapist";

    /** The mood line an assigned settler carries. */
    public static final String THOUGHT_ID = "swh_therapy";

    /** Therapy places. The player's number, and the reason for four slots. */
    public static final int SLOTS = 4;

    /** Coins per trait swap. */
    public static final int TRAIT_SWAP_PRICE = 50000;

    /**
     * By how much therapy lifts a patient's mood, in percent of what they feel
     * WITHOUT it.
     *
     * <p>Necesse's happiness is a signed sum of thought modifiers, so "+50%"
     * has to be read before it can be implemented. It is read here as <b>half
     * of the distance to neutral, always upwards</b>: a settler at +40 goes to
     * +60 (= 1.5x, the obvious reading) and a settler at -20 goes to -10 (half
     * the misery, rather than 1.5x -20 = -30, which would make a therapist
     * something you inflict on people). See {@code docs/settlers.md}.
     */
    public static final int BONUS_PERCENT = 50;

    /** The registered thought instance, held so the therapist can add it. */
    public static TherapyThought THOUGHT;

    /**
     * Every registry this feature touches, in one call.
     *
     * <p>All of these close after {@code init()}, so this is called from
     * {@code StairwayToHeavenMod.init}. IDs are written as literals in the
     * registration calls — {@code tools/locale_audit.py} follows literals, and
     * an ID handed over as a constant is an ID the audit cannot name-check.
     */
    public static void register() {
        MobRegistry.registerMob("therapisthuman", stairwaytoheaven.mobs.TherapistHumanMob.class, false);
        SettlerRegistry.registerSettler("therapist", new TherapistSettler());
        THOUGHT = new TherapyThought();
        SettlerThoughtRegistry.registerSettlerThought("swh_therapy", THOUGHT);
        SettlerDialogueRegistry.registerSettlerDialogue("swh_therapyslots", TherapySlotsDialogue.class);
        SettlerDialogueRegistry.registerSettlerDialogue("swh_traittherapy", TraitTherapyDialogue.class);
        PacketRegistry.registerPacket(PacketSettlerPersonalities.class);
        PacketRegistry.registerPacket(PacketTraitSwapResult.class);
    }

    // --- the mood bonus -------------------------------------------------

    /**
     * The thought an assigned patient carries.
     *
     * <p>Modelled on vanilla's own settler-lifts-settler effect,
     * {@code InspiringSettlerPersonality} + {@code InspiringHappinessPersonalityThought}:
     * a registered thought whose modifier is computed rather than constant, put
     * on the OTHER mob's {@link ActiveSettlerThoughtsManager}. The number
     * itself is worked out by the therapist (it depends on the patient's other
     * thoughts, which this class must not recurse into) and parked in
     * {@link #BONUS}; this class only reads it.
     *
     * <p>{@code shouldClearIfDoesntHavePersonalitySubmitter} is false because
     * this thought has no submitting personality — with the default true,
     * {@code ActiveSettlerThoughtsManager.init} would drop it on world load.
     */
    public static class TherapyThought
            extends necesse.level.maps.levelData.settlementData.settler.thoughts.SettlerThought {

        /**
         * Patient -> current bonus. Weak keys: a settler that despawns or dies
         * must not be held alive by a bookkeeping map, and a stale entry is
         * worthless anyway — the therapist recomputes it every couple of
         * seconds.
         */
        private static final Map<HumanMob, Integer> BONUS =
                Collections.synchronizedMap(new WeakHashMap<>());

        public static void setBonus(HumanMob patient, int bonus) {
            BONUS.put(patient, bonus);
        }

        public static int getBonus(HumanMob patient) {
            Integer value = BONUS.get(patient);
            return value == null ? 0 : value;
        }

        public static void clearBonus(HumanMob patient) {
            BONUS.remove(patient);
        }

        @Override
        protected necesse.engine.localization.message.GameMessage getDescription(HumanMob mob) {
            return new necesse.engine.localization.message.LocalMessage("misc", "swhtherapythought");
        }

        @Override
        protected int getHappinessModifier(HumanMob mob) {
            return getBonus(mob);
        }

        /** One stack, and the bonus does not scale with it. */
        @Override
        public int getMaxStacks(HumanMob mob) {
            return 1;
        }

        @Override
        public int getStackedHappinessModifier(HumanMob mob, int stacks) {
            return getBonus(mob);
        }

        @Override
        public boolean shouldClearIfDoesntHavePersonalitySubmitter() {
            return false;
        }
    }

    /**
     * Put (or refresh) therapy on one patient.
     *
     * <p>The bonus is {@link #BONUS_PERCENT} of the patient's happiness
     * WITHOUT therapy, which is their current happiness minus the bonus they
     * are already carrying. That subtraction is what stops the effect from
     * feeding on itself: without it every refresh would add 50% of a number
     * that already contained the last 50%.
     *
     * <p>The thought is removed and re-added rather than left to run: that is
     * the only public call that marks {@code ActiveSettlerThoughtsManager}'s
     * happiness cache dirty, so a changed bonus is a changed mood in the same
     * tick.
     */
    public static void applyTherapy(HumanMob patient) {
        int carried = TherapyThought.getBonus(patient);
        int withoutTherapy = patient.getSettlerHappiness() - carried;
        int bonus = Math.round(Math.abs(withoutTherapy) * (BONUS_PERCENT / 100.0F));
        TherapyThought.setBonus(patient, bonus);
        ActiveSettlerThoughtsManager thoughts = patient.getActiveThoughts();
        thoughts.removeThought(THOUGHT);
        thoughts.addThoughtSeconds(THOUGHT, 60.0F);
        patient.updateHappiness();
    }

    /** Take therapy off a patient — unassigned, moved out, or dead therapist. */
    public static void clearTherapy(HumanMob patient) {
        TherapyThought.clearBonus(patient);
        patient.getActiveThoughts().removeThought(THOUGHT);
        patient.updateHappiness();
    }

    // --- the trait swap -------------------------------------------------

    /**
     * Roll the replacement for one trait, the way the game itself would.
     *
     * <p>A random personality is not enough: vanilla's draw enforces three
     * things this has to honour, or the therapist hands out traits that make no
     * sense on that settler.
     *
     * <ol>
     * <li>a per-personality FILTER — "elder" only ever sits on the Elder,
     *     "voyager" only on the four settlers who travel, "gardener" never on a
     *     Guard;</li>
     * <li>MUTUAL EXCLUSIONS — warrior/ranger/magician/summoner are one pick
     *     between four, and pacifist refuses all of them plus bloodthirsty;</li>
     * <li>the split between ordinary traits and BONUS PERKS, which vanilla
     *     draws from two separate pools.</li>
     * </ol>
     *
     * <p>Every one of those rules lives on {@code SettlerPersonalityFilter}
     * objects held by {@code SettlerPersonalityRegistry}, and the registry
     * hands them out through {@code getElements()}, which is <b>protected</b> —
     * a mod cannot read them, and copying the rules into this file would leave
     * a second source of truth to drift.
     *
     * <p>So this asks the game instead. {@code getNewRandomSettlerPersonalities}
     * with a count larger than the registry drains both ticket pools and
     * returns a MAXIMAL legal set for this very mob: every filter satisfied,
     * every exclusion resolved. A draw that happens to contain all the traits
     * the settler is KEEPING certifies, by construction, that anything else in
     * that same draw can stand beside them. Repeat until such a draw turns up
     * (it takes a handful of tries — the pruning only ever removes members of
     * the few exclusive groups) and pick at random from what it offers.
     *
     * @param mob     the settler being treated
     * @param replace the trait being given up
     * @return the replacement, already constructed for {@code mob}, or null if
     *         the game offers nothing legal (no trait is then taken away)
     */
    public static SettlerPersonality rollReplacement(HumanMob mob, SettlerPersonality replace,
                                                     GameRandom random) {
        Set<String> current = new HashSet<>();
        Set<String> kept = new HashSet<>();
        for (SettlerPersonality personality : mob.getPersonalities()) {
            current.add(personality.getStringID());
            if (personality.getID() != replace.getID()) {
                kept.add(personality.getStringID());
            }
        }
        boolean wantBonusPerk = SettlerPersonalityRegistry.isBonusPerk(replace.getID());

        for (int attempt = 0; attempt < 60; attempt++) {
            ArrayList<SettlerPersonality> draw = SettlerPersonalityRegistry
                    .getNewRandomSettlerPersonalities(mob, random, Integer.MAX_VALUE, Integer.MAX_VALUE);
            if (draw == null || draw.isEmpty()) {
                return null;
            }
            Set<String> drawn = new HashSet<>();
            List<SettlerPersonality> candidates = new ArrayList<>();
            for (SettlerPersonality personality : draw) {
                drawn.add(personality.getStringID());
                if (current.contains(personality.getStringID())) {
                    continue;
                }
                if (SettlerPersonalityRegistry.isBonusPerk(personality.getID()) != wantBonusPerk) {
                    continue;
                }
                candidates.add(personality);
            }
            // Only a draw that also carries every kept trait proves the
            // candidates can live beside them.
            if (!drawn.containsAll(kept) || candidates.isEmpty()) {
                continue;
            }
            return candidates.get(random.nextInt(candidates.size()));
        }
        return null;
    }

    /**
     * Swap one trait for another, permanently, and tell every client that can
     * see the settler.
     *
     * <p>The replacement goes in at the SAME INDEX the old one had. That is not
     * cosmetic: {@code HumanMob.addSaveData} writes each personality's own save
     * block in list order and {@code applyLoadData} reads them back in list
     * order, so an append-and-remove would hand the next personality along the
     * previous one's saved data.
     *
     * @return true if the swap happened
     */
    public static boolean swapPersonality(HumanMob mob, SettlerPersonality replace,
                                          SettlerPersonality replacement) {
        ArrayList<SettlerPersonality> personalities = mob.getPersonalities();
        for (int i = 0; i < personalities.size(); i++) {
            if (personalities.get(i).getID() != replace.getID()) {
                continue;
            }
            personalities.set(i, replacement);
            replacement.init();
            // The mood lines the old trait submitted have to go with it, or the
            // settler keeps a thought nothing produces any more.
            mob.getActiveThoughts().init();
            mob.updateHappiness();
            Server server = mob.getLevel() == null ? null : mob.getLevel().getServer();
            if (server != null) {
                server.network.sendToClientsWithEntity(new PacketSettlerPersonalities(mob), mob);
            }
            return true;
        }
        return false;
    }
}
