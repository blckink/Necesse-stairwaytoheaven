package stairwaytoheaven.quest.ladder;

import necesse.engine.modifiers.ModifierValue;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.SimpleTrinketBuff;
import necesse.inventory.item.Item;
import necesse.level.maps.levelData.settlementData.settler.Settler;

/**
 * The quest ladder's unique rewards: nine accessories and drinks and one
 * greatsword, each paid by exactly one step and sold, dropped or crafted
 * nowhere ({@code docs/design/chapter-03-spire-village.md} §5 has the table and
 * the reasoning). Plus the Doctor's Blood-Fever Tincture, which is Dorian's
 * and is registered here only because it is another shop-only item of this
 * pass.
 *
 * <h2>Every number names its vanilla anchor</h2>
 * (the rule of {@code docs/PLAN_ONE_PLANE.md}). All anchors VERIFIED [jar] in
 * the decompiled 1.3.2 {@code BuffRegistry}/{@code ItemRegistry}:
 * <pre>
 *   itemattractortrinket   ITEM_PICKUP_RANGE 5.0          (BuffRegistry.java:731)
 *   zephyrcharmtrinket     STAMINA_CAPACITY 0.5
 *   miningcharmtrinket     TOOL_DAMAGE 0.4                (:705)
 *   spelunkerpotion        SPELUNKER true                 (:448)
 *   trackerpotion          ENEMY_TRACKER true             (:450)
 *   frozenwavetrinket      CRIT_DAMAGE 0.25               (:759)
 *   fuzzydicetrinket       CRIT_CHANCE 0.05               (:760)
 *   frozenhearttrinket     MAX_HEALTH_FLAT 50             (:758)
 *   lifependanttrinket     COMBAT_HEALTH_REGEN_FLAT 1.0   (:727)
 *   templependanttrinket   DASH_COOLDOWN -0.25            (:775)
 *   spikedbootstrinket     SPEED 0.15
 *   juniorburger / cheeseburger (gourmet, 720 s)          (ItemRegistry.java:2595-2625)
 * </pre>
 */
public final class LadderItems {

    private LadderItems() {
    }

    /** Every item this class registers, for {@code SkyItems.ALL_ITEM_IDS}' probe. */
    public static final String[] IDS = {
            "magpiesatchel", "stormbrew", "readerslens", "paradisecider", "echoconch",
            "vergerlantern", "memoryblade", "mourningbrooch", "rootcrown", "knottkeyring",
            "auditorsseal", "bloodfevertincture",
    };

    public static void register() {
        registerBuffs();

        // I. Magpie — the courier's satchel. Pickup range is vanilla's item
        // attractor at full value; half a zephyr charm of stamina because a
        // courier carries. Disables the attractor it contains.
        ItemRegistry.registerItem("magpiesatchel",
                new stairwaytoheaven.items.LadderTrinketItem("explorersatchel",
                        Item.Rarity.RARE, "magpiesatcheltrinket", 600)
                        .addDisables("itemattractor"), 120.0F, true);
        // I. Halda — Storm Brew: a fine drink, a juniorburger's 10% speed for
        // the same 720 s plus a fine dish's 5% attack speed; three per turn-in,
        // then her shop.
        ItemRegistry.registerItem("stormbrew",
                new stairwaytoheaven.items.LadderFoodItem("unlabeledpotion", 20, Item.Rarity.UNCOMMON,
                        Settler.FOOD_FINE, 20, 720,
                        new ModifierValue<>(BuffModifiers.SPEED, 0.10F),
                        new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.05F)), 30.0F, true);
        // I. Ossian — the Reader's Lens: a spelunker potion that never runs out,
        // and half a mining charm.
        ItemRegistry.registerItem("readerslens",
                new stairwaytoheaven.items.LadderTrinketItem("scryingmirror",
                        Item.Rarity.RARE, "readerslenstrinket", 700), 140.0F, true);
        // II. Halda — Paradise Cider: a gourmet drink one notch under a
        // cheeseburger's health (40 of 50) and regen, with a 3% crit instead of
        // its speed. The Eden rung pays more than the Skyreach one.
        ItemRegistry.registerItem("paradisecider",
                new stairwaytoheaven.items.LadderFoodItem("passivepotion", 20, Item.Rarity.RARE,
                        Settler.FOOD_GOURMET, 30, 900,
                        new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 40),
                        new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 0.5F),
                        new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.03F)), 45.0F, true);
        // III. Ossian — the Echo Conch: a frozen wave's crit damage whole, and
        // three of a fuzzy die's five points of crit. Disables the wave.
        ItemRegistry.registerItem("echoconch",
                new stairwaytoheaven.items.LadderTrinketItem("prophecyslab",
                        Item.Rarity.EPIC, "echoconchtrinket", 1400)
                        .addDisables("frozenwave"), 220.0F, true);
        // III. Ives — the Verger's Lantern: a tracker potion that never runs
        // out (he sees the ones who still walk), and 60% of a frozen heart.
        ItemRegistry.registerItem("vergerlantern",
                new stairwaytoheaven.items.LadderTrinketItem("lantern",
                        Item.Rarity.EPIC, "vergerlanterntrinket", 1400), 220.0F, true);
        // IV. Caspern — the Remembering Blade (numbers in MemoryBlade).
        ItemRegistry.registerItem("memoryblade", new stairwaytoheaven.items.MemoryBlade(), 460.0F, true);
        // IV. Mortimer — the Mourning Brooch: 8 armour and half a frozen
        // heart. Flat armour has no vanilla trinket of its own; 8 is a third
        // of the Stormsteel helm's 26 and stays under the Ghost set's gap.
        ItemRegistry.registerItem("mourningbrooch",
                new stairwaytoheaven.items.LadderTrinketItem("companionlocket",
                        Item.Rarity.EPIC, "mourningbroochtrinket", 1800), 260.0F, true);
        // V. Eveleen — the Root Crown: the Aurora Locket's two stats one notch
        // up (60 health for 50, the same lifependant regen). Disables the
        // locket and both anchors, like the locket does.
        ItemRegistry.registerItem("rootcrown",
                new stairwaytoheaven.items.LadderTrinketItem("dryadcrown",
                        Item.Rarity.EPIC, "rootcrowntrinket", 2400)
                        .addDisables("auroralocket", "frozenheart", "lifependant", "regenpendant"),
                320.0F, true);
        // V. Mr. Knott — Knott's Key Ring: a temple pendant's dash and a
        // tracker boot's speed. Disables the pendant.
        ItemRegistry.registerItem("knottkeyring",
                new stairwaytoheaven.items.LadderTrinketItem("ignitionkey",
                        Item.Rarity.EPIC, "knottkeyringtrinket", 2400)
                        .addDisables("templependant"), 320.0F, true);
        // VI. Ossian — the Auditor's Seal: the only 10% all-damage trinket in
        // the mod, and a fuzzy die. Hell is past the game (BALANCE.md §5); this
        // is the ladder's last rung and the one number here that is a
        // HYPOTHESIS until Hell has been played through.
        ItemRegistry.registerItem("auditorsseal",
                new stairwaytoheaven.items.LadderTrinketItem("templependant",
                        Item.Rarity.LEGENDARY, "auditorssealtrinket", 3600), 480.0F, true);
        // Dorian's concept, E5: the Doctor's cure in a bottle.
        ItemRegistry.registerItem("bloodfevertincture",
                new stairwaytoheaven.items.BloodFeverTinctureItem(), 25.0F, true);
    }

    private static void registerBuffs() {
        BuffRegistry.registerBuff("magpiesatcheltrinket", new SimpleTrinketBuff(
                new ModifierValue<>(BuffModifiers.ITEM_PICKUP_RANGE, 5.0F),
                new ModifierValue<>(BuffModifiers.STAMINA_CAPACITY, 0.25F)));
        BuffRegistry.registerBuff("readerslenstrinket", new SimpleTrinketBuff(
                "readerslenssight",
                new ModifierValue<>(BuffModifiers.SPELUNKER, true),
                new ModifierValue<>(BuffModifiers.TOOL_DAMAGE, 0.20F)));
        BuffRegistry.registerBuff("echoconchtrinket", new SimpleTrinketBuff(
                new ModifierValue<>(BuffModifiers.CRIT_DAMAGE, 0.25F),
                new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.03F)));
        BuffRegistry.registerBuff("vergerlanterntrinket", new SimpleTrinketBuff(
                "vergerlanternsight",
                new ModifierValue<>(BuffModifiers.ENEMY_TRACKER, true),
                new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 30)));
        BuffRegistry.registerBuff("mourningbroochtrinket", new SimpleTrinketBuff(
                new ModifierValue<>(BuffModifiers.ARMOR_FLAT, 8),
                new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 25)));
        BuffRegistry.registerBuff("rootcrowntrinket", new SimpleTrinketBuff(
                new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 60),
                new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 1.0F)));
        BuffRegistry.registerBuff("knottkeyringtrinket", new SimpleTrinketBuff(
                new ModifierValue<>(BuffModifiers.DASH_COOLDOWN, -0.25F),
                new ModifierValue<>(BuffModifiers.SPEED, 0.10F)));
        BuffRegistry.registerBuff("auditorssealtrinket", new SimpleTrinketBuff(
                new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.10F),
                new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.05F)));
    }
}
