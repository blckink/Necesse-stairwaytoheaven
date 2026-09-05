# Agent board — who sits on what

**Stand: 2026-09-05.** Gepflegt vom Lead auf `master`.

## Wozu diese Datei

Eine Fernsitzung hat kein Gedaechtnis und sieht keine lokalen Statusdateien.
Sie sieht **das Repository**. Damit zwei Agenten nicht dieselbe Datei anfassen,
muss die Belegung deshalb hier stehen und nicht im Kopf des Lead.

`docs/AGENT_WORKFLOW.md` sagt "one owner per file — ownership is declared up
front". Diese Datei ist der Ort, an dem sie erklaert wird.

## Regeln

1. **Nur der Lead schreibt hier**, und zwar auf `master`. Ein Arbeitszweig
   aendert diese Datei nie — sonst gibt es bei jedem Merge einen Konflikt
   genau in der Datei, die Konflikte verhindern soll.
2. **Ein Zweig, ein Arbeitsbaum, ein Auftrag.** Wer keinen Eintrag hat, hat
   keine Dateien.
3. **Ueberschneiden sich zwei Aufträge in einer Datei, laufen sie
   nacheinander** — nicht parallel. Das zu pruefen ist Aufgabe des Lead beim
   Eintragen, nicht des Arbeiters beim Anfangen.
4. Ein Zweig ohne Eintrag hier ist **verwaist** und darf nicht weitergefuehrt
   werden, bevor der Lead ihn wieder einträgt.

## Belegt

| Zweig | Agent | Besitzt | Auftrag | Zustand | Seit |
|---|---|---|---|---|---|
| `claude/mod-areas-quests-expansion-pmiygp` | Fernsitzung (beendet) | `tools/area_census.py`, `docs/AREA_OVERVIEW.md`, `docs/MOD_SUMMARY.md`, `mobs/BorrowedMobIcon` | — (vor diesem Ablauf entstanden) | **fertig, wartet auf Merge-Entscheidung des Nutzers** | 2026-09-05 |

Sonst ist nichts belegt. `master` ist frei.

## Zweige: Benennung und Lebensdauer

- `claude/<thema>-<suffix>` — Fernsitzung von claude.ai/code (Suffix vergibt
  die Oberfläche).
- `codex/<thema>`, `ai/<agent>` — andere Agenten.
- `wip/<thema>` — abgebrochene Sitzung, die ihren Stand noch wegschreiben
  konnte. **Kein Arbeitszweig**, sondern ein Fundstueck: entweder wird daraus
  ein richtiger Auftrag, oder er wird geloescht.
- Ein Zweig, der in `master` steckt (`git rev-list --count origin/master..<zweig>`
  = 0), hat seine Aufgabe erfuellt und wird geloescht. Zweige sind Transport,
  kein Archiv — die Historie liegt in `master`.

### Stand der Fernstelle am 2026-09-05

15 Zweige neben `master`. Davon sind **14 vollstaendig in `master` enthalten**
(0 Commits voraus) und tragen nichts mehr:

`ai/claude` · `claude/aktueller-stand-offene-themen-k4ztas` ·
`claude/necesse-stairway-heaven-mod-65rgo3` ·
`claude/skyreach-doors-alignment-bug-39tkcd` · `codex/integrate-0309-assets` ·
`codex/poi-worldgen-spec` · `wip/crooked` · `wip/eden` · `wip/ghost` ·
`wip/npcs` · `wip/realmquests` · `wip/steinfeld` · `wip/steinfeld2` ·
`wip/veilfog`

Der einzige Zweig mit eigener Arbeit ist
`claude/mod-areas-quests-expansion-pmiygp` (3 voraus, 0 zurueck).

Das Aufraeumen ist zerstoererisch und liegt beim Nutzer — siehe Uebergabe.

## Wer bekommt was

Die Trennlinie ist **nicht** Schwierigkeit, sondern ob ein Mensch hinsehen
muss:

**Lead, lokal** — alles, wofuer jemand auf den Bildschirm schauen muss:
spielen, Screenshots beurteilen, Sprites ansehen, der Vanilla-Sprite-Dump
(braucht einen Client), GPU-Laeufe ueber ComfyUI, und jeder Merge nach
`master`.

**Auslagerbar** — alles, wo ein gruener Test die Antwort ist: Java-Aenderungen,
Doku, Audits, der kopflose Integrationstest, Recherche in den dekompilierten
Quellen. Der Dedicated Server ist ein freier Download
(`scripts/fetch_dedicated_server.sh`) und kann alles ausser rendern. Eine
Fernsitzung, die "kein Spiel installiert" meldet, hat das Skript nicht
laufen lassen.

**Nur der Lead merged nach `master`.** Fernsitzungen schieben auf ihren
eigenen Zweig und hoeren dort auf.
