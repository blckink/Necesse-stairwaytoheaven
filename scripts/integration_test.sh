#!/usr/bin/env bash
# Headless integration test for the Stairway to Heaven mod.
#
# Phase 1 — boots the official dedicated server with the freshly built mod jar,
#   creates a throwaway world, then drives the mod's `skyreachstatus` and
#   `veilstatus` debug commands via the server console. Passes when the Skyreach
#   generates with the expected tiles/biomes and the log stays free of errors.
#
# Phase 2 — restarts the server on the SAME world and re-runs `skyreachstatus`.
#   This is the persistence check: the spire must come back at the identical
#   coordinates, and the Warden and both cats must still be there. Siggi and
#   Peanut are only save-persistent because CritterMob.shouldSave() is
#   `shouldSave && !canDespawn()`, which is easy to break by accident — this
#   phase turns that from a source-reading into an observed fact.
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
# Unique per run. Two of these running at once used to share one directory and
# one world: the second run's `rm -rf` pulled the world file out from under the
# first, which then died inside WorldFile.write with a ClosedFileSystemException
# that reads exactly like a mod bug. Parallel agents make that a normal
# occurrence, not a corner case.
WORK_DIR="${INTEGRATION_WORK_DIR:-$REPO_DIR/build/integration-test-$$}"
WORLD="stairwaytest"
# ...and its own PORT, for the same reason. Giving each run its own directory
# fixed the world file being pulled out from under a concurrent run, but both
# servers still bound the same default socket: the second one dies with
# "java.net.BindException: Address already in use" while the FIRST one is left
# holding a half-written world, which reads exactly like a mod crash in the
# logs. ServerLoader accepts -port (ServerLoader.java:424).
PORT="${INTEGRATION_PORT:-$(( 15000 + $$ % 2000 ))}"

JAVA_BIN="$GAME_DIR/jre/bin/java"
[ -x "$JAVA_BIN" ] || JAVA_BIN="java"

ls "$MOD_DIR"/*.jar >/dev/null 2>&1 || { echo "FAIL: no mod jar in $MOD_DIR (run ./gradlew buildModJar)"; exit 1; }

rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR"
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

start_server() { # log_file
    LOG="$1"
    PIPE="$WORK_DIR/cmd.pipe"
    rm -f "$PIPE"
    mkfifo "$PIPE"
    unset JAVA_TOOL_OPTIONS
    # allowAttachSelf: on installs without a bundled jre/ (plain JDK >= 9 on
    # PATH) ByteBuddy's self-attach fallback fails and kills the boot before
    # mods load. The flag makes the game's own patching step work everywhere.
    "$JAVA_BIN" -Xms256m -Xmx2G -Djdk.attach.allowAttachSelf=true -jar "$GAME_DIR/Server.jar" -nogui -localdir \
        -world "$WORLD" -owner tester -port "$PORT" -mod "\"$MOD_DIR\"" \
        < "$PIPE" > "$LOG" 2>&1 &
    SERVER_PID=$!
    exec 3> "$PIPE"   # hold the pipe open
    echo "Waiting for mod load..."
    wait_for "Loaded mods:.*Stairway to Heaven|Stairway to Heaven" 120
    echo "Waiting for world to be ready..."
    wait_for "Type help for list of commands|Server started|world loaded" 240
    sleep 3
}

stop_server() {
    echo "Stopping server..."
    echo "stop" >&3
    for _ in $(seq 1 30); do
        kill -0 "$SERVER_PID" 2>/dev/null || break
        sleep 1
    done
    kill "$SERVER_PID" 2>/dev/null
    exec 3>&-
    SERVER_PID=""
}

# --- Phase 1: fresh world -----------------------------------------------------
start_server "$WORK_DIR/server.log"

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

# Third pass: coax BOTH spire cats home the way a Cloudpuff Treat does, so the
# travel-home path is actually executed rather than only read. Phase 2 then
# checks they are still at the basket after the world has been written to disk
# and read back -- a cat that is only saved at its LAIR would pass the old
# "cats=2" count and still be missing from the place the quest sent it.
echo "Running skyreachstatus cats (coax the cats home)..."
echo "skyreachstatus cats" >&3
for _ in $(seq 1 60); do
    [ "$(grep -c SKYREACH_STATUS_DONE "$LOG")" -ge 3 ] && break
    sleep 2
done

echo "Running veilstatus..."
echo "veilstatus" >&3
wait_for "VEIL_STATUS_DONE" 180

# Surface pass. The Surface POIs are placed by vanilla's own world-preset
# machinery, which means "the preset compiles" and "the preset lands in a world"
# are two completely different claims. `stamp` measures both: it reads the queue
# the world preset system built for whole 1024x1024 preset regions, then
# force-generates a couple of the queued sites and counts the structures in the
# world that came out of them.
echo "Running skysurfacestatus stamp (surface POIs)..."
echo "skysurfacestatus stamp" >&3
wait_for "SKYSURFACE_STATUS_DONE" 240
# ...and start a Skyfall, leaving it RUNNING with shards on the ground, so the
# world is saved mid-event. Phase 2 then has to restore it and clean up after
# it, which is the only way to prove the event cannot leave anything behind.
echo "Running skysurfacestatus seed (start a Skyfall and leave it running)..."
echo "skysurfacestatus seed" >&3
for _ in $(seq 1 120); do
    [ "$(grep -c SKYSURFACE_STATUS_DONE "$LOG")" -ge 2 ] && break
    sleep 2
done

stop_server
LOG1="$WORK_DIR/server.log"

# --- Phase 2: same world, restarted -------------------------------------------
echo "Restarting server on the same world (persistence pass)..."
start_server "$WORK_DIR/server2.log"
echo "skyreachstatus" >&3
wait_for "SKYREACH_STATUS_DONE" 180
# Wander pass. Give the cats time to actually run their AI before the second
# probe: the homesick tether is what is supposed to keep them at the basket
# (HomesickCritterAI only pulls a critter back past 96px), and a tether rebuilt
# around the WRONG tile only shows once the wanderer has picked a few targets.
# The check immediately after load cannot see it.
echo "Letting the cats wander for 25s..."
sleep 25
# Night pass. Hostiles carry spawnLightThreshold 0 and the Skyreach is a
# NON-cave level, so its ambient light follows world time -- which means the
# whole hostile roster is unreachable in daylight and the sky reads as empty.
# Measuring the same probe at midnight is the only way to tell "the mobs are
# broken" apart from "the player was up there in the afternoon".
echo "Setting midnight and re-probing spawns..."
echo "time midnight" >&3
echo "skyreachstatus" >&3
wait_for "SKYREACH_STATUS_DONE" 180
# The Skyfall that was still running when the world was written: it has to come
# back knowing which tiles it wrote, and clearing them has to leave nothing.
echo "Finishing the restored Skyfall..."
echo "skysurfacestatus event" >&3
wait_for "SKYSURFACE_STATUS_DONE" 240
stop_server
LOG2="$WORK_DIR/server2.log"

# --- verification -------------------------------------------------------------
echo "--- verifying log ---"
STATUS=0
LOG="$LOG1"
grep -qE "Skyreach OK: class=SkyLevel" "$LOG1" || { echo "FAIL: SkyLevel was not instantiated"; STATUS=1; }
grep -qE "tile (cloudturftile|mistseatile)" "$LOG1" || { echo "FAIL: sky terrain did not generate"; STATUS=1; }
grep -qE "biome (driftlands|stormveil|aurorashoals|skyway)" "$LOG1" || { echo "FAIL: sky biomes did not paint"; STATUS=1; }
grep -qE "spirePlaced=true" "$LOG1" || { echo "FAIL: Warden's Spire was not stamped"; STATUS=1; }
grep -qE "beaconObject=wardenbeaconoff" "$LOG1" || { echo "FAIL: spire beacon object missing"; STATUS=1; }
# The way in. The Spire's doors sit on the axes through its origin, and the
# forecourt lamp ring once stood a candelabra on the south one, so a player
# arriving walked into a lamp instead of through the grand door.
grep -qE "entrance check: door=[a-z]*door .*isDoor=true" "$LOG1" || { echo "FAIL: no door in the middle of the spire's south wall"; STATUS=1; }
grep -qE "entrance check: .* clear=true" "$LOG1" || { echo "FAIL: something is standing in the spire's entrance approach"; STATUS=1; }
grep -qE "wardenFloor=marblecheckertile" "$LOG1" || { echo "FAIL: spire interior floor missing"; STATUS=1; }
grep -qE "npc check: wardens=1 cats=2" "$LOG1" || { echo "FAIL: Warden/cat NPCs not spawned exactly once"; STATUS=1; }
grep -qE "settler check: wardensettler=WardenSettler" "$LOG1" || { echo "FAIL: the recruited Warden is not a registered settler"; STATUS=1; }
grep -qE "Veil OK: class=VeilLevel" "$LOG1" || { echo "FAIL: VeilLevel was not instantiated"; STATUS=1; }
grep -qE "tile (murkmosstile|murkwatertile)" "$LOG1" || { echo "FAIL: Veil terrain did not generate"; STATUS=1; }
grep -qE "biome (gloomfen|ashenreach)" "$LOG1" || { echo "FAIL: Veil biomes did not paint"; STATUS=1; }

echo "--- verifying the harvest-tool audit ---"
# Every custom deco/prop object must report the tool type and HP decided in
# the audit (vanilla archetypes: flora/clutter ALL, trees AXE, stone/crystal
# PICKAXE, quest pieces UNBREAKABLE). See docs/TECHNICAL_LEARNINGS.md.
for expected in \
    "gloomwillow=AXE/100" "gloomshroom=ALL/1" "ashbones=ALL/50" "deadtree=AXE/100" \
    "skywatchtelescope=PICKAXE/100" "skywatchastrolabe=PICKAXE/100" \
    "stormscreed=ALL/1" "skywatchrubble=PICKAXE/100" "chargecrystal=PICKAXE/100" \
    "withershrub=ALL/1" "aurorashards=PICKAXE/100" "starfall=PICKAXE/100" \
    "skyballoon=ALL/100" "aeronautwreck=AXE/100" "skyparcel=ALL/1" \
    "wardenbeaconoff=UNBREAKABLE/100" "wardenbeaconon=UNBREAKABLE/100" "skyanchor=UNBREAKABLE/100"; do
    grep -qF "tool $expected" "$LOG1" || { echo "FAIL: tool audit expected $expected"; STATUS=1; }
done
# The snail must implement NetableMob — the marker the vanilla net checks.
grep -qF "net dewsnail=NETABLE" "$LOG1" || { echo "FAIL: dewsnail is not netable"; STATUS=1; }

echo "--- verifying the Cloud Lamb is a coherent husbandry animal ---"
# Three player questions, three measured values: what shearing yields, what the
# offspring is (vanilla SheepMob breeds a 50% chance of a plain `ram`), and what
# the feeding trough accepts (FeedingTroughObjectEntity filters on
# `instanceof GrainItem` and nothing else, so a berry that is not one can never
# go in the trough no matter what canFeed says).
grep -qE "husbandry check: cloudlamb shear=windsilkx[0-9]+" "$LOG1" \
    || { echo "FAIL: shearing a Cloud Lamb does not yield windsilk"; \
         grep -E "husbandry check:" "$LOG1" | tail -1; STATUS=1; }
grep -qF "child=cloudlamb" "$LOG1" \
    || { echo "FAIL: Cloud Lambs do not breed true (vanilla SheepMob rolls a ram)"; STATUS=1; }
grep -qF "cloudberry=hand:true/trough:true" "$LOG1" \
    || { echo "FAIL: cloudberries are not accepted as Cloud Lamb feed"; STATUS=1; }
grep -qF "wheat=hand:true/trough:true" "$LOG1" \
    || { echo "FAIL: vanilla wheat stopped working as feed"; STATUS=1; }
grep -qF "skystone=hand:false/trough:false" "$LOG1" \
    || { echo "FAIL: the feed check accepts things that are not food"; STATUS=1; }

echo "--- verifying the Warden's quest chain has no dead ends ---"
# Every reachable save state must be owed a chapter; only a finished chain may
# hand out nothing. The three historically dead states are named explicitly so a
# regression says which one broke rather than just "a state".
grep -qF "chain check:" "$LOG1" \
    || { echo "FAIL: the quest chain state probe never ran"; STATUS=1; }
grep -qF "no-dead-ends" "$LOG1" \
    || { echo "FAIL: a save state is owed no quest chapter at all"; \
         grep -E "chain check:" "$LOG1" | tail -1; STATUS=1; }
for expected in \
    "met-him-old-build=RECRUIT" "legacy-settler-no-record=CATS" \
    "both-cats-home-never-had-quest=CATS_TURNIN" "cats-paid-out=ANCHOR" \
    "anchored=DONE"; do
    grep -qF "$expected" "$LOG1" || { echo "FAIL: quest chain expected $expected"; \
        grep -E "chain check:" "$LOG1" | tail -1; STATUS=1; }
done

echo "--- verifying the cats have somewhere to come home to ---"
# The spire preset reserved the basket tile and put nothing on it, so "home"
# was a bare floor square. SkyLevel heals it onto existing worlds too.
grep -qE "cat home check: basket=-?[0-9]+,-?[0-9]+ object=catbasket" "$LOG1" \
    || { echo "FAIL: no cat basket stands on the tile the quest calls home"; \
         grep -E "cat home check:" "$LOG1" | tail -1; STATUS=1; }
grep -qF "cat coax: sent 2 cat(s) home" "$LOG1" \
    || { echo "FAIL: the travel-home path did not run for both cats"; \
         grep -E "cat coax:" "$LOG1" | tail -1; STATUS=1; }
COAXED="$(grep -E "cat home check:" "$LOG1" | tail -1)"
[ "$(echo "$COAXED" | grep -c AT_BASKET)" -ge 1 ] \
    || { echo "FAIL: a coaxed cat is not at its basket ($COAXED)"; STATUS=1; }
echo "$COAXED" | grep -q "AWAY_FROM_BASKET" \
    && { echo "FAIL: a cat is flagged home but is not at the basket ($COAXED)"; STATUS=1; }

echo "--- verifying the built landscape ---"
# The whole Skyreach comes out of one pure function
# (SkyTerrainPainter.describeTile), and the offline map renderer that worldgen
# is calibrated on calls that same function. If the field and the painted world
# ever disagree, every calibration render becomes fiction — so the oracle must
# match the real level exactly, outside the spire preset's own footprint.
grep -qE "painter oracle: tileMismatches=0 " "$LOG1" \
    || { echo "FAIL: the generated world does not match SkyTerrainPainter.describeTile"; \
         grep -E "painter oracle:" "$LOG1" | tail -1; STATUS=1; }

# The Skywatch roads, the designed places and the sky gates, counted in the
# world rather than predicted. The hub is a forced four-road junction with a
# railed forecourt, a chequered inlay ring and a lamp ring, so within the scan
# radius all of these are guaranteed to be non-zero for EVERY seed.
ROADS="$(grep -oE 'skyroads: paved=[0-9]+ chequer=[0-9]+ lamps=[0-9]+ fences=[0-9]+ gatewalls=[0-9]+' "$LOG1" | tail -1)"
if [ -z "$ROADS" ]; then
    echo "FAIL: no skyroads report — the built landscape never ran"; STATUS=1
else
    for field in paved chequer lamps fences gatewalls; do
        value="$(echo "$ROADS" | grep -oE "$field=[0-9]+" | cut -d= -f2)"
        [ "${value:-0}" -gt 0 ] || { echo "FAIL: built landscape has no $field ($ROADS)"; STATUS=1; }
    done
    LAMPS="$(echo "$ROADS" | grep -oE 'lamps=[0-9]+' | cut -d= -f2)"
    # The forecourt ring alone is six candelabra; fewer means the hub
    # composition did not stamp.
    [ "$LAMPS" -ge 6 ] || { echo "FAIL: the Warden's Forecourt lamp ring is missing ($ROADS)"; STATUS=1; }
fi
# A raw string ID here would mean the road paves itself with nothing.
grep -qE "roadtile=snowstonepathtile" "$LOG1" \
    || { echo "FAIL: the road paving material did not resolve"; STATUS=1; }
grep -qE "designed place: kind=[0-2] radius=[0-9]+" "$LOG1" \
    || { echo "FAIL: no designed place within three lattice cells of the hub"; STATUS=1; }

echo "--- verifying the Skyway Passages generate ---"
# Every piece of this biome was registered and reachable long before anything
# generated it, so "the tile exists" and "the statue is craftable" prove
# nothing. These are counts taken from the world the server actually painted.
grep -qE "skyway: ground=skywaytile " "$LOG1" \
    || { echo "FAIL: the Skyway ground tile did not resolve"; \
         grep -E "skyway:" "$LOG1" | tail -1; STATUS=1; }
SKYWAY="$(grep -oE 'skyway: ground=[a-z]+ tiles=[0-9]+ seraphtrees=[0-9]+ cloudtrees=[0-9]+ trees=[0-9]+ seraphstatues=[0-9]+ rails=[0-9]+ railgates=[0-9]+' "$LOG1" | tail -1)"
if [ -z "$SKYWAY" ]; then
    echo "FAIL: no skyway report — the Skyway Passages never ran"; STATUS=1
else
    # The scan is a fixed radius around the hub, and the hub is pulled into the
    # Driftlands band by construction, so a given seed may legitimately have no
    # Skyway within it. Ground and trees are asserted together: paving with no
    # Seraph on it would mean the biome generated its floor and nothing else.
    # Both species share the passages' tree band, so count them together.
    # The band is 0.022 and roughly 27% of tiles are claimed before it rolls,
    # so expected trees is about tiles * 0.016: below ~200 tiles of ground a
    # count of zero is ordinary sampling, not a broken biome. At 200 tiles the
    # expectation is about 3, so zero there does mean nothing is growing.
    SKYWAY_TILES="$(echo "$SKYWAY" | grep -oE ' tiles=[0-9]+' | cut -d= -f2)"
    SKYWAY_TREES="$(echo "$SKYWAY" | grep -oE ' trees=[0-9]+' | cut -d= -f2)"
    if [ "${SKYWAY_TILES:-0}" -ge 200 ] && [ "${SKYWAY_TREES:-0}" -eq 0 ]; then
        echo "FAIL: ${SKYWAY_TILES} tiles of Skyway paving and not one tree on it ($SKYWAY)"; STATUS=1
    fi
fi

echo "--- verifying the Surface points of interest ---"
# The three POIs are placed by vanilla's own world-preset system, so nothing
# here is asserted from source: the census reads the queue that system actually
# built for whole 1024x1024 preset regions, and the stamp lines count real
# objects in a world that was really generated.
#
# A preset names its pieces by string ID, and both ObjectRegistry.getObjectID
# and TileRegistry.getTileID answer -1 for a name they do not know, which
# Preset.setObject reads as "leave this cell alone". So one typo silently
# deletes part of a structure without failing anything. That is what
# unresolved=0 is guarding.
grep -qE "surface materials: named=[0-9]+ unresolved=0( |$)" "$LOG1" \
    || { echo "FAIL: a Surface POI names an object or tile that does not resolve"; \
         grep -E "surface materials:" "$LOG1" | tail -1; STATUS=1; }
grep -qE "surface loot: items=[0-9]+ unresolved=0( |$)" "$LOG1" \
    || { echo "FAIL: a Surface loot table names an item that does not exist"; \
         grep -E "surface loot:" "$LOG1" | tail -1; STATUS=1; }
# Clutter, not masonry: the shard must break with anything, like vanilla's own
# small ground debris (docs/IMPLEMENTATION_RULES.md rule 4).
grep -qF "surface tool skyfallshard=ALL/1" "$LOG1" \
    || { echo "FAIL: the Fallen Skyshard does not have clutter tool behaviour"; \
         grep -E "surface tool" "$LOG1" | tail -1; STATUS=1; }

CENSUS="$(grep -oE 'poi census: presetregions=[0-9]+ tilespan=[0-9]+x[0-9]+ CraterGeneration=[0-9]+ CampGeneration=[0-9]+ ShrineGeneration=[0-9]+ total=[0-9]+ perpresetregion=[0-9.]+' "$LOG1" | tail -1)"
if [ -z "$CENSUS" ]; then
    echo "FAIL: no poi census — the Surface POI world preset never ran"; STATUS=1
else
    for field in CraterGeneration CampGeneration ShrineGeneration; do
        value="$(echo "$CENSUS" | grep -oE "$field=[0-9]+" | cut -d= -f2)"
        [ "${value:-0}" -gt 0 ] || { echo "FAIL: no $field queued anywhere ($CENSUS)"; STATUS=1; }
    done
    # Rarity has to be a measured band, not a comment. Vanilla's own surface
    # list queues roughly 150-250 structures per preset region; ours is one
    # order of magnitude below that on purpose, and a change either way should
    # be deliberate rather than discovered in a playtest.
    PER="$(echo "$CENSUS" | grep -oE 'perpresetregion=[0-9.]+' | cut -d= -f2 | cut -d. -f1)"
    [ "${PER:-0}" -ge 5 ] || { echo "FAIL: Surface POIs are so rare they may as well not exist ($CENSUS)"; STATUS=1; }
    [ "${PER:-0}" -le 40 ] || { echo "FAIL: Surface POIs are no longer rare ($CENSUS)"; STATUS=1; }
fi

# Queued is an intention. These lines are the structure standing in the world.
for pair in CraterGeneration:aetheriumrock CampGeneration:aeronautwreck ShrineGeneration:seraphstatue; do
    kind="${pair%%:*}"
    signature="${pair##*:}"
    LINE="$(grep -oE "poi stamp: $kind queued=[0-9]+ generated=[0-9]+ placedcounter=[0-9]+ ${signature}objects=[0-9]+" "$LOG1" | tail -1)"
    if [ -z "$LINE" ]; then
        echo "FAIL: $kind was never stamped into the world"; STATUS=1; continue
    fi
    gen="$(echo "$LINE" | grep -oE 'generated=[0-9]+' | cut -d= -f2)"
    counter="$(echo "$LINE" | grep -oE 'placedcounter=[0-9]+' | cut -d= -f2)"
    found="$(echo "$LINE" | grep -oE "${signature}objects=[0-9]+" | cut -d= -f2)"
    [ "${gen:-0}" -ge 1 ] || { echo "FAIL: no $kind site could be generated ($LINE)"; STATUS=1; }
    [ "${counter:-0}" -eq "${gen:-0}" ] || { echo "FAIL: $kind generated $gen site(s) but stamped $counter ($LINE)"; STATUS=1; }
    [ "${found:-0}" -ge "${gen:-0}" ] || { echo "FAIL: $kind stamped but wrote no $signature ($LINE)"; STATUS=1; }
done

# A preset writes object IDs with the raw layer setter and never runs
# MultiTile.placeObject, so every half of a multi-tile piece has to be written
# by hand -- and Region.checkTilesGenerationValid DELETES a piece whose other
# halves are missing. These are the two multi-tile pieces the POIs use.
CRATER="$(grep -E "poi contents: CraterGeneration" "$LOG1" | tail -1)"
CAMP="$(grep -E "poi contents: CampGeneration" "$LOG1" | tail -1)"
SHRINE="$(grep -E "poi contents: ShrineGeneration" "$LOG1" | tail -1)"
for piece in stormcrystalx stormcrystalrx deadwoodchestx; do
    echo "$CRATER" | grep -qF "$piece" \
        || { echo "FAIL: the fallen sky fragment is missing $piece ($CRATER)"; STATUS=1; }
done
for piece in bigtentx bigtent2x bigtent3x bigtent4x aeronautwreckx skyballoonx skyparcelx oakchestx; do
    echo "$CAMP" | grep -qF "$piece" \
        || { echo "FAIL: the aeronaut camp is missing $piece ($CAMP)"; STATUS=1; }
done
for piece in cloudmarblewallx seraphstatuex wardencandelabrax; do
    echo "$SHRINE" | grep -qF "$piece" \
        || { echo "FAIL: the skyward shrine is missing $piece ($SHRINE)"; STATUS=1; }
done
# Loot has to land in the container, not just next to it: Preset.addInventory
# reaches the chest's ObjectEntity at stamp time and silently gives up if it is
# not there.
CHEST_ITEMS="$(echo "$CRATER" | grep -oE 'chestitems=[0-9]+' | cut -d= -f2)"
[ "${CHEST_ITEMS:-0}" -ge 2 ] \
    || { echo "FAIL: the crater's strongbox is empty ($CRATER)"; STATUS=1; }
# The signs are the only part of a preset that is not object IDs: the text is
# handed over by an addCustomApply hook at stamp time, and it must arrive
# translated rather than as a raw misc.<key>.
for pair in "aeronaut camp:$CAMP" "skyward shrine:$SHRINE"; do
    label="${pair%%:*}"
    line="${pair#*:}"
    echo "$line" | grep -q 'sign="' \
        || { echo "FAIL: the $label's sign carries no text ($line)"; STATUS=1; }
    echo "$line" | grep -q 'sign="misc\.' \
        && { echo "FAIL: the $label's sign shows a raw locale key ($line)"; STATUS=1; }
done

echo "--- verifying the Skyfall world event ---"
# Phase 1 left a shower RUNNING with shards on the ground, so the world was
# written mid-event.
SEED="$(grep -oE 'skyfall seed: remainingms=[0-9]+ placed=[0-9]+ live=[0-9]+ inworld=[0-9]+' "$LOG1" | tail -1)"
if [ -z "$SEED" ]; then
    echo "FAIL: the Skyfall never started"; STATUS=1
else
    seeded="$(echo "$SEED" | grep -oE ' placed=[0-9]+' | cut -d= -f2)"
    live="$(echo "$SEED" | grep -oE ' live=[0-9]+' | cut -d= -f2)"
    inworld="$(echo "$SEED" | grep -oE ' inworld=[0-9]+' | cut -d= -f2)"
    [ "${seeded:-0}" -gt 0 ] || { echo "FAIL: the Skyfall placed no shards ($SEED)"; STATUS=1; }
    [ "${live:-0}" -eq "${inworld:-0}" ] \
        || { echo "FAIL: the Skyfall's shard list disagrees with the world ($SEED)"; STATUS=1; }
fi
# ...and phase 2 has to restore that event, still knowing which tiles it wrote,
# and leave nothing behind when it ends. This is the whole "time-limited and
# self-cleaning" claim, checked across a save/load round trip rather than
# within one session.
RUN="$(grep -oE 'skyfall run: restored=(true|false) remainingms=[0-9]+ placed=[0-9]+ live=[0-9]+ inworld=[0-9]+' "$LOG2" | tail -1)"
CLEAN="$(grep -oE 'skyfall clean: cleared=[0-9]+ leftbehind=[0-9]+ over=(true|false)' "$LOG2" | tail -1)"
if [ -z "$RUN" ] || [ -z "$CLEAN" ]; then
    echo "FAIL: the restarted server never finished the Skyfall"; STATUS=1
else
    echo "$RUN" | grep -q "restored=true" \
        || { echo "FAIL: the running Skyfall did not survive a restart ($RUN)"; STATUS=1; }
    restored_live="$(echo "$RUN" | grep -oE ' live=[0-9]+' | cut -d= -f2)"
    [ "${restored_live:-0}" -gt 0 ] \
        || { echo "FAIL: the restored Skyfall forgot the shards it placed ($RUN)"; STATUS=1; }
    left="$(echo "$CLEAN" | grep -oE 'leftbehind=[0-9]+' | cut -d= -f2)"
    [ "${left:-1}" -eq 0 ] \
        || { echo "FAIL: the Skyfall left shards in the world forever ($CLEAN)"; STATUS=1; }
    echo "$CLEAN" | grep -q "over=true" \
        || { echo "FAIL: the Skyfall never ended ($CLEAN)"; STATUS=1; }
fi
# The schedule is a WorldData, so it has to be there and have picked a night.
grep -qE "skyfall schedule: day=[0-9]+ night=(true|false) nextday=[0-9]+" "$LOG1" \
    || { echo "FAIL: the Skyfall schedule never initialised"; \
         grep -E "skyfall schedule:" "$LOG1" | tail -1; STATUS=1; }

echo "--- verifying persistence across restart ---"
SPIRE1="$(grep -oE 'spire=-?[0-9]+,-?[0-9]+' "$LOG1" | tail -1)"
SPIRE2="$(grep -oE 'spire=-?[0-9]+,-?[0-9]+' "$LOG2" | tail -1)"
if [ -z "$SPIRE2" ]; then
    echo "FAIL: the restarted server never reported quest data"; STATUS=1
elif [ "$SPIRE1" != "$SPIRE2" ]; then
    echo "FAIL: spire moved across a restart ($SPIRE1 -> $SPIRE2)"; STATUS=1
fi
# The cats are only written to the save because canDespawn is false; if that
# ever flips they vanish here while catsSpawned stays true, so they never
# come back. That is exactly what this assertion is guarding.
grep -qE "npc check: wardens=1 cats=2" "$LOG2" \
    || { echo "FAIL: Warden or cats did not survive a save/load round trip"; STATUS=1; }
grep -qE "catsSpawned=true" "$LOG2" || { echo "FAIL: quest data did not persist"; STATUS=1; }
# The point of the coax pass: a cat brought home must still be AT THE BASKET
# after the world has been written to disk and read back. Its save home is a
# region it was never generated in, and its homesick tether has to be rebuilt
# around the basket by init() on load, not around the lair it came from.
HOME2="$(grep -E "cat home check:" "$LOG2" | tail -1)"
if [ -z "$HOME2" ]; then
    echo "FAIL: the restarted server never reported the cats' home"; STATUS=1
else
    echo "$HOME2" | grep -qF "homeFlags black=true tabby=true" \
        || { echo "FAIL: the cats' home flags did not survive the restart ($HOME2)"; STATUS=1; }
    echo "$HOME2" | grep -qF "object=catbasket" \
        || { echo "FAIL: the cat basket did not survive the restart ($HOME2)"; STATUS=1; }
    [ "$(echo "$HOME2" | grep -o AT_BASKET | wc -l)" -eq 2 ] \
        || { echo "FAIL: both cats should be at the basket after a restart ($HOME2)"; STATUS=1; }
    echo "$HOME2" | grep -q "STILL_WILD" \
        && { echo "FAIL: a cat forgot it was brought home ($HOME2)"; STATUS=1; }
    echo "$HOME2" | grep -q "WRONG_TETHER" \
        && { echo "FAIL: a cat's homesick tether does not point at the basket ($HOME2)"; STATUS=1; }
    echo "$HOME2" | grep -q "AWAY_FROM_BASKET" \
        && { echo "FAIL: a cat wandered out of the spire ($HOME2)"; STATUS=1; }
fi

for L in "$LOG1" "$LOG2"; do
    if grep -nE "Exception|ERROR|ModLoadException" "$L" | grep -vE "libraryPatches|SLF4J" > "$WORK_DIR/errors.txt"; then
        echo "FAIL: errors found in $(basename "$L"):"
        cat "$WORK_DIR/errors.txt"
        STATUS=1
    fi
done

if [ "$STATUS" -eq 0 ]; then
    echo "PASS: mod loads, Skyreach generates, world survives a restart, no errors."
    echo "--- skyreachstatus output ---"
    sed -n '/Skyreach OK/,/SKYREACH_STATUS_DONE/p' "$LOG1"
    echo "--- after restart ---"
    grep -E "quest: stage=|npc check:|settler check:|recruit check:|name check:|cat home check:|husbandry check:" "$LOG2"
    echo "--- spawn probe, midnight pass ---"
    awk '/Setting midnight|time midnight/{n=1} n && /spawn check:/' "$LOG2" | tail -13
    echo "--- surface POIs and the Skyfall ---"
    grep -E "surface registry:|surface materials:|surface loot:|poi census:|poi stamp:|poi contents:|skyfall seed:|skyfall schedule:" "$LOG1"
    grep -E "skyfall run:|skyfall clean:" "$LOG2"
    # Only after the logs have been read, and only on success: a failed run's
    # world and logs are the evidence for diagnosing it.
    rm -rf "$WORK_DIR"
fi
exit "$STATUS"
