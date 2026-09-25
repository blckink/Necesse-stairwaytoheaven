package stairwaytoheaven.mobs;

import java.awt.Point;
import java.util.Comparator;

import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.Level;
import stairwaytoheaven.settlement.SkyDoctor;

/**
 * The Doctor runs to a fallen settler of his own settlement and gets them back
 * on their feet — no Revival Potion, no player needed.
 *
 * <h2>What "fallen" is in vanilla</h2>
 *
 * With {@code canSettlersDie=false} a settler at 0 health is not killed but
 * <b>downed</b> ({@code HumanMob.setHealthHidden}, 1.3.3): it lies on the spot,
 * is removed from the settlement's settler list, and waits for a player to
 * talk to it with a Revival Potion. That list removal is why a downed settler
 * vanishes from the settlement menu. What it keeps is
 * {@code savedSettlerSettings}, built from its {@code LevelSettler} just
 * before the removal — and that record's {@code settlementUniqueID} is the
 * one reliable "this was ours" key. The other candidate,
 * {@code recruitReservedSettlementUniqueID}, is useless: vanilla zeroes
 * {@code settlementUniqueID} one line before it reads it for the reservation.
 *
 * <h2>Where it sits</h2>
 *
 * Directly after vanilla's player-command nodes and before everything else
 * ({@link DoctorAI}): an order from the player still wins, but a fallen
 * settler outranks fighting, hiding at low health, working and wandering.
 * The actual revive is {@link SkyDoctor#revive}, vanilla's recruit path
 * without the payment.
 */
public class DoctorReviveAINode<T extends DoctorHumanMob> extends MoveTaskAINode<T> {

    /** How far he notices a fallen settler, in pixels (32 = one tile). */
    private static final int NOTICE_RANGE = 64 * 32;

    /** Close enough to kneel down and work. */
    private static final float REVIVE_DISTANCE = 48.0F;

    private HumanMob patient;
    private int searchCooldown;

    @Override
    protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
        blackboard.onUnloading(e -> this.patient = null);
    }

    @Override
    public void init(T mob, Blackboard<T> blackboard) {
    }

    @Override
    public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
        if (!mob.isServer() || !mob.isSettler() || mob.isDowned()) {
            this.patient = null;
            return AINodeResult.FAILURE;
        }
        if (this.patient != null && (this.patient.removed() || !this.patient.isDowned()
                || this.patient.getLevel() != mob.getLevel())) {
            this.patient = null;
        }
        if (this.patient == null) {
            if (--this.searchCooldown > 0) {
                return AINodeResult.FAILURE;
            }
            // One second between scans: quick enough that he sets off while
            // the raid is still going, cheap enough for every tick of peace.
            this.searchCooldown = 20;
            this.patient = findPatient(mob);
            if (this.patient == null) {
                return AINodeResult.FAILURE;
            }
        }

        if (mob.getDistance(this.patient) <= REVIVE_DISTANCE) {
            HumanMob revived = this.patient;
            this.patient = null;
            if (blackboard.mover.isCurrentlyMovingFor(this)) {
                blackboard.mover.stopMoving(mob);
            }
            if (!SkyDoctor.revive(mob, revived)) {
                // Settlement full or gone: try again later, not every tick.
                this.searchCooldown = 200;
                return AINodeResult.FAILURE;
            }
            return AINodeResult.SUCCESS;
        }

        if (blackboard.mover.isCurrentlyMovingFor(this)) {
            return AINodeResult.RUNNING;
        }

        final HumanMob target = this.patient;
        return this.moveToTileTask(target.getTileX(), target.getTileY(),
                (current, wanted) -> isAdjacent(current, wanted),
                path -> {
                    if (path.result.foundTarget) {
                        path.move(null);
                        return AINodeResult.RUNNING;
                    }
                    // Unreachable (walled in, across water): leave them for the
                    // player's Revival Potion rather than stand still forever.
                    this.patient = null;
                    this.searchCooldown = 200;
                    return null;
                });
    }

    private static boolean isAdjacent(Point current, Point wanted) {
        return Math.abs(current.x - wanted.x) <= 1 && Math.abs(current.y - wanted.y) <= 1;
    }

    /**
     * The nearest downed settler that belonged to his settlement. Public for
     * {@code skyreachstatus doctor}, which runs the search and the revive
     * directly: a headless server unloads the surface without a player, so
     * the walk itself cannot be watched there.
     */
    public static HumanMob findPatient(DoctorHumanMob doctor) {
        Level level = doctor.getLevel();
        if (level == null) {
            return null;
        }
        return level.entityManager.mobs.streamInRegionsInRange(doctor.x, doctor.y, NOTICE_RANGE)
                .filter(m -> m instanceof HumanMob && m != doctor && !m.removed())
                .map(m -> (HumanMob) m)
                .filter(h -> h.isDowned() && SkyDoctor.belongsTo(h, doctor))
                .min(Comparator.comparingDouble(h -> doctor.getDistance(h)))
                .orElse(null);
    }
}
