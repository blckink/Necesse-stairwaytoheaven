package stairwaytoheaven.mobs;

import java.awt.Point;

import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.NetworkSettlementData;

/**
 * The night hunt: he leaves the walls and empties something wild.
 *
 * <h2>Why this is not the vanilla hunting profession</h2>
 *
 * A settler's jobs are fenced in by {@code HumanMob.getJobRestrictZone()},
 * which hands back {@code isTileInSettlementBoundsAndRestrictZoneTester}
 * (VERIFIED [jar]). Vanilla's {@code hunting} job therefore hunts INSIDE the
 * settlement — i.e. exactly the cows and chickens the player is raising, which
 * is the one thing this character must not touch. The hunt outside the wall
 * has to be its own behaviour, and that is this node.
 *
 * <h2>The hard limit worth knowing</h2>
 *
 * Only mobs in loaded regions exist at all. "The world around the base" is
 * therefore the loaded ring around the player's settlement, not the map: on a
 * night when nobody is near, the settlement is not simulated and no hunt
 * happens. That is a property of the game, not a shortcut taken here.
 *
 * <h2>Where it sits</h2>
 *
 * Directly above the sleep node and below the job nodes ({@link VampireAI}):
 * a night with settlement work in it is spent working, and an idle night is
 * spent hunting. It only runs when he is actually thirsty, so a fed vampire
 * stays home and behaves like any other settler on the night shift.
 */
public class VampireHuntAINode<T extends VampireSettlerMob> extends MoveTaskAINode<T> {

    /** How far outside he will walk for a meal, in pixels (32 = one tile). */
    private static final int HUNT_RANGE = 640;

    /** Below this he goes looking; a fed vampire has no reason to leave. */
    private static final float THIRSTY_BELOW = 0.85F;

    private Mob prey;
    private int searchCooldown;

    @Override
    protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
        blackboard.onUnloading(e -> this.prey = null);
    }

    @Override
    public void init(T mob, Blackboard<T> blackboard) {
    }

    @Override
    public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
        if (!mob.isServer() || !mob.isNightTime() || mob.isHiding || !mob.isSettler()
                || mob.getBloodThirst() >= THIRSTY_BELOW) {
            this.prey = null;
            return AINodeResult.FAILURE;
        }
        if (this.prey != null && (this.prey.removed() || this.prey.getLevel() != mob.getLevel())) {
            this.prey = null;
        }
        if (this.prey == null) {
            if (--this.searchCooldown > 0) {
                return AINodeResult.FAILURE;
            }
            // A full area scan is not free; a few seconds apart is plenty for
            // something that ends in a walk across half a screen.
            this.searchCooldown = 120;
            this.prey = findPrey(mob);
            if (this.prey == null) {
                return AINodeResult.FAILURE;
            }
        }

        if (mob.getDistance(this.prey) <= 40.0F) {
            mob.drain(this.prey);
            this.prey = null;
            if (blackboard.mover.isCurrentlyMovingFor(this)) {
                blackboard.mover.stopMoving(mob);
            }
            return AINodeResult.SUCCESS;
        }

        if (blackboard.mover.isCurrentlyMovingFor(this)) {
            return AINodeResult.RUNNING;
        }

        final Mob target = this.prey;
        return this.moveToTileTask(target.getTileX(), target.getTileY(),
                (current, wanted) -> isAdjacent(current, wanted),
                path -> {
                    if (path.result.foundTarget) {
                        path.move(null);
                        return AINodeResult.RUNNING;
                    }
                    // Unreachable prey — across water, behind a wall. Drop it
                    // rather than stand still: the next scan finds another.
                    this.prey = null;
                    return null;
                });
    }

    private static boolean isAdjacent(Point current, Point wanted) {
        return Math.abs(current.x - wanted.x) <= 1 && Math.abs(current.y - wanted.y) <= 1;
    }

    /**
     * Something wild, alive, and outside the walls.
     *
     * <p>Hostiles are left alone (he is a settler, not a soldier — the ordinary
     * target finder already deals with those), humans are left alone, and
     * anything standing inside the settlement bounds is left alone. That last
     * test is the whole promise of the character: the player's own livestock is
     * safe precisely because it lives inside the flag's rectangle.
     */
    private static Mob findPrey(VampireSettlerMob mob) {
        Level level = mob.getLevel();
        if (level == null) {
            return null;
        }
        NetworkSettlementData settlement = mob.getSettlerSettlementNetworkData();
        return level.entityManager.mobs.streamInRegionsInRange(mob.x, mob.y, HUNT_RANGE)
                .filter(m -> m != mob && !m.removed() && !m.isHostile && !m.isHuman)
                .filter(m -> !(m instanceof PlayerMob))
                .filter(m -> settlement == null
                        || !settlement.isTileWithinBounds(m.getTileX(), m.getTileY()))
                .findFirst()
                .orElse(null);
    }
}
