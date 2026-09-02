package stairwaytoheaven.veil;

import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.staticBuffs.Buff;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;

/**
 * The fog itself — {@code docs/WORLD_DESIGN.md} §8's "permanent fog effect".
 *
 * <h2>Why the fog is a second buff and not part of Soul Exposure</h2>
 *
 * §9 is explicit about what happens after the Veil Mark:
 *
 * <blockquote>Soul Exposure is disabled. <b>The fog stays visible</b>, and
 * parts locally around the player when crossing — so the border stays
 * legible.</blockquote>
 *
 * A player who has earned the Mark must still SEE where the Veil begins, or the
 * one landmark that divides the world stops existing the moment it stops
 * hurting. So the atmosphere and the punishment are two different things:
 * {@link VeilWorldData} applies this one to everybody inside the region, and
 * {@link SoulExposureBuff} only to those without the Mark.
 *
 * <p>It is also what makes the border readable BEFORE the debuff bites. A
 * player walking out from Steinfeld sees the fog close in a moment before the
 * first stack lands, which is the difference between a designed wall and an
 * invisible damage zone.
 *
 * <h2>Invisible, unsaved, short</h2>
 *
 * No HUD icon (there is nothing for the player to act on — the fog is scenery,
 * and the debuff has its own icon), no modifiers, and a two-second duration
 * that {@link VeilWorldData} refreshes every second. That is deliberate
 * belt-and-braces: if the server stops refreshing for any reason — the player
 * left, disconnected, died, the region check changed — the fog lifts on its own
 * within two seconds rather than living on as a permanently hazed screen that
 * only a relog can clear. A {@code isPassive} marker buff would have had
 * exactly that failure mode.
 *
 * <h2>The art is vanilla's</h2>
 *
 * {@code GameResources.fogParticles} — the game's own {@code particles/fog}
 * sheet, four 32x16 frames — spawned the way {@code HuginStatueObjectEntity}
 * spawns its fog (jar 1.3.2, HuginStatueObjectEntity.java:72-89). No new art,
 * recorded in {@code docs/VANILLA_ASSET_MAP.md} §1.3.
 */
public class VeilFogBuff extends Buff {

    /** Registered ID. Invisible, so this never reaches a player as text. */
    public static final String ID = "veilfog";

    /** Vanilla particle sheet this borrows. Recorded in VANILLA_ASSET_MAP §1.3. */
    public static final String BORROWED_PARTICLES = "particles/fog";

    /** How far from the player fog may appear, in tiles. */
    private static final int FOG_RADIUS_TILES = 7;

    /** Spawn attempts per client tick, and the chance each one takes. */
    private static final int FOG_ATTEMPTS = 3;
    private static final float FOG_CHANCE = 0.06F;

    /** Milliseconds a fog wisp lives. Vanilla's own figure for this sheet. */
    private static final int FOG_LIFETIME_MS = 3000;

    public VeilFogBuff() {
        this.isVisible = false;   // scenery, not a status the player acts on
        this.isPassive = false;   // must expire on its own if the server stops refreshing
        this.canCancel = false;
        this.shouldSave = false;  // the world decides every tick; a saved copy can only be stale
        this.isImportant = false;
    }

    @Override
    public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
        // No modifiers. The fog does not do anything to you; Soul Exposure does.
    }

    /**
     * Client-side atmosphere.
     *
     * <p>Only for the client's OWN player, the same guard
     * {@code SwampSporesBuff.clientTick} uses (SwampSporesBuff.java:31-32):
     * everyone in the fog is rendering their own, so drawing other players'
     * wisps as well would multiply the particle count by the player count for
     * no visible gain.
     *
     * <p>This method never runs on a dedicated server — {@code BuffManager}
     * has separate {@code serverTick} and {@code clientTick} loops
     * (BuffManager.java:220 and :257) — which is what makes it safe for a
     * server-loaded class to touch {@code GameResources}.
     */
    @Override
    public void clientTick(ActiveBuff buff) {
        super.clientTick(buff);
        Mob owner = buff.owner;
        if (!owner.isClient() || owner.getClient() == null || owner.getClient().getPlayer() != owner) {
            return;
        }
        if (!owner.isVisible()) {
            return;
        }
        for (int i = 0; i < FOG_ATTEMPTS; i++) {
            if (!GameRandom.globalRandom.getChance(FOG_CHANCE)) {
                continue;
            }
            float px = owner.x + GameRandom.globalRandom.getFloatBetween(
                    -FOG_RADIUS_TILES * 32.0F, FOG_RADIUS_TILES * 32.0F);
            float py = owner.y + GameRandom.globalRandom.getFloatBetween(
                    -FOG_RADIUS_TILES * 32.0F, FOG_RADIUS_TILES * 32.0F);
            final boolean mirror = GameRandom.globalRandom.nextBoolean();
            owner.getLevel().entityManager
                    .addParticle(px, py, Particle.GType.COSMETIC)
                    .sprite(GameResources.fogParticles.sprite(GameRandom.globalRandom.nextInt(4), 0, 32, 16))
                    .fadesAlpha(0.4F, 0.4F)
                    .size((options, lifeTime, timeAlive, lifePercent) -> {
                    })
                    .height(30.0F)
                    .dontRotate()
                    .movesConstant(GameRandom.globalRandom.getFloatBetween(2.0F, 5.0F)
                            * GameRandom.globalRandom.getOneOf(1.0F, -1.0F), 0.0F)
                    .modify((options, lifeTime, timeAlive, lifePercent) -> options.mirror(mirror, false))
                    .lifeTime(FOG_LIFETIME_MS);
        }
    }
}
