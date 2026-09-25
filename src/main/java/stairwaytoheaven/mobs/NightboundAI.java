package stairwaytoheaven.mobs;

import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.HumanSleepAINode;
import necesse.entity.mobs.ai.behaviourTree.trees.HumanAI;

/**
 * The settler brain with its night turned around.
 *
 * <p>Vanilla's {@code HumanAI} builds a fixed list of children in its own
 * constructor, so a nocturnal settler cannot be had by passing arguments. Two
 * of those children decide what "night" means, and both are reachable from a
 * subclass without reflection:
 *
 * <ul>
 * <li>{@code HumanSleepAINode} — replaced in place, because
 *     {@code CompositeTypedAINode.children} is a {@code protected ArrayList}.
 *     The replacement sits at the same index, which matters: the node order in
 *     a selector IS the priority order, and sleeping deliberately ranks below
 *     jobs and above idle wandering.</li>
 * <li>{@code wandererAINode.hideInside} — a {@code public Predicate} field.
 *     Vanilla's sends an idle settler indoors while it is dark; left alone, a
 *     night settler would spend its whole shift standing in the house.</li>
 * </ul>
 *
 * <p>Split out of {@link VampireAI} so that a resident Dorian has turned
 * ({@link SkySettlerMob#isNightbound}) runs the same inverted schedule
 * without his hunt.
 */
public class NightboundAI<T extends SkySettlerMob> extends HumanAI<T> {

    public NightboundAI(int searchDistance, boolean attackHostiles, boolean ignoreHiding,
            int wanderFrequency) {
        super(searchDistance, attackHostiles, ignoreHiding, wanderFrequency);

        AINode<T> vanillaSleep = null;
        for (int i = 0; i < this.children.size(); i++) {
            if (this.children.get(i) instanceof HumanSleepAINode) {
                vanillaSleep = this.children.get(i);
                this.children.set(i, new VampireSleepAINode<>());
                break;
            }
        }
        if (vanillaSleep == null) {
            // A vanilla refactor that renames or drops the node would otherwise
            // ship a vampire who sleeps at night like everyone else, which is
            // the kind of fault that only shows up three playtests later.
            throw new IllegalStateException(
                    "HumanAI no longer contains a HumanSleepAINode to invert");
        }

        // Idle behaviour: indoors while off duty, out in the open on shift.
        // The test is isOnDuty() rather than plain "is it night", so that all
        // three "is he up" decisions — sleeping, working, standing around —
        // answer the same way. With a bare night test he would drift into the
        // nearest house between jobs while the player had him out by day in
        // the adventure party, which is exactly the case the party exception
        // exists to allow.
        this.wandererAINode.hideInside = mob ->
                !mob.isOnDuty() || mob.getLevel().isCave || mob.isHiding;
    }
}
