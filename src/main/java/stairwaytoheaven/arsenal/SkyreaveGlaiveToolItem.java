package stairwaytoheaven.arsenal;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.glaiveToolItem.GlaiveToolItem;
import necesse.inventory.lootTable.presets.IncursionGlaiveWeaponsLootTable;

/**
 * Skyreave — an Aetherium double-crescent on a cloudwood haft. The swung
 * melee weapon of the tier: {@link GlaiveToolItem} sweeps a full circle around
 * the wielder (its {@code getHitboxes} walks a {@code LineHitbox} around
 * {@code attackRange / 2}), so it trades single-target punch for crowd control
 * against Skyreach packs.
 *
 * <p><b>Calibrated against {@code SlimeGlaiveToolItem}</b> — VERIFIED [jar].
 * It is the sole member of {@code IncursionGlaiveWeaponsLootTable} and
 * therefore the whole of vanilla's incursion-tier glaive class: enchant cost
 * 1900, EPIC, 400 ms swing, {@code attackDamage} 60.0 rising to 75.83335 at
 * forge tier 1, range 200, knockback 150, width 20.0. Shape, speed, cost and
 * rarity are taken from it directly.
 *
 * <p><b>Damage is set above it, deliberately.</b> Necesse subtracts armour
 * flat — {@code DamageType.getDamageReduction(armor, isItemsVsItems)} is
 * {@code armor * 0.5F} against a player-owned attack and
 * {@code GameDamage.getTotalDamage} then does
 * {@code max(0, damage - reduction)}, VERIFIED [jar] — so a Skyreach enemy at
 * 40 armour eats 20 off every swing and a Veil enemy at 70 armour eats 35. At
 * SlimeGlaive's 60 the Skyreave would be doing chip damage to the biome it was
 * made for. The upgrade ratio is still SlimeGlaive's exactly
 * (75.83335 / 60.0 = 1.2638892), applied to a base of 150.0, which puts the
 * forge-tier-1 value at 189.58337 — inside the 175-200 band the top of
 * vanilla's own {@code attackDamage} distribution occupies, and about six
 * swings through a 1000 HP / 40 armour Skyreach enemy.
 *
 * <p><b>Why EPIC.</b> Rarity is read off the tier rather than off this one
 * glaive: {@code ArcanicChestplateArmorItem} is 29 armour / enchant 1900 /
 * EPIC, and EPIC is what the incursion weapon tables are mostly made of —
 * {@code SlimeGlaive}, {@code SlimeGreatbow}, {@code TheRavensNest},
 * {@code PhantomPopper}, {@code SlimeStaff}, {@code BloodGrimoire},
 * {@code PhantomCaller}, {@code OrbOfSlimes}, {@code CrystallizedSkull},
 * {@code EmpressCommand}, {@code IgnitionKey}. VERIFIED [jar].
 *
 * <p><b>Why the range stops short of SlimeGlaive's 200.</b> {@code attackRange / 2}
 * is the sweep radius, but the blade the player sees only reaches
 * {@code attackXOffset} pixels, and vanilla keeps the two in step — QuartzGlaive
 * 140/50, CryoGlaive 160/58, SlimeGlaive 200/74, i.e. pivot ≈ 0.4 × range − 6.
 * Ours is a 96x96 sheet with a 48 px pivot, which is where 150 comes from.
 * Reaching 200 means a pivot near 74, i.e. roughly half again the sheet, cut in
 * {@code tools/asset_generator/gen_arsenal.py} first; that is an art change, so
 * the reach stays behind the tier until the sprite catches up. Damage, swing
 * speed, knockback, enchant cost and rarity are all at the tier.
 */
public class SkyreaveGlaiveToolItem extends GlaiveToolItem {

    public SkyreaveGlaiveToolItem() {
        // Loot pool: the incursion glaive table, not the general one. The mod is
        // endgame content now, so the Skyreave must not roll out of the ordinary
        // glaive-weapon chests a fresh character opens. ToolItem's constructor
        // adds the item straight into whichever shared OneOfLootItems it is
        // handed (ToolItem.addToLootTable), so the argument IS the gate.
        super(1900, IncursionGlaiveWeaponsLootTable.incursionGlaiveWeapons); // SlimeGlaive enchant 1900
        this.rarity = Item.Rarity.EPIC;                     // incursion tier; ArcanicChestplate is EPIC
        this.attackAnimTime.setBaseValue(400);              // SlimeGlaive 400 ms
        // 150.0 x SlimeGlaive's own 75.83335/60.0 upgrade ratio; see the note above
        this.attackDamage.setBaseValue(150.0F).setUpgradedValue(1.0F, 189.58337F);
        this.attackRange.setBaseValue(150);                 // SlimeGlaive 200; held back by the 48 px sprite pivot
        this.knockback.setBaseValue(150);                   // SlimeGlaive 150
        this.width = 20.0F;                                 // SlimeGlaive 20.0
        // The rotation pivot of player/weapons/skyreave.png, which is 96x96
        // with the grip at its centre. Vanilla pairs 108x92 with 50/50
        // (QuartzGlaive) and goes up to 74/74 on the SlimeGlaive — the offsets
        // ARE the sprite's pivot, not a free-floating tuning number.
        this.attackXOffset = 48;
        this.attackYOffset = 48;
        // Raid loadouts. A 150-damage weapon must not turn up in a settlement
        // raid against someone who has never climbed the stairway, and
        // ToolItem.getRaiderTicketModifier returns 0.0F — no raider gets it —
        // when useForRaidsOnlyIfObtained is set and the ID is not in the
        // world's obtained set. SlimeGlaive sets exactly this pair.
        this.canBeUsedForRaids = true;
        this.useForRaidsOnlyIfObtained = true;              // SlimeGlaive true
        this.raidTicketsModifier = 0.25F;                   // SlimeGlaive 0.25F
    }

    /**
     * The item's flavour line. Overriding this is what actually PUTS a
     * description on the item: an [itemtooltip] locale key nothing calls is
     * dead text, which is why every weapon in this package names its own key.
     */
    @Override
    public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective,
                                                     GameBlackboard blackboard) {
        ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
        tooltips.add(Localization.translate("itemtooltip", "skyreavetip"));
        return tooltips;
    }
}
