#!/usr/bin/env python3
"""The machine-readable version of docs/ASSET_REQUESTS.md.

That file is the player's shopping list: for every realm, every sprite that is
still a borrowed vanilla stand-in, the exact size the replacement must land at,
and the context to draw it from. It is prose with tables, written for a human.
This turns it into rows a pipeline can iterate, and -- the part that matters --
**checks each row against reality** instead of trusting it:

  * the vanilla stand-in must exist in the sprite dump,
  * its real pixel size must match the size the table promises,
  * a row already marked DONE is dropped, so finished work never comes back.

A row whose file is missing or whose size disagrees is reported as `broken`
rather than silently skipped: the table is hand-maintained, and a wrong number
there becomes a wrongly-sized sprite three steps later.

Usage:
    PYTHONPATH=/home/blackoffset/dev/pylib python3 tools/asset_worklist.py
    ... --json                 machine-readable, for the pipeline
    ... --realm eden           one realm only
    ... --limit 5              the next five (the batch size the player asked for)
    ... --include-broken       show the rows that failed their check too
"""
import argparse
import json
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REQUESTS = os.path.join(REPO, "docs", "ASSET_REQUESTS.md")
# The vanilla sprite dump. The dedicated server ships no PNGs at all, so this
# client-side dump is the only source of an original to draw on.
VANILLA = os.environ.get(
    "NECESSE_VANILLA_SPRITES", "/home/blackoffset/dev/Necesse sprites")

# "384×320", "382x320", "**192×32**" -- the table uses the multiplication sign.
SIZE_RE = re.compile(r"(\d+)\s*[×x]\s*(\d+)")
# A finished row is struck through and carries a DONE stamp.
DONE_RE = re.compile(r"\bDONE\b", re.IGNORECASE)


def _dump_index(_cache={}):
    """Index every PNG in the dump by its separator-stripped path.

    docs/ASSET_REQUESTS.md writes `tiles/sandsplat.png` where the dump has
    `tiles/sand_splat.png`: the prose drops the underscores the filenames
    carry. Rather than special-casing "splat", match on the name with all
    underscores and hyphens removed -- that resolves every such row, and an
    ambiguous key is reported instead of guessed.
    """
    if _cache:
        return _cache
    for root, _, files in os.walk(VANILLA):
        for f in files:
            if not f.lower().endswith(".png"):
                continue        # skips the :Zone.Identifier ADS files too
            rel = os.path.relpath(os.path.join(root, f), VANILLA)
            _cache.setdefault(_key(rel), []).append(rel)
    return _cache


def _key(path):
    return path.lower().replace("_", "").replace("-", "").replace(os.sep, "/")


def resolve_vanilla(rel):
    """Return (relative_path, note) for a stand-in named in the table."""
    if os.path.isfile(os.path.join(VANILLA, rel)):
        return rel, None
    hits = _dump_index().get(_key(rel), [])
    if len(hits) == 1:
        return hits[0], "resolved '%s' -> '%s'" % (rel, hits[0])
    if len(hits) > 1:
        return None, "ambiguous in the dump: %s" % ", ".join(sorted(hits))
    return None, "vanilla sprite not in the dump: %s" % rel


def clean(cell):
    """Strip the markdown a human wrote for a human."""
    s = cell.strip()
    s = re.sub(r"!?\[([^\]]*)\]\([^)]*\)", r"\1", s)   # links
    s = re.sub(r"[*_`~]+", "", s)                       # emphasis, code, strike
    return re.sub(r"\s+", " ", s).strip()


def parse_rows(text):
    """Walk the file, remembering which ## realm heading we are under."""
    realm = None
    header = None
    for line in text.splitlines():
        h = re.match(r"^##\s+(.*)", line)
        if h:
            realm = clean(h.group(1))
            header = None
            continue
        if not line.startswith("|"):
            header = None
            continue
        cells = [c for c in line.strip().strip("|").split("|")]
        if set("".join(cells).strip()) <= set("-: "):
            continue                      # the |---|---| separator
        if header is None:
            header = [clean(c).lower() for c in cells]
            continue
        if len(cells) != len(header):
            continue
        row = dict(zip(header, cells))
        if "id" in row and "current stand-in" in row:
            row["_realm"] = realm
            yield row


def build(include_broken=False):
    with open(REQUESTS, encoding="utf-8") as f:
        text = f.read()

    out, broken, done = [], [], 0
    for raw in parse_rows(text):
        what_raw = raw.get("what", "")
        if DONE_RE.search(what_raw) or DONE_RE.search(raw.get("current stand-in", "")):
            done += 1
            continue

        standin = clean(raw["current stand-in"])
        # The cell often carries a parenthetical caveat after the path.
        m = re.search(r"([A-Za-z0-9_/]+\.png)", standin)
        size_m = SIZE_RE.search(raw.get("size", ""))

        item = {
            "id": clean(raw["id"]).split()[0] if clean(raw["id"]) else "",
            "what": clean(what_raw),
            "realm": raw["_realm"],
            "standin": m.group(1) if m else None,
            "declared_size": [int(size_m.group(1)), int(size_m.group(2))] if size_m else None,
            "notes": clean(raw.get("format notes", "")),
        }

        problems = []
        if not item["standin"]:
            problems.append("no vanilla png named in 'current stand-in'")
        if not item["declared_size"]:
            problems.append("no WxH in 'size'")

        if item["standin"]:
            rel, note = resolve_vanilla(item["standin"])
            if rel is None:
                problems.append(note)
            else:
                if note:
                    item["resolved_note"] = note
                item["standin"] = rel
                src = os.path.join(VANILLA, rel)
                item["vanilla_path"] = src
                try:
                    from PIL import Image
                    with Image.open(src) as im:
                        item["actual_size"] = list(im.size)
                        item["mode"] = im.mode
                    if item["declared_size"] and item["actual_size"] != item["declared_size"]:
                        problems.append(
                            "size mismatch: table says %dx%d, file is %dx%d"
                            % (*item["declared_size"], *item["actual_size"]))
                except ImportError:
                    problems.append("Pillow missing (PYTHONPATH=/home/blackoffset/dev/pylib)")
                except Exception as exc:
                    problems.append("unreadable: %s" % exc)

        if problems:
            item["problems"] = problems
            broken.append(item)
            if include_broken:
                out.append(item)
        else:
            out.append(item)

    return out, broken, done


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--json", action="store_true", help="emit JSON for the pipeline")
    ap.add_argument("--realm", help="substring match on the realm heading")
    ap.add_argument("--limit", type=int, help="only the first N rows")
    ap.add_argument("--include-broken", action="store_true",
                    help="also list rows that failed their own check")
    args = ap.parse_args()

    rows, broken, done = build(args.include_broken)
    if args.realm:
        needle = args.realm.lower()
        rows = [r for r in rows if needle in (r["realm"] or "").lower()]
    if args.limit:
        rows = rows[:args.limit]

    if args.json:
        json.dump({"open": rows, "broken": broken, "done": done}, sys.stdout, indent=1)
        print()
        return 0

    print("%d open, %d already done, %d rows failed their own check"
          % (len(rows), done, len(broken)))
    realm = None
    for r in rows:
        if r["realm"] != realm:
            realm = r["realm"]
            print("\n== %s ==" % realm)
        size = "x".join(map(str, r.get("actual_size") or r.get("declared_size") or []))
        print("  %-18s %-10s <- %s" % (r["id"], size, r["standin"]))
        if r.get("problems"):
            for p in r["problems"]:
                print("      ! %s" % p)
    if broken and not args.include_broken:
        print("\n%d row(s) need a fix in docs/ASSET_REQUESTS.md "
              "(--include-broken to see them):" % len(broken))
        for b in broken:
            print("  %-18s %s" % (b["id"] or "(no id)", "; ".join(b["problems"])))
    return 0


if __name__ == "__main__":
    sys.exit(main())
