#!/usr/bin/env bash
# Install the mod into an EXISTING world and prove the Skyreach comes up in it.
#
# WHY THIS EXISTS, next to scripts/integration_test.sh. That test creates its
# own world, so every level in it is born with the mod already loaded. The
# question a player actually asks is the opposite one: "I have played this world
# for two months without your mod — can I install it now, or do I have to throw
# the save away?" docs/SAVE_COMPAT.md answers it in prose; this answers it by
# booting the real save.
#
# What it checks, one phase per claim:
#
#   Phase A — a world that has never seen the mod boots with it, and the four
#     pieces of world data the mod owns are created on that FIRST LOAD rather
#     than only at world creation:
#       1. the realm plane   — skyreach2 does not exist in the save; the mod's
#                              WorldGenerator has to mint it on demand
#       2. swh_realmpois     — the WorldPreset catalogue has to run over the
#                              regions that plane generates
#       3. boss portals      — SkyLevel.onRegionGenerated has to place them
#       4. residents         — the same, plus the one-per-world claim in
#                              SkywatchWorldData, which the save has no record of
#   Phase B — restart on the same world: everything above survived the write.
#
#   ...and the surface half of the save has to come out untouched, which is
#   docs/DESIGN_DECISIONS.md's hard rule ("Surface data is never touched by
#   Skyreach migration").
#
# The world zip is COPIED, never moved and never opened in place: the argument
# is expected to be a backup, and this script must stay safe to point at one.
#
# Requirements:
#   - NECESSE_GAME_DIR points at a dedicated-server install (Server.jar [+ jre/])
#   - the mod jar was built: ./gradlew buildModJar
#
# Usage: scripts/save_compat_check.sh <world-zip> [extra-mods-dir]
#
#   world-zip        a COPY of a real world, e.g. from backup-saves-<date>/.
#                    The server addresses a world by file name and its inner
#                    folder has to match, so the zip's basename is the name.
#   extra-mods-dir   the other mods the world was played with. Each *.jar in it
#                    is loaded beside ours, so an unknown-object warning caused
#                    by a MISSING third-party mod cannot be mistaken for a bug
#                    in this one.

set -u
REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
GAME_DIR="${NECESSE_GAME_DIR:?Set NECESSE_GAME_DIR to the dedicated server directory}"

SRC_ZIP="${1:?Usage: scripts/save_compat_check.sh <world-zip> [extra-mods-dir]}"
[ -f "$SRC_ZIP" ] || { echo "FAIL: no such world zip: $SRC_ZIP"; exit 1; }
EXTRA_MODS="${2:-}"
WORLD="$(basename "$SRC_ZIP" .zip)"

# Same reasoning as integration_test.sh: own directory and own port per run, so
# two agents running this at once cannot pull each other's world file away or
# fight over a socket.
WORK_DIR="${SAVE_COMPAT_WORK_DIR:-$REPO_DIR/build/save-compat-$$}"
PORT="${SAVE_COMPAT_PORT:-$(( 17000 + $$ % 2000 ))}"

# Steam ships a WINDOWS jre/; only the dedicated-server download has a usable
# one. Fall back to PATH java, which is what runs this under WSL.
JAVA_BIN="$GAME_DIR/jre/bin/java"
[ -x "$JAVA_BIN" ] || JAVA_BIN="java"

BUILT_JAR="$(ls "$REPO_DIR"/build/jar/*.jar 2>/dev/null | head -1)"
[ -n "$BUILT_JAR" ] || { echo "FAIL: no mod jar in build/jar (run ./gradlew buildModJar)"; exit 1; }

rm -rf "$WORK_DIR"
# The jars go into the run's OWN mods/ directory, which `-localdir` makes the
# server read, and `-mod` is deliberately NOT used.
#
# `-mod <dir>` declares a DEVELOPMENT mod, and a development mod directory must
# hold exactly one jar. Point it at a directory with our jar plus three
# third-party ones and the server loads NONE of them, prints a single
# "Development mod must be a directory with one jar file in it" warning, and
# starts anyway — on a world whose world.dat holds our data. It then throws
# `Could not instantiate world data with id swhskyfall` out of
# WorldDataRegistry.loadWorldData and keeps going with that data dropped. That
# reads exactly like a save-compatibility bug in this mod and is nothing of the
# sort, which is why it is written down here rather than only fixed.
mkdir -p "$WORK_DIR/saves/worlds" "$WORK_DIR/mods"
cp "$BUILT_JAR" "$WORK_DIR/mods/"
if [ -n "$EXTRA_MODS" ] && [ -d "$EXTRA_MODS" ]; then
    for jar in "$EXTRA_MODS"/*.jar; do
        [ -e "$jar" ] && cp "$jar" "$WORK_DIR/mods/"
    done
    echo "Loading beside ours: $(ls "$WORK_DIR/mods" | grep -vF "$(basename "$BUILT_JAR")" | tr '\n' ' ')"
fi
cp "$SRC_ZIP" "$WORK_DIR/saves/worlds/$WORLD.zip" || { echo "FAIL: could not copy the world"; exit 1; }
cd "$WORK_DIR"

LOG=""
SERVER_PID=""
PIPE=""

fail() {
    echo "FAIL: $1"
    echo "--- last 40 log lines ---"
    tail -40 "$LOG" 2>/dev/null || true
    [ -n "$SERVER_PID" ] && kill "$SERVER_PID" 2>/dev/null
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

wait_count() { # pattern count timeout_seconds
    local pattern="$1" want="$2" timeout="$3" waited=0
    while [ "$(grep -cE "$pattern" "$LOG" 2>/dev/null || true)" -lt "$want" ]; do
        sleep 2
        waited=$((waited + 2))
        kill -0 "$SERVER_PID" 2>/dev/null || fail "server exited early while waiting for $want x: $pattern"
        [ "$waited" -ge "$timeout" ] && fail "timeout waiting for $want x: $pattern"
    done
}

start_server() { # log_file
    LOG="$1"
    PIPE="$WORK_DIR/cmd.pipe"
    rm -f "$PIPE"
    mkfifo "$PIPE"
    unset JAVA_TOOL_OPTIONS
    "$JAVA_BIN" -Xms256m -Xmx3G -Djdk.attach.allowAttachSelf=true -jar "$GAME_DIR/Server.jar" -nogui -localdir \
        -world "$WORLD" -owner tester -port "$PORT" \
        < "$PIPE" > "$LOG" 2>&1 &
    SERVER_PID=$!
    exec 3> "$PIPE"
    echo "Waiting for mod load..."
    wait_for "Loaded mods:.*Stairway to Heaven|Stairway to Heaven" 180
    echo "Waiting for world to be ready..."
    wait_for "Type help for list of commands|Server started|world loaded" 600
    sleep 3
}

# Identical to integration_test.sh's, and for the identical reason: vanilla's
# `stop` path can lose a race with its own save handler and leave the world at
# the last autosave, which reads exactly like a mod bug. `save` cannot.
save_world() {
    local before waited=0
    before="$(grep -c "Completed world save" "$LOG" 2>/dev/null || true)"
    before="${before:-0}"
    echo "Saving world (and waiting for it to land on disk)..."
    echo "save" >&3
    while [ "$(grep -c "Completed world save" "$LOG" 2>/dev/null || true)" -le "$before" ]; do
        sleep 1
        waited=$((waited + 1))
        kill -0 "$SERVER_PID" 2>/dev/null || fail "server exited while saving the world"
        [ "$waited" -ge 300 ] && fail "timeout waiting for the world save to complete"
    done
}

stop_server() {
    save_world
    echo "Stopping server..."
    echo "stop" >&3
    for _ in $(seq 1 60); do
        kill -0 "$SERVER_PID" 2>/dev/null || break
        sleep 1
    done
    kill "$SERVER_PID" 2>/dev/null
    exec 3>&-
    SERVER_PID=""
}

# --- what the save held BEFORE the mod ever touched it ------------------------
# Recorded from the untouched source zip, not from the copy the server is about
# to rewrite. Everything below is measured against these numbers.
BEFORE_LIST="$WORK_DIR/before.txt"
unzip -Z1 "$SRC_ZIP" > "$BEFORE_LIST"
BEFORE_SKY="$(grep -c "levels/skyreach2" "$BEFORE_LIST" || true)"
BEFORE_SURFACE_REGIONS="$(grep -c "levels/regions/surface/" "$BEFORE_LIST" || true)"
BEFORE_PLAYERS="$(grep -c "players/" "$BEFORE_LIST" || true)"
BEFORE_SETTLEMENTS="$(grep -c "levels/settlements/" "$BEFORE_LIST" || true)"
echo "Before: skyreach2 entries=$BEFORE_SKY surfaceRegions=$BEFORE_SURFACE_REGIONS players=$BEFORE_PLAYERS settlements=$BEFORE_SETTLEMENTS"

# --- Phase A: the mod's first ever load of this world --------------------------
start_server "$WORK_DIR/serverA.log"

# The mod's own entry point into the plane. On a world that has no skyreach2
# this is the call that has to MINT one: World.getLevel finds nothing on disk,
# falls through to WorldGenerator.generateNewLevel, and the mod's generator
# answers with a SkyLevel seeded from the world's own worldSeed.
echo "Running skyreachstatus (this has to mint skyreach2 in a world that has none)..."
echo "skyreachstatus" >&3
wait_for "SKYREACH_STATUS_DONE" 300

# Second pass: the first call loads the plane; its serverTick then stamps the
# Warden's Spire and spawns the cats. Same two-step as integration_test.sh.
sleep 6
echo "Running skyreachstatus (spire/Warden verification pass)..."
echo "skyreachstatus" >&3
wait_count "SKYREACH_STATUS_DONE" 2 300

# What this world holds BEFORE the retrofit — the honest starting point of an
# old save, and the line docs/SAVE_COMPAT.md sends the player to read first.
echo "Running swhreset (report only)..."
echo "swhreset" >&3
wait_for "SWH_RESET_DONE" 180

# Ground in the OTHER realm bands of the same plane. This is not decoration:
# the realm POI catalogue is thinly spread and its Skyreach-band kinds are the
# rarest of the six (measured: 7 swh_realmpois records against 115
# swh_crookedhouse ones in a fully explored sky), so a 1024-box around the
# spire is simply not a place where swh_realmpois can be observed. edenstatus
# and veilstatus force-generate ground out in Eden and in the Ghost/Crooked
# bands, which is both where the catalogue lives and the harder claim: a plane
# minted inside an old world has to generate correctly everywhere, not only at
# its origin.
echo "Running edenstatus (generate ground in the Eden band)..."
echo "edenstatus" >&3
wait_for "EDEN_STATUS_DONE" 600
echo "Running veilstatus (generate ground in the Ghost/Crooked bands)..."
echo "veilstatus" >&3
wait_for "VEIL_STATUS_DONE" 600

# The retrofit. On a world this age it is mostly a GENERATOR: the 1024-box
# around the spire has never existed, so walking it is what runs the preset
# catalogue, the boss-portal lattice, the guard packs and the residents.
echo "Running swhreset world (walk the box: portals, packs, residents, POIs)..."
echo "swhreset world" >&3
wait_count "SWH_RESET_DONE" 2 600

# ...and read the same report again, now that the box exists.
echo "Running swhreset (report after the retrofit)..."
echo "swhreset" >&3
wait_count "SWH_RESET_DONE" 3 180

stop_server
LOGA="$WORK_DIR/serverA.log"

# --- Phase B: the same world again, to prove it was written -------------------
echo "Restarting the server on the same world (persistence pass)..."
start_server "$WORK_DIR/serverB.log"
echo "skyreachstatus" >&3
wait_for "SKYREACH_STATUS_DONE" 300
# The retrofit again, over the identical box. This is the persistence
# assertion, and it has to be phrased this way round: /swhreset's own portal
# count only sees LOADED ground (SkyLevel.countBossPortals skips an unloaded
# tile), so on a freshly booted server with nobody in the sky it reports 0 no
# matter what is on disk. Re-walking the box loads it, and a box whose portals
# survived the write reports +0 — anything else means the portals were lost and
# are being placed a second time.
echo "Running swhreset world again (must place nothing: the sky was written)..."
echo "swhreset world" >&3
wait_for "SWH_RESET_DONE" 600
echo "swhreset" >&3
wait_count "SWH_RESET_DONE" 2 180
stop_server
LOGB="$WORK_DIR/serverB.log"

# --- verification -------------------------------------------------------------
echo "--- verifying: the world had no mod plane to begin with ---"
STATUS=0
[ "$BEFORE_SKY" -eq 0 ] \
    || echo "NOTE: this save ALREADY carried $BEFORE_SKY skyreach2 entries — it is a re-install, not a first install"

echo "--- verifying (1) the realm plane was minted on first load ---"
grep -qE "Skyreach OK: class=SkyLevel" "$LOGA" || { echo "FAIL: SkyLevel was not instantiated in the existing world"; STATUS=1; }
grep -qE "tile (cloudturftile|mistseatile)" "$LOGA" || { echo "FAIL: sky terrain did not generate"; STATUS=1; }
grep -qE "biome (driftlands|stormveil|aurorashoals|skyway)" "$LOGA" || { echo "FAIL: sky biomes did not paint"; STATUS=1; }
grep -qE "spirePlaced=true" "$LOGA" || { echo "FAIL: Warden's Spire was not stamped"; STATUS=1; }
# Two cats, always. The WARDEN is only asserted on a world that has not already
# recruited him: once `recruited=true` he has left the tower and lives in the
# player's settlement, so wardens=0 in the spire is the correct answer there and
# asserting 1 would be asserting a bug.
grep -qE "npc check: wardens=[0-9]+ cats=2" "$LOGA" || { echo "FAIL: the two spire cats are not there"; STATUS=1; }
if grep -qE "recruited=true" "$LOGA"; then
    echo "NOTE: this world already recruited the Warden — he is in a settlement, not the spire"
else
    grep -qE "npc check: wardens=1 " "$LOGA" || { echo "FAIL: no Sky Warden stands in the spire"; STATUS=1; }
fi
# The way in, measured rather than assumed: a player arriving from the stairway
# materialises on the spire's south approach and walks through it. The approach
# being CLEAR is the assertion that matters and it holds either way; the door
# itself is only asserted on a spire this run stamped, because a spire the
# player has already lived in may legitimately have had its door taken out, and
# the mod deliberately never re-stamps built ground (docs/SAVE_COMPAT.md §2).
grep -qE "entrance check: .* clear=true" "$LOGA" || { echo "FAIL: the spire's entrance approach is blocked — the Skyreach has no way in"; STATUS=1; }
if [ "$BEFORE_SKY" -eq 0 ]; then
    grep -qE "entrance check: door=[a-z]*door .*isDoor=true" "$LOGA" \
        || { echo "FAIL: the freshly stamped spire has no door in its south wall"; STATUS=1; }
fi

echo "--- verifying (3) boss portals exist in this world ---"
# Only on a plane this run minted. On a world whose sky was walked by an OLDER
# build the ground around the spire predates the portal lattice, and
# docs/SAVE_COMPAT.md §6 is explicit that walking further out is the answer —
# so requiring a portal here would be asserting against the documented design.
if [ "$BEFORE_SKY" -eq 0 ]; then
    grep -qE "portals in [0-9]+ tiles of the spire: [1-9]" "$LOGA" \
        || { echo "FAIL: no boss portal was placed in the sky this run minted"; STATUS=1; }
else
    echo "NOTE: pre-existing sky — portals near the spire: $(grep -aoE "portals in [0-9]+ tiles of the spire: [0-9]+" "$LOGA" | tail -1)"
fi

echo "--- verifying (4) the residents' one-per-world claim was created ---"
grep -qE "claims residents=" "$LOGA" || { echo "FAIL: SkywatchWorldData's resident claim record was never created"; STATUS=1; }
if [ "$BEFORE_SKY" -eq 0 ]; then
    grep -qE "claims residents=\[[a-z]" "$LOGA" \
        || { echo "FAIL: worldgen stood up no resident at all in the sky it minted"; STATUS=1; }
fi

WORLD_ZIP="$WORK_DIR/saves/worlds/$WORLD.zip"
AFTER_LIST="$WORK_DIR/after.txt"
unzip -Z1 "$WORLD_ZIP" > "$AFTER_LIST"

echo "--- verifying the plane reached the disk ---"
grep -qE "levels/skyreach2\.dat" "$AFTER_LIST" || { echo "FAIL: skyreach2.dat is not in the saved world"; STATUS=1; }
[ "$(grep -c "levels/regions/skyreach2/" "$AFTER_LIST" || true)" -gt 0 ] \
    || { echo "FAIL: the saved world holds no skyreach2 regions"; STATUS=1; }

echo "--- verifying (2) the WorldPreset catalogue ran over the new plane ---"
# The preset region files name the presets they generated by stringID
# (LevelPresetsRegion.GeneratedPresetData), so this reads the catalogue's own
# record off disk rather than inferring it from tiles.
#
# What is asserted is that the catalogue RAN and placed something on skyreach2,
# not that swh_realmpois specifically did. The two mod presets on this plane
# have very different densities — measured on a fully explored sky: 115
# swh_crookedhouse records against 7 swh_realmpois — so demanding the rarer one
# from a bounded probe would make this test fail on chance rather than on a
# defect. Which ones were found is printed, so the rare one is still visible
# when it lands.
PRESET_FILES="$(grep "levels/presets/skyreach2/" "$AFTER_LIST" || true)"
[ -n "$PRESET_FILES" ] || { echo "FAIL: no preset region file was written for skyreach2 — the WorldPreset catalogue never placed anything on the new plane"; STATUS=1; }
mkdir -p "$WORK_DIR/presetcheck"
if [ -n "$PRESET_FILES" ]; then
    ( cd "$WORK_DIR/presetcheck" && unzip -qo "$WORLD_ZIP" "$WORLD/levels/presets/skyreach2/*" )
    FOUND="$(grep -rhao "swh_[a-z]*" "$WORK_DIR/presetcheck" | sort | uniq -c | tr '\n' ' ')"
    if [ -n "$FOUND" ]; then
        echo "OK: the catalogue's own records on skyreach2: $FOUND"
    else
        echo "FAIL: skyreach2 has preset region files but none of them names a mod preset"
        STATUS=1
    fi
fi

echo "--- verifying the surface half of the save was not touched ---"
AFTER_SURFACE_REGIONS="$(grep -c "levels/regions/surface/" "$AFTER_LIST" || true)"
AFTER_PLAYERS="$(grep -c "players/" "$AFTER_LIST" || true)"
AFTER_SETTLEMENTS="$(grep -c "levels/settlements/" "$AFTER_LIST" || true)"
[ "$AFTER_SURFACE_REGIONS" -ge "$BEFORE_SURFACE_REGIONS" ] \
    || { echo "FAIL: surface regions went from $BEFORE_SURFACE_REGIONS to $AFTER_SURFACE_REGIONS"; STATUS=1; }
[ "$AFTER_PLAYERS" -ge "$BEFORE_PLAYERS" ] \
    || { echo "FAIL: player files went from $BEFORE_PLAYERS to $AFTER_PLAYERS"; STATUS=1; }
[ "$AFTER_SETTLEMENTS" -ge "$BEFORE_SETTLEMENTS" ] \
    || { echo "FAIL: settlement files went from $BEFORE_SETTLEMENTS to $AFTER_SETTLEMENTS"; STATUS=1; }
echo "After:  surfaceRegions=$AFTER_SURFACE_REGIONS players=$AFTER_PLAYERS settlements=$AFTER_SETTLEMENTS"

echo "--- verifying it survived the restart ---"
grep -qE "Skyreach OK: class=SkyLevel" "$LOGB" || { echo "FAIL: the Skyreach did not come back after a restart"; STATUS=1; }
grep -qE "spirePlaced=true" "$LOGB" || { echo "FAIL: the spire did not survive the world write"; STATUS=1; }
# Re-walking the identical box must place nothing. That is both the idempotence
# assertion and the persistence one: a portal or a resident that had been lost
# in the write would be placed again here and show up as a non-zero.
grep -qE "regions=[0-9]+ mobs=\+0 bossportals=\+0" "$LOGB" \
    || { echo "FAIL: the second walk over the same box placed something — the sky was not written, or a placement is not idempotent"; STATUS=1; }

echo "--- verifying logs ---"
# Two different questions, and conflating them is what makes a check like this
# useless once real third-party mods are in the directory. An exception that
# names OUR package is a failure. Anything else is reported and not failed on:
# a world played with other mods will produce warnings that are not ours to fix,
# and the test would then fail for reasons that have nothing to do with save
# compatibility.
for L in "$LOGA" "$LOGB"; do
    if grep -nE "stairwaytoheaven.*(Exception|Error)|at stairwaytoheaven\." "$L" > "$WORK_DIR/ourerrors.txt"; then
        echo "FAIL: the mod appears in an exception in $(basename "$L"):"
        head -20 "$WORK_DIR/ourerrors.txt"
        STATUS=1
    fi
    if grep -nE "Exception|ERROR|ModLoadException" "$L" | grep -vE "libraryPatches|SLF4J" > "$WORK_DIR/otherrors.txt"; then
        echo "NOTE: other errors in $(basename "$L") — read them, they are not asserted on:"
        head -20 "$WORK_DIR/otherrors.txt"
    fi
done

if [ "$STATUS" -eq 0 ]; then
    echo "PASS: the mod installs into $WORLD, mints the Skyreach, fills it, and the save survives."
else
    echo "Logs: $LOGA $LOGB"
fi
exit "$STATUS"
