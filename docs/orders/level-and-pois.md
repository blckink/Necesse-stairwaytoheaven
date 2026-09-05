# Auftrag: Die Skyreach muss beim ersten Betreten fertig wirken

**Zweig:** `claude/skyreach-worldgen-pois` (neu anlegen von `origin/master`)
**Angelegt:** 2026-09-06 vom Lead, auf Ansage des Nutzers
**Eigene Sitzung.** Der Lead arbeitet parallel an Sprites/Tiles und fasst
**nichts** unter `worldgen/` an.

## Worum es geht

> „Sobald der Spieler das erste Mal Skyreach erreicht, soll die Welt sich
> vollständig anfühlen. Der Spieler muss mind blown sein, wenn er die
> Himmelstreppe nutzt. Dazu brauchen wir POIs und Orte mit Straßen, Häusern
> etc."

Das ist das Ziel. Alles Folgende ist Befund, nicht Vorgabe — wenn du einen
besseren Weg siehst, nimm ihn und sag warum.

## Der Stand, gemessen am 2026-09-06

Die dreizehn bewohnten Orte **existieren und sind registriert**:

- `src/main/java/stairwaytoheaven/worldgen/pois/RealmPoiPresets.java`
- `src/main/java/stairwaytoheaven/worldgen/pois/RealmPoiWorldPreset.java`
- registriert in `StairwayToHeavenMod.java:73-74` als `swh_realmpois`
- aus PR #5 (`453dac2`, gemergt als `593e5e5` am 2026-09-04)

Der Spieler hat sie im Spiel **nicht gefunden**. Untersucht, mit diesen
Befunden:

1. **Sie stehen nur in der Skyreach** — `shouldAddToRegion` lässt nur
   `SkyRegistry.SKYREACH_IDENTIFIER` zu, Level `skyreach2`. Auf der Oberfläche
   gibt es keine. Das allein könnte den Befund schon erklären.
2. **Dichte ist nicht das Problem.** Die Platzierung ist ein deterministischer
   Hash (`SkyNoise.hash`, `CELL=220`, `SITE_CHANCE=0.42`); exakt nachgerechnet
   über 40 Seeds: **~41 Kandidaten je 2048×2048**, mittlerer Abstand ~339
   Kacheln. Die 100-Kachel-Sperre um den Warden Spire verwirft im Mittel 0,3.
3. **Verdacht Nummer eins: die Geländeprüfung.** `validSite` verlangt neun
   Stichproben, alle auf Land; in der Skyreach heißt „Land" schlicht *nicht
   `mistsea`*. Die Grundflächen sind groß — Sky Town **57** Kacheln breit,
   Hell Administration **61**, Sky Tower **49**. Skyreach ist konzeptionell
   *schwebende Inseln über einem Nebelmeer*. Ist die typische Insel kleiner als
   die Grundfläche, fällt fast jeder Kandidat lautlos durch.
4. **Verdacht Nummer zwei:** der ganze Fußabdruck muss in **eine**
   Preset-Region passen (`if (x < startX || … ) continue;`).

**Nichts davon ist gemessen.** Punkt 3 und 4 sind Hypothesen aus dem Quelltext
— `VERIFIED [jar]`, nicht `[run]`.

## Der eigentliche Skandal: kein Gate deckt das ab

`scripts/integration_test.sh` zählt **nur** die Oberflächen-POIs
(`CraterGeneration`, `CampGeneration`, `ShrineGeneration` — das ist
`swh_surfacepois`, ein **anderes System**). Das Wort `realmpoi` kommt im
Testskript nicht vor; `SkyreachStatusCommand` meldet sie auch nicht.

Diese dreizehn Orte könnten seit dem 4. September zu **null** erzeugt werden,
und der Integrationstest wäre trotzdem „Exit 0, 0 FAIL" gewesen — war er auch,
am 2026-09-05 um 22:08.

**Fang hiermit an, nicht mit dem Bauen.**

## Fertig, wenn

1. **Ein Zensus existiert und läuft**, analog zu dem, den
   `surface/SkySurfaceStatusCommand.java:241` schon für die Oberfläche druckt
   (`poi census: presetregions=… total=… perpresetregion=…`). Er muss je
   Realm-Band ausweisen: Kandidaten · nach `validSite` übrig · wirklich
   gesetzt. Damit ist die Frage beantwortet statt plausibel.
2. **Der Zensus hängt als Gate in `scripts/integration_test.sh`**, mit einer
   Untergrenze je Realm. Ein Ort, der nie erzeugt wird, färbt den Test rot.
3. **Der Spieler findet beim ersten Aufstieg etwas.** Konkret prüfbar: in
   Sichtweite der Landung (`RealmLanding`) beginnt eine Straße oder ein Weg,
   der zu einem bewohnten Ort führt. Die 100-Kachel-Sperre um den Spire ist
   für „unverstellter erster Landmark" gedacht — sie darf nicht dazu führen,
   dass in Laufweite gar nichts steht.
4. Gates grün: `./gradlew buildModJar`, `scripts/integration_test.sh`,
   `python3 tools/content_ledger.py --check`, `python3 tools/locale_audit.py`
   (33 Probleme sind vorbestehend, keine neuen).

## Besitzt diese Dateien

- `src/main/java/stairwaytoheaven/worldgen/**` (alles darunter)
- `src/main/java/stairwaytoheaven/commands/SkyreachStatusCommand.java`
- `scripts/integration_test.sh`
- `docs/PLAN_ONE_PLANE.md`, `docs/design/chapter-01-skyreach-*.md`
- neue Doku unter `docs/` zu Worldgen

## Nicht anfassen

- `tools/asset_*.py`, `tools/mob_sheet_intake.py`, `tools/palette_reduce.py`,
  `src/main/resources/tiles/**`, `src/main/resources/mobs/**` — **der Lead
  arbeitet dort parallel an Sprites und Tiles.**
- `docs/AGENT_BOARD.md`, `docs/orders/` — gehören dem Lead
- `src/main/resources/kk-sprites/_incoming/` — Zulieferung des Nutzers
- Keine Entscheidung aus `docs/DESIGN_DECISIONS.md` stillschweigend umdrehen.
  Insbesondere **`docs/PLAN_ONE_PLANE.md` ist Architekturgesetz**: eine
  Änderung, die eine Dimension hinzufügt, bricht es.

## Vorher lesen

`AGENTS.md` · `docs/PLAN_ONE_PLANE.md` · `docs/WORLD_DESIGN.md` (die
Verfassung) · `docs/OVERVIEW.md` (was wirklich läuft) ·
`docs/FOGKEY_AND_BOSSPORTALS.md` · `docs/design/chapter-01-skyreach-pois.md`
(14 Orte mit Raumplänen, schon entworfen) ·
`docs/design/chapter-01-skyreach-cast.md`

## Prüfstufe

`docs/IMPLEMENTATION_RULES.md` §14. Der Integrationstest läuft gegen den
Dedicated Server; der rendert nicht. **`[run]` ist nicht `[game]`** — „der
Zensus zählt 12 Orte" ist nicht „der Spieler sieht eine Stadt".

## Abschluss

Auf den eigenen Zweig committen und **nur dorthin** schieben. Nicht nach
`master` mergen. Melden: geänderte Dateien · welche Gates liefen und mit
welchem Ergebnis · was der Zensus **wirklich zählt** · was offen blieb.

Laufzeit: dieser Auftrag braucht `AETHERGATE_JOB_TIMEOUT=3600` — ein
Serverlauf passt nicht in 30 Minuten.
