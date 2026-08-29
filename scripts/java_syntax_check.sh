#!/usr/bin/env bash
# Java syntax gate for sessions with no game install.
#
# The mod compiles only against Necesse.jar / Server.jar, so `./gradlew
# buildModJar` cannot run without NECESSE_GAME_DIR. That leaves Java edits with
# NO automated check at all in such a session, which is how a stray brace or a
# wrong argument count reaches the user's build.
#
# javac parses before it resolves, so it reports syntax errors even when every
# `necesse.*` import is missing. This runs javac over the whole source tree and
# asserts that EVERY diagnostic is a symbol-resolution one — "package does not
# exist", "cannot find symbol", and the errors that cascade from them. Anything
# else is a real fault in our own code.
#
# WHAT THIS DOES NOT DO, and saying so is the point: it cannot check that a
# vanilla method exists, that its signature matches, or that an override is
# legal. It catches typos, not wrong assumptions. A change verified only by
# this is HYPOTHESIS, never VERIFIED — see docs/IMPLEMENTATION_RULES.md §14.
# When NECESSE_GAME_DIR is set, run `./gradlew buildModJar` instead; it is
# strictly stronger and this script says so and exits.
set -uo pipefail
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_DIR"

if [ -n "${NECESSE_GAME_DIR:-}" ]; then
    echo "NECESSE_GAME_DIR is set — run './gradlew buildModJar' instead; it checks"
    echo "everything this does and the vanilla API as well."
    exit 0
fi

OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
mapfile -t SRC < <(find src/main/java -name '*.java')
echo "javac over ${#SRC[@]} source files, no game jar on the classpath..."

javac -nowarn -proc:none -d "$OUT" "${SRC[@]}" 2>"$OUT/err.txt"

# Keep only real diagnostic lines ("path:line: error: message"), then drop the
# ones that are pure symbol resolution against the absent game jar.
REAL=$(grep -E '^[^ ].*:[0-9]+: error:' "$OUT/err.txt" \
     | grep -vE 'error: (package [a-zA-Z0-9_.]+ does not exist|cannot find symbol|cannot access [a-zA-Z0-9_.]+)' \
     || true)

if [ -n "$REAL" ]; then
    echo
    echo "SYNTAX / STRUCTURE ERRORS (not explained by the missing game jar):"
    echo
    echo "$REAL"
    echo
    echo "$(echo "$REAL" | wc -l) problem(s)."
    exit 1
fi

SYMBOLS=$(grep -cE '^[^ ].*:[0-9]+: error:' "$OUT/err.txt" || true)
echo "OK: every one of the $SYMBOLS diagnostics is a symbol that lives in the game"
echo "jar. No syntax or structure errors in our own code."
echo "This is a HYPOTHESIS-level check: it cannot see a wrong vanilla signature."
