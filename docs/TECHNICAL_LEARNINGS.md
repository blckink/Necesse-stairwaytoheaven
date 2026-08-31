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

**[run]** The oracle only guards the *tiles*. It compares
`describeTile`'s ground against the live world and nothing else, so a renderer
can pass `tileMismatches=0` while drawing every OBJECT wrong — which is what it
was doing. `TreeObject.addDrawables` hard-codes `spriteRes = 128`, so a tree
sheet is a grid of 128x128 cells and exactly one cell is drawn, at
`(drawX - 48, drawY - 96)`. `scripts/sky_map_render.py` was treating the sheet
as a single tall sprite — correct for a 32px flower, and for a 128x512 tree
column it stacks all four variants in a pile above the tile. Every calibration
render since the tree pass showed **four times** as many canopies as the world
contains: one 40x22 screen with 8 Nimbus Willows in it rendered 32. The two
statues were wrong the same way (`StatueObject` draws one sprite of
`width / spriteCount`, and both sky statues register `spriteCount = 1`, so the
variant width is the whole sheet, not 32px).

The lesson is the shape of the gate, not the arithmetic: an oracle that checks
one of the two things a renderer draws will let the other rot indefinitely, and
it will rot in the direction that flatters the world.

**[run]** `FenceObject` attaches to its four ORTHOGONAL neighbours only, and
`FENCE_MIN_THICKNESS = 1.6` tiles is the rule derived from that. It holds for a
new fence band too, measured rather than assumed: the Cloudmarble balustrade
introduced for the Skyway Passages, laid at exactly 1.6 tiles from
`ROAD_HALF_WIDTH + 0.3`, comes out at **0.29% lone posts and mean degree 2.26**
over six seeds and 7,129 fence tiles — indistinguishable from the established
Skywatch railing's 0.20% / 2.21 over 27,968. Any new railing should be measured
this way before it is believed; a diagonal band that rasterises into loose
posts looks perfectly fine in the source.

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

## Wall body columns are tile halves, not variants (v0.8)

`WallObject.addWallDrawOptions` draws the left half of a wall tile at `drawX`
from **columns 0 and 2**, and the right half at `drawX + 16` from **columns 1
and 3**:

```java
// left half, at drawX
sprite(0,3) sprite(0,4)      // no wall to the left
sprite(2,3) sprite(2,4) sprite(2,5)   // wall to the left
// right half, at drawX + 16
sprite(1,3) sprite(1,4)      // no wall to the right
sprite(3,3) sprite(3,4) sprite(3,6)   // wall to the right
```

So a column pair is *(left half, right half)* of the same connection state, not
two interchangeable variants of a whole tile.

This matters for what a wall can depict. `gen_walls.py` paints the four columns
as interchangeable variants, which forces any motif to repeat with a **16 px
period** — fine for masonry courses, impossible for anything that spans a tile.
`gen_cloudmarble.py` treats them as halves instead, which is what lets its
arcade carry **one arch per 32 px tile** with the pilaster straddling the tile
boundary (x29–31 of the left half against x0–2 of the right).

`skystonebrickwall` and `nightfellwall` are not broken by this — their designs
are course-based and read correctly — but they cannot grow a tile-spanning
motif without moving to the half-column reading first.

Same lesson as the door cells and the window rows before it: one sheet, several
readers, and the reader you did not check is where the next report comes from.
`tools/sheet_format_audit.py` now covers all three wall sheets (42 cells).

**Incomplete — see the next section.** The mapping above is right for rows 0, 3
and 4 and WRONG for rows 1 and 2, where columns 1 and 2 swap sides.

## …and in rows 1 and 2, columns 1 and 2 swap sides (Beetlefreak wall)

Read off `WallObject.addWallDrawOptions` cell by cell rather than from the
row-3/4 sample above, the column-to-half map is not constant down the sheet:

| rows | col 0 | col 1 | col 2 | col 3 |
|---|---|---|---|---|
| 0, 3, 4 | left / closed | **right** / open | **left** / open | right / closed |
| 1, 2    | left / closed | **left** / open  | **right** / open | right / closed |

`if (left) { if (botLeft) { sprite(1,2)@drawX … sprite(1,1)@drawX } }` puts
column 1 on the LEFT, while `if (right) sprite(1,3)@drawX+16` puts the same
column on the right one row group down. The fully-surrounded fast path agrees:
it reads `section(16,32,…)` — cell column 1 — at `drawX`.

The vertical grammar is equally load-bearing. A tile is drawn as three 16px
bands at `drawY-16`, `drawY` and `drawY+16`, so consecutive tiles OVERLAP by
16px, and a run of N tiles reads top to bottom as

    row 0                     the free top cap, 16px (only when nothing is above)
    (row 2, row 1) × (N-1)    the repeating ROOF, 32px per tile
    row 3, row 4              the front FACE, 32px (the bottom tile only)

so a wall shows its roof for every tile but the last. Rows 2 and 1 are always
the upper and lower half of the same tile, which is what lets the roof carry a
32×32 field instead of one 16px strip stamped twice.

Six cells are **unreachable**: (2,5) (3,5) (2,6) (3,6) (2,7) (3,7). The public
overload passes the same `boolean[]` as `adj`, `sameWall` AND `isWall`, so every
`isWall[n] && !sameWall[n]` branch — and `botWall = isWall[6]` inside the
`!bot` branch — is dead. Vanilla fills them anyway; nothing draws them.

**This is what "die Wandtexturen sind komplett für'n Arsch" was.** The supplied
`beetlewall.png` was one continuous illustration painted across the 4×8 block —
swirls, striped bands and an arch running straight over the 16px cell edges. The
occupancy pattern matched vanilla exactly and `sheet_format_audit.py` passed,
because that audit guards GEOMETRY (which cell, what size, what extent) and
cannot see whether the art inside a cell joins its neighbour. The door cells
were also generic art rather than eight door frames, and the window's two views
were swapped: the front-facing pane sat in rows 0-1, which the engine draws as
the wall's ROOF seen from above.

## Prove a wall tiles by composing it, not by auditing its cells

`tools/wall_render_preview.py` is a line-by-line port of
`WallObject.addWallDrawOptions` (both overloads),
`WallWindowObject.addWallDrawOptions` and the two `WallDoorObject` draw paths.
It runs the engine's own cell selection over synthetic levels — a solid block,
an L, a T, a free-standing tile, doors in both orientations, windows in both —
and writes 4× contact sheets on a dark and a light backdrop plus a 1× in-context
mock on Stormslate and Cloudturf.

Run it against a vanilla sheet first (`--vanilla stonewall`): if the port is
right, vanilla renders correctly, and only then does a failure on our sheet mean
our art. That check is the whole value — it turns "the walls look wrong" into a
picture of exactly which cells do not meet.

A sheet-format audit and a composed render answer different questions. Passing
the audit is necessary and **not** sufficient.

## …and a composed render of OUR sheet alone is still not sufficient

Two more faults on `beetlewall` survived both gates and were caught by the
player, not by us. Both are questions the tool could not ask, because it only
ever drew our own sheet: "is this door as tall as a door" and "is this the view
a side wall is supposed to show" are judgements against vanilla, and nobody can
hold vanilla in their head across a session.

**[sprites]** Fault 1, the doors. The eight door cells' bounding boxes were
byte-identical to `stonewall`'s (tops 88/68/70/68/88/68/70/90, cell 5 spanning
x14-31 and cell 9 x0-17), which is exactly why `sheet_format_audit.py` was
green. The fault was compositional. Decoded cell by cell off `stonewall` and
`woodwall`, vanilla's rule is:

* **One leaf, running the full height of the cell.** `woodwall`'s edge-on leaf
  (cell 5, x18-27) is unbroken from y70 to its threshold at y124;
  `stonewall`'s from y70 to y123. Only the bottom 32px — the part inside the
  tile — gains a reveal either side of it.
* **The crown is the part above row 96, and it carries all the ornament.**
  `stonewall` puts an 8px arch cap there, `woodwall` a 12px pediment. Nothing
  cream, bright or horizontal crosses the leaf itself.
* **The leaf reads one ramp step lighter above row 96 than below it** — the
  part above is the door's top catching the sky, the part below stands in the
  doorway's shade. `stonewall` does it literally: the same leaf is
  (115,130,151) above row 96 and (60,65,74) below.

Ours had, in cell 5, a stub of wall masonry with a lantern on it above the tile
edge, a full-width cream bead band at row 96, and a 3px sliver of leaf lost in a
slab of roof pixels below — three unrelated things stacked, the door being the
smallest. In the head-on cells a 16px leaf sat between 8px jambs, and a bead
rail ran across it at 60% height. That is what "die Türen wirken viel zu kurz"
was: not the silhouette, the banding.

**[sprites]** Fault 2, the side-wall window. `WallWindowObject.getWindowDir`
returns 1 for a north-south wall — the LEFT and RIGHT walls of a room — and
then only cells (4,0) (5,0) (4,1) (5,1) are drawn, over the band
drawY-16..drawY+16. In a vertical run that band is unbroken ROOF, so the
picture is the wall's top surface with a hole looked down into. `stonewall`,
`brickwall`, `granitewall`, `icewall`, `dungeonwall` and `woodwall` all draw
the identical grammar: wall top down both sides, an opening 10-12px wide
running ALONG the wall for almost the full 32px of the cell, a dark reveal on
its near faces, a lit lip on the far one, glass at the bottom of the cut, and
**no horizontal terminator at either end** — the opening simply runs until the
roof resumes. Vanilla's top-down glass is also brighter than the roof around
it, not darker.

Ours was a brass-framed pane with a two-by-two lattice standing upright in the
middle of the cell, so an east-west-facing wall showed a window facing south
out of its flat top. The player: "Fenster an der Seite zeigen nie in Richtung
Süden … das wäre ja mitten in der Wand, nach oben ausgerichtet." Note that the
previous pass already knew the cell was the roof and tried to fix it by making
the pane dark; darkening a front-facing pane does not make it lie down. Only
the slot shape does.

`sheet_format_audit.py` asserts window rows 0-1 are 512/512 opaque, which both
the wrong art and the right art satisfy — the sheet has no hole in it either
way; the PICTURE is the hole.

**The fix to the process:** `tools/wall_render_preview.py` now renders every
scene for our sheet AND for vanilla `stonewall` and `woodwall` directly
beneath, same scene, same scale, same backdrop, and its scene set is chosen to
reach every branch of the port — windows in the left/right walls (getWindowDir
1) and in the top/bottom walls (getWindowDir 0) as separate scenes, closed and
open doors in all four rotations, plus a solid block, an L corner and
free-standing runs. Both faults are obvious the moment `stonewall` is drawn one
strip down; neither is visible without it.

## `generate_assets.py`'s one-producer guard tested existence, not authorship

The guard at the end of `main()` listed the files `tools/convert_biome_art.py`
owns and failed if any of them EXISTED after the run. Every one of them exists
in a normal checkout, so `python3 tools/asset_generator/generate_assets.py` —
the workflow `CONTRIBUTING.md` and `docs/assets-style-guide.md` both document —
always failed, and the generator could only be run into an empty directory.
It now stamps `(st_mtime_ns, st_size)` for each guarded path BEFORE generating
and compares after, so it fails on a file this run actually wrote or created,
and passes on one it left alone.

## A preset does not place multi-tile furniture; it writes both halves (v0.8)

`Preset.applyToLevel` writes object IDs with
`level.objectLayer.setObject(layer, x, y, id)` — the raw layer setter. It does
**not** call `MultiTile.placeObject`, so writing only the master of a bench,
bed or dinner table leaves half an object in the world: the master with no
counter, which draws as one end of the piece and behaves as a broken multi-tile.

Vanilla's own furniture presets prove the intended idiom — both halves,
explicitly, with the **same rotation**:

```java
// BenchPreset:       objects = [oakbench, oakbench2],           rotations = [1, 1]
// BedDresserPreset:  objects = [oakbed2, oakbed, oakdresser],   rotations = [3, 3, 2]
// DinnerTablePreset: objects = [.., oakdinnertable, ..,
//                               .., oakdinnertable2, ..],       rotations = [1,2,3, 1,2,3]
```

The counter always sits in the direction the rotation points — `0` up, `1`
right, `2` down, `3` left — because `BenchObject.getMultiTile` builds a
`SideMultiTile(0, 1, 1, 2, rotation, true, counterID, getID())` and the rotation
rotates that 1x2 column. So "never place the `<id>2` half" is true of the
*registry* (the helpers register it, it has no recipe and no item icon) and
false of *worldgen*: a preset must write it.

Rotation for the other furniture is the direction the piece FACES, which is why
vanilla's presets require a wall on the side the rotation points away from:
a dresser under a north wall is rotation 2 (facing down), a chair at a table is
rotated toward the table (`ChairObject.facesTable` checks exactly that tile),
and `DeskObject extends TableObject`, so a chair turned to a desk counts as
facing a table.

Wall decor is the **opposite** convention and a different layer.
`PaintingObject` and `WallTorchObject` live on `ObjectLayerRegistry.WALL_DECOR`,
their own tile must NOT be a wall, and the rotation names where the wall is:
`0` = wall below, `1` = wall left, `2` = wall above, `3` = wall right. Writing a
banner onto the base layer of a wall tile — which the old spire preset did —
replaces the wall with the banner and opens a hole in the building.

## The spire preset and the forecourt share a boundary that is one tile wide (v0.8)

`SkyLandscape` builds the Warden's Forecourt around the same origin the spire
preset is stamped on: `HUB_PROP_MIN = 10.5` reserves everything inside that
radius for the preset, the lamp ring sits at radius 11, and the railing
(`discRing`, radius 13) at 13. Measured, the six forecourt lamps land at
`(+-10, +-5/6)` and `(0, +-11)`, and the only railing tiles inside a
`|dx|,|dy| <= 9` box are the four diagonal links at `(+-9, +-9)`.

So a 21x21 preset can write local 1..19 and keep every forecourt lamp, but it
must leave those four corners alone or the railing loses its diagonal links and
reads as four gaps. `WardenSpirePreset.WRITTEN_RADIUS` is that box, and
`SkyreachStatusCommand`'s painter-oracle exclusion is derived from it rather
than hand-copied, so the two cannot drift apart.

## Supplied art carries 1000x vanilla's colour count, and that is fine (v0.9)

Measured over the sheets supplied for this mod against their vanilla templates:

| sheet | opaque px | colours | px per colour |
|---|---|---|---|
| supplied `cloudtree` | 77,182 | 46,556 | **1.7** |
| supplied `nimbuswillow` | 33,322 | 29,534 | **1.1** |
| supplied `cloudmarblewall` | 18,476 | 10,728 | **1.7** |
| converted `skyseraphtree` | 36,099 | 30 | 1203 |
| vanilla `birchtree` | 76,256 | 23 | 3316 |
| vanilla `willowtree` | 73,480 | 28 | 2624 |
| vanilla `stonewall` | 18,928 | 19 | 996 |

Roughly a colour per pixel and a half, against vanilla's one per thousand. The
obvious conclusion — quantize them to a vanilla-tight ramp, the way
`convert_reference.quantize_opaque` does for converted art — **is wrong, and
was tested rather than assumed.**

Quantizing `cloudtree` to 30 colours does two things. At 4× it hardens the
crown's soft rim into what reads as a stray dark outline, and it collapses the
baked teal ground shadow from a gradient into a flat blob that reads as a
puddle. At **1×, which is the size players see, the quantized and the original
are all but indistinguishable** apart from that worse shadow.

So the colour count is not a defect to fix here. It costs a little file size
and nothing else, because the engine draws the sheet as-is and the extra
colours fall below the eye's resolution at game zoom. Leave supplied art alone.

The general rule this is a case of: a metric that separates our art from
vanilla is a *hypothesis* about how it will look, not a finding. Render both at
1× before acting on it.

`beetlewall` is not a counter-example to this. Its supplied sheet carried 15,299
colours and now ships at 36, but nothing was quantized: the sheet had to be
REDRAWN because its layout could not tile (see "in rows 1 and 2, columns 1 and
2 swap sides"), and a redraw is native pixel art whose colour count falls out of
its ramps. "Leave supplied art alone" still stands wherever the supplied
geometry is usable — `cloudmarblewall`, `cloudtree`, `nimbuswillow` are all
still shipped exactly as supplied.

## A window may only sit mid-run in a straight wall (verified in-game)

`WallWindowObject.isValid` (WallWindowObject.java:131) returns false whenever
`getWindowDir` returns -1, and `getWindowDir` (line 75) only accepts a window
whose connected walls form exactly ONE opposite pair:

```java
if (connectedWallUp && connectedWallBot)  return (!left && !right) ? 1 : -1;
else if (!connectedWallLeft || !connectedWallRight) return -1;
else                                      return (!up && !bot)   ? 0 : -1;
```

So a window tucked into a corner — wall above AND wall to one side — scores -1
and is deleted the moment the level validates it. Nothing logs, nothing throws;
the house simply has fewer windows than the preset drew.

The first Crooked House shipped 1 of its 3 windows for exactly this reason: two
of them sat at the inside of a stepped wall. The per-house survey in
`veilstatus` is what caught it (`walls=48/48 windows=1/3` on all three houses at
once — a consistent fraction, which is what distinguishes a systematic rule from
a placement accident).

**Rule:** every window tile needs wall above and below with open sides, or wall
left and right with open ends. Corners take a plain wall.

## A mod biome painted per-tile cannot use the world-preset ticket system

`GenerationPresetsWorldPreset` weights each entry by
`LevelPresetsRegion.biomeIDWeights`, and those weights are sampled from
`worldEntity.getGeneratorStack().getLazyBiomeID(...)`
(LevelPresetsRegion.java:62-116) — the **vanilla** biome generator.

Our sky and Veil biomes are written straight into the region's biome layer by
`SkyTerrainPainter` / `VeilTerrainPainter` and never pass through that
generator. A `SimpleGenerationPreset` scoped to one of them therefore scores a
biome weight of 0, gets clamped to a single ticket against the entry's
thousands, and effectively never places.

Extend `WorldPreset` directly instead, the way vanilla's own biome-independent
structures do (`SpiderNestsWorldPreset`, `VampireCryptWorldPreset`): implement
`shouldAddToRegion` on the level identifier alone, and do the site test with the
mod's own noise functions. `CrookedHouseWorldPreset` is the worked example.

Corollary: `presetsRegion.hasAnyOfBiome(ourBiome)` always answers false, so it
must not appear in `shouldAddToRegion`.

## Worldgen rarity constants are measured, not chosen

`VeilTerrainPainter.HOLLOW_THRESHOLD` was first set to 0.615 "so the Hollows
stay rare". It painted **23.9% of walkable ground** — a quarter of the layer.

The cheap way to get this right: the painters are pure functions of the world
seed, so a throwaway `main` with the mod jar on the classpath can sweep a
threshold over millions of tiles in seconds, with no server involved. That sweep
gave 0.660 -> 18.0%, 0.700 -> 11.8%, 0.740 -> 6.9%, **0.780 -> 3.6%**, 0.820 ->
1.6%, and the shipped value came from it. The in-game survey then confirmed
3.69%.

The same sweep sets structure placement: a 15x13 footprint whose four corners
and centre all land inside a Hollow fits on 0.129% of tiles, so the number of
placement ATTEMPTS is the real rate dial, not the points-per-region figure.
## A mod mob can wear a vanilla body sheet without shipping any art (content/arsenal)

**VERIFIED [jar] + [run].** `ResourceFolder` merges every mod's resources into
the SAME path-keyed map the base game's resources live in
(`ResourceFolder.java:102`, `this.files.put(pathStripped, ...)`), and
`GameTexture.fromFile` resolves through `ResourceEncoder.getResourceBytes(path)`
against that one map. Two consequences, both load bearing:

1. `GameTexture.fromFile("mobs/ancientvulture")` from a mod resolves the game's
   own sprite. There is no separate mod namespace.
2. A mod PNG at a vanilla path **overrides** the vanilla one (the loader counts
   these as `overrides` and logs them). So a derived/recoloured copy must never
   reuse the source's path.

But for a mob the simpler route is better still. `MobRegistry.Textures` is a
compiled vanilla class of ~800 `public static` fields, filled once by
`GameResources.loadTextures()`. A mod mob that **subclasses a vanilla mob and
does not override `addDrawables`** therefore renders from those fields with no
mod texture, no `loadTextures()` entry and no PNG at all. That is how the four
arsenal enemies (`FrostSentryMob`, `CryoFlakeMob`, `SpiritGhoulMob`,
`AncientSkeletonMageMob` subclasses) ship without a single new mob sheet.

**The one piece that cannot be inherited is the bestiary icon.**
`MobRegistry.MobRegistryElement.loadIcon()` is hard-wired to
`mobs/icons/<this stringID>` and `MobRegistryElement` is `protected static`
with no setter, so a mod cannot point it at the vanilla icon. Every registered
mob needs its own `mobs/icons/<id>.png` or it draws `GameResources.error` in
the kill list (`fromFile` falls back to the error texture rather than throwing,
which is why this fails silently).

## A vanilla mob's damage may be unreachable from a subclass (content/arsenal)

**VERIFIED [jar].** Whether a vanilla mob can be re-tuned by subclassing
depends entirely on where its numbers live, and the two shapes look identical
from the outside:

- `CryoFlakeMob`, `SpiritGhoulMob`, `AncientSkeletonMageMob` build their damage
  **inside `init()`** — either as a local (`new GameDamage(52.0F)`) or through a
  static the AI closes over. Overriding just `getLootTable()` is safe; changing
  the damage means re-declaring the whole behaviour tree.
- `FrostSentryMob` and `SwampShooterMob` keep it in a `public static GameDamage`
  field that the anonymous AI in `init()` reads. Writing to it from a subclass
  would change **every instance of the vanilla mob in the world**, so the only
  correct move is to rebuild the same AI shape against your own constant. That
  is what `RimeSentryMob.init()` does.

HP is the easy half: `setMaxHealth(n); setHealthHidden(getMaxHealth());` inside
`init()` after `super.init()` is vanilla's own idiom (`CryoFlakeMob.init` uses
it for its incursion bump).

## Projectiles: the registry, not the class, owns the sprite (content/arsenal)

**VERIFIED [jar] + [run].** `Projectile.init` assigns `this.texture` and
`this.shadowTexture` from `ProjectileRegistry.Textures` **by the projectile's
own registered ID**, so subclassing a vanilla projectile class and registering
it under a new stringID gives vanilla's behaviour with your own art and no
texture code:

```
ProjectileRegistry.registerProjectile("prismbolt", PrismBoltProjectile.class,
                                      "prismbolt", "bolt_shadow");
```

Paths are relative to `projectiles/`, and a null path leaves the field null.
Vanilla itself reuses one shadow across many bolts (`quartzbolt` ships
`quartzbolt` + the shared `bolt_shadow`), so a mod bolt needs one 18x18 sprite
and nothing else. `ProjectileRegistry` closes with the rest of the registry
list right after the mods' `init()` loop, so registration must happen there —
and before any item that names the projectile by stringID is constructed.

## A repeated `[section]` header in a .lang file resumes the section

**VERIFIED [jar].** `Translation`'s parser (line 136) does
`if (categories.containsKey(newCategoryName)) currentCategory = categories.get(...)`
— a second `[item]` block later in the file **appends** to the first rather
than replacing it. That is what makes it safe for a work stream to add its keys
as one contiguous block at the end of `en.lang`/`de.lang` instead of editing
five places in the middle of a file another agent may be holding.

## An `[itemtooltip]` key nobody calls is dead text

**VERIFIED [jar].** Nothing in the engine reads `itemtooltip.<id>tip` by
convention. A description only appears if the item class asks for it —
`getPreEnchantmentTooltips` for most weapons, `addExtraBowTooltips` for a
greatbow (so the line lands next to vanilla's own charge explanation), or the
three-arg `MatItem(stackSize, rarity, tooltipKey)` constructor for a material.
`tools/locale_audit.py` scans for the literal `Localization.translate(...)`
call sites, so a wired tooltip is checked and an unwired key is merely unused.
## A settler "profession" is a WORKSTATION, not a work zone (v0.9)

**[jar]** Necesse has two unrelated mechanisms behind "put a settler to work",
and only one of them is a zone.

`SettlementWorkZoneRegistry.registerCore` registers exactly three zones —
`forestry`, `husbandry`, `fertilize`. A `SettlementWorkZone` is a painted
AREA (`engine/util/Zoning`); it exists so a settler knows *where* to go
chopping, shearing or fertilizing.

A workstation is a single object. `SettlementStorageManager.assignWorkstation`
(`SettlementStorageManager.java:196`) builds a `SettlementWorkstation` for a
tile and keeps it only if `SettlementWorkstation.isTileValid()`, which is
`getWorkstationObject() != null`, which is `new SettlementWorkstationLevelObject(...)`
succeeding — and that constructor throws unless
`object instanceof SettlementWorkstationObject`. **That single `instanceof` is
the whole gate.** The settlement UI applies the same test client-side
(`SettlementAssignWorkForm.java:642`, "settlementcannotworkstation").

`ServerSettlementData.tickJobs` (`:786`) then publishes
`new UseWorkstationLevelJob(workstation, …)` for every assigned station every
tick, and `LevelJobRegistry.registerCore` (`:95`) files that job under the job
type **`"crafting"`** — the vanilla settler work priority. So a new station
needs **no new zone and no new job type**: implement the interface, and the
existing crafting priority already picks it up.

`UseWorkstationLevelJob.getJobSequence` branches on
`isProcessingWorkstation()`: a NON-processing station (a
`CraftingStationObject`) makes the settler fetch ingredients from settlement
storage, walk over and craft in place; a processing one makes them drop the
ingredients into the station's input inventory and haul the output away later.
Both are genuinely unattended.

**[run]** `/skyreachstatus` now measures both halves per station, and
`scripts/integration_test.sh` asserts them:

```
workstation windsilkloom  settlementWorkstation=true processing=false recipes=2 makes=skyweavex1+windsilkx1
workstation aetherforge   settlementWorkstation=true processing=true  recipes=2 makes=aetheriumbarx1+stormsteelbarx1
workstation stormglasskiln settlementWorkstation=true processing=true recipes=1 makes=stormglassx2
```

The second half matters as much as the first: a station whose `Tech` carries no
recipes is assignable in the UI and has nothing to do, and nothing in game says
why.

## A mod `Tech` needs a `[tech]` locale key nothing in the source writes

**[jar]** `RecipeTechRegistry.registerTech(stringID, itemStringID)` — the
two-argument overload — fills in the display name itself as
`new LocalMessage("tech", stringID)` (`RecipeTechRegistry.java:99`). The
crafting menu prints that through `tech.madein` ("Made in: <tech>") on every
recipe the station owns, so an unnamed tech reads `tech.aetherforge` on all of
them. Nothing in the mod's source contains the string `"tech"`, which is
exactly the blind spot `tools/locale_audit.py` exists to close; it now walks
`registerTech` call sites and requires the key in both locales.

The second argument is `Tech.itemStringID`, the item the tech is *named after*.
Ours is each station's own object item, which `ObjectRegistry.onRegister`
creates automatically from the object's string ID.

## A repeated `[section]` header in a `.lang` file RESUMES that section

**[jar]** `Translation.java:135`: when a `[name]` line names a category that is
already in `this.categories`, the parser sets `currentCategory` to the existing
one instead of creating a new one. So a locale block appended at the end of the
file may re-declare `[object]`, `[item]`, `[tech]` and so on, and its keys join
the sections above rather than replacing them. This is what makes per-stream
append-only locale blocks safe for parallel work.

## Vanilla furniture puts each ROTATION at a different row band

**[jar]** `BookshelfObject`/`CabinetObject` draw at
`pos(drawX, drawY - height + 64)`, `ClockObject` and `CraftingStationObject` at
`drawY - height + 32`, `DisplayStandObject` and the processing stations at
`drawY - (height - 32)`. Within one 128px sheet vanilla then shifts the art
per column, and the shift is not decoration:

| sheet | back (col 0) | side (1/3) | front (col 2) |
|---|---|---|---|
| `oakbookshelf` | rows 36..99 | 18..99, 12px wide | 16..77 |
| `oakcabinet` | 34..95 | 20..95, 16px wide | 12..73 |
| `oakclock` | 20..61 | 18..57, 12px wide | 6..47 |
| `oakdisplay` | 0..31 (all four columns identical) | | |

A case whose back is against the north wall stands higher on screen than the
same case turned around, and the side views are a narrow slab hugging the wall
edge — which is also what the class's own `getCollision` says (rot 1 is
`(x*32, y*32, 12, 32)` for a bookshelf, `16` for a cabinet). Drawing all four
columns bottom-aligned makes the piece jump a tile when the player rotates it.
`tools/sheet_format_audit.py` now holds our four pieces to those exact bands.

## `CheesePressObject`'s sprite index is a vanilla bug you must not copy

**[jar]** It reads `rotation % texture.getWidth() / 32`, which Java groups as
`(rotation % width) / 32` — 0 for every rotation 0..3 whatever the sheet is.
Harmless on `cheesepress.png`, which is 32px wide and has one column; silently
pins a four-column sheet to column 0. Every other station
(`ProcessingForgeObject`, `AlchemyTableObject`) writes `rotation % 4`, and so
does `StormglassKilnObject`.

## The forge's fire is a separate 32px animation row, and it fills the mouth

**[jar]** `ProcessingForgeObject.addDrawables` draws the body as
`sprite(rotation % 4, 0, 32, height - 32)` at `drawY - 32` and the fire as
`sprite(frame, (height - 32) / 32, 32)` at `drawY`, only when the fuel is
running and only at rotation 2 — the one rotation whose mouth faces the camera.
`forge.png` is therefore 128x96: four 32x64 body columns plus four 32x32 fire
frames on row 2, and the fire frames land over body rows 36..47.

**Measured:** each vanilla fire frame carries 184 opaque px filling rows 4..15
across x 8..23 — 96% of that rectangle. The fire is not a few tongues on a
transparent hearth, it is the whole mouth full of light with the opening's dark
rim drawn into the frame. Drawing thin flames on transparency there produces a
frame at a third of vanilla's mass that reads as a spark, not a furnace.

## A modded farm animal has no mate, so it cannot breed (v1.0 livestock)

**[jar]** Breeding is driven by the MALE.
`HusbandryImpregnateWandererAI.HusbandryImpregnateAINode.tickNode` only looks
for a partner when `mob.canImpregnate()` — `isGrown() && getGender() == MALE &&
tameness >= 1` — and then requires `mob.canImpregnateMob(other)` on top of
`other.canBirth()` (FEMALE). `HusbandryMob.canImpregnateMob` returns **false**,
so the male half is always an override, and every vanilla one is a hard string
test against a VANILLA id:

    RamMob      -> other.getStringID().equals("sheep")
    BullMob     -> other.getStringID().equals("cow")
    RoosterMob  -> other.getStringID().equals("chicken")
    BoarMob     -> other.getStringID().equals("pig")

No modded animal can satisfy any of them, and `SheepMob`/`CowMob` are
FEMALE-gendered, so a mod species registered as a single mob is female-only and
**breeds nothing at all**, however correct the rest of it is.

**[run]** The Cloud Lamb is in exactly that state today:
`husbandry check: cloudlamb ... child=cloudlamb name=Cloudlamb mate=NONE`. Its
`getRandomChildMobStringID` override is correct and unreachable — nothing can
impregnate it. `docs/CURRENT_STATE.md` describing the lamb as "breeds true" is
a claim about one override, not about the loop.

**[jar]** `getGender()` is asked of the INSTANCE everywhere it is read —
`HusbandryMob.canBirth`/`canImpregnate` and `SettlementHusbandryZone.tickJobs`,
which sorts a zone's animals into males/females/neutrals for the slaughter
ratio. Nothing treats gender as a property of the mob TYPE. So one registered
mob can carry both sexes: roll it on the server in `init()` behind an
"is it still unset" guard (the idiom `ChickenMob.init` uses for its first egg
timer, and it is order-independent against `applyLoadData`), save it, and send
it in the spawn packet so the client picks the right sheet.
`stairwaytoheaven.livestock.SkyBreed` is that, and `canImpregnateMob` becomes
`other.getStringID().equals(getStringID())`.

**[run]** Measured on all three new animals:
`husbandry check: nimbusyak ... mate=nimbusyak`, `thunderquill ...
mate=thunderquill`, `glimmergoat ... mate=glimmergoat`.

## Livestock CAN be table-spawned — the mob just has to say so

**[jar]** `MobChance.spawnMob` drops a mob whose `isValidSpawnLocation` answers
false and `Mob`'s own implementation is `return false`; nothing in the
husbandry chain overrides it. That is the whole reason vanilla places sheep,
cows and chickens from the island generator. It is **not** a rule that
livestock cannot be table-spawned: implementing the method is enough, and
`CritterMob`'s entire implementation is one line —
`new MobSpawnLocation(this, x, y).checkMobSpawnLocation().validAndApply()`.
`Mob.checkSpawnLocation` already means "not in liquid, not on a solid tile, not
indoors on a floor, not colliding".

**[jar]** Nothing sets `canDespawn` for a husbandry mob: the field is a plain
`boolean` defaulting to Java's false, and only `HostileMob` and `CritterMob`
set it true. So a table-spawned farm animal is permanent and saves into its
region for free — which is what makes a biome table an acceptable substitute
for an island generator here, and it is why the local cap belongs in the check
(`checkMaxMobsAround(6, 14, HusbandryMob::isInstance)`) as well as in
`addLimited`.

**[run]** `spawn check: nimbusyak threshold=0 validSpawnLocation=implemented
accepted lit=5/6 dark=5/6`, the same for thunderquill and glimmergoat, against
`cloudlamb ... INHERITS Mob's false accepted lit=0/6 dark=0/6` in the same log.

## Vanilla resources are reachable from a mod, and that is enough art for a reskin

**[jar]** `GameTexture.fromFile` formats the extension, looks the path up in
`GameTexture.loadedTextures` and on a miss reads it through
`ResourceEncoder.getResourceBytes(path)` — ONE flat `resources.files` map keyed
by path (ResourceEncoder.java:75-86) with mod resources merged into it. So
`mobs/cow`, `player/armor/clothhat` and `items/milk` resolve from mod code
exactly as `mobs/cloudlamb` does.

**[jar]** Every hook needed to point an item or a mob at one of those is
open: `Item.loadItemTextures` is protected (Item.java:562),
`ArmorItem.loadArmorTexture` is protected and `armorTexture`,
`frontArmorTexture`, `backArmorTexture` are public (ArmorItem.java:84-86), and
mob sheets are our own static fields anyway. Vanilla does the same thing to
itself: `FoodConsumableItem.loadItemTextures` crops a crop sheet, and
`FoodConsumableItem.loadTextures` composites the buff icon out of the item icon
pixel by pixel at load time.

**[jar]** Two mechanics of that pixel work are easy to get wrong.
`GameTexture.fromFile(path, true)` (forceNotFinalize) is how vanilla asks for a
READABLE texture; `makeFinal()` uploads to the GPU and drops the buffer, after
which any read costs a `glGetTexImage` round trip through `restoreFinal()`. And
a texture handed to something that reads it again — a `FoodConsumableItem`'s
item icon — must be left un-finalized, because that class finalizes it itself.

**[run]** The whole v1.0 livestock layer ships **zero new PNGs**: three animals
(both sexes, young, sheared/plucked states), eight item icons and two armour
sheets, all recoloured from vanilla at load time by
`stairwaytoheaven.livestock.SkyPelt` (hue and saturation replaced, VALUE kept,
which preserves vanilla's shading and silhouette). `tools/locale_audit.py` was
extended rather than exempted: `ITEM_CLASS_VANILLA_ICON` maps each such class
to the vanilla file it reads, a new check 8 resolves EVERY literal texture path
in our source against `src/main/resources` or the vanilla dump, and both were
proven to fail on a deliberately mistyped path.

## Chicken egg-laying is hardcoded to vanilla, in three separate places

**[jar]** `ChickenMob.ChickenLayEggAINode.tickNode` builds its
`ProcessObjectHandler` inline and its `process()` is
`new InventoryItem("egg")`. `EggNestObject.getLayEggHandler` does the same.
And `EggFoodConsumableItem.getHatchMobStringID` returns `"rooster"` or
`"chicken"`. There is no hook anywhere in that path, so a mod bird laying eggs
lays VANILLA eggs, and — worse — `ChickenMob.onImpregnated` does not give
birth, it sets `nextEggIsFertilized` and lets the nest hatch the egg, which
means **a modded fowl bred through the vanilla path produces vanilla
chickens**.

The way out is not to reimplement the AI node. `canShear`/`onShear` are open
hooks on `HusbandryMob` itself, not on `SheepMob`, and
`ShearsItem.canMobInteract` is `mob instanceof HusbandryMob && canShear(item)`
— so the bird's own product is taken with shears, on the same
20-to-30-in-game-minute regrow timer vanilla's fleece uses, and the inherited
vanilla egg is kept as a bonus. Breeding is fixed by overriding
`onImpregnated` to give birth live, which is what `HusbandryMob.onImpregnated`
does for every other animal in the game.

**[run]** `husbandry check: thunderquill shear=stormdownx2 milk=NO
child=thunderquill name=Thunderquill Fowl mate=thunderquill`.

## The integration test is seed-flaky in two known places

**[run]** `scripts/integration_test.sh` creates a NEW world with a random seed
on every run. Two assertions are sensitive to it and were both seen failing on
otherwise-good builds while the same build passed on the next run:

  * the Skyway tree count, whose own comment already admits it is probabilistic
    (`203 tiles of Skyway paving and not one tree on it` — with about three
    expected, zero is ordinary sampling);
  * the cats' home flags after a restart, on a seed where the coax and the
    `stop` fall about two seconds apart.

**[run] A third, added 2026-08-28 (content/itempolish):** `skyfall run:
restored=false remainingms=120000` after the restart, i.e. the Skyfall started
a NEW shower on the restarted server instead of resuming the one the world was
saved mid-way through. Observed once, then passed twice in a row on the same
build with `restored=true remainingms=890065`; the same commit's parent
(`0d1761c`) failed its own run on the cats instead. The shard list itself
survived in the failing run (`placed=12 live=12 inworld=12`), so what wobbles
is the restore flag, not the data.

Neither is caused by the change under test when it also passes cleanly on the
next seed. Re-run before believing a single red run, and say which run the
result came from. On 2026-08-28 the run counts were: `content/itempolish` red
once (skyfall + cats), then green twice; `master` red once (cats) on the run in
between.

**UPDATE 2026-08-28 (feature/catbasket): the second and third of these were not
seed flakiness at all.** The cats' home flags and the Skyfall's `restored` flag
are both world-scoped state written by the same world save, which is why they
went red together and green together — the run had lost vanilla's stop-save
race and come back at the previous autosave. Root cause, log signature and the
fix are under "A dedicated server's `stop` can save NOTHING" at the end of this
file; the test now saves explicitly and waits before stopping. The Skyway tree
count remains genuinely probabilistic.

## Surface structures are placed by the WorldPreset system, not by a level hook

**[jar]** `SurfaceLevel.generateRegion` (necesse/level/maps/SurfaceLevel.java:23)
brackets every region it generates with
`worldEntity.startPresetGenerationInRegion(region, this.seed)` and
`runPresetGenerationInRegion(...)`. Those resolve to `WorldPresetsRegion` →
`LevelPresetsRegion`, whose queue is filled once per **1024x1024 preset region**
(`WorldPresetsRegion.tileWidth = 1024`, `PRESET_REGION_REGION_SIZE = 64`) by
`WorldPresetRegistry.initRegion`, which walks every registered `WorldPreset` and
calls `addToRegion` on the ones whose `shouldAddToRegion` accepts the level.

`PresetGeneration` (necesse/level/maps/generationModules/PresetGeneration.java)
is a **different, finite-level** mechanism and is NOT how the streamed surface
gets its structures. It is easy to find first and wrong to copy.

Vanilla's own surface structures are one registry entry:
`registerPreset("surfacepresets", new SurfacePresetsWorldPreset())`, a
`GenerationPresetsWorldPreset` holding a ticket-weighted list of
`SimpleGenerationPreset`s at `presetsPerRegion = 0.05F`. A mod adds structures by
registering its **own** `GenerationPresetsWorldPreset` in `init()` — never by
touching vanilla's list or the level class.

**[run]** Measured against the real thing: our own list at
`presetsPerRegion = 0.0035F` queues **14.25 structures per 1024x1024 preset
region** while vanilla queues **~220** into the same regions (878 over four).
So the density knob behaves linearly and vanilla's own number is the yardstick
to be rare against.

### Two guards make a world preset unable to overwrite anything

**[jar]** `LevelPresetsRegion.startGenerateRegion` sets
`hasAlreadyGeneratedRegion` on any queued preset one of whose occupied regions is
already generated, and `runGenerateRegion` then skips it entirely. A player can
only build in a region that has generated, so a structure placed this way can
never land on a build. `SimpleGenerationPreset` additionally checks and claims
the `"villages"`, `"minibiomes"` and `"loot"` occupied-space boards, and
`PlaceableWorldPreset` gives every SURFACE preset
`removeIfWithinSpawnRegionRange = SpawnTileFinder.CLEAR_SPAWN_REGION_RANGE`.

**[jar]** Registration order matters and is free: `WorldPresetRegistry`
`onRegistryClose` sorts by `-priority`, every vanilla preset uses the default
priority 0, and vanilla's `registerCore()` runs before any mod's `init()`. So a
mod preset is evaluated last and sees every vanilla structure already on the
boards.

### When a `GenerationPreset` is actually constructed

**[run]** `GenerationPresetsWorldPreset.onRegistryClosed()` calls
`addCorePresets()`, and `addPreset` immediately calls
`SimpleGenerationPreset.init()`, which calls `getPreset(...)` — so the `Preset`
subclass runs its constructor **at registry close**, between `ModEntry.init()`
and `ModEntry.postInit()`. Anything the preset names must therefore be resolved
during `init()`. In this repo that rules out `SkyRegistry.skyroadTileID` and
`skyplinthTileID`, which `SkyBuildingSet.resolveWorldgenMaterials()` assigns in
`postInit()` and which would still be 0 (= `emptytile` / `air`).

**[jar]** That failure is silent in both directions: `ObjectRegistry.getObjectID`
and `TileRegistry.getTileID` answer **−1** for an unknown name
(`GameRegistry.getElementID`), `Preset.setObject(x, y, −1)` means *leave this
cell alone*, and **0** means *air*, which clears the cell. One typo therefore
deletes part of a structure without throwing, warning, or failing the build.
`stairwaytoheaven.surface.SurfaceMaterials` routes every lookup through one
recorder so `/skysurfacestatus` can report `unresolved=0`, and the integration
test fails the day it is not.

### Queued is not stamped — measure both

**[run]** `LevelPresetsRegion.getLevelRegions(identifier, 0)` and
`getDebugData()` let a probe read the whole queue for a 1024x1024 preset region
**without generating a single tile** (4 preset regions in 1–300 ms), and
`PresetDebugData.getDebugName()` carries
`preset.getStringID() + ":" + index + "@hash\n" + debugName`, where
`SimpleGenerationPreset.addToRegion` has already set `debugName` to the
generation preset's class simple name. Passing `customSeed = 0` is correct for
the Surface: `BiomeGeneratorStackLevel.seed` is 0 there, and a non-zero custom
seed would make `initRegion` use the SAME random for every preset region.

That measures the decision. It does not prove anything was written, so
`/skysurfacestatus stamp` then filters the queue for sites none of whose regions
are generated yet, calls `WorldPreset.ensureRegionsAreGenerated` on one, and
counts the structure's signature object in the world. Measured: 2 sites per POI
kind generated, 2 stamps counted, and 4 `aetheriumrock` / 2 `aeronautwreck` /
2 `seraphstatue` standing in them.

### `bigtent` is vanilla's 2x2 multi-tile, and a preset must write all four

**[jar]** `BigTentObject.registerTent` registers the four parts at multi
coordinates (0,0), (1,0), (0,1), (1,1) — `bigtent`, `bigtent2`, `bigtent3`,
`bigtent4` — laid out as two rows of two. Since `Preset.applyToLevel` never runs
`MultiTile.placeObject`, all four have to be written, exactly as vanilla's
`TravellersCampsitePreset` does. Verified by counting them in a stamped camp:
`bigtentx1,bigtent2x1,bigtent3x1,bigtent4x1`. The same rule caught the Storm
Crystal pair (`stormcrystal` + `stormcrystalr` on the tile to its right).

### Chest loot and sign text come from `addCustomApply`, at stamp time

**[jar]** `Preset.addInventory(lootTable, random, x, y)` is an `addCustomApply`
that reaches `level.entityManager.getObjectEntity(x, y)`; the entity exists
because `ObjectRegionLayer.setObjectByRegion` calls `level.replaceObjectEntity`
on every base-layer change. A sign is the same pattern with
`SignObjectEntity.setMessage(GameMessage)` — a `LocalMessage`, so each reader
gets their own language (vanilla: `AbandonedCampPreset`).
**[run]** Both observed in stamped POIs: `chestitems=3` and the sign's real
English text rather than `misc.<key>`.

## `WorldEvent` is the world-scoped event system, and its load order matters

**[jar]** `necesse/engine/world/worldEvent/`. `WorldEntity.serverTick` ticks
every live event and drops it as soon as `isOver()`; `addWorldEvent` runs
`init()` and sends a `PacketWorldEvent` to every client; `shouldSave` puts it in
the world file through `WorldEventSave`. Vanilla registers exactly one
(`"ascendedflash"`, a pure client visual that calls `over()` on the server
immediately). `WorldEventRegistry.registerEvent(stringID, class)` registers by
**class** — the registry instantiates reflectively, so a public no-argument
constructor is mandatory — and throws outright for a client-side-only mod.

**[jar]** Ordering, and it is not the obvious one: both `applyLoadData` (save)
and `applySpawnPacket` (network) run **before** `init()`, and
`WorldEntity.applyLoadData` restores `EVENTS` **before** `WORLDDATA`. So an
event cannot introduce itself to a `WorldData` scheduler from `init()` — the
scheduler is not loaded yet and would be replaced a moment later. Doing it on
the event's first `serverTick()` works, because the events list is ticked before
the world data list in the same `WorldEntity.serverTick`.

**[jar]** `WorldEntity.time` IS saved and restored (`save.addLong("time", ...)`),
so a deadline expressed as `getTime() + duration` survives a restart.

**[run]** A recurring schedule wants a `WorldData`: `WorldEntity.serverTick`
ticks every one of them, right beside the events. But `WorldData` is created
lazily on first access, so it only starts ticking once something asks for it —
`GameEvents.addListener(ServerStartEvent.class, ...)` is the hook that
guarantees it exists on every world, and it fires from
`Server.markWorldInitialized`, i.e. after `world.init()` has loaded the world
entity. `WorldData.tick()` runs on the client too; guard with `isServer()`.

**[run]** Proven end to end by `scripts/integration_test.sh`: phase 1 starts a
Skyfall and leaves it running with 12 shards on the ground, the server is
stopped mid-event, and phase 2 reports
`skyfall run: restored=true ... live=12 inworld=12` followed by
`skyfall clean: cleared=12 leftbehind=0 over=true`. The shard list has to be
saved with the event for that to work; the cleanup calls
`regionManager.ensureTileIsLoaded` per tile so a shard whose region has streamed
out is still removed, and it only clears a tile whose object is still ours, so a
shard the player mined or built over is left alone.

**[jar]** Announcements:
`server.network.sendToClientsAtEntireLevel(new PacketChatMessage(new
LocalMessage(...)), LevelIdentifier.SURFACE_IDENTIFIER)` is vanilla's own idiom
(`IncursionLevelEvent:522`), and vanilla's event lines carry colour codes in the
locale value (`raidapproaching`, `bossapproaching` are both `§b`).

## An object's WORLD sheet is a separate file from its item icon

**[jar]** `GameObject.loadTextures()` reads whatever the class says; only the
ITEM icon defaults to `items/<stringID>.png`. `SkyDecoObject` takes its texture
NAME as constructor argument 0
(`GameTexture.fromFile("objects/" + textureName)`), which is allowed to differ
from the registered ID — `SkyfallShardObject` registers `skyfallshard` and draws
`objects/starfall.png` on purpose. A missing world sheet fails exactly like a
missing icon: `GameTexture.fromFile` swallows the exception and returns
`GameResources.error`, so the player sees an ERR tile **standing on the ground**.

`tools/locale_audit.py` only checked item icons, and got away with it purely
because every object so far happened to name its sheet after its own ID. Check 7
(`OBJECT_TEXTURE_BY_CLASS` / `check_world_textures`) closes that: 20 objects now
have their world sheet verified, and a listed class whose texture argument is
not a literal is reported rather than skipped. Negative-tested by pointing the
shard at a nonexistent sheet — the audit fails, naming the file it wanted.

## `Localization.translate` never reports a miss, and `translationExists` lies

**[jar]** `Localization.getTranslation` (`engine/localization/Localization.java`)
falls through to `new DebugTranslationElement(category, key)` when neither the
current language nor English has the key, and `DebugTranslationElement`'s
translation is literally `category + "." + key`. So a missing entry is not
absent — it is the raw key, printed at the player, exactly like the three
display-name bugs `tools/locale_audit.py` already exists to catch.

The obvious guard is **not usable**: `Language.translationExists` reaches
`Translation.exists` (`fileLanguage/Translation.java:216`), which is

```java
TranslationCategory cat = this.categories.get(category);
return cat != null ? cat.exists(key) : true;
```

— it answers **true** when the whole category is missing, because it exists for
translation-coverage tooling rather than for lookups. The reliable test is to
call the three-argument `translate(category, key, debug=false)` and compare the
result against `category + "." + key`; that is what
`stairwaytoheaven.items.ItemDescription` does, and `debug=false` also keeps a
deliberately description-less item from printing
"Translation of … is not found." into the log every ten seconds.

## Only a MatItem has a per-item description hook; armour and trinkets are final

**[jar]** `Item.getBaseTooltips` (`inventory/item/Item.java:189`) is exactly
three blocks — display name, debug block, `getCraftingMatTooltips` — and the
last one only ever yields the generic "used as crafting material" sentence
carried by the `Tech`s that consume the item and by its global ingredients. An
item nothing consumes says nothing; an item something consumes says only that
it is a material. Neither tells the player whether he is holding an ore, a
petal, a bolt of cloth or a bar.

- `MatItem.getTooltips` is overridable and already appends
  `Localization.translate("itemtooltip", tooltipKey)` when a tooltip key was
  passed to its three-argument constructor.
- `ArmorItem.getTooltips` is **`final`** (`ArmorItem.java:212`). The way in is
  `getPreEnchantmentTooltips`, which is where vanilla itself puts the
  "Head slot" / "Chest slot" lines.
- `TrinketItem.getTooltips` is **`final`** too (`TrinketItem.java:113`), same
  way in.
- `FoodConsumableItem.getTooltips` is ordinary and can simply be overridden.

Vanilla's naming convention for the line is `[itemtooltip] <stringID>tip`
(`surgicalmasktip`, `voidshardtip`, `glassbottletip`, ~80 others).

## The item category tree is what the SORT button reads, not the settlers

**[jar]** `Item.compareTo` (`Item.java:1014`) compares
`ItemCategory.masterManager.getItemsCategory(this)` first and only falls back to
the display name inside one category, and `Inventory.sortItems`
(`Inventory.java:1217`) is what every chest's sort button and
`OEInventoryContainer` call. So the category tree literally decides where an
item lands when a chest is sorted.

`SettlementStorageManager` contains **no** reference to `ItemCategory` at all —
a settler hauling to storage does not sort by category. "So the settlers file
it properly" is therefore half true: the category is what makes the chest sort
right, not what makes the settler choose a chest.

**The `materials` leaves vanilla creates** (`ItemCategory.java:213-222`) are
`ore`, `minerals`, `bars`, `stone`, `logs`, `specialfish`, `flowers`,
`mobdrops`, `essences` — `flowers` (`FlowerObject.java:50` files every picked
flower there, plus `mushroom` by hand) and `stone`/`essences` are easy to miss.
`misc/questitems` (`ItemCategory.java:286`) is where `QuestItem.java:29` files
all 32 vanilla quest items. Bare `materials` is where vanilla puts a crafted
intermediate that belongs to no family — `glass`, `glassbottle`.

## The chest sheet's attack sprites are addressed on a 32px grid

**[jar]** `ChestArmorItem.getAttackArmSprite` returns
`new GameSprite(armorTexture, 0, 8, 32)`, and that constructor is
`(texture, spriteX, spriteY, spriteRes, size)` — a **32px** grid. Sprite (0, 8)
is therefore the pixel block (0,256)-(32,288): the top-left QUARTER of cell
(column 0, row 4) of the 64px sheet, i.e. half-res x0..15, y0..15.

Vanilla `tungstenchest` row 4 accordingly holds BOTH its clusters inside the
first 64px cell — the sleeve at half-res x8..11 / y7..11 and a second at
x23..27 / y4..7. **`gen_armor._mantle_attack_row` puts its cuff cell in column
1**, which is pixels (64,256)-(96,288) = sprite (2, 8), so the Warden's mantle
draws nothing over the back of the hand. Not fixed here (the Warden's art
belongs to another stream); `gen_skygear` copies vanilla's placement instead.

## Vanilla armour is DARK: measure the value spread, not just the mass

**[jar/measured]** `player/armor/tungstenhelmet.png` spans luminance 38..187,
and its **most common colour is the darkest one** — 8120 of 22064 opaque pixels
(37%) at luminance 38. A three-step ramp whose darkest step is luminance 65
covers the same opaque mass and still reads as one flat mid-tone blob on a
composited body, which `size_audit.py` cannot see because mass is all it
measures. `gen_skygear` therefore mixes a fourth step toward the outline colour
and shades in four planes.

Two further things only a composited mock catches, both found that way here:
a repeating row of bright glints inside a helmet's visor slit reads as **teeth**
once the helm is on a head, and a shaded dome with no face plate reads as a
balloon. Compose the armour onto `player/skin/{head,body,feet,arms_*}` before
believing any armour sheet.

## An audit that reads comments as code cries wolf

**[run]** `tools/locale_audit.py`'s `check_registration_wrappers` matched the
words `registerTech(stringID, itemStringID)` inside a PROSE COMMENT in
`SkyreachStatusCommand` and reported a correct, documented probe as a
registration hiding an ID behind a variable — a red gate on `master` with
nothing wrong. The audit now blanks comment bodies (and, for the brace-depth
scanner, string contents) before matching, preserving every offset so findings
still name the right line.

## The cats' home has to be WorldData, and a mob crosses a level with `TeleportEvent`

**[jar]** `furnitureType` is *only* a bucket key. `SettlementRoom.calculateStats`
(SettlementRoom.java:114-115) counts an object toward a room at all only when it
is `instanceof RoomFurniture`, and files it under `getFurnitureType()`;
`getFurnitureScore()` is `sum(count^0.44)` over those buckets, fed to
`SettlerThoughtRegistry.getRoomQualityThought`. Nothing in the 1.3.2 jar calls
`getFurnitureTypes(String)` with a particular name, and **"petbed" does not
appear in vanilla at all** — so the Cat Basket's `furnitureType = "petbed"`
names its own bucket and is worth exactly one distinct piece of furniture to a
room. Keeping it is right; the thing that actually has to stay is
`extends FurnitureObject`, because that is what implements `RoomFurniture`.

**[jar]** `GameObject.placeObject(level, layerID, x, y, rotation, byPlayer)` is
the single funnel every placement goes through — `ObjectItem.onPlaceObject`
(ObjectItem.java:401) calls it for a player's own placement, and so do the
settler build path and worldgen. `Level.setObject` does NOT go through it, which
is why `SkyLevel.setQuestObject` healing the spire's basket onto old worlds
cannot claim a home the player never chose. The mirror is
`GameObject.onDestroyed`, reached from `LevelObject.destroy`,
`DamagedObjectEntity.destroyObject` (DamagedObjectEntity.java:227) and
`PacketTileDestroyed`. Both run on the client too — guard with `level.isServer()`.

**[jar]** `setPos` cannot move a mob to another level; it moves it inside the
level it is filed under. The cross-level mechanism is
`necesse.entity.levelEvent.TeleportEvent(Mob target, int delay,
LevelIdentifier targetLevel, float sicknessTime,
Function<LevelIdentifier,Level> destinationGenerator,
Function<Level,TeleportResult> destinationCheck)`, added with
`level.entityManager.events.add(event)` — `SettlersWorldData.returnToSettlement`
(SettlersWorldData.java:161) is the worked example on a mob.

  * The position in a `TeleportResult` is in **PIXELS**. `performTeleport` feeds
    it to `setPos` / `changeMobLevel`, and `EntityManager.changeMobLevel` passes
    it to `addMob(mob, (float) x, (float) y)`. Tiles here would land the mob 32x
    closer to the origin.
  * `destinationGenerator` may be `null`: `World.getLevel(identifier, null)`
    still loads or generates (World.java:273-299).
  * `delay = 0` makes the whole move **synchronous inside `events.add`** —
    `LevelEventsManager.addHidden` calls `event.init()` directly, and `init()`
    with no delay calls `performTeleport()` there and then.

**[jar]** A mob that changes level does NOT get `init()` again.
`EntityList.addHidden` (EntityList.java:205-209) calls `init()` only for an
entity that was never initialised and **`onLevelChanged()`** for one that was.
Anything rebuilt in `init()` — for `SpireCatMob`, the `HomesickCritterAI` home
tether — has to be rebuilt in `onLevelChanged()` too, or the mob arrives in its
new home still tethered to the tile it left, on a level that tile is not on.

**[jar]** `HomesickCritterAI.homeTile` is a tile on the mob's OWN level; the
node walks the critter toward it. A tile belonging to another dimension is a
place it can walk toward forever, so the tether must only ever be set when the
home is on the same level — travelling is the teleport's job, not the
pathfinder's.

**[jar]** `SkywatchQuestData.get(level)` **creates** the level data when it is
missing. Calling it on the Surface for a cat that moved there would silently
mint a record with basket and lair at 0,0 — and a cat tethered to 0,0 is the
same bug class as a cat teleported into the void. Read the sky's quest data only
when the level IS the Skyreach, and keep everything about a cat that can live
anywhere (the coaxed flags, the home tile, the home's level identifier) in
`SkywatchWorldData`, which is `WorldData` and survives a level unload or a
generation bump. `LevelIdentifier.stringID` matches `[a-z0-9-+]{1,50}`
(LevelIdentifier.java:13), so it round-trips through `addSafeString` unharmed.

## A dedicated server's `stop` can save NOTHING, and that was the "flaky" test

**[jar]** `Server.stop` (Server.java:986-1011) sets `stopCalled = true` **first**,
prints "Starting world save", and only then builds the `ServerSaveHandler`.
`Server.tick` (Server.java:296-303) runs, on the server thread,

```java
this.tickSaveHandler(this.stopCalled);
if (this.stopCalled) { if (this.saveHandler == null) { this.privateStop(); } }
```

so if constructing the handler takes longer than one tick, the server thread
observes `stopCalled` with `saveHandler` still null and stops outright.
`privateStop` disposes every level and the world; the handler the other thread
finishes building is then never ticked. **The world file is left at the last
autosave and everything after it is gone.**

**[run]** The log signature is exact: `Starting world save` and
`Stopped server ... SERVER_STOPPED` in the same second, with **no** "Completed
world save before stopping server" line, and no exception anywhere. A healthy
stop-save on the same machine takes 16-18 seconds.

**[run]** Construction is not free — `RegionManager.getSaveHandler`
(RegionManager.java:271-275) takes each level's `entityManager.lock` and
`RegionFilesManager.getSaveHandler` (RegionFilesManager.java:218-236) calls
`getWorldRegion(rx, ry, true)` for **every loaded region** — so the more regions
a run has loaded, the more reliably it loses the race. Measured: the cat-basket
build, which force-loads the basket and lair regions on two levels, lost it
**twice out of two**; the identical jar with only the basket placement removed
from the test saved for 18 seconds and passed. So this is a vanilla race that a
region-heavy run makes near-certain, not a mod bug — but the data loss is real.

**[run]** `scripts/integration_test.sh` now issues the `save` console command
and waits for "Completed world save" before it ever sends `stop`.
`SaveServerCommand` calls `startFullSave` **without** `stopCalled` set, so
nothing can stop underneath it. This is almost certainly what the section above
("The integration test is seed-flaky in two known places") was really seeing:
the cats' home flags AND the Skyfall's `restored=false` are both world-scoped
state, both are written by the same save, and both come back stale together —
which is precisely what losing the stop-save looks like. Both were red on the
same runs and green together once the explicit save went in.

## `size_audit.py` passed by measuring nothing, on every machine but one

**[run]** `AGENTS.md` documents the gate as `python3 tools/size_audit.py`, no
arguments. Its `--vanilla` default was the hard-coded path
`/home/user/necesse-game/sprites` — a dev-container location. Anywhere else,
every pair resolved to "vanilla ref missing", `flagged` stayed 0, and the script
printed **"0 sprite(s) flagged below 75% of vanilla mass"** and exited 0.

Measured on this checkout before the fix: **0 of 122 rows actually compared**,
119 of them reporting a missing reference, and the run still passed. That green
tick is quoted in `docs/CURRENT_STATE.md` as verified. It meant nothing unless
whoever ran it happened to pass `--vanilla vanilla-sprites` by hand.

Two changes, both in `tools/size_audit.py`:

- `default_vanilla()` prefers this checkout's own `vanilla-sprites/` when it
  holds an `items/` directory, falling back to the old path. The dump is
  gitignored on purpose but it is where it actually lives in a working tree.
- The summary now prints how many rows compared, how many are manual, and how
  many had no reference, and a run where **nothing** was measured exits 1 with
  `FAIL: nothing was measured`. A gate that compares no sprite is not a pass.

**The general rule this is the third instance of:** ask what a green gate
actually measured, not whether it was green. `sheet_format_audit` passed the
Beetlefreak wall because it guards cell geometry, not whether art tiles.
`wall_render_preview` drew both faults without complaint because it only ever
composed our own sheet. This one passed because it compared nothing at all.

## The audit only sees what it is pointed at — 207 of 307 PNGs were invisible

**[run]** `size_audit.PAIRS` is a hand-maintained mapping, so a sprite with no
row is simply never measured. Cross-checking every shipped PNG against the
mapping: **100 of 307 covered, 207 not**. Among the uncovered were 94 32x32 item
icons, **47 of which sit below the thinnest vanilla item icon in the dump**.

Measured reference, from `vanilla-sprites/items/` (34 icons at 32x32):
opaque mass **288–712 px, median 440**, bbox at least 16x20 and typically 24x26.
Against that, the mod shipped `flickerlightgarland` at 29 px, `tempestedge` — one
of its two original weapons — at **45 px**, `veilessence` 70, `ghostlantern` 77,
`wardencandelabra` 78, `stormshard` 85.

`tempestedge`'s cause is visible in the generator: `gen_items._tempest_blade`
draws the blade core as single stacked pixels per diagonal step. The helper's own
docstring warns that the outline pass eats thin diagonals and says to lay a dark
mass first — it does that for the silhouette, then puts a 1px core on top, so the
finished icon is a hairline. `docs/REVIEW-2026-08-24.md` listed widening this
blade as art action **#1** and it stayed undone for four months.

**[run]** Both held weapon sprites are on a **32x32** canvas while every later
mod weapon matches vanilla's much larger held sheets — `skyreave` 96x95 against
`quartzglaive` 104x88, `thunderhead` 22x62 against `tungstengreatbow` 20x60. So
`tempestedge` and `galehowl` are drawn at roughly a third of the linear size of
everything else in the player's hand. Their audit rows are deliberately manual
(a mass ratio between canvases differing 3x measures the canvas, not the art).
**Open question, not fixed here:** changing that canvas is rendering geometry,
not art, and wants its own verified pass.

## Driving a second agent non-interactively as an art worker

**[run]** `codex exec` takes a brief on stdin/argv and runs headless:

```bash
codex exec -C <repo> -s workspace-write --skip-git-repo-check \
  -c model_reasoning_effort="high" -o <report.md> "$(cat brief.md)"
```

`-C` scopes it to one worktree, `-o` captures its final message, and
`/home/blackoffset/dev` is already `trust_level = "trusted"` in
`~/.codex/config.toml`, so subdirectories inherit trust. It is NOT a Claude peer
session and does not appear in `ListAgents`; `codex queue --thread <id>` talks to
an already-running interactive session instead.

What made the hand-off work, in the order it mattered:

- **Numeric acceptance criteria in the brief.** Every icon got its vanilla
  analogue *and* that analogue's measured mass, plus a floor (>=300 px, bbox
  >=20x22) and a self-check script to run before reporting. The worker's
  criterion and the reviewer's gate then rest on the same fact.
- **Explicit file ownership, including a deny list.** `tools/size_audit.py` was
  being edited in parallel and was named as forbidden; it came back untouched.
  Workers do not commit — `docs/AGENT_WORKFLOW.md` — and saying so in the brief
  is what makes that true.
- **Pillow is not installed system-wide here**, so the exact
  `PYTHONPATH=/home/blackoffset/dev/pylib python3 …` command belongs in the
  brief verbatim, or the worker's own QA silently does not run.

**[run]** What the numbers could not catch, and a human read of a contact sheet
did: the redrawn `aurorapetal` came back at 565 px as a full five-petal bloom
with leaves and a stem — a **fuller flower than `aurorabloom` itself** (141 px,
untouched, sitting next to it in the inventory). Every acceptance number passed.
The brief caused it: "must read as *that* plant" was meant as "keep the plant's
identity", and was read as "draw the plant". **Say what the SUBJECT is, not only
which family it belongs to** — for a picked material, name it as picked.

## This Linux box: what it had, and what had to be built (2026-08-29)

**[run]** The v0.6 notes above describe a Mac with a dedicated server in
`~/Downloads`. This machine (WSL2, Ubuntu, Python 3.14, Temurin 21) started with
**none** of the toolchain: no game jar, no decompiled tree, no Pillow, no pip, no
`python3-venv` that can bootstrap pip, and no passwordless sudo. Every gate
except `locale_audit.py` was therefore unrunnable, and `gradlew` could not
resolve a game jar at all.

All three are fixable without root, and this is how:

- **Game jar.** `https://necessegame.com/server/` lists every build, not just the
  newest, as presigned S3 links of the form
  `.../server/<ver>/necesse-server-linux64-<ver>.zip?X-Amz-...`. The version the
  mod pins, `1-3-2-24650233`, is still offered. It is unzipped at
  `/home/blackoffset/dev/necesse-server-1-3-2-24650233/necesse-server-1-3-2-24650233`
  (the zip contains its own top-level directory, so the path repeats) and that
  is what `NECESSE_GAME_DIR` must point at — it holds `Server.jar`. **Take the
  pinned version, not "latest":** 1.3.3 is on the same page and building against
  it would silently move the compile target.
- **Decompiled sources.** `./gradlew decompileToSources` (quiltflower 1.8.1,
  fetched from `maven.quiltmc.org`) writes
  `$NECESSE_GAME_DIR/decompiled/Necesse-sources.jar`; unzipped beside it that is
  **6,464 `.java` files**. This is far better than the `javap -c` route the v0.6
  notes had to fall back to, and it is what `AGENTS.md`'s "verify API behaviour
  before implementing" needs. Note the path is `$NECESSE_GAME_DIR/decompiled`,
  not `$NECESSE_GAME_DIR/../decompiled` as AGENTS.md says.
- **Pillow without pip.** Fetch the wheel URL from
  `https://pypi.org/pypi/Pillow/json`, pick the `cp<pyver>`+`manylinux`+`x86_64`
  build, and unzip it into a directory — manylinux wheels bundle their native
  libs, so no compiler and no root is needed:

  ```bash
  curl -sSL -o pillow.whl "<url from the JSON>"
  python3 -c "import zipfile; zipfile.ZipFile('pillow.whl').extractall('/home/blackoffset/dev/pylib')"
  export PYTHONPATH=/home/blackoffset/dev/pylib     # every Pillow tool needs this
  ```

  It lives at `/home/blackoffset/dev/pylib`. Pillow 12 deprecates
  `Image.getdata()` in favour of `get_flattened_data()`; the repo's tools use
  `.load()` and are unaffected.

**[run]** With those three in place every gate runs here: `buildModJar`,
`scripts/integration_test.sh` (boots, generates, restarts, cats persist),
`locale_audit`, `size_audit`, `tile_behaviour_audit`, `sheet_format_audit`,
`furniture_audit`, and the generator reproduces `src/main/resources`
byte-for-byte.

**[run]** The repo is checked out as three worktrees off one `.git`
(`Necesse-stairwaytoheaven` on `master`, `Necesse-claude` on `ai/claude`,
`Necesse-opencode` on `ai/opencode`). A worktree can sit far behind: `ai/claude`
was **87 commits** behind `master` on 2026-08-29, so the docs it held — and any
"owned by another agent" note in them — were three days stale. `git worktree
list` plus `git rev-list --left-right --count master...HEAD` is the first thing
to run, before believing any doc in the tree.

**[run]** `opencode` is the other worker CLI on this box and takes the same
shape of instruction:

```bash
opencode run --dir <worktree> --variant high "$(cat brief.md)"
```

`--dir` scopes it to one worktree (the equivalent of `codex exec -C`),
`--variant` is the reasoning-effort knob, `--format json` gives machine-readable
events, and `-m provider/model` picks the model. Its binary is at
`~/.opencode/bin/opencode`, which is NOT on the default PATH; credentials live in
`~/.local/share/opencode/auth.json`. Verified working against this worktree on
2026-08-29 (`opencode run --dir …` reported branch, HEAD and PNG count correctly).

**[run] Run workers one at a time, and never leave one unattended in a worktree
you are editing.** On 2026-08-29 two commits appeared on `ai/claude` inside a
three-minute window — `13afc95` at 21:51:53 and `98dab20` at 21:54:05 — that the
reviewer driving the session did not issue; the reviewer's own commit sits
between them at 21:52:40, and one of them rewrote a CHANGELOG section while it
was being written. Ruled out afterwards, none of which explained it: git hooks
(none installed), Claude hooks (none), cron and systemd timers (none relevant),
the interactive Codex TUI (idle since 20:44, read-only git only) and all four
`codex exec` rollouts (no commit call, no matching message text). **The origin
was never established.** The content happened to be correct and every gate was
green, which is luck, not process. `docs/AGENT_WORKFLOW.md` already says two
agents must never hold the same file; the practical form of that rule is: one
worker at a time, dispatched by the reviewer, and check `git log` before AND
after every dispatch so an unattributed commit is noticed immediately rather
than discovered during a rebase.

## Right cell, right size, same picture: the rotation-variety bug (v0.9)

**[game]** Reported from a session: *"die durch Claude hinzugefügten Skyreach
Türen und Tore etc lassen sich alle nicht ausrichten wie sonst im Game …
eigentlich je nach Richtung in die man schaut sind Sachen oft am unteren oder
oberen Ende des Blocks platziert am Rand statt einfach immer an selber
Position."* Asked to narrow it down, the player picked **"dreht sich gar
nicht"** — placing it looks the same whichever way they face.

**[sprites]** Measured across every mod sheet the engine addresses per
rotation, exactly one is a duplicate set: `objects/skywatchbanner.png`. Its
four 32x32 `PaintingObject` rows are **byte-identical to each other**, because
`gen_banner_painting` built one cell and pasted it four times:

```python
sheet = Canvas(32, 128)
cell = banner_cell()
for row in range(4):
    sheet.paste(cell, 0, row * 32)     # one banner, four walls
```

So the engine dutifully read row 0 on a south wall, row 2 on a north wall and
rows 1/3 on the side walls, and all four held the same face-on banner. Nothing
about it is a geometry fault: the sheet is 32x128, the rows are 32px, every
extent is where it belongs. `sheet_format_audit.py` was green on it, and always
would have been — **it guards which cell, what size, what extent, and is blind
to a cell holding the wrong picture, including the picture next door's.**

**[jar]** What the four rows have to be, and why the art must not bake the
offset in. For wall decor the rotation names WHERE THE WALL IS, not where the
piece faces (`PaintingObject.attachesToObject`; the same convention
`WardenSpirePreset.WALL_BELOW/LEFT/ABOVE/RIGHT` already places banners with),
and the engine supplies the vertical nudge itself:

| row | rotation | wall | engine offset | what the camera sees |
|---|---|---|---|---|
| 0 | 0 | below | `+8px` | the far side of that wall — rod and cloth foreshortened over its cap |
| 1 | 1 | left | none | edge-on slab against the tile's left edge |
| 2 | 2 | above | `-32px` | the face-on view, landing on the wall tile |
| 3 | 3 | right | none | mirror of row 1, against the right edge |

That `-32px` is why the face-on cell may use its whole 32px height, and the
`+8px` is why row 0's art has to stay inside cell rows 0..23 — drawing it low
*and* letting the engine push it down puts it a third of a tile into the wall.
The same trap as the door cells, one class over.

**[run]** `tools/rotation_variety_audit.py` is the gate for the class, not for
this one sheet: for every family where the repository has an actual engine read
— `PaintingObject` rows, `WallTorchObject`'s state x orientation grid,
`StreetlampObject`'s two 32x96 state rows, `LampObject`'s lit/unlit pair, the
eight wall-sheet door cells, the six fence-gate columns, the five fence columns
and the four rotation columns of everything `sheet_format_audit` already knows
— it asserts that cells the engine reads apart hold different pictures. Mirror
pairs are reported and allowed: vanilla's own left/right views are mirrors.
Verified against the pre-fix sheet, where it reports all six banner row pairs;
green on the fix. It reuses `sheet_format_audit`'s two sheet tables rather than
copying them, so a piece added there cannot silently miss this check.

**[run]** `tools/rotation_preview.py` is the picture, on the same principle as
`wall_render_preview.py`: it draws every cell **where the engine puts it**, over
a tile grid, with the object's own tile at the centre of a 3x3 stage and a grey
block on the wall the rotation names — so "is this the right view" and "does it
land on that wall" are both judgeable by eye. Where no anchor is recorded (the
wall lights, the streetlamps) the strip says so instead of implying one.

**Deliberately not covered by either tool:** the 1x2 multi-tile furniture
(bench, bed, dinner table). `docs/research/furniture-formats.md` records their
sheet size but not the engine read that splits it, and their generators paste
64px-wide blocks across two 32px columns — so "column 2 equals column 3" cannot
be judged without the decompiled draw call. `skywatchdinnertable` does have two
byte-identical 32px columns under a 4-column reading; that is a **hypothesis
about a frame we have not read**, not a finding. Read `DinnerTableObject`, then
add it.

**[unverified]** One record disagrees with two others and the disagreement is
recorded rather than silently resolved. `docs/research/structures-furniture.md`
§3.7 writes the side rows as "1=east, 3=west"; `WardenSpirePreset` and the wall
decor section above both say `1` = wall **left**, `3` = wall right, and the
preset cites `PaintingObject.attachesToObject`. The banner's side rows are drawn
to the preset's convention because it names the method it came from. If a
screenshot ever shows the edge-on banner hugging the wrong edge, that is this
line, and swapping the two `paste` calls in `gen_banner_painting` is the fix.

## The side-wall window is a slot in the roof, and three sets never got the fix (v0.9)

**[jar]** `WallWindowObject.getWindowDir` returns 1 for a NORTH-SOUTH wall —
the left and right walls of a room — and then draws only cols 4-5 rows 0-1 over
the band `drawY-16 .. drawY+16`. In a north-south run that band is unbroken
ROOF, so the picture is the wall from directly above with an opening cut into
it, and the player looking DOWN into the opening.

**[sprites]** Measured off vanilla `stonewall`, `brickwall` and `granitewall`:
the opening runs ALONG the wall — 10-12px wide, ~28px long, never a wide pane
across the cell — with a dark reveal on the NEAR inside faces, a lit lip on the
far one, the glass at the BOTTOM of the cut and BRIGHTER than the roof around
it, and no horizontal terminator at either end.

**[game]** `gen_beetlewall` learned that in August and **the fix was never
ported**. A player looking at the spire's side walls reported it as "die
Fenster sind seitlich falsch und nicht wie bei Käferwand gefixt" — and it was
three sheets, not one: cloudmarble drew a gold-framed 2x2-mullion pane,
skystonebrick and nightfell an 18x22 frame with glazing bars, all standing
upright out of the roof. The geometry gate was green on all three, because
`sheet_format_audit` asserts rows 0-1 are 512/512 opaque and rows 2-4 empty,
which a pane satisfies exactly as well as a slot does.

The construction now lives once, in `tools/asset_generator/wall_window_slot.py`,
and all four sets call it. **A frame seen from above is still a frame**; three
passes tried to fix this by making the pane flatter or darker, and only the
slot's shape and its reveals read as an opening.

## A supplied illustration is a source of record, not a sheet (v0.9)

**[sprites]** `objects/cloudmarblewall.png` shipped as the hand-made art from
`kk-sprites/`, copied in as-is, with `gen_cloudmarble_wall` deliberately not
called. Measured against the three walls that are drawn:

| sheet | distinct colours | cap mean luminance | cap dominant tone |
|---|---|---|---|
| cloudmarble (supplied) | **10,858** | **228** | none — commonest was pure white at 6% |
| skystonebrick | 19 | 52 | 91% |
| nightfell | 19 | 25 | 91% |
| beetlefreak | 38 | 31 | 78% |

Vanilla wall caps are ~93% one flat tone. The cap is the band the engine draws
for **every tile of a run except the last**, so a bright, tone-less cap is most
of a building — which is exactly what reached the player: "die ganzen Wände
blenden fast". The same illustration is also why the side-window cell held a
pane: an illustration draws a window, and the engine wanted a roof.

Beetlefreak had already been moved off its supplied sheet for the same reason.
Cloudmarble now is too, at 22 colours and a 79% dominant cap.

**[sprites]** The second half of "blenden" is a TRIM INVERSION and it is worth
stating as a rule. `SKYGOLD`'s base is luminance 178. The old stone ramp ran
205..249, so **the gold arcade, cornice and stars were up to 47 steps darker
than the marble they decorate** — a white-and-gold set in which the gold cannot
read as gold. Trim must be brighter than what it trims. The stone base is now
~152 and `SKYGOLD["hi"]` (~221) is the brightest pixel on the sheet. The value
law for a whole room is in `docs/ART_DIRECTION.md`.

## The dedicated server is a free download, and it carries the whole toolchain (v0.9)

**[run]** Three sessions in a row reported "no game install here, so nothing can
be compiled, tested or verified" and worked around it. That was wrong, and the
player said so: the **dedicated server** at <https://necessegame.com/server/> is
a free public download — no account, no purchase, no Steam. Every published
version is on that page, 1.3.2 included.

`scripts/fetch_dedicated_server.sh` fetches it (the page's links are S3
presigned URLs that expire in an hour, so they must be read fresh, never
hard-coded) and unpacks `Server.jar` plus a bundled `jre`. Measured on this
session's run, that single file turns on:

| gate | needs | status without the server |
|---|---|---|
| `./gradlew buildModJar` | `Server.jar` on the compile classpath | impossible |
| `scripts/integration_test.sh` | `Server.jar` + `jre` | impossible |
| `scripts/tile_sprite_check.sh` | same | impossible |
| `scripts/sky_map_render.sh` (the offline painter) | same | impossible |
| `./gradlew decompileToSources` | the jar to decompile | impossible |

The decompile writes `$NECESSE_GAME_DIR/decompiled/Necesse-sources.jar` in about
a minute — **6,464 readable classes**. Every "I cannot check that signature"
answer in this repo's history was one command away from being checkable.

**[run]** What the server does NOT bring is art. It never renders, so
`Server.jar` contains **zero `.png` entries** — verified by listing the archive.
So the vanilla sprite dump the art tooling wants stays missing:
`wall_render_preview --vanilla stonewall` still warns that its comparison strips
are absent, and `size_audit` still reports individual vanilla refs missing. Those
need a client install. Say "unavailable", not "skipped".

**[jar]** First thing the decompile settled, and it had been blocking a player
report for two sessions: `FruitBushObject`'s constructor is

```java
FruitBushObject(String textureName, String seedStringID,
                float minGrowTimeSeconds, float maxGrowTimeSeconds,
                String fruitStringID, float fruitPerStage, int maxStage,
                Color mapColor)
```

and vanilla's three berry bushes all register as
`("blueberrybush", "blueberrysapling", 900.0F, 1800.0F, "blueberry", 1.0F, 2, colour)`.
It carries a `FruitGrowerObjectEntity`, publishes a `HarvestFruitLevelJob` (so
settlers harvest it), and sets `objectHealth = 1`, `toolType = ALL`.

And `loadTextures` reads the sheet as **64x64 cells** —
`textures[width/64][height/64]`, variants across, growth stages down. That is
the second half of the same player report: a fruit bush is drawn on a 64px cell,
two tiles wide and two tall, where our cloudberry bush is a `GrassObject` on a
32px one. The archetype swap fixes *both* "one berry and it is gone" and "die
Buesche sind viel zu klein" — they were always the same bug.

## A berry bush that regrows and a berry bush that is big are the same fix (v0.9)

**[jar]** The whole loop, read out of the decompile:

- `FruitBushObject(textureName, seedStringID, minGrow, maxGrow, fruitStringID,
  fruitPerStage, maxStage, mapColor)`. Vanilla's three berry bushes are all
  `(..., "<x>sapling", 900.0F, 1800.0F, "<x>", 1.0F, 2, colour)` registered
  `0.0F, false, false, true`.
- It carries a `FruitGrowerObjectEntity` whose `stage` climbs while
  `stage < maxStage` and **resets to 0 on harvest** — the bush is never
  consumed. It publishes a `HarvestFruitLevelJob`, so settlers pick it.
- `getLootTable` returns **the seed alone** — breaking a bush drops the sapling
  and no fruit. That is the growth gate: replanting cannot be an infinite-berry
  exploit because the berries only come from the timer.
- `getFruitDropCount` sums `fruitPerStage` once per stage, the fractional part
  as a chance. So `1.0F` at maxStage 2 is 2 berries; ours is `1.5F` = 3.
- `loadTextures` slices `textures[width / 64][height / 64]` — **64px cells**,
  variants across, **stages down** — and `addDrawables` uses
  `spriteY = min(fruitStage, rows - 1)` with a per-tile random `spriteX`.
- Anchor: `.pos(drawX - 32 + 16, drawY - height + offset)`, `offset = 28 ± 4`.
  The cell is centred on the tile in x and its bottom row lands 28px below the
  tile's top edge, so the plant stands ~36px proud. The cell is also **mirrored
  at random per tile**, so nothing in the art may depend on handedness.

**[game]** Ours was a `GrassObject` on a 32px cell. The player reported it as
two things — *"man kriegt nur eine Beere beim Abbauen statt wie bei den Vanilla
Bueschen die Buesche abbauen kann und wieder aufbauen damit die Beeren
nachwachsen"* and *"die Buesche sind auch viel zu klein"* — and they were one:
the grass archetype is one-shot **and** one tile. Nothing about the loot table
could have fixed the first; nothing about the drawing could have fixed the
second.

**[jar]** The trap in the second half: `SaplingObject.validTiles` defaults to
`grasstile / overgrown* / swampgrass* / plainsgrass* / dirttile / farmland /
snowtile`. **The Skyreach has none of them.** Without passing `cloudturftile`
through the varargs the player could not replant a sky bush anywhere in the sky.
The mod's tree saplings already document this trap; a fourth family hit it.

**[run]** Verified end to end on a real server: `buildModJar` succeeds,
`integration_test.sh` exits 0, and the census reports `object cloudberrybush
x53 / expected x53` — the archetype swap did not disturb worldgen placement,
because `SkyTerrainPainter` places it by registry ID and the string ID is
unchanged.

## What a vanilla ground tile actually is, in three numbers

**[run]** Measured across the vanilla tile dump (240 sheets; the repo's own
`vanilla-sprites/` had only items, mobs and objects until 2026-08-30 — the
tiles were sitting unused in the sprite dump beside it and are now copied in).
Every figure below is per 32x32 full-tile variant cell, cells (3..6, 0).

| | density | mean \|dRGB\| | peak |
|---|---|---|---|
| natural ground, 37 sheets | 294-993 (median 603) | 5.0-16.7 | 5-33 |
| crafted floors, 9 sheets | 446-771 | 7.9-27.0 | 16-102 |
| liquid (lava) | 401 | 4.3 | 8 |

- **density** = pixels that are not the single most common colour.
- **mean |dRGB|** = how far those pixels deviate from it. `grass_splat` carries
  its ENTIRE texture inside **five RGB levels**.

**[run] Every vanilla splat is 100% coherent on a 2x2 pixel block grid.** grass,
dirt, ash, mud, snow, rock and all nine crafted floors: exactly 100.0%, no
exceptions. The tone unit is a 2x2 block, never a lone pixel. That is how vanilla
reaches 300-600 density without dithering, which the style guide forbids as
texture. This mod violated it on all fifteen sheets.

**The trap, paid for twice.** Density alone is a gameable target: it counts
pixels and says nothing about how far they deviate or what unit they are drawn
in. A first repair pass hit the band exactly — cloudturf 345 against vanilla
grass's 344 — and looked like camouflage netting, measuring mean 35 against
vanilla's 5. Both agents had optimised the brief honestly; the brief was wrong.
**Density, loudness and block unit have to be gated together**, which
`tools/tile_behaviour_audit.py` now does.

**[run] The palette is what caps loudness.** The terrain ramps' smallest step
from base was d25-d32 where vanilla's whole texture range is d5, so ANY density
built from them was automatically 2-7x too loud. Fixed by adding `grain_d` /
`grain_l` at ~7 RGB either side of base on every ground ramp — same hue, nearer
the base. That is a palette EXTENSION, not one of the biome palette shifts the
playtest marks KEEP, and it changed no other sprite.

**Quiet bed, loud features.** Vanilla `ash_splat` reaches 514 density on a
nearly uniform dark bed carrying LARGE black debris shapes. The bed obeys the
contrast ceiling; the motifs do not, and should not. An early draft of the audit
checked peak deviation and flagged six sheets for exactly the tufts, sparks and
flecks the agents had been told to preserve — the peak is set by a handful of
feature pixels and vanilla's own floors peak at 102. The mean is what measures
whether the BED is loud.

**Liquids animate, so texture has to travel coherently.** A liquid splat is
224*frames x 96*variants on an 8-frame ping-pong. A grain re-randomised per
frame boils rather than flows. Murkwater's bed is frame-independent and only one
ripple crest travels, one 2px block per frame: frame-to-frame delta 0.34-0.46
against a within-frame energy of 9-10.

**[jar] Animation time is global.** A bright accent that appears on certain
frames appears on EVERY tile of that liquid at the same instant — a field-wide
blink, not a twinkle. Murkwater's green glint was removed for that reason.
Position-varied features are the way to get sparkle; a time-keyed bed is not.

## `crystalwall` is a RockObject, not a wall (2026-08-31, read out of the 1.3.2 jar)

Supplied art arrived named `…-crystalwall-now-evilwall.png` at **128x208**,
which matches nothing this mod ships: every wall sheet here is **352x128**. The
art was right and the assumption was wrong.

`ObjectRegistry` builds the thing that owns `objects/crystalwall.png` as a
**`RockObject`** — a mineable rock — registered under the object ID
**`crystalrock`**, with map colour RGB(99,15,143), dropping `crystalstone`, and
`toolTier` 10. The texture name and the object ID are simply different strings.

The `RockObject` sheet contract, from `RockObject.java`:

- `loadTextures()` reads `objects/<rockTextureName>.png`;
  `generateItemTexture()` reads `items/<rockTextureName>.png`. **One name feeds
  both halves**, which is why a supplied pair carries the same name twice.
- `addRockDrawables` draws in **16px sprite cells**, not 32
  (`list.addSprite(spriteX, spriteY, 16)`).
- `int randomWidth = rockTexture.getWidth() / 32` and
  `r1 = random.nextInt(randomWidth) * 2` — so each random VARIANT is **two 16px
  sprite columns = 32px wide**, and the number of variants is `width / 32`.
- Sprite rows 0..12 are read, i.e. **13 rows of 16px = 208px**.

So a `RockObject` sheet is `32 * variants` wide by `208` tall. The supplied
128x208 is exactly **4 variants**, 1:1 vanilla. Rows 0-4 are the plain set,
5-9 the set used when a rock sits above/below, 10-12 the transitions.

`RockObject` also sets `regionType = RegionType.WALL` and `isRock = true`, so
one placed by worldgen is a solid, mineable wall.

**The lesson**: "wall" in a vanilla filename says nothing about the class. Look
up the registration before matching a sheet to a format — the mod already had
two `RockObject`s (`skystonerock`, `veilrock`) doing exactly this.

**A gate trap this exposed**: `tools/locale_audit.py` finds registrations by
matching `registerObject("...")` in the SOURCE TEXT, comments included. Pasting
vanilla's registration into a comment as documentation made the audit believe
the mod registers `crystalrock` and demand `items/crystalrock.png`. Describe
vanilla registrations in prose, never as a code-shaped comment.

## This container can have a full game install (2026-08-31)

Recorded because two sessions stood down for want of one:

- `scripts/fetch_dedicated_server.sh` works here and takes about a minute.
- `./gradlew decompileToSources -PuseDecompiledSources=true` then writes
  `$NECESSE_GAME_DIR/decompiled/Necesse-sources.jar` — **unpack it** (`unzip`
  into `decompiled/src`) to get 6,464 readable `.java` files. The task can
  report success while the jar is still being written; wait until its size
  stops changing and `unzip -t` passes.
- `pip install Pillow numpy` works, which unlocks every python art audit.
- Still absent: the **client sprite dump**. `scripts/sky_map_render.sh` renders
  its text dump fine and then dies in the image half on a missing vanilla tile
  PNG. For measuring worldgen, compile a small probe against `build/mod` +
  `Server.jar` and call the pure functions directly — but note that
  `SkyRegistry`'s ID fields are all **0** outside a running game, so a probe
  must compare biome CLASSES and call `SkyOutlands`/`SkyNoise` rather than
  compare registry IDs.

## Vanilla can re-skin a whole existing area, and it has an API for it (2026-08-31, VERIFIED [jar])

The player's observation: the incursion-10 Void Wizard fight happens partly in
your own base, with every texture darkened and trees turned to deadwood — and
the question was whether we can reuse that to fill our realms quickly from
existing buildings.

**Yes, and it is a named, reusable mechanism.**

### The level-copy pattern

`necesse/level/maps/incursion/SettlementRuinsIncursionLevel.generateLevel(Level
copyLevel, Rectangle tileRectangle, ...)` takes a **source level** and a
rectangle, reads it through a `RegionBoundsExecutor`, and writes a transformed
copy. Every single tile and object passes through one of two hooks:

```
changeTile  (random, newX, newY, sourceTile,   sourceLevel, srcX, srcY)
changeObject(random, layerID, newX, newY, sourceLevel, sourceObject, srcX, srcY)
```

Object copying preserves **rotation** and the **player-placed** flag, iterates
**every object layer**, and handles multi-tile objects by acting only on
`isMultiTileMaster()`. Returning `null` from a hook deletes that tile/object.

### What the "make it dark" table actually does

`changeTile`: `farmland` is kept; anything that is not a floor and not a liquid
becomes a random pick from a `TicketSystemList` of dark grounds (`cryptash`,
`basaltrock`); a `PathTiledTile` becomes `cryptpath` or `basaltpath`; a floor
has a 25% chance to become `basaltfloor` or `deadwoodfloor`; otherwise kept.

`changeObject`, in order: grass is removed; **15% of everything is removed
outright** (that is the decay); a blacklist is removed; **anything with
`getLightLevel() > 0` is removed** — which is literally how the place goes dark;
trees become `deadwoodtree`; fruit bushes and seeds are removed; a
`SingleRockSmall` becomes `basaltcaverocksmall` and a `SingleRockObject` becomes
`basaltcaverock`; **everything else falls through to the furniture-set swap.**

The important property: the table branches on **type** (`isFloor`, `isLiquid`,
`isTree`, `isGrass`, `instanceof PathTiledTile`, `instanceof SingleRockObject`),
almost never on a specific ID. That is why it works on ANY input layout,
including a base the player built by hand.

### `FurnitureSet` — the piece that makes this cheap for us

`necesse/level/maps/presets/set/FurnitureSet` is a prefix-keyed furniture family
with a fixed slot list: `chest`, `dinnerTable`, `dinnerTable2`, `desk`,
`modularTable`, `chair`, `bench`, `bench2`, `bookshelf`, `cabinet`, `bed`,
`bed2`, `doublebed1..3`, and more. Vanilla ships twelve of them: **oak, spruce,
maple, birch, pine, palm, dungeon, deadwood, willow, bone, bamboo, dryad**.

The swap is one call:

```
targetSet.replaceSingleObjectRandomly(random, oldSet, objectID)
```

It returns a different ID when `objectID` belongs to `oldSet`, and the same ID
when it does not — so a caller loops its `oldFurnitureSets` and takes the first
hit.

**What this buys this mod.** Any vanilla building, preset or player settlement
can be restyled into another family for the cost of naming two sets. Our own
`SkyFurnitureSet` already ships the matching slots (bookshelf, cabinet, clock,
display stand, tables, chairs, beds) — registering it as a real `FurnitureSet`
with a `prefixStringID` would make every vanilla preset re-skinnable into
Skyreach style automatically, and the same trick gives Ghost Realm the `bone`
and `deadwood` sets for free.

**Caveats worth knowing before relying on it:**
- The furniture swap only covers pieces that are IN a set. Bespoke objects fall
  through unchanged, so a re-skin is never 100% and still needs a blacklist.
- `SettlementRuinsIncursionLevel` is an `IncursionLevel`; the copy machinery is
  in its own `generateLevel`, not in a shared utility, so we would port the
  pattern rather than call it.
- Deleting every light source is what sells the mood. Any re-skin that keeps the
  lamps will not read as corrupted.

## A mob that does not override `getCollisionDamage` deals nothing (2026-08-31, VERIFIED [jar])

Found during the endgame rebalance, and it had shipped: the **Mistserpent dealt
zero damage in play**.

`MistserpentHead`/`MistserpentBody` defined `HEAD_COLLISION` and
`BODY_COLLISION` constants — and nothing ever read them. `Mob.getCollisionDamage`
returns **`null`** by default (`Mob.java:2124-2125`), meaning "no collision hit",
and neither class overrode it. The AI it uses,
`PlayerChargingCirclingChaserAI`, carries no `GameDamage` of its own. A worm's
entire offence IS its collision — vanilla's own does it explicitly
(`SandwormHead:121`, `SandwormBody:71` both override the method).

**The trap generalises to every mob in this mod.** Declaring a damage constant
is not wiring it. If a mob has no attack animation and no projectile, its damage
must come from a `getCollisionDamage` override or it has none, and nothing in
the build, the audits or the integration test will say so — the mob spawns,
moves, and is simply harmless.

Worth knowing alongside it, from the same investigation:

- **A segmented worm has ONE health pool, on the head.** `WormMobHead.init()`
  copies the head's pool onto every segment as it builds the chain
  (`WormMobHead.java:219-220`); `WormMobBody.tickMaster()` re-mirrors max health,
  current health **and armour** off the master every tick (`:150-163`);
  `getHealth()/getMaxHealth()` delegate to the head (`:236-258`); and
  `setHealthHidden()` pushes coil damage back onto the master (`:288-295`).
  Health or armour set on a body class is a dead number.
- **The chain lands one collision hit per window, not one per segment.**
  `WormMobHead.modifyBodyPart` shares the head's own `collisionHitCooldowns`
  with every segment (`:188-189`).
- **`MaxHealthGetter` works on a worm head.** `WormMobHead.init()` calls
  `super.init()` first (`:195`), so `Mob.init()`'s `difficultyChanges.init()`
  has already applied difficulty health before the chain copies it outward.

## Armour is a flat halving, which sets the whole damage economy (2026-08-31, VERIFIED [jar])

`DamageType.getDamageReduction(float armor, boolean isItemsVsItems)` returns
`armor * (isItemsVsItems ? 0.125F : 0.5F)` — so for a player hitting a mob,
**every point of armour removes half a point from every hit**.

Consequences the rebalance had to respect:

- A 55-armour mob eats 27.5 off every swing; a 70-armour one eats 35.
- Therefore **weapon damage and mob armour cannot be tuned separately.** At the
  mod's old weapon damage (Skyreave 54) a 2800 HP / 55 armour enemy needed about
  a hundred hits.
- Vanilla's own weapon ceiling, measured across
  `necesse/inventory/item/toolItem/`: fully-upgraded damage clusters at
  **175-262**, with outliers at 310 and 510. Damage is set as
  `attackDamage.setBaseValue(B).setUpgradedValue(1.0F, U)`, not as a
  `GameDamage` constant.
- **There are no arcanic weapons.** Arcanic exists as armour, tiles, presets and
  machinery only — a plausible-sounding anchor that does not exist, and one this
  session briefed two workers against before checking.
- **A two-handed weapon carries TWO damage stats**, and a grep that is not
  anchored on the field name will read the wrong one:

  ```
  BarkBladeSwordToolItem:
    attackDamage     .setBaseValue( 60.0F).setUpgradedValue(1.0F,  93.33336F)
    greatswordDamage .setBaseValue(160.0F).setUpgradedValue(1.0F, 186.66672F)
    resilienceGain   .setBaseValue(  3.0F).setUpgradedValue(1.0F,   6.0F)
  ```

  All four stats use the same `setBaseValue(...).setUpgradedValue(...)` shape,
  so `grep "setUpgradedValue"` alone returns crit chance and resilience gain
  mixed in with damage. **Always anchor the grep on the field**
  (`attackDamage.setBaseValue`), and compare like with like: a two-handed mod
  weapon's `greatswordDamage` belongs against a vanilla `greatswordDamage`, not
  against an `attackDamage`. This session sent two workers a set of example
  weapons read from the unanchored pattern; the band was right and the examples
  were the wrong field.
- The 310 and 510 outliers are genuine `attackDamage`:
  `ChargeBeamProjectileToolItem` 300 → 310 and `ChargeShowerProjectileToolItem`
  500 → 510, both charge weapons.
- **A bow's arrow adds to the bow**, it does not replace it: `ArrowItem.modDamage`
  (`ArrowItem.java:49-51`) calls `damage.add(this.damage, ...)`, and vanilla
  arrows run 5 (stone) to 17 (spiderite). A bow should therefore sit slightly
  under a blade of the same tier.

## A negative broker value plus a yield above 1 is a money printer (2026-08-31, VERIFIED [jar])

Found during the value pass, and it had shipped on **all four** of the mod's
wall sets.

A negative `setBrokerValue` means "compute this from the recipe". The computation
is `RecipeBrokerValueCompute`:

```
handler.handle(element.itemID, element.bestRecipeBrokerValue * element.valueMultiplier)
```

and the multiplier is `Math.abs(element.setBrokerValue)`
(`ItemRegistry.java:3428-3429`). It sums the ingredients and **never divides by
the recipe's yield.**

Every mod wall is crafted four at a time and registered at `-1.0F`, so each
single wall carried the price of the whole craft: buy 29F of stock, craft four,
sell 116F.

**Vanilla never hits this**, which is why the pattern looked safe: every vanilla
wall recipe yields one — `stonewall`, `woodwall` and `cryptwall` are all
`new Recipe(..., 1`. The mod's yield of 4 is what turns the same registration
into an exploit.

**The fix is `-1.0F / yield`** — `-0.25F` for a yield of four — because
`Math.abs` turns the fraction into exactly the per-unit divisor. Applied to
`skystonebrickwall`, `nightfellwall`, `beetlewall` and `cloudmarblewall`.

**The rule to carry forward:** any item registered with a negative broker value
AND a recipe yield above one needs `-1/yield`, not `-1.0F`. Check the yield
whenever a computed price is set. Nothing in the build, the audits or the
integration test detects this — the item registers, crafts and sells correctly,
just at four times its worth.

Related, from the same pass: a resource node should never be worth less than
what falls out of it. `stormcrystal` was hand-priced at 30.0F while dropping
2-3 `stormshard` now worth 25.0F each. Vanilla hand-prices no cluster at all —
`registerCrystalCluster("amethystcluster", ..., -1.0F, true, ...)` — so ours is
now on the same computed convention.

## The Tungsten crafting gate is not the hole it looks like (2026-08-31, VERIFIED [jar])

Three of the eight rebalance workers independently flagged the same thing as
"the single highest-impact leftover": after the endgame re-tier, the mod's
EPIC gear is still crafted at `TUNGSTEN_WORKSTATION` / `TUNGSTEN_ANVIL`, so
"a player who has just smelted tungsten can craft a 150-damage EPIC weapon".

**That reading is wrong, and the proposed fix would have broken crafting.**

### The real gate is the material, not the station

Every one of the five arsenal weapons needs **`aetheriumbar`**. The only recipe
that produces it runs on the mod's own `AETHER_FORGE` tech
(`SkyProfessions.java:174`), and the Aether Forge itself costs
`skystone 20, ironbar 8, stormshard 4`. `skystone` and `stormshard` exist only
in the Skyreach.

So the actual prerequisite chain is: reach the Skyreach → gather skystone and
stormshard → build the Aether Forge → smelt aetheriumbar → only then craft.
Tungsten is incidental to that, not the gate. The same holds for the Stormsteel
set, whose bars come off the same forge.

### And AETHER_FORGE cannot host a weapon recipe

The obvious-looking fix — move the recipes onto the mod's own forge tech — does
not work. `AetherForgeObjectEntity extends AnyLogFueledProcessingTechInventoryObjectEntity`:
it is a **processing** station (load ore and fuel, collect bars later), not a
crafting-menu station. A weapon recipe registered against that tech would have
no UI to be crafted from. `StormglassKilnObjectEntity` is the same shape.

**If a stricter station gate is genuinely wanted**, it needs a NEW crafting
station object at endgame tier — a content family (object, entity, recipe,
sheet, icon, locale, ledger row), not a one-line tech swap. That is a design
decision, not a bug fix.

### The lesson

"Which station" and "which materials" are two different gates, and in this mod
the material chain is the load-bearing one. Check what a recipe's ingredients
actually require before concluding that its station lets anyone in early —
three careful workers reached the same wrong conclusion from the tech constant
alone.

## `MobSpawnTable.addLimited`'s searchRange is in PIXELS, not tiles (2026-08-31, VERIFIED [jar])

The single most consequential finding of the endgame rebalance, and it had
silently disabled almost every spawn cap this mod ever wrote.

`addLimited(weight, mobID, max, searchRange)` measures `searchRange` in
**pixels**: it builds `GameUtils.rangeBounds(spawnTile.x * 32 + 16, ...,
searchRange)` and filters on `Mob.getDistance`. The sibling that takes tiles is
`rangeTileBounds`, which multiplies by 32 — and the neighbouring
`MobSpawnLocation.checkMaxMobsAround(max, tileRange)` *is* in tiles, which is
exactly how the confusion survived review.

Every cap in this mod passed **60, 80 or 96** — that is **1.9, 2.5 and 3
tiles**. None of them could bind in practice, and several code comments called
80 an "eight-tile radius". The tables now use named constants
(`SkyBiome.RANGE_STANDARD` = 8*32, `RANGE_RANGED` = 12*32, `RANGE_ELITE` =
16*32) so the unit is visible at every call site.

Three more facts that bound what a spawn table can actually do:

- **A per-kind cap is a MIX control, not a pressure control.** The engine
  already allows only four hostiles within eight tiles
  (`EntityManager.tickMobSpawning` → `checkMaxHostilesAround(4, 8, client)`)
  plus a per-player headcount. Your table cannot raise total pressure; it only
  decides *which* four.
- **A cap of 4 at 8 tiles is therefore dead weight**, and
  `checkMaxHostilesAround` measures the enclosing SQUARE
  (`GameMath.squareDistance` = `max(|dx|,|dy|)`), not a circle.
- **A bound cap redistributes tickets rather than emptying ground** —
  `addCanSpawns` plus `spawnRandomMob`'s `withoutRandomMob` retry means the
  slot goes to another entry instead of going unused.
- And, as recorded above: an entry for a mob without `isValidSpawnLocation` is
  silently inert.

### The loot gap this exposed

Crate loot tables exist **only** in the four Skyreach biomes.
`VeilBiome` has no `getCrateLootTable` override and `VeilTerrainPainter` places
no crate at all — `skycrate` is scattered by `SkyTerrainPainter` alone. So the
harder realms have no container to pay out of, and if a crate were ever placed
down there the inherited fallback is surface-tier `basicCrate`. That is a
worldgen gap, not a loot-table one, and it is still open.
