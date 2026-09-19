package stairwaytoheaven.mobs;

import necesse.entity.mobs.hostile.CryptBatMob;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;

/**
 * A Blood Thrall — what sometimes gets up again after the vampire has fed.
 *
 * <p>One animal in six does not stay down: {@link VampireSettlerMob#drain}
 * spawns one of these instead of leaving a carcass. It is hostile, it is the
 * player's problem, and it carries loot no ordinary animal does — which is the
 * whole point of the mechanic. The settlement's own guards will deal with it if
 * the player does not.
 *
 * <p><b>It costs no new art.</b> The class subclasses vanilla's
 * {@code CryptBatMob} and overrides nothing about how it is drawn, so it wears
 * the crypt bat's own sheet and its own bestiary face — the same trade
 * {@code CrookedGolemMob} and {@code SourvatBloomMob} make, minus the custom
 * sheet those two carry. What is ours is the loot table, which is the part the
 * player is meant to notice. A drawn sheet of its own is an art task, not a
 * blocker.
 */
public class BloodThrallMob extends CryptBatMob {

    /**
     * Blood, always, and sometimes what is left of the animal it used to be.
     * Deliberately modest: he makes one of these out of an ordinary field
     * animal, and a farm-animal grinder would be worth more than the hunt it
     * comes from.
     */
    public static LootTable thrallLoot = new LootTable(
            LootItem.between("bloodvial", 1, 2),
            new ChanceLootItemList(0.35F, LootItem.between("leather", 1, 2)));

    /** Unused hook kept for symmetry with the mod's other borrowed mobs. */
    public static GameTexture texture;

    @Override
    public LootTable getLootTable() {
        return thrallLoot;
    }
}
