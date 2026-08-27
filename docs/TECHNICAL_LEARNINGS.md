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

## RockObject variant + geometry contract (v0.6 rock family)

**[jar]** `RockObject` supports ANY variant count with zero code changes:
`variants = rockTexture.getWidth() / 32`, picked per tile via
`oreTextureRandom.seeded(getTileSeed(x, y) * 4621).nextInt(variants)`, then
`* 2` for the 16px half-column. Verified by disassembling `RockObject`
(`lambda$addRockDrawables$2`) in Server.jar 1.3.2 with `javap` — on this Mac
the `$NECESSE_GAME_DIR/../decompiled` tree documented above does not exist;
`javap -c` on the classes extracted from `Server.jar` is the working method
here. Vanilla `rock.png` ships 4 variants, `caverock.png` 8; we shipped 2
(the playtest's "same two shapes"), now 8 for Skystone / 6 for Veilrock.

**[jar]** The draw geometry is NOT one 32x32 tile: top-cap rows 0/5 draw at
`drawY - 16` (16px ABOVE the collision tile), and when the tile below is not
rock the face draws as a stacked PAIR — row 3/8 (upper 16px, at `drawY`) plus
row 4/9 (lower 16px, at `drawY + 16`, lit from below). A lone rock tile is
therefore ~64px of visible sprite over a 32px footprint. Art consequence: the
bottom 6px of rows 4/9 are the rock's ground line and rows 3/8 sit directly
on top of them.

**[jar]** Vanilla's "rock shadow" is a baked soft-alpha skirt, not a cast
shadow: `rock.png` rows 9/4 fade alpha 195 → 195 → 113 → 78 → 55 → 29 over
their bottom 6px with NO bottom outline (plus faint ~133-alpha AO under the
top lip). The old mod sheet had zero semi-transparent pixels and filled the
face rows ~86% with the deep ramp tone — that opaque dark band was the
playtest's "far too long and dark" shadow. The v0.6 sheet copies the vanilla
fade (shadow tone (18,32,32)) and fills faces base-dominant.

## CrystalClusterObject variant contract

**[jar]** `CrystalClusterObject.addDrawables` picks
`variant = drawRandom.seeded(getTileSeed(x, y)).nextInt(texture.getWidth() / 64)`
and draws each 32px half of the 64px variant bottom-anchored
(`drawY - height + 32`), so sheet height is free (vanilla `amethystcluster`
is 4 variants × 64×64). We shipped 2 variants × 48 tall; the v0.6 Storm
Shards rebuild is 4 × 64 with tilted blades (`_blade` in gen_objects.py:
angled axis walk + belly profile + overlap cut-seams), which took the size
audit ratio from 0.74 to 1.01 of the `crystalwall` reference.

## Net catching is one marker interface

**[jar]** `NetToolItem.canHitMob` returns `mob instanceof
necesse.entity.mobs.misc.NetableMob` — a marker interface with no members.
Vanilla implementers are `HoneyBeeMob`, `QueenBeeMob` and
`SmallFlyingBugCritterMob` (butterflies/fireflies); birds, rodents etc. are
NOT netable. `hitMob` removes the mob via `mob.remove(0, 0, attacker, true)`,
which runs the normal death path (`hasDied=true`, death particles/sound,
`onDeath`) — so the mob's loot table still drops where it was caught.

**[run]** Making a critter catchable = adding `implements NetableMob`.
The Dewsnail does; `skyreachstatus` prints
`net dewsnail=NETABLE` and the integration test asserts it. The real in-game
swing is still only player-verifiable (the server test cannot swing nets).

## Object tool interaction: the `GameObject` pickaxe default

**[jar]** Base `GameObject` sets `toolType = ToolType.PICKAXE` and
`objectHealth = 100`. Any object class that does not override them is
pickaxe-only. Vanilla soft archetypes override in their constructors:

| Class | toolType | objectHealth |
|---|---|---|
| `GrassObject` (and every grass/flower subclass) | ALL | 1 |
| `FruitBushObject`, `BurnedBushObject` | ALL | 1 |
| `StreetlampObject` | ALL | 1 |
| `RandomBreakObject` (crates/clutter) | ALL | 1 |
| `CowSkeletonObject` | ALL | 50 |
| `BigTentObject` | ALL | 100 |
| `TreeObject` | AXE | 100 |
| `SaplingObject` | ALL | – |
| `StatueObject` | PICKAXE | 100 |
| `CrystalClusterObject`, `RockObject`, `FurnitureObject` | PICKAXE (inherited) | 100 |

**[jar]** Matching semantics (`ToolType.canDealDamageTo(other)`): false only if
the object is UNBREAKABLE or the types are disjoint; an object typed ALL is
damaged by everything. All vanilla analogue classes leave `toolTier` at 0, so
tier parity needs no code.

**[run]** This was the root cause of the playtest's "flora is
pickaxe-harvestable regardless of material": our custom `SkyDecoObject`s sat
on the PICKAXE/100 default while native-archetype flora (GrassObject family)
was already correct. Fixed per object in `a58e43b`; `skyreachstatus` prints a
`tool <id>=TYPE/HP` block and `scripts/integration_test.sh` asserts all 18
values, so regressions fail the gate.

## Server boot needs `-Djdk.attach.allowAttachSelf=true` on plain JDK installs

**[run]** On an install without a bundled `jre/` (system JDK on PATH), the
dedicated server dies during mod loading with ByteBuddy
`Could not self-attach to current VM using external process` — before any mod
class runs, so it looks like a mod error but is environmental. Launching with
`-Djdk.attach.allowAttachSelf=true` fixes it; `scripts/integration_test.sh`
now passes the flag unconditionally.

## v0.6 sprint environment facts

**[run]** This Mac has no Necesse Steam install; builds and the integration
test run against the dedicated server at
`~/Downloads/necesse-server-1-3-2-24650233` (export `NECESSE_GAME_DIR` to
it). `./gradlew buildModJar` and `scripts/integration_test.sh` both pass
there (2026-08-25): mod loads, Skyreach generates, world survives a restart,
spire/beacon/warden/cats persist, no log errors. Long shell heredocs get
mangled by this terminal's shell integration — write helper scripts to files
instead.

**[run]** Determinism re-verified after the whole sprint: regenerating into
a temp dir reproduces `src/main/resources` byte-for-byte (only `.DS_Store`
and the hand-maintained `locale/*.lang` differ, which the generator does not
write). `tools/locale_audit.py` reports 77 IDs named in both locales; 
`tools/size_audit.py --vanilla vanilla-sprites` reports 0 flags.

## There is exactly one jar, and it is in `build/jar`

**[run]** `./gradlew buildModJar` writes the mod to `build/jar/`. Gradle's stock
`jar` task used to also emit one into `build/libs/` — 200 files against the mod
jar's 304, **no `mod.info`**, and none of the packaged dependencies. Necesse
ignores a jar without `mod.info`, so installing it produced a mod that loaded
as nothing while looking entirely plausible (the filename differs only subtly:
`Stairway to Heaven-0.5.0.jar` versus `Stairway_to_Heaven-1.3.2-0.5.0.jar`).

The stock task is now disabled and `assemble` depends on `buildModJar`, so
`buildModJar`, `assemble` and `build` all produce the same single correct jar
and `build/libs` stays empty. Verified by running all three.

## Some registered IDs appear nowhere in the source

**[jar]** `WallObject.registerWallObjects(prefix, ...)` registers **six**
objects from one call: `<prefix>wall`, `<prefix>door`, `<prefix>dooropen`,
`<prefix>doorlocked`, `<prefix>doorunlocked` and `<prefix>window`. Not one of
those strings is written anywhere in our source, so a source-scanning audit is
blind to all of them. That is how the Skystone Brick window shipped with no
display name and the engine's error icon: nothing was checking an ID nobody had
typed.

**[jar]** Vanilla names only three of the six in its own `en.lang` — the wall,
the door and the locked door. The open and unlocked states inherit their name
from their counterpart at runtime, and vanilla leaves its window unnamed
because vanilla windows are not separately craftable. Ours are, so ours are
named.

**[jar]** Vanilla ships `items/` icons for exactly the three obtainable pieces:
`stonewall.png`, `stonedoor.png`, `stonewindow.png`.

**[run]** `tools/locale_audit.py` now derives the wall-set IDs from each
`registerWallObjects` call, and separately requires that everything appearing
as a `new Recipe("<id>", ...)` output has an `items/<id>.png` — because a
craftable with no icon is what puts an error texture in the crafting menu.
Floor tiles and rock nodes are exempt: `TerrainSplatterTile`, `RockObject` and
`RockOreObject` all override `generateItemTexture` and build their icon
themselves.

The general lesson: audit what the registry actually contains, not what the
source says it registers. A runtime dump of the mod's ID block would be
stronger still than the current source-plus-derivation approach.

## The wall sheet is addressed at fixed offsets, so cell geometry is load-bearing

**[jar]** `objects/<wall>.png` is 352x128 and does triple duty. The wall body
reads it as a 16px grid (columns 0-3, rows 0-7); the window insert as 16px
columns 4-5; and the doors as **eight 32x128 cells** at 32-cell indices 3-10 —
closed and open, once per rotation. `WallObject.registerWallObjects` creates
all of those from one call, so a mod that never mentions a door still ships
eight door cells.

**[jar]** `WallDoorObject.addDrawables` and `WallDoorOpenObject.addDrawables`
both draw their cell with `.pos(drawX, drawY - 96)`. Sheet row 96 is therefore
the tile's top edge, and every row above 96 sticks out over the tile. The wall
body beside it only rises 16px above its own tile (its row-0 cap is drawn at
`drawY - 16`).

**[jar]** Measured off vanilla `stonewall.png`, the eight cells occupy
y88..127 (closed head-on), y68..127 (open, leaf edge-on), y70..127 (closed
edge-on), y68..127 and y90..127 (open, leaf head-on, swung north / south).
A closed door is 40px tall — 8px above its tile, i.e. deliberately *shorter*
than the wall around it.

**[game]** Our generator painted every door cell from row 0 to row 127. The
door rendered 128px tall against a 48px wall: "die Tür ist 3x so hoch wie
normale Türen aber der Rest der Wand ja nicht", reported from the spire.

**[hypothesis]** `WallObject.loadTextures` defaults `outlineTextureName` to
`"walloutlines"` and overlays that texture using the *same* 32x128 cells. The
sprite dump we work from contains no `objects/walloutlines.png` — it also
contains no `*_short.png` — which suggests the dumper only captured textures
loaded via `fromFile` and skipped the `fromFileRaw` ones. If the file does ship,
its door cells carry vanilla's geometry, so a door drawn outside those extents
also gets an outline floating in mid-air. Not verified either way; matching
vanilla's extents is correct regardless.

**[run]** `tools/sheet_format_audit.py` asserts those extents. The size audit
cannot catch this class of bug: it measures opaque mass, and a door painted
over its whole cell scores *higher* on mass while rendering three tiles too
tall. Verified by running the audit against the pre-fix sheets, where it
reports all sixteen cells as 68-88px too tall.

## Two vanilla sprites, two different things to copy

**[jar]** `mobs/crystaldragon.png` is 320x1792 and `CrystalDragonHead` /
`CrystalDragonBody` address it as `GameSprite(texture, 0, row, 224)` — column 0
only, eight 224px rows: head, shoulder, five body segments, tail. The extra
96px of width is never sampled. It is a `BossWormMobHead`, i.e. the same worm
chain the sandworm uses, at seven times the cell size.

**[jar]** Measured off that sheet, what makes the crystal dragon read as a
creature is not its size: it is a COMPACT rounded cranium carrying two very
large dark eyes, with the entire width of the silhouette supplied by pale
blades radiating out from behind it. Its body segments are built the same way —
a small core inside a big fan. Blade aspect is roughly 14 wide to 90 long.

**[game]** Building the Mist Serpent from the sandworm's *format* while
ignoring the crystal dragon's *construction* produced an armoured capsule with
a plated skull that read as a beetle. Format and construction are separate
decisions and it is worth stating which sprite each one came from.

**[run]** Three failed attempts at the fan, each instructive:
1. Thin blades at wide angles read as insect legs.
2. Longer blades at the same spacing still read as legs, because
   `Canvas.outline` traces every disconnected shape — blades that part company
   anywhere along their length each pick up their own black contour.
3. A connected polar mass fixed the outline problem, but a fan per segment with
   high-frequency scallops turned a 14-part chain into visual fuzz at 1x.

What works: ONE connected polar silhouette per fan, scalloped once per blade
with the notches cutting about a third of the radius, seams drawn as darker
rays rather than gaps, and few large lobes rather than many small ones. The
rule underneath all three failures is that a 14-segment worm multiplies
whatever detail one segment carries by fourteen, so segment detail has to be
judged on the whole chain at 1x, never on one cell zoomed in.

## Hiring an NPC is a vanilla mechanism; do not hand-roll it

**[jar]** `HumanMob`'s third constructor argument is a **SettlerRegistry key**,
not a free-form type name: `ElderHumanMob` passes `"elder"`, which is the key
`SettlerRegistry.registerSettler("elder", new ElderSettler())` registered. And
`Settler`'s own constructor argument is a **MobRegistry ID** — `ElderSettler`
passes `"elderhuman"`. Two different namespaces, one line apart, easy to swap.

**[run]** Our Sky Warden passed `"skywarden"` as its settler key, and nothing
ever registered that key. `HumanMob.getSettler()` therefore returned null, and
`PacketShopContainerUpdate.recruitSettler` refuses with `settlement.notsettler`
when it is. So vanilla's recruit button could never work for him, for two
releases. `/skyreachstatus` now asserts this directly per mob rather than
asserting that *a* settler is registered somewhere.

**[jar]** The vanilla hiring flow, which is what a player expects because every
world NPC uses it: `HumanShop.interact` opens the shop container;
`getShopContainerData` puts `getRecruitItems(client)` on its recruit page;
`ShopContainer.canPayForRecruit`/`payForRecruit` check and take those items
**server-side, only on the button press**; then `recruitSettler` teleports the
mob to the settlement's level itself (`changeMobLevel`, guarded by
`shouldTeleportToLevelOnRecruited`, default `!isDowned()`), calls
`serverData.moveIn`, and broadcasts `ui.settlementjoined`. One mob, one
transaction, settler immediately — assignable bed included.

**[game]** Working around that instead of fixing the key produced three
separate player-visible bugs from one cause: coins taken by *talking* with no
dialogue option; the Warden "disappearing" and reappearing in the village as a
stranger who had to be recruited a second time; and no bed assignment possible
until that second recruitment, because until then he was not a settler at all.
All three were reported in one playtest message.

**[jar]** `HumanMob.getMessages(client)` defaults to
`getLocalMessages("humantalk", 5)` in category `mobmsg`. Any HumanMob that does
not override it makes generic villager small talk — a playtester screenshotted
the last keeper of the Skywatch saying "I often think about the big questions
in life". `getDialogueIntroMessage` is the line at the top of the dialogue
window and is the right place for an offer, because it sits directly above the
price and the recruit button instead of in a bubble that scrolls away.

**[jar]** `HumanMob.getLocalization()` returns
`mob.<mobStringID>name` **with a `<name>` argument** once the human has a
settler name, and plain `mob.<mobStringID>` before that. Vanilla writes these
as `elderhumanname=<name> the Elder`. A mod that only defines the plain key
ships a raw `mob.<id>name` string to the player the moment the NPC is named.

**[run]** Registered-but-never-given quests are invisible, not broken.
`SpireCatsQuest` shipped in v0.2 with its lair positions, treat item, coax
interaction, travel-home puff and journal sync all working, and
`QuestRegistry.registerQuest` called — but no code path ever called
`makeActiveFor`, so no player could ever see it. Registration is not
hand-out. The same was true of `BeaconDeliveryQuest` and `AnchorDeliveryQuest`,
which are still dormant.

## A tile that looks like it places 3x3 is over-splatting, not misplacing

**[jar]** `TileItem.onPlace` calls `tile.placeTile(level, tileX, tileY, true)`
exactly once, for the single tile under the cursor. There is no brush, no
radius and no terrain-vs-floor difference in that path. So "this floor places
far larger than one block" can never be a placement bug — it is the `_splat`
atlas.

**[jar]** `SplattingOptions` decides what a tile draws *over itself* from its
neighbours, not the other way round. `splatsInto(current, adjacent)` returns
true when `comparePriority(current, adjacent) < 0`, i.e. the HIGHER-priority
neighbour bleeds into the lower-priority tile. Floors are PRIORITY_FLOOR 400
and terrain is 0–200, so a floor always bleeds into the ground around it —
that part is vanilla.

**[jar]** How much it bleeds is pure sheet geometry. Within a 224x96 block
(7x3 cells of 32px) the four orthogonal neighbours pick a cell by marching
squares (`SplattingOptions.newTerrainSprites`), and the four *diagonal* cases
are handled separately: adjacency index 0/2/5/7 (top-left / top-right /
bottom-left / bottom-right) draw cells **(2,2) / (0,2) / (2,0) / (0,0)**
respectively, at offset (0,0). Those four are the smallest pieces on the
sheet — a nub in the named corner.

**[run]** Measured with PIL over the vanilla sprite dump, 66 land `_splat`
sheets and 10 liquid ones:

| cell class | cells | vanilla coverage | median |
|---|---|---|---|
| full tile variants | (3..6, 0) | exactly 100% | 100% |
| diagonal corner nub | (0,0) (2,0) (0,2) (2,2) | 0.8 – 29.3% | 12.5% |
| one orthogonal side | (1,0) (0,1) (2,1) (1,2) | 23.4 – 66.4% | 47% |
| two adjacent sides | (3,1) (4,1) (3,2) (4,2) | 41.4 – 82.4% | 65% |
| three sides | (5,1) (6,1) (5,2) (6,2) | 60.5 – 90.2% | 82% |
| all four sides | (1,1) | 51.6 – 81.6% | 71% |

Vanilla never paints the three-side and all-four pieces solid: it keeps a
ragged *eye* of the tile underneath, pulled toward whichever side is still
open ((1,1) is bordered on all four, so its eye sits dead centre).

**[run]** Our generator shipped the four diagonal cells at 83–89% — the
complement of the intended nub, a disc of radius 26 parked inside the cell
with a bite taken out of the far corner. Every mod tile therefore repainted
each of its four diagonal neighbours almost completely, so one placed floor
read as a 3x3 blob. This was the "White floor places huge" report, and it was
on all 14 sheets, floors, terrain and liquids alike. The three-side and
all-four cells had also gone solid (98–100%) because unioning the directional
discs covers everything. Both are fixed in `gen_splats.py` and gated by
`tools/tile_behaviour_audit.py`.

## `-1.0F` broker value means "derive it from the recipe", and it ignores yield

**[jar]** `ItemRegistry.calculateBrokerValues` feeds every item whose
registered value is **negative** to `RecipeBrokerValueCompute`, using
`Math.abs(value)` as a multiplier. The compute sums
`brokerValue(ingredient) * amount` over the recipe's ingredients and **never
divides by the recipe's yield**.

**[jar]** That is safe in vanilla because *every* vanilla floor recipe is 1
ingredient → 1 tile (`stonefloor`, `woodfloor`, `pinefloor`, `granitefloor`,
… all `new Recipe("<id>", 1, …)`). Ours craft 6 tiles from 2 skystone. Copying
vanilla's `-1.0F` onto a bulk recipe would price each of the six at the full
cost of the craft — a broker money printer. Our floors keep a flat positive
value on purpose; `tools/tile_behaviour_audit.py` fails the combination
"negative broker value + recipe yield > 1" rather than the flat value.

## Judge worldgen at screen scale, and keep the renderer honest

**[run]** A Necesse screen is roughly 40x22 tiles. Every density judgement in
this repo that was made from a whole-world overview has been wrong in one
direction or the other: the rock pass first left a good outcrop looking like a
speck, then carpeted the world, and the landscape pass paved a 26-tile plaza
that a 300-tile overview rendered as a tasteful detail and a real screen
rendered as a chequerboard filling the view. Render 40x22 with real sprites at
1x, or do not have an opinion about density.

**[run]** An offline renderer that reads the generator directly is worth
building — it needs no game boot and iterates in seconds — but it can silently
drift from what the server actually writes, and then the calibration is
calibrating a fiction. The fix is one assertion in the integration test:
`painter oracle: tileMismatches=0`, every tile in a 129x129 scan of the live
world compared against the pure function the renderer uses. Build the oracle
at the same time as the renderer, not after.

**[jar]** Two vanilla behaviours worth knowing before inventing your own:
`PathTiledTile` (`snowstonepathtile`) blends its own edges into whatever
terrain it crosses, bridges a shore tile below it, and grants +10% movement
speed, so a road built from it pays the player to follow it. And a warp applied
to the QUERY POINT rather than to the segments bends a whole network together:
junctions stay joined while the roads between them curve.

## One sheet, three readers, three chances to get the height wrong

**[game]** The wall sheet's window strip shipped the same bug as its door
cells, and survived the door fix, because the audit written for the doors only
looked at doors. `WallWindowObject` draws its edge-on variant from rows 2..7 at
`drawY-64, -48, -32, -16, 0, +16` — a reach of two full tiles above the tile —
and vanilla leaves rows 2-4 empty so the window ends up 48px, the height of its
wall. Ours filled all six.

The general lesson is about the audit, not the pixels: when a gate is written
for one consumer of a shared sheet, it does not cover the others, and the
remaining ones are exactly where the next report will come from. Enumerate the
readers first — the wall body, the window and the doors all read
`objects/<wall>.png`, at three different cell sizes.

## A spawn table entry is a request, not a guarantee

**[jar]** `MobChance.spawnMob` calls `mob.isValidSpawnLocation(...)` and drops
the mob when it answers false — and `Mob`'s own implementation is
`return false`. Every spawn therefore depends on some class in the chain
overriding it. `HostileMob` and `CritterMob` do; **`SheepMob` does not**, nor
does anything between it and `Mob`. A sheep can never be placed by a spawn
table at all, and vanilla knows: it places sheep, rams, cows and bulls from the
island generator (`ig.spawnMobHerds`), never from a table. Livestock is terrain,
not weather.

**[run]** Our Driftlands critter table asked for `cloudlamb` for three releases
and silently got nothing. A table entry that does nothing looks identical to a
table entry that is merely unlucky, which is why this survived so long.

**[jar]** `HostileMob.isValidSpawnLocation` calls `checkLightThreshold`, which
compares **ambient + static** light against `spawnLightThreshold` (0 by
default). On a non-cave level the ambient is
`worldEntity.getAmbientLightFloat() * 150` — 150 in daylight — so `150 <= 0`
fails and no hostile can be placed anywhere while the sun is up. Right for a
vanilla island; wrong for a layer that is only ever surface and that the player
travels TO.

**[jar]** Vanilla's fix for a mob that belongs to a place rather than to the
night is `spawnLightThreshold = new ModifierValue<>(..., 0).min(150, MAX)`
(PhantomMob, AshGolemMob, PirateMob, CryptBatMob, the slimes). It passes for
ANY light, so it also switches off torch protection. `checkStaticLightThreshold`
is the same check against `getStaticLight` alone — placed lamps, no daylight —
which keeps both properties at once.

**[run]** `/skyreachstatus` now measures every mob twice, at the level's real
light and with the ambient forced dark:

    zephyrray   validSpawnLocation=implemented         accepted lit=4/6 dark=4/6
    galehound   validSpawnLocation=implemented         accepted lit=0/6 dark=4/6
    cloudlamb   validSpawnLocation=INHERITS Mob's false accepted lit=0/6 dark=0/6
    zephyrfinch validSpawnLocation=implemented         accepted lit=6/6 dark=6/6

A mob blocked only by light accepts in the dark column; a mob that inherits
`Mob`'s false is zero in both. Two numbers, two different bugs, no inference.

**[run]** Two corrections to the probe are worth as much as the probe. It first
sampled radius 6..30 around the spire — which the landscape pass had filled
with a lamp-lit forecourt — so it measured "standing next to a candelabra" and
reported a working fix as broken. And it now prints the world clock and the
per-tile static light: a probe that cannot show whether its own precondition
held is worse than no probe, because it is believed.

## Catching a critter pays exactly what its loot table says

**[jar]** `NetToolItem.hitMob` ends with `target.remove(0, 0, attacker, true)`.
That is the whole catch: the mob is removed and its loot table drops. So a
chance-based table means the animal frequently vanishes for nothing, which is
what a net feels like when it is wrong. Vanilla's netted critters drop
themselves unconditionally — `FireflyMob.getLootTable()` is one bare
`LootItem`.

## Which way is the wall facing? The engine already decided

**[jar]** `WallWindowObject.getWindowDir(up, right, bot, left)` returns **1**
when the wall runs north-south (connected walls above and below, none either
side) and **0** when it runs east-west. Those two draw completely different
pictures out of the same 32px strip:

| dir | wall runs | cells | drawn at | what you are looking at |
|---|---|---|---|---|
| 1 | north-south | rows 0-1 | drawY-16, drawY | the wall's **roof**, from directly above — vanilla is 512/512 opaque, the window is drawn ONTO the cap |
| 0 | east-west | rows 2..7 | drawY-64 … drawY+16 | the wall's **front face** — vanilla empties the middle of rows 5-6 so the ground shows THROUGH |

**[game]** We shipped one front-facing glazed pane for both, so a window in a
north-south wall faced the camera instead of lying flat in the roof, and the
east-west one sat as a pane inside the wall's dark cap band instead of being a
hole in its face. The player diagnosed it exactly: "das Fenster links am Block
zeigt weiterhin nach unten".

The rule to carry forward: **in a top-down game a hole is only a hole from the
side you can see through it.** From overhead a window shows roof. Glass seen
from above is looking at the sky and reads DARKER than the same glass edge-on —
at full strength it looks like a sticker on the roof.

**[run]** This is the third bug of the shape "one sheet, several readers, the
fix covered one of them". The door cells, then the window's height, then the
window's orientation — each fix left the next reader unguarded, and each time
the next player report came from exactly there. When a gate is written for one
consumer of a shared sheet, enumerate the others in the same sitting; the audit
now asserts both window views and all eight door cells.

## A level nobody is standing on is unloaded, and `isLoaded` then lies

**[jar]** `Server.tick` unloads every level whose `unloadLevelBuffer` exceeds
`20 * max(2, Settings.unloadLevelsCooldown)` (Server.java:365-375). A level with
no players on it therefore disappears from `levelManager` after a minute or so
of ordinary play, and `levelManager.isLoaded(identifier)` starts answering
false for a dimension that very much still exists on disk.

**[jar]** `World.getLevel(identifier)` is the honest accessor: it loads the
level when the manager does not have it (World.java:273-281), generating it as
a last resort.

**[jar]** Both `SkyWardenMob.handleTurnIns` and `onRecruited` guarded their
Skyreach lookup with `isLoaded(...) ? getLevel(...) : null` and then silently
did nothing when the answer was null. So the whole post-recruitment chapter
machine — cats quest, lair markers, anchor chapter, and the write-back of
`anchorDone` — was skipped for the single most ordinary case there is: a player
who came down from the sky, played on the surface for a minute, and then walked
over to the Warden in the village. The player-facing symptom is a build that
contains the fix and behaves exactly like the build that did not: "warden gibt
weiterhin keine quests die ich finden kann".

The general rule: **`isLoaded` answers "is it in memory", never "does it
exist"**. Any cross-dimension read on a deliberate player action should use
`World.getLevel`, and an `isLoaded` guard around game logic is a silent
early-return waiting to happen.

## A quest chain must be a function of world state, not of the journal

**[run]** Hand-out code that reacts to quests the player is *already holding*
has one dead end per state it forgot. Three of them shipped at once:

  * `RecruitWardenQuest` was only given inside the `stage == 0` branch, so a
    world that met the Warden under an older build (stage already 1) could
    never be given it again;
  * the cats catch-up was gated on `!(blackHome && tabbyHome)` while the
    turn-in needed a *held* quest — and `SpireCatMob.interact` sets those flags
    for anyone carrying a treat, quest or not. A player who coaxed both cats
    home without ever holding the quest got no reward and never opened the
    anchor chapter;
  * nothing ever re-issued `AnchorDeliveryQuest`.

`SkyWardenMob.chapterFor(SkywatchQuestData, boolean isSettler)` is now a pure
function with no client, journal or level in it, and `/skyreachstatus` prints
the chapter for eight reachable save states (`chain check: fresh=RECRUIT
met-him-old-build=RECRUIT ... anchored=DONE no-dead-ends`). The integration
test fails if any state that is not a finished chain is owed nothing.

**[jar]** `HumanMob.isSettler()` is world truth the mob carries itself: he can
only be a settler if the world recruited him. A settler standing next to a
record that says `recruited == false` is a broken record, not a state, and is
repaired rather than believed.

## A teleported mob is re-filed under its region on the next tick, not at once

**[jar]** `EntityList.tick` calls `regionList.updateRegion(entity)` for every
mob once per server tick (EntityList.java:362-371), and
`Region.onUnloaded` walks `getSaveToRegion(rx, ry)` calling
`limitWithinRegionBounds(thatRegion)` and `remove()` on everything still listed
there (Region.java:407-417). A mob moved with `setPos` across a region boundary
is therefore still filed under its OLD region for up to one tick, and a region
unload inside that window would clamp it back into the region it just left.
`EntityList.getRegionList().updateRegion(mob)` right after the move closes it;
`SpireCatMob.sendHome` does exactly that.

**[jar]** `Entity.shouldRemoveOnRegionUnload()` is `shouldSave()` on the
server, and `Mob.shouldRemoveWhenInUnloadedRegion()` is `canDespawn`. For the
spire cats that reads: they ARE written into their region file and dropped from
memory when it unloads (correct persistence), and they are NEVER deleted for
being in an unloaded region. Both follow from the same `canDespawn = false`.

**[run]** Observed end to end: `/skyreachstatus cats` coaxes both cats to the
basket, and after a full server restart they load back at it with the tether
rebuilt — `spirecatblack at=127,154 d=0 tether=127,154 AT_BASKET`. After 25s of
AI they sit 3-7 tiles out and come back: `HomesickCritterAI` only pulls a
critter home past 96px (3 tiles), so an exact position is not a property the AI
has and only the TETHER is worth asserting exactly.

## Vanilla's animal feed is a class check, and only wheat passes it

**[jar]** `HusbandryMob.canFeed` is `!isOnFeedCooldown() && item.item instanceof
GrainItem`, and `FeedingTroughObjectEntity`'s inventory filter is the same test
(FeedingTroughObjectEntity.java:182). There is no tag, no registry and no
recipe behind it — and `ItemRegistry` registers exactly ONE `GrainItem` in the
whole game: `wheat` (ItemRegistry.java:1955). So "what goes in the trough" has
precisely one vanilla answer, and any mod animal kept away from a wheat farm
cannot be fed at all.

**[jar]** Overriding `canFeed` is not enough on its own: hand-feeding runs
through `GrainItem.canMobInteract`/`onMobInteract`, so the ITEM has to be a
GrainItem for a right-click to feed rather than to place or eat. `GrainItem
extends FoodMatItem`, so promoting an existing food item costs nothing — ours
(`cloudberry`) keeps its food behaviour, spoil timer, value, icon and locale
and merely becomes feedable. Measured: `husbandry check: ...
cloudberry=hand:true/trough:true wheat=hand:true/trough:true
skystone=hand:false/trough:false`.

**[jar]** `SheepMob` has two more vanilla behaviours a sky subclass does not
want: `getRandomChildMobStringID` is `getOneOf(this.getStringID(), "ram")`, so
half of every lamb bred in the Skyreach was a plain vanilla **ram**; and
`getLocalization` returns `mob.lamb` for anything not grown up, so a mod lamb
wore a vanilla display name until it matured. Both are one-line overrides and
both are now asserted (`child=cloudlamb name=Cloudlamb`).

**[jar]** `FriendlyRopableMob`'s rope tracker calls
`changeMobLevel(this, roper.getLevel(), ...)` when the roper changes level
(FriendlyRopableMob.java:45-52), so a roped animal follows the player down the
Stairway. That is what makes "haul a breeding pair down" a real loop rather
than a suggestion. **Not yet observed in a client.**


## The fence sheet's five columns are the engine's, not the artist's

**[jar]** `FenceObject.addDrawables` reads `objects/<name>.png` as five 32-wide
columns of the full sheet height, all bottom-anchored at
`drawY = tileDrawY - height + 32`. With the vanilla height of 64 that makes
**sheet row 32 the tile's top edge and row 63 its bottom edge**.

| col | what the engine draws it as | when | where |
|---|---|---|---|
| 0 | the post | always | `(drawX, drawY)` |
| 1 | north joint | the tile ABOVE attaches | `(drawX, drawY)`, **before** col 0 |
| 2 | south rail | the tile BELOW attaches | `(drawX, drawY)` |
| 2 | again, to bridge into a wall | the tile above attaches and is NOT a fence attaching back | `(drawX, drawY - 24)` |
| 3 | the run to the WEST | the tile to the left attaches | `(drawX, drawY)` |
| 4 | the run to the EAST | the tile to the right attaches | `(drawX, drawY)` |

**[jar]** Measured off vanilla `ironfence.png`, `woodfence.png` and
`stonefence.png`, all three agree on the geometry (ironfence's solid extents):

| cell | solid extent | opaque px |
|---|---|---|
| col 0 post | x10..21, y22..51, soft skirt to y55 | 424 |
| col 1 joint | x10..21, y26..33 — **8 rows, inside the post's own footprint** | 128 |
| col 2 rail | x12..19, y34..63 — **runs to the tile's bottom edge** | 360 |
| col 3 west | x0..9, y30..47, skirt to y53 — **reaches x=0** | 220 |
| col 4 east | x22..31, y30..47, skirt to y53 — **reaches x=31** | 228 |

Three consequences the mod's own sheet got wrong, each visible in a run:

- col 1 is drawn BEFORE col 0 and sits inside its footprint, so anything wide
  painted there shows past the post. Ours held a full-width horizontal rail,
  which is why a fence connecting north sprouted a rail across the tile.
- col 2 must tile with itself at a 32px pitch and still read when shifted 24px
  up. Ours was a 3px hairline, so every vertical run was a thread.
- cols 3 and 4 are NOT mirrors of each other in the world: 3 must reach the
  left edge and 4 the right. Ours were mirrored the wrong way round, so a
  horizontal run had a gap at each tile boundary and a doubled post.

**[jar]** The perspective is carried by two things and neither is optional. A
horizontal rail is **2 rows of outline, 2 rows of LIT TOP SURFACE, 2 rows of
DARK FRONT FACE, 2 rows of outline** — you see the top of the rail and its
front, which is the top-down-with-forward-lean the whole game is drawn in.
And every piece stands on a baked **soft-alpha ground skirt** (alpha 74 then
29), the same trick vanilla's rocks use, never an opaque dark band.

**[jar]** Vanilla `ironfence.png` uses nine colours: outline (34,35,35), a
four-step iron ramp 67/98/130/166, two rust browns and two alpha values. The
mod's pre-fix sheet used **three**, two of which were within four units of the
outline tone — 670 of its 851 pixels were literally the outline colour. That is
what "perspektivisch schrecklich" looks like from the pixel side: no ramp, so
no top face, so no lean.

**[jar]** `objects/<name>fencegate.png` is 192x64 and its six columns are:
0 = open horizontal, 1 = closed horizontal, 2 = the vertical gate post **drawn
twice, at `drawY-14` and `drawY+14`**, 3 = a latch piece drawn at `drawY+14`
for rotation 3 only, 4 = the closed vertical leaf at `drawY-14`, 5 = the open
vertical leaf at `(drawX-16, drawY+14)`. Column 2 being drawn twice is the one
that punishes a wrong guess hardest: ours held an "open horizontal gate", so
every north-south gate rendered as two of them stacked 28px apart.

## A fence band thinner than 1.6 tiles is not a fence

**[run]** `FenceObject.attachesToObject` looks at the four ORTHOGONAL
neighbours only. So any fence line thin enough to step diagonally on the tile
grid is not a line at all — it is a row of unconnected posts.

Rasterising a straight band at every angle from 0 to 90 degrees and measuring
the largest 4-connected component: at 0.9, 1.0, 1.2 and 1.4 tiles thick the
band falls apart (its largest piece holds 2–3% of its tiles at the worst
angle, and the sweep leaves 33–169 lone posts); at **1.6** it is a single
component at every angle with zero lone posts. That number is now
`SkyLandscape.FENCE_MIN_THICKNESS`.

**[run]** The same problem in polar form: a ring taken as the annulus
`|d - r| <= 0.5` is one tile thick and steps diagonally near its 45-degree
points. Over radii 7, 8, 9, 10, 11, 13 and 20 that rule leaves 60–70% of the
ring as lone posts and dead ends. The **8-neighbour inner boundary** — inside
the disc, with at least one of the eight neighbours outside — is a single
closed loop in which every tile has exactly two orthogonal neighbours, at
every radius tested. It runs two tiles wide across the diagonals, and that
second tile is the whole fix (`SkyLandscape.discRing`).

**[run]** Measured over the offline painter dumps for three seeds (hub, two
designed places, countryside, and a 400x400 overview each), before and after:

| | fence tiles | lone posts | dead ends | part of a run |
|---|---|---|---|---|
| before | 4111 | 159 (3.9%) | 1076 (26.2%) | 2876 (70.0%) |
| after | 5562 | 13 (0.2%) | 332 (6.0%) | 5217 (93.8%) |

The dead ends that remain are the ones a fence is supposed to have: a gate
wing terminating at its lamp, and a roadside bed's two ends where it opens
onto the path.

## The grey ground was empty because three separate rules switched it off

**[run]** `SkyTerrainPainter.describeTile` turns any biome's ground grey where
`isRockPatch` is true. `rollObject` then answers `isRockPatch ? 0 : plant` for
nearly every plant, the meadow-carpet rule is gated on `!isRockPatch`, and so
is the aurora-colony rule. Three independent suppressions, nothing to replace
them. Measured over three seeds and 235,528 natural land tiles:

| biome / ground | land | objects/tile | contents |
|---|---|---|---|
| Driftlands / cloudturf | 157863 | 0.384 | grass, reeds, wheat, bells, trees |
| AuroraShoals / cloudturf | 14481 | 0.347 | prismgrass, blooms, lilies, ferns |
| Stormveil / stormslate | 28586 | 0.311 | sedge, moss, crystals, pines |
| Stormveil / skystone | 4871 | 0.099 | rock, moss, crystal |
| AuroraShoals / skystone | 3247 | 0.044 | **rock only** |
| Driftlands / skystone | 26480 | 0.032 | **rock only** |

14.7% of all land, four to twelve times emptier than anything else. That is
the whole of the player's "graue Böden viel leerer ... nur paar einzelne
Steinblöcke".

**[run]** The replacement is a formation field, not a probability:
`screeObject` puts lichen beds on a 9-tile lattice with a wobbled boundary and
a fill roll, exactly the shape `auroraColonyObject` already uses, and picks
lichen → cragbloom → scree → boulder → lit accent inside a bed, with a small
stray roll outside so the open plate is never dead flat. Re-measured on the
same 235k tiles: **0.304 / 0.352 / 0.356**, inside the 0.311–0.384 band the
vegetated grounds occupy.

**[run]** The first screen-scale render of the new barrens was correct in
density and still wrong: lichen, cragbloom and scree are all low 32px objects,
so 40x22 tiles of them read as one flat field of pebbles. The meadow gets its
relief from trees, and nothing tall grows on bare plate — so the barrens get
theirs from boulders standing in the beds. This is the third time in this repo
a density number has been right while the screen was wrong, and the second
time the fix was variety of HEIGHT rather than count.

**[run]** `painter oracle: tileMismatches=0` still holds after all of the
above, so the 40x22 renders these numbers were calibrated on are the tiles the
live server actually writes.
