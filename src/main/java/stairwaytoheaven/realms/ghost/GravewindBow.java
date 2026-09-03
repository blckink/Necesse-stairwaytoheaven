package stairwaytoheaven.realms.ghost;

import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.BowProjectileToolItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import stairwaytoheaven.items.ItemDescription;

/**
 * Gravewind Bow — the Ghost Realm's bow, and the ranged half of the pair
 * {@code docs/FOGKEY_AND_BOSSPORTALS.md} A3 calls "ghost weapons".
 *
 * <h2>Why a plain bow</h2>
 * It fires whatever arrow the player has loaded and adds no projectile of its
 * own, exactly like vanilla's {@code AntiqueBowProjectileToolItem}
 * (AntiqueBowProjectileToolItem.java, a bare {@code BowProjectileToolItem}
 * subclass, VERIFIED [jar]). A bespoke bolt would need a sprite, and this realm
 * ships no new pixels.
 *
 * <h2>Calibration — against the vanilla bow of the tier below</h2>
 *
 * The vanilla bow at the incursion tier is
 * {@code TheCrimsonSkyProjectileToolItem} (:41-46, <b>VERIFIED [jar]</b>):
 *
 * <pre>
 *   enchantCost 1900   EPIC   damage 90 -> 110.83
 *   attackAnimTime 500 ms     velocity 350     attackRange 1600
 * </pre>
 *
 * {@code docs/BALANCE.md} §7 puts <b>Spiritsteel one rung above</b> the
 * incursion floor — chest 34 / enchant 2400 against 29 / 1900 — so the damage
 * is Crimson Sky's scaled by that rung (34/29 = ×1.1724) and rounded:
 *
 * <pre>
 *    90     x 1.1724 = 105.5  ->  105
 *   110.83  x 1.1724 = 129.9  ->  130
 * </pre>
 *
 * Draw speed, arrow speed and range are Crimson Sky's own numbers, unchanged:
 * they describe what a bow of this tier FEELS like, and the tier step is meant
 * to be felt in the damage rather than in a bow that also happens to shoot
 * further and faster than anything else in the game.
 *
 * <h2>Loot table: deliberately null</h2>
 * Same argument, same words, as {@link SpiritsteelReaver}: passing
 * {@code BowWeaponsLootTable.bowWeapons} would put a tier-8 bow in a starting
 * chest. It is reached through the Ghost Guide's trade and through the
 * Aftergarden's own drop tables ({@link GhostLoot}) and nowhere else.
 *
 * <h2>Borrowed art — no new pixels</h2>
 * Icon and draw sheet are the game's own {@code necroticbow}, read straight
 * from the game's resources by literal path. Nothing copied, nothing
 * recoloured. Recorded in {@code docs/VANILLA_ASSET_MAP.md} §1.3b.
 */
public class GravewindBow extends BowProjectileToolItem {

    /** Vanilla icon under {@code items/} and sheet under {@code player/weapons/}. */
    public static final String ART = "necroticbow";

    public GravewindBow() {
        // thecrimsonsky: enchantCost 1900 at the incursion floor;
        // BALANCE §7 puts Spiritsteel one rung up at 2400.
        super(2400, (OneOfLootItems) null);
        this.rarity = Item.Rarity.EPIC;                  // thecrimsonsky: EPIC
        this.attackAnimTime.setBaseValue(500);           // thecrimsonsky: 500
        // thecrimsonsky 90 -> 110.83, x 34/29 (BALANCE §7's chest step)
        this.attackDamage.setBaseValue(105.0F).setUpgradedValue(1.0F, 130.0F);
        this.velocity.setBaseValue(350);                 // thecrimsonsky: 350
        this.attackRange.setBaseValue(1600);             // thecrimsonsky: 1600
        // antiquebow's own offsets: where the stave is drawn against the hand.
        this.attackXOffset = 12;
        this.attackYOffset = 12;
        this.canBeUsedForRaids = true;                   // antiquebow: true
    }

    /** See {@link SpiritsteelReaver#loadItemTextures()} — same seam, same reason. */
    @Override
    protected void loadItemTextures() {
        this.itemTexture = GameTexture.fromFile("items/" + ART);
    }

    /** See {@link SpiritsteelReaver#loadAttackTexture()} — vanilla's own try/catch. */
    @Override
    protected void loadAttackTexture() {
        try {
            this.attackTexture = GameTexture.fromFileRaw("player/weapons/" + ART);
        } catch (java.io.FileNotFoundException e) {
            this.attackTexture = null;
        }
    }

    @Override
    public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective,
            GameBlackboard blackboard) {
        ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
        String line = ItemDescription.of(this.getStringID());
        if (line != null) {
            tooltips.addFirst(line);
        }
        return tooltips;
    }
}
