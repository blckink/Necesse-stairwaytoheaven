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
| `SheepMob` | Glimmergoat | Skyreach (Aurora variant) | husbandry: shears, breeding |
| `CowMob` | Nimbus Yak | Skyreach (Driftlands variant) | husbandry: bucket — sheet swapped 2026-09-02, see §4 |
| `ChickenMob` | Thunderquill Fowl | Skyreach (Stormveil variant) | husbandry: eggs + shears |

### 1.3 Vanilla textures loaded by literal path (recolours)

`livestock/SkyPelt` recolours these at load time — the file is never copied, it
is read from the game's own resources and tinted. This is the cheapest possible
stand-in and swaps out by shipping a real sheet under the mod's own name.

| vanilla texture | realm | used for |
|---|---|---|
| `mobs/sheep`, `mobs/sheep_sheared` | Skyreach | Glimmergoat doe + shorn |
| `mobs/ram`, `mobs/ram_sheared` | Skyreach | Glimmergoat buck + shorn |
| `mobs/lamb` | Skyreach | Glimmergoat kid |
| `mobs/chicken`, `mobs/rooster`, `mobs/chick` | Skyreach | Thunderquill, all ages |
| `items/clothhat`, `items/clothboots` | Skyreach | Thunderplume Cowl, Glimmerstride Boots |

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

---

## 2. Proposed stand-ins for the realms not yet built

**Nothing below is in the code yet.** This is the shopping list, so the player
can see in advance what the style swap will cover. Every entry was checked
against the real dump in `vanilla-sprites/` (6,121 files, game 1.3.2).

### 2.1 Garden of Eden (Tier 1)

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

### 2.2 Steinfeld (Tier 2)

| vanilla asset | stands in for |
|---|---|
| `tiles/gravel`, `tiles/rock` | Weathered Stone |
| `tiles/granite`, `granitepath` | Pale Stone ground |
| `tiles/cryptash`, `tiles/cryptpath` | Ash Grass, Grave Soil |
| `objects/gravestone1`, `gravestone2` | gravestones |
| `objects/statues` | the broken angel statues |
| `objects/burnedtree`, `burnedtreestump` | dead trees |
| `objects/largemossysteppingstone` | stone slabs |

### 2.3 Ghost Realm (Tier 3) — partly built already

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

### 2.4 Crooked Beyond (Tier 4) — partly built already

| vanilla asset | stands in for |
|---|---|
| `tiles/ascendedvoid` + `_fog` / `_grime` / `_stars` / `_swirls` | Wrong-Way Tile, Spiral Soil — vanilla's own "reality is broken" ground |
| `tiles/ascendedcorruption`, `ascendedgrowth` | Bent Grass |
| `tiles/crystaltile` | Checker Stone |
| `tiles/arcanicfloor`, `arcanicpath` | Crooked architecture floor |
| `tiles/spidercastlecarpet` | Stripe Carpet ground |
| `objects/obsidianrock` | teeth-rocks |

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
| `crystalarmadillo` (mob, by string ID) | `crookedarmadillo` — `mobs/CrookedArmadilloMob` on `mobs/crookedarmadillo.png`, same relationship to `CrystalArmadillo`. One thing did NOT come across: vanilla's second `crystalarmadillo_light` glow pass, because we have one sheet and not two — see the class comment | this pass |

The vanilla sheets stay the format reference for these three and are listed as
such in §1.5; the runtime no longer touches them.
