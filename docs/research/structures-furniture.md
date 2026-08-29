# Necesse 1.3.2 — Structures, Presets, Walls & Furniture: How-To Reference

Source: decompiled `necesse/*` at `/home/user/necesse-game/decompiled/necesse/`
(read via grep + targeted `Read`, cross-checked against the extracted vanilla
sprite pack at `/home/user/necesse-game/sprites/` for exact pixel dimensions).

**Legal note**: per instruction, no multi-line decompiled method *bodies* are
reproduced here. What follows is signatures, one-line registration calls
(these are data/config, not logic), field names, and tables describing sprite
sheet layouts that were reverse-engineered from reading the draw code (the
draw code itself is described in prose, not quoted).

---

## 1. Presets (structures)

### 1.1 The `Preset` class — two authoring styles

`necesse.level.maps.presets.Preset` (`level/maps/presets/Preset.java`, ~2280
lines) is the one class that represents "a stamped chunk of level content"
(tiles + two object layers + wall-decor/table-decor layers + rotations +
wires + logic gates + object-entities). Every vanilla hand-built structure
(`CaveHoboHomePreset`, `MinersOfficePreset`, `DungeonEntrancePreset`, etc.)
extends it. Two constructors matter for authoring:

```java
public Preset(int width, int height)   // build up empty, then call setters
public Preset(String script)           // load from an embedded PRESET script
public Preset(LoadData save)           // load from binary SaveData
```

**Style A — embedded script** (what almost every vanilla preset actually
uses, including both `CaveHoboHomePreset` and `MinersOfficePreset`): the
constructor calls `super("PRESET = { width = .., height = .., tileIDs = [...],
tiles = [...], objectIDs = [...], objects = [...], rotations = [...],
tileObjectsClear = true, wallDecorObjectsClear = true, tableDecorObjectsClear
= true, clearOtherWires = false }")`. This string is exactly what the
in-game "Preset tool" exports when you build a structure in Creative Mode and
copy it out — `tileIDs`/`objectIDs` are local ID→stringID lookup tables
scoped to that one preset (so the numeric IDs in `tiles`/`objects` are
preset-local, not global registry IDs), and `tiles`/`objects`/`rotations`
are flat `width*height` arrays, row-major, `-1` = "leave untouched" for
tiles, and object `0` (mapped to stringID `"air"`) = "clear any object".
Optional arrays also seen: `tableDecorObjects`, `tableDecorRotations`.

**Style B — imperative**: build with `new Preset(width, height)` then call
the mutator API directly (`setTile`, `setObject`, `fillObject`, `boxObject`,
`addSign`, etc. — full list below). Nothing in vanilla actually authors this
way for hand-designed rooms (script export is easier to eyeball-test in
game), but it's fully supported and is how a mod without access to the
in-game Preset tool would build one from raw code, or how procedural code
assembles one tile-by-tile.

After the base geometry is loaded, **both constructors then run ordinary
Java code** to re-skin/parameterize the preset per-call: the constructor
signature itself carries the "knobs" (see next section) and the constructor
body calls mutators like `replaceObject`, `replaceNonEmptyTiles`,
`addInventory`, `addCustomApply`, `addSign`, `iteratePreset` to
randomize/finish the room. A preset is **not** static data — it's a small
program that produces slightly different, seeded-random output every time
it's instantiated (see `random.getOneOf(...)`, `random.getEveryXthChance(3)`
usage below).

### 1.2 Constructor patterns, from the two reference presets

```java
// CaveHoboHomePreset.java — a "hand-decorated room", themed via 3 Set objects
public CaveHoboHomePreset(Biome biome, LevelIdentifier levelIdentifier, GameRandom random,
                           RockAndOreSet walls, FurnitureSet furniture, WallSet doors)

// MinersOfficePreset.java — same idea, 5 Set objects (adds CrystalSet, FloorSet, ColumnSet)
public MinersOfficePreset(Biome biome, LevelIdentifier levelIdentifier, GameRandom random,
                           RockAndOreSet rocksAndWalls, CrystalSet crystal, FloorSet floor,
                           ColumnSet columns, FurnitureSet furniture)
```

Common shape: `(Biome, LevelIdentifier, GameRandom, ...one-or-more PresetSet<T> "palette" objects)`.
The `Set` types (`level/maps/presets/set/*.java`: `RockAndOreSet`, `FurnitureSet`,
`WallSet`, `CrystalSet`, `FloorSet`, `ColumnSet`, `CarpetSet`, all `extends
PresetSet<T>`) are named bundles of pre-registered object IDs for one theme
(e.g. `FurnitureSet.oak`, `.deadwood`, `.bamboo` each carry `chair`, `bed`,
`dinnerTable`, `bookshelf`, `cabinet`, `desk`, ... as `public final int`
fields). `SomeSet.oak.replaceWith(pickedSet, this)` walks the preset and
swaps every placeholder ID from the "authoring" set to the runtime-picked
theme's equivalent ID — this is how one preset script serves 6+ biome
re-skins. **A mod's preset does not need this system** — it's a convenience
for vanilla's "one preset, many wood types" reuse. A mod can hardcode its
own registered object IDs directly into the preset script/constructor.

Other patterns seen in the constructor bodies (call signatures only):

```java
this.replaceNonEmptyObjects(int oldObjectID, int newObjectID)
this.replaceNonEmptyTiles(int oldTile, int newTile)
this.iteratePreset(BiConsumer<Integer,Integer> perTileXY)      // scan+mutate
this.addInventory(LootTable lootTable, GameRandom random, int x, int y, Object... extra)
this.addSign(String text, int x, int y)
this.addCustomApply(int tileX, int tileY, int dir, Preset.CustomApplyFunction apply)
```

`addCustomApply` is how `CaveHoboHomePreset` spawns and homes an NPC as part
of placement (the lambda receives `(level, levelX, levelY, dir, blackboard)`,
runs once when the preset is actually stamped into a real level, and can
return an "undo" callback run if the placement is later reverted):

```java
this.addCustomApply(14, 9, 0, (level, levelX, levelY, dir, blackboard) -> {
    HumanMob settler = (HumanMob) MobRegistry.getMob(randomSettlerID, level);
    settler.setHome(levelX, levelY);
    settler.canDespawn = false;
    level.entityManager.addMob(settler, ...spawnLocation...);
    return (level1, presetX, presetY) -> settler.remove();   // undo hook
});
```

### 1.3 Full `Preset` mutator/query API (signatures, grouped)

Geometry / placement:
```java
LinkedList<Preset.UndoLogic> applyToLevel(Level level, int levelX, int levelY)
LinkedList<Preset.UndoLogic> applyToLevel(Level level, int levelX, int levelY, GameBlackboard blackboard)
void applyToLevelCentered(Level level, int x, int y[, GameBlackboard])
boolean canApplyToLevel(Level level, int levelX, int levelY)
Preset subPreset/subPresetFull(int x, int y, int width, int height)
Preset tryMirrorX() / tryMirrorY() / tryRotate(PresetRotation rotation)
```
Tile/object/rotation/wire get-set (single cell and rectangle-fill variants):
```java
setTile / getTile(int x, int y)
setObject / getObject(int layerID, int x, int y)      // and no-layerID overload = layer 0
setRotation / getObjectRotation(...)
setWireData / putWire / clearWire / hasWire(int x, int y, int wireID)
fillTile / fillObject / boxTile / boxObject(int x, int y, int w, int h, ...)
replaceTile / replaceObject / replaceNonEmptyTiles / replaceNonEmptyObjects(...)
randomlyReplaceObjects(int oldObjectID, Supplier<Integer> getRandomObjectID)
setLogicGate(int tileX, int tileY, LogicGateEntity entity)
setObjectEntity(int tileX, int tileY, ObjectEntity entity)
```
Hooks that run at stamp-time (all take tile coords local to the preset, `dir`
= the rotation the preset is being placed with):
```java
onTileApply / onObjectApply / onWireApply(listener)
addCanApplyPredicate(...) / addCanApplyAreaPredicate(...) / addCanApplyRectPredicate(...)
addCustomPreApply(...) / addCustomPreApplyArea/Rect(...)     // before geometry is written
addCustomApply(...)      / addCustomApplyArea/Rect(...)      // after geometry is written
```
Population helpers:
```java
addMob(String mobStringID, int tileX, int tileY, Consumer<Mob> onAdded[, boolean canDespawn/limitWithinBounds])
addHuman(String mobStringID, int homeX, int homeY, Consumer<HumanMob> onAdded, GameRandom random)
addInventory(LootTable lootTable, GameRandom random, int tileX, int tileY, Object... extra)
addSign(String text | Supplier<String> text, int x, int y[, int object, int objectRotation])
```
Save/serialize:
```java
SaveData getSaveData([String saveName][, PresetCopyFilter filter])
String getScript() / String getCompressedBase64Script()
static Preset copyFromLevel(Level level, int x, int y, int width, int height)
```

### 1.4 How a preset gets stamped into a level

Two separate mechanisms exist, for two different situations:

**(A) `PresetGeneration`** (`level/maps/generationModules/PresetGeneration.java`)
— a small per-`Level` helper used by *ordinary procedural level generation*
(the kind that runs once when a level's terrain generator builds it, e.g.
placing dungeon rooms or cave features while the cave is being carved). Full
API surface (this file is short enough to be complete, and is all
signatures/simple bodies, so quoting the shape is safe):
```java
public PresetGeneration(Level level)
void addOccupiedSpace(Rectangle | int x,int y,int w,int h)
void applyPreset(Preset preset, int tileX, int tileY)         // preset.applyToLevel(...) + records occupied space
boolean canPlacePreset(Preset preset, int tileX, int tileY)   // !isSpaceOccupied(...)
Point getRandomPresetPosition(GameRandom random, Preset preset, int edgeSpace)
Point findRandomValidPositionAndApply(GameRandom random, int attempts, Preset preset, int edgeSpace,
        boolean randomizeMirrorX, boolean randomizeMirrorY, boolean randomizeRotation, boolean overrideCanPlace)
```
`findRandomValidPositionAndApply` is the actual "pick a spot" algorithm: it
loops up to `attempts` times, each time calling `getRandomPresetPosition`
(uniform-random tile inside the level, `edgeSpace` tiles clear of the
border), and applies the preset at the first position that doesn't overlap
`occupiedSpace` and passes `preset.canApplyToLevel(...)`. **This is
random-within-the-level positioning**, not deterministic/fixed.

**(B) `WorldPresetRegistry` / `WorldPreset` / `LevelPresetsRegion`**
(`engine/world/worldPresets/*`, `engine/registries/WorldPresetRegistry.java`)
— the system behind essentially every named point-of-interest in the game
(villages, dungeon entrances, boss arenas, ore-crystal clusters, the
"CaveHoboHome"/"MinersOffice" rooms investigated above, etc.). This is the
one that answers "how does vanilla decide positions" and "how to do this
once, deterministically" — see 1.5–1.7.

`GenerationPreset<T>` / `SimpleGenerationPreset` (used by
`CaveHoboHomeGenerationPreset`, `MinersOfficeGenerationPreset`) is a third,
thin wrapper specifically for "one small room placed via `WorldPresetTester`
inside a cave-generation pass" — constructor shape:
```java
public SimpleGenerationPreset(int placeAttempts, boolean randomizeMirrorX, boolean randomizeMirrorY,
                               boolean randomizeRotation, boolean checkCanPlaceForAllOptions, Biome... biomes)
// subclass overrides:
public Preset getPreset(GameRandom random)          // return `new MyPreset(biome, levelIdentifier, random, ...pickedSets)`
public void setupTester(WorldPresetTester tester)    // add corner/area validity predicates
```

### 1.5 How vanilla decides *where* (region model)

The overworld is divided into `WorldPresetsRegion`s — fixed 64×64-**level-region**
blocks per `LevelIdentifier` (surface / cave / deep-cave are tracked
separately: `LevelPresetsRegion.identifier`). Real fields (from
`WorldPresetsRegion.java`):
```java
public final WorldEntity worldEntity;
public final int worldPresetRegionX, worldPresetRegionY;   // which 64x64 block, in region-grid coords
public final int startLevelRegionX, startLevelRegionY, endLevelRegionX, endLevelRegionY;
public final int startTileX, startTileY, endTileX, endTileY, tileWidth, tileHeight;
```
`WorldPresetRegistry.initRegion(LevelPresetsRegion region, int customSeed, ...)`
runs **once** the first time a given `(worldPresetRegionX, worldPresetRegionY,
identifier)` triple is generated (i.e. once per 64×64 chunk of surface, once
per 64×64 chunk of caves, once per 64×64 chunk of deep caves — as the
infinite world expands outward from spawn). It builds a `GameRandom` seeded
from `worldEntity.getNewWorldRandom().nextSeeded(worldPresetRegionX)
.nextSeeded(worldPresetRegionY).nextSeeded(identifier.hashCode())` — i.e.
**fully deterministic per world-seed + region + level-layer** — then, for
every registered `WorldPreset` (priority-sorted), reseeds again with
`.nextSeeded(preset.getStringID().hashCode())` and, if
`preset.shouldAddToRegion(region)` is true, calls `preset.addToRegion(seededRandom,
region, generatorStack, timer)`.

Once a region has been generated this way, `WorldPresetsRegion` persists
that fact (`saveGeneratedPresetsFile(LevelIdentifier)`) — **the region is
never re-rolled**, even across game restarts; this is the same mechanism
that guarantees a village or dungeon entrance you found once stays exactly
where it was.

`WorldPreset` abstract contract (`engine/world/worldPresets/WorldPreset.java`):
```java
public abstract boolean shouldAddToRegion(LevelPresetsRegion presetsRegion);
public abstract void addToRegion(GameRandom random, LevelPresetsRegion presetsRegion,
                                  BiomeGeneratorStack generatorStack, PerformanceTimerManager timer);
public int getPriority();   // higher runs first within a region
static int getTotalBiomePoints(GameRandom random, LevelPresetsRegion presetsRegion, Biome biome, float pointsPerRegion);
static void ensureRegionsAreGenerated(Level level, int tileX, int tileY, int width, int height);
```
`getTotalBiomePoints(...)` is the vanilla "how many of these per region"
density knob — it turns a `pointsPerRegion` float (e.g. `0.002F` for
`DungeonEntranceWorldPreset`, `0.0375F` for forest vampire crypts) into an
integer count for the region, proportional to how much of the target biome
that region actually contains. `addToRegion` then typically loops that many
times, each time picking a **random** valid tile
(`findRandomBiomePresetTile(...)`, or a hand-rolled corner-check loop as in
`DungeonEntranceWorldPreset`) and, on success, calls:
```java
presetsRegion.addPreset(this, tileX, tileY, sizeDimension, occupiedSpaceBoardNames,
    (rnd, level, timer) -> {
        WorldPreset.ensureRegionsAreGenerated(level, tileX, tileY, w, h);
        MyPreset preset = new MyPreset(...);
        PresetUtils.clearMobsInPreset(preset, level, tileX, tileY);
        preset.applyToLevel(level, tileX, tileY);
    });
```
Registration (mod-visible; the vanilla `registerCore()` in
`WorldPresetRegistry.java` is just ~60 calls of this exact shape):
```java
WorldPresetRegistry.registerPreset("mymod_myroom", new MyRoomWorldPreset());
```

### 1.6 Random-in-region vs. fixed — what vanilla actually does

Every vanilla `WorldPreset` reviewed (`DungeonEntranceWorldPreset`,
`CaveHoboHomeGenerationPreset`, `BiomeCenterWorldPreset`, and the density
model in general) places **N random valid tiles per region**, where N is
usually 0 or 1 for a "rare" structure (`pointsPerRegion` small) and can be
larger for common ones (crystal clusters, spider nests). There is no vanilla
example of "exactly one, ever, at a literal fixed coordinate" in the classes
read — the closest is `BiomeCenterWorldPreset` (used for things anchored to
the *middle* of a large biome blob — desert temple-style placement), which
does a breadth-first flood fill to find a biome's connected-region centroid;
that's a much heavier mechanism than a simple unique structure needs.

### 1.7 Recommended recipe: ONE unique preset, deterministic position, first-generation-only

Combining the above into the cleanest pattern for "stamp exactly one
hand-built room somewhere in the world, at a position that's the same every
time you regenerate that world from the same seed, and never re-roll it":

1. Author the room as a normal `Preset` subclass (script-based or
   imperative — see 1.1–1.2).
2. Wrap it in a `WorldPreset` subclass. In `shouldAddToRegion`, restrict to
   exactly the one `LevelPresetsRegion` you want to own the structure —
   either the region identifier alone (e.g. only `SURFACE_IDENTIFIER`) if
   "somewhere on the surface, once per world" is enough, **or**, for a truly
   single global instance, additionally pin it to one specific region
   coordinate:
   ```java
   public boolean shouldAddToRegion(LevelPresetsRegion r) {
       return r.identifier.equals(LevelIdentifier.SURFACE_IDENTIFIER)
           && r.worldRegion.worldPresetRegionX == 0
           && r.worldRegion.worldPresetRegionY == 0;   // the region containing world spawn
   }
   ```
   Because each `(worldPresetRegionX, worldPresetRegionY, identifier)` is
   generated exactly once per world (ever — see 1.5), gating on a single
   fixed region coordinate makes `addToRegion` fire **exactly once, ever,
   for that world seed** — which *is* "on first generation of that region."
3. In `addToRegion`, use the `random` argument as given (it's already
   deterministically seeded from world-seed + region + your preset's string
   ID, per 1.5 — do not substitute `GameRandom.globalRandom`) to pick the
   exact tile inside that region (search for a valid spot with
   `findRandomBiomePresetTile`/a corner-check loop, or — if you want a
   *fully* fixed tile, not just a fixed region — skip the search and use a
   constant offset from `region.worldRegion.startTileX/startTileY`).
4. Call `presetsRegion.addPreset(this, x, y, size, boards, placeFn)` exactly
   as in 1.5; inside `placeFn`, call `WorldPreset.ensureRegionsAreGenerated`
   then `preset.applyToLevel(level, x, y)`.
5. Register once: `WorldPresetRegistry.registerPreset("mymod_uniquestructure", new MyUniqueWorldPreset());`
6. **"Recording that it was placed" is automatic** — you do not need your
   own flag. `LevelPresetsRegion` generation state is persisted per-world
   (`WorldPresetsRegion.saveGeneratedPresetsFile`), so `addToRegion` for
   that region genuinely never runs again for that save. If you additionally
   want a *queryable* record (e.g. to point a quest/waypoint at it, matching
   the vanilla `HumanMob.setHome(...)` + `canDespawn = false` idiom seen in
   `CaveHoboHomePreset`), store the tile coordinate yourself the moment
   `placeFn` runs — e.g. write it into a custom `LevelData`/`WorldEntity`
   field via your mod's own data component; that is a convenience for later
   lookup, not a requirement for uniqueness.

---

## 2. Walls

### 2.1 `WallObject` — registration

`level/gameObject/WallObject.java`. Constructor:
```java
public WallObject(String textureName, String outlineTextureName, Color mapColor, float toolTier, ToolType toolType)
```
Sets `isWall = true`, 32×32 collision, `itemCategory`/`craftingCategory` =
`{"objects","wallsanddoors"}`, `stackSize = 500`. The **registration helper**
that mods actually call (creates the wall object *and* its matching door
pair *and* a window variant, all at once) — real vanilla call, verbatim:
```java
int[] stoneWallIDs = WallObject.registerWallObjects(
    "stone", "stonewall", 0.0F, new Color(130, 139, 152), -1.0F, -1.0F);
// -> returns [wallID, doorID, doorID2, windowID]; registers stringIDs
//    "stonewall", "stonedoor"(+counter), "stonewindow"
```
Full overload chain (all bottom out in the 10-arg form):
```java
static int[] registerWallObjects(String stringIDPrefix, String textureName, String outlineTextureName,
    float toolTier, Color mapColor, ToolType toolType, float wallBrokerValue, float doorBrokerValue,
    boolean itemObtainable, boolean itemCountInStats)
```
Internally this does exactly:
```java
WallObject wallObject = new WallObject(textureName, outlineTextureName, mapColor, toolTier, toolType);
int wall = ObjectRegistry.registerObject(prefix + "wall", wallObject, wallBrokerValue, itemObtainable, itemCountInStats);
int[] doors = WallDoorObject.registerDoorPair(prefix + "door", wallObject, doorBrokerValue, itemObtainable, itemCountInStats);
int window = ObjectRegistry.registerObject(prefix + "window", new WallWindowObject(wallObject), wallBrokerValue, false, false);
```
`outlineTextureName` defaults to `"walloutlines"` (a shared, vanilla-provided
outline atlas) if you use the shorter overloads — you generally don't need
your own outline file. Texture is loaded as `objects/<textureName>` (an
optional `<textureName>_short` variant is tried first, for a
cave-ceiling-cropped variant — not required).

### 2.2 `objects/stonewall.png` (352×128) — decoded cell layout

Reverse-engineered from `WallObject.addWallDrawOptions` (16px auto-tile
blob), `WallWindowObject.addWallDrawOptions` (columns 4–5), and
`WallDoorObject.addDrawables` (32px-wide door-frame columns 3–10). All three
classes share one texture file (`this.wallObject.wallTexture`).

| Pixel X range | Cell grid | Content |
|---|---|---|
| 0–64 (cols 0–3, 16px cells, rows 0–7) | 4×8 @ 16px | Wall **auto-tile blob**: every corner/edge/fill combination the 3×3-neighbor algorithm needs (top edge, bottom edge with/without side, inner corners, etc.) — this is what makes adjacent same-type walls join into one continuous wall visually. When a wall tile is fully surrounded by the same wall on all 4 sides, the fast path pulls a fixed 32×32 block at pixel (16,16)–(48,48) instead (still inside this region). |
| 64–96 (cols 4–5, 16px cells, rows 0–7) | 2×8 @ 16px | **Window insert** graphic (`WallWindowObject`) — a 32-px-wide, up to 8×16=128-px-tall column pair, drawn either as a short "high window" (rows 0–1) when the wall run is straight horizontal, or a tall vertical strip (rows 2–7) otherwise. |
| 96–352 (8×32px columns, full 128px tall each) | cols 3,4,5,6,7,8,9,10 (@32px stride) | **Door frame** graphics (`WallDoorObject`), one 32×128 image per (rotation × open/closed): col 3 = rotation 0 closed, col 4 = rotation 0 open, col 5 = rot 1 closed, col 6 = rot 1 open, col 7 = rot 2 closed, col 8 = rot 2 open, col 9 = rot 3 closed, col 10 = rot 3 open. |

Total: 64 + 32 + 256 = **352px wide**, 128px tall — matches the reference
file exactly. **A new wall texture must reproduce this same 352×128 layout**
(auto-tile blob + window pair + 8 door-frame columns) if it's meant to pair
with the stock `WallDoorObject`/`WallWindowObject` door-and-window system via
`registerWallObjects`.

### 2.3 Doors

`registerWallObjects` already wires up a matching `WallDoorObject` pair for
you (this is the normal path — you don't hand-build a door class per wall).
`WallDoorObject extends DoorObject`, keeps a reference to its parent
`WallObject` and reads sprite frames straight out of the **wall's own**
352×128 texture (columns 3–10, table above) — there is no separate door
image file. A **freestanding** door not attached to one of your own walls
(rare) would instead use the plain `DoorObject` base directly.

---

## 3. Furniture / deco object classes

### 3.1 Base plumbing

`level/gameObject/furniture/FurnitureObject.java` — the base every "real
furniture" item extends:
```java
public class FurnitureObject extends GameObject implements RoomFurniture {
    public String furnitureType = null;         // "chair","table","lamp","carpet","candelabra",...
}
public interface RoomFurniture { String getFurnitureType(); }
```
Constructing one automatically does `roomProperties.add("furniture")` and
sets `itemCategory`/`craftingCategory = {"objects","furniture"}` (see §4 for
why `roomProperties`/`furnitureType` matter). Convention confirmed across
every furniture class read: **item category and crafting category are the
same two-string array**, and the second string is the specific furniture
craft group (`"furniture"`, `"lighting"`, `"decorations"`,
`"decorations","carpets"`, `"decorations","paintings"`,
`"fencesandgates"`, `"wallsanddoors"`, `"landscaping","masonry"`,
`"landscaping","plants"`, `"landscaping","tabledecorations"`).

### 3.2 Chairs — `ChairObject`

`level/gameObject/furniture/ChairObject.java`:
```java
public ChairObject(String textureName, ToolType toolType, Color mapColor, String... category)
public ChairObject(String textureName, Color mapColor, String... category)   // toolType = ALL
```
`extends FurnitureObject implements ObjectUsersObject, ChairObjectInterface`
— sets `furnitureType = "chair"`, wires up sit-down interaction via
`ChairObjectEntity` (`getNewObjectEntity` returns one), and a `TileLevelJob`
(`SitDownTileLevelJob`) so idle settlers/players path to it and sit. Texture
`objects/<textureName>.png`: **4 columns of 32px** (one per rotation 0–3),
height = whatever the art needs (`texture.getHeight()`, read dynamically) —
reference `oakchair.png` is **128×64** (4×32 wide, 64 tall). Real
registration: `registerObject("oakchair", new ChairObject("oakchair", oakMapColor, oakCategory), 5.0F, true)`.

### 3.3 Tables — `TableObject`

`level/gameObject/furniture/TableObject.java` — deliberately minimal:
```java
public TableObject(Rectangle collision, Color mapColor)   // furnitureType = "table"
```
`implements TableObjectInterface` (a marker interface other furniture, e.g.
`ChairObject.facesTable()`, checks via `instanceof` to know "is there a
table in front of this chair"). For a table with a big custom sprite sheet
and per-instance size options, vanilla uses `ModularTableObject` (registered
e.g. `registerObject("oakmodulartable", new ModularTableObject("oakmodulartable",
oakMapColor, oakCategory), 10.0F, true)`, reference texture 96×64) — plain
`TableObject` is the simpler base if you don't need modular resizing.

### 3.4 Lamps / streetlamps / candelabra — on/off + light

`level/gameObject/furniture/LampObject.java` is the general "lamp furniture"
base:
```java
public LampObject(String textureName, Rectangle collision, ToolType toolType, Color mapColor, float lightHue, float lightSat)
```
Sets `lightLevel = 150`, `roomProperties.add("lights")`, `furnitureType =
"lamp"`. Loads **two** textures: `objects/<name>.png` (lit) and
`objects/<name>_off.png` (unlit) — both same layout, N columns of 32px (one
per rotation). On/off state (`isActive(level,x,y)`) is driven by the
**wire/logic-gate network**: a lamp is "on" iff none of its tiles have an
active wire signal (`level.wireManager.isWireActiveAny(...)`) — i.e. wiring
a switch/lever to it and toggling the wire turns the light off, matching
vanilla's wired-lighting convention; `getLightLevel(...)` returns 0 while
inactive. `CandelabraObject extends LampObject` is the **candelabra /
tabletop-flame variant** — same constructor shape plus an animated flame
particle effect (`tickEffect`), collision shrunk to `Rectangle(4,4,24,24)`,
default category `{"objects","lighting"}`. Reference `oakcandelabra.png` /
`oakcandelabra_off.png` are both **128×64** (4 rotation columns × 64 tall).
`StreetlampObject` (`level/gameObject/StreetlampObject.java`, not a
`FurnitureObject`) is the freestanding **outdoor post lamp**: single texture
`objects/<stringID>.png`, no rotation columns — instead 2 **rows** of 32×96
(`sprite(0, active?0:1, 32, 96)`), i.e. on/off is row-selected, not a second
file. `DoubleStreetlampObject`/`DoubleStreetlamp2Object` are the same idea
with two lamp heads.

### 3.5 Wall-mounted light / deco — `WallTorchObject` (walllantern, 64×128)

`level/gameObject/WallTorchObject.java` is the base for anything that hangs
on a wall and needs the wall-decor attach/orientation logic (torches,
lanterns, garlands):
```java
public WallTorchObject()   // no-arg; sets lightLevel=150, lightHue=50, lightSat=0.2, roomProperties.add("lights"),
                            // validObjectLayers += ObjectLayerRegistry.WALL_DECOR
```
It auto-attaches to any adjacent wall/rock tile in one of 4 orientations —
hanging from a wall above (`sprite=0`), mounted on a side wall right/left
(`sprite=1`/`3`), or sitting on a support below (`sprite=2`) — and reads
`objects/walltorch.png` by default via `sprite(active?0:1, sprite, 32)`:
**2 columns (on/off) × 4 rows (the 4 attach orientations) of 32×32 = 64×128**,
matching the reference file exactly. `WallCandleObject extends
WallTorchObject` overrides only `loadTextures()` to load `objects/<its own
registered stringID>.png` instead of the shared `walltorch` art — this is
exactly how vanilla's `walllantern` is built:
```java
registerObject("walllantern", new WallCandleObject().setItemDroppedStringID("lantern"), 0.0F, false);
```
**This is the class to subclass for a "wall garland with colored lights"**:
extend `WallTorchObject` (or copy `WallCandleObject`'s one-method override),
override `loadTextures()` for your own 64×128 art, and set
`this.lightHue`/`this.lightSat`/`this.lightLevel` after `super()` for a
custom light color (these are plain public fields, not constructor params,
on the base class).

### 3.6 Carpets — two systems, both under `objects/carpets/*` or plain `objects/*`

- **`CarpetObject`** (`level/gameObject/furniture/CarpetObject.java`) — a
  fixed 2×2-tile rug placed as *one* item. Texture `objects/<name>.png`,
  **64×64** (2×2 grid of 32px cells). Registered via a helper that creates
  all 4 quadrant sub-objects for you:
  ```java
  static int[] registerCarpet(String stringID, String textureName, ToolType toolType, Color mapColor, float brokerValue)
  // -> registers stringID, stringID+"r", stringID+"d", stringID+"dr" (only the first is player-obtainable)
  ```
  Reference `carpets/bluecarpet.png` = 64×64, confirming the layout.
- **`ModularCarpetObject`** (`level/gameObject/ModularCarpetObject.java`,
  `implements RoomFurniture` directly, not via `FurnitureObject`) — an
  **auto-tiling floor-covering** you paint tile-by-tile like a wall run
  (grows/shrinks freely, not fixed-size). Loads from `objects/carpets/<name>.png`
  **plus a companion mask** `objects/carpets/<name>mask.png` (both loaded
  raw/pre-multiplied: `GameTexture.fromFile("objects/carpets/"+name, true)`).
  This is the `objects/carpets/*` convention referenced in the prompt;
  reference `bluecarpet.png`/`bluecarpetmask.png` are both 64×64.

### 3.7 Paintings — `PaintingObject`

`level/gameObject/PaintingObject.java`:
```java
public PaintingObject(Item.Rarity rarity)
```
Wall-decor layer only (`validObjectLayers += WALL_DECOR`), must attach to a
wall (`attachesToObject` requires `isWall && !isDoor` on the tile it faces),
`roomProperties.add("painting")`. Texture defaults to
`objects/paintings/<stringID>.png` (override via `this.texturePath`) —
**32×128, 4 rows of 32×32**, one row per rotation (0=south/low placement
`+8px` offset, 1=east, 2=north/high `-32px` offset, 3=west). Reference
`paintings/paintingapple.png` = 32×128, exact match.

> **Disputed, and it decides which side row gets which art.** For wall decor the
> rotation names *where the wall is*, and
> `WardenSpirePreset.WALL_BELOW/LEFT/ABOVE/RIGHT` — citing
> `PaintingObject.attachesToObject` — reads the side rows the other way round:
> `1` = wall **left**, `3` = wall **right**. Rows 0 and 2 are not in dispute.
> `gen_banner_painting` follows the preset, because the preset names the method
> it came from and this line does not. Whoever next has the decompile open:
> read `attachesToObject`, settle it here, and delete this box.

All four rows must also hold DIFFERENT art. The Skywatch Banner shipped with one
cell pasted four times and therefore did not react to being turned at all;
`tools/rotation_variety_audit.py` now fails on that. `LargePaintingObject`/
`LargePaintingObject2` are bigger multi-tile variants of the same idea, also
tagging `roomProperties.add("painting")` — used by the `ArtConnoisseurSettlerPersonality`
settler trait, which literally filters room objects by
`roomProperties.contains("painting")`.

### 3.8 Fences & gates

**`FenceObject`** (`level/gameObject/FenceObject.java`):
```java
public FenceObject(String textureName, Color mapColor, int collisionWidth, int collisionHeight[, int torchYOffset])
```
`isFence = true`, `implements FenceObjectInterface, TorchHolderInterface`
(so a `TorchObject`/`WallTorchObject` can be mounted on top of a fence
post). Texture `objects/<name>.png`, **5 columns of 32px, each spanning the
full texture height** (post / top-connector / top-cap / left-connector /
right-connector) — reference `woodfence.png` / `ironfence.png` = **160×64**
exactly (5×32 wide, 64 tall), confirming the layout. No static
`registerFence` helper exists — fences are registered directly:
```java
int ironFenceID = registerObject("ironfence", new FenceObject("ironfence", new Color(130,139,152), 12, 10, -26), 2.0F, true);
```
**`FenceGateObject`** (`level/gameObject/FenceGateObject.java`) `extends
DoorObject implements FenceObjectInterface` — its own, separate 6-column
texture (post/open/closed/leaf variants; not the same sheet as the plain
fence — reference `woodfencegate.png`/`ironfencegate.png` = **192×64**, i.e.
6×32). Paired to an already-registered fence via the real, verbatim vanilla
call:
```java
int ironFenceID = registerObject("ironfence", new FenceObject("ironfence", new Color(130, 139, 152), 12, 10, -26), 2.0F, true);
FenceGateObject.registerGatePair(ironFenceID, "ironfencegate", "ironfencegate", new Color(130, 139, 152), 12, 10, 4.0F);
```
(`registerGatePair(int fenceID, String stringIDPrefix, String textureName, Color mapColor, int collisionWidth, int collisionHeight, float brokerValue)`.)
**Vanilla already ships an `ironfence`/`ironfencegate` pair with exactly
this API** — an "iron fence + gate" mod piece can reuse this call verbatim
with new art.

### 3.9 Statues — `StatueObject`

`level/gameObject/StatueObject.java`:
```java
public StatueObject(String texturePath)                          // spriteCount defaults to 4
public StatueObject(String texturePath, int xOffset, int spriteCount)
```
Texture `objects/statues/<texturePath>.png`; frame width =
`texture.getWidth() / spriteCount`, and `rotation % (width/frameWidth)`
picks the column — so `spriteCount` must equal however many distinct
rotation frames your art actually has (not always 4). `xOffset` shifts the
draw position left, for art wider than one tile so it can be recentered.
Real vanilla examples: `new StatueObject("ravenstatue", 16, 1)` (a
single-pose 64×96 statue, `raven` theme already exists in vanilla — see
build plan), `new StatueObject("pigstatue", ...)` via a small
`PigStatueObject.registerPigStatue` helper (its `pigstatue.png` is 96×128 =
3 frames). `getLevelJobs` wires it into the "enjoy art" idle job, same as
paintings, and it gets the same `"beautyrec"` (beauty recreation) tooltip.

### 3.10 A simple static deco base (for a "dead crooked tree")

There is no vanilla `SingleTreeDecoObject`-style class simpler than a full
tree, but there are two good templates depending on how "alive" you want it:

- **`SingleRockObject`** (`level/gameObject/SingleRockObject.java`) is the
  cleanest example in the codebase of "static prop, one static texture,
  optional per-tile random Y-wobble, no real interaction beyond
  mine-for-loot": constructor takes `(RockObject type, String textureName,
  Color mapColor, int minDrop, int maxDrop, int placedDrop, String...
  category)`, texture `objects/<textureName>.png`, draws a seeded-per-tile
  random column (`drawRandom.seeded(getTileSeed(x,y)).nextInt(width/64)`)
  purely for cosmetic variety — a good pattern to imitate for a bespoke
  "just decoration" `GameObject` subclass (drop the rock-specific loot/type
  fields, keep the seeded-variant draw idiom if you want more than one look).
- **`TreeObject`** (`level/gameObject/TreeObject.java`) is the full
  chop-down tree (logs, sapling, leaf-particle system,
  `roomProperties.add("plant")`/`add("tree")`, `isTree = true`,
  `toolType = ToolType.AXE`). `DeadwoodTreeObject extends TreeObject` is
  **already the vanilla "dead tree" reskin** (same mechanics, different
  loot table — no sapling regrowth flavor text). For a purely decorative,
  non-choppable crooked dead tree, the least-effort correct choice is
  `DeadwoodTreeObject`/`TreeObject` with `logStringID = null,
  saplingStringID = null` (empty loot, but keeps the obstacle/`isTree`
  collision + chop animation) — or a bespoke `GameObject` styled after
  `SingleRockObject` if even the chopping should be removed entirely.
  `BurnedTreeObject extends TreeObject` shows the pattern for adding ambient
  particle effects (embers) on top of the same base, if "crooked and a bit
  ominous" wants a subtle particle flourish.
- **`BearRugObject`** (`level/gameObject/happinessObject/BearRugObject.java`)
  is the closest vanilla analogue to a **floor-level "cat bed"-style prop**:
  `extends StaticMultiObject implements HappinessObject`, 2×2-tile static
  multi-object, single texture, registered as 4 sub-objects the same way
  `CarpetObject` is:
  ```java
  static int[] registerBearRug()   // registers "bearrug","bearrug2","bearrug3","bearrug4"
  ```
  A "cat bed" that should be 1×1 and *not* need `HappinessObject` (a vanilla
  pet-taming-adjacent interface) is simpler still as a plain `FurnitureObject`
  or bare `GameObject` with a 32×32 texture and no rotation columns — use
  `BearRugObject` only if you also want it to participate in the
  happiness-object tooltip/mechanic.

### 3.11 Item registration + broker value + crafting category conventions

Canonical registration call (`engine/registries/ObjectRegistry.java`):
```java
static int registerObject(String stringID, GameObject object, float itemBrokerValue, boolean itemObtainable)
static int registerObject(String stringID, GameObject object, float itemBrokerValue, boolean itemObtainable,
                           boolean itemCountInStats, String... isObtainedByOtherItemStringIDs)
```
`itemBrokerValue` is the sell price the shop broker pays (`-1.0F` seen
everywhere in wall registrations means "compute automatically from
materials/tier" rather than a hardcoded price — used whenever the object
doesn't need a custom price). `itemObtainable` gates whether placing it
generates a matching inventory item at all. Every furniture-ish class
constructor calls `setItemCategory(String[])` **and** `setCraftingCategory(String[])`
with the *same* array — observed category leaves: `{"objects","furniture"}`,
`{"objects","lighting"}`, `{"objects","decorations"}`,
`{"objects","decorations","carpets"}`, `{"objects","decorations","paintings"}`,
`{"objects","fencesandgates"}`, `{"objects","wallsanddoors"}`,
`{"objects","landscaping","masonry"}` (statues), `{"objects","landscaping","plants"}`
(trees), `{"objects","landscaping","rocksandores"}`,
`{"objects","landscaping","tabledecorations"}`. Use whichever leaf matches
what you're building so the in-game crafting-menu tab and the creative/build
palette group your new item next to its vanilla siblings.

---

## 4. Room / settlement relevance

`SettlementRoom` (`level/maps/levelData/settlementData/SettlementRoom.java`)
is what scans a settler's room and turns "which objects/tiles are in it"
into a happiness score. It reads from **every object on every layer, and the
floor tile**, in two independent, both-additive ways:

```java
object.roomProperties.forEach(s -> this.addRoomProperty(s, 1));      // any GameObject
tile.roomProperties.forEach(s -> this.addRoomProperty(s, 1));         // the GameTile
if (object instanceof RoomFurniture) {
    String type = ((RoomFurniture) object).getFurnitureType();
    if (type != null) this.addFurnitureType(type, 1);
}
if (!tile.isFloor) this.addRoomProperty("outsidefloor", 1);
```

- **`roomProperties`** — a plain `HashSet<String>` field that exists on both
  `GameObject` and `GameTile`. Every matching tag across the whole room is
  **counted** (`getRoomProperty(String) -> int`), and specific counts are
  read directly by settler-happiness logic, e.g.
  `getRoomProperty("lights") <= 0` triggers a "missing lights" unhappy
  thought (`calculateHappinessModifiers()`), `getRoomProperty("outsidefloor")
  > 0` triggers a "missing floor" thought, and personality traits read
  specific tags too (`EcologistSettlerPersonality` reads `"plant"`,
  `AudiophileSettlerPersonality` reads `"musicplayer"`,
  `ArtConnoisseurSettlerPersonality` reads `"painting"`). Observed vocabulary
  (grepped across all of `necesse/level/gameObject/**`): `"furniture"`
  (every `FurnitureObject`), `"lights"` (any light source — lamps, torches,
  candelabras, streetlamps, brazier, LED panel, firefly jar), `"bed"`,
  `"painting"`, `"plant"` / `"flower"` / `"tree"` (separately, plants tag
  both `"plant"` and their more specific tag), `"metalwork"` (forge/anvil),
  `"potionwork"` (alchemy table), `"musicplayer"`, `"goldfurniture"`
  (gold-tier reskins, added as an *extra* tag on top of normal furniture
  tags), `"happinessobject"`, `"outsidefloor"` (synthetic, added by the room
  scanner itself, not by any object).
- **`furnitureType` / `RoomFurniture` interface** — a *second*, separate
  counting bucket, one string per furniture piece (`"chair"`, `"table"`,
  `"lamp"`, `"candelabra"`, `"carpet"`, `"bearrug"`-style pieces via
  `HappinessObject` are outside this system). `getFurnitureScore()` sums
  `count^0.44` **per distinct type**, not a flat total — i.e. **variety of
  furniture types is rewarded far more than stacking copies of one type**
  (5 chairs and nothing else scores much lower than 1 chair + 1 table + 1
  lamp + 1 candelabra + 1 carpet).

**Practical guidance for new furniture**: extend `FurnitureObject` (gets
`"furniture"` free) and set a **distinct, sensible `furnitureType`** string
per new piece so it counts as its own variety bucket rather than colliding
with an existing vanilla type (unless you *want* it to be interchangeable
with, say, chairs). Any light-emitting piece should
`roomProperties.add("lights")` (copy `LampObject`/`WallTorchObject`'s
constructor line) so it satisfies the "needs lighting" happiness check. A
new floor tile should keep `isFloor = true` so rooms don't get penalized
with the `"outsidefloor"` unhappy thought.

---

## 5. Critters (friendly ambient mobs)

### 5.1 Base class

`entity/mobs/friendly/critters/CritterMob.java` `extends FriendlyMob`:
```java
public CritterMob()            // health = 10
public CritterMob(int health)
```
Sets `isCritter = true`, **`canDespawn = true`** (inherited `Mob` field),
default AI `new BehaviourTreeAI<>(this, new CritterAI<>())` in `init()`.
Despawn logic (`canDespawn()` override):
```java
public boolean canDespawn() {
    return !this.canDespawn ? false
        : GameUtils.streamServerClients(getLevel())
              .noneMatch(c -> getDistance(c.playerMob) < CRITTER_SPAWN_AREA.maxSpawnDistance + 100);
}
```
i.e. a critter despawns only if **(a)** its own `canDespawn` field is still
`true`, **and (b)** no player is within roughly "spawn distance + 100px".
Setting the field `this.canDespawn = false` short-circuits this to "never
despawn" — the exact same field/idiom `CaveHoboHomePreset` uses to pin its
settler NPC in place.

Simplest concrete example: `RabbitMob extends CritterMob`
(`entity/mobs/friendly/critters/RabbitMob.java`) — no-arg constructor sets
speed/collision/hitbox/selectBox, one `LootTable`, one texture
(`MobRegistry.Textures.rabbit`), `checkSpawnLocation` restricted to
grass tiles. This is the template to copy for a new ambient critter.

### 5.2 AI: default wander vs. home-tethered

- **`CritterAI<T>`** (`entity/mobs/ai/behaviourTree/trees/CritterAI.java`) —
  the default assigned in `CritterMob.init()`: flee-when-hurt +
  wander-freely, no concept of "home".
- **`HomesickCritterAI<T extends CritterMob>`** (`entity/mobs/ai/behaviourTree/trees/HomesickCritterAI.java`)
  — **exactly** the "stay near a home point" behavior asked for:
  ```java
  public HomesickCritterAI(Mob mob)                                             // homeTile = mob's spawn tile
  public HomesickCritterAI(Mob mob, AINode<T> runner, WandererAINode<T> wanderer)
  public Point homeTile;
  public float distanceBeforeHomesick = 96.0F;   // 3 tiles
  ```
  It's a `SelectorAINode` with 3 children tried in order: the flee/"runner"
  node, a `WanderHomeAtConditionAINode` that walks the mob straight back to
  `homeTile` whenever `mob.getDistance(homeTile) > distanceBeforeHomesick`,
  and otherwise a normal `WandererAINode` for local roaming. `homeTile` is
  captured from the mob's **current position at AI-construction time**
  (i.e. wherever it was when `init()` ran, normally right after spawning) —
  it is a public field, so it can also be overwritten immediately after
  construction to pin the mob to a designer-chosen point instead of its
  spawn tile. **Real vanilla usage**: `FireflyMob` (`entity/mobs/friendly/critters/flyingbugs/FireflyMob.java`)
  sets `this.ai = new BehaviourTreeAI<>(this, new HomesickCritterAI<>(this));`
  — fireflies drift locally but always return to where they appeared.

**Recipe for "friendly critter that stays near a home point instead of
despawning"**: `extends CritterMob`, in `init()` call
`super.init(); this.ai = new BehaviourTreeAI<>(this, new HomesickCritterAI<>(this));`
(optionally overwrite `((HomesickCritterAI)...).homeTile` to a fixed point,
e.g. one recorded by a preset via §1.7's placement hook), and set
`this.canDespawn = false` either in the constructor or right after spawning
it (mirroring `CaveHoboHomePreset`'s settler-pinning idiom).

### 5.3 Spawning: registration + biome spawn tables

```java
MobRegistry.registerMob("rabbit", RabbitMob.class, true);   // (stringID, class, countKillStat)
```
Ambient/critter spawn *rates* are separate from monster spawning and driven
by a `MobSpawnTable`, resolved per-biome via an overridable method:
```java
// Biome.java
public MobSpawnTable getCritterSpawnTable(Level level);   // default returns a shared table
public static MobSpawnTable defaultSurfaceCritters = new MobSpawnTable()
    .add(100, "rabbit").add(80, "squirrel").add(50, "bird") ... ;
public static MobSpawnTable defaultCaveCritters = new MobSpawnTable()
    .add(100, "spider").add(100, "mouse") ... ;
```
Concrete biomes build their own **public static** table by including the
shared default and layering biome-specific extras, e.g. (verbatim,
`ForestBiome.java`):
```java
public static MobSpawnTable surfaceCritters = new MobSpawnTable().include(Biome.defaultSurfaceCritters).add(30, "butterfly");
public static MobSpawnTable caveCritters     = new MobSpawnTable().include(Biome.defaultCaveCritters).add(100, "stonecaveling");
```
Because these are `public static` fields, a mod can add its own critter to
any specific biome's ambient pool directly, without touching world-gen code:
```java
ForestBiome.surfaceCritters.add(40, "mymod_firefly_variant");
```
(`MobSpawnTable.add(int tickets, String mobStringID)` — weighted "tickets"
out of the table's running total; higher = more common relative to the
other entries already in that table.) Actual spawn rolls happen
per-`EntityManager` tick via `client.getCritterSpawnTable(level, x, y)` →
`level.getBiome(x,y).getCritterSpawnTable(level)`, so registering into the
right biome's static table is the complete, sufficient integration step.

---

## 6. Floor tiles

### 6.1 `GameTile(boolean isFloor)` and registration

`level/gameTile/GameTile.java`:
```java
public GameTile(boolean isFloor)
```
Setting `isFloor = true` sets `smartMinePriority = isFloor`,
`tileHealth = 50` (vs 100 for non-floor), and auto-assigns item category
`{"tiles","floors"}` (vs `{"tiles","liquids"}` for `LiquidTile` or
`{"tiles","terrain"}` for other `TerrainSplatterTile`s) — this is the one
line that fundamentally distinguishes a floor from ground/terrain. Floors
also matter to settlement rooms directly: `!tile.isFloor` inside a defined
room adds the `"outsidefloor"` roomProperty, which triggers an unhappy
settler thought (§4). Registration mirrors objects/tiles generally:
```java
static int registerTile(String stringID, GameTile tile, float itemBrokerValue, boolean itemObtainable)
static int registerTile(String stringID, GameTile tile, float itemBrokerValue, boolean itemObtainable,
                         boolean itemCountInStats, String... isObtainedByOtherItemStringIDs)
```

### 6.2 Texture convention — legacy vs. modern "`_splat`"

Floor tiles (like most non-liquid tiles) `extend TerrainSplatterTile`
(`level/gameTile/TerrainSplatterTile.java`):
```java
public TerrainSplatterTile(boolean isFloor, String terrainTextureName[, String alphaMaskTextureName])
public abstract int getTerrainPriority();                 // e.g. 400 for floors (PRIORITY_FLOOR)
public Point getTerrainSprite(GameTextureSection terrainTexture, Level level, int tileX, int tileY);  // override point
```
At texture-load time it **auto-detects** which of two systems your art
uses, by trying to load `tiles/<name>_splat.png` first:

- **Modern (`tiles/<name>_splat.png` present)** — a sheet organized as
  224×96-px **sections** (7 columns × 3 rows of 32px cells): columns 0–2/4–6
  are directional edge-blend pieces, and the 4 fixed cells at (3,0),(4,0),
  (5,0),(6,0) are alternate **full-tile** looks, chosen per-tile via a
  seeded random pick (`NEW_FULL_TILE_SPRITES`) for subtle non-repeating
  variety, purely automatically — **your `getTerrainSprite()` override is
  bypassed entirely** in this mode. Multiple 224-px-wide **frames**
  side-by-side animate (e.g. lava); multiple 96-px-tall **sections** stacked
  vertically give extra random full-tile variants (reference
  `deadwoodfloor_splat.png` = 224×192 = 2 variants; `ravenfloor_splat.png` =
  224×576 = 6 variants). This is the modern, best-looking, best-blending
  choice for almost any new floor.
- **Legacy (no `_splat` file — plain `tiles/<name>.png` + the shared
  `tiles/splattingmask.png`, 64×64)** — the engine auto-generates every
  edge-blend combination at load time by multiplying your plain texture's
  grid cells against the mask. **Your `getTerrainSprite()` override is what
  actually runs** here, per real tile, to choose *which* grid cell of your
  plain texture is the "full tile" look for that coordinate.

### 6.3 The floor tile subclasses that matter

All three below are real, registered vanilla classes, all `extends
TerrainSplatterTile(true, name)`, differing only in `getTerrainSprite`:

| Class | `getTerrainSprite` rule | Visual result |
|---|---|---|
| `SimpleFloorTile` | random row in **column 0** (`drawRandom.seeded(tileSeed).nextInt(height/32)`) | Uniform floor with subtle **random** per-tile variation, no visible pattern. Used for `woodfloor`, `stonefloor`, **`deadwoodfloor`**. |
| `SimpleTiledFloorTile` | `(tileX % (width/32), tileY % (height/32))` — absolute **world-coordinate modulo** | A literal **repeating grid pattern** locked to world position — this is what produces a true checkerboard when the source art alternates two colors across a 2×2 (or larger) grid. Used for `dryadfloor`, `willowfloor`, `palmfloor`. |
| `DungeonFloorTile` | identical rule to `SimpleFloorTile` (random column-0 row) | Same "random variant" look, just its own class (`dungeonfloor`, toolTier 1.0). |

**Important interaction**: `deadwoodfloor` and `dryadfloor` are both
registered with `-1.0F`-style stock calls **and both ship a `_splat.png`
file** (confirmed: `registerTile("deadwoodfloor", new
SimpleFloorTile("deadwoodfloor", color), ...)`, and `tiles/deadwoodfloor_splat.png`
exists). Because the `_splat` file is present, the **modern system wins**
and their `getTerrainSprite` override never actually fires in-game — it's
purely a legacy fallback. This means:

- **If you want the "modern" random-full-tile-variant look with best edge
  blending** (recommended default for any new plain floor, including a
  gloomwood-style reskin): pick `SimpleFloorTile` (matches the exact
  precedent of `deadwoodfloor`) and ship a `_splat.png` — `getTerrainSprite`
  barely matters, but keep the class for correctness/back-compat.
- **If you specifically want a coordinate-locked checkerboard** (the
  "checkered marble floor" ask): you must use `SimpleTiledFloorTile` **and
  deliberately NOT ship a `_splat.png`** — only a plain `tiles/<name>.png`
  grid (e.g. 64×64 for a simple 2×2 alternating pattern). Shipping a
  `_splat` file for this one would silently defeat the checkerboard (the
  engine would pick a random variant per tile instead of the
  coordinate-locked one). You still get automatic edge-blending against
  neighboring non-floor tiles either way (via the shared
  `tiles/splattingmask.png`), just via the older/simpler blend algorithm.

---

## 7. Build plan hints

| Mod piece | Vanilla base class | Texture size / files to author | Notes |
|---|---|---|---|
| Skystone brick wall (+ door) | `WallObject` via `WallObject.registerWallObjects("skystone", "skystonebrickwall", toolTier, mapColor, toolType, wallBroker, doorBroker)` | `objects/skystonebrickwall.png` — **352×128**, laid out exactly per §2.2 (64px auto-tile blob + 32px window-pair + 8×32px door frames) | Door + window are generated for free by `registerWallObjects`; no separate door class needed. |
| Gloomwood-style floor | `SimpleFloorTile("gloomwoodfloor", mapColor)` via `TileRegistry.registerTile` | `tiles/gloomwoodfloor_splat.png` — **224×(96×N)**, N = number of random full-tile variants (copy `deadwoodfloor_splat.png`'s 224×192/N=2 as a size template) | Exact precedent: vanilla's own `deadwoodfloor` uses this identical class + modern splat file. |
| Checkered marble floor | `SimpleTiledFloorTile("marblecheckertile", mapColor)` | Plain `tiles/marblecheckertile.png` only — **e.g. 64×64** (2×2 grid, alternating light/dark 32px cells); **do not** ship a `_splat` file | See §6.3 — a `_splat` file would override the coordinate-locked pattern with random variants. |
| Iron fence + gate | `FenceObject` + `FenceGateObject.registerGatePair(fenceID, ...)` | `objects/<name>fence.png` **160×64** (5×32 cols); `objects/<name>fencegate.png` **192×64** (6×32 cols) | Vanilla already ships this exact pair (`ironfence`/`ironfencegate`) with this exact registration call — safe to copy verbatim with new art/name. |
| Candelabra streetlamp variant | `CandelabraObject extends LampObject` | `objects/<name>candelabra.png` + `objects/<name>candelabra_off.png`, both **128×64** (4 rotation cols × 64 tall) | Direct precedent: every wood-type candelabra (`oakcandelabra.png` etc.) uses exactly this size/pair. |
| Wall garland with colored lights | `WallTorchObject` subclass (copy `WallCandleObject`'s pattern: override `loadTextures()` only) | `objects/<name>.png` — **64×128** (2 cols on/off × 4 rows attach-orientation) | Set `this.lightHue`/`this.lightSat` after `super()` for a custom glow color; `roomProperties.add("lights")` is inherited free. |
| Raven statue | `StatueObject` | `objects/statues/<name>.png` — size = `frameWidth × spriteCount` columns, e.g. vanilla's own `ravenstatue.png` is **64×96** with `spriteCount=1` (single static pose) | Verbatim vanilla precedent: `new StatueObject("ravenstatue", 16, 1)` — vanilla already has a raven statue asset/registration to mirror exactly. |
| Dead crooked tree deco | `DeadwoodTreeObject`/`TreeObject` with `logStringID=null, saplingStringID=null` (keep chop/obstacle feel), or a bespoke `GameObject` styled on `SingleRockObject` for zero interaction | `objects/<name>.png` full tree sprite (+ optional `<name>roots.png` if imitating `TreeStumpObject`'s two-layer draw) | Use `TreeObject` if it should block movement and be visually consistent with other trees; use a bare `GameObject` if it must be walk-through pure decoration. |
| Cat-bed-like deco | Plain `GameObject` or `FurnitureObject` (1×1, single 32×32 texture, `furnitureType="petbed"`), or `StaticMultiObject`-based like `BearRugObject` if 2×2 | `objects/<name>.png` — **32×32** for a simple 1-tile version, or **64×64** (2×2) if following `BearRugObject`'s exact pattern | Only reach for `BearRugObject`'s `HappinessObject` interface if you specifically want the vanilla "happiness object" tooltip/mechanic; otherwise a plain object is simpler and still counts toward room `"furniture"` scoring via `FurnitureObject`. |

**Unique structure placement** (tying back to §1.7): author the structure as
a `Preset`, wrap it in a `WorldPreset` gated to one fixed
`(worldPresetRegionX, worldPresetRegionY, LevelIdentifier)` triple, register
via `WorldPresetRegistry.registerPreset(...)` — this gives a single,
seed-deterministic, "generated exactly once and never again" placement with
no extra bookkeeping required.
