package stairwaytoheaven.arsenal;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.summonToolItem.SummonToolItem;
import necesse.inventory.lootTable.presets.IncursionSummonWeaponsLootTable;

/**
 * Skywatch Whistle — blow it and a Watch Mote peels off the old frost
 * machinery and circles you.
 *
 * <p>{@code SummonToolItem(mobStringID, followPosition, summonSpaceTaken,
 * enchantCost, lootTableCategory)}: the first argument is the MobRegistry
 * stringID the focus spawns, so {@code "watchmote"} must be registered before
 * this item is ever used (both happen in {@code init()}, see
 * {@link SkyArsenal}). {@code summonType} is left at the default
 * {@code "summonedmob"} — a permanent follower that counts against the
 * player's summon slots, not the temporary kind
 * {@code FrostPiercerSummonToolItem} uses.
 *
 * <p><b>Calibrated against {@code CrystallizedSkullSummonToolItem}</b> —
 * VERIFIED [jar]: {@code super("rubydragonhead", FLYING_CIRCLE_FAST, 2.0F,
 * 1900, IncursionSummonWeaponsLootTable.incursionSummonWeapons)}, EPIC,
 * {@code attackDamage} 180.0 rising to 210.00006 at forge tier 1. It is the
 * only vanilla summon focus whose number is in the range this tier needs, and
 * it buys that number with two summon slots.
 *
 * <p>The rest of the incursion summon table takes one slot each, as this does,
 * and is far smaller: {@code OrbOfSlimesToolItem} 41.0 -> 51.333347,
 * {@code PhantomCallerSummonToolItem} 27.0 -> 35.000008,
 * {@code EmpressCommandToolItem} 20.0 -> 25.666674,
 * {@code IgnitionKeySummonToolItem} 10.0 -> 12.833337 with a further x0.25 in
 * {@code getAttackDamage}. VERIFIED [jar].
 *
 * <p><b>Why this leaves the one-slot band behind.</b> Armour is a flat
 * subtraction, not a percentage: {@code DamageType.getDamageReduction(armor,
 * isItemsVsItems)} is {@code armor * 0.5F} against a player-owned attack and
 * {@code GameDamage.getTotalDamage} then does {@code max(0, damage - reduction)}, VERIFIED
 * [jar]. A summon pays that on every single hit, so at PhantomCaller's 27 a
 * mote does 7 damage to a 40-armour Skyreach enemy and nothing at all past
 * about 55 armour — the whole weapon would stop existing exactly where the mod
 * begins. The upgrade ratio is CrystallizedSkull's exactly
 * (210.00006 / 180.0 = 1.166667) applied to a base of 150.0, giving 175.00005
 * at forge tier 1: the floor of the 175-200 band, and below CrystallizedSkull
 * on both numbers at half its summon cost.
 *
 * <p>The band is really a cadence table, and this mote is the slow end of it.
 * It is vanilla's own {@code CryoFlakeFollowingMob} unchanged, and its
 * {@code PlayerFlyingFollowerShooterChaserAI(576, TICK, 800, 480, 640, 64)}
 * fires one {@code CryoMissileProjectile} every 800 ms; the Orb of Slimes'
 * slime collides every 500 ms ({@code collisionHitCooldowns.hitCooldown = 500})
 * and the charging phantom every 750 ms. VERIFIED [jar].
 *
 * <p>Rarity is the tier's, and here it is also the whole band's: every
 * incursion summon focus is EPIC, as is {@code ArcanicChestplateArmorItem}
 * (29 armour / enchant 1900 / EPIC).
 */
public class SkywatchWhistleSummonToolItem extends SummonToolItem {

    public SkywatchWhistleSummonToolItem() {
        // Loot pool: the incursion summon table. See the note in
        // SkyreaveGlaiveToolItem — the loot table handed to the constructor is
        // what decides where in the game this can be found.
        // 1.0F space: one summon slot, as every incursion focus in the band.
        super("watchmote", FollowPosition.FLYING_CIRCLE, 1.0F, 1900,
                IncursionSummonWeaponsLootTable.incursionSummonWeapons); // incursion summon foci, all 1900
        this.rarity = Item.Rarity.EPIC;                      // incursion tier; every incursion focus is EPIC
        // 150.0 x CrystallizedSkull's own 210.00006/180.0 upgrade ratio; see the note above
        this.attackDamage.setBaseValue(150.0F).setUpgradedValue(1.0F, 175.00005F);
        // Raiders do not get a summon focus. ToolItem.canBeUsedForRaids is false
        // by default and not one vanilla SummonToolItem turns it on — the only
        // one that mentions it, EyeOfTheVoidSummonToolItem, sets it false by
        // hand. Handing this to a settlement raider would put a 150-damage
        // Watch Mote in the raid, which no vanilla summon can do. VERIFIED [jar].
        this.canBeUsedForRaids = false;
    }

    /** See the note in {@link SkyreaveGlaiveToolItem}. Appended after
     * SummonToolItem's own slot/space lines, which the super call adds. */
    @Override
    public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective,
                                                     GameBlackboard blackboard) {
        ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
        tooltips.add(Localization.translate("itemtooltip", "skywatchwhistletip"));
        return tooltips;
    }
}
