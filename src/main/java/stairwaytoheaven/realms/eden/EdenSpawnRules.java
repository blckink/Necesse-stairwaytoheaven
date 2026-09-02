package stairwaytoheaven.realms.eden;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobSpawnLocation;

/**
 * Spawn rules for the Garden of Eden's residents.
 *
 * <p>Eden inherits the Skyreach's problem exactly, and its solution. VERIFIED
 * [jar]: {@code HostileMob.isValidSpawnLocation} calls
 * {@code checkLightThreshold}, which measures AMBIENT + static light against
 * the mob's {@code spawnLightThreshold} (0 by default). On a non-cave level the
 * ambient is {@code worldEntity.getAmbientLightFloat() * 150}, i.e. 150 in
 * daylight, so {@code 150 <= 0} fails and not one hostile can be placed
 * anywhere while the sun is up. Eden is a level with no night worth speaking of
 * — it is heaven's garden — so under the vanilla rule it would be empty
 * forever.
 *
 * <p>{@code SkySpawnRules} already worked this out and its reasoning is not
 * repeated here: the fix is to swap the ambient check for
 * {@code checkStaticLightThreshold}, which measures placed lamps and torches
 * alone. The realm stays dangerous at noon and a lit camp is still safe.
 *
 * <p><b>Why Eden has its own copy rather than calling the sky's.</b> One line
 * of it differs, and it is the line that matters here:
 * {@code checkMaxHostilesAround(3, 8, client)} instead of vanilla's 4. Eden's
 * standard enemy is a 1500 HP serpent and its ambient rate is already the
 * lowest in the mod; the fourth simultaneous hostile buys nothing but the
 * feeling A4.1 exists to remove. Everything else — the mob's own location
 * check, the static-light gate — is the sky's chain unchanged.
 */
public final class EdenSpawnRules {

    /**
     * Vanilla's {@code HostileMob} chain with the ambient-light check swapped
     * for the static one, and the local hostile cap one lower than vanilla's.
     */
    public static boolean gardenSpawn(Mob mob, Server server, ServerClient client, int targetX, int targetY) {
        return new MobSpawnLocation(mob, targetX, targetY)
                .checkStaticLightThreshold(client)
                .checkMobSpawnLocation()
                .checkMaxHostilesAround(3, 8, client)
                .validAndApply();
    }

    private EdenSpawnRules() {
    }
}
