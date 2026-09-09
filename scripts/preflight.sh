#!/usr/bin/env bash
# One command that answers "is the mod ready to play?".
#
# Runs every static gate plus the build, against the version that is actually
# played. Written after 2026-09-09, when a sprite with four empty variant cells
# reached both player profiles while every gate that was run came back green --
# because none of those gates looked at that file.
#
# Usage:
#   scripts/preflight.sh            # build against the PLAYED install (1.3.3)
#   scripts/preflight.sh --headless # build against the dedicated server (1.3.2)
#
# THIS SCRIPT DEPLOYS. `./gradlew buildModJar` runs deploySplitroast as part of
# the build, so a green default run has already copied the jar into both
# SplitRoast profiles -- that is what makes "ready to play" true rather than
# merely claimed. deploySplitroast refuses when the built game version does not
# match the played one, so it cannot replace a 1.3.3 install with a 1.3.2 jar.
#
# --headless therefore builds but does NOT deploy, and leaves a 1.3.2 jar in
# build/jar/. Run the script again without the flag to put the played 1.3.3 jar
# back.

set -u
REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_DIR" || exit 1

PLAYED_DIR="/mnt/c/Program Files (x86)/Steam/steamapps/common/Necesse"
HEADLESS_DIR="$HOME/dev/necesse-server-1-3-2-24650233/necesse-server-1-3-2-24650233"

if [ "${1:-}" = "--headless" ]; then
    export NECESSE_GAME_DIR="$HEADLESS_DIR"
else
    export NECESSE_GAME_DIR="$PLAYED_DIR"
fi

# Every python tool in this repo needs the unpacked Pillow; the system python
# has no PIL. ENVIRONMENT.md calls this mandatory.
export PYTHONPATH="${PYTHONPATH:-}${PYTHONPATH:+:}$HOME/dev/pylib"

if [ ! -d "$NECESSE_GAME_DIR" ]; then
    echo "preflight: NECESSE_GAME_DIR does not exist: $NECESSE_GAME_DIR" >&2
    exit 1
fi
echo "preflight: building against $NECESSE_GAME_DIR"
echo

fails=""
run() {
    local name="$1"; shift
    printf '=== %s\n' "$name"
    if "$@"; then
        printf '    OK\n\n'
    else
        printf '    FAILED\n\n'
        fails="$fails $name"
    fi
}

# The variant audit runs first: it is the cheapest, and a half-empty sprite
# makes everything after it moot.
run "variant_strip_audit"  python3 tools/variant_strip_audit.py
run "size_audit"           python3 tools/size_audit.py
run "sheet_format_audit"   python3 tools/sheet_format_audit.py
run "content_ledger"       python3 tools/content_ledger.py --check
run "buildModJar"          ./gradlew buildModJar -q

# locale_audit is reported but does not gate: it stands at 33 known problems
# (2026-09-09), so failing on it would make preflight permanently red and
# therefore useless. Fix the count down, then promote it into `run` above.
printf '=== locale_audit (report only)\n'
python3 tools/locale_audit.py 2>&1 | tail -1
printf '\n'

echo "=================================================="
if [ -n "$fails" ]; then
    echo "preflight: FAILED ->$fails"
    exit 1
fi
JAR=$(ls -1 build/jar/*.jar 2>/dev/null | head -1)
echo "preflight: all gates green"
[ -n "$JAR" ] && echo "preflight: jar $(basename "$JAR")"
echo "preflight: this says the mod is on-format and builds."
echo "preflight: it does NOT say the art reads -- only playing does that."
