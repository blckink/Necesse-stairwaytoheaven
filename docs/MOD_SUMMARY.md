# Mod-Summary — alles auf einen Blick

**Deutsch, weil dies die Checkliste für den Spieler ist** und nicht für einen
Agenten. Die englischen Statusdokumente bleiben `docs/OVERVIEW.md` (was
funktioniert), `docs/AREA_OVERVIEW.md` (wie voll jedes Gebiet ist) und
`docs/SAVE_COMPAT.md` (bestehende Spielstände). Diese Datei fasst alles
zusammen, was ein Spieler tatsächlich antrifft: jede Person, jedes Sortiment,
jede Quest, jeder Boss, jedes Sprite.

Stand **2026-09-05**, aus dem Code gelesen (`python3 tools/area_census.py`).

**Am Handy lesbar:** dieselbe Zusammenfassung als Seite —
<https://claude.ai/code/artifact/16ff4236-4ba9-4054-a87c-729452ef881d>

---

## 1. Die Frage zuerst: haben wir Human Settlers mit neuen Berufen?

**Menschliche Siedler: ja — zehn benannte, alle anwerbbar.**
**Neue Berufe: nein — und das ist eine bewusste Entscheidung, kein Versäumnis.**

Necesse registriert 12 Job-Typen (`JobTypeRegistry`). Genau **fünf** davon
tragen `defaultDisabledBySettler = true` — das sind die, die im engeren Sinn
„Beruf" heißen, weil nur ein bestimmter Siedler sie ausüben darf. Der Mod
belegt inzwischen **alle fünf**:

| Vanilla-Beruf | Vanillas Träger | unser Träger |
|---|---|---|
| `fertilize` (Düngen) | Farmer | **Eveleen** |
| `husbandry` (Tierhaltung) | Animal Keeper | **Eleanor** (Ende STAY) |
| `fishing` (Angeln) | Angler | **Halda** |
| `hunting` (Jagd) | Hunter | **Mortimer** |
| `tradingmission` (Handelsmissionen) | Trader | **Magpie** und **Mr. Knott** |

Der Mod setzt dabei exakt dasselbe Flag wie Vanilla
(`SkySettlerMob.enableProfession`). **Ein eigener, neu registrierter Job-Typ
existiert nicht** — es gibt im ganzen Repo keinen einzigen Aufruf von
`JobTypeRegistry.registerJobType`.

**Was ein wirklich neuer Beruf bräuchte.** Eine Arbeitspriorität ist nur eine
von fünf Mechaniken, die Vanilla für „Beruf" benutzt. Die passendste offene ist
**`ExpeditionMissionRegistry`** — so funktioniert „nur der Miner arbeitet in der
Mine": eine `SettlerExpedition`-Unterklasse plus ein Siedler, der als einziger
`canDoExpedition` dafür mit `true` beantwortet. Die API ist vollständig public
(`registerExpedition`, `registerMinerExpedition`, `registerMiningTrip`,
`registerFishingTrip`, `registerExplorerExpedition`). **Der Mod registriert
heute keine einzige.** Das ist die nächste ehrliche Ausbaustufe, wenn du echte
neue Berufe willst — Details in `docs/OVERVIEW.md` §5.

**Ohne Spezialberuf, absichtlich:** Ossian, Caspern und Ives. Alle drei
verweigern Ackerbau und Forstwirtschaft (`refuseJob`, Vanillas eigener Zug bei
seiner Wache) und arbeiten in Handwerk und Transport. Ihre Identität ist ihr
Sortiment und ihre Quest, kein Etikett.

**Die drei Werkstätten sind keine Berufe.** Windseiden-Webstuhl, Ätherschmiede
und Sturmglas-Ofen sind `SettlementWorkstationObject`; `LevelJobRegistry` legt
jeden Werkstatt-Job unter die geteilte **Handwerk**-Priorität — derselbe Topf
wie Vanillas Schmiede. Jeder Siedler mit Handwerk kann jede davon bedienen.

**Geisterschmiede und Seelenwebstuhl** (Geisterreich) lassen sich seit
2026-09-24 an der Werkbank bauen — vorher gab es sie nirgends:
Geisterschmiede = 20 Knochenholz + 8 Spektralerz + 6 Ektoplasma,
Seelenwebstuhl = 12 Knochenholz + 8 Ektoplasma + 4 Seelenfaden. Die Mengen sind
ein Vorschlag.

**Sortierung und Abbau (2026-09-24).** Alles aus der Mod steht jetzt in echten
Vanilla-Kategorien (Material, Rohkost, Quest-Items, Fallen, Landschaft …), und
Welt-Szenerie gibt beim Abbauen Material statt sich selbst: Toter Baum →
Totholz, Sturmschutt/Wächter-Trümmer → Himmelsstein, Kristalle → ihre
Scherben, wildes Gras → wie Vanilla-Gras (Wurmköder, beim Eden-Gras selten
Saat). Selbst gebaute Stücke geben sich weiter selbst zurück. Details:
`docs/ITEM_CATEGORIES.md`.

---

## 2. Alle NPCs

Zehn benannte Bewohner in der Welt, dazu **Dorian** (elf benannte Figuren),
zwei Katzen, ein beschworener Geist — und vier Berufs- bzw. Besucher-NPCs ohne
Ort in der Welt (Therapeut, Arzt, Kriegsveteran, Zwielichtiger Händler), die
von selbst in eine Siedlung kommen. Die benannten Bewohner existieren
**genau einmal pro Welt** (`SkywatchWorldData.residentsClaimed`). Volle Läden
und Texte aller Figuren: `docs/KOMPLETTUEBERSICHT.md` §3.

| Wer | Gebiet | Gefunden bei | Anwerbung | Beruf | Quest | Kommt von selbst in die Siedlung? |
|---|---|---|---|---|---|---|
| **Sky Warden** | Skyreach | Alte Wächterspitze (beim ersten Aufstieg gestempelt) | 30 000 | — | die ganze Warden's-Call-Kette **+ alle 5 Region-Keys** | nein, wird angeworben |
| **Magpie** | Skyreach | **Skyway-Zollhaus** ⭐, einmal pro Welt · bewacht vom **Zollwerk** | 12 000 **+ Verzollte Kassette** ⭐ | Handelsmissionen | — | nein |
| **Halda** | Skyreach | **Grange-Keller** ⭐, einmal pro Welt · bewacht von der **Sauerbottich-Blüte** | 9 000 **+ Die Mutter** ⭐ | **Angeln** | — | nein |
| **Ossian Vane** | Skyreach | **Sturmschleier-Testgelände** ⭐, einmal pro Welt · bewacht von **Prototyp Neun** | 18 000 **+ Sturmlinsenkern** ⭐ | — | — | nein |
| **Eveleen** | Eden | bei einem Baum der Erkenntnis | 7 000 → **frei** | **Düngen** | `swh_edenreach`, `swh_edenplants` | ja, wenn Eden-Gras in der Siedlung wächst |
| **Ives** ⭐ | **Steinfeld** | bei einem **zerbrochenen Engel** | 11 000 → **frei** | — | `swh_steinfeldvigil` | nein |
| **Mortimer** | Ghost Realm | bei einem Grabstein | 8 000 → **frei** | **Jagd** | `swh_mortimerrites` ⭐ | ja, bei einem Friedhof in der Siedlung |
| **Caspern** | Ghost Realm | bei einem Grabstein | 14 000 → **frei** | — | `swh_caspernforge` ⭐ | ja, bei einer Ätherschmiede in der Siedlung |
| **Eleanor** | Ghost Realm | bei einem Grabstein | 5 000 (nur Ende STAY) | **Tierhaltung** | `swh_eleanor` | nein — sie ist ein Ende, kein Besuch |
| **Mr. Knott** | Crooked Beyond | am Türhof | 22 000 | Handelsmissionen | `swh_crookedarrival`, `swh_crookeddoor` | nein |
| **Ghost Guide** | Ghost Realm | am Séance-Kreis **beschworen** | nicht anwerbbar | — | — | — |
| **Siggi + Peanut** | Skyreach | Lairs im Sturmschleier / in den Aurorabänken | nicht anwerbbar | — | Ziel von `swh_cats` | ziehen in den Katzenkorb |
| **Dorian, der Nachtgebundene** | — | nirgends in der Welt | 11 000 | **Jagd** (nur nachts) | — | ja, wenn ein Sarg in der Siedlung steht |
| **Therapeut** | — | — | 900–1 400 | Therapieplätze, Wesenszug tauschen | — | ja, wie ein Schmied |
| **Arzt** | — | — | 900–1 400 | Heilung, Blutfieber heilen | — | ja, wie ein Schmied |
| **Kriegsveteran** | — | — | 900 | Verteidigungsausbau (5 Stufen) | — | ja, wie ein Schmied |
| **Zwielichtiger Händler** | — | — | nicht anwerbbar (Besucher) | wechselnde Kostüme + Deko | — | kommt als Besucher |

⭐ = neu in diesem Durchgang.

**„→ frei"** heißt: die Quest dieser Person erlässt ihre Anwerbegebühr. Über
`getRecruitItems`, also Vanillas eigene Anwerbeseite — eine leere Liste *ist*
eine kostenlose Anwerbung. Wer vorher voll bezahlt hat, kann die Quest trotzdem
noch abschließen und bekommt die Barren.

**Findbarkeit, ehrlich.** ⭐ Magpie, Halda und Ossian hingen früher an einer
**vom Spieler gebauten** Werkstatt, die schon stehen musste, während eine Region
zum **ersten Mal** generiert — in der Praxis waren sie kaum zu finden. Seit dem
10.09.2026 sitzt jeder der drei stattdessen in einem eigenen Gebäude, das die
Welt genau **einmal** stempelt: Zollhaus, Grange-Keller, Testgelände. Jedes
davon hat einen Wächter, und bei zweien der drei trägt der Wächter den
Schlüsselgegenstand, ohne den die Anwerbung nicht geht — der Ort ist also ein
Kampf, kein Raum. Die anderen sieben hängen an natürlich gemalten Landmarken
(Baum der Erkenntnis, Grabstein, zerbrochener Engel, rote Tür) und sind in
Ordnung.

---

## 3. Sortimente — was jeder Händler verkauft und kauft

Preise bewegen sich mit der Zufriedenheit des Siedlers
(`setStaticPriceBasedOnHappiness(min, max, Schritt)`), deshalb stehen hier die
Waren, nicht die Zahlen.

### Sky Warden — Skyreach
| verkauft | |
|---|---|
| Geisterkreide ×3 (+1 pro Tag) @ 1 200 | Nachschub, nachdem er dir das erste Stück geschenkt hat |
| Wolkengleve ×1 @ 4 500 · Himmelswacht-Banner ×1 @ 800 | erst, wenn irgendwer in der Welt die Anker-Quest abgegeben hat |
| Katzenkorb ×1 @ 500 · Flackerlicht-Girlande ×2 @ 500 | erst, wenn die Katzen daheim sind |
| Abenteurer-Tagebuch ×1 @ 100 | Ersatz für ein verlorenes Tagebuch, +1 pro Tag |

Er kauft nichts. Er **schenkt** 1× Geisterkreide (beim ersten Mal, dass *du*
im Nebel gestanden hast — pro Charakter, nie geteilt). Das Silberglöckchen gibt
es nicht mehr zu kaufen oder zu verdienen; der Séance-Zirkel braucht es nicht.

### Magpie — Skyreach · der Aufkäufer
| verkauft | kauft |
|---|---|
| Wurmköder, Sandstein, Kokosnuss, Schneeball, Glas | **Himmelsstein, Windseide, Aetheriumerz, Sturmsplitter, Aurorablatt, Fulgurit, Prismensplitter** |
| **Kurierkappe** ×1 (ihre eigene) | **Himmelspost-Pakete** |
| | die fünf Trophäen: Himmelsstein-Herz, Blütenzahn, Trauerflor, Seelenhalsband, Gestreiftes Horn |

Sie zahlt über Broker-Kurs für Himmelsbergung — dafür ist sie da.

### Halda — Skyreach · die Kellermeisterin
| verkauft | kauft |
|---|---|
| Himmelsgewebe, Sturmglas, **Sturmstahlbarren**, Wolkenzupf-Leckerli, Wolkenbeere | Windweizen, Wolkenbeere, Nimbusholz, Kohlenholz |

Die einzige Quelle für **Wolkenzupf-Leckerli** außerhalb des Kochens — das ist
der Köder für Siggi und Peanut.

### Ossian Vane — Skyreach · exklusive Incursion-Beute, täglich wechselnd
| verkauft (3 von 8 pro Tag) | kauft |
|---|---|
| Kristallessenz · Aufgestiegene Essenz · Leerengeschoss · Arkanahelm · Arkanaharnisch · Arkanastiefel · Leerentasche · **Auge der Leere** | Aetheriumbarren, Sturmstahlbarren, Himmelsgewebe |

Alles davon kommt aus einer Incursion und hat **sonst keinen Verkäufer im
Spiel**. Das Fenster ist drei breit und rückt um Mitternacht weiter, gesteuert
vom Welttag selbst — jeder Spieler in einer Welt sieht dasselbe Regal, alles
kommt in unter drei Tagen einmal vorbei.

### Eveleen — Eden · die Botanikerin
| verkauft | kauft |
|---|---|
| Eden-Grassamen, Weizen-/Karotten-/Kürbis-/Erdbeersamen | Windweizen, Wolkenbeere, Weizen, Sonnenblume |
| Wolkenbeeren-Setzling, Apfel-, Zitronen-, Bananensetzling | |
| Dünger, Blumentopf, Hübsche Blume | |
| **Bienenkönigin** ×1 | |

### Ives — Steinfeld · der Küster ⭐ **NEU**
| verkauft | kauft |
|---|---|
| Grabstein (2 Typen), Kerze, Vase | **Blasser Stein, Grabsalz, Geistermoos, Echosplitter** |
| **Steinzaun + Steinzauntor** — die Friedhofsmauer | |
| **Blasser Stein** — Steinfelds eigener Baustein | |

**Der einzige Händler des Realms.** Vorher gab es in einem 2280 Kacheln tiefen
Band niemanden, dem man etwas verkaufen konnte — man trug das Erz zurück zu
einem Skyreach-Broker und bekam Broker-Kurse. Er kauft alle vier
Steinfeld-Materialien darüber. Mortimer verkauft, was *ins* Grab kommt; Ives
verkauft, was *drumherum* steht — die beiden Regale lesen sich nicht als ein
halbierter Laden.

### Mortimer — Ghost Realm · der Bestatter
| verkauft | kauft |
|---|---|
| Grabstein ×2, Krypta-Grabstein ×2 | Knochen, Ektoplasma, **Schleier-Essenz** |
| **Sarkophag** (braucht: Sumpfwächter erlegt) | |
| Kerze, Knochenkandelaber, Totholzkerzen | |
| **Geisterbecken** (braucht: Reaper erlegt) | |
| Die Knochenholz-Familie: Stuhl, Modularer Tisch, Bücherregal, Kommode, Uhr, Truhe, Totenkopf | |

### Caspern — Ghost Realm · der Geisterschmied
| verkauft | kauft |
|---|---|
| Nachtstahlerz, **Nachtstahlbarren** | Aetheriumbarren, Sturmstahlbarren, Knochen, Ektoplasma |
| Phantomstaub, Seide | |
| Knochenpfeil, Knochengriff | |
| **Nachtstahlschleier** ×1 | |

### Eleanor — Ghost Realm · die verlorene Seele
| verkauft | kauft |
|---|---|
| Hübsche Blume, Blumenstrauß, Topfblume ×3 | Schleier-Essenz, Ektoplasma |
| Laterne, Wasserlaterne | |

### Mr. Knott — Crooked Beyond · der Türsteher
| verkauft | kauft |
|---|---|
| Leerenwürfel, Runenstein (klein) | Krümmungsharz, Augensamen, Realitätssplitter |
| Leerenmaske, Alienmaske, Haifischmaske | |

### Ghost Guide — beschworen, ein normaler Münz-Laden
| verkauft | kauft |
|---|---|
| **Geisterstahl-Schnitter**, **Grabwind-Bogen** — die Geisterwaffen | Ektoplasma, Schleier-Essenz, Geisterstahlbarren |
| Geisterstahlbarren, Schleier-Essenz | |

Er handelt **gegen Münzen**, zu Endgame-Preisen (Schnitter 14 000–24 000,
Bogen 12 000–20 000) — die ursprüngliche Tauschidee („kein Geld, nur
Geisterwaren oder gutes Essen") hast du verworfen: *„Auf garkeinen fall! dann
lieber hohe münzbeträge und normaler shop."* Dafür kauft er die Drops des
Realms deutlich über Broker. Seine Begrüßung sagt das jetzt auch („Münzen? Die
Toten haben reichlich davon …"). Beim **ersten** Gebrauch des Kreises schaltet er dich
frei — danach wirkt die Soul-Exposure des Ghost-Bands für diesen Charakter
nicht mehr. Dieselben Geisterwaffen fallen auch zufällig in der Region, damit
ein Spieler, der nie handelt, sie trotzdem findet.

---

### Stylistin (Vanilla-NPC) — der Friseurladen ⭐ **NEU**

Die Vanilla-Stylistin verkauft jetzt zusätzlich die Einrichtung für ihren
eigenen Laden. Keins der sechs Stücke hat ein Rezept — sie ist die einzige
Quelle. Preise sinken mit der Zufriedenheit der Siedlung (wie bei ihrer
Kleidung).

| Stück | Preis (gut → schlecht) | was es ist |
|---|---|---|
| Friseurstuhl | 900 → 1400 | hydraulischer Salonsessel, vier Richtungen, ein Siedler kann darin sitzen |
| Beleuchteter Friseurspiegel | 700 → 1100 | hängt an der Wand, Birnenkranz, leuchtet, zählt als Lichtquelle |
| Friseurladen-Schild | 600 → 950 | wird wie eine Lampe an die Wand gehängt, an/aus, leuchtet rosé |
| Friseurladen-Kasse | 800 → 1250 | Messingkasse für den Tisch, vier Richtungen |
| Pflegemittel-Tablett | 250 → 400 | Flaschen, Schere, Kamm, Bürste — Tischdeko, vier Richtungen |
| Barbier-Säule | 450 → 700 | rot-weiß-blaue Wendel, steht auf dem Boden, leuchtet leicht |

**Alle sechs erhöhen die Zufriedenheit der Siedler** (Vanillas
`HappinessObject`, 40–50 Punkte je Stück — dieselbe Mechanik wie hinter
Schafsstuhl und Holzente). Spiegel und Schild zählen zusätzlich als Licht für
die Raumwertung.

## 4. Alle Quests — 20 registriert, 19 aktiv

Stand **2026-09-24** (Feinschliff der Belohnungen, siehe unten). Reihenfolge =
die Reihenfolge, in der du sie im Spiel triffst. Die Warden-Kette ist streng
linear; die Region-Keys beginnen erst, wenn sie fertig ist, und kommen einzeln
in Boss-Reihenfolge. Die Ketten der Bewohner laufen nebenher: jede Person gibt
ihre Aufgabe beim ersten Treffen.

### Skyreach — „The Warden's Call"

| ID | Geber | Aufgabe | Belohnung |
|---|---|---|---|
| `swh_findspire` | erster Aufstieg | finde die Alte Wächterspitze | Karten-Pin |
| `swh_recruitwarden` | Warden | 30 000 zahlen | der Warden zieht ein |
| `swh_cats` | Warden | beide Katzen mit Wolkenzupf-Leckerli heimlocken | **Katzenkorb**, 2× Flackerlicht-Girlande, 10× Sturmstahlbarren |
| `swh_anchor` | Warden | 20× Aetheriumbarren, 80× Himmelsstein, 8× Sturmstahlbarren, **2× Himmelsstein-Herz** | ⭐ **Wolkengleve**, Himmelswacht-Banner, 5× Aurorablatt |
| `swh_beacon` | *niemand* | — | **TOT** — registriert, wird nie vergeben; existiert nur, damit Spielstände vor 0.5 laden |

**Die Wolkengleve ⭐ NEU** — die Gleve der Himmelswacht: die Kryogleve der
Cryo-Königin, weiß-golden statt eisblau, und statt Frostfunken hinterlässt der
Schwung goldene und wolkenweiße Lichtfunken. **165 Schaden → 208,5** voll
geschmiedet (Kryogleve: 60 → 75,8; dein herstellbarer Himmelsreißer: 150 →
189,6; der Geisterstahl-Schnitter eine Welt weiter: 176). Reichweite 160 wie
die Kryogleve, Rückstoß 150, Verzauberungskosten 2000, EPISCH. **Nur hier zu
bekommen** — kein Rezept, keine Truhe. Sie trägt dich durch Eden und Steinfeld.

*Alter Spielstand, Anker schon gesetzt?* Beim nächsten Gespräch mit dem
Warden bekommst du die Wolkengleve einmal nachgereicht (die Armschiene von
damals behältst du).

### Die sechs Region-Keys — Warden, erst wenn die Kette DONE ist

Einer nach dem anderen, in Boss-Reihenfolge. Jeder verlangt Materialien aus
seinem Realm und zahlt das Schlüsselstück, Barren, **eine Waffe und ein Stück
zum Anziehen**. Stell das Schlüsselstück **in eine Siedlung** — dann wachen die
Beschwörungssteine dieses Realms auf.

| ID | Aufgabe | Belohnung |
|---|---|---|
| `swh_keyskyreach` | 10× Sturmsplitter, 5× Fulgurit, **2× Himmelsstein-Herz** | Skyreach-Wachfeuer, 4× Sturmstahlbarren, **4× Sturmscheibe** (ein ganzer Satz — eine einzelne ließ sich nicht schmieden), Himmelswacht-Kapuze |
| `swh_keyeden` | 8× Edensaft, 6× Goldener Pollen, **2× Blütenzahn** | Gartenstiege von Eden, 5× Sturmstahlbarren, Windheuler, Mantel des Hüters |
| `swh_keysteinfeld` | 8× Echosplitter, 20× Blasser Stein, **2× Trauerflor** | Trauerengel von Steinfeld, 6× Sturmstahlbarren, Sturmklinge, Stiefel des Hüters |
| `swh_keyghostrealm` | 12× Knochenholz, 8× Spektralerz, **2× Seelenhalsband** | Rabenkanzel des Nachgartens, 6× Geisterstahlbarren, **Geisterstahl-Schnitter** (statt Himmelsreißer), Sturmstahl-Armschiene |
| `swh_keycrookedbeyond` | 16× Seltsamholz, 8× Realitätssplitter, **2× Gestreiftes Horn** | Knotts Krumme Tür, 8× Geisterstahlbarren, **Grabwind-Bogen** (statt Prismenrufer), Auroramedaillon |
| `swh_keyhell` | 16× Realitätssplitter, 24× Seltsamholz, **4× Gestreiftes Horn** | Höllensiegel, **16× Geisterstahlbarren** (statt Donnerhaupt + Zephyr-Gurtzeug + 10×) |

Die Waffen steigen jetzt mit: Skyreach-Waffen auf den ersten drei Stufen, die
Geisterwaffen ab dem Ghost Realm. Hell zahlt vorerst nur Barren, weil es noch
keine Waffe über der Geister-Stufe gibt — ein Donnerhaupt (herstellbarer
Skyreach-Bogen) auf der letzten Stufe war ein Rückschritt, und das
Zephyr-Gurtzeug gab es bei Mr. Knott schon.

### Eden

| ID | Geber | Aufgabe | Belohnung |
|---|---|---|---|
| `swh_edenreach` | Eden-Tor | finde Eveleen (Baum der Erkenntnis) | Wegweiser |
| `swh_edenplants` | Eveleen | 1× Edenbeere, 1× Mondmelone, 1× Sonnentraube | 3× Wissenssteckling, 10× Sturmstahlbarren, **ihre Gebühr entfällt** |

### Steinfeld ⭐ **NEU**

| ID | Geber | Aufgabe | Belohnung |
|---|---|---|---|
| `swh_steinfeldvigil` | **Ives** | 14× Grabsalz, 10× Geistermoos | **seine 11 000 entfallen** + **12×** Sturmstahlbarren (vorher 10 — gleich viel wie Edens drei Früchte, obwohl eine Welt weiter und 24 Drops) |

*Die Totenwache.* Beide Materialien gibt es nur in Steinfeld — und beide hatte
vorher **überhaupt keinen Abnehmer**: kein Rezept im Mod nennt sie, und der
Region-Key nimmt die anderen zwei (Echosplitter, Blasser Stein).

### Ghost Realm

| ID | Geber | Aufgabe | Belohnung |
|---|---|---|---|
| `swh_eleanor` | Eleanor | **PASS ON:** 12× Schleier-Essenz in der Hand halten · **STAY:** ohne Essenz reden und anwerben | PASS ON: **Irrlichtlaterne** + **10×** Geisterstahlbarren, *sie ist dauerhaft weg* · STAY: **10×** Geisterstahlbarren, sie zieht ein (vorher 14 — mehr als Knott eine Welt weiter) |
| `swh_mortimerrites` ⭐ | **Mortimer** | 12× Seelenfaden, 10× Knochenholz | **seine 8 000 entfallen** + 6× Geisterstahlbarren |
| `swh_caspernforge` ⭐ | **Caspern** | 12× Spektralerz, 8× Schleier-Essenz | **seine 14 000 entfallen** + 6× Geisterstahlbarren |

*Die letzte Ehre* (Leichentuch ist Faden, Sarg ist Holz) und *Die kalte
Schmiede* (Erz nährt das Feuer, Essenz löscht es). Casperns Auftrag ist das
**erste im ganzen Mod, das dich gezielt in den Gloomfen und die Aschenweite
schickt** — Schleier-Essenz fällt nur dort, und für diese beiden Ex-Veil-Biome
gab es bis jetzt keinen Grund, sie zu betreten.

### Crooked Beyond

| ID | Geber | Aufgabe | Belohnung |
|---|---|---|---|
| `swh_crookedarrival` | Krumme Tür | finde Mr. Knott (rote Tür, die allein steht) | Wegweiser |
| `swh_crookeddoor` | Mr. Knott | 5× Realitätssplitter, 8× Krümmungsharz, 8× Seltsamer Stoff | **Zephyr-Gurtzeug**, 12× Geisterstahlbarren, 6× Realitätssplitter |

---

### Die Himmelslanze ⭐ NEU — keine Quest, ein Rezept

Die Drachenlanze, als Lanze aus gehaltenem Tageslicht: derselbe gelenkte
Strahl, der dem Mauszeiger folgt und alles auf seiner Linie trifft, nur golden.
**95 Schaden pro Treffer → 149,6** voll geschmiedet, 4 Treffer pro Sekunde,
10 Mana pro Sekunde, du stehst still, solange sie brennt (Drachenlanze: 80 →
126). Verzauberungskosten 2000, EPISCH.

**Rezept (Wolfram-Werkbank):** 3× Ätherwerker-Gehäuse, 10× Edenbronzebarren,
8× Goldener Pollen, 6× Aetheriumbarren. Das Gehäuse (Testgelände, Prototyp
Neun) war genau für „die erste Waffenstufe nach Sturmstahl" gedacht und hatte
bis jetzt keinen Abnehmer — Edenbronze auch nicht. Absichtlich **keine**
Questbelohnung: die Wolkengleve ist schon eine.

### Das Abenteurer-Tagebuch ⭐ NEU — alles an einem Ort

**Wie man es bekommt:** Jeder Spieler bekommt beim ersten Mal, wenn er in der
Himmelsweite steht (also beim ersten Aufstieg über die Treppe), ein
Abenteurer-Tagebuch ins Inventar, mit der Chatzeile „In deinem Gepäck liegt
ein Abenteurer-Tagebuch …". Bestehende Spielstände bekommen es **einmal**
nachgereicht: wer schon in der Himmelsweite war, eine Mod-Quest hat, den
Nebel berührt hat oder den Wächter angeworben hat, bekommt es beim nächsten
Einloggen. Wer schon eins trägt, bekommt kein zweites. Verloren? Der
**Himmelswächter** verkauft Ersatz für **100 Münzen** (1 auf Lager, +1 pro Tag).
Nicht herstellbar.

**Wie man es benutzt:** Rechtsklick auf das Buch im Inventar (oder aus der
Schnellleiste benutzen). Links stehen die sechs Kapitel — Himmelsweite,
Garten Eden, Steinfeld, Geisterreich, Krummes Jenseits, Hölle — jeweils mit
„erledigt/gesamt". Rechts das offene Kapitel, scrollbar:

- **Geschichte & Aufträge:** jede Stufe und Quest mit Status —
  `[Gesperrt]` (mit „Freigeschaltet nach: …"), `[Verfügbar]`, `[Aktiv]` (mit
  den Zielen, z. B. „10x Sturmsplitter (du hast 4 dabei)"), `[Erledigt]`
  (mit „Belohnung erhalten: …"). Dazu **von wem** und **wo diese Person
  wohnt**, und „(gilt für alle in dieser Welt)" bei allem, was welt- statt
  spielerbezogen ist. **Jede Quest des Wächters** (Turm finden, Anheuern,
  Katzen, Anker, Geisterkreide, die sechs Region-Schlüssel) sagt außerdem
  **„Warum:"** (wozu du das bauen/holen sollst) und **„Schaltet frei:"** (was
  danach aufgeht) — Spielerbefund 2026-09-24: „man checkt null warum man was
  jetzt bauen muss und was es macht".
- **Schlüssel, Rufsteine & Boss:** warst du schon hier; Region-Schlüssel
  erhalten; Schlüsselstück aufgestellt (Rufsteine wach); Boss besiegt (zählt
  jeden Sieg über diesen Vanilla-Boss).
- **Bewohner:** wen du schon getroffen hast (Name, Rolle, wo), die anderen als
  „???" mit einem Hinweis, wo man suchen muss.
- **Orte:** Wächterspitze, Zollhaus, Grange-Keller, Testgelände, Baum der
  Erkenntnis, zerbrochener Engel, Nebelwand, Gräber, Séance-Zirkel, Türhof,
  Höllensaum — gefunden oder mit Hinweis.
- **Überlieferung:** 14 kurze Texte, die mit dem Fortschritt aufgehen.

Knopf **Aktualisieren** holt den Stand neu vom Server, **Schließen** oder
**Esc** schließt das Fenster.

**Mehrspieler:** Quests, Nebelmal, getroffene Leute und besuchte Reiche sind
pro Spieler; Warden-Kette, Region-Schlüssel, Rufsteine und die „einmal
bezahlten" Bewohner-Quests sind pro Welt — genau so, wie das Spiel sie
speichert. Jeder Spieler sieht nur sein eigenes Tagebuch.

**Der Wächter im Dialog:** Sprichst du ihn an, steht oben im Dialogfenster
nicht mehr Smalltalk, sondern **die Aufgabe, die er dir gerade gegeben hat** —
seine eigenen Worte, dann „Deine Aufgabe: …", die Ziele mit deinen
Stückzahlen, Warum, Wo, Schaltet frei und Belohnung. Das Fenster bleibt offen,
bis du es schließt, und ist scrollbar. Die Sprechblase über seinem Kopf trägt
nur noch kurze Reaktionen (Begrüßung, „erledigt", Kreide); seine Bitten um die
Region-Schlüssel stehen nicht mehr in der Blase, sondern im Dialog. Ist nichts
offen, macht er Smalltalk wie jeder Siedler.

**Stand:** Server-Seite im Integrationstest geprüft (`/swhjournal`). Wie das
Fenster aussieht, ist **noch nicht im Spiel gesehen** — bitte einmal öffnen
und Rückmeldung geben.

---

## 5. Die Bosse — sechs, einer pro Realm

Beschworen an **Beschwörungssteinen**, die verstreut im eigenen Band stehen
(~0,97 pro 1000×1000 Kacheln). Nicht abbaubar. **Inert**, bis das
Schlüsselstück des Realms in einer Siedlung steht.

Skalierung ist Vanillas eigene Incursion-Kurve, angewandt per Mob über einen
permanenten Buff — nie über `LevelModifiers`, das würde die ganze Ebene buffen.

| Realm | Boss | aus welcher Incursion | Basis-HP | Stufe | **finale HP** |
|---|---|---|---|---|---|
| Skyreach | `cryoqueen` — Cryo-Königin | Snow Deep Cave | 18 000 | 8 (×3,18, dazu Aufschlag ×1,30) | **74 412** |
| Eden | `moonlightdancer` — Mondlichttänzerin | Moon Arena | 40 000 | 8 (×3,18, dazu Aufschlag ×1,35) | **171 720** |
| Steinfeld | `ascendedwizard` — Aufgestiegener Magier | Settlement Ruins | 44 000 | 9 (×3,58, dazu Aufschlag ×1,40) | **220 528** |
| Ghost Realm | `pestwarden` — Pestwächter | Swamp Deep Cave | 45 000 | 9 (×3,58, dazu Aufschlag ×1,45) | **233 595** |
| Crooked Beyond | `crystaldragon` — Kristalldrache | Crystal Hollow | 52 000 | 10 (×4,00, dazu Aufschlag ×1,55) | **322 400** |
| Hell | `mutanthydra` — Mutantenhydra | Scrapyard | 80 000 | 10 (×4,00, dazu Aufschlag ×1,65) | **528 000** |

Die Leiter ist absichtlich monoton: nach außen laufen heißt nach oben laufen.
Dein Wunsch war *„grundsätzlich sollen die Bosse auf Incursion Level 8–10
sein"* — genau da liegen sie.

### Die drei ortsgebundenen Wächter ⭐ — keine Bosse, aber auch kein Mob von der Wiese

Nicht beschworen, nicht wandernd: jeder steht in genau **einem** Gebäude und
gehört dazu. Alle drei erben von einem Vanilla-Archetyp und tragen dessen
Sprite-Sheet, kosten also **kein neues Pixel**; die HP kommen aus derselben
`SkyMobTiers`-Rollentabelle wie der Rest von Skyreach.

| Wächter | steht im | Rolle | lässt fallen |
|---|---|---|---|
| **Zollwerk** (`tollwright`) | Skyway-Zollhaus | Elite (Nahkampf, Stampfer) | 3–6 Himmelsstein, 65 %: 1–3 Sturmstahlbarren, 40 %: 1–2 Aetheriumbarren, 80 %: 400–1 200 Münzen — der Anwerbeschlüssel liegt nicht bei ihm, sondern in der Tresor-Vitrine |
| **Sauerbottich-Blüte** ＋ **Bottichlinge** | Grange-Keller | Elite, ruft Bottichlinge | **Die Mutter** — bei **jedem** Kill |
| **Prototyp Neun** | Sturmschleier-Testgelände | Fernkampf | **Sturmlinsenkern** — bei **jedem** Kill |

Zwei Vanilla-Verhalten sind absichtlich aus: das Zollwerk kann **keine Objekte
zerbrechen** (ein Aschegolem liefe sonst durch die Wände des Hauses, in dem
Magpie steht), und die Blüte bleibt **eine Frenzy-Stufe unter dem Limit** und
entlässt Bottichlinge, statt zu explodieren — ein Stachelbusch tötet sich am Limit
selbst, und das wäre ein Wächter, der vor dem Kampf stirbt. Beide greifen nur
**Spieler** an, nie den Siedler im selben Raum.

### Die acht einzigartigen Belohnungen ⭐

Kein einziges Stück ist „dieselbe Waffe mit größerer Zahl". **Sechs** liegen in
einer Truhe, Vitrine oder einem Fass am Ort, **zwei** fallen vom Wächter — und
zwar bei jedem Kill, nicht mit einer Chance, denn beide sind Anwerbeschlüssel.

| # | Belohnung | wo | wofür |
|---|---|---|---|
| 1 | **Verzollte Kassette** | Zollhaus, Tresor-Vitrine | Magpies Anwerbeschlüssel |
| 2 | **Himmelsweg-Freibrief** | Zollhaus, Vitrine im Kontor | **derzeit ohne Wirkung** — ein Andenken; die geplante Aufwertung von Magpies Handelsmissionen ist nicht gebaut |
| 3 | **Buch der unzustellbaren Post** | Zollhaus, Schrank | ein Andenken; Magpie kauft das Buch nicht, wohl aber jedes **Himmelspost-Paket**, das in der Welt liegt (140→90 Münzen) |
| 4 | **Die Mutter** | in der Sauerbottich-Blüte | Haldas Anwerbeschlüssel + nie verbrauchte Zutat |
| 5 | **Der Wächtertrunk** | Grange-Keller, das eine alte Fass | einmaliger, langer, starker Skywatch-Buff |
| 6 | **Sturmlinsenkern** | in Prototyp Neun | Vanes Anwerbeschlüssel + Antrieb seines Zeichentischs |
| 7 | **Ätherwerker-Gehäuse** ×2–4 | Testgelände, Vitrine | Material der Himmelslanze (3 Stück) |
| 8 | **Siegelring der Himmelswacht** | Grange-Keller, Schrank | Schmuck: zeigt unerforschte Skyreach-Bauten auf der Karte |

**Zum Siegel, ehrlich:** die Vorlage legt es ins Archiv der Wächterspitze,
übergeben „wenn der Haushalt vollständig ist". Dieses Story-Tor ist **nicht
gebaut**, und die Spitze ist in jedem bestehenden Spielstand längst gestempelt —
dort läge das Siegel also genau in den Spielständen unerreichbar, in denen es
etwas wert wäre. Es liegt darum bei Halda im Keller: die Letzte des Haushalts
bewahrt das Siegel des Haushalts. Wird das Tor später gebaut, zieht es um.

**Alle acht borgen sich vorerst ein Vanilla-Icon** (siehe `VANILLA_ASSET_MAP.md`
§1.7) — gezeichnet ist noch keines, und ein Item ohne Icon wäre im Inventar eine
ERR-Kachel.

### Kapitel 02 — sechs Schatzorte mit Mimik-Truhen ⭐ **NEU**

Dein Wunsch war: *„interessante neue Orte und Aufbauten / Mimic-Truhen die
geheimen Loot haben aber von Bossen beschützt werden"*. Das Ergebnis sind
**sechs neue Orte in fünf Reichen**, jeder mit einem anderen Grundriss-Gedanken,
aber alle um dasselbe gebaut: **eine bewachte Schatztruhe, und daneben Kisten,
von denen manche keine Kisten sind.** Pläne, Geschichten und Karten stehen in
`docs/design/chapter-02-hoards-and-mimics.md`.

| Ort | Reich | die Idee | Wächter (Mini-Boss) | Mimiks | Hauptbeute (Truhe hinter dem Wächter) |
|---|---|---|---|---|---|
| **Die falsche Schatzkammer** | Skyreach | Achsenhalle der Himmelsmünze: 8 Truhen am goldenen Läufer, dahinter der Tresor | Himmelsstein-Golem (gestärkt) + 2 Raureif-Wächter | **5 von 8** Truhen | 4–8 Sturmstahlbarren, 3–6 Aetherium, Sturmglas, 900–2200 Münzen; 30 % Sturmscheibe/Himmelsreißer/Donnerkopf, 15 % Himmelsstein-Herz |
| **Das eingestürzte Observatorium** | Skyreach | runde Kuppel, halb eingestürzt — und ein Nachtfall-Würfel **ohne Tür**: der versiegelte Instrumentenkeller, nur mit der Spitzhacke zu öffnen | Morgenstecher (gestärkt) + 2 Sturmirrlichter | die Truhe des Astronomen | Prismasplitter, Sturmglas, Sturmsplitter, 500–1400 Münzen; 20 % Aurora-Medaillon |
| **Das Heckenlabyrinth** | Eden | echtes 10×10-Irrgarten aus Waldhecke; 6 Sackgassen enden in einer Kiste, in der Mitte eine Lichtung | Verbotene Schlange (gestärkt) + 2 Eifersüchtige Ranken + 2 Blütenrachen | **3 von 6** Sackgassen | 4–8 Edenbronze, Schlangenschuppen, Giftzahn, Goldpollen, 1100–2800 Münzen; 15 % Blütenzahn |
| **Das Pilger-Ossarium** | Steinfeld | Kirchhof, Gruft, und ein Prozessionsgang mit **3 Druckplatten, verdrahtet mit Pfeilfallen** in der Wand; 6 Nischen | Hohler Engel (gestärkt) + 2 Steinerne Trauernde zwischen den Gräbern | **3 von 6** Nischen | Blassstein, Grabsalz, Echosplitter, Geistermoos, 1400–3500 Münzen; 20 % Trauerflor |
| **Das Hochzeitsmahl** | Ghost Realm | Festsaal, gedeckt für 24 — **13 der Stühle sind Besessene Stühle**; die Geschenke stapeln sich an der Ostwand | Trauerbraut (gestärkt) | 13 Stühle + **2 von 4** Geschenken | 4–8 Geisterstahl, Seelenfaden, Spektralerz, Knochenholz, 1700–4200 Münzen; 20 % Seelenhalsband |
| **Die Halle der vielen Türen** | Crooked Beyond | eine Wand, fünf Durchgänge: zwei sind Türen, **drei sind Türmimiken** | Seltener krummer Golem (gestärkt) + Krummes Gürteltier | **3 von 5** Durchgängen | Realitätssplitter, Warp-Harz, Seltsamer Stoff, Irrholz, 2200–5500 Münzen; 20 % Streifenhorn — und die Truhe selbst ist eine **echte Mimik-Truhe** (Vanilla `mimicchest`), die wie ein Mimik aussieht |

**Wie die Mimiks funktionieren — Vanilla-Mechanik, unverändert.** Das Spiel
macht es in seinen eigenen Höhlen-Truhenräumen genauso: statt der `storagebox`
steht dort ein `mimic`-Mob mit der Beute im Bauch (im dekompilierten
`RandomCaveChestRoom` nachgelesen). Er sieht aus wie eine Truhe, steht still,
wacht auf, wenn du auf **drei Felder** herankommst, und lässt beim Tod seine
Beute **plus eine Mimik-Truhe** fallen. Neu ist nur der Mob **Hortmimik**
(`hoardmimic`): der Vanilla-Mimik mit Skyreach-Elite-Werten (**1820 LP**, 50
Rüstung) statt der 600 LP aus der Tiefhöhle; in Eden, Steinfeld und dem Ghost
Realm wird er auf die Werte des jeweiligen Reichs angehoben. Echte Kisten sind
dieselbe Vanilla-`storagebox`, die das Spiel selbst gegen einen Mimik tauscht,
und Kiste wie Mimik schauen **in dieselbe Richtung** — man kann sie nicht am
Aussehen unterscheiden, nur am Näherkommen.

**Wächter:** immer ein Elite-Gegner **aus dem eigenen Reich**, mit dem
unsichtbaren Stufen-Buff der Boss-Portale auf Stufe 5 (**×2,12 LP, ×1,52
Schaden**) — ein Mini-Boss, kein Leiter-Boss (die beginnen bei ×3,18).

**Wie oft:** die vier äußeren Orte gibt es in ihrem Reich regelmäßig; die zwei Skyreach-Orte stehen auf einem eigenen, dünneren Raster — auf einem Testseed 9 Schatzkammern und 5 Observatorien im ganzen Band, damit die 16 alten Skyreach-Orte keinen Platz verlieren.

**Einmal, nie wieder:** alle Gegner entstehen genau einmal, wenn der Ort
generiert wird, verschwinden nicht und spawnen nicht nach. Die Truhen werden bei
der Generierung gefüllt und nie wieder aufgefüllt. **Alte Spielstände** bekommen
die Orte nur in Gegenden, die noch nie generiert wurden (`/swhreset world` kann
POIs nicht nachrüsten).

**Schilder** (Deutsch im Spiel): *„Alle Truhen werden täglich geprüft. Manche
prüfen zurück."* · *„Im Instrumentenkeller befindet sich nichts von Wert."* ·
*„Geh … in der Mitte, und nie auf die hellen Steine."* (die Mitte ist genau da,
wo die Druckplatten liegen) · *„Bitte nehmen Sie Platz. Die Plätze finden Sie."*
· *„EINE DAVON IST EINE TÜR."* (Es sind zwei.)

**Geprüft:** Auf einem echten Server stehen alle sechs mit `missing=0`, jeder
Gegner an seinem Platz, jeder Wächter gestärkt, jeder Hortmimik mit Beute, jede
Truhe gefüllt (`scripts/integration_test.sh`, Zeilen `realmpoi hoard`). **Nicht
geprüft:** wie sie im Spiel aussehen und sich spielen — das kann nur ein Blick
im Client beantworten.

---

## 6. Die Gebiete, kompakt

| Gebiet | Kacheln vom Ursprung | Biome | Feinde | Critter | Tiere | NPCs | Quests | POIs | bewachte Orte / 1000×1000 |
|---|---|---|---|---|---|---|---|---|---|
| **Skyreach** | 0 – 1 800 | 4 | 8 | 4 | 2 | 7 | 5 | 18 ⭐ | 28,7 |
| **Eden** | 600 – 2 880 | 3 | 5 | 0 | 0 | 1 | 3 | 2 | 11,4 ⭐ *(vorher 0)* |
| **Steinfeld** | 1 920 – 4 200 | 3 | 4 | 0 | 0 | 1 ⭐ | 2 ⭐ | 1 | 20,3 |
| **Ghost Realm** | 2 880 – 5 280 | 5 | 9 | 0 | 0 | 4 | 4 ⭐ | 1 | 21,8 |
| **Crooked Beyond** | 4 200 – 5 640 | 5 | 8 | 1 | 0 | 1 | 3 | 1 | 30,9 |
| **Hell** | 4 800+ | 2 | 4 | 0 | 0 | 0 | 1 (`swh_keyhell`, beim Warden) | 4 | 0 (Wächter-Packs definiert, nie platziert) |

**Sechs neue Orte im Skyreach (Stand 2026-09-09).** Zu Turm, Stadt, Zollbrücke
und Gasthaus kommen sechs Plätze aus dem POI-Dossier: das **Skyway-Zollhaus**
mit Magpie im Kontor, die **Skywatch-Wegstation** am Straßenrand, die
**Tauhalter-Hütte** mit fünf Tauschnecken im Gehege, der **Schäferhof** mit
Weberei, Bett und einer Herde aus vier Glimmerziegen und einem Nimbus-Yak, das
**Institut für Angewandte Fallkunst** (eine Startrampe, die im Nichts endet,
sechs abstürzende Maschinen und ein Krater — in dessen Mitte ein völlig
unversehrtes Nimbus-Yak steht und kaut) und das **Passagen-Wegehaus** mit zwei
echten Betten und einem Vorratsschrank. Was diesen Orten noch fehlt, sind die
Stelen, der Wolkenquell-Brunnen und die Wegsteine: dafür gibt es noch keine
Grafik, deshalb stehen sie nicht da, statt als Fehlertextur.

**Stadt, Gasthaus und Zollbrücke neu eingerichtet (Stand 2026-09-23).** Nach
deinem Test ("keine schönen angeordneten Häuser … zu oft wiederholte
Anordnungen") hat die **Himmelsstadt** jetzt fünf verschiedene Häuser statt vier
gleicher: eine **Bäckerei** (Vorratskammer mit Fässern und Mehlsäcken,
Backstube mit Kochtopf, Arbeitstisch, Verkaufstheke und Kundentisch, Kammer des
Bäckers), einen **Laden** an der Nordstraße (Lager, Theke, vier Auslagen), das
**Haus des Gelehrten** (Studierzimmer voller Regale um einen Lesetisch auf dem
Teppich, zwei Schreibpulte, Instrumentenzimmer mit Fernrohr und Astrolabium,
Schlafzimmer), das **Weberhaus** (Werkstatt mit zwei Webstühlen und
Tuch-Auslage, Schlafzimmer, Küchenecke) und das **Familienhaus** (Stube mit
Esstisch für sechs auf dem Teppich und Teeecke, Küche, Elternzimmer,
Kinderzimmer mit zwei Betten und Spieltisch). Der Platz in der Mitte ist ein
Teich mit vier Bänken und Blumenbeeten an den Ecken; die beiden Gärten haben
je genau ein Tor, dort wo der Weg hineinführt. Das **Gasthaus** hat zwei
Gästezimmer, eine Küche und eine Gaststube für sechzehn mit Theke, Hockern und
zwei Fässern; die **Zollbrücke** hat ein Geländer, und ihre zwei Häuser sind
jetzt ein Zollkontor und eine Wächterstube statt zweimal dasselbe Zimmer. Gilt
für Gegenden, die neu erzeugt werden.

**Alle übrigen Häuser und Orte eingerichtet (Stand 2026-09-24).** Jedes Gebäude
im Mod ist jetzt ein bestimmter Ort mit einem Zweck, keine zwei gleich:

- **Skyreach — der Bogenturm** ist das Ordenshaus der Skywatch: unten eine
  Versammlungshalle mit Bänken und Bannern, links das Refektorium (Küche, zwei
  Esstische für acht), rechts die Bibliothek; darüber der Schlafsaal der
  Novizen (fünf Betten), der Kartensaal (Kartentisch, Kartografentisch,
  Astrolabium, Fernrohr) und die Stube des Wächters; ganz oben die
  Sternwarte und die Laterne mit dem Seraphen. Jede Tür sitzt jetzt in einer
  Wand (drei standen vorher frei auf dem Boden).
- **Eden — der Kronengarten**: das L-Haus des Obergärtners (Küche für acht,
  Schlafzimmer, Pflanzraum mit Setzlingen), die „Krone" selbst (das
  Samenbecken in einem Blumenring, vier Bäume der Fülle, zwei Bänke, ein Tor),
  ein eingezäunter Obstgarten mit Beerensträuchern auf Ackerboden und das Haus
  des Samenhüters (Vorraum, Samenlager, Stube). **Das Gärhaus**: Gärhalle mit
  vier Fässern, Kochtöpfen und Obstsäcken, Probierstube mit Tafel für acht,
  Büro des Winzers mit Bett.
- **Steinfeld — der Gedenkhof**: ein gefallener Engel auf dunkel gewordenem
  Goldsockel, Blumenbeet, zwei Gräberreihen, eine Pilgerrast mit
  Opfertisch, die Ecke des Steinmetzen. **Der Friedhof**: Weg zum Trauernden,
  acht Gräber mit Blumen, eine Besucherbank, die Truhe des Totengräbers. **Die
  Kapellenruine**: Altar mit angelaufenen Kelchen, Lesepult, Kirchenbänke zum
  Altar hin — eine davon unter einer eingestürzten Deckenplatte — und die
  Sakristeitruhe.
- **Ghost Realm — das Laternenarchiv**: Lesesaal mit Regalreihen, dem
  Seelenbecken und zwei Lesetischen für zwölf, Katalograum, Zimmer des
  Archivars. **Das Spukhaus** des kopflosen Butlers: die lange Tafel ist für
  Gäste gedeckt, die nie kamen, dazu Teeecke, Schlafzimmer und Studierzimmer
  mit der Knochentruhe. **Das Mausoleum**: Familienaltar, Sarkophag und zwei
  Särge (jetzt ganz — vorher lag nur die halbe Sargkiste da). **Der
  versunkene Friedhof**: Weg zur erhöhten Mitte, ein offener Sarg vor der
  Truhe, Trauerbank, Laternen am Tor.
- **Crooked Beyond — der Basar**: der Türverkäufer (Türen zum Verkauf, frei
  stehend), der Uhrmacher, der Krämer mit Lager, zwei Marktstände. **Die lange
  Tafel** hat jetzt **einen echten Tisch**: siebzehn Tische in einer Reihe,
  gedeckt mit einem längst verdorbenen Festmahl, die langen Stühle zu beiden
  Seiten, am Kopfende der einzige normale Stuhl des Reichs. **Das umgestülpte
  Haus**: Schlafzimmer, Küche, Wohnzimmer — und Badewanne samt Klo mitten im
  Freien. **Der Türenhof** gehört jetzt dem Türsteher: Fußmatte vor jeder Tür,
  sein Schreibtisch, eine Warteschlange aus langen Stühlen. **Das krumme Haus**
  im Beetlefreak-Hollow ist das Haus eines Wächters: Esstisch für sechs, Bett,
  Küche, der Rabe am Ostfenster.
- **Hell — Grenzamt 666-B**: die Straße läuft durchs Amt über einen roten
  Läufer, an zwei Schaltern mit vier Beamten vorbei; Wartebänke, der seit 400
  Jahren wartende Skelett-Antragsteller, ein Ticketautomat, das Aktenzimmer.
  **Die Verwaltung**: Abteilung für ewiges Warten, Akten und Siegel, Moxies
  Kantine, das Büro des Direktors (Thron hinter dem Schreibtisch) mit dem
  Schlafsaal der Beamten. **Brims Schmiede**: fünf Essen, sechs Ambosse,
  Rüstungsständer, Übungspuppen, Ladentheke, Waffenständer. **Der
  Höllenjahrmarkt**: ein Karussell aus Schafstühlen, Moxies Essensstand,
  Kraft- und Schießbude mit Plüschpreisen, die Wahrsagerin und Vex' Hehlerei
  mit gestohlenen Himmelswaren.
- **Oberfläche**: das Aeronautenlager hat jetzt eine Bank am Feuer, einen
  Lagertisch mit dem Logbuch und eine Reparaturecke; der Himmelsschrein einen
  Opfertisch mit Kerze und zwei Pilgerbänke; am Kraterrand stehen die
  Laterne und der Probensack eines Schürfers, der schneller war.

Zwei Fehler sind dabei aufgefallen: Deko auf **Schreibtischen** verschwindet in
Necesse sofort (ein Schreibtisch ist kein Deko-Träger) — in den neuen Orten
steht darum nichts mehr auf Schreibtischen; in Stadt, Zollbrücke und Zollhaus
(auch das Buch auf Magpies Tisch) ist das noch **offen**. Und die Beute von
Friedhof und Kapelle lag in einer **Kiste, die gar kein Inventar hat** — sie
liegt jetzt in einer echten Truhe. Gilt für Gegenden, die neu erzeugt werden.

**Drei weitere, und die ersten feindlichen (Stand 2026-09-10).** Die
**Nightfell-Schanze** ist ein umwehrtes Lager mit Türen auf allen vier Achsen,
zwei Blockhäusern und einem absichtlich leeren Hof, den du überquerst statt ihn
zu plündern — gebaut aus dem dunklen Nightfell-Mauerwerk, das seit vier
Versionen herstellbar ist und das die Weltenerzeugung noch nie irgendwo
hingestellt hat. Zwei Skystone-Golems und zwei Rime-Sentries halten sie; der
leere Sockel mitten im Hof ist die ganze Geschichte: jemand war vor dir da. In
den beiden Schränken und dem Fass des Zeughauses liegen Stormsteel-, Aetherium-
und Sturmglas-Vorräte und mit 35 % eine der drei schweren Waffen.

Die **Aether-Manufaktur** ist das erste wirklich mehrräumige Gebäude im Mod:
Maschinenhalle, Lager und Kontor, mit allen drei Berufs-Werkbänken in einer
Reihe an der Westwand — wer noch nie eine Aetherschmiede gebaut hat, läuft hier
in eine, die schon arbeitet. Ein durchgehender 5×13-Teppich liegt der Länge
nach in der Halle, und es steht nichts darauf. Der tiefste Materialfund des Mods
liegt in ihren drei Schränken.

Der **Amboss des Souveräns** ist gar kein Gebäude: eine zersprungene
Schieferschale, eine geschlossene Balustrade mit vier Toren, vier Seraphen und
ein Podest. Was noch fehlt, ist der Altar — dafür gibt es keine Grafik, und das
Dossier sagt ausdrücklich, ihn nicht mit einer Beschwörung auszuliefern, die er
nicht einlösen kann. Ohne ihn ist es ein Ort zum Finden, noch kein Kampf. Auch
die Skywatch-Revenants und die Fulgur-Shades, die alle drei Orte bevölkern
sollen, warten auf ihr Sprite-Sheet.

**Und noch drei am selben Tag — damit stehen zwölf der vierzehn Orte des
Dossiers.** Das **Ungeöffnete Tor** ist das beste Bild des Dossiers und das
billigste: ein Damm führt auf ein Schachbrett-Podest hinaus, zwei riesige
Seraphen stehen einander gegenüber — und das Tor dazwischen **ist nicht da**.
Niemand hat es je aufgehängt. Zwölf Kandelaber auf einem geschlossenen Ring,
zwei Schränke am Podest mit Seraphenholz, Aetherium- und Goldbarren. 272 seiner
621 Kacheln bleiben leer, und genau das ist der Ort.

Der **Prismenchor** ist ein Ring in den Aurora-Untiefen mit einem Podest darin —
und ehrlicherweise noch nicht mehr. Seine sieben singenden Kristallsäulen und
der Wolkenquell-Brunnen in der Mitte sind seine ganze Mitte und sein ganzes
Licht, und für beide gibt es noch keine Grafik. Damit wartet auch das Rätsel
(alle sieben Säulen anschlagen, bevor die erste verklingt) und Souveränsplitter
III auf die Kunst. Zwei Aurora-Flocken halten die Mitte.

Das **Serpentinenriff** ist der einzige Ort im ganzen Katalog, der **im offenen
Nebelmeer** liegt: 123 Kachelchen Riff in 625 Kacheln Wasser, ein Rücken aus
Skystone quer hindurch, zwei Aeronauten-Wracks mit einem halb zusammengefallenen
Ballon darüber, ein Laderaum mit Aetheriumerz, Prismensplittern und Windseide,
zwei Aetherium- und zwei Prismensplitter-Adern zum Abbauen — und eine
**Nebelschlange**, die garantiert dort ihre Runden zieht. Die Wurmkette gibt es
seit v0.5.1, aber sie erscheint nur über offener See, wo niemand schwimmt; hier
triffst du sie zuverlässig, und das Riff ist der einzige feste Boden weit und
breit. Der Reefmaw im Wrack wartet noch auf sein Symbol.

**Zur Dichte, weil die Zahl leicht falsch gelesen wird:** ein Spawn-Gewicht
entscheidet, **was** dir entgegenkommt, nie **ob**. Das macht die Kachel selbst
über ihre Spawn-Tickets (`SkyPressure`): 600 auf bewachtem Boden, 100 im
Anmarschring, 45 in den „Wilds" (~1/6 des Landes, Crooked 30 und seltener),
und **0 überall sonst**. Vanillas normaler Boden ist 100, sein totester
(`AshTile`) ist 2 — die Zahlen liegen auf Vanillas eigener Skala. Offenes Land
zwischen Orten ist wirklich still, genau wie gewünscht.

---

## 7. Sprites — was eigen ist und was geliehen

**Der Mod liefert 358 eigene PNGs.**

| Ordner | Dateien | was drin ist |
|---|---|---|
| `items/` | 131 | Item- und Menü-Icons |
| `objects/` | 97 | Welt-Sheets für Objekte, Wände, Möbel, Stationen |
| `mobs/` | 33 | Lauf-Sheets der Kreaturen |
| `mobs/icons/` | 26 | Bestiarium-/Siedlungs-Gesichter |
| `tiles/` | 21 | Boden + `_splat`-Autotile-Atlanten |
| `art/supplied/` | 16 | Generator-Zwischenstufen |
| `player/armor/` | 12 | getragene Rüstung auf dem Spielerkörper |
| `player/weapons/` | 5 | Waffen in der Hand |
| `particles/` · `projectiles/` | 5 · 4 | Effekte |
| `ui/mapicons/` | 3 | Weltkarten-Pins |
| `objects/statues/` · `objects/carpets/` | 2 · 2 | Statuen, Teppiche |
| Wurzel | 1 | `preview.png` |

**Von 56 registrierten Mobs haben 22 ein eigenes Welt-Sheet und 26 ein eigenes
Bestiarium-Icon.** Der Rest teilt sich so auf:

| Gruppe | Anzahl | warum ohne eigenes Sheet |
|---|---|---|
| **Menschen** (die 10 Siedler + Ghost Guide + Warden-Siedler) | 11 | **korrekt so** — ein `HumanShop` wird aus Vanillas Menschenkörper plus echten Kleidungs-**Items** gezeichnet, genau wie Vanillas Ältester. Kein Mensch im Spiel hat ein eigenes Sheet. |
| **Segmente / Begleiter** (Nebelschlangen-Körper und -Schwanz, Watchmote) | 3 | werden vom Kopf bzw. Elternobjekt gezeichnet |
| **Feinde, die eine Vanilla-Klasse beerben** | 19 | Absicht und dokumentiert: sie erben Sheet und Verhalten vom Vanilla-Mob, den sie unterklassen. Arbeitsweise laut `AGENTS.md`: erst mit Vanilla-Platzhaltern bauen, später in einem Durchgang ersetzen. Jeder geliehene Pfad steht mit Pixelmaß in `docs/VANILLA_ASSET_MAP.md`. |

### Geliehene Vanilla-Texturen — die vollständige Liste

**21 literale Pfade** zeigen auf Vanilla-Dateien statt auf unsere:

| Pfad | wofür |
|---|---|
| `mobs/icons/farmerhuman` | Eveleens Gesicht |
| `mobs/icons/pawnbrokerhuman` | Mortimers Gesicht |
| `mobs/icons/blacksmithhuman` | Casperns Gesicht |
| `mobs/icons/stylisthuman` | Eleanors Gesicht |
| `mobs/icons/exoticmerchanthuman` | Mr. Knotts Gesicht |
| `mobs/icons/elderhuman` ⭐ | **Ives' Gesicht** |
| `mobs/bee` · `mobs/cow` · `mobs/mimic` ⭐ (Hortmimik) · `mobs/crocodile` · `mobs/dragonwhelp` · `mobs/dryadsentinel` · `mobs/scorpion` · `mobs/stabbybush` | Kreaturen, die eine Vanilla-Klasse beerben |
| `tiles/cryptash_splat` · `ravenfloor_splat` · `stonebrickfloor_splat` · `swampgrass_splat` · `swamprock_splat` | Autotile-Atlanten für Böden ohne eigene Kunst |
| `items/cryoglaive` · `player/weapons/cryoglaive` ⭐ | **Wolkengleve** — beim Laden weiß-golden umgefärbt (`RecolouredVanillaTexture`); legst du `items/cloudglaive.png` / `player/weapons/cloudglaive.png` ab, gewinnt deine Datei |
| `items/dragonlance` · `player/weapons/dragonlance` ⭐ | **Himmelslanze** — genauso umgefärbt; eigene Datei: `items/skylance.png` / `player/weapons/skylance.png` |

**Wie die Umfärbung aussieht, ist ungeprüft:** der Server hat keine Sprites.
`python3 tools/recolour_preview.py --vanilla <Sprite-Dump>` zeigt es vorab,
sonst erst im Spiel.

Dazu **getragene Kleidung**, die nur Item-IDs sind und keine Pixel kostet:
Eveleen (`dryadhat`/`dryadchestplate`/`dryadboots`) · Mortimer (`tophat` /
`thiefscloak` / `dressshoes`) · Caspern (`nightsteelveil` / `smithingapron` /
`smithingshoes`) · Eleanor und Ghost Guide (`snowhood` / `snowcloak` /
`clothboots`) · Knott (`jesterhat` / `labcoat` / `jesterboots`) · Ossian
(`runichat` / `voidrobe` / `arcanicboots`) · **Ives ⭐ (`leatherhood` /
`clothrobe` / `clothboots`)**.

### In diesem Durchgang gezeichnet: **null**

Ehrlich gesagt: dieser Pass hat **kein einziges neues PNG** produziert — der
Commit enthält null Bilddateien. Ives trägt Vanillas Ältesten-Gesicht und drei
Vanilla-Kleidungsstücke, beides mit Pixelmaß in `docs/VANILLA_ASSET_MAP.md`
eingetragen. Der Reproduzierbarkeits-Gate bestätigt es:
`tools/asset_generator/generate_assets.py` läuft durch und produziert
byte-identische Dateien.

### Die Bestiarium-Icons: gelöst, ohne ein neues Pixel

**Das war vorher als „zwölf fehlende Icons" notiert. Es war keine Kunstfrage.**

Neunzehn Kreaturen tragen absichtlich keine eigene Kunst — jede erbt entweder
eine Vanilla-Klasse samt Zeichnung oder blittet direkt ein Vanilla-Sheet. Das
Bestiarium wusste das nicht: `MobRegistry.loadMobIcons` lädt
`mobs/icons/<id>` für **jeden** registrierten Mob und fällt auf die ERR-Kachel
zurück. **Sechs davon zeigten das schon heute im Journal** — Steinfelds vier
plus Türmimik und Zungenpflanze.

Der Ausweg war eine Zeile, kein PNG: `Mob.getMobIcon()` ist überschreibbar, und
das Journal fragt den **Mob**, nicht das Register
(`FormJournalEntryComponent.java:240`, VERIFIED [jar]). Alle neunzehn geben
jetzt das Gesicht des Tiers zurück, dessen Körper sie tragen —
`mobs/BorrowedMobIcon`.

**Zwölf Icons zu malen wäre auch inhaltlich falsch gewesen.** Ein Drifter, der
als Deep Cave Spirit herumläuft und im Journal etwas anderes zeigt, sind für den
Spieler zwei Kreaturen. Wenn diese Mobs eigene Körper bekommen, bekommen sie im
selben Durchgang eigene Gesichter — und jede Überschreibung verschwindet mit
ihnen.

**Was jetzt zählt:** zehn Feinde neu im Bestiarium (Edens drei, Ghosts sieben),
sechs kaputte Zeilen repariert.

**Zwei bleiben offen, nachvollziehbar.** Eden-Schlange trägt `crocodile`,
Verbotene Schlange trägt `petdragonwhelp` — die einzigen beiden Eltern, die
Vanilla selbst *nicht* ins Bestiarium stellt. Ob es dort ein Icon zu leihen
gibt, lässt sich von einem Dedicated Server aus nicht prüfen: der rendert nichts
und liefert null PNGs. **Ein Blick in dein Journal klärt es.** Zeigen die beiden
ein Bild, ist es je ein Wort in `EdenRealm.registerMobs`; zeigen sie ERR, sind
das die einzigen zwei Icons, die dieser Mod wirklich schuldet.

### Die Galerie

Alle 341 einzeln abbildbaren Sprites stecken in der Handy-Seite oben —
originalgroß, mit Pixelraster, auf Schachbrett damit Transparenz sichtbar
bleibt. Nicht dabei: `art/supplied/` (Generator-Zwischenstufen) und das
Vorschaubild.

---

## 8. Bestehende Spielstände: `/swhreset`

Alles ADMIN, alles serverseitig. Vollständig in `docs/SAVE_COMPAT.md`.

| Befehl | was er tut |
|---|---|
| `/swhreset` | **meldet nur.** Story-Stufe, Region-Keys, Portal-Freischaltungen, Bewohner-Ansprüche, Nebel- und Kreide-Register, wer gerade im Himmel steht, wie viele Portale in der Nähe stehen. Ändert nichts. |
| `/swhreset world` | trägt fehlende Inhalte in **bereits erzeugtes Gelände** nach: Boss-Portale, Wachtrupps, Bewohner, Herden. 1024×1024 um dich herum. Zweimal laufen lassen ist sicher. |
| `/swhreset quests confirm` | setzt die ganze Kette auf **vor den ersten Aufstieg** zurück |
| `/swhreset all confirm` | beides + löscht die Ein-pro-Welt-Ansprüche *(Warnung in der Doku lesen)* |
| `/swhreset regenerate` | **meldet nur**, was ein kompletter Neuaufbau des Himmels tun würde |
| `/swhreset regenerate confirm` | **löscht den ganzen Himmel** (alle Mod-Gebiete, auch die Warden-Spitze und alles drumherum) und erzeugt ihn mit dem aktuellen Build **neu**. Quest-Fortschritt bleibt. Siehe unten. |

**Behoben am 24.09.2026:** `confirm` wurde bisher gar nicht verlangt — ein
`/swhreset quests` ohne `confirm` hat die Kette trotzdem zurückgesetzt
(nachgewiesen mit dem alten Build). Jetzt ändert die Form ohne `confirm`
wirklich nichts.

### Den ganzen Himmel neu erzeugen: `/swhreset regenerate confirm`

Für den Fall „ich will die Mod neu testen, und **alle** Mod-Gebiete, auch direkt
um die Spitze, sollen mit dem neuen Build neu entstehen — die Quests dürfen
bleiben“.

**Schritt für Schritt (Einzelspieler):**

1. **Spielstand sichern** (Welt-Datei kopieren). Der Befehl legt zwar selbst
   eine Sicherung des Himmels an, aber eine Kopie der ganzen Welt schadet nie.
2. Den neuen Mod-Build installieren und die Welt laden.
3. Im Chat: `/swhreset regenerate` — **ändert nichts**, zeigt nur an, was
   passieren würde: wie viele Himmels-Dateien es gibt, wer gerade oben steht,
   welche Bewohner-Ansprüche bleiben oder frei werden, und eine Warnung, falls
   der Warden in einer Siedlung *im Himmel* wohnt (er ginge mit verloren —
   dann vorher umziehen lassen).
4. Alles, was du **im Himmel gebaut oder gelagert** hast und behalten willst,
   vorher herunterholen: Truhen-Inhalte, Tiere, Möbel. **Alles, was im Himmel
   steht, ist danach weg.**
5. Im Chat: `/swhreset regenerate confirm`. Stehst du gerade im Himmel, wirst
   du zuerst zu deiner Treppe auf der Oberwelt zurückgeschickt (Chat-Meldung
   „Der Himmel wird neu erschaffen …“). Die Meldung endet mit
   `swhreset regenerate: DONE`.
6. Die Treppe wieder hinaufsteigen — du kommst an der **frisch gebauten
   Warden-Spitze** an, am selben Ort wie vorher.

**Dedizierter Server:** dasselbe als Admin im Spiel-Chat. In der Server-Konsole
geht es nur, wenn **niemand online** ist (dann pausiert der Server) — sonst
lehnt der Befehl ab. Läuft gerade eine Speicherung, ebenfalls ablehnen und
nach „Completed world save“ nochmal.

**Was bleibt:** Story-Stufe, angeworbener Warden, Katzen „nach Hause gebracht“,
Anker, Region-Keys, freigeschaltete Boss-Portale, alle Bewohner-Questketten,
Eleanor/Eveleen/Knott, Veil-Marken und Kreide, deine Treppen-Heimwege, das
Tagebuch, Inventare, **die ganze Oberwelt** samt Siedlungen — und die drei
Oberwelt-Orte (Aeronautenlager, Himmelsschrein, Himmelssplitter-Krater) bleiben
so, wie sie sind.

**Was neu entsteht:** das gesamte Himmelsgelände aller Realms, alle Gebäude
und POIs, die Warden-Spitze mit Leuchtfeuer und Katzenkorb, Mautstelle,
Gutskeller und Testgelände samt Wächtern und Belohnungen, Boss-Portale,
Wachtrupps, Herden, die Bewohner, die nur im Himmel standen, und die Katzen
(wohnen sie schon in deiner Stadt, bleiben sie dort und werden nicht doppelt
erzeugt).

**Was verloren geht:** alles, was du im Himmel gebaut hast, inklusive einer
Siedlung im Himmel mit ihren Siedlern, Truhen und ihrem Inhalt, Betten dort
(dein Wiederbelebungspunkt geht dann zurück auf den Weltstart), ein dort
stehendes Schlüsselstück, ein Séance-Kreis. Die Belohnungen der drei
Wahrzeichen liegen im neuen Himmel **noch einmal** bereit.

**Sicherung:** `%APPDATA%\Necesse\swh-sky-backups\<Welt>-<Datum-Uhrzeit>\`
(dedizierter Server: in dessen Datenordner). Darin liegen die Himmels-Dateien
in derselben Ordnerstruktur wie im Spielstand; zum Zurückholen die drei
`skyreach2`-Teile im Spielstand löschen und den Ordner `levels/` aus der
Sicherung hineinkopieren.

**Nachgewiesen** auf einem echten Server (`scripts/regenerate_check.sh`):
Markierung im fernen Himmel weg, Spitze am selben Ort neu, drei Wahrzeichen
neu, Zensus `kinds=30/30`, Fortschritt auch nach Neustart da, Markierung auf
der Oberwelt unberührt. **Noch nicht** nachgewiesen (der Test hat keinen
echten Spieler): das Zurückschicken eines Spielers, der oben steht, und ein
Einzelspieler-Durchlauf — das ist der erste echte Test durch dich.

**Warum das nötig ist:** `onRegionGenerated` läuft **genau einmal pro Region,
für immer**. Eine Welt, die vor dem 03.09.2026 erkundet wurde, hat dort **keine
Boss-Portale** — und keine Menge Spielen erzeugt eines. Die Reparatur geht nur,
weil jedes Gitter in `SkyLevel` eine reine Funktion aus Weltseed und Kachel
ist: dieselbe Region ein zweites Mal ablaufen liefert exakt die Orte, die die
ursprüngliche Generierung geliefert hätte.

**Was er nicht kann:** vorhandenen Boden neu malen. Gelände, POI-Presets und
der `WorldPreset`-Katalog schreiben Kacheln, und erkundetes Gelände ist
vielleicht längst deine Basis. Für Gebäude gilt: weiter rausgehen.

**Was er nie anfasst:** Oberflächen-Ebenen, Inventare, Siedlungen, Items —
`docs/DESIGN_DECISIONS.md` verbietet es.

### Testreihenfolge für einen Durchlauf A–Z

```
/swhreset                    # erst schauen: was hält diese Welt eigentlich?
/swhreset world              # nachtragen, was seit deinem letzten Build dazukam
/swhreset quests confirm     # und die Kette neu starten
```

1. **Skyreach** — hochsteigen, Spitze finden, Warden anwerben (30 000),
   Katzen, Anker.
2. **Nebel und Kreide** — rausgehen, bis Soul Exposure stapelt, dann mit dem
   Warden reden: er gibt die Geisterkreide. (`/veilmark` ist die Abkürzung.)
3. **Region-Keys** — er bietet sie einzeln an, sobald die Kette DONE ist. Jedes
   Schlüsselstück **in eine Siedlung stellen**.
4. **Bosse** — fünf, an ihren Beschwörungssteinen.
5. **Die Leute der Realms** — Eveleen, **Ives**, Mortimer, Caspern, Eleanor,
   Knott. Fünf davon erlassen dir ihre Gebühr, wenn du ihre Quest machst.

---

## 9. Neu in diesem Durchgang — die Checkliste

| # | Was | Datei |
|---|---|---|
| 1 | **Gebiets-Zensus** — misst Besetzung, Dichte, NPCs, Quests, POIs pro Realm aus dem Quelltext | `tools/area_census.py`, `docs/AREA_OVERVIEW.md` |
| 2 | **Edens Wachtrupps waren toter Code** — `getGuard()` in allen drei Biomen definiert, nie aufgerufen. 0 → 11,4 bewachte Orte | `SkyLevel.placeGuardPacks` |
| 3 | **Ives**, Steinfelds erster Bewohner überhaupt, mit Sortiment | `mobs/IvesMob.java`, `settlement/SteinfeldResidents.java` |
| 4 | **Drei Quest-Ketten**: `swh_steinfeldvigil`, `swh_mortimerrites`, `swh_caspernforge` | `quest/*.java` |
| 5 | **`/swhreset`** — Bericht, Retrofit, Reset | `commands/SwhResetCommand.java`, `docs/SAVE_COMPAT.md` |
| 6 | **Bugfix: doppelte Boss-Portale.** Ein Ort, dessen Zielkachel blockiert war, konnte ein **zweites** Portal bekommen. Gemessen: `bossportals=+1` beim zweiten Durchlauf | `SkyLevel.placePortalAt` |
| 7 | Wachtrupps und Herden sind jetzt idempotent | `SkyLevel` |
| 8 | 28 neue Locale-Einträge, EN + DE synchron | `locale/*.lang` |

**Gates:** Build 0 · Content-Ledger 0 undokumentiert · Locale 33 Probleme (alle
vorbestehend, 0 neue) · Sheet-Format + Rotation OK · Generator byte-identisch ·
**Integrationstest exit 0, 0 FAIL**, erweitert um drei `/swhreset`-Durchgänge
und eine dritte Serverphase.

---

## 10. Was offen bleibt — ehrlich, nach Kosten für den Spieler

1. **Hell ist gebaut, aber leer an Menschen.** Zwei Biome (Höllensaum,
   Ofenweite), vier Gegner, vier POIs und die Mutantenhydra (528 000 LP, über
   `swh_keyhell`) stehen — es fehlen NPCs, Critter und eigene Quests, und die
   Wächter-Packs der Biome werden nie platziert (`tools/area_census.py`).
2. **Nur Skyreach hat Critter oder Tiere.** Vier Realms haben null
   Umgebungsleben. Steinfelds Antwort steht fertig in `WORLD_DESIGN` §A3.4 und
   ist schlicht ungebaut: Geister, die nicht angreifen, die stehen, die ewig
   denselben Weg zwischen zwei Grabsteinen gehen.
3. **Zwölf Feinde kommen nie ins Bestiarium** — Blocker sind zwölf Icons, nicht
   das Flag.
4. **Zehn Realm-Materialien nennt kein Rezept.** Crookeds sechs und Steinfelds
   vier haben jetzt Quest- und Shop-Nachfrage; *gecraftet* wird mit keinem.
5. **Keine echten neuen Berufe** — siehe §1. `ExpeditionMissionRegistry` ist
   der offene Weg.
6. **Eden und Crooked Beyond haben je nur eine Person.**
7. **Eden Shallows hat genau einen Spawn-Eintrag**, und Edens drei Tabellen
   sind die einzigen im Mod ohne Obergrenze pro Ring (`add` statt `addLimited`).
8. **Die 13 alten POI-Hüllen bleiben dünn möbliert** — und POI-Presets sind das
   Einzige, was `/swhreset world` nicht nachtragen kann.
