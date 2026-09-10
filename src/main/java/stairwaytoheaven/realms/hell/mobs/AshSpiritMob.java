package stairwaytoheaven.realms.hell.mobs;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.hostile.AshGolemMob;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import stairwaytoheaven.mobs.SkyMobTiers;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * The Ash Spirit — Hell's elite, from §17's Infernal Fringe roster. What the
 * Furnace (A3.8) leaves behind when it has finished with something, standing
 * up again.
 *
 * <h2>Body</h2>
 * Vanilla's {@code AshGolemMob} — the game's own heavy ash-and-ember body,
 * and the one place the mod does not have to invent a hell creature at all.
 * <b>VERIFIED [jar]</b> ({@code javap} on {@code necesse.entity.mobs.hostile
 * .AshGolemMob}) it holds its two damages as PUBLIC INSTANCE fields,
 * {@code collisionDamage} and {@code ashenWaveDamage}, rather than building
 * them inside its AI the way {@code AncientArmoredSkeletonMob} does. So this
 * mob writes through those two fields and leaves vanilla's behaviour tree
 * alone — which is why it is the one Hell resident with no
 * {@code AGGRO_RANGE} of its own: there is no tree of ours to hand one to,
 * and inventing a range would mean replacing a working AI to change a number
 * that already reads correctly.
 *
 * <p>Both fields are written, not only the collision one: the ashen wave IS
 * the elite's reason to exist, and an elite whose ranged attack still carried
 * vanilla's number would be exactly the half-landed rebalance
 * {@code scripts/balance_check.sh} exists to catch.
 *
 * <p><b>The sheet is a placeholder</b> until Hell's own art lands.
 *
 * <h2>Statline</h2>
 * {@link HellTier}'s elite row: 10278 HP / 387.6 damage / 81 armour.
 */
public class AshSpiritMob extends AshGolemMob {

    public static final MaxHealthGetter MAX_HEALTH =
            HellTier.health(SkyMobTiers.ROLE_ELITE_HP);
    /** Elite carries the realm's damage unchanged; the role pays in HP. */
    public static final GameDamage DAMAGE = HellTier.damage(100);
    public static final int ARMOR = HellTier.ARMOR;

    public static LootTable lootTable = new LootTable(
            LootItem.between("realityshard", HellTier.drop(1), HellTier.drop(3)),
            ChanceLootItem.between(0.45F, "oddwood", 3, 8),
            ChanceLootItem.between(0.20F, "charwood", 2, 5));

    public AshSpiritMob() {
        super();
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
        this.collisionDamage = DAMAGE;
        this.ashenWaveDamage = DAMAGE;
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
        return stairwaytoheaven.mobs.BorrowedMobIcon.from("ashgolem", super.getMobIcon());
    }
}
