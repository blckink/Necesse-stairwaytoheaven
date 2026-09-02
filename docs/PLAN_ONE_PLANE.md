# Correction: ONE plane, not six dimensions

**Written 2026-09-02, by the player's ruling.** This document is the spec for
undoing an architectural mistake. It outranks every other doc except
`docs/WORLD_DESIGN.md`, which it exists to obey.

---

## What went wrong

`docs/WORLD_DESIGN.md` §3 is explicit and was never ambiguous:

> Every world has a fixed **`SkyOrigin`**. For each generated island the
> generator computes **`distanceFromSkyOrigin`**, from which comes
> `realmDepth = normalizedDistance`. **Distance does not set a hard biome
> zone. It sets biome weights** … Every island additionally carries a
> **`distortion`** value … *This is what dissolves the hard optical borders.*

And §41.5, on the Veil:

> They move from the `veil2` dimension **into the one world** at the Ghost
> Realm's realmDepth band.

The mod today has **six separate dimensions**: `skyreach2` (+1), `eden2` (+2),
`steinfeld2` (+3), `ghost2` (+4), `crooked2` (+5) and `veil2` (−3). Each is an
unconnected level with its own generator. That is the exact opposite of the
concept: hard borders, no overlap, no distortion gradient, no anti-rush gate,
and a Veil that was supposed to stop existing as a world.

**The spine was already correct and was ignored.** `worldgen/RealmDepth.java`
exists, is well built, holds `DEPTH_SCALE = 6000` as the single world-size
dial, and already maps distance onto the concept's bands as pure functions.
The realms were built beside it instead of on it.

`biomes/OutlandsBiome` + `worldgen/SkyOutlands` are the one piece that got it
right — a realm expressed as a distance-gated biome on the sky plane. §41.4
says so outright. **That is the pattern every realm must follow.**

---

## The target

**One level: `skylevel` / `skyreach2`.** Everything the player walks to is on
it. Realms are BIOME WEIGHT BANDS over `RealmDepth.depthAt`, overlapping as
§3's table sets out:

| realm depth | main biomes | tiles from SkyOrigin (at DEPTH_SCALE 6000) |
|---|---|---|
| 0.00–0.15 | Skyreach | 0 – 900 |
| 0.10–0.30 | Skyreach + Eden | 600 – 1800 |
| 0.20–0.42 | Eden | 1200 – 2520 |
| 0.32–0.48 | Eden + Steinfeld | 1920 – 2880 |
| 0.42–0.58 | Steinfeld | 2520 – 3480 |
| 0.48–0.70 | Steinfeld + Ghost | 2880 – 4200 |
| 0.60–0.80 | Ghost Realm | 3600 – 4800 |
| 0.70–0.88 | Ghost + Crooked | 4200 – 5280 |
| 0.80–0.94 | Crooked + Infernal Fringe | 4800 – 5640 |
| 0.90–1.00 | Infernal / Hell | 5400 – 6000+ |

The Skyreach's four existing sub-biomes (Driftlands, Stormveil, Skyway, Aurora
Shoals) stay, as **distortion variants inside Tier 0** — §41.3 says do not
delete them.

**No dimension is added by a realm.** The only levels that remain are the sky
plane, the vanilla surface, and vanilla's own caves.

---

## The anti-rush gate

A player must not be able to sprint from the spire to Hell. The gate is the
one the concept already specifies for the Veil (§8) — **Soul Exposure**,
generalised to the plane and already built in `stairwaytoheaven/veil/`:

- The debuff applies when the player is at a realm depth **above what they have
  unlocked**, and stacks the longer they stay: vision, then slow, then health
  drain, then heavy damage.
- §8's abuse case is binding: **do not merely block tiles.** The check is
  against the world REGION, so teleporting past the edge does not help.
- A short step over the line is possible and is meant to be — that is how the
  player learns the next realm exists. Running through is not.

Unlocking a band is a story beat, not a timer. Reuse the existing world-scoped
store (`quest/SkywatchWorldData`) for which bands are open.

`stairwaytoheaven/veil/VeilRegion` + `VeilGate` + `SoulExposureBuff` are the
machinery. They keep their names; only their meaning widens from "the Veil" to
"any band you have not earned".

---

## Travel: the Warden's house (§A2.3)

Not a generic teleporter net. Each anchor is a themed room in the Warden's
house, and **a route only becomes fast travel after the player has physically
made it once**.

| stage | what opens |
|---|---|
| early | ordinary travel between Skyreach, Eden and Steinfeld |
| after the séance | house ↔ **Ghost** anchor — a séance mirror or spirit door |
| after Crooked | a second anchor — Mr. Knott's absurd red door (§15) |
| after the Hell unlock | the **Infernal** anchor — a demonic lift |

**The séance is fast travel to the Ghost BAND of the one plane.** It is not a
door to another world, because there is no other world.

---

## What is kept, and what changes

**Kept, all of it.** Every tile, object, mob, item, POI preset, settler and
quest built for Eden, Steinfeld, Ghost and Crooked is good content and stays.
This is a change of HOSTING, not of content. Nothing under
`realms/*/` gets deleted for being in the wrong dimension.

**Changed:**

1. `StairwayToHeavenMod.registerDimension` registers **one** modded level.
   `edenlevel`, `steinfeldlevel`, `ghostlevel`, `crookedlevel` and `veillevel`
   stop being registered as dimensions.
2. Each realm's `*TerrainPainter` becomes a **band painter** the sky painter
   calls when `RealmDepth` puts a region in that realm's band — the way
   `SkyTerrainPainter` already calls the Outlands path.
3. Each realm's biomes register as ordinary sky sub-biomes (they already do —
   `countInStats=false`), and their spawn tables come into play through the
   band rather than through a level.
4. `*Level` classes are deleted or reduced to nothing, whichever is cleaner.
   `VeilLevel` goes with them; the Veil's biomes (`gloomfen`, `ashenreach`,
   `beetlefreakhollow`) move to the Ghost and Crooked bands per §41.5.
5. Settler placement moves from each realm's level to the sky level, gated on
   the region's realm rather than on which level it is.
6. The realm gate objects that were built as level portals (`EdenGateObject`,
   `CrookedDoorObject`, the séance circle) become either **doors between bands
   on the plane** or **house anchors** per §A2.3 — not level teleports.

## Save compatibility

There is none to preserve and none is owed: the dimensions being retired were
added today and have never been in a released build. Do not write migration
code for them.

---

## Then, and only then: the content pass

Once the plane is one plane again, the realms get checked for what the concept
asks for and the build forgot. Known gaps, from `docs/WORLD_DESIGN.md`:

- **Eden (§5)** — the farmables (Paradise Wheat, Golden Carrot, Eden Berry,
  Moon Melon, Sun Grape, Paradise Pepper), the food chain, the **Eden Press**
  station, Eden Copper → **Eden Bronze**, and the **Knowledge Tree** as a rare
  worldgen object. Much of this is named in the design and absent in the code.
- **Steinfeld (§7)** — the ghost world event: unattackable apparitions that
  walk to a grave, a door, or the map edge.
- **Ghost (§10)** — the **Soul Loom** and **Spirit Forge** as settler-operable
  stations, the way `settlement/SkyProfessions` builds the existing three.
- **Infernal Fringe (§17–18) and Hell (§19–23)** — not built at all.
- **Buildings are empty.** Every POI preset stamps walls, a floor and a chest.
  The concept wants inhabited rooms: tables, chairs, beds, lights, shelves,
  clutter. `SkyFurnitureSet` already ships 17 pieces and almost none are used
  by any preset. This is the cheapest large win in the whole mod.

---

## The rules, unchanged

1. **No new pixel art.** Existing mod sprite, else vanilla by literal path,
   else leave it out and record it. Never `tools/asset_generator/`.
2. **Never recolour at load time.**
3. **No farm animals.** Chickens were cut deliberately.
4. **Smaller and building beats bigger and broken.**
5. Every balance number names its vanilla analogue in a comment.
6. Gates, all of them, before any commit:
   ```
   export NECESSE_GAME_DIR=/opt/necesse-server/necesse-server-1-3-2-24650233
   ./gradlew buildModJar
   python3 tools/locale_audit.py --vanilla vanilla-sprites
   python3 tools/content_ledger.py --check
   python3 tools/tile_behaviour_audit.py --vanilla vanilla-sprites
   python3 tools/asset_generator/generate_assets.py
   ```
   The `--vanilla vanilla-sprites` flag is mandatory on both audits that take
   it; without it they cannot see the game's own resources and report every
   borrowed texture as missing. Never pipe a gate into `head`/`tail`.
