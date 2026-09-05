# Auftrag: <kurzer Titel>

<!--
Der Lead legt eine Kopie dieser Datei als docs/orders/<zweigname>.md auf
master an, BEVOR eine Fernsitzung anfaengt, und traegt den Zweig in
docs/AGENT_BOARD.md ein.

Der Massstab fuer diese Datei: eine Sitzung ohne Vorgeschichte muss sie lesen
und losarbeiten koennen, ohne eine einzige Rueckfrage. Jede Frage, die sie
stellen muesste, ist ein Loch hier drin. Sie kann nicht nachfragen — sie
laeuft, wenn niemand da ist.
-->

**Zweig:** `<claude/... | codex/...>`
**Ausgangspunkt:** `origin/master` bei `<sha>`
**Angelegt:** <Datum> vom Lead

## Auftrag

<Was zu tun ist, in zwei bis fuenf Saetzen. Nicht wie — das entscheidet der
Agent. Sondern was hinterher anders sein soll.>

## Fertig, wenn

<Pruefbar, nicht "gut geworden". Eine Zahl oder ein Befehl, wo es geht:
"tools/locale_audit.py meldet keine neue Zeile", "der Ort taucht in
scripts/integration_test.sh im Weltbericht auf". Wenn du hier nichts
Pruefbares hinschreiben kannst, ist der Auftrag noch nicht fertig gedacht.>

## Besitzt diese Dateien

<Vollstaendige Liste. Was nicht hier steht, wird nicht angefasst. Deckt sich
diese Liste mit einem anderen Eintrag in docs/AGENT_BOARD.md, laufen die
beiden Auftraege nacheinander.>

## Nicht anfassen

- `docs/AGENT_BOARD.md` und `docs/orders/` — gehoeren dem Lead
- `CHANGELOG.md` und die geteilten Doks — gehoeren dem Lead
  (`docs/AGENT_WORKFLOW.md`, "Shared files belong to the coordinator")
- `src/main/resources/kk-sprites/_incoming/` — Zulieferung des Nutzers, Quelle
  und nie Zwischenstand
- alles, was `docs/DESIGN_DECISIONS.md` festhaelt: nicht stillschweigend
  umdrehen. Wer es fuer falsch haelt, sagt es und wartet.
- <auftragsspezifisch>

## Gates, die gruen sein muessen

<Nur die passenden ankreuzen — ein Gate, das die Aenderung nicht beruehrt,
ist kein Beleg, und ein Gate wegzulassen ist ehrlicher, als es zu behaupten.>

```bash
export NECESSE_GAME_DIR=<pfad>        # scripts/fetch_dedicated_server.sh druckt ihn
./gradlew buildModJar                 # muss 0 zurueckgeben
scripts/integration_test.sh           # exit 0, 0 FAIL
python3 tools/locale_audit.py         # keine NEUEN Probleme (Stand: 33 vorbestehend)
python3 tools/content_ledger.py --check   # 0 undokumentiert
python3 tools/size_audit.py           # 0 flags
```

## Pruefstufe

Sag am Ende fuer jede Behauptung, welche es ist
(`docs/IMPLEMENTATION_RULES.md` §14):
**VERIFIED [jar]** aus der Quelle gelesen · **VERIFIED [run]** kopflos laufen
gesehen · **VERIFIED [game]** im echten Client gesehen.

Der Integrationstest laeuft gegen den Dedicated Server. Der rendert nicht —
er belegt **kein** Client-Rendering. `[run]` ist nicht `[game]`.

## Abschluss

1. Auf `<zweig>` committen und **nur dorthin** schieben.
2. **Nicht nach `master` mergen.** Das macht der Lead.
3. Melden: geaenderte Dateien · welche Gates wirklich liefen und mit welchem
   Ergebnis · was offen blieb · was du gelernt hast, das noch nirgends steht.
