package stairwaytoheaven.bosses;

import necesse.engine.registries.BuffRegistry;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.Buff;

/**
 * Puts vanilla's incursion tier curve on ONE spawned mob.
 *
 * <h2>Why a buff, and not {@code LevelModifiers}</h2>
 *
 * <p>Vanilla scales an incursion with {@code LevelModifiers.ENEMY_MAX_HEALTH}
 * and {@code ENEMY_DAMAGE} ({@code BiomeMissionIncursionData.initModifiers},
 * :131-141), which is right for an incursion because an incursion IS a level:
 * every mob on it belongs to the fight. The sky plane is not. It is the whole
 * world ({@code docs/PLAN_ONE_PLANE.md}), and a level modifier there would hand
 * x3.18 health and x1.87 damage to every Cloud Lamb, every guard pack and every
 * settler-bothering critter in six realms at once, permanently, because one
 * player woke one boss.
 *
 * <p>So the scaling rides a permanent buff on the boss itself.
 * {@code Mob.getMaxHealth} (Mob.java:3817-3825) multiplies the flat health by
 * {@code buffManager.getModifier(BuffModifiers.MAX_HEALTH)} and
 * {@code GameDamage.getDamageModifier} (GameDamage.java:192-203) reads the
 * attacker's {@code BuffModifiers.ALL_DAMAGE} — both {@code Modifier<Float>}
 * (BuffModifiers.java:305 and :15), both with a buff-manager default of
 * {@code 1.0F} and {@code FLOAT_ADD_APPEND} (Modifier.java:20). <b>VERIFIED
 * [jar]</b>. That last detail is why the buff stores <i>multiplier − 1</i>:
 * the manager starts at 1.0 and ADDS, exactly as
 * {@code BiomeMissionIncursionData.getHealthIncrease} returns an increase
 * rather than a factor.
 *
 * <h2>The tier travels on the ActiveBuff, not on the Buff</h2>
 *
 * <p>A {@code Buff} is a registry singleton — one instance for the whole game —
 * so it cannot hold "tier 9". The tier is written into the {@link ActiveBuff}'s
 * own {@code GNDItemMap}, which is saved with the mob
 * ({@code ActiveBuff.addSaveData}, :516-518) and written into its spawn packet
 * ({@code setupContentPacket}, :468). Both the load path and the packet path
 * re-enter {@code BuffManager.addBuff}, which calls {@link ActiveBuff#init}
 * ({@code BuffManager.java:80-110, :639-679}) — so {@link TierBuff#init} is the
 * one place the modifiers are ever set, and a boss keeps its tier across a
 * server restart and reaches every client with it.
 *
 * <h2>Permanent, and synced</h2>
 *
 * <p>{@code isPassive = true} is how a Necesse buff becomes permanent:
 * {@code ActiveBuff.isExpired} and {@code tickExpired} both return early for a
 * passive buff ({@code ActiveBuff.java:167, :282-287}), so its duration is
 * never counted and it is never removed. Passive also switches the network off
 * — {@code Buff.shouldNetworkSync} is {@code !isPassive || overrideSync}
 * ({@code Buff.java:250-252}) — and a client that does not know about the buff
 * computes the wrong maximum health and draws a boss health bar that is wrong
 * from the first hit. Hence {@code overrideSync = true}.
 *
 * <h2>What this does NOT reach, and why that is acceptable</h2>
 *
 * <p>The buff lands on the ONE mob the portal spawns. Two of §B4's five bosses
 * are worms and two of them bring company, so it is worth saying exactly what
 * that covers.
 *
 * <p><b>Health is complete for all five.</b> {@code WormMobBody.getHealth} and
 * {@code getMaxHealth} (WormMobBody.java:237-255) both delegate to the head, and
 * {@code isHit} (:261-270) routes every hit to it, so the Pest Warden and the
 * Crystal Dragon carry the scaled body on their heads. <b>VERIFIED [jar]</b>.
 *
 * <p><b>Damage is complete for everything the boss itself fires</b>, because
 * {@code GameDamage.getDamageModifier} reads the attack OWNER's modifier. It is
 * NOT applied to a worm's body-segment contact damage —
 * {@code HostileWormMobBody.getOutgoingDamageModifier} (:18-26) reads that
 * segment's own buff manager, and segments are created lazily during the head's
 * tick ({@code WormMobHead.java:196-215}), not at spawn, so there is nothing to
 * buff at the moment this runs — nor to the Ascended Wizard's summoned
 * peripherals. Both are LEFT UNSCALED on purpose rather than chased with a
 * per-tick hook: the error is small, it is in the safe direction (the fight is
 * slightly easier, never harder than §B4 says), and §B4's ladder is written in
 * health, which is exact.
 */
public final class BossScaling {

    /** Registered buff ID; also the ledger row and the {@code [buff]} key. */
    public static final String BUFF_ID = "incursionpressure";

    /**
     * Key the tier is stored under inside the {@link ActiveBuff}'s GND map.
     * Prefixed because that map is a flat namespace shared with the engine.
     */
    public static final String TIER_KEY = "swhtier";

    private static Buff pressure;

    private BossScaling() {
    }

    /**
     * Registers the buff. Called once, from
     * {@link BossPortalObject#register()}.
     */
    public static void register() {
        pressure = BuffRegistry.registerBuff(BUFF_ID, new BossScaling.TierBuff());
    }

    /** The registered buff, or {@code null} before registration. */
    public static Buff buff() {
        return pressure;
    }

    /**
     * Puts one tier's worth of vanilla incursion scaling on one mob.
     *
     * <p><b>Call this AFTER {@code entityManager.addMob}.</b> A mob's health is
     * set to its maximum during construction ({@code Mob.onConstructed},
     * Mob.java:308-311) and some bosses set it again in {@code init()}, which
     * {@code EntityList} runs as part of adding them (EntityList.java:183-186).
     * Raising the maximum afterwards would otherwise leave the boss standing at
     * its unscaled health — 18 000 of a possible 57 240 — so the last thing
     * this does is top it back up.
     *
     * @param tier the incursion tablet tier, from {@link SkyBossLadder.Boss}.
     */
    public static void apply(Mob mob, int tier) {
        if (mob == null || pressure == null || tier <= 0) {
            return;
        }
        ActiveBuff active = new ActiveBuff(pressure, mob, 0, null);
        active.getGndData().setInt(TIER_KEY, tier);
        mob.buffManager.addBuff(active, true);
        // The buff is in place, so getMaxHealth() now reports the scaled body.
        mob.setHealth(mob.getMaxHealth());
    }

    /**
     * The buff itself: invisible, permanent, synced, and carrying nothing but
     * one integer.
     *
     * <p>Invisible because nothing draws a MOB's buff bar — only the local
     * player's — so a visible one would be a name and an icon no one can ever
     * see. That also matches every other buff this mod puts on something other
     * than the player.
     */
    public static class TierBuff extends Buff {

        public TierBuff() {
            // Permanent: see the class header. Duration is never read.
            this.isPassive = true;
            // ...but the client still needs the modifier for the health bar.
            this.overrideSync = true;
            // A boss that survives a restart must survive it at its own tier.
            this.shouldSave = true;
            this.isVisible = false;
            this.canCancel = false;
        }

        /**
         * Reads the tier off this {@link ActiveBuff} and turns it into the two
         * modifiers.
         *
         * <p>A tier of 0 means the GND data never arrived — an old save, or a
         * mob something else buffed by hand. Setting no modifier at all is the
         * right answer there: the boss is merely unscaled, which is survivable,
         * where a guessed tier would be a silent balance change.
         */
        @Override
        public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
            int tier = buff.getGndData().getInt(TIER_KEY, 0);
            if (tier <= 0) {
                return;
            }
            // -1.0F because the buff manager starts these two modifiers at
            // 1.0F and ADDS each buff's value (Modifier.FLOAT_ADD_APPEND).
            // Vanilla's own incursion code stores the same increase for the
            // same reason -- BiomeMissionIncursionData.getHealthIncrease.
            buff.setModifier(BuffModifiers.MAX_HEALTH,
                    SkyBossLadder.healthMultiplier(tier) - 1.0F);
            buff.setModifier(BuffModifiers.ALL_DAMAGE,
                    SkyBossLadder.damageMultiplier(tier) - 1.0F);
        }
    }
}
