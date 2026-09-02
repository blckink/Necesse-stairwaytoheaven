# What actually works right now

**The one page that answers "was geht jetzt?".** Everything else is design or
history. Updated 2026-08-31.

**Verification legend** (from `IMPLEMENTATION_RULES.md` §14):
`[game]` seen in a real client · `[run]` observed in an automated run ·
`[jar]` read from decompiled source · `[build]` compiles and registers only.

> **Nothing below is `[game]`.** The last real play session was v0.5.0 on
> 2026-08-24. Everything since is `[run]` at best.

---

## 1. Biomes — 8 registered, all generate

| biome | realm | ground | status |
|---|---|---|---|
| Driftlands | Skyreach | cloudturf | `[run]` full — flora, ore, critters, livestock, crates |
| Stormveil | Skyreach | stormslate | `[run]` full |
| Aurora Shoals | Skyreach | aurorashoal | `[run]` full |
| Skyway Passages | Skyreach | skyway + cloudmarble | `[run]` full — roads, balustrades, statues |
| **Beetle Outlands** | Crooked Beyond | beetlefreak + blackpeat | `[run]` **new**, distance-gated past 900 tiles |
| Gloomfen | Ghost (in the Veil dimension) | murkmoss | `[run]` full |
| Ashen Reach | Ghost (in the Veil dimension) | ashsand | `[run]` full |
| Beetlefreak Hollows | Ghost (in the Veil dimension) | beetlefreak | `[run]` full |

**Not built at all:** Eden, Steinfeld, Infernal Fringe, Hell Antechamber, Hell.
Eden's first brick exists as of 2026-09-01: `overgrownedentile` (supplied art)
+ `overgrownedenseed`, plantable on dirt and Cloudturf, found in sky crates,
self-propagating — `[run]`, `eden check` in the integration test. The realm
band itself ships with the Eden chapter.
The `RealmDepth` field already places all six realms by distance, but only
Skyreach and Crooked Beyond have any content behind them.

**Crate loot exists only in the four Skyreach biomes.** The Veil/Ghost biomes
have no `getCrateLootTable` and no crate is placed there — the harder realms pay
out nothing. Highest-value open gap.

## 2. Quests — 5 registered

| quest | what it does | status |
|---|---|---|
| `swh_findspire` | find the Old Warden Spire | `[run]` |
| `swh_recruitwarden` | pay 30,000, Warden moves in as a settler | `[run]` |
| `swh_cats` | Siggi & Peanut, ending in the Cat Basket | `[run]` |
| `swh_anchor` | sky anchor | `[run]` |
| `swh_beacon` | **registered and never handed out** — dead | `[build]` |

**No boss quest exists.** The Séance Circles stand at fixed sites in the
Outlands and say so out loud (`misc.seancesilent`); there is nothing behind
them.

## 3. NPCs — 4 recruitable + 2 cats

| NPC | role | status |
|---|---|---|
| Sky Warden | progression NPC, recruitable settler | `[run]` |
| Magpie | courier, buys sky salvage in quantity | `[run]` |
| Halda | cellarer, sells the three stations' goods | `[run]` |
| Ossian Vane | incursion-tier salvage, stock rotates daily | `[run]` |
| Siggi & Peanut | unkillable cats, live in a placed Cat Basket | `[run]` |

**Not built:** Caretaker, Seraphine, Aurelius, Pip, Eveleen, Madame Orla,
the Ferryman, Mortimer, Caspern, Eleanor, Mr. Knott, Clerk 666-B, Brim, Moxie,
Vex. **The Warden's house-room progression does not exist** — see
`WORLD_DESIGN.md` Part A2.

## 4. Stations — 3, all settler-workable

`windsilkloom` · `aetherforge` · `stormglasskiln` — all `[run]`, all placed by
worldgen where a player can find them. Halda's Fermentation Vat is designed and
unbuilt.

## 4b. Play feel — A4.1 shipped 2026-09-01

Enemies now GUARD instead of harass, `[run]`: ~80% of sky land spawns nothing
(ticket field `SkyPressure`, wilds ~16-18%), every wreck/workshop site carries
a placed persistent guard pack (4-8 by realm, `GuardedBiome`), ambient rate
0.55x / cap 0.75x. Gated by `pressure check` + `guard check` in the
integration test.

## 5. Combat — re-tiered 2026-08-31

Every enemy and weapon now sits on the incursion ladder in `docs/BALANCE.md`.
Floor = 1000 HP / 130 damage / 40 armour (vanilla's ascended baseline).
Mod weapons are at 175–200 upgraded damage, Stormsteel at 29/1900/EPIC.
All `[run]`; the ladder itself is `[jar]`.

**Known inconsistency:** the four arsenal enemies scale with world difficulty
(`MaxHealthGetter`); the six core hostiles are flat. Identical on Classic,
divergent on Casual/Brutal. Undecided.

## 6. Structures

| structure | where | status |
|---|---|---|
| Old Warden Spire | Skyreach origin | `[run]` furnished 21x21 hall |
| Crooked House | Outlands + Beetlefreak Hollows | `[run]` |
| Surface POIs | Surface | `[run]` craters, camps, shrines |
| Settlement workshop | Skyreach | `[run]` holds the three residents |

**No level is "fully populated" in the sense of `WORLD_DESIGN.md` §35.** The
four Skyreach biomes come closest — they have terrain, flora, ore, fauna,
enemies, crates and one structure each. Every other realm fails §35 on several
counts.

## 7. Travel

**Nothing.** No fast travel, no waypoints, no anchors. `WORLD_DESIGN.md` A2.3
designs it (the Warden's house as the hub) and none of it is built. This is the
blocker for building any realm past Ghost.

---

## Sprites, for rework

- **`docs/SPRITE_INVENTORY.md`** — all **334** PNGs this mod ships (regenerated
  2026-09-02; was 323 — the Mistserpent resheet, the livestock art pass and the
  Eden tiles landed since), by folder, with dimensions. This is the rework
  list.
- **`docs/VANILLA_ASSET_MAP.md`** — every vanilla asset borrowed, per realm:
  **0** mobs by string ID (the Outlands' last three were replaced on
  2026-09-01), 6 vanilla mobs subclassed for behaviour (down from 8: the
  Thunderquill Fowl was removed and the Fen Wraith's sheet swap moved it to
  "replaced"; of the 6 left, only Cinder Cantor, Rime Sentry and the Watch Mote
  still wear the vanilla sheet too — the Aurora Flake, Glimmergoat and Nimbus
  Yak kept the borrowed behaviour but swapped to their own sheets on
  2026-09-02), 1 texture recoloured at load (down from 12 — the livestock pass
  moved the Yak and the Glimmergoat off `SkyPelt` recolours onto drawn
  sheets), 2 items dropped, 7 sheets used as a format template (the
  Mistserpent's crystal-dragon-format sheet joined this count 2026-09-02).
  Replacing these means drawing something new, not reworking a file.

**Before touching any PNG:** they are all written by `tools/asset_generator/`.
A hand-edited PNG is reverted on the next run. Supplied art goes in
`src/main/resources/kk-sprites/` and gets added to `generate_assets.py`'s
`CONVERTED` guard.
