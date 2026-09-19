# Brief — Friseursalon der Stylistin (6 Blätter)

Sechs Objekte, die die Vanilla-Stylistin als Ladenausstattung verkauft. Der
Code steht (`src/main/java/stairwaytoheaven/SalonWares.java`); jedes Blatt, das
in den Jar kommt, wird beim nächsten Start registriert und steht in ihrem Laden.
**Fehlt ein Blatt, fällt genau dieses Objekt still weg** — es ist also erlaubt,
die sechs einzeln zu liefern.

Stil: Vanilla-Necesse-Pixelart, 1:1, keine Skalierung, keine Weichzeichnung.
Kontur wie Vanilla (1–2 px, innere Lage als dunkler Eigenton, nie ganz
weggeschnitten). Palette wie vanilla-Möbel: warmes Holz, Messing, gedecktes
Violett/Rosé für den Salon.

---

## 1. `objects/salonchair.png` — Friseurstuhl

- **Blatt 128 × H**, vier Spalten à 32 px, eine je Blickrichtung.
- **Spaltenreihenfolge ist Code, nicht Geschmack:** Spalte 0 = von **hinten**
  gesehen (Rückenlehne zum Betrachter, Figur sitzt davor), dann im selben
  Umlauf wie der Vanilla-Stuhl `objects/chair.png` — dieses Blatt daneben legen
  und Spalte für Spalte vergleichen.
- Höhe wie der Vanilla-Stuhl (der Code liest `texture.getHeight()`, gezeichnet
  wird bottom-anchored: unterste Zeile liegt auf der Kachelunterkante).
- Motiv: hydraulischer Salonsessel, breite Fußplatte, Fußstütze vorn,
  Kopfstütze. Lehne hoch genug, dass sie den Sitzenden rahmt.

## 2. `objects/paintings/salonmirror.png` — beleuchteter Friseurspiegel

- **Blatt 32 × 128**, vier Zellen à 32 × 32, eine je **Wandrichtung**
  (PaintingObject-Raster, wie `magicmirror`/`hauntedwallclock`).
- Er hängt **an der Wandfläche**, nicht an der Decke: das Motiv sitzt im
  oberen Teil der Zelle auf der Wand, nicht mittig schwebend. Vorher auf eine
  echte Wand legen (`tools/align_wall_piece.py`), Wandfläche ist 48 px.
- **Seitenansichten schmal:** in den beiden seitlichen Zellen ist der Spiegel
  nur wenige Pixel breit (Kante + Rahmen), genau wie ein Bild von der Seite.
- Motiv: ovaler oder rechteckiger Spiegel mit Glühbirnenkranz. Die Birnen
  dürfen hell sein — das Objekt trägt im Spiel eigenes Licht (Stufe 100,
  fast weiß), die Zeichnung muss das nur andeuten, nicht simulieren.

## 3. `objects/salonsign.png` — beleuchtetes Friseurladen-Schild

- **Blatt 64 × 128**: zwei Spalten (links **an**, rechts **aus**) × vier
  Zeilen (vier Anbau-Ausrichtungen), Raster wie `objects/walllantern.png`.
  Dieses Vanilla-Blatt als Vorlage öffnen und die Zellenaufteilung 1:1
  übernehmen — der Code erbt `WallTorchObject` komplett.
- Motiv: kleines Ladenschild an einem Ausleger, Schere oder Kamm als Zeichen,
  Leuchtschrift-Rand. Im „aus"-Zustand dieselbe Form, nur ohne Leuchten.
- Lichtfarbe im Code ist rosé/magenta (Hue 320, Sättigung 0,45) — die
  Leuchtelemente in dieselbe Richtung färben.

## 4. `objects/saloncashregister.png` — Kasse

- **Blatt 128 × H**, vier Spalten à 32 px, eine je Blickrichtung
  (TableDecorationObject: `sprite(rotation, 0, 32, height)`).
- Steht **auf einem Tisch**, nicht auf dem Boden: Unterkante bündig, Höhe so,
  dass sie auf einer Vanilla-Tischplatte sitzt wie `thingbox`.
- Motiv: messingne Registrierkasse, Tastenfeld, Zahlenanzeige oben, Schublade.
  In den vier Richtungen dreht sich die Front sichtbar mit.

## 5. `objects/salonproducts.png` — Pflegemittel-Tablett

- Gleiches Raster wie die Kasse: **128 × H**, vier Spalten.
- Motiv: flaches Tablett mit Flaschen, Scheren, Kamm, Bürste. Kleiner und
  flacher als die Kasse.

## 6. `objects/salonbarberpole.png` — Barbier-Säule

- **Blatt 32 × H** (eine Spalte; mehrere 32er-Spalten wären Zufallsvarianten —
  hier nicht gewollt, also genau eine).
- Bottom-anchored, steht auf dem Boden, **keine Kollision**.
- Motiv: die klassische rot-weiß-blaue Wendel unter Glaszylinder, Messingkappe
  oben und unten. Trägt im Spiel eigenes Licht (Stufe 90).

---

## Prüfung nach der Lieferung

1. `tools/sheet_format_audit.py` und `tools/direction_sheet_check.py` über die
   sechs Blätter.
2. `tools/rotation_preview.py` für Stuhl, Kasse und Tablett — die vier
   Richtungen müssen sich unterscheiden und in der Vanilla-Reihenfolge stehen.
3. `tools/align_wall_piece.py` für Spiegel und Schild, auf eine echte Wand
   gelegt: hängt es an der Wandfläche, nicht darüber?
4. Größenvergleich gegen das Vanilla-Original derselben Bauart
   (`tools/size_audit.py`).
