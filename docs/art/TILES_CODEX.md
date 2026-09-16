# Codex-Pipeline für Boden-Splats

Nicht committen -- Arbeitsnotiz für die Codex-Pipeline zu `tiles/<name>_splat.png`.

## Befund

`TerrainSplatterTile.getTerrainTexture` (1.3.2 und 1.3.3, aus dem
Dekompilat gemessen) würfelt **jede volle Bodenkachel einzeln**: einen
zufälligen 96px-Block, darin zufällig eine der vier vollen Zellen
(Spalte 3..6, Zeile 0 je Block, 32×32 px). Die übrigen 17 Zellen je Block
sind Alpha-Übergangsstücke zu Nachbarböden (siehe
`docs/research/splat-format.md` §5.3).

Folge: zwei benachbarte Kacheln im Level zeigen fast immer zwei unabhängig
gewürfelte Vollzellen nebeneinander -- nie ein zusammenhängendes Motiv. Ein
Blatt, das als durchgehende Fläche gemalt ist (Motive laufen über
Zellkanten), zeigt deshalb ein sichtbares Raster. Ein Blatt, dessen Motive
innerhalb der Zelle bleiben und dessen Zellrand ruhig/einheitlich ist,
wirkt nahtlos (vgl. `junkfloor_splat.png`).

## Werkzeuge

### `tools/splat_seam_audit.py`

Misst für ein oder mehrere `_splat.png`-Blätter den mittleren
Helligkeitskontrast an der Naht zufällig gepaarter Vollzellen (waagerecht
und senkrecht, standardmäßig 3000 gewürfelte Paare) gegen den mittleren
Kontrast innerhalb einer Zelle. PASS/FAIL bei Verhältnis > 1,3 (Vorschlag,
per `--threshold` änderbar). `--vanilla` prüft zusätzlich
`grass_splat.png`, `sand_splat.png`, `junkfloor_splat.png` aus dem
Vanilla-Dump (Pfad über `size_audit.default_vanilla()`) als Referenz.
Schreibt je geprüftem Blatt ein Feldbild (12×12 Kacheln, gewürfelt wie
`scene_preview.ground_tile`, 2× skaliert) nach `build/qa/splatseam/`.

```
PYTHONPATH=/home/blackoffset/dev/pylib python3 tools/splat_seam_audit.py \
    src/main/resources/tiles/cloudturf_splat.png --vanilla
```

Gemessene Werte (16.09., diese absolute Skala ist Werkzeug-eigen, nicht
identisch mit von Hand geschätzten Werten -- die Rangfolge ist der Befund):

| Blatt | Kante | Innen | Verhältnis | Status |
|---|---|---|---|---|
| cloudturf (alt) | 45,5 | 9,4 | **4,81** | FAIL |
| junkfloor (Vanilla) | 8,5 | 5,2 | 1,63 | FAIL* |
| grass (Vanilla) | 0,85 | 0,62 | 1,36 | FAIL* |
| sand (Vanilla) | 2,65 | 2,23 | 1,19 | PASS |

\* Grass und Junkfloor liegen nahe der Schwelle 1,3; absolut ist ihr Kontrast
(0,6-8,5) winzig gegenüber cloudturf (45,5) -- die Metrik zeigt cloudturf als
klaren Ausreißer, aber die Schwelle 1,3 ist noch nicht kalibriert genug, um
Vanilla zuverlässig als PASS einzustufen. Vor Produktivbetrieb: Schwelle
gegen mehr Vanilla-Blätter eichen oder auf einen Absolutwert statt Verhältnis
umstellen.

### `tools/codex_splat.py`

Bereitet einen Codex-Lauf vor: Zellraster-Vorlage (rot markierte Vollzellen
auf dem Referenzblatt), Vanilla-Referenz (`junkfloor_ref.png`), fertiger
Brief. Startet Codex NICHT automatisch (max. ein Lauf gleichzeitig,
`RUN.sh` liegt bereit).

```
PYTHONPATH=/home/blackoffset/dev/pylib python3 tools/codex_splat.py cloudturf \
    --theme "sattes Gruen, hellgruene Huegelkuppen, tuerkis Akzente" \
    --like src/main/resources/tiles/cloudturf_splat.png
sh build/codexsplat/cloudturf/RUN.sh
```

Nach dem Codex-Lauf: `--compose build/codexsplat/<name>/splat.png` setzt die
neuen Vollzellen ein und übernimmt für die 17 Übergangszellen je Block die
Alpha-Maske des Referenzblatts, aber die Farbe aus Spalte 3 derselben Zeile
(weltverankert, kein Stempel-Bruch an Kachelkanten). Ergebnis:
`build/codexsplat/<name>/splat_composed.png`.

## Cloudturf: erster Lauf

Brief + Referenzen von Hand vorbereitet (identischer Aufbau wie
`codex_splat.py` liefert) unter `build/codexsplat/cloudturf/`:
`cloudturf_splat_alt.png` (Farbwelt-Referenz, = aktuelles Blatt),
`junkfloor_ref.png` (Nahtlosigkeits-Referenz), `brief.md`.

Codex-Lauf im Hintergrund gestartet:

```
nohup /home/blackoffset/.npm-global/bin/codex exec \
  -C /home/blackoffset/projekte/necesse-mod/ablage \
  -s workspace-write --skip-git-repo-check \
  -i build/codexsplat/cloudturf/cloudturf_splat_alt.png \
  -i build/codexsplat/cloudturf/junkfloor_ref.png \
  -o build/codexsplat/cloudturf/last.txt \
  "$(cat build/codexsplat/cloudturf/brief.md)" \
  > build/codexsplat/cloudturf/codex_run.log 2>&1 &
```

Log: `build/codexsplat/cloudturf/codex_run.log`. Ergebnis erwartet unter
`build/codexsplat/cloudturf/splat.png`.

**Ergebnis (19:04, ~3 Min. Laufzeit):** `build/codexsplat/cloudturf/splat.png`,
224×576, 16 Farben. Codex hat pro Zelle per Skript modal herunterskaliert
(keine PIL-Formen) und den 2px-Rand jeder Zelle vereinheitlicht. Seam-Audit:

```
splat.png (neu)        Kante  0.00  Innen  8.79  Verhaeltnis 0.00  PASS
cloudturf_splat.png alt Kante 45.48  Innen  9.45  Verhaeltnis 4.81  FAIL
```

Feldbild (`build/codexsplat/cloudturf/qa/splat_field.png`) bestätigt: kein
Raster mehr sichtbar, jedes Motiv (Busch/Baumform, blaue Beerenkuppen) bleibt
in seiner Zelle. Das Nahtlosigkeits-Problem ist gelöst.

**Aber:** Farblich ist das Ergebnis ein gesättigtes, eher dunkles Moos-/
Tannenwald-Grün -- nicht die (nachträglich vom Nutzer gewünschte) "ruhige,
aber fröhliche" helle, freundliche, weiche Himmelsreich-Stimmung. Folgebrief
mit unveränderter Technik, aber hellerer/pastellener Palette liegt bereit:
`build/codexsplat/cloudturf_v2/` (brief.md + ref1_technik.png = das neue
Blatt als Technik-Referenz, Farbe NICHT übernehmen).

## Hausböden (Auftrag vom 16.09., 19:04): stylische Alternative zu Standard-Holz

Drei Aufträge vorbereitet (Briefs + Referenzen unter `build/codexsplat/`),
noch NICHT gestartet:

- `nimbusfloor/` -- helles Wolkenholz-Fischgrät-Parkett mit Goldintarsien
- `prismfloor/` -- Terrazzo-Mosaik in Pastell mit Prismasplittern
- `gloomwoodfloor/` -- dunkles poliertes Ebenholz mit grün glimmenden Intarsien

Jeder Ordner enthält `brief.md`, `ref1_alt.png` (aktuelles Blatt),
`ref2_junkfloor.png` (Vanilla-Nahtlosigkeit) und `ref3_technik_beispiel.png`
(das fertige cloudturf-Blatt als Beweis, dass die Zell-Technik funktioniert --
Farbe davon NICHT übernehmen). `charfloor_splat.png` existiert ebenfalls,
aber ohne konkretes Thema vom Nutzer -- noch nicht vorbereitet.

Start (respektiert die eingebaute SLOTS=2-Drosselung von `codex_art.sh`):

```
tools/codex_art.sh build/codexsplat/cloudturf_v2 build/codexsplat/nimbusfloor \
    build/codexsplat/prismfloor build/codexsplat/gloomwoodfloor
```

## Nächste Schritte

- Schwelle in `splat_seam_audit.py` gegen mehr Vanilla-Blätter eichen (grass/
  junkfloor liegen knapp über 1.3, obwohl sie im Spiel ruhig wirken --
  Verhältnis ist noch nicht die perfekte Metrik, Absolutwert evtl. besser).
- `codex_splat.py --compose` an einem echten Codex-Ergebnis durchtesten (noch
  nicht gemacht; der erste cloudturf-Lauf brauchte es nicht, Codex hat Alpha
  selbst sauber übernommen).
- cloudturf_v2 + die drei Hausboden-Aufträge oben starten und auswerten.
- Wenn cloudturf_v2 durchläuft: gleiches Rezept für weitere raster-auffällige
  Splats (`splat_seam_audit.py --vanilla` bzw. gegen alle `tiles/*_splat.png`
  im Mod als erster Scan, welche Blätter überhaupt betroffen sind).
