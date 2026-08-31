package stairwaytoheaven.arsenal;

import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.modifiers.ResilienceOnHitProjectileModifier;
import necesse.gfx.GameResources;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.MagicProjectileToolItem;
import necesse.inventory.lootTable.presets.IncursionMagicWeaponsLootTable;
import necesse.level.maps.Level;

/**
 * Prismcaller — a prismwood staff crowned with an Aurora Shoals prismshard.
 *
 * <p>Single-bolt caster: {@code onAttack} constructs one projectile, attaches a
 * {@code ResilienceOnHitProjectileModifier}, reseeds its unique ID from the
 * attack seed (so client and server agree) and hands it to
 * {@code addAndSendAttackerProjectile}, then spends mana. Every one of those
 * five steps is load bearing — dropping the reseed desynchronises the
 * projectile in multiplayer.
 *
 * <p><b>Calibrated against {@code IncursionMagicWeaponsLootTable}</b>, whose
 * four members are vanilla's incursion-tier magic weapons — VERIFIED [jar]:
 *
 * <ul>
 *   <li>{@code SlimeStaffProjectileToolItem} — 1900 / EPIC / 300 ms /
 *       32.0 -> 49.000015 / velocity 100 / range 1250 / mana 3.2. Its
 *       {@code onAttack} loops {@code i = -1..1} and fires THREE bolts, so its
 *       per-cast total is 96.0 base and 147.000045 at forge tier 1.</li>
 *   <li>{@code PhantomPopperProjectileToolItem} — 1900 / EPIC / 600 ms /
 *       59.0 -> 91.00002 / velocity 50 / range 100 / mana 4.0 / knockback 20 /
 *       resilience 2.0. The one-projectile-per-cast member.</li>
 *   <li>{@code BloodGrimoireProjectileToolItem} — 1900 / EPIC / 2000 ms /
 *       84.0 -> 126.00004, paid for in life rather than mana.</li>
 *   <li>{@code RefractorProjectileToolItem} — 1900 / RARE / 2000 ms /
 *       50.0 -> 70.00002 / mana 10.0.</li>
 * </ul>
 *
 * <p>The Prismcaller throws one bolt, so the SlimeStaff is the analogue whose
 * shape it inherits: its whole three-bolt spread, delivered as one projectile,
 * at PhantomPopper's 600 ms cast and PhantomPopper's 4.0 mana.
 *
 * <p><b>Damage is set above both, deliberately.</b> Necesse subtracts armour
 * flat — {@code DamageType.getDamageReduction} is {@code armor * 0.5F} against
 * a player-owned attack, VERIFIED [jar] — and a caster is where that bites
 * hardest, because a spread staff pays the subtraction once per bolt. The
 * upgrade ratio is SlimeStaff's exactly (49.000015 / 32.0 = 1.5312505),
 * applied to a base of 118.0, giving 180.68756 at forge tier 1: inside the
 * 175-200 band the top of vanilla's own {@code attackDamage} distribution
 * occupies, and just under {@code AscendedStaffToolItem}, the Ascended
 * Wizard's own drop, at 130.0 -> 182.00005 (2000 / UNIQUE / 500 ms anim plus a
 * 500 ms cooldown / mana 5.0). VERIFIED [jar]. Staying under the final boss's
 * staff on the number while casting faster than it is the trade this weapon
 * makes; the 4.0 mana is what pays for the cadence.
 *
 * <p>Rarity is the tier's: {@code ArcanicChestplateArmorItem} is 29 armour /
 * enchant 1900 / EPIC, and three of the four incursion magic weapons are EPIC.
 * VERIFIED [jar].
 */
public class PrismcallerStaffToolItem extends MagicProjectileToolItem {

    public PrismcallerStaffToolItem() {
        // Loot pool: the incursion magic table. See the note in
        // SkyreaveGlaiveToolItem — the loot table handed to ToolItem's
        // constructor is what decides where in the game this can be found.
        super(1900, IncursionMagicWeaponsLootTable.incursionMagicWeapons); // incursion magic weapons, all 1900
        this.rarity = Item.Rarity.EPIC;                      // incursion tier; ArcanicChestplate is EPIC
        this.attackAnimTime.setBaseValue(600);               // PhantomPopper 600 ms
        // 118.0 x SlimeStaff's own 49.000015/32.0 upgrade ratio; see the note above
        this.attackDamage.setBaseValue(118.0F).setUpgradedValue(1.0F, 180.68756F);
        this.manaCost.setBaseValue(4.0F).setUpgradedValue(1.0F, 4.0F);        // PhantomPopper 4.0, flat across tiers
        // Flight profile — the weapon's own shape, not a tier stat. The
        // incursion casters are slow and short because their bolts carry
        // effects (PhantomPopper velocity 50 / range 100, SlimeStaff 100 / 1250,
        // AscendedStaff 120 / 800); this one is a plain fast straight bolt that
        // pierces exactly one target, which is QuartzBoltProjectile's behaviour.
        this.velocity.setBaseValue(165);
        this.attackRange.setBaseValue(720);
        this.knockback.setBaseValue(50);
        // Pivot of player/weapons/prismcaller.png (50x50) — the same sheet
        // size and offsets vanilla's quartzstaff uses.
        this.attackXOffset = 14;
        this.attackYOffset = 4;
        this.itemAttackerProjectileCanHitWidth = 5.0F;       // PhantomPopper 10.0, SlimeStaff 5.0
        this.itemAttackerPredictionDistanceOffset = -40.0F;  // PhantomPopper -40.0
        // Raid loadouts — see the note in SkyreaveGlaiveToolItem.
        this.canBeUsedForRaids = true;
        this.useForRaidsOnlyIfObtained = true;               // PhantomPopper true
        this.raidTicketsModifier = 0.25F;                    // PhantomPopper 0.25F
    }

    /** See the note in {@link SkyreaveGlaiveToolItem}: this is what puts a
     * description on the item rather than leaving the locale key dead. */
    @Override
    public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective,
                                                     GameBlackboard blackboard) {
        ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
        tooltips.add(Localization.translate("itemtooltip", "prismcallertip"));
        return tooltips;
    }

    @Override
    public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions,
                                      float attackDirX, float attackDirY, float attackProgress) {
        drawOptions.pointRotation(attackDirX, attackDirY).forEachItemSprite(i -> i.itemRotateOffset(45.0F));
    }

    @Override
    public InventoryItem onAttack(Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight,
                                  InventoryItem item, ItemAttackSlot slot, int animAttack, int seed,
                                  GNDItemMap mapContent) {
        GameRandom random = new GameRandom((long) seed);
        Projectile projectile = new PrismBoltProjectile(
                level, attackerMob, attackerMob.x, attackerMob.y, (float) x, (float) y,
                (float) this.getProjectileVelocity(item, attackerMob),
                this.getAttackRange(item),
                this.getAttackDamage(item),
                this.getKnockback(item, attackerMob));
        projectile.setModifier(new ResilienceOnHitProjectileModifier(this.getResilienceGain(item)));
        projectile.resetUniqueID(random);
        attackerMob.addAndSendAttackerProjectile(projectile, 40);
        this.consumeMana(attackerMob, item);
        return item;
    }

    @Override
    protected SoundSettings getAttackSound() {
        return new SoundSettings(GameResources.magicbolt1).volume(0.5F);
    }
}
