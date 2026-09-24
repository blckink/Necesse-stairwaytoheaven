# Chapter 02 — Hoards and mimics

> "interessante neue Orte und Aufbauten / Mimic-Truhen die geheimen Loot haben
> aber von Bossen beschützt werden usw. Bitte interessante und abwechslungsreiche
> Level Designs."
> — the player, and the whole brief for this document.

Design intent **and** the record of what was built from it on 2026-09-24: the
six maps below are transcribed character for character into
`RealmPoiPresets` (`TREASURY_PLAN` … `DOORS_PLAN`), and
`tools/plan_transcription_audit.py` fails if the two drift apart. The prose
around the maps is the design; §7 says what shipped and how it was proven.

`docs/AREA_OVERVIEW.md` is why these six and not others: on 2026-09-24 the
Skyreach had eighteen places and every realm after it one or two
(`python3 tools/area_census.py`: Eden 2, Steinfeld 1, Ghost 1, Crooked 1). The
chapter goes to the thin realms, with two Skyreach places first because the
Skyreach is where a player meets a mimic before they know what one is. Hell
already had four presets and got none.

Every place answers chapter 01's three questions — **what does the player do
here, what do they leave with, and who is standing in it** — and every one of
them is built around the same object: **a treasure somebody is guarding, and
boxes around it that may or may not be boxes.** That object is the whole
chapter; the six layouts are six different answers to "how do you make the
player earn it".

---

## Contents

| # | Name | Realm | Footprint | The layout idea | Guardian (mini-boss) | Mimics |
|---|---|---|---|---|---|---|
| 1 | **The Counterfeit Treasury** | Skyreach | 27×19 | an axial hall of eight chests down a gold runner, a vault behind it | Skystone Golem, lifted | 5 of 8 chests |
| 2 | **The Fallen Observatory** | Skyreach | 25×23 | a round dome broken open, and a sealed cube with **no door** | Dawnpiercer, lifted | the astronomer's chest |
| 3 | **The Hedge Labyrinth** | Eden | 23×23 | a 10×10-cell hedge maze; six dead ends, a clearing at its heart | Forbidden Serpent, lifted | 3 of 6 dead ends |
| 4 | **The Pilgrims' Ossuary** | Steinfeld | 21×29 | a trapped processional: pressure plates wired to arrow traps | Hollow Angel, lifted | 3 of 6 niches |
| 5 | **The Wedding Feast** | Ghost Realm | 25×21 | a banquet laid for twenty-four; thirteen of the chairs are guests | Mourning Bride, lifted | 13 chairs + 2 of 4 gifts |
| 6 | **The Hall of Many Doors** | Crooked Beyond | 25×19 | one wall, five ways through it; three are Door Mimics | Rare Crooked Golem, lifted | 3 of 5 doorways |

---

## 0. The rules this chapter adds to chapter 01's

Chapter 01's §0 (legend, rotation, windows, fences, lighting, rarity) applies
unchanged — every plan below is read by the same `RealmPoiPresets.plan`
interpreter and throws at startup on any breach of §0.2–§0.4. What this
chapter adds is five shared letters and four mechanics.

### 0.1 The shared letters

```
  T  the prize: a vanilla chest, filled from RealmPoiHoards.prize at generation
  C  an honest storage box, filled from RealmPoiHoards.bait
  M  a box that is not one: floor in the preset, a MIMIC MOB placed on it
  W  the guardian: a realm elite lifted to mini-boss (floor in the preset)
  S  a vanilla sign, whose text is the place's one line of story
```

Mobs are not objects. `M`, `W` and each place's guard letters write floor, and
`RealmPoiHoards.placeInhabitants` reads the same plan rows back to put a mob on
exactly those tiles — the map is the single source for both.

### 0.2 The mimic, and why it is vanilla's own

**VERIFIED [jar]** in the decompiled `RandomCaveChestRoom.openingApply`
(lines 109–120): vanilla's deep-cave chest rooms delete the `storagebox` they
would have placed and spawn a `"mimic"` mob on that tile, `canDespawn = false`,
turned with `setDir` to the chest's rotation, with the room's loot table
rolled into `MimicMob.loot`. `MimicMob.getLootTable` (196–219) drops that list
plus a `mimicchest` when it dies, and `addSaveData`/`applyLoadData` (80–110)
save the list with the mob. The mimic sits `isDisguised` — drawn as a chest,
perfectly still — until a player or human is within 96 px (three tiles,
`MimicAI`'s target filter), then unfolds and chases.

So a mimic here is exactly that, with one change: the mob is
**`hoardmimic`** (`mobs/HoardMimicMob`), vanilla's `MimicMob` on the Skyreach
ELITE row (`BALANCE.md` §5/§6: 1820 HP, 50 armour, damage dice scaled to the
row's 149.5 average) instead of vanilla's 600-HP deep-cave numbers. Deeper
places lift it onto their own row with `BossScaling.applyTier` (Eden tier 3,
Steinfeld 5, Ghost 7 — the floor-relative multipliers of `BALANCE.md` §5). The
Crooked Beyond uses its own `doormimic`, and the Ghost Realm adds its own
`possessedchair` — both were already `MimicMob` subclasses.

**Why not a mod "mimic chest" object that spawns a boss when opened:** it would
be a new object class re-implementing, badly, a mechanic the engine already
ships complete — disguise, wake radius, network sync, save format, loot. The
honest box beside every mimic is vanilla's `storagebox`, the same object vanilla
itself swaps for a mimic, so the two cannot be told apart by look. They cannot
be told apart by facing either: a chest and a mimic are both turned toward the
open side of their tile nearest the middle of the place
(`RealmPoiPresets.faceInward`), one rule for both.

### 0.3 The guardian

Every prize has a guardian standing between it and the door: that realm's own
elite (never a mob from another realm, never a ladder boss) with
`BossScaling.applyTier(mob, 5)` — the permanent, synced, saved `incursionpressure`
buff the boss portals already use, at vanilla's tier-5 curve: **×2.12 health,
×1.52 damage**. A mini-boss, not a boss: the ladder bosses start at ×3.18.

### 0.4 Once, and never again

Every mob here is placed by the preset's place function, which the engine runs
once, when that rectangle generates — the path the Dew-Keeper's snails and the
Redoubt's garrison already take — with `canDespawn = false`, and none of the
seven mob types involved is added to any spawn table by this chapter. A cleared
place stays cleared. The chests are filled at generation by
`Preset.addInventory`; nothing refills them.

### 0.5 Traps, and what vanilla has for puzzles

**VERIFIED [jar]:** vanilla has pressure plates (`stonepressureplate` and nine
siblings), wall traps that fire on a wire's rising edge (`stonearrowtrap`:
`WallArrowTrapObject`, 40 fixed damage, 1 s cooldown, fires the way it is
turned), levers (`rocklever`) and doors that open on a live wire
(`DoorObject.onWireUpdate`). `RandomCaveChestRoom.placeTrap` (150–170) is the
recipe: plate and trap on one straight line, wire 0 on every tile between.
§4 uses exactly that. A lever **cannot** lock anything — a player opens any
door by hand — so there is no lever puzzle in this chapter; the secrets are
spatial instead (§2's doorless cellar, §3's maze, §6's wrong doorways).

---

## 1. The Counterfeit Treasury

**`counterfeittreasury` · 27×19 · per region (the Skyreach hoard lattice, §7) · any Skyreach biome.**

The Sky Mint's public treasury: the room every sky citizen was allowed into.
It is a single axial composition — in through the west door, down a three-wide
gold runner, and at the far end a vault door. Eight chests stand in two rows of
four either side of the runner, and **five of them are mimics.** Behind the
vault door a lifted Skystone Golem stands in front of the only chest in the
building the Mint ever meant to keep.

```
       0         1         2      
       012345678901234567890123456
  y0   ...........................
  y1   ...........................
  y2   ..####O###O###O####........
  y3   ..#c,,,,n,,,n,,,,c#......L.
  y4   ..#,,,,,,,,,,,,,,,###O###..
  y5   ..#,,M,,C,,M,,M,,,#q++++#..
  y6   ..#,,,,,,,,,,,,,a,#++d++#..
  y7   .S#,,,,,,,,,,,,,,,#+++++O..
  y8   ..#,rrrrrrrrrrrrr,#+++++#..
  y9   ..D,rrrrrrrrrrrrr,D+W++T#..
  y10  ..#,rrrrrrrrrrrrr,#+++++#..
  y11  .L#,,,,,,,,,,,,,,,#+++++O..
  y12  ..#,,,,,,,,,,,,,a,#++d++#..
  y13  ..#,,C,,M,,M,,C,,,#q++++#..
  y14  ..#,,,,,,,,,,,,,,,###O###..
  y15  ..#c,,,,q,,,q,,,,c#......L.
  y16  ..####O###O###O####........
  y17  ...........................
  y18  ...........................
```

```
  #  cloudmarblewall   O  cloudmarblewindow   D  cloudmarbledoor
  ,  skywaytile        +  marblecheckertile (the vault, accent scale)
  r  skywatchcarpet (TILE_LAYER, 13x3 runner)
  c  skywatchcandelabra      n  skywatchbanner (WALL_DECOR, wall above)
  q  mistglasslantern (WALL_DECOR; wall below, (19,5) wall above)
  d  skywatchdisplay (empty on purpose)   L  wardencandelabra (outside)
  C  storagebox (honest)  M  hoardmimic   T  birchchest (the prize)
  a  rimesentry ×2        W  skystonegolem, lifted to tier 5
  S  sign
```

**The hall.** x3..x17 × y3..y15, 195 tiles. Chests at (5,5) (8,5) (11,5)
(14,5) and (5,13) (8,13) (11,13) (14,13); the row reads `M C M M` over
`C M M C`. Each is turned toward the runner. The two Rime Sentries at (16,6)
and (16,12) flank the vault door — the only enemies a player sees on the way
in, which is what makes the chests look safe.

**The vault.** x19..x23 × y5..y13 on chequer, one door at (18,9). The golem
stands at (20,9), directly inside it; the prize is at (23,9) against the east
wall. Two display stands at (21,6) and (21,12) stand empty: somebody has
already taken what was on show. What was not on show is in the chest.

**Lighting:** 4 candelabra + 4 wall lanterns over 240 interior tiles = 1 per
30, inside §0.5's 25–40 band; 3 lamps outside.

**The sign:** *"SKY MINT — PUBLIC TREASURY. Open to all citizens. Please take
only what is yours. (All chests are inspected daily. Some of them inspect
back.)"*

**What the player leaves with.** Prize: 4–8 Stormsteel Bar, 3–6 Aetherium Bar,
3–7 Stormglass, 900–2200 coin, 30% one of Storm Disc / Skyreave / Thunderhead,
15% Skystone Heart. Honest boxes: 60–200 coin, 50% 1–3 Stormglass, 30% 2–5
Skystone. Each mimic: 150–450 coin, 40% 1–2 Aetherium Bar, 8% Silver Bell, plus
vanilla's `mimicchest`.

## 2. The Fallen Observatory

**`fallenobservatory` · 25×23 · per region (the Skyreach hoard lattice, §7).**

A round skystone-brick dome whose north-west side came down. The telescope
still points at nothing from a chequer dais; beside it stands the astronomer's
chest, which is a mimic. And east of the dais, inside the ring, stands a
**nightfell cube with no door at all** — the instrument cellar, sealed when the
dome fell. It is the only building in the mod you get into with a pickaxe.
The sign outside swears there is nothing in it.

```
       0         1         2    
       0123456789012345678901234
  y0   .........................
  y1   .........................
  y2   ..........x..............
  y3   ......rx.r##O##..........
  y4   .....x..,,,,,,,##........
  y5   ......,,,,,,,,,,,##......
  y6   ......#,r,,,,,,g,,#......
  y7   .....#,,,,,,,,,,,,,#.....
  y8   .....#,,,W,,,,%%%%%#.....
  y9   ....#,,+++++,,%==k%,#....
  y10  ....#,,++Y++,,%=T=%,#....
  y11  ....O,,+++++,,%===%,O....
  y12  ....#,,+M+U+,,%%%%%,#....
  y13  ....#,b+++++,,,,,,,,#....
  y14  .....#w,,,,,,,,,,,,#.....
  y15  .....#,g,,,,,w,,,,,#.....
  y16  ......#,,,,,,,,,r,#......
  y17  ......##,,,,,,,,,,,r.....
  y18  ........##,,,,,##r.x.....
  y19  ..........##D##..........
  y20  ..........L.,.L..........
  y21  ...........S,............
  y22  .........................
```

```
  #  skystonebrickwall   O  skystonebrickwindow   D  skystonebrickdoor
  %  nightfellwall — the sealed cellar, NO door    =  gloomwoodfloortile
  ,  skystonetile        +  marblecheckertile (the dais)
  Y  skywatchtelescope   U  skywatchastrolabe    g  chargecrystal
  k  skywatchbookshelf (inside the cellar)       b  skywatchbookshelf (ruin)
  r  skywatchrubble      x  skystonerock          L  wardencandelabra
  M  hoardmimic (the astronomer's chest)          T  birchchest (the prize)
  w  stormwisp ×2        W  dawnpiercer, lifted to tier 5
  S  sign
```

**The ring** is the real 4-neighbour boundary of a radius-9 disc — the
§0.4-style predicate, generated rather than drawn — with three windows each
mid-run (§0.3) and a door at (12,19). The collapse opens (6,5) (7,5) (8,4)
(9,4); the crack opens (17,17) (18,17). Rubble lies BESIDE the gaps, never in
them: `skywatchrubble` carries a 20×20 collision box and would seal them.

**The cellar.** x14..x18 × y8..y12, interior 3×3: the prize at (16,10), a
shelf of the Warden's own star charts at (17,9). The Dawnpiercer that nests on
the lens hangs above the dais at (9,8); two Storm Wisps drift the ruin.

**Lighting:** 2 charge crystals inside, 2 lamps on the approach — a broken
place, deliberately under the band like chapter 01's Redoubt.

**The sign:** *"OBSERVATORY CLOSED. The dome is not structurally sound. The
instrument cellar has been sealed for your safety. There is nothing of value in
the instrument cellar."*

**What the player leaves with.** Prize: 3–7 Prismshard, 4–8 Stormglass, 2–4
Stormshard, 500–1400 coin, 20% Aurora Locket, 15% Skystone Heart. Cellar shelf:
2–5 Aurora Petal, 4–8 Prismwood, 40% 1–3 Prismshard. Ruin shelf: 40–150 coin,
50% Cloudberries. The mimic: 120–380 coin, 40% 1–3 Prismshard.

## 3. The Hedge Labyrinth

**`edenhedgelabyrinth` · 23×23 · common (Eden lattice).**

Eden is "uncomfortably perfect" (`WORLD_DESIGN.md` §A3.3), and nothing is more
perfect than a hedge maze. A 10×10-cell **perfect maze** — one path between
any two points, generated once with a fixed seed and then frozen into the map —
of vanilla forest hedge, with a hedged clearing at its heart. Every one of its
six dead ends ends in a box, and half of the boxes are mimics. Two Jealous
Vines wait in corridors. In the clearing, under four candelabra, a lifted
Forbidden Serpent coils in front of the Garden's chest, with a Bloom Maw either
side of the gate.

```
       0         1         2  
       01234567890123456789012
  y0   .......................
  y1   .hhhhhhhhhhhhhhhhhhhhh.
  y2   .h,,,,,,,h,,,h,,,hM,,h.
  y3   .hhhhhhh,h,h,h,h,hhh,h.
  y4   .h,,,h,,,h,h,h,h,,,,,h.
  y5   .h,h,h,hhh,h,h,hhhhh,h.
  y6   .h,h,h,h,,,h,h,,,hC,,h.
  y7   .h,h,h,hhhhhhhhh,hhh,h.
  y8   .hvh,,,hc,,,,,ch,,,h,h.
  y9   .h,hhhhh,,,T,,,hhh,h,h.
  y10  .h,h,,,h,,,,,,,h,h,h,h.
  y11  .h,hhh,h,,,W,,,h,h,h,h.
  y12  .h,,,h,h,,,,,,,h,h,h,h.
  y13  .hhh,hhh,m,,,m,hhh,h,h.
  y14  .hMh,,,hc,,,,,ch,h,hCh.
  y15  .h,hhhhhhhhGhhhh,h,hhh.
  y16  .h,h,,,,,,,,,h,v,h,,,h.
  y17  .h,h,hhh,hhh,h,hhhhh,h.
  y18  .h,h,hCh,,,,,h,h,,,,,h.
  y19  .h,h,h,hhhhhhhhh,hhh,h.
  y20  .h,,,,,,,,,,,,,,,hM,,h.
  y21  .hhhhhhhhhhGhhhhhhhhhh.
  y22  .......................
```

```
  h  foresthedge (vanilla FenceObject — every run checked against §0.4)
  G  foresthedgegate: the entrance (11,21) and the clearing's gate (11,15)
  ,  edenmosstile        c  palmcandelabra ×4 (the clearing)
  C  storagebox (honest) M  hoardmimic, lifted to tier 3 (Eden row)
  T  palmchest (the prize)
  v  jealousvine ×2      m  bloommaw ×2      W  forbiddenserpent, lifted to tier 5
```

**The maze** is solvable and every dead end reachable — checked by a
breadth-first walk from the entrance gate over the same map when it was drawn.
Dead ends at (18,2) M, (18,6) C, (2,14) M, (20,14) C, (6,18) C, (18,20) M.
The clearing is x8..x14 × y8..y14 (49 tiles) inside its own hedge ring, gate on
the south side.

**Lighting:** the maze none — it is a garden at dusk — the clearing 4 in 49.

**No sign.** A maze explains itself.

**What the player leaves with.** Prize: 4–8 Eden Bronze Bar, 3–6 Serpent
Scale, 1–3 Venom Fang, 2–5 Golden Pollen, 1100–2800 coin, 50% 2–4 Paradise
Apple, 15% Bloom Fang. Honest boxes: 3–8 Eden Berry, 50% 3–8 Eden Copper Ore,
60% 80–260 coin. Each mimic: 200–520 coin, 40% 1–3 Eden Bronze Bar.

## 4. The Pilgrims' Ossuary

**`steinfeldossuary` · 21×29 · common (Steinfeld lattice).**

Steinfeld is *"the place where the sky stops working properly"* (§A3.4), and
the pilgrims who came here to lay their dead came up this corridor. A
churchyard of crypt gravestones where two Stone Mourners stand like statues;
a stone door; and a three-wide processional fifteen tiles long, **with three
pressure plates in its middle column, each wired to an arrow trap in the wall
of its own row**. Six ossuary niches open off the corridor, three with honest
boxes of grave goods and three with mimics. At the top, behind a second door,
the reliquary: four chapel columns, two broken angels, and a lifted Hollow
Angel standing guard over the pilgrims' offerings.

```
       0         1         2
       012345678901234567890
  y0   .....................
  y1   ......#########......
  y2   ......#c,,T,,c#......
  y3   ......#,i,,,i,#......
  y4   ......#,,,W,,,#......
  y5   ......#,i,,,i,#......
  y6   ......#e,,,,,e#......
  y7   ......####D####......
  y8   ........#,,,#........
  y9   ......###,,,###......
  y10  ......#M,,,,,C#......
  y11  ......###,,,###......
  y12  ........t,p,#........
  y13  ......###,,,###......
  y14  ......#C,,,,,M#......
  y15  ......###,,,###......
  y16  ........#,p,u........
  y17  ......###,,,###......
  y18  ......#M,,,,,C#......
  y19  ......###,,,###......
  y20  ........t,p,#........
  y21  ........#,,,#........
  y22  .....g..#,,,#........
  y23  ........##D##..g.....
  y24  ........c;;;c........
  y25  .....gn..;;;..g......
  y26  .........;;;...n.....
  y27  ....g..g.;;;.S..g....
  y28  .........;;;.........
```

```
  #  stonewall        D  stonedoor         ,  crackedmarbletile
  ;  miststonetile (the churchyard path)
  t  stonearrowtrap turned 1 — fires EAST across its row
  u  stonearrowtrap turned 3 — fires WEST across its row
  p  stonepressureplate, wired (wire 0) to the trap on its row
  i  chapelcolumn      e  brokenangel      c  stonecandlepedestal
  g  cryptgravestone1 (on the painter's ground)
  C  storagebox (honest)  M  hoardmimic, lifted to tier 5 (Steinfeld row)
  T  deadwoodchest (the prize)
  n  stonemourner ×2 (among the graves)   W  hollowangel, lifted to tier 5
  S  sign
```

**The traps.** Plates at (10,12), (10,16), (10,20); traps at (8,12) → east,
(12,16) → west, (8,20) → east. Wire from plate to trap inclusive, so a player
walking the middle of the corridor takes an arrow on three rows — and a player
who reads the sign walks the edge. The stone family is the only vanilla wall
family with a trap AND a plate in its own face, which is why the whole crypt is
built of it.

**The niches.** One tile deep, at y10, y14, y18 on both sides: (7,10) M,
(13,10) C, (7,14) C, (13,14) M, (7,18) M, (13,18) C.

**Lighting:** 2 pedestals in the reliquary (35 tiles) and 2 at the crypt door;
the corridor has none.

**The sign:** *"Walk the way the pilgrims walked: slowly, in the middle, and
never on the pale stones. What was given here stays here."* (The middle is
where the plates are. The pilgrims were meant to be hit.)

**What the player leaves with.** Prize: 6–12 Palestone, 4–9 Grave Salt, 2–5
Echo Shard, 3–7 Spirit Moss, 1400–3500 coin, 20% Mourning Band. Honest boxes:
1–3 Grave Salt, 60% 100–320 coin. Each mimic: 250–650 coin, 40% 1–2 Echo Shard.

## 5. The Wedding Feast

**`ghostweddingfeast` · 25×21 · common (Ghost Realm lattice).**

The Ghost Realm has *"a society: some friendly, some mad, some hostile"*
(§A3.5), and this is its party. A nightfell hall laid for a wedding that never
ended: one long bone table of eleven sections set with old plates, broken
plates, dirty plates and skulls, and twenty-four seats around it — **eleven are
real bone chairs and thirteen are Possessed Chairs**, the realm's own
furniture-that-wakes. The wedding gifts are stacked along the east wall; two
are mimics. At the head of the table, in the mouth of the apse, the Mourning
Bride, lifted, still waits — and behind her, in the apse, the dowry.

```
       0         1         2    
       0123456789012345678901234
  y0   ........#########........
  y1   ........#c,,T,,c#........
  y2   ........#,,,,,,,#........
  y3   ..###O###,,,,,,,###O###..
  y4   ..#c,,,,#,,,,,,,#,,,,c#..
  y5   ..#,,,,,,,,,W,,,,,,,,,#..
  y6   ..#,,,,,,,,,,,,,,,,,,M#..
  y7   ..O,,,,,,,,,,,,,,,,,,,O..
  y8   ..#,,,,,,,,,,,,,,,,,,C#..
  y9   ..#,,,,hPhPPhPhPPh,,,,#..
  y10  ..#,,,Pttttttttttth,,,#..
  y11  ..#,,,,PhPhhPPhPhP,,,,#..
  y12  ..#,,,,,,,,,,,,,,,,,,M#..
  y13  ..O,,,,,,,,,,,,,,,,,,,O..
  y14  ..#,,,,,,,,,,,,,,,,,,C#..
  y15  ..#,,,,,,,,,,,,,,,,,,,#..
  y16  ..#c,,,,,,,,,,,,,,,,,c#..
  y17  ..###O######D######O###..
  y18  ............;............
  y19  ...........L;L...........
  y20  ..........S.;............
```

```
  #  nightfellwall      O  nightfellwindow      D  nightfelldoor
  ,  blackcobbletile    ;  spiritstonetile (the path)
  t  bonemodulartable ×11, decorated in reading order with
     oldplate, brokenplate, skull, dirtyplate (FENCE_AND_TABLE_DECOR)
  h  bonechair (turned to the table by the interpreter)
  P  possessedchair ×13 (a mob: vanilla MimicMob on the Ghost row)
  c  deadwoodcandelabra ×6   L  ghostlantern ×2 (outside)
  C  storagebox (honest)  M  hoardmimic, lifted to tier 7 (Ghost row)
  T  bonechest (the dowry)
  W  mourningbride, lifted to tier 5
  S  sign
```

**The hall** x3..x21 × y4..y16, the apse x9..x15 × y1..y4 open to it. Table
x7..x17 on y10; seats on y9 and y11 and at both ends. Gifts at (21,6) M,
(21,8) C, (21,12) M, (21,14) C. The bride at (12,5); the dowry at (12,1).

**Lighting:** 6 candelabra over ~270 tiles = 1 per 45, a little under the band:
it is a feast, but it is a dead one.

**The sign:** *"You are cordially invited. The feast has been going on for some
time. Please find a seat. The seats will find you."*

**What the player leaves with.** Dowry: 4–8 Spiritsteel Bar, 5–10 Soul Thread,
4–8 Spectral Ore, 6–12 Bonewood, 1700–4200 coin, 20% Soul Collar. Honest gifts:
2–4 Soul Thread, 60% 120–380 coin. Each mimic: 300–800 coin, 35% 1–2
Spiritsteel Bar. The Possessed Chairs drop their own `GhostLoot.ambusher()`.

## 6. The Hall of Many Doors

**`crookedhallofdoors` · 25×19 · common (Crooked Beyond lattice).**

`WORLD_DESIGN.md` §A3.1 makes free-standing doors the Crooked Beyond's pillar,
and §A3.6 says *the rules of the world decay*. So: one arcanic building cut in
two by a wall with **five ways through it** — two are doors, and the other
three are Door Mimics standing in the gap where a door should be. Beyond it the
gallery, where the windows lie on the floor and the clocks are crooked, and a
lifted Rare Crooked Golem guards the prize — which is vanilla's `mimicchest`,
**a real chest that looks exactly like a mimic.** In the one place in the mod
where the doors bite, the only honest thing is the thing that looks like it
would.

```
       0         1         2    
       0123456789012345678901234
  y0   .........................
  y1   .........................
  y2   ..####O###########O####..
  y3   ..#l======k===k======l#..
  y4   ..#==w======T======w==#..
  y5   ..#====a==============#..
  y6   ..O=========W=========O..
  y7   ..#===================#..
  y8   ..#==w===w=====w===w==#..
  y9   ..#===================#..
  y10  ..#===================#..
  y11  ..###M##D###M###D##M###..
  y12  ..#;;;;;;;;;;;;;;;;;;;#..
  y13  ..#;;;;;;;;;;;;;;;;;;;#..
  y14  ..O;C;;;;;;;x;;;;;;;C;O..
  y15  ..#;;;;;;;;;;;;;;;;;;;#..
  y16  ..#l;;;;;;;;;;;;;;;;;l#..
  y17  ..####O#####D#####O####..
  y18  ...........S;............
```

```
  #  arcanicwall        O  arcanicwindow        D  arcanicdoor
  =  checkerstonetile (gallery)   ;  crookedstripetile (antechamber)
  w  groundwindow ×6 (the windows in the floor)
  l  bentlantern ×4     k  crookedclock ×2     x  crookedcrate
  M  doormimic ×3, in the doorways (5,11) (12,11) (19,11)
  C  storagebox (honest)
  T  mimicchest — vanilla's real 40-slot chest (MimicStorageBoxInventoryObject)
  a  crookedarmadillo   W  rarecrookedgolem, lifted to tier 5
  S  sign
```

**The wall** is y11, x2..x22. Real doors at (8,11) and (16,11); the middle
doorway, the obvious one, is a mimic. Antechamber x3..x21 × y12..y16 with two
honest boxes and a breakable crate; gallery x3..x21 × y3..y10.

**Lighting:** 4 bent lanterns over 247 tiles = 1 per 62: the realm is lit by
its own neon, not by lamps.

**The sign:** *"ONE OF THESE IS A DOOR. — the management"* (Two are.)

**What the player leaves with.** Prize: 3–6 Reality Shard, 5–10 Warp Resin,
5–10 Strange Fabric, 6–12 Oddwood, 2200–5500 coin, 50% 2–4 Eye Seed, 20%
Striped Horn. Honest boxes: 3–6 Oddwood, 60% 180–480 coin. The Door Mimics drop
their own realm table (`DoorMimicMob.lootTable`); they carry no hoard.

---

## 7. What shipped, and how it was proven

| place | kind # | realm lattice | cast per stamp |
|---|---|---|---|
| Counterfeit Treasury | 27 | Skyreach | golem (lifted) + 2 sentries + 5 hoard mimics |
| Fallen Observatory | 28 | Skyreach | dawnpiercer (lifted) + 2 wisps + 1 hoard mimic |
| Hedge Labyrinth | 29 | Eden | serpent (lifted) + 2 vines + 2 bloom maws + 3 mimics |
| Pilgrims' Ossuary | 30 | Steinfeld | hollow angel (lifted) + 2 stone mourners + 3 mimics |
| Wedding Feast | 31 | Ghost Realm | bride (lifted) + 13 possessed chairs + 2 mimics |
| Hall of Many Doors | 32 | Crooked Beyond | rare golem (lifted) + armadillo + 3 door mimics |

Kinds are APPENDED (27–32); no existing ordinal moved. None is once-per-world
— the treasure is not a person. The four outer-realm places are appended to
their band's row of `RealmPoiWorldPreset.REALM_KINDS`, where one or two kinds
shared a band that offers 120–270 sites.

**The two Skyreach places are on a lattice of their own**
(`RealmPoiWorldPreset.SKY_HOARD_KINDS`: the same 220-tile grid re-seeded with
`HOARD_SALT`, a site in 16% of cells instead of 42%). Appending them to the
Skyreach row was tried first and failed on the second seed it met: the band is
bounded by its own depth and offers only ~35–38 sites, `skyreachRotate` deals
them out by rank, and eighteen kinds instead of sixteen left the 49×55 Sky Tower
one or two cells, both of which missed land (seed 1486191071:
`realmpoi kind skytower: … accepted=0`). The sixteen chapter-01 kinds now keep
exactly the cells they had, and the vaults get their own — about §0.6's "per
region" rarity: on seed 1526752859, 9 treasuries and 5 observatories in the
whole band.

**Evidence** is `/skyreachstatus pois` (`RealmPoiCensus`), asserted in
`scripts/integration_test.sh`: each kind's nearest site is force-generated and
compared with its preset tile by tile (`realmpoi stamp: … missing=0`), and a
`realmpoi hoard` line reads the world back — every mob the plan draws standing
in the footprint (`cast=`), the mimics among them (`mimics=`), every hoard
mimic carrying its hoard (`mimicloot=`), the guardian carrying the tier buff
(`lifted=1/1`), every container non-empty (`loot=`). Two full runs PASSed on seeds 1526752859 and 1478681286 (`kinds=30/30 queuedkinds=30/30`, all six `missing=0`, every hoard line full, e.g. `realmpoi hoard ghostweddingfeast: … cast=16/16 mimics=2/2 mimicloot=2/2 guardian=mourningbride lifted=1/1 loot=3/3`). The run that first proved
it is quoted in the commit that landed this file.

**Not proven, and how it would be:** whether the places *read* well — `[game]`.
Whether a mimic and a `storagebox` look alike on screen is vanilla's own claim
(it swaps one for the other), not something this server-only run can see.
Whether the arrow traps actually fire on a player walking the plates is
source-read (`VERIFIED [jar]`), not run.
