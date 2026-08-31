# Handoff — where this is, and what to pick up

**Rewritten 2026-08-31.** The previous version briefed a session on Skyreach
walls and buildings; that work is done or superseded. What follows is the state
after the Beetle Outlands landed.

Branch: `claude/aktueller-stand-offene-themen-k4ztas`, pushed, clean.

---

## 1. What changed, in one paragraph

The Skyreach used to be one bright, pale world from the spire to the horizon,
and all the contrast in the mod lived in a second dimension (the Veil) behind a
craftable Seance Circle almost nobody opened. The player retired that plan —
*"das wird zu viel arbeit, wir machen nur sky region ... auf eine welt
eindampfen"* — so the Veil's ground, props, mobs and its one building now appear
IN the sky, gated by **distance from the spire** instead of by a door. The Veil
dimension is still registered and still generates, deliberately: un-registering
it would strand every save that has been there. It simply takes no new content.

Details and measurements: `docs/CURRENT_STATE.md`. History: `CHANGELOG.md`.

## 2. The five commits

| commit | what |
|---|---|
| `56b1f4f` | The Beetle Outlands: the distance ramp, the biome, the props, the Crooked House in the sky, Seance Circles standing at fixed sites |
| `0be590e` | `evilwall` from supplied art, registered as a vanilla `RockObject`; crystal massifs through the outcrop formation field |
| `2111c6a` | `evilwall` drops `crystalstone` rather than vanilla stone |
| `01b7cba` | The Outlands spawn `crystalgolem`, `ascendedgolem`, `crystalarmadillo` |
| `e1b2f7d` | Both of the above turned into live gates in `integration_test.sh` |

## 3. The state of the gates

Everything below was run on this branch and passes:

```
./gradlew buildModJar          # against the real 1.3.2 Server.jar
scripts/integration_test.sh    # real server boot, REAL_EXIT=0, no FAIL lines
python3 tools/locale_audit.py
python3 tools/content_ledger.py --check
python3 tools/sheet_format_audit.py
python3 tools/tile_behaviour_audit.py
python3 tools/rotation_variety_audit.py
```

Two numbers the integration test now prints and asserts, so you do not have to
take the paragraphs above on trust:

```
outlands check: floor=900 inside=0/13582 r1200=90/2838 r2000=0/1008
                r3200=40/2029 evilwall=18 portals=0 biome=Beetle Outlands

spawn check: crystalgolem      validSpawnLocation=implemented lit=0/6 dark=4/6
spawn check: ascendedgolem     validSpawnLocation=implemented lit=0/6 dark=4/6
spawn check: crystalarmadillo  validSpawnLocation=implemented lit=0/6 dark=4/6
```

`inside=` is the whole disc within the 900-tile floor, and it is asserted as an
exact zero — that is the floor promise tested directly. The per-radius numbers
are one seed through a small window and swing hard (`r2000=0/1008` here against
7.68% over five seeds offline), so only the floor and the by-3200 arrival are
asserted.

**Run the test unpiped.** `integration_test.sh | tail` reads *tail's* exit code
and scrolls early FAIL lines away; two runs in the session that produced this
work were checked that way and were worth nothing. Redirect to a file instead.

## 4. This container can have a full game install

Recorded because sessions kept standing down for want of one, and the whole
`crystalwall` question above was only answerable with it:

```
scripts/fetch_dedicated_server.sh                             # ~1 min, free
export NECESSE_GAME_DIR=/opt/necesse-server/necesse-server-1-3-2-24650233
./gradlew decompileToSources -PuseDecompiledSources=true      # then UNZIP it:
#   the task writes decompiled/Necesse-sources.jar and can report success while
#   it is still being written. Wait until its size stops changing and
#   `unzip -t` passes, then unpack -> 6,464 readable .java files.
pip install Pillow numpy                                      # unlocks the art audits
```

Still absent: the **client sprite dump**. `scripts/sky_map_render.sh` writes its
text dump fine and then dies in the image half on a missing vanilla tile PNG.
To measure worldgen, compile a probe against `build/mod` + `Server.jar` and call
the pure functions — but note `SkyRegistry`'s ID fields are all **0** outside a
running game, so a probe must compare biome CLASSES and call
`SkyOutlands`/`SkyNoise`, never registry IDs.

## 5. What to pick up, in the order that makes sense

### The fork the player has not answered

The mod now has a gradient pointing the wrong way: **the Outlands hit at
ascended tier, but the sky's own gear is calibrated at deep-cave tier**
(Stormsteel sits deliberately under Glacial). You cannot craft anything to walk
in there with. Two ways out, and the player has not picked one:

1. **A boss behind the portals.** The Seance Circles already stand at fixed
   hashed sites in every Outland and already say out loud that nothing answers
   (`misc.seancesilent`). This is the piece that turns the region from scenery
   into a destination. The **Storm Sovereign** in `ROADMAP.md` is the candidate.
2. **Pull the Sky Arsenal up to ascended tier**, so the mod is coherent with
   itself.

Ask before building either — they are large and they are not interchangeable.

### Smaller, unambiguous

- **The Outlands have no cast of their own.** They are the Veil's furniture plus
  vanilla's ascended mobs. No Outland-specific loot, no structure but the
  Crooked House.
- **`evilwall` has no tool tier.** Vanilla's `crystalrock` uses 10. Copying it
  would pickaxe-gate a whole biome — a real decision, still open.
- **Wall density is a first number, not a balanced one** (2.7–5.5% of Outland
  tiles). Re-run the gate after changing it.
- **Cloud biomes are not *enclosed* shapes.** The player asked for that
  (*"die wolkengebiete sind halt im idealfall immer geschlossen"*); the Outlands
  are cut out of the biome field but the bright biomes were not reshaped.
- **"Ein garten eden der schlange"** — named by the player, never designed.
  Needs a chapter brief before any art.

### Art, if that is your lane

`docs/design/asset-work-order.md` opens with **what a whole biome actually needs
in tiles** — the `_splat` format (224×96 blocks, 7×3 cells of 32px, width =
224×frames, height = 96×sections), the two hard rules (100% coherence on the 2×2
pixel block grid; mean |dRGB| 7–27, because density alone is gameable), and the
full family checklist per ground. Beetle World still wants its own ground, a
calmer second ground, and 3–5 objects that grow nowhere else. The queue under it
is unchanged and still starts at Halda's Fermentation Vat.

## 6. Two traps this session paid for

- **`tools/locale_audit.py` parses source TEXT, comments included.** Pasting
  vanilla's `registerObject("crystalrock", ...)` into a comment as documentation
  made the audit believe the mod registers it and demand
  `items/crystalrock.png`. Describe vanilla registrations in prose.
- **A spawn-table entry whose mob rejects every location is invisible.**
  `Mob.isValidSpawnLocation` is `return false`; that is why the Cloud Lamb's
  Driftlands entry did nothing for three releases. `HostileMob` implements it,
  which is why the three vanilla mobs above work — but that was *read out of the
  class*, not assumed, and then asserted in the test.

## 7. Nothing here has been played

Not one thing in the Outlands has been seen in a real client. Every claim in
this file is either a gate result or a measurement, and the player's own words
outrank both — see `docs/PLAYTEST_LOG.md`, where everything marked KEEP is
player-approved and must not be "improved".
