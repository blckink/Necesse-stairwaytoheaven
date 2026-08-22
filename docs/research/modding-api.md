# Necesse Modding API — Research Notes

Compiled from the official Necesse Wiki modding portal and the official
example mod repository it links to. This document only contains
information actually observed in the fetched sources below. Anything not
found is explicitly marked **Not covered by wiki**, and anything derived
from a paraphrased/summarized fetch (rather than a directly quoted
verbatim snippet) is marked **UNVERIFIED**.

## Note on wiki structure (read this first)

The task brief assumed the wiki has per-topic subpages such as
`/Modding/Items`, `/Modding/Getting_Started`, `/Modding/Levels`, etc.
Those pages **do not exist** — they were checked directly and return
HTTP 404 (confirmed for `/Modding/Items`, `/Modding/Getting_Started`,
`/Modding/Levels`; `/Mod_Creation` also 404s). Necesse Wiki's modding
documentation is actually a single flat page,
**[necessewiki.com/Modding](https://necessewiki.com/Modding)**, plus a
community-maintained **[Modding_Snippets](https://necessewiki.com/Modding_Snippets)**
page. The `Modding` page itself links out to two things that carry most
of the concrete technical detail:

1. A **published Google Doc** ("Necesse Modding Guide") — the wiki page
   itself says this doc is *"in the process of getting updated and moved
   to the wiki"*, i.e. it is the legacy primary reference and may be
   partly stale relative to the current game version.
2. The **official example mod on GitHub** ([DrFair/ExampleMod](https://github.com/DrFair/ExampleMod))
   — a buildable Gradle project with real, compilable registration code
   and real texture assets. Because the wiki text itself is quite thin,
   this repo (linked directly from the wiki as *"the official example
   project"*) was used to find and verify exact class/method names and
   real texture dimensions. It targets game version **1.3.2** per its
   `build.gradle`.

Every fact below is tagged with which of these three sources it came
from. Where the Google Doc's fetch came back as prose summary rather
than an exact quote, that is flagged UNVERIFIED even though it is
presented in a code block.

---

## 1. Mod structure

### mod.info

Source: wiki `Modding` page (prose) + GitHub `ExampleMod/build.gradle`
(the actual generation logic — corroborates the wiki field list exactly).

The wiki states every mod requires a `mod.info` file at the jar's root
with these fields:

- `id` — unique lowercase identifier
- `name` — display name
- `version` — mod version
- `gameVersion` — target game version
- `description` — brief description
- `author` — creator name
- `dependencies` (optional) — required mod IDs
- `optionalDependencies` (optional) — optional mod IDs
- `clientside` (optional) — true/false for client/server-only mods

In the official example project, `mod.info` is **not hand-written** —
it is generated at build time by a Gradle task from properties declared
at the top of `build.gradle`:

```gradle
// From ExampleMod/build.gradle
project.ext.modID = "fair.examplemod"
project.ext.modName = "Example Mod"
project.ext.modVersion = "1.0"
project.ext.gameVersion = "1.3.2"
project.ext.modDescription = "Just an example mod"
project.ext.author = "Fair"
project.ext.clientside = false
//project.ext.modDependencies = ["other.modid1", "other.modid2"]
//project.ext.modOptionalDependencies = ["optional.modid1", "optional.modid2"]

tasks.register('createModInfoFile', JavaExec) {
    group "necesse"
    description "Creates the mod info file"
    classpath = files(gameDirectory + "/Necesse.jar")
    getMainClass().set("CreateModInfoFile")
    args "-file", "${sourceSets.main.java.getClassesDirectory().get()}/mod.info",
            "-id", "${project.ext.modID}",
            "-name", "${project.ext.modName}",
            "-version", "${project.ext.modVersion}",
            "-gameVersion", "${project.ext.gameVersion}",
            "-description", "${project.ext.modDescription}",
            "-author", "${project.ext.author}",
            "-clientside", "${project.ext.clientside}",
            "-dependencies", project.ext.has("modDependencies") ? "[" + project.ext.modDependencies.join(", ") + "]" : "",
            "-optionalDependencies", project.ext.has("modOptionalDependencies") ? "[" + project.ext.modOptionalDependencies.join(", ") + "]" : ""
}
classes.dependsOn("createModInfoFile")
```

The build script calls a game-provided CLI class `CreateModInfoFile`
(shipped inside `Necesse.jar`) to produce the actual `mod.info` file —
this is the field list source of truth and matches the wiki's list
1:1.

**IMPORTANT — clientside flag semantics** (wiki, verbatim comment in
`build.gradle`):

> "When setting clientside to true, it means servers do not need this
> mod for clients to connect and vice versa. IMPORTANT: If you set this
> to true, make sure that your mod does not add any content or do
> anything that could cause clients and servers to desync. This
> includes registering any items, objects, tiles, packets etc."

### @ModEntry lifecycle

Source: wiki `Modding` page.

A class annotated `@ModEntry` is the mod's entry point. Lifecycle
methods, in the order the wiki documents them:

| Method | Wiki description |
|---|---|
| `preInit()` | Called before core game loading (rarely used) |
| `init()` | Called after core loading; register tiles, objects, items, mobs |
| `initResources()` | Client-side only; load additional resources |
| `postInit()` | Called after all loading complete; register recipes and commands |
| `dispose()` | Called on game close for cleanup |

Real example from `ExampleMod.java` (GitHub) showing `init()`,
`initResources()`, and `postInit()` in use:

```java
@ModEntry
public class ExampleMod {

    public static ExampleBiome EXAMPLE_BIOME;

    public void init() {
        System.out.println("Hello world from my example mod!");
        EXAMPLE_BIOME = BiomeRegistry.registerBiome("exampleincursion", new ExampleBiome(), false);
        IncursionBiomeRegistry.registerBiome("exampleincursion", new ExampleIncursionBiome(), 1);
        LevelRegistry.registerLevel("exampleincursionlevel", ExampleIncursionLevel.class);
        TileRegistry.registerTile("exampletile", new ExampleTile(), 1, true);
        ObjectRegistry.registerObject("exampleobject", new ExampleObject(), 2, true);
        ItemRegistry.registerItem("exampleitem", new ExampleMaterialItem(), 10, true);
        ItemRegistry.registerItem("examplehuntincursionitem", new ExampleHuntIncursionMaterialItem(), 50, true);
        ItemRegistry.registerItem("examplesword", new ExampleSwordItem(), 20, true);
        ItemRegistry.registerItem("examplestaff", new ExampleProjectileWeapon(), 30, true);
        ItemRegistry.registerItem("examplepotionitem", new ExamplePotionItem(), 10, true);
        ItemRegistry.registerItem("examplefooditem", new ExampleFoodItem(),15, true);
        MobRegistry.registerMob("examplemob", ExampleMob.class, true);
        ProjectileRegistry.registerProjectile("exampleprojectile", ExampleProjectile.class, "exampleprojectile", "exampleprojectile_shadow");
        BuffRegistry.registerBuff("examplebuff", new ExampleBuff());
        PacketRegistry.registerPacket(ExamplePacket.class);
    }

    public void initResources() {
        // Fixes texture edge antialiasing artifacts: run the preAntialiasTextures gradle task
        ExampleMob.texture = GameTexture.fromFile("mobs/examplemob");
    }

    public void postInit() {
        Recipes.registerModRecipe(new Recipe(
                "exampleitem", 1, RecipeTechRegistry.NONE,
                new Ingredient[]{ new Ingredient("ironbar", 2) }
        ).showAfter("woodboat"));
        // ... more recipes ...
        Biome.defaultCaveMobs.add(100, "examplemob");
        CommandsManager.registerServerCommand(new ExampleChatCommand());
    }
}
```
(Source: `examplemod/ExampleMod.java`, GitHub `DrFair/ExampleMod`. Trimmed
for length; full recipe list is under [Recipes](#5-recipes-crafting--loot-tables) below.)

### Registration order

Source: wiki `Modding` page (verbatim recommended order):

> Tiles → Objects → Biomes → Buffs → Global ingredients → Recipe techs →
> Items → Enchantments → Mobs → Projectiles → Level data → Containers →
> World generators → Packets → Quests → Other.

### Resource folder layout

Source: wiki `Modding` page (prose) **+ verified against the actual file
tree of `DrFair/ExampleMod`** via the GitHub API
(`git/trees/master?recursive=1`). All paths below are confirmed present
in the shipped mod:

```
<mod jar root>
├── mod.info                        (generated at build time)
└── resources/
    ├── preview.png                 (Steam Workshop thumbnail)
    ├── locale/
    │   └── en.lang                 (language code file name, e.g. en.lang)
    ├── items/
    │   └── <item stringID>.png
    ├── tiles/
    │   └── <tile stringID>.png
    ├── objects/
    │   └── <object stringID>.png
    ├── mobs/
    │   └── <mob stringID>.png
    ├── buffs/
    │   └── <buff stringID>.png
    ├── projectiles/
    │   ├── <projectile stringID>.png
    │   └── <projectile stringID>_shadow.png   (optional separate shadow sprite)
    └── player/
        └── weapons/
            └── <itemStringID>.png  (in-hand weapon swing sprite, separate from item icon)
```

The wiki additionally states a `biomes/<biome stringID>` resource path
exists, but the example mod repo does **not** ship a `resources/biomes/`
folder (its example biome has no dedicated texture asset) — treat this
path as optional/biome-feature-dependent. UNVERIFIED what a biome-level
texture is actually used for.

Wiki, verbatim: *"Resources can overwrite existing game textures if
placed at identical paths."* — i.e. a mod's `resources/` tree can act as
a texture-pack style override of vanilla assets at matching paths.

### Building & testing (Gradle tasks)

Source: wiki `Modding` page + `ExampleMod/build.gradle` (verbatim).

| Task | Purpose |
|---|---|
| `runClient` | Run client with current mod, for single-instance testing |
| `runDevClient` | Run a second client instance (`-dev 1`) to test multiplayer locally against `runClient`, connecting via "localhost" |
| `runServer` | Run a dedicated server with the current mod |
| `buildModJar` | Builds the distributable jar into `./build/jar/` |
| `createModInfoFile` | Generates `mod.info` from `build.gradle` properties (runs automatically before `classes`) |
| `preAntialiasTextures` | Wiki/code comment: fixes black/halo edges on textures caused by alpha blending under rotation/scaling — reprocesses all textures in the resources folder and re-saves with fixed alpha edge color |

To publish to Steam Workshop, the wiki states the mod must: be enabled,
include a `preview.png` in the resources folder, and be loaded via the
`-mod` launch option; a button then appears in the in-game mods menu.

Decompiling the base game for reference: wiki says to add `Necesse.jar`
as a dependency in IntelliJ IDEA (Project Structure → Modules →
Dependencies) to browse decompiled core game code.

---

## 2. Registries — classes and example registration calls

All snippets below are verbatim from either the wiki (`Modding_Snippets`
page) or the GitHub `DrFair/ExampleMod` source files, as noted per
snippet. **The numeric/boolean trailing arguments in several `register*`
calls below are not explained in any fetched source** — see
[Confidence & gaps](#confidence--gaps).

### Tiles — `TileRegistry`

Google Doc (paraphrased summary, UNVERIFIED exact wording):
`TileRegistry.registerTile(stringID, tile, …)`; *"Registered tiles are
static objects, and there should never be constructed more than one of
the same tile"*; texture loading happens via overriding `loadTextures()`
on the tile class.

Real call, from `ExampleMod.java` (GitHub, verbatim):
```java
TileRegistry.registerTile("exampletile", new ExampleTile(), 1, true);
```

Real tile class, `ExampleTile.java` (GitHub, verbatim) — extends
`TerrainSplatterTile`:
```java
package examplemod.examples;

import necesse.engine.util.GameRandom;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.gameTile.TerrainSplatterTile;
import necesse.level.maps.Level;

import java.awt.*;

public class ExampleTile extends TerrainSplatterTile {

    private GameTexture texture;
    private final GameRandom drawRandom; // Used only in draw function

    public ExampleTile() {
        super(false, "exampletile");
        canBeMined = true;
        drawRandom = new GameRandom();
        roomProperties.add("outsidefloor");
        mapColor = new Color(200, 50, 200);
    }

    @Override
    protected void loadTextures() {
        super.loadTextures();
        texture = GameTexture.fromFile("tiles/exampletile");
    }

    @Override
    public Point getTerrainSprite(GameTextureSection gameTextureSection, Level level, int tileX, int tileY) {
        int tile;
        synchronized (drawRandom) {
            tile = drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(texture.getHeight() / 32);
        }
        return new Point(0, tile);
    }

    @Override
    public int getTerrainPriority() {
        return TerrainSplatterTile.PRIORITY_TERRAIN;
    }
}
```

Note the `texture.getHeight() / 32` — confirms tile sprite rows are
32px tall (see [Textures](#3-textures) below).

### Objects — `ObjectRegistry`

Real call, from `ExampleMod.java` (verbatim):
```java
ObjectRegistry.registerObject("exampleobject", new ExampleObject(), 2, true);
```

Real object class, `ExampleObject.java` (GitHub, verbatim, extends
`GameObject`):
```java
public class ExampleObject extends GameObject {

    private GameTexture texture;

    public ExampleObject() {
        super(new Rectangle(4, 4, 26, 26)); // Collision relative to the tile it's placed on
        // Remember that tiles are 32x32 pixels in size
        hoverHitbox = new Rectangle(0, -32, 32, 64); // 2 tiles high mouse hover hitbox
        toolType = ToolType.ALL; // Can be broken by all tools
        isLightTransparent = true; // Lets light pass through
        mapColor = new Color(31, 150, 148); // Also applies as debris color if not set
    }

    @Override
    public void loadTextures() {
        super.loadTextures();
        texture = GameTexture.fromFile("objects/exampleobject");
    }

    @Override
    public void addDrawables(List<LevelSortedDrawable> list, OrderableDrawables tileList, Level level, int tileX, int tileY, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        GameLight light = level.getLightLevel(tileX, tileY);
        int drawX = camera.getTileDrawX(tileX);
        int drawY = camera.getTileDrawY(tileY);
        TextureDrawOptions options = texture.initDraw().light(light).pos(drawX, drawY - texture.getHeight() + 32);
        list.add(new LevelSortedDrawable(this, tileX, tileY) {
            @Override
            public int getSortY() { return 16; } // Y-sort anchor, expected range [0-32]
            @Override
            public void draw(TickManager tickManager) { options.draw(); }
        });
    }

    @Override
    public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
        int drawX = camera.getTileDrawX(tileX);
        int drawY = camera.getTileDrawY(tileY);
        texture.initDraw().alpha(alpha).draw(drawX, drawY - texture.getHeight() + 32);
    }

    @Override
    public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
        return null; // return something else if this object has an object entity
    }
}
```
The code comment **"Remember that tiles are 32x32 pixels in size"** is
the clearest direct confirmation found of tile resolution.

### Items — `ItemRegistry`

Google Doc (paraphrased, UNVERIFIED): `ItemRegistry.registerItem(stringID, item, …)`;
*"Registered items are static objects, and they should never be
constructed more than once."* Item textures load from
`resources/items/<item stringID>`.

Real calls, `ExampleMod.java` (verbatim):
```java
ItemRegistry.registerItem("exampleitem", new ExampleMaterialItem(), 10, true);
ItemRegistry.registerItem("examplesword", new ExampleSwordItem(), 20, true);
ItemRegistry.registerItem("examplestaff", new ExampleProjectileWeapon(), 30, true);
```

Item base classes actually observed in the example mod (real,
subclassable base types — package paths as imported):

```java
// Simple crafting material
import necesse.inventory.item.matItem.MatItem;
public class ExampleMaterialItem extends MatItem {
    public ExampleMaterialItem() {
        super(100, Rarity.UNCOMMON);
    }
}
```

```java
// Melee weapon
import necesse.inventory.item.toolItem.swordToolItem.SwordToolItem;
public class ExampleSwordItem extends SwordToolItem {
    // Weapon attack textures are loaded from resources/player/weapons/<itemStringID>
    public ExampleSwordItem() {
        super(400, null);
        rarity = Item.Rarity.UNCOMMON;
        attackAnimTime.setBaseValue(300); // 300 ms attack time
        attackDamage.setBaseValue(20).setUpgradedValue(1, 95); // base + tier-1 upgraded value
        attackRange.setBaseValue(120);
        knockback.setBaseValue(100);
    }
}
```

```java
// Ranged/magic weapon that fires a custom Projectile
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.MagicProjectileToolItem;
public class ExampleProjectileWeapon extends MagicProjectileToolItem {
    // Code comment lists sibling classes for other weapon styles:
    // GunProjectileToolItem, BowProjectileToolItem, BoomerangToolItem, etc.
    public ExampleProjectileWeapon() {
        super(400, null);
        rarity = Rarity.RARE;
        attackAnimTime.setBaseValue(300);
        attackDamage.setBaseValue(20).setUpgradedValue(1, 110);
        velocity.setBaseValue(100);
        knockback.setBaseValue(50);
        attackRange.setBaseValue(1500);
        attackXOffset = 12;
        attackYOffset = 22;
    }
    // ... showAttack() plays a sound via SoundManager.playSound(GameResources.magicbolt1, ...)
    // ... onAttack() constructs and fires an ExampleProjectile, see Projectiles below
}
```

`Item.Rarity` values actually observed in fetched sources: `COMMON`,
`UNCOMMON`, `RARE` (the enum likely has more values, e.g. for
legendary/epic tiers, but none were seen in the fetched files —
UNVERIFIED beyond these three).

**Discrepancy worth flagging**: the wiki's `Modding_Snippets` "Adding a
sword" example uses `super(400);` (one-arg `SwordToolItem` constructor)
and `rarity = Item.Rarity.COMMON;`, while the current GitHub example mod
(targeting game version 1.3.2) uses `super(400, null);` (two-arg
constructor). This suggests the wiki snippet is older/outdated relative
to the constructor signature in the current game version. Treat the
two-arg form as more current, but the meaning of the second (`null`)
argument was not documented in any fetched source — UNVERIFIED.

### Mobs — `MobRegistry`

Google Doc (paraphrased, UNVERIFIED): *"Mobs are registered on a class
basis, and must have an empty constructor for the register to construct
them."* *"Textures used by mobs must be loaded during `initResources()`"*.

Real call, `ExampleMod.java` (verbatim):
```java
MobRegistry.registerMob("examplemob", ExampleMob.class, true);
```
(Note: registered by **Class**, not by instance, unlike tiles/objects/items.)

Texture actually loaded in `initResources()` (verbatim, matches the
Google Doc's claim):
```java
public void initResources() {
    ExampleMob.texture = GameTexture.fromFile("mobs/examplemob");
}
```

Real mob class, `ExampleMob.java` (GitHub, verbatim, extends
`HostileMob`):
```java
public class ExampleMob extends HostileMob {

    public static GameTexture texture; // Loaded in examplemod.ExampleMod.initResources()

    public static LootTable lootTable = new LootTable(
            ChanceLootItem.between(0.5f, "exampleitem", 1, 3) // 50% chance to drop between 1-3 example items
    );

    // MUST HAVE an empty constructor
    public ExampleMob() {
        super(200);
        setSpeed(50);
        setFriction(3);
        collision = new Rectangle(-10, -7, 20, 14);
        hitBox = new Rectangle(-14, -12, 28, 24);
        selectBox = new Rectangle(-14, -7 - 34, 28, 48);
    }

    @Override
    public void init() {
        super.init();
        ai = new BehaviourTreeAI<>(this, new CollisionPlayerChaserWandererAI<>(null, 12 * 32, new GameDamage(25), 25, 40000));
    }

    @Override
    public LootTable getLootTable() { return lootTable; }

    @Override
    public void spawnDeathParticles(float knockbackX, float knockbackY) {
        for (int i = 0; i < 4; i++) {
            getLevel().entityManager.addParticle(new FleshParticle(
                    getLevel(), texture, GameRandom.globalRandom.nextInt(5), 8, 32,
                    x, y, 20f, knockbackX, knockbackY
            ), Particle.GType.IMPORTANT_COSMETIC);
        }
    }

    @Override
    protected void addDrawables(List<MobDrawable> list, OrderableDrawables tileList, OrderableDrawables topList, Level level, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
        GameLight light = level.getLightLevel(getTileX(), getTileY());
        int drawX = camera.getDrawX(x) - 32;
        int drawY = camera.getDrawY(y) - 51;
        Point sprite = getAnimSprite(x, y, getDir()); // helper: current animation/direction frame
        drawY += getBobbing(x, y);
        drawY += getLevel().getTile(getTileX(), getTileY()).getMobSinkingAmount(this);
        DrawOptions drawOptions = texture.initDraw().sprite(sprite.x, sprite.y, 64).light(light).pos(drawX, drawY);
        list.add(new MobDrawable() {
            @Override public void draw(TickManager tickManager) { drawOptions.draw(); }
        });
        addShadowDrawables(tileList, level, x, y, light, camera);
    }

    @Override
    public int getRockSpeed() { return 20; } // Speed at which the mob's animation plays
}
```
AI is built from `BehaviourTreeAI` + a stock behaviour
`CollisionPlayerChaserWandererAI<>(target, aggroRangePx, GameDamage, damageAmount, cooldownMs)`
— constructor args as literally passed: `(null, 12 * 32, new GameDamage(25), 25, 40000)`.
Exact meaning of each positional argument beyond what's inferable from
names is UNVERIFIED (not documented in fetched text).

### Buffs — `BuffRegistry`

Google Doc (paraphrased, UNVERIFIED): `BuffRegistry.registerBuff(stringID, buff)`;
*"Registered buffs are static objects, and they should never be
constructed more than once."* Texture from `resources/buffs/<buff stringID>`.

Real call (verbatim): `BuffRegistry.registerBuff("examplebuff", new ExampleBuff());`

Real buff class, `ExampleBuff.java` (GitHub, verbatim, extends `Buff`):
```java
public class ExampleBuff extends Buff {

    public ExampleBuff() {
        canCancel = true;
        isVisible = true;
        shouldSave = true;
    }

    @Override
    public void init(ActiveBuff activeBuff, BuffEventSubscriber buffEventSubscriber) {
        activeBuff.setModifier(BuffModifiers.SPEED, 0.5f); // +50% speed
    }

    @Override
    public void serverTick(ActiveBuff buff) { /* server-side tick logic */ }

    @Override
    public void clientTick(ActiveBuff buff) { /* client-side tick logic, e.g. particles */ }
}
```

### Projectiles — `ProjectileRegistry`

Google Doc (paraphrased, UNVERIFIED): `ProjectileRegistry.registerProjectile(stringID, projectile, texturePath)`;
*"Texture path will be a sub path after resources/projectiles/<texturePath>.
Can be null if the projectile doesn't have a texture."* Also: *"Projectiles
are never saved in the game, meaning they will disappear when a level is
unloaded."*

Real call, `ExampleMod.java` (verbatim — takes a **Class**, plus a
texture path and a separate shadow-texture path):
```java
ProjectileRegistry.registerProjectile("exampleprojectile", ExampleProjectile.class, "exampleprojectile", "exampleprojectile_shadow");
```

Real projectile class, `ExampleProjectile.java` (GitHub, verbatim,
extends `FollowingProjectile`):
```java
public class ExampleProjectile extends FollowingProjectile {

    public ExampleProjectile() {} // Each projectile must have an empty constructor for the registry

    // Constructor used on attack to spawn with real parameters
    public ExampleProjectile(Level level, Mob owner, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback) {
        this.setLevel(level);
        this.setOwner(owner);
        this.x = x; this.y = y;
        this.setTarget(targetX, targetY);
        this.speed = speed;
        this.distance = distance;
        this.setDamage(damage);
        this.knockback = knockback;
    }

    public void init() {
        super.init();
        turnSpeed = 1.25f;      // homing turn speed
        givesLight = false;
        height = 18;            // flying height above ground, in pixels
        trailOffset = -14f;
        setWidth(16, true);
        piercing = 2;           // pierces 2 mobs before disappearing
        bouncing = 10;          // can bounce 10 times off walls
    }

    @Override
    public Color getParticleColor() { return new Color(63, 157, 18); }

    @Override
    public Trail getTrail() { return new Trail(this, getLevel(), new Color(191, 147, 22), 26, 500, getHeight()); }

    @Override
    public void updateTarget() {
        if (traveledDistance > 20) {
            findTarget(m -> m.isHostile, 200, 450);
        }
    }
    // addDrawables(...) rotates the texture around its tip and draws a matching shadow
}
```
Fired from the weapon item via (verbatim, from `ExampleProjectileWeapon.onAttack`):
```java
Projectile projectile = new ExampleProjectile(
        level, attackerMob, attackerMob.x, attackerMob.y, x, y,
        getProjectileVelocity(item, attackerMob), getAttackRange(item),
        getAttackDamage(item), getKnockback(item, attackerMob)
);
GameRandom random = new GameRandom(seed);
projectile.resetUniqueID(random); // sync uniqueID between attacking client and server
projectile.moveDist(40);
attackerMob.addAndSendAttackerProjectile(projectile);
consumeMana(attackerMob, item);
```

### Biomes — `BiomeRegistry` / `IncursionBiomeRegistry`

Real calls (verbatim, `ExampleMod.java`):
```java
// Register a simple biome that will not appear in natural world gen (3rd arg = false)
EXAMPLE_BIOME = BiomeRegistry.registerBiome("exampleincursion", new ExampleBiome(), false);

// Register an "incursion" biome (a boss-altar-triggered instanced biome), tier requirement 1
IncursionBiomeRegistry.registerBiome("exampleincursion", new ExampleIncursionBiome(), 1);
```
From `Modding_Snippets` (wiki, verbatim) — the 3rd boolean argument to
`BiomeRegistry.registerBiome` corroborated as "does this biome generate
naturally":
```java
// Generation weight determines the likelihood of a biome spawning in the world. 1.0f is the typical vanilla value.
// The generation weight MUST be set for the biome to spawn naturally in the world.
BiomeRegistry.registerBiome("example_biome", new ExampleBiome().setGenerationWeight(1.0f), true);
```
So: `registerBiome(stringID, Biome instance, boolean generatesNaturally)`,
and a biome intended to generate naturally must also call
`.setGenerationWeight(float)` on itself.

Full natural-biome example, `Modding_Snippets` page, "Creating a new
biome" by wiki contributor **Eryr** (verbatim, abbreviated to the
class-level API surface — see the live wiki page for the complete
~200-line class):
```java
public class ExampleBiome extends Biome {
    public static FishingLootTable SurfaceFish = new FishingLootTable().addAll(Biome.defaultSurfaceFish);
    public static MobSpawnTable SurfaceMobs = new MobSpawnTable().add(100, "zombie");
    public static MobSpawnTable CaveMobs = new MobSpawnTable().add(100, "skeleton");
    public static MobSpawnTable DeepCaveMobs = new MobSpawnTable().add(100, "ancientskeleton");

    public ExampleBiome() {}

    public boolean canRain(Level level) { return true; }

    public FishingLootTable getFishingLootTable(FishingSpot Spot) {
        if (Spot.tile.level.getIdentifier() == LevelIdentifier.DEEP_CAVE_IDENTIFIER) return DeepCaveFish;
        else if (Spot.tile.level.isCave) return CaveFish;
        else return SurfaceFish;
    }

    public MobSpawnTable getMobSpawnTable(Level Level) {
        if (Level.getIdentifier() == LevelIdentifier.DEEP_CAVE_IDENTIFIER) return DeepCaveMobs;
        else if (Level.isCave) return CaveMobs;
        else return SurfaceMobs;
    }

    public AbstractMusicList getLevelMusic(Level Level, PlayerMob perspective) {
        if (Level.getIdentifier() == LevelIdentifier.DEEP_CAVE_IDENTIFIER) return new MusicList(new GameMusic[]{MusicRegistry.ForgottenDepths});
        else if (Level.isCave) return new MusicList(new GameMusic[]{MusicRegistry.Away});
        else return new MusicList(new GameMusic[]{MusicRegistry.ByTheField});
    }

    public int getGenerationWaterTileID() { return TileRegistry.waterID; }
    public int getGenerationTerrainTileID() { return TileRegistry.grassID; }
    public int getGenerationCaveTileID() { return TileRegistry.rockID; }
    public int getGenerationBeachTileID() { return TileRegistry.sandID; }
    public int getGenerationCaveRockObjectID() { return ObjectRegistry.rockID; }
    public int getGenerationDeepCaveRockObjectID() { return ObjectRegistry.deepRockID; }
    public VillageSet[] getVillageSets() { return new VillageSet[] { VillageSet.pine, VillageSet.oak, VillageSet.spruce }; }

    public void initializeGeneratorStack(BiomeGeneratorStack stack) {
        super.initializeGeneratorStack(stack);
        stack.addRandomSimplexVeinsBranch("biomeTrees", 2.0F, 0.2F, 0.4F, 0);
        stack.addRandomVeinsBranch("biomeBushes", 0.045F, 4, 8, 0.6F, 0, false);
        stack.addRandomVeinsBranch("biomeCopper", 0.72F, 3, 6, 0.4F, 2, false);
    }

    public void generateRegionSurfaceTerrain(Region region, BiomeGeneratorStack stack, GameRandom random) {
        super.generateRegionSurfaceTerrain(region, stack, random);
        stack.startPlaceOnVein(this, region, random, "biomeTrees").onlyOnTile(TileRegistry.grassID).chance(0.15).placeObject("oaktree");
        stack.startPlaceOnVein(this, region, random, "biomeBushes").onlyOnTile(TileRegistry.grassID).placeObjectFruitGrower("blueberrybush");
        region.updateLiquidManager();
    }

    @Override
    public void generateRegionCaveTerrain(Region region, BiomeGeneratorStack stack, GameRandom random) {
        super.generateRegionCaveTerrain(region, stack, random);
        stack.startPlaceOnVein(this, region, random, "biomeCopper").onlyOnObject(ObjectRegistry.rockID).placeObjectForced("copperorerock");
    }
}
```
Registration for the natural biome (`Modding_Snippets`, verbatim):
```java
BiomeRegistry.registerBiome("example_biome", new ExampleBiome().setGenerationWeight(1.0f), true);
```

The stripped-down `ExampleBiome` actually shipped in the GitHub repo
(used only as the incursion's biome, not naturally generated) is much
shorter (verbatim):
```java
public class ExampleBiome extends Biome {
    public static MobSpawnTable critters = new MobSpawnTable().include(Biome.defaultCaveCritters);
    public static MobSpawnTable mobs = new MobSpawnTable().add(100,"examplemob");

    @Override
    public AbstractMusicList getLevelMusic(Level level, PlayerMob perspective) {
        return new MusicList(MusicRegistry.ForestPath);
    }
    @Override
    public MobSpawnTable getCritterSpawnTable(Level level) { return critters; }
    @Override
    public MobSpawnTable getMobSpawnTable(Level level) { return mobs; }
}
```

`IncursionBiome` real subclass, `ExampleIncursionBiome.java` (GitHub,
verbatim) — defines a boss-gated instanced biome:
```java
public class ExampleIncursionBiome extends IncursionBiome {

    public ExampleIncursionBiome() {
        super("reaper"); // boss mob string ID for this incursion
    }

    @Override
    public Collection<Item> getExtractionItems(IncursionData data) {
        return Collections.singleton(ItemRegistry.getItem("tungstenore"));
    }

    @Override
    public LootTable getHuntDrop(IncursionData incursionData) {
        return new LootTable(new ChanceLootItem(0.66F, "examplehuntincursionitem"));
    }

    @Override
    public TicketSystemList<Supplier<IncursionData>> getAvailableIncursions(int tabletTier, IncursionData incursionData) {
        TicketSystemList<Supplier<IncursionData>> system = new TicketSystemList<>();
        int huntTickets = 100;
        int extractionTickets = 100;
        if (incursionData != null) {
            huntTickets = (int) (huntTickets * incursionData.nextIncursionModifiers.getModifier(IncursionDataModifiers.MODIFIER_HUNT_DROPS));
            extractionTickets = (int) (extractionTickets * incursionData.nextIncursionModifiers.getModifier(IncursionDataModifiers.MODIFIER_EXTRACTION_DROPS));
        }
        system.addObject(huntTickets, () -> new BiomeHuntIncursionData(1.0F, this, tabletTier));
        system.addObject(extractionTickets, () -> new BiomeExtractionIncursionData(1.0F, this, tabletTier));
        return system;
    }

    @Override
    public IncursionLevel getNewIncursionLevel(FallenAltarObjectEntity altar, LevelIdentifier identifier,
                                               BiomeMissionIncursionData incursion, Server server,
                                               WorldEntity world, AltarData altarData) {
        return new ExampleIncursionLevel(identifier, incursion, world, altarData);
    }

    @Override
    public ArrayList<Color> getFallenAltarGatewayColorsForBiome() {
        // IncursionBiome requires this method; expected to return a list of 6 colors
        ArrayList<Color> colors = new ArrayList<>();
        colors.add(new Color(181, 80, 120));
        colors.add(new Color(215, 42, 52));
        colors.add(new Color(181, 92, 59));
        colors.add(new Color(181, 80, 120));
        colors.add(new Color(215, 42, 52));
        colors.add(new Color(181, 92, 59));
        return colors;
    }
}
```

### Levels — `LevelRegistry` (see also §6 below)

Real call (verbatim): `LevelRegistry.registerLevel("exampleincursionlevel", ExampleIncursionLevel.class);`
— registers a **custom `Level` subclass by Class**, keyed by string ID.

### Recipe techs — `RecipeTechRegistry`

Google Doc: `RecipeTechRegistry.registerTech("EXAMPLETECH")`. Default
techs listed by the Google Doc: `NONE (Inventory), WORKSTATION, FORGE,
CARPENTER, IRONANVIL, DEMONIC, ALCHEMY, ADVANCED`. The real example mod
additionally references `RecipeTechRegistry.COOKING_POT` (verbatim, used
for the food item recipe) — so the full tech list is larger than the
Google Doc's summary suggests; treat that list as non-exhaustive.

### World Presets — `WorldPresetRegistry`

Wiki (`Modding_Snippets`, "Creating a world preset" by **Eryr**, verbatim):
```java
WorldPresetRegistry.registerPreset("example_preset", (WorldPreset) new ExampleCabinWorldPreset());
```

### Packets — `PacketRegistry`

See the dedicated [Packets](#packets) subsection below.

### Sounds

**Not covered by wiki.** No registry for adding *new* custom sound
effects was found in any fetched source. What was found is only reuse
of existing engine sound facilities:
- `SoundSettingsRegistry.wind` — an existing sound-settings object
  returned from `Biome.getWindSound(Level)` (wiki `Modding_Snippets`).
- `SoundManager.playSound(GameResources.magicbolt1, SoundEffect.effect(attackerMob).volume(0.7f).pitch(...))`
  — playing a **built-in** game sound resource (`GameResources.magicbolt1`)
  from an item's attack code (GitHub `ExampleProjectileWeapon.java`,
  verbatim call shown above under Items).

No mod-supplied `.ogg`/`.wav` asset folder or a `SoundRegistry.register...`
call was observed anywhere in the wiki, snippets page, or the example
mod's file tree (there is no `resources/sounds/` folder in
`DrFair/ExampleMod`). UNVERIFIED whether/how mods can ship brand-new
audio assets — needs decompiled-source check.

---

## 3. Textures

Sizes below were **not just read from wiki text** — where possible they
were directly measured from the actual PNG files shipped in
`DrFair/ExampleMod` (downloaded via `raw.githubusercontent.com` and
inspected with the `file` command), which is the most reliable way to
answer "what resolution is one tile" without decompiling the game
itself.

| Asset | Measured size | Source |
|---|---|---|
| **One tile** | **32 × 32 px** | Code comment in `ExampleObject.java`: *"Remember that tiles are 32x32 pixels in size"*; corroborated by `ExampleTile.java`'s `texture.getHeight() / 32` sprite-row math and by measuring `tiles/exampletile.png` |
| Tile texture sheet (`tiles/exampletile.png`) | **32 × 128 px** (1 column × 4 rows of 32×32 variant sprites) | Measured PNG; matches `ExampleTile.getTerrainSprite()` picking a random row via `nextInt(texture.getHeight()/32)` and returning `Point(0, tile)` |
| Item icon (`items/exampleitem.png`, `items/examplesword.png`) | **32 × 32 px** | Measured PNGs; matches wiki `Modding_Snippets` text: *"The item icon in the items folder should be 32x32"* |
| Buff icon (`buffs/examplebuff.png`) | **32 × 32 px** | Measured PNG |
| Object texture (`objects/exampleobject.png`) | **32 × 64 px** (1 tile wide × 2 tiles tall) | Measured PNG; consistent with the object's `hoverHitbox = new Rectangle(0, -32, 32, 64)` ("2 tiles high") and its draw call anchoring at `drawY - texture.getHeight() + 32` |
| Mob sprite sheet (`mobs/examplemob.png`) | **384 × 320 px overall**; drawn using **64 × 64 px** frame cells (`texture.initDraw().sprite(sprite.x, sprite.y, 64)`) → a grid of 6 columns × 5 rows = 30 possible frame cells | Measured PNG + `ExampleMob.addDrawables()` verbatim draw call |
| In-hand weapon sprite, sword (`player/weapons/examplesword.png`) | **36 × 36 px** | Measured PNG. Separate from the 32×32 item icon; wiki comment: *"Weapon attack textures are loaded from resources/player/weapons/<itemStringID>"* |
| In-hand weapon sprite, staff (`player/weapons/examplestaff.png`) | **40 × 40 px** | Measured PNG — confirms weapon swing-sprite size is **not fixed**, it's per-weapon/design-dependent |
| Projectile sprite (`projectiles/exampleprojectile.png`) | **22 × 28 px** | Measured PNG — arbitrary/design-dependent, no fixed requirement found |

Notes / UNVERIFIED points:
- The exact **mapping convention** of rows/columns in a mob sheet to
  specific directions and animation frame indices (e.g. which row is
  "facing down", how many walk-cycle frames per direction) was **not
  documented in any fetched source** — it's produced by a
  `getAnimSprite(x, y, getDir())` helper method inherited from the base
  `Mob`/`HostileMob` classes, whose implementation was not visible in
  any fetched page. This needs decompiled-source verification.
- Item icon size (32×32) and weapon swing-sprite size (variable, not
  32×32) are two **different, easily confused** assets that both live
  under `resources/` for the same item stringID but in different
  subfolders (`items/` vs `player/weapons/`).
- Texture packs (wiki `Modding` page): official textures can be
  overwritten wholesale by extracting to `<Necesse install
  directory>/res`; a mod's own `resources/` tree overrides vanilla
  assets at matching paths (this is the same mechanism used for adding
  new textures, just pointed at an existing path).
- Wiki mentions a `preAntialiasTextures` Gradle task to fix
  alpha-blending edge artifacts after rotation/scaling — run it before
  final build if textures show a black/halo outline.

---

## 4. Localization

Source: wiki `Modding` page (format description, verbatim) + GitHub
`ExampleMod/src/main/resources/locale/en.lang` (real, complete file).

### File format & location

- Path: `resources/locale/<language code>.lang` (e.g. `en.lang`).
- Format is category/key based:

```
[mycategory]
mykey=Text
[item]
exampleitem=Example item
```

- Wiki: *"Translations support runtime replacements using `<key>`
  syntax."*
- Access translations via (wiki, verbatim):
  - `Localization.translate(category, key)`
  - `new LocalMessage(category, key, replaceKey, replaceString)`

Real, complete `en.lang` from the example mod (GitHub, verbatim —
this is the full file):
```
[tile]
exampletile=Example Tile

[object]
exampleobject=Example Object

[item]
exampleitem=Example Item
examplehuntincursionitem=Example Hunt Incursion Item
examplepotionitem=Example Potion
examplesword=Example Sword
examplestaff=Example Staff
examplefooditem=Example Food

[itemtooltip]
examplestafftip=Shoots a homing, piercing projectile
examplepotionitemtip= An example potion

[mob]
examplemob=Example Mob

[buff]
examplebuff=Example Buff

[biome]
exampleincursion=Example Incursion

[incursion]
exampleincursion=Example Incursion
```

This confirms the key-naming convention for each content category:
**the category name is the registry type in lowercase singular**
(`[tile]`, `[object]`, `[item]`, `[mob]`, `[buff]`, `[biome]`,
`[incursion]`), and **the key is exactly the stringID used when
registering that content**, mapping to its display name. Tooltips use a
`[itemtooltip]` category with keys the mod itself invents and then
fetches explicitly (see below), not an automatic stringID-based key.

Real usage of a tooltip translation key, `ExampleProjectileWeapon.java`
(verbatim):
```java
tooltips.add(Localization.translate("itemtooltip", "examplestafftip"));
```

Real usage of a multi-key message helper for NPC dialogue lines
(`Modding_Snippets`, "Example Shopkeeper", verbatim) — looks up
`example_shopkeeper_greeting1`, `..._greeting2`, `..._greeting3` under
category `[mobmsg]`:
```java
protected ArrayList<GameMessage> getMessages(ServerClient client) {
    // Looks up example_shopkeeper_greeting1/2/3 under category 'mobmsg'
    return this.getLocalMessages("example_shopkeeper_greeting", 3);
}
```
with the matching `.lang` entries:
```
[mobmsg]
example_shopkeeper_greeting1=Exp1!
example_shopkeeper_greeting2=Exp2!
example_shopkeeper_greeting3=Exp3!
```

---

## 5. Recipes, crafting & loot tables

### Recipes

Registered during `postInit()` (wiki). Two mechanisms documented:

**A. Programmatic**, via `Recipes.registerModRecipe(new Recipe(...))`.
Real, verbatim examples from `ExampleMod.postInit()`:
```java
// Crafted in inventory (RecipeTechRegistry.NONE) for 2 iron bars
Recipes.registerModRecipe(new Recipe(
        "exampleitem", 1, RecipeTechRegistry.NONE,
        new Ingredient[]{ new Ingredient("ironbar", 2) }
).showAfter("woodboat")); // Show recipe after wood boat recipe in the menu

// Crafted in iron anvil using 4 example items and 5 copper bars
Recipes.registerModRecipe(new Recipe(
        "examplesword", 1, RecipeTechRegistry.IRON_ANVIL,
        new Ingredient[]{
                new Ingredient("exampleitem", 4),
                new Ingredient("copperbar", 5)
        }
));

// Crafted in workstation using 4 example items and 10 gold bars
Recipes.registerModRecipe(new Recipe(
        "examplestaff", 1, RecipeTechRegistry.WORKSTATION,
        new Ingredient[]{
                new Ingredient("exampleitem", 4),
                new Ingredient("goldbar", 10)
        }
).showAfter("exampleitem"));

// Food item, crafted at a cooking pot
Recipes.registerModRecipe(new Recipe(
        "examplefooditem", 1, RecipeTechRegistry.COOKING_POT,
        new Ingredient[]{
                new Ingredient("bread", 1),
                new Ingredient("strawberry", 2),
                new Ingredient("sugar", 1)
        }
));
```
`Recipe(String resultStringID, int resultAmount, RecipeTech tech,
Ingredient[] ingredients)` is the constructor shape used consistently
across every example seen. `.showAfter(otherItemStringID)` controls menu
ordering.

**B. Data-driven**, via a `recipes.cfg` file. Google Doc gives this
syntax example (UNVERIFIED — presented as prose-recovered, not a
guaranteed-exact quote):
```
{firearrow, 5, NONE, INGREDIENTS{{stonearrow, 5}, {torch, 1}}}
```

Default recipe techs (Google Doc, UNVERIFIED wording, but the identifiers
themselves are corroborated by real code — `RecipeTechRegistry.NONE`,
`.IRON_ANVIL`, `.WORKSTATION`, `.COOKING_POT` were all seen used
verbatim in the GitHub example mod):
> NONE (Inventory), WORKSTATION, FORGE, CARPENTER, IRONANVIL, DEMONIC,
> ALCHEMY, ADVANCED

Custom tech registration (Google Doc, UNVERIFIED exact call):
```java
RecipeTechRegistry.registerTech("EXAMPLETECH")
```

### Loot tables

Interfaces/classes seen: `LootItemInterface`, `LootTable`, `LootItem`,
`ChanceLootItem`, `OneOfTicketLootItems`.

Real, verbatim usage from the GitHub example mod:
```java
// Static loot table for a mob
public static LootTable lootTable = new LootTable(
        ChanceLootItem.between(0.5f, "exampleitem", 1, 3) // 50% chance to drop between 1 and 3 example items
);

// Loot table for an incursion "hunt" objective drop
return new LootTable(
        new ChanceLootItem(0.66F, "examplehuntincursionitem")
);
```
Note `ChanceLootItem` has (at least) two different construction forms
seen in real code: a 2-arg constructor `new ChanceLootItem(chance, stringID)`
and a static factory `ChanceLootItem.between(chance, stringID, min, max)`
for a random quantity range.

Google Doc (paraphrased, UNVERIFIED) additionally describes:
```java
lootTable.items.add(new LootItem("myitem"));
lootTable.items.add(new ChanceLootItem(0.5f, "myitem"));
lootTable.items.add(new OneOfTicketLootItems(
        1, LootItemInterface1,
        3, LootItemInterface2
));
```
`LootTablePresets` is also referenced (e.g.
`LootTablePresets.basicCaveChest`, `.hunterCookedFoodLootTable`) as a
library of ready-made vanilla loot tables reusable by mods
(`Modding_Snippets`, verbatim usages).

---

## 6. Levels & dimensions

Source: mostly the Google Doc (much of it paraphrased/UNVERIFIED) plus
real, verbatim code from `Modding_Snippets` and the GitHub example mod
for the parts that actually have working sample code
(`LevelIdentifier`, `LevelRegistry`, `IncursionLevel`).

### How the game addresses levels — `LevelIdentifier`

`necesse.engine.util.LevelIdentifier` is imported and used directly in
real code (`Modding_Snippets`, verbatim):
```java
import necesse.engine.util.LevelIdentifier;
...
if (Spot.tile.level.getIdentifier() == LevelIdentifier.DEEP_CAVE_IDENTIFIER) { ... }
else if (Spot.tile.level.isCave) { ... }
else { ... } // surface
```
Constants/members actually observed in use:
- `LevelIdentifier.DEEP_CAVE_IDENTIFIER`
- `LevelIdentifier.SURFACE_IDENTIFIER` (used in `Modding_Snippets`'
  world-preset example: `presetsRegion.identifier.equals(LevelIdentifier.SURFACE_IDENTIFIER)`)
- `Level.getIdentifier()` — returns a level's `LevelIdentifier`
- `Level.isCave` — boolean field distinguishing a cave-type level from
  a surface-type one

No fetched source explained the internal structure/fields of
`LevelIdentifier` itself (e.g. whether it encodes dimension index +
island coordinates, or is an opaque key) — **UNVERIFIED**, needs
decompiled-source check.

### Registering a custom Level class — `LevelRegistry`

Real, verbatim call (`ExampleMod.java`):
```java
LevelRegistry.registerLevel("exampleincursionlevel", ExampleIncursionLevel.class);
```

Real custom `Level` subclass, `ExampleIncursionLevel.java` (GitHub,
verbatim, extends `necesse.level.maps.IncursionLevel`, which itself
extends `Level`):
```java
public class ExampleIncursionLevel extends IncursionLevel {

    // A constructor with this exact signature is REQUIRED and used for loading, etc.
    public ExampleIncursionLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
        super(identifier, width, height, worldEntity);
        this.baseBiome = ExampleMod.EXAMPLE_BIOME;
        this.isCave = true;
    }

    // Constructor used when an incursion is generated and entered — creates a
    // fixed-size level and immediately generates its contents
    public ExampleIncursionLevel(LevelIdentifier identifier, BiomeMissionIncursionData incursionData, WorldEntity worldEntity, AltarData altarData) {
        super(identifier, 150, 150, incursionData, worldEntity);
        this.baseBiome = ExampleMod.EXAMPLE_BIOME;
        this.isCave = true;
        generateLevel(incursionData, altarData);
    }

    public void generateLevel(BiomeMissionIncursionData incursionData, AltarData altarData) {
        CaveGeneration cg = new CaveGeneration(this, "deeprocktile", "deeprock");
        cg.random.setSeed(incursionData.getUniqueID()); // deterministic layout per mission

        GameEvents.triggerEvent(
                new GenerateCaveLayoutEvent(this, cg),
                e -> { cg.generateLevel(0.38F, 4, 3, 6); }
        );

        PresetGeneration entranceAndPerkPresets = new PresetGeneration(this);
        boolean hasBiggerArenaPerk = altarData.hasPerk(IncursionPerksRegistry.BIGGER_ARENA);
        IncursionBiome.generateEntrance(
                this, entranceAndPerkPresets, cg.random, 32, cg.rockTile,
                "exampletile", "exampletile", "exampleobject", hasBiggerArenaPerk
        );

        generatePresetsBasedOnPerks(altarData, incursionData, entranceAndPerkPresets, cg.random, baseBiome);
        GenerationTools.checkValid(this); // clears invalid cut-in-half objects/tiles

        if (incursionData instanceof BiomeExtractionIncursionData) {
            cg.generateGuaranteedOreVeins(40, 4, 8, ObjectRegistry.getObjectID("tungstenoredeeprock"));
        }
        cg.generateGuaranteedOreVeins(75, 6, 12, ObjectRegistry.getObjectID("upgradesharddeeprock"));
        cg.generateGuaranteedOreVeins(75, 6, 12, ObjectRegistry.getObjectID("alchemysharddeeprock"));
        generateOresBasedOnPerks(altarData, cg, this, baseBiome, cg.random);

        GameEvents.triggerEvent(new GeneratedCaveOresEvent(this, cg));
    }
}
```
This is the most concrete, real, compiling example of "a custom level
type" found in any fetched source — but it is specifically an
**Incursion** level (a boss-altar-gated instanced dungeon reached
through a `FallenAltarObjectEntity`, generated on demand via
`IncursionBiome.getNewIncursionLevel(...)`), **not** a generic new
persistent overworld dimension/island. Treat everything about
"dimensions" below with more caution.

### Cave layers vs. surface

Confirmed distinctions that exist and are checkable from mod code:
`Level.isCave` (boolean), `LevelIdentifier.DEEP_CAVE_IDENTIFIER` vs.
implicit "surface" (checked via `!isCave` or via
`LevelIdentifier.SURFACE_IDENTIFIER`). Biomes provide different tile
IDs/mob spawn tables/music per layer via methods like
`getGenerationCaveTileID()`, `getGenerationDeepCaveRockObjectID()`,
`getMobSpawnTable(Level)` branching on `level.isCave`, etc. (all shown
above under Biomes).

### Ladders / how surface connects to caves

**Not covered by wiki, Modding_Snippets, the Google Doc, or the example
mod.** No object/tile class, registry call, or API related to a
"ladder" or "cave entrance" transition mechanism was found in any
fetched source. This is a gap — needs decompiled-source verification.

### Custom "island" dimensions

**Not directly covered as a worked example.** The only material found
is the `WorldGenerator` abstract class description in the Google Doc
(see [World generation](#7-world-generation) below), whose core method
signature explicitly takes a `dimension` integer:
```java
Level getNewLevel(int islandX, int islandY, int dimension, Server server)
```
This implies the base game already models the world as multiple
dimensions, each containing a grid of islands addressed by
`(islandX, islandY)` coordinates, and that a modded `WorldGenerator` can
branch on the `dimension` parameter to serve different `Level`s per
dimension. **No fetched source showed a real, worked code example of a
mod registering a brand-new dimension index or wiring up the
registration call itself** — the registration call
(`WorldGenerator.registerGenerator(...)`, per the Google Doc summary)
came back only as a paraphrase with a literal "…" in place of real
arguments, so treat it as **UNVERIFIED** shorthand, not a real
signature. This whole area needs decompiled-source verification before
relying on it.

---

## 7. World generation

Source: Google Doc (paraphrased overview, UNVERIFIED in its exact
wording) + `Modding_Snippets` wiki page (real, verbatim worked
examples for biome-level generation and world presets).

### `WorldGenerator` (Google Doc summary — UNVERIFIED exact code)

> "A world generator determines which levels are generated where. Mods
> can register their own generators which CAN then overwrite the
> previous world generator."

Abstract class methods described (UNVERIFIED — recovered as isolated
signatures, not confirmed verbatim from the original doc formatting):
```java
Level getNewLevel(int islandX, int islandY, int dimension, Server server) // required; should be deterministic for the same inputs
Biome biome(int islandX, int islandY)      // optional
float islandSize(int islandX, int islandY) // optional
long islandSeed(int islandX, int islandY)  // optional
Point startingIsland(int spawnSeed)        // optional
```
The doc states multiple generators "cascade" — if one generator returns
`null`, the next registered generator is tried.

### Biome-level terrain generation — `BiomeGeneratorStack` (real, verbatim)

From `Modding_Snippets`, "Creating a new biome" (Eryr) — this is fully
worked, real (if age-uncertain, see caveat below) code showing how a
biome lays down tiles/objects during world gen:

```java
public void initializeGeneratorStack(BiomeGeneratorStack stack) {
    super.initializeGeneratorStack(stack);
    // Branch for objects placed with even spacing (e.g. trees)
    stack.addRandomSimplexVeinsBranch("biomeTrees", 2.0F, 0.2F, 0.4F, 0);
    // Branch for objects placed in clustered groups (e.g. bushes)
    stack.addRandomVeinsBranch("biomeBushes", 0.045F, 4, 8, 0.6F, 0, false);
    // Ore veins for the cave layer
    stack.addRandomVeinsBranch("biomeCopper", 0.72F, 3, 6, 0.4F, 2, false);
    stack.addRandomVeinsBranch("biomeIron", 0.56F, 3, 6, 0.4F, 2, false);
    stack.addRandomVeinsBranch("biomeGold", 0.16F, 3, 6, 0.4F, 2, false);
}

public void generateRegionSurfaceTerrain(Region region, BiomeGeneratorStack stack, GameRandom random) {
    super.generateRegionSurfaceTerrain(region, stack, random);
    stack.startPlaceOnVein(this, region, random, "biomeTrees")
         .onlyOnTile(TileRegistry.grassID).chance(0.15).placeObject("oaktree");
    stack.startPlaceOnVein(this, region, random, "biomeBushes")
         .onlyOnTile(TileRegistry.grassID).placeObjectFruitGrower("blueberrybush");
    stack.startPlace(this, region, random).chance(0.003).placeObject("surfacerock");
    region.updateLiquidManager();
}

@Override
public void generateRegionCaveTerrain(Region region, BiomeGeneratorStack stack, GameRandom random) {
    super.generateRegionCaveTerrain(region, stack, random);
    stack.startPlaceOnVein(this, region, random, "biomeCopper")
         .onlyOnObject(ObjectRegistry.rockID).placeObjectForced("copperorerock");
}
```
Chained builder calls seen: `.onlyOnTile(id)`, `.onlyOnObject(id)`,
`.chance(double)`, `.placeObject(stringID)`,
`.placeObjectFruitGrower(stringID)`, `.placeObjectForced(stringID)`.
`region.updateLiquidManager()` is called after placing water-adjacent
content.

Also present in the same example: `getGenerationCaveRockObjectChance()`,
`getGenerationDeepCaveRockObjectChance()`, and preset-room hooks
`getNewCaveChestRoomPreset(...)`, `getNewDeepCaveChestRoomPreset(...)`,
`getNewCaveRuinsPreset(...)`, `getNewDeepCaveRuinsPreset(...)` returning
`RandomCaveChestRoom` / `CaveRuins` objects built from
`ChestRoomSet`/`WallSet`/`FurnitureSet` enums and `LootTablePresets`
constants — real, verbatim in the wiki source, but not reproduced in
full here for brevity (see the live `Modding_Snippets` page for the
complete class).

### World Presets — placing hand-built structures (real, verbatim)

From `Modding_Snippets`, "Creating a world preset" (Eryr). Workflow
described: build a structure in Creative Mode, use the in-game "Preset
tool" to export it as an alphanumeric string, then wire it up in code.

```java
public class ExamplePreset extends Preset {
    public ExamplePreset() {
        super("eNrlVc1u3CAQfhUegIPBWa998KnJoadUaaUeqj1gm41pqVlhVlu16ruXGZB..."); // preset-tool-exported string
        this.addMob("pawnbrokerhuman", 4, 4, PawnBrokerHumanMob.class, (pwn) -> {
            pwn.setHome(pwn.getTileX(), pwn.getTileY());
        });
        this.addInventory(LootTablePresets.hunterCookedFoodLootTable, GameRandom.globalRandom, 10, 2, new Object[0]);
    }
}

public class ExampleCabinWorldPreset extends WorldPreset {
    protected Dimension size = new Dimension(13, 9);

    public boolean shouldAddToRegion(LevelPresetsRegion presetsRegion) {
        return presetsRegion.identifier.equals(LevelIdentifier.SURFACE_IDENTIFIER)
                && presetsRegion.hasAnyOfBiome(BiomeRegistry.SNOW.getID());
    }

    public void addToRegion(GameRandom random, LevelPresetsRegion presetsRegion, BiomeGeneratorStack generatorStack, PerformanceTimerManager performanceTimer) {
        int total = getTotalBiomePoints(random, presetsRegion, BiomeRegistry.DESERT, 0.016F);
        for (int i = 0; i < total; i++) {
            Point tile = findRandomBiomePresetTile(random, presetsRegion, generatorStack, BiomeRegistry.SNOW, 50, size,
                    new String[]{"loot", "villages"},
                    (tileX, tileY) -> !generatorStack.isSurfaceExpensiveWater(tileX, tileY)
                            && generatorStack.getLazyBiomeID(tileX, tileY) == BiomeRegistry.SNOW.getID());
            if (tile != null) {
                presetsRegion.addPreset(this, tile.x, tile.y, size, new String[]{"loot", "villages"},
                        (random2, level, timer) -> {
                            WorldPreset.ensureRegionsAreGenerated(level, tile.x, tile.y, 11, 9);
                            ExamplePreset preset = new ExamplePreset();
                            PresetUtils.clearMobsInPreset(preset, level, tile.x, tile.y);
                            preset.applyToLevel(level, tile.x, tile.y);
                        }).setRemoveIfWithinSpawnRegionRange(1);
            }
        }
    }
}

// Registration:
WorldPresetRegistry.registerPreset("example_preset", (WorldPreset) new ExampleCabinWorldPreset());
```
Note the `shouldAddToRegion` example checks for the **Snow** biome but
the code comment above it says "should only spawn ... Snow biome" while
the docstring-style comment in the wiki text mysteriously references
"Desert" for `getTotalBiomePoints`'s biome argument — this looks like a
copy/paste inconsistency in the wiki's own snippet (`BiomeRegistry.DESERT`
appears where `BiomeRegistry.SNOW` would be expected for point density).
**Flagging as UNVERIFIED / possible wiki bug**, do not assume this
inconsistency is intentional — verify against decompiled source before
copying this exact pattern.

---

## 8. Spawn tables & mob spawning per level/biome

Real, verbatim class/usages seen across multiple sources:

- `MobSpawnTable` — a weighted "ticket" table:
  ```java
  public static MobSpawnTable SurfaceMobs = new MobSpawnTable().add(100, "zombie");
  public static MobSpawnTable mobs = new MobSpawnTable().add(100, "examplemob");
  public static MobSpawnTable critters = new MobSpawnTable().include(Biome.defaultCaveCritters);
  ```
  So the two chainable operations actually seen are `.add(int tickets, String mobStringID)`
  and `.include(MobSpawnTable other)` (merges another table's entries in).

- `Biome.getMobSpawnTable(Level)` / `Biome.getCritterSpawnTable(Level)`
  — per-biome overrides, typically branching on `Level.isCave` /
  `Level.getIdentifier()` to return a different static table for
  surface / cave / deep cave (see the full `ExampleBiome` example under
  §2 Biomes).

- Static vanilla default tables that mods can extend, real verbatim
  usage: `Biome.defaultCaveMobs`, `Biome.defaultCaveCritters`,
  `Biome.defaultSurfaceFish`, `Biome.defaultCaveFish`. Example, adding a
  modded mob into the vanilla default cave spawn pool globally
  (`ExampleMod.postInit()`, verbatim):
  ```java
  // Spawn tables use a ticket/weight system. In general, common mobs have about 100 tickets.
  Biome.defaultCaveMobs.add(100, "examplemob");
  ```

- Fishing has its own parallel table type, `FishingLootTable`, with the
  same `.addAll(...)` composition style:
  ```java
  public static FishingLootTable SurfaceFish = new FishingLootTable().addAll(Biome.defaultSurfaceFish);
  ```

- Google Doc mentions a validation hook (UNVERIFIED — paraphrased, exact
  signature not confirmed): *"spawn tables use weighted mob selection
  with fallback validation through `Mob.isValidSpawnLocation(...)`"*.
  No fetched source showed this method's real parameter list or how/when
  it's invoked — needs decompiled-source check.

No fetched source described a **per-level** (as opposed to per-biome)
spawn table override mechanism, nor any explicit spawn-rate/spawn-cap
tuning knobs beyond the ticket weight in `MobSpawnTable.add(...)`.

---

## Packets

Source: Google Doc (paraphrased, UNVERIFIED wording) + GitHub
`ExamplePacket.java` (real, complete, verbatim file).

Google Doc summary: custom packets `extend engine.network.Packet`;
must have a `public CustomPacket(byte[] data)` constructor; override
`processServer(NetworkPacket packet, Server server, ServerClient client)`
and/or `processClient(NetworkPacket packet, Client client)` depending on
transmission direction. `PacketWriter`/`PacketReader` are described as:
*"These iterator classes work like you would expect, it populates at
the current index and adds the current index with the size of the
populated content."* Methods named in the doc: `getNextInt()`,
`getNextBoolean()`, `getNextString()`, `putNextInt()`,
`putNextBoolean()`, `putNextString()`.

Real, complete file, `ExamplePacket.java` (GitHub, verbatim — this is
the entire file except the trailing brace of the class):
```java
public class ExamplePacket extends Packet {

    public final int playerSlot;
    public final int someInteger;
    public final boolean someBoolean;
    public final String someString;
    public final Packet someContent;

    // MUST HAVE - Used for construction in registry
    public ExamplePacket(byte[] data) {
        super(data);
        PacketReader reader = new PacketReader(this);
        // Important that it's same order as written in
        playerSlot = reader.getNextByteUnsigned(); // Since player slots never go over 255
        someInteger = reader.getNextInt();
        someBoolean = reader.getNextBoolean();
        someString = reader.getNextString();
        someContent = reader.getNextContentPacket();
    }

    public ExamplePacket(ServerClient client, int someInteger, boolean someBoolean, String someString, Packet someContent) {
        this.playerSlot = client.slot;
        this.someInteger = someInteger;
        this.someBoolean = someBoolean;
        this.someString = someString;
        this.someContent = someContent;

        PacketWriter writer = new PacketWriter(this);
        // Important that it's same order as read in
        writer.putNextByteUnsigned(playerSlot);
        writer.putNextInt(someInteger);
        writer.putNextBoolean(someBoolean);
        writer.putNextString(someString);
        writer.putNextContentPacket(someContent);

        // Examples how to send packets:
//        client.sendPacket(this); // To a single client
//        server.network.sendToAllClients(packet); // To all clients
    }

    @Override
    public void processClient(NetworkPacket packet, Client client) {
        // Do some stuff with the packet
    }
}
```
This adds real, confirmed method names beyond the Google Doc's list:
`getNextByteUnsigned()` / `putNextByteUnsigned(int)` and
`getNextContentPacket()` / `putNextContentPacket(Packet)` (a packet can
nest another packet as content). Registration (verbatim, `ExampleMod.java`):
```java
PacketRegistry.registerPacket(ExamplePacket.class);
```

---

## Confidence & gaps

Everything below still needs to be checked against the decompiled game
(`Necesse.jar`) before being relied on for real mod code — either
because it came only from a paraphrased fetch of the Google Doc, or
because no fetched source covered it at all:

1. **Custom dimension / new persistent overworld creation.** No fetched
   source has a real worked example of registering a brand-new
   dimension index and generating levels in it. Only the abstract
   `WorldGenerator.getNewLevel(int islandX, int islandY, int dimension, Server server)`
   signature (Google Doc, itself only paraphrased) hints at how
   dimensions/islands are modeled. The actual `WorldGenerator.registerGenerator(...)`
   call was recovered only as "…"-elided prose, not real code — treat
   as a strong hint, not a confirmed API. **This is the single biggest
   gap relative to the user's stated goal of adding a new dimension.**
2. **Ladders / cave-entrance transition mechanics.** Not found in any
   fetched source. Unknown whether this is even mod-hookable.
3. **`LevelIdentifier`'s internal structure** (what it actually
   encodes — dimension index? island coordinates? an opaque ID?) is
   unknown; only its use as an equality-comparable token and two
   constants (`SURFACE_IDENTIFIER`, `DEEP_CAVE_IDENTIFIER`) were
   observed.
4. **Mob sprite sheet direction/frame layout convention** — confirmed
   pixel sizes (384×320 sheet, 64×64 frame cells) but not the
   row/column-to-direction/animation-frame mapping; that logic lives in
   a `getAnimSprite(...)` helper whose implementation wasn't visible in
   any fetched source.
5. **Meaning of several numeric/boolean trailing parameters** in
   registration calls — e.g. the `10`/`20`/`30`/`1`/`2` integers passed
   to `ItemRegistry.registerItem`, `TileRegistry.registerTile`,
   `ObjectRegistry.registerObject`; the tier integer `1` in
   `IncursionBiomeRegistry.registerBiome(id, biome, 1)`. None of these
   were explained in prose anywhere fetched.
6. **Sound asset registration** — no evidence mods can add wholly new
   sound effects (only reuse of built-in `GameResources.*` sounds was
   observed); the example mod ships no `resources/sounds/` folder.
7. **`recipes.cfg` exact syntax** and **`Mob.isValidSpawnLocation(...)`
   signature** — both only seen as Google-Doc paraphrase, not verbatim
   quotes or real code.
8. **Wiki snippet vs. current API version drift.** The wiki
   `Modding_Snippets` page's `SwordToolItem` usage
   (`super(400); rarity = Item.Rarity.COMMON;`) does not match the
   current GitHub example mod (`super(400, null); rarity = Item.Rarity.UNCOMMON;`,
   targeting game version 1.3.2). Any wiki snippet's exact method
   signatures should be re-checked against the currently installed game
   version before use; prefer the GitHub example project's code when the
   two disagree.
9. **Possible bug/inconsistency in the wiki's own "world preset" example**
   — see the `BiomeRegistry.DESERT` vs `BiomeRegistry.SNOW` mismatch
   noted in §7. Don't copy that line verbatim without checking.
10. **`Item.Rarity` full enum value list** — only `COMMON`, `UNCOMMON`,
    `RARE` were observed; higher tiers (if any) are unconfirmed.
11. **Full `PacketWriter`/`PacketReader` method surface** — only the
    handful of methods actually used in `ExamplePacket.java` and named
    in the Google Doc summary are confirmed; the complete list of
    supported data types is unknown.
12. **Biome-level `resources/biomes/<stringID>` texture purpose** — the
    wiki claims this path exists, but no shipped example asset or usage
    of it was found to confirm what it's for.

---

## Sources

Primary (wiki):
- https://necessewiki.com/Modding — full page content fetched
- https://necessewiki.com/Modding_Snippets — full page content fetched (community examples: "Creating a new biome" and "Creating a world preset" by Eryr; "Example Shopkeeper" by Eryr; utility snippets by Kamikaze, kiriharu, DimitarBogdanov/m4trixglitch, FerrenF)

Checked and confirmed **not to exist** (HTTP 404), disproving the
hypothesized subpage structure:
- https://necessewiki.com/Mod_Creation
- https://necessewiki.com/Modding/Items
- https://necessewiki.com/Modding/Getting_Started
- https://necessewiki.com/Modding/Levels

Wiki housekeeping pages checked while mapping the site (no modding
content found):
- https://necessewiki.com/Special:AllPages (used to confirm the wiki is a general game-content wiki, not modding-API-structured)
- https://necessewiki.com/Special:Search?search=LevelIdentifier&fulltext=1&ns0=1 (zero results)

Supplementary — linked directly from the `Modding` wiki page:
- https://docs.google.com/document/u/1/d/e/2PACX-1vTexy0ZwJmztm6KhvwUCpSbgdNFV5hxUOr_6rSiCyqvjlj80Sj28Alenodq6AbOfnKaWoj-zv0iziyL/pub — "Necesse Modding Guide" (legacy doc, wiki says it's being migrated in; fetched in 6 topic-focused passes: items, tiles/objects, mobs/projectiles/buffs, levels/dimensions/world-gen, recipes/loot/spawn, packets/mod.info/resources)
- https://github.com/DrFair/ExampleMod — official example mod repository (targets game version 1.3.2 per its build.gradle)

GitHub files fetched directly (raw content, and one API tree listing):
- https://api.github.com/repos/DrFair/ExampleMod/git/trees/master?recursive=1 (full file listing)
- https://raw.githubusercontent.com/DrFair/ExampleMod/master/build.gradle
- https://raw.githubusercontent.com/DrFair/ExampleMod/master/src/main/java/examplemod/ExampleMod.java
- https://raw.githubusercontent.com/DrFair/ExampleMod/master/src/main/java/examplemod/examples/ExampleIncursionLevel.java
- https://raw.githubusercontent.com/DrFair/ExampleMod/master/src/main/java/examplemod/examples/ExampleTile.java
- https://raw.githubusercontent.com/DrFair/ExampleMod/master/src/main/java/examplemod/examples/ExampleObject.java
- https://raw.githubusercontent.com/DrFair/ExampleMod/master/src/main/java/examplemod/examples/ExampleMob.java
- https://raw.githubusercontent.com/DrFair/ExampleMod/master/src/main/java/examplemod/examples/ExampleBiome.java
- https://raw.githubusercontent.com/DrFair/ExampleMod/master/src/main/java/examplemod/examples/ExampleIncursionBiome.java
- https://raw.githubusercontent.com/DrFair/ExampleMod/master/src/main/java/examplemod/examples/ExampleSwordItem.java
- https://raw.githubusercontent.com/DrFair/ExampleMod/master/src/main/java/examplemod/examples/ExamplePacket.java
- https://raw.githubusercontent.com/DrFair/ExampleMod/master/src/main/java/examplemod/examples/ExampleBuff.java
- https://raw.githubusercontent.com/DrFair/ExampleMod/master/src/main/java/examplemod/examples/ExampleProjectile.java
- https://raw.githubusercontent.com/DrFair/ExampleMod/master/src/main/java/examplemod/examples/ExampleProjectileWeapon.java
- https://raw.githubusercontent.com/DrFair/ExampleMod/master/src/main/java/examplemod/examples/items/ExampleMaterialItem.java
- https://raw.githubusercontent.com/DrFair/ExampleMod/master/src/main/resources/locale/en.lang
- https://raw.githubusercontent.com/DrFair/ExampleMod/master/src/main/resources/items/exampleitem.png (and sibling `.png` assets: `items/examplesword.png`, `tiles/exampletile.png`, `objects/exampleobject.png`, `mobs/examplemob.png`, `buffs/examplebuff.png`, `projectiles/exampleprojectile.png`, `player/weapons/examplesword.png`, `player/weapons/examplestaff.png`) — downloaded and measured locally with the `file` command for exact pixel dimensions

Also consulted, no new modding-specific information found:
- https://github.com/DrFair/ExampleMod (repo root page, HTML view)
- https://github.com/DrFair/ExampleMod/tree/master/src/main (HTML view — superseded by the API tree listing above)
