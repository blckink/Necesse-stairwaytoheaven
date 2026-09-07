#!/usr/bin/env bash
# Reads the mod's shipped enemy statline OUT OF THE BUILT JAR and holds it
# against the expected table in scripts/BalanceCheck.java.
#
# Why it exists: "the enemies are harder now" is not a claim anybody can check.
# docs/BALANCE.md is a target document — it says what a mob SHOULD be — and
# nothing before this script tested whether the classes agree with it. A
# rebalance is exactly the kind of change that half-lands: one realm's tier
# holder gets the new multiplier, another realm's mobs still carry the literal
# they were written with, and both compile. This reads the numbers the players'
# jar actually ships and fails on any row that disagrees.
#
# What it asserts, per mob and per boss:
#   1. the value in the jar EQUALS the expected new value,
#   2. the value in the jar is STRICTLY ABOVE the value it replaced, and
#   3. per role, HP rises monotonically outwards across the five realm bands —
#      "damit die Kurve nicht kippt", measured rather than asserted.
#
# WHAT IT DOES NOT PROVE, and saying so is the point:
#   * It reads STATIC FIELDS, not a live mob. Instantiating a Necesse mob
#     headlessly fails (the constructor wants an initialised engine), so this
#     cannot witness that a mob's AI tree was handed the AGGRO_RANGE constant
#     rather than a literal left behind next to it. The grep gate below closes
#     that specific gap mechanically: it fails if any realm mob's chaser tree
#     still carries a numeric range literal.
#   * It says nothing about whether the new numbers are FUN. That is
#     docs/PLAYTEST_LOG.md's job and needs the real client. This is
#     VERIFIED [jar] / VERIFIED [run] — never player-confirmed.
#
# Requirements: NECESSE_GAME_DIR (contains Server.jar) and a built mod jar in
# build/jar. Run ./gradlew buildModJar first — this deliberately does NOT build,
# so that it always reads the jar that was actually shipped.
set -eu
REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
GAME_DIR="${NECESSE_GAME_DIR:?Set NECESSE_GAME_DIR to a Necesse directory containing Server.jar}"
MOD_JAR="$(ls "$REPO_DIR"/build/jar/*.jar 2>/dev/null | head -1)"
[ -n "$MOD_JAR" ] || { echo "FAIL: no mod jar in build/jar (run ./gradlew buildModJar)"; exit 1; }

echo "balance_check: $(basename "$MOD_JAR")  ($(date -r "$MOD_JAR" '+%Y-%m-%d %H:%M'))"

# ---- gate 1: no chaser tree still carries a numeric range literal ----------
# Every realm enemy that builds its own chaser tree must pass AGGRO_RANGE as the
# range argument. The range is argument 2 of every chaser constructor in the
# game (VERIFIED [jar], javap -c on CollisionPlayerChaserWandererAI,
# PlayerChaserWandererAI and CollisionShooterPlayerChaserWandererAI), so a
# digit directly after the first comma is a range that was never lifted.
STRAY="$(grep -rnE '(Confused)?(Collision)?(Shooter)?PlayerChaser(Circling)?(Wanderer)?AI<[^>]*>\((null|\(\) -> false), [0-9]+,' \
        "$REPO_DIR"/src/main/java/stairwaytoheaven/mobs \
        "$REPO_DIR"/src/main/java/stairwaytoheaven/arsenal \
        "$REPO_DIR"/src/main/java/stairwaytoheaven/realms 2>/dev/null \
    | grep -v 'PlayerChargingCirclingChaserAI' || true)"
if [ -n "$STRAY" ]; then
    echo
    echo "FAIL: a chaser tree carries a numeric aggression range instead of AGGRO_RANGE:"
    echo "$STRAY"
    echo
    echo "Two mobs are allowed to: MistserpentHead (PlayerChargingCirclingChaserAI,"
    echo "2560 = 80 tiles) and TonguePlantMob (960 = 30 tiles). Both already see"
    echo "further than a player can react to, so lifting them changes nothing but"
    echo "the table. Both are excluded above by name/idiom, not by accident."
    exit 1
fi
echo "balance_check: no stray range literal in any chaser tree."
echo

# ---- gate 2: the numbers in the jar against the table ---------------------
OUT="$REPO_DIR/build/balance-check"
rm -rf "$OUT"
mkdir -p "$OUT"

unset JAVA_TOOL_OPTIONS
CP="$GAME_DIR/Server.jar:$MOD_JAR"
# The game's bundled JRE ships no compiler, so build with the toolchain javac
# and run on whichever java is on PATH — the same split scripts/tile_sprite_check.sh
# uses and for the same reason.
javac -nowarn -cp "$CP" -d "$OUT" "$REPO_DIR/scripts/BalanceCheck.java"
java -cp "$CP:$OUT" BalanceCheck
