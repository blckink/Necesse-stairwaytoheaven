package stairwaytoheaven.arsenal;

import java.awt.Color;
import java.awt.Point;
import java.io.FileNotFoundException;

import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MouseBeamLevelEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.attackHandler.MouseBeamAttackHandler;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.enchants.ToolItemModifiers;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemStatTipList;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.MagicProjectileToolItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import necesse.level.maps.Level;

/**
 * Himmelslanze / Sky Lance — vanilla's Dragon Lance, re-made as a lance of
 * held daylight: the same channelled beam, in gold instead of dragonfire.
 *
 * <h2>What the Dragon Lance is — VERIFIED [jar]</h2>
 * {@code DragonLanceProjectileToolItem} is not a projectile weapon at all
 * despite its package. Its {@code onAttack} starts a
 * {@code MouseBeamLevelEvent} — a beam that follows the cursor, hits every
 * mob along it once per {@code hitCooldown} (250 ms, i.e. four ticks a
 * second) and costs mana per second through a {@code MouseBeamAttackHandler}
 * — and the wielder stands still while it runs
 * ({@code getFinalAttackMovementMod} returns 0). Its numbers:
 *
 * <pre>
 *   enchant 1800   RARE   80.0 -> 126.00004   range 400   knockback 10
 *   mana 10 on start + 10/s   resilience 1.0   cooldown 500 ms   beam colour (212, 75, 41)
 *   sources: Sage and Grit's shared drop pool; FALLEN_ANVIL, 20 primordial essence
 * </pre>
 *
 * This class copies that behaviour line for line. It does not EXTEND the
 * vanilla class because that constructor registers into
 * {@code MagicWeaponsLootTable.magicWeapons}, and ToolItem keeps every table it
 * is handed ({@code ToolItem.addToLootTable}); a crafted weapon must not also
 * roll out of chests.
 *
 * <h2>Damage — calibrated per TICK, because armour is</h2>
 * Necesse subtracts armour flat ({@code armor * 0.5F} against a player's
 * attack, {@code DamageType.getDamageReduction}, VERIFIED [jar]), and a beam
 * pays that subtraction four times a second. So the number that matters is
 * damage per second AFTER armour, and the yardstick is the mod's own staff one
 * realm back: the Prismcaller, 118 per 600 ms cast, puts
 * (118 − 25) / 0.6 = 155 per second into a Skyreach mob (50 armour, 1300 HP,
 * {@code docs/BALANCE.md} §10) — an 8.4 s kill. This lance is the first weapon
 * past the Stormsteel band, so it is set to kill an EDEN mob (56 armour,
 * 2025 HP) a little faster than that staff kills a Skyreach one:
 *
 * <pre>
 *   95 per tick:  (95 − 28) × 4 = 268 per second  -> 7.6 s on an Eden mob
 *   Dragon Lance: (80 − 28) × 4 = 208 per second  -> 9.7 s on the same mob
 * </pre>
 *
 * <b>95.0</b>, with the Dragon Lance's own upgrade ratio
 * (126.00004 / 80.0 = 1.5750005): <b>149.63</b> at forge tier 1. It pays for
 * that the way the Dragon Lance does — rooted in place, draining mana every
 * second it is held — and it is not a Skyreave-style ×2.5 of its vanilla twin
 * on purpose: a beam that ticks four times a second multiplies every point of
 * damage by four, and the vanilla weapon is already the strongest magic weapon
 * its tier has against armour.
 *
 * <p>Enchant cost <b>2000</b> — past the incursion floor's 1900, short of
 * Spiritsteel's 2400, the same rung as the Wolkengleve. EPIC, the tier's
 * rarity, one step up from the Dragon Lance's RARE. Everything that is the
 * weapon's SHAPE rather than its tier stays the Dragon Lance's: range 400,
 * knockback 10, 500 ms cooldown, mana 10, resilience 1.0.
 *
 * <h2>Where it comes from — a recipe, gated on Aetherwright's Casing</h2>
 * The Dragon Lance is crafted in vanilla (FALLEN_ANVIL, 20 primordial
 * essence), so its adaptation is crafted too — and not handed out by yet
 * another quest. {@code chapter-01-skyreach-cast.md} §3 already names the
 * material for exactly this weapon: Aetherwright's Casing is <i>"the gate
 * material of the first weapon tier past Stormsteel — the one the arsenal is
 * currently missing"</i>, and before this recipe nothing consumed it. The
 * rest of the recipe is Eden's: Eden Bronze (which had no recipe consumer
 * either) and Golden Pollen, so the lance can only be made by someone who has
 * been through the Eden Gate. See {@link SkyArsenal#registerRecipes()}.
 *
 * <h2>Art</h2>
 * Icon and held sheet are vanilla's {@code dragonlance}, re-inked by
 * {@link RecolouredVanillaTexture#whiteGold}. The beam itself is vanilla's
 * {@code ParticleBeamHandler} drawing {@code GameResources.chains}, tinted by
 * the colour handed to the event, so the beam turns gold by that one argument.
 * A mod PNG under this item's own name wins over the recolour once shipped.
 */
public class SkyLanceToolItem extends MagicProjectileToolItem {

    /** The vanilla item whose icon and held sheet this re-inks. */
    public static final String ART = "dragonlance";

    /** Held daylight — warm gold, light enough to read as white at the core. */
    public static final Color BEAM = new Color(255, 214, 120);

    public SkyLanceToolItem() {
        super(2000, (OneOfLootItems) null);                  // DragonLance 1800; this rung 2000
        this.rarity = Item.Rarity.EPIC;                      // DragonLance RARE; the tier is EPIC
        this.attackAnimTime.setBaseValue(2000);              // DragonLance 2000
        // 95 per tick, see the class doc; DragonLance's ratio 126.00004/80 = 1.5750005
        this.attackDamage.setBaseValue(95.0F).setUpgradedValue(1.0F, 149.62505F);
        this.knockback.setBaseValue(10);                     // DragonLance 10
        this.velocity.setBaseValue(120);                     // DragonLance 120
        this.attackCooldownTime.setBaseValue(500);           // DragonLance 500
        this.attackRange.setBaseValue(400);                  // DragonLance 400
        this.attackXOffset = 20;                             // DragonLance's sheet pivot
        this.attackYOffset = 20;
        this.manaCost.setBaseValue(10.0F).setUpgradedValue(1.0F, 10.0F); // DragonLance 10
        this.resilienceGain.setBaseValue(1.0F);              // DragonLance 1.0
        this.itemAttackerProjectileCanHitWidth = 5.0F;       // DragonLance 5.0
        this.canBeUsedForRaids = false;                      // DragonLance false
    }

    @Override
    public float getFinalAttackMovementMod(InventoryItem item, ItemAttackerMob attackerMob) {
        return 0.0F;
    }

    @Override
    public int getAttackAnimTime(InventoryItem item, ItemAttackerMob attackerMob) {
        return item.getGndData().getBoolean("charging") ? 2000 : super.getAttackAnimTime(item, attackerMob);
    }

    @Override
    public boolean animDrawBehindHand(InventoryItem item) {
        return false;
    }

    @Override
    public Point getItemAttackerAttackPosition(Level level, ItemAttackerMob attackerMob, Mob target, int seed,
                                               InventoryItem item) {
        return this.applyInaccuracy(attackerMob, item, new Point(target.getX(), target.getY()));
    }

    @Override
    protected SoundSettings getSwingSound() {
        return new SoundSettings(GameResources.magicbolt1).basePitch(1.4F).volume(0.3F);
    }

    @Override
    protected SoundSettings getAttackSound() {
        // The Dragon Lance's hum, pitched up a little: lighter, not heavier.
        return new SoundSettings(GameResources.dragonLance).basePitch(1.1F).volume(0.05F);
    }

    @Override
    public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective,
                                                     GameBlackboard blackboard) {
        ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
        tooltips.add(Localization.translate("itemtooltip", "skylancetip"));
        return tooltips;
    }

    @Override
    public void addStatTooltips(ItemStatTipList list, InventoryItem currentItem, InventoryItem lastItem,
                                ItemAttackerMob perspective, boolean forceAdd) {
        this.addAttackDamageTip(list, currentItem, lastItem, perspective, forceAdd);
        this.addResilienceGainTip(list, currentItem, lastItem, perspective, forceAdd);
        this.addCritChanceTip(list, currentItem, lastItem, perspective, forceAdd);
        this.addManaCostTip(list, currentItem, lastItem, perspective);
    }

    @Override
    public int getItemAttackerAttackRange(ItemAttackerMob mob, InventoryItem item) {
        return (int) ((float) this.getAttackRange(item) * 0.8F);
    }

    @Override
    public InventoryItem onAttack(Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight,
                                  InventoryItem item, ItemAttackSlot slot, int animAttack, int seed,
                                  GNDItemMap mapContent) {
        float enchantmentSpeedModifier = this.getEnchantment(item)
                .applyModifierLimited(ToolItemModifiers.ATTACK_SPEED, ToolItemModifiers.ATTACK_SPEED.defaultBuffValue);
        MouseBeamLevelEvent event = new MouseBeamLevelEvent(attackerMob, x, y, seed, 50.0F,
                (float) this.getAttackRange(item), this.getAttackDamage(item),
                this.getKnockback(item, attackerMob), null, 250, enchantmentSpeedModifier, 0,
                this.getResilienceGain(item), BEAM);
        attackerMob.addAndSendAttackerLevelEvent(event);
        float startManaCost = this.getManaCost(item);
        if (startManaCost > 0.0F) {
            this.consumeMana(startManaCost, attackerMob);
        }
        attackerMob.startAttackHandler(new MouseBeamAttackHandler(attackerMob, slot, 75, seed, event)
                .setManaCostPerSecond(this.getManaCost(item)));
        return item;
    }

    // --- art: a shipped mod PNG wins; otherwise vanilla's, re-inked --------

    @Override
    protected void loadItemTextures() {
        try {
            this.itemTexture = GameTexture.fromFileRaw("items/" + this.getStringID());
        } catch (FileNotFoundException none) {
            this.itemTexture = RecolouredVanillaTexture.whiteGoldOrError(
                    "items/" + ART, "items/" + this.getStringID());
        }
    }

    @Override
    protected void loadAttackTexture() {
        try {
            this.attackTexture = GameTexture.fromFileRaw("player/weapons/" + this.getStringID());
        } catch (FileNotFoundException none) {
            try {
                this.attackTexture = RecolouredVanillaTexture.whiteGold(
                        "player/weapons/" + ART, "player/weapons/" + this.getStringID());
            } catch (FileNotFoundException alsoNone) {
                this.attackTexture = null;                   // Item.loadAttackTexture's own fallback
            }
        }
    }
}
