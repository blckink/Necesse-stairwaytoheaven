# The player's journey, A to Z

Read out of the code, not from memory. Every claim below names the class that
implements it, so this file can be checked rather than trusted.

## 0. Getting the stairway

There is exactly one entry point: the **Sky Stairway** object,
`skystairwaydown`, crafted at the **Tungsten Workstation** for
`8 tungstenbar + 15 quartz` (`SkyItems.registerRecipes`). The cost deliberately
mirrors the Deep Cave Ladder's tungsten investment, so the sky unlocks
alongside the deep caves and never before them.

> **Gap:** nothing points the player at this recipe. It simply appears in the
> Tungsten Workstation list once they can craft there. There is no hint item,
> no NPC line and no journal entry before the first ascent.

## 1. The ascent

Placing and using the stairway runs `SkywardStairwayObjectEntity`:

1. `SkyLevel.ensureWardenSpire()` stamps the Warden's Spire at the canonical
   Skyreach origin, **idempotently** — the hub exists before the player takes a
   step, and it is one fixed place, not one per stairway.
2. `clearArrivalLanding` clears the 3×3 arrival pad and turns any Mistsea under
   it into Cloudturf, so nobody lands in the cloud sea.
3. The stairway is bound to the player's authentication as their way home
   (`quest.setReturnStairway`). The **Skywatch Gate** at the spire reads that
   binding back; without it the gate answers `gatenobinding`.
4. On the **first** ascent in a world (`quest.stage == 0`):
   - a chat line, `skyreachhint`, naming a compass direction toward the spire —
     *"Etwas flackert über dem Nebel im &lt;dir&gt;…"*
   - the journal quest **FindSpireQuest**
5. `SkyMapMarkers.onAscent` puts two permanent markers on the world map: the
   spire and the player's own stairway.

## 2. The Warden

The chain is a single pure function, `SkyWardenMob.chapterFor(quest, isSettler)`,
so what the `/skyreachstatus` probe reports and what the player is given cannot
drift apart. Its chapters:

| chapter | condition | what happens |
|---|---|---|
| `RECRUIT` | not recruited | FindSpireQuest cleared, **RecruitWardenQuest** given |
| `CATS` | recruited, cats not home | **SpireCatsQuest** given + both lair markers |
| `CATS_TURNIN` | both cats home, unpaid | quest completed, **catbasket + 2× flickerlightgarland** |
| `ANCHOR` | paid, not anchored | **AnchorDeliveryQuest**; on delivery **skywatchbanner + 5× aurorapetal** |
| `DONE` | anchored | chain finished |

**Recruitment is real settlement recruitment**, not a spawn item: the Warden is
a `HumanMob` with a settler key, and the money moves only when the player
presses the recruit button in the shop container. Talking to him costs nothing.

`onRecruited` is where the payoff lands: the beacon lights, `stage` becomes 2,
SpireCatsQuest is handed out with the cat lairs on the map, and the **Silver
Bell** changes hands. The bell has no other source.

## 3. The cats

Siggi lairs in Stormveil, Peanut in the Aurora Shoals. Both are coaxed home
with a `cloudpufftreat`. Their home is the real `catbasket` object at the
spire. Neither cat can be permanently killed.

## 3b. The Skyway Passages

The fourth Skyreach sub-biome, and the only one that was **built** rather than
grown. `SkyTerrainPainter.biomeClassOf` cuts it out of the biome field's
`0.40 .. 0.47` band — directly above Stormveil, so the two cold grounds border
each other and the Sky Seraph's frost form (`SkyTreeObject`, keyed on
`stormslatetile` and `skywaytile`) reads as one region rather than two
coincidences. Measured over eight seeds it holds **14.6% of the sky's land**,
next to Stormveil's 18.6% and Aurora's 13.1%.

What the player finds there:

- **Skyway paving** (`skywaytile`) as the ground, with the same skystone
  barrens surfacing through it that every other sky ground has.
- **Sky Seraphs** growing wild, in their frost form, at 1 per 85 Skyway land
  tiles — between the Fulgurpine (72) and the Prismabirch (116).
- **Cloudmarble balustrades** running the length of any road whose two ends
  both stand in the passages (`SkyLandscape.PROP_RAIL`), with a **Cloudmarble
  fence gate** wherever a carriageway breaks one, and **Cloudmarble piers**
  where a passage crosses a gate.
- **Seraph statues** at the junctions — the centre of a Skyway garden court,
  where four spokes meet — and at roughly every fifth roadside waypoint along
  a causeway, which is one statue per ~70 tiles walked.
- Its own spawn table (`SkywayBiome`): Skystone Golems as the masonry's
  guardians, Galehounds hunting the corridors, Zephyr Rays straying in.

Density, measured over eight seeds and 2,197,075 natural land tiles: **0.371
objects per tile**, against Driftlands 0.358, AuroraShoals 0.322 and Stormveil
0.307. A paved biome has no obvious wild growth to fill it, and the first cut
measured 0.297 — tying the emptiest ground in the world — before the scatter
band was widened.

## 4. The Veil

Craft a **Séance Circle** (`6 stormshard + 4 windsilk + 2 aurorapetal`, Tungsten
Workstation) and use it. `SeanceCircleObject.interact` checks the player is
**holding the Silver Bell** — it is the key, never the fuel, and is not
consumed. The circle then becomes a rift down into the Veil.

So the Veil is gated behind finishing the Warden's recruitment, because that is
the only place the bell comes from.

## What is fully integrated

Counts from the registries: **61 objects, 24 items, 15 tiles, 18 mobs, 8 biomes,
5 journal quests**, 163 IDs named in **both** locales with the two in sync, and
120 holdable IDs with a real icon file.

All five gates pass together — `furniture`, `locale`, `sheet_format`,
`tile_behaviour`, `size` — and the dedicated server boots, generates and
survives a restart.

**Proven in the running game**, not read off the source. These lines come out of
the integration test's probes against a live world:

```
Skyreach OK: class=SkyLevel identifier=skyreach2 dimension=1 isCave=false
painter oracle: tileMismatches=0 (scan radius 64, spire footprint excluded)
skyway: ground=skywaytile tiles=2520 seraphtrees=18 cloudtrees=17 rails=70
entrance check: door=cloudmarbledoor isDoor=true approach=[air air air air] clear=true
recruit check: skywarden settler=WardenSettler price=coinx30000
npc check: wardens=1 cats=2 cloudlambs=48
cat home check: ... STILL_WILD -> ... AT_BASKET   (both cats, across a restart)
husbandry check: cloudlamb shear=windsilkx1 child=cloudlamb
                 feed: cloudberry=hand:true/trough:true wheat=hand:true/trough:true
```

### The four Skyreach biomes generate

Driftlands (53.7% of land), Stormveil (18.6%), **Skyway Passages (14.6%)**,
Aurora Shoals (13.1%), plus the skystone barrens that cut across all of them.
Object density measured over 8 seeds and 2.2M natural land tiles: 0.358 / 0.307
/ 0.371 / 0.322 — no biome is meaningfully emptier than another any more.

### Worldgen actually places

Terrain and flora for every biome; the Skyway's paving, cloudmarble railings
with real gates where a road crosses, Seraph statues at junctions, and both
tree species alternating along planted avenues; rock and ore formations;
Skystone Lichen, Cragbloom and Sky Scree on the bare plate; the Warden's Spire
at one canonical origin; cloud lamb flocks.

### The Spire is furnished

21×21 on the supplied plan: a double wall ring with a circulation corridor,
eight doors on the axes, twelve candelabra on a regular rhythm, four furnished
corner rooms (refectory, council table, the Warden's quarters, archive), and the
beacon chamber left open. Machine-checked on the stamped world: 11 chairs, every
one facing a table or desk; no unreachable interior tile; every table decoration
on a real modular table.

## Craftable but never generated — by design

Saplings, the Séance Circle, the stairway pair, the Veil rifts and the
decorative props (gloomwillow, withershrub, stormscreed, sky balloon, aeronaut
wreck, sky parcel, ghost lantern) are player-built or quest-given on purpose.

## Not placed anywhere yet

- **Beetlefreak ground** (`beetlefreaktile`) and the **Beetlefreak wall set** —
  registered, craftable and passing every gate, but no Veil worldgen rule puts
  them anywhere. They need a home in `VeilTerrainPainter`.
- The four furniture pieces the Spire's reference plan wants and we do not have:
  `skywatchbookshelf`, `skywatchcabinet`, `skywatchclock`, `skywatchdisplay`.
  Dresser and desk stand in for them today.
