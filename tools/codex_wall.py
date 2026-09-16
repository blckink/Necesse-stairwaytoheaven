#!/usr/bin/env python3
"""Fast, specialised setup for turning a wall THEME into a Codex image_gen job.

Why this exists: every wall set drawn so far went through general-purpose Codex
briefs and came back bad -- the player calls ALL current walls schlecht. Codex's
image_gen does not understand the 352x128 cell atlas (WallObject's neighbour
grammar), so asking it to "paint a wall sheet" makes it paint one continuous
illustration across cell boundaries that were never meant to touch.

`wall_from_layout.py` already solves the atlas problem for HUMAN painters: it
turns the 32 body cells + 8 door slots + window slots into one continuous
canvas, laid out exactly as the shapes look in game (a HARD MASK -- named,
bordered regions), and slices the 352x128 sheet back out of it mechanically.
This script is nothing but that same canvas, handed to Codex instead of a
human, with a filled-in vanilla reference in the SAME layout so Codex sees
what "done" looks like for this exact set of shapes -- not a generic wall
photo it has to reinterpret.

Pipeline:
  1. wall_from_layout.py --new NAME             -> blank paint canvas + guide
  2. wall_from_layout.py --new NAME_ref \
         --from-sheet <vanilla>.png             -> SAME canvas, filled with a
                                                    vanilla wall, so the shapes
                                                    Codex must fill are already
                                                    demonstrated once
  3. brief.md written here from a template
  4. run.sh: the codex exec command, for nohup backgrounding (this script does
     not call codex itself -- only ONE codex run may be in flight repo-wide)
  5. --finish, run after Codex wrote filled.png:
         wall_from_layout.py filled.png -o sheet.png     (cut)
         wall_render_preview.py --sheet sheet.png         (compare vs vanilla)
         conform_wall_sheet.py sheet.png                  (seam/colour report)

Usage:
    python3 tools/codex_wall.py skystonebrick --theme "..." [--vanilla-ref stonewall]
        -> sets up build/codexwall/skystonebrick/{paint.png,guide.png,
           vanilla-filled.png,brief.md,run.sh}
    bash build/codexwall/skystonebrick/run.sh          # or nohup it yourself
        -> writes build/codexwall/skystonebrick/filled.png
    python3 tools/codex_wall.py skystonebrick --finish
        -> cuts sheet.png, renders the comparison scene, runs conform, prints
           a summary and the file paths to look at
"""
import argparse
import os
import shutil
import subprocess
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TOOLS = os.path.join(REPO, "tools")
OUT_ROOT = os.path.join(REPO, "build", "codexwall")
WALLPAINT = os.path.join(REPO, "build", "qa", "wallpaint")
CODEX_BIN = os.path.expanduser("~/.npm-global/bin/codex")
NECESSE_SPRITES = "/home/blackoffset/dev/Necesse sprites"

BRIEF_TEMPLATE = '''# Codex-Auftrag: Wandset "{name}"

Thema: {theme}

## Was du bekommst
1. `paint.png` -- eine TRANSPARENTE Leinwand. Jede sichtbare Form darauf ist
   eine benannte Wandansicht, wie sie im Spiel wirklich aussieht (Kappe,
   Saeule, Ecke, Tuerslots, Fensterslots). Die Formen sind schon an der
   richtigen Stelle -- du fuellst nur die transparenten Flaechen innerhalb
   jeder Form, du verschiebst und veraenderst NICHTS an Position oder Groesse.
2. `guide.png` -- dieselbe Leinwand mit Beschriftung: jede Zelle ist
   durchnummeriert, jede Form ist benannt (siehe die Kopfkommentare in
   tools/wall_from_layout.py fuer die genaue Grammatik). Nur zum Verstehen,
   NICHT die Vorlage zum Malen.
3. `vanilla-filled.png` -- DIESELBE Leinwand, bereits mit einer fertigen
   Vanilla-Wand ({vanilla_ref}) gefuellt. Das ist der Massstab: Kontur, Licht,
   Fugenbreite, Farbzahl. Deine Wand muss densel­ben Detailgrad haben, nicht
   mehr, nicht weniger.

## Was du lieferst
`filled.png` -- exakte Kopie von `paint.png` (gleiche Groesse, gleiche
Formen an gleicher Stelle), aber jede Form vollstaendig gefuellt mit Material
zum Thema oben. Die Tuer-Slots (8 schmale Kaesten unten) und die zwei
Fenster-Slots (ganz unten) bekommen eigenen Inhalt -- Tuer als game-typische
Holz/Metalltuer, Fenster als Necesse-Fensterrahmen. Alles ausserhalb der
Formen bleibt komplett transparent (Alpha 0) -- nichts wird ausserhalb der
vorgegebenen Umrisse gemalt, auch nicht "damit es zusammenhaengt".

## Technik (PFLICHT)
- Baue zuerst ein hochaufgeloestes image_gen-Master pro Form (oder fuer die
  ganze Leinwand in einem Bild, wenn das die Formgrenzen exakt einhaelt).
- Skaliere NICHT einfach linear herunter: nimm pro Zielpixel die Modalfarbe
  (haeufigste Farbe im entsprechenden Block), sonst verschwimmen die Kanten.
- Reduziere danach auf hoechstens 40 Farben insgesamt (siehe
  art/supplied/readme.md, Abschnitt Waende: Illustrationen mit tausenden
  Farben scheitern; gute Wandblaetter haben 19-38 Farben).
- Alpha hart auf 0 oder 255 setzen, keine Halbtransparenz, keine
  Anti-Aliasing-Raender.
- 1px Kontur in (34,34,46) um jede undurchsichtige Flaeche, wie bei
  vanilla-filled.png zu sehen.
- Zeichne die Endkunst NICHT mit PIL-Formen (draw.rectangle, draw.ellipse
  etc.) -- das sieht man sofort als Vektor-Look. Der Weg ist immer:
  image_gen-Master -> Modalfarben-Downscale -> <=40 Farben -> Alpha hart
  0/255 -> 1px-Kontur (34,34,46).
- Falls du mit PIL arbeitest (Zuschnitt, Kanal-Arbeit, Farbreduktion):
  PYTHONPATH=/home/blackoffset/dev/pylib python3 -- kein venv, nichts
  nachladen.
- Du darfst NUR unterhalb von build/ schreiben. Speichere das Ergebnis als
  build/codexwall/{name}/filled.png, exakte Pixelmasse wie paint.png.

## Nicht vergessen
- Jede Form ist eine EIGENSTAENDIGE Ansicht -- Dach/Kappe-Form, Saeule,
  Ecke/Junction usw. duerfen sich unterscheiden, solange das Material klar
  dasselbe ist (Necesse-Waende zeigen oben eine Deckflaeche, vorn die
  Mauerwerksfront).
- Die "junction"-Form hat ausgegraute Bereiche (fremdes Material) -- die
  bleiben UNVERAENDERT ausgegraut, du malst nur die eigenen (nicht-grauen)
  Zellen.
- Tuer-Slots: rot0/rot2 werden im Spiel gespiegelt benutzt, muessen also von
  beiden Seiten lesbar sein.
'''


def sh(cmd, env=None, allow_rc1=False):
    print("+ " + " ".join(cmd))
    r = subprocess.run(cmd, cwd=REPO, env=env)
    ok = (0,) if not allow_rc1 else (0, 1)
    if r.returncode not in ok:
        # wall_from_layout returns 1 on clash/report-fail (cut step only),
        # which --finish needs to see but not die silently on.
        raise SystemExit("command failed (%d): %s" % (r.returncode, " ".join(cmd)))
    return r.returncode


def env_with_sprites():
    e = dict(os.environ)
    e["NECESSE_SPRITES"] = NECESSE_SPRITES
    pylib = "/home/blackoffset/dev/pylib"
    e["PYTHONPATH"] = pylib + os.pathsep + e["PYTHONPATH"] if e.get("PYTHONPATH") else pylib
    return e


def setup(name, theme, vanilla_ref):
    outdir = os.path.join(OUT_ROOT, name)
    os.makedirs(outdir, exist_ok=True)
    env = env_with_sprites()

    sh([sys.executable, os.path.join(TOOLS, "wall_from_layout.py"), "--new", name], env=env)
    shutil.copy(os.path.join(WALLPAINT, name + "-paint.png"),
                os.path.join(outdir, "paint.png"))
    shutil.copy(os.path.join(WALLPAINT, name + "-guide.png"),
                os.path.join(outdir, "guide.png"))

    ref_sheet = os.path.join(NECESSE_SPRITES, "objects", vanilla_ref + ".png")
    if not os.path.exists(ref_sheet):
        print("WARNING: vanilla reference not found at %s -- vanilla-filled.png "
              "will be skipped, Codex only gets paint.png + guide.png" % ref_sheet,
              file=sys.stderr)
    else:
        refname = name + "_ref"
        sh([sys.executable, os.path.join(TOOLS, "wall_from_layout.py"), "--new", refname,
            "--from-sheet", ref_sheet], env=env)
        shutil.copy(os.path.join(WALLPAINT, refname + "-paint.png"),
                    os.path.join(outdir, "vanilla-filled.png"))
        # tidy the intermediate files wall_from_layout insists on writing under
        # build/qa/wallpaint/ -- codexwall/ is the canonical copy from here on
        os.remove(os.path.join(WALLPAINT, refname + "-guide.png"))

    brief = BRIEF_TEMPLATE.format(name=name, theme=theme, vanilla_ref=vanilla_ref)
    with open(os.path.join(outdir, "brief.md"), "w") as f:
        f.write(brief)

    images = ["paint.png", "guide.png"]
    if os.path.exists(os.path.join(outdir, "vanilla-filled.png")):
        images.append("vanilla-filled.png")
    run_sh = "#!/bin/sh\nset -e\ncd %s\n%s exec -C %s -s workspace-write " \
        "--skip-git-repo-check %s -o last.txt \"$(cat brief.md)\"\n" % (
            outdir, CODEX_BIN, REPO,
            " ".join("-i %s" % os.path.join(outdir, i) for i in images))
    run_path = os.path.join(outdir, "run.sh")
    with open(run_path, "w") as f:
        f.write(run_sh)
    os.chmod(run_path, 0o755)

    print("\nset up: %s" % os.path.relpath(outdir, REPO))
    for f in images + ["brief.md", "run.sh"]:
        print("  %s" % os.path.join(os.path.relpath(outdir, REPO), f))
    print("\nstart with (background, only ONE codex run repo-wide at a time):")
    print("  nohup %s > %s/codex.log 2>&1 &" % (run_path, os.path.relpath(outdir, REPO)))
    print("\nwhen filled.png exists:")
    print("  python3 tools/codex_wall.py %s --finish" % name)


def finish(name):
    outdir = os.path.join(OUT_ROOT, name)
    filled = os.path.join(outdir, "filled.png")
    if not os.path.exists(filled):
        raise SystemExit("no %s yet -- codex has not written it (check codex.log)" % filled)
    env = env_with_sprites()

    sheet = os.path.join(outdir, "sheet.png")
    rc = sh([sys.executable, os.path.join(TOOLS, "wall_from_layout.py"), filled,
             "-o", sheet], env=env, allow_rc1=True)
    if rc != 0:
        print("\nwall_from_layout reported clashes above -- sheet.png was NOT written "
              "cleanly, fix filled.png (or re-run codex) before trusting the rest.")
        return rc

    preview_out = os.path.join(outdir, "preview")
    sh([sys.executable, os.path.join(TOOLS, "wall_render_preview.py"),
        "--sheet", sheet, "--out", preview_out], env=env)

    conform_log = os.path.join(outdir, "conform.txt")
    r = subprocess.run([sys.executable, os.path.join(TOOLS, "conform_wall_sheet.py"), sheet],
                        cwd=REPO, env=env, capture_output=True, text=True)
    with open(conform_log, "w") as f:
        f.write(r.stdout + r.stderr)
    print(r.stdout)
    print(r.stderr, file=sys.stderr)

    print("\nsheet:    %s" % os.path.relpath(sheet, REPO))
    print("preview:  %s/" % os.path.relpath(preview_out, REPO))
    print("conform:  %s (exit %d)" % (os.path.relpath(conform_log, REPO), r.returncode))
    return 0


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("name")
    ap.add_argument("--theme", help="one-line description for the brief, required to set up")
    ap.add_argument("--vanilla-ref", default="stonewall",
                    help="vanilla wall (from the sprite dump) to demonstrate the layout with")
    ap.add_argument("--finish", action="store_true",
                    help="cut filled.png into sheet.png, render preview, run conform")
    args = ap.parse_args()

    if args.finish:
        return finish(args.name)
    if not args.theme:
        raise SystemExit("--theme is required to set up a new job (or pass --finish)")
    setup(args.name, args.theme, args.vanilla_ref)
    return 0


if __name__ == "__main__":
    sys.exit(main())
