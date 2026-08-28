package stairwaytoheaven.livestock;

import java.awt.Point;
import java.util.ArrayList;

import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobSpawnLocation;
import necesse.entity.mobs.friendly.HusbandryMob;
import necesse.gfx.HumanGender;

/**
 * The two facts every farmable animal in this package needs and that the three
 * vanilla base classes do NOT provide, kept in one place because
 * {@link necesse.entity.mobs.friendly.CowMob}, {@code ChickenMob} and
 * {@code SheepMob} are siblings — there is no shared subclass to hang them on.
 *
 * <h2>1. A species needs both sexes, or it cannot breed at all</h2>
 *
 * <p>[jar 1.3.2] Breeding is driven by the MALE.
 * {@code HusbandryImpregnateWandererAI.HusbandryImpregnateAINode.tickNode}
 * only looks for a partner when {@code mob.canImpregnate()} — which is
 * {@code isGrown() && getGender() == MALE && tameness >= 1} — and then requires
 * {@code mob.canImpregnateMob(other)} on top of {@code other.canBirth()}
 * (FEMALE). {@code HusbandryMob.canImpregnateMob} returns <b>false</b>, so the
 * male half is always an override, and every vanilla one is a hard string test
 * on the vanilla partner's ID: {@code RamMob} answers only to {@code "sheep"},
 * {@code BullMob} only to {@code "cow"}, {@code RoosterMob} only to
 * {@code "chicken"}, {@code BoarMob} only to {@code "pig"}.
 *
 * <p>That is why a modded animal cannot simply be dropped into a surface pen
 * with a vanilla ram and expected to multiply — it never will. Vanilla's answer
 * is a second registered mob per species. Ours is one registered mob whose
 * <b>gender is per-instance</b>: rolled once on the server, saved, and sent in
 * the spawn packet so the client draws the right sheet. Nothing in the engine
 * treats gender as a property of the mob TYPE — the only readers are
 * {@code HusbandryMob.canBirth}/{@code canImpregnate} and
 * {@code SettlementHusbandryZone.tickJobs}, which sorts the animals inside a
 * husbandry zone into males/females/neutrals for the slaughter ratio, and all
 * three ask the instance.
 *
 * <h2>2. Livestock is invisible to a spawn table until it says otherwise</h2>
 *
 * <p>[jar 1.3.2] {@code MobChance.spawnMob} drops any mob whose
 * {@code isValidSpawnLocation} answers false, and {@code Mob}'s own
 * implementation is {@code return false}. Nothing between {@code SheepMob} and
 * {@code Mob} overrides it, so vanilla livestock can never be table-spawned —
 * vanilla places its sheep, cows and chickens from the island generator
 * ({@code ig.spawnMobHerds}) instead. Our sky biomes have no island generator,
 * so the animals implement the check themselves, exactly the way
 * {@code CritterMob} does (its whole body is
 * {@code new MobSpawnLocation(this, x, y).checkMobSpawnLocation().validAndApply()}),
 * plus a density cap so a player's own pen does not attract a wild herd.
 *
 * <p>They are permanent once placed: {@code Mob.canDespawn} is a plain field
 * that defaults to Java's {@code false} and is only set true by
 * {@code HostileMob} and {@code CritterMob}. A HusbandryMob therefore already
 * saves into its region and is never removed for being in an unloaded one, so
 * a herd the player walked past is still there on the way back.
 */
public final class SkyBreed {

    /** Not decided yet: a fresh mob on the client, or one before its first server tick. */
    public static final int UNSET = 0;
    public static final int FEMALE = 1;
    public static final int MALE = 2;

    /**
     * How many husbandry animals may already stand within
     * {@link #CROWD_TILE_RANGE} tiles before the sky refuses to add another.
     * Keeps wild herds out of a stocked pen and stops a much-travelled region
     * from silting up with permanent animals.
     */
    public static final int CROWD_LIMIT = 6;
    public static final int CROWD_TILE_RANGE = 14;

    private SkyBreed() {
    }

    /** Roll a sex, but only on the server and only if one is not already set. */
    public static int rollIfUnset(int current, Mob mob) {
        if (current != UNSET || mob.isClient()) {
            return current;
        }
        return GameRandom.globalRandom.nextBoolean() ? MALE : FEMALE;
    }

    public static HumanGender gender(int sex) {
        return sex == MALE ? HumanGender.MALE : HumanGender.FEMALE;
    }

    public static boolean isMale(int sex) {
        return sex == MALE;
    }

    /**
     * The spawn check every animal in this package shares: vanilla's own
     * location rules ({@code Mob.checkSpawnLocation} = not in liquid, not on a
     * solid tile, not indoors on a floor, not colliding) plus the herd cap.
     */
    public static boolean validPastureSpawn(Mob mob, necesse.engine.network.server.ServerClient client,
                                            int targetX, int targetY) {
        return new MobSpawnLocation(mob, targetX, targetY)
                .checkMobSpawnLocation()
                .checkMaxMobsAround(CROWD_LIMIT, CROWD_TILE_RANGE, m -> m instanceof HusbandryMob, client)
                .validAndApply();
    }

    /**
     * Give birth to a live young of the mother's own species.
     *
     * <p>This is {@code HusbandryMob.onImpregnated}'s behaviour, rewritten here
     * for {@link ThunderquillMob}: {@code ChickenMob} overrides
     * {@code onImpregnated} to set {@code nextEggIsFertilized} instead, and the
     * egg path that follows is hardcoded to vanilla — the AI node lays a
     * {@code new InventoryItem("egg")} and {@code EggNestObject}'s lay handler
     * does the same, so an {@code EggFoodConsumableItem} hatches
     * {@code "chicken"} or {@code "rooster"}
     * ({@code EggFoodConsumableItem.getHatchMobStringID}). A sky fowl bred
     * through that path would produce vanilla chickens.
     */
    public static void birthLiveYoung(HusbandryMob mother, HusbandryMob father) {
        Mob child = MobRegistry.getMob(mother.getRandomChildMobStringID(father), mother.getLevel());
        if (child == null) {
            return;
        }
        if (child instanceof HusbandryMob) {
            ((HusbandryMob) child).startBaby();
        }
        ArrayList<Point> free = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if (x == 0 && y == 0) {
                    continue;
                }
                Point point = new Point(mother.getX() + x * 4, mother.getY() + y * 4);
                if (!child.collidesWith(mother.getLevel(), point.x, point.y)) {
                    free.add(point);
                }
            }
        }
        Point spawn = GameRandom.globalRandom.getOneOf(free);
        if (spawn == null) {
            spawn = new Point(mother.getX(), mother.getY());
        }
        mother.getLevel().entityManager.addMob(child, (float) spawn.x, (float) spawn.y);
    }
}
