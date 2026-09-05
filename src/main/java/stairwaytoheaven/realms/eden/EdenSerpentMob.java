package stairwaytoheaven.realms.eden;

import java.awt.Color;
import java.awt.Rectangle;

import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedCollisionPlayerChaserWandererAI;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.particle.Particle;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;

/**
 * Eden Serpent — the snake in the tall grass, and the realm's standard enemy.
 *
 * <p>§5: <i>"Eden Serpent — poison attack; drops Serpent Scale, Venom Fang"</i>.
 * A3.3 states the thesis it exists to prove: <b>beauty can be dangerous</b>.
 *
 * <p><b>Borrowed art:</b> vanilla {@code mobs/crocodile} — a bright green
 * scaled reptile seen from above, 768x640, i.e. six 128px animation columns
 * over four direction rows plus a particle row. Drawn with
 * {@code CrocodileMob.addDrawables}' own offsets ({@code drawX - 64},
 * {@code drawY - 128 + 36}, {@code sprite(x, y, 128)}), copied rather than
 * guessed. Not subclassed, because {@code CrocodileMob} is a {@code FriendlyMob}
 * that only turns hostile when struck — it would never guard anything, and its
 * {@code serverTick} would keep switching itself back.
 *
 * <p><b>Tier: Eden floor, standard role</b> ({@link EdenTiers}).
 * <b>1500 HP / 165 damage / 45 armour</b> on CLASSIC. Every number is derived
 * there from vanilla's own incursion-3 scaling over the measured Skyreach floor
 * ({@code AscendedGolemMob.MAX_HEALTH} = 1000 CLASSIC and
 * {@code CrystalGolemMob.damage} = 130, both VERIFIED [jar]); vanilla's own
 * crocodile is 250 HP / 35 damage / 12 armour and is untouched by any of this.
 *
 * <p><b>The poison</b> is vanilla's {@code poison} buff, applied by the same
 * call {@code SwampSlimeMob} and the spiders use — six seconds on a hit, which
 * is what makes the serpent a fight you leave rather than one you tank.
 */
public class EdenSerpentMob extends EdenHostileMob {

    /** Loaded in {@link EdenRealm#loadTextures()} from the GAME's own resources. */
    public static GameTexture texture;

    /** Eden floor 1500 HP on CLASSIC, on vanilla's own difficulty ratios. */
    public static final MaxHealthGetter MAX_HEALTH = EdenTiers.health();
    /** Eden floor 165 damage, standard role — no modifier. */
    public static final GameDamage DAMAGE = EdenTiers.damage();
    /** Eden armour: one step over the Skyreach's measured 40. */
    public static final int ARMOR = EdenTiers.EDEN_ARMOR;

    /**
     * Drops, at Eden's x1.3 drop value.
     *
     * <p>The scale is the realm's common mob material and the fang is the rare
     * one, which is the shape A4.5 asks for: one thing you get every few kills
     * and one thing you go looking for. Both have sinks — see
     * {@link EdenRealm#registerRecipes()}.
     */
    public static LootTable lootTable = new LootTable(
            new ChanceLootItemList(0.75F, LootItem.between("serpentscale",
                    EdenTiers.drop(1), EdenTiers.drop(2))),
            new ChanceLootItemList(0.28F, LootItem.between("venomfang", 1, 1)));

    public EdenSerpentMob() {
        super(EdenTiers.EDEN_HP);
        // Registered in construction: MobDifficultyChanges throws if it is
        // touched after init() (the AscendedGolemMob pattern).
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
        // Vanilla's crocodile moves at 45 when hostile; the serpent keeps that
        // and its friction, so the borrowed animation cadence still matches
        // the distance it covers.
        this.setSpeed(45.0F);
        this.setFriction(3.0F);
        this.setKnockbackModifier(0.3F);
        this.prioritizeVerticalDir = true;
        this.collision = new Rectangle(-10, -7, 20, 14);
        this.hitBox = new Rectangle(-20, -16, 40, 32);
        this.selectBox = new Rectangle(-20, -50, 40, 55);
    }

    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(this,
                new ConfusedCollisionPlayerChaserWandererAI<EdenSerpentMob>(null, 480, DAMAGE, 110, 40000) {
                    @Override
                    public boolean attackTarget(EdenSerpentMob mob, Mob target) {
                        boolean hit = super.attackTarget(mob, target);
                        if (hit) {
                            // §5's "poison attack". Vanilla's own debuff,
                            // applied the way vanilla's ivy set applies it
                            // (IvyHatSetBonusBuff.java:40): the buff, the
                            // target, seconds, the attacker, and the server
                            // flag taken from the target.
                            target.buffManager.addBuff(
                                    new ActiveBuff(BuffRegistry.Debuffs.GENERIC_POISON, target, 6.0F, mob),
                                    target.isServer());
                        }
                        return hit;
                    }
                });
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    @Override
    protected GameTexture sheet() {
        return texture;
    }

    @Override
    protected int spriteSize() {
        return 128;
    }

    @Override
    protected int drawOffsetX() {
        return -64;
    }

    @Override
    protected int drawOffsetY() {
        // CrocodileMob.addDrawables:142 — `camera.getDrawY(y) - 128 + 36`.
        return -128 + 36;
    }

    @Override
    public void spawnDeathParticles(float knockbackX, float knockbackY) {
        for (int i = 0; i < 12; i++) {
            this.getLevel().entityManager
                    .addParticle(this.x, this.y, Particle.GType.IMPORTANT_COSMETIC)
                    .movesConstant(deathSpeed(knockbackX), deathSpeed(knockbackY))
                    .color(new Color(96, 168, 62));
        }
    }

    static float deathSpeed(float knockback) {
        return knockback / 2.0F + GameRandom.globalRandom.getIntBetween(5, 15)
                * (GameRandom.globalRandom.nextBoolean() ? -1 : 1);
    }

    /**
     * Bestiary face: it wears mobs/crocodile (EdenRealm.loadTextures), so it wears that creature's
     * face in the journal too. {@code Mob.getMobIcon()} is overridable and
     * {@code FormJournalEntryComponent} asks the MOB rather than the registry,
     * so this needs no PNG of its own -- see {@link stairwaytoheaven.mobs.BorrowedMobIcon}
     * for why borrowing the face is the right answer and not a shortcut.
     */
    @Override
    public necesse.gfx.gameTexture.GameTexture getMobIcon() {
        return stairwaytoheaven.mobs.BorrowedMobIcon.from("crocodile", super.getMobIcon());
    }

}
