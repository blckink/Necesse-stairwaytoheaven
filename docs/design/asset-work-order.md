# Standing asset work order

A queue, not a briefing. A sprite worker picks the **top unchecked item**, does
that one, reports, and stops. The reviewer verifies, ticks the box, commits, and
dispatches the next. This file is the memory between runs — two machine reboots
have already eaten in-flight work, and anything not written here is lost.

**Every target below is measured**, against the vanilla dump in
`vanilla-sprites/` (items, mobs, objects, and — since 2026-08-30 — 120 tiles).
Do not renegotiate a number without re-measuring the analogue and saying so.

## Rules that apply to every item

1. **Read `.claude/skills/necesse-pixel-art/SKILL.md` first.** Then open 2-3
   vanilla sprites of the same category and match their *construction*, not
   just their palette.
2. **Change the generator, never the PNG.** `tools/asset_generator/` owns every
   file under `src/main/resources/`; a hand-edited PNG is silently reverted.
3. **Palette ramps only** (`palette.py`), outline `palette.OUTLINE`, light from
   top-left. Adding a *step* to an existing ramp is allowed and should be said
   out loud; changing a biome's hue is not — the playtest marks those KEEP.
4. **Never redraw anything on the KEEP list** in `docs/PLAYTEST_LOG.md`:
   tulips, grasses, reeds, small flowers, mini vegetation, the Aurora plant core
   sprite, Zephyr Finch, Dewsnail, Zephyr Ray, Storm Wisp, tree sizes and
   silhouettes.
5. **Determinism**: regenerating twice must produce identical bytes.
6. **Nothing else may change.** The reviewer diffs the whole regenerated tree.
7. **No git commands that write.** The reviewer commits.
8. **Look at it.** Render a contact sheet with the vanilla analogue beside it,
   at 1x AND magnified, on the ground it will stand on. Every art fault this
   project has shipped passed its numbers and was obvious in a picture.
9. Pillow is not installed system-wide here:
   `PYTHONPATH=/home/blackoffset/dev/pylib python3 …`

---

## Queue

### 1. Skystone Golem — wrong canvas, not just small
- [ ] **`mobs/skystonegolem.png`**

The player: *"golem sollte neu gemacht werden weil er viel zu klein ist statt
die grösse eines Golems zu haben im Game."* Measured, and it is worse than
"small" — it is on the **smallest mob cell the engine offers**:

| | mass | bbox | cell |
|---|---|---|---|
| vanilla `ashgolem` | 4168 | 60x160 | **64x160** |
| vanilla `furnacegolem` | 8808 | 166x112 | **192x112** |
| vanilla `swampguardian` | 3268 | 70x58 | 80x72 |
| vanilla `crystalgolem` / `ascendedgolem` | 1596 | 36x80 | **64x80** |
| **ours** | **1390** | **43x50** | **64x64** |

Target: the **`crystalgolem` / `ascendedgolem` format — 64x80 cells, sheet
384x320**, 6 columns x 4 rows (Up/Right/Down/Left). Mass >= 1600, bbox at least
40x76. It is an armoured bruiser in the Tungsten-plus tier, so it should read
heavier than a boar (1536), not lighter.
Open `ascendedgolem.png` and `crystalgolem.png` in the dump first — they are the
exact format and they are the same kind of creature.
Watch: `SkyRegistry`/`SkyMobs` may hardcode 64x64 draw offsets; if the taller
cell needs a Java change, **say so and stop** — that is the reviewer's to make.

### 2. Cloud Lamb — a sheep with different wool has no reason to exist
- [ ] **`mobs/cloudlamb.png`**, `mobs/cloudlamb_sheared.png`,
      `mobs/icons/cloudlamb.png`

The player: *"Schafe sollten eher in Furby Richtung gehen oder sowas da es ja
schon Schafe gibt."* Currently 734px at 38x27 against vanilla `sheep` 1180px at
46x40 — smaller than the animal it is meant to be an alternative to, and built
on the same silhouette.

Target: mass >= 950, bbox >= 44x34. **Its own silhouette**: rounder and
squatter than a sheep, an oversized head low on a fat body, big forward-facing
eyes, tiny feet under the mass, tufted ears. The style guide's "cute roundness"
taken further than anywhere else in the mod. Keep the Driftlands palette so it
still belongs to the biome. The sheared sheet must read as the same creature,
deflated — that is where the joke lands.

### 3. The Mistsea — the last loud, non-block-built surface
- [ ] **`tiles/mistsea_shallow_splat.png`**, **`tiles/mistsea_deep_splat.png`**

The whole tile pass landed except this one, which is in
`tile_behaviour_audit.KNOWN_UNFIXED`. Density is fine (617-704); it runs **mean
|dRGB| 31.8-49.7 at 40-48% 2x2 coherence** where every vanilla splat in the game
is 100% coherent and vanilla's only liquid, lava, runs mean 4.3.

Target: keep the density, keep the rolling cloud-deck design and its 8-frame
ping-pong, but put the tone on the **2x2 block grid** and bring the mean to
**<= 14, peak <= 34**. `MISTSEA` will need `grain_d`/`grain_l` steps like the
other grounds. **Read the murkwater work in `gen_veil.py` first** — it solved
exactly this for a liquid, including that a per-frame re-randomised grain boils
instead of flowing, and that a time-keyed bright accent blinks across every tile
at once because animation time is global.
This is the sky's signature surface: one candidate, then stop for review.

### 4. The thin item icons — 35 left under vanilla's floor
- [ ] batch A: `cloudbell` 120, `thunderbloom` 124, `skywatchtelescope` 129,
      `aurorabloom` 141, `prismshard` 141, `stormcrystal` 141
- [ ] batch B: `skyballoon` 146, `auroralily` 148, `cloudberry` 149,
      `gloomwillow` 149, `cloudpufftreat` 157, `silverbell` 161
- [ ] batch C: `skywatchastrolabe` 166, `cinderpearl` 178, `aetheriumore` 181,
      `aetheriumbar` 184, `cloudberrybush` 186, `skystone` 189
- [ ] batch D: `windsilk` 198, `starfall` 203, `skytulip` 207, `skyreeds` 210,
      `mistglasslantern` 213, `charwood`/`nimbuswood`/`prismwood` 225
- [ ] batch E: `staticmoss` 230, `stormscreed` 239, `skystonerock` 245,
      `seraphstatue` 247, `seancecircle` 251, `windwheat` 255,
      `skywatchchalice` 267, `catbasket` 277, `skyparcel` 283

Every vanilla 32x32 item icon in the dump carries **288-712 opaque px, median
440**, in a bbox of at least 16x20 and usually 24x26. Target **>= 300 px, bbox
>= 20x22** each, and name the vanilla analogue you worked from.

Two lessons from the twelve already done, both cost a correction pass:
- **Mass without form fails.** A fattened stroke passes the number and still
  reads as a hairline. Draw the object, then check the number.
- **Say what the SUBJECT is.** "Must read as that plant" produced a full bloom
  for `aurorapetal`, fuller than `aurorabloom` beside it in the inventory. For a
  picked material, draw it picked.
`aurorabloom` in batch A is the pointed one: the petal picked from it is 461 px.

### 5. Held weapon sprites on the wrong canvas
- [ ] **`player/weapons/tempestedge.png`**, **`player/weapons/galehowl.png`**

Both sit on 32x32 while every later mod weapon matches vanilla's larger held
sheets — `skyreave` 96x95 against `quartzglaive` 104x88, `thunderhead` 22x62
against `tungstengreatbow` 20x60. The mod's two original weapons are drawn at
roughly a third of the linear size of everything else in the player's hand.
**Changing the canvas is rendering geometry, not art**: propose the size, do not
ship it, and let the reviewer verify the attack animation first.

### 6. The Veil, in its own language — needs design before art
- [ ] blocked: no chapter brief exists yet

`docs/ROADMAP.md`: contrast, not darkness. Poison green on violet, bone white on
black, stripes, checkerboard, spirals, sickly pink, brass and verdigris — black
is outline and shadow, never fill. One saturated accent per set that nothing
else has, and one funny piece per chapter. The Model Town, the Office of
Eternity, the Ashen Reach zombie quarter, Mortimer and Vesper are all unbuilt.
**Do not start this from the roadmap alone** — it needs a chapter brief naming
the pieces, or it becomes 30 decorative PNGs that never touch the player.

---

## Done

- 12 item icons, 29-117 px -> 310-655 (`13afc95`)
- 6 world sprites incl. the ghost lantern 0.29 -> 1.0 of vanilla (`f6debe4`)
- 6 natural terrains, 63-114 density -> 364-406 at vanilla loudness (`f8e72b2`)
- 4 craftable floors + murkwater onto the 2x2 grid (`6854591`)
- `aurorashoal`, the Aurora Shoals' own ground, new (`ab4a5e7`)
- `skycrate` on vanilla's 6-variant crate layout (`cb60fd1`)
