#!/usr/bin/env python3
"""Turn CategoryCensus server output into the table in docs/ITEM_CATEGORIES.md.

The census (src/main/java/stairwaytoheaven/CategoryCensus.java) prints one
`swhcat item <id> kind=... cat=... craft=... obt=... creative=... listed=...
broker=... rarity=... loot=... recipe=...` line per mod item at server start.
Any server log that booted the mod has them -- scripts/integration_test.sh's
phase-1 log included.

    python3 tools/category_table.py AFTER.log [--before BEFORE.log] [--write]

With --before, a "vorher" column shows what changed. With --write, the table
between the two TABLE markers in docs/ITEM_CATEGORIES.md is replaced; without
it the table goes to stdout. This is a reading tool, not a gate: the gate is
the `swhcat census: ... bad=0` assertion in the integration test.
"""
import argparse
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOC = os.path.join(REPO, "docs", "ITEM_CATEGORIES.md")
START = "<!-- TABLE:START (tools/category_table.py) -->"
END = "<!-- TABLE:END -->"
LINE = re.compile(r"swhcat item (\S+) (.*)$")


def parse(path):
    rows = {}
    with open(path, encoding="utf-8", errors="replace") as f:
        for raw in f:
            m = LINE.search(raw.rstrip())
            if not m:
                continue
            fields = dict(kv.split("=", 1) for kv in m.group(2).split(" ") if "=" in kv)
            rows[m.group(1)] = fields
    return rows


KIND_DE = {"item": "Item", "object": "Objekt", "tile": "Boden", "mob": "Spawn-Item"}


def table(after, before):
    out = []
    head = "| ID | Art | Klasse | Kategorie (Inventar) | Crafting-Tab | erhältlich / Kreativ | Broker | Seltenheit | Rezept an |"
    sep = "|---|---|---|---|---|---|---|---|---|"
    if before is not None:
        head = head.replace("| Kategorie (Inventar) |", "| Kategorie vorher | Kategorie (Inventar) |")
        sep += "---|"
    out.append(head)
    out.append(sep)
    order = {"item": 0, "object": 1, "tile": 2, "mob": 3}
    for sid in sorted(after, key=lambda k: (order.get(after[k].get("kind"), 9), after[k].get("cat", ""), k)):
        r = after[sid]
        cat = r.get("cat", "")
        craft = r.get("craft", "")
        changed = False
        cells = [f"`{sid}`", KIND_DE.get(r.get("kind"), r.get("kind", "")), r.get("class", "")]
        if before is not None:
            b = before.get(sid, {})
            bcat = b.get("cat", "—")
            bcraft = b.get("craft", "—")
            changed = bcat != cat or bcraft != craft
            cells.append(bcat if bcat != cat else "")
        cells.append(f"**{cat}**" if changed else cat)
        cells.append(craft)
        cells.append(("ja" if r.get("obt") == "1" else "nein") + " / " + ("ja" if r.get("creative") == "1" else "nein")
                     + ("" if r.get("listed", "1") == "1" else " (Teilstück)"))
        cells.append(r.get("broker", ""))
        cells.append(r.get("rarity", ""))
        cells.append("" if r.get("recipe") == "-" else r.get("recipe", ""))
        out.append("| " + " | ".join(cells) + " |")
    return "\n".join(out)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("after")
    ap.add_argument("--before")
    ap.add_argument("--write", action="store_true")
    a = ap.parse_args()
    after = parse(a.after)
    if not after:
        sys.exit("no 'swhcat item' lines in " + a.after)
    before = parse(a.before) if a.before else None
    text = table(after, before)
    if not a.write:
        print(text)
        return
    doc = open(DOC, encoding="utf-8").read()
    if START not in doc or END not in doc:
        sys.exit("markers missing in " + DOC)
    head, rest = doc.split(START, 1)
    _, tail = rest.split(END, 1)
    open(DOC, "w", encoding="utf-8").write(head + START + "\n" + text + "\n" + END + tail)
    print(f"wrote {len(after)} rows to {os.path.relpath(DOC, REPO)}")


if __name__ == "__main__":
    main()
