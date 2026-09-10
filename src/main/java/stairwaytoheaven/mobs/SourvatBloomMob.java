package stairwaytoheaven.mobs;

import java.awt.Point;

import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.leaves.TargetFinderAINode;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedPlayerChaserWandererAI;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.hostile.StabbyBushMob;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;

/**
 * The Sourvat Bloom — what grew out of the burst vat in the Grange Cellar and
 * ate the Skywatch's culture.
 *
 * <p>{@code chapter-01-skyreach-cast.md} §2: <i>"ambusher — reads as scenery
 * until you are close, then bursts and keeps releasing a swarm of floating
 * adds"</i>, built on <i>"vanilla's ambushing plant ({@code StabbyBushMob})"</i>
 * with the adds on {@link VatlingMob}. It sits at (7,8) of §2.13's plan,
 * between the four vats, and is placed by {@code SkyLandmarkPois} alone — it
 * never spawns on the open map.
 *
 * <h2>The art: none</h2>
 * §1.3's pattern. Nothing here overrides {@code addDrawables} or
 * {@code spawnDeathParticles}, so the Bloom is drawn out of
 * {@code MobRegistry.Textures.stabbyBush} by vanilla's own code — a bush that
 * reads as scenery is exactly the silhouette the ambush needs — and
 * {@link #getMobIcon} gives the bestiary the stabby bush's face.
 *
 * <h2>Why the frenzy is HELD instead of inherited</h2>
 * VERIFIED [jar]: a Stabby Bush gains a stack of
 * {@code BuffRegistry.STABBY_BUSH_FRENZY_BUFF} every time it is hit
 * ({@code doWasHitLogic}, StabbyBushMob.java:97) and its {@code serverTick}
 * (:120) blows the mob up and removes it the moment those stacks reach the
 * buff's maximum. On a 100 HP bush that is a fair trade. On a boss it is a
 * boss that kills itself after a handful of swings, before the fight has
 * started — so {@link #doWasHitLogic} clears the stacks one short of the cap
 * and RELEASES A WAVE OF VATLINGS instead. The burst the brief asks for still
 * happens; it just happens repeatedly, which is what "keeps releasing a swarm"
 * means, and the Bloom stays alive to keep doing it.
 *
 * <p>The wave is capped at {@link #MAX_VATLINGS} live adds inside
 * {@link #SWARM_TILES} tiles. Without a cap a player who kites the fight would
 * come back to a cellar with a hundred flakes in it.
 *
 * <h2>Tier</h2>
 * The Skyreach's ELITE rung, like the Tollwright, and for the same reason: it
 * is a once-per-world place's boss. Its speed stays vanilla's 0 — it is rooted,
 * and the swarm is its reach. Vanilla's bush is 100 HP / 20 damage and stays
 * exactly that.
 */
public class SourvatBloomMob extends StabbyBushMob {

    /** Skyreach elite rung on Classic, with vanilla's own five-difficulty ratios. */
    public static final MaxHealthGetter MAX_HEALTH = SkyMobTiers.scaled(
            SkyMobTiers.hp(SkyMobTiers.SKYREACH_HP, SkyMobTiers.ROLE_ELITE_HP));

    /** The elite role takes the band's damage unchanged. Vanilla builds 20 inline in its AI. */
    public static final GameDamage DAMAGE = new GameDamage(SkyMobTiers.SKYREACH_DAMAGE);

    /** The band's armour. Vanilla's bush wears none. */
    public static final int ARMOR = SkyMobTiers.SKYREACH_ARMOR;

    /**
     * How far it notices somebody: vanilla's 576 x1.25 = 720 (docs/BALANCE.md
     * §10). Its melee reach stays vanilla's 64, which is what makes it an
     * ambush rather than a turret.
     */
    public static final int AGGRO_RANGE = SkyMobTiers.aggro(576, SkyMobTiers.UPLIFT_SKYREACH_AGGRO);

    /** Live adds allowed around it at once. */
    public static final int MAX_VATLINGS = 5;
    /** How many it lets out per burst. */
    public static final int VATLINGS_PER_BURST = 2;
    /** The radius, in tiles, the cap is counted over — the cellar is 21 wide. */
    public static final int SWARM_TILES = 14;

    /**
     * §2's drop column: <i>"the Mother (once), Wild Skyyeast (brewing
     * reagent), Spent Grain"</i>. The Mother is reward 4 and is guaranteed —
     * it is Halda's recruit key, so a chance roll would be a world in which
     * she can never be hired. Wild Skyyeast and Spent Grain are the brief's own
     * unbuilt art (they belong with the Fermentation Vat), so the brewhouse
     * pays in what the mod really has instead.
     */
    public static LootTable lootTable = new LootTable(
            new LootItem("themother"),
            LootItem.between("cloudberry", 4, 9),
            new ChanceLootItemList(0.55F, LootItem.between("windsilk", 2, 4)));

    public SourvatBloomMob() {
        super();
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
    }

    /**
     * Vanilla's tree rebuilt one-for-one against {@link #DAMAGE}: the same 576
     * search, 64 reach, no wander, and the same "players and humans, plus
     * whoever hit me" target filter. Its own {@code attackTarget} adds a frenzy
     * stack on every landed hit; that is left out here, because
     * {@link #doWasHitLogic} is the one place this mob's frenzy is managed and
     * two writers to the same cap is how the suicide would come back.
     */
    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(this,
                new ConfusedPlayerChaserWandererAI<SourvatBloomMob>(null, AGGRO_RANGE, 64, -1, false, false) {
                    @Override
                    public boolean attackTarget(SourvatBloomMob mob, Mob target) {
                        if (!mob.canAttack()) {
                            return false;
                        }
                        mob.attack(target.getX(), target.getY(), false);
                        target.isServerHit(DAMAGE, mob.dx, mob.dy, 15.0F, mob);
                        return true;
                    }

                    @Override
                    public GameAreaStream<Mob> streamPossibleTargets(SourvatBloomMob mob, Point base,
                            TargetFinderDistance<SourvatBloomMob> distance) {
                        return TargetFinderAINode.streamPlayersAndHumans(mob, base, distance)
                                .filter(m -> m.isPlayer || mob.isAttacker(m));
                    }
                });
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    /**
     * Vanilla adds the frenzy stack; this holds it one below the cap and lets
     * the swarm out instead of the explosion.
     */
    @Override
    protected void doWasHitLogic(MobWasHitEvent event) {
        super.doWasHitLogic(event);
        ActiveBuff frenzy = this.buffManager.getBuff(BuffRegistry.STABBY_BUSH_FRENZY_BUFF);
        if (frenzy == null || frenzy.getStacks() < frenzy.getMaxStacks() - 1) {
            return;
        }
        this.buffManager.removeBuff(BuffRegistry.STABBY_BUSH_FRENZY_BUFF, true);
        this.buffManager.forceUpdateBuffs();
        this.burst();
    }

    /** Server side only: spawning on both ends is how you get two of everything. */
    private void burst() {
        if (!this.isServer() || this.getLevel() == null) {
            return;
        }
        long live = this.getLevel().entityManager.mobs
                .streamInRegionsInTileRange(this.getTileX(), this.getTileY(), SWARM_TILES)
                .filter(m -> m instanceof VatlingMob && !m.removed())
                .count();
        for (int i = 0; i < VATLINGS_PER_BURST && live + i < MAX_VATLINGS; i++) {
            Mob vatling = MobRegistry.getMob("vatling", this.getLevel());
            if (vatling == null) {
                return;
            }
            // A flier: it leaves the vat it grew in and finds its own way out.
            this.getLevel().entityManager.addMob(vatling,
                    this.x + (i % 2 == 0 ? 24 : -24), this.y + (i < 2 ? -24 : 24));
        }
    }

    /** The face of the creature whose body it wears. See {@link BorrowedMobIcon}. */
    @Override
    public GameTexture getMobIcon() {
        return BorrowedMobIcon.from("stabbybush", super.getMobIcon());
    }
}
