#!/usr/bin/env bash
# Review sprite templates that another agent produced ON ANOTHER MACHINE.
#
# Codex (or anyone else) works in their own checkout. There is no shared
# filesystem — the git remote is the ONLY channel between that machine and this
# one. So the round trip is:
#
#   1. they push a branch carrying art-templates/
#   2. this script fetches it into a throwaway worktree, runs the audit against
#      it, and writes art-templates/REVIEW.md
#   3. the review is committed onto THEIR branch and pushed back, so the next
#      Codex run starts by reading its own fix list
#
# Nothing here notifies anybody. Whoever runs this has to know the branch exists;
# see docs/CODEX_SPRITE_TEMPLATE_BRIEF.md §7 for how that is signalled.
#
# Usage:  scripts/review_templates_branch.sh <branch> [--push]
set -euo pipefail
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_DIR"

BRANCH="${1:?usage: scripts/review_templates_branch.sh <branch> [--push]}"
PUSH="${2:-}"
WT="$(mktemp -d)/wt"

cleanup() { git worktree remove --force "$WT" >/dev/null 2>&1 || true; }
trap cleanup EXIT

echo "Fetching $BRANCH ..."
for i in 1 2 3 4; do
    git fetch origin "$BRANCH" && break
    echo "  retry $i"; sleep $((2 ** i))
done

# A worktree, not a checkout: reviewing must never disturb the branch this
# session is working on.
git worktree add --detach "$WT" "origin/$BRANCH"

echo "Auditing art-templates/ on $BRANCH ..."
set +e
# --root is load-bearing: template_audit derives its paths from its own file
# location, so without it this would audit THIS checkout and call the branch
# clean without ever looking at it.
mkdir -p "$WT/art-templates"
python3 "$REPO_DIR/tools/template_audit.py" --root "$WT" --review "$WT/art-templates/REVIEW.md"
STATUS=$?
set -e

if [ -f "$WT/art-templates/REVIEW.md" ]; then
    echo
    cat "$WT/art-templates/REVIEW.md"
fi

if [ "$PUSH" = "--push" ] && [ -f "$WT/art-templates/REVIEW.md" ]; then
    ( cd "$WT" \
      && git add art-templates/REVIEW.md \
      && git commit -q -m "Template review: $( [ $STATUS -eq 0 ] && echo 'clean' || echo 'fixes needed' )" \
      && git push origin "HEAD:$BRANCH" )
    echo
    echo "Review pushed onto $BRANCH. Tell Codex to pull and read art-templates/REVIEW.md."
fi

exit $STATUS
