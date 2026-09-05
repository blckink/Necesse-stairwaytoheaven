package stairwaytoheaven.realms.ghost;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.PlayerChaserWandererAI;
import necesse.entity.mobs.ai.behaviourTree.util.FlyingAIMover;
import necesse.entity.mobs.hostile.PhantomMob;
import necesse.entity.projectile.PhantomBoltProjectile;
import necesse.inventory.lootTable.LootTable;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * Lantern Widow — she has been looking for him with that lamp for a long time,
 * and she has stopped being careful about who she shines it at.
 *
 * <p><b>Vanilla base:</b> {@link PhantomMob}, art {@code mobs/phantom}: a
 * floating shroud with a cold trail behind it that fires a bolt at range and
 * never lets you close. Subclassing keeps all three things that make it the
 * realm's ranged answer — the {@code FlyingAIMover} (so the marsh does not
 * stop her), the {@code Trail} built in {@code init()}, and the bolt itself.
 *
 * <h2>Tier</h2>
 * Ghost Realm row with the RANGED discount ({@code docs/BALANCE.md} §6:
 * HP x0.7, damage x0.85): 2800 x 0.7 = <b>1960 HP</b> and 230 x 0.85 = 195.5
 * snapped to the ladder's five-step grid = <b>195 damage</b>, at the row's full
 * <b>55 armour</b>. Vanilla's phantom is a night-surface mob at 450 HP / 115
 * damage / 30 armour and stays exactly that.
 *
 * <h2>Why init() is overridden</h2>
 * The bolt's damage is the STATIC field {@code PhantomMob.damage}, shared with
 * every phantom in the game — writing to it would re-tune vanilla's own nights.
 * So {@code super.init()} runs first (it is what builds the trail and registers
 * the bolt sound ability) and the tree is then rebuilt one for one against our
 * damage: same 512 search and shoot range, same 40s wander, same 60 speed / 768
 * range / 50 knockback bolt, same {@code changePositionConstantly} and the same
 * {@code FlyingAIMover}. Only the number changes.
 *
 * <p>Vanilla's bolt-sound ability is <b>not</b> re-fired here, and that is
 * deliberate rather than an omission: {@code playBoltSoundAbility} is a
 * {@code final} field on the vanilla class and is reachable, but firing it from
 * a rebuilt tree would double up with nothing — the ability is only ever run
 * from the tree vanilla is no longer using. The shot keeps the projectile's own
 * sound.
 */
public class LanternWidowMob extends PhantomMob {

    /**
     * Ghost Realm row x0.7 (ranged) = <b>1960 HP</b> on Classic; the other four
     * difficulties use {@code AscendedGolemMob.MAX_HEALTH}'s measured spread of
     * 0.40 / 0.75 / 1.00 / 1.30 / 1.80 (VERIFIED [jar]). Vanilla's phantom is
     * 450.
     */
    public static final MaxHealthGetter MAX_HEALTH =
            new MaxHealthGetter(784, 1470, 1960, 2548, 3528);

    /** Ghost Realm row x0.85 (ranged) = <b>195 damage</b>. Vanilla's is 115. */
    public static final GameDamage DAMAGE = new GameDamage(195.0F);

    /** Ghost Realm row = <b>55 armour</b>. Vanilla's phantom wears 30. */
    public static final int ARMOR = 55;

    public static LootTable lootTable = GhostLoot.standard();

    public LanternWidowMob() {
        super();
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
        // Vanilla's phantom sets spawnLightThreshold to .min(150, MAX) so it
        // can appear in ANY light on the night surface. That flag has to come
        // back off here, and this is the one line in the class that is a real
        // behaviour change rather than a number.
        //
        // The reason is a rule this mod already agreed to: "Fackellicht muss
        // schuetzen". Every mod hostile routes its spawn through
        // SkySpawnRules.daylightSpawn, which swaps vanilla's ambient-light gate
        // for checkStaticLightThreshold -- placed lamps and torches only, no
        // daylight -- so the realm stays dangerous in its permanent dark AND a
        // lit camp is still safe. With a threshold of 150 that check passes for
        // any light at all and the lit camp would stop meaning anything, so the
        // Widow would be the one hostile that walks into a torch-lit base.
        // Zero is Mob's own default (Mob.java:144).
        this.spawnLightThreshold = new necesse.engine.modifiers.ModifierValue<>(
                necesse.entity.mobs.buffs.BuffModifiers.MOB_SPAWN_LIGHT_THRESHOLD, 0);
    }

    @Override
    public void init() {
        super.init();
        PlayerChaserWandererAI<LanternWidowMob> chaserAI =
                new PlayerChaserWandererAI<LanternWidowMob>(null, 512, 512, 40000, true, false) {
                    @Override
                    public boolean canHitTarget(LanternWidowMob mob, float fromX, float fromY, Mob target) {
                        return true;
                    }

                    @Override
                    public boolean attackTarget(LanternWidowMob mob, Mob target) {
                        if (!mob.canAttack()) {
                            return false;
                        }
                        mob.attack(target.getX(), target.getY(), false);
                        PhantomBoltProjectile projectile = new PhantomBoltProjectile(
                                mob.getLevel(), mob, mob.x, mob.y, target.x, target.y,
                                60.0F, 768, DAMAGE, 50);
                        projectile.moveDist(15.0);
                        mob.getLevel().entityManager.projectiles.add(projectile);
                        return true;
                    }
                };
        chaserAI.playerChaserAI.chaserAINode.changePositionConstantly = true;
        this.ai = new BehaviourTreeAI<>(this, chaserAI, new FlyingAIMover());
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkySpawnRules.daylightSpawn(this, server, client, targetX, targetY);
    }

    /**
     * Bestiary face: it subclasses PhantomMob, so it wears that creature's
     * face in the journal too. {@code Mob.getMobIcon()} is overridable and
     * {@code FormJournalEntryComponent} asks the MOB rather than the registry,
     * so this needs no PNG of its own -- see {@link stairwaytoheaven.mobs.BorrowedMobIcon}
     * for why borrowing the face is the right answer and not a shortcut.
     */
    @Override
    public necesse.gfx.gameTexture.GameTexture getMobIcon() {
        return stairwaytoheaven.mobs.BorrowedMobIcon.from("phantom", super.getMobIcon());
    }

}
