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

## 4. The Veil

Craft a **Séance Circle** (`6 stormshard + 4 windsilk + 2 aurorapetal`, Tungsten
Workstation) and use it. `SeanceCircleObject.interact` checks the player is
**holding the Silver Bell** — it is the key, never the fuel, and is not
consumed. The circle then becomes a rift down into the Veil.

So the Veil is gated behind finishing the Warden's recruitment, because that is
the only place the bell comes from.

## What is fully integrated

At the time of writing: 59 objects, 23 items, 14 tiles and 18 mobs registered;
154 IDs and 47 literal message keys named in **both** `en.lang` and `de.lang`,
locales in sync; 113 holdable IDs with a real icon file. All five audit gates
(`furniture`, `locale`, `sheet_format`, `tile_behaviour`, `size`) pass together,
the dedicated server boots and generates, and the painter oracle reports zero
tile mismatches.

## What is NOT integrated yet

Everything below is registered, craftable, named and iconned — and **never
appears in a generated world**, because no worldgen rule places it:

- **Skyway Passages** — the biome itself does not exist. `skywaytile` is a
  registered terrain tile with its splat, but there is no biome class, no
  `SkyTerrainPainter` rule and no spawn table, so the passages are not
  generated anywhere.
- **Sky Seraph tree** — tree, sapling, Seraphwood and leaf particles all work,
  but nothing plants one. Only a player-placed sapling grows into it.
- **Seraph statue** and the **Cloudmarble** wall/door/window/railing/gate —
  craftable only; no structure or preset uses them.
- **Skywatch furniture** — the same. The Warden's Spire preset still furnishes
  itself from the old decoration objects, so the inhabited-POI request is not
  done.

Adding the biome means changing `SkyTerrainPainter`, the integration test's
painter oracle and `scripts/sky_map_render.py` together — all three mirror the
same classification, and they have to stay in step.
