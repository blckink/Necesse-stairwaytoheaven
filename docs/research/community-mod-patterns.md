# Necesse Community Mod Patterns — Research Notes

**Purpose:** concrete, verified code patterns for custom levels/dimensions, ladder/teleport
objects, tiles, objects, mobs, items/recipes/loot, biomes, and buildscript conventions,
pulled from real open-source Necesse mods on GitHub.

**Method:** GitHub code search (`mcp__github__search_code`) plus direct `raw.githubusercontent.com`
fetches of the matched files. Every excerpt below was fetched in this session; none is
reconstructed from memory. Where a fetch failed or a pattern could not be located, that is
stated explicitly instead of being filled in.

**A note on sources.** Two kinds of repositories turned up:

1. **Genuine mods** — real `@ModEntry` projects published by their authors specifically to
   ship or share their work (`DrFair/ExampleMod`, `768gareth/necesse-expanded`,
   `greatcoltini/NecessePlus`, `KingEnder04/vulpes-nova`, `AizSave/AphoreaMod`,
   `Azuraei/Soul-Chasm`, `AldiandyaIrsyad/Necesse-AoA`, etc.). Excerpts from these are quoted
   fairly fully below, since that's exactly the "real mod code" this research asked for.
2. **`pyralisxc/NECESSE_MODDING_RESOURCES`** — a community repo of the *decompiled vanilla
   game engine* (not a mod), organized for modders to browse `necesse.*` base classes. This
   project's own template (`Necesse-ModTemplate`, which `stairwaytoheaven` is built from)
   states explicitly: *"Distributing decompiled sources is not allowed."* So vanilla-engine
   excerpts below (`Level`, `PortalObjectEntity`, `LevelRegistry`, `BiomeRegistry`, ladder
   classes, etc.) are kept intentionally short — just the method signatures/contracts needed
   to explain the mechanism — rather than reproduced in full, even though longer versions
   were fetched and read during research.

---

## ModEntry lifecycle (context for everything below)

Every mod's `@ModEntry` class implements some subset of these methods, called by the mod
loader in this order. This is the skeleton all the registrations in this doc hang off of.

**Source:** [`768gareth/necesse-expanded` → `src/main/java/NecesseExpanded/Main.java`](https://github.com/768gareth/necesse-expanded/blob/master/src/main/java/NecesseExpanded/Main.java)

```java
@ModEntry
public class Main
{
    public void preInit()
    {
        PreRegisterMobs.Register();
        PreRegisterItems.Register();
    }

    public void init()
    {
        RegisterItems.Register();
        RegisterMobs.Register();
        RegisterObjects.Register();
        RegisterEvents.Register();
        RegisterBuffs.Register();
        RegisterTiles.Register();
        RegisterProjectiles.Register();
        RegisterExpeditions.Register();
        RegisterRecipeTechs.Register();
        RegisterBiomes.Register();
        RegisterLevels.Register();
        RegisterWorldPresets.Register();
        RegisterJournal.Register();
    }

    public void initResources()
    {
        IceGolemMob.texture = GameTexture.fromFile("mobs/ice_golem");
        WolfMob.Texture = new MobTexture(GameTexture.fromFile("mobs/wolf"), GameTexture.fromFile("mobs/wolfshadow"));
    }

    public void postInit()
    {
        UpdateHappinessMetrics.Update();
        UpdateLootTables.Update();
        UpdateSpawnTables.Update();
        UpdateAmmoTypes.Update();
        RegisterRecipes.Register();
    }

    public ModSettings initSettings() { /* ... */ }
}
```

`DrFair/ExampleMod` and `greatcoltini/NecessePlus` both use the same `init()` →
`initResources()` → `postInit()` split but inline the registration calls directly in
`@ModEntry` rather than farming them out to per-concern classes; `necesse-expanded` shows
the "split into `RegisterX`/`UpdateX` static helper classes called from `Main`" style, which
scales better once a mod has dozens of registrations. Textures are always loaded in
`initResources()` (after registries are open but before world/UI code needs them), and
recipes/spawn-table edits that reference *other mods' or vanilla's* content go in
`postInit()` (after every mod has finished `init()`, so cross-mod IDs like `"ironbar"` or
`"woodboat"` are guaranteed to already be registered).

---

## 1. Custom Level / Dimension

### 1.1 The `Level` subclass contract, enforced by `LevelRegistry`

**Source:** [`pyralisxc/NECESSE_MODDING_RESOURCES` → `readable_source_complete/necesse/engine/registries/LevelRegistry.java`](https://github.com/pyralisxc/NECESSE_MODDING_RESOURCES/blob/master/readable_source_complete/necesse/engine/registries/LevelRegistry.java)
(decompiled vanilla engine class — excerpt kept minimal, see note above)

```java
public static int registerLevel(String stringID, Class<? extends Level> levelClass) {
    if (LoadedMod.isRunningModClientSide()) {
        throw new IllegalStateException("Client/server only mods cannot register levels");
    }
    try {
        return instance.register(stringID, new LevelRegistryElement(levelClass));
    }
    catch (NoSuchMethodException e) {
        System.err.println("Could not register level " + levelClass.getSimpleName()
            + ": Missing constructor with parameters: LevelIdentifier, int (width), int (height), WorldEntity");
        return -1;
    }
}

public static Level getNewLevel(int id, LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
    return (Level) ((LevelRegistryElement) instance.getElement(id))
        .newInstance(identifier, width, height, worldEntity);
}
```

`registerLevel` reflectively demands a `(LevelIdentifier, int width, int height, WorldEntity)`
constructor on every registered class (`LevelRegistryElement` wraps `ClassIDDataContainer`,
which looks that constructor up via reflection) — **this is the "how does the server
instantiate it" hook**: whenever the server needs to load or create a level with your
registered string ID, it reflectively calls exactly that constructor. If it's missing, your
mod prints a warning at load time and the level silently fails to register (id `-1`), so this
constructor is not optional even if your mod never calls it directly.

### 1.2 A Level subclass with the two-constructor idiom

**Source:** [`DrFair/ExampleMod` → `src/main/java/examplemod/examples/ExampleIncursionLevel.java`](https://github.com/DrFair/ExampleMod/blob/master/src/main/java/examplemod/examples/ExampleIncursionLevel.java)

```java
public class ExampleIncursionLevel extends IncursionLevel {

    /**
     * A constructor with this signature (LevelIdentifier, int, int, WorldEntity) is required and is used for loading, etc.
     */
    public ExampleIncursionLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
        super(identifier, width, height, worldEntity);
        this.baseBiome = ExampleMod.EXAMPLE_BIOME;
        this.isCave = true;
    }

    /**
     * Constructor used when an incursion is generated and entered.
     * Creates a fixed-size level and immediately generates its contents.
     */
    public ExampleIncursionLevel(LevelIdentifier identifier, BiomeMissionIncursionData incursionData, WorldEntity worldEntity, AltarData altarData) {
        super(identifier, 150, 150, incursionData, worldEntity);
        this.baseBiome = ExampleMod.EXAMPLE_BIOME;
        this.isCave = true;
        generateLevel(incursionData, altarData);
    }

    public void generateLevel(BiomeMissionIncursionData incursionData, AltarData altarData) {
        CaveGeneration cg = new CaveGeneration(this, "deeprocktile", "deeprock");
        cg.random.setSeed(incursionData.getUniqueID());
        GameEvents.triggerEvent(new GenerateCaveLayoutEvent(this, cg), e -> {
            cg.generateLevel(0.38F, 4, 3, 6);
        });
        // ... entrance + preset generation, ore veins, GenerationTools.checkValid(this) ...
    }
}
```

This is the canonical pattern across every mod fetched: **one constructor matching the
`LevelRegistry` contract exactly** (used for loading an existing level off disk, or for
reflective creation), and **a second, mod-specific convenience constructor** that the mod's
own code calls when actually spawning a brand-new instance of the level, which runs
`generateLevel(...)` immediately. `necesseplus.biomes.corruption.CorruptionSurfaceLevel` and
`vulpesnova...FlatlandsSurfaceLevelVN` (below) repeat the exact same two-constructor shape.

Registered with (from `DrFair/ExampleMod` → `src/main/java/examplemod/ExampleMod.java`):
```java
LevelRegistry.registerLevel("exampleincursionlevel", ExampleIncursionLevel.class);
```

### 1.3 A biome-owned "surface island" Level

**Source:** [`greatcoltini/NecessePlus` → `src/main/java/necesseplus/biomes/corruption/CorruptionSurfaceLevel.java`](https://github.com/greatcoltini/NecessePlus/blob/master/src/main/java/necesseplus/biomes/corruption/CorruptionSurfaceLevel.java)

```java
public class CorruptionSurfaceLevel extends Level {
  public CorruptionSurfaceLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
    super(identifier, width, height, worldEntity);
  }

  public CorruptionSurfaceLevel(int islandX, int islandY, float islandSize, WorldEntity worldEntity) {
    super(new LevelIdentifier(islandX, islandY, 0), 300, 300, worldEntity);
    generateLevel(islandSize);
  }

  public void generateLevel(float islandSize) {
    int size = (int)(islandSize * 100.0F) + 100;
    IslandGeneration ig = new IslandGeneration(this, size);
    int waterTile = TileRegistry.getTileID("watertile");
    int sandTile = TileRegistry.getTileID("corruptsandtile");
    int grassTile = TileRegistry.getTileID("corruptgrasstile");
    GameEvents.triggerEvent(new GenerateIslandLayoutEvent(this, islandSize, ig), e -> {
          ig.generateSimpleIsland(this.width / 2, this.height / 2, waterTile, grassTile, sandTile);
          ig.generateRiver(waterTile, grassTile, sandTile);
          ig.generateLakes(0.01F, waterTile, grassTile, sandTile);
          this.liquidManager.calculateHeights();
        });
    // ... flora, structures, GenerationTools.checkValid(this) ...
  }
}
```

Note this extends `Level` directly (not a vanilla `CaveLevel`/`SurfaceLevel` subtype) and
uses `IslandGeneration` instead of `CaveGeneration` — this is the "overworld island" shape
of world gen (vanilla surface biomes are procedurally-placed islands), as opposed to
`ExampleIncursionLevel`'s fixed-size cave-room shape. The `(islandX, islandY, islandSize,
WorldEntity)` convenience constructor is called from the owning `Biome` (see §6.2).

### 1.4 Subclassing an existing vanilla `Level` subtype

**Source:** [`AizSave/AphoreaMod` → `src/main/java/aphorea/levels/InfectedTrialRoomLevel.java`](https://github.com/AizSave/AphoreaMod/blob/master/src/main/java/aphorea/levels/InfectedTrialRoomLevel.java)

```java
public class InfectedTrialRoomLevel extends TrialRoomLevel {
    public InfectedTrialRoomLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
        super(identifier, width, height, worldEntity);
    }

    public InfectedTrialRoomLevel(LevelIdentifier levelIdentifier, WorldEntity worldEntity) {
        super(levelIdentifier, worldEntity);
    }

    int presentPlayersAnt = 0;

    @Override
    public void serverTick() {
        super.serverTick();
        if (presentPlayersAnt != presentPlayers) {
            if (presentPlayersAnt == 0) {
                // spawn 3 "infectedtreant" mobs when the room becomes occupied
                entityManager.addMob(MobRegistry.getMob("infectedtreant", this), tileX * 32 + 16, tileY * 32 + 16);
            } else if (presentPlayers == 0) {
                for (Mob mob : entityManager.mobs) { if (mob.isHostile) mob.remove(); }
            }
            presentPlayersAnt = presentPlayers;
        }
    }

    @Override
    public Stream<ModifierValue<?>> getMobModifiers(Mob mob) {
        return Stream.concat(super.getMobModifiers(mob),
                Stream.of(new ModifierValue<>(BuffModifiers.BLINDNESS, 0.6F)));
    }
}
```
Registered with (`AizSave/AphoreaMod` → `src/main/java/aphorea/registry/AphLevels.java`):
```java
public class AphLevels {
    public static void registerCore() {
        LevelRegistry.registerLevel("infectedtrialroom", InfectedTrialRoomLevel.class);
    }
}
```

This is the cheapest way to add a "variant" level: inherit an entire vanilla level type's
generation/behavior for free and only override the hooks you care about (here,
`serverTick()` for a room-occupancy trigger and `getMobModifiers()` to blind mobs inside).

### 1.5 How a mod-created level's runtime state survives save/load

**Source:** [`768gareth/necesse-expanded` → `src/main/java/NecesseExpanded/World/Levels/FishianBiomeLevel.java`](https://github.com/768gareth/necesse-expanded/blob/master/src/main/java/NecesseExpanded/World/Levels/FishianBiomeLevel.java)

```java
public class FishianBiomeLevel extends Level {
   public FishianBiomeLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
      super(identifier, width, height, worldEntity);
    }

    public FishianBiomeLevel(LevelIdentifier identifier, WorldEntity worldEntity) {
      super(identifier, 200, 200, worldEntity);
      this.baseBiome = BiomeRegistry.getBiome("fishian_biome");
      this.isCave = true;
      generateLevel();
    }

   public void onLoadingComplete() {
      super.onLoadingComplete();
      this.baseBiome = BiomeRegistry.getBiome("fishian_biome");
   }
   // ...
}
```

This is the concrete answer to "how does a mod-created level get loaded": the
`(LevelIdentifier, width, height, WorldEntity)` constructor used by `LevelRegistry`'s
reflective loading path does **not** set `baseBiome` — only the convenience "new level"
constructor does. So on every actual disk load, `onLoadingComplete()` runs and re-derives
`baseBiome` from the registry by string ID. The lesson generalizes: any field your custom
`Level`/`ObjectEntity` sets in its "just generated" constructor but that Necesse's own save
system doesn't serialize must be re-derived in a load hook (`onLoadingComplete()` for
`Level`; `applyLoadData()` for `ObjectEntity`/save-data-bearing classes, see §2.1) rather
than assumed to persist automatically. Terrain/object/tile contents themselves *are*
persisted by the engine's own region/save system — it's mod-added transient Java fields
that need this treatment.

### 1.6 Registering multiple custom levels

**Source:** [`greatcoltini/NecessePlus` → `src/main/java/necesseplus/NecessePlus.java`](https://github.com/greatcoltini/NecessePlus/blob/master/src/main/java/necesseplus/NecessePlus.java)
```java
// 11 register level generators
LevelRegistry.registerLevel("corruptionsurface", CorruptionSurfaceLevel.class);
LevelRegistry.registerLevel("corruptioncave", CorruptionCaveLevel.class);
LevelRegistry.registerLevel("corruptiondeepcave", CorruptionDeepCaveLevel.class);
```

**Source:** [`768gareth/necesse-expanded` → `src/main/java/NecesseExpanded/Registry/RegisterLevels.java`](https://github.com/768gareth/necesse-expanded/blob/master/src/main/java/NecesseExpanded/Registry/RegisterLevels.java)
```java
public class RegisterLevels
{
    public static void Register()
    {
        LevelRegistry.registerLevel("fishian_dungeon", FishianBiomeLevel.class);
    }
}
```

Every mod fetched registers levels the same way — one `LevelRegistry.registerLevel(stringID,
Class)` call per level class, always in `init()` (never `preInit()`/`postInit()`), and always
alongside the `Biome` that owns them (see §6).

---

## 2. Ladder-like objects / teleporting the player between levels

### 2.1 The vanilla base class: `PortalObjectEntity`

**Source:** [`pyralisxc/NECESSE_MODDING_RESOURCES` → `readable_source_complete/necesse/entity/objectEntity/PortalObjectEntity.java`](https://github.com/pyralisxc/NECESSE_MODDING_RESOURCES/blob/master/readable_source_complete/necesse/entity/objectEntity/PortalObjectEntity.java)
(decompiled vanilla engine class — excerpt kept minimal)

```java
public class PortalObjectEntity extends ObjectEntity {
    public LevelIdentifier destinationIdentifier;
    public int destinationTileX;
    public int destinationTileY;

    public PortalObjectEntity(Level level, String type, int x, int y,
                               LevelIdentifier destinationIdentifier, int destinationTileX, int destinationTileY) {
        super(level, "portal." + type, x, y);
        this.destinationIdentifier = destinationIdentifier;
        this.destinationTileX = destinationTileX;
        this.destinationTileY = destinationTileY;
    }

    // Simplest possible teleport: jump straight to the stored destination.
    public void use(Server server, ServerClient client) {
        client.changeLevel(this.getDestinationIdentifier(),
            level -> new Point(this.getDestinationX(), this.getDestinationY()), true);
    }

    // Safer teleport used by ladders: waits for the destination level to be generated/loaded,
    // re-validates a predicate against it, then finds a free tile around the target point.
    protected void teleportClientToAroundDestination(ServerClient client, Function<LevelIdentifier, Level> generator,
                                                       Predicate<Level> validCheck, boolean mountFollow) {
        client.changeLevelCheck(this.getDestinationIdentifier(), generator, level -> {
            if (validCheck != null && !validCheck.test(level)) return new TeleportResult(false, null);
            Point teleportPos = PortalObjectEntity.getTeleportDestinationAroundObject(
                level, client.playerMob, this.destinationTileX, this.destinationTileY, true);
            return new TeleportResult(true, teleportPos != null ? teleportPos
                : new Point(this.destinationTileX * 32 + 16, this.destinationTileY * 32 + 16));
        }, mountFollow);
    }
}
```

`destinationIdentifier`/`destinationTileX`/`destinationTileY` are persisted via
`addSaveData`/`applyLoadData` (an `ObjectEntity` save-data pair, same idiom as §1.5) so a
placed portal remembers its target across a save/reload. Every teleport in the game funnels
through `ServerClient.changeLevel(...)` (fire-and-forget) or `.changeLevelCheck(...)`
(generator + validity predicate, used when the destination level might not exist yet).

### 2.2 A complete, real mod-authored teleport object (GameObject + ObjectEntity pair)

**Source:** [`768gareth/necesse-expanded` → `src/main/java/NecesseExpanded/Objects/FishianDeepCaveEntranceObject.java`](https://github.com/768gareth/necesse-expanded/blob/master/src/main/java/NecesseExpanded/Objects/FishianDeepCaveEntranceObject.java)

```java
public class FishianDeepCaveEntranceObject extends GameObject {
  public GameTexture texture;

  public FishianDeepCaveEntranceObject() {
    super(new Rectangle(32, 32));
    this.toolType = ToolType.UNBREAKABLE;
    this.isLightTransparent = true;
  }

  public void loadTextures() {
    super.loadTextures();
    this.texture = GameTexture.fromFile("objects/dungeonentrance");
  }

  public boolean canInteract(Level level, int x, int y, PlayerMob player) { return true; }

  public void interact(Level level, int x, int y, PlayerMob player) {
    if (level.isServer() && player.isServerClient()) {
      ObjectEntity objectEntity = level.entityManager.getObjectEntity(x, y);
      if (objectEntity instanceof PortalObjectEntity)
        ((PortalObjectEntity)objectEntity).use(level.getServer(), player.getServerClient());
    }
    super.interact(level, x, y, player);
  }

  public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
    return new FishianDeepCaveEntranceObjectEntity(level, x, y);
  }
}
```

**Source:** [`768gareth/necesse-expanded` → `src/main/java/NecesseExpanded/Objects/FishianDeepCaveEntranceObjectEntity.java`](https://github.com/768gareth/necesse-expanded/blob/master/src/main/java/NecesseExpanded/Objects/FishianDeepCaveEntranceObjectEntity.java)

```java
public class FishianDeepCaveEntranceObjectEntity extends PortalObjectEntity {

    public FishianDeepCaveEntranceObjectEntity(Level level, int x, int y) {
        super(level, "fishian_dungeon_entrance", x, y, level.getIdentifier(), x, y);
    }

    public void init() {
        super.init();
        this.destinationTileX = Integer.MIN_VALUE;
        this.destinationTileY = Integer.MIN_VALUE;
    }

    public void use(Server server, ServerClient client)
    {
        client.setFallbackLevel(getLevel(), this.tileX, this.tileY);
        LevelIdentifier currentIdentifier = getLevel().getIdentifier();
        this.destinationIdentifier = new LevelIdentifier(currentIdentifier.stringID + "-fishiandungeon" + this.tileX + "x" + this.tileY);

        Function<LevelIdentifier, Level> levelGenerator = identifier ->
        {
            (getLevel()).childLevels.add(this.destinationIdentifier);
            return new FishianBiomeLevel(identifier, server.world.worldEntity);
        };
        teleportClientToAroundDestination(client, levelGenerator, level ->
        {
            if (this.destinationTileX != Integer.MIN_VALUE && this.destinationTileY != Integer.MIN_VALUE) {
                return true;
            }
            // first entry: scan the freshly generated level for its "dungeonexit" object,
            // remember that tile as this portal's landing spot, and wire the exit object's
            // own PortalObjectEntity to point back at this tile.
            int exitID = ObjectRegistry.getObjectID("dungeonexit");
            // ... scan level for exitID, place one if missing ...
            PortalObjectEntity exitEntity = (PortalObjectEntity) level.entityManager
                .getObjectEntity(this.destinationTileX, this.destinationTileY, PortalObjectEntity.class);
            if (exitEntity != null) {
                exitEntity.destinationTileX = this.tileX;
                exitEntity.destinationTileY = this.tileY;
                exitEntity.destinationIdentifier = currentIdentifier;
            }
            runClearMobs(level, this.destinationTileX, this.destinationTileY);
            return true;
        }, true);
    }
}
```

This is the fullest real-world example found: `LevelIdentifier`s for mod-generated
sub-levels are built by string-concatenating the parent level's `stringID` with the tile
coordinates (`currentIdentifier.stringID + "-fishiandungeon" + tileX + "x" + tileY`), so
each placed entrance gets its own unique, deterministic child level. The `Function<LevelIdentifier,
Level>` passed to `teleportClientToAroundDestination` is the "create if it doesn't exist yet"
generator — it's only invoked the first time a player uses that specific entrance, at which
point it constructs the mod's `Level` subclass directly (§1.5) and registers it as a
`childLevels` entry on the parent (for save/cleanup bookkeeping). `client.setFallbackLevel(...)`
is called first so the player has somewhere safe to respawn if the destination level ever
fails to load. `GameObject.interact()` looking up the tile's `ObjectEntity`, checking
`instanceof PortalObjectEntity`, and calling `.use(server, client)` is the exact same wiring
vanilla's own `LadderUpObject.interact()` uses (§2.3) — it's the idiomatic way any
interactive object delegates into its entity's teleport logic.

### 2.3 Vanilla's own ladder pair (for comparison / direct reuse)

**Source:** [`pyralisxc/NECESSE_MODDING_RESOURCES` → `readable_source_complete/necesse/entity/objectEntity/LadderUpObjectEntity.java`](https://github.com/pyralisxc/NECESSE_MODDING_RESOURCES/blob/master/readable_source_complete/necesse/entity/objectEntity/LadderUpObjectEntity.java)
and `LadderDownObjectEntity.java` (decompiled vanilla — excerpts kept minimal)

```java
public class LadderUpObjectEntity extends PortalObjectEntity {
    private final int ladderDownID;

    public LadderUpObjectEntity(String type, Level level, int x, int y, LevelIdentifier destination, int ladderDownID, GameSprite mapSprite) {
        super(level, type, x, y, destination, x, y);
        this.ladderDownID = ladderDownID;
    }

    @Override
    public void use(Server server, ServerClient client) {
        // if the far side doesn't already have a matching ladder-down object, place one there first
        this.teleportClientToAroundDestination(client, level -> {
            if (level.getObjectID(this.destinationTileX, this.destinationTileY) != this.ladderDownID) {
                LadderDownObjectEntity.clearAndPlaceLadder(server, level, this.destinationTileX, this.destinationTileY, this.ladderDownID, true);
            }
            client.newStats.ladders_used.increment(1);
            this.runClearMobs(level, this.destinationTileX, this.destinationTileY);
            return true;
        }, true);
    }
}
```

`LadderDownObjectEntity` is structurally the mirror image (swap `ladderDownID` for
`ladderUpID`). Both extend `PortalObjectEntity` and add exactly one behavior on top: lazily
placing the matching ladder object on the far side the first time it's used, and clearing a
small radius of hostile mobs on arrival (`runClearMobs`, inherited from
`PortalObjectEntity`). The `GameObject` side (`LadderUpObject`, in the same package) wires
`interact()` the identical way shown in §2.2, and its `getNewObjectEntity()` hard-codes the
surface as the up-ladder's destination via the constant `LevelIdentifier.SURFACE_IDENTIFIER`.

### 2.4 The low-level `changeLevel` API directly (no portal object involved)

**Source:** [`Shuazijun/TPA-Commands` → `src/main/java/tpamod/commands/WarpCommand.java`](https://github.com/Shuazijun/TPA-Commands/blob/master/src/main/java/tpamod/commands/WarpCommand.java)
```java
// 使用changeLevel API切换关卡并直接传送到目标位置  (= "use the changeLevel API to switch level and teleport directly to target position")
serverClient.changeLevel(targetLevelIdentifier, level -> {
    return new Point(targetX, targetY);
});
```

**Source:** [`EliasVahlberg/necesse-headless-harness` → `src/main/java/necesseheadlessharness/HeadlessPlayer.java`](https://github.com/EliasVahlberg/necesse-headless-harness/blob/master/src/main/java/necesseheadlessharness/HeadlessPlayer.java)
```java
// Nothing is going to answer here, so do that part directly.
spawned.changeLevel(level.getIdentifier());
```

For chat commands, admin tools, or any code that isn't itself an interactable object,
`ServerClient.changeLevel(LevelIdentifier, Function<Level,Point> posGetter, boolean...)` is
called directly — no `ObjectEntity`/`PortalObjectEntity` subclass required at all. This is
the right tool when "teleport" is triggered by a command or event rather than a placed
world object.

---

## 3. Custom tiles (`TileRegistry`) and objects (`ObjectRegistry`) with textures

### 3.1 A terrain tile

**Source:** [`DrFair/ExampleMod` → `src/main/java/examplemod/examples/ExampleTile.java`](https://github.com/DrFair/ExampleMod/blob/master/src/main/java/examplemod/examples/ExampleTile.java)
```java
public class ExampleTile extends TerrainSplatterTile {

    private GameTexture texture;
    private final GameRandom drawRandom;

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
        // Runs asynchronously — synchronize the random call if you want deterministic-per-tile variation
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
Registered with: `TileRegistry.registerTile("exampletile", new ExampleTile(), 1, true);`
(`ExampleMod.java`). Texture loads from `resources/tiles/exampletile.png` — the string
passed to `GameTexture.fromFile` is `"<category-folder>/<stringID>"`.

### 3.2 A non-entity placed object

**Source:** [`DrFair/ExampleMod` → `src/main/java/examplemod/examples/ExampleObject.java`](https://github.com/DrFair/ExampleMod/blob/master/src/main/java/examplemod/examples/ExampleObject.java)
```java
public class ExampleObject extends GameObject {

    private GameTexture texture;

    public ExampleObject() {
        super(new Rectangle(4, 4, 26, 26)); // collision relative to the tile it's placed on
        hoverHitbox = new Rectangle(0, -32, 32, 64); // 2 tiles high mouse hover hitbox
        toolType = ToolType.ALL;
        isLightTransparent = true;
        mapColor = new Color(31, 150, 148);
    }

    @Override
    public void loadTextures() {
        super.loadTextures();
        texture = GameTexture.fromFile("objects/exampleobject");
    }

    @Override
    public void addDrawables(List<LevelSortedDrawable> list, OrderableDrawables tileList, Level level,
                              int tileX, int tileY, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        GameLight light = level.getLightLevel(tileX, tileY);
        int drawX = camera.getTileDrawX(tileX);
        int drawY = camera.getTileDrawY(tileY);
        TextureDrawOptions options = texture.initDraw().light(light).pos(drawX, drawY - texture.getHeight() + 32);
        list.add(new LevelSortedDrawable(this, tileX, tileY) {
            @Override public int getSortY() { return 16; } // sort position on the Y axis, range [0,32]
            @Override public void draw(TickManager tickManager) { options.draw(); }
        });
    }

    @Override
    public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
        return null; // no entity needed for a static decorative object
    }
}
```
Registered with: `ObjectRegistry.registerObject("exampleobject", new ExampleObject(), 2, true);`.
Texture loads from `resources/objects/exampleobject.png`. Returning `null` from
`getNewObjectEntity` is the explicit "this object has no entity/state" case; §2.2 and §2.3
show what returning a real `ObjectEntity` subclass looks like instead.

### 3.3 Registration call-shape caveat

Two different `TileRegistry.registerTile` call shapes were observed:

```java
// DrFair/ExampleMod: int as 3rd argument
TileRegistry.registerTile("exampletile", new ExampleTile(), 1, true);

// greatcoltini/NecessePlus: float as 3rd argument
TileRegistry.registerTile("corruptgrasstile", (GameTile) new CorruptGrassTile(), 0.0F, false);
```
The vanilla `TileRegistry` source itself was not fetched (only the registry classes for
`Level`/`Biome` were), so the exact overload semantics of that 3rd argument are **not
confirmed** here — flagged under Gaps rather than guessed at.

---

## 4. Custom mobs (`MobRegistry`)

### 4.1 A `HostileMob` with a behaviour-tree AI

**Source:** [`DrFair/ExampleMod` → `src/main/java/examplemod/examples/ExampleMob.java`](https://github.com/DrFair/ExampleMod/blob/master/src/main/java/examplemod/examples/ExampleMob.java)
```java
public class ExampleMob extends HostileMob {

    public static GameTexture texture; // loaded in ExampleMod.initResources()

    public static LootTable lootTable = new LootTable(
            ChanceLootItem.between(0.5f, "exampleitem", 1, 3) // 50% chance to drop 1-3 example items
    );

    // MUST HAVE an empty constructor
    public ExampleMob() {
        super(200); // max health
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
            getLevel().entityManager.addParticle(new FleshParticle(getLevel(), texture,
                GameRandom.globalRandom.nextInt(5), 8, 32, x, y, 20f, knockbackX, knockbackY),
                Particle.GType.IMPORTANT_COSMETIC);
        }
    }

    @Override
    public int getRockSpeed() { return 20; } // animation playback speed
}
```
Registered with (`ExampleMod.java`): `MobRegistry.registerMob("examplemob", ExampleMob.class, true);`
and texture loaded in `initResources()`: `ExampleMob.texture = GameTexture.fromFile("mobs/examplemob");`

### 4.2 A flying-variant hostile mob (real third-party mod)

**Source:** [`greatcoltini/NecessePlus` → `src/main/java/necesseplus/mob/hostile/Eater.java`](https://github.com/greatcoltini/NecessePlus/blob/master/src/main/java/necesseplus/mob/hostile/Eater.java)
```java
public class Eater extends FlyingHostileMob {

    public static GameTexture texture;
    public static LootTable lootTable = new LootTable(ChanceLootItem.between(0.5f, "coin", 1, 3));

    // MUST HAVE an empty constructor
    public Eater() {
        super(200);
        setSpeed(15);
        setFriction(3);
        collision = new Rectangle(-20, -12, 64, 94);
        hitBox = new Rectangle(-24, -16, 68, 104);
        selectBox = new Rectangle(-24, -12 - 34, 68, 98);
    }

    @Override
    public void init() {
        super.init();
        ai = new BehaviourTreeAI<>(this, new CollisionPlayerChaserWandererAI<>(null, 12 * 32, new GameDamage(10), 25, 40000));
    }

    @Override
    public LootTable getLootTable() { return lootTable; }
}
```
Registered with: `MobRegistry.registerMob("eater", Eater.class, true);`. The base class
changes (`HostileMob` → `FlyingHostileMob`) but the AI tree
(`BehaviourTreeAI<>(this, new CollisionPlayerChaserWandererAI<>(...))`) and the "MUST HAVE an
empty constructor" comment are copied essentially verbatim from `ExampleMod` — strong
evidence `ExampleMod` is widely used as the literal starting template by other modders, not
just a doc reference. `CollisionPlayerChaserWandererAI` (package
`necesse.entity.mobs.ai.behaviourTree.trees`) is the one concrete AI-tree implementation
observed across all fetched mods: wander until a player is within range, then chase and
melee-damage them.

### 4.3 Wiring a mob into a biome's spawn tables

**Source:** [`DrFair/ExampleMod` → `src/main/java/examplemod/examples/ExampleBiome.java`](https://github.com/DrFair/ExampleMod/blob/master/src/main/java/examplemod/examples/ExampleBiome.java)
```java
public class ExampleBiome extends Biome {
    public static MobSpawnTable critters = new MobSpawnTable().include(Biome.defaultCaveCritters);
    public static MobSpawnTable mobs = new MobSpawnTable().add(100, "examplemob");

    @Override public MobSpawnTable getCritterSpawnTable(Level level) { return critters; }
    @Override public MobSpawnTable getMobSpawnTable(Level level) { return mobs; }
}
```

**Source:** [`AizSave/AphoreaMod` → `src/main/java/aphorea/biomes/InfectedFieldsBiome.java`](https://github.com/AizSave/AphoreaMod/blob/master/src/main/java/aphorea/biomes/InfectedFieldsBiome.java)
```java
@Override
public MobSpawnTable getMobSpawnTable(Level level) {
    if (!level.isCave) {
        return surfaceMobs;
    } else {
        return level.getIslandDimension() == -2 ? deepCaveMobs : caveMobs;
    }
}
```
`level.getIslandDimension() == -2` is the repeated idiom (also seen in
`necesseplus.CorruptionBiome`) for telling a deep cave apart from a regular cave inside a
single `Biome` subclass, since both call the same `getMobSpawnTable`/`getCritterSpawnTable`
hooks. Adding a mob to an *existing* vanilla table, rather than a mod's own biome, is a
one-liner, e.g. `ExampleMod.postInit()`: `Biome.defaultCaveMobs.add(100, "examplemob");` and
`NecessePlus.postInit()`: `Biome.defaultSurfaceMobs.add(60, "demoneye");`.

### 4.4 Patching a vanilla biome's spawn table without owning the class

**Source:** [`768gareth/necesse-expanded` → `src/main/java/NecesseExpanded/Patches/Gameplay/CritterSpawnTablePatch.java`](https://github.com/768gareth/necesse-expanded/blob/master/src/main/java/NecesseExpanded/Patches/Gameplay/CritterSpawnTablePatch.java)
```java
@ModMethodPatch(target = Biome.class, name = "getCritterSpawnTable", arguments = {Level.class})
public class CritterSpawnTablePatch
{
    @OnMethodExit
    static void onExit(@This Biome Biome, @Advice.Argument(0) Level ThisLevel, @Advice.Return(readOnly = false) MobSpawnTable Table)
    {
        if (ThisLevel.baseBiome == BiomeRegistry.CRYSTAL_HOLLOW) {
            Table.add(20, "shard_caveling").add(20, "crystal_caveling");
        } else if (ThisLevel.baseBiome == BiomeRegistry.SLIME_CAVE) {
            Table.add(20, "shard_caveling").add(20, "slime_caveling");
        } else if (ThisLevel.isIncursionLevel) {
            Table.add(20, "shard_caveling");
        }
    }
}
```
`necesse.engine.modLoader.annotations.ModMethodPatch` plus ByteBuddy's `net.bytebuddy.asm.Advice`
(`@OnMethodExit`, `@This`, `@Advice.Argument`, `@Advice.Return`) let a mod splice logic onto
the *exit* of any vanilla method — here, mutating the `MobSpawnTable` every `Biome` (vanilla
or modded) returns from `getCritterSpawnTable`, filtered by which biome instance it's called
on. This is the tool for "add my mob to this specific vanilla biome's spawns" when you don't
own that `Biome` subclass and can't just override a method. See §7 for the build/dependency
implication.

---

## 5. Custom items, recipes, and loot tables

### 5.1 Item base classes

**Source:** [`DrFair/ExampleMod` → `src/main/java/examplemod/examples/items/ExampleMaterialItem.java`](https://github.com/DrFair/ExampleMod/blob/master/src/main/java/examplemod/examples/items/ExampleMaterialItem.java)
```java
public class ExampleMaterialItem extends MatItem {
    public ExampleMaterialItem() { super(100, Rarity.UNCOMMON); }
}
```

**Source:** [`DrFair/ExampleMod` → `src/main/java/examplemod/examples/items/ExampleFoodItem.java`](https://github.com/DrFair/ExampleMod/blob/master/src/main/java/examplemod/examples/items/ExampleFoodItem.java)
```java
public class ExampleFoodItem extends FoodConsumableItem {
    public ExampleFoodItem() {
        super(250, Item.Rarity.COMMON, Settler.FOOD_FINE, 20, 480,
                new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 10),
                new ModifierValue<>(BuffModifiers.SPEED, 0.05f));
        spoilDuration(480);
        addGlobalIngredient("anycookedfood");
    }
}
```

**Source:** [`DrFair/ExampleMod` → `src/main/java/examplemod/examples/ExampleSwordItem.java`](https://github.com/DrFair/ExampleMod/blob/master/src/main/java/examplemod/examples/ExampleSwordItem.java)
```java
public class ExampleSwordItem extends SwordToolItem {
    // Weapon attack textures are loaded from resources/player/weapons/<itemStringID>
    public ExampleSwordItem() {
        super(400, null);
        rarity = Item.Rarity.UNCOMMON;
        attackAnimTime.setBaseValue(300);
        attackDamage.setBaseValue(20).setUpgradedValue(1, 95);
        attackRange.setBaseValue(120);
        knockback.setBaseValue(100);
    }
}
```
Registered in `ExampleMod.init()`: `ItemRegistry.registerItem("exampleitem", new ExampleMaterialItem(), 10, true);`
(and similarly for the sword/food/potion items) — always an **instance**, not a `Class`
(contrast `MobRegistry`/`LevelRegistry`, which register classes).

### 5.2 Recipes

**Source:** [`DrFair/ExampleMod` → `src/main/java/examplemod/ExampleMod.java`](https://github.com/DrFair/ExampleMod/blob/master/src/main/java/examplemod/ExampleMod.java) (`postInit()`)
```java
Recipes.registerModRecipe(new Recipe(
        "exampleitem", 1, RecipeTechRegistry.NONE,
        new Ingredient[]{ new Ingredient("ironbar", 2) }
).showAfter("woodboat")); // shows in the crafting list right after the wood boat recipe

Recipes.registerModRecipe(new Recipe(
        "examplesword", 1, RecipeTechRegistry.IRON_ANVIL,
        new Ingredient[]{ new Ingredient("exampleitem", 4), new Ingredient("copperbar", 5) }
));
```

**Source:** [`greatcoltini/NecessePlus` → `src/main/java/necesseplus/NecessePlus.java`](https://github.com/greatcoltini/NecessePlus/blob/master/src/main/java/necesseplus/NecessePlus.java) (`postInit()`)
```java
Recipes.registerModRecipe(new Recipe(
    "trophycase", 1, RecipeTechRegistry.WORKSTATION,
    new Ingredient[]{ new Ingredient("oaklog", 5) },
    true   // trailing boolean argument, meaning not confirmed from fetched source — see Gaps
));
```
`RecipeTechRegistry` constants (`NONE`, `IRON_ANVIL`, `WORKSTATION`, `CARPENTER`,
`DEMONIC_WORKSTATION`, `TUNGSTEN_LANDSCAPING`, ... all seen across mods) select which
crafting station the recipe requires; `Recipe.showAfter(otherItemStringID)` controls its
position in that station's recipe list. All recipe registration happens in `postInit()`,
never `init()` — by then every mod's items are guaranteed registered, so
`new Ingredient("ironbar", 2)` can safely reference vanilla *or* another mod's item string
ID.

### 5.3 Loot tables

**Source:** [`DrFair/ExampleMod` → `src/main/java/examplemod/examples/ExampleIncursionBiome.java`](https://github.com/DrFair/ExampleMod/blob/master/src/main/java/examplemod/examples/ExampleIncursionBiome.java)
```java
@Override
public LootTable getHuntDrop(IncursionData incursionData) {
    return new LootTable(new ChanceLootItem(0.66F, "examplehuntincursionitem"));
}
```

**Source:** [`greatcoltini/NecessePlus` → `src/main/java/necesseplus/biomes/corruption/CorruptionBiome.java`](https://github.com/greatcoltini/NecessePlus/blob/master/src/main/java/necesseplus/biomes/corruption/CorruptionBiome.java)
```java
public LootTable getExtraMobDrops(Mob mob) {
    if (mob.isHostile && !mob.isBoss() && !mob.isSummoned) {
        if (mob.getLevel().getIslandDimension() == -1)
            return new LootTable(randomPortalDrop, super.getExtraMobDrops(mob));
        if (mob.getLevel().getIslandDimension() == -2)
            return new LootTable(randomShadowGateDrop, super.getExtraMobDrops(mob));
    }
    return super.getExtraMobDrops(mob);
}
```

**Source:** [`768gareth/necesse-expanded` → `src/main/java/NecesseExpanded/Registry/UpdateLootTables.java`](https://github.com/768gareth/necesse-expanded/blob/master/src/main/java/NecesseExpanded/Registry/UpdateLootTables.java)
```java
// Editing vanilla loot tables directly, post-hoc, from a mod's postInit()
Biome.defaultSurfaceFish.addSaltWater(30, "saltwater_shark").addSaltWater(20, "seaweed")
        .addWater(10, "surface_treasure_chest").addSaltWater(1, "pearl_oyster");

VampireMob.lootTable.items.add(new ChanceLootItemList(0.05f, CaveCryptLootTable.uniqueItems));
SnowWolfMob.lootTable.items.add(new LootItem("leather"));

LootTablePresets.startChest = new LootTable(
    new LootItem("settlementflag"), new LootItem("coin", 500), new LootItem("sprucelog", 250),
    new LootItem("stone", 150), new LootItem("torch", 25) /* ... */
);
```
Three distinct shapes: a **fixed** `LootTable` built from `ChanceLootItem`/`LootItem`
entries for a mod's own content (§5.3.1-2); overriding a `Biome`/`Mob` hook method to
*compose* a table dynamically (`CorruptionBiome.getExtraMobDrops`, delegating to
`super.getExtraMobDrops(mob)` so vanilla drops aren't lost); and, when a mod wants to change
*vanilla's* loot without subclassing anything, directly mutating the public static
`LootTable`/`MobSpawnTable`-like fields vanilla exposes (`Biome.defaultSurfaceFish`,
`VampireMob.lootTable`, `LootTablePresets.startChest`) from `postInit()`.

---

## 6. Custom biomes (`BiomeRegistry`)

### 6.1 Registration — and an observed signature discrepancy

**Source:** [`pyralisxc/NECESSE_MODDING_RESOURCES` → `readable_source_complete/necesse/engine/registries/BiomeRegistry.java`](https://github.com/pyralisxc/NECESSE_MODDING_RESOURCES/blob/master/readable_source_complete/necesse/engine/registries/BiomeRegistry.java)
(decompiled vanilla engine class — signature only)
```java
public static <T extends Biome> T registerBiome(String stringID, T biome, boolean countInStats) {
    if (LoadedMod.isRunningModClientSide())
        throw new IllegalStateException("Client/server only mods cannot register biomes");
    return BiomeRegistry.instance.registerObj(stringID, new BiomeRegistryElement<T>(biome, countInStats)).biome;
}
```
Every mod using this 3-argument shape returns the registered instance, which is how
`ExampleMod` captures `EXAMPLE_BIOME = BiomeRegistry.registerBiome("exampleincursion", new ExampleBiome(), false);`
as a static field for later reference (e.g. from `ExampleIncursionLevel`'s constructor).

However, `greatcoltini/NecessePlus` calls a **4-argument** form:
```java
// necesseplus/NecessePlus.java
BiomeRegistry.registerBiome("corruptionbiome", new CorruptionBiome(), 600, "forest");
```
`(String, Biome, int, String)` does not match the 3-arg `(String, Biome, boolean)` signature
above. This was not resolved — most likely `NecessePlus` targets an older Necesse API version
with a different `BiomeRegistry.registerBiome` overload (e.g. `weight` + "spread from"
parent biome, given `CaveLevel`'s generation code separately references a `spreadBiome`
concept), but this is inference, not confirmed from fetched source. **Practical takeaway for
this project:** confirm the exact `BiomeRegistry`/`TileRegistry` method signatures against
the specific `Necesse.jar` this mod compiles against (via the `decompileToSources` Gradle
task already in this repo) rather than trusting any one mod snippet, since the modding API
has visibly drifted across game versions.

### 6.2 A biome supplying its own surface/cave/deep-cave `Level`s

**Source:** [`greatcoltini/NecessePlus` → `src/main/java/necesseplus/biomes/corruption/CorruptionBiome.java`](https://github.com/greatcoltini/NecessePlus/blob/master/src/main/java/necesseplus/biomes/corruption/CorruptionBiome.java)
```java
public class CorruptionBiome extends Biome {

  public static MobSpawnTable defaultSurfaceCritters = new MobSpawnTable().add(100, "eater");
  public static MobSpawnTable caveCritters = new MobSpawnTable().include(Biome.defaultCaveCritters).add(100, "stonecaveling");
  public static MobSpawnTable deepCaveCritters = new MobSpawnTable().include(Biome.defaultCaveCritters).add(100, "deepstonecaveling");

  public Level getNewSurfaceLevel(int islandX, int islandY, float islandSize, Server server, WorldEntity worldEntity) {
    return new CorruptionSurfaceLevel(islandX, islandY, islandSize, worldEntity);
  }

  public Level getNewCaveLevel(int islandX, int islandY, int dimension, Server server, WorldEntity worldEntity) {
    return new CorruptionCaveLevel(islandX, islandY, dimension, worldEntity);
  }

  public Level getNewDeepCaveLevel(int islandX, int islandY, int dimension, Server server, WorldEntity worldEntity) {
    return new CorruptionDeepCaveLevel(islandX, islandY, dimension, worldEntity);
  }

  public MobSpawnTable getCritterSpawnTable(Level level) {
    if (level.isCave) {
      return level.getIslandDimension() == -2 ? deepCaveCritters : caveCritters;
    }
    return defaultSurfaceCritters;
  }
}
```
**This is the direct answer to "how a biome supplies its surface/cave levels and mob
spawns."** `Biome` exposes `getNewSurfaceLevel`/`getNewCaveLevel`/`getNewDeepCaveLevel`
factory hooks — whatever world-gen code decides "this island's biome is `CorruptionBiome`"
calls back into exactly these three methods to actually construct the three registered
`Level` subclasses for that island (surface + its cave + its deep cave), while
`getMobSpawnTable`/`getCritterSpawnTable` are queried per-tick/per-spawn-attempt against
whichever `Level` the query originated from, branching on `level.isCave` and
`level.getIslandDimension() == -2` to pick the right table for the depth.

### 6.3 A biome that reuses part of a vanilla biome instead of building its own levels

**Source:** [`768gareth/necesse-expanded` → `src/main/java/NecesseExpanded/World/Biomes/FishianBiome.java`](https://github.com/768gareth/necesse-expanded/blob/master/src/main/java/NecesseExpanded/World/Biomes/FishianBiome.java)
```java
public class FishianBiome extends SwampBiome
{
  public MobSpawnTable getMobSpawnTable(Level level) {
    return new MobSpawnTable().add(100, "staticjellyfish");
  }
  public MobSpawnTable getCritterSpawnTable(Level level) { return deepCaveCritters; }
  public float getSpawnRateMod(Level level) { return super.getSpawnRateMod(level) * 1.2F; }
  public AbstractMusicList getLevelMusic(Level level, PlayerMob perspective) {
      return new MusicList(MusicRegistry.Away);
  }
}
```
Registered (`768gareth/necesse-expanded` → `src/main/java/NecesseExpanded/Registry/RegisterBiomes.java`):
```java
public class RegisterBiomes
{
    public static void Register()
    {
        BiomeRegistry.registerBiome("fishian_biome", new FishianBiome(), true);
        BiomeRegistry.registerBiome("tropical_biome", new TropicalBiome().setGenerationWeight(0.75f), true);
        BiomeRegistry.registerBiome("haunted_biome", new HauntedBiome().setGenerationWeight(0.75f), true);
    }
}
```
By extending `SwampBiome` instead of `Biome` directly, `FishianBiome` inherits all of
`SwampBiome`'s island/cave/deep-cave `Level` factory methods for free and only overrides
spawn tables, spawn rate, and music — it never defines its own surface `Level` class (its
"fishian_dungeon" `Level`, §1.5, is instead reached via a placed portal object rather than
being one of the biome's own generated layers). `.setGenerationWeight(float)` is the
observed way to control how often a biome's island type is picked during world gen (compare
vanilla's own `new ForestBiome().setGenerationWeight(1.0f)`, `new DesertBiome().setGenerationWeight(1.5f)`
seen in the decompiled `BiomeRegistry.registerCore()`).

---

## 7. Buildscript quirks and resource folder structure

### 7.1 `build.gradle` — the standard shape

**Source:** [`DrFair/ExampleMod` → `build.gradle`](https://github.com/DrFair/ExampleMod/blob/master/build.gradle)
```groovy
plugins { id 'java' }

project.ext.modID = "fair.examplemod"
project.ext.modName = "Example Mod"
project.ext.modVersion = "1.0"
project.ext.gameVersion = "1.3.2"
project.ext.clientside = false   // false = server needs it too; true = safe to omit from dedicated servers

def gameDirectory = getDefaultGamePath()   // auto-detects the Steam install per-OS

dependencies {
    implementation files(gameDirectory + "/Necesse.jar")   // NOT a maven artifact — a direct file dependency
    implementation fileTree(gameDirectory + "/lib/")
    implementation fileTree("./mods/")                      // other installed mods, for cross-mod compiling
}

tasks.register('createModInfoFile', JavaExec) {
    classpath = files(gameDirectory + "/Necesse.jar")
    getMainClass().set("CreateModInfoFile")   // runs a class shipped INSIDE Necesse.jar itself
    args "-file", "${sourceSets.main.java.getClassesDirectory().get()}/mod.info",
            "-id", "${project.ext.modID}", "-name", "${project.ext.modName}", /* ... */
}
classes.dependsOn("createModInfoFile")

tasks.register('preAntialiasTextures', JavaExec) {
    // fixes black/halo edges on textures caused by alpha blending under rotation/scaling
    classpath = files(gameDirectory + "/Necesse.jar")
    main "PreAntialiasTextures"
    args "-folders", "${sourceSets.main.resources.srcDirs...}"
}

tasks.register('runClient', JavaExec) {
    dependsOn "buildModJar", "createAppID"
    classpath = files(gameDirectory + "/Necesse.jar")
    getMainClass().set("StartSteamClient")   // also a class from Necesse.jar
    jvmArgs "-Xms512m", "-Xmx4G", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseG1GC", /* ... */
    args "-dev", "-mod \"${buildLocation}\""
}
```
The two load-bearing quirks: **`Necesse.jar`/`Server.jar` are consumed as raw local `files(...)`
dependencies pointing at the Steam install directory**, not published Maven artifacts; and
**several Gradle tasks (`createModInfoFile`, `preAntialiasTextures`, `runClient`,
`runDevClient`, `runServer`) run utility/bootstrap classes that live inside `Necesse.jar`
itself** (`CreateModInfoFile`, `PreAntialiasTextures`, `StartSteamClient`,
`StartDesktopServer`) via `JavaExec`, rather than any separate tool. `sourceSets.main.output`
is redirected to `build/mod/` so compiled classes and resources end up merged in one place
before `buildModJar` zips them.

### 7.2 A real mod's variation: declaring a mod dependency + a bundled third-party jar

**Source:** [`768gareth/necesse-expanded` → `build.gradle`](https://github.com/768gareth/necesse-expanded/blob/master/build.gradle)
```groovy
project.ext.modDependencies = ["aizsave.modsettingslib"]   // load-order dependency on another mod's ID

dependencies {
    implementation files(gameDirectory + "/Necesse.jar")
    implementation fileTree(gameDirectory + "/lib/")
    implementation fileTree("./mods/")
    compileOnly files('libs/ModSettingsLib-0.33.1-1.2.3.jar')       // compile against it...
    implementation files('libs/ModSettingsLib-0.33.1-1.2.3.jar')    // ...but don't bundle it (compileOnly + implementation for autocomplete)
}
```
`project.ext.modDependencies` (a list of other mods' `modID` strings) sets load order and is
baked into the generated `mod.info` by `createModInfoFile` (`-dependencies` arg). A local jar
checked into `libs/` for a shared library dependency is added twice — `compileOnly` so it's
on the compile classpath without the build re-packaging it into this mod's own jar, and
`implementation` again purely so the IDE gets autocomplete — the actual runtime copy is
expected to be the dependency mod's own separately-installed jar.

**Bonus quirk:** `768gareth/necesse-expanded` uses `@ModMethodPatch` +
`net.bytebuddy.asm.Advice` (§4.4) with **no corresponding `libDepends`/`implementation`
entry for ByteBuddy anywhere in its `build.gradle`** — ByteBuddy ships as part of the
Necesse modloader itself (used for the game's own patch-based mod support), so a mod can
`import net.bytebuddy.asm.Advice.*` and `necesse.engine.modLoader.annotations.ModMethodPatch`
without adding any new dependency at all.

### 7.3 Resource folder structure

**Source:** [`DrFair/ExampleMod`](https://github.com/DrFair/ExampleMod) file listing (via GitHub tree API) + `GameTexture.fromFile(...)` call sites read above
```
src/main/resources/
├── locale/en.lang
├── buffs/<stringID>.png
├── items/<stringID>.png
├── mobs/<stringID>.png                     (ExampleMob.texture = GameTexture.fromFile("mobs/examplemob"))
├── objects/<stringID>.png                  (ExampleObject: GameTexture.fromFile("objects/exampleobject"))
├── tiles/<stringID>.png                    (ExampleTile: GameTexture.fromFile("tiles/exampletile"))
├── player/weapons/<itemStringID>.png       (ExampleSwordItem: attack anim textures, per its own comment)
└── projectiles/<stringID>.png
```
The convention is uniform across every mod fetched: `GameTexture.fromFile("<category>/<stringID>")`,
where `<category>` is a fixed top-level resource folder name matching the registry kind
(`mobs`, `objects`, `tiles`, `items`, `buffs`, `projectiles`), and `<stringID>` is exactly the
string first argument passed to that thing's `registerX(...)` call. This project's own
`src/main/resources/locale/en.lang` (already present in `Necesse-stairwaytoheaven`) matches
this convention already.

---

## Key repos

- **[`DrFair/ExampleMod`](https://github.com/DrFair/ExampleMod)** — the single best reference
  found. A compact, actively-referenced official-style example mod covering every registry
  (level, biome, incursion biome, tile, object, mob, item, projectile, buff, packet, chat
  command) plus recipes and `build.gradle`/`preAntialiasTextures`. Other mods' code (e.g.
  `greatcoltini/NecessePlus`'s `Eater.java`) is visibly copy-adapted from it.
- **[`768gareth/necesse-expanded`](https://github.com/768gareth/necesse-expanded)** — the
  largest real overhaul mod fetched from. Best source for: a full custom dungeon-entrance
  teleport object pair (§2.2), the `RegisterX`/`UpdateX`-per-concern mod-organization
  pattern (§ModEntry lifecycle, §6.3), `@ModMethodPatch`/ByteBuddy usage (§4.4), and a
  `build.gradle` with a real mod-to-mod dependency (§7.2).
- **[`greatcoltini/NecessePlus`](https://github.com/greatcoltini/NecessePlus)** — best source
  for a complete biome-owns-its-levels trio wired through `getNewSurfaceLevel`/
  `getNewCaveLevel`/`getNewDeepCaveLevel` (§6.2), and a second full mob example
  (`FlyingHostileMob`).
- **[`AizSave/AphoreaMod`](https://github.com/AizSave/AphoreaMod)** — best source for
  subclassing an existing vanilla `Level` type (`TrialRoomLevel`) instead of building one
  from scratch (§1.4), and per-depth (`surface`/`cave`/`deepCave`) spawn-table branching on
  a custom `Biome` (§4.3).
- **[`KingEnder04/vulpes-nova`](https://github.com/KingEnder04/vulpes-nova)** and
  **[`AldiandyaIrsyad/Necesse-AoA`](https://github.com/AldiandyaIrsyad/Necesse-AoA)** —
  further real examples of the surface/cave/deep-cave `Level` trio-per-biome pattern (island
  generation variant), useful for cross-checking §1.3/§6.2 against a second and third
  implementation.
- **[`Azuraei/Soul-Chasm`](https://github.com/Azuraei/Soul-Chasm)** and
  **[`luna-system/LunarIncursions`](https://github.com/luna-system/LunarIncursions)** —
  further incursion-style `Biome`/`IncursionBiome`/`Level` triples, structurally identical to
  `ExampleMod`'s incursion example but at real-mod scale (custom mobs, bosses, items,
  armor).
- **[`Shuazijun/TPA-Commands`](https://github.com/Shuazijun/TPA-Commands)** — best source for
  the direct `ServerClient.changeLevel(...)` API used outside of any placed object (§2.4).
- **[`EliasVahlberg/necesse-headless-harness`](https://github.com/EliasVahlberg/necesse-headless-harness)**
  and **[`EliasVahlberg/arcane-storage`](https://github.com/EliasVahlberg/arcane-storage)** —
  not used for the core patterns above, but worth knowing about: a headless integration-test
  harness for Necesse mods, and a large, heavily-documented `InventoryObjectEntity`-based
  mod (storage network) with unusually thorough inline commentary on `ObjectEntity` save/load
  quirks (e.g. `docs/MOD_COMPAT.md`: *"Necesse has no tags and no annotations"* for
  cross-mod object compatibility).
- **[`pyralisxc/NECESSE_MODDING_RESOURCES`](https://github.com/pyralisxc/NECESSE_MODDING_RESOURCES)**
  — not a mod; a decompiled copy of the vanilla engine, organized under
  `readable_source_complete/necesse/...`, used here only to confirm exact vanilla method
  signatures (`LevelRegistry`, `BiomeRegistry`, `PortalObjectEntity`, the ladder classes).
  Treat as a browsing aid for your own local `decompileToSources` output, not as something to
  copy from directly — see the note at the top of this document.

## Gaps

- **`TileRegistry.registerTile` / `ObjectRegistry.registerObject` exact signatures were not
  confirmed.** Only call-site usages were fetched (e.g. `TileRegistry.registerTile(id, tile,
  1, true)` vs `TileRegistry.registerTile(id, tile, 0.0F, false)` — an `int` in one mod, a
  `float` in another, at the same argument position), so the real meaning of that 3rd
  argument (and the trailing `boolean`) is not established here. Needs checking against this
  project's actual decompiled `Necesse.jar`.
- **`BiomeRegistry.registerBiome` signature drift is unresolved.** The vanilla decompiled
  source only shows a 3-arg `(String, Biome, boolean)` overload, but `greatcoltini/NecessePlus`
  calls a 4-arg `(String, Biome, int, String)` form. Likely explained by the two projects
  targeting different Necesse game versions, but this was not verified against either mod's
  declared `gameVersion` in `build.gradle`.
- **`Level`'s own `addSaveData`/`applyLoadData` (or equivalent) were not fetched.** We
  confirmed the *reflective constructor contract* (`LevelRegistry`, §1.1) and one concrete
  "re-derive a transient field after load" example (`FishianBiomeLevel.onLoadingComplete()`,
  §1.5), and separately confirmed the save-data pair on `PortalObjectEntity` (§2.1), but we
  did not find or read `Level`'s own save/load implementation, so exactly which `Level`
  fields the engine persists automatically (versus needing a mod's own re-derivation hook)
  is only partially confirmed.
- **No mod-authored "player transfer between levels via a non-portal, non-command trigger"**
  (e.g. a scripted cutscene or timed event forcing a level change) was found — everything
  located routes through either a `PortalObjectEntity` subclass (§2.2/§2.3) or a direct
  `ServerClient.changeLevel(...)` call from a command/packet handler (§2.4).
- **`Recipe`'s trailing boolean argument** (seen in `NecessePlus`'s
  `new Recipe(id, count, tech, ingredients, true)`, vs the 4-arg form without it used
  elsewhere) was not resolved — plausibly "unlocked/known by default" or a "carpenter-only"
  flag, but no vanilla `Recipe` source was fetched to confirm.
- **`necesse-modding-cli`** (`the-aspecty/necesse-modding-cli`) turned up repeatedly in
  searches as a scaffolding tool for new mods/items/mobs/tiles, but its generated code was
  not fetched or verified — mentioned here only as a lead, not as a source for any pattern
  above.
- **`Necesse-Community/ModStarter`** — repeatedly recommended in search results as the
  community-maintained starter template with "register methods ordered as recommended by the
  Necesse developer," but its GitHub API tree/file fetches were blocked by rate-limiting in
  this session (`403`), so it could not be read or cited directly. Worth revisiting.
