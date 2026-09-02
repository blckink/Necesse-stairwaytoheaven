package stairwaytoheaven.realms.crooked;

import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * The Striped Waste — Crooked Beyond's normal state, and the ground the rest of
 * the realm is cut out of.
 *
 * <p>Black-and-white stripes running to the horizon, interrupted by violet mud
 * and by the black runs where the path stops working
 * ({@link WrongWayTile}). It is the ground the Skyreach's Outlands rim has been
 * showing the player a scrap of since the Outlands shipped, at the size it was
 * always meant to be.
 *
 * <p><b>The realm's standard roster stands here.</b> The two golems and the
 * armadillo have been Crooked-branded since the art pass that gave them their
 * sheets; what changed with this realm is that they are finally on the row the
 * balance table always said they were ({@code SkyMobTiers.CROOKED_*}, 4000 /
 * 280 / 60) instead of on the vanilla numbers they inherited.
 */
public class StripedWasteBiome extends CrookedBiome {

    /**
     * Deliberately three entries and no more.
     *
     * <p>The engine will not place a fourth hostile within eight tiles anyway
     * (see {@link CrookedBiome}), so a wider table would only change WHICH of
     * the three arrives, at the cost of making each one less recognisable. All
     * three inherit {@code HostileMob.isValidSpawnLocation} through their
     * vanilla parents, so each entry can actually place — the check
     * {@code getRandomMob} runs before it draws.
     *
     * <p>They stay dark-spawners, as they have been since they shipped: the
     * realm follows the day/night cycle, so the Striped Waste is uneasy by day
     * and genuinely dangerous after dark. That is inherited behaviour and this
     * pass does not change it, because changing it is a balance decision and
     * this is a worldbuilding one.
     */
    public static final MobSpawnTable mobs = new MobSpawnTable()
            // The bruiser: plants itself, charges, and fires down the line it
            // warned you about. The cap is what stops two arriving together.
            .addLimited(55, "crookedgolem", 2, RANGE_STANDARD)
            // The charger: armour 60 until it commits, then it rolls.
            .addLimited(45, "crookedarmadillo", 2, RANGE_STANDARD)
            // The wall, and the rarest thing in the mix — an event rather than
            // a fight you have on the way past.
            .addLimited(20, "rarecrookedgolem", 1, RANGE_ELITE);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    /**
     * The realm's one catchable animal, and the only critter table in it.
     *
     * <p>{@code CritterMob} implements {@code isValidSpawnLocation}, so unlike
     * the Cloud Lamb this entry actually places — the lamb inherited
     * {@code Mob}'s {@code return false} and sat inert in a table for three
     * releases. Checked against the class, not assumed.
     */
    public static final MobSpawnTable critters = new MobSpawnTable()
            .addLimited(100, "stripebeetle", 3, RANGE_STANDARD);

    @Override
    public MobSpawnTable getCritterSpawnTable(Level level) {
        return critters;
    }

    /**
     * The Waste's guard.
     *
     * <p>Anchored by the Rare Crooked Golem with a Door Mimic beside it — the
     * mimic is the anchor that is not obviously an anchor, which is the whole
     * point of standing one next to a crate — and the ordinary golems and
     * armadillos filling in.
     */
    @Override
    public Guard getGuard() {
        return new Guard(
                new String[]{"rarecrookedgolem", "doormimic"},
                new String[]{"crookedgolem", "crookedarmadillo", "crookedgolem"},
                5, 7);
    }
}
