package stairwaytoheaven.realms.ghost;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedCollisionPlayerChaserWandererAI;
import necesse.entity.mobs.ai.behaviourTree.util.FlyingAIMover;
import necesse.entity.mobs.hostile.DeepCaveSpiritMob;
import necesse.inventory.lootTable.LootTable;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * Drifter — the Aftergarden's ordinary dead, going nowhere in particular until
 * somebody living walks past.
 *
 * <p><b>Vanilla base:</b> {@link DeepCaveSpiritMob}, art
 * {@code mobs/deepcavespirit}. Subclassing keeps everything that makes it the
 * right body: it FLIES (a {@code FlyingAIMover}, so the ectoplasm marsh is no
 * obstacle to it and the player cannot use water as a wall), it is translucent,
 * and it already drops ectoplasm in vanilla — which is the realm's universal
 * ghost resource, so the identity did not have to be invented.
 *
 * <h2>Tier</h2>
 * Ghost Realm row, no role discount: <b>2800 HP / 230 damage / 55 armour</b>
 * ({@code docs/BALANCE.md} §5). <b>VERIFIED [jar]</b>
 * {@code BiomeMissionIncursionData}'s cumulative arrays summed to incursion
 * tier 7 give +1.80 health and +0.75 damage against the mod's measured floor of
 * 1000 HP / 130 damage, i.e. 2800 and 227.5 taken as 230; armour has no
 * incursion array and is walked up the ladder by hand from the floor's 40.
 * Vanilla's own spirit is a deep-cave mob at 225 HP / 65 damage / 20 armour and
 * stays exactly that — only this subclass moves.
 *
 * <h2>Why init() is overridden</h2>
 * Vanilla's damage lives in the static fields {@code DeepCaveSpiritMob
 * .baseDamage} and {@code .incursionDamage}, which are SHARED with every
 * vanilla deep-cave spirit in the world — writing to them would re-tune the
 * game's own caves. So {@code super.init()} is called (it is what builds the
 * mob and, on other subclasses, does bookkeeping worth keeping) and only
 * {@code this.ai} is replaced afterwards, with vanilla's own tree rebuilt
 * against our damage: same 448 search, same 100 knockback, same 40s wander,
 * same {@code FlyingAIMover}.
 */
public class DrifterMob extends DeepCaveSpiritMob {

    /**
     * Ghost Realm row = <b>2800 HP</b> on Classic. The other four difficulties
     * reuse the ratios of the getter the mod's floor was measured from —
     * {@code AscendedGolemMob.MAX_HEALTH}'s 0.40 / 0.75 / 1.00 / 1.30 / 1.80
     * around Classic (VERIFIED [jar]). Vanilla's spirit is 225.
     */
    public static final MaxHealthGetter MAX_HEALTH =
            new MaxHealthGetter(1120, 2100, 2800, 3640, 5040);

    /** Ghost Realm row = <b>230 damage</b>. Vanilla's spirit hits for 65. */
    public static final GameDamage DAMAGE = new GameDamage(230.0F);

    /** Ghost Realm row = <b>55 armour</b>. Vanilla's spirit wears 20. */
    public static final int ARMOR = 55;

    public static LootTable lootTable = GhostLoot.standard();

    public DrifterMob() {
        super();
        // Registered in construction the way AscendedGolemMob registers its
        // own MAX_HEALTH: MobDifficultyChanges throws if it is touched after
        // init().
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
    }

    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(this,
                new ConfusedCollisionPlayerChaserWandererAI<>(null, 448, DAMAGE, 100, 40000),
                new FlyingAIMover());
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    /**
     * The Aftergarden's hostiles use the same static-light rule the sky and the
     * Veil use: a torch-lit camp still protects the player, but the realm does
     * not go quiet just because the player is carrying a light.
     */
    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkySpawnRules.daylightSpawn(this, server, client, targetX, targetY);
    }
}
