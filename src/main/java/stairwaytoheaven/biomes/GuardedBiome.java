package stairwaytoheaven.biomes;

/**
 * A biome that can say who stands guard over the loot in it.
 *
 * <p>{@code docs/WORLD_DESIGN.md} A4.1, in the player's words:
 *
 * <blockquote><i>"sie sollen mal geballt kommen und ein Gebiet z.b bewachen wo
 * es loot gibt in anderen Ecken aber nicht dauernd angeflogen kommen"</i></blockquote>
 *
 * <p>and the design note's own instruction on how not to answer it: <b>"Do not
 * answer this by re-weighting spawn tables."</b> A
 * {@link necesse.level.maps.biomes.MobSpawnTable} is a per-tile weighted roll
 * with no notion of place, so it can only ever change WHICH thing walks up to
 * you, never WHETHER one does. The pack is therefore PLACED, at generation, on
 * the ground around the wreck or workshop it guards
 * ({@code SkyLevel.placeGuardPacks}), and this interface is where a biome
 * names its guards.
 *
 * <p>It is an interface rather than a method on a base class because the sky
 * and the Veil are two separate hierarchies — {@code SkyBiome extends Biome}
 * and {@code VeilBiome extends Biome}, with nothing between them — and both
 * layers own guarded places.
 *
 * <p>Two engine facts make a placed pack the right answer rather than an
 * expensive one, both <b>VERIFIED [jar]</b>:
 * {@code EntityManager.tickMobSpawning} counts only
 * {@code m.isHostile && m.canDespawn} against the player's spawn cap, so
 * persistent guards cost the ambient budget nothing; and because they never
 * despawn, a site stays guarded between visits rather than being repopulated by
 * whatever the weather happened to roll.
 */
public interface GuardedBiome {

    /** Who guards a site on this ground, or null for ground that guards nothing. */
    default Guard getGuard() {
        return null;
    }

    /**
     * A guard pack: the one that always stands there, and the ones filling in
     * around it.
     *
     * <p>Split in two because a guarded place needs a shape — a fight against
     * five of the same thing is a spawn table with extra steps. The
     * <b>anchors</b> are the reason the place is dangerous and are each placed
     * once; the <b>rabble</b> is why you cannot simply walk around the anchor,
     * and is drawn from at random until the pack reaches its size.
     */
    final class Guard {
        /** Placed once each, in order, nearest the site centre. */
        public final String[] anchors;
        /** Drawn from with replacement to fill the pack out. */
        public final String[] rabble;
        public final int minSize;
        public final int maxSize;

        public Guard(String[] anchors, String[] rabble, int minSize, int maxSize) {
            this.anchors = anchors;
            this.rabble = rabble;
            this.minSize = minSize;
            this.maxSize = maxSize;
        }

        /** The mob at index {@code i} of a pack of this shape. */
        public String memberAt(int i, float roll) {
            if (i < this.anchors.length) {
                return this.anchors[i];
            }
            return this.rabble[(int) (roll * this.rabble.length) % this.rabble.length];
        }
    }
}
