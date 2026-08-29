#!/usr/bin/env python3
"""Content ledger: nothing reaches a player undescribed.

The rule this enforces was asked for directly — *"sauber dokumentiert jeder
Inhalt + Quest, Sprite etc der dazu kommt"* — and it is the one kind of
documentation that rots fastest, because writing it is never the interesting
part of the work.

So it is not remembered, it is derived. Every string ID the mod registers is
read out of the source by `tools/locale_audit.py`'s own `registered_ids()`, and
this checks each one against `docs/CONTENT_LEDGER.md`. Reusing that function
rather than re-implementing the scan is deliberate: two scanners drift, and the
one that drifts is always the one nobody runs.

    python3 tools/content_ledger.py            # report coverage
    python3 tools/content_ledger.py --check    # fail on anything undescribed
    python3 tools/content_ledger.py --scaffold # add empty rows for new IDs
    python3 tools/content_ledger.py --baseline # stamp today's IDs as pre-ledger

THE BASELINE. The mod already had ~200 registrations when the ledger was
introduced, described across CHANGELOG.md, CURRENT_STATE.md and the design docs.
Demanding a fresh row for each of them on day one would have produced 200 rows
of filler and a gate nobody could pass, so those IDs sit in a `Baseline` list
that `--check` exempts. **Everything registered after that must have a real
row.** A scaffolded row with no description does NOT pass — the tool will help
you write the ledger, it will not let you fake it.
"""
import argparse
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LEDGER = os.path.join(REPO, "docs", "CONTENT_LEDGER.md")
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import locale_audit  # noqa: E402

ROW = re.compile(r"^\|\s*`([^`]+)`\s*\|\s*([^|]*?)\s*\|\s*([^|]*?)\s*\|")
BASELINE_HEAD = "## Baseline — registered before the ledger existed"
TABLE_HEAD = ("| id | kind | what it is, in one line |\n"
              "|---|---|---|\n")


def registered():
    """{id: kind}, from the same scan the locale audit uses."""
    found = locale_audit.registered_ids(locale_audit.source_text())
    out = {}
    for kind, ids in sorted(found.items()):
        for i in sorted(ids):
            out.setdefault(i, kind)
    return out


def read_ledger():
    if not os.path.exists(LEDGER):
        return set(), {}, ""
    text = open(LEDGER, encoding="utf-8").read()
    baseline = set()
    if BASELINE_HEAD in text:
        tail = text.split(BASELINE_HEAD, 1)[1]
        # ONLY list items. A bare backtick scan also swallows the prose above
        # the list -- `CHANGELOG.md` and friends -- which inflates the count and,
        # worse, would exempt any future ID that happened to share a name with a
        # file mentioned in the explanation.
        baseline = set(re.findall(r"^- `([^`]+)`\s*$", tail, re.M))
    described = {}
    body = text.split(BASELINE_HEAD)[0]
    for line in body.split("\n"):
        m = ROW.match(line)
        if m:
            described[m.group(1)] = m.group(3).strip()
    return baseline, described, text


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true",
                    help="exit 1 if anything registered is undescribed")
    ap.add_argument("--scaffold", action="store_true",
                    help="append an empty row for each new ID, to be filled in")
    ap.add_argument("--baseline", action="store_true",
                    help="stamp every currently registered ID as pre-ledger")
    args = ap.parse_args()

    reg = registered()
    baseline, described, text = read_ledger()

    if args.baseline:
        ids = sorted(reg)
        head = ("# Content ledger\n\n"
                "Every string ID this mod registers, and one line saying what it is\n"
                "for a player. Enforced by `python3 tools/content_ledger.py --check`,\n"
                "which reads the registrations out of the source rather than trusting\n"
                "this file — so a new item cannot ship undescribed.\n\n"
                "Add the row in the SAME COMMIT that adds the content. Quests and\n"
                "sprites that carry no registration of their own go in the free-text\n"
                "sections at the bottom.\n\n"
                + TABLE_HEAD)
        tail = ("\n" + BASELINE_HEAD + "\n\n"
                "These predate the ledger and are described in `CHANGELOG.md`,\n"
                "`docs/CURRENT_STATE.md` and the design documents. `--check` exempts\n"
                "them. Anything registered from now on does not get that exemption.\n\n"
                + "\n".join("- `%s`" % i for i in ids) + "\n")
        with open(LEDGER, "w", encoding="utf-8") as fh:
            fh.write(head + tail)
        print("baseline stamped: %d IDs" % len(ids))
        return 0

    missing = {i: k for i, k in reg.items()
               if i not in baseline and not described.get(i)}

    if args.scaffold:
        if not missing:
            print("nothing to scaffold — every registered ID has a row.")
            return 0
        rows = "".join("| `%s` | %s |  |\n" % (i, k) for i, k in sorted(missing.items()))
        if TABLE_HEAD in text:
            text = text.replace(TABLE_HEAD, TABLE_HEAD + rows, 1)
        else:
            text = text.rstrip("\n") + "\n\n" + TABLE_HEAD + rows
        open(LEDGER, "w", encoding="utf-8").write(text)
        print("scaffolded %d empty row(s) — an empty description still fails "
              "--check, so fill them in." % len(missing))
        return 0

    print("%d registered ID(s); %d baseline, %d described, %d undescribed."
          % (len(reg), len(baseline), len(described), len(missing)))
    if args.check and missing:
        print("\nUNDESCRIBED CONTENT:\n")
        for i, k in sorted(missing.items()):
            print("  - `%s` (%s)" % (i, k))
        print("\n%d thing(s) a player can meet with no line saying what they are."
              "\nRun --scaffold, then write the descriptions." % len(missing))
        return 1
    if args.check:
        print("OK: nothing registered is undescribed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
