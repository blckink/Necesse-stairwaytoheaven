# Vanilla asset map — what the mod borrows, and for which realm

**This is the file the player works from.** The agreed method
(2026-08-31): build every realm now out of mod assets plus suitable vanilla
assets, then replace all borrowed assets in one pass so the style swaps out
wholesale.

That only works if every borrowed asset is written down. So:

1. **Every vanilla asset used in worldgen, a preset or a spawn table gets a
   row here, in the same commit that uses it.**
2. A row names the vanilla asset, the realm it serves, what it stands in for,
   and where it is referenced.
3. Vanilla assets are referenced **by string ID** wherever possible — never
   copied into `src/main/resources/`. A copy is a fork, and a fork will not
   swap out cleanly. The two exceptions below are marked and explained.
4. When a mod asset replaces a stand-in, its row moves to §4 rather than being
   deleted, so the swap keeps its history.

Realm names follow `docs/WORLD_DESIGN.md`.

---

## 1. In use today — verified against the source

Everything in this table is referenced by the shipped code right now. Nothing
here is a plan.

### 1.1 Mobs referenced by vanilla string ID (spawn tables)

These are vanilla mobs placed directly. No art was copied; the game supplies it.
They swap out by replacing the string ID with a mod mob of the same role.

*(empty — the three Crooked Beyond entries that used to live here were the only
ones, and they moved to §4 on 2026-09-01.)*

| vanilla ID | realm | stands in for | referenced in |
|---|---|---|---|

### 1.1b Vanilla objects grown at runtime

| vanilla asset | where | why |
|---|---|---|
| `grass` (object, by string ID) | `tiles/OvergrownEdenTile` grows it on empty Eden-grass tiles (tick + simulate), exactly as vanilla's `OvergrownGrassTile` does | Eden's own flora does not exist yet; vanilla's green tufts read right on the supplied deep-green ground and swap out when the Eden chapter lands |

### 1.2 Vanilla mobs subclassed (behaviour + sheet both borrowed)

Each of these is a mod class that extends a vanilla mob and wears that mob's own
texture from `MobRegistry.Textures`. Swapping means giving the subclass its own
sheet — the behaviour stays. (The Outlands' three did exactly that on
2026-09-01 and moved to §4; they still subclass vanilla mobs, so they borrow
behaviour, but no vanilla art reaches the screen through them.)

| vanilla base | mod mob | realm | why that base |
|---|---|---|---|
| `AncientSkeletonMageMob` | Cinder Cantor | Ghost Realm | ranged caster |
| `FrostSentryMob` | Rime Sentry | Skyreach (Stormveil variant) | immobile turret |
| `CryoFlakeMob` | Aurora Flake | Skyreach (Aurora variant) | drifting cold hazard — sheet swapped 2026-09-02, see §4 |
| `CryoFlakeFollowingMob` | Watch Mote | Skyreach | summon behaviour |
| `SheepMob` | Glimmergoat | Skyreach (Aurora variant) | husbandry: shears, breeding — sheets swapped 2026-09-02, see §4 |
| `CowMob` | Nimbus Yak | Skyreach (Driftlands variant) | husbandry: bucket — sheet swapped 2026-09-02, see §4 |

### 1.3 Vanilla textures loaded by literal path (recolours)

`livestock/SkyPelt` recolours these at load time — the file is never copied, it
is read from the game's own resources and tinted. This is the cheapest possible
stand-in and swaps out by shipping a real sheet under the mod's own name.

| vanilla texture | realm | used for |
|---|---|---|
| `items/clothboots` | Skyreach | Glimmerstride Boots |

### 1.3b Vanilla textures used as-is by literal path

Loaded straight from the game's own resources, no copy in
`src/main/resources/`. Each swaps out by shipping a mod file under the name the
engine would look for by default.

| vanilla texture | realm | used for | referenced in |
|---|---|---|---|
| `buffs/spirithaunted` | the Veil | the Soul Exposure debuff icon — vanilla's own "the dead have hold of you" icon, 32x32 | `veil/SoulExposureBuff.BORROWED_ICON`; swaps out by adding `buffs/soulexposure.png` and deleting the `loadTextures()` override |
| `particles/fog` (`GameResources.fogParticles`) | the Veil | the drifting mist the fog is made of — the same sheet `HuginStatueObjectEntity` uses, four 32x16 frames | `veil/VeilFogBuff.clientTick` |
| `items/necroticgreatsword` (32x32) | Ghost Realm | the Spiritsteel Reaver's inventory icon — vanilla's own necrotic greatsword, the closest two-hander the game has to a blade forged out of the dead | `realms/ghost/SpiritsteelReaver.ART`; swaps out by adding `items/spiritsteelreaver.png` and deleting the `loadItemTextures()` override |
| `player/weapons/necroticgreatsword` (96x96) | Ghost Realm | the same blade drawn in the player's hands mid-swing | `realms/ghost/SpiritsteelReaver.loadAttackTexture` |
| `items/necroticbow` (32x32) | Ghost Realm | the Gravewind Bow's inventory icon — vanilla's own necrotic bow | `realms/ghost/GravewindBow.ART`; swaps out by adding `items/gravewindbow.png` and deleting the `loadItemTextures()` override |
| `player/weapons/necroticbow` (36x66) | Ghost Realm | the same bow drawn at full draw | `realms/ghost/GravewindBow.loadAttackTexture` |

### 1.3b Vanilla art worn or shown by the mod's settlers (NO recolour)

The five new settlers add **zero PNGs**. A `HumanShop` settler is drawn as a
plain human body wearing real clothing ITEMS — the way vanilla dresses its Elder
in `elderhat`/`eldershirt`/`eldershoes` — plus one 32px face icon for the
settlement screen. So each new person costs three vanilla item IDs and one
vanilla icon path, both read straight out of the game's resources.

**These are used AS THEY ARE. Nothing here is tinted at load time** — the player
forbade load-time recolouring for new content, so §1.3's `SkyPelt` technique is
deliberately not used by any of this.

| who | source path / item ID | what it stands in for |
|---|---|---|
| Eveleen | `mobs/icons/farmerhuman` (icon) | her settlement-screen face — she is the farming settler |
| Eveleen | `dryadhat`, `dryadchestplate`, `dryadboots` (worn items) | the Eden Botanist's leaf-and-bark clothes |
| Mortimer | `mobs/icons/pawnbrokerhuman` (icon) | his settlement-screen face |
| Mortimer | `tophat`, `thiefscloak`, `dressshoes` (worn items) | the Undertaker's top hat and black coat |
| Caspern | `mobs/icons/blacksmithhuman` (icon) | his settlement-screen face — he is the smith |
| Caspern | `nightsteelveil`, `smithingapron`, `smithingshoes` (worn items) | the Spirit Smith's veil and apron |
| Eleanor | `mobs/icons/stylisthuman` (icon) | her settlement-screen face |
| Eleanor | `snowhood`, `snowcloak`, `clothboots` (worn items) | the Lost Soul's pale, cold clothes |
| Ghost Guide | `snowhood` (32x32), `snowcloak` (32x32), `clothboots` (30x32) (worn items) | the summoned guide's pale hooded silhouette — deliberately Eleanor's own three, so the Ghost Realm's two dead read as the same kind of person. No icon row: he is never a settler, so no settlement screen ever asks for a face |
| Mr. Knott | `mobs/icons/exoticmerchanthuman` (icon) | his settlement-screen face — a merchant who deals in stranger things |
| Mr. Knott | `jesterhat`, `labcoat`, `jesterboots` (worn items) | the Doorman's jester hat and lab coat — a showman testing whether a door leads anywhere |

Note that the icon only shows where no mob is available:
`Settler.getSettlerFaceDrawOptions` draws the settler's own human face from the
live mob whenever there is one, so in practice the borrowed icon is a fallback.

### 1.3c Vanilla items standing in for named mod materials (shop goods)

`WORLD_DESIGN.md` §11 names three Ghost-Realm materials that do not exist and
are not this pass's to invent. Each is sold under its vanilla nearest neighbour
until the Ghost Realm ships its own, at which point only the shop line changes.

| named in the design | vanilla stand-in | sold by |
|---|---|---|
| Spiritsteel | `nightsteelbar`, `nightsteelore` | Caspern |
| Soul Thread | `phantomdust` | Caspern |
| spectral weapons | `bonearrow`, `bonehilt`, `nightsteelveil` | Caspern |
| Bonewood furniture, coffin, urns, black candles (§11) | `bonechair`, `bonemodulartable`, `bonebookshelf`, `bonedresser`, `boneclock`, `bonechest`, `sarcophagus`, `spiritbasin`, `bonecandelabra`, `deadwoodcandles`, `candle`, `skull`, `gravestone1/2`, `cryptgravestone1/2` | Mortimer |
| Bee Hive (§5) | `queenbee` — vanilla's `beehive` object is registered NOT obtainable | Eveleen |
| Eleanor's "strong trinket" (§11) | `willowisplantern` | her PASS ON ending |

### 1.4 Vanilla items used as drops

| vanilla item | realm | dropped by |
|---|---|---|
| `crystalstone` | Crooked Beyond | `evilwall` — the player's call: it matches the sprite |
| `stone` | Ghost Realm | `veilrock` |

### 1.5 Vanilla sheets a mod asset was DRAWN ON

Not borrowed at runtime — the art is the player's own, but its layout is
vanilla's, so the vanilla file is the format reference and must stay findable.

| vanilla sheet | mod asset | what the format gave us |
|---|---|---|
| `objects/crystalwall.png` | `objects/evilwall.png` + `items/evilwall.png` | `RockObject`: 16px sprite cells, `width/32` variants, 13 rows. See `docs/TECHNICAL_LEARNINGS.md` |
| `tiles/spidernest_splat.png` | `tiles/beetlefreak_splat.png` | `TerrainSplatterTile` on the **wide** alpha mask (`splattingmaskwide`), not the default |
| `objects/stonewall.png` | `objects/beetlewall.png` | `WallObject` autotile layout, doors and the roof-slot window |
| `mobs/crystalgolem.png` | `mobs/crookedgolem.png` | walking-mob sheet, 384x320: 6 cols (idle, walk x4, in-liquid) x 4 dir rows at 64px, plus the 32px gib strip at row 8 cols 0-3 that `FleshParticle` cuts death chunks from |
| `mobs/ascendedgolem.png` | `mobs/rarecrookedgolem.png` | same 384x320 layout |
| `mobs/crystalarmadillo.png` | `mobs/crookedarmadillo.png` | same layout at 512x320 — 8 cols, because columns 6 and 7 are the two rolled-up ball frames |
| `mobs/crystaldragon.png` + `mobs/crystaldragonhead.png` | `mobs/mistserpent.png` + `mobs/mistserpent_shadow.png` + `mobs/mistserpenthead.png` | worm-chain sheet: body/shadow at 320x1792 (pixel-identical to the vanilla dragon's), head at 68x68. Behaviour is NOT borrowed from `CrystalDragonHead` — `MistserpentHead`/`MistserpentBody` extend the generic `HostileWormMobHead`/`Body`, the same non-boss base `SandwormHead` uses; only the sheet geometry is the dragon's, added 2026-09-02 |

### 1.6 MOD sheets reused as stand-ins by a second object

Not vanilla art at all — the mod's own PNGs, worn by a **second** object because
the thing that object should look like has not been drawn yet. They are listed
here for the same reason everything else is: the swap pass has to be able to
find every place a sprite is standing in for something, and "it is our own file"
does not make it any less a stand-in.

**The boss portals** (`docs/FOGKEY_AND_BOSSPORTALS.md` §B3) are supposed to look
like their realm's **key piece** — the buildable object §B1 makes an Elder-quest
reward — so that a player who meets one recognises what they need. Those key
pieces are §B1's work and do not exist yet. Until they do, each realm's portal
borrows the closest thing the mod already draws. Each swaps out by changing one
constant in `bosses/BossPortalObject`; nothing is copied and nothing is
recoloured.

| mod sheet | size | realm | worn a second time by | why that sheet |
|---|---|---|---|---|
| `objects/wardenbeaconon.png` | 32x96 | Skyreach | `bossportalskyreach` (`BossPortalObject.SPRITE_SKYREACH`) | the one thing the Skyreach already draws that reads as "something happens here" — a lit standing marker |
| `objects/skystairwaydown.png` | 32x96 | Garden of Eden | `bossportaleden` (`SPRITE_EDEN`) | already Eden's doorway: `EdenGateObject` passes `"skystairway"` to `LadderDownObject`, which reads exactly this file |
| `objects/statues/seraph.png` | 96x192 | Steinfeld | `bossportalsteinfeld` (`SPRITE_STEINFELD`) | §B1 names *"a statue for Steinfeld"*, and the realm already stands this sheet up as its `brokenangel` (`StatueObject("seraph", 32, 1)`) |
| `objects/statues/gloomraven.png` | 64x96 | Ghost Realm | `bossportalghostrealm` (`SPRITE_GHOST`) | the mod's own grave-marker statue, already scattered by `SkyTerrainPainter` through the realms the Skyway does not reach |
| `objects/veilriftdown.png` | 32x96 | Crooked Beyond | `bossportalcrookedbeyond` (`SPRITE_CROOKED`) | this file IS Mr. Knott's door in the shipped mod — `CrookedDoorObject` passes `"veilrift"` to `LadderDownObject` — and §B1 names that door as Crooked's key piece |

All five are loaded by literal path from `BossPortalObject.loadBorrowedSheets()`,
which exists so `tools/locale_audit.py` can see them: the objects themselves must
load through a field (the path differs per realm), and a path the audit cannot
read as a literal is a path nothing checks.
### 1.6 The mod's OWN sprites read by a second registration

Not a vanilla borrow at all, and listed here for the same reason the vanilla
rows are: a file drawn for one thing and read by another is a fact somebody
will need when the art is redone, and the generator that owns the file must not
be renamed without checking this column.

| mod sprite | size | also read by | why |
|---|---|---|---|
| `items/seancecircle.png` (generated by `tools/asset_generator/gen_veil.py:gen_seancecircle`) | 32x32 | `ghostchalk` (`items/GhostChalkItem`) | the chalk's icon is a picture of the ring it draws. The `seancecircle` OBJECT is registered unobtainable, so it no longer owns an item of its own and the icon is free; nothing was copied and nothing was recoloured |

---

## 2. Stand-ins for the later realms

Every entry was checked against the real dump in `vanilla-sprites/` (6,121
files, game 1.3.2). Eden, Ghost and Crooked entries are now in use where their
realm sections say so; the remaining rows are the future shopping list.

### 2.1 Garden of Eden (Tier 1) — generated core in use

| vanilla asset | stands in for |
|---|---|
| `tiles/overgrowngrass`, `tiles/overgrownplainsgrass` | Eden Grass — the densest green vanilla has |
| `tiles/swampgrass` | Eden Moss |
| `tiles/sand` | White Paradise Sand |
| `tiles/saltwater_shallow` | Turquoise Shallow Water |
| `tiles/dryadfloor`, `tiles/dryadpath` | Root Floor |
| `tiles/ancientroots` | the Knowledge Tree's ground |
| `objects/palmtree`, `coconuttree` | Paradise Palm |
| `objects/bananatree`, `lemontree`, `appletree` | Tree of Plenty, Fig, Pomegranate |
| `objects/dryadtree` | Knowledge Tree |
| `objects/blackberrybush`, `blueberrybush` | Eden Berry bushes |
| `objects/blueflowerpatch` | Blue Paradise Flower |
| `objects/ivyore` | Eden Copper / Verdant Ore |
| the whole `palm*` furniture family (bed, chair, table, chest, wall, …) | Eden architecture — a complete set already exists |
| `mobs/crocodile`, `stabbybush`, `dryadsentinel`, `bee`, `dragonwhelp` | Eden Serpent, Bloom Maw, Jealous Vine, Golden Hornet and Forbidden Serpent bodies |
| `items/palmlog`, `dryadbranch`, `apple`, `sharkscales`, `fangoftheprotector`, `honey`, `dryadsapling`, `coconut`, `blueberry`, `frozenberry`, `raspberry`, `ivyore`, `ivybar` | Eden's first material and fruit icons |
| `objects/spiritbasin.png`, `items/spiritbasin.png` | the Eden Threshold (`edenseedbasin`) — vanilla's Spirit Basin, reused a second time; the Ghost Gate's Soul Basin already borrows it once |
| this mod's own `objects/skystairwaydown.png`, `objects/skystairwayup.png` | the Eden Gate down/up pair (`edengatedown`/`edengateup`) — the Skyward Stairway's own sheets, reused a second time rather than drawn fresh |

### 2.2 Steinfeld (Tier 2) — in use

The rows above the line are the original exploratory notes, kept for record;
the realm that actually shipped diverged from several of them (Weathered
Stone ended up this mod's own `skystone`, not vanilla gravel/rock). The rows
below are what `SteinfeldRealm` actually registers — see
`docs/realms/steinfeld.md` for the full table with reasoning per entry.

| vanilla asset | stands in for |
|---|---|
| `tiles/gravel`, `tiles/rock` | Weathered Stone |
| `tiles/granite`, `granitepath` | Pale Stone ground |
| `tiles/cryptash`, `tiles/cryptpath` | Ash Grass, Grave Soil |
| `objects/gravestone1`, `gravestone2` | gravestones |
| `objects/statues` | the broken angel statues |
| `objects/burnedtree`, `burnedtreestump` | dead trees |
| `objects/largemossysteppingstone` | stone slabs |
| --- | --- |
| `tiles/stonetiledfloor`, `dirt`, `rock` | Cracked Heaven Marble, Dead Soil and Mist Stone (`SteinfeldRealm`) |
| mod `tiles/cloudturf`, `skystone`; `tiles/murkmoss`, `cryptash` | Pale Grass, Weathered Stone, Ash Grass and Grave Soil (`SteinfeldRealm`) |
| `objects/witheredgrass` | Withered Tuft |
| mod `objects/skyreeds`, `cragbloom`, `aurorabloom`, `gloomshroom`, `staticmoss` | Pale Reed, Widow Flower, Dead Heaven Bloom, Ghost Mushroom and Spirit Moss Patch (`SteinfeldRealm`) |
| mod `objects/skystonerock`, `veilrock` | Pale Stone Rock and Grave Salt Rock, on vanilla's `RockObject` (`SteinfeldRealm`) |
| `objects/statues/mossymonkstatue`; mod `objects/statues/seraph` | Mourner Statue and Broken Angel, on vanilla's `StatueObject` (`SteinfeldRealm`) |
| `objects/cryptcolumn` | Chapel Column, on vanilla's `ColumnObject` (`SteinfeldRealm`) |
| mod `objects/skywatchrubble` | Heaven Slab, on `SkyDecoObject` (`SteinfeldRealm`) |
| `objects/cryptfence`, `cryptgravestone1` | Grave Fence and (this realm's) Gravestone — read by ID directly, not re-registered (`SteinfeldRealm`) |
| `items/cryptstone`, `alchemyshard`, `phantomdust`, `pearlescentshard` | Pale Stone, Grave Salt, Spirit Moss and Echo Shard icons (`SteinfeldRealm`) |
| `mobs/deepcavespirit`, `ancientarmoredskeleton`, `crystalgolem`, `crazedraven` | Lost Pilgrim, Stone Mourner, Hollow Angel and Grave Crow bodies (`SteinfeldRealm`) |

### 2.3 Ghost Realm (Tier 3) — in use on `codex/ghost`

| vanilla asset | stands in for |
|---|---|
| `tiles/cryptash`, `cryptpath` | Graveyard Soil |
| `tiles/ooze` | Ectoplasm Puddle |
| `tiles/ravenfloor`, `willowfloor` | Black Cobble |
| `tiles/moonpath`, `darkmoonpath`, `darkfullmoonpath` | Spirit Stone paths |
| `objects/cryptgravestone1/2`, `cryptcolumn`, `cryptfence`, `cryptfencegate` | the graveyard kit — complete |
| `objects/cryptcoffin`, `stonecoffin`, `basaltcoffin` | coffins and sarcophagi |
| `objects/cryptwall` | mausoleum walls |
| `objects/ravenskull`, `mosscoveredskull`, `skull` | bone deco |
| `objects/deadwoodcandles`, `waterlantern`, `swampcandlestand` | floating candles, lanterns |
| the whole `bone*` furniture family (bed, bench, bookshelf, clock, chest, …) | Bonewood furniture — a complete set already exists |
| `objects/cryptorerock_nightsteelore` | Spectral Ore |
| `tiles/swampgrass`, `swamprock`, `swampfreshwater_shallow`, `swampfreshwater_deep` | Ghost Moss, Graveyard Soil and the two Ectoplasm liquid sheets (`GhostRealm`) |
| `tiles/stonebrickfloor`, `ravenfloor`; mod `tiles/murkmoss` | Spirit Stone, Black Cobble and Haunted Grass (`GhostRealm`) |
| `objects/deadtree`, `deadwood`, `willowtree`; mod `objects/gloomwillow` | Crooked Dead Tree, Bonewood Tree, Spirit Willow and Lantern Tree (`GhostRealm`) |
| `objects/aurorabloom`, `cragbloom`, `gloomshroom`, `withershrub` | the five soft Ghost plants (`GhostRealm`) |
| `objects/veilrock`, `cryptgravestone1`, `spiritbasin` | Ghost Rock, world gravestone and Soul Basin (`GhostRealm`) |
| mod `objects/windsilkloom`, `aetherforge`; `items/caveglowalchemytable`, `forge` | Soul Loom and Spirit Forge world sheets and icons (`GhostRealm`) |
| `items/deadwoodlog`, `clothscraps`, `nightsteelore`, `nightsteelbar` | Bonewood, Soul Thread, Spectral Ore and Spiritsteel Bar (`GhostRealm`) |
| `items/soulseedcrown`, `soulseedchestplate`, `soulseedboots`; matching `player/armor/soulseed*` | complete Spiritsteel armour stand-ins |
| mod `objects/veilriftdown`, `veilriftup` | Ghost Gate pair |

### 2.4 Crooked Beyond (Tier 4) — partly built already

| vanilla asset | stands in for |
|---|---|
| `tiles/ascendedvoid` + `_fog` / `_grime` / `_stars` / `_swirls` | Wrong-Way Tile, Spiral Soil — vanilla's own "reality is broken" ground |
| `tiles/ascendedcorruption`, `ascendedgrowth` | Bent Grass |
| `tiles/crystaltile` | Checker Stone |
| `tiles/arcanicfloor`, `arcanicpath` | Crooked architecture floor |
| `tiles/spidercastlecarpet` | Stripe Carpet ground |
| `objects/obsidianrock` | teeth-rocks |
| `tiles/ascendedgrowth`, `ascendedcorruption`, `deepstonetiledfloor`, `ascendedvoid` | Spiral Soil, Violet Mud, Checker Stone and Wrong-Way ground in `realms/crooked` |
| `objects/burnedbush`, `voidtrap`, `glowcoral`, `mushroom`, `witheredgrass` | Spiral Tree, Eyeball Shrub, Screaming Flower, Striped Mushroom and Bent Grass |
| `objects/voidflame`, `boneclock`, `bonechair`, `voidcube`, `smallrunestone` | Bent Lantern, Crooked Clock, Long Chair, Ground Window and Teeth-Rock |
| `mobs/mimic`, `mobs/dryadsentinel`, `mobs/scorpion` | Door Mimic, Tongue Plant and Stripe Beetle bodies |
| `items/deadwoodlog`, `bioessence`, `clothscraps`, `crystalessence`, `crystallizedskull`, `ascendedshard` | Oddwood, Warp Resin, Strange Fabric, Eye Seed, Striped Shell and Reality Shard icons |

### 2.5 Infernal Fringe + Hell (Tier 5)

| vanilla asset | stands in for |
|---|---|
| `tiles/lava`, `lavapath`, `lavapath_light` | lava |
| `tiles/basaltfloor`, `basaltpath`, `basaltrock` | hell stone |
| `tiles/scrapfloor`, `junkfloor`, `factoryfloortile` | the bureaucratic/industrial floors |
| `tiles/rustgravel`, `tiles/ash` | Ash Soil |
| `objects/demonicanvil`, `demonicworkstation` | Infernal Forge stand-in |
| `objects/demonchest` | hell storage |
| `objects/scrapheap`, `scrapheap_big`, `scraplamp`, `scraprock` | Scrap Heap, Scrap Lamp |
| `objects/charredrock` | Hellsteel-bearing rock |
| `objects/burnedbush`, `burnedleafpile` | hell vegetation |
| `objects/fuelskullencasing` | Boiler / Furnace Core deco |

---

## 3. What vanilla does NOT have

Worth knowing before the player draws anything, because these have no stand-in
and the realm cannot be faked without them:

- **Skyreach's whole palette.** Nothing in vanilla is white/cream/gold/pink sky.
  This realm was always going to be mod art, and it already is.
- **Eden's giant flora.** Vanilla's jungle is normal-sized; §5 of the world
  design asks for oversized. Palms and fruit trees stand in for placement, not
  for scale.
- **The Crooked stripe/checker language.** `ascendedvoid` is the closest and it
  is cosmic-purple, not black-and-white striped. The mod's own `beetlefreak`
  ground is nearer the brief than anything vanilla has.
- **Hell as a city.** Vanilla has lava and scrap; it has no demon tenements,
  shops or offices. §19's whole point is architecture vanilla does not carry.

---

## 4. Replaced — stand-ins that have been swapped out

| vanilla asset | replaced by | commit |
|---|---|---|
| `crystalgolem` (mob, by string ID) | `crookedgolem` — `mobs/CrookedGolemMob` on `mobs/crookedgolem.png`, a subclass of `CrystalGolemMob` that inherits every number and behaviour and overrides only `addDrawables` (and the sheet the death gibs are cut from) | this pass |
| `ascendedgolem` (mob, by string ID) | `rarecrookedgolem` — `mobs/RareCrookedGolemMob` on `mobs/rarecrookedgolem.png`, same relationship to `AscendedGolemMob` | this pass |
| `spiritghoul` (mob sheet) | `fenwraith` — `arsenal/FenWraithMob` on `mobs/fenwraith.png`, still a `SpiritGhoulMob` subclass for behaviour, with `addDrawables` ported so only the texture changes. Composed by `tools/resheet_mob.py` from the player's cut frames | 2026-09-02 |
| `mobs/cow`, `mobs/bull`, `mobs/calf` (sheets, recoloured at load time) | `nimbusyak`, `nimbusyak_bull`, `nimbusyak_calf` — the Nimbus Yak's three ages on the mod's own sheets, `GameTexture.fromFile` instead of `SkyPelt.tintFinal`. Composed by `tools/resheet_mob.py`. The mob is still a `CowMob` for behaviour, and `cow_shadow`/`calf_shadow` are still vanilla's — a shadow is a black blob | 2026-09-02 |
| `mobs/cryoflake` (sheet) | `auroraflake` — `arsenal/AuroraFlakeMob` on `mobs/auroraflake.png`, still a `CryoFlakeMob` subclass for behaviour (chime, shatter particles, the spinning two-layer draw) with `addDrawables` ported so only the texture changes. Supplied already on format: 64x128, body over pulse, both centred on the rotation pivot | 2026-09-02 |
| `mobs/sheep`, `mobs/sheep_sheared`, `mobs/ram`, `mobs/ram_sheared`, `mobs/lamb` (sheets, recoloured at load time) | the Glimmergoat's five states on the mod's own sheets, supplied already on vanilla's grid — `glimmergoat-doe`, `-doe_shorn`, `-ram`, `-ram_shorn`, `-lamb`. Four of the five shipped as `gimmergoat-*`; the player confirmed on 2026-09-03 that the missing `l` was a typo, so all five are spelled `glimmergoat-` now, matching the mob id | 2026-09-02 |
| `crystalarmadillo` (mob, by string ID) | `crookedarmadillo` — `mobs/CrookedArmadilloMob` on `mobs/crookedarmadillo.png`, same relationship to `CrystalArmadillo`. One thing did NOT come across: vanilla's second `crystalarmadillo_light` glow pass, because we have one sheet and not two — see the class comment | this pass |

The vanilla sheets stay the format reference for these three and are listed as
such in §1.5; the runtime no longer touches them.
