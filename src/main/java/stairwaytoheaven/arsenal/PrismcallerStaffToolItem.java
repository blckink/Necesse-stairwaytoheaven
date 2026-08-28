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
import necesse.inventory.lootTable.presets.MagicWeaponsLootTable;
import necesse.level.maps.Level;

/**
 * Prismcaller — a prismwood staff crowned with an Aurora Shoals prismshard.
 *
 * <p>Single-bolt caster, built the way {@code QuartzStaffProjectileToolItem}
 * is: {@code onAttack} constructs one projectile, attaches a
 * {@code ResilienceOnHitProjectileModifier}, reseeds its unique ID from the
 * attack seed (so client and server agree) and hands it to
 * {@code addAndSendAttackerProjectile}, then spends mana. Every one of those
 * five steps is load bearing — dropping the reseed desynchronises the
 * projectile in multiplayer.
 *
 * <p><b>Calibrated against {@code QuartzStaffProjectileToolItem}</b>
 * (77 dmg / 600 ms / velocity 150 / range 700 / knockback 50 / mana 2.5,
 * UNCOMMON). Ours is a shade stronger and faster on the same mana, which is
 * the same relationship the Tempest Edge has to the tungsten sword.
 */
public class PrismcallerStaffToolItem extends MagicProjectileToolItem {

    public PrismcallerStaffToolItem() {
        super(1300, MagicWeaponsLootTable.magicWeapons);
        this.rarity = Item.Rarity.RARE;
        this.attackAnimTime.setBaseValue(560);
        this.attackDamage.setBaseValue(80.0F).setUpgradedValue(1.0F, 158.0F);
        this.velocity.setBaseValue(165);
        this.attackRange.setBaseValue(720);
        this.knockback.setBaseValue(50);
        this.manaCost.setBaseValue(2.5F).setUpgradedValue(1.0F, 2.5F);
        // Pivot of player/weapons/prismcaller.png (50x50) — the same sheet
        // size and offsets vanilla's quartzstaff uses.
        this.attackXOffset = 14;
        this.attackYOffset = 4;
        this.itemAttackerProjectileCanHitWidth = 5.0F;
        this.itemAttackerPredictionDistanceOffset = -40.0F;
        this.canBeUsedForRaids = true;
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
