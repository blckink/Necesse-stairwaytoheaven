# Wandsets ueber Codex: die spezialisierte Pipeline

Werkzeug: `tools/codex_wall.py`. Baut auf `tools/wall_from_layout.py`,
`tools/wall_render_preview.py`, `tools/conform_wall_sheet.py` auf -- nichts
davon wird verdoppelt, nur zu einer Codex-tauglichen HARTEN MASKE verkettet.

## Warum

Codex' image_gen versteht das 352x128-Zellatlas nicht (`WallObject`s
Nachbarschafts-Grammatik: 32 Koerperzellen, 8 Tuerslots, Fensterslots, Zeilen
1/2 tauschen Spaltenbedeutung). Ein allgemeiner Brief "male ein Wandblatt"
fuehrt dazu, dass Codex EIN zusammenhaengendes Bild ueber Zellgrenzen malt,
die im Spiel nie nebeneinander liegen -- daher wirken alle bisherigen Waende
schlecht.

`wall_from_layout.py` loest das fuer menschliche Maler bereits: es legt eine
Leinwand an, auf der jede Spielansicht (Kappe, Saeule, Ecke/Junction, Tuer x8,
Fenster x2) als eigene, benannte, umrandete Flaeche erscheint -- genau so,
wie sie im Spiel aussieht, an der Position, die die Engine tatsaechlich
zeichnet. Das ist die harte Maske. Dieses Skript gibt Codex exakt dieselbe
Leinwand statt einem Menschen.

## Ablauf

```
python3 tools/codex_wall.py NAME --theme "Beschreibung" [--vanilla-ref stonewall]
```

Erzeugt `build/codexwall/NAME/`:
- `paint.png` -- transparente Leinwand, Formen an der richtigen Stelle, leer
- `guide.png` -- dieselbe Leinwand, beschriftet (Zellnummern, Formnamen) --
  nur zur Orientierung, NICHT die Malvorlage
- `vanilla-filled.png` -- dieselbe Leinwand, bereits mit `--vanilla-ref`
  (Default `stonewall`) gefuellt. Das ist der Massstab fuer Codex: gleicher
  Detailgrad, gleiche Kontur, gleiche Fugenbreite, wie eine fertige Vanilla-
  Wand in exakt diesem Layout aussieht -- kein generisches Wandfoto.
- `brief.md` -- der Auftragstext (siehe unten)
- `run.sh` -- der fertige `codex exec`-Aufruf mit allen drei Bildern als `-i`

Start (nur EIN Codex-Lauf gleichzeitig im Repo -- Kontingent!):
```
nohup build/codexwall/NAME/run.sh > build/codexwall/NAME/codex.log 2>&1 &
```

Codex schreibt `build/codexwall/NAME/filled.png` (gleiche Masse wie
`paint.png`, jede Form gefuellt, Rest transparent).

Danach schneiden + pruefen:
```
python3 tools/codex_wall.py NAME --finish
```
Das cuttet `sheet.png` (352x128) via `wall_from_layout.py`, rendert die
Vergleichs-Szene via `wall_render_preview.py` nach
`build/codexwall/NAME/preview/` (unser Sheet direkt neben Vanilla-Referenzen,
hell + dunkel), und schreibt `conform.txt` (Nahtpruefung, Farbzahl,
Fensterslot-Regel) via `conform_wall_sheet.py`.

## Was im Brief steht (Kernpunkte)

- Jede Form einzeln fuellen, Position/Groesse nicht veraendern, Rest bleibt
  Alpha 0.
- Technikkette PFLICHT: image_gen-Master -> Modalfarben-Downscale (kein
  simples Resize!) -> hoechstens 40 Farben -> Alpha hart 0/255 -> 1px-Kontur
  in dunklerem Eigenton (x0.5), nie Schwarz/Grau. Keine PIL-Formen (`draw.rectangle` etc.) fuer die Endkunst --
  das sieht man sofort als Vektor-Look.
- PIL nur mit `PYTHONPATH=/home/blackoffset/dev/pylib python3`, kein venv.
- Nur unterhalb von `build/` schreiben.
- Junction-Form: ausgegraute Zellen sind fremdes Material, bleiben
  unveraendert.
- Tuer rot0/rot2 werden im Spiel gespiegelt verwendet -- von beiden Seiten
  lesbar malen.

## Bekannte Fallen

- `NECESSE_SPRITES` zeigt NICHT automatisch auf den echten Vanilla-Dump
  (`/home/blackoffset/dev/Necesse sprites`) -- `codex_wall.py` setzt die
  Umgebungsvariable selbst; wer die drei Basiswerkzeuge einzeln aufruft, muss
  das von Hand tun.
- `PYTHONPATH` fehlt in der Standard-Shell -- `wall_from_layout.py` bricht
  sonst mit `ModuleNotFoundError: PIL` ab, OHNE dass ein aufrufendes Skript
  das automatisch als Fehler erkennt (Exitcode 1 sieht wie eine legitime
  Nahtwarnung aus). `codex_wall.py` setzt `PYTHONPATH` selbst; bei eigenen
  Aufrufen der Basiswerkzeuge immer voranstellen.
- `wall_from_layout.py --new` schreibt IMMER nach `build/qa/wallpaint/`,
  nicht konfigurierbar -- `codex_wall.py` kopiert danach nach
  `build/codexwall/NAME/` und raeumt das Referenz-Zwischenpaar (`*_ref-*`)
  wieder auf. Bei Handarbeit an dieser Stelle die Zwischendateien selbst
  loeschen, sonst sammeln sie sich in `build/qa/wallpaint/`.
- `conform_wall_sheet.py` kann Codex-Kunst nicht reparieren, die eine
  durchgehende Illustration ueber Zellgrenzen ist -- das meldet es nur
  (`redraw`), gemessen an der Farbzahl. Wenn das passiert, ist die Ursache
  fast immer, dass Codex `paint.png`s Formgrenzen ignoriert hat -- Brief
  praeziser machen (z. B. jede Form als SEPARATES Bild anfordern) statt am
  Nachbearbeiten zu drehen.
- Hoechstens EIN Codex-Lauf gleichzeitig im ganzen Repo (Kontingent, siehe
  Nutzer-Memory "Parallele Jobs im selben Baum"). Vor dem Start `ps aux |
  grep codex` pruefen.
- Codex-Log auf "usage limit" pruefen, bevor man einen leeren `filled.png`
  als Bug im eigenen Werkzeug missversteht.

## Naechste Schritte (offen)

- `--finish` prueft nicht automatisch, ob `filled.png` dieselbe Groesse wie
  `paint.png` hat -- `wall_from_layout.py` schneidet stur nach Koordinaten;
  bei Groessenabweichung entstehen falsche Zellen ohne Fehlermeldung. Vor
  dem naechsten Lauf eine Groessenpruefung ergaenzen.
- Bisher nur EIN Vanilla-Referenzmaterial pro Lauf (`--vanilla-ref`). Fuer
  Materialien ohne gute Vanilla-Entsprechung (z. B. Himmelsstein) waere ein
  zweites, thematisch naeheres Referenzblatt (falls vorhanden) hilfreicher
  als `stonewall`.
