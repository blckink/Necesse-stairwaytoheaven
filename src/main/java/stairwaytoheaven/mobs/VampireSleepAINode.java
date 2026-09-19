package stairwaytoheaven.mobs;

import necesse.entity.mobs.ai.behaviourTree.leaves.HumanSleepAINode;

/**
 * Sleep by day, work by night — the one node that decides it.
 *
 * <p>Vanilla's {@code shouldSleep} is {@code isHiding || isNight()}. Turning
 * that around is the whole of a nocturnal settler's rhythm; everything else
 * about going to bed — finding the bed, walking to it, holding it, waking on a
 * raid or a hit — is inherited untouched, so he beds down exactly the way the
 * rest of the settlement does, just twelve hours out of step.
 *
 * <p>{@code canSleep} gains one extra refusal: a vampire the player has taken
 * into the adventure party does not lie down at noon. Vanilla's own night lock
 * already treats party membership as "this settler is on the clock"
 * ({@code HumanMob.findJob}), but the sleep node does not ask — it only checks
 * {@code hasCommandOrders()}, and party membership sets none of the three
 * command fields (VERIFIED [jar]: guardPoint / followMob / attackMob). Without
 * this the player would walk an expedition into a dungeon and lose their
 * vampire to the nearest bed at sunrise.
 */
public class VampireSleepAINode<T extends VampireSettlerMob> extends HumanSleepAINode<T> {

    @Override
    public boolean shouldSleep(T mob) {
        if (mob.isOnStrike()) {
            return false;
        }
        if (mob.isHiding) {
            return true;
        }
        return !mob.isOnDuty();
    }

    @Override
    public boolean canSleep(T mob) {
        if (mob.adventureParty.isInAdventureParty()) {
            return false;
        }
        return super.canSleep(mob);
    }
}
