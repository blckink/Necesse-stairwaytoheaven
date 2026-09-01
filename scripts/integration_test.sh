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

# Write the world through the `save` console command and WAIT for it, before
# ever sending `stop`.
#
# Vanilla's stop path loses a race that this test exists to be sure about.
# Server.stop (jar 1.3.2, Server.java:986-1011) sets stopCalled = true FIRST,
# prints "Starting world save", and only then builds the ServerSaveHandler --
# while Server.tick (Server.java:296-303) does
#     tickSaveHandler(stopCalled); if (stopCalled && saveHandler == null) privateStop();
# on the server thread. If constructing the handler takes longer than one tick,
# the server thread sees stopCalled with saveHandler still null and stops
# outright: the log shows "Starting world save" and then "Stopped server" in
# the same second, with no "Completed world save before stopping server", and
# the world file is left at the last AUTOSAVE. Everything after it is gone.
#
# Construction is not free -- RegionManager.getSaveHandler takes each level's
# entityManager lock and calls RegionFilesManager.getWorldRegion(.., true) for
# every loaded region -- so the more regions a run has loaded, the more reliably
# it loses. Observed exactly that: a run that had force-loaded the cats' basket
# regions on two levels lost it twice out of two, and the same build without
# that placement saved for 18 seconds and passed. `save` (SaveServerCommand)
# calls startFullSave WITHOUT stopCalled set, so nothing can stop underneath it,
# and it prints "Completed world save" when the write is really on disk.
#
# This is almost certainly the "known flaky persistence assertions" this test
# has had: the flakiness is the race, not the assertions.
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
        [ "$waited" -ge 180 ] && fail "timeout waiting for the world save to complete"
    done
}

stop_server() {
    save_world
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

# The cats' home, moved to the SURFACE. This is the player's own report --
# "Katzenbetten sollen in normalem Haus platziert werden koennen etc in der
# Stadt damit die Katzen dort wohnen ... ich habe beide gerade platziert und
# die sind weg" -- so the only assertion worth anything is that a cat actually
# MOVES. `basket` mode places a Cat Basket near the surface spawn through the
# same ObjectItem.onPlaceObject path a player's placement takes.
echo "Running skysurfacestatus basket (place a cat basket on the surface)..."
echo "skysurfacestatus basket" >&3
for _ in $(seq 1 120); do
    [ "$(grep -c SKYSURFACE_STATUS_DONE "$LOG")" -ge 3 ] && break
    sleep 2
done
# ...and re-probe, so the cat report is taken AFTER the move rather than before.
echo "Running skyreachstatus (cats should now live on the surface)..."
echo "skyreachstatus" >&3
for _ in $(seq 1 90); do
    [ "$(grep -c SKYREACH_STATUS_DONE "$LOG")" -ge 4 ] && break
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

echo "--- verifying the arsenal stream's enemies can actually be placed ---"
# Two failure modes this catches, and they look identical from a log:
#  1. the class inherits Mob's `isValidSpawnLocation` (which returns false), so
#     nothing a spawn table asks for is ever placed -- the probe prints
#     "INHERITS Mob's false" and both columns read 0;
#  2. the class implements it but still measures AMBIENT light, so it accepts
#     at midnight and rejects at noon -- lit=0, dark>0.
# Every arsenal enemy uses SkySpawnRules, so it must be `implemented` AND
# accept in DAYLIGHT (LOG1 is the daytime pass; LOG2 is midnight).
for arsenal_mob in rimesentry auroraflake fenwraith cindercantor; do
    grep -qE "spawn check: $arsenal_mob .*validSpawnLocation=implemented" "$LOG1" \
        || { echo "FAIL: $arsenal_mob does not implement isValidSpawnLocation"; STATUS=1; }
    grep -qE "spawn check: $arsenal_mob .*accepted lit=[1-9][0-9]*/" "$LOG1" \
        || { echo "FAIL: $arsenal_mob accepts no daylight spawn tile"; STATUS=1; }
    grep -qE "spawn check: $arsenal_mob .*dark=[1-9][0-9]*/" "$LOG2" \
        || { echo "FAIL: $arsenal_mob accepts no dark spawn tile"; STATUS=1; }
done
# The Beetle Outlands' ascended cast. These are our own classes wearing our own
# art, but each inherits its spawn rule from the vanilla mob it subclasses
# rather than declaring one, so only the live registry can prove the entries
# still place -- which is the whole reason they are probed.
#
# The expectation differs from the arsenal block above ON PURPOSE. Arsenal
# enemies use SkySpawnRules and must accept in DAYLIGHT. These three do not:
# HostileMob.isValidSpawnLocation calls checkLightThreshold, so they are
# dark-spawners, and asserting daylight here would be asserting a bug.
for outland_mob in crookedgolem rarecrookedgolem crookedarmadillo; do
    grep -qE "spawn check: $outland_mob .*validSpawnLocation=implemented" "$LOG1" \
        || { echo "FAIL: $outland_mob does not implement isValidSpawnLocation -- its Outlands spawn entry is inert"; STATUS=1; }
    grep -qE "spawn check: $outland_mob .*dark=[1-9][0-9]*/" "$LOG2" \
        || { echo "FAIL: $outland_mob accepts no dark spawn tile"; STATUS=1; }
done

# The Outlands' distance ramp, measured in the live world.
#
# The floor is asserted as an EXACT zero at 200, 600 and 850 tiles, not as
# "rare". "The spire's surroundings are safe" is a promise this mod makes out
# loud, and a promise that holds most of the time is a different promise.
grep -qE "outlands check: floor=900 " "$LOG1" \
    || { echo "FAIL: no outlands check line, or the 900-tile floor moved"; STATUS=1; }
# The floor, as the promise itself: zero wrong tiles anywhere in the disc
# inside 900, out of a land count that proves the sweep actually found ground.
grep -qE "outlands check: .* inside=0/[1-9][0-9]*" "$LOG1" \
    || { echo "FAIL: wrong ground appears inside the 900-tile floor (or the sweep found no land)"; STATUS=1; }
# ...and it must actually arrive further out, or the region is unreachable.
grep -qE "outlands check: .* r3200=[1-9][0-9]*/" "$LOG1" \
    || { echo "FAIL: no Outland ground at 3200 tiles -- the ramp never rises"; STATUS=1; }
grep -qE "outlands check: .* biome=NOT REGISTERED" "$LOG1" \
    && { echo "FAIL: the Outlands biome is not registered"; STATUS=1; }

# A4.1 -- guard, do not harass. Two halves, both asserted, because both are
# invisible in a five-minute play session and both are easy to break by a
# one-line edit somewhere else.
#
# The QUIET half: most of the sky must return zero spawn tickets, which is what
# makes walking between places calm. Asserted as a band rather than a point --
# under 60% and the drizzle the player complained about is back ("es nervt aber
# wenn die alle 2 Sekunden ueberall angreifen"), over 95% and the world is a
# museum. Measured 81-82% across runs.
grep -qE "pressure check: land=[1-9][0-9]* calm=[0-9]+\((6[0-9]|7[0-9]|8[0-9]|9[0-4])\.[0-9]%\)" "$LOG1" \
    || { echo "FAIL: calm share of the sky is outside 60-95% -- see the pressure check line"; STATUS=1; }
# ...and the wilds must not be empty, or "calm" has quietly become "dead".
grep -qE "pressure check: .* wilds=[1-9][0-9]*\(" "$LOG1" \
    || { echo "FAIL: no wild ground at all -- nothing spawns anywhere outside a site"; STATUS=1; }
# ...and guarded ground must exist, or there is nowhere for a pack to stand.
grep -qE "pressure check: .* guarded=[1-9][0-9]*" "$LOG1" \
    || { echo "FAIL: no guarded ground in the swept window"; STATUS=1; }

# The LOUD half: a real site must actually have guards standing on it. This is
# the assertion that catches the whole class of "placed by nothing" bugs this
# mod has shipped before -- the Cloud Lamb, the three workstations, the three
# sky oddities were all registered, correct and never put in the world.
grep -qE "guard check: site=NONE FOUND" "$LOG1" \
    && { echo "FAIL: no guarded site within 900 tiles of the spire"; STATUS=1; }
grep -qE "guard check: .* atSite=[1-9][0-9]*" "$LOG1" \
    || { echo "FAIL: a guarded site has no guards standing on it -- placeGuardPacks did nothing"; STATUS=1; }

# The Eden ground pair (supplied art, 2026-09-01): the tile and the seed must
# be live registrations, and the seed must accept Cloudturf -- the one override
# that makes it plantable in the sky at all.
grep -qE "eden check: tile=[1-9][0-9]* seed=[1-9][0-9]* name=.+ placesOnCloudturf=true" "$LOG1" \
    || { echo "FAIL: the Eden ground pair is not registered, or the seed rejects Cloudturf"; STATUS=1; }

# The realm field (WORLD_DESIGN section 3). Two things are asserted: that depth 0
# is Skyreach for every seed -- you always spawn at home -- and that the far end
# is Hell, so the progression spine actually spans the world rather than
# petering out. The realms between are noise-picked and vary by seed, so they
# are reported and not asserted.
grep -qE "realm check: scale=[0-9]+ .* 0=skyreach" "$LOG1" \
    || { echo "FAIL: depth 0 is not Skyreach -- the spawn realm moved"; STATUS=1; }
grep -qE "realm check: .* 5800=hell" "$LOG1" \
    || { echo "FAIL: the far end of the realm field is not Hell"; STATUS=1; }

# ...and the five weapons must be real registered items with a name, not IDs.
for arsenal_item in skyreave thunderhead prismcaller skywatchwhistle stormdisc; do
    grep -qE "arsenal check: $arsenal_item id=[0-9]+ name=[^ ]" "$LOG1" \
        || { echo "FAIL: arsenal item $arsenal_item did not register with a display name"; STATUS=1; }
done
grep -qE "arsenal check: recipes=5" "$LOG1" \
    || { echo "FAIL: the five arsenal recipes are not all registered"; STATUS=1; }
echo "--- verifying the Skywatch workstations a settler can be put on ---"
# A profession in Necesse is a workstation object plus recipes on its Tech.
# `SettlementStorageManager.assignWorkstation` gates on
# `instanceof SettlementWorkstationObject`, and a station whose Tech carries no
# recipes is assignable and has nothing to do — both fail silently in game.
# The expected products are named so a recipe that lands on the wrong Tech
# (or after the mod recipe registry closed) fails here instead of shipping.
for expected in \
    "workstation windsilkloom settlementWorkstation=true processing=false recipes=2" \
    "workstation aetherforge settlementWorkstation=true processing=true recipes=2" \
    "workstation stormglasskiln settlementWorkstation=true processing=true recipes=1"; do
    grep -qF "$expected" "$LOG1" || { echo "FAIL: workstation audit expected $expected"; \
        grep -E "^.*workstation " "$LOG1" | tail -3; STATUS=1; }
done
grep -qE "workstation windsilkloom .* makes=.*skyweavex1" "$LOG1" \
    || { echo "FAIL: the Windsilk Loom does not weave skyweave"; STATUS=1; }
grep -qE "workstation aetherforge .* makes=.*stormsteelbarx1" "$LOG1" \
    || { echo "FAIL: the Aether Forge does not make stormsteel"; STATUS=1; }
grep -qE "workstation stormglasskiln .* makes=.*stormglassx2" "$LOG1" \
    || { echo "FAIL: the Stormglass Kiln does not fire stormglass"; STATUS=1; }
grep -qF "makes=NOTHING" "$LOG1" \
    && { echo "FAIL: a workstation has no recipes on its tech at all"; STATUS=1; }
grep -qF "TECH_MISSING" "$LOG1" \
    && { echo "FAIL: a workstation's recipe tech was never registered"; STATUS=1; }

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

echo "--- verifying the three farmable sky animals ---"
# One line per animal, every value read off the mob the engine built.
#
# `mate=` is the one that cannot be inferred from the class hierarchy.
# Breeding is driven by the MALE: HusbandryImpregnateWandererAI only looks for
# a partner when canImpregnate() (grown, MALE, tame) and then requires
# canImpregnateMob(other) — which HusbandryMob returns false for, and which
# every vanilla male overrides with a hard test against a VANILLA string ID
# (RamMob accepts only "sheep", BullMob only "cow", RoosterMob only
# "chicken"). A modded species with no male of its own therefore breeds
# nothing at all, however correct everything else about it is.
grep -qE "husbandry check: nimbusyak shear=NO milk=nimbusmilkx[0-9]+ child=nimbusyak name=Nimbus Yak mate=nimbusyak" "$LOG1" \
    || { echo "FAIL: the Nimbus Yak is not a complete milk animal"; \
         grep -E "husbandry check: nimbusyak" "$LOG1" | tail -1; STATUS=1; }
grep -qE "husbandry check: thunderquill shear=stormdownx[0-9]+.* child=thunderquill name=Thunderquill Fowl mate=thunderquill" "$LOG1" \
    || { echo "FAIL: the Thunderquill Fowl is not a complete down animal"; \
         grep -E "husbandry check: thunderquill" "$LOG1" | tail -1; STATUS=1; }
grep -qE "husbandry check: glimmergoat shear=aurorafleecex[0-9]+.* child=glimmergoat name=Glimmergoat mate=glimmergoat" "$LOG1" \
    || { echo "FAIL: the Glimmergoat is not a complete fleece animal"; \
         grep -E "husbandry check: glimmergoat" "$LOG1" | tail -1; STATUS=1; }
# ...and each of them must be placeable by a biome spawn table, which a
# HusbandryMob is NOT by default: Mob.isValidSpawnLocation is `return false`
# and nothing in the husbandry chain overrides it. That is why the Cloud Lamb's
# own row above reads INHERITS, and why its biome entry did nothing for three
# releases. A table entry for a mob that answers false is indistinguishable
# from bad luck, so the override is asserted rather than trusted.
for animal in nimbusyak thunderquill glimmergoat; do
    grep -qE "spawn check: $animal .*validSpawnLocation=implemented" "$LOG1" \
        || { echo "FAIL: $animal inherits Mob's 'return false' and can never be table-spawned"; STATUS=1; }
done
# The override also has to ANSWER TRUE somewhere, or it is a check that always
# says no. All three share one predicate, so one accepting row is the whole bit.
grep -qE "spawn check: (nimbusyak|thunderquill|glimmergoat) .*accepted lit=[1-9]" "$LOG1" \
    || { echo "FAIL: no sky livestock accepted a single probe tile"; \
         grep -E "spawn check: (nimbusyak|thunderquill|glimmergoat)" "$LOG1" | tail -3; STATUS=1; }

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

echo "--- verifying a player-placed cat basket becomes the cats' home ---"
# The whole player report. A Cat Basket shipped for four releases as plain
# decoration: placing one did nothing at all, because the cats' home was
# hard-wired to the basket tile inside the Warden's Spire, in the Skyreach.
# "The feature compiles" proves nothing here -- only a cat that MOVED does.
FIRST="$(grep -E "cat basket place: step=first " "$LOG1" | tail -1)"
SECOND="$(grep -E "cat basket place: step=second " "$LOG1" | tail -1)"
BROKEOLD="$(grep -E "cat basket place: step=brokeold " "$LOG1" | tail -1)"
BROKEACTIVE="$(grep -E "cat basket place: step=brokeactive " "$LOG1" | tail -1)"
PLACE="$(grep -E "cat basket place: step=final " "$LOG1" | tail -1)"
# Rule 1: the first basket takes both cats off the Skyreach and onto the surface.
if [ -z "$FIRST" ]; then
    echo "FAIL: the first cat basket was never placed"; STATUS=1
else
    echo "$FIRST" | grep -q "surfacecats=2 skyreach2cats=0" \
        || { echo "FAIL: placing a basket did not bring both cats to the surface ($FIRST)"; STATUS=1; }
fi
# Rule 2 (the newest basket wins) and rule 3 (a basket that is NOT the recorded
# home may never evict anybody), then the release path: breaking the ACTIVE
# basket must send the cats back to the spire, across the dimension boundary the
# other way.
if [ -n "$SECOND" ]; then
    SECOND_AT="$(echo "$SECOND" | grep -oE ' at=surface:-?[0-9]+,-?[0-9]+' | sed 's/ at=//')"
    echo "$SECOND" | grep -qF "recordedHome=$SECOND_AT" \
        || { echo "FAIL: the newest basket did not win ($SECOND)"; STATUS=1; }
    echo "$BROKEOLD" | grep -qF "recordedHome=$SECOND_AT" \
        || { echo "FAIL: breaking a spare basket evicted the cats ($BROKEOLD)"; STATUS=1; }
    echo "$BROKEOLD" | grep -q "surfacecats=2" \
        || { echo "FAIL: breaking a spare basket moved the cats ($BROKEOLD)"; STATUS=1; }
    echo "$BROKEACTIVE" | grep -qF "recordedHome=NONE" \
        || { echo "FAIL: breaking the active basket did not clear the home ($BROKEACTIVE)"; STATUS=1; }
    echo "$BROKEACTIVE" | grep -q "surfacecats=0 skyreach2cats=2" \
        || { echo "FAIL: breaking the active basket did not send the cats back to the spire ($BROKEACTIVE)"; STATUS=1; }
fi
if [ -z "$PLACE" ]; then
    echo "FAIL: the cat basket was never placed on the surface"; STATUS=1
else
    echo "$PLACE" | grep -q "object=catbasket" \
        || { echo "FAIL: no cat basket stands on the surface tile it was placed on ($PLACE)"; STATUS=1; }
    BASKET_AT="$(echo "$PLACE" | grep -oE ' at=surface:-?[0-9]+,-?[0-9]+' | head -1 | sed 's/ at=//')"
    BASKET_HOME="$(echo "$PLACE" | grep -oE 'recordedHome=[a-z0-9+-]+:-?[0-9]+,-?[0-9]+' | sed 's/recordedHome=//')"
    [ -n "$BASKET_AT" ] \
        || { echo "FAIL: the basket was not placed on the surface ($PLACE)"; STATUS=1; }
    # Tile AND level: a home record that remembers only the tile is the bug.
    [ "$BASKET_AT" = "$BASKET_HOME" ] \
        || { echo "FAIL: placing a basket did not record it as the cats' home ($PLACE)"; STATUS=1; }
    # ...and the cats are actually standing on the surface now.
    HOME1="$(grep -E "cat home check:" "$LOG1" | tail -1)"
    echo "$HOME1" | grep -qF "home=$BASKET_HOME" \
        || { echo "FAIL: the cat probe does not report the placed basket as home ($HOME1)"; STATUS=1; }
    echo "$HOME1" | grep -qF "homeSource=placed" \
        || { echo "FAIL: the spire basket is still in effect after one was placed ($HOME1)"; STATUS=1; }
    echo "$HOME1" | grep -qF "homeObject=catbasket" \
        || { echo "FAIL: the recorded home tile carries no basket ($HOME1)"; STATUS=1; }
    [ "$(echo "$HOME1" | grep -o "on=surface" | wc -l)" -eq 2 ] \
        || { echo "FAIL: both cats should have moved to the surface basket ($HOME1)"; STATUS=1; }
    [ "$(echo "$HOME1" | grep -o AT_BASKET | wc -l)" -eq 2 ] \
        || { echo "FAIL: both cats should be at the surface basket ($HOME1)"; STATUS=1; }
    echo "$HOME1" | grep -q "WRONG_LEVEL" \
        && { echo "FAIL: a cat is on the wrong level for its home ($HOME1)"; STATUS=1; }
    echo "$HOME1" | grep -q "WRONG_TETHER" \
        && { echo "FAIL: a cat's tether does not point at the placed basket ($HOME1)"; STATUS=1; }
fi

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

# blockedbyspawn=N sits between tilespan and the per-kind counts: the census
# now runs vanilla's own near-spawn guard itself (see SkySurfaceStatusCommand)
# and reports how many queued POIs that guard drops. A pattern pinned to the
# old field order silently reports "the world preset never ran", which is the
# opposite of what happened -- so it is matched here rather than tolerated.
CENSUS="$(grep -oE 'poi census: presetregions=[0-9]+ tilespan=[0-9]+x[0-9]+ blockedbyspawn=[0-9]+ CraterGeneration=[0-9]+ CampGeneration=[0-9]+ ShrineGeneration=[0-9]+ total=[0-9]+ perpresetregion=[0-9.]+' "$LOG1" | tail -1)"
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
    # ...and the home is still the SURFACE basket, on the surface, after the
    # world has been written to disk and read back. This is the half a level
    # record could never carry: SkywatchQuestData is LevelData on the Skyreach,
    # so a home standing in a town on the Surface has to live in the world
    # record or it cannot survive at all.
    echo "$HOME2" | grep -qF "homeSource=placed" \
        || { echo "FAIL: the placed cat basket did not survive the restart ($HOME2)"; STATUS=1; }
    echo "$HOME2" | grep -qF "homeObject=catbasket" \
        || { echo "FAIL: the recorded home tile lost its basket across a restart ($HOME2)"; STATUS=1; }
    [ "$(echo "$HOME2" | grep -o "on=surface" | wc -l)" -eq 2 ] \
        || { echo "FAIL: both cats should still live on the surface after a restart ($HOME2)"; STATUS=1; }
    echo "$HOME2" | grep -q "WRONG_LEVEL" \
        && { echo "FAIL: a cat came back on the wrong level ($HOME2)"; STATUS=1; }
    # The tether has to be rebuilt around the SURFACE basket by init() on load.
    # A cat that comes back tethered to the spire tile it left would drift away
    # from the basket the moment the AI ran, which is what the 25s wander pass
    # above is for.
    HOME_TILE2="$(echo "$HOME2" | grep -oE ' home=[a-z0-9+-]+:-?[0-9]+,-?[0-9]+' | sed 's/ home=[a-z0-9+-]*://')"
    [ -n "$HOME_TILE2" ] \
        && [ "$(echo "$HOME2" | grep -o "tether=$HOME_TILE2" | wc -l)" -eq 2 ] \
        || { echo "FAIL: a cat's tether does not point at the placed basket after a restart ($HOME2)"; STATUS=1; }
    HOME_PLACE1="$(grep -E "cat home check:" "$LOG1" | tail -1 | grep -oE ' home=[a-z0-9+-]+:-?[0-9]+,-?[0-9]+')"
    HOME_PLACE2="$(echo "$HOME2" | grep -oE ' home=[a-z0-9+-]+:-?[0-9]+,-?[0-9]+')"
    [ "$HOME_PLACE1" = "$HOME_PLACE2" ] \
        || { echo "FAIL: the cats' home moved across a restart ($HOME_PLACE1 ->$HOME_PLACE2)"; STATUS=1; }
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
    awk '/Setting midnight|time midnight/{n=1} n && /spawn check:/' "$LOG2" | tail -20
    awk '/Setting midnight|time midnight/{n=1} n && /spawn check:/' "$LOG2" | tail -13
    echo "--- surface POIs and the Skyfall ---"
    grep -E "surface registry:|surface materials:|surface loot:|poi census:|poi stamp:|poi contents:|skyfall seed:|skyfall schedule:" "$LOG1"
    grep -E "skyfall run:|skyfall clean:" "$LOG2"
    echo "--- the cats moving into a player-placed basket ---"
    grep -E "cat coax:|cat basket place:|cat home check:" "$LOG1"
    # Only after the logs have been read, and only on success: a failed run's
    # world and logs are the evidence for diagnosing it. INTEGRATION_KEEP=1
    # keeps a successful run's world too, for probing output the summary above
    # does not print.
    if [ -z "${INTEGRATION_KEEP:-}" ]; then
        rm -rf "$WORK_DIR"
    else
        echo "kept work dir: $WORK_DIR"
    fi
fi
exit "$STATUS"
