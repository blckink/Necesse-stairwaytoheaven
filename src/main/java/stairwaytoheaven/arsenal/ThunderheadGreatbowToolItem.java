package stairwaytoheaven.arsenal;

import java.awt.Color;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.greatbowProjectileToolItem.GreatbowProjectileToolItem;
import necesse.inventory.lootTable.presets.IncursionGreatbowWeaponsLootTable;

/**
 * Thunderhead — a seraphwood greatbow strung with windsilk.
 *
 * <p>A greatbow is not a bow with bigger numbers: {@code GreatbowProjectileToolItem}
 * installs a {@code GreatbowAttackHandler} and scales velocity, range, damage,
 * knockback and resilience by the charge percentage, so an uncharged shot lands
 * at 5-40% of these values. That is why the base damage looks enormous next to
 * an ordinary bow's — it is the fully-drawn number.
 *
 * <p>Like every bow in the game this class holds no arrow: at attack time
 * {@code BowProjectileToolItem.getArrowItem} asks the wielder for its equipped
 * {@code ArrowItem} and the ARROW builds the projectile
 * (verified in {@code BowProjectileToolItem} / {@code ArrowItem.getProjectile}).
 *
 * <p><b>Calibrated against {@code NightPiercerGreatBowProjectileToolItem}</b> —
 * VERIFIED [jar]: enchant cost 1900, 500 ms draw, {@code attackDamage} 124.0
 * rising to 157.50005 at forge tier 1, range 1600, velocity 500. It is the
 * straight-shot member of {@code IncursionGreatbowWeaponsLootTable} and so the
 * honest like-for-like: the other two trade raw output for a mechanic —
 * {@code SlimeGreatbowProjectileToolItem} lobs arcing shots (1900 / EPIC /
 * 500 ms / 96.0 -> 122.50003 / range 1600 / velocity 350 / resilience 1.0) and
 * {@code TheRavensNestProjectileToolItem} is a slow flock bow (1900 / EPIC /
 * 600 ms / 55.0 -> 64.16669 / range 1400 / velocity 200).
 *
 * <p><b>Damage is set above it, deliberately.</b> Necesse subtracts armour flat
 * — {@code DamageType.getDamageReduction} is {@code armor * 0.5F} against a
 * player-owned attack, VERIFIED [jar] — so at NightPiercer's 124 a fully drawn
 * arrow would be chipping at the Skyreach and the Veil. The upgrade ratio is
 * NightPiercer's exactly (157.50005 / 124.0 = 1.2701617), applied to a base of
 * 142.0, giving 180.36296 at forge tier 1.
 *
 * <p><b>Why that sits under the Skyreave's 189.58337.</b> A bow's number is not
 * the whole shot: {@code ArrowItem.modDamage} ADDS the arrow's own damage — 5
 * for stone up to 17 for spiderite — on top of the bow's, rather than replacing
 * it. VERIFIED [jar]. Aiming the bow a blade's-worth of arrow low is what keeps
 * the two in line once the ammo is counted. It also lands beside vanilla's own
 * high-water marks for greatbow {@code attackDamage} at forge tier 1:
 * {@code VoidGreatbowProjectileToolItem} 65.0 -> 175.00005 and
 * {@code GoldGreatbowProjectileToolItem} 52.0 -> 180.83339. A greatbow keeps
 * all of this in {@code attackDamage} — {@code GreatbowProjectileToolItem}
 * declares no second damage field, it multiplies {@code attackDamage} by the
 * charge percentage at attack time.
 *
 * <p>Rarity is the tier's: {@code ArcanicChestplateArmorItem} is 29 armour /
 * enchant 1900 / EPIC, and the incursion greatbows that are not built around a
 * gimmick — SlimeGreatbow, TheRavensNest — are EPIC too. VERIFIED [jar].
 */
public class ThunderheadGreatbowToolItem extends GreatbowProjectileToolItem {

    public ThunderheadGreatbowToolItem() {
        // Loot pool: the incursion greatbow table. See the note in
        // SkyreaveGlaiveToolItem — the loot table handed to ToolItem's
        // constructor is what decides where in the game this can be found, and
        // the mod is endgame content now.
        super(1900, IncursionGreatbowWeaponsLootTable.incursionGreatbowWeapons); // NightPiercer enchant 1900
        this.rarity = Item.Rarity.EPIC;                      // incursion tier; ArcanicChestplate is EPIC
        this.attackAnimTime.setBaseValue(500);               // NightPiercer 500 ms
        // 142.0 x NightPiercer's own 157.50005/124.0 upgrade ratio, aimed a
        // blade's-worth of arrow under the Skyreave; see the notes above
        this.attackDamage.setBaseValue(142.0F).setUpgradedValue(1.0F, 180.36296F);
        this.attackRange.setBaseValue(1600);                 // NightPiercer 1600
        this.velocity.setBaseValue(500);                     // NightPiercer 500
        // Pivot of player/weapons/thunderhead.png (24x64), the same sheet size
        // and offsets vanilla uses for its own greatbow attack sprites — every
        // incursion greatbow is 12/38 on a slightly wider sheet.
        this.attackXOffset = 10;
        this.attackYOffset = 36;
        this.particleColor = new Color(136, 216, 220);   // palette.AETHERIUM light
        // Raid loadouts — see the note in SkyreaveGlaiveToolItem.
        this.canBeUsedForRaids = true;
        this.useForRaidsOnlyIfObtained = true;               // NightPiercer true
        this.raidTicketsModifier = 0.2F;                     // NightPiercer 0.2F
    }

    /**
     * Added through the greatbow's own tooltip hook rather than
     * getPreEnchantmentTooltips, so our line lands next to vanilla's
     * "greatbowtip" charge explanation instead of above it.
     */
    @Override
    protected void addExtraBowTooltips(ListGameTooltips tooltips, InventoryItem item, PlayerMob perspective,
                                       GameBlackboard blackboard) {
        super.addExtraBowTooltips(tooltips, item, perspective, blackboard);
        tooltips.add(Localization.translate("itemtooltip", "thunderheadtip"));
    }
}
