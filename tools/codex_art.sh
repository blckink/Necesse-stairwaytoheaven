#!/usr/bin/env bash
# Run Codex image jobs, at most $SLOTS at a time, and log how long each took.
#
#   tools/codex_art.sh build/myround/a build/myround/b ...
#
# Each job directory holds brief.md (the job itself, one asset or one family)
# and optionally ref*.png (images handed to Codex). The runner prepends the
# pointer to docs/art/CODEX_ART.md, so the rules and lessons travel with every
# job, starts the jobs in the background and appends one line per job to
# build/codex_runs.tsv: start, minutes, job, result. "usage limit" in the log
# means the Codex quota is gone; the job is marked QUOTA and not retried.
#
# SLOTS defaults to 2: six parallel image_gen runs emptied the quota in four
# minutes on 2026-09-15. Review, verdicts and lessons go to docs/art/RUNS.md.
set -u
cd "$(dirname "$0")/.." || exit 1
CODEX=${CODEX:-/home/blackoffset/.npm-global/bin/codex}
SLOTS=${SLOTS:-2}
LEDGER=build/codex_runs.tsv
mkdir -p build
[ -f "$LEDGER" ] || printf 'start\tminutes\tjob\tresult\n' > "$LEDGER"

run_job() {
    local d=$1 imgs=() start end result
    for f in "$d"/ref*.png; do [ -f "$f" ] && imgs+=(-i "$f"); done
    rm -f "$d/codex_last.txt"
    start=$(date +%s)
    "$CODEX" exec -C "$PWD" -s workspace-write --skip-git-repo-check "${imgs[@]}" \
        -o "$d/codex_last.txt" \
        "Art job. Read docs/art/CODEX_ART.md first (rules and lessons), then the brief below. Write only into $d/.

$(cat "$d/brief.md")" > "$d/codex.log" 2>&1
    end=$(date +%s)
    # the log echoes the prompt, and CODEX_ART.md mentions "usage limit" -- match
    # only the real message
    if grep -q "hit your usage limit" "$d/codex.log"; then result=QUOTA
    elif [ -s "$d/codex_last.txt" ]; then result=done
    else result=FAILED; fi
    printf '%s\t%.1f\t%s\t%s\n' "$(date -d @"$start" '+%F %T')" \
        "$(echo "($end-$start)/60" | bc -l)" "$d" "$result" >> "$LEDGER"
}

for d in "$@"; do
    [ -f "$d/brief.md" ] || { echo "skip $d: no brief.md" >&2; continue; }
    while [ "$(jobs -rp | wc -l)" -ge "$SLOTS" ]; do wait -n; done
    run_job "$d" &
done
wait
tail -n "$#" "$LEDGER"
