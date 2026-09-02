package stairwaytoheaven.veil;

import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicBoolean;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.Buff;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;

/**
 * Soul Exposure — {@code docs/WORLD_DESIGN.md} §8's debuff, and the reason the
 * Veil is a wall rather than a walk.
 *
 * <h2>§8's table, and how it is spelled here</h2>
 *
 * <pre>
 *   seconds   effect
 *   0-3       slight vision reduction
 *   4-7       slow
 *   8-12      health drain
 *   12+       massive damage
 * </pre>
 *
 * <b>One stack is one second in the fog.</b> The table is written in seconds
 * and nothing else in it is finer than a second, so the stack count IS the
 * clock: {@link VeilWorldData} adds one stack per second while the player is
 * inside the region, and {@link #getRemainingStacksDuration} gives one back per
 * second once they are out. That makes the four bands literal stack numbers
 * ({@link #BAND_SLOW}, {@link #BAND_DRAIN}, {@link #BAND_LETHAL}), the HUD
 * countdown a real "seconds of exposure" readout, and the tooltip a direct
 * quote of the design table rather than a translation of it.
 *
 * <p>The bands are cumulative — at 9 seconds the player is dimmed, slowed AND
 * bleeding, which is how "a short step in is possible; running through is not"
 * actually reads in play.
 *
 * <p><b>Recovery is symmetric on purpose.</b> Twelve seconds in costs twelve
 * seconds out. An instant reset at the border would make the fog a rhythm game
 * — step out, step in, repeat — which is exactly the "running through" §8
 * forbids. Stepping back in resumes where you left off.
 *
 * <h2>The vanilla archetype</h2>
 *
 * {@code QuicksandStacksBuff} (jar 1.3.2): an environmental stacking debuff
 * whose source re-applies it while the condition holds and whose stacks drain
 * on their own once it stops. <b>VERIFIED [jar]</b>, the machinery that makes
 * that work is {@link #overridesStackDuration()} — {@code ActiveBuff.stack}
 * (ActiveBuff.java:102-108) then keeps ONE {@code BuffTime}, takes the longer of
 * the two durations and adds the stacks, and {@code ActiveBuff.tickExpired}
 * (:183-193) decrements one stack per {@link #getRemainingStacksDuration} when
 * that single time runs out instead of removing the buff. {@code SwampSporesBuff}
 * and {@code StarvingBuff} supply the rest of the shape: blindness as a
 * modifier, and shutting health regen off with a high-priority
 * {@code setMaxModifier} so the drain is not quietly healed away.
 *
 * <h2>Nothing here decides WHERE the fog is</h2>
 *
 * This class knows only how bad the fog is at N seconds. Whether a player is in
 * it is {@link VeilRegion}'s question and {@link VeilWorldData}'s tick, which is
 * what keeps the region check a region check (§8) instead of leaking into a
 * per-tile hook the buff could be desynchronised from.
 */
public class SoulExposureBuff extends Buff {

    /** Registered ID; also the locale key and the ledger row. */
    public static final String ID = "soulexposure";

    /**
     * The vanilla buff icon this borrows, by literal path.
     *
     * <p>The mod draws no new art for this (see {@code docs/VANILLA_ASSET_MAP.md}
     * §1.3). {@code spirithaunted} is vanilla's own "the dead have hold of you"
     * debuff icon, which is the right reading for the Veil, and it is a normal
     * 32x32 buff icon. It swaps out by shipping {@code buffs/soulexposure.png}
     * under the mod's own name — at which point {@link #loadTextures()} can be
     * deleted and the base class finds it by ID.
     */
    public static final String BORROWED_ICON = "buffs/spirithaunted";

    /** First stack at which movement is slowed. §8: seconds 4-7. */
    public static final int BAND_SLOW = 4;
    /** First stack at which health drains. §8: seconds 8-12. */
    public static final int BAND_DRAIN = 8;
    /** First stack of §8's "12+ massive damage". */
    public static final int BAND_LETHAL = 13;

    /**
     * Ceiling on the clock.
     *
     * <p>Three seconds past the lethal band, which is all the headroom the
     * table needs: at {@link #BAND_LETHAL} the damage is already flat, so
     * counting past 16 would change nothing and only make the HUD number grow.
     * It also bounds the recovery — sixteen seconds is the longest the fog can
     * ever hold on to someone who left it.
     */
    public static final int MAX_STACKS = 16;

    /** One stack given back per second once the player is out of the fog. */
    public static final int DECAY_MS = 1000;

    /**
     * Priority for the modifier floors/ceilings this buff sets.
     *
     * <p>Copied from {@code StarvingBuff} (10000) rather than invented: a floor
     * at that priority beats the {@code ModifierLimiter} that slow-immunity and
     * regen gear install, which is the point. The Veil is not a status effect
     * you counter with an accessory — §8 says the only way out of it is the
     * Veil Mark, and a trinket that shrugged off the slow would be a hole in
     * the gate.
     */
    private static final int LIMIT_PRIORITY = 10000;

    public SoulExposureBuff() {
        this.isVisible = true;
        this.isImportant = true;
        this.canCancel = false;      // there is no cancelling the Veil
        this.sortByDuration = false; // it sorts by severity, and severity is stacks
        // Not saved: the world decides every tick who is in the fog, so a saved
        // copy could only ever be wrong -- either still burning someone who
        // logged out and walked away, or a second later than the live check.
        this.shouldSave = false;
    }

    // ------------------------------------------------------------------
    // the four bands
    // ------------------------------------------------------------------

    /** §8 band 0-3s and up: how much of the view the fog takes. */
    public static float blindnessAt(int stacks) {
        float v = 0.10F + 0.05F * (stacks - 1);
        return v < 0.10F ? 0.10F : (v > 0.60F ? 0.60F : v);
    }

    /** §8 band 4-7s: movement penalty, 0 below the band. */
    public static float slowAt(int stacks) {
        if (stacks < BAND_SLOW) {
            return 0.0F;
        }
        float v = 0.15F + 0.05F * (stacks - BAND_SLOW);
        return v > 0.45F ? 0.45F : v;
    }

    /**
     * §8 bands 8-12s and 12+: damage per second, 0 below the drain band.
     *
     * <p>The drain ramps 10 → 30 across its five seconds, which is a nuisance
     * the player can walk out of. {@link #BAND_LETHAL} then jumps to 150, and
     * that is §8's "massive damage": against the health a player carries at
     * this tier it is a handful of seconds, after twelve seconds of escalating
     * warning. It is meant to kill someone who tries to cross, and to be
     * survivable by someone who turns round.
     */
    public static float damagePerSecondAt(int stacks) {
        if (stacks >= BAND_LETHAL) {
            return 150.0F;
        }
        if (stacks < BAND_DRAIN) {
            return 0.0F;
        }
        return 10.0F + 5.0F * (stacks - BAND_DRAIN);
    }

    /**
     * Applies the three bands for the current stack count.
     *
     * <p><b>VERIFIED [jar], and the reason the damage is divided:</b>
     * {@code BuffManager.tickDamageOverTime} (BuffManager.java:292) computes
     * {@code buff.getModifier(POISON_DAMAGE_FLAT) * buff.getStacks()} — the
     * engine multiplies the flat DOT by the stack count itself. A buff whose
     * severity is NOT linear in stacks (ours is a step function) therefore has
     * to store damage-per-second-per-stack, or the curve is silently squared.
     * Everything else in {@code BuffModifiers} is read as-is.
     *
     * <p>{@code POISON_DAMAGE_FLAT} is the channel because it is the one
     * vanilla itself uses for being harmed by the dead — {@code HauntedBuff},
     * {@code SpiritCorruptedBuff} and {@code DryadPossessedBuff} all deal their
     * damage through it — and because the engine's DOT loop gives us damage
     * text, combat timing, the difficulty multiplier and correct death
     * attribution for free (BuffManager.java:285-311).
     */
    private void updateModifiers(ActiveBuff buff) {
        int stacks = buff.getStacks();

        buff.setModifier(BuffModifiers.BLINDNESS, blindnessAt(stacks));

        float slow = slowAt(stacks);
        if (slow > 0.0F) {
            buff.setModifier(BuffModifiers.SLOW, slow);
            buff.setMinModifier(BuffModifiers.SLOW, Float.valueOf(slow), LIMIT_PRIORITY);
        } else {
            buff.setModifier(BuffModifiers.SLOW, 0.0F);
            buff.clearMinModifier(BuffModifiers.SLOW);
        }

        float dps = damagePerSecondAt(stacks);
        if (dps > 0.0F) {
            buff.setModifier(BuffModifiers.POISON_DAMAGE_FLAT, dps / stacks);
            // Regen off while the Veil is taking health, exactly as
            // StarvingBuff does it. Without this the drain band is a fight
            // between two numbers the player cannot see.
            buff.setMaxModifier(BuffModifiers.HEALTH_REGEN, Float.valueOf(0.0F), LIMIT_PRIORITY);
            buff.setMaxModifier(BuffModifiers.COMBAT_HEALTH_REGEN, Float.valueOf(0.0F), LIMIT_PRIORITY);
        } else {
            buff.setModifier(BuffModifiers.POISON_DAMAGE_FLAT, 0.0F);
            buff.clearMaxModifier(BuffModifiers.HEALTH_REGEN);
            buff.clearMaxModifier(BuffModifiers.COMBAT_HEALTH_REGEN);
        }
    }

    @Override
    public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
        this.updateModifiers(buff);
    }

    @Override
    public void onStacksUpdated(ActiveBuff buff, ActiveBuff other) {
        super.onStacksUpdated(buff, other);
        this.updateModifiers(buff);
    }

    // ------------------------------------------------------------------
    // stacking and decay
    // ------------------------------------------------------------------

    @Override
    public int getStackSize(ActiveBuff buff) {
        return MAX_STACKS;
    }

    @Override
    public boolean overridesStackDuration() {
        return true;
    }

    @Override
    public int getRemainingStacksDuration(ActiveBuff buff, AtomicBoolean sendUpdatePacket) {
        // true: the stack count is the severity AND the HUD readout, so a
        // client whose count has drifted shows the wrong band and the wrong
        // blindness. One small packet per second per affected player buys an
        // honest screen.
        sendUpdatePacket.set(true);
        return DECAY_MS;
    }

    // ------------------------------------------------------------------
    // what the player sees
    // ------------------------------------------------------------------

    @Override
    public boolean shouldDrawDuration(ActiveBuff buff) {
        return true;
    }

    /** Seconds of exposure, which is the stack count. */
    @Override
    public String getDurationText(ActiveBuff buff) {
        return buff.getStacks() + "s";
    }

    @Override
    public int getStacksDisplayCount(ActiveBuff buff) {
        // The duration text already IS the stack count; drawing it twice in one
        // 32x32 icon reads as two different numbers.
        return 0;
    }

    /**
     * The tooltip has to answer "why am I losing health", because an
     * unexplained drain reads as a bug rather than as a wall.
     *
     * <p>So it names the effect, then prints the band the player is actually in
     * and what comes next — the design table, live, at the moment it matters.
     */
    @Override
    public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
        ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
        tooltips.add(Localization.translate("bufftooltip", "soulexposuretip"));
        // Written out rather than assembled from bandOf(): tools/locale_audit.py
        // reads Localization.translate call sites for LITERAL keys, and a key
        // built at runtime is a key nothing checks. Four lines buys four keys
        // that cannot ship missing.
        switch (bandOf(ab.getStacks())) {
            case 4:
                tooltips.add(Localization.translate("bufftooltip", "soulexposureband4"));
                break;
            case 3:
                tooltips.add(Localization.translate("bufftooltip", "soulexposureband3"));
                break;
            case 2:
                tooltips.add(Localization.translate("bufftooltip", "soulexposureband2"));
                break;
            default:
                tooltips.add(Localization.translate("bufftooltip", "soulexposureband1"));
                break;
        }
        return tooltips;
    }

    /**
     * Which of §8's four rows a given number of seconds falls in, 1..4. The one
     * place the thresholds are read, so the tooltip and the debug readout can
     * never disagree about which band a player is in.
     */
    public static int bandOf(int stacks) {
        if (stacks >= BAND_LETHAL) {
            return 4;
        }
        if (stacks >= BAND_DRAIN) {
            return 3;
        }
        if (stacks >= BAND_SLOW) {
            return 2;
        }
        return 1;
    }

    /** The band's locale key, for logs and {@code /veilmark}. */
    public static String bandKey(int stacks) {
        return "soulexposureband" + bandOf(stacks);
    }

    /**
     * Client-side only ({@code GameResources} calls this, GameResources.java:861,
     * and the dedicated server never builds it), so borrowing a vanilla texture
     * by literal path here is safe on a headless server.
     *
     * <p>Falls back to {@code super}, which is vanilla's own
     * {@code buffs/unknown}, if the borrowed path ever moves — an ugly icon is
     * better than a client that will not start.
     */
    @Override
    public void loadTextures() {
        try {
            this.iconTexture = GameTexture.fromFileRaw(BORROWED_ICON);
        } catch (FileNotFoundException e) {
            super.loadTextures();
        }
    }
}
