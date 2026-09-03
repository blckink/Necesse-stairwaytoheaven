# What actually works right now

**The one page that answers "was geht jetzt?".** Everything else is design or
history. Updated 2026-09-02.

**Verification legend** (from `IMPLEMENTATION_RULES.md` §14):
`[game]` seen in a real client · `[run]` observed in an automated run ·
`[jar]` read from decompiled source · `[build]` compiles and registers only.

> **Nothing below is `[game]`.** The last real play session was v0.5.0 on
> 2026-08-24. Everything since is `[run]` at best.

---

## 1. Biomes — 17 registered

| biome | realm | ground | status |
|---|---|---|---|
| Driftlands | Skyreach | cloudturf | `[run]` full — flora, ore, critters, livestock, crates |
| Stormveil | Skyreach | stormslate | `[run]` full |
| Aurora Shoals | Skyreach | aurorashoal | `[run]` full |
| Skyway Passages | Skyreach | skyway + cloudmarble | `[run]` full — roads, balustrades, statues |
| **Beetle Outlands** | Crooked Beyond band (depth 0.70+, 4200 tiles) | beetlefreak + blackpeat | `[build]` moved out of the Skyreach onto its true band |
| Gloomfen | Ghost band (WORLD_DESIGN §41.5) | murkmoss | `[build]` the fen inside the Ghost Realm |
| Ashen Reach | Ghost band (§41.5) | ashsand | `[build]` |
| Beetlefreak Hollows | Crooked band (§41.5) | beetlefreak | `[build]` |
| Eden Garden | Eden band (depth 0.20-0.42, 1200-2520 tiles) | Eden grass + moss | `[build]` band painter; vegetation aliases use native vanilla objects |
| Eden Canopy | Eden band | root floor + rich soil | `[build]` Knowledge Tree and copper stand-ins |
| Eden Shallows | Eden band | Paradise Sand + shallow lagoon | `[build]` |
| Quiet Meadow | Steinfeld band (depth 0.42-0.58, 2520-3480) | Eden grass + weathered stone | `[build]` |
| Slab Fields | Steinfeld band | pale grass + cracked marble | `[build]` |
| Grave Heath | Steinfeld band | ash grass + grave soil | `[build]` |
| Striped Waste | Crooked band (depth 0.70-0.88, 4200-5280) | crooked stripe | `[build]` |
| Spiral Fields | Crooked band | spiral soil + violet mud | `[build]` |
| Checkerworks | Crooked band | checker stone | `[build]` |
| Aftergarden | Ghost band (depth 0.60-0.80, 3600-4800) | haunted grass + black cobble | `[build]` |
| Bone Orchard | Ghost band | violet dirt + spirit stone | `[build]` |
| Ectomarsh | Ghost band | ghost moss + ectoplasm | `[build]` |

**ONE PLANE, as of 2026-09-02.** `docs/PLAN_ONE_PLANE.md` collapsed the six
modded dimensions into one. `edenlevel`, `steinfeldlevel`, `ghostlevel`,
`crookedlevel` and `veillevel` are gone; the only modded level is
`skylevel` / `skyreach2`, and every realm is a BAND of it chosen by
`worldgen.RealmDepth` from distance to the Old Warden Spire, exactly as
`WORLD_DESIGN` §3 specifies. Every realm's biome status above dropped from
`[run]` to `[build]` for that reason: the content is unchanged, but the run
that observed it observed it on a level that no longer exists.

Measured offline over three seeds (`describeTile` + `RealmDepth`, ring samples):
depth 0 is 100% Skyreach; 900 tiles is 72/28 Skyreach/Eden; 1500 is 96% Eden;
3000 is 100% Steinfeld; 3800 is 23/77 Steinfeld/Ghost; 4400 is 95% Ghost;
5000 is 88% Crooked; 5600+ is the Hell band, painted as the far end of Crooked
until §17-23 exists. Land share runs 55% in the Skyreach, 64-78% in Eden,
80% in Steinfeld and ~60% out past the Ghost Realm.

**Not built at all:** Infernal Fringe, Hell Antechamber, Hell. Eden's
player-facing farming/livestock, Press, fishing, quest chapter and Keeper boss
remain unbuilt; see `docs/realms/eden.md`.

The four Skyreach biomes and all three Eden biomes define crate loot. Eden's
painter places the native `skycache` alias; Veil/Ghost still have no
`getCrateLootTable` and place no crates, so those harder realms pay out nothing.

## 2. Quests — 5 registered

| quest | what it does | status |
|---|---|---|
| `swh_findspire` | find the Old Warden Spire | `[run]` |
| `swh_recruitwarden` | pay 30,000, Warden moves in as a settler | `[run]` |
| `swh_cats` | Siggi & Peanut, ending in the Cat Basket | `[run]` |
| `swh_anchor` | sky anchor | `[run]` |
| `swh_beacon` | **registered and never handed out** — dead | `[build]` |

**No boss quest exists.** A Séance Circle standing on Outland ground (now
inside the Crooked band) still becomes the Crooked door; circles elsewhere on
the plane retain the silent-site response, except where the tile is already in
the Ghost band or deeper, which answers "you are already there".

## 3. NPCs — 8 recruitable/resolvable + 2 cats

| NPC | role | status |
|---|---|---|
| Sky Warden | progression NPC, recruitable settler | `[run]` |
| Magpie | courier, buys sky salvage in quantity | `[run]` |
| Halda | cellarer, sells the three stations' goods | `[run]` |
| Ossian Vane | incursion-tier salvage, stock rotates daily | `[run]` |
| Siggi & Peanut | unkillable cats, live in a placed Cat Basket | `[run]` |
| Eveleen | Eden botanist; settlement arrival keyed to Eden grass | `[run]` registration/arrival probe |
| Mortimer | undertaker; arrival keyed to a graveyard | `[run]` registration/arrival probe |
| Caspern | spirit smith; arrival keyed to an Aether Forge | `[run]` registration/arrival probe |
| Eleanor | lost soul with stay/pass-on resolution | `[run]` registration/arrival probe; ending not played |

**Not built:** Caretaker, Seraphine, Aurelius, Pip, Madame Orla,
the Ferryman, Mr. Knott, Clerk 666-B, Brim, Moxie,
Vex. **The Warden's house-room progression does not exist** — see
`WORLD_DESIGN.md` Part A2.

## 4. Stations — 5, all settler-workable by archetype

`windsilkloom` · `aetherforge` · `stormglasskiln` — all `[run]`, all placed by
worldgen where a player can find them. Halda's Fermentation Vat is designed and
unbuilt.

`soulloom` and `spiritforge` are registered crafting-station workstations and
compile with their Ghost recipes — `[build]`; the existing integration command
does not yet probe those two IDs at runtime.

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

No fast-travel hub, waypoints or player anchors exist. Paired realm-entry and
return objects now exist for Crooked and Ghost (`[build]`), while the Warden's
house travel network from `WORLD_DESIGN.md` A2.3 remains unbuilt.

---

## Sprites, for rework

- **`docs/SPRITE_INVENTORY.md`** — all **341** PNGs this mod ships (regenerated
  2026-09-02 after the supplied Mob and Tile upload), by folder, with
  dimensions. This is the rework list.
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
