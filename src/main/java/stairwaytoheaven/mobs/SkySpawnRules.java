package stairwaytoheaven.mobs;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobSpawnLocation;

/**
 * Spawn rules for the Skyreach's residents.
 *
 * THE BUG THIS EXISTS FOR. {@code HostileMob.isValidSpawnLocation} calls
 * {@code checkLightThreshold}, which measures AMBIENT + static light against
 * the mob's {@code spawnLightThreshold} — 0 by default. On a non-cave level the
 * ambient is {@code worldEntity.getAmbientLightFloat() * 150}, i.e. 150 in
 * daylight, so `150 <= 0` fails and not one hostile can be placed anywhere
 * while the sun is up. Measured, not reasoned: {@code /skyreachstatus} reports
 * every sky hostile at `accepted lit=0/6 dark=6/6`, and every critter at 6/6 in
 * both — which is exactly the player report, "kein einziger Gegner ... nur
 * Critter".
 *
 * That rule is right for a vanilla island, where night comes to you and caves
 * are next door. The Skyreach is only ever surface, and it is somewhere you
 * travel TO: a hostile place that is empty in daylight reads as broken.
 *
 * WHY NOT VANILLA'S OWN FIX. Vanilla's idiom for a mob that belongs to a place
 * rather than to the night is
 * {@code spawnLightThreshold = new ModifierValue<>(..., 0).min(150, MAX)} —
 * PhantomMob, AshGolemMob, PirateMob, CryptBatMob and the slimes all do it. But
 * a threshold of 150 passes for ANY light, so a torch-lit camp would stop
 * protecting the player, and "Fackellicht muss schützen" is a rule this mod
 * already agreed to.
 *
 * {@code checkStaticLightThreshold} is the same check against
 * {@code getStaticLight} alone — placed lamps and torches, no daylight. So the
 * sky stays dangerous at noon and a lit base is still safe, which is both
 * things at once.
 */
public final class SkySpawnRules {

    private SkySpawnRules() {
    }

    /**
     * HostileMob's own chain with the ambient light check swapped for the
     * static one. Everything else — the mob's own location check and the
     * four-hostiles-within-eight-tiles cap — is left exactly as vanilla.
     */
    public static boolean daylightSpawn(Mob mob, Server server, ServerClient client, int targetX, int targetY) {
        return new MobSpawnLocation(mob, targetX, targetY)
                .checkStaticLightThreshold(client)
                .checkMobSpawnLocation()
                .checkMaxHostilesAround(4, 8, client)
                .validAndApply();
    }
}
