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
import difflib
import json
import os
import re
import subprocess
import sys

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "src", "main", "resources")
INBOX = os.path.join(REPO, "art-inbox")
SIZES = os.path.join(REPO, "tools", "vanilla_sizes.json")

# "drawn on the vanilla X sheet, to become our Y". The separator is written by
# hand on a phone, so accept what people actually type.
SPLIT = re.compile(r"[-_](?:new|now|to|becomes)[-_]", re.I)
OUT = os.path.join(REPO, "build", "qa", "inbox")


def shipped_sprites():
    """Every PNG the mod ships, LOWERCASE basename -> [paths].

    Lowercase because the names get typed on a phone and our files are all
    lowercase. A bestiary icon sorts last: `mobs/icons/rimesentry.png` and
    `mobs/rimesentry.png` are different jobs, and the sheet is the one a file
    named after the mob almost always means.
    """
    found = {}
    for root, _dirs, files in os.walk(RES):
        for f in files:
            if f.endswith(".png"):
                found.setdefault(f[:-4].lower(), []).append(os.path.join(root, f))
    for paths in found.values():
        paths.sort(key=lambda p: ("/icons/" in p.replace(os.sep, "/"), p))
    return found


def vanilla_sizes():
    """name -> (w, h) for every vanilla sheet, from a table committed to the repo.

    The sprite dump is gitignored and CI has no copy of it, so the size of a
    vanilla sheet cannot be measured there. This table is generated from the
    dump with --refresh-sizes and committed, which is the only reason the
    <vanilla>-new-<ours> route works in the Action at all.
    """
    if not os.path.exists(SIZES):
        return {}
    with open(SIZES) as fh:
        return {k.lower(): tuple(v) for k, v in json.load(fh).items()}


def resolve(name, shipped, vsizes):
    """(target_path, (w, h), how) -- where this file goes and what size it must be.

    Two routes. A name that matches a sprite we already ship replaces it. A name
    written `<vanillasheet>-new-<ourname>` is NEW art drawn on a vanilla sheet:
    the size comes from that VANILLA sheet, and the file is CREATED under our
    own name -- which is the whole point of the route. It used to demand that
    our file already exist, so it could never once do the job it was built for.
    """
    low = name.lower()
    if low in shipped:
        p = shipped[low][0]
        from PIL import Image as _I
        with _I.open(p) as im:
            return p, im.size, "replaces the sprite we already ship"
    parts = SPLIT.split(name, 1)
    if len(parts) == 2:
        vanilla, ours = parts[0].lower(), parts[1].lower()
        # The VANILLA half wins here, and the order matters. Naming a file
        # `AncientSkeletonMage-new-CinderCantor` states which sheet the art was
        # drawn on; that sheet's size is the answer. Looking our own name up
        # first found `mobs/icons/cindercantor.png` -- the 32x32 bestiary
        # portrait -- and squashed a 448x320 mob sheet into it.
        cand = [k for k in vsizes if k.split("/")[-1] == vanilla]
        if cand:
            key = sorted(cand, key=len)[0]
            folder = key.rsplit("/", 1)[0] if "/" in key else "mobs"
            dest = os.path.join(RES, folder.replace("/", os.sep), ours + ".png")
            return dest, vsizes[key], ("NEW file, sized from vanilla's %s (%dx%d)"
                                       % (key, vsizes[key][0], vsizes[key][1]))
        if ours in shipped:
            p = shipped[ours][0]
            from PIL import Image as _I
            with _I.open(p) as im:
                return p, im.size, "no vanilla sheet by that name; sized from ours"
    return None, None, None


def is_splat(w, h):
    return w == 224 and h % 96 == 0


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--apply", action="store_true",
                    help="write the fitted file over the sprite it resolved to")
    ap.add_argument("--colours", type=int, default=32)
    ap.add_argument("--refresh-sizes", action="store_true",
                    help="rebuild tools/vanilla_sizes.json from the local sprite "
                         "dump. Run it when the game updates; CI cannot, because "
                         "the dump is gitignored")
    args = ap.parse_args()

    if args.refresh_sizes:
        dump = os.path.join(REPO, "vanilla-sprites")
        out = {}
        for root, _d, fs in os.walk(dump):
            for f in fs:
                if not f.endswith(".png"):
                    continue
                rel = os.path.relpath(os.path.join(root, f), dump)[:-4].replace(os.sep, "/")
                try:
                    with Image.open(os.path.join(root, f)) as im:
                        out[rel] = list(im.size)
                except Exception:
                    pass
        with open(SIZES, "w") as fh:
            json.dump(out, fh, indent=0, sort_keys=True)
        print("wrote %s (%d sheets)" % (os.path.relpath(SIZES, REPO), len(out)))
        return 0

    if not os.path.isdir(INBOX):
        print("no art-inbox/ -- nothing to do")
        return 0
    files = sorted(f for f in os.listdir(INBOX) if f.lower().endswith(".png"))
    if not files:
        print("art-inbox/ is empty -- nothing to do")
        return 0

    shipped = shipped_sprites()
    vsizes = vanilla_sizes()
    os.makedirs(OUT, exist_ok=True)
    done, unresolved = [], []

    for f in files:
        src = os.path.join(INBOX, f)
        name = f[:-4]
        target, size, how = resolve(name, shipped, vsizes)
        print("\n=== %s" % f)
        if target is None:
            print("  UNRESOLVED: nothing called '%s'." % name)
            pool = sorted(set(list(shipped) + [k.split("/")[-1] for k in vsizes]))
            near = difflib.get_close_matches(name.lower(), pool, n=3, cutoff=0.6)
            if near:
                print("  Did you mean: %s" % ", ".join(near))
            print("  Name it after the sprite it replaces, or -- for NEW art drawn")
            print("  on a vanilla sheet -- as <vanillasheet>-new-<ourname>.png")
            print("  (new/now/to all work, case does not matter).")
            unresolved.append(f)
            continue
        tw, th = size
        print("  -> %s" % os.path.relpath(target, REPO))
        print("     %s" % how)

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

        # A NEW file under our own name is only half the job: something in the
        # code has to READ it. RimeSentryMob extends vanilla's FrostSentryMob,
        # which draws from the static MobRegistry.Textures.frostSentry loaded
        # once from "mobs/frostsentry" -- our subclass inherits that draw code,
        # so a file at mobs/rimesentry.png is never looked at. Say so here
        # rather than let a green run imply the sprite is in the game.
        if how.startswith("NEW file"):
            stem = os.path.splitext(os.path.basename(target))[0]
            folder = os.path.relpath(os.path.dirname(target), RES).replace(os.sep, "/")
            hit = subprocess.run(
                ["grep", "-rl", "%s/%s" % (folder, stem),
                 os.path.join(REPO, "src", "main", "java")],
                capture_output=True, text=True).stdout.strip()
            if hit:
                print("     read by: %s" % ", ".join(
                    os.path.relpath(h, REPO) for h in hit.splitlines()[:3]))
            else:
                print("     WARNING: no Java file loads \"%s/%s\". The sheet will sit"
                      % (folder, stem))
                print("     unused until the mob overrides its draw -- a subclass of a"
                      " vanilla")
                print("     mob keeps drawing the vanilla sheet. Fitting it was still"
                      " correct.")

        fitted = os.path.join(OUT, f)
        if args.apply:
            os.makedirs(os.path.dirname(target), exist_ok=True)
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
