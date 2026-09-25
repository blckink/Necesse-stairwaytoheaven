package stairwaytoheaven.mobs;

/**
 * Dorian's brain: {@link NightboundAI} — the settler tree with its night
 * turned around — plus his hunt.
 *
 * <p>The hunt is added as a new child rather than a replacement, directly
 * before the sleep node: while the settlement has work for him he works, and
 * only an idle night sends him over the wall.
 */
public class VampireAI<T extends VampireSettlerMob> extends NightboundAI<T> {

    public VampireAI(int searchDistance, boolean attackHostiles, boolean ignoreHiding,
            int wanderFrequency) {
        super(searchDistance, attackHostiles, ignoreHiding, wanderFrequency);

        this.addChildBefore(this.children.stream()
                .filter(c -> c instanceof VampireSleepAINode)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("sleep node vanished")),
                new VampireHuntAINode<>());
    }
}
