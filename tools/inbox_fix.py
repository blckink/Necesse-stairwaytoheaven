#!/usr/bin/env python3
"""Fit everything dropped in art-inbox/ to the sprite it is meant to replace.

This is the one command the GitHub Action runs, and the whole point is that
you never have to tell it anything. Drop `gloomshade.png` in `art-inbox/` from
your phone and it works out on its own that the target is 384x256, because
that is what `src/main/resources/mobs/gloomshade.png` already is.

    python3 tools/inbox_fix.py                 # report only
    python3 tools/inbox_fix.py --apply         # also write into the mod

How the target is resolved, in order:

  1. The file's own name against every sprite the mod ships. `cloudturf_splat`
     resolves to `tiles/cloudturf_splat.png`.
  2. `<name>-new-<ourname>` -- new art drawn on a vanilla sheet, the naming
     the kk-sprites folder already uses.
  3. Nothing. It is reported as unresolved and left alone, because guessing
     which sprite a file replaces is exactly how art gets overwritten.

The SIZE decides the class, never the folder a file sits in:

  224 wide, height a multiple of 96   ground splat  -> 2x2 snapped
  384x320 / 384x256 / 192x128         mob sheet     -> NOT snapped, a
                                                       silhouette must stay sharp
  everything else                                   -> fitted and quantised only
"""
import argparse
import os
import subprocess
import sys

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "src", "main", "resources")
INBOX = os.path.join(REPO, "art-inbox")
OUT = os.path.join(REPO, "build", "qa", "inbox")


def shipped_sprites():
    """Every PNG the mod ships, by basename -> [paths]."""
    found = {}
    for root, _dirs, files in os.walk(RES):
        for f in files:
            if f.endswith(".png"):
                found.setdefault(f[:-4], []).append(os.path.join(root, f))
    return found


def resolve(name, shipped):
    if name in shipped:
        return shipped[name]
    if "-new-" in name:
        ours = name.split("-new-", 1)[1]
        if ours in shipped:
            return shipped[ours]
    return []


def is_splat(w, h):
    return w == 224 and h % 96 == 0


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--apply", action="store_true",
                    help="write the fitted file over the sprite it resolved to")
    ap.add_argument("--colours", type=int, default=32)
    args = ap.parse_args()

    if not os.path.isdir(INBOX):
        print("no art-inbox/ -- nothing to do")
        return 0
    files = sorted(f for f in os.listdir(INBOX) if f.lower().endswith(".png"))
    if not files:
        print("art-inbox/ is empty -- nothing to do")
        return 0

    shipped = shipped_sprites()
    os.makedirs(OUT, exist_ok=True)
    done, unresolved = [], []

    for f in files:
        src = os.path.join(INBOX, f)
        name = f[:-4]
        targets = resolve(name, shipped)
        print("\n=== %s" % f)
        if not targets:
            print("  UNRESOLVED: no shipped sprite called '%s'." % name)
            print("  Rename it after the sprite it replaces, or after the vanilla")
            print("  sheet it was drawn on as <vanillaname>-new-<ourname>.png")
            unresolved.append(f)
            continue
        if len(targets) > 1:
            with Image.open(src) as probe:
                sw, sh = probe.size
            same = [t for t in targets
                    if Image.open(t).size in ((sw, sh),)] or targets
            target = same[0]
            print("  %d sprites share that name; size picked %s"
                  % (len(targets), os.path.relpath(target, REPO)))
        else:
            target = targets[0]
        with Image.open(target) as ref:
            tw, th = ref.size

        cmd = [sys.executable, os.path.join(REPO, "tools", "autofit.py"), src,
               "--target", "%dx%d" % (tw, th),
               "--colours", str(args.colours), "--outdir", OUT]
        if is_splat(tw, th):
            cmd.append("--snap")
        r = subprocess.run(cmd, capture_output=True, text=True)
        print("  " + "\n  ".join(l for l in r.stdout.strip().splitlines()
                                 if "Deprecat" not in l))
        if r.returncode:
            print("  FAILED: %s" % r.stderr.strip()[:200])
            continue

        fitted = os.path.join(OUT, f)
        if args.apply:
            Image.open(fitted).convert("RGBA").save(target)
            print("  APPLIED -> %s" % os.path.relpath(target, REPO))
        done.append((f, os.path.relpath(target, REPO)))

    print("\n%d fitted, %d unresolved" % (len(done), len(unresolved)))
    for f, t in done:
        print("  %-34s -> %s" % (f, t))
    for f in unresolved:
        print("  %-34s -> ?" % f)
    return 1 if unresolved else 0


if __name__ == "__main__":
    sys.exit(main())
