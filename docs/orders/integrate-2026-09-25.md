# Auftrag: Zusammenführen und weiterbauen (Übergabe 2026-09-25)

**Für:** den Claude, der über Äthergate auf dem PC des Spielers läuft.
**Zweig:** `claude/integrate-2026-09-25` (neu anlegen, siehe Schritt 1)
**Ausgangspunkt:** `origin/master` bei `40653a5`
**Angelegt:** 2026-09-25 von der Cloud-Sitzung „Mod Texturen, NPCs, Quests und Gebietsgeneration“
(`session_01DWRtSGhK4dnCftnFZq9nd1`), auf Ansage des Spielers:
> „Übergabe für Claude in Äthergate + Info, dass er sinnvoll zusammenführen soll, mit To-dos.“

Laufzeit: braucht `AETHERGATE_JOB_TIMEOUT=3600` oder mehr. Ein einzelner Lauf von
`scripts/integration_test.sh` dauert etwa 5 Minuten, `scripts/regenerate_check.sh` ebenso.

---

## 0. Worum es geht

Es gab zwei parallele Arbeitsstränge:

1. **Die Cloud-Sitzung.** Sie hat zwischen dem 23. und 25.09. 25 Commits auf `master`
   gebracht (`96aa406..40653a5`, Liste in §2).
2. **Deine eigenen Sitzungen auf diesem PC** (Remote-Control, „petrolscribble…“). Die
   haben am 24.09. abends Übergaben geschrieben:
   - `plan/WEITERMACHEN-2026-09-25-selbstheilung.md` (bzw. `…-selfheilung.md`)
   - `plan/WEITERMACHEN-2026-09-25.md`

   **Berichtigt 2026-09-25 (Auftrag In-1369d8):** Diese Annahme stimmt nicht. Beide
   Dateien liegen unter `~/aethergate/plan/` und sind Übergaben der Äthergate-Arbeit
   selbst (PWA, Modellwahl, Dateiablage, WSL-Hänger, VPS), keine Mod-Arbeit; das Wort
   „Necesse“ kommt darin nicht vor. Im Mod-Repo gibt es kein `plan/`, keinen Zweig
   `local/stand-2026-09-24` und keinen Stash. Es gab also **keine** ungepushte
   Mod-Arbeit vom 24.09. zusammenzuführen; §1 Schritte 1, 2 und 4 entfallen.
   `claude/integrate-2026-09-25` ist inzwischen in `master` gemergt (`a80d895`).

**Deine Aufgabe:** beides **sinnvoll zusammenführen** und dabei nichts überschreiben,
danach die To-dos in §4 abarbeiten, in der Reihenfolge ihrer Priorität. Sinnvoll heißt:
- Arbeit, die dasselbe Problem zweimal löst, wird **eine** Lösung. Nimm die bessere,
  begründe die Wahl im Commit und in `docs/TECHNICAL_LEARNINGS.md`.
- Nichts, was im Spiel schon funktioniert, darf dabei verloren gehen.

## 1. Zusammenführen — Schritt für Schritt

1. Im lokalen Repo `git status` und `git log --oneline -20` anschauen. Gibt es
   uncommittete Änderungen oder lokale Commits, die nicht auf `origin` sind? Dann
   **zuerst** auf einem eigenen Sicherungszweig committen
   (`git switch -c local/stand-2026-09-24 && git commit -am …`). Niemals verwerfen.
2. Beide Übergabe-Dateien unter `plan/` vollständig lesen. Notieren, was sie als
   erledigt, halb fertig und offen beschreiben.
3. `git fetch origin`, dann `git switch -c claude/integrate-2026-09-25 origin/master`.
4. Den lokalen Stand hineinholen: `git merge local/stand-2026-09-24`, oder
   Cherry-Picks, falls er von einem alten Stand abzweigt. **Keine Rebases fremder
   Zweige, kein Force-Push.**
5. Konflikte lösen. Die heißen Stellen, an denen die Cloud diese Woche am meisten
   geändert hat:

| Datei | Was die Cloud dort geändert hat |
|---|---|
| `worldgen/pois/RealmPoiPresets.java` | Stadt/Gasthaus/Zollbrücke als Pläne, alle alten Orte neu eingerichtet, Kapitel-02-Orte (27–32), `dryRing` an 11 Plänen, `clearTreeRing`, Schreibtisch-Deko entfernt |
| `worldgen/pois/RealmPoiWorldPreset.java` | Hoard-Gitter, `skyreachRescues` (jede Ortsart mindestens einmal), `SPIRE_KEEP_CLEAR = 64` auf den ganzen Grundriss, Turm-Kreuztest |
| `worldgen/SkyLandscape.java`, `SkyTerrainPainter.java` | Straßen enden am Eingang, ein Tor pro Straße, 4 Platzarten mit wechselnder Mitte, `SURFACE_POOL` |
| `level/SkyLevel.java` | Wächter dürfen auf Gras stehen, Region-übersprungen-Meldung, Katzen |
| `quest/CatHome.java` | **Wichtig:** `loadAround` darf nicht ungenerierte Regionen laden (Fix `ec61133`). Wenn der lokale Stand die Katzensuche weiter umgebaut hat, diese Regel unbedingt behalten. Begründung in TECHNICAL_LEARNINGS, Abschnitt „A region loaded without generation is empty AND ‚generated‘ forever“ |
| `commands/SwhResetCommand.java` | `regenerate`, Fix für `quests` ohne `confirm`, Leiter-Reset, `NAMED_RESIDENTS` |
| `mobs/*`, `settlement/*` | Dorf-Bewohner an ihr Haus gebunden, Dorian E1–E5, Arzt, Prototype Nine gebunden |
| `src/main/resources/locale/*.lang` | Umlaute, Wächter/Himmelswacht-Benennung, viele neue Schlüssel |
| `scripts/integration_test.sh` | viele neue Prüfungen, `INTEGRATION_SEED=<Seed-String>` für feste Welten |
| `docs/TECHNICAL_LEARNINGS.md`, `OVERVIEW.md`, `MOD_SUMMARY.md` | wurden nur hinten angehängt; bei Konflikten beide Seiten behalten |

6. Gates laufen lassen (§3). Ein rotes Gate zuerst mit `INTEGRATION_SEED` nachstellen
   und die Ursache suchen. **Nicht als „Flake“ abtun.** Jeder angebliche Flake der
   letzten zwei Tage war ein echter, vom Welt-Seed abhängiger Fehler; die Tabelle dazu
   steht in TECHNICAL_LEARNINGS unter „seed flakes“.
7. Auf `claude/integrate-2026-09-25` pushen. Mergen nach `master` nur, wenn der Spieler
   es sagt.

## 2. Was seit `96aa406` auf master neu ist (Stand `40653a5`)

Alles ist gegen den Dedicated Server getestet (**[run]**), im echten Spiel ist noch
**nichts** gesehen (**[game]** fehlt).

- **Welt:**
  - Straßen und Plätze neu:
    - Tor-Gruppen auf drei Seeds von 99/132/124 auf 50/57/35;
    - keine 4–7 Tore breiten Reihen mehr;
    - Plätze seltener (0,42 statt 0,66), dazu 4 Platzarten statt 3.
  - Stadt, Gasthaus und Zollbrücke als eingerichtete Pläne. Alle übrigen Gebäude und
    Orte zweckgebunden eingerichtet (`8024b80` / `39a7e34`).
  - Kapitel 02: sechs Hort-Orte mit Mimics und Boss-Wächtern (`d9bc852`,
    `docs/design/chapter-02-hoards-and-mimics.md`).
  - **Kapitel 03, Spire-Dorf:**
    - 12 Gebäude um den Spire, 9 Bewohner mit Heimbindung;
    - Questleiter mit 20 Stufen und einzigartigen Belohnungen;
    - Entwurf in `docs/design/chapter-03-spire-village.md`;
    - Spielerentscheidung eingetragen in `docs/DESIGN_DECISIONS.md`.
- **Seed-Fehler, behoben:**
  - Katzensuche ließ Regionen leer (`ec61133`), dadurch fehlten ganze Gebäude;
  - Turm nirgends platziert (`d6e90ff`);
  - Prototype Nine lief weg (`8bbf069`);
  - Wachen fanden auf Wiesen keinen Platz (`79d4077`);
  - Sovereign's Anvil verlor Geröll (`0839445`);
  - Ortsarten verhungerten, Outland-Probe, Baum neben Baum, Orte zu nah am Spire
    (`6e33eea`, `b05707f`);
  - Wasser löschte Möbel an sieben weiteren Orten (`decf765`).
- **Inhalte:**
  - Wolkengleve und Himmelslanze;
  - Questbelohnungen neu abgestimmt (`docs/quests.md`);
  - Item-Kategorien und Naturdrops (`docs/ITEM_CATEGORIES.md`, mit `CategoryCensus`-Gate);
  - Rezepte für Seelenwebstuhl und Geisterschmiede;
  - Abenteurer-Tagebuch (`journal/`, liest die Questleiter über `QuestLadderSource`);
  - Dorian lesbar gemacht (E1–E5 aus `docs/design/concept-nightbound-dorian.md`).
- **Werkzeuge:**
  - `/swhreset regenerate [confirm]`: ganzen Himmel neu, Fortschritt bleibt, mit
    Sicherung; Test `scripts/regenerate_check.sh`;
  - `/swhshowroom`, `/swhshots`, `/swhdumpsprites`, `scripts/preset_render.sh`
    (`docs/PREVIEW_TOOLS.md`);
  - `tools/draw_rect_audit.py`: Banner-Fix `a074c38`; Wanddeko wird nach den
    Zeilen-Offsets der Engine geprüft;
  - `INTEGRATION_SEED`.
- **Texte:**
  - alle deutschen Umlaute, Tippfehler und Doppel-Schlüssel bereinigt;
  - Wächter/Himmelswacht einheitlich;
  - **keine Chat-Nachrichten** aus der Mod (Spielerregel; das Tagebuch kommt ohne
    Chatzeile);
  - Nachschlagewerk: `docs/KOMPLETTUEBERSICHT.md` (alle Auslöser, Quests, NPCs,
    Dialoge, Items, Flags, Unstimmigkeiten).

Gate-Stand auf `decf765` (Mod-Stand identisch mit `40653a5`, dort wurde nur die
Katzenprüfung gelockert):
- Integrationstest 3 Läufe: 2× `PASS: mod loads, Skyreach generates, world survives a
  restart, no errors.`, 1× rot, weil die Katzen-Prüfung zu streng war (in `40653a5`
  gelockert, danach nicht erneut gelaufen).
- `regenerate_check` PASS.
- `locale_audit` OK, `content_ledger --check` OK, `plan_transcription_audit` 0 Flags.

## 3. Gates

```bash
export NECESSE_GAME_DIR=<pfad>                 # scripts/fetch_dedicated_server.sh druckt ihn
./gradlew buildModJar
python3 tools/locale_audit.py                  # OK
python3 tools/content_ledger.py --check        # OK
python3 tools/plan_transcription_audit.py      # 0 flag(s)
python3 tools/draw_rect_audit.py               # OK
python3 tools/sheet_format_audit.py            # OK
scripts/integration_test.sh                    # mindestens 3 Läufe, alle PASS
INTEGRATION_SEED=6xK4d scripts/integration_test.sh   # früher rot, muss PASS
INTEGRATION_SEED=F6mfM scripts/integration_test.sh   # früher rot, muss PASS
scripts/regenerate_check.sh                    # PASS
```

Auf dem PC gibt es einen Client. **Nutze ihn:** `/swhdumpsprites`, dann
`tools/preset_render.py --sprites <dump>` für echte Vorschaubilder, und
`/swhshots` für echte Screenshots. Damit werden Aussagen zu **[game]**.

## 4. To-dos, nach Priorität

### A. Sofort (Voraussetzung, damit der Spieler testen kann)
1. **Zusammenführen** wie in §1, alle Gates grün.
2. **Einmal im echten Spiel durchspielen** und jeweils mit [game] belegen, am besten mit
   Screenshot:
   - `/swhreset regenerate confirm` in einer Kopie des Spieler-Spielstands, auch im
     Einzelspieler, und mit einem Spieler, der gerade im Himmel steht (nur [run]
     bewiesen, ohne verbundenen Spieler);
   - das Spire-Dorf ansehen, eine Leiter-Quest annehmen und abgeben, Belohnung
     prüfen;
   - das Tagebuch öffnen: Fenster, Esc, Umbrüche;
   - Dorian: Dialog, Blutschale, Siedlungsmeldung, Tinktur beim Arzt;
   - `/swhshowroom build`, `/swhshots day`, `/swhdumpsprites`: laufen die überhaupt?
     Sie sind nur aus dem Quellcode abgeleitet ([jar]);
   - Wolkengleve und Himmelslanze: Umfärbung sichtbar?
   - Banner an der Wand, Kerzenständer aus, Teppiche unter Stühlen, Bänke zum Teich.
3. **Alte Spielstände:** Welten, die mit `ec7dee2` gelaufen sind, können leere Regionen
   nahe den Katzenverstecken haben. `/swhreset regenerate` sollte das beheben. Prüfen
   und in `docs/SAVE_COMPAT.md` festhalten.

### B. Ausbau, den der Spieler ausdrücklich will (Kapitel-03-Rückstand, `chapter-03-spire-village.md` §6)
4. Einsiedler (gute NPCs) an abgelegenen Orten, mit eigener Quest und einzigartiger
   Belohnung.
5. Wandernde Händler mit echter Route: Dorf ↔ Orte in den Reichen.
6. Böse NPCs: Banditenlager, Schurken mit Namen und eigenen Quests (Kopfgeld).
7. Dörfer in den tieferen Reichen (Eden, Steinfeld, Geisterreich …), jeweils mit
   Handel und Quests.
8. Große Arenen mit Wellen oder Boss, mit Belohnung.
9. Dorian **E6**: Questreihe „Tageswandler“ (Konzept §3.6).
10. Eine Quest, die den Seelenwebstuhl freischaltet (`WORLD_DESIGN.md` wünscht das).
    Heute ist die Material-Hürde nur ein Platzhalter.

### C. Aufräumen und Qualität
11. `docs/KOMPLETTUEBERSICHT.md` §8: alle Punkte, die noch nicht „BEHOBEN“ sind, z. B.:
    - Rückkehr-Tore und Himmelsanker werden nirgends platziert;
    - Eden-Rohstoffe sind Vanilla-Platzhalter;
    - Materialien ohne Verwendung;
    - `docs/quests.md` nennt noch das Silberglöckchen.
12. Renderer (`tools/preset_render.py`):
    - Wasser-Tile zeichnen;
    - Animationsframes der Kerzenständer auswählen statt stapeln;
    - Fruchtbäume richtig schneiden.
13. Die Zahl der Laternen an den Wänden der Himmelsstadt prüfen. Im Render wirken es
    zu viele.
14. `unopenedGate`, `prismChoir`, `serpentsReef` haben bewusst kein `dryRing`, weil sie
    am oder auf dem Wasser stehen. Mit festen Seeds prüfen, ob dort Stücke verloren
    gehen.
15. Größenvergleich aller Möbel gegen Vanilla (`tools/size_audit.py` mit dem
    Sprite-Dump vom PC).
16. Siedler-Anwesenheit (Ossian/Halda „nicht an ihrem Ort“) wurde nach der Heimbindung
    nicht wieder gesehen. Mit mehreren Seeds bestätigen.

## 5. Regeln, die diese Woche gelernt wurden (kurz; ausführlich in TECHNICAL_LEARNINGS)

- **Jeder Lauf von `integration_test.sh` würfelt einen neuen Seed.** Ein Fehler wird
  mit `INTEGRATION_SEED` nachgestellt, nicht wegdiskutiert.
- `ensure*ButDontGenerate` macht eine Region dauerhaft leer **und** „generiert“. Nur
  verwenden, wenn der Code die ganze Region selbst überschreibt.
- Gras-, Blumen- und Baum-Objekte löschen sich auf Pflaster. Bäume löschen sich neben
  anderen Bäumen. Wände, Türen und Möbel mit offenem Wasser unter den 8 Nachbarn
  löschen sich: dafür gibt es `dryRing`.
- Tischdeko hält nur auf einem `DecorationHolderInterface`. Schreibtische sind keins.
- `PaintingObject` liest Zeilen 2/3/0/1 für die Rotationen 0/1/2/3, nicht
  „Zeile = Rotation“.
- Keine Chat-Nachrichten aus der Mod.
- Neue **Fortschritts**-Felder gehören in `SkywatchQuestData.copyProgressFrom`, sonst
  löscht `regenerate` sie. **Platzierungs**-Flags gehören ausdrücklich nicht hinein.

## 6. Abschluss

Auf `claude/integrate-2026-09-25` committen und pushen. Dem Spieler melden:
- was zusammengeführt wurde und welche Doppelungen entschieden wurden;
- welche Gates liefen, mit ihrer wörtlichen Ausgabe;
- was jetzt [game] ist;
- welche To-dos erledigt sind und welche offen bleiben.

`master` nur auf ausdrückliche Ansage.
