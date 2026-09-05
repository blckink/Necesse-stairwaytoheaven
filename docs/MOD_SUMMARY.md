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

---

## 2. Alle NPCs

Zehn benannte Menschen, zwei Katzen, ein beschworener Geist. Jeder existiert
**genau einmal pro Welt** (`SkywatchWorldData.residentsClaimed`).

| Wer | Gebiet | Gefunden bei | Anwerbung | Beruf | Quest | Kommt von selbst in die Siedlung? |
|---|---|---|---|---|---|---|
| **Sky Warden** | Skyreach | Alte Wächterspitze (beim ersten Aufstieg gestempelt) | 30 000 | — | die ganze Warden's-Call-Kette **+ alle 5 Region-Keys** | nein, wird angeworben |
| **Magpie** | Skyreach | an einer verlassenen Skywatch-Werkstatt | 12 000 | Handelsmissionen | — | nein |
| **Halda** | Skyreach | an einer verlassenen Skywatch-Werkstatt | 9 000 | **Angeln** | — | nein |
| **Ossian Vane** | Skyreach | an einer verlassenen Skywatch-Werkstatt | 18 000 | — | — | nein |
| **Eveleen** | Eden | bei einem Baum der Erkenntnis | 7 000 → **frei** | **Düngen** | `swh_edenreach`, `swh_edenplants` | ja, wenn Eden-Gras in der Siedlung wächst |
| **Ives** ⭐ | **Steinfeld** | bei einem **zerbrochenen Engel** | 11 000 → **frei** | — | `swh_steinfeldvigil` | nein |
| **Mortimer** | Ghost Realm | bei einem Grabstein | 8 000 → **frei** | **Jagd** | `swh_mortimerrites` ⭐ | ja, bei einem Friedhof in der Siedlung |
| **Caspern** | Ghost Realm | bei einem Grabstein | 14 000 → **frei** | — | `swh_caspernforge` ⭐ | ja, bei einer Ätherschmiede in der Siedlung |
| **Eleanor** | Ghost Realm | bei einem Grabstein | 5 000 (nur Ende STAY) | **Tierhaltung** | `swh_eleanor` | nein — sie ist ein Ende, kein Besuch |
| **Mr. Knott** | Crooked Beyond | am Türhof | 22 000 | Handelsmissionen | `swh_crookedarrival`, `swh_crookeddoor` | nein |
| **Ghost Guide** | Ghost Realm | am Séance-Kreis **beschworen** | nicht anwerbbar | — | — | — |
| **Siggi + Peanut** | Skyreach | Lairs im Sturmschleier / in den Aurorabänken | nicht anwerbbar | — | Ziel von `swh_cats` | ziehen in den Katzenkorb |

⭐ = neu in diesem Durchgang.

**„→ frei"** heißt: die Quest dieser Person erlässt ihre Anwerbegebühr. Über
`getRecruitItems`, also Vanillas eigene Anwerbeseite — eine leere Liste *ist*
eine kostenlose Anwerbung. Wer vorher voll bezahlt hat, kann die Quest trotzdem
noch abschließen und bekommt die Barren.

**Findbarkeit, ehrlich.** Magpie, Halda und Ossian brauchen eine **vom Spieler
gebaute** Werkstatt, die schon innerhalb von 3 Kacheln steht, während eine
Region zum **ersten Mal** generiert — also das Gegenteil davon, wie irgendwer
baut. In der Praxis sind sie kaum zu finden. Die anderen sieben hängen an
natürlich gemalten Landmarken (Baum der Erkenntnis, Grabstein, zerbrochener
Engel, rote Tür) und sind in Ordnung.

---

## 3. Sortimente — was jeder Händler verkauft und kauft

Preise bewegen sich mit der Zufriedenheit des Siedlers
(`setStaticPriceBasedOnHappiness(min, max, Schritt)`), deshalb stehen hier die
Waren, nicht die Zahlen.

### Sky Warden — Skyreach
| verkauft | |
|---|---|
| Silberglöckchen ×2 @ 5 000 | erst nachdem er eingezogen ist; der einzige Schlüssel zum Séance-Kreis |
| Geisterkreide ×3 | Nachschub, nachdem er dir das erste Stück geschenkt hat |

Er **schenkt** außerdem: 1× Silberglöckchen (bei der Anwerbung) und 1×
Geisterkreide (beim ersten Mal, dass *du* im Nebel gestanden hast — pro
Charakter, nie geteilt).

### Magpie — Skyreach · der Aufkäufer
| verkauft | kauft |
|---|---|
| Wurmköder, Sandstein, Kokosnuss, Schneeball, Glas | **Himmelsstein, Windseide, Aetheriumerz, Sturmsplitter, Aurorablatt, Fulgurit, Prismensplitter** |
| Silberglöckchen ×1 (Ersatz) | |

Er zahlt über Broker-Kurs für Himmelsbergung — dafür ist er da.

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

### Ghost Guide — beschworen, handelt nicht mit Münzen
| verkauft | kauft |
|---|---|
| **Geisterstahl-Schnitter**, **Grabwind-Bogen** — die Geisterwaffen | Ektoplasma, Schleier-Essenz, Geisterstahlbarren |
| Geisterstahlbarren, Schleier-Essenz | |

Er nimmt **kein Geld**: Wertsachen aus der Geisterregion oder hochwertiges
selbstgekochtes Essen. Beim **ersten** Gebrauch des Kreises schaltet er dich
frei — danach wirkt die Soul-Exposure des Ghost-Bands für diesen Charakter
nicht mehr. Dieselben Geisterwaffen fallen auch zufällig in der Region, damit
ein Spieler, der nie handelt, sie trotzdem findet.

---

## 4. Alle Quests — 18 registriert, 17 aktiv

### Skyreach — „The Warden's Call"

| ID | Geber | Aufgabe | Belohnung |
|---|---|---|---|
| `swh_findspire` | erster Aufstieg | finde die Alte Wächterspitze | Karten-Pin |
| `swh_recruitwarden` | Warden | 30 000 zahlen | der Warden zieht ein + Silberglöckchen |
| `swh_cats` | Warden | beide Katzen mit Wolkenzupf-Leckerli heimlocken | **Katzenkorb**, 2× Flackerlicht-Girlande, 10× Sturmstahlbarren |
| `swh_anchor` | Warden | 20× Aetheriumbarren, 80× Himmelsstein, 8× Sturmstahlbarren | **Himmelswacht-Banner**, 5× Aurorablatt, **Sturmstahl-Armschiene** |
| `swh_beacon` | *niemand* | — | **TOT** — registriert, wird nie vergeben; existiert nur, damit Spielstände vor 0.5 laden |

### Die fünf Region-Keys — Warden, erst wenn die Kette DONE ist

Einer nach dem anderen, in Boss-Reihenfolge. Jeder verlangt **zwei Materialien,
die es nur in diesem Realm gibt**, und zahlt das Schlüsselstück plus Barren.
Stell das Schlüsselstück **in eine Siedlung** — dann wachen die
Beschwörungssteine dieses Realms auf.

| ID | Aufgabe | Belohnung |
|---|---|---|
| `swh_keyskyreach` | 10× Sturmsplitter, 5× Fulgurit | Skyreach-Wachfeuer + **6×** Sturmstahlbarren |
| `swh_keyeden` | 8× Edensaft, 6× Goldener Pollen | Eden-Schlüsselstück + **8×** Sturmstahlbarren |
| `swh_keysteinfeld` | 8× Echosplitter, 20× Blasser Stein | Steinfeld-Schlüsselstück + **10×** Sturmstahlbarren |
| `swh_keyghostrealm` | 12× Knochenholz, 8× Spektralerz | Ghost-Schlüsselstück + **10×** Geisterstahlbarren |
| `swh_keycrookedbeyond` | 16× Seltsamholz, 8× Realitätssplitter | Crooked-Schlüsselstück + **12×** Geisterstahlbarren |

Die Kurve 6 – 8 – 10 Sturmstahl, dann 10 – 12 Geisterstahl liegt genau auf der
monotonen Boss-Leiter darunter.

### Eden

| ID | Geber | Aufgabe | Belohnung |
|---|---|---|---|
| `swh_edenreach` | Eden-Tor | finde Eveleen (Baum der Erkenntnis) | Wegweiser |
| `swh_edenplants` | Eveleen | 1× Edenbeere, 1× Mondmelone, 1× Sonnentraube | 3× Wissenssteckling, 10× Sturmstahlbarren, **ihre Gebühr entfällt** |

### Steinfeld ⭐ **NEU**

| ID | Geber | Aufgabe | Belohnung |
|---|---|---|---|
| `swh_steinfeldvigil` | **Ives** | 14× Grabsalz, 10× Geistermoos | **seine 11 000 entfallen** + 10× Sturmstahlbarren |

*Die Totenwache.* Beide Materialien gibt es nur in Steinfeld — und beide hatte
vorher **überhaupt keinen Abnehmer**: kein Rezept im Mod nennt sie, und der
Region-Key nimmt die anderen zwei (Echosplitter, Blasser Stein).

### Ghost Realm

| ID | Geber | Aufgabe | Belohnung |
|---|---|---|---|
| `swh_eleanor` | Eleanor | **PASS ON:** 12× Schleier-Essenz in der Hand halten · **STAY:** ohne Essenz reden und anwerben | PASS ON: **Irrlichtlaterne** + 14× Geisterstahlbarren, *sie ist dauerhaft weg* · STAY: 14× Geisterstahlbarren, sie zieht ein |
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
| `swh_crookeddoor` | Mr. Knott | 5× Realitätssplitter, 8× Krümmungsharz, 8× Seltsamer Stoff | **Zephyr-Gurtzeug**, 12× Sturmstahlbarren, 6× Realitätssplitter |

---

## 5. Die Bosse — fünf, plus eine reservierte Sprosse

Beschworen an **Beschwörungssteinen**, die verstreut im eigenen Band stehen
(~0,97 pro 1000×1000 Kacheln). Nicht abbaubar. **Inert**, bis das
Schlüsselstück des Realms in einer Siedlung steht.

Skalierung ist Vanillas eigene Incursion-Kurve, angewandt per Mob über einen
permanenten Buff — nie über `LevelModifiers`, das würde die ganze Ebene buffen.

| Realm | Boss | aus welcher Incursion | Basis-HP | Stufe | **finale HP** |
|---|---|---|---|---|---|
| Skyreach | `cryoqueen` — Cryo-Königin | Snow Deep Cave | 18 000 | 8 (×3,18) | **57 240** |
| Eden | `moonlightdancer` — Mondlichttänzerin | Moon Arena | 40 000 | 8 (×3,18) | **127 200** |
| Steinfeld | `ascendedwizard` — Aufgestiegener Magier | Settlement Ruins | 44 000 | 9 (×3,58) | **157 520** |
| Ghost Realm | `pestwarden` — Pestwächter | Swamp Deep Cave | 45 000 | 9 (×3,58) | **161 100** |
| Crooked Beyond | `crystaldragon` — Kristalldrache | Crystal Hollow | 52 000 | 10 (×4,00) | **208 000** |
| Hell | `mutanthydra` reserviert | Scrapyard | 80 000 | — | **nicht gebaut** |

Die Leiter ist absichtlich monoton: nach außen laufen heißt nach oben laufen.
Dein Wunsch war *„grundsätzlich sollen die Bosse auf Incursion Level 8–10
sein"* — genau da liegen sie.

---

## 6. Die Gebiete, kompakt

| Gebiet | Kacheln vom Ursprung | Biome | Feinde | Critter | Tiere | NPCs | Quests | POIs | bewachte Orte / 1000×1000 |
|---|---|---|---|---|---|---|---|---|---|
| **Skyreach** | 0 – 1 800 | 4 | 8 | 4 | 2 | 7 | 5 | 4 | 28,7 |
| **Eden** | 600 – 2 880 | 3 | 5 | 0 | 0 | 1 | 3 | 2 | 11,4 ⭐ *(vorher 0)* |
| **Steinfeld** | 1 920 – 4 200 | 3 | 4 | 0 | 0 | 1 ⭐ | 2 ⭐ | 1 | 20,3 |
| **Ghost Realm** | 2 880 – 5 280 | 5 | 9 | 0 | 0 | 4 | 4 ⭐ | 1 | 21,8 |
| **Crooked Beyond** | 4 200 – 5 640 | 5 | 8 | 1 | 0 | 1 | 3 | 1 | 30,9 |
| **Hell** | 4 800+ | 0 | 0 | 0 | 0 | 0 | 0 | 4 unerreichbar | 0 |

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
| `kk-sprites/` | 16 | Generator-Zwischenstufen |
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

**20 literale Pfade** zeigen auf Vanilla-Dateien statt auf unsere:

| Pfad | wofür |
|---|---|
| `mobs/icons/farmerhuman` | Eveleens Gesicht |
| `mobs/icons/pawnbrokerhuman` | Mortimers Gesicht |
| `mobs/icons/blacksmithhuman` | Casperns Gesicht |
| `mobs/icons/stylisthuman` | Eleanors Gesicht |
| `mobs/icons/exoticmerchanthuman` | Mr. Knotts Gesicht |
| `mobs/icons/elderhuman` ⭐ | **Ives' Gesicht** |
| `mobs/bee` · `mobs/cow` · `mobs/crocodile` · `mobs/dragonwhelp` · `mobs/dryadsentinel` · `mobs/scorpion` · `mobs/stabbybush` | Kreaturen, die eine Vanilla-Klasse beerben |
| `tiles/cryptash_splat` · `ravenfloor_splat` · `stonebrickfloor_splat` · `swampgrass_splat` · `swamprock_splat` | Autotile-Atlanten für Böden ohne eigene Kunst |

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

### Was an Kunst offen ist: 12 Bestiarium-Icons

Edens fünf und die sieben des Ghost Realms sind mit `countKillStat = false`
registriert, während Skyreach, Steinfeld und Crooked mit `true` registrieren.
Diese zwölf haben **keine Bestiarium-Zeile und zählen keine Kills** — du kannst
Eden leerräumen und das Spiel gibt nicht zu, dass du dort warst.

Das Flag umzulegen ist **kein Einzeiler**: der Drei-Argument-`registerMob`
reicht `countKillStat` auch als `createSpawnItem` durch, und
`MobRegistry.loadMobIcons` lädt `mobs/icons/<id>` für **jeden** Mob — ohne
Datei bekommst du zwölf Zeilen mit der ERR-Textur der Engine. Die zwölf Icons
à 32×32 sind der echte Preis; sie stehen mit Begründung in
`docs/ASSET_REQUESTS.md`.

---

## 8. Bestehende Spielstände: `/swhreset`

Alles ADMIN, alles serverseitig. Vollständig in `docs/SAVE_COMPAT.md`.

| Befehl | was er tut |
|---|---|
| `/swhreset` | **meldet nur.** Story-Stufe, Region-Keys, Portal-Freischaltungen, Bewohner-Ansprüche, Nebel- und Kreide-Register, wer gerade im Himmel steht, wie viele Portale in der Nähe stehen. Ändert nichts. |
| `/swhreset world` | trägt fehlende Inhalte in **bereits erzeugtes Gelände** nach: Boss-Portale, Wachtrupps, Bewohner, Herden. 1024×1024 um dich herum. Zweimal laufen lassen ist sicher. |
| `/swhreset quests confirm` | setzt die ganze Kette auf **vor den ersten Aufstieg** zurück |
| `/swhreset all confirm` | beides + löscht die Ein-pro-Welt-Ansprüche *(Warnung in der Doku lesen)* |

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

1. **Hell ist ein Loch mit vier Gebäuden drin.** Das Band 0,80–1,00 malt sich
   als Crooked; die vier Hell-POI-Presets stehen in krummem Boden. Kein Biom,
   keine Besetzung, kein NPC, keine Quest, keine Boss-Sprosse.
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
