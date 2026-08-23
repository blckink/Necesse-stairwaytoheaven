#!/usr/bin/env bash
# Headless integration test for the Stairway to Heaven mod.
#
# Boots the official dedicated server with the freshly built mod jar, creates
# a throwaway world, then drives the mod's `skyreachstatus` debug command via
# the server console. Passes when the Skyreach generates with the expected
# tiles/biomes and the log stays free of errors.
#
# Requirements:
#   - NECESSE_GAME_DIR points at a dedicated-server install (Server.jar [+ jre/])
#   - the mod jar was built: ./gradlew buildModJar
#
# Usage: scripts/integration_test.sh

set -u
REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
GAME_DIR="${NECESSE_GAME_DIR:?Set NECESSE_GAME_DIR to the dedicated server directory}"
MOD_DIR="$REPO_DIR/build/jar"
WORK_DIR="${INTEGRATION_WORK_DIR:-$REPO_DIR/build/integration-test}"
LOG="$WORK_DIR/server.log"
WORLD="stairwaytest"

JAVA_BIN="$GAME_DIR/jre/bin/java"
[ -x "$JAVA_BIN" ] || JAVA_BIN="java"

ls "$MOD_DIR"/*.jar >/dev/null 2>&1 || { echo "FAIL: no mod jar in $MOD_DIR (run ./gradlew buildModJar)"; exit 1; }

rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR"
cd "$WORK_DIR"

# Command pipe: keep stdin open so we can feed console commands over time
PIPE="$WORK_DIR/cmd.pipe"
mkfifo "$PIPE"

unset JAVA_TOOL_OPTIONS
"$JAVA_BIN" -Xms256m -Xmx2G -jar "$GAME_DIR/Server.jar" -nogui -localdir \
    -world "$WORLD" -owner tester -mod "\"$MOD_DIR\"" \
    < "$PIPE" > "$LOG" 2>&1 &
SERVER_PID=$!
exec 3> "$PIPE"   # hold the pipe open

fail() {
    echo "FAIL: $1"
    echo "--- last 40 log lines ---"
    tail -40 "$LOG" || true
    kill "$SERVER_PID" 2>/dev/null
    exit 1
}

wait_for() { # pattern timeout_seconds
    local pattern="$1" timeout="$2" waited=0
    while ! grep -qE "$pattern" "$LOG" 2>/dev/null; do
        sleep 1
        waited=$((waited + 1))
        kill -0 "$SERVER_PID" 2>/dev/null || fail "server exited early while waiting for: $pattern"
        [ "$waited" -ge "$timeout" ] && fail "timeout waiting for: $pattern"
    done
}

echo "Waiting for mod load..."
wait_for "Loaded mods:.*Stairway to Heaven|Stairway to Heaven" 120

echo "Waiting for world to be ready..."
wait_for "Type help for list of commands|Server started|world loaded" 240

sleep 3
echo "Running skyreachstatus..."
echo "skyreachstatus" >&3

wait_for "SKYREACH_STATUS_DONE" 180

# Second pass: the first call loads the Skyreach; its serverTick then stamps
# the Warden's Spire and spawns the cats. Give it a few ticks and re-check.
sleep 6
echo "Running skyreachstatus (quest verification pass)..."
echo "skyreachstatus" >&3
for _ in $(seq 1 60); do
    [ "$(grep -c SKYREACH_STATUS_DONE "$LOG")" -ge 2 ] && break
    sleep 2
done

echo "Running veilstatus..."
echo "veilstatus" >&3
wait_for "VEIL_STATUS_DONE" 180

echo "Stopping server..."
echo "stop" >&3
for _ in $(seq 1 30); do
    kill -0 "$SERVER_PID" 2>/dev/null || break
    sleep 1
done
kill "$SERVER_PID" 2>/dev/null
exec 3>&-

echo "--- verifying log ---"
STATUS=0
grep -qE "Skyreach OK: class=SkyLevel" "$LOG" || { echo "FAIL: SkyLevel was not instantiated"; STATUS=1; }
grep -qE "tile (cloudturftile|mistseatile)" "$LOG" || { echo "FAIL: sky terrain did not generate"; STATUS=1; }
grep -qE "biome (driftlands|stormveil|aurorashoals)" "$LOG" || { echo "FAIL: sky biomes did not paint"; STATUS=1; }
grep -qE "spirePlaced=true" "$LOG" || { echo "FAIL: Warden's Spire was not stamped"; STATUS=1; }
grep -qE "beaconObject=wardenbeaconoff" "$LOG" || { echo "FAIL: spire beacon object missing"; STATUS=1; }
grep -qE "wardenFloor=marblecheckertile" "$LOG" || { echo "FAIL: spire interior floor missing"; STATUS=1; }
grep -qE "npc check: wardens=1 cats=2" "$LOG" || { echo "FAIL: Warden/cat NPCs not spawned exactly once"; STATUS=1; }
grep -qE "Veil OK: class=VeilLevel" "$LOG" || { echo "FAIL: VeilLevel was not instantiated"; STATUS=1; }
grep -qE "tile (murkmosstile|murkwatertile)" "$LOG" || { echo "FAIL: Veil terrain did not generate"; STATUS=1; }
grep -qE "biome (gloomfen|ashenreach)" "$LOG" || { echo "FAIL: Veil biomes did not paint"; STATUS=1; }
if grep -nE "Exception|ERROR|ModLoadException" "$LOG" | grep -vE "libraryPatches|SLF4J" > "$WORK_DIR/errors.txt"; then
    echo "FAIL: errors found in server log:"
    cat "$WORK_DIR/errors.txt"
    STATUS=1
fi

if [ "$STATUS" -eq 0 ]; then
    echo "PASS: mod loads, Skyreach generates, no errors."
    echo "--- skyreachstatus output ---"
    sed -n '/Skyreach OK/,/SKYREACH_STATUS_DONE/p' "$LOG"
fi
exit "$STATUS"
