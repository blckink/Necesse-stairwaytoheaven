package stairwaytoheaven.mobs;

import necesse.entity.mobs.ai.behaviourTree.trees.HumanAI;

/**
 * Vanilla's settler brain with one node added: {@link DoctorReviveAINode}.
 *
 * <p>It goes in right before {@code wanderHomeLowHealthAINode}, i.e. after the
 * mission-idle, interacting and player-command nodes. The order of a selector
 * IS its priority, so a player's standing order still wins, and a fallen
 * settler beats running home at low health, fighting and working.
 */
public class DoctorAI<T extends DoctorHumanMob> extends HumanAI<T> {

    public DoctorAI(int searchDistance, boolean attackHostiles, boolean ignoreHiding,
            int wanderFrequency) {
        super(searchDistance, attackHostiles, ignoreHiding, wanderFrequency);
        this.addChildBefore(this.wanderHomeLowHealthAINode, new DoctorReviveAINode<>());
    }
}
