# Necesse 1.3.2 — Registration & Content API Reference

**Source of truth:** decompiled game sources at `/home/user/necesse-game/decompiled/necesse/` (local
reference only, read-only for this research pass). Every claim below was checked directly against
that code (grep + targeted `Read` of the relevant line ranges) during this session. Per the legal
constraint on this task, no multi-line method **bodies** are reproduced — only signatures,
constructor parameter meanings, and enum/constant lists, transcribed as plain signatures/tables. The
one exception is a single literal error-message string in §8, quoted because it *is* the evidence
for the most load-bearing claim in this document.

Anything I could not fully verify by reading the implementation is marked **UNCERTAIN** with a
pointer to what to check next.

---

## 0. The registry base classes (context for everything below)

All content registries extend `necesse.engine.registries.GameRegistry<T>`
(`engine/registries/GameRegistry.java`). Key mechanics that apply uniformly:

- Every registry starts **open** and is force-closed exactly once by `GlobalData.loadAll` (see §12).
  Registering after close throws `RegistryClosedException`.
- `register(stringID, object)` assigns the next integer ID, stores the `stringID → ID` mapping, and
  rejects duplicate stringIDs (`IllegalStateException`) or invalid ones (must match
  `[a-zA-Z0-9_\-]+`, i.e. `GameRegistry.stringIDPattern`).
- Two registration styles exist:
  - **Instance-based** (`ItemRegistry`, `ObjectRegistry`, `TileRegistry`, `BiomeRegistry`): you
    construct the object yourself and hand the *instance* to `registerX(stringID, instance, ...)`.
  - **Class-based via reflection** (`MobRegistry`, `LevelRegistry`, and the generic
    `EmptyConstructorGameRegistry`): you hand a `Class<? extends T>` to the registry, which looks up
    a specific constructor via `Class.getConstructor(...)` and instantiates on demand later. This is
    why Mob and Level classes have hard constructor-shape requirements — see §6 and §8.
- Items, Objects, and Tiles all auto-generate a companion `Item` when registered (see §4.6/§5.3).

---

## 1. ItemRegistry — items (materials, sword, bow)

File: `engine/registries/ItemRegistry.java` (3630 lines; overloads at lines 3426–3471).

### 1.1 `registerItem` overloads

All are `public static int`, defined in `ItemRegistry`. Parameter meanings (shared across every
overload):

| Param | Meaning |
|---|---|
| `String stringID` | Unique registry key, becomes the item's save/network identity. |
| `Item item` | The constructed item instance (already has textures/behavior wired via its own constructor). |
| `float brokerValue` | Base sell/shop value. **If negative**, its absolute value is used as a multiplier fed into `RecipeBrokerValueCompute` at the very end of boot (`ItemRegistry.calculateBrokerValues()`, called from `GlobalData.loadAll` after all recipes load) — the value is then auto-computed from the item's cheapest recipe cost. This is why almost every vanilla registration passes `-1.0F` rather than a hand-picked number. |
| `boolean isObtainable` | Whether normal survival play can obtain this item (affects e.g. "% items found" stats and default creative-obtainability, see below). |
| `boolean countInStats` | Whether obtaining this item counts toward the player's completion/collection stats. |
| `boolean isObtainableInCreative` | Whether the item shows in the Creative inventory picker. |
| `String... isObtainedByOtherItemStringIDs` / `List<String> isObtainedByOtherItemStringIDs` | StringIDs of other items whose obtainment should *also* count this item as obtained (e.g. a crate that contains it). |

Overload set (5 total):

```
registerItem(String stringID, Item item, float brokerValue, boolean isObtainable)
registerItem(String stringID, Item item, float brokerValue, boolean isObtainable,
             boolean countInStats, String... isObtainedByOtherItemStringIDs)
registerItem(String stringID, Item item, float brokerValue, boolean isObtainable,
             boolean countInStats, boolean isObtainableInCreative, String... isObtainedByOtherItemStringIDs)
registerItem(String stringID, Item item, float brokerValue, boolean isObtainable,
             boolean countInStats, List<String> isObtainedByOtherItemStringIDs)
registerItem(String stringID, Item item, float brokerValue, boolean isObtainable,
             boolean countInStats, boolean isObtainableInCreative, List<String> isObtainedByOtherItemStringIDs)
```

The 4-arg form delegates with `countInStats = isObtainable` and `isObtainableInCreative = isObtainable`
— i.e. the common case ("this is a normal obtainable item") only needs 4 args. All overloads throw
`IllegalStateException` if called from a clientside-only mod (`LoadedMod.isRunningModClientSide()`)
— items must be registered identically on client and server.

There's a mirrored `replaceItem(...)` family with identical signatures, used to override an
already-registered item (own or another mod's/vanilla's) rather than add a new one.

### 1.2 A simple material — `MatItem`

File: `inventory/item/matItem/MatItem.java`. Extends `Item` directly. Constructors:

```
MatItem(int stackSize, String... globalIngredients)
MatItem(int stackSize, Item.Rarity rarity, String... globalIngredients)
MatItem(int stackSize, Item.Rarity rarity, String tooltipKey)
MatItem(int stackSize, Item.Rarity rarity, String tooltipKey, String... globalIngredients)
```

- `stackSize` — max stack in inventory.
- `globalIngredients` — stringIDs registered in `GlobalIngredientRegistry` that this item should
  count as for recipes (e.g. "any wood plank"); optional, usually omitted for a plain material.
  Wires up via `Item.addGlobalIngredient(...)` in the base `Item` constructor path.
  MatItem also sets `dropsAsMatDeathPenalty = true` and the item category to `{"materials"}`.
- `tooltipKey` — if set, appends `Localization.translate("itemtooltip", tooltipKey)` to the item's
  tooltip (used for special crafting-material blurbs, e.g. compost/mill hints).
- `rarity` — sets `Item.rarity` (see §1.5), defaults to `Item.Rarity.NORMAL` if omitted.

Real vanilla registration (`ItemRegistry.java:886`):

```
registerItem("tungstenbar", new MatItem(250, Item.Rarity.UNCOMMON).setItemCategory(new String[]{"materials", "bars"}), 20.0F, true);
```
(250 stack, uncommon rarity, custom category override, broker value 20, obtainable.)

### 1.3 A sword — `SwordToolItem` (mid-tier example: Tungsten)

Class hierarchy: `Item` → `ToolItem` (`inventory/item/toolItem/ToolItem.java`) → `SwordToolItem`
(`inventory/item/toolItem/swordToolItem/SwordToolItem.java`) → concrete sword, e.g.
`TungstenSwordToolItem` (`inventory/item/toolItem/swordToolItem/TungstenSwordToolItem.java`).

**Constructors are NOT where damage/speed/knockback go.** Both `ToolItem` and `SwordToolItem` take
only:

```
ToolItem(int enchantCost, OneOfLootItems lootTableCategory)
SwordToolItem(int enchantCost, OneOfLootItems lootTableCategory)   // calls super(enchantCost, lootTableCategory)
```

- `enchantCost` — base "essence" cost at the enchanting altar; stored in the `enchantCost`
  `IntUpgradeValue` field (see below).
- `lootTableCategory` — a shared `OneOfLootItems` bucket (e.g.
  `CloseRangeWeaponsLootTable.closeRangeWeapons`) that this weapon self-registers into via
  `ToolItem.addToLootTable(...)`, called from the `ToolItem` constructor. This is how "random sword
  from a chest" loot tables stay in sync automatically — every sword that wants to be eligible just
  passes the shared bucket constant into its constructor.

Damage/speed/range/knockback/enchant-cost are `protected` mutable fields on `ToolItem`, each of type
`FloatUpgradeValue` or `IntUpgradeValue` (`inventory/item/upgradeUtils/`), set via fluent setters in
the **subclass constructor body**, after `super(...)`:

| Field | Type | Meaning |
|---|---|---|
| `attackDamage` | `FloatUpgradeValue` | Base damage via `.setBaseValue(x)`. |
| `attackAnimTime` | `IntUpgradeValue` | Swing duration in ms — this is the "speed" knob (lower = faster). |
| `attackRange` | `IntUpgradeValue` | Melee reach in pixels. |
| `knockback` | `IntUpgradeValue` | Knockback strength. |
| `enchantCost` | `IntUpgradeValue` | Set from the constructor's `enchantCost` param; `.setUpgradedValue(1.0F, 2000)` is baked in by `ToolItem`'s own constructor as the tier-1 upgrade cost. |
| `resilienceGain`, `manaCost`, `lifeSteal`, `lifeCost` | upgrade values | Secondary combat stats; `SwordToolItem` tunes `resilienceGain`. |

`.setBaseValue(v)` sets the value at upgrade tier 0; `.setUpgradedValue(tier, v)` pins the value at a
specific forge-upgrade tier — the upgrade system interpolates/extrapolates between whatever points
you define. A weapon that never calls `.setUpgradedValue` just doesn't get extra scaling beyond
whatever default per-tier multiplier the field's own constructor set.

Real vanilla mid-tier example, in full (`TungstenSwordToolItem.java`, 18 lines, quoted here as it is
short enough to be the clearest illustration of the pattern):

```
public TungstenSwordToolItem() {
   super(1300, CloseRangeWeaponsLootTable.closeRangeWeapons);
   this.rarity = Item.Rarity.UNCOMMON;
   this.attackAnimTime.setBaseValue(300);
   this.attackDamage.setBaseValue(65.0F).setUpgradedValue(1.0F, 93.33336F);
   this.attackRange.setBaseValue(80);
   this.knockback.setBaseValue(100);
   this.canBeUsedForRaids = true;
   this.maxRaidTier = IncursionData.ITEM_TIER_UPGRADE_CAP;
}
```
Registered as: `registerItem("tungstensword", new TungstenSwordToolItem(), 200.0F, true);`
(`canBeUsedForRaids`/`maxRaidTier` are Incursion-raid-loadout flags — safe to omit for a basic mod
weapon.)

**Texture wiring**: `TungstenSwordToolItem` has **no `loadTextures()` override** — it relies entirely
on the base `Item` convention (see §1.6). So for a new sword you only need to drop
`resources/items/<stringid>.png` (icon) and optionally
`resources/player/holditems/<stringid>.png` (held-in-hand sprite) and
`resources/player/weapons/<stringid>.png` (mid-swing sprite) — no code needed for textures at all.

### 1.4 A bow — `BowProjectileToolItem`

Hierarchy: `Item` → `ToolItem` → `ProjectileToolItem` (`.../projectileToolItem/ProjectileToolItem.java`)
→ `BowProjectileToolItem` (`.../projectileToolItem/bowProjectileToolItem/BowProjectileToolItem.java`)
→ concrete bow, e.g. `TungstenBowProjectileToolItem`.

Constructor, same two-arg shape as swords:

```
ProjectileToolItem(int enchantCost, OneOfLootItems lootTableCategory)
BowProjectileToolItem(int enchantCost, OneOfLootItems lootTableCategory)
```

`ProjectileToolItem` adds a `velocity` (`IntUpgradeValue`) field for projectile speed. Real example
(`TungstenBowProjectileToolItem.java`, full file):

```
public TungstenBowProjectileToolItem() {
   super(1300, BowWeaponsLootTable.bowWeapons);
   this.rarity = Item.Rarity.UNCOMMON;
   this.attackAnimTime.setBaseValue(500);
   this.attackDamage.setBaseValue(60.0F).setUpgradedValue(1.0F, 114.33337F);
   this.attackRange.setBaseValue(800);
   this.velocity.setBaseValue(200);
   this.attackXOffset = 12;
   this.attackYOffset = 28;
}
```
Registered as: `registerItem("tungstenbow", new TungstenBowProjectileToolItem(), 200.0F, true);`

**Arrow/projectile wiring — important, and different from what you might expect**: a bow class does
**not** hold a `Projectile` class reference itself. At attack time it calls
`BowProjectileToolItem.getArrowItem(level, attackerMob, seed, item)`, which asks the attacking mob
for its currently-equipped `ArrowItem` (`AmmoUserMob.getFirstAvailableArrow("arrowammo")`, falling
back to vanilla `"stonearrow"` if none). It then calls `arrow.getProjectile(x, y, targetX, targetY,
velocity, range, damage, knockback, owner)` — **the arrow item itself is responsible for producing
the `Projectile`** (`ArrowItem.getProjectile(...)` returns `null` in the base class; every concrete
arrow overrides it). So:

- To make a new bow shoot, you generally don't need a new arrow at all — it will happily fire any
  existing registered `ArrowItem` the player has equipped as ammo (e.g. vanilla `stonearrow`,
  `ironarrow`, etc.), scaled by the bow's own `attackDamage`/`attackRange`/`velocity`/`knockback`.
- `ArrowItem` base class (`inventory/item/arrowItem/ArrowItem.java`) fields: `damage`, `armorPen`,
  `critChance`, `speedMod` — set these in a custom arrow's constructor if you do add one, and
  override `getProjectile(...)` to return the actual `Projectile` instance (registered separately
  via `ProjectileRegistry`, not covered further here — out of this task's scope).
- `BowProjectileToolItem` itself has no texture override either (same convention as swords).

### 1.5 `Item.Rarity` and `Item.Type` (exact enum lists)

From `inventory/item/Item.java` (lines ~1447–1497):

**`Item.Rarity`**: `NORMAL, COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, QUEST, UNIQUE` — each carries a
`GameColor` (name-tooltip color) and an outline-hue range; purely cosmetic, no gameplay effect
observed in this class.

**`Item.Type`**: `MAT, TOOL, ARMOR, TRINKET, MOUNT, ARROW, BULLET, SEED, BAIT, FOOD, QUEST, MISC`.
This is **derived automatically** from the item's Java class (`Item.findType`, matches
`instanceof` against each enum constant's associated base class — e.g. anything extending `ToolItem`
is `TOOL`) — you never set it yourself.

### 1.6 Item texture convention (applies to materials, swords, bows, everything)

`Item.loadTextures()` (base class, `inventory/item/Item.java` lines 562–586) is `public` and is
called once per registered item from `GameResources.loadTextures()` (client-only boot step, see
§12). Unless a subclass overrides it, the convention is:

- Icon: `GameTexture.fromFile("items/" + this.getStringID())` — **mandatory**, throws if missing.
- Held-in-hand sprite: `GameTexture.fromFileRaw("player/holditems/" + this.getStringID())` —
  optional, silently `null` on `FileNotFoundException`.
- Mid-attack sprite: `GameTexture.fromFileRaw("player/weapons/" + this.getStringID())` — optional,
  same fallback.

So: **the resource file name is always exactly the item's registered stringID**, under
`resources/items/`, `resources/player/holditems/`, `resources/player/weapons/` respectively. No
registration-time texture wiring is needed at all as long as you follow this naming.

---

## 2. Recipes

### 2.1 `RecipeTechRegistry` — all tech constants

File: `engine/registries/RecipeTechRegistry.java` (121 lines, read in full). Every constant is a
public static `Tech` field, populated in `registerCore()`:

`ALL`, `NONE`, `WORKSTATION`, `COOKING_POT`, `ROASTING_STATION`, `FORGE`, `CARPENTER`,
`LANDSCAPING`, `IRON_ANVIL`, `ALCHEMY`, `DEMONIC_WORKSTATION` (alias `DEMONIC`, **@Deprecated**),
`COOKING_STATION`, `DEMONIC_ANVIL`, `VOID_ALCHEMY`, `TUNGSTEN_WORKSTATION` (alias
`ADVANCED_WORKSTATION`, **@Deprecated**), `TUNGSTEN_ANVIL`, `CAVEGLOW_ALCHEMY`,
`TUNGSTEN_CARPENTER`, `TUNGSTEN_LANDSCAPING`, `FALLEN_WORKSTATION`, `FALLEN_ANVIL`,
`FALLEN_ALCHEMY`, `FALLEN_CARPENTER`, `FALLEN_LANDSCAPING`, `TRANSMUTATION_STATION`,
`COMPOST_BIN`, `GRAIN_MILL`, `CHEESE_PRESS`.

`ALL` is special — `Recipe.matchTech(tech)` treats it as a wildcard that matches any station.
`NONE` means "craftable from the inventory with no station."

Mods can register their **own** custom tech (e.g. for a custom crafting station) via the public
static factory:

```
Tech registerTech(String stringID, String itemStringID, GameMessage displayName, GameMessage craftingMatTip)
Tech registerTech(String stringID, String itemStringID, GameMessage displayName)   // craftingMatTip defaults to a generic "used as crafting material" message
Tech registerTech(String stringID, String itemStringID)                           // displayName defaults to LocalMessage("tech", stringID)
```
`itemStringID` is the object/item stringID whose presence in the world unlocks the tech (i.e. the
crafting station object). Unlike Item/Object/Tile/Mob/Biome/Level registration, `registerTech` has
**no** `LoadedMod.isRunningModClientSide()` guard in the source — call it from `init()` regardless.

### 2.2 `Recipe` class — constructor signatures

File: `inventory/recipe/Recipe.java`. Four public constructors, each delegating to the next with a
default:

```
Recipe(String resultStringID, Tech tech, Ingredient[] ingredients)                                             // resultAmount = 1
Recipe(String resultStringID, int resultAmount, Tech tech, Ingredient[] ingredients)                            // isHidden = false
Recipe(String resultStringID, int resultAmount, Tech tech, Ingredient[] ingredients, boolean isHidden)          // gndData = null
Recipe(String resultStringID, int resultAmount, Tech tech, Ingredient[] ingredients, boolean isHidden, GNDItemMap gndData)
```

- `resultStringID` — must already be a registered item (constructor does
  `ItemRegistry.getItem(resultStringID)` and throws `NullPointerException` if not found — **this is
  why recipe registration must happen after the referenced items exist**, see §2.4).
- `resultAmount` — output stack size, clamped to ≥1.
- `tech` — the `Tech` this recipe requires (a station, or `RecipeTechRegistry.NONE`/`ALL`).
- `ingredients` — an `Ingredient[]` (see `Ingredient` class; also constructible via
  `ingredientsFromScript`, below). Duplicate ingredient stringIDs in the same recipe throw
  `IllegalArgumentException`.
- `isHidden` — if true, the recipe is only shown once craftable (no "grayed out, missing
  ingredients" preview).
- `gndData` — a `GNDItemMap` copied onto the crafted result's network data (used for e.g. presetting
  custom item state on craft); rarely needed for a simple recipe.

Fluent modifiers after construction: `.showBefore(itemStringID)` / `.showAfter(itemStringID)`
(sort position in the crafting list), `.setCraftingCategory(String... categoryTree)`,
`.onCrafted(Consumer<RecipeCraftedEvent> listener)` (side-effect hook, e.g. copying GND data from a
consumed ingredient onto the result).

### 2.3 `ingredientsFromScript` syntax

`Recipes.ingredientsFromScript(String script)` (`inventory/recipe/Recipes.java`) parses a
`LoadData`-script array-of-arrays. Each inner array is one ingredient:
`{itemStringID}` (amount defaults to 1), `{itemStringID, amount}`, or
`{itemStringID, amount, requiredToShow}` (boolean — if true, this ingredient must be present for the
recipe to even show up in the "hidden" list, per `IngredientData`/`Ingredient` semantics).

Example straight from vanilla (`Recipes.java:134`):
```
ingredientsFromScript("{{upgradeshard, 10}, {alchemyshard, 10}}")
```
i.e. plain comma-and-brace text, no quotes needed around the item stringID inside the script.

`Ingredient` itself can also be constructed directly: `new Ingredient(stringID, amount)` or
`new Ingredient(stringID, amount, requiredToShow)`. If `stringID` isn't a registered item, the
constructor treats it as a **global ingredient** stringID instead (`GlobalIngredientRegistry`) —
useful for "any wood plank"-style recipes.

### 2.4 How mods add recipes — `Recipes.registerModRecipe` and its timing

`Recipes` (`inventory/recipe/Recipes.java`) is a plain static utility, **not** a `GameRegistry`
subclass, with its own independent open/close flag:

```
static void registerModRecipe(Recipe recipe)      // throws IllegalStateException if closed
static void closeModRecipeRegistry()               // sets canRegisterModRecipes = false
```

Boot order proof, from `GlobalData.loadAll` (`engine/GlobalData.java`):

1. `registry.registerCore()` for the big registry list (includes `ItemRegistry`).
2. all mods' `init()` run.
3. `GameMessage.registry.closeRegistry()` then **every** registry in the big list closes — this
   closes `ItemRegistry`, `ObjectRegistry`, `TileRegistry`, `MobRegistry`, `BiomeRegistry`,
   `RecipeTechRegistry`, etc. **right after `init()`**, before `initResources()`/`postInit()` even run.
4. (client only) resource/texture loading, then all mods' `initResources()` run.
5. all mods' `postInit()` run.
6. **only then**: `Recipes.loadDefaultRecipes(); Recipes.closeModRecipeRegistry();` followed by
   `WorldGenerator.closeRegistry(); CommandsManager.closeRegistry();` and finally
   `ItemRegistry.calculateBrokerValues()`.

**Conclusion: `Recipes.registerModRecipe(...)` must be called by `postInit()` at the very latest**
(it technically stays open through `init()`/`initResources()` too, since `closeModRecipeRegistry()`
isn't called until after all mods' `postInit()`), but **`postInit()` is the only point where it's
guaranteed every mod's items/techs already exist** (since `ItemRegistry`/`RecipeTechRegistry` close
right after the `init()` loop, i.e. after *all* mods, not just yours, have registered their items).
Registering a recipe in your own `init()` that references another mod's item is a load-order hazard;
registering in `postInit()` is not. **Recommendation: always register recipes in `postInit()`.**

---

## 3. LootTable API

Package: `inventory/lootTable/` and `inventory/lootTable/lootItem/`. Everything implements
`LootItemInterface` (`addItems(...)`, `addPossibleLoot(...)`), which is what lets tables nest freely.

| Class | Constructor(s) | Behavior |
|---|---|---|
| `LootTable` | `LootTable()`, `LootTable(LootItemInterface... items)` | Container; **always** rolls every contained entry. Public field `items` (`List<LootItemInterface>`) can be appended to later (`table.items.add(...)`). |
| `LootItemList` | `LootItemList(LootItemInterface... items)` | An `ArrayList<LootItemInterface>` that is *itself* a `LootItemInterface` — functionally like `LootTable` (rolls everything) but also usable as a plain list; supports `.setCustomListName(...)` for grouped chest-preview text. |
| `LootItem` | `LootItem(String itemStringID)` (amount 1) · `LootItem(String itemStringID, int amount)` · `LootItem(String itemStringID, int amount, GNDItemMap gndData)` · `LootItem(String itemStringID, Function<GameRandom,Integer> amountSupplier[, GNDItemMap])` | One guaranteed drop. Static helpers `LootItem.between(itemStringID, minAmount, maxAmount[, gndData])` (random range) and `LootItem.offset(itemStringID, middle, offset[, gndData])` (random middle±offset). **Note: there is no `LootItem.of(...)` — only `.between`/`.offset` plus the plain constructors** (the task brief's guess of `.of` doesn't exist in this version). Modifiers: `.preventLootMultiplier()` (ignore server loot-rate buffs — used for guaranteed non-scaling drops like flowerpots), `.splitItems(maxSplitStacks)` / `.splitItems(minItemsPerStack, maxSplitStacks)` (break the amount into several randomly-sized stacks — this is what rocks/ore use for their stone/ore piles). |
| `ChanceLootItem extends LootItem` | Same shapes as `LootItem` plus a leading `float chance` (0–1) | Only drops with probability `chance` per roll (`LootTable.runChance`, which is multiplier-aware: a loot multiplier > 1 can trigger multiple independent rolls). Static helpers `ChanceLootItem.between(chance, itemStringID, minAmount, maxAmount)` / `.offset(...)`. |
| `ChanceLootItemList extends LootItemList` | `ChanceLootItemList(float chance)`, `ChanceLootItemList(float chance, LootItemInterface... items)` | The **whole group** rolls together with probability `chance` — if it hits, every item inside is granted. |
| `OneOfLootItems extends LootItemList` | `OneOfLootItems(LootItemInterface... items)` | Picks exactly **one** random entry (`random.getOneOf(this)`) and rolls only that one. This is the class backing shared weapon-drop pools like `CloseRangeWeaponsLootTable.closeRangeWeapons` / `BowWeaponsLootTable.bowWeapons` that `ToolItem.addToLootTable(...)` plugs into (§1.3). |
| `ConditionLootItem` / `ConditionLootItemList` | Take a `(GameRandom, Object[]) -> boolean` predicate instead of a flat chance | Gate a drop on arbitrary runtime state (e.g. "player hasn't already picked up this item," seen in `HostileMob.randomPrivatePortalDrop`). |

Real, compact vanilla examples (each is a full static field initializer, one line):
```
AshGolemMob.lootTable       = new LootTable(new LootItem("livingash"));
StabbyBushMob.lootTable     = new LootTable(new LootItem("stabbybush"), new LootItem("blueberry", 2), new LootItem("blueberrysapling"));
SpiderkinArcherMob.lootTable= new LootTable(new ChanceLootItemList(0.75F, new LootItem("spideritearrow", 10)));
ChieftainMob.lootTable      = new LootTable(new ChanceLootItem(0.2F, "theruneboundtrialpart1"), new ChanceLootItem(0.2F, "theruneboundtrialpart2"));
```
And the rock/ore idiom (`RockObject.getLootTable` / `RockOreObject.getLootTable`, §4.2):
```
new LootTable(LootItem.between("stone", 3, 5).splitItems(5))
```

A `LootTable` is consumed either by calling `.getNewList(random, lootMultiplier, extra...)` (returns
`ArrayList<InventoryItem>`) or `.addItemsToInventory(random, lootMultiplier, inventory, purpose,
extra...)` for chest-style containers, or `.applyToLevel(random, lootMultiplier, level, tileX, tileY,
extra...)` to push straight into a placed container's inventory.

---

## 4. ObjectRegistry — mineable rocks/ore, plants/decor, crystal

File: `engine/registries/ObjectRegistry.java` (2223 lines; overloads at lines 2082–2111).

### 4.1 `registerObject` overloads

Identical shape to `ItemRegistry.registerItem` (§1.1), just with `GameObject object` instead of
`Item item` — same `itemBrokerValue`/`itemObtainable`/`itemCountInStats`/`itemObtainableInCreative`/
`isObtainedByOtherItemStringIDs` parameters, same 4-arg/6-arg/varargs-vs-List overload shapes, same
clientside-mod guard. See §1.1's table for parameter meanings — they carry over verbatim because
(§4.6) registering a `GameObject` **auto-registers a companion `Item`** using these exact fields.

```
registerObject(String stringID, GameObject object, float itemBrokerValue, boolean itemObtainable)
registerObject(String stringID, GameObject object, float itemBrokerValue, boolean itemObtainable,
               boolean itemCountInStats, String... isObtainedByOtherItemStringIDs)
registerObject(String stringID, GameObject object, float itemBrokerValue, boolean itemObtainable,
               boolean itemCountInStats, boolean itemObtainableInCreative, String... isObtainedByOtherItemStringIDs)
```
(plus `List<String>` twins, mirroring §1.1 exactly). There's also a mirrored `replaceObject(...)`
family.

`GameObject`'s own base constructors (`level/gameObject/GameObject.java`): `GameObject()` (no
collision box — used for pure decor like grass/flowers) and `GameObject(Rectangle collision)` (sets
a pixel-space hitbox relative to the tile's top-left corner).

### 4.2 Mineable rock with ore variants — `RockObject` / `RockOreObject`

Files: `level/gameObject/RockObject.java`, `level/gameObject/RockOreObject.java` (both read in full).

**`RockObject`** (a plain rock — no ore):
```
RockObject(String rockTexture, Color rockColor, String droppedStone, int minStoneAmount, int maxStoneAmount, int placedStoneAmount, String... category)
RockObject(String rockTexture, Color rockColor, String droppedStone, String... category)   // = the above with (3, 5, 4)
```
- `rockTexture` — base name under `resources/objects/<rockTexture>.png` (a strip of rock-wall tile
  variants) **and** `resources/items/<rockTexture>.png` (used by `generateItemTexture()` for the
  pickup icon) — note this is a separate string from the registry `stringID`; several vanilla ore
  variants of the same base rock all reuse the parent's texture name.
- `rockColor` — `mapColor` field only (minimap dot color); has no effect on the actual sprite, which
  always comes from the texture file.
- `droppedStone` — stringID of the item this rock drops when mined naturally (world-generated, not
  player-placed); `null`/`maxStoneAmount<=0` means no drop.
- `minStoneAmount`/`maxStoneAmount` — random drop range, fed through `LootItem.between(...).splitItems(5)`.
- `placedStoneAmount` — **UNCERTAIN exact consumption site**: stored on the object but not read
  anywhere inside `RockObject.java`/`RockOreObject.java` itself. Very likely consumed by the
  auto-generated placeable `Item`/`ObjectItem` machinery to control give-back amount when a
  player-placed rock is broken, but this wasn't traced further — check `ObjectItem`/whatever
  `RockObject.generateNewObjectItem()` resolves to (it doesn't override it, so check the inherited
  `GameObject.generateNewObjectItem()` → `new ObjectItem(this)`) if this matters for your rock.
- `category` — item/crafting category tree; defaults to `{"objects","landscaping","rocksandores"}`.

Sets `isRock = true`, `regionType = RegionType.WALL`, `stackSize = 500` (the item stacks to 500).

**Mining-tool gating** — `toolTier` (float, default `0.0F`) and `toolType`
(`inventory/item/toolItem/ToolType`, default `PICKAXE`) are plain **public mutable fields on the
`GameObject` base class**, not constructor params. Vanilla sets a higher tier by field assignment
after construction, e.g. (`ObjectRegistry.java:1393`): `sandstoneRock.toolTier = 4.0F;`.
`ToolType` enum (`inventory/item/toolItem/ToolType.java`, full list): `UNBREAKABLE, NONE, ALL, AXE,
PICKAXE, SHOVEL`.

**`RockOreObject extends RockObject`** (an ore vein embedded in a specific parent rock):
```
RockOreObject(RockObject parentRock, String oreMaskTextureName, String oreTextureName, Color oreColor,
              String droppedOre, int droppedOreMin, int droppedOreMax, int placedDroppedOre,
              boolean isIncursionExtractionObject, String... category)
RockOreObject(RockObject parentRock, ..., int placedDroppedOre, String... category)     // isIncursionExtractionObject defaults true
RockOreObject(RockObject parentRock, String oreMaskTextureName, String oreTextureName, Color oreColor,
              String droppedOre, String... category)                                    // droppedOreMin/Max/placed default to 1/3/2
```
- `parentRock` — the base rock this ore is a variant of; `RockOreObject` **draws the parent's rock
  texture as the base layer**, then overlays a tinted ore-mask sprite on top. It also **inherits
  `toolTier` from the parent** (`this.toolTier = parentRock.toolTier`) — you gate mining difficulty
  on the parent rock, not the ore variant.
- `oreMaskTextureName` / `oreTextureName` — `oreTextureName` names a texture under
  `resources/objects/` used purely as a **color source**; `oreMaskTextureName` names a black/white
  alpha-mask texture that gets tinted by that color and merged onto the rock. If the named mask file
  doesn't exist, it falls back to the shared `resources/objects/oremask.png` — **meaning you can
  reuse the shared vanilla mask and just supply a distinct color + item-icon color source, with zero
  new mask art**. Same fallback exists for the item icon (`resources/items/oremask.png`).
- `oreColor` — tints the mask AND is the map/minimap color.
- `droppedOre`/`droppedOreMin`/`droppedOreMax` — extra drop **added on top of** the parent rock's own
  stone drop (`getLootTable` calls `parentRock.getLootTable(...)` then appends the ore item).
- `isIncursionExtractionObject` — adds a pulsing glow animation used by Incursion "extraction
  target" ores; pass `false` (or use the convenience overload, which defaults `true` — **so pass the
  full 10-arg constructor with `false` explicitly if you don't want the Incursion glow**).

Real vanilla registration pair (`ObjectRegistry.java:1364,1369`):
```
rockID = registerObject("rock", rock = new RockObject("rock", new Color(72, 71, 77), "stone", forestRocksCategory), -1.0F, true);
registerObject("ironorerock", new RockOreObject(rock, "oremask", "ironore", new Color(169, 128, 106), "ironore", forestRocksCategory), -1.0F, true);
```
(Note the `-1.0F` broker value — auto-computed from the item's cheapest recipe cost, per §1.1.)

### 4.3 Plants / decor objects — `GrassObject` and `FlowerObject`

**`GrassObject`** (`level/gameObject/GrassObject.java`, full file read) — the natural pick for
ground-cover decor, and named directly in the task brief:
```
GrassObject(String textureName, int undergroundPixels, int density)
GrassObject(String textureName, int density)     // undergroundPixels = 0
```
- `textureName` — sprite strip under `resources/objects/<textureName>.png`; each 32px-wide frame is
  one visual variant, randomly picked per-tile from a seeded hash of tile coordinates.
- `undergroundPixels` — height (from the sprite's bottom) treated as "underwater base" and drawn
  separately/dimmer when the tile beneath is liquid (0 = feature unused).
- `density` — max number of adjacent grass-type objects allowed before natural placement refuses
  (`densityCheck`); world-gen scattering knob, not consulted for player placement.
- Comes with built-in wind-sway animation (`weaveTime`, `weaveAmount`, random X/Y jitter) "for free."
- Sets `objectHealth = 1`, `toolType = ToolType.ALL` (harvestable with any tool), `isGrass = true`,
  `isLightTransparent = true`, category `{"objects","landscaping","plants"}`.

**`FlowerObject`** (`level/gameObject/furniture/FlowerObject.java`) — simpler, single-sprite,
flowerpot-hosted decor:
```
FlowerObject(String textureName, int spriteX, int stackSize, int itemSpoilDurationMinutes, String wildObjectStringID, Color mapColor)
FlowerObject(String textureName, int spriteX, int stackSize, int itemSpoilDurationMinutes, String customDrop, String wildObjectStringID, Color mapColor)
```
- `spriteX` — which 32px column of the shared texture strip this specific flower variant uses (many
  flower colors share one sprite sheet).
- `wildObjectStringID` — an optional companion "wild" object (e.g. found growing outdoors) this
  flowerpot item can also represent; drives `canPlace`/`placeObject` delegation.
- `itemSpoilDurationMinutes` — if >0, the auto-generated item is perishable.
- Only plantable into a tile whose object `isFlowerpot` is true (`canPlace` check) — this makes it
  specifically a flowerpot-furniture decor item, not a freestanding world object. For a **freestanding,
  non-flowerpot** single-sprite decorative object, extend `GameObject` directly the way
  `CrystalClusterObject` does (§4.4) rather than `FlowerObject`.

### 4.4 Crystal-like object — `CrystalClusterObject`

File: `level/gameObject/CrystalClusterObject.java` (full file read). This is exactly the
"searchable: crystal" example: a glowing, mineable, 2-tile-wide decorative object.

```
static void registerCrystalCluster(String textureName, Color mapColor, float glowHue, String dropItem,
                                    int minDropAmount, int maxDropAmount, int placedDropAmount,
                                    float brokerValue, boolean isObtainable, String... category)
static void registerCrystalCluster(String textureName, Color mapColor, float glowHue, String dropItem,
                                    float brokerValue, boolean isObtainable, String... category)   // amounts default 2/3/2
```
This static helper does the **entire registration for you**, including the required companion tile
(see below) — call it directly instead of calling `ObjectRegistry.registerObject` yourself. Under
the hood it does two `registerObject` calls:
1. `textureName` → the visible/minable "front" `CrystalClusterObject` (uses your `brokerValue`/`isObtainable`).
2. `textureName + "r"` → a `CrystalClusterRObject` **companion tile that fills the other half of the
   2×1 multi-tile footprint** (`MultiTile`/`StaticMultiTile(0, 0, 2, 1, true, frontID, counterID)`),
   always registered non-obtainable (`0.0F, false`) since it's not a separate pickup.

Constructor fields worth knowing if you subclass directly instead of using the helper:
`glowHue` sets `lightHue`/`lightSat=1.0F`/`lightLevel=150` — the object emits its own colored light,
which is what makes "crystal" read visually distinct from a rock. `minDropAmount`/`maxDropAmount`
work exactly like `RockObject`'s stone range.

### 4.5 Summary — toolType/toolTier applies to ALL objects, not just rocks

Since `toolType`/`toolTier` live on the `GameObject` base class (§4.2), the same mining-gate pattern
applies to grass, flowers, crystals, everything: set `this.toolType = ToolType.X;` /
`this.toolTier = Y;` in the constructor (or via field assignment after construction, vanilla-style)
to control which tool tier can damage/harvest the object. `GrassObject` defaults to
`ToolType.ALL` (harvestable by anything); `RockObject` inherits the base default `ToolType.PICKAXE`.

### 4.6 Auto-item generation (why `registerObject` takes item-shaped params)

`ObjectRegistry.onRegister(...)` (lines 2041–2063, read in full) calls
`object.object.generateNewObjectItem()`, and if non-null, immediately calls
`ItemRegistry.replaceItem(stringID, item, itemBrokerValue, itemObtainable, itemCountInStats,
itemObtainableInCreative, isObtainedByOtherItemStringIDs)` — **every registered `GameObject`
automatically gets a matching placeable `Item` under the same stringID**, no separate `ItemRegistry`
call needed. The base `GameObject.generateNewObjectItem()` returns `new ObjectItem(this)`; the base
`GameObject.generateItemTexture()` defaults to `items/<stringID>.png` but `RockObject`/`RockOreObject`
override it to derive the icon from their own already-loaded body/ore textures instead of requiring
a separate hand-drawn icon file.

---

## 5. TileRegistry

File: `engine/registries/TileRegistry.java` (423 lines; overloads at lines 290–319).

### 5.1 `registerTile` overloads

Exactly the same shape as `registerObject`/`registerItem` — `GameTile tile` instance, same trailing
broker/obtainable/stats/creative/otherItems params, same clientside-mod guard, mirrored
`replaceTile(...)` family:
```
registerTile(String stringID, GameTile tile, float itemBrokerValue, boolean itemObtainable)
registerTile(String stringID, GameTile tile, float itemBrokerValue, boolean itemObtainable,
             boolean itemCountInStats, String... isObtainedByOtherItemStringIDs)
registerTile(String stringID, GameTile tile, float itemBrokerValue, boolean itemObtainable,
             boolean itemCountInStats, boolean itemObtainableInCreative, String... isObtainedByOtherItemStringIDs)
```

### 5.2 The `dirttile` example, decoded

Real vanilla line (`TileRegistry.java:158`):
```
dirtID = registerTile("dirttile", new DirtTile(), 0.0F, false, false, true);
```
This is the **6-positional-arg** overload (no varargs used), i.e. params 4/5/6 are exactly
`itemObtainable`, `itemCountInStats`, `itemObtainableInCreative`:

| Position | Param | Value | Meaning here |
|---|---|---|---|
| 3 | `itemBrokerValue` | `0.0F` | Worthless — plain dirt has no sell value. |
| 4 | `itemObtainable` | `false` | Digging up a dirt tile in normal survival does **not** hand you a placeable dirt-tile item. |
| 5 | `itemCountInStats` | `false` | Doesn't count toward any "items obtained" completion stat. |
| 6 | `itemObtainableInCreative` | `true` | Despite not being survival-obtainable, it **does** still appear in the Creative-mode item picker, since Creative bypasses normal item obtainability. |

### 5.3 Tile item auto-generation and the texture-convention gap

`TileRegistry.onRegister(...)` (lines 264–277, read in full) mirrors `ObjectRegistry` exactly: calls
`object.tile.generateNewTileItem()` and, if non-null, `ItemRegistry.replaceItem(...)` with the same
6 trailing params. **Unlike `Item`/`GameObject`, `GameTile.generateItemTexture()`'s default is not a
"file named after the stringID" convention** — the base implementation
(`level/gameTile/GameTile.java:217`) generates a placeholder icon by multiplying a shared
`tiles/itemmask.png` over `GameResources.error` (the pink/black missing-texture placeholder). In
practice every real tile subclass overrides `generateItemTexture()` (or `loadTextures()`) itself to
build a proper icon from its own terrain texture — don't rely on the base default for a new tile's
item icon.

`GameTile`'s own constructor: `GameTile(boolean isFloor)` — only param controls default
`tileHealth` (50 for floors, 100 for walls-like tiles) and default item category
(`"tiles","floors"` vs `"tiles","terrain"`/`"tiles","liquids"` based on `instanceof` checks against
`LiquidTile`/`TerrainSplatterTile`).

---

## 6. MobRegistry — hostile mobs

File: `engine/registries/MobRegistry.java` (1871 lines).

### 6.1 `registerMob` overloads

Unlike Item/Object/Tile, this is **class-based (reflection)**, not instance-based — you pass
`Class<? extends Mob>`, not a `Mob` instance:

```
registerMob(String stringID, Class<? extends Mob> mobClass, boolean countKillStat)
registerMob(String stringID, Class<? extends Mob> mobClass, boolean countKillStat, boolean isBossMob, boolean createSpawnItem)
registerMob(String stringID, Class<? extends Mob> mobClass, boolean countKillStat, boolean isBossMob)
registerMob(String stringID, Class<? extends Mob> mobClass, boolean countKillStat, boolean isBossMob, GameMessage killHint)
registerMob(String stringID, Class<? extends Mob> mobClass, boolean countKillStat, boolean isBossMob, GameMessage displayName, GameMessage killHint)
registerMob(String stringID, Class<? extends Mob> mobClass, boolean countKillStat, boolean isBossMob, GameMessage displayName, GameMessage killHint, boolean createSpawnItem)
```
- `countKillStat` — counts kills toward the player's kill-stat tracker (used by journal/achievements).
- `isBossMob` — boss UI treatment (health bar etc.).
- `displayName` — defaults to `new LocalMessage("mob", stringID)` if omitted.
- `killHint` — optional objective-style hint message shown for a not-yet-killed mob.
- `createSpawnItem` — see §6.4.

Real vanilla examples (`MobRegistry.java:473,474,631`):
```
registerMob("jackal", JackalMob.class, true);
registerMob("giantscorpion", GiantScorpionMob.class, false);
registerMob("chieftain", ChieftainMob.class, true, true);      // countKillStat=true, isBossMob=true
```

### 6.2 Mob classes need a public no-arg constructor — proven, not assumed

`MobRegistry.MobRegistryElement` extends `ClassIDDataContainer<Mob>`
(`engine/registries/ClassIDDataContainer.java`) and its constructor calls
`super(mobClass)` — i.e. `ClassIDDataContainer(Class<? extends C> aClass, Class<?>...
constructorParameters)` with **zero** extra parameter-type varargs. That flows into
`ClassIDData(aClass, constructorParameters)` → `aClass.getConstructor(constructorParameters)` =
`aClass.getConstructor()` — **a no-arg lookup**. `registerMob` catches `NoSuchMethodException` and
logs `"Could not register mob <X>: Missing constructor with no parameters"` if the class lacks one.

Instantiation: `MobRegistry.getMob(id, level)` calls `instance.getElement(id).newInstance(new
Object[0])` (no-arg reflective construction) and then calls `out.onConstructed(level)` — **the
`Level` is attached after construction, not passed into the constructor**. So a mob class must look
like:
```
public MyHostileMob() {
   super(healthValue);   // whatever your HostileMob-derived base needs
   ...
}
```
exactly like the real `GiantScorpionMob() { super(125); ... }` (`entity/mobs/hostile/GiantScorpionMob.java`,
extends `HostileMob`, which itself just takes `HostileMob(int health)` and sets `isHostile = true`,
`team = -2`, `canDespawn = true`).

### 6.3 Mob textures — `MobRegistry.Textures` is vanilla-closed; mods hold their own

`MobRegistry.Textures` (`engine/registries/MobRegistry.java`, static nested class, ~800 hardcoded
`public static GameTexture`/`MobTexture`/`HumanTexture` fields) is loaded once by
`MobRegistry.Textures.load()`, called from `GameResources.loadTextures()`
(`gfx/GameResources.java:725`, client-only boot step). **This is a fixed, compiled vanilla class —
mods cannot add fields to it.** Real vanilla mobs reference it directly, e.g.
`GiantScorpionMob.addDrawables(...)` draws `MobRegistry.Textures.giantScorpion.body` /
`.shadow` (a `MobTexture(GameTexture body, GameTexture shadow)` pair,
`entity/mobs/MobTexture.java`).

**The correct mod pattern**: declare your **own** static texture field(s) on your mob class (or a
small dedicated resource-holder class in your mod), and load them in your mod's `initResources()`
(client-only lifecycle step — see §13) via plain `GameTexture.fromFile("yourpath")`, pointing at a
file under your mod's own `resources/` folder. Then reference that field from your mob's own
`addDrawables(...)` override exactly the way vanilla mobs reference `MobRegistry.Textures.x`. There
is no per-stringID texture-loading convention for mobs (unlike items) — it is 100% manual, by
design, because mob rendering needs multiple frames/directions that don't fit one fixed naming
scheme. (`MobRegistry.Textures` internally uses a private `fromFile(path)` helper that just
prepends `"mobs/"` — useful to know as the vanilla path convention if you want to mirror it, but it
is not something a mod calls directly.)

Separately, `MobRegistry.loadMobIcons()` (called right after `Textures.load()` in the same boot
step) loads a `GameTexture` **per registered mob** from `mobs/icons/<stringID>` — this one **is**
stringID-convention-based and is used for the bestiary/kill-list icon, distinct from the in-world
body sprite.

### 6.4 `createSpawnItem` — what it actually does

`MobRegistry.onRegister(...)` (lines 710–717, read in full): if `createSpawnItem` is true, it
automatically calls
`ItemRegistry.registerItem(stringID + "spawnitem", new MobSpawnItem(1, true, stringID), 50.0F, false, false, true)`
— i.e. it creates and registers a companion item stringID'd `"<mobstringid>spawnitem"` that spawns
the mob when used (creative/debug-obtainable only: `isObtainable=false, isObtainableInCreative=true`).
**UNCERTAIN**: `MobSpawnItem`'s exact 3-arg constructor meaning (`inventory/item/placeableItem/MobSpawnItem.java`)
wasn't traced — the `(1, true, stringID)` shape strongly suggests `(amount-or-uses, someFlag,
mobStringID)` but confirm by reading that file if you plan to use `createSpawnItem=true` yourself.
For a normal hostile mob (spawned only via world-gen `MobSpawnTable`, not via item), pass `false`.

### 6.5 Hooking a mob into world spawns (`MobSpawnTable`)

`level/maps/biomes/MobSpawnTable.java` — ticket-weighted spawn list, simplest form (used
everywhere in vanilla): `new MobSpawnTable().add(tickets, "mobstringid")`, e.g.
`.add(80, "zombie").add(20, "zombiearcher")` — a mob with more tickets is proportionally more likely.
This is what a custom `Biome.getMobSpawnTable(Level)` override returns (§7).

---

## 7. BiomeRegistry — custom biome

File: `engine/registries/BiomeRegistry.java` (205 lines, read in full).

### 7.1 `registerBiome` signature

Instance-based, generic, returns the biome you passed in for convenient static-field assignment:
```
static <T extends Biome> T registerBiome(String stringID, T biome, boolean countInStats)
```
`countInStats` — whether visiting this biome counts toward exploration-completion stats. Same
clientside-mod guard as other registries. Real example:
`FOREST = registerBiome("forest", new ForestBiome().setGenerationWeight(1.0F), true);` — note
`.setGenerationWeight(float)` on `Biome` controls how often world-gen picks this biome for a new
island (weight is turned into lottery "tickets" at registry-close time;
`0` weight = never naturally generated, used for all the Incursion-only/fixed biomes in vanilla).

### 7.2 `Biome` override points (exact method names, from `level/maps/biomes/Biome.java`, full file read)

For a **fixed-biome custom level** (the task's scenario), these are the methods to override:

| Method | Returns | Purpose |
|---|---|---|
| `getMobSpawnTable(Level level)` | `MobSpawnTable` | Hostile spawn table; base picks `defaultSurfaceMobs` vs `defaultDeepCaveMobs`/`forestCaveMobs` by `level.isCave`/identifier. |
| `getCritterSpawnTable(Level level)` | `MobSpawnTable` | Passive/critter spawn table (separate pool from hostiles). |
| `getLevelMusic(Level level, PlayerMob perspective)` | `AbstractMusicList` | Background music playlist; base branches on cave/night. |
| `getCrateLootTable(Level level, int tileX, int tileY)` | `LootTable` | Loot for crates generated in this biome; base returns `LootTablePresets.basicCrate`/`basicDeepCrate`. |
| `getFishingLootTable(FishingSpot spot)` | `FishingLootTable` | **Exact name is `getFishingLootTable`**, not "-ish" — takes a `FishingSpot`, not a `Level`. |
| `canRain(Level level)` | `boolean` | Base: `!level.isCave` (caves never get weather). |
| `getExtraMobDrops(Mob mob)` / `getExtraBiomeMobDrops(LevelIdentifier)` / `getExtraPrivateMobDrops(Mob, ServerClient)` | `LootTable` | Bonus biome-wide drop tables layered on top of a mob's own loot table; base returns empty `new LootTable()`. |
| `getNewSurfaceLevel(...)` / `getNewCaveLevel(...)` / `getNewDeepCaveLevel(...)` | `Level` | **The world-gen-time level factory** — see §8.4, this is the other half of the Level/Biome relationship. |
| `hasVillage()` | `boolean` | Whether this biome generates the village preset. |
| `getGenerationCaveRockObjectID()` / `getGenerationCaveTileID()` / `getGenerationTerrainTileID()` / etc. | `int` (object/tile ID) | The specific tile/object IDs used by the shared cave/surface generator modules for this biome — override to reskin generation without writing a whole new generator. |

---

## 8. LevelRegistry — CRITICAL: exact constructor contract for save/load

File: `engine/registries/LevelRegistry.java` (159 lines, read in full).

### 8.1 `registerLevel` and the mandated constructor

```
static int registerLevel(String stringID, Class<? extends Level> levelClass)
```
Class-based (reflection), same as `MobRegistry`. `LevelRegistry.LevelRegistryElement` is built as:
```
protected static class LevelRegistryElement extends ClassIDDataContainer<Level> {
   public LevelRegistryElement(Class<? extends Level> levelClass) throws NoSuchMethodException {
      super(levelClass, LevelIdentifier.class, int.class, int.class, WorldEntity.class);
   }
}
```
i.e. it does `levelClass.getConstructor(LevelIdentifier.class, int.class, int.class,
WorldEntity.class)`. If missing, `registerLevel` catches `NoSuchMethodException` and prints — quoted
verbatim as the clearest possible statement of the contract:

> `"Could not register level " + levelClass.getSimpleName() + ": Missing constructor with parameters: LevelIdentifier, int (width), int (height), WorldEntity"`

**Every custom `Level` subclass you register MUST expose exactly:**
```
public MyLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity)
```

### 8.2 Proof this is also the save/load path, not just registration

`LevelRegistry.getNewLevel(int id, LevelIdentifier identifier, int width, int height, WorldEntity
worldEntity)` reflectively invokes that exact constructor. Grepping every caller of `getNewLevel`
shows it is used identically for **fresh instantiation and for save/load reconstruction** — there is
no separate reflection path for loading:

- `engine/save/LevelSave.java:60` (world load): reads the level's `stringID` from the save file
  (`save.getUnsafeString("stringID", ...)`), looks up `LevelRegistry.getLevelID(levelStringID)`, then
  calls `LevelRegistry.getNewLevel(levelID, identifier, width, height, server.world.worldEntity)`. If
  the stringID can't be resolved (e.g. the mod that registered it is no longer installed), it falls
  back to a **plain `new Level(identifier, width, height, worldEntity)`** — so a missing custom level
  class degrades gracefully to the base class rather than crashing the load.
- `engine/network/client/ClientLevelManager.java:78` (client receiving a level from the server over
  the network): identical call shape.
- `gfx/forms/presets/PresetPreviewForm.java` / `PresetDebugPreviewForm.java` (level preview UI):
  identical call shape.

So: registering your `Level` subclass and giving it this one constructor is **sufficient** for both
normal use and save/load — you do not need to write any custom save/load code yourself for the level
*type* to survive a save/reload (individual custom level *data* you add still goes through the
normal `LevelDataRegistry`/`applyLoadData` mechanisms, not covered here).

### 8.3 The other half of the contract: the class itself must be registered, unconditionally

`Level`'s own base constructor (`level/maps/Level.java:209`, `Level(LevelIdentifier identifier, int
width, int height, WorldEntity worldEntity)`) — which **every** Level subclass constructor chains
into eventually — contains this call, unconditionally, for every single Level instance ever created
by any means:
```
LevelRegistry.instance.applyIDData((Class<? extends Level>) this.getClass(), this.idData);
```
`ClassedGameRegistry.applyIDData` (`engine/registries/ClassedGameRegistry.java`) looks up
`this.getClass()` in the registry's class→ID map and throws
`IllegalStateException("Cannot construct unregistered Level class " + clazz.getSimpleName())` if not
found. **This means: even if you only ever construct your `Level` subclass directly (e.g. from a
custom `Biome.getNewSurfaceLevel` override, §8.4) and never rely on stringID-based reconstruction,
you must still call `LevelRegistry.registerLevel("mylevel", MyLevel.class)` in your mod's `init()`,
or every single construction of that class — including the very first one during world-gen —
throws.**

### 8.4 The dual-constructor pattern (how a biome's level actually gets built)

Looking at how vanilla's own biome-tied levels are structured (`level/maps/biomes/BasicSurfaceLevel.java`,
`level/maps/biomes/BasicCaveLevel.java`, both read in full) reveals the real-world pattern for
"a fixed biome with a custom level," which combines §7.2 and §8.1:

Each level class carries **two** constructors:
1. The **mandated reflection constructor** `(LevelIdentifier, int, int, WorldEntity)` — does nothing
   but `super(...)`. This exists purely so `LevelRegistry`/save-load/network can reconstruct the
   class; it does **not** generate any terrain.
2. A **generation-time constructor** with whatever shape is convenient (vanilla uses
   `(int islandX, int islandY, float islandSize, WorldEntity worldEntity, Biome biome)` for surface
   levels, `(int islandX, int islandY, int dimension, WorldEntity worldEntity)` for caves) — this one
   builds a `LevelIdentifier` itself, calls `super(identifier, width, height, worldEntity)`, and then
   immediately calls a `this.generateLevel(...)` method that actually lays down terrain/structures.
   This constructor is called **directly** (`new BasicSurfaceLevel(...)`, not through the registry)
   from `Biome.getNewSurfaceLevel(...)`/`getNewCaveLevel(...)`/`getNewDeepCaveLevel(...)`.

So the recipe for "a biome with a fixed custom level" is:
1. Write `MyLevel` with both constructors as above (the reflection one can be a one-line
   pass-through; put your actual generation code behind the second one).
2. `LevelRegistry.registerLevel("mylevel", MyLevel.class)` in `init()` (mandatory per §8.3, even
   though normal play only ever calls constructor #2).
3. In your custom `Biome` subclass, override `getNewSurfaceLevel`/`getNewCaveLevel`/
   `getNewDeepCaveLevel` (whichever dimensions you support) to `return new MyLevel(...)` using
   constructor #2.
4. `BiomeRegistry.registerBiome("mybiome", new MyBiome(), ...)` in `init()`.

---

## 9. Commands

File: `engine/commands/CommandsManager.java` (full file read).

### 9.1 Registration API

Plain static methods on `CommandsManager`, backed by two static `List<ChatCommand>` (not a
`GameRegistry` subclass, no reflection involved):
```
static <T extends ChatCommand> T registerServerCommand(T command)
static <T extends ChatCommand> T registerClientCommand(T command)
static void closeRegistry()
```
`registerServerCommand` is for commands runnable by a connected client against a server (or from the
server console); `registerClientCommand` is for purely local/client-side commands (settings toggles
etc. — see `HelpClientCommand`, `ZoomClientCommand` for vanilla examples). Both throw
`IllegalStateException("Command registration is closed")` after `closeRegistry()`.

**Timing**: `CommandsManager.registerCoreCommands()` runs once, right after the core-registry
`registerCore()` loop and **before** any mod's `init()` (so all vanilla commands already exist by
the time your mod runs). `CommandsManager.closeRegistry()` runs at the very end of
`GlobalData.loadAll`, **after every mod's `postInit()`** (same line as `Recipes.closeModRecipeRegistry()`,
§2.4) — so, like recipes, command registration is safe anywhere through `postInit()`. There is
**no dedicated `@ModCommand`-style annotation** (unlike `@ModEntry` for the lifecycle hooks, §13) —
you call `CommandsManager.registerServerCommand(new MyCommand())` directly from your `ModEntry`
class's `init()` (or later).

### 9.2 Writing a command — `ChatCommand` / `ModularChatCommand`, and a real example

`ChatCommand` (`engine/commands/ChatCommand.java`) is the abstract base:
`ChatCommand(String name, PermissionLevel permissionLevel)`, with abstract `run(...)`,
`getUsage()`, `getAction()`, `autocomplete(...)`, `getCurrentUsage(...)` — verbose to implement by
hand. In practice, vanilla nearly always extends the convenience subclass `ModularChatCommand`
(`engine/commands/ModularChatCommand.java`):
```
ModularChatCommand(String name, String action, PermissionLevel permissionLevel, boolean isCheat, CmdParameter... parameters)
```
which handles all the argument-parsing/autocomplete/usage boilerplate for you via a declarative
`CmdParameter[]` and leaves you to implement just:
```
abstract void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs)
```

Real, minimal vanilla example, in full (`engine/commands/serverCommands/DieServerCommand.java`, 24
lines):
```
public class DieServerCommand extends ModularChatCommand {
   public DieServerCommand() {
      super("die", "Kills yourself", PermissionLevel.USER, false);
   }
   public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
      if (serverClient == null) {
         logs.add("Command cannot be run from server.");
      } else {
         serverClient.playerMob.setHealth(0);
      }
   }
}
```

`PermissionLevel` enum (`engine/commands/PermissionLevel.java`, full list, ordinal-ordered from
lowest to highest): `USER, CREATIVESETTINGS, MODERATOR, ADMIN, OWNER, SERVER` (`SERVER` is
`reserved` — console-only, never assignable to a player).

---

## 10. GameEvents / GameEventListener (world-gen hooks)

Files: `engine/GameEvents.java`, `engine/GameEventInterface.java`, `engine/GameEventListener.java`,
`engine/GameEventsHandler.java` (all short, all read in full).

Static pub/sub keyed by event class:
```
static <T extends GameEvent, R extends GameEventInterface<T>> R addListener(Class<T> eventClass, R listener)
static <T extends GameEvent> void triggerEvent(T event)
static <T extends PreventableGameEvent> void triggerEvent(T event, Consumer<T> runOnNotPrevented)
```
`GameEventInterface<T>` itself has 3 methods (`init(Runnable)`, `onEvent(T)`, `isDisposed()`), so
don't implement it raw — subclass the provided convenience base instead:
```
public abstract class GameEventListener<T> implements GameEventInterface<T> {
   public abstract void onEvent(T event);   // (only method you actually need to implement)
}
```
Usage pattern: `GameEvents.addListener(GeneratedCaveOresEvent.class, new GameEventListener<GeneratedCaveOresEvent>() { public void onEvent(GeneratedCaveOresEvent e) { ... } });`,
called from your mod's `init()`. No registry-close gate exists for listeners — they can be added at
any time, though registering early (`init()`) is idiomatic since world-gen can start soon after boot.

Relevant to level generation specifically, `BasicCaveLevel.generateLevel()`
(`level/maps/biomes/BasicCaveLevel.java`) fires, in this order:
`GenerateCaveLayoutEvent` → `GeneratedCaveLayoutEvent` → `GenerateCaveMiniBiomesEvent` →
`GeneratedCaveMiniBiomesEvent` → `GenerateCaveOresEvent` → `GeneratedCaveOresEvent` →
`GenerateCaveStructuresEvent` → `GeneratedCaveStructuresEvent` (all in
`engine/events/worldGeneration/`). Each event class is a plain data-holder, e.g.
`GeneratedCaveOresEvent(Level level, CaveGeneration caveGeneration)` — a mod could listen on
`GeneratedCaveOresEvent` to scatter extra custom ore rocks after vanilla cave-ore placement finishes,
using the passed-in `CaveGeneration` helper (`cg.generateRandomSingleRocks(objectID, chance)` is the
method vanilla itself uses for this, per `BasicCaveLevel.generateLevel`).

---

## 11. Buffs

`BuffRegistry.registerBuff` signature only (`engine/registries/BuffRegistry.java:968`):
```
static <T extends Buff> T registerBuff(String stringID, T buff)
```
(Not explored further per task scope — sufficient to register a custom debuff instance if needed.)

---

## 12. Boot order (verified from `engine/GlobalData.java`, `loadAll`, lines ~272–480)

This is the ground truth for §13 below. In order:

1. `ModLoader.loadMods(isServer)`, `Localization.loadModsLanguage()`, `Settings.loadModSettings(false)`.
2. **`mod.preInit()`** for every enabled mod.
3. `GameSeasons.loadSeasons()`.
4. **`registry.registerCore()`** for every core registry (a fixed array of ~50 registries including
   `TileRegistry`, `ObjectRegistry`, `BiomeRegistry`, `RecipeTechRegistry`, `ItemRegistry`,
   `MobRegistry`, `LevelRegistry`, etc.) — this is where **all vanilla content** gets registered.
5. `CommandsManager.registerCoreCommands()`.
6. **`mod.init()`** for every enabled mod.
7. `GameMessage.registry.closeRegistry()`, then **every core registry from step 4 closes**
   (`registry.closeRegistry()`) — `ItemRegistry`/`ObjectRegistry`/`TileRegistry`/`MobRegistry`/
   `BiomeRegistry`/`LevelRegistry`/`RecipeTechRegistry` etc. are now permanently closed.
8. `GameEyes.loadEyeTypes()`, `GameHair.loadHairTypes()`.
9. *(client only)* mod preview images, `ResourceEncoder.addModResources(mod)`, player stats/
   achievements load, `GameSeasons.loadResources()`, shaders, cursors,
   **`GameResources.loadTextures()`** (loads every registered tile/object/item/buff/biome/mob
   texture by convention or via `MobRegistry.Textures.load()`/`loadMobIcons()`), client settings.
10. *(client only)* **`mod.initResources()`** for every enabled mod.
11. *(client only)* `GameResources.finishLoadingSounds()`, `Biome.generateBiomeTextures()`,
    `GameTile.generateTileTextures()`, `GameLogicGate.generateLogicGateTextures()`,
    `WallObject.generateWallTextures()`, `GameTexture.finalizeLoadedTextures()`.
12. `Settings.loadBanned()`.
13. **`mod.postInit()`** for every enabled mod.
14. `Recipes.loadDefaultRecipes()`, `Recipes.closeModRecipeRegistry()`, `WorldGenerator.closeRegistry()`,
    `CommandsManager.closeRegistry()`, `ItemRegistry.calculateBrokerValues()`.

---

## 13. Mod init skeleton

Based directly on §12's verified order. `ModEntry`'s lifecycle methods
(`engine/modLoader/classes/EntryClass.java`) are, in call order: `preInit → init → initResources →
postInit → dispose`. `initResources()` **only runs client-side** (server-only processes skip it
entirely, per step 10 above being inside the `if (!isServer)` block) — never put registration calls
that must exist on the server (which is everything in §1–§9) inside `initResources()`.

**`preInit()`** — rarely needed for content mods. Runs before *any* registry (even core vanilla
`registerCore()`) has executed. Use only for things with zero registry dependency (e.g. reading your
own config file). Do not touch any registry here.

**`init()`** — put here, because the corresponding registries close right after every mod's `init()`
has run (§12 step 7):
- `TileRegistry.registerTile(...)` — new tiles.
- `ObjectRegistry.registerObject(...)` — rocks/ore (`RockObject`/`RockOreObject`), plants/decor
  (`GrassObject`/`FlowerObject`), crystal-likes (`CrystalClusterObject.registerCrystalCluster(...)`).
- `ItemRegistry.registerItem(...)` — materials (`MatItem`), the sword (`SwordToolItem` subclass), the
  bow (`BowProjectileToolItem` subclass) — note items registered via `ObjectRegistry`/`TileRegistry`
  are auto-created (§4.6/§5.3); only call `ItemRegistry.registerItem` directly for standalone items
  (materials, weapons) that aren't the placeable form of a tile/object.
- `MobRegistry.registerMob(...)` — the hostile mob class (must have a public no-arg constructor,
  §6.2).
- `BiomeRegistry.registerBiome(...)` — the custom biome.
- `LevelRegistry.registerLevel(...)` — the custom level class (must expose the
  `(LevelIdentifier, int, int, WorldEntity)` constructor, §8.1/§8.3 — required even if you also use
  a second generation-time constructor).
- `RecipeTechRegistry.registerTech(...)` — only if adding a *new* crafting station; otherwise just
  reference an existing `RecipeTechRegistry.X` constant later.
- `BuffRegistry.registerBuff(...)` — a custom debuff, if any.
- `GameEvents.addListener(...)` — world-gen event hooks (no hard ordering requirement, but `init()`
  is idiomatic and matches vanilla's own usage timing).
- `CommandsManager.registerServerCommand(...)` / `registerClientCommand(...)` — safe here too (stays
  open through `postInit()`, §9.1), but `init()` keeps it next to everything else.

**`initResources()`** (client-only) — put here:
- Loading your **own** mob body/shadow textures (`GameTexture.fromFile(...)`) into your own static
  field(s), since `MobRegistry.Textures` is closed/vanilla-only (§6.3). This is the *only* piece of
  content in this task's scope that needs explicit texture-loading code — everything else (item
  icons, object/tile sprites) is convention-loaded automatically from your mod's `resources/` folder
  the moment `GameResources.loadTextures()` runs, with no code required.
- Any other bespoke texture/sound loading your custom draw code needs that doesn't fit a
  by-stringID convention.

**`postInit()`** — put here, and *only* here, for correctness against other mods' load order:
- `Recipes.registerModRecipe(new Recipe(...))` for every recipe (materials → sword/bow, and any
  station-unlock recipes) — §2.4's timing argument: this is the first point where every mod's items
  and techs are guaranteed to exist, since `ItemRegistry`/`RecipeTechRegistry` close after `init()`
  but recipe registration itself doesn't close until after `postInit()`.

**`dispose()`** — not needed for a content-only mod; exists for cleanup of non-registry resources
held across a mod reload.
