package stairwaytoheaven.realms.ghost;

import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem.GreatswordToolItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import stairwaytoheaven.items.ItemDescription;

/**
 * Spiritsteel Reaver — the Ghost Realm's greatsword, and the first weapon the
 * realm has ever had.
 *
 * <h2>Why a new weapon at all</h2>
 * The Aftergarden already ships Spiritsteel <b>armour</b>
 * ({@link SpiritsteelHelmet}, {@link SpiritsteelChestplate},
 * {@link SpiritsteelBoots}) and nothing to swing. So
 * {@code docs/FOGKEY_AND_BOSSPORTALS.md} A3's "he sells ghost weapons" had
 * nothing to sell: the set's own metal had no weapon recipe, and the realm's
 * loot tables paid only in materials. This is that weapon, forged from the bar
 * the armour is forged from.
 *
 * <h2>Calibration — every number against the vanilla weapon of the same class
 * at the tier below</h2>
 *
 * The vanilla weapon of this class at the incursion tier is
 * {@code RavenwingGreatswordToolItem}, and it is measured rather than guessed
 * (RavenwingGreatswordToolItem.java:18-29, <b>VERIFIED [jar]</b>):
 *
 * <pre>
 *   enchantCost 1900   EPIC   damage 150 -> 186.67
 *   attackRange 100    knockback 150     charge levels 150/300/450 ms
 * </pre>
 *
 * {@code docs/BALANCE.md} §7 puts <b>Spiritsteel one rung above</b> that: the
 * Ghost Realm's set is chest 34 / enchant 2400 against the incursion floor's
 * chest 29 / enchant 1900, which the mod's own Stormsteel sits exactly on. So
 * the damage is Ravenwing's, scaled by that rung — 34/29 = ×1.1724 — and
 * rounded to whole numbers:
 *
 * <pre>
 *   150     x 1.1724 = 175.9  ->  176
 *   186.67  x 1.1724 = 218.8  ->  219
 * </pre>
 *
 * Everything that is about the SHAPE of a greatsword rather than its tier is
 * Ravenwing's, unchanged: range 100, knockback 150, and the same three charge
 * levels, because a Ghost greatsword should feel like a greatsword and not like
 * a new class of weapon. {@code attackAnimTime} is left at
 * {@code GreatswordToolItem}'s own 200 ms for the same reason.
 *
 * <p>For scale on the mod's own ladder: {@code TempestEdgeSwordToolItem}, the
 * Skyreach's one-handed blade, is 156 → 182 at enchant 1900. This is the rung
 * above it and a two-hander, so it lands above it in both.
 *
 * <h2>Loot table: deliberately null</h2>
 * Both {@code null}s that vanilla's own {@code AncestorSwordToolItem} passes
 * (AncestorSwordToolItem.java:8), and for the reason {@code StormsteelArmor}
 * and {@link SpiritsteelHelmet} both write out at length: handing this to
 * {@code GreatswordWeaponsLootTable.greatswordWeapons} would drop a tier-8
 * greatsword out of an ordinary surface chest and reverse the entire climb.
 * It reaches the player two ways instead, both inside the realm that made it —
 * the Ghost Guide's trade ({@code mobs/GhostGuideMob}) and the Aftergarden's
 * own elite drop table ({@link GhostLoot#elite()}).
 *
 * <h2>Borrowed art — no new pixels</h2>
 * Icon and mid-swing sheet are the game's own {@code necroticgreatsword}, read
 * straight from the game's resources by literal path; nothing is copied into
 * {@code src/main/resources} and nothing is recoloured. Recorded in
 * {@code docs/VANILLA_ASSET_MAP.md} §1.3b.
 */
public class SpiritsteelReaver extends GreatswordToolItem {

    /** Vanilla icon under {@code items/} and sheet under {@code player/weapons/}. */
    public static final String ART = "necroticgreatsword";

    public SpiritsteelReaver() {
        // ravenwinggreatsword: enchantCost 1900 at the incursion floor;
        // BALANCE §7 puts Spiritsteel one rung up at 2400.
        super(2400, (OneOfLootItems) null, getThreeChargeLevels(150, 300, 450));
        this.rarity = Item.Rarity.EPIC;                 // ravenwinggreatsword: EPIC
        // ravenwinggreatsword 150 -> 186.67, x 34/29 (BALANCE §7's chest step)
        this.attackDamage.setBaseValue(176.0F).setUpgradedValue(1.0F, 219.0F);
        this.attackRange.setBaseValue(100);             // ravenwinggreatsword: 100
        this.knockback.setBaseValue(150);               // ravenwinggreatsword: 150
        this.canBeUsedForRaids = true;                  // ravenwinggreatsword: true
    }

    /**
     * {@code Item.loadItemTextures} is {@code items/<stringID>} (Item.java:569),
     * which would look for a PNG this mod deliberately does not ship. Same seam
     * {@link GhostMatItem} uses for every material in this realm.
     */
    @Override
    protected void loadItemTextures() {
        this.itemTexture = GameTexture.fromFile("items/" + ART);
    }

    /**
     * The blade drawn in the player's hands mid-swing.
     *
     * <p>{@code Item.loadAttackTexture} is {@code player/weapons/<stringID>}
     * inside a try/catch that leaves the field null when the file is missing
     * (Item.java:581-588, VERIFIED [jar]). The catch is kept exactly as vanilla
     * writes it: if a future game version renames the borrowed sheet, the
     * weapon draws nothing in hand rather than failing resource load.
     */
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
