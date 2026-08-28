#!/usr/bin/env bash
# Renders the Skyreach as the player sees it, for worldgen calibration.
#
# Compiles scripts/SkyMapDump.java against the built mod jar, dumps the real
# per-tile decision (SkyTerrainPainter.describeTile — the same function
# paintRegion writes into the world), and composites it with real sprites at
# 32 px per tile.
#
# The screen-scale renders are the ones that decide anything: Necesse shows
# roughly 40x22 tiles, so `screen-*.png` IS one screen. The wide renders exist
# only to check that the road network's topology is sane. Judging density on a
# wide render is how the last two worldgen passes went wrong in both directions.
#
# Requirements: NECESSE_GAME_DIR (Server.jar), a built mod jar, python3 + PIL.
# Optional: NECESSE_SPRITES (vanilla sprite dump; default /home/user/necesse-game/sprites)
#
# Usage: scripts/sky_map_render.sh [seed ...]
set -eu
REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
GAME_DIR="${NECESSE_GAME_DIR:?Set NECESSE_GAME_DIR to the dedicated server directory}"
MOD_JAR="$(ls "$REPO_DIR"/build/jar/*.jar 2>/dev/null | head -1)"
[ -n "$MOD_JAR" ] || { echo "FAIL: no mod jar in build/jar (run ./gradlew buildModJar)"; exit 1; }

OUT="$REPO_DIR/build/skymap"
mkdir -p "$OUT"
unset JAVA_TOOL_OPTIONS
CP="$GAME_DIR/Server.jar:$MOD_JAR"
javac -nowarn -cp "$CP" -d "$OUT" "$REPO_DIR/scripts/SkyMapDump.java"

SEEDS="${*:-12345 777 20260827}"

for SEED in $SEEDS; do
    # One screen on the spire's doorstep: the arrival composition.
    java -cp "$CP:$OUT" SkyMapDump "$SEED" -20 -11 40 22 "$OUT/hub-$SEED.txt" origin
    python3 "$REPO_DIR/scripts/sky_map_render.py" "$OUT/hub-$SEED.txt" "$OUT/screen-hub-$SEED.png"

    # Three screens out along the countryside, where most play happens.
    java -cp "$CP:$OUT" SkyMapDump "$SEED" 130 -60 40 22 "$OUT/far-$SEED.txt" origin
    python3 "$REPO_DIR/scripts/sky_map_render.py" "$OUT/far-$SEED.txt" "$OUT/screen-far-$SEED.png"

    java -cp "$CP:$OUT" SkyMapDump "$SEED" -240 190 40 22 "$OUT/far2-$SEED.txt" origin
    python3 "$REPO_DIR/scripts/sky_map_render.py" "$OUT/far2-$SEED.txt" "$OUT/screen-far2-$SEED.png"

    # One screen framed on a designed place, and on another one further out:
    # the composition either reads at 40x22 or it does not exist.
    java -cp "$CP:$OUT" SkyMapDump "$SEED" 0 0 40 22 "$OUT/place-$SEED.txt" station
    python3 "$REPO_DIR/scripts/sky_map_render.py" "$OUT/place-$SEED.txt" "$OUT/screen-place-$SEED.png"

    java -cp "$CP:$OUT" SkyMapDump "$SEED" 260 -180 40 22 "$OUT/place2-$SEED.txt" station
    python3 "$REPO_DIR/scripts/sky_map_render.py" "$OUT/place2-$SEED.txt" "$OUT/screen-place2-$SEED.png"

    # One screen inside the Skyway Passages, framed on a real causeway. The
    # passages are ~14% of the land, so a fixed offset would show them only by
    # luck; `skyway` mode searches for them the way `station` searches for a
    # designed place.
    java -cp "$CP:$OUT" SkyMapDump "$SEED" 0 0 40 22 "$OUT/skyway-$SEED.txt" skyway
    python3 "$REPO_DIR/scripts/sky_map_render.py" "$OUT/skyway-$SEED.txt" "$OUT/screen-skyway-$SEED.png"

    # Topology only: does the network connect, and are the gaps sane?
    java -cp "$CP:$OUT" SkyMapDump "$SEED" -200 -200 400 400 "$OUT/wide-$SEED.txt" origin
    python3 "$REPO_DIR/scripts/sky_map_render.py" "$OUT/wide-$SEED.txt" "$OUT/wide-$SEED.png"
done

echo "renders in $OUT"
