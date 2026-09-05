#!/usr/bin/env bash
# Legt das gebaute Mod-Jar in die Mods-Ordner des Splitscreen-Setups.
#
# Warum es das gibt: der Dateiname traegt Spiel- und Modversion
# (Stairway_to_Heaven-<gv>-<mv>.jar). Wer nur kopiert, hat nach einem
# Versionssprung zwei Staende nebeneinander liegen, und Necesse laedt beide.
# Deshalb wird pro Ziel erst jedes Stairway_to_Heaven-*.jar geloescht und
# danach nachgeprueft, dass genau eine Datei dieses Mods dort liegt.
#
# SplitRoast startet die Spiele mit Goldberg im lokalen Splitscreen und
# biegt dafuer USERPROFILE/APPDATA pro Spieler um
# (SplitRoast.Launch/Coop/UserProfileEnv.cs). Necesse legt seine Mods
# deshalb NICHT im Spielordner ab, sondern unter
#   %USERPROFILE%\SplitRoast\Profiles\Player<N>\AppData\Roaming\Necesse\mods
# Die Instanzordner unter SplitRoast_Instances/1169040/p1|p2 tragen das
# Spiel, aber keine Mods -- dort zu kopieren waere wirkungslos.
#
# Ziele werden auf ihren echten Pfad aufgeloest und ueber Geraet:Inode
# entdoppelt: SplitRoast arbeitet mit Verknuepfungen, zwei scheinbar
# verschiedene Ordner koennen derselbe sein.
#
# Aufruf:
#   scripts/deploy_splitroast.sh [--with-singleplayer] [--dry-run]
#
# Umgebung:
#   SWH_NO_DEPLOY=1        schaltet das Ausliefern ab (fuer kopflose Laeufe)
#   SWH_DEPLOY_TARGETS     eigene Ziele, mit : getrennt; schlaegt die Suche
#
# Rueckgabe: 0 wenn ausgeliefert ODER bewusst uebersprungen (kopfloser
# Rechner ohne Windows-Laufwerk), sonst != 0.

set -uo pipefail

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DRY=0
WITH_SP=0
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY=1 ;;
    --with-singleplayer) WITH_SP=1 ;;
    -h|--help) sed -n '2,32p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) echo "deploy: unbekannte Option '$arg'" >&2; exit 2 ;;
  esac
done

say() { printf 'deploy: %s\n' "$*"; }
die() { printf 'deploy: FEHLER: %s\n' "$*" >&2; exit 1; }

if [ "${SWH_NO_DEPLOY:-0}" = "1" ]; then
  say "SWH_NO_DEPLOY=1 -- uebersprungen."
  exit 0
fi

# Zuerst pruefen, ob hier ueberhaupt ausgeliefert werden kann: auf einem
# kopflosen Rechner (Fernsitzung, Dedicated Server) gibt es kein
# Windows-Laufwerk. Das muss VOR allen anderen Pruefungen stehen, sonst
# scheitert dort der Build an einer Bedingung, die dort nie erfuellbar ist.
if [ -z "${SWH_DEPLOY_TARGETS:-}" ] && [ ! -d /mnt/c ]; then
  say "kein /mnt/c -- kopfloser Rechner, nichts auszuliefern. Uebersprungen."
  exit 0
fi

# ---- 1. Das gebaute Jar finden -------------------------------------------
shopt -s nullglob
JARS=("$REPO"/build/jar/Stairway_to_Heaven-*.jar)
shopt -u nullglob
if [ ${#JARS[@]} -eq 0 ]; then
  die "kein Jar in $REPO/build/jar/ -- erst './gradlew buildModJar' laufen lassen."
fi
if [ ${#JARS[@]} -gt 1 ]; then
  # buildModJar raeumt den Ordner selbst; mehr als eins heisst, jemand hat
  # von Hand hineinkopiert. Nicht raten, welches gemeint ist.
  die "mehrere Jars in build/jar/: ${JARS[*]}"
fi
JAR="${JARS[0]}"
JAR_NAME="$(basename "$JAR")"
say "Jar: $JAR_NAME ($(du -h "$JAR" | cut -f1))"

# ---- 1b. Passt die Spielversion zu dem, was die Spieler starten? ----------
# Der Dateiname traegt die Spielversion, und die kommt aus dem Spiel, auf das
# NECESSE_GAME_DIR beim Bauen zeigte (gradle/main.gradle liest
# GameInfo.version). AGENTS.md schickt Agenten ausdruecklich auf den
# Dedicated Server -- der ist 1.3.2, waehrend die Spieler 1.3.3 starten. Ohne
# diese Sperre wuerde ein ganz normaler kopfloser Build den gespielten Stand
# durch einen aelteren ersetzen, und zwar lautlos.
jar_gv() { basename "$1" | sed -E 's/^Stairway_to_Heaven-(.+)-[^-]+\.jar$/\1/'; }
game_version_of() {
  # Dieselbe Konstante, die gradle ueber Reflection liest -- hier aus dem
  # Klassen-Konstantenpool gefischt, damit kein Spiel gestartet werden muss.
  unzip -p "$1" necesse/engine/GameInfo.class 2>/dev/null \
    | strings -n 5 | grep -m1 -oE '^[0-9]+\.[0-9]+\.[0-9]+$'
}

BUILT_GV="$(jar_gv "$JAR")"
shopt -s nullglob
INST_JARS=(/mnt/c/Program\ Files*/Steam/SplitRoast_Instances/1169040/p*/Necesse.jar)
shopt -u nullglob
PLAY_GV=""
[ ${#INST_JARS[@]} -gt 0 ] && PLAY_GV="$(game_version_of "${INST_JARS[0]}")"

if [ -n "$PLAY_GV" ] && [ "$BUILT_GV" != "$PLAY_GV" ]; then
  if [ "${SWH_DEPLOY_FORCE:-0}" = "1" ]; then
    say "ACHTUNG: Jar ist fuer $BUILT_GV, die Spieler starten $PLAY_GV -- trotzdem ausgeliefert (SWH_DEPLOY_FORCE=1)."
  else
    say ""
    say "NICHT AUSGELIEFERT: das Jar ist fuer Spielversion $BUILT_GV gebaut,"
    say "die Spieler starten aber $PLAY_GV."
    say "Der gespielte Stand bleibt unangetastet."
    say ""
    say "Grund: die Version im Dateinamen kommt aus dem Spiel, auf das"
    say "NECESSE_GAME_DIR beim Bauen zeigte. Fuer einen Build, der gespielt"
    say "werden soll:"
    say "  export NECESSE_GAME_DIR=\"/mnt/c/Program Files (x86)/Steam/steamapps/common/Necesse\""
    say "  ./gradlew buildModJar"
    say "Der Dedicated Server ($BUILT_GV) taugt zum Pruefen, nicht zum Ausliefern."
    say "Trotzdem ausliefern: SWH_DEPLOY_FORCE=1"
    # Bewusst 0: ein kopfloser Lauf soll hieran nicht scheitern, er soll es
    # nur nicht tun.
    exit 0
  fi
fi
[ -n "$PLAY_GV" ] && say "Spielversion: $PLAY_GV -- passt."

# ---- 2. Ziele suchen ------------------------------------------------------
declare -a CANDIDATES=()
if [ -n "${SWH_DEPLOY_TARGETS:-}" ]; then
  IFS=: read -r -a CANDIDATES <<< "$SWH_DEPLOY_TARGETS"
  say "Ziele aus SWH_DEPLOY_TARGETS."
else
  # Die Spielerprofile. Kein cmd.exe/wslpath noetig: der Pfad ist eindeutig
  # genug, und Windows-Interop ist unter WSL nicht immer erreichbar.
  shopt -s nullglob
  CANDIDATES=(/mnt/c/Users/*/SplitRoast/Profiles/Player*/AppData/Roaming/Necesse/mods)
  if [ "$WITH_SP" = "1" ]; then
    CANDIDATES+=(/mnt/c/Users/*/AppData/Roaming/Necesse/mods)
  fi
  shopt -u nullglob
fi

if [ ${#CANDIDATES[@]} -eq 0 ]; then
  die "keine Mods-Ordner gefunden. Erwartet unter /mnt/c/Users/<user>/SplitRoast/Profiles/Player<N>/AppData/Roaming/Necesse/mods -- hat sich das Splitroast-Setup geaendert?"
fi

# ---- 3. Auf echte Verzeichnisse entdoppeln --------------------------------
# Schluessel ist Geraet:Inode, nicht der Pfadtext: eine Windows-Verknuepfung
# kann zwei Pfade auf dasselbe Verzeichnis zeigen lassen, und dann duerfen
# wir dort nicht zweimal ablegen.
declare -a TARGETS=()
declare -A SEEN=()
for c in "${CANDIDATES[@]}"; do
  [ -d "$c" ] || { say "uebergangen (kein Ordner): $c"; continue; }
  real="$(realpath "$c")"
  key="$(stat -c '%d:%i' "$real" 2>/dev/null)" || key="$real"
  if [ -n "${SEEN[$key]:-}" ]; then
    say "doppelt, zeigt auf ${SEEN[$key]} -- uebergangen: $c"
    continue
  fi
  SEEN[$key]="$real"
  TARGETS+=("$real")
done

[ ${#TARGETS[@]} -gt 0 ] || die "nach dem Entdoppeln blieb kein Ziel uebrig."
say "${#TARGETS[@]} echte Mods-Verzeichnisse:"
for t in "${TARGETS[@]}"; do say "  - $t"; done

# ---- 4. Alte Version weg, neue hin ---------------------------------------
FAIL=0
for t in "${TARGETS[@]}"; do
  shopt -s nullglob
  old=("$t"/Stairway_to_Heaven-*.jar)
  shopt -u nullglob
  if [ "$DRY" = "1" ]; then
    say "[dry-run] $t: ${#old[@]} alte Datei(en) loeschen, $JAR_NAME ablegen"
    continue
  fi
  for o in "${old[@]}"; do
    if ! rm -f "$o" 2>/dev/null || [ -e "$o" ]; then
      # Auf DrvFs schlaegt das Loeschen fehl, solange Windows die Datei
      # offen haelt -- also solange eine Necesse-Instanz laeuft.
      say "kann '$(basename "$o")' nicht loeschen -- laeuft Necesse noch? Erst beide Fenster schliessen." >&2
      FAIL=1
      continue 2
    fi
  done
  if ! cp -f "$JAR" "$t/$JAR_NAME"; then
    say "Kopieren nach $t fehlgeschlagen." >&2
    FAIL=1
    continue
  fi
done

if [ "$DRY" = "1" ]; then
  say "dry-run, nichts geaendert."
  exit 0
fi

# ---- 5. Nachpruefen: genau eine Datei dieses Mods pro Verzeichnis ---------
say "Nachpruefung:"
for t in "${TARGETS[@]}"; do
  shopt -s nullglob
  now=("$t"/Stairway_to_Heaven-*.jar)
  shopt -u nullglob
  n=${#now[@]}
  if [ "$n" -eq 1 ] && [ "$(basename "${now[0]}")" = "$JAR_NAME" ]; then
    say "  OK   $t -> $n Datei ($JAR_NAME)"
  else
    say "  FEHL $t -> $n Datei(en): ${now[*]:-keine}" >&2
    FAIL=1
  fi
  # Nur ein Hinweis: modlist.data haelt den Mod ueber seine ID fest
  # (id = stairwaytoheaven), nicht ueber den Dateinamen. Ein Versionssprung
  # laesst 'enabled = true' deshalb stehen.
  if [ -f "$t/modlist.data" ] && ! grep -q "id = stairwaytoheaven" "$t/modlist.data" 2>/dev/null; then
    say "  Hinweis: $t/modlist.data kennt 'stairwaytoheaven' nicht -- der Mod ist im Spiel evtl. noch nicht aktiviert (einmal im Mod-Menue anhaken)."
  fi
done

[ "$FAIL" = "0" ] || die "Ausliefern unvollstaendig (siehe oben)."
say "fertig -- $JAR_NAME liegt in ${#TARGETS[@]} Mods-Verzeichnis(sen), je genau einmal."
