#!/usr/bin/env bash
# Offline preview of every building, material gallery and realm ground sample,
# rendered from what the REAL game writes.
#
#   1. boots the dedicated server with the built mod on a throwaway world,
#   2. runs `swhshowroom build` (every preset stamped in the showroom, then
#      checked: expected/placed/missing per exhibit),
#   3. runs `swhshowroom export` (tiles + every object layer + rotation per
#      exhibit, and a registry of the classes/texture names involved),
#   4. renders each exhibit with tools/preset_render.py into build/qa/presets/.
#
# Mod sprites come from src/main/resources. Vanilla sprites come from
# NECESSE_SPRITES (a /swhdumpsprites dump; default /home/user/necesse-game/sprites)
# when it exists, else they are drawn as labelled colour blocks.
#
# Usage: scripts/preset_render.sh [exhibit-id ...]      (no ids = all)
# Env:   NECESSE_GAME_DIR (required), NECESSE_SPRITES (optional),
#        PRESET_RENDER_SKIP_SERVER=1 to re-render an existing export only.
set -u
REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$REPO_DIR/build/qa/presets"
DATA="$OUT/data"
mkdir -p "$OUT"

if [ "${PRESET_RENDER_SKIP_SERVER:-0}" != "1" ]; then
    GAME_DIR="${NECESSE_GAME_DIR:?Set NECESSE_GAME_DIR to the dedicated server directory}"
    MOD_DIR="$REPO_DIR/build/jar"
    ls "$MOD_DIR"/*.jar >/dev/null 2>&1 || { echo "FAIL: no mod jar in $MOD_DIR (run ./gradlew buildModJar)"; exit 1; }
    WORK="$REPO_DIR/build/preset-render-$$"
    PORT="${PRESET_RENDER_PORT:-$(( 17000 + $$ % 2000 ))}"
    JAVA_BIN="$GAME_DIR/jre/bin/java"
    [ -x "$JAVA_BIN" ] || JAVA_BIN="java"
    rm -rf "$WORK" "$DATA"
    mkdir -p "$WORK"
    cd "$WORK" || exit 1
    LOG="$WORK/server.log"
    mkfifo cmd.pipe
    unset JAVA_TOOL_OPTIONS
    "$JAVA_BIN" -Xms256m -Xmx2G -Djdk.attach.allowAttachSelf=true -jar "$GAME_DIR/Server.jar" -nogui -localdir \
        -world showroom -owner tester -port "$PORT" -mod "\"$MOD_DIR\"" < cmd.pipe > "$LOG" 2>&1 &
    PID=$!
    exec 3> cmd.pipe
    wait_for() {
        local waited=0
        while ! grep -qE "$1" "$LOG" 2>/dev/null; do
            sleep 1; waited=$((waited + 1))
            kill -0 "$PID" 2>/dev/null || { echo "FAIL: server exited"; tail -30 "$LOG"; exit 1; }
            [ "$waited" -ge "$2" ] && { echo "FAIL: timeout waiting for $1"; tail -30 "$LOG"; kill "$PID"; exit 1; }
        done
    }
    count() { grep -c "$1" "$LOG" 2>/dev/null || true; }
    wait_for "Type help for list of commands|Server started|world loaded" 300
    sleep 3
    echo "swhshowroom build" >&3
    wait_for "SWH_SHOWROOM_DONE" 900
    echo "swhshowroom export $DATA" >&3
    while [ "$(count SWH_SHOWROOM_DONE)" -lt 2 ]; do sleep 1; kill -0 "$PID" 2>/dev/null || break; done
    echo "stop" >&3
    for _ in $(seq 1 30); do kill -0 "$PID" 2>/dev/null || break; sleep 1; done
    kill "$PID" 2>/dev/null
    exec 3>&-
    grep -E "SHOWROOM" "$LOG" | sed 's/^/  /'
    cd "$REPO_DIR" || exit 1
    rm -rf "$WORK"
fi

python3 "$REPO_DIR/tools/preset_render.py" --data "$DATA" --out "$OUT" "$@"
