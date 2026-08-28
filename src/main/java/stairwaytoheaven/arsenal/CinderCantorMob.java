package stairwaytoheaven.arsenal;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.hostile.AncientSkeletonMageMob;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * Cinder Cantor — a masked singer of the old rite, still walking the ash. The
 * Ashen Reach's first real inhabitant: until now only stray Gloom Shades
 * wandered in from the fen.
 *
 * <p><b>Vanilla base:</b> {@link AncientSkeletonMageMob}, art
 * {@code mobs/ancientskeletonmage} plus its two arm sheets, composed through
 * {@code HumanDrawOptions} (a bone-and-ash robed figure with a crimson-trimmed
 * mantle — already the Ashen Reach's palette, no colour shift needed).
 * Subclassing keeps its whole character: a caster that shoots
 * {@code AncientSkeletonMageProjectile} at range 640 AND owns a
 * {@code TeleportOnProjectileHitAINode} — get hit by one of ITS bolts and it
 * blinks away in a puff of smoke, which is what makes it a different fight
 * from anything else in the mod.
 *
 * <p><b>Nothing is re-tuned.</b> 400 HP / 90 damage / 25 armour is the hardest
 * enemy either dimension carries, and the Veil is deliberately the harder
 * layer. Its projectile damage is built inside the AI in {@code init()}, so
 * changing it would mean re-declaring the whole behaviour tree for a number
 * that is already correctly tiered.
 *
 * <p>Vanilla drops plain bone; ours keeps the bone (the Veil has no other
 * source) and adds the two Veil materials.
 */
public class CinderCantorMob extends AncientSkeletonMageMob {

    public static LootTable lootTable = new LootTable(
            LootItem.between("bone", 1, 3),
            new ChanceLootItemList(0.55F, LootItem.between("cinderpearl", 1, 2)),
            new ChanceLootItemList(0.45F, LootItem.between("veilessence", 1, 2)));

    public CinderCantorMob() {
        super();
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
