# Technical learnings — verified Necesse 1.3.2 behaviour

Shared memory so agents stop rediscovering the same APIs and the same bugs.

**Only verified behaviour goes here.** Each entry says how it was established:

- **[jar]** — read from the decompiled game sources. True about the code.
- **[run]** — observed executing, on a real server or in a headless harness.
- **[game]** — observed in an actual play session.
- **[hypothesis]** — believed but not yet proven. Must be resolved or deleted.

Decompiled sources live at `$NECESSE_GAME_DIR/../decompiled`, the sprite dump
beside it.

---

## Client rendering is invisible to the server integration test

**[run]** `scripts/integration_test.sh` boots a real dedicated server, generates
both dimensions, restarts on the same world and asserts persistence. It cannot
catch anything in a `draw`/`addDrawables`/texture path, because a dedicated
server never loads textures or renders. The Marble Checker crash below passed
every server test while making saves unloadable.

`scripts/tile_sprite_check.sh` closes part of that gap for tiles by running the
sprite-index arithmetic headlessly against the real classes. Anything else
client-side is still only covered by actually playing.

## Marble Checker: `ArrayIndexOutOfBoundsException` at TerrainSplatterTile:120

**[game]** Placing the tile crashed the client; the tile persisted, so the save
could not be reopened.

**[jar]** `SimpleTiledFloorTile.getTerrainSprite` picks its cell with:

```java
int spriteX = tileX % (terrainTexture.getWidth() / 32);
int spriteY = tileY % (terrainTexture.getHeight() / 32);
```

Java's `%` keeps the sign of the *dividend*. Necesse worlds are centred on
(0, 0), so most Surface tiles have a negative coordinate, and any negative odd
`tileX` yields `-1`. `TerrainSplatterTile.getSplattingTexture` line 120 then
evaluates `splattingTextures[-1][...]`.

**[jar]** Why vanilla never trips it: `dryadfloor`, `willowfloor` and
`palmfloor` are the *only* `SimpleTiledFloorTile` users in the game, and all
three ship a 224×192 `_splat` atlas. A `_splat` sets
`isUsingNewTerrainSplatting`, which takes the *other* branch of
`getSplattingTexture` — the branch that picks a section by seeded random and
never calls `getTerrainSprite` at all. The vanilla bug is real but unreachable.
Our checker ships without a `_splat` on purpose (an atlas randomises cells per
tile, which would destroy a checkerboard), which made it the only tile in the
game to reach the legacy path.

**[run]** Fix: `stairwaytoheaven.tiles.CheckerFloorTile` overrides
`getTerrainSprite` with `Math.floorMod`. Over a −600..600 sweep the vanilla
implementation goes out of bounds 630,600 times (43.7% of tiles, exactly the
predicted fraction) and the fixed one zero times, with the checkerboard still
anchored to world coordinates. Commit `ca2ddad`.

The tile's stringID, registration, tile ID and texture were all left untouched,
which is what lets an already-persisted tile keep loading.

**The general rule:** a `TerrainSplatterTile` whose texture has no `_splat`
sibling takes the legacy path, where `getTerrainSprite` is live code and its
result indexes a fixed-size array. Any sprite index derived from raw tile
coordinates must use `Math.floorMod`. `SimpleFloorTile` is safe by
construction — its index comes from `seeded(...).nextInt(...)`, never negative.

**[run]** As of `ca2ddad`, `marblechecker.png` is the only tile texture in the
mod without a `_splat` sibling.

## `_splat` atlas format

**[jar]** Width is `224 * frames`, height is `96 * variants`. Columns 3–6 of a
row are the four full-tile variants (`NEW_FULL_TILE_SPRITES`); the rest are
transition blends. Frames animate as a **ping-pong**:

```java
frame = GameUtils.getAnim(localTime, frames * 2 - 2, frames * 400);
if (frame >= frames) frame = frames * 2 - frame - 2;
```

So 8 frames play 0,1…7,6…1 over 3.2 s. Adjacent-pair smoothness is what
matters; there is no 7→0 wrap to make seamless.

## Settlers and `HumanShop`

**[jar]** `HumanShop`'s only constructor is
`(int nonSettlerHealth, int settlerHealth, String settlerStringID)`. The Elder
passes `(500, 500, "elder")`.

**[run]** Registering the mob is not enough. `HumanMob.getSettler()` resolves
`settlerStringID` through `SettlerRegistry`, and `LevelSettler`'s constructor
runs `Objects.requireNonNull` on the result. Without a registered `Settler`,
the vanilla recruit path answers "not a settler" and the NPC can never take a
bed. Verified by `/skyreachstatus`, which reported
`wardensettler=NOT REGISTERED mobRegistered=true` before the fix and
`wardensettler=WardenSettler` after.

**[jar]** `Settler` objects must be constructed while `SettlerRegistry` is open
— i.e. during `init()`. `Settler.onSettlerRegistryClosed()` then validates that
`mobStringID` resolves to a mob implementing `SettlerMob`, so a bad
registration fails the server boot rather than failing silently.

**[jar]** `Settler.isPartOfCompleteHost` defaults to `true`, and vanilla's
COMPLETE_HOST achievement requires one of every settler in the settlement. A
modded settler must set it to `false` or installing the mod makes that
achievement unreachable.

**[jar]** `HumanShop.getRecruitItems(client)` returns `null` by default, which
leaves the recruit button permanently dead. Returning `Collections.emptyList()`
is vanilla's idiom for a *free* recruit — `TraderHumanMob` uses it after being
freed from a trap.

**[jar]** `Settler.loadTextures()` is called from `GameResources`, client only,
and tries `mobs/icons/<id>human`, then `mobs/icons/<id>`, then
`settlers/<id>`.

## `Waystone.findTeleportLocation` returns PIXEL coordinates

**[jar]** It builds its candidates internally as `tile * 32 + 16`, and vanilla
call sites (`AbandonedMineshaftPreset`, `WaystoneObject`) pass the returned
Point straight into `addMob`. Converting again places the mob 32× too far out —
this shipped once and dropped a settler thousands of tiles into the wilderness.

## Critters: `canDespawn` does double duty

**[jar]** `CritterMob.shouldSave()` is `shouldSave && !canDespawn()`. So
`canDespawn = false` is not only what stops distance despawn — it is also what
makes the mob save-persistent. Flipping it back to `true` would delete the cats
from the save while `SkywatchQuestData.catsSpawned` stays `true`, so they would
never respawn.

**[jar]** `canTakeDamage() == false` gates every damage entry point in `Mob`:
`isHit`, `addHealth` and `setHealth` all check it, and `setHealth` only allows
increases when it is false.

**[run]** Both together are asserted on every integration-test run: after a
server restart on the same world, the Warden and both cats are still present.
The NPC count forces the spire and both lair regions into memory first —
without that it measures which regions happen to be streamed in, and reported a
missing cat purely because a lair 60 tiles out had not loaded.

## `SkyOrigin` range

**[run]** `compute()` uses `mix(...) % 385 - 192` per axis. Java's `%` again:
the real range is **−576..+192**, not the symmetric ±192 the arithmetic reads
like. This is harmless (the hub clamp guarantees land, and the level streams in
every direction) and is now part of the world contract — correcting the
arithmetic would move the spire in every existing save. Documented in the class.
**Do not "fix" it.**

## Region and level APIs

**[run]** Writing terrain during generation: `region.tileLayer.setTileByRegion`,
`region.biomeLayer.setBiomeByRegion(rx, ry, biomeID)`,
`region.objectLayer.setObjectByRegion(ObjectLayerRegistry.BASE_LAYER, rx, ry, id)`.
Coordinates are region-local; add `region.tileXOffset` / `tileYOffset` for world
coordinates.

**[run]** Multi-tile objects must have every tile written or
`Region.checkGenerationValid` removes the torso. 2×1 crystal clusters need both
the base and its `"r"` counterpart.

**[run]** `level.regionManager.ensureTileIsLoaded(x, y)` before reading or
writing a tile outside the loaded set. Anything that counts entities without
this is measuring region streaming, not world state.

## Mod lifecycle

**[run]** `@ModEntry` order: `init()` for registries (they close afterwards —
anything registry-constructed must happen here), `initResources()` for client
textures only, `postInit()` for recipes, `WorldGenerator` and commands.

**[run]** Registering an object that auto-creates its item and then also calling
`ItemRegistry.registerItem` for the same stringID crashes the server with
`Tried to register duplicate Item`. Grass and plant objects auto-create items.

**[run]** `new MatItem(...).setItemCategory("materials", "logs")` — the category
must exist in vanilla. `materials.wood` does not.

## Build

**[run]** Gradle 9.7.1 wrapper, checksum-pinned; JDK 17–25. Gradle 8.10.2 fails
on JDK 24+ with `Could not initialize class org.codehaus.groovy.runtime.InvokerHelper`.
`NECESSE_GAME_DIR` must point at a directory containing `Server.jar`.

**[run]** The game's bundled JRE has no compiler module, so a single-file Java
source launch against it fails with `Module jdk.compiler not in boot Layer`.
Compile with the toolchain `javac` and run separately — see
`scripts/tile_sprite_check.sh`.

## Asset generator

**[run]** Generation is deterministic: regenerating any function reproduces the
shipped PNG byte-for-byte. That property is the review gate — after an art
change, regenerate into a temp directory and `cmp` against
`src/main/resources/`. It also detects collateral damage, because shared helper
functions will change unrelated sprites.

**[run]** Change the generator, never the PNG. A hand-edited PNG is silently
reverted by the next regeneration. A generator function was once overwritten by
calling a stale legacy generator, which replaced a 64×128 wall-light sheet with
a 64×32 one — always check which function `generate_assets.py` actually calls.

**[run]** `tools/size_audit.py` compares each sprite's opaque mass against a
vanilla analogue and must report 0 flags. It is a floor, not a target: it only
sees sprites that have an entry, and passing it says nothing about whether a
sprite reads in game. See `docs/ART_DIRECTION.md`.

## Object item icons resolve to `items/<stringID>`

**[jar]** `GameObject.generateItemTexture()` is `GameTexture.fromFile("items/" + getStringID())`.
`TreeObject` and `TreeSaplingObject` do **not** override it, so any tree or
sapling registered with `createItem=true` needs an `items/<id>.png` or it shows
the engine's error texture. `RockObject` and `RockOreObject` DO override it
(they resolve through their rock/ore texture name instead), so ore nodes are
safe without their own item file.

**[jar]** Vanilla ships a sapling's item icon as a byte-identical copy of its
object sprite (`objects/birchsapling.png` == `items/birchsapling.png`), but
draws a tree's item icon as its own compact 32x32 glyph rather than shrinking
the 256x512 object sheet.

**[run]** `tools/locale_audit.py` catches missing display names but NOT missing
item textures — those are a separate class of "internal detail reaches the
player" bug. There is currently no automated gate for them.

## Placement is not a sprite problem

**[game]** Rocks that came out of an independent per-tile probability roll read
in game as "individual rectangular tombstones, evenly scattered". An
independent roll is statistically even by definition, so no tuning of its
chance could produce a formation — it needed a different mechanism.

**[run]** The replacement is a lattice formation field (`SkyTerrainPainter.outcropAt`):
lattice cells, hashed sites, elongated rotated lobes plus an offset second
lobe, a noise-wobbled boundary and an apron of scree. Reading only the 3x3
cells around a tile keeps cost constant and keeps region borders seamless.
Tuned against rendered maps: the first attempt covered 39% of the world in
rock. Judge such a field at **screen scale** (roughly 60x40 tiles) — a 300-tile
overview makes a perfectly good 6-tile outcrop look like a speck and will
mislead you into over-tuning.

## A passing size audit does not mean the sprite works

**[game]** The Galehound was raised from 0.43 to 0.80 of a vanilla boar's mass,
passed `tools/size_audit.py`, and the player then played the build and called it
"a grey sausage". The redesign that fixed it came out at 0.78 — slightly *less*
mass, much better sprite. What was missing was silhouette: a waist, a head held
clear of the back line, legs that change pose.

**[run]** Side-view paw shapes do not transfer to head-on and rear frames. A
5px horizontal paw block with a forward toe nub reads as a detached square
furniture foot when the animal faces the camera; those frames need a compact
rounded stub, and a head-on animal should simply not show its rear paws.
