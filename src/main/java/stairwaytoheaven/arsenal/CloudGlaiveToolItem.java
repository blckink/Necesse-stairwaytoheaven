package stairwaytoheaven.arsenal;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.FileNotFoundException;

import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.entity.levelEvent.GlaiveShowAttackEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.glaiveToolItem.GlaiveToolItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import necesse.level.maps.Level;

/**
 * Wolkengleve / Cloud Glaive — the Skywatch's own glaive, handed over by the
 * Sky Warden when the spire island is anchored ({@code AnchorDeliveryQuest},
 * the finale of "The Warden's Call"). The Cryo Queen's frost glaive, re-made
 * in cloud-white and gold.
 *
 * <h2>Where it sits</h2>
 * Three glaives bracket it, every number VERIFIED [jar]:
 *
 * <pre>
 *   CryoGlaiveToolItem     vanilla, Cryo Queen   1500  EPIC  400 ms   60.0 -> 75.83335  range 160  kb 100  pivot 58
 *   SlimeGlaiveToolItem    vanilla, incursion    1900  EPIC  400 ms   60.0 -> 75.83335  range 200  kb 150  pivot 74
 *   SkyreaveGlaiveToolItem this mod, crafted     1900  EPIC  400 ms  150.0 -> 189.58337 range 150  kb 150  pivot 48
 *   SpiritsteelReaver      this mod, Ghost tier  2400  EPIC  (greatsword) 176 -> 219
 * </pre>
 *
 * The brief was "stronger than the Cryo Glaive", and every weapon in this mod
 * already is — the Skyreave hits two and a half times as hard. The question
 * that matters is where a QUEST REWARD sits against the glaive the same player
 * can already craft. A finale reward weaker than the recipe next to it is not
 * a reward, so it sits above the Skyreave; and it must stay below the Ghost
 * Realm's Spiritsteel Reaver, or the three realms after the Skyreach have
 * nothing to hand out. The Wolkengleve is what carries a player through the
 * two realms in between — Eden and Steinfeld — so it lands between the two:
 * <b>165.0</b>, the Skyreave ×1.10, against the Reaver's 176.
 *
 * <p>The upgrade ratio is the one every vanilla glaive above uses
 * (75.83335 / 60.0 = 1.2638892), so the forge-tier-1 value is
 * 165.0 × 1.2638892 = <b>208.54</b>. Against the realms it is meant for,
 * with armour subtracted flat at {@code armor * 0.5F}
 * ({@code DamageType.getDamageReduction}, VERIFIED [jar]) and the shipped
 * armour in {@code docs/BALANCE.md} §10: 137 through an Eden mob's 56 and
 * 134 through a Steinfeld mob's 62, where the Skyreave does 122 and 119.
 *
 * <h2>Shape — the Cryo Glaive's, deliberately</h2>
 * Swing 400 ms (every glaive), width 20.0 (every glaive). <b>Range 160 and
 * pivot 58 are the Cryo Glaive's own</b>: the held sprite IS the Cryo
 * Glaive's sheet (120x108 in {@code tools/vanilla_sizes.json}), and vanilla
 * keeps range and pivot in step (pivot ≈ 0.4 × range − 6), so that sheet
 * carries exactly this reach. It is also ten pixels more than the Skyreave's
 * 150, which is held back by its own 96 px sheet. Knockback is the incursion
 * glaive's 150 rather than the Cryo Glaive's 100 — the tier's number, the same
 * choice the Skyreave makes.
 *
 * <p>Enchant cost <b>2000</b>: one step past the Stormsteel/incursion floor's
 * 1900 and short of Spiritsteel's 2400 ({@code BALANCE.md} §7), which is the
 * band it is carried through. Rarity EPIC, the tier's rarity.
 *
 * <h2>Loot table: deliberately null</h2>
 * A quest reward that also rolls out of a chest is not one. Same null, same
 * reason, as {@code realms/ghost/SpiritsteelReaver}.
 *
 * <h2>The swing</h2>
 * The Cryo Glaive's own {@code showAttack} (CryoGlaiveToolItem.java:37-53):
 * a {@link GlaiveShowAttackEvent} that drops a light-giving particle off
 * each end of the blade, 75 px out, every tick of the sweep. Kept whole, with
 * frost turned into daylight — the two ends leave warm gold and cloud-white
 * motes instead of cyan ones, and the light they give is gold. Same sound.
 * This class does not EXTEND {@code CryoGlaiveToolItem}: its constructor
 * registers into {@code GlaiveWeaponsLootTable.glaiveWeapons}, and ToolItem
 * keeps every table it is handed ({@code ToolItem.addToLootTable}).
 *
 * <h2>Art — vanilla's, re-inked at load time</h2>
 * Icon and swing sheet are vanilla's {@code cryoglaive}, run through
 * {@link RecolouredVanillaTexture#whiteGold} on the client. A mod PNG under
 * this item's own name ({@code items/cloudglaive.png},
 * {@code player/weapons/cloudglaive.png}) wins over the recolour the moment
 * one is shipped, so the stand-in swaps out without touching this class.
 * Rows in {@code docs/VANILLA_ASSET_MAP.md} §1.3.
 */
public class CloudGlaiveToolItem extends GlaiveToolItem {

    /** The vanilla item whose icon and swing sheet this re-inks. */
    public static final String ART = "cryoglaive";

    /** The two mote colours: warm gold and cloud white. */
    private static final Color GOLD = new Color(255, 206, 96);
    private static final Color CLOUD = new Color(255, 250, 236);

    public CloudGlaiveToolItem() {
        super(2000, (OneOfLootItems) null);                 // between 1900 (Stormsteel) and 2400 (Spiritsteel)
        this.rarity = Item.Rarity.EPIC;                     // CryoGlaive / SlimeGlaive: EPIC
        this.attackAnimTime.setBaseValue(400);              // every glaive: 400 ms
        // Skyreave 150 x 1.10; glaive upgrade ratio 75.83335/60.0 = 1.2638892
        this.attackDamage.setBaseValue(165.0F).setUpgradedValue(1.0F, 208.54172F);
        this.attackRange.setBaseValue(160);                 // CryoGlaive 160 -- its sheet, its reach
        this.knockback.setBaseValue(150);                   // SlimeGlaive 150 (CryoGlaive 100)
        this.width = 20.0F;                                 // every glaive: 20.0
        this.attackXOffset = 58;                            // CryoGlaive's sheet pivot
        this.attackYOffset = 58;
        // A 165-damage glaive must not turn up in a raid against someone who
        // never earned one: ToolItem.getRaiderTicketModifier returns 0 when
        // useForRaidsOnlyIfObtained is set and the world has not obtained it.
        this.canBeUsedForRaids = true;
        this.useForRaidsOnlyIfObtained = true;              // CryoGlaive true
        this.raidTicketsModifier = 0.25F;                   // SlimeGlaive 0.25F (CryoGlaive 0.5F)
    }

    @Override
    public void showAttack(Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight,
                           InventoryItem item, int animAttack, int seed, GNDItemMap mapContent) {
        super.showAttack(level, x, y, attackerMob, attackHeight, item, animAttack, seed, mapContent);
        if (level.isClient()) {
            level.entityManager.events.addHidden(new GlaiveShowAttackEvent(attackerMob, x, y, seed, 10.0F) {
                private boolean flip;

                @Override
                public void tick(float angle) {
                    Point2D.Float dir = this.getAngleDir(angle);
                    // Alternate which end is gold, so the ring the sweep draws
                    // reads as gold AND white rather than as two coloured arcs.
                    this.flip = !this.flip;
                    mote(this.level, this.attackMob, dir.x * 75.0F, dir.y * 75.0F, this.flip ? GOLD : CLOUD);
                    mote(this.level, this.attackMob, -dir.x * 75.0F, -dir.y * 75.0F, this.flip ? CLOUD : GOLD);
                }
            });
        }
    }

    private static void mote(Level level, necesse.entity.mobs.AttackAnimMob mob, float dx, float dy, Color color) {
        level.entityManager.addParticle(
                        mob.x + dx + (float) mob.getCurrentAttackDrawXOffset(),
                        mob.y + dy + (float) mob.getCurrentAttackDrawYOffset(),
                        Particle.GType.COSMETIC)
                .color(color)
                .minDrawLight(150)                          // CryoGlaive 150
                .givesLight(45.0F, 0.5F)                    // CryoGlaive (179 cyan, 1.0) -> gold, softer
                .lifeTime(400);                             // CryoGlaive 400
    }

    @Override
    protected SoundSettings getAttackSound() {
        return new SoundSettings(GameResources.cryoGlaive).volume(0.4F); // CryoGlaive's own
    }

    @Override
    public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective,
                                                     GameBlackboard blackboard) {
        ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
        tooltips.add(Localization.translate("itemtooltip", "cloudglaivetip"));
        return tooltips;
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
                this.attackTexture = null;                  // Item.loadAttackTexture's own fallback
            }
        }
    }
}
