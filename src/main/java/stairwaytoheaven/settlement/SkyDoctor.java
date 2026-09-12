package stairwaytoheaven.settlement;

import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.SettlerDialogueRegistry;
import necesse.engine.registries.SettlerRegistry;

/**
 * The Doctor — the mod's second settler profession, and the proof that a third
 * is cheap.
 *
 * <h2>What it costs to add one now</h2>
 *
 * The Therapist needed seven classes because it invented the shape: what a
 * profession is, how a service hangs off the settler talk menu, how a dialogue
 * carries state across the network. The Doctor reuses all of that and needs
 * <b>four</b>:
 *
 * <ul>
 * <li>{@link DoctorSettler} — about twenty lines on {@link ProfessionSettler},
 *     which holds the recruit ticket, the COMPLETE_HOST opt-out, the icon and
 *     the clothes;</li>
 * <li>{@code mobs.DoctorHumanMob} — the shop, i.e. the profession's actual
 *     identity;</li>
 * <li>{@link DoctorHealDialogue} — its one service;</li>
 * <li>this class, plus one line in {@code StairwayToHeavenMod.init} and the
 *     locale rows in both languages.</li>
 * </ul>
 *
 * <p>No packet and no container event this time: the Therapist needed one
 * because a trait swap's outcome is invisible until something says it out loud.
 * Being healed is a health bar filling up.
 *
 * <h2>The two things a Doctor does</h2>
 *
 * <ul>
 * <li><b>Patches you up on the spot</b> for {@link #HEAL_PRICE} coins — see
 *     {@link DoctorHealDialogue}.</li>
 * <li><b>Sells the good consumables</b> — six Greater-tier potions and five
 *     gourmet dishes, all vanilla, all craftable by the player anyway, priced
 *     off each item's own registered broker value by vanilla's own Alchemist
 *     rule. The table and the arithmetic are in {@code mobs.DoctorHumanMob}.</li>
 * </ul>
 */
public final class SkyDoctor {

    private SkyDoctor() {
    }

    /** Mob string ID. Vanilla's own professions read {@code <job>human}. */
    public static final String MOB_ID = "doctorhuman";

    /** Settler string ID. Vanilla's own professions read {@code <job>}. */
    public static final String SETTLER_ID = "doctor";

    /**
     * Coins for a full heal.
     *
     * <p>Deliberately small. A Superior Health Potion off this same Doctor's
     * shelf costs 20-60 and does not heal you fully, so the visit is worth
     * making — but you have to walk home to a settlement that houses a Doctor
     * to get it, which is the whole trade.
     */
    public static final int HEAL_PRICE = 100;

    /**
     * Three registries, one call, from {@code StairwayToHeavenMod.init} —
     * they all close right after it.
     *
     * <p>IDs are written as literals: {@code tools/locale_audit.py} follows
     * literals in the source, and an ID handed over as a constant is an ID the
     * audit cannot name-check.
     */
    public static void register() {
        MobRegistry.registerMob("doctorhuman", stairwaytoheaven.mobs.DoctorHumanMob.class, false);
        SettlerRegistry.registerSettler("doctor", new DoctorSettler());
        SettlerDialogueRegistry.registerSettlerDialogue("swh_doctorheal", DoctorHealDialogue.class);
    }
}
