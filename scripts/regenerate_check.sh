#!/usr/bin/env bash
# Proves `/swhreset regenerate confirm` on a real dedicated server.
#
# What it has to show (docs/SAVE_COMPAT.md, "regenerate"):
#   - the whole sky level is generated AGAIN: a marker placed far out in the
#     sky before the command is gone after it, and the region files the old sky
#     wrote went into the backup and off the save;
#   - the Warden's Spire stands again at the same seed-derived tile, the three
#     once-per-world landmarks are stamped again, and the realm-POI census still
#     reads kinds=30/30;
#   - quest PROGRESS is kept: the Warden recruited, the Skyreach key and its
#     portals, both cats coaxed home - and still kept after a restart;
#   - the surface is untouched: a marker near its spawn survives, and no
#     surface region / settlement / player file disappears;
#   - the dry runs change nothing (bare `regenerate`, and `quests` without
#     `confirm`, which used to be destructive - see SwhResetCommand).
#
# Phase 1 runs everything on a fresh world; phase 2 restarts on the same world.
#
# Requirements: NECESSE_GAME_DIR (dedicated server), ./gradlew buildModJar.
# Usage: scripts/regenerate_check.sh      (REGEN_KEEP=1 keeps the work dir)

set -u
REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
GAME_DIR="${NECESSE_GAME_DIR:?Set NECESSE_GAME_DIR to the dedicated server directory}"
MOD_DIR="$REPO_DIR/build/jar"
WORK_DIR="${REGEN_WORK_DIR:-$REPO_DIR/build/regenerate-check-$$}"
WORLD="regentest"
PORT="${REGEN_PORT:-$(( 17000 + $$ % 2000 ))}"
JAVA_BIN="$GAME_DIR/jre/bin/java"
[ -x "$JAVA_BIN" ] || JAVA_BIN="java"

ls "$MOD_DIR"/*.jar >/dev/null 2>&1 || { echo "FAIL: no mod jar in $MOD_DIR (run ./gradlew buildModJar)"; exit 1; }
rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR"
cd "$WORK_DIR"

LOG=""
SERVER_PID=""
STATUS=0

fail() {
    echo "FAIL: $1"
    tail -40 "$LOG" 2>/dev/null || true
    [ -n "$SERVER_PID" ] && kill "$SERVER_PID" 2>/dev/null
    exit 1
}
check() { # description, command...
    local what="$1"; shift
    if "$@"; then echo "ok:   $what"; else echo "FAIL: $what"; STATUS=1; fi
}
wait_for() { # pattern timeout
    local waited=0
    while ! grep -qE "$1" "$LOG" 2>/dev/null; do
        sleep 1; waited=$((waited + 1))
        kill -0 "$SERVER_PID" 2>/dev/null || fail "server exited early while waiting for: $1"
        [ "$waited" -ge "$2" ] && fail "timeout waiting for: $1"
    done
}
count() { grep -cE "$1" "$LOG" 2>/dev/null || true; }
# Sends a command and waits until its end marker has been printed once more.
run_cmd() { # command marker timeout
    local before; before="$(count "$2")"
    echo "> $1"
    echo "$1" >&3
    local waited=0
    while [ "$(count "$2")" -le "${before:-0}" ]; do
        sleep 1; waited=$((waited + 1))
        kill -0 "$SERVER_PID" 2>/dev/null || fail "server exited while running: $1"
        [ "$waited" -ge "$3" ] && fail "timeout running: $1"
    done
}
start_server() {
    LOG="$1"
    PIPE="$WORK_DIR/cmd.pipe"; rm -f "$PIPE"; mkfifo "$PIPE"
    unset JAVA_TOOL_OPTIONS
    "$JAVA_BIN" -Xms256m -Xmx2G -Djdk.attach.allowAttachSelf=true -jar "$GAME_DIR/Server.jar" -nogui -localdir \
        -world "$WORLD" -owner tester -port "$PORT" -mod "\"$MOD_DIR\"" < "$PIPE" > "$LOG" 2>&1 &
    SERVER_PID=$!
    exec 3> "$PIPE"
    wait_for "Stairway to Heaven" 120
    wait_for "Type help for list of commands|Server started|world loaded" 240
    sleep 3
}
save_world() {
    run_cmd "save" "Completed world save" 180
}
stop_server() {
    save_world
    echo "stop" >&3
    for _ in $(seq 1 30); do kill -0 "$SERVER_PID" 2>/dev/null || break; sleep 1; done
    kill "$SERVER_PID" 2>/dev/null
    exec 3>&-
    SERVER_PID=""
}
# The regenerate refuses on the console while the server ticks; an empty
# dedicated server pauses after 200 ticks and says so 100 ticks later.
wait_paused() {
    wait_for "Suggesting garbage collection due to empty server" 120
}
# Lists the entries of the save (folder or zip) under a prefix, one per line.
save_list() { # prefix
    python3 - "$WORK_DIR" "$WORLD" "$1" <<'PY'
import os, sys, zipfile
work, world, prefix = sys.argv[1:4]
hits = []
for root, dirs, files in os.walk(work):
    for f in files:
        if f == world + ".zip":
            with zipfile.ZipFile(os.path.join(root, f)) as z:
                for n in z.namelist():
                    rel = n.split("/", 1)[1] if "/" in n else n
                    if rel.startswith(prefix) and not n.endswith("/"):
                        hits.append(rel)
    if os.path.basename(root) == world and "world.dat" in files:
        for r2, d2, f2 in os.walk(root):
            for g in f2:
                rel = os.path.relpath(os.path.join(r2, g), root).replace(os.sep, "/")
                if rel.startswith(prefix):
                    hits.append(rel)
print("\n".join(sorted(set(hits))))
PY
}
field() { grep -oE "$1" "$LOG" | tail -1; }

# --- Phase 1 --------------------------------------------------------------
start_server "$WORK_DIR/phase1.log"
LOG1="$LOG"
run_cmd "skyreachstatus" "SKYREACH_STATUS_DONE" 240
sleep 5
run_cmd "skyreachstatus cats" "SKYREACH_STATUS_DONE" 240
run_cmd "swhreset fixture" "SWH_RESET_DONE" 180
run_cmd "swhreset fixture check" "SWH_RESET_DONE" 120
FIX_BEFORE="$(grep -E "regenerate fixture: skymarker=" "$LOG" | tail -1)"
PROG_BEFORE="$(grep -E "regenerate fixture: progress" "$LOG" | tail -1)"
SPIRE_BEFORE="$(field 'spire=-?[0-9]+,-?[0-9]+')"
save_world
SKY_REGIONS_BEFORE="$(save_list "levels/regions/skyreach2/" | grep -c . || true)"
save_list "levels/regions/surface/" > "$WORK_DIR/surface_regions_before.txt"
save_list "levels/settlements/" > "$WORK_DIR/settlements_before.txt"
save_list "players/" > "$WORK_DIR/players_before.txt"
echo "sky region files before: $SKY_REGIONS_BEFORE"

wait_paused
# Dry runs: both must change nothing.
run_cmd "swhreset quests" "SWH_RESET_DONE" 60
run_cmd "swhreset regenerate" "SWH_RESET_DONE" 60
DRY_START="$(grep -n "swhreset regenerate: NOTHING WAS CHANGED" "$LOG" | tail -1 | cut -d: -f1)"
run_cmd "swhreset fixture check" "SWH_RESET_DONE" 120
FIX_AFTER_DRY="$(grep -E "regenerate fixture: skymarker=" "$LOG" | tail -1)"
PROG_AFTER_DRY="$(grep -E "regenerate fixture: progress" "$LOG" | tail -1)"

# The real thing.
run_cmd "swhreset regenerate confirm" "SWH_RESET_DONE" 600
run_cmd "swhreset fixture check" "SWH_RESET_DONE" 120
FIX_AFTER="$(grep -E "regenerate fixture: skymarker=" "$LOG" | tail -1)"
PROG_AFTER="$(grep -E "regenerate fixture: progress" "$LOG" | tail -1)"
run_cmd "skyreachstatus" "SKYREACH_STATUS_DONE" 240
SPIRE_AFTER="$(field 'spire=-?[0-9]+,-?[0-9]+')"
run_cmd "skyreachstatus pois" "SKYREACH_STATUS_DONE" 600
# A second regenerate, with the cats living in a SURFACE town this time. They
# were never in the sky that goes away, so the new sky must not spawn a second
# pair (SkyLevel.spawnSpireCat).
run_cmd "skysurfacestatus basket" "SKYSURFACE_STATUS_DONE" 240
run_cmd "swhreset regenerate confirm" "SWH_RESET_DONE" 600
run_cmd "skyreachstatus" "SKYREACH_STATUS_DONE" 240
stop_server
SKY_REGIONS_AFTER="$(save_list "levels/regions/skyreach2/" | grep -c . || true)"
save_list "levels/regions/surface/" > "$WORK_DIR/surface_regions_after.txt"
save_list "levels/settlements/" > "$WORK_DIR/settlements_after.txt"
save_list "players/" > "$WORK_DIR/players_after.txt"
echo "sky region files after (new sky): $SKY_REGIONS_AFTER"

# --- Phase 2: restart on the same world -----------------------------------
start_server "$WORK_DIR/phase2.log"
LOG2="$LOG"
run_cmd "swhreset fixture check" "SWH_RESET_DONE" 180
FIX_P2="$(grep -E "regenerate fixture: skymarker=" "$LOG" | tail -1)"
PROG_P2="$(grep -E "regenerate fixture: progress" "$LOG" | tail -1)"
run_cmd "skyreachstatus" "SKYREACH_STATUS_DONE" 240
SPIRE_P2="$(field 'spire=-?[0-9]+,-?[0-9]+')"
stop_server

# --- Assertions -----------------------------------------------------------
echo "--- evidence ---"
echo "before:     $FIX_BEFORE"
echo "            $PROG_BEFORE"
echo "after dry:  $FIX_AFTER_DRY"
echo "            $PROG_AFTER_DRY"
grep -E "  (players moved home|backup|deleted|resident claims released|new spire|progress kept)|swhreset regenerate: (DONE|REFUSED|FAIL)" "$LOG1"
echo "after:      $FIX_AFTER"
echo "            $PROG_AFTER"
echo "restart:    $FIX_P2"
echo "            $PROG_P2"
grep -E "landmark stamps:" "$LOG1" | tail -1
grep -E "realmpoi census: " "$LOG1" | tail -1
echo "spire before=$SPIRE_BEFORE after=$SPIRE_AFTER restart=$SPIRE_P2"
echo "--- checks ---"

check "fixture placed both markers" \
    grep -qE "skymarker=.* present=1 surfacemarker=.* present=1" <<<"$FIX_BEFORE"
check "fixture recorded progress" \
    grep -qE "recruited=true .*wardenRecruited=true skyreachKey=true skyreachPortals=true" <<<"$PROG_BEFORE"
check "cats were coaxed home before" grep -qE "catsHome=11" <<<"$PROG_BEFORE"
check "'swhreset quests' without confirm changed nothing" \
    grep -qF "swhreset quests: NOTHING WAS CHANGED" "$LOG1"
check "bare 'swhreset regenerate' is a dry run" \
    grep -qF "swhreset regenerate: NOTHING WAS CHANGED" "$LOG1"
check "dry runs left the sky marker standing" grep -qE "skymarker=.* present=1" <<<"$FIX_AFTER_DRY"
check "dry runs left the progress alone" \
    grep -qE "recruited=true .*wardenRecruited=true skyreachKey=true" <<<"$PROG_AFTER_DRY"
check "regenerate ran to DONE" grep -qF "swhreset regenerate: DONE" "$LOG1"
check "regenerate wrote a backup" grep -qE "  backup: .*swh-sky-backups" "$LOG1"
BACKUP_DIR="$(grep -oE "  backup: .*" "$LOG1" | head -1 | sed "s/^  backup: //")"
BACKUP_REGIONS="$(find "$BACKUP_DIR/levels/regions/skyreach2" -type f 2>/dev/null | wc -l)"
echo "backup region files: $BACKUP_REGIONS (save had $SKY_REGIONS_BEFORE)"
check "backup holds every sky region file the save had" [ "$BACKUP_REGIONS" -eq "$SKY_REGIONS_BEFORE" ]
check "backup holds the level file" [ -f "$BACKUP_DIR/levels/skyreach2.dat" ]
check "regenerate deleted as many region files as the save had" \
    grep -qE "  deleted: $SKY_REGIONS_BEFORE region file" "$LOG1"
# Every run: the backup holds exactly the region files that run deleted, and
# nothing of the old sky was left on the save before the new one generated.
paste -d'|' <(grep -oE "  backup: .*" "$LOG1" | sed "s/^  backup: //") \
            <(grep -oE "  deleted: [0-9]+ region" "$LOG1" | grep -oE "[0-9]+") > "$WORK_DIR/backups.txt"
while IFS='|' read -r dir deleted; do
    n="$(find "$dir/levels/regions/skyreach2" -type f 2>/dev/null | wc -l)"
    check "backup $(basename "$dir") holds the $deleted region file(s) that run deleted (has $n)" [ "$n" -eq "$deleted" ]
done < "$WORK_DIR/backups.txt"
check "no old sky file was left on the save (both runs)" \
    [ "$(grep -cE "  sky files left on the save before regenerating: 0$" "$LOG1")" -eq 2 ]
check "the sky marker is gone (ground generated anew)" grep -qE "skymarker=.* present=0 " <<<"$FIX_AFTER"
check "the surface marker survived" grep -qE "surfacemarker=.* present=1" <<<"$FIX_AFTER"
check "new spire stamped with its beacon, at the same tile" \
    grep -qE "  new spire=${SPIRE_BEFORE#spire=} \(was ${SPIRE_BEFORE#spire=}\) beacon=1 " "$LOG1"
check "recruited world: the new spire is awake without a keeper" \
    grep -qE "  new spire=.* spireWardens=0 " "$LOG1"
check "landmarks re-stamped by the regenerate" grep -qE "  new spire=.* landmarks=3/3 " "$LOG1"
check "skyreachstatus: landmark stamps 3/3 after regenerate" \
    grep -qE "landmark stamps: 3/3 skywaytollhouse=1/magpiesettler=1 grangecellar=1/haldasettler=1 stormveiltestrange=1/ossiansettler=1" \
    <<<"$(awk '/swhreset regenerate: DONE/{n=1} n' "$LOG1")"
check "realm-POI census kinds=30/30 on the new sky" \
    grep -qE "realmpoi census: .* kinds=30/30 " <<<"$(awk '/swhreset regenerate: DONE/{n=1} n' "$LOG1")"
AFTER_STATUS="$(awk '/swhreset regenerate: DONE/{n=1} n' "$LOG1")"
check "cats: exactly two, both at the new spire basket (they were coaxed home)" \
    bash -c "grep -qE 'npc check: wardens=0 cats=2 ' <<<\"\$1\" \
        && [ \"\$(grep -E 'cat home check:' <<<\"\$1\" | head -1 | grep -o AT_BASKET | wc -l)\" -eq 2 ]" _ "$AFTER_STATUS"
SECOND="$(awk '/swhreset regenerate: DONE/{n++} n==2' "$LOG1")"
SECOND_CATS="$(grep -E 'cat home check:' <<<"$SECOND" | tail -1)"
echo "second regenerate (cats in a surface town): $(grep -E 'npc check:' <<<"$SECOND" | tail -1 | cut -c1-120)"
echo "  $SECOND_CATS" | cut -c1-400
check "second regenerate ran to DONE" [ "$(grep -c 'swhreset regenerate: DONE' "$LOG1")" -eq 2 ]
# npc check counts the sky AND the level of a placed basket, so 2 = no duplicate.
check "cats living on the surface are not duplicated in the new sky (2 in the world, both on=surface)" \
    bash -c "grep -qE 'npc check: wardens=0 cats=2 ' <<<\"\$1\" \
        && [ \"\$(grep -o 'on=surface' <<<\"\$2\" | wc -l)\" -eq 2 ] \
        && ! grep -q 'on=skyreach2' <<<\"\$2\"" _ "$SECOND" "$SECOND_CATS"
check "sky residents' claims were released and re-placed" \
    grep -qE "  resident claims released: .*magpiesettler" "$LOG1"
check "progress kept after regenerate" \
    grep -qE "stage=2 recruited=true catsHome=11 wardenRecruited=true skyreachKey=true skyreachPortals=true" <<<"$PROG_AFTER"
check "spire at the same tile after regenerate" [ "$SPIRE_BEFORE" = "$SPIRE_AFTER" ]
check "the new sky wrote region files" [ "${SKY_REGIONS_AFTER:-0}" -gt 0 ]
check "restart: sky marker still gone" grep -qE "skymarker=.* present=0 " <<<"$FIX_P2"
check "restart: surface marker still there" grep -qE "surfacemarker=.* present=1" <<<"$FIX_P2"
check "restart: progress kept" \
    grep -qE "stage=2 recruited=true catsHome=11 wardenRecruited=true skyreachKey=true skyreachPortals=true" <<<"$PROG_P2"
check "restart: spire at the same tile" [ "$SPIRE_BEFORE" = "$SPIRE_P2" ]
check "restart: landmark stamps 3/3" grep -qE "landmark stamps: 3/3 " "$LOG2"
check "surface: no region file disappeared" \
    bash -c "[ -z \"\$(comm -23 '$WORK_DIR/surface_regions_before.txt' '$WORK_DIR/surface_regions_after.txt')\" ]"
check "surface: settlement files unchanged" \
    cmp -s "$WORK_DIR/settlements_before.txt" "$WORK_DIR/settlements_after.txt"
check "players: no player file disappeared" \
    bash -c "[ -z \"\$(comm -23 '$WORK_DIR/players_before.txt' '$WORK_DIR/players_after.txt')\" ]"
for L in "$LOG1" "$LOG2"; do
    if grep -nE "Exception|ERROR|ModLoadException" "$L" | grep -vE "libraryPatches|SLF4J" > "$WORK_DIR/errors.txt"; then
        echo "FAIL: errors in $(basename "$L"):"; cat "$WORK_DIR/errors.txt"; STATUS=1
    fi
done

if [ "$STATUS" -eq 0 ]; then
    echo "PASS: /swhreset regenerate generated the sky anew, kept the progress, left the surface alone."
    [ -z "${REGEN_KEEP:-}" ] && rm -rf "$WORK_DIR"
else
    echo "kept work dir for diagnosis: $WORK_DIR"
fi
exit "$STATUS"
