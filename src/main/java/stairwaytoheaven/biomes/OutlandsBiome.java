package stairwaytoheaven.biomes;

import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import stairwaytoheaven.SkyRegistry;

/**
 * The Beetle Outlands — the sky's first wrong place, and the only sky biome you
 * cannot reach by walking a short way.
 *
 * Where the other four sub-biomes are weather and geology, this one is a
 * symptom. It is cut out of them by {@link stairwaytoheaven.worldgen.SkyOutlands},
 * whose patch threshold falls with distance from the spire, so it is impossible
 * near home and ordinary in the far reaches.
 *
 * It is deliberately built from what the Veil already owned: the striped
 * beetlefreak ground, blackpeat, dead trees, ash bones, gloom shrooms and the
 * Gloom Shade. That layer was behind a Seance Circle almost nobody opened; the
 * art was drawn, registered and unseen. Nothing new had to be made for the sky
 * to stop being uniformly bright.
 *
 * The densest hostile population in the sky, on purpose. Wrong ground the
 * player can stroll across is just a different colour of grass.
 */
public class OutlandsBiome extends SkyBiome {

    /**
     * The Outlands hit at ASCENDED tier, and that is the whole point of them.
     *
     * <p>The player, after finishing incursion 10: <i>"mir ist langweilig!
     * alles zu einfach überall"</i>. They are right, and the numbers say why.
     * Everything this mod ships tops out around the Skystone Golem's 520 HP /
     * 70 damage, while vanilla's ordinary ascended-tier mobs — the ones a
     * post-incursion player fights as routine — sit at 1000 HP on Classic
     * ({@code AscendedGolemMob.MAX_HEALTH}) and 130 damage behind 40 armour
     * ({@code CrystalGolemMob}). Our hardest enemy is half of vanilla's
     * ordinary one. A region that only exists past 900 tiles from the spire
     * has no business being softer than the fen it borrowed its props from.
     *
     * <p><b>Vanilla behaviour, our art.</b> The three ascended entries used to
     * be vanilla's own mobs by string ID, wearing vanilla's crystal-cave sheets
     * in a biome that is neither a cave nor crystal. They are now
     * {@link stairwaytoheaven.mobs.CrookedGolemMob},
     * {@link stairwaytoheaven.mobs.RareCrookedGolemMob} and
     * {@link stairwaytoheaven.mobs.CrookedArmadilloMob} — subclasses that
     * inherit every number, ability and AI tree from those same vanilla mobs
     * and change nothing but the {@code GameTexture} they draw with — plus, on
     * the armadillo alone, the minimum-light floor that had to move onto its
     * one draw pass when vanilla's second one was dropped. The tier did not
     * move; only the sprite did. Spawn tables still resolve through the
     * one {@code MobRegistry}, and {@code HostileMob.isValidSpawnLocation} is
     * still the inherited implementation (light threshold, spawn location,
     * max-hostiles-around), so unlike the Cloud Lamb's inert entry these
     * actually place. Verified by reading the class, not assumed — that exact
     * trap cost this mod three releases.
     *
     * <p><b>What this deliberately changes about progression</b>: it gives the
     * sky a non-incursion source of ascended-tier fights, and of what they
     * drop. That is a real decision, not a side effect. It is gated behind
     * distance rather than behind a boss — the Outlands are 0.4% of land at
     * 900 tiles and only a quarter of it by 3200 — so a player meets one when
     * they have walked far enough to have earned it.
     *
     * <p><b>Night, mostly.</b> {@code checkLightThreshold} makes all three
     * dark-spawners, and the Skyreach follows the world's day/night cycle
     * ({@code isCave} is false). So the Outlands are uneasy by day and
     * genuinely dangerous after dark. That started as a consequence of using
     * vanilla's mobs directly rather than a choice, and it reads well enough to
     * keep. The three subclasses could now override
     * {@code isValidSpawnLocation} with {@code SkySpawnRules.daylightSpawn} the
     * way every arsenal enemy does — deliberately they do not, because that is a
     * balance change and the art pass that created them was not one.
     */
    public static final MobSpawnTable mobs = new MobSpawnTable()
            // Radii are PIXELS, not tiles -- see SkyBiome's header. Every cap
            // in this table used to pass 60/80/96, i.e. 1.9/2.5/3 tiles, so
            // none of them could bind. They now use the shared archetype
            // radii, which is what makes a cap a mix control at all.
            //
            // Weights changed with the endgame re-tier. The old comment here
            // said the mod's own mobs were "demoted to atmosphere" beside
            // vanilla's ascended cast; after the rebalance that is no longer
            // true -- the Gloom Shade is 2800 HP and the Fen Wraith 2800/230,
            // the same rung as crystalgolem. They are peers now, so they are
            // weighted as peers.
            //
            // The bruiser: 130 damage behind 40 armour, slow. Punishes standing
            // still, and the cap is what stops two from arriving together.
            .addLimited(50, "crookedgolem", 2, RANGE_STANDARD)
            // The wall. 1000 HP on Classic and the rarest thing in the mix, so
            // it is an event rather than a fight you have on the way past.
            .addLimited(25, "rarecrookedgolem", 1, RANGE_ELITE)
            // The charger: armour 60 until it commits, then speed 200 at you.
            .addLimited(45, "crookedarmadillo", 2, RANGE_STANDARD)
            // The Veil's own residents, now at the same tier rather than below
            // it. The shade is the ground's common enemy and keeps the widest
            // share; the wraith is its slow bruiser, the cantor its range.
            .addLimited(70, "gloomshade", 3, RANGE_STANDARD)
            .addLimited(40, "fenwraith", 2, RANGE_STANDARD)
            .addLimited(30, "cindercantor", 2, RANGE_RANGED);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    /**
     * Bridging a channel out here reclaims the striped ground, not cloudturf.
     *
     * Same reasoning the Beetlefreak Hollows shipped with: the wrongness is the
     * point, and a player who bridges a gap inside an Outland should not be
     * handing themselves a patch of the nice world back.
     */
    @Override
    public GameTile getUnderLiquidTile(Level level, int tileX, int tileY) {
        return SkyRegistry.beetlefreakTile;
    }

    /**
     * What an Outland crate holds: the Veil's materials, which have never had a
     * container to come out of because the Veil had no crates at all.
     */
    @Override
    public LootTable getCrateLootTable(Level level, int tileX, int tileY) {
        return new LootTable(
                LootItem.between("veilessence", 2, 5),
                ChanceLootItem.between(0.40F, "gloomshroom", 1, 3),
                ChanceLootItem.between(0.25F, "charwood", 2, 6),
                super.getCrateLootTable(level, tileX, tileY)
        );
    }
}
