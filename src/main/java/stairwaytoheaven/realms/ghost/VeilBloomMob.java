package stairwaytoheaven.realms.ghost;

import java.awt.Point;

import necesse.engine.util.GameRandom;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.leaves.TargetFinderAINode;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedPlayerChaserWandererAI;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.hostile.StabbyBushMob;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import stairwaytoheaven.mobs.BorrowedMobIcon;
import stairwaytoheaven.mobs.SkyMobTiers;
import stairwaytoheaven.veil.VeilFogBuff;

/**
 * Veil Bloom / Nebelbluete — a grave flower in the deep Ghost Realm that
 * breathes the Geisternebel out of its own petals (user request 2026-09-15:
 * "Blumen-Gegner, die in der Tiefe auch Nebel machen").
 *
 * <p>Vanilla archetype {@link StabbyBushMob} (§1.3, the same seam
 * {@code SourvatBloomMob} uses): it sits still as a plant and lashes out when
 * a player comes close. What it adds is the cloud. Around every bloom the fog
 * is thicker than the Veil's own, so a marsh with two of them is a place you
 * walk into half blind and find the flower only when it bites.
 *
 * <p>The cloud is client-side scenery drawn with
 * {@link VeilFogBuff#spawnWisp}; it changes no rule on the server. Wears the
 * Stabby Bush sheet until it has its own art.
 */
public class VeilBloomMob extends StabbyBushMob {

    public static final String ID = "veilbloom";

    public static final MaxHealthGetter MAX_HEALTH = SkyMobTiers.scaled(
            SkyMobTiers.hp(SkyMobTiers.VEIL_HP, SkyMobTiers.ROLE_ELITE_HP));

    public static final GameDamage DAMAGE = new GameDamage(SkyMobTiers.VEIL_DAMAGE);

    public static final int ARMOR = SkyMobTiers.VEIL_ARMOR;

    public static final int AGGRO_RANGE = SkyMobTiers.aggro(384, SkyMobTiers.UPLIFT_VEIL_AGGRO);

    /** How far from the flower its own fog drifts, in tiles. */
    private static final int CLOUD_RADIUS_TILES = 5;

    /** Wisps per client tick: about 1.4, so some eighty hang around one bloom. */
    private static final int CLOUD_ATTEMPTS = 2;
    private static final float CLOUD_CHANCE = 0.7F;

    public static LootTable lootTable = new LootTable(
            LootItem.between("ectoplasm", 3, 7),
            ChanceLootItem.between(0.45F, "soulthread", 1, 3));

    public VeilBloomMob() {
        super();
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
    }

    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(this,
                new ConfusedPlayerChaserWandererAI<VeilBloomMob>(null, AGGRO_RANGE, 64, -1, false, false) {
                    @Override
                    public boolean attackTarget(VeilBloomMob mob, Mob target) {
                        if (!mob.canAttack()) {
                            return false;
                        }
                        mob.attack(target.getX(), target.getY(), false);
                        target.isServerHit(DAMAGE, mob.dx, mob.dy, 15.0F, mob);
                        return true;
                    }

                    @Override
                    public GameAreaStream<Mob> streamPossibleTargets(VeilBloomMob mob, Point base,
                            TargetFinderDistance<VeilBloomMob> distance) {
                        return TargetFinderAINode.streamPlayersAndHumans(mob, base, distance)
                                .filter(m -> m.isPlayer || mob.isAttacker(m));
                    }
                });
    }

    @Override
    public void clientTick() {
        super.clientTick();
        if (this.getLevel() == null || !this.isVisible()) {
            return;
        }
        GameRandom r = GameRandom.globalRandom;
        for (int i = 0; i < CLOUD_ATTEMPTS; i++) {
            if (!r.getChance(CLOUD_CHANCE)) {
                continue;
            }
            float px = this.x + r.getFloatBetween(-CLOUD_RADIUS_TILES * 32.0F, CLOUD_RADIUS_TILES * 32.0F);
            float py = this.y + r.getFloatBetween(-CLOUD_RADIUS_TILES * 32.0F, CLOUD_RADIUS_TILES * 32.0F);
            VeilFogBuff.spawnWisp(this.getLevel(), px, py, true);
        }
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    @Override
    public GameTexture getMobIcon() {
        return BorrowedMobIcon.from("stabbybush", super.getMobIcon());
    }
}
