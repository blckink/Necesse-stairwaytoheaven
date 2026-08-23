# Stairway to Heaven

A content mod for [Necesse](https://necessegame.com) (game version **1.3.2**) that completes
the game's vertical axis: after the Cave Ladder (down) and the Deep Cave Ladder (further
down), the **Stairway to Heaven** ascends **up** — through the cloud ceiling into the
**Skyreach**, a persistent sky dimension of floating islands drifting over an endless
Mistsea.

> _Deutsch? Siehe [unten](#-deutsch)._

![Preview](src/main/resources/preview.png)

## Features (v0.1.0 "First Ascent")

- **A real third world layer.** The Skyreach is a persistent, infinite, seeded dimension
  (`+1`, above `surface`/`cave`/`deepcave`), generated region-by-region exactly like the
  underground layers — not an instanced pocket level. Each world gets its own sky.
- **Stairway pair.** Craft the Stairway to Heaven at a Tungsten Workstation
  (8 Tungsten Bars + 15 Quartz), place it on the surface, climb up. A return stairway is
  placed in the sky automatically; both sides use the vanilla ladder netcode, so
  multiplayer works out of the box.
- **Three sky sub-biomes**, painted per-tile into the biome layer like cave biomes:
  - **Driftlands** (common) — silver-green isles, Sky Reeds, Zephyr Rays
  - **Stormveil** (uncommon) — charcoal slate, glowing Storm Crystals, Storm Wisps
  - **Aurora Shoals** (rare) — cold dawn light, Aurora Blooms, rich Aetherium, Skystone Golems
- **The Mistsea** — the swimmable cloud-ocean between islands; bridge it by placing
  tiles (reclaims Cloudturf, not dirt) or swim across.
- **Three enemies** tuned to the Tungsten era: Zephyr Ray (fast melee flier),
  Storm Wisp (ranged spark-caster), Skystone Golem (armored bruiser).
- **New materials & gear:** Skystone, Aetherium Ore/Bars, Storm Shards, Windsilk,
  Aurora Petals — crafted into the **Tempest Edge** (sword) and **Galehowl** (bow),
  deliberate sidegrades to Tungsten weapons, not power creep.
- **English + German localization.**
- **Reproducible pixel-art pipeline:** every texture is generated deterministically by
  `tools/asset_generator/` in vanilla sheet formats — regenerate or hand-replace any
  sprite at will.

## New in v0.2.0 "The Warden's Call"

- **A story and a resident.** The ruined **Warden's Spire** now stands somewhere in the
  Driftlands (seed-deterministic, once per world — existing v0.1 worlds get it too). The
  **Sky Warden** inside gives a four-stage quest chain: find him, rekindle the spire's
  beacon, bring his two runaway cats **Siggi** and **Peanut** home with Cloudpuff
  Treats, and reforge an island anchor. Every stage visibly changes the spire, all
  turn-ins are server-authoritative and multiplayer-safe, dialogue is fully localized
  (EN/DE) with speech bubbles.
- **"Nightfell & Skylight" building set.** Two wall sets with doors and windows,
  a world-locked checkered marble floor, gloomwood planks, wrought-iron fence + gate,
  the Warden's Candelabra, Mistglass Lantern, gothic Gloomraven Statue, crooked
  Gloomwillow — plus quest-exclusive rewards: the colorful Flickerlight Garland,
  the Cat Basket and the Skywatch Banner.
- **Render upgrade.** All sky terrain moved to the vanilla `_splat` autotile format
  (verified cell-by-cell against 1.3.2) and the Mistsea got animated liquid splats.

See [ROADMAP.md](ROADMAP.md) for what comes next (storm events, Aetherium armor,
structures, settlements, the Storm Sovereign boss).

## Installation (players)

1. Build the jar (below) or grab a release jar.
2. Drop it into your Necesse mods folder (`%appdata%/Necesse/mods` on Windows,
   `~/.config/Necesse/mods` on Linux) — or subscribe on the Steam Workshop once
   published.
3. Start the game. Progress to Tungsten tech, craft the Stairway to Heaven, place it on
   the surface, and climb.

## Building (modders)

Requirements: a **JDK 17–23** (recommended: [Temurin 21 LTS](https://adoptium.net) —
on Windows simply `winget install EclipseAdoptium.Temurin.21.JDK`; note that the very
newest JDKs 24/25 are too new for this project's Gradle 8.10 wrapper), plus a Necesse
install (client **or** dedicated server). The build emits Java 8 bytecode via
`options.release` regardless of the JDK used.

**Windows** (cmd.exe — a Steam install of Necesse is auto-detected, no env var needed):

```bat
gradlew.bat buildModJar                     &:: -> build\jar\Stairway_to_Heaven-<gv>-<mv>.jar
gradlew.bat runClient                       &:: launch the game with the mod
gradlew.bat runDevClient                    &:: same, with -dev 1
```

In PowerShell prefix with `.\` (e.g. `.\gradlew.bat buildModJar`). The plain `./gradlew`
script is the Linux/macOS wrapper and won't run in cmd.exe.

**Linux/macOS**:

```bash
# auto-detects a Steam install; otherwise point at any game/server directory:
export NECESSE_GAME_DIR=/path/to/Necesse   # contains Necesse.jar or Server.jar
./gradlew buildModJar                       # -> build/jar/Stairway_to_Heaven-<gv>-<mv>.jar
./gradlew runClient                         # launch the game with the mod (client install)
./gradlew runDevClient                      # same, with -dev 1
```

The Gradle setup was modernized to Gradle 8.10 (runs on current JDKs) while staying
compatible with the upstream template layout (`settings.gradle` holds mod info,
`gradle/main.gradle` the shared logic, `decompileToSources` still works).

### Headless integration test

`scripts/integration_test.sh` boots the official **dedicated server** with the freshly
built jar, creates a throwaway world, drives the mod's `skyreachstatus` admin command
through the server console, and asserts that the Skyreach generates (tiles, biomes,
objects) with a clean log. This catches registry mistakes, world-gen regressions and
load-order bugs without a client:

```bash
export NECESSE_GAME_DIR=/path/to/necesse-dedicated-server
./gradlew buildModJar && scripts/integration_test.sh
```

`skyreachstatus` also works on any modded server as a diagnostics command (admin,
non-cheat): it reports generated tile/biome/object counts around the origin plus
placement diagnostics — paste its output into bug reports.

## Troubleshooting

**The sky (or the Veil) suddenly looks like a vanilla ocean — sharks, zombies,
fireflies — with unwalkable black patches:** that world was opened in a session
where the mod was NOT active (duplicate/old jars in the mods folder, the mod
disabled in the Mods menu, or a Necesse game update that version-locked the
mod). Without the mod, the game regenerates untouched sky regions with the
vanilla island generator and can no longer read the mod's tiles in explored
regions (they render black and block movement).

Fix, in order:
1. Ensure exactly ONE `Stairway_to_Heaven-*.jar` sits in the mods folder and
   the Mods menu shows it enabled without warnings. If the game updated past
   the jar's version, rebuild the jar against the new game version.
2. With the game CLOSED, open the world save zip — usually
   `%appdata%/Necesse/saves/<world>.zip`; some co-op/server tools keep it in a
   `worlds/<world>/` folder instead — and delete BOTH artifacts of the level:
   - the level file `levels/skyreach.dat` (and/or `levels/veil.dat`), and
   - the matching region folder `levels/regions/skyreach/`
     (and/or `levels/regions/veil/`).

   The `.dat` holds the level header, the `regions/` folder holds the explored
   terrain — leaving either behind keeps the corruption. Only the sky/Veil
   resets: it regenerates cleanly on the next ascent (same layout — the seed
   derives from the world seed); quest progress up there restarts. The rest of
   the world is untouched, no cheats involved.
3. Multiplayer: world data lives ONLY in the host's save. Only the host does
   step 2 — joining players store no world files, so there is nothing to
   delete on their side. They just need the same single, current mod jar.

## Repository layout

| Path | Contents |
|---|---|
| `src/main/java/stairwaytoheaven/` | mod code: entry, registries facade, `level/`, `worldgen/`, `biomes/`, `tiles/`, `objects/`, `mobs/`, `items/`, `commands/` |
| `src/main/resources/` | generated textures (vanilla sheet formats), `locale/` (en, de), mod preview |
| `tools/asset_generator/` | deterministic Python/Pillow pixel-art pipeline |
| `scripts/` | headless dedicated-server integration test |
| `docs/DESIGN.md` | full design document (vision, content spec, tuning) |
| `docs/ARCHITECTURE.md` | how the mod hooks the engine, module by module |
| `docs/assets-style-guide.md` | palette + pixel style rules + sheet format cheat sheet |
| `docs/research/` | knowledge base: verified engine/API notes this mod is built on |
| `ROADMAP.md`, `CHANGELOG.md`, `CONTRIBUTING.md` | project management |

## Compatibility

- Built and tested against **Necesse 1.3.2** (dedicated server, headless world-gen test).
- Purely additive: no vanilla registry entries are modified and no bytecode is patched,
  so it should coexist with most mods.
- Removing the mod leaves worlds loadable (unknown level identifiers are simply not
  entered; unknown objects/items are dropped by the game's standard handling).

## License

Like the upstream mod template, this repository is released into the public domain.

---

## 🇩🇪 Deutsch

**Stairway to Heaven** ergänzt Necesse um die dritte Vertikale: Nach Leiter (Untergrund)
und Tiefen-Leiter (Deep Caves) führt die **Himmelstreppe** nach **oben** in die
**Himmelsweite** (Skyreach) — eine persistente, unendliche Himmelsebene mit schwebenden
Inseln über einem begehbaren Nebelmeer.

- **Bauen:** Tungsten-Werkbank → 8 Wolframbarren + 15 Quarz → Treppe auf der Oberfläche
  platzieren und benutzen. Der Rückweg wird oben automatisch platziert; Multiplayer
  funktioniert wie bei Vanilla-Leitern.
- **Drei Himmels-Biome:** Driftlande (häufig), Sturmschleier (Sturmkristalle, Irrlichter)
  und die seltenen Aurorabänke (Aurorablüten, viel Aetherium, Golems).
- **Neue Gegner** (Wolfram-Ära): Zephyrrochen, Sturmirrlicht, Himmelsstein-Golem.
- **Neue Materialien & Waffen:** Aetherium-Erz/-Barren, Sturmsplitter, Windseide,
  Aurorablätter → **Sturmklinge** (Schwert) und **Windheuler** (Bogen).
- Vollständig auf Deutsch lokalisiert.

Bauen: Ein JDK 17–23 installieren (empfohlen: Temurin 21, z. B. per
`winget install EclipseAdoptium.Temurin.21.JDK`), neues Terminal öffnen, dann im
Repo-Ordner `gradlew.bat buildModJar` ausführen (Windows-Eingabeaufforderung; in
PowerShell `.\gradlew.bat buildModJar`) — eine Steam-Installation von Necesse wird
automatisch gefunden. Unter Linux/macOS stattdessen `./gradlew buildModJar`; Details
oben. Die Roadmap (Sturm-Events, Aetherium-Rüstung, Strukturen, Himmels-Boss) steht in
[ROADMAP.md](ROADMAP.md).
