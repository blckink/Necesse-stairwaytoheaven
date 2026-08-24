#!/usr/bin/env bash
# Client-side sprite-index check for the mod's terrain/floor tiles.
#
# The dedicated-server integration test never renders, so it cannot see bugs in
# TerrainSplatterTile.getSplattingTexture — that method only runs on a client.
# This compiles and runs the same arithmetic headless against the real game
# classes. See scripts/TileSpriteCheck.java for what it asserts.
#
# Requirements: NECESSE_GAME_DIR (Server.jar) and a built mod jar in build/jar.
set -eu
REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
GAME_DIR="${NECESSE_GAME_DIR:?Set NECESSE_GAME_DIR to the dedicated server directory}"
MOD_JAR="$(ls "$REPO_DIR"/build/jar/*.jar 2>/dev/null | head -1)"
[ -n "$MOD_JAR" ] || { echo "FAIL: no mod jar in build/jar (run ./gradlew buildModJar)"; exit 1; }

OUT="$REPO_DIR/build/tile-sprite-check"
rm -rf "$OUT"
mkdir -p "$OUT"

unset JAVA_TOOL_OPTIONS
CP="$GAME_DIR/Server.jar:$MOD_JAR"
# The game's bundled JRE ships no compiler, so build with the toolchain javac
# and run on whichever java is on PATH.
javac -nowarn -cp "$CP" -d "$OUT" "$REPO_DIR/scripts/TileSpriteCheck.java"
java -cp "$CP:$OUT" TileSpriteCheck
