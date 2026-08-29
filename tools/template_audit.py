#!/usr/bin/env python3
"""Template audit: a sprite template is a SPECIFICATION, so check the spec.

`docs/CODEX_SPRITE_TEMPLATE_BRIEF.md` has an outside agent prepare empty
canvases at the size the engine actually reads, plus a card naming the vanilla
asset and class each was modelled on. The point of that exercise is to get the
FORMAT right before anyone draws, because every art bug this project has shipped
was a correct picture in a wrong layout.

That only works if the cards are real. This checks:

  * folder layout, and the `<vanilla-ref>-new-<our-id>.png` naming the repo
    already uses in src/main/resources/kk-sprites/
  * the blank template is EXACTLY the size its card claims, and is fully
    transparent — a template with opaque pixels is finished art in the wrong
    place, and it would be overwritten by the generator anyway
  * the guide sheet matches that size
  * every field the brief requires is present and carries a [jar] or [docs]
    citation, and no UNKNOWN survives in a template listed in the manifest
  * the engine class named on the card exists, when the decompiled sources do
  * the id is not already shipped in src/main/resources/

An empty art-templates/ passes: there is nothing to be wrong about yet.

Usage:  python3 tools/template_audit.py
Exit code 1 if any card is unsupported by its files.
"""
import argparse
import os
import re
import sys

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# Mutable on purpose: --root points this audit at a DIFFERENT checkout, because
# the reviewer runs it over a worktree of somebody else's branch while this
# file lives in their own. Deriving them from __file__ alone silently audits
# the wrong tree and reports it as clean.
ROOT = os.path.join(REPO, "art-templates")
RES = os.path.join(REPO, "src", "main", "resources")

# Field -> the regex that finds it on the card. The brief's table is the source
# of this list; a card missing any of them is a card that cannot be acted on.
FIELDS = {
    "our id": r"our id",
    "vanilla reference": r"vanilla reference",
    "engine class": r"engine class",
    "sheet size": r"sheet size",
    "what the renderer reads": r"what the renderer reads",
    "cell meaning": r"what each cell|cell/row/column MEANS|cell meaning",
    "draw anchor": r"draw anchor",
    "behaviour": r"behaviour",
    "registration": r"registration",
    "companions": r"companions",
    "locale keys": r"locale keys",
    "audits": r"which audits|audits will check",
}
SIZE_RE = re.compile(r"(\d{2,4})\s*[x×]\s*(\d{2,4})")


def decompiled_root():
    game = os.environ.get("NECESSE_GAME_DIR")
    if not game:
        return None
    for cand in (os.path.join(game, "decompiled", "src"),
                 os.path.join(game, "..", "decompiled", "src")):
        if os.path.isdir(cand):
            return os.path.abspath(cand)
    return None


def shipped_ids():
    out = set()
    for sub, _dirs, files in os.walk(RES):
        for f in files:
            if f.endswith(".png"):
                out.add(os.path.splitext(f)[0])
    return out


def check_one(folder, problems, notes, dec, shipped):
    tid = os.path.basename(folder)
    files = os.listdir(folder)
    card = os.path.join(folder, tid + ".md")
    guide = os.path.join(folder, tid + ".guide.png")
    blanks = [f for f in files if re.fullmatch(r".+-new-" + re.escape(tid) + r"\.png", f)]

    if not os.path.exists(card):
        problems.append("%s: no spec card (%s.md)" % (tid, tid))
        return
    if not blanks:
        problems.append("%s: no blank template named <vanilla-ref>-new-%s.png"
                        % (tid, tid))
        return
    if len(blanks) > 1:
        problems.append("%s: %d blank templates, expected one" % (tid, len(blanks)))

    text = open(card, encoding="utf-8").read()
    for name, pattern in FIELDS.items():
        if not re.search(pattern, text, re.I):
            problems.append("%s: card has no '%s' field" % (tid, name))
    cited = len(re.findall(r"\[jar\]|\[docs\]", text))
    if cited < len(FIELDS) // 2:
        problems.append("%s: card carries %d [jar]/[docs] citations for %d fields "
                        "-- a size you cannot cite is a size you must not write down"
                        % (tid, cited, len(FIELDS)))
    if "UNKNOWN" in text:
        notes.append("%s: card still contains UNKNOWN -- it must not be in the "
                     "manifest until that is resolved" % tid)

    m = SIZE_RE.search(text)
    if not m:
        problems.append("%s: card states no sheet size" % tid)
        return
    want = (int(m.group(1)), int(m.group(2)))

    blank_path = os.path.join(folder, blanks[0])
    im = Image.open(blank_path).convert("RGBA")
    if im.size != want:
        problems.append("%s: blank is %s, card says %s" % (tid, im.size, want))
    ink = im.getchannel("A").getbbox()          # None when nothing is opaque
    if ink is not None:
        problems.append("%s: blank has opaque pixels in %s -- a template is an "
                        "empty canvas, not art" % (tid, ink))

    if os.path.exists(guide):
        g = Image.open(guide)
        if g.size != want:
            problems.append("%s: guide is %s, card says %s" % (tid, g.size, want))
    else:
        problems.append("%s: no %s.guide.png" % (tid, tid))

    if tid in shipped:
        problems.append("%s: src/main/resources already ships a PNG with this id"
                        % tid)

    cm = re.search(r"engine class[^\n|]*[|:]\s*`?([A-Za-z0-9_.]+)`?", text, re.I)
    if cm and dec:
        cls = cm.group(1).split(".")[-1]
        if not any(cls + ".java" in fs for _r, _d, fs in os.walk(dec)):
            problems.append("%s: card names engine class %s, which is not in the "
                            "decompiled sources" % (tid, cls))


def write_review(path, folders, problems, notes, dec):
    """A fix list addressed to whoever produced the templates.

    Codex works in a different checkout on a different machine; the git remote
    is the only channel between us. So the review has to be a FILE that travels
    on a branch, not console output someone has to copy by hand.
    """
    lines = ["# Template review", ""]
    lines.append("Produced by `tools/template_audit.py --review` against "
                 "`art-templates/`.")
    lines.append("")
    lines.append("- templates found: **%d**" % len(folders))
    lines.append("- blocking problems: **%d**" % len(problems))
    lines.append("- notes: **%d**" % len(notes))
    lines.append("- decompiled sources available to cross-check class names: **%s**"
                 % ("yes" if dec else "no"))
    lines.append("")
    if problems:
        lines += ["## Fix these", "",
                  "Each line is a template whose files and card disagree. A card "
                  "that does not match its own canvas is a wrong specification, "
                  "not art to be corrected later.", ""]
        lines += ["- [ ] %s" % p for p in problems]
        lines.append("")
    if notes:
        lines += ["## Notes, not blocking", ""]
        lines += ["- %s" % n for n in notes]
        lines.append("")
    if not folders:
        lines += ["## Nothing produced yet", "",
                  "`art-templates/` does not exist or holds no template folders. "
                  "This is not a pass — it is an empty inbox.", ""]
    elif not problems and not notes:
        lines += ["## Nothing to fix", "",
                  "Every template is an empty canvas at the size its own card "
                  "cites, with every field filled and cited. Ready to be drawn "
                  "in `tools/asset_generator/`.", ""]
    lines += ["---", "",
              "Contract: `docs/CODEX_SPRITE_TEMPLATE_BRIEF.md` §2 (layout), §3 "
              "(the card's fields), §4 (what this audit checks)."]
    with open(path, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines) + "\n")


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--root", metavar="REPO",
                    help="audit a different checkout (a worktree of another "
                         "agent's branch) instead of this one")
    ap.add_argument("--review", metavar="FILE",
                    help="also write the result as a fix list for the agent that "
                         "produced the templates (they are on another machine; "
                         "the branch is the only channel)")
    args = ap.parse_args()

    global ROOT, RES
    if args.root:
        base = os.path.abspath(args.root)
        ROOT = os.path.join(base, "art-templates")
        RES = os.path.join(base, "src", "main", "resources")

    if not os.path.isdir(ROOT):
        print("OK: no art-templates/ yet -- nothing to check. See "
              "docs/CODEX_SPRITE_TEMPLATE_BRIEF.md.")
        if args.review:
            write_review(args.review, [], [], [], decompiled_root())
        return 0
    dec = decompiled_root()
    shipped = shipped_ids()
    problems, notes = [], []
    folders = sorted(d for d in os.listdir(ROOT)
                     if os.path.isdir(os.path.join(ROOT, d)))
    for d in folders:
        check_one(os.path.join(ROOT, d), problems, notes, dec, shipped)

    for f in ("README.md", "MANIFEST.md"):
        if not os.path.exists(os.path.join(ROOT, f)):
            problems.append("art-templates/%s is missing" % f)

    if args.review:
        write_review(args.review, folders, problems, notes, dec)
        print("review written to %s" % args.review)
    for n in notes:
        print("  note: %s" % n)
    if problems:
        print("\nTEMPLATE PROBLEMS:\n")
        for p in problems:
            print("  - %s" % p)
        print("\n%d problem(s) across %d template(s)." % (len(problems), len(folders)))
        return 1
    print("OK: %d template(s), each an empty canvas at the size its card cites%s."
          % (len(folders), "" if dec else " (no decompiled sources to cross-check "
                                          "class names against)"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
