package stairwaytoheaven.realms.hell.mobs;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedCollisionPlayerChaserWandererAI;
import necesse.entity.mobs.hostile.AncientArmoredSkeletonMob;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * The Infernal Clerk — Hell's standard resident, and the first thing §17's
 * Infernal Fringe puts in front of a player.
 *
 * <p>{@code docs/WORLD_DESIGN.md} §18 is explicit that hell does not begin
 * with a barrier: <i>"Hell begins with an authority."</i> The Clerk is that
 * authority in its everyday form — something in plate that has stood at a
 * counter long enough to be part of the furniture, and objects to you being
 * on this side of it.
 *
 * <h2>Body</h2>
 * Vanilla's {@code AncientArmoredSkeletonMob}, the same body Steinfeld's Stone
 * Mourner already borrows, and for the same reason: it is the game's own
 * slow, heavily armoured humanoid melee and it is the shape a clerk in brass
 * plate reads as. <b>The sheet is a placeholder</b> — Hell's own art is
 * {@code docs/VANILLA_ASSET_MAP.md}'s job and lands with the texture pass.
 *
 * <h2>Statline</h2>
 * {@link HellTier}'s standard row: 7342 HP / 387.6 damage / 81 armour. Nothing
 * here is a literal.
 */
public class InfernalClerkMob extends AncientArmoredSkeletonMob {

    public static final MaxHealthGetter MAX_HEALTH = HellTier.health(100);
    public static final GameDamage DAMAGE = HellTier.damage(100);
    public static final int ARMOR = HellTier.ARMOR;

    /** Vanilla's own 512 for this body, lifted by Hell's x1.55. */
    public static final int AGGRO_RANGE = HellTier.aggro(512);

    public static LootTable lootTable = new LootTable(
            LootItem.between("oddwood", HellTier.drop(2), HellTier.drop(4)),
            ChanceLootItem.between(0.30F, "realityshard", 1, 3));

    public InfernalClerkMob() {
        super();
        // Registered in construction: MobDifficultyChanges throws if it is
        // touched after init().
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
    }

    @Override
    public void init() {
        super.init();
        // AncientArmoredSkeletonMob.init() builds `new GameDamage(100.0F)` as a
        // constructor-local inside its own AI, so there is no field to write
        // through -- the tree is rebuilt against OUR damage instead, on
        // vanilla's own shape: 100 knockback, 40s wander.
        this.ai = new BehaviourTreeAI<>(this,
                new ConfusedCollisionPlayerChaserWandererAI<>(null, AGGRO_RANGE, DAMAGE, 100, 40000));
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkySpawnRules.daylightSpawn(this, server, client, targetX, targetY);
    }

    @Override
    public necesse.gfx.gameTexture.GameTexture getMobIcon() {
        return stairwaytoheaven.mobs.BorrowedMobIcon.from("ancientarmoredskeleton", super.getMobIcon());
    }
}
