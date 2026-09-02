package stairwaytoheaven.arsenal;

import java.util.List;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.mobAbilityLevelEvent.RuneSpiritPoolEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedCollisionPlayerChaserWandererAI;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.entity.mobs.MobDrawable;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import java.awt.Point;
import necesse.entity.mobs.hostile.SpiritGhoulMob;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * Fen Wraith — the Gloomfen's own dead, wading the murkwater and leaving a
 * burning wake behind them.
 *
 * <p><b>Vanilla base:</b> {@link SpiritGhoulMob}, art {@code mobs/spiritghoul}
 * (a translucent teal-green wraith; measured against the mod's own
 * {@code GHOSTFLAME} ramp it is already the Veil's colour and needs no shift).
 * Subclassing keeps the whole reason to pick it: a slow (speed 15) armoured
 * chaser that swims, and a {@code serverTick} that drops a
 * {@code RuneSpiritPoolEvent} every 16 units it runs on dry land — so the fen
 * fills up with pools behind it and standing still is not an option.
 *
 * <h2>Tier</h2>
 * The ladder and the incursion measurement behind it are written out once, in
 * {@link RimeSentryMob} — the mob that sits on its floor. In short: incursion
 * tier 1 applies no multiplier at all (VERIFIED [jar]:
 * {@code BiomeMissionIncursionData}'s cumulative per-tier arrays both begin
 * {@code 0.0F}), which pins the Skyreach floor at 1000 HP / 130 damage / 40
 * armour, and summing those same arrays to <b>incursion tier 7</b> gives +1.80
 * health / +0.75 damage — 1000 x 2.80 = 2800 and 130 x 1.75 = 227.5, taken as
 * 230. That is the Ghost Realm's rung: <b>2800 HP / 230 damage / 55 armour</b>
 * (armour has no incursion array and is walked up the ladder by hand).
 *
 * <p>The Fen Wraith is the Ghost Realm's standard bruiser, so it takes the rung
 * whole with no role discount. Vanilla's own ghoul is a surface-cave mob at
 * 275 HP / 52 damage / 20 armour and stays exactly that; only this subclass
 * moves.
 *
 * <p><b>Why {@code init()} and {@code serverTick()} are overridden.</b> Neither
 * of the ghoul's two damage numbers is reachable by assignment: the melee
 * damage is a {@code new GameDamage(52.0F)} local built inside
 * {@code SpiritGhoulMob.init}, and the pool damage is a
 * {@code new GameDamage(38.0F)} local built inside its {@code serverTick}
 * (VERIFIED [jar]). Both are therefore re-declared here — see
 * {@link #serverTick()} for how the pool is taken over without vanilla
 * double-spawning one.
 *
 * <p>Vanilla's loot table is surface-cave loot (coins, amber, dryad saplings);
 * ours is replaced with Veil materials.
 *
 * <h2>Our own art, on vanilla's body</h2>
 * As of 2026-09-02 this no longer wears {@code mobs/spiritghoul}. The player
 * supplied a Spirit Wraith sheet as CUT FRAMES and
 * {@code tools/resheet_mob.py} composed it onto the 384x320 grid; it ships as
 * {@code mobs/fenwraith.png} and {@link #addDrawables} samples it.
 *
 * <p>The override exists because {@code SpiritGhoulMob.addDrawables} reads its
 * texture from the static {@code MobRegistry.Textures.spiritGhoul} inline, and
 * {@code Mob} exposes no per-instance texture hook (VERIFIED [jar]). Assigning
 * into that static would repaint vanilla's own ghouls in vanilla's own caves,
 * so the draw is ported instead — same offsets (-32 / -36), same animation
 * frame, same bobbing, same sinking amount, same swim mask, same glow options
 * and enemy tracker. Only the {@link GameTexture} is ours.
 *
 * <p>It deliberately does NOT call {@code super.addDrawables}: that IS vanilla's
 * body draw and would put the ghoul's sprite back on top of ours. Nothing is
 * lost — the {@code super} call at the top of vanilla's own version reaches
 * {@code Mob.addDrawables}, whose body is empty; health and status bars are
 * added by {@code Mob.addDrawablesLoop} around it (VERIFIED [jar], the same
 * reasoning written out in {@code mobs/CrookedGolemMob}).
 */
public class FenWraithMob extends SpiritGhoulMob {

    /**
     * Our sheet, filled by {@code SkyMobs.loadTextures} on the client only.
     * It stays null on a dedicated server, which never draws, hence the guard.
     */
    public static GameTexture texture;

    /**
     * Vanilla's {@code SpiritGhoulMob.addDrawables}, ported line for line with
     * our sheet in place of {@code MobRegistry.Textures.spiritGhoul}.
     */
    @Override
    public void addDrawables(List<MobDrawable> list, OrderableDrawables tileList,
            OrderableDrawables topList, Level level, int x, int y,
            TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        if (texture == null) {
            return;
        }
        GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
        int drawX = camera.getDrawX(x) - 32;
        int drawY = camera.getDrawY(y) - 36;
        Point sprite = this.getAnimSprite(x, y, this.getDir());
        drawY += this.getBobbing(x, y);
        drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y))
                .getMobSinkingAmount(this);
        final MaskShaderOptions swimMask =
                this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
        final DrawOptions body = texture.initDraw()
                .sprite(sprite.x, sprite.y, 64)
                .addMaskShader(swimMask)
                .startGlowOptions(level, (long) this.getID())
                .light(light)
                .applyEnemyTracker(this, perspective)
                .pos(drawX, drawY);
        list.add(new MobDrawable() {
            @Override
            public void draw(TickManager tickManager) {
                swimMask.use();
                body.draw();
                swimMask.stop();
            }
        });
    }

    /**
     * Ghost Realm rung (incursion tier 7) = 1000 x 2.80 = <b>2800 HP</b> on
     * Classic, no role discount. The other four difficulties reuse the ratios of
     * the getter the floor was measured from —
     * {@code AscendedGolemMob.MAX_HEALTH}'s 0.40 / 0.75 / 1.00 / 1.30 / 1.80
     * around Classic (VERIFIED [jar]). Vanilla's ghoul is 275.
     */
    public static final MaxHealthGetter MAX_HEALTH = new MaxHealthGetter(1120, 2100, 2800, 3640, 5040);

    /**
     * Ghost Realm rung (incursion tier 7) = 130 x 1.75 = 227.5, snapped onto the
     * ladder's five-step damage grid = <b>230</b>; the 130 is
     * {@code CrystalGolemMob.damage} (VERIFIED [jar]). Vanilla builds 52 as a
     * local inside {@code SpiritGhoulMob.init}.
     */
    public static final GameDamage DAMAGE = new GameDamage(230.0F);

    /**
     * The burning wake. Vanilla's pool is {@code GameDamage(38)} against a melee
     * {@code GameDamage(52)} — 0.73 of the mob's own hit (VERIFIED [jar]). Held
     * at that same ratio against our 230, the trail is 168, so dawdling in the
     * wake stays the mistake it was designed to be instead of a rounding error.
     */
    public static final GameDamage POOL_DAMAGE = new GameDamage(168.0F);

    /** Vanilla's cadence, unchanged: one pool per 16 units run on dry land. */
    public static final double POOL_SPAWN_RUN_DISTANCE = 16.0;

    /** Vanilla's linger, unchanged. */
    public static final float POOL_LINGER_SECONDS = 4.0F;

    /**
     * Ghost Realm rung = <b>55 armour</b>. There is no armour array in
     * {@code BiomeMissionIncursionData} (VERIFIED [jar]: it scales health and
     * damage only), so the ladder walks armour up by hand from the floor's 40 —
     * {@code CrystalGolemMob}'s {@code setArmor(40)}, matched by the rolling
     * {@code CrystalArmadillo} and {@code AscendedBatMob}. Vanilla's ghoul
     * wears 20.
     */
    public static final int ARMOR = 55;

    /**
     * Veil essence is what a shade is made of and what the mod already drops
     * from the Gloom Shade; the cinder pearl is the Stormdisc's burning hub.
     *
     * <p>Quantities are the Skyreach baseline (1-2 apiece) x the Ghost Realm's
     * drop-value multiplier of 1.9, rounded to whole items: 1-2 becomes 2-4.
     * The chances are unchanged — the rung is paid in stack size, so a kill
     * still sometimes drops nothing and the drop still feels earned.
     */
    public static LootTable lootTable = new LootTable(
            new ChanceLootItemList(0.65F, LootItem.between("veilessence", 2, 4)),
            new ChanceLootItemList(0.35F, LootItem.between("cinderpearl", 2, 4)));

    public FenWraithMob() {
        super();
        // Registered in construction the way AscendedGolemMob registers its own
        // MAX_HEALTH: MobDifficultyChanges throws if it is touched after init().
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
    }

    @Override
    public void init() {
        super.init();
        // Same tree vanilla builds (768 search, 50 knockback, 40s wander), only
        // against our own damage — SpiritGhoulMob.init's GameDamage(52) is a
        // local with no seam to write through.
        this.ai = new BehaviourTreeAI<>(this,
                new ConfusedCollisionPlayerChaserWandererAI<>(null, 768, DAMAGE, 50, 40000));
    }

    /**
     * Takes over the burning wake so it lands at {@link #POOL_DAMAGE} instead of
     * vanilla's 38.
     *
     * <p>{@code SpiritGhoulMob.serverTick} spawns its pool from a
     * {@code GameDamage} it constructs inline, so the only way to re-tune it is
     * to get there first: spawn ours, then move
     * {@code distanceRanSinceLastPoolSpawn} (vanilla's own {@code protected}
     * marker) up to the current distance. When {@code super.serverTick()} then
     * runs its identical check it measures a delta of exactly zero, so it never
     * fires and the pool is never doubled.
     *
     * <p>That zero is not a near miss but a guarantee. <b>VERIFIED [jar]:</b>
     * {@code distanceRan} is written in exactly one place,
     * {@code Mob.tickMovement}, and {@code EntityManager} runs movement as a
     * separate pass from the server tick — {@code frameTick} does
     * {@code mobs.frameTick(tickManager, Mob::tickMovement)} while
     * {@code serverTick} does {@code mobs.serverTick(mob -> mob.serverTick())}.
     * Nothing between our write and vanilla's read can move the mob, whatever
     * its speed or the cadence threshold.
     */
    @Override
    public void serverTick() {
        if (!this.inLiquid()) {
            double distanceRan = this.getDistanceRan();
            if (distanceRan - this.distanceRanSinceLastPoolSpawn > POOL_SPAWN_RUN_DISTANCE) {
                this.getLevel().entityManager.events.add(new RuneSpiritPoolEvent(
                        this, (int) this.x, (int) this.y, GameRandom.globalRandom,
                        POOL_DAMAGE, POOL_LINGER_SECONDS));
                this.distanceRanSinceLastPoolSpawn = distanceRan;
            }
        }
        super.serverTick();
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    /** The Veil's hostiles use the same static-light rule as the sky's. */
    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkySpawnRules.daylightSpawn(this, server, client, targetX, targetY);
    }
}
