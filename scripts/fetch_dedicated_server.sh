#!/usr/bin/env bash
# Fetch and unpack the Necesse DEDICATED SERVER, then point the build at it.
#
# WHY THIS EXISTS. Sessions kept stalling on "there is no game install here, so
# nothing can be compiled or tested" — and that was simply wrong. The dedicated
# server is a FREE public download from https://necessegame.com/server/; it
# needs no Steam account and no purchase. It carries Server.jar and a bundled
# jre, which is everything the build, the integration test, the headless sprite
# check, the offline painter AND `./gradlew decompileToSources` need.
#
# What it does NOT carry: sprites. A dedicated server never renders, so
# Server.jar ships zero PNGs. The vanilla sprite dump the art tooling wants
# (`wall_render_preview --vanilla stonewall`, size_audit's reference sheets)
# still has to come from a client install.
#
# Usage:
#   scripts/fetch_dedicated_server.sh [version] [install-dir]
#   version      dashed, e.g. 1-3-2 (default: the version in settings.gradle)
#   install-dir  default /opt/necesse-server
#
# Then, for the rest of the session:
#   export NECESSE_GAME_DIR=<the path this prints>
#   ./gradlew buildModJar
#   ./gradlew decompileToSources -PuseDecompiledSources=true
#   scripts/integration_test.sh
set -euo pipefail
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

WANT="${1:-}"
if [ -z "$WANT" ]; then
    # settings.gradle says gameVersion "auto" (it reads the version out of the
    # jar it finds), so the target version is only written down in the README.
    WANT="$(grep -oE 'game version \*\*[0-9]+\.[0-9]+\.[0-9]+\*\*' "$REPO_DIR/README.md" \
            | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1 | tr '.' '-')"
fi
[ -n "$WANT" ] || { echo "could not determine a version; pass one, e.g. 1-3-2" >&2; exit 1; }
DEST="${2:-/opt/necesse-server}"

TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
echo "Looking for the linux64 dedicated server, version $WANT ..."

# The download links on the page are S3 presigned URLs that expire in an hour,
# so they must be read fresh every time rather than hard-coded here.
curl -sS -L --max-time 120 https://necessegame.com/server/ -o "$TMP/page.html"
URL="$(grep -oE 'https://necesse-website[^"]*\.zip[^"]*' "$TMP/page.html" \
       | sed 's/&amp;/\&/g' | grep "linux64-${WANT}-" | head -1)"
if [ -z "$URL" ]; then
    echo "No linux64 build for version $WANT on that page. Available:" >&2
    grep -oE 'necesse-server-linux64-[0-9-]+\.zip' "$TMP/page.html" | sort -u | tail -12 >&2
    exit 1
fi

echo "Downloading ..."
curl -sS -L --max-time 600 -o "$TMP/server.zip" "$URL"
mkdir -p "$DEST"
unzip -q -o "$TMP/server.zip" -d "$DEST"

GAME_DIR="$(find "$DEST" -maxdepth 2 -name Server.jar -printf '%h\n' | head -1)"
[ -n "$GAME_DIR" ] || { echo "unpacked, but no Server.jar found under $DEST" >&2; exit 1; }

echo
echo "Server.jar is at $GAME_DIR"
[ -d "$GAME_DIR/jre" ] && echo "Bundled jre present — integration_test.sh will use it."
echo
echo "  export NECESSE_GAME_DIR=$GAME_DIR"
