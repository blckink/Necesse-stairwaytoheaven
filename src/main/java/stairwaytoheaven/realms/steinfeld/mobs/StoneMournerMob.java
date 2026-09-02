package stairwaytoheaven.realms.steinfeld.mobs;

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
 * Stone Mourner — a statue that wakes, {@code docs/WORLD_DESIGN.md} §7's
 * second named resident and the Slab Fields' own ground.
 *
 * <h2>Vanilla base</h2>
 * {@link AncientArmoredSkeletonMob}, art {@code mobs/ancientarmoredskeleton} —
 * a rigid figure in full plate under a smooth, featureless helm, silver-grey
 * from head to foot with no visible face. It is the closest thing the game's
 * own sheets have to "a statue" that still walks: nothing about the shape
 * reads as a skeleton once it is standing still among Steinfeld's grey slabs,
 * which is the entire point — a player is meant to mistake it for scenery
 * until it moves. Subclassing keeps the rigid stance, the ordinary melee
 * chase and vanilla's own bone drop; only the numbers move.
 *
 * <h2>Tier: Steinfeld row, standard (no role discount)</h2>
 * {@link SteinfeldTier}: the realm's floor, unmodified —
 * <b>2100 HP / 200 damage / 50 armour</b>, exactly the line
 * {@code SteinfeldTier}'s own class comment prints for this mob. It is the
 * anchor of both inner guard tables (see {@code QuietMeadowBiome} and
 * {@code SlabFieldsBiome}'s {@code getGuard()}) precisely because it carries
 * no discount: everything else in the realm is measured against it. Vanilla's
 * armoured skeleton is 550 HP / 100 damage / 25 armour and is left untouched.
 */
public class StoneMournerMob extends AncientArmoredSkeletonMob {

    /** Steinfeld row, unmodified: 2100 on Classic. */
    public static final MaxHealthGetter MAX_HEALTH = SteinfeldTier.health(100);
    /** Steinfeld row, unmodified: 200 damage. */
    public static final GameDamage DAMAGE = SteinfeldTier.damage(100);
    /** The realm's armour, unreduced. */
    public static final int ARMOR = SteinfeldTier.ARMOR;

    /**
     * A mourner is made of the ground it stands on: mostly Pale Stone, with the
     * rare Grave Salt worked into the plate the way a real mourning-suit is
     * trimmed in black. Quantities at the realm's x1.6 drop value.
     */
    public static LootTable lootTable = new LootTable(
            LootItem.between("palestone", SteinfeldTier.drop(2), SteinfeldTier.drop(4)),
            ChanceLootItem.between(0.30F, "gravesalt", 1, 3));

    public StoneMournerMob() {
        super();
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
    }

    @Override
    public void init() {
        super.init();
        // AncientArmoredSkeletonMob.init() builds `new GameDamage(100.0F)` as a
        // constructor-local inside its own AI, so there is no field to write
        // through -- the tree is rebuilt against OUR damage instead, on
        // vanilla's own shape: 512 search, 100 knockback, 40s wander.
        this.ai = new BehaviourTreeAI<>(this,
                new ConfusedCollisionPlayerChaserWandererAI<>(null, 512, DAMAGE, 100, 40000));
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkySpawnRules.daylightSpawn(this, server, client, targetX, targetY);
    }
}
