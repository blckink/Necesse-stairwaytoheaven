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
     * How far from every player a live spawn must land, in pixels: past the
     * edge of the screen, and past every uplifted aggro range but the
     * Mistserpent's.
     *
     * THE COMPLAINT (12n, 2026-09-24): "Gegner sollen nicht immer in Bereich
     * fliegen der gecleart ist ... Sichtfeld wenn ich stehen bleibe ... wie auf
     * Oberwelt von Vanilla". VERIFIED [jar 1.3.3] the mod has no waves of its
     * own — every ambient hostile comes through vanilla's
     * {@code EntityManager.tickMobSpawning}, whose only distance rule is
     * {@code Mob.MOB_SPAWN_AREA = (700, 1400)}. Vanilla gets away with 700
     * because no vanilla chaser looks further than 640 (Ninja, Magechanic,
     * AncientSkeletonMage; Zombie 384, Skeleton 512), so a vanilla hostile is
     * born idle and has to wander into you — and on the surface it is born
     * only at night or in the dark, which is what makes a cleared field stay
     * cleared there by day. Here the day is never safe (see above), and the
     * band uplifts push aggro past 700 — Fen Wraith 1075, Tongue Plant 960,
     * Cinder Cantor 896, Forbidden Serpent 832, every Veil 512 to 716 — so they
     * were born already chasing and came straight in from the screen edge.
     *
     * 1100 is half a 1920x1080 screen's diagonal (1101) and above all of those,
     * so a spawn is out of sight AND idle. It still leaves the outer half of
     * vanilla's ring to spawn in; a draw inside it is a failed attempt, which
     * the engine retries at half cost — ambient pressure drops rather than
     * relocating, which is the other half of the complaint.
     */
    public static final int SIGHT_RANGE = 1100;

    /**
     * True when no player on the level is within {@link #SIGHT_RANGE} of the
     * spawn point. Only a live spawn tick has a {@code client}; generation and
     * {@code /skyreachstatus} pass null and keep asking the question they asked
     * before.
     */
    public static boolean outOfSight(Mob mob, ServerClient client, int targetX, int targetY) {
        if (client == null || mob.getLevel() == null) {
            return true;
        }
        // MOB_SPAWN_AREA is a mutable static; never ask for more than most of
        // the ring, or a narrowed ring would spawn nothing at all.
        final float range = Math.min(SIGHT_RANGE, Mob.MOB_SPAWN_AREA.maxSpawnDistance * 0.85F);
        return mob.getLevel().entityManager.players.streamArea(targetX, targetY, (int) range)
                .noneMatch(p -> !p.removed() && p.getDistance(targetX, targetY) < range);
    }

    /**
     * HostileMob's own chain with the ambient light check swapped for the
     * static one, and the spawn kept out of every player's sight. Everything
     * else — the mob's own location check and the four-hostiles-within-
     * eight-tiles cap — is left exactly as vanilla.
     */
    public static boolean daylightSpawn(Mob mob, Server server, ServerClient client, int targetX, int targetY) {
        return new MobSpawnLocation(mob, targetX, targetY)
                .checkStaticLightThreshold(client)
                .checkLocation((x, y) -> outOfSight(mob, client, x, y))
                .checkMobSpawnLocation()
                .checkMaxHostilesAround(4, 8, client)
                .validAndApply();
    }
}
