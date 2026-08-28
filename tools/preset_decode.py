#!/usr/bin/env python3
"""Decode a Necesse preset share-code into readable layout data.

Necesse's in-game preset copy tool (`PresetCopyGameTool`) puts
`preset.getCompressedBase64Script()` on the clipboard, and `new Preset(String)`
parses it back. That makes a share-code a perfectly good way to hand a POI
design over — and this turns one into something we can actually study: the
size, the tile and object palettes, and an ASCII map per layer.

We adapt these rather than paste them: the layouts teach room sizes, door
placement, lighting rhythm and how furniture is grouped, and then the same
shapes get rebuilt from our own tiles and objects.

    python3 tools/preset_decode.py <file-with-code>          # or - for stdin
    python3 tools/preset_decode.py code.txt --map            # + ASCII maps
"""

import argparse
import base64
import re
import sys
import zlib
from collections import Counter


def decode(code):
    """Base64 (URL-safe or standard) then zlib.

    Tolerant on purpose: a code pasted through a chat client can lose its
    trailing bytes, and zlib.decompress then raises on the checksum even
    though the whole script has already been recovered. A decompressobj
    returns what it got, which is what we actually want.
    """
    code = "".join(code.split())
    code = code.replace("-", "+").replace("_", "/")
    raw = base64.b64decode(code + "=" * (-len(code) % 4))
    # Raw-deflate with the 2-byte zlib header skipped: no adler32 is verified,
    # so a code that lost bytes in transit still yields everything up to the
    # damage instead of raising and giving us nothing.
    for obj, data in ((zlib.decompressobj(), raw),
                      (zlib.decompressobj(-15), raw[2:])):
        try:
            text = obj.decompress(data)
            try:
                text += obj.flush()
            except zlib.error:
                pass
            if text:
                return text.decode("utf-8", "replace")
        except zlib.error:
            continue
    raise SystemExit("could not decompress - the code looks damaged; "
                     "re-copy it and make sure nothing was cut off")


def palette(script, key):
    """`<key>IDs = [id, name, id, name, ...]` -> {id: name}."""
    m = re.search(key + r"IDs\s*=\s*\[(.*?)\]", script, re.S)
    if not m:
        return {}
    parts = [p.strip() for p in m.group(1).split(",")]
    out = {}
    for i in range(0, len(parts) - 1, 2):
        try:
            out[int(parts[i])] = parts[i + 1]
        except ValueError:
            pass
    return out


def grid(script, key):
    m = re.search(key + r"\s*=\s*\[(.*?)\]", script, re.S)
    if not m:
        return []
    return [int(p) for p in m.group(1).split(",") if p.strip().lstrip("-").isdigit()]


def ascii_map(values, names, width, height):
    """One character per distinct id, so the shape of the layout is visible."""
    ids = [i for i, _ in Counter(v for v in values if v >= 0).most_common()]
    alphabet = ".:*#+=%@oO0xXvVwWsSnNmM"
    charof = {-1: " "}
    for n, i in enumerate(ids):
        charof[i] = alphabet[n] if n < len(alphabet) else "?"
    rows = []
    for y in range(height):
        rows.append("".join(charof.get(values[y * width + x], "?")
                            for x in range(width) if y * width + x < len(values)))
    legend = "  ".join(f"{charof[i]}={names.get(i, i)}" for i in ids)
    return rows, legend


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("src", help="file containing the share-code, or - for stdin")
    ap.add_argument("--map", action="store_true", help="also print ASCII layer maps")
    ap.add_argument("--out", help="write the raw decoded script here")
    a = ap.parse_args()

    code = sys.stdin.read() if a.src == "-" else open(a.src).read()
    # tolerate a pasted message: take the longest base64-looking run
    runs = re.findall(r"[A-Za-z0-9+/=_-]{80,}", code)
    if runs:
        code = max(runs, key=len)
    script = decode(code)
    if a.out:
        open(a.out, "w").write(script)

    m = re.search(r"width\s*=\s*(\d+).*?height\s*=\s*(\d+)", script, re.S)
    w, h = (int(m.group(1)), int(m.group(2))) if m else (0, 0)
    print(f"preset {w}x{h}  ({w * h} tiles)")

    for layer in ("tile", "object"):
        names = palette(script, layer)
        values = grid(script, layer + "s")
        used = Counter(v for v in values if v >= 0)
        print(f"\n{layer}s: {len(names)} in palette, {len(values)} cells, "
              f"{sum(used.values())} filled")
        for i, n in used.most_common():
            print(f"    {used[i]:5d}  {names.get(i, '?')}")
        if a.map and w and values:
            rows, legend = ascii_map(values, names, w, h)
            print("  legend:", legend)
            for r in rows:
                print("   |" + r + "|")


if __name__ == "__main__":
    main()
