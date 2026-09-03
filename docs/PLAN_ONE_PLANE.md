# The world is ONE plane

**Architecture law.** Cited from ~35 source files; the filename stays even
though the migration it once described is finished. It obeys
`docs/WORLD_DESIGN.md` §3, §41.3–41.5 and outranks every other doc but that one.

## The law

**One level: `skylevel` / identifier `skyreach2`.** Everything the player walks
to is on it. `StairwayToHeavenMod` registers exactly one modded level; a realm
never adds a dimension. A change that adds `registerLevel` breaks this law.

Realms are **biome-weight bands** over `worldgen/RealmDepth.depthAt`, not zones.
`DEPTH_SCALE = 6000` is the single world-size dial. Bands OVERLAP — that is what
dissolves the hard optical borders §3 forbids:

| realm | depth band | tiles from SkyOrigin |
|---|---|---|
| Skyreach | 0.00–0.30 | 0 – 1800 |
| Eden | 0.10–0.48 | 600 – 2880 |
| Steinfeld | 0.32–0.70 | 1920 – 4200 |
| Ghost Realm | 0.48–0.88 | 2880 – 5280 |
| Crooked Beyond | 0.70–0.94 | 4200 – 5640 |
| Hell | 0.80–1.00 | 4800 – 6000+ |

`SkyTerrainPainter.describeTile` asks `RealmDepth.realmForDepth` and hands the
tile to that realm's `describeBand`. `biomes/OutlandsBiome` + `worldgen/SkyOutlands`
are the reference implementation of a realm as a distance-gated biome (§41.4).

**One waterline, blended.** `REALM_WATERLINE` is per realm and
`waterlineAt(depth)` blends it by realm weight, so no band border shows a
coastline step.

## The anti-rush gate

`stairwaytoheaven/veil/` — `VeilRegion`, `VeilGate`, `SoulExposureBuff`. They keep
their names; their meaning is **any band you have not earned**, not "the Veil".
The debuff stacks the longer you stay: vision, slow, health drain, heavy damage.

§8's abuse case is binding: **do not merely block tiles.** The check is against
the world REGION, so teleporting past the edge does not help. A short step over
the line is meant to be possible — that is how the player learns the next realm
exists. Running through is not.

Unlocks are story beats, stored in `quest/SkywatchWorldData`.

## Travel: the Warden's house (§A2.3)

Not a teleporter net. Each anchor is a themed room in the Warden's house, and a
route becomes fast travel only **after the player has physically made it once**.
The séance is fast travel to the Ghost BAND of this plane — not a door to
another world, because there is no other world.

## Two known holes

1. **Hell has no painter.** The 0.80–1.00 band falls back to the Crooked painter
   so the world has no unpainted hole. One `case` to delete once Hell exists.
2. **`distortion` is threaded and unread.** §3's second value reaches every band
   painter; no painter reads it. Calm/mad variants per realm are content, not
   hosting.

## The rules, unchanged

1. **No new pixel art.** Existing mod sprite, else vanilla by literal path, else
   leave it out and record it in `docs/ASSET_REQUESTS.md`.
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
   `--vanilla vanilla-sprites` is MANDATORY on both audits that take it; without
   the dump they cannot see the game's own resources and report every borrowed
   texture as missing. Never pipe a gate into `head`/`tail` — you read the pipe's
   exit code and the failures scroll away.
