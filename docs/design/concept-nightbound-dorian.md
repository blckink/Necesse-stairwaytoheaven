# Konzept: Dorian, der Nachtgebundene — transparent und verständlich

**Status: ENTWURF, wartet auf Freigabe.** Nichts davon ist gebaut. Stand der
Recherche: 2026-09-24, gelesen aus dem Code (`VampireSettlerMob`, `VampireAI`,
`VampireHuntAINode`, `BloodFeverBuff`, `DoctorHealDialogue`, `SkySettlers`,
`SkyArrivals`, beide Sprachdateien).

## 1. Was heute passiert — und was der Spieler davon mitbekommt

| Mechanik (existiert) | Sieht der Spieler das? |
|---|---|
| Dorian kommt nur als Reisender, wenn ein **Vanilla-Sarkophag** in der Siedlung steht und der Warden rekrutiert ist. 11 000 Münzen. | Tipp sagt nur „Sarg“. Das mod-eigene **Sargbett zählt nicht** — steht nirgends. |
| Er schläft tagsüber, jagt nachts Wildtiere **außerhalb** der Siedlungsgrenzen. | Nur über den Tipp-Text. |
| Unsichtbarer **Blutdurst** (0–1, 20 Min. Wachzeit bis leer). | **Gar nicht.** Kein Balken, kein Hinweis. |
| Tier ausgesaugt: Blutfläschchen (+40 % Hammelfleisch). 1 von 6 Tieren wird zum feindlichen **Blutknecht**. | Nur Sprechblase „...“ bzw. „Das da ist noch nicht fertig“, und nur, wenn man danebensteht. |
| Durst auf 0 → er **beißt alle 3 Min. einen Siedler** in der Nähe (nie Spieler). Opfer bekommt **Blutfieber** für einen Spieltag: −20 % Tempo, −15 % Abbau/Bauen, −10 % Max-Leben. | Nur Sprechblase „Verzeih. Es hat niemand etwas rausgestellt.“ — **kein Name, keine Meldung**, nachts meist ungesehen. |
| Arzt heilt Blutfieber (100 Münzen, alle Gebissenen auf einmal). | **Versteckt** im Untermenü „Flick mich zusammen“; der Einleitungssatz sagt u. U. „Dir fehlt nichts“, während der Heil-Knopf da ist. Kein Arzt-Satz erwähnt Bisse. |
| Dialogzeile „Stellt ein Fläschchen raus …“ | **Versprechen ohne Funktion**: Man kann ihm kein Blutfläschchen geben. |

Nirgends im Spiel steht das Wort **Vampir**.

Dazu kommen Fehler, die unabhängig vom Konzept behoben werden sollten:

- Der Blutfieber-Text behauptet „ohne Appetit“, es gibt aber keinen Hunger-Effekt.
- Bisse können auch tagsüber passieren, wenn er in der Abenteuergruppe ist; der Text sagt „nachts“.
- „Blutfaeschchen“ ist falsch geschrieben (es fehlt ein l).
- Seine Jagd filtert nur „außerhalb der Grenzen“. Eigenes Vieh oder Haustiere, die draußen stehen, sind damit nicht geschützt, obwohl der Tipp „niemals euer Vieh“ verspricht.

## 2. Leitidee

**Der Spieler soll alles, was Dorian tut, von Dorian selbst oder vom Arzt
erfahren können — ohne Wiki, ohne Chat-Spam.** Die Spielerregel „keine
Chat-Nachrichten, generell“ (`SkySettlerMob.java:161`) bleibt: Meldungen laufen
über Siedlungs-Meldungen, Dialoge, Tooltips und Sprechblasen.

## 3. Bausteine

### 3.1 Er sagt, was er ist
- **Name:** „Dorian <Name>, der Nachtgebundene (Vampir)“. Alternativ bleibt der Name und nur die Beschreibung sagt „Vampir“, siehe Entscheidung E1.
- **Anwerbe-Tipp:** der Siedlungs-Tipp wird neu geschrieben:
  - „Vampir. Kommt nur, wenn ein **Sarkophag** in der Siedlung steht (kein Sargbett).“
  - „Schläft am Tag, jagt nachts Wildtiere außerhalb der Mauern.“
  - „Hat er Durst und niemand hat ihm ein Blutfläschchen hingestellt, **beißt er einen Siedler** — der Arzt kann das heilen.“
- **Anwerbe-Satz**, bevor er einzieht: Er sagt es offen: „Ich bin, was eure Großmütter
  einen Vampir nannten. Ich trinke von Tieren — und von euch, wenn ihr mich
  hungern lasst.“

### 3.2 Man kann mit ihm reden (neuer Dialog, wie beim Arzt)
Ein eigenes Dialogmenü, wie beim Arzt oder Therapeuten, mit drei Optionen:

1. **„Wie steht es um deinen Durst?“**
   - Er antwortet in einer von vier Stufen: satt / durstig / sehr durstig / „ich muss heute Nacht jemanden beißen“.
   - Damit wird der unsichtbare Durstwert lesbar, ohne dass es eine neue Anzeige braucht.
2. **„Blutfläschchen geben“** — sein Versprechen wird endlich eingelöst:
   - Ein Fläschchen füllt seinen Durst um +0,5.
   - Die Option erscheint nur, wenn du ein Fläschchen im Inventar hast.
3. **„Was war letzte Nacht?“** — sein Nachtbericht:
   - wie viele Tiere er ausgesaugt hat;
   - ob ein Blutknecht entstanden ist;
   - **wen er gebissen hat, mit Namen.**

### 3.3 Optional: eine Blutschale
Ein neues Möbelstück, die „Blutschale“, aus der er selbst trinkt:
- Du legst Blutfläschchen hinein. Er trinkt nachts daraus, bevor er beißt.
- Das ist die Automatisierung zu 3.2. Ob du sie willst, entscheidest du in E3.

### 3.4 Man merkt es, wenn er gebissen hat
- **Siedlungsmeldung:** „Dorian hat heute Nacht <Siedler> gebissen — Blutfieber. Der Arzt kann helfen.“
  - Sie erscheint in der Siedlungsübersicht, am selben Ort wie Vanillas „Lager voll“.
  - Sie verschwindet, wenn niemand mehr Blutfieber hat.
  - Machbarkeit: **HYPOTHESE**. Die Mod benutzt bisher nur Vanillas vorhandene Meldungen, keine eigenen. Ob sich eine eigene sauber registrieren lässt, muss ich im Vanilla-Code prüfen. Wenn nicht, bleibt als Ausweg der Nachtbericht aus 3.2.
- **Das Opfer sagt es selbst:**
  - Sprechblase beim Aufwachen: „Mir ist so kalt … am Hals ist etwas.“
  - Solange das Fieber anhält, hat es eigene Gesprächszeilen dazu.
- **Blutfieber-Tooltip ehrlich formulieren:** „Von Dorian gebissen. Langsamer, schwächer, weniger Leben. Vergeht nach einem Tag — oder der Arzt heilt es sofort.“
  - Den „Appetit“-Satz streichen, oder einen echten Hunger-Effekt einbauen (E4).

### 3.5 Der Arzt sagt es einem
- **Eigene Hauptoption**, sobald jemand Blutfieber hat: „Die Gebissenen behandeln (100 Münzen)“. Nicht mehr versteckt unter „Flick mich zusammen“.
- **Der Einleitungssatz erwähnt die Kranken:** „Zwei deiner Leute haben Bissspuren am Hals. Ich habe ein Mittel.“ Nicht mehr „Dir fehlt nichts“.
- **Plauderzeilen:** zwei neue Zeilen über Dorian, z. B. „Euer Nachtgast … Ich halte immer etwas gegen Blutfieber bereit.“ So erfährt man vom Heilmittel, bevor man es braucht.
- Optional (E5): Der Arzt **verkauft ein Heilmittel** als Gegenstand, „Blutfieber-Tinktur“, das man einem Siedler selbst gibt. Dann muss man nicht jedes Mal zum Arzt.

### 3.6 Eine kleine Questreihe mit Dorian (optional, E6)
„Tageswandler“ war schon geplant (`settlers.md:238`) und wurde nie gebaut. Vorschlag in drei Schritten:
1. **Dorian stellt sich vor:** der Vampir-Hinweis und seine Regeln.
2. **„Halte mich satt“:** 5 Blutfläschchen bringen.
   - Belohnung: Er beißt nie mehr, solange die Blutschale gefüllt ist.
3. **„Tageswandler“:** eine seltene Zutat aus dem Geisterreich.
   - Belohnung: Er darf tagsüber in der Abenteuergruppe mitkämpfen, ohne zu beißen.

### 3.7 Verbindliche Aufräumarbeiten (auch ohne Konzept-Freigabe sinnvoll)
- Vieh und Haustiere schützen:
  - Er jagt keine gezähmten oder dir gehörenden Tiere, auch nicht außerhalb der Grenzen.
  - Das ist heute nicht so, obwohl der Tipp es verspricht. Vorher prüfe ich im Vanilla-Code, woran man „dein Tier“ erkennt.
- Tippfehler „Blutfaeschchen“ → „Blutfläschchen“.
- Bisse tagsüber (Abenteuergruppe) im Text erwähnen oder abstellen.

## 4. Entscheidungen, die ich von dir brauche

| # | Frage | Mein Vorschlag |
|---|---|---|
| E1 | „Vampir“ im **Namen** oder nur in Beschreibung/Dialog? | Nur Beschreibung + Dialog; der Name „der Nachtgebundene“ bleibt stimmungsvoll |
| E2 | Wo erfährt man vom Biss? Siedlungsmeldung / Chat / nur Dialog | Siedlungsmeldung + Nachtbericht im Dialog, **kein Chat** |
| E3 | Blutschale (automatisches Füttern) bauen? | Ja — sonst ist Füttern nur Mikromanagement |
| E4 | Blutfieber schärfer machen (z. B. Hunger, Arbeitsverweigerung) oder nur Text korrigieren? | Nur Text korrigieren; die Strafe ist jetzt schon spürbar |
| E5 | Arzt verkauft ein Heilmittel-Item? | Ja, teurer als die Behandlung vor Ort |
| E6 | Kleine Dorian-Questreihe? | Ja, aber als zweiter Schritt nach 3.1–3.5 |

---

# Frage 1: Erkundete Mod-Gebiete bei Updates neu erzeugen

**Kurz: Ja, das ist möglich.** Bisher ist es bewusst nicht gebaut: `docs/SAVE_COMPAT.md`
sagt „zeichnet nie schon existierenden Boden neu“, weil dort deine Basis stehen
könnte.

## Wie es technisch geht (VERIFIED [run] am Test-Spielstand)

Ein Spielstand ist eine Zip-Datei. Der Himmel liegt darin getrennt von der
Oberwelt:

```
<welt>/levels/regions/skyreach2/<x>x<y>.dat    ← Boden, Objekte, Mobs je Gebietsblock
<welt>/levels/presets/skyreach2/<x>x<y>.dat    ← Merkliste der Gebäude-Platzierung
<welt>/levels/skyreach2.dat                    ← Level-Daten (Quest-Stand, Spire)
<welt>/world.dat                               ← Welt-Flags
```

Fehlt eine Region-Datei, erzeugt das Spiel diesen Block beim nächsten Betreten neu,
mit dem aktuellen Stand der Mod. So entsteht auch der Himmel in einem frischen
Spielstand.

Die Welt-Erzeugung ist aus dem Seed berechnet. Ein neu erzeugter Block passt
deshalb nahtlos an alles, was nicht gelöscht wurde. Das gilt nur für Blöcke, die
mit **demselben** Mod-Stand gebaut wurden; an der Grenze zu altem Boden kann eine
Naht bleiben.

## Die drei Wege

| | Was | Vorteil | Nachteil |
|---|---|---|---|
| **A** | **Ganzen Himmel neu** (Skript auf einer Kopie des Spielstands): alle `skyreach2`-Blöcke löschen, Quest-Fortschritt behalten | einfach, alles aktuell | **alles, was du im Himmel gebaut hast, ist weg**, Siedlungen im Himmel inklusive |
| **B** | **Himmel neu, außer deinen Basen**: alle Blöcke löschen, die weiter als z. B. 64 Felder von einer Siedlungsflagge oder einem Bett entfernt sind | behält deine Bauten, der Rest wird aktuell | Nähte am Rand der geschützten Zone; Siedlungsflaggen im Himmel muss das Skript sicher finden |
| **C** | **Im Spiel per Befehl** `/swhreset regenerate <radius> confirm` | kein Skript nötig | braucht einen Neustart des Servers, weil geladene Blöcke nicht im laufenden Betrieb gelöscht werden können; aufwendiger und riskanter |

**Empfehlung: B als Skript** (`scripts/regenerate_sky.sh <welt.zip>`):
- Es arbeitet immer auf einer **Kopie** und legt eine Sicherung an.
- Es listet vorher auf, was gelöscht und was behalten wird, und fragt nach.
- Danach prüft `scripts/save_compat_check.sh` die Kopie automatisch mit einem echten Server-Start.

Die Oberwelt fasse ich nicht an. Die drei Oberwelt-Orte (Aeronauten-Lager usw.)
kommen dort weiterhin nur in unerkundeten Gebieten.

Offene Punkte für B:
- **Wie die Blöcke aufgeteilt sind:** Wie viele Regionen eine Datei `<x>x<y>.dat` umfasst, lese ich aus dem Vanilla-Code, bevor ich schneide.
- **Spire und Wahrzeichen:** Der Spire und die einmaligen Wahrzeichen sind als „gesetzt“ gemerkt (`spirePlaced`, `landmarksStamped`). Werden ihre Blöcke gelöscht, muss das Skript diese Merker zurücksetzen, sonst fehlen sie danach. Alternativ bleiben ihre Blöcke immer geschützt.
- **Bewohner:** Warden, Katzen und die anderen Bewohner, die im gelöschten Himmel standen, verschwinden mit ihm. Die „einmal pro Welt“-Merker müssen passend mitgeräumt werden, wie bei `/swhreset all`.
