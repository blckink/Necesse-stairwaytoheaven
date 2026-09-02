package stairwaytoheaven.realms.eden;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.stream.Stream;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedPlayerChaserWandererAI;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;

/**
 * Bloom Maw — the carnivorous flower (§5: <i>"Bloom Maw — carnivorous
 * flower"</i>).
 *
 * <p><b>It does not move.</b> That is the whole design. A3.3's dangerous beauty
 * needs one thing that is not a chaser: something you walk INTO because it
 * looked like scenery, which is a different feeling from something that runs at
 * you, and which A4.1 explicitly wants more of. Speed is 0 and the AI's reach
 * is short; a Bloom Maw you can see is a Bloom Maw you can walk around, and one
 * standing over a cache is a wall you have to deal with.
 *
 * <p><b>Borrowed art:</b> vanilla {@code mobs/stabbybush} — a green mossy bush
 * with pale blue eyes and, in the attack column, an open mouth. 382x320: six
 * 64px columns (idle, attack, four walk) over four direction rows plus a
 * particle row. Drawn with {@code StabbyBushMob.addDrawables}' own offsets
 * ({@code drawX - 32}, {@code drawY - 44 - 7}). NOT subclassed: vanilla's bush
 * carries a frenzy buff that detonates it at max stacks
 * ({@code StabbyBushMob.serverTick}), which is a fine joke at 100 HP and an
 * unreadable one at 1500.
 *
 * <p><b>Tier: Eden floor, standard role</b> ({@link EdenTiers}) —
 * <b>1500 HP / 165 damage / 45 armour</b> on CLASSIC, derived there from
 * vanilla's incursion-3 scaling over the measured Skyreach floor. Vanilla's own
 * Stabby Bush is 100 HP / 20 damage / no armour and is untouched.
 */
public class BloomMawMob extends EdenHostileMob {

    /** Loaded in {@link EdenRealm#loadTextures()} from the GAME's own resources. */
    public static GameTexture texture;

    /** Eden floor 1500 HP on CLASSIC, on vanilla's own difficulty ratios. */
    public static final MaxHealthGetter MAX_HEALTH = EdenTiers.health();
    /**
     * Eden floor 165 damage, standard role.
     *
     * <p>It keeps the full floor despite never having to close distance,
     * because it also never gets to choose its fight: the ladder's ranged
     * discount buys reach, and a maw has none.
     */
    public static final GameDamage DAMAGE = EdenTiers.damage();
    public static final int ARMOR = EdenTiers.EDEN_ARMOR;

    /** Drops, at Eden's x1.3 drop value. Sap is the plant-side material. */
    public static LootTable lootTable = new LootTable(
            new ChanceLootItemList(0.80F, LootItem.between("edensap",
                    EdenTiers.drop(1), EdenTiers.drop(2))),
            new ChanceLootItemList(0.30F, LootItem.between("paradiseapple", 1, 2)));

    public BloomMawMob() {
        super(EdenTiers.EDEN_HP);
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
        // Rooted. StabbyBushMob's own speed/friction pair.
        this.setSpeed(0.0F);
        this.setFriction(2.0F);
        this.setKnockbackModifier(0.0F);
        this.collision = new Rectangle(-10, -7, 20, 14);
        this.hitBox = new Rectangle(-16, -22, 32, 32);
        this.selectBox = new Rectangle(-26, -32, 52, 42);
        this.attackCooldown = 1000;
        this.attackAnimTime = 200;
    }

    @Override
    public void init() {
        super.init();
        // The vanilla bush's tree shape, with our damage. 96 pixels of reach is
        // three tiles: far enough that standing next to one is a mistake, short
        // enough that it can never hit you from off screen.
        this.ai = new BehaviourTreeAI<>(this,
                new ConfusedPlayerChaserWandererAI<BloomMawMob>(null, 384, 96, -1, false, false) {
                    @Override
                    public boolean attackTarget(BloomMawMob mob, Mob target) {
                        if (!mob.canAttack()) {
                            return false;
                        }
                        mob.attack(target.getX(), target.getY(), false);
                        target.isServerHit(DAMAGE, mob.dx, mob.dy, 25.0F, mob);
                        return true;
                    }
                });
    }

    /**
     * A rooted mob must not be dragged around by the target-range modifier, or
     * it slides. Vanilla's own bush does exactly this when its speed is 0
     * ({@code StabbyBushMob.getDefaultModifiers}).
     */
    @Override
    public Stream<ModifierValue<?>> getDefaultModifiers() {
        return Stream.concat(super.getDefaultModifiers(),
                Stream.of(new ModifierValue<>(BuffModifiers.TARGET_RANGE, -1.0F)));
    }

    /**
     * Idle, or the open mouth while an attack is on cooldown — the bush's own
     * two-frame tell, so a player can read "it has noticed me" off the sprite.
     * Copied from {@code StabbyBushMob.getAnimSprite}; the walk columns are
     * unreachable here because the maw never moves.
     */
    @Override
    public Point getAnimSprite(int x, int y, int dir) {
        Point p = new Point(0, dir);
        if (this.getNextAttackCooldown() >= -100L) {
            p.x = 1;
        }
        return p;
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
        return 64;
    }

    @Override
    protected int drawOffsetX() {
        return -32;
    }

    @Override
    protected int drawOffsetY() {
        // StabbyBushMob.addDrawables:159 — `camera.getDrawY(y) - 44 - 7`.
        return -44 - 7;
    }

    @Override
    public void spawnDeathParticles(float knockbackX, float knockbackY) {
        for (int i = 0; i < 14; i++) {
            this.getLevel().entityManager
                    .addParticle(this.x, this.y, Particle.GType.IMPORTANT_COSMETIC)
                    .movesConstant(EdenSerpentMob.deathSpeed(knockbackX), EdenSerpentMob.deathSpeed(knockbackY))
                    .color(new Color(74, 132, 46));
        }
    }
}
