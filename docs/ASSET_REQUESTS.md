# Asset requests — the player's shopping list

This is the concrete list: for every realm, every sprite that is currently a
**borrowed vanilla stand-in**, the exact pixel size the replacement has to
land at, and enough context to draw to the realm's brief instead of to a bare
filename. It was built by reading the registration code in
`src/main/java/stairwaytoheaven/realms/{eden,ghost,crooked}/`,
`level/SkyLevel.java` + `biomes/` + `veil/`,
following every texture argument to the file it actually resolves to, and
measuring that file with Pillow — nothing here is a guess or a copy of a
number from another doc. Several of the other asset docs (`docs/realms/*.md`,
`docs/VANILLA_ASSET_MAP.md`) were written before recent art landed and say a
few things that are no longer true; where this list disagrees with them, this
list is the one checked against the source on 2026-09-02 and is correct. The
worst offenders are called out realm by realm below.

Three-way split used throughout:

- **OURS** — a file already under `src/main/resources/`, generated or
  hand-drawn, not vanilla's. Not in this list; nothing to do.
- **PLAYER** — one of the sheets you already supplied (see
  `src/main/resources/kk-sprites/readme.md` and `docs/VANILLA_ASSET_MAP.md`
  §4). Also not in this list.
- **BORROWED** — resolves to a file under `vanilla-sprites/` and nothing
  under `src/main/resources/` shadows it. This is what the tables below are.

Steinfeld isn't here: it has no merged code yet (`docs/OVERVIEW.md` §1 —
"isolated WIP, not merged"), so there is nothing to audit, and
`docs/realms/steinfeld.md` belongs to someone else's pass right now anyway.

## How to use this

**Naming.** Drop a finished PNG in `art-inbox/` named one of two ways:

- **`<name>.png`** replaces the sprite the mod already ships under that exact
  basename (`items/skystone.png` is replaced by an inbox file called
  `skystone.png`).
- **`<vanillaname>-new-<ourname>.png`** is new art drawn on a vanilla sheet's
  layout, where `<ourname>` is a name the mod already ships (a generated
  placeholder waiting to be replaced). `kk-sprites/readme.md` has worked
  examples.

**The catch, specific to this list.** `tools/inbox_fix.py` resolves a target
by matching your filename against sprites the mod **already ships** — it
never looks inside `vanilla-sprites/`. Most rows below have no such
placeholder yet (the "current stand-in" column is a `vanilla-sprites/...`
path with nothing under `src/main/resources/` standing in for it), so for a
first-time replacement of one of these, the auto-match has nothing to find.
Name the file after the vanilla stand-in's own basename (the last path
segment in the "current stand-in" column) and **hit the exact size in this
table yourself** before it goes in `art-inbox/` — the fixer will still cut the
palette and harden the alpha on whatever lands at that path once it's placed,
but it can't discover the target on its own for these. Check the run's log
either way: an unresolved file is reported and left alone (never silently
guessed at), and the workflow clears `art-inbox/` on every run regardless, so
"the PR merged" isn't proof a given file actually landed.

**The pipeline, for everything that does resolve.** Push a PNG into
`art-inbox/` (or run **Actions → Fix sprites → Run workflow**) and the
**Fix sprites** GitHub Action runs `tools/inbox_fix.py --apply`: it fits the
art to the resolved target's exact size, cuts the palette to vanilla's own
range (32 colours by default, shipped sheets run 19–38), hardens the alpha,
and — for anything shaped like a ground splat — snaps it to 2×2 blocks. Mob
sheets are deliberately **not** snapped, so a silhouette stays sharp. It never
writes straight to a branch you use; it opens a pull request with before/after
previews attached as a run artifact, for a look at the 1× view before merging.

**The four format families**, sizes measured off real shipped files rather
than assumed round numbers:

| family | size | notes |
|---|---|---|
| **Walking mob sheet** | 384×320, 384×256, or 192×128 | four direction rows over N animation columns (e.g. `mobs/crookedgolem.png` 384×320, `mobs/gloomshade.png` 384×256). A few mobs below deliberately don't fit this: a 448×320 body-plus-two-arms trio for a human-rigged caster, single-row 192×32 sheets for a stationary turret and a floating shroud, a 64×128 rotating "spinner" pair for drifting hazards, a 42×42 pet mote. Draw to the size this table gives you, not to the family default. |
| **Ground splat** | 224px wide, height a multiple of 96 | 2×2-snapped automatically (`tiles/cloudturf_splat.png` is 224×576 = 96×6). A file whose registered convention still ends in `_splat` but is **not** 224 wide is a *liquid* sheet instead (several animation frames side by side, e.g. 1792×224) — the fixer's own `is_splat()` check requires width 224, so these fall through to the generic "everything else" path and are fitted/quantised but not block-snapped. Several rows below are exactly this case; each says so. |
| **Object sheet** | no fixed size — sized per object and variant count | fitted and quantised only. Real examples already shipped: `objects/nimbuswillow.png` 128×512, `objects/windsilkloom.png` 128×64. |
| **Item icon** | 32×32, almost always | a borrowed vanilla icon occasionally ships a pixel or two off-square already (30×32 shows up twice below) — match the exact figure in the table, don't round it to 32×32. |

Tables below are ordered **by realm** (Skyreach → Eden → the Veil → Ghost
Realm → Crooked Beyond, the mod's own progression order), and **within a
realm, by how much difference replacing it would make** — the things a player
sees constantly and that define the realm's identity are listed first; a
generic vanilla wall or chest reused wholesale in one POI preset is listed
last. Where several registered IDs share one literal picture, they're listed
in one row — that sharing is itself worth knowing about (two named things that
currently look identical).

---

## Skyreach (Tier 0)

**Palette and mood** (`docs/WORLD_DESIGN.md` §4, §A3.2): white, cream, light
blue, pink, warm gold, single pastel tones — "no desaturated fantasy look,"
and explicitly **not sterile**: *"Viele Vanilla-Himmelwelten bestehen nur aus
weißem Marmor und Gold. Deine sollte fast gemütlich sein"* — a small celestial
town around the tower, cloud paths, golden gates, fountains, little gardens,
with small rainbow / mother-of-pearl accents beyond the base palette.

Almost all of Skyreach is already the player's own art or generated art — see
`docs/OVERVIEW.md` §3, and `docs/VANILLA_ASSET_MAP.md` §4 for the mob sheets
swapped out on 2026-09-01/02. Two arsenal-tier mobs are the last vanilla
holdouts found by following `level/SkyLevel.java` → `biomes/` → the mobs those
biomes spawn (`arsenal/SkyArsenal.java`):

| what | id | current stand-in | size | format notes |
|---|---|---|---|---|
| Rime Sentry — immobile ice-crystal turret, subclasses vanilla's `FrostSentryMob` untouched (`arsenal/RimeSentryMob.java`) | `rimesentry` | `mobs/frostsentry.png` | **192×32** | Walking-mob family by convention, but not the four-direction shape: one row, six 32px frames — matches a stationary turret's own vanilla layout. Not auto-snapped either way. |
| Watch Mote — the Skywatch Whistle's summoned companion, subclasses `CryoFlakeFollowingMob` untouched (`arsenal/WatchMoteFollowingMob.java`) | `watchmote` | `mobs/playercryoflake.png` *(inferred from the field name `MobRegistry.Textures.cryoFlakePet` in the class javadoc — the vanilla source itself isn't in this repo to confirm the exact path, so double-check in-engine before drawing)* | **42×42** | Small pet sprite, not the 64×128 "spinner" pair the wild Cryo Flake / Aurora Flake use. |

---

## Garden of Eden (Tier 1)

**Palette and mood** (`docs/WORLD_DESIGN.md` §5, §A3.3): not "green heaven" —
an exaggerated biological explosion, deep green ground, intense blue sky,
turquoise water, white sand, oversized colourful blooms and giant fruit,
*"fast schon unangenehm perfekt und fruchtbar"* (uncomfortably perfect and
fertile). Trees should read **much larger than vanilla Necesse trees**. The
design thesis in one line: **beauty can be dangerous** — snakes in the tall
grass, carnivorous flowers, jealous vines, aggressive paradise insects.

Every tile, plant, tree and mob body in Eden is presently a direct alias of a
vanilla asset — `EdenRealm.registerObjects()` calls `ObjectRegistry
.getObjectID(vanillaID)` and places that exact vanilla object, so these
aren't just borrowed pictures, they're the literal vanilla objects (same
drops, same tool response) standing in until Eden gets its own. 39 unique
files, 44 registrations (some IDs deliberately share a picture — flagged
below). None of it is in `docs/realms/eden.md`'s table by name, but that
doc's summary ("Terrain uses the literal vanilla sheets... Vegetation is made
from vanilla registry objects") is directionally accurate; this table is the
itemised version, verified against the resource tree.

| what | id | current stand-in | size | format notes |
|---|---|---|---|---|
| Eden Serpent — the realm's standard threat, "poison attack" (§5) | `edenserpent` | `mobs/crocodile.png` | **768×640** | Walking mob sheet, non-standard width: 6×128px animation columns over 4 direction rows + a particle row (`spriteSize()` = 128 in `EdenSerpentMob`). |
| Bloom Maw — carnivorous flower | `bloommaw` | `mobs/stabbybush.png` | **382×320** | Walking-mob-family height, odd width — read exactly, don't round to 384. Cell size 64px. |
| Jealous Vine — attacks out of vegetation | `jealousvine` | `mobs/dryadsentinel.png` | **768×896** | Large multi-row sheet, cell 128px. **Same file as Crooked Beyond's Tongue Plant** below — two mod mobs in two different realms currently wear identical art. |
| Golden Hornet — fast air enemy | `goldenhornet` | `mobs/bee.png` | **64×128** | Small critter-scale sheet, cell 32px (2 cols × 4 direction rows). |
| Forbidden Serpent — elite, guards the Knowledge Tree | `forbiddenserpent` | `mobs/dragonwhelp.png` | **448×320** | Walking mob sheet, cell 64px. |
| Paradise Palm (+ Paradise Coconut) | `paradisepalm` (alias of vanilla `palmtree`) | `objects/palmtree.png` | **256×514** | Object/tree sheet. §A3.3 explicitly wants Eden's trees **larger than vanilla's** — this is vanilla's own normal-sized palm, the opposite of the brief. Height is 514, not 512 — read from the file. |
| Tree of Plenty | `treeofplenty` (alias `appletree`) | `objects/appletree.png` | **256×640** | Object/tree sheet. Same oversized-canopy note as Paradise Palm. |
| Giant Fig Tree | `giantfigtree` (alias `bananatree`) | `objects/bananatree.png` | **256×640** | Object/tree sheet. Same note. |
| Knowledge Tree — the realm's one-of-a-kind worldgen landmark | `knowledgetree` (alias `dryadtree`) | `objects/dryadtree.png` | **384×512** | Object/tree sheet — the single most distinctive silhouette in the realm and still 100% vanilla. |
| Paradise Fern **and** Giant Monstera **and** Adam's Vine — three different named plants | `paradisefern`, `giantmonstera`, `adamsvine` (all alias `swampgrass`) | `objects/swampgrass.png` | **256×32** | Object sheet. All three currently render as the exact same picture. |
| Serpent Grass **and** Flowering Vine | `serpentgrass`, `floweringvine` (alias `grass`) | `objects/grass.png` | **256×32** | Object sheet. Two named plants, one picture. |
| Blue Paradise Flower **and** Giant Lotus | `blueparadiseflower`, `giantlotus` (alias `blueflowerpatch`) | `objects/blueflowerpatch.png` | **128×32** | Object sheet. Two named plants, one picture. |
| Red Paradise Flower | `redparadiseflower` (alias `redflowerpatch`) | `objects/redflowerpatch.png` | **128×32** | Object sheet. |
| Golden Orchid | `goldenorchid` (alias `yellowflowerpatch`) | `objects/yellowflowerpatch.png` | **128×32** | Object sheet. |
| Sun Grape Bush **and** Moon Melon Bush — two different farmable crops | `sungrapebush`, `moonmelonbush` (alias `blueberrybush`) | `objects/blueberrybush.png` | **128×192** | Object sheet. Two named crops, one picture. |
| Eden Berry Bush | `edenberrybush` (alias `blackberrybush`) | `objects/blackberrybush.png` | **128×192** | Object sheet. |
| Eden Rock | `edenrock` (alias `rock`) | `objects/rock.png` | **128×208** | `RockObject` layout: 4 width/32 variants × 13 rows of 16px. |
| Eden Copper Rock (registered ID `ivyoreswamp`) | `edencopperrock` | `objects/ivyore.png` | **32×32** | Ore overlay drawn over the rock via `RockOreObject`. |
| Paradise Reeds | `paradisereeds` (alias `reeds`) | `objects/reeds.png` | **256×96** | Object sheet. |
| Eden Shells | `edenshells` (alias `seashell`) | `objects/seashell.png` | **128×32** | Object sheet. |
| White Paradise Sand | `paradisesandtile` | `tiles/sand_splat.png` | **224×288** | Ground splat, 2×2-snap eligible. |
| Eden Moss | `edenmosstile` | `tiles/overgrowngrass_splat.png` | **224×576** | Ground splat. Note: this is the *vanilla* overgrown-grass sheet, distinct from `tiles/overgrowneden_splat.png` — that one is already the player's own art, used by a different tile (`stairwaytoheaven.tiles.OvergrownEdenTile`), not by Eden Moss. |
| Rich Eden Soil | `edensoiltile` | `tiles/mud_splat.png` | **224×192** | Ground splat. |
| Root Floor | `edenrootfloortile` | `tiles/ancientroots_splat.png` | **224×192** | Ground splat — the Canopy biome's own ground, currently identical to any vanilla ancient-roots floor. |
| Turquoise Shallow Water (shallow layer) | `edenshallowstile` | `tiles/saltwater_shallow_splat.png` | **1792×224** | **Not** the 224-wide splat shape — this is a liquid animation strip (several frames side by side); `inbox_fix.py`'s splat auto-detect won't fire on it, so it's fitted/quantised as a generic sheet. The design wants this *turquoise*; the colour is already applied at draw time (`EdenShallowsTile.getLiquidColor`), so only the sheet's own texture (currently vanilla salt water) needs redrawing. |
| Turquoise Shallow Water (deep layer) | `edenshallowstile` | `tiles/saltwater_deep_splat.png` | **1792×224** | Same liquid-strip note as above. |
| Eden Wood (icon) | `edenwood` | `items/palmlog.png` | **32×32** | Item icon. |
| Eden Sap (icon) | `edensap` | `items/dryadbranch.png` | **32×32** | Item icon. |
| Paradise Apple (icon) | `paradiseapple` | `items/apple.png` | **32×32** | Item icon. |
| Serpent Scale (icon) | `serpentscale` | `items/sharkscales.png` | **32×32** | Item icon. |
| Venom Fang (icon) | `venomfang` | `items/fangoftheprotector.png` | **32×32** | Item icon. |
| Golden Pollen (icon) | `goldenpollen` | `items/honey.png` | **32×32** | Item icon. |
| Knowledge Cutting (icon) | `knowledgecutting` | `items/dryadsapling.png` | **32×32** | Item icon. |
| Paradise Coconut (icon) | `paradisecoconut` | `items/coconut.png` | **32×32** | Item icon. |
| Eden Berry (icon) | `edenberry` | `items/blueberry.png` | **32×32** | Item icon. |
| Moon Melon (icon) | `moonmelon` | `items/frozenberry.png` | **32×32** | Item icon. |
| Sun Grape (icon) | `sungrape` | `items/raspberry.png` | **32×32** | Item icon. |
| Eden Copper Ore (icon) | `edencopperore` | `items/ivyore.png` | **32×32** | Item icon. |
| Eden Bronze Bar (icon) | `edenbronzebar` | `items/ivybar.png` | **32×32** | Item icon. |

---

## The Veil (now a field inside the Ghost band, not a world)

**Palette and mood** (`docs/WORLD_DESIGN.md` §8–§9): the mod's afterlife
band, the Gloomfen and Ashen Reach fields inside the Ghost band — no
daylight palette at all, only what glows. Before the Veil Mark is earned, a
stacking **Soul Exposure** debuff punishes lingering in the fog (mild at 0–3s,
slowed at 4–7s, draining at 8–12s, lethal past 12s), so a short step in reads
as tense rather than merely dim. Cold blue-white mist, candlelight, and
ectoplasm glow are the only light sources this realm has.

Almost everything here is already the player's own or generated art (Gloom
Shade, Fen Wraith — see `docs/OVERVIEW.md`). Following `veil/`
and the Veil's own biome files turned up one mob that's still a
straight vanilla reskin, plus the two literal-path borrows already on record
in `docs/VANILLA_ASSET_MAP.md` §1.3b:

| what | id | current stand-in | size | format notes |
|---|---|---|---|---|
| Cinder Cantor (body) — the Ashen Reach's ranged caster, subclasses vanilla's `AncientSkeletonMageMob` untouched (`arsenal/CinderCantorMob.java`). Spawns in the Veil's Gloomfen/Ashen Reach/Beetlefreak Hollow **and** in Skyreach's distance-gated Outlands — `docs/VANILLA_ASSET_MAP.md` §1.2 mislabels it "Ghost Realm," which is the one factual correction this list makes to that table | `cindercantor` | `mobs/ancientskeletonmage.png` | **448×320** | Composited `HumanDrawOptions` rig, not a plain walking sheet — body sheet plus two arm sheets, all three on the same 448×320 canvas. Redraw as a matched trio (below), or the parts won't line up. |
| Cinder Cantor (left arm) | `cindercantor` | `mobs/ancientskeletonmagearms_left.png` | **448×320** | Same rig, left-arm layer. |
| Cinder Cantor (right arm) | `cindercantor` | `mobs/ancientskeletonmagearms_right.png` | **448×320** | Same rig, right-arm layer. |
| Soul Exposure debuff icon — the HUD icon for the pre-Veil-Mark fog debuff (`veil/SoulExposureBuff.BORROWED_ICON`) | — | `buffs/spirithaunted.png` | **32×32** | Standard buff icon. |
| Veil fog particles — what the permanent mist itself is drawn out of (`veil/VeilFogBuff.BORROWED_PARTICLES`, and the same sheet `HuginStatueObjectEntity` uses) | — | `particles/fog.png` | **128×16** | 4 animation frames, 32×16 each, laid out in one row. |

---

## Ghost Realm / Aftergarden (Tier 3)

**Palette and mood** (`docs/WORLD_DESIGN.md` §10, §A3.5): Tim-Burton-like,
spooky but explicitly **not grey** — petrol, turquoise, violet, poison green,
black, cold white; *"intense violet shadows, glowing ectoplasm, blue
moonlight — statt einfach Grau."* The world here is still physically
coherent, just dead and crooked; ghosts get a society, not a monster
manual — some friendly, some mad, some hostile, and one of them says "Rude."
on death.

**This is the biggest correction to the existing docs.** `docs/realms/ghost.md`
and `docs/VANILLA_ASSET_MAP.md` §2.3/§4 both list several sheets as vanilla
borrows that are, on the ground truth of `src/main/resources/`, already
custom mod art: `objects/deadtree.png` (Crooked Dead Tree), `objects/gloomwillow.png`
(Lantern Tree), `objects/aurorabloom.png` (Ghost Lily), `objects/cragbloom.png`
(Mourning Rose), `objects/gloomshroom.png` (Ectoplasm Fern **and** Spirit
Mushroom), `objects/withershrub.png` (Widow Vine), `objects/veilrock.png` +
`items/veilrock.png` (Ghost Rock, world **and** icon), `objects/windsilkloom.png`
+ `objects/aetherforge.png` (Soul Loom / Spirit Forge world sheets),
`objects/veilriftdown.png` + `objects/veilriftup.png` (the Ghost Gate pair),
and — the one that will surprise anyone reading `docs/realms/ghost.md`'s
table literally — `tiles/murkmoss_splat.png` (Haunted Grass), which is
already an existing mod sheet, not a vanilla one. None of the eleven above
are in the table below; they need nothing.

Two IDs placed directly in `HauntedManorPreset.java` — `deadwoodwall` and
`deadwooddoor` — don't resolve to any file in the vanilla sprite dump at all
(checked against all 6,121 files under `vanilla-sprites/`, including every
`*wall*.png`). That's flagged in its own row rather than guessed at; it looks
like a stale/invalid vanilla object reference; see the note at the end of
this document.

52 borrowed files with a known size remain, across the realm's mobs, grounds,
unique world objects, materials, armour and the three POI presets' generic
dressing — plus two more IDs, the Haunted Manor's wall and door, that don't
resolve to any file at all (last two rows of the table, explained after it).

| what | id | current stand-in | size | format notes |
|---|---|---|---|---|
| Drifter — the realm's ordinary dead, the mob a player sees most (§10) | `drifter` | `mobs/deepcavespirit.png` | **192×256** | Walking mob sheet. |
| Possessed Chair — "deco wakes" (§10) | `possessedchair` | `mobs/mimic.png` | **384×320** | Walking mob sheet. **Same file as Crooked Beyond's Door Mimic** below — a shared sheet drives two named enemies in two different realms. |
| Mourning Bride — the realm's elite (§10) | `mourningbride` | `mobs/forestspector.png` | **320×320** | Walking mob sheet. |
| Headless Butler — melee, says "Rude." on death | `headlessbutler` | `mobs/bonewalker.png` | **448×320** | Walking mob sheet. |
| Soul Hound — fast | `soulhound` | `mobs/jackal.png` | **448×256** | Walking mob sheet. |
| Lantern Widow — ranged | `lanternwidow` | `mobs/phantom.png` | **192×32** | Single row, not the four-direction grid — matches how vanilla's own floating phantom renders. |
| Coffin Crawler — "a coffin with legs" | `coffincrawler` | `mobs/desertcrawler.png` | **128×32** | Small single-row sheet — most of the mob's screen presence is the ground-mound trail it leaves (next three rows), not a large walking body. |
| Coffin Crawler's ground-mound trail (frame 1) | `coffincrawler` | `mobs/mound1.png` | **18×8** | Tiny prop sprite. |
| Coffin Crawler's ground-mound trail (frame 2) | `coffincrawler` | `mobs/mound2.png` | **16×8** | Tiny prop sprite. |
| Coffin Crawler's ground-mound trail (frame 3) | `coffincrawler` | `mobs/mound3.png` | **14×8** | Tiny prop sprite. |
| Ghost Moss | `ghostmosstile` | `tiles/swampgrass_splat.png` | **224×384** | Ground splat. |
| Black Cobble | `blackcobbletile` | `tiles/ravenfloor_splat.png` | **224×576** | Ground splat. |
| Violet Dirt | `violetdirttile` | `tiles/cryptash_splat.png` | **224×192** | Ground splat. |
| Spirit Stone | `spiritstonetile` | `tiles/stonebrickfloor_splat.png` | **224×192** | Ground splat. |
| Graveyard Soil | `graveyardsoiltile` | `tiles/swamprock_splat.png` | **224×192** | Ground splat. |
| Ectoplasm Puddle (shallow layer) | `ectoplasmtile` | `tiles/swampfreshwater_shallow_splat.png` | **1792×224** | Liquid animation strip, not the 224-wide splat shape — same caveat as Eden's shallow water. Colour is already turquoise at draw time (`EctoplasmTile.getLiquidColor`); only the sheet's own texture needs new art. |
| Ectoplasm Puddle (deep layer) | `ectoplasmtile` | `tiles/swampfreshwater_deep_splat.png` | **1792×224** | Same liquid-strip note. |
| Bonewood Tree | `bonewoodtree` (world) | `objects/deadwood.png` | **128×512** | Object/tree sheet. |
| Spirit Willow (world sprite) | `spiritwillow` (world) | `objects/willowtree.png` | **256×512** | Object/tree sheet. |
| Spirit Willow **and** Lantern Tree (shared icon) | `spiritwillow`, `lanterntree` (icon) | `items/willowtree.png` | **32×32** | Item icon — both trees currently show the same icon even though Lantern Tree's world sprite is already custom (`objects/gloomwillow.png`). |
| Crooked Dead Tree **and** Bonewood Tree (shared icon) | `crookeddeadtree`, `bonewoodtree` (icon) | `items/deadwoodtree.png` | **32×32** | Item icon — same note: Crooked Dead Tree's world sprite is already custom. |
| Spectral Ore Rock (world sprite) | `spectralorerock` (world) | `objects/cryptorerock_nightsteelore.png` | **32×96** | Ore-rock overlay sheet. |
| Spectral Ore Rock (icon) **and** Spectral Ore material (icon) | `spectralorerock` (icon), `spectralore` | `items/nightsteelore.png` | **32×32** | Item icon, shared between the rock and the raw material. |
| Ghost Gravestone — the realm's registered natural gravestone, and the same picture placed directly by ID in the Mausoleum and Sunken Graveyard presets | `ghostgravestone` (world), plus preset placements | `objects/cryptgravestone1.png` | **128×96** | Object sheet — one file, three placements. |
| Ghost Gravestone (icon) | `ghostgravestone` | `items/cryptgravestone1.png` | **30×32** | Item icon — not quite square; read exactly. |
| Soul Basin (world) — the object that gates entry to the whole realm: fill it with 12 Ectoplasm and it becomes the Ghost Gate | `soulbasin` (world) | `objects/spiritbasin.png` | **32×64** | Object sheet. Small file, outsized narrative weight — this is the one prop every player interacts with to get in. |
| Soul Basin (icon) | `soulbasin` | `items/spiritbasin.png` | **32×32** | Item icon. |
| Soul Loom (icon only — world sprite is already custom, `objects/windsilkloom.png`) | `soulloom` | `items/caveglowalchemytable.png` | **32×32** | Item icon. |
| Spirit Forge (icon only — world sprite is already custom, `objects/aetherforge.png`) | `spiritforge` | `items/forge.png` | **32×32** | Item icon. |
| Bonewood (material icon) | `bonewood` | `items/deadwoodlog.png` | **32×32** | Item icon. |
| Soul Thread (material icon) | `soulthread` | `items/clothscraps.png` | **32×32** | Item icon. |
| Spiritsteel Bar (material icon) | `spiritsteelbar` | `items/nightsteelbar.png` | **32×32** | Item icon. |
| Spiritsteel Crown (icon) | `spiritsteelhelmet` | `items/soulseedcrown.png` | **30×32** | Item icon — not quite square. |
| Spiritsteel Plate (icon) | `spiritsteelchestplate` | `items/soulseedchestplate.png` | **32×32** | Item icon. |
| Spiritsteel Greaves (icon) | `spiritsteelboots` | `items/soulseedboots.png` | **30×32** | Item icon — not quite square. |
| Spiritsteel Crown (worn) | `spiritsteelhelmet` | `player/armor/soulseedcrown.png` | **448×320** | Worn-armour sheet, full animation grid. |
| Spiritsteel Plate (worn body) | `spiritsteelchestplate` | `player/armor/soulseedchest.png` | **448×320** | Worn-armour sheet. |
| Spiritsteel Plate (worn right arm) | `spiritsteelchestplate` | `player/armor/soulseedarms_right.png` | **448×320** | Worn-armour sheet. |
| Spiritsteel Plate (worn left arm) | `spiritsteelchestplate` | `player/armor/soulseedarms_left.png` | **16×16** | This is vanilla's own file, measured as shipped — the left-arm sheet is far smaller than the right. Confirm in-engine before assuming a mismatch is a mistake to fix. |
| Spiritsteel Greaves (worn) | `spiritsteelboots` | `player/armor/soulseedboots.png` | **448×320** | Worn-armour sheet. |
| Mausoleum wall | POI preset dressing | `objects/cryptwall.png` | **352×128** | Wall-set sheet (autotile + door cells) — see `docs/references/wall-template-map.png` before drawing a wall set. Reused generic architecture, low priority. |
| Haunted Manor chest, Mausoleum chest, Sunken Graveyard chest | POI preset dressing | `objects/bonechest.png` | **128×64** | Object sheet, shared by all three presets. Low priority. |
| Mausoleum coffin | POI preset dressing | `objects/cryptcoffin.png` | **192×64** | Object sheet. Low priority. |
| Sunken Graveyard fence | POI preset dressing | `objects/cryptfence.png` | **160×64** | Object sheet. Low priority. |
| Sunken Graveyard gate | POI preset dressing | `objects/cryptfencegate.png` | **192×64** | Object sheet. Low priority. |
| Sunken Graveyard gravestone (second type) | POI preset dressing | `objects/cryptgravestone2.png` | **128×96** | Object sheet. Low priority. |
| Haunted Manor chair | POI preset dressing | `objects/deadwoodchair.png` | **128×64** | Object sheet. Low priority. |
| Haunted Manor table | POI preset dressing | `objects/deadwoodmodulartable.png` | **96×64** | Object sheet. Low priority. |
| Haunted Manor candelabra | POI preset dressing | `objects/deadwoodcandelabra.png` | **128×64** | Object sheet. Low priority. |
| Mausoleum column | POI preset dressing | `objects/cryptcolumn.png` | **32×64** | Object sheet. Low priority. |
| Mausoleum candle | POI preset dressing | `objects/candle.png` | **32×32** | Object sheet. Low priority. |
| Mausoleum urn | POI preset dressing | `objects/vases.png` | **128×64** | Object sheet. Low priority. |
| Haunted Manor wall | POI preset dressing | `ObjectRegistry.getObjectID("deadwoodwall")` — **not found under any name in the vanilla dump** | **unknown** | Do not guess a size for this one. `HauntedManorPreset.java:17` asks the registry for an object ID that doesn't match any of the 6,121 files under `vanilla-sprites/`, including every vanilla wall sheet — see the note at the end of this document. |
| Haunted Manor door | POI preset dressing | `ObjectRegistry.getObjectID("deadwooddoor")` — **not found under any name in the vanilla dump** | **unknown** | Same issue as the wall above; same note. |

---

## Crooked Beyond (Tier 4)

**Palette and mood** (`docs/WORLD_DESIGN.md` §13, §A3.6): reality has stopped
working. Black-and-white stripes, neon green, violet, red, cyan,
checkerboard, spirals — but **still clean Necesse pixel art, not chaotic
random textures**; every asset shares one deliberately defined palette. The
"why" behind the look: *"After death it is not only the landscape that
decays. The rules of the world decay."* Doors with no house, windows lying in
the floor, eyes in the plants, teeth in the rocks, absurdly long chairs,
crooked clocks — the wrongness is specific, not noisy.

`docs/realms/crooked.md`'s table is close to right and matches this list
almost exactly — the one addition here is exact, Pillow-measured sizes for
every row (that doc gives none), plus the Spill's shape. 25 borrowed files.

| what | id | current stand-in | size | format notes |
|---|---|---|---|---|
| Door Mimic — "not livestock; looks like a door; enemy" (§14), the realm's guard-pack anchor | `doormimic` | `mobs/mimic.png` | **384×320** | Walking mob sheet. **Same file as Ghost Realm's Possessed Chair** above. Per its own class javadoc: "it does not yet read as a DOOR" — the joke needs new art to land. |
| Tongue Plant — scenery until it moves | `tongueplant` | `mobs/dryadsentinel.png` | **768×896** | Large multi-row sheet. **Same file as Eden's Jealous Vine** above. |
| Stripe Beetle — catchable, drops Striped Shell | `stripebeetle` | `mobs/scorpion.png` | **224×128** | Walking mob sheet, cell 32px, 7 columns × 4 direction rows. |
| Wrong-Way Tile — "reality no longer works properly," the realm's loudest single idea, drawn under everything else | `wrongwaytile` | `tiles/ascendedvoid_splat.png` | **224×192** | Ground splat. §A3.6 asks for "paths that run visibly wrong"; vanilla's own note in `docs/VANILLA_ASSET_MAP.md` §3 already flags this family as "cosmic-purple, not black-and-white striped" — an acknowledged mismatch with the brief. |
| Checker Stone — the Checkerworks' floor | `checkerstonetile` | `tiles/deepstonetiledfloor_splat.png` | **224×192** | Ground splat. Same "not actually checkered" gap as Wrong-Way Tile above. |
| Violet Mud — the realm's slow ground | `violetmudtile` | `tiles/ascendedcorruption_splat.png` | **224×96** | Ground splat. |
| Spiral Soil — the Spiral Fields' ground | `spiralsoiltile` | `tiles/ascendedgrowth.png` | **128×128** | No `_splat` sibling exists in vanilla, so this takes `TerrainSplatterTile`'s legacy path (4 variant rows through the shared `splattingmask`) rather than the usual 224-wide splat shape. |
| The Spill — the realm's sea, between the landmasses | `spilltile` | `tiles/ooze_splat.png` | **1792×416** | Liquid animation strip, not the 224-wide splat shape (same caveat as Eden's/Ghost's water tiles) — won't be auto-snapped by the fixer. Colour is already neon green at draw time (`SpillTile.getLiquidColor`); only the texture needs new art. |
| Spiral Tree — §13's own name, the realm's woody flora | `spiraltree` | `objects/burnedbush.png` | **128×64** | Object sheet, 2×64px variants. |
| Eyeball Shrub — "eyes in plants" | `eyeballshrub` | `objects/voidtrap.png` | **160×64** | Object sheet, 5×32px variants. |
| Screaming Flower — glows faintly red | `screamingflower` | `objects/glowcoral.png` | **128×32** | Object sheet, 4×32px variants. |
| Striped Mushroom | `stripedmushroom` | `objects/mushroom.png` | **224×64** | Object sheet, 7×32px growth variants. |
| Bent Grass — the realm's carpet | `bentgrass` | `objects/witheredgrass.png` | **256×32** | Object sheet, 8×32px variants. |
| Bent Lantern — the realm's only natural light source | `bentlantern` | `objects/voidflame.png` | **128×64** | Object sheet, 4×32px variants. |
| Crooked Clock | `crookedclock` | `objects/boneclock.png` | **128×64** | Object sheet, 4×32px rotations. |
| Long Chair — "absurdly long chairs" | `longchair` | `objects/bonechair.png` | **128×64** | Object sheet, 4×32px rotations. |
| Ground Window — "windows in the ground," §A3.6's clearest single image | `groundwindow` | `objects/voidcube.png` | **32×48** | Object sheet. |
| Teeth-Rock — "a rock that reads as a clenched mouth" | `teethrock` | `objects/smallrunestone.png` | **192×64** | Object sheet, 6×32px variants — the realm's one mineable formation. |
| Oddwood (icon) | `oddwood` | `items/deadwoodlog.png` | **32×32** | Item icon. |
| Warp Resin (icon) | `warpresin` | `items/bioessence.png` | **32×32** | Item icon. |
| Strange Fabric (icon) | `strangefabric` | `items/clothscraps.png` | **32×32** | Item icon. |
| Eye Seed (icon) | `eyeseed` | `items/crystalessence.png` | **32×32** | Item icon. |
| Striped Shell (icon) | `stripedshell` | `items/crystallizedskull.png` | **32×32** | Item icon. |
| Reality Shard (icon) | `realityshard` | `items/ascendedshard.png` | **32×32** | Item icon. |
| Door Yard, Inverted House and Long Table presets' loot container | POI preset dressing | `objects/barrel.png` | **32×64** | Object sheet, shared across three presets. Low priority — a barrel is a barrel. |

*(Not in this table: `tiles/beetlefreak_splat.png` for Crooked Stripe Tile,
and `objects/veilriftdown.png` / `objects/veilriftup.png` for the two Crooked
doors — all three are already the mod's own art, reused rather than
borrowed. `objects/skycrate.png` for the Crooked Crate is likewise already
ours.)*

---

## A note on the two unresolved Ghost IDs

`HauntedManorPreset.java` asks `ObjectRegistry.getObjectID("deadwoodwall")`
and `ObjectRegistry.getObjectID("deadwooddoor")` for the manor's outer walls
and door. Neither string matches a file anywhere under `vanilla-sprites/` —
every vanilla wall sheet was checked (`ancientruinwall`, `arcanicwall`,
`ascendedwall`, `bamboowall`, `basaltwall`, `brickwall`, `cryptwall`,
`crystalwall`, `dawnwall`, `deepsandstonewall`, `deepsnowstonewall`,
`deepstonewall`, `deepswampstonewall`, `dryadwall`, `dungeonwall`, `duskwall`,
`factorywall`, `granitewall`, `icewall`, `obsidianwall`, `palmwall`,
`pinewall`, `ravenwall`, `sandstonewall`, `snowstonewall`, `spidercastlewall`,
`stonewall`, `swampstonewall`, `willowwall`, `woodwall` — no "deadwood" among
them), and vanilla's actual Deadwood furniture family (`deadwoodchair`,
`deadwoodchest`, `deadwoodclock`, and a dozen more — all present) has no wall
or door piece at all. This isn't an asset-format question this document can
answer; it reads like a stale or invalid object reference from before the
wall/door IDs it expects existed (or never did), which would mean the Haunted
Manor preset currently places no outer wall and no door — worth a second pair
of eyes rather than a guessed-at replacement sprite.
