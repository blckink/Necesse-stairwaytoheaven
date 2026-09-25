# Komplettübersicht — Trigger, Quests, NPCs, Dialoge, Items

**Für den Spieler zum Überarbeiten.** Stand 2026-09-24, Commit `96aa406`.
**Alles hier ist aus dem Quellcode gelesen** (`src/main/java/stairwaytoheaven/**`,
`src/main/resources/locale/de.lang`), nicht aus älteren Docs. Wo ein Doc etwas
anderes sagt, gilt der Code — die Abweichung steht in §8.

Verifikationsstand (nach `docs/IMPLEMENTATION_RULES.md` §14): **alles in dieser
Datei ist aus dem Quelltext gelesen, nichts davon in dieser Sitzung im Spiel oder
auf einem Server ausgeführt.** Wo ich eine Folgerung ziehe, die der Code allein
nicht beweist (z. B. was ein Vanilla-Objekt fallen lässt), steht **HYPOTHESE**.
Pfade sind relativ zu `src/main/java/stairwaytoheaven/`. Deutsche Texte stehen
wörtlich mit ihrem Schlüssel `abschnitt.schlüssel` aus `de.lang`.

Inhalt:
1. Die komplette Reihe (Ablauf)
2. Alle Quests
3. Alle NPCs / Bewohner / Siedler
4. Alle Dialoge
5. Alle Items
6. Bosse, Rufsteine, Tore, Gegner
7. Trigger- und Flag-Referenz
8. Gefundene Unstimmigkeiten

---

## 0. Die Welt in einem Satz

Es gibt **eine** Mod-Ebene (`skylevel`, Kennung `skyreach2`). Alle Reiche sind
**Entfernungsringe** um die Alte Wächterspitze (`worldgen/RealmDepth.java:83-91`,
`DEPTH_SCALE = 6000` Kacheln). Die Ringe überlappen, dazwischen wird per Rauschen
gemischt.

| Reich | Tiefe | Kacheln vom Ursprung | Biome (DE) |
|---|---|---|---|
| Himmelsweite (Skyreach) | 0,00–0,30 | 0–1 800 | Driftlande, Sturmschleier, Himmelspfade, Aurorabänke |
| Garten Eden | 0,10–0,48 | 600–2 880 | Garten Eden, Eden-Baumkronen, Eden-Ufer |
| Steinfeld / „Die Stille Weite" | 0,32–0,70 | 1 920–4 200 | Stille Wiese, Plattenfelder, Grabheide |
| Geisterreich / „Der Nachgarten" | 0,48–0,88 | 2 880–5 280 | Nachgarten, Knochenhain, Ektoplasmasumpf, Düsterfenn, Aschenöde |
| Krummes Jenseits / „Jenseits der Krümmung" | 0,70–0,94 | 4 200–5 640 | Gestreifte Öde, Spiralfelder, Schachbrettwerke, Käfergrund, Käferlande |
| Hölle | 0,80–1,00 | 4 800+ | Höllensaum, Ofenweite |

**Die einzige harte Sperre** ist die **Nebelwand** (Seelenblöße) bei Tiefe
**0,581 ≈ 3 486 Kacheln** (`veil/VeilRegion.java` `deriveVeilDepth()`: erste Tiefe,
an der Geisterreich > 0 und Steinfeld < 1). Alles davor — Himmelsweite, Eden,
Steinfeld und der äußere Rand des Geisterreichs — ist frei zu Fuß erreichbar.

---

## 1. Die komplette Reihe (Ablauf)

Hauptstrang (Warden-Kette) nummeriert; Nebenstränge mit Buchstaben dort, wo sie
im Spiel frühestens auftauchen. Alle Welt-Flags sind in
`quest/SkywatchQuestData` (Q, LevelData auf der Himmelsweite),
`quest/SkywatchWorldData` (W, WorldData) oder `veil/VeilWorldData` (V, WorldData,
pro Charakter).

### 1.1 Oberfläche — vor dem Aufstieg

| # | Auslöser | Wer/Wo | Was der Spieler tut | Belohnung | Schaltet frei | Quest | Flags | Code |
|---|---|---|---|---|---|---|---|---|
| 0a | Weltgenerierung Oberfläche | Oberflächen-Fundorte: **Himmelsschrein** (Schild `misc.swhshrinesign`), **Aeronautenlager** (Schild `misc.swhaeronautsign`), **Kraterfragment** | hinlaufen, plündern | Himmelsstein, Aetheriumerz, Windseide, Münzen (Kisten) | — (reine Vorschau auf das Material) | — | — | `surface/SkySurfacePresets.java:90-95` (0,35 % pro Region; Tickets Krater 120 / Lager 100 / Schrein 70) |
| 0b | Weltereignis alle **4–9 Tage** | ganze Oberfläche | 2 Minuten lang fallen bis zu 48 „Gefallene Himmelsscherben"; abbauen | je Scherbe 1–3 Himmelsstein, 18 % Sturmsplitter, 10 % Aurorablatt | — | — | `SkyfallWorldData` (nächster Termin) | `surface/SkyfallWorldData.java:37-38`, `SkyfallWorldEvent.java:62-69`; Ansagen `misc.swhskyfallstart` „Der Himmel bricht auf - Scherben fallen." / `misc.swhskyfallend` „Der Sternenfall ist vorüber." |
| 0c | Rezept | Wolfram-Werkbank | **Stairway to Heaven** herstellen: 8× Wolframbarren + 15× Quarz, dann an der Oberfläche platzieren | — | den Aufstieg | — | — | `SkyItems.java:518` |

### 1.2 Himmelsweite — die Warden-Kette

| # | Auslöser (exakt) | Wer/Wo | Was der Spieler tut | Belohnung | Schaltet frei | Quest | Flags gesetzt | Code |
|---|---|---|---|---|---|---|---|---|
| 1 | Treppe benutzen: `SkywardStairwayObjectEntity.use()` | Oberfläche → Ankunftsfeld Himmelsweite | Treppe anklicken | Karten-Pins „Wardens Turm" (`misc.spiremarker`) und „Deine Treppe" (`misc.stairsmarker`) | Spitze wird gestempelt (`SkyLevel.ensureWardenSpire`, spawnt auch die Katzen und stempelt die drei Landmarken), Rückweg über das **Skywatch-Tor** an der Spitze | **`swh_findspire`** (nur wenn `Q.stage == 0`) | `Q.returnStairs[auth]` = diese Treppe; `Q.spirePlaced`, `Q.catsSpawned`, `Q.landmarksStamped` | `objects/SkywardStairwayObjectEntity.java:48-102` (Quest bei `:87-97`) |
| 2 | Erstes Gespräch mit dem Himmelswächter **in der Himmelsweite**, solange er kein Siedler ist und `Q.stage == 0` | Alte Wächterspitze | ansprechen | **6× Windseide** | — | `swh_findspire` wird entfernt; direkt danach **`swh_recruitwarden`** | `Q.stage = 1` | `mobs/SkyWardenMob.java:280-311` (Intro), `:721-727` (Recruit-Quest) |
| 3 | Anwerben über das Vanilla-Anwerbefenster (`getRecruitItems` = **30 000 Münzen**), braucht eine eigene Siedlung | Wächterspitze | 30 000 Münzen zahlen, Siedlung wählen | Der Wächter zieht ein (wird zu `wardensettler`, unsterblich); das Leuchtfeuer an der Spitze wird entzündet | alle Bewohner-Anreisen, die den Wächter voraussetzen (Eveleen, Mortimer, Caspern); Magpies Himmelsfahrten | `swh_recruitwarden` entfernt; **`swh_cats`** vergeben + Karten-Pins „Siggis Lager"/„Peanuts Lager" | `W.wardenRecruited`, `W.wardenAuth`, `Q.recruited`, `Q.recruitedAuth`, `Q.stage = 2` | `mobs/SkyWardenMob.java:176-178` (Preis), `:829-873` (`onRecruited`) |
| 4 | Eine Katze mit **Wolkenzupf-Leckerli im Inventar** ansprechen (in der Himmelsweite) | Siggi (Lager im Sturmschleier), Peanut (Lager in den Aurorabänken) | 1 Leckerli wird verbraucht | Katze „puff" → in den Korb (Spitze oder eigener Katzenkorb) | Streicheln in der Siedlung: 1 Spieltag Buff (Siggi: +8 % Krit, +5 % Schaden, +4 Rüstung; Peanut: +12 % Tempo, +0,5 Regen, +10 % Abbau) | Fortschritt in `swh_cats` | `Q.blackHome`/`Q.tabbyHome` und gespiegelt `W.blackHome`/`W.tabbyHome` | `mobs/SpireCatMob.java:158-248` (**funktioniert auch ohne Quest**) |
| 5 | Wächter ansprechen, wenn beide Katzen daheim | überall, wo der Wächter steht (Siedlung) | ansprechen | **Katzenkorb, 2× Flackerlicht-Girlande, 10× Sturmstahlbarren** | im Wächter-Laden: Katzenkorb (500) und Girlande (500) | `swh_cats` abgeschlossen; **sofort `swh_anchor`** | `Q.catsRewardGiven = true` | `SkyWardenMob.java:741-768` |
| 6 | Wächter ansprechen mit **20× Aetheriumbarren, 80× Himmelsstein, 8× Sturmstahlbarren** | Wächter | abgeben | **Wolkengleve, Himmelswacht-Banner, 5× Aurorablatt** | im Wächter-Laden: Wolkengleve (4 500), Banner (800) | `swh_anchor` abgeschlossen | `Q.anchorDone = true`; `W.residentChainsDone += "anchorcloudglaive"` | `SkyWardenMob.java:770-785`, `:817-820`. Alte Saves mit Anker aber ohne Gleve bekommen sie beim nächsten Gespräch (`:787-791`, Text `misc.wardenglaivecatchup`) |
| 7–12 | Wächter ansprechen, wenn Kapitel `DONE` (rekrutiert + beide Katzen + Katzenbelohnung + Anker) | Wächter | pro Gespräch **eine** Aktion: entweder eine fällige Abgabe ODER die nächste Frage | siehe Tabelle 1.3 | Schlüsselstück → in Siedlung stellen → Rufsteine des Reichs wachen auf | `swh_keyskyreach` → `swh_keyeden` → `swh_keysteinfeld` → `swh_keyghostrealm` → `swh_keycrookedbeyond` → `swh_keyhell` (streng in dieser Reihenfolge) | `W.regionKeysEarned += realm` | `SkyWardenMob.java:610-687` (`advanceRegionKeys`), Reihenfolge `:465-605` |
| 13 | Schlüsselstück **innerhalb einer Siedlung** platzieren: `RegionKeyObject.placeObject()` | eigene Siedlung | platzieren (außerhalb: Fehlermeldung `misc.regionkeyneedsettlement`) | Meldung `misc.regionkeyunlocked` | Rufsteine dieses Reichs → Boss beschwörbar; Magpies Himmelsfahrt in dieses Reich wird sicherer | — | `W.bossPortalsUnlocked += realm` | `objects/RegionKeyObject.java:317-333` |

Jederzeit parallel (jedes Gespräch mit dem Wächter): **Geisterkreide-Geschenk**,
siehe 1.5 Schritt F.

### 1.3 Die sechs Region-Schlüssel (Warden, nach Kapitel DONE)

| Reihenfolge | Quest-ID | Titel (DE) | Abgabe | Belohnung (Code) | Boss danach |
|---|---|---|---|---|---|
| 1 | `swh_keyskyreach` | „Das Wachfeuer" | 10× Sturmsplitter, 5× Fulgurit | Wachfeuer der Himmelsweite, 4× Sturmstahlbarren, **4× Sturmscheibe**, Himmelswacht-Kapuze | Cryo-Königin |
| 2 | `swh_keyeden` | „Die Gartenstiege" | 8× Edensaft, 6× Goldener Pollen | Gartenstiege von Eden, 5× Sturmstahlbarren, Windheuler, Mantel des Hüters | Mondlichttänzerin |
| 3 | `swh_keysteinfeld` | „Der Trauerengel" | 8× Echosplitter, 20× Blasser Stein | Trauerengel von Steinfeld, 6× Sturmstahlbarren, Sturmklinge, Stiefel des Hüters | Aufgestiegener Magier |
| 4 | `swh_keyghostrealm` | „Die Rabenkanzel" | 12× Knochenholz, 8× Spektralerz | Rabenkanzel des Nachgartens, 6× Geisterstahlbarren, Geisterstahl-Schnitter, Sturmstahl-Armschiene | Pestwächter |
| 5 | `swh_keycrookedbeyond` | „Eine eigene Tür" | 16× Seltsamholz, 8× Realitätssplitter | Knotts Krumme Tür, 8× Geisterstahlbarren, Grabwind-Bogen, Auroramedaillon | Kristalldrache |
| 6 | `swh_keyhell` | „Alles scheint in Ordnung zu sein" | 16× Realitätssplitter, 24× Seltsamholz | Das Höllensiegel, 16× Geisterstahlbarren | Mutantenhydra |

Code: Materialien in `quest/*KeyQuest.java` (`super(new ItemObjective(...))`),
Belohnungen in `mobs/SkyWardenMob.java:465-605`.

### 1.4 Himmelsweite — die drei Landmarken (parallel, ab dem ersten Aufstieg)

Einmal pro Welt, beim ersten Aufstieg gestempelt (`worldgen/pois/SkyLandmarkPois.java`).

| Ort | wo | Wächter (Gegner) | Bewohner | Schlüsselgegenstand für die Anwerbung | Weitere Beute im Ort |
|---|---|---|---|---|---|
| **Skyway-Zollhaus** | 180–700 Kacheln, Biom Himmelspfade | **Zollwerk** (`tollwright`) — Drops: 1–2 Aetheriumbarren, 1–3 Sturmstahlbarren, 3–6 Himmelsstein | **Magpie** („Elster") | **Verzollte Kassette** (liegt in der Tresor-Vitrine, Kachel 16,11 — NICHT beim Wächter) | Himmelsweg-Freibrief (Vitrine 16,6), Buch der unzustellbaren Post (Schrank 19,6) |
| **Grange-Keller** | 400–1 100, Driftlande | **Sauerbottich-Blüte** (`sourvatbloom`) + **Bottichlinge** | **Halda** | **Die Mutter** — fällt bei **jedem** Tod der Blüte (+ 4–9 Wolkenbeeren, 2–4 Windseide) | Die Runde des Wächters (Fass 12,6), Siegelring der Himmelswacht (Schrank 13,10) |
| **Sturmschleier-Testgelände** | 700–1 700, Sturmschleier | **Prototyp Neun** (`prototypenine`) | **Ossian Vane** | **Sturmlinsenkern** — fällt bei **jedem** Tod (+ 1–2 Ätherwerker-Gehäuse, 2–5 Sturmglas, 1–2 Sturmstahlbarren) | 2–4 Ätherwerker-Gehäuse (Vitrine 19,6) |

Code: `SkyLandmarkPois.java:84-89` (Ringe), `:99-126` (Bewohner, Wächter),
`:160-167` (Beute). Flags: `Q.landmarksStamped`, `Q.landmarkGuards`,
`Q.landmarkLoot`, `W.residentsClaimed`.

### 1.5 Die Reiche — Nebenstränge in Reiseordnung

| # | Auslöser (exakt) | Wer/Wo | Was der Spieler tut | Belohnung | Schaltet frei | Quest | Flags | Code |
|---|---|---|---|---|---|---|---|---|
| A | (optional) **Edenschwelle** herstellen (Wolfram-Werkbank: 4 Aetheriumbarren, 10 Himmelsstein, 8 Windweizen), platzieren außerhalb Edens, mit **6 Eden-Grassamen** im Inventar anklicken | beliebig | Samen werden verbraucht, Schwelle wird zum **Edentor** | — | Schnellreise in den Eden-Ring | — | — | `realms/eden/EdenSeedBasinObject.java:80-103`, Rezept `EdenRealm.java:219` |
| B | Edentor benutzen: `EdenGateObjectEntity.use()` | Edentor → Landeplatz in Eden | durchgehen | — | — | **`swh_edenreach`** (nur wenn `W.edenPlantsGiven == false`). **Wer zu Fuß nach Eden läuft, bekommt diese Quest nie.** | — | `realms/eden/EdenGateObjectEntity.java:64-103` |
| C | Eveleen ansprechen (Gesprächsbeginn) | Eden, neben einem Baum der Erkenntnis (35 % Chance/Region + Baum in 3 Kacheln) | 1× Edenbeere, 1× Mondmelone, 1× Sonnentraube abgeben | 3× Wissenssteckling, 10× Sturmstahlbarren, **Anwerbegebühr 7 000 entfällt** | Eveleen gratis anwerbbar | `swh_edenreach` entfernt, **`swh_edenplants`** vergeben/abgeschlossen | `W.edenPlantsGiven` | `mobs/EveleenMob.java:146-196` |
| D | Ives ansprechen | Steinfeld, neben einem Zerbrochenen Engel (14 %/Region) | 14× Grabsalz, 10× Geistermoos | 12× Sturmstahlbarren, **Gebühr 11 000 entfällt** | Ives gratis | **`swh_steinfeldvigil`** | `W.residentChainsDone += "steinfeldvigil"` | `mobs/IvesMob.java:150-170`, `quest/SkyQuests.java:109-130` |
| E | Spieler steht zum ersten Mal hinter der Nebelwand (Tiefe ≥ 0,581) — geprüft jede Sekunde in `VeilWorldData.tickClient` | Nebelwand ~3 486 Kacheln | — | — | Ohne Mal: **Seelenblöße** stapelt pro Sekunde (≥1 Sicht, ≥4 Verlangsamung, ≥8 Schaden 10–35/s, ≥13 150 Schaden/s, max 16). Warnung `misc.veilexposurewarning` | — | `V.fogTouchedAuths += auth` | `veil/VeilWorldData.java:319-383`, `veil/SoulExposureBuff.java:85-165` |
| F | Wächter ansprechen, nachdem E passiert ist (egal ob rekrutiert, egal welches Kapitel) | Wächter (Spitze oder Siedlung) | ansprechen | **1× Geisterkreide** (einmal pro Charakter) | Séance-Zirkel | — | `V.chalkGivenAuths += auth` | `SkyWardenMob.java:349-363` |
| G | Geisterkreide **innerhalb einer Siedlung** platzieren (sonst `misc.seanceneedsettlement`), Zirkel anklicken | eigene Siedlung | Zirkel benutzen | Geisterführer erscheint (`misc.seanceguidearrives`) | Laden des Geisterführers | — | — | `objects/SeanceCircleObject.java:174-203` |
| H | **Erstes** Gespräch mit dem Geisterführer | am Zirkel | ansprechen | Bubble `misc.ghostguideunlock1` | **Nebelmal**: Seelenblöße wirkt nie mehr für diesen Charakter → Geisterreich, Krummes Jenseits, Hölle frei | — | `V.markAuths += auth` | `mobs/GhostGuideMob.java:201-213` |
| I | (optional) **Seelenbecken** herstellen (Wolfram-Werkbank: 12 Ektoplasma, 6 Schleier-Essenz, 8 Knochen), außerhalb des Geisterreichs platzieren, mit **12 Ektoplasma** anklicken | beliebig | Ektoplasma wird verbraucht | wird zum **Geistertor** | Schnellreise in den Geister-Ring | — | — | `realms/ghost/SoulBasinObject.java:72-95` |
| J | Mortimer ansprechen | Geisterreich, neben einem Geistergrabstein (14 %/Region, zufällig einer der drei) | 12× Seelenfaden, 10× Knochenholz | 6× Geisterstahlbarren, **Gebühr 8 000 entfällt** | Mortimer gratis | **`swh_mortimerrites`** | `W.residentChainsDone += "mortimerrites"` | `mobs/MortimerMob.java:113-135` |
| K | Caspern ansprechen | Geisterreich, neben einem Geistergrabstein | 12× Spektralerz, 8× Schleier-Essenz | 6× Geisterstahlbarren, **Gebühr 14 000 entfällt** | Caspern gratis | **`swh_caspernforge`** | `W.residentChainsDone += "caspernforge"` | `mobs/CaspernMob.java:93-115` |
| L | Eleanor ansprechen (nicht als Siedlerin/Besucherin) | Geisterreich, neben einem Geistergrabstein | **LOSLASSEN:** mit Schleier-Essenz **in der Hand** ansprechen, ≥ 12 im Inventar · **BLEIBEN:** normal anwerben (5 000) | Loslassen: Irrlichtlaterne (vanilla `willowisplantern`) + 10× Geisterstahlbarren, Eleanor verschwindet für immer · Bleiben: 10× Geisterstahlbarren beim Einzug | Tierhaltung (nur bei Bleiben) | **`swh_eleanor`** (bei erstem Gespräch) | Loslassen: `W.eleanorPassedOn` | `mobs/EleanorMob.java:144-232` |
| M | Krumme Tür benutzen: `CrookedDoorObjectEntity.use()` | — | — | — | — | **`swh_crookedarrival`** — **TOT**: nichts platziert die Krumme Tür (§8) | — | `realms/crooked/CrookedDoorObjectEntity.java:57-98` |
| N | Mr. Knott ansprechen | Krummes Jenseits (und Höllen-Ring), innerhalb 9 Kacheln eines Türhofs (20 %/Region) | 5× Realitätssplitter, 8× Krümmungsharz, 8× Seltsamer Stoff | Zephyr-Gurtzeug, 12× Geisterstahlbarren, 6× Realitätssplitter (**Gebühr 22 000 bleibt**) | — | `swh_crookedarrival` entfernt, **`swh_crookeddoor`** | `W.crookedDoorwayOpened` | `mobs/KnottMob.java:115-147`; Platzierung `settlement/CrookedResidents.java` |
| O | Rufstein im Reich benutzen, nachdem dessen Schlüsselstück steht | Rufsteine (0,35 Chance pro 600×600-Zelle, nur im eigenen Reich) | anklicken | Boss erscheint (Meldung `misc.bossportalawoke`) | — | — | liest `W.bossPortalsUnlocked` | `bosses/BossPortalObjectEntity.java:107-135`, Platzierung `level/SkyLevel.java:446-535` |

### 1.6 In der eigenen Siedlung (unabhängig vom Himmel)

| Auslöser | Wer | Bedingung (Code) | Code |
|---|---|---|---|
| Siedlungs-Anreise (Vanilla-Ticketsystem) | **Eveleen** | Wächter rekrutiert **und** ≥ 9 Kacheln Eden-Gras in der Siedlung, 90 Tickets | `settlement/SkySettlers.java:134`, `SkyArrivals.java` `EDEN_PATCH` |
| dito | **Mortimer** | Wächter **und** ≥ 3 Grabsteine (`gravestone1/2`, `cryptgravestone1/2`, `sarcophagus`) | `SkyArrivals.GRAVEYARD` |
| dito | **Caspern** | Wächter **und** eine Ätherschmiede | `SkyArrivals.FORGE` |
| dito | **Dorian** (Vampir) | **kein Wächter nötig**; ein Sarg (`sarcophagus`, `twilightsarcophagus`, `coffinbed`) | `SkySettlers.java:187`, `SkyArrivals.COFFIN` |
| dito | **Therapeut**, **Arzt**, **Kriegsveteran** | immer, 75 Tickets, wie ein Schmied | `settlement/ProfessionSettler.java:96` |
| Besucher | **Zwielichtiger Händler** | immer, 60 Tickets | `TwilightWares.java:212-225` |
| Vanilla-Stylistin | verkauft zusätzlich 6 Friseur-Möbel | — | `mobs/SalonStylistMob.java`, `SalonWares.java:270-278` |
| Himmelsfahrten | **Magpie** als Siedlerin | Wächter rekrutiert; Erfolg hängt am tiefsten freigeschalteten Rufstein | `settlement/SkyVoyages.java:262-297` |

---

## 2. Alle Quests

20 registriert (`StairwayToHeavenMod.java:144-176`), davon 18 erreichbar,
2 tot (`swh_beacon`, `swh_crookedarrival`).

| ID | Name (DE, `quests.*title`) | Geber | Auslöser | Aufgabe | Belohnung (Code) | Folge | Wiederholbar? | Flags | Code |
|---|---|---|---|---|---|---|---|---|---|
| `swh_findspire` | Der Ruf des Wardens | Treppe | 1. Aufstieg bei `stage==0` | Wächter finden | nur Karten-Pin | `swh_recruitwarden` | nein | `Q.stage` | `quest/FindSpireQuest.java` |
| `swh_recruitwarden` | Der letzte Hüter | Wächter | 1. Gespräch | 30 000 Münzen zahlen | Wächter zieht ein, Leuchtfeuer | `swh_cats` | nein | `Q.recruited`, `W.wardenRecruited` | `quest/RecruitWardenQuest.java` |
| `swh_beacon` | Entzünde das Leuchtfeuer | **niemand** | — | 12× Sturmsplitter, 8× Windseide | Text: 2× Flickerlicht-Girlande | — | — | — | **TOT** `quest/BeaconDeliveryQuest.java` |
| `swh_cats` | Die Turmkatzen | Wächter | Anwerbung / Gespräch in Kapitel CATS | beide Katzen mit Leckerli heimlocken | Katzenkorb, 2× Flackerlicht-Girlande, 10× Sturmstahlbarren | `swh_anchor` | nein | `Q/W.blackHome`, `.tabbyHome`, `Q.catsRewardGiven` | `quest/SpireCatsQuest.java` |
| `swh_anchor` | Verankere die Insel | Wächter | nach Katzen-Abgabe | 20× Aetheriumbarren, 80× Himmelsstein, 8× Sturmstahlbarren | Wolkengleve, Himmelswacht-Banner, 5× Aurorablatt | Region-Schlüssel | nein | `Q.anchorDone`, `W…"anchorcloudglaive"` | `quest/AnchorDeliveryQuest.java` |
| `swh_keyskyreach` | Das Wachfeuer | Wächter | Kapitel DONE | 10× Sturmsplitter, 5× Fulgurit | Wachfeuer, 4× Sturmstahl, 4× Sturmscheibe, Himmelswacht-Kapuze | `swh_keyeden` | nein | `W.regionKeysEarned` | `quest/SkyreachKeyQuest.java` |
| `swh_keyeden` | Die Gartenstiege | Wächter | nach Schlüssel 1 | 8× Edensaft, 6× Goldener Pollen | Gartenstiege, 5× Sturmstahl, Windheuler, Mantel des Hüters | `swh_keysteinfeld` | nein | dito | `quest/EdenKeyQuest.java` |
| `swh_keysteinfeld` | Der Trauerengel | Wächter | nach 2 | 8× Echosplitter, 20× Blasser Stein | Trauerengel, 6× Sturmstahl, Sturmklinge, Stiefel des Hüters | `swh_keyghostrealm` | nein | dito | `quest/SteinfeldKeyQuest.java` |
| `swh_keyghostrealm` | Die Rabenkanzel | Wächter | nach 3 | 12× Knochenholz, 8× Spektralerz | Rabenkanzel, 6× Geisterstahl, Geisterstahl-Schnitter, Sturmstahl-Armschiene | `swh_keycrookedbeyond` | nein | dito | `quest/GhostKeyQuest.java` |
| `swh_keycrookedbeyond` | Eine eigene Tür | Wächter | nach 4 | 16× Seltsamholz, 8× Realitätssplitter | Knotts Krumme Tür, 8× Geisterstahl, Grabwind-Bogen, Auroramedaillon | `swh_keyhell` | nein | dito | `quest/CrookedKeyQuest.java` |
| `swh_keyhell` | Alles scheint in Ordnung zu sein | Wächter | nach 5 | 16× Realitätssplitter, 24× Seltsamholz | Das Höllensiegel, 16× Geisterstahl | — | nein | dito | `quest/HellKeyQuest.java` |
| `swh_edenreach` | Hinein in den Garten | Edentor | erste Benutzung, solange Eden-Kette offen | Eveleen finden | — | `swh_edenplants` | nein | — | `quest/EdenArrivalQuest.java` |
| `swh_edenplants` | Ein Geschmack von Eden | Eveleen | Gespräch | 1× Edenbeere, 1× Mondmelone, 1× Sonnentraube | 3× Wissenssteckling, 10× Sturmstahl, Gebühr entfällt | — | nein | `W.edenPlantsGiven` | `quest/EdenPlantsQuest.java` |
| `swh_steinfeldvigil` | Die Totenwache | Ives | Gespräch | 14× Grabsalz, 10× Geistermoos | 12× Sturmstahl, Gebühr entfällt | — | nein | `W.residentChainsDone` | `quest/SteinfeldVigilQuest.java` |
| `swh_eleanor` | Warum sie blieb | Eleanor | Gespräch | Loslassen (12 Essenz in der Hand) oder Anwerben | s. 1.5 L | — | nein | `W.eleanorPassedOn` | `quest/EleanorQuest.java` |
| `swh_mortimerrites` | Die letzte Ehre | Mortimer | Gespräch | 12× Seelenfaden, 10× Knochenholz | 6× Geisterstahl, Gebühr entfällt | — | nein | `W.residentChainsDone` | `quest/MortimerRitesQuest.java` |
| `swh_caspernforge` | Die kalte Schmiede | Caspern | Gespräch | 12× Spektralerz, 8× Schleier-Essenz | 6× Geisterstahl, Gebühr entfällt | — | nein | `W.residentChainsDone` | `quest/CaspernForgeQuest.java` |
| `swh_crookedarrival` | Eine Tür, die irgendwohin führt | Krumme Tür | erste Benutzung | Knott finden | — | `swh_crookeddoor` | — | — | **TOT** (Tür wird nirgends platziert) |
| `swh_crookeddoor` | Überzeug die Tür | Mr. Knott | Gespräch | 5× Realitätssplitter, 8× Krümmungsharz, 8× Seltsamer Stoff | Zephyr-Gurtzeug, 12× Geisterstahl, 6× Realitätssplitter | — | nein | `W.crookedDoorwayOpened` | `quest/CrookedDoorQuest.java` |

**Alle „Zahlt einmal"-Quests sind welt-, nicht spielerbezogen.** Ein zweiter
Spieler auf derselben Welt bekommt die Belohnung nicht noch einmal; dafür
verkauft der Wächter Gleve/Banner/Korb/Girlande, sobald sie verdient sind.

Quest-Texte (DE, wörtlich):

| Quest | `…desc` | `…obj` / Belohnungszeile |
|---|---|---|
| findspire | „Etwas hält über den Wolken noch ein Licht am Brennen." | „Finde Wardens Turm - folge dem Flackern über dem Nebel." |
| recruitwarden | „Der Himmelswächter würde die Himmelswacht hinter sich lassen - für einen Preis, den keine kleine Siedlung zahlen kann." | „Heuere den Himmelswächter für deine Siedlung an (<cost> Münzen)" |
| beacon (tot) | „Das Leuchtfeuer der Himmelswacht ist zu lange dunkel gewesen. Bring dem Warden, was es zum Brennen braucht." | „2x Flickerlicht-Girlande" |
| cats | „Zwei Katzen streunen noch durch Sturm und Bänke. Ein Wolkenzupf-Leckerli überzeugt sie vom Heimweg." | Ziele `swhcatblack` „Lock die schwarze Katze heim (streunt im Sturmschleier)", `swhcattabby` „Lock die getigerte Katze heim (streunt in den Aurorabänken)", erledigt `swhcatblackhome` „Siggi ist daheim - schläft im Korb im Turm des Wardens" / `swhcattabbyhome` „Peanut ist daheim - …"; Belohnung „10x Sturmstahlbarren + Katzenkorb + 2x Flackerlicht-Girlande" |
| anchor | „Die Turminsel driftet. Der Warden kann sie verankern - mit deinem Metall und Stein." | „Wolkengleve + Skywatch-Banner + 5x Aurorablatt" |
| keyskyreach | „Der Warden kann ein Wachfeuer bauen, auf das die Rufsteine der Himmelsweite antworten - wenn du ihm den Sturm bringst, mit dem es brennt." | „Wachfeuer der Himmelsweite + 4x Sturmscheibe + Himmelswacht-Kapuze + 4x Sturmstahlbarren" |
| keyeden | „Eden bewacht seinen Saft und seinen Pollen mit Zähnen und Stacheln. Hol beides aus dem Garten, dann schlägt dir der Warden eine Stiege, die seine Steine wiedererkennen." | „Gartenstiege von Eden + Windheuler + Mantel des Hüters + 5x Sturmstahlbarren" |
| keysteinfeld | „Steinfeld ist nichts als bleicher Stein und das Echo, das darin gefangen ist. Bring genug von beidem zurück, dann stellt dir der Warden einen seiner Engel in die eigene Siedlung." | „Trauerengel von Steinfeld + Sturmklinge + Stiefel des Hüters + 6x Sturmstahlbarren" |
| keyghostrealm | „Aus dem Nachgarten kommt nichts, was der Nebel nicht schon berührt hat. Trag sein Knochenholz und sein Spektralerz heraus, dann setzt dir der Warden einen Raben über das Tor." | „Rabenkanzel des Nachgartens + Geisterstahl-Schnitter + Sturmstahl-Armschiene + 6x Geisterstahlbarren" |
| keycrookedbeyond | „Jede Tür in der Krümmung führt irgendwohin, wo sie nicht hinführen sollte. Bring dem Warden ihr Holz und ihre Splitter, dann hängt er dir eine ein, die es mit Absicht tut." | „Knotts Krumme Tür + Grabwind-Bogen + Auroramedaillon + 8x Geisterstahlbarren" |
| keyhell | „Die Hölle macht niemandem ohne Papiere auf. Ein Formular kann der Warden dir nicht besorgen, also nimmt er den Beweis, dass du so weit draußen warst: die Krümmung, doppelt." | „Das Höllensiegel + 16x Geisterstahlbarren" |
| edenreach | „Irgendwo über den Wolken wächst noch Grün, üppiger als alles in der Himmelsweite. Jemand pflegt es noch." | „Finde, wer den Garten Eden noch pflegt - halte Ausschau nach einem Baum der Erkenntnis." |
| edenplants | „Eveleen will einen Beweis, dass der Garten echt ist: eine Edenbeere, eine Mondmelone und eine Sonnentraube." | „3x Wissenssteckling + 10x Sturmstahlbarren" |
| steinfeldvigil | „Ives hält die Stille Weite in Ordnung, und die Stille Weite bleibt nicht in Ordnung. Die, die hier draußen umhergehen, spuken niemandem - sie wurden nur nie niedergelegt. Er weiß, was dafür nötig ist, und hat von beidem nichts mehr." | „Ives zieht kostenlos in deine Siedlung + 12x Sturmstahlbarren" |
| eleanor | „Etwas hält Eleanor hier, nachdem alle anderen weitergezogen sind. Sie weiß nicht, warum. Vielleicht kannst du ihr helfen, sich zu entscheiden." | obj „Sprich mit Eleanor. Halte Schleieressenz in der Hand, um ihr beim Loslassen zu helfen, oder komm mit leeren Händen und bitte sie zu bleiben." · Belohnung „Loslassen: Irrlichtlaterne + 10x Geisterstahlbarren. Bleiben: sie zieht in deine Siedlung ein + 10x Geisterstahlbarren." |
| mortimerrites | „Mortimer kann jedes Grab im Nachgarten benennen und fast keines erreichen. Ein Leichentuch ist Faden, ein Sarg ist Holz, und ihm fehlt beides." | „Mortimer zieht kostenlos in deine Siedlung + 6x Geiststahlbarren" |
| caspernforge | „Das Feuer des Geisterschmieds ist ausgegangen. Spektralerz nährt es, Schleieressenz löscht es - und Letztere fällt nur dort, wo der Nebel am dichtesten steht." | „Caspern zieht kostenlos in deine Siedlung + 6x Geiststahlbarren" |
| crookedarrival (tot) | „Türen stehen hier reihenweise, verschlossen vor dem Nichts. Jemand glaubt noch, dass eine von ihnen funktioniert." | „Finde, wer den Türhof noch hütet - halte Ausschau nach einer roten Tür, die für sich allein steht." |
| crookeddoor | „Mr. Knott glaubt schon länger als jeder andere, dass sich eine dieser Türen überzeugen lässt, irgendwohin zu führen. Bring ihm, was eine solche Tür brauchen würde." | „Zephyr-Gurtzeug + 12x Geisterstahlbarren + 6x Realitätssplitter" |

Abgabe-Hinweise: `swhreturnwarden` „Sprich mit dem Sky Warden.", `swhspeaktoeveleen`,
`swhspeaktoives`, `swhspeaktomortimer`, `swhspeaktocaspern`, `swhspeaktoeleanor`,
`swhspeaktoknott` („Sprich mit <Name>.").

---

## 3. Alle NPCs / Bewohner / Siedler

Alle benannten Bewohner (`SkySettlerMob`-Unterklassen) sind **unverwundbar**,
**einmal pro Welt** (`W.residentsClaimed`), **nicht vertreibbar, ziehen nicht aus**,
kommen **nach dem Tod nicht wieder** (`SkySettlers.java:272-363`). Ihre Talk-Zeilen
sind `misc.<talkKey>1..4`, ihre Begrüßung im Fenster (solange nicht Siedler)
`misc.<talkKey>pitch`. Preise mit `setStaticPriceBasedOnHappiness(min, max, Schritt)`
bewegen sich mit der Zufriedenheit zwischen min (glücklich) und max; Ankaufpreise
umgekehrt (erster Wert = glücklich).

### 3.1 Der Himmelswächter (`skywarden` → `wardensettler`)

- **Name DE:** `mob.skywarden` „Der Himmelswächter"; Siedler `mob.wardensettler` „Himmelswächter", `wardensettlername` „<name>, der Himmelswächter". Siedlungs-Hinweis `misc.wardensettlertip` „Über den Wolken angeworben, für ein Vermögen in Münzen."
- **Treffen:** Alte Wächterspitze, beim ersten Aufstieg gestempelt.
- **Anwerbung:** 30 000 Münzen (`SkyWardenMob.java:72`), nur einmal pro Welt (`isValidRecruitment`), braucht eigene Siedlung.
- **Berufe:** Handwerk, Ackerbau, Forst, Transport (Vanilla-Standard).
- **Laden:** Geisterkreide 3 Stück/+1 pro Tag @ **1 200**; sobald verdient: Wolkengleve 1 @ 4 500, Himmelswacht-Banner 1 @ 800 (nach Anker), Katzenkorb 1 @ 500, Flackerlicht-Girlande 2 @ 500 (nach Katzen). Kauft nichts. (`:116-137`)
- **Dialog:** Fenster-Einleitung = `misc.wardenrecruit1` solange nicht angeworben. Alles andere Bubbles (siehe §4.1).
- **Talk:** `misc.wardentalk1..6`.
- **Besonderes:** kann nicht sterben; gibt Geisterkreide; führt die ganze Warden-Kette und alle Region-Schlüssel.

### 3.2 Magpie („Elster") — `magpiesettler`

- **Name DE:** „Elster", Siedler „<name>, genannt Elster". Tip `misc.magpiesettlertip`: „In der Himmelsweite zu finden, an einer verlassenen Skywatch-Werkstatt." (**veraltet**, §8)
- **Treffen:** Skyway-Zollhaus (Landmarke); zusätzlich alter Weg: neben einer **spielergebauten** Werkstation in einer frisch generierten Himmelsweite-Region (16 %, `SkyLevel.java:150-208`).
- **Anwerbung:** 12 000 + **Verzollte Kassette**. Beruf: **Handelsmissionen** (`tradingmission`) + Himmelsfahrten.
- **Verkauft:** Wurmköder 120/+12 @ 9–22 · Sandstein 80/+8 @ 14–30 · Kokosnuss 30/+3 @ 18–38 · Schneeball 60/+6 @ 10–24 · Glas 40/+4 @ 20–44 · **Kurierkappe** 1 @ 900–1 500.
- **Kauft:** Himmelsstein 30→18 · Windseide 40→26 · Aetheriumerz 70→45 · Sturmsplitter 85→55 · Aurorablatt 85→55 · Fulgurit 95→62 · Prismensplitter 95→62 · Himmelspost-Paket 140→90 · Himmelsstein-Herz 45→30 · Blütenzahn 65→42 · Trauerflor 90→58 · Seelenhalsband 125→80 · Gestreiftes Horn 175→115.
- **Himmelsfahrten** (`settlement/SkyVoyages.java:171-205`), nur als Siedlerin, nur wenn der Wächter rekrutiert ist:

| Fahrt (`expedition.*`) | Kosten | Wert der Ladung | Ladung aus | Erfolg |
|---|---|---|---|---|
| Die Himmelsweite | 900 | 700–1 100 | Himmelsstein, Wolkenholz, Windseide, Aurorablatt, Fulgurit, Sturmsplitter | 70 % / 85 % / 100 % je nachdem, wie viele Reiche tiefer der tiefste freigeschaltete Rufstein liegt |
| Garten Eden | 1 800 | 1 300–2 000 | Edenholz, Edensaft, Edenbeere, Paradiesapfel, Goldener Pollen, Edenkupfererz, Sonnentraube | 0 %, bis Eden-Schlüssel steht |
| Die Stille Weite | 2 600 | 1 900–2 900 | Blasser Stein, Grabsalz, Geistermoos, Echosplitter, Kohlenholz | ab Steinfeld-Schlüssel |
| Der Nachgarten | 3 600 | 2 600–3 900 | Knochenholz, Seelenfaden, Spektralerz, Schleier-Essenz | ab Geister-Schlüssel |
| Jenseits der Krümmung | 4 800 | 3 400–5 200 | Seltsamholz, Krümmungsharz, Seltsamer Stoff, Augensamen, Gestreifter Panzer, Realitätssplitter | ab Krummen Schlüssel |
| Die Große Runde | 9 000 | 6 000–9 000 | Himmelsgewebe, Sturmstahl, Edenbronze, Echosplitter, Geisterstahl, Realitätssplitter, Prismensplitter + legendäres Zufriedenheits-Objekt | 75 %, erst ab Krummem Schlüssel |

  UI-Texte: `ui.skyvoyageask` „Ich will dich wieder auf die alte Straße schicken", `ui.skyvoyageselect` „Wohin denn?", `ui.skyvoyagecost` „Ich laufe bis <expedition> und zurück, für <cost> Münzen. <chance>% sagen, ich komme an.", `ui.skyvoyagemore` „Zeig mir den Rest der Straße", `ui.skyvoyagecomplete` „Alles, was auf dem Frachtbrief stand. Und etwas, das nicht draufstand.", gesperrt `expedition.skyvoyagelocked` „Diese Straße ist noch nicht offen".
- **Talk (DE wörtlich):** pitch „Die kurze Fassung: Ich flog Fracht für die Skywatch, die Skywatch hörte auf zu antworten, und seither sitze ich auf verzollter Ladung ohne Landeplatz. Gib mir ein Dach und einen Markt, und ich mache dich reich genug, dass du es bereust gefragt zu haben." · 1 „Alles in diesem Sack hat eine Seriennummer. Frag mich nach keiner davon." · 2 „Ich bestehle nicht die eigene Siedlung. Schreib das auf, das spart uns später Streit." · 3 „Es fiel ohnehin. Ich habe nur arrangiert, wohin." · 4 „Bring mir Bergungsgut. Ich bin die Einzige hier oben, die anständig dafür zahlt."
- **Kein eigenes `interact()`**, keine Quest.

### 3.3 Halda — `haldasettler`

- **Name DE:** „Halda", Siedler „<name> die Kellermeisterin". Tip wie Magpie (veraltet).
- **Treffen:** Grange-Keller (+ alter Werkstatt-Weg). **Anwerbung:** 9 000 + **Die Mutter**. Beruf: **Angeln**.
- **Verkauft:** Kellermeisterin-Haube 1 @ 700–1 200 · Himmelsgewebe 20/+2 @ 150–300 · Sturmglas 20/+2 @ 140–280 · **Sturmstahlbarren** 10/+1 @ 420–800 · Wolkenzupf-Leckerli 30/+3 @ 60–130 · Wolkenbeere 60/+6 @ 12–26.
- **Kauft:** Windweizen 24→15 · Wolkenbeere 20→12 · Nimbusholz 26→16 · Kohlenholz 30→19.
- **Talk:** pitch „Ich habe die Vorräte gehütet, nachdem der Letzte gegangen war. Vierzig Jahre Fässer zählen für niemanden. Ich komme mit - und ich bringe den Keller mit, du wirst wollen, was daraus kommt." · 1 „Ein Keller hält die Zeit besser als eine Uhr. Beide sind geduldig. Nur eines lohnt das Öffnen." · 2 „Bring mir das Rohe, ich gebe dir das Fertige zurück. Das ist der ganze Handel." · 3 „Die Katzen saßen immer auf dem warmen Bottich. Ich brachte es nicht übers Herz." · 4 „Alles hier oben schmeckt nach Sturm. Man lernt, das Würze zu nennen."

### 3.4 Ossian Vane — `ossiansettler`

- **Name DE:** „Ossian Vane", Siedler „Ossian <name>, der letzte Leser". Tip wie Magpie (veraltet).
- **Treffen:** Testgelände (+ alter Werkstatt-Weg). **Anwerbung:** 18 000 + **Sturmlinsenkern**. Verweigert Ackerbau/Forst.
- **Verkauft, 3 von 8 im Tagesrhythmus** (Fenster rückt mit dem Welttag, `OssianMob.java` `offerOnRotation`): Kristallessenz (6/+1 @ 900–1 800) · Aufgestiegene Scherbe (3/+1 @ 2 600–5 000) · Leerengeschoss (200/+40 @ 26–52) · Arkanahelm (1 @ 5 200–9 000) · Arkanaharnisch (1 @ 6 400–11 000) · Arkanastiefel (1 @ 4 800–8 400) · Leerentasche (1 @ 7 500–13 000) · Auge der Leere (1 @ 12 000–20 000); dauerhaft Werkmeister-Reif 1 @ 1 100–1 900.
- **Kauft:** Aetheriumbarren 150→95 · Sturmstahlbarren 220→140 · Himmelsgewebe 90→58.
- **Talk:** pitch „Ich blieb wegen des Archivs, nicht wegen des Ordens. Die Hälfte ist unleserlich, die andere habe ich zu oft gelesen. Ich habe Dinge im Beutel von weiter draußen als diesem Himmel - und einen Preis, der dir nicht gefallen wird." · 1 „Ich habe jede Liste gelesen, die die Skywatch führte. Deiner ist der erste neue Name seit Langem." · 2 „Was ich verkaufe, stammt nicht von hier. Frag nicht, woher." · 3 „Der Turm ist keine Ruine. Eine Ruine hat aufgehört, etwas zu bedeuten." · 4 „Bring mir Barren, kein Erz. Für Schmelzen fehlt mir die Geduld."

### 3.5 Eveleen — `eveleensettler`

- **Name DE:** „Eveleen", „Eveleen <name>, Botanikerin von Eden". Tip: „Im Garten Eden zu finden, bei einem Baum der Erkenntnis. Sie kommt außerdem in eine Siedlung, in der Eden-Gras wächst. Arbeitet in Ackerbau, Forstwirtschaft und Düngen."
- **Treffen:** Eden, 35 %/Region, Baum der Erkenntnis in 3 Kacheln. Anreise: Wächter + 9 Eden-Gras.
- **Anwerbung:** 7 000 → **0** nach `swh_edenplants`. Beruf **Düngen**.
- **Verkauft:** Eden-Grassamen 12/+2 @ 180–380 · Weizensamen 20/+4 @ 20–50 · Karottensamen @ 60–120 · Kürbissamen @ 80–160 · Erdbeersamen @ 100–200 · Wolkenbeeren-Setzling 6/+1 @ 220–440 · Apfelsetzling 3 @ 500–1 000 · Zitronen- und Bananensetzling 3 @ 1 000–1 800 (erst nach Kill „sageandgrit") · Dünger 200/+40 @ 12–30 · Blumentopf 10/+2 @ 40–90 · Bienenkönigin 1 @ 1 000–1 600 (nach Kill „piratecaptain") · Hübsche Blume 2 @ 600–1 200.
- **Kauft:** Windweizen 26→16 · Wolkenbeere 22→13 · Weizen 10→2 · Sonnenblume 12→3.
- **Dialog:** Quest-Bubbles `misc.eveleenasksplants` / `misc.eveleenplantsdone` (§4).
- **Talk:** pitch „Ich bin den weiten Weg heraufgekommen, auf der Suche nach einem Garten, der angeblich noch wächst. Gefunden habe ich ihn nicht. Aber du hast Boden, und ich habe Samen, die dir hier oben sonst niemand verkauft." · 1 „Alles wächst, wenn man in der richtigen Reihenfolge geduldig und unbarmherzig ist." · 2 „Dieses Beet hat Durst. Nein, nicht gießen. Es hat auf etwas anderes Durst." · 3 „Ich habe drei Samen, die ich nie gepflanzt habe. Ich wüsste gern, was sie sind, bevor ich es herausfinde." · 4 „Ein Garten ist keine Dekoration. Ein Garten ist ein Streit, den du mit dem Boden führst."

### 3.6 Ives — `ivessettler`

- **Name DE:** „Ives", „Ives <name>, Küster der Stillen Weite". Tip: „In Steinfeld zu finden, bei einem zerbrochenen Engel. Er kommt nicht von selbst in eine Siedlung. Arbeitet in Handwerk und Transport - und kauft alles, was die Stille Weite hergibt."
- **Treffen:** Steinfeld, 14 %/Region, Zerbrochener Engel in 3 Kacheln. Keine Anreise.
- **Anwerbung:** 11 000 → **0** nach `swh_steinfeldvigil`. Verweigert Ackerbau/Forst.
- **Verkauft:** Grabstein 1 + 2 (je 10/+2 @ 90–200) · Kerze 40/+8 @ 14–34 · Vase 8/+2 @ 70–150 · Steinzaun 30/+8 @ 20–44 · Steinzauntor 6/+2 @ 40–90 · Blasser Stein 40/+10 @ 18–40.
- **Kauft:** Blasser Stein 12→7 · Grabsalz 34→22 · Geistermoos 40→26 · Echosplitter 75→48.
- **Talk:** pitch „Hier draußen landen die Dinge. Am Ende auch die Leute. Irgendwer muss aufschreiben, was sie einmal waren, und offenbar bin das ich." · 1 „Hier wird nichts begraben. Hier wird nur abgelegt. Das ist ein Unterschied, und er stört mich." · 2 „Einer von ihnen geht jeden Abend dieselben elf Schritte. Ich habe mitgezählt. Unterbrochen habe ich ihn nicht." · 3 „Das Gold wird dunkel hier draußen. Alles, worauf der Himmel stolz war, wird hier draußen dunkel." · 4 „Ich führe eine Liste. Eine Seite davon habe ich noch nie zu Ende gebracht."

### 3.7 Mortimer — `mortimersettler`

- **Name DE:** „Mortimer", „Mortimer <name>, Bestatter". Tip: „Im Nachgarten zu finden, bei einem Grabstein. Er kommt außerdem in eine Siedlung, die einen Friedhof gebaut hat. Arbeitet in Transport, Handwerk und Jagd."
- **Treffen:** Geisterreich, 14 %/Region (einer der drei zufällig), Geistergrabstein in 3 Kacheln. Anreise: Wächter + 3 Grabsteine.
- **Anwerbung:** 8 000 → **0** nach `swh_mortimerrites`. Beruf **Jagd**, verweigert Ackerbau/Forst.
- **Verkauft:** Grabstein 1/2 @ 90–200 · Krypta-Grabstein 1/2 @ 120–260 · Sarkophag 2 @ 1 800–3 400 (nach Kill Sumpfwächter) · Kerze @ 14–34 · Knochenkandelaber 8 @ 100–220 · Totholzkerzen 8 @ 80–180 · Geisterbecken 1 @ 2 400–4 200 (nach Kill Reaper) · Knochenstuhl @ 70–150 · Knochen-Modultisch @ 90–190 · Knochen-Bücherregal @ 150–320 · Knochen-Kommode @ 150–320 · Knochenuhr @ 200–420 · Knochentruhe @ 160–340 · Schädel @ 60–130.
- **Kauft:** Knochen 14→8 · Ektoplasma 40→26 · Schleier-Essenz 70→45.
- **Talk:** pitch „Du hast Tote zu verwahren und keinen Ort dafür. Ich habe einen Spaten, einen Hut und keine besondere Eile. Wir können einander helfen." · 1 „Eine Siedlung ohne Friedhof ist eine Siedlung, die noch nicht ehrlich war." · 2 „Ich jage auch. Irgendjemand muss etwas hereinbringen, und ich bin ohnehin passend gekleidet." · 3 „Bedanke dich nicht. Bedanke dich später. Am besten viel später." · 4 „Jeder Stein, den ich verkaufe, ist leer. Was du darauf schreibst, ist deine Sache."

### 3.8 Caspern — `caspernsettler`

- **Name DE:** „Caspern", „Caspern <name>, Geisterschmied". Tip: „Im Nachgarten zu finden, bei einem Grabstein. Er kommt außerdem in eine Siedlung, die eine Ätherschmiede gebaut hat. Arbeitet in Handwerk und Transport - und nichts im Freien."
- **Anwerbung:** 14 000 → **0** nach `swh_caspernforge`. Verweigert Ackerbau/Forst.
- **Verkauft:** Nachtstahlerz 40/+8 @ 45–100 · Nachtstahlbarren 20/+4 @ 160–340 · Phantomstaub 30/+6 @ 70–160 · Seide 40/+8 @ 30–70 · Knochenpfeil 300/+60 @ 4–10 · Knochengriff 1 @ 900–1 700 (nach Sumpfwächter) · Nachtstahlschleier 1 @ 2 600–4 800 (nach Reaper).
- **Kauft:** Aetheriumbarren 150→95 · Sturmstahlbarren 220→140 · Knochen 16→9 · Ektoplasma 45→29.
- **Talk:** pitch „Ich bearbeite Metall, das sich erinnert. Das ist kein Trick und es ist nicht billig. Bau mir eine Schmiede, und ich zeige dir, wie ein Barren klingen soll." · 1 „Kaltes Eisen vergisst. Dieses hier nicht." · 2 „Nein, ich gehe nicht nach draußen. Das Feuer ist hier drinnen." · 3 „In jedem Barren, den ich ziehe, steckt jemandes Geduld. Meistens meine." · 4 „Bring mir Erz. Bring mir Staub. Bring mir keine Fragen, woher der Staub kommt."

### 3.9 Eleanor — `eleanorsettler`

- **Name DE:** „Eleanor", „Eleanor <name>, die noch immer hier ist". Tip: „Im Nachgarten zu finden, bei einem Grabstein. Sie kommt nie zu dir - du entscheidest, ob sie bleibt. Arbeitet in der Tierhaltung."
- **Anwerbung:** 5 000 (wird nie erlassen). Beruf **Tierhaltung**.
- **Verkauft:** Hübsche Blume 3 @ 500–1 000 · Hübscher Blumenstrauß 1 @ 2 200–4 000 · Topfblume 1/2/3 je 6 @ 60–140 · Laterne 10 @ 50–120 · Wasserlaterne 10 @ 60–140.
- **Kauft:** Schleier-Essenz 60→38 · Ektoplasma 38→24.
- **Besonders:** Zwei Enden (§1.5 L). Nach Loslassen wird sie nie wieder platziert (`VeilResidents.java` prüft `eleanorGone`).
- **Talk:** pitch „Ich weiß nicht, warum ich noch hier bin. Ich dachte, ich wüsste es. Wenn du möchtest, dass ich bleibe, bleibe ich - ich kann gut mit Tieren, und ihnen ist egal, was ich bin." · 1 „Ich erinnere mich an eine Küche. An wessen, weiß ich nicht mehr." · 2 „Die Ziegen fürchten sich nicht vor mir. Das ist sehr viel wert." · 3 „Sei meinetwegen nicht traurig. Ich bin nicht sicher, ob ich es kann." · 4 „Manchmal ertappe ich mich beim Warten. Ich frage schon lange nicht mehr, worauf."

### 3.10 Mr. Knott — `knottsettler`

- **Name DE:** „Mr. Knott", „Mr. Knott <name>, der Türsteher". Tip: „Im Jenseits der Krümmung zu finden, auf dem Türhof. Er kommt nicht von selbst in eine Siedlung. Arbeitet im Handel, und nichts im Freien."
- **Treffen:** Krummes Jenseits **und Höllen-Ring** (`SkyLevel.java` ruft `CrookedResidents.place` für beide), 20 %/Region, ≤ 9 Kacheln vom Türhof.
- **Anwerbung:** 22 000 (bleibt auch nach der Quest). Beruf **Handelsmissionen**, verweigert Ackerbau/Forst.
- **Verkauft:** Leerenwürfel 6 @ 220–440 · Kleiner Runenstein 6 @ 180–360 · Leerenmaske 2 @ 1 400–2 600 · Alienmaske 3 @ 700–1 300 · Haifischmaske 3 @ 700–1 300.
- **Kauft:** Krümmungsharz 26→17 · Augensamen 34→22 · Realitätssplitter 65→42.
- **Talk:** pitch „Jede Tür auf diesem Hof ist überzeugt, irgendwohin zu führen. Ich glaube ihnen das schon länger, als vernünftig ist. Du siehst aus wie jemand, der einen von uns beiden recht behalten lassen könnte." · 1 „Gehst du irgendwohin?" · 2 „Eine Tür, die sich zu nichts hin öffnet, ist immer noch eine Tür. Sie hat nur Manieren." · 3 „Ich verkaufe keine Schlüssel zu Türen, die ich nicht getestet habe. Testet man genug davon, braucht man irgendwann keine Schlüssel mehr." · 4 „Irgendjemand muss weiter fragen, wohin sie führen. Dann kann es auch ich sein."

### 3.11 Dorian, der Nachtgebundene — `vampiresettler`

- **Name DE:** `mob.vampiresettler` „Dorian", „Dorian <name>, der Nachtgebundene". Tip: „Kommt in eine Siedlung, in der ein Sarg steht. Schläft tagsüber und arbeitet nachts; jagt Wildtiere außerhalb der Mauern, niemals euer Vieh."
- **Treffen:** **Nirgends in der Welt** — nur per Anreise (Sarg in der Siedlung, **kein Wächter nötig**, 90 Tickets).
- **Anwerbung:** 11 000. Beruf **Jagd**, verweigert Ackerbau/Forst. **Kein Laden** (keine Sell/Buy-Einträge).
- **Mechanik** (`mobs/VampireSettlerMob.java`):
  - schläft tagsüber, arbeitet nachts (oder im Abenteuertrupp/auf Befehl); nachts ×1,45 Tempo, außer in Sonnenlicht.
  - isst nicht; statt Hunger **Blutdurst** 0–1, leert sich in 1 200 s Dienst (`tickHunger`, `:184`).
  - **Jagd** (`VampireHuntAINode`): nachts, nicht-feindliche, nicht-menschliche Mobs **außerhalb** der Siedlungsgrenzen; „leersaugen" `drain()` (`:338`): +0,34 Durst; 5/6: Beute verschwindet, 1× **Blutfläschchen** + 40 % Hammel; **1/6: Beute wird zum „Blutknecht"** (feindliche Krypta-Fledermaus, Drops 1–2 Blutfläschchen, 35 % Leder). Bubble `misc.vampiredrained` „..." bzw. `misc.vampireturned` „Das da ist noch nicht fertig. Zurück."
  - **Biss:** bei Durst 0 alle 3 600 Settler-Ticks (≈ 3 min) ein Siedler im Umkreis 320 px ohne Blutfieber → **Blutfieber** (1 Spieltag: −20 % Tempo, −15 % Abbau, −15 % Bauen, −10 % max. LP), +0,5 Durst, Bubble `misc.vampirebite` „Verzeih. Es hat niemand etwas rausgestellt." (`:212-230`)
  - **Heilung:** Arzt, Option „Die Gebissenen behandeln (<price> Münzen)" = 100 Münzen.
  - **Fläschchen hinstellen/füttern: NICHT implementiert** (§8).
- **Talk:** pitch „Ihr haltet einen Sarg und niemanden darin. Das ist entweder eine Einladung oder ein Versehen, und ich hoffe auf das Erste." · 1 „Eure Leute arbeiten im Licht und schlafen im Dunkeln. Ich habe das nie verstanden, aber ich übernehme die andere Hälfte." · 2 „Zwei Felder weiter steht ein Reh, das mich noch nicht bemerkt hat. Gib ihm eine Stunde." · 3 „Ich nehme nichts, was euer Zeichen trägt. Das ist keine Höflichkeit. Das ist die Abmachung." · 4 „Stellt ein Fläschchen raus, wenn ihr lieber nicht habt, dass ich losziehe. Ich bin nicht stolz."

### 3.12 Geisterführer — `ghostguide`

- **Name DE:** „Geisterführer", „<name>, der Geisterführer".
- **Treffen:** am Séance-Zirkel beschworen (nur einer in 320 px, sonst `misc.seanceguidehere` „Dein Führer ist bereits hier.").
- **Nicht anwerbbar** (`misc.ghostguidenohire` „Die Toten ziehen nicht ein. Komm wieder, wenn du etwas brauchst."), unverwundbar.
- **Laden (Münzpreise im Code!):** Geisterstahl-Schnitter 1 @ 14 000–24 000 · Grabwind-Bogen 1 @ 12 000–20 000 · Geisterstahlbarren 20/+4 @ 400–900 · Schleier-Essenz 30/+6 @ 250–560. **Kauft:** Ektoplasma 60→38 · Schleier-Essenz 170→108 · Geisterstahlbarren 275→175.
- **Erstes Gespräch:** Nebelmal (s. 1.5 H), öffnet dann keinen Laden.
- **Fenster:** `misc.ghostguideintro` „Münzen? Die Toten haben reichlich davon und nichts, wofür sie sie ausgeben. Ich nehme deine trotzdem - und ich kaufe, was der Nebel dir gibt." (seit 2026-09-24 passend zum Münz-Laden)
- **Talk:** 1 „Alle stellen dieselbe erste Frage, und keinem gefällt die Antwort." · 2 „Vorsicht mit dem Nebel weiter draußen. Der gehört nicht mir." · 3 „Ich war Landvermesser. Ich zähle immer noch Dinge. Das beruhigt." · 4 „Bring mir irgendwann ein gutes Essen. Essen kann ich es nicht. Riechen würde ich es gern."

### 3.13 Siggi und Peanut — `spirecatblack` / `spirecattabby`

- Siggi = schwarz, Lager im Sturmschleier; Peanut = getigert, Lager in den Aurorabänken (`SkyLevel.findLairSite`).
- Leckerli-Trigger s. 1.2 Schritt 4. Ohne Leckerli: Bubble „Mrrp?" + `misc.wardencatnotreat`.
- **Katzenkorb:** Wer einen Korb aufstellt, bekommt die Katzen dorthin (neuester Korb gewinnt, `W.catHomeSet/X/Y/Level`); Korb entfernt → zurück zur Spitze.
- **In der Siedlung:** Streicheln = Buff 1 Spieltag (`mobs/CatCuddleBuff.java`); alle ~8 min pro Katze ein „Moment" (`SpireCatMob.java:302-319`): Geschenk (Siggi: gebratener Fisch / selten Rabenfeder; Peanut: Lachs / selten Käse), Jagd-Szene, oder **Dünger**.

### 3.14 Berufs-Siedler ohne Himmelsbezug

| Wer | Name DE | Anreise | Anwerbung | Laden | Dienst | Talk (`mobmsg.*`) |
|---|---|---|---|---|---|---|
| **Therapeut** `therapisthuman` | „Therapeut", „<name>, der Therapeut"; Tip `settlement.therapisttip` „Zieht von selbst in deine Siedlung, so bereitwillig wie ein Schmied" | immer, 75 Tickets | 900–1 400 Münzen | Hübsche Blume 600–950 · Buch 300–550 · Rezeptbuch 400–700 · Musikspieler 1 200–2 000 · Heim-Vinyl 400–750 · Regenerationstrank 150–300 · Manaregenerationstrank 150–300 · Blazer 75–150 · Anzugschuhe 75–150; kauft Sonnenblume 18→11, Seide 20→12 | **4 Therapieplätze**: Patient bekommt +50 % „Mir hört jemand zu"-Stimmung (`SkyTherapy.BONUS_PERCENT`); **Wesenszug tauschen** für 50 000 Münzen (zufälliger neuer Zug) | `therapisttalk1..5` |
| **Arzt** `doctorhuman` | „Arzt", „<name>, der Arzt"; Tip „Zieht von selbst in deine Siedlung, so bereitwillig wie ein Schmied" | immer | 900–1 400 | Überlegener Heiltrank 20–60 · Großer Tempotrank 40–120 · Großer Angriffstempotrank 40–120 · Großer Regenerationstrank 40–120 · Großer Widerstandstrank 60–180 · Großer Kampftrank 100–300 · Schweinefilet 70–210 · Frittiertes Hähnchen 70–210 · Sushi 64–192 · Spaghetti Bolognese 60–180 · Rindergulasch 60–180; kauft Feuermohn, Eisblüte 12→3 | **Volle Heilung** 100 Münzen; **Blutfieber heilen** 100 Münzen | `doctortalk1..5` |
| **Kriegsveteran** `warveteranhuman` | „Kriegsveteran", „<name>, der Kriegsveteran"; Tip „Zieht von selbst in deine Siedlung ein, genau wie ein Schmied" | immer | 900 | Barrikade 120–220 · Stacheldrahtzaun 90–160 · Selbstschussanlage 600–950 · Kanone 1 400–2 200 | **Verteidigungsausbau** 5 Stufen: Stufe 1 = 100 Steinpfeile + 5 Eisenbomben, 2 = 150 Eisenpfeile + 8 Eisenbomben, 3 = 200 Feuerpfeile + 12 Eisenbomben, 4 = 250 Frostpfeile + 6 Dynamit, 5 = 300 Giftpfeile + 10 Dynamit; je Stufe +25 % Geschütz-, +40 % Kanonenschaden | `warveterantalk1..5` |
| **Zwielichtiger Händler** `twilightmerchanthuman` | „Zwielichtiger Händler", „<name>, der Zwielichtige Händler"; Persönlichkeit `personalities.twilightmerchant_desc` „<name> kommt nur auf der Durchreise und verkauft, was sonst niemand hat." | Besucher, 60 Tickets | nicht anwerbbar (Besucher) | je Besuch **3 Kostüme** (Kopf 300–500, Brust 350–600, Schuhe 250–450) + **5 Deko-Stücke** (400–900) aus den Listen in 5.8 | — | `misc.twilightmerchanttalk1..5` |

Talk-Texte (wörtlich):
- Therapeut: 1 „Hier ist niemand kaputt. Manche tragen nur zu viel mit sich herum." · 2 „Ich habe vier Stühle. Das ist keine Bescheidenheit, so viel Aufmerksamkeit habe ich." · 3 „Sesshaft werden heißt zur Hälfte zugeben, dass man sesshaft ist." · 4 „Man kann einen Menschen ändern. Es kostet mehr, als die Leute denken." · 5 „Setz dich, wenn du magst. Ich nehme Geld für die Arbeit, nicht fürs Reden."
- Arzt: 1 „Das meiste, was die Leute da unten umbringt, hätten sie aussitzen können." · 2 „Ich verkaufe die guten Tränke. Und Abendessen, das besser wirkt – nur glaubt mir das keiner." · 3 „Komm wieder, bevor es interessant wird. Interessant ist teuer." · 4 „Nein, ich sehe mir das nicht an. Ja, ich bringe es in Ordnung." · 5 „Iss was, bevor du steigst. Das ist die ganze Medizin."
- Veteran: 1 „Ich hab schon schlimmere Postenrunden gedreht als diese hier." · 2 „Barrikaden gewinnen keinen Kampf. Sie kaufen dir Zeit dafür." · 3 „Jeder Überfall, den ich gesehen hab, fing gleich an: Einer hat aufgehört, die Mauer zu beobachten." · 4 „Kauf den Stacheldraht. Ich hab gesehen, was der mit einem Ansturm macht." · 5 „Halt die Munition voll, dann sorg ich dafür, dass sie was bringt."
- Zwielichthändler: 1 „Psst. Hier drüben. Nein, der andere Schatten." · 2 „Alles gebraucht. Die Vorbesitzer beschweren sich nicht mehr." · 3 „Sag dreimal meinen Namen und ich geb dir Rabatt. Lieber doch nicht." · 4 „Das Sargbett? Schläft sich wie tot. Garantiert." · 5 „Ich bin nur bis Sonnenuntergang da. Oder -aufgang. Vergess ich immer."

### 3.15 Vanilla-Stylistin (erweitert)

Verkauft zusätzlich (`SalonWares.java:270-278`, kein Rezept, einzige Quelle):
Friseurstuhl 900–1 400 · Beleuchteter Friseurspiegel 700–1 100 · Friseurladen-Schild
600–950 · Friseurladen-Kasse 800–1 250 · Pflegemittel-Tablett 250–400 · Barbier-Säule 450–700.

---

## 4. Alle Dialoge

Necesse zeigt Mod-Text auf vier Wegen: **Bubble** (Sprechblase über dem NPC;
eine neue ersetzt die alte), **Fenster-Einleitung** (oben im Handels-/Anwerbefenster),
**Dialogoption** (Knopf im Fenster), **Kacheltext** (`TileText`, schwebt über einer
Kachel, nur für den Spieler). Talk-Zeilen stehen in §3.

### 4.1 Himmelswächter

| Schlüssel | Weg | Text (DE) | Wann | Wirkung |
|---|---|---|---|---|
| `misc.wardenrecruit1` | Fenster | „Hier ist eine seltsame Bitte an einen Fremden: Mein Orden hütete den Himmel vierhundert Jahre, und ich bin der letzte Name auf seiner Liste. Ein Hüter ohne Turm ist nur ein alter Mann im Wind." | solange nicht Siedler | — |
| `misc.wardenintro1/2/3` | Bubble (alle 3 in einer) | „Hm. Die Treppe. Lange hat niemand mehr die Treppe genommen." / „Ich bin der Wächter. Der letzte, wenn du mitzählst. Ich hüte das Licht. Hütete, besser gesagt. Es erlosch mit dem Sturm." / „Nimm diese Windseide. Du bist den ganzen Weg geklettert; mit leeren Taschen schicke ich dich nicht fort." | erstes Gespräch, `stage==0` | +6 Windseide |
| `misc.wardengiveschalk` | Bubble | „Du warst also draußen im Nebel. Nimm das - Kreide, aus Grabsalz gemahlen. Zeichne zu Hause einen Kreis, dann antwortet jemand." | erstes Gespräch nach Nebelkontakt | +1 Geisterkreide |
| `misc.wardencatsdone` | Bubble | „Beide. Unter einem Dach. Damit ist der Turm ordentlich geschlossen - nimm den Korb, sie kennen ihn." | Katzen-Abgabe | Katzen-Belohnung |
| `misc.wardenanchordone` | Bubble | „Metall und Stein, und die alte Insel hört auf zu wandern. Sie wird da sein, wo du sie verlassen hast. Das zählt, später. Nimm die Gleve - die Himmelswacht hatte nie jemanden, dem sie sie geben konnte." | Anker-Abgabe | Anker-Belohnung |
| `misc.wardenglaivecatchup` | Bubble | „Ich schulde dir noch etwas für den Anker. Die Gleve der Himmelswacht - sie hat Wolken geschnitten, bevor sie je etwas anderes schnitt. Sie gehört dir." | Kapitel DONE, Gleve nie gezahlt | +Wolkengleve |
| `misc.wardenkeyask<realm>` | Bubble | skyreach „Es gibt einen Weg, das zu rufen, was da draußen schläft - aber es antwortet nur seinesgleichen. Bring mir den Sturm, dann baue ich dir ein Wachfeuer." · eden „Dann also Eden. Saft und Pollen, und beides wird dir niemand reichen." · steinfeld „Steinfeld lässt seine Toten stehen. Bring mir bleichen Stein und das Echo darin, dann stelle ich dir einen seiner Engel auf." · ghostrealm „Jetzt der Nachgarten, und nimm deine Kreide mit. Knochenholz und Spektralerz." · crookedbeyond „Der letzte, und der schlimmste. Die Krümmung hat für alles eine Tür. Bring mir das Holz und die Splitter, dann hänge ich dir eine ein, die sich benimmt." · hell „Noch einer. Weiter draußen als die Türen liegt etwas, das Buch führt. Bring mir das Doppelte von dem, was die Krümmung gekostet hat, dann stemple ich dir etwas, das es anerkennt." | Schlüssel-Quest vergeben | Quest |
| `misc.wardenkeydone<realm>` | Bubble | skyreach „Das brennt. Stell es daheim ab, wo die Mauern dir gehören." · eden „Grünes Zeug, und noch warm. Nimm die Stiege - sie weiß, wie ihre eigenen Steine geschnitten sind." · steinfeld „Sie ist schwerer, als sie aussieht. Gib ihr Boden, auf dem man sie in Ruhe lässt." · ghostrealm „Ein Vogel für die Toten, und er sitzt überall, wo du ihn hinstellst. Das ist mehr, als die meisten von ihnen tun." · crookedbeyond „So. Eine eigene Tür, und sie öffnet mit Absicht. Das ist der ganze Satz." · hell „Lebendig, tot und unvernünftig. Alles scheint in Ordnung zu sein. Verlier es nicht." | Abgabe | Belohnung |
| `misc.gatenobinding` | Kacheltext | „Das Tor summt schlafend.\nEs hat noch nirgendwohin zu führen - steig erst von deiner eigenen Treppe auf." | Skywatch-Tor ohne gespeicherte Treppe | — |
| *ungenutzt* `wardenrecruit2`, `wardenrecruitconfirm`, `wardenrecruitwait`, `wardenrecruitdone1/2`, `wardenrecruitnohome`, `wardenfarewell`, `wardenshopclosed` | — | z. B. `wardenrecruit2` „Stell mich ein. Hunderttausend Münzen …" | **nie** | — (§8) |

### 4.2 Katzen

| Schlüssel | Weg | Text | Wann |
|---|---|---|---|
| `misc.wardencatfound1` | Bubble | „Mrrp?" | jedes Ansprechen ohne Heim |
| `misc.wardencatnotreat` | Kacheltext | „Die Katze beäugt dich misstrauisch.\nSie erwartet eindeutig einen Snack." | ohne Leckerli |
| `misc.wardencattreat` | Kacheltext | „Die Katze schnappt sich das Leckerli und verschwindet in einem Wölkchen -\ndirekt in den Korb im Turm des Wardens, <dist> Felder <dir> von hier." | Leckerli, kein eigener Korb |
| `misc.wardencattreatbasket` | Kacheltext | „… direkt in den Korb, den du bei <x>, <y> aufgestellt hast (<level>)." | Leckerli, eigener Korb |
| `misc.wardencatpurr` | Bubble | „Prrrrt." | Katze ist daheim / Streicheln |
| `misc.wardencatathome` / `…basket` | Kacheltext | „Diese Katze ist endgültig eingezogen.\nSie schläft jetzt im Korb im Turm des Wardens." / „… Ihr Korb steht bei <x>, <y> - das ist jetzt ihr Zuhause." | Katze schon daheim, nicht in Siedlung |
| `misc.siggipetted` / `peanutpetted` | Kacheltext | „Siggi drückt sich in deine Hand und brummt wie ein kleines Gewitter.\nSiggis Schnurren: einen ganzen Tag schärfer und zäher." / „Peanut wirft sich auf den Rücken, alle vier Pfoten in der Luft.\nPeanuts Schnurren: einen ganzen Tag flinker auf den Beinen." | Streicheln in Siedlung |
| `misc.siggigiftfish`, `siggigiftfeather`, `peanutgiftfish`, `peanutgiftcheese`, `siggihunt`, `peanutbutterfly`, `siggipoop`, `peanutpoop` | Bubble | „*legt dir einen gebratenen Fisch vor die Füße* Mrrrp." · „*überreicht eine Rabenfeder. Unfassbar stolz.*" · „*schleppt einen ganzen Lachs an. Woher, weiß keiner.*" · „*hat Käse "gefunden". Nicht fragen.*" · „*fixiert eine Motte... wackelt... SPRUNG!* ...daneben." · „*jagt einen Schmetterling im Kreis und verliert*" · „*buddelt ein Loch und hält dabei die ganze Zeit Blickkontakt*\n(hat Dünger hinterlassen)" · „*hinterlässt etwas hinter den Blumen*\n(hat Dünger hinterlassen)" | zufällige Stadtmomente |
| `misc.catbasketready`, `catbasketmovedin`, `catbasketmoved`, `catbasketremoved` | Kacheltext | „Der Korb steht bereit, und leer.\nSiggi und Peanut sind noch oben in der Himmelsweite - finde sie und gib ihnen ein Wolkenpuff-Leckerli,\ndann ziehen sie hier ein." · „Katzen erkennen einen guten Korb:\n<count> Katze(n) sind gerade bei <x>, <y> eingezogen." · „Der neueste Korb gewinnt:\n<count> Katze(n) haben den alten verlassen und sind bei <x>, <y> eingezogen." · „Der Korb ist weg, also sind die Katzen zurück in den Korb im Turm des Wardens gegangen.\nStell einen neuen auf, dann kommen sie dorthin." | Korb aufstellen/abbauen |
| Buff-Tooltips `bufftooltip.siggicuddletip` / `peanutcuddletip` | — | „Siggi findet dich gut. Für heute." / „Peanuts fünf Minuten sind ansteckend." | — |

### 4.3 Bewohner-Quests (Bubbles)

| Schlüssel | Text | Wann |
|---|---|---|
| `misc.eveleenasksplants` | „Bring mir von jedem eins: eine Edenbeere, eine Mondmelone, eine Sonnentraube. Beweise mir, dass es dort draußen noch wächst." | Quest vergeben |
| `misc.eveleenplantsdone` | „Also. Es wächst noch, dort draußen. Gut." | Abgabe |
| `misc.ivesasksvigil` | „Bring mir Grabsalz und Geistermoos, dann lege ich sie ordentlich nieder. Vierzehn und zehn. Sie haben lange genug gewartet." | vergeben |
| `misc.ivesvigildone` | „So. Jetzt können sie aufhören zu laufen. Nimm die Barren - der Wärter schuldete sie mir, und ich wollte sie nie." | Abgabe |
| `misc.mortimerasksrites` | „Zwölf da draußen habe ich nicht erreichen können. Bring mir Seelenfaden für die Leichentücher und Knochenholz für die Kisten, dann hole ich sie." | vergeben |
| `misc.mortimerritesdone` | „Erledigt, und ordentlich erledigt. Behalt dein Geld. Nimm stattdessen die Barren - und nimm mich." | Abgabe |
| `misc.caspernasksforge` | „Mein Feuer ist ausgegangen, und ich bekomme es nicht wieder an. Spektralerz, um es zu nähren, Schleieressenz, um es zu löschen. Danach reden wir darüber, was du geschmiedet haben willst." | vergeben |
| `misc.caspernforgedone` | „Hör dir das an. Das ist der Klang, von dem ich dir erzählt habe. Nimm die hier, und bezahl mich nie wieder." | Abgabe |
| `misc.knottasksdoor` | „Wenn du wirklich glaubst, dass eine davon irgendwohin führt, beweise es. Bring mir, was eine solche Tür brauchen würde." | vergeben |
| `misc.knottdoordone` | „Ha! Sie führt also doch irgendwohin. Hier - das hast du dir erstritten." | Abgabe |
| `misc.eleanorneedessence` | „Noch nicht genug von mir. <have> von <need>." | Essenz in der Hand, < 12 |
| `misc.eleanorfarewell` | „Oh. Oh, das war also alles." | Loslassen |
| `misc.eleanorpassedon` (Kacheltext) | „Eleanor verweht wie Atem auf kaltem Glas.\nWo sie stand, liegt eine kleine Laterne, noch warm." | Loslassen |

„Noch zu wenig dabei" sagt **niemand** etwas (bewusst, `SkyQuests.java:74-77`) — nur das Journal zeigt es.

### 4.4 Nebel, Séance, Tore, Rufsteine, Schlüsselstücke

| Schlüssel | Weg | Text | Wann |
|---|---|---|---|
| `misc.veilexposurewarning` | Kacheltext | „Von allen Seiten strömt Kälte herein.\nDer Nebel hat begonnen, dich zu wiegen - geh zurück." | erster Stapel Seelenblöße |
| `misc.veilmarkcrossing` | Kacheltext | „Der Nebel öffnet sich einen Schritt vor dir und schließt sich hinter dir.\nDas Mal hält." | mit Mal im 160-Kachel-Nebelring |
| `bufftooltip.soulexposuretip`, `…band1..4` | Buff | „Der Schleier wiegt dich. Kehr um, bevor er fertig ist." · „Die Sicht schwindet - der Nebel rückt heran" · „…, die Glieder werden schwer" · „…, das Leben rinnt weg" · „Der Schleier hat dein Maß. Geh, oder bleib hier." | — |
| `deaths.swhveil1..3` | Todesmeldung | „<victim> wurde von <attacker> gewogen und zu leicht befunden" · „<victim> ging zu weit in <attacker> hinein" · „<attacker> hat <victim> behalten" (Angreifer `deaths.swhveilname` „der Schleier") | Tod durch Seelenblöße |
| `misc.seanceneedsettlement` | Bubble (Client) | „Kreide hält nur dort, wo Menschen wohnen. Zeichne sie in deiner Siedlung." | Kreide außerhalb Siedlung |
| `misc.seancenobodyanswers` | Kacheltext | „Die Kreide hier ist alt und kalt.\nNichts antwortet darauf." | Zirkel außerhalb Siedlung |
| `misc.seanceguidehere` / `seanceguidearrives` | Kacheltext | „Dein Führer ist bereits hier." / „Die Kreide raucht, und <name> tritt heraus." | Zirkel benutzen |
| `misc.ghostguideunlock1` | Bubble | „Atme aus. So. Jetzt kennt dich der Nebel - die Seelenblöße rührt dich nicht mehr an." | erstes Gespräch Führer |
| `misc.edenbasinneedsseeds` / `edenbasinalreadyeden` / `edengateopened` | Kacheltext | „Das Becken braucht 6 Eden-Grassamen." / „Das Becken ist hier leer.\nDu stehst bereits in Eden." / „Das Becken füllt sich mit grünem Licht und öffnet sich zum Garten Eden." | Edenschwelle |
| `misc.basinneedsectoplasm` / `basinalreadyghost` / `ghostgateopened` | Kacheltext | „Das Becken braucht zwölf Ektoplasma." / „Das Becken schweigt hier.\nDu bist bereits bei den Toten." / „Das Becken füllt sich mit kaltem Licht und öffnet sich zum Nachgarten." | Seelenbecken |
| `misc.crookeddooropened` | — | „Die Kreidelinie erhebt sich vom Boden. Sie ist zu einer Tür geworden." | **nie** (tot) |
| `misc.bossportallocked` | Kacheltext | „Der Stein bleibt kalt.\nBau das Schlüsselstück dieses Reiches daheim auf, dann antwortet er." | Rufstein ohne Schlüssel |
| `misc.bossportalbusy` | Kacheltext | „Sein Wächter ist bereits erwacht.\nBeende erst diesen Kampf." | Boss lebt noch |
| `misc.bossportalsilent` | Kacheltext | „Nichts antwortet.\nWas hier schlief, hat noch keinen Namen." | kein Boss definiert / Spawn fehlgeschlagen |
| `misc.bossportalawoke` | Kacheltext an alle | „Der Stein öffnet sich. <name> ist erwacht!" | Boss beschworen |
| `misc.regionkeyneedsettlement` | Bubble | „Ein Schlüsselstück hält nur dort, wo Menschen wohnen. Stell es in deiner Siedlung auf." | außerhalb Siedlung |
| `misc.regionkeyunlocked` | Kacheltext an alle | „Die <key> setzt sich in den Boden,\nund weit draußen wird ein kalter Stein warm." | Schlüsselstück gesetzt |

### 4.5 Menü-Dialoge (Dialogoptionen)

| Wer | Option (Schlüssel → Text) | Folgedialog | Wirkung |
|---|---|---|---|
| Therapeut | `misc.swhtherapyoption` „Über die Therapieplätze" | `swhtherapyintro` „Ich habe vier Plätze. Wer auf einem sitzt, fühlt sich <percent>% wohler mit dem Leben hier – solange der Platz ihm gehört." · `swhtherapyslot` „Platz <number>: <name>" / `swhtherapyslotfree` „Platz <number>: frei" · `swhtherapypick` „Wer soll den Platz bekommen?" · `swhtherapyclear` „Platz freigeben" | Platz vergeben → Gedanke `misc.swhtherapythought` „Mir hört jemand zu" |
| Therapeut | `misc.swhtraitoption` „An jemandes Wesen arbeiten (<price> Münzen)" | `swhtraitintro` „Das Wesen ist kein Schicksal. Für <price> Münzen nehme ich jedem hier eine Eigenschaft ab – aber was stattdessen nachwächst, wählt weder ich noch du." · `swhtraitwhich` „Welcher Teil von <name> soll gehen?" · `swhtraitconfirm` „Ich nehme <name> die Eigenschaft <trait> ab, für <price> Münzen. Was dafür zurückkommt, weiß keiner von uns beiden vorher." · `swhtraitdoit` „Mach es" · `swhtraitdone` „<name> ist nicht mehr <old>. <name> ist jetzt <new>." · `swhtraitnocoins` „Das sind <price> Münzen, und die hast du nicht." · `swhtraitnotrait` „In ihm ist nichts mehr, woraus ich das machen könnte. Behalt dein Geld." | 50 000 Münzen, Wesenszug zufällig ersetzt |
| Arzt | `misc.swhdoctoroption` „Flick mich zusammen (<price> Münzen)" | `swhdoctorintro` „Setz dich. <price> Münzen, und du gehst hier heil wieder raus – halbe Sachen mache ich nicht, und Raten auch nicht." · `swhdoctordoit` „<price> Münzen zahlen" · `swhdoctordone` „Fertig. Versuch, es eine Stunde lang so zu lassen." · `swhdoctorfine` „Dir fehlt nichts. Heb dir die Münzen für den Tag auf, an dem doch was fehlt." · `swhdoctorcure` „Die Gebissenen behandeln (<price> Münzen)" · `swhdoctorcured` „Bis morgen früh sind sie wieder sie selbst. Und füttert den da." | 100 Münzen: volle LP; bzw. Blutfieber von allen Gebissenen entfernen |
| Veteran | `misc.swhveteranoption` „Verteidigungsausbau (Stufe <level>/5)" | `swhveteranintro` „Die Verteidigung der Siedlung ist auf Stufe <level> von <max>.\nNächste Stufe braucht: <req>." · `swhveterandoit` „Munition übergeben" · `swhveteranmax` „Die Verteidigung ist bereits auf der höchsten Stufe. Da ist nichts mehr zu übergeben." | Stufe +1 (Tabelle §3.14) |
| Magpie | Himmelsfahrt-Menü | siehe §3.2 | Expedition |

### 4.6 Schilder an Orten

`misc.swhshrinesign` „...und die Treppe steht, wo der Himmel am dünnsten ist,\nund der Hüter zählt, was herunterkommt.\n\n- der Rest ist verwittert -" ·
`misc.swhaeronautsign` „Ballon runter. Fracht größtenteils heil.\nGehe nach Osten. Wer das hier findet: Die Kisten gehören dir.\nVorsicht bei denen, die noch warm sind." ·
`misc.swhsigntreasury` „HIMMELSMÜNZE - ÖFFENTLICHE SCHATZKAMMER\nFür alle Bürger geöffnet.\nBitte nehmen Sie nur, was Ihnen gehört.\n\n(Alle Truhen werden täglich geprüft. Manche prüfen zurück.)" ·
`misc.swhsignobservatory` „OBSERVATORIUM GESCHLOSSEN\nDie Kuppel ist nicht standsicher.\nDer Instrumentenkeller wurde zu Ihrer Sicherheit versiegelt.\nIm Instrumentenkeller befindet sich nichts von Wert." ·
`misc.swhsignossuary` „Geh den Weg, den die Pilger gingen:\nlangsam, in der Mitte, und nie auf die hellen Steine.\nWas hier gegeben wurde, bleibt hier." ·
`misc.swhsignfeast` „Sie sind herzlich eingeladen.\nDas Fest dauert schon eine Weile.\nBitte nehmen Sie Platz. Die Plätze finden Sie." ·
`misc.swhsigndoors` „EINE DAVON IST EINE TÜR.\n- die Direktion"

### 4.7 Letzte Worte von Gegnern

`misc.butlerlastwords1..3` „Unhöflich." / „Das Haus wird davon erfahren." / „Ich war mit der Runde fast fertig." · `misc.bridelastwords` „Du kommst zu spät." · `misc.chairlastwords` „Endlich saß jemand still."

---

## 5. Alle Items

Legende Herkunft: **R** = Rezept (Station), **D** = Drop, **K** = Kiste/Ort,
**S** = Laden, **Q** = Quest. Tooltips `itemtooltip.<id>tip`, wörtlich.

### 5.1 Waffen

| ID | DE-Name | Tooltip (DE) | Werte (Schaden Basis → voll geschmiedet) | Herkunft | Verwendung |
|---|---|---|---|---|---|
| `tempestedge` | Sturmklinge | „Aus Himmelsmetall geschmiedet, leicht wie der Wind" | Schwert 156 → 182, Reichweite 80, Verzauberung 1 900 | R Wolfram-Werkbank: 8 Aetheriumbarren + 5 Sturmsplitter · Q `swh_keysteinfeld` | — |
| `galehowl` | Windheuler | „Mit Windseide bespannt – ihre Pfeile überholen den Sturm" | Bogen 145 → 178, Reichweite 800 | R Wolfram-Werkbank: 4 Aetheriumbarren, 6 Windseide, 3 Aurorablatt · Q `swh_keyeden` | — |
| `skyreave` | Himmelsreißer | „Fegt einen ganzen Kreis. Damit schnitt die Himmelswacht den Hagel." | Gleve 150 → 189,6, EPISCH | R Wolfram-Werkbank: 8 Aetheriumbarren, 6 Fulgurit, 10 Wolkenholz · K Schatzkammer (30 % einer von 3) | — |
| `thunderhead` | Donnerhaupt | „Ganz durchgezogen - oder gar nicht erst gespannt." | Großbogen 142 → 180,4 | R: 10 Aetheriumbarren, 8 Windseide, 4 Fulgurit, 12 Seraphholz · K | — |
| `prismcaller` | Prismenrufer | „Der Splitter erinnert sich an jede Dämmerung, die er durchstand." | Stab 118 → 180,7, Reichweite 720 | R: 4 Aetheriumbarren, 10 Prismensplitter, 6 Sturmsplitter, 8 Prismenholz | — |
| `skywatchwhistle` | Himmelswacht-Pfeife | „Ein Ton, und ein Stück der alten Maschinerie antwortet." | Beschwörung (Wachtfunke) 150 → 175, 1 Slot | R: 6 Aetheriumbarren, 8 Sturmsplitter, 6 Windseide | — |
| `stormdisc` | Sturmscheibe | „Sie kommt zurück. Sie kam immer zurück." | Wurfscheibe 150 → 175, Stapel 4 | R ×4: 8 Aetheriumbarren, 8 Sturmsplitter, 4 Glutperle, 3 Schleier-Essenz · Q `swh_keyskyreach` (4×) · K | — |
| `cloudglaive` | Wolkengleve | „Die Gleve der Himmelswacht selbst. Wo die Klinge der Kryokönigin Frost hinterlässt, hinterlässt diese Tageslicht. Geschenkt, nie geschmiedet." | Gleve 165 → 208,5, EPISCH | Q `swh_anchor` · S Wächter 4 500 (nach Anker) | — |
| `skylance` | Himmelslanze | „Halte sie, und der Himmel hält dir eine Linie aus Licht. Solange sie brennt, stehst du still." | Strahl 95 → 149,6 pro Treffer | R Wolfram-Werkbank: 3 Ätherwerker-Gehäuse, 10 Edenbronzebarren, 8 Goldener Pollen, 6 Aetheriumbarren | — |
| `spiritsteelreaver` | Geisterstahl-Schnitter | „Eine zweihändige Klinge aus Geisterstahl. Sie schwingt, als hielte sie noch jemand anderes." | 176 → 219, Verzauberung 2 400 | Q `swh_keyghostrealm` · S Geisterführer · D Geister-Elite 4 % · K Spukhaus 20 % | — |
| `gravewindbow` | Grabwind-Bogen | „Ein Geisterstahl-Bogen, bespannt mit Seelenfaden. Das Spannen ist völlig lautlos." | 105 → 130, Verzauberung 2 400 | Q `swh_keycrookedbeyond` · S Geisterführer · D 4 % · K 20 % | — |

Beschworener Mob: `watchmote` „Wachtfunke".

### 5.2 Rüstung und Kleidung

| ID | DE-Name | Tooltip | Werte | Herkunft |
|---|---|---|---|---|
| `stormsteelhelmet` | Sturmstahl-Helm | „Kopfrüstung - Sturmstahlplatte mit Sturmglas-Visier. Am Wolframamboss geschmiedet." | Rüstung 26 | R Wolfram-Amboss: 8 Sturmstahl, 10 Sturmglas, 4 Himmelsgewebe |
| `stormsteelchestplate` | Sturmstahl-Harnisch | „Brustrüstung - ein Sturmstahl-Harnisch mit Sturmlinse auf der Brust. …" | 29 | R: 11 Sturmstahl, 14 Sturmglas, 6 Himmelsgewebe |
| `stormsteelboots` | Sturmstahl-Beinschienen | „Fußrüstung - Sturmstahl-Beinschienen über Himmelsgewebe-Futter. …" | 19 | R: 6 Sturmstahl, 8 Sturmglas, 3 Himmelsgewebe |
| (Set-Bonus) | — | — | +30 Resilienz, +10 % Tempo | — |
| `spiritsteelhelmet` | Geisterstahlkrone | „Kopfrüstung - eine kalte Krone aus Geisterstahl." | 31 | R Geisterschmiede: 8 Geisterstahl, 4 Seelenfaden |
| `spiritsteelchestplate` | Geisterstahlplatte | „Brustrüstung - schwere Platte aus Geisterstahl." | 34 | R: 12 Geisterstahl, 6 Seelenfaden |
| `spiritsteelboots` | Geisterstahlbeinschienen | „Beinrüstung - Schienen, die auf Stein kaum ein Geräusch machen." | 24 | R: 6 Geisterstahl, 3 Seelenfaden |
| (Set-Bonus) | — | — | +40 Resilienz, +10 % Tempo | — |
| `glimmerstrides` | Schimmerschritt-Stiefel | — (kein Tooltip-Schlüssel) | 7 Rüstung, +6 % Tempo, +0,4 Regen | R Wolfram-Amboss: 8 Auroravlies, 2 Aetheriumbarren |
| `skywatchhood` / `wardenmantle` / `wardenboots` | Himmelswacht-Kapuze / Mantel des Hüters / Stiefel des Hüters | — | **0 Rüstung** (kosmetisch) | Q Schlüssel Himmelsweite / Eden / Steinfeld |
| `magpiecap` | Kurierkappe | „Magpies eigene Schirmkappe. Keine Rüstung, und sie verkauft dir ihre zweite." | 0 | S Magpie |
| `haldakerchief` | Kellermeisterin-Haube | „Haldas weiche Kellerhaube, von vierzig Jahren Bottichen rosa geworden." | 0 | S Halda |
| `vanecowl` | Werkmeister-Reif | „Ossian Vanes Arbeitsreif, mit der Lupe, die er nie abnimmt." | 0 | S Ossian |
| 11 Kostüme ×3 (`<outfit>head/chest/boots`) | z. B. Zwielicht-Perücke / Gestreifter Jenseits-Anzug / Abgewetzte Streifenschuhe | — | 0 | S Zwielichtiger Händler |

Kostüme: `twilightsuit` (Zwielicht-Perücke, Gestreifter Jenseits-Anzug, Abgewetzte Streifenschuhe) · `skeleton` (Grinsender Schädel, Rippen-Torso, Knochenbeine) · `grinclown` (Grinseclown-Gesicht, Rüschen-Clownsanzug, Clownsschuhe) · `hockeyslasher` (Schlitzer-Hockeymaske, Zerrissene Schlitzerjacke, Schlitzerstiefel) · `dreamstalker` (Traumjäger-Hut, Geringelter Albtraumpullover, Traumjäger-Hose) · `screamrobe` (Schreimaske, Zerfetzte Schreikutte, Schattenfüße) · `pincushion` (Nadelkissenkopf, Lederrobe mit Haken, Lederstiefel mit Haken) · `widowgown` (Witwenschleier, Witwenkleid, Spitze Witwenschuhe) · `stitchedmonster` (Genähter Monsterkopf, Flickenjackett, Monster-Plateaustiefel) · `pumpkinscarecrow` (Kürbis-Sackkopf, Stroh-Vogelscheuchenhemd, Strohstiefel) · `hauntedpuppet` (Verfluchter Puppenkopf, Puppen-Latzhose, Puppen-Turnschuhe).

### 5.3 Schmuck / Trinkets

| ID | DE-Name | Tooltip | Wirkung | Herkunft |
|---|---|---|---|---|
| `stormsteelvambrace` | Sturmstahl-Armschiene | „Accessoire - eine geschiente Sturmstahl-Armschiene, über Himmelsgewebe geschnürt." | +50 % Resilienzgewinn, +50 max. Resilienz | R Wolfram-Werkbank: 6 Sturmstahl, 4 Himmelsgewebe, 2 Aetheriumbarren · Q Geister-Schlüssel · K |
| `auroralocket` | Auroramedaillon | „Accessoire - ein Aurorablatt unter Sturmglas an einer Schnur aus Auroravlies." | +50 max. LP, +1 Kampf-Regen | R: 10 Aurorablatt, 6 Auroravlies, 4 Sturmglas · Q Krummer Schlüssel · K Observatorium 20 % |
| `zephyrharness` | Zephyr-Gurtzeug | „Accessoire - ein Windseiden-Flugband mit Sturmflaum, in Sturmstahl verschnallt." | +20 % Tempo, +50 % Ausdauer | R: 12 Windseide, 8 Auroravlies, 4 Himmelsgewebe · Q `swh_crookeddoor` · K |
| `skywatchsignet` | Siegelring der Himmelswacht | „Das Siegel des Haushalts. Wer ihn trägt, dem versteckt der Himmel seine Bauten nicht mehr." + „Zeigt beim Gehen mehr von der Welt und hebt hervor, was sich zu öffnen lohnt" | erweiterte Kartenaufdeckung + Schatzsucher (schließt Piratenfernrohr aus) | K Grange-Keller (Schrank) |

### 5.4 Materialien

| ID | DE-Name | Tooltip (gekürzt, wörtlicher Anfang) | Herkunft | Verwendung |
|---|---|---|---|---|
| `skystone` | Himmelsstein | „Mineral - der helle Stein, aus dem jede Himmelsinsel besteht. …" | Himmelsstein-Felsen, Golem, Kisten, Sternenfall | viele Rezepte, Anker 80×, Magpie kauft |
| `aetheriumore` | Aetheriumerz | „Erz - türkises Metall in den Adern der Himmelsweite. …" | Erzader im Himmelsstein, Golem, Nebelschlange | Barren (Schmiede 3:1, Ätherschmiede 2:1), Sturmstahl (4 + 1 Sturmsplitter) |
| `aetheriumbar` | Aetheriumbarren | „Metallbarren - geschmolzenes Aetheriumerz. …" | R, Kisten | fast alle Himmelswaffen, Anker 20× |
| `stormshard` | Sturmsplitter | „Mineral - ein Splitter aus einem Sturmkristall im Sturmschleier, der noch immer summt." | Sturmkristall, Sturmirrlicht, Raureif-Wächter, Nebelschlange | Waffen, Sturmstahl, Schlüssel Himmelsweite 10× |
| `stormsteelbar` | Sturmstahlbarren | „Metallbarren - Aetherium, in der Aetheresse in Sturmsplitter abgeschreckt. …" | **nur** R Ätherschmiede · S Halda · Quest-Belohnungen · K | Sturmstahl-Set, Armschiene, Anker 8× |
| `stormglass` | Sturmglas | „Mineral - Fulgurit, im Sturmglasofen über Himmelssteinfluss …" | R Sturmglas-Ofen (2 Fulgurit + 1 Himmelsstein → 2) · S Halda · D Prototyp Neun | Sturmstahl-Set, Medaillon, Möbel |
| `fulgurite` | Fulgurit | „Mineral - Sand, den ein Blitzschlag im Boden des Sturmschleiers zu Glas verschmolzen hat." | Fulgurit-Vorkommen, Raureif-Wächter | Sturmglas, Waffen, Schlüssel Himmelsweite 5× |
| `prismshard` | Prismensplitter | „Mineral - Kernholz einer Prismenbirke, zu Kristall geworden. Aus den Aurora-Untiefen gebrochen." | Prismensplitter-Ader, Auroraflocke, Morgenstecher | Prismenrufer |
| `cinderpearl` | Glutperle | „Mineral - eine Glut, die vergessen hat zu erlöschen, aufgelesen in der Aschenweite. Brennt grün." | Aschengebein, Moorgeist, Aschekantor | Sturmscheibe, Käferwand/-tür |
| `windsilk` | Windseide | „Faser - aus geerntetem Windweizen gesponnen oder einem Wolkenschaf abgeschoren." | R Hand (3 Windweizen) oder Webstuhl (2), viele Himmels-Mobs, Wächter-Intro 6× | viel |
| `skyweave` | Himmelsgewebe | „Stoff - ein Ballen Windseide, von einem Siedler am Windseiden-Webstuhl gewebt." | R Webstuhl (3 Windseide) · S Halda | Rüstung, Trinkets, Möbel |
| `aurorapetal` | Aurorablatt | „Blütenblatt - von den Aurorablüten der Untiefen gepflückt …" | Aurorablüte, Auroraflocke | Leckerli, Waffen, Medaillon |
| Hölzer `cloudwood`, `seraphwood`, `nimbuswood`, `charwood`, `prismwood` | Wolkenholz, Seraphholz, Nimbusholz, Kohlenholz, Prismenholz | „Holz - von einem Wolkenbaum in den Driftlanden …" usw. | die jeweiligen Bäume | zählen als „jedes Holz"; Böden, Waffen, Webstuhl |
| `veilessence` | Schleier-Essenz | „Kreaturenbeute - das, woraus ein Düsterschemen besteht. …" | Düsterschemen, Moorgeist, Aschekantor, Käferlande-Kisten, Grabheide 18 % | Caspern-Quest 8×, Eleanor 12×, Seelenbecken, Sturmscheibe, Käferwahn-Boden |
| `dewsnail` | Tauschnecke | „Kleintier - eine Tauschnecke, lebend mit dem Netz gefangen. …" | Kleintier (Netz) | Leckerli (1 + 1 Windseide → 2) |
| `aurorafleece` | Auroravlies | „Hoferzeugnis - Vlies, einer Schimmerziege abgeschoren. …" | Schimmerziege scheren | Seelenfaden, Medaillon, Gurtzeug, Schimmerschritt, Leckerli, Teppich |
| Eden: `edenwood` Edenholz, `edensap` Edensaft, `serpentscale` Schlangenschuppe, `venomfang` Giftzahn, `goldenpollen` Goldener Pollen, `edencopperore` Edenkupfererz, `edenbronzebar` Edenbronzebarren | Tooltips z. B. „Lebendes Harz aus Edens angriffslustigen Pflanzen." | Eden-Mobs, Eden-Kisten; Edenbronze R Wolfram-Werkbank (3 Edenkupfer + 1 Edensaft) | Edensaft/Pollen: Eden-Schlüssel; Edenbronze + Pollen: Himmelslanze; **Edenholz, Schuppe, Giftzahn: keine Verwendung** |
| Steinfeld: `palestone` Blasser Stein, `gravesalt` Grabsalz, `spiritmoss` Geistermoos, `echoshard` Echosplitter | „Heller Baustein - …" · „Eine bittere Mineralkruste, geschätzt in der Alchemie." · „Moos, das weiterwächst, … Wonach eine Séance verlangt." · „Ein Splitter einer Stimme, …" | Steinfeld-Felsen, Mobs, Friedhof/Kapelle | Ives-Quest, Steinfeld-Schlüssel, Ives kauft; **kein Rezept** |
| Geister: `bonewood` Knochenholz, `soulthread` Seelenfaden, `spectralore` Spektralerz, `spiritsteelbar` Geisterstahlbarren | „Holz von einem Baum, der seinen Garten überlebt hat." · „Kalte Faser, aus Ektoplasma und Vlies gesponnen." · „Erz, das mehr Erinnerung als Stein ist." · „Ein Barren, ohne Flamme geschmiedet." | Geisterbäume/-felsen, GhostLoot; Seelenfaden R Seelenwebstuhl (3 Ektoplasma + 2 Auroravlies → 2); Geisterstahl R Geisterschmiede (3 Spektralerz + 2 Ektoplasma) | Geisterstahl-Set, Quests, Schlüssel |
| Krumm: `oddwood` Seltsamholz, `warpresin` Krümmungsharz, `strangefabric` Seltsamer Stoff, `eyeseed` Augensamen, `stripedshell` Gestreifter Panzer, `realityshard` Realitätssplitter | „Ein Stück Holz, das in die falsche Richtung gewachsen ist." · „Klebriger Beweis dafür, dass Wirklichkeit auslaufen kann." · „Stoff, dessen Muster nicht stillhalten will." · „Er beobachtet, wo er besser nicht wachsen sollte." · „Ein harter Panzer in unmöglichen Streifen." · „Ein scharfer Splitter einer Regel, der die Welt nicht mehr folgt." | Krumme Objekte/Mobs/Kisten, Höllen-Mobs | Knott-Quest, Krummer + Höllen-Schlüssel, Knott kauft; **kein Rezept; Gestreifter Panzer ohne jede Verwendung** |
| Trophäen `skystoneheart` Himmelsstein-Herz, `bloomfang` Blütenzahn, `mourningband` Trauerflor, `soulcollar` Seelenhalsband, `stripedhorn` Gestreiftes Horn | „Trophäe - … Magpie zahlt über Wert." | Golem 12 %, Blütenrachen, Steinerner Trauernder 12 %, Trauerbraut 12 %, Türmimik 20 %; Schatzorte | nur Verkauf an Magpie |
| `aetherwrightcasing` | Ätherwerker-Gehäuse | „Eine gefräste Hülle, die Ladung hält. Vane weiß, was damit anzufangen ist." | Testgelände 2–4, Prototyp Neun 1–2 | Himmelslanze (3×) |
| `bloodvial` | Blutfläschchen | „Kreaturenbeute - vor einer Stunde hat das noch gegrast. Noch warm, und darüber redet niemand gern." | Dorians Jagd, Blutknecht, Alchemietisch (rohes Fleisch + Glasflasche) | Alchemietisch |

### 5.5 Schlüssel- und Quest-Items

| ID | DE-Name | Tooltip | Herkunft | Verwendung |
|---|---|---|---|---|
| `ghostchalk` | Geisterkreide | „Aus Grabsalz gemahlen. Reicht für einen Kreis, und nur innerhalb einer Siedlung." | Wächter (1× pro Charakter nach Nebel), S Wächter 1 200 | platziert den Séance-Zirkel; Abbau des Zirkels gibt die Kreide zurück |
| `bondedlockbox` | Verzollte Kassette | „Versiegelt, verzollt, nie abgeholt. Magpie will sie zurück." | K Zollhaus-Tresor | Magpie-Anwerbung |
| `themother` | Die Mutter | „Die lebende Kultur des Kellers. Älter als der Haushalt, der sie fütterte." | D Sauerbottich-Blüte | Halda-Anwerbung |
| `stormlenscore` | Sturmlinsenkern | „Läuft immer noch an. Was er antreiben sollte, hat niemand abgestellt." | D Prototyp Neun | Ossian-Anwerbung |
| `skywaywrit` | Himmelsweg-Freibrief | „Wegerecht auf jeder Straße der Himmelswacht, zweimal gegengezeichnet." | K Zollhaus | **keine Wirkung im Code** |
| `postledger` | Buch der unzustellbaren Post | „Jedes Paket, das die Straße nie zustellte, von Hand aufgeführt. Magpie zahlt dafür." | K Zollhaus | **keine Wirkung; Magpie kauft es nicht** |
| `silverbell` | Silberglöckchen | „Sie war Siggis. Er hasst sie" (Duplikat-Schlüssel, zweiter gewinnt vermutlich: „Ein Andenken aus dem Turm des Hüters. Es öffnet nichts mehr.") | **nirgends** (ausgemustert) | keine |
| Schlüsselstücke `regionkey*` | Wachfeuer der Himmelsweite · Gartenstiege von Eden · Trauerengel von Steinfeld · Rabenkanzel des Nachgartens · Knotts Krumme Tür · Das Höllensiegel | — | Q Region-Schlüssel | in Siedlung setzen → Rufsteine |
| `adventurersjournal` (nachgetragen 2026-09-24) | Abenteurer-Tagebuch | „Hält deine Reise durch die Reiche fest: …" | beim ersten Moment in der Himmelsweite (einmal pro Charakter, auch rückwirkend für alte Spielstände; `journal/AdventurerJournal.maybeGiveJournal`), S Wächter 100 | Rechtsklick im Inventar / Benutzen → Tagebuch-Fenster (`journal/JournalForm`), liest nur, schreibt nichts |

### 5.6 Nahrung und Tränke

| ID | DE-Name | Tooltip | Wirkung | Herkunft |
|---|---|---|---|---|
| `cloudberry` | Wolkenbeere | (Item-Abschnitt `cloudberrytip` „Pralle Himmelsbeeren - süß, kalt und mit einem Hauch Gewitter." — vermutlich ungenutzt) | Getreide-Nahrung, verdirbt | Wolkenbeeren-Busch, Kisten, S Halda |
| `cloudpufftreat` | Wolkenzupf-Leckerli | „Tierleckerli - aus Windseide und Blütenblättern gerollt. Siggi und Peanut folgen dir dafür überallhin." (Duplikat: „Unwiderstehlich für Himmelskatzen. Riecht schwach nach Gewitter") | Katzenköder | R Hand: 1 Windseide + 2 Aurorablatt → 3 · 1 Tauschnecke + 1 Windseide → 2 · 1 Auroravlies → 4 · S Halda · Himmels-Kisten 12 % |
| `nimbusmilk` | Nimbusmilch | „Nahrung - einem Nimbusyak mit dem Eimer abgemolken. …" | +15 max. LP | Nimbusyak melken |
| `skycurd` | Himmelsquark | „Nahrung - Nimbusmilch, in der Käsepresse gepresst. …" | +25 max. LP | R Käsepresse (1 Milch) |
| `cloudcustard` | Wolkenbeeren-Creme | „Nahrung - Nimbusmilch und Wolkenbeeren im Kochtopf. Macht danach flink auf den Beinen." | +6 % Tempo, +4 % Angriffstempo | R Kochtopf (2 Milch + 3 Wolkenbeeren) |
| `nimbusdraught` | Nimbustrunk | „Trank - Nimbusmilch mit einem Aurorablatt gebraut. Macht die Haut fest gegen einen tiefen Sturz." | +6 Rüstung, +20 Resilienz | R Alchemie (2 Milch + 1 Aurorablatt) |
| `wardensround` | Die Runde des Wächters | „Ausgeschenkt für die Wache am Ende einer Schicht. Ein Fass war noch übrig." | 1 800 s: +60 max. LP, +12 Rüstung, +40 Resilienz | K Grange-Keller (1×) |
| Eden-Früchte `edenberry` Edenbeere, `moonmelon` Mondmelone, `sungrape` Sonnentraube, `paradiseapple` Paradiesapfel, `paradisecoconut` Paradieskokosnuss | „Eine juwelenhelle Beere aus Eden." usw. | als Material registriert (`GhostMatItem`), **nicht essbar** (HYPOTHESE aus Klassenname) | Eden-Kisten, POIs; Paradiesapfel auch Baum der Fülle | nur die drei Quest-Früchte haben eine Verwendung |

### 5.7 Werkstationen und Tore (platzierbar)

| ID | DE-Name | Tooltip | Rezept | Wirkung |
|---|---|---|---|---|
| `skystairwaydown` | Stairway to Heaven | „Ermöglicht den Aufstieg in die Himmelsweite" | Wolfram-Werkbank: 8 Wolframbarren, 15 Quarz | Aufstieg |
| `windsilkloom` | Windseiden-Webstuhl | „Spinnt Windweizen und webt Windseide. …" | Werkbank: 12 Wolkenholz, 4 Eisenbarren, 4 Windseide | Windweizen 2 → 1 Windseide; 3 Windseide → 1 Himmelsgewebe |
| `aetherforge` | Ätherschmiede | „Verbrennt Holz und läutert Ätheriumerz mit doppelter Ausbeute einer gewöhnlichen Schmiede - und nur hier entsteht Sturmstahl" | Werkbank: 20 Himmelsstein, 8 Eisenbarren, 4 Sturmsplitter | 2 Erz → 1 Barren; 4 Erz + 1 Sturmsplitter → 1 Sturmstahl |
| `stormglasskiln` | Sturmglas-Ofen | „Brennt Fulgurit und Himmelsstein zu Sturmglas. …" | Werkbank: 16 Himmelsstein, 4 Eisenbarren, 4 Fulgurit | 2 Fulgurit + 1 Himmelsstein → 2 Sturmglas |
| `soulloom` | Seelenwebstuhl | „Werkstation - spinnt Seelenfaden für Siedler und Spieler." | **keins** | Seelenfaden |
| `spiritforge` | Geisterschmiede | „Werkstation - verhüttet Spektralerz und schmiedet Geisterstahl ohne Feuer." | **keins** | Geisterstahl + Set |
| `edenseedbasin` | Edenschwelle | — | Wolfram-Werkbank: 4 Aetheriumbarren, 10 Himmelsstein, 8 Windweizen | + 6 Eden-Samen → Edentor |
| `soulbasin` | Seelenbecken | — | Wolfram-Werkbank: 12 Ektoplasma, 6 Schleier-Essenz, 8 Knochen | + 12 Ektoplasma → Geistertor |
| `seancecircle` | Séance-Zirkel | (Item-Abschnitt `seancecircletip` „Zeichne den Kreis, läute das Silberglöckchen - und klopfe an." — veraltet) | über Geisterkreide | Geisterführer beschwören |
| `catbasket` | Katzenkorb | — | keins (Q `swh_cats`, S Wächter 500) | Zuhause der Katzen |
| `veteranbarricade`, `barbedwirefence`, `veteranturret`, `veterancatapult` | Barrikade, Stacheldrahtzaun, Selbstschussanlage, Kanone | — | keins (S Veteran) | Verteidigung, Schaden skaliert mit Veteranenstufe |

### 5.8 Möbel und Deko

**Himmelswacht-Möbel** (Werkbank): Stuhl (4 Himmelsstein + 1 Eisen), Bank (6 + 2), Tisch (6 + 1), Esstisch (10 + 2), Schreibtisch (8 + 2), Kommode (8 Himmelsstein + 2 Windseide), Bett (6 + 8 Windseide), Kandelaber (2 Eisen, 4 Himmelsstein, 1 Aurorablatt), Windseiden-Teppich (6 Windseide; oder Zimmermann: 1 Auroravlies → 2), Kelch (1 Eisen + 1 Sturmsplitter), Windseiden-Kerze ×2 (1 Windseide + 1 Aurorablatt), Foliant (2 Windseide + 2 Himmelsstein), Wolkenbeere im Topf (3 Himmelsstein + 2 Wolkenbeeren), Bücherregal / Schrank (8 Himmelsstein, 6 Wolkenholz, 2 Himmelsgewebe), Uhr (6 Himmelsstein, 2 Eisen, 2 Sturmglas), Vitrine (6 Himmelsstein, 3 Sturmglas).

**Bau** (Werkbank): Himmelsstein-Ziegelwand ×4 (2 Himmelsstein), -tür (3), Nightfell-Wand ×4 / -Tür (2 Himmelsstein + 1 Sturmsplitter), Käferwahn-Wand ×4 (4 Schleierfels + 1 Glutperle) / -Tür (6 + 2), Wolkenmarmor-Wand ×4 (4 Himmelsstein, 1 Windseide, 1 Goldbarren), -Tür, -Geländer ×4, -Tor, Himmelseisen-Zaun ×4 / -Tor, Seraphstatue (20 Himmelsstein, 4 Gold, 2 Aetherium). Böden ×6: Schachbrett-Marmor, Düsterholz, Nimbus-, Kohlen-, Prismenholz; Käferwahn-Boden ×5 (Wolfram-Landschaftsbau: 5 Stein + 2 Schleier-Essenz).

**Deko** (Werkbank): Geisterlaterne, Kandelaber des Hüters, Nebelglas-Laterne, Düsterraben-Statue, Düsterweide, Fernrohr, Astrolab, Sturmversengter Boden, Zerbrochener Himmelswacht-Stein ×2, Geladener Kristall, Abgestorbener Strauch, Aurora-Splittercluster, Sternschnuppen-Fragment, Verlorener Wetterballon, Luftschiffer-Wrack, Himmelspost-Paket.

**Nur aus Quest/Laden:** Flackerlicht-Girlande, Himmelswacht-Banner.

**Zwielichtiger Händler** (registriert nur, wenn die Grafik existiert, `TwilightWares.java`): Sargbett, Spukuhr, Galgenbaum, Totenkopf-Kandelaber, Händchen-Kiste, Zwielicht-Hexenkessel, Sandwurm-Grabstein, Elektrischer Stuhl, Auge in der Wand, Verdrehte Wanduhr, Spukspiegel, Grinsender Sarkophag. **Hängende Schrumpfköpfe** und **Grinsende Vogelscheuche** haben keine PNG → nicht registriert, erscheinen nie (§8). Ausgemusterte Gemälde Wachendes Auge / Schrumpfkopf-Trophäe bleiben registriert, werden nicht verkauft.

**Friseursalon** (Stylistin): siehe 3.15.

### 5.9 Samen, Pflanzen, Tiere

| ID | DE-Name | Herkunft | Verwendung |
|---|---|---|---|
| `overgrownedenseed` | Eden-Grassamen | S Eveleen, Eden-Gras abbauen 4 %, Himmels-Kisten 8 %, Stille Wiese 22 % | pflanzt Eden-Gras auf **Erde oder Wolkengras**; 6 × Edenschwelle; 9 Kacheln → Eveleen |
| `cloudberrysapling` | Wolkenbeeren-Setzling | S Eveleen | Wolkenbeeren-Busch |
| Setzlinge `nimbussapling`, `fulgursapling`, `prismasapling`, `skyseraphsapling`, `cloudsapling` | Nimbus-/Fulgur-/Prismen-/Himmelsseraph-/Wolkenbaum-Setzling | vom Baum | Bäume |
| `knowledgecutting` | Wissenssteckling | Q Eveleen (3×), Verbotene Schlange, Eden-Kisten 6 % | Tooltip „Ein lebender Steckling vom Baum der Erkenntnis." — **nicht pflanzbar, keine Verwendung** (Material-Klasse) |
| `windwheat` (Objekt/Item) | Windweizen | Driftlande (Gras), Kisten | Windseide |
| Nutztiere `nimbusyak` Nimbusyak, `glimmergoat` Schimmerziege | Herden 2–5 in Driftlanden (8,5 %/Region) bzw. Aurorabänken (7 %) | Milch / Vlies; fressen Wolkenbeeren oder Weizen. Tips `misc.nimbusyaktip`, `misc.glimmergoattip` |

---

## 6. Bosse, Rufsteine, Tore, Gegner

### 6.1 Bosse (`bosses/SkyBossLadder.java`)

Vanilla-Bosse, im Stein beschworen (Abstand bis 960 px), skaliert über einen
unsichtbaren Buff. **Beute: unverändert die Vanilla-Beute des Bosses** (der Mod
setzt keine eigene Loot-Tabelle; was genau das ist, habe ich nicht geprüft).

| Reich | Boss | Stein (DE) | Basis-LP | Stufe | LP-Faktor | **End-LP** | Schadensfaktor | Freischaltung |
|---|---|---|---|---|---|---|---|---|
| Himmelsweite | Cryo-Königin `cryoqueen` | Rufstein der Himmelsweite | 18 000 | 8 | 3,18 × 1,30 | **74 412** | 1,87 × 1,15 = 2,15 | Wachfeuer in Siedlung |
| Eden | Mondlichttänzerin `moonlightdancer` | Rufstein von Eden | 40 000 | 8 | 3,18 × 1,35 | **171 720** | 2,21 | Gartenstiege |
| Steinfeld | Aufgestiegener Magier `ascendedwizard` | Rufstein von Steinfeld | 44 000 | 9 | 3,58 × 1,40 | **220 528** | 2,42 | Trauerengel |
| Geisterreich | Pestwächter `pestwarden` | Rufstein des Nachgartens | 45 000 | 9 | 3,58 × 1,45 | **233 595** | 2,48 | Rabenkanzel |
| Krumm | Kristalldrache `crystaldragon` | Rufstein der Krümmung | 52 000 | 10 | 4,00 × 1,55 | **322 400** | 2,80 | Knotts Krumme Tür |
| Hölle | Mutantenhydra `mutanthydra` | Höllische Rufesse | 80 000 | 10 | 4,00 × 1,65 | **528 000** | 2,92 | Das Höllensiegel |

Rufsteine: unzerstörbar, 0,35 Chance pro 600×600-Zelle, nur wo das Reich gilt.

### 6.2 Tore und Wege

| Objekt | DE | platziert durch | Wirkung | Rückweg |
|---|---|---|---|---|
| Stairway to Heaven | Stairway to Heaven | Spieler (Rezept) | Oberfläche → Himmelsweite | Skywatch-Tor an der Spitze (merkt sich die Treppe pro Spieler) |
| Skywatch-Tor | Skywatch-Tor | Spitze | Himmelsweite → eigene Treppe | — |
| Edentor | Edentor | Edenschwelle | → Landeplatz Eden | `edengateup` „Rückkehrtor" wird **nirgends platziert** |
| Geistertor | Geistertor | Seelenbecken | → Landeplatz Geisterreich | `ghostgateup` nie platziert |
| Krumme Tür | Krumme Tür | **nichts** | → Krummes Jenseits | tot |
| Schleierriss | Schleierriss / Rückweg-Riss | nichts (Altlast) | — | tot |

### 6.3 Gegner und ihre Drops (Kurzfassung)

| Reich | Gegner (DE) | Haupt-Drops |
|---|---|---|
| Himmelsweite | Sturmwindhund, Morgenstecher, Zephyrrochen, Sturmirrlicht, Himmelsstein-Golem, Raureif-Wächter, Auroraflocke, Nebelschlange | Windseide, Wolkenbeere · Aurorablatt, Prismensplitter · Windseide · Sturmsplitter · Himmelsstein, Aetheriumerz, 12 % Himmelsstein-Herz · Fulgurit, Sturmsplitter · Aurorablatt, Prismensplitter · Aetheriumerz, Sturmsplitter |
| Himmelsweite (ortsgebunden) | Zollwerk, Sauerbottich-Blüte, Bottichling, Prototyp Neun, Hortmimik | s. 1.4; Hortmimik = Vanilla-Mimik mit Beute im Bauch |
| Eden | Edenschlange, Blütenrachen, Eifersüchtige Ranke, Goldhornisse, Verbotene Schlange | Schuppe, Giftzahn · Edensaft 80 %, Paradiesapfel, Blütenzahn · Edensaft 85 %, Edenholz · Goldener Pollen 70 %, Giftzahn · Schuppe, Giftzahn, Wissenssteckling |
| Steinfeld | Verlorener Pilger, Steinerner Trauernder, Hohler Engel, Grabkrähe | 15 % Geistermoos, 40 % Echosplitter · Blasser Stein, 30 % Grabsalz, 12 % Trauerflor · 1–3 Echosplitter, 20 % Grabsalz, 45 % Blasser Stein · 25 % Geistermoos, 10 % Echosplitter |
| Geisterreich | Treiber, Kopfloser Butler, Laternenwitwe, Trauerbraut, Besessener Stuhl, Seelenhund, Nebelblüte, Sargkriecher; ex-Schleier: Düsterschemen, Moorgeist, Aschekantor | Ektoplasma, Seelenfaden, Knochenholz, Spektralerz, 20 % Geisterstahl, je 4 % Schnitter/Bogen (Elite), 12 % Seelenhalsband (Braut) · Schleier-Essenz, Glutperle, Knochen |
| Krumm | Türmimik, Zungenpflanze, Streifenkäfer, Krummer Golem, Seltener krummer Golem, Krummes Gürteltier, Gestreifter Riesenhai | Seltsamer Stoff, Krümmungsharz, Augensamen, 20 % Gestreiftes Horn · Seltsamholz, Harz, 12 % Realitätssplitter · Gestreifter Panzer, Harz · Riesenhai: 3 Haischuppen, 6 Wolframbarren |
| Hölle | Höllischer Sachbearbeiter, Aschegeist, Nummernkobold, Kesselhund | Seltsamholz, Realitätssplitter, Kohlenholz (Krumm-Material in Höllen-Mengen) |
| Siedlung | Blutknecht | 1–2 Blutfläschchen, 35 % Leder |

### 6.4 Schatzorte mit Mimiken (Kapitel 02, `worldgen/pois/RealmPoiHoards.java:160-300`)

| Ort | Hauptbeute (Truhe hinter dem Wächter) |
|---|---|
| Falsche Schatzkammer (Himmelsweite) | 4–8 Sturmstahl, 3–6 Aetherium, 3–7 Sturmglas, 900–2 200 Münzen, 30 % Sturmscheibe/Himmelsreißer/Donnerhaupt, 15 % Himmelsstein-Herz |
| Eingestürztes Observatorium (Himmelsweite) | 3–7 Prismensplitter, 4–8 Sturmglas, 2–4 Sturmsplitter, 500–1 400 Münzen, 20 % Auroramedaillon, 15 % Herz |
| Heckenlabyrinth (Eden) | 4–8 Edenbronze, 3–6 Schuppen, 1–3 Giftzahn, 2–5 Pollen, 1 100–2 800 Münzen, 50 % Paradiesapfel, 15 % Blütenzahn |
| Pilger-Ossarium (Steinfeld) | 6–12 Blasser Stein, 4–9 Grabsalz, 2–5 Echosplitter, 3–7 Geistermoos, 1 400–3 500 Münzen, 20 % Trauerflor |
| Hochzeitsmahl (Geisterreich) | 4–8 Geisterstahl, 5–10 Seelenfaden, 4–8 Spektralerz, 6–12 Knochenholz, 1 700–4 200 Münzen, 20 % Seelenhalsband |
| Halle der vielen Türen (Krumm) | 3–6 Realitätssplitter, 5–10 Harz, 5–10 Stoff, 6–12 Seltsamholz, 2 200–5 500 Münzen, 50 % Augensamen, 20 % Gestreiftes Horn |

---

## 7. Trigger- und Flag-Referenz

`/swhreset quests confirm` → `SkywatchQuestData.resetProgress()` +
`SkywatchWorldData.resetProgress(false)` + `VeilWorldData.resetProgress()` + alle
Mod-Quests aus allen Journalen + Wächter zurück an die Spitze.
`/swhreset all confirm` → zusätzlich `residentsClaimed` leeren + `world`-Nachrüstung.
`/swhreset world` → Mobs/Rufsteine im 1 024×1 024-Feld nachrüsten, Spitzenboden reparieren
(`commands/SwhResetCommand.java`).

### 7.1 `SkywatchQuestData` (LevelData der Himmelsweite, Schlüssel `skywatchquest`)

| Feld | gesetzt von | gelesen von | reset `quests`? |
|---|---|---|---|
| `stage` (0/1/2) | Treppe (liest), Wächter-Intro →1, Anwerbung →2 | Treppe (findspire), Wächter | ja |
| `recruited`, `recruitedAuth` | `SkyWardenMob.onRecruited`/`advanceChain` | `chapterFor` | ja |
| `spirePlaced`, `spireX/Y`, `beaconX/Y`, `basketX/Y`, `basketPlaced` | `SkyLevel.ensureWardenSpire` | Leuchtfeuer, Katzen, Richtungsangabe | nein (Gelände) |
| `returnStairs` | Treppe `use()` | Skywatch-Tor | nein |
| `catsSpawned`, `blackLairX/Y`, `tabbyLairX/Y` | `SkyLevel.spawnSpireCats` | Karten-Pins | `catsSpawned` ja, Lager nein |
| `blackHome`, `tabbyHome` | `SpireCatMob.interact` (Leckerli) | `chapterFor`, Quest | ja |
| `catsIntroShown` | (nicht mehr beschrieben) | — | ja |
| `catsRewardGiven` | Katzen-Abgabe | `chapterFor` | ja |
| `anchorIntroShown` | (nicht mehr beschrieben) | — | ja |
| `anchorDone` | Anker-Abgabe | `chapterFor` | ja |
| `finaleShown` | (nicht mehr beschrieben) | — | ja |
| `spireMarkerAuths`, `stairsMarkerAuths`, `catMarkerAuths` | `SkyMapMarkers` | dito (Pins nur einmal senden) | ja |
| `landmarksStamped`, `landmarkGuards`, `landmarkLoot` | `SkyLandmarkPois` | dito | **nein** |

### 7.2 `SkywatchWorldData` (WorldData `skywatchworld`)

| Feld | gesetzt von | gelesen von | reset `quests`? |
|---|---|---|---|
| `wardenRecruited`, `wardenAuth` | Wächter (Anwerbung, Tick als Siedler) | Wächter-Anwerbung (Dublette verhindern), Bewohner-Anreise, Himmelsfahrten | ja |
| `blackHome`, `tabbyHome` | Katze | Wächter-Laden (Korb/Girlande), Abgleich mit Q | ja |
| `catHomeSet`, `catHomeX/Y`, `catHomeLevel` | Katzenkorb aufstellen/abbauen | Katzen-Heimreise | ja |
| `eleanorPassedOn` | Eleanor Loslassen | Platzierung Eleanor | ja |
| `edenPlantsGiven` | Eveleen-Abgabe | Eveleen (Gebühr, Quest), Edentor | ja |
| `crookedDoorwayOpened` | Knott-Abgabe | Knott, Krumme Tür | ja |
| `residentsClaimed` | jede Platzierung/Einzug eines Bewohners | Platzierung, Anreise | **nur `all`** |
| `bossPortalsUnlocked` | Schlüsselstück gesetzt | Rufstein, Himmelsfahrten | ja (**gesetzte Schlüsselstücke bleiben stehen**, wirken aber erst beim nächsten Neusetzen) |
| `regionKeysEarned` | Schlüssel-Abgabe | Wächter | ja |
| `residentChainsDone` = {`steinfeldvigil`, `mortimerrites`, `caspernforge`, `anchorcloudglaive`} | Ives/Mortimer/Caspern-Abgabe, Gleve | Gebühren, Quests, Wächter-Laden | ja |

### 7.3 `VeilWorldData` (WorldData `swhveil`, pro Charakter-Auth)

| Feld | gesetzt von | gelesen von | reset `quests`? |
|---|---|---|---|
| `markAuths` (Nebelmal) | Geisterführer 1. Gespräch, `/veilmark` | Seelenblöße-Tick | ja |
| `fogTouchedAuths` | Seelenblöße-Tick hinter der Wand | Wächter (Kreide) | ja |
| `chalkGivenAuths` | Wächter | Wächter | ja |

### 7.4 Weitere

| Speicher | Inhalt |
|---|---|
| `SkyfallWorldData` (`swhskyfall`) | nächster Sternenfall |
| `JournalWorldData` (`swhjournal`, nachgetragen 2026-09-24) | wer sein Tagebuch schon bekommen hat (pro Auth); was jeder Spieler **gesehen** hat (benannte Bewohner/Wächter in 14 Kacheln, besuchte Reiche), alle 2 s gemessen; welche Bewohner als Siedler gesehen wurden (pro Welt). Nur das Tagebuch liest es; `/swhreset` setzt es nicht zurück |
| `VeteranDefense` (`veterandefense`) | Verteidigungsstufe 0–5 |
| Mob-Speicher | Dorian `bloodthirst`; Therapeut Patienten-Plätze; Rufstein `bossUniqueID` |

### 7.5 Befehle (Admin)

`/swhreset [status|quests|world|all] [confirm]` · `/skyreachstatus …` · `/edenstatus` ·
`/veilstatus` · `/veilmark` (Mal setzen/entziehen) · `/skysurfacestatus` — alles Debug.

---

## 8. Gefundene Unstimmigkeiten

Sortiert grob nach Wichtigkeit für die Überarbeitung. „Code" = was tatsächlich
passiert.

### 8.1 Tote oder unerreichbare Inhalte

1. **`swh_crookedarrival` ist unerreichbar.** Sie wird nur in `CrookedDoorObjectEntity.use()` vergeben; `crookeddoordown` wird von nichts platziert (`CrookedRealm.java:280-296` sagt noch „by using a Seance Circle inside the Beetle Outlands", der Zirkel tut das nicht mehr). Der Text `misc.crookeddooropened` ist ebenfalls tot.
2. **`swh_beacon`** registriert, nie vergeben (Absicht: alte Saves). Texte `swhbeacon*` bleiben sichtbar in der Locale.
3. **Rückweg-Tore** `edengateup`, `ghostgateup`, `crookeddoorup`, `veilriftup` und `veilriftdown` werden nie platziert.
4. **Dorian: Fläschchen hinstellen/füttern ist versprochen, aber nicht gebaut.** `vampiretalk4` „Stellt ein Flaeschchen raus …", Biss-Bubble „Es hat niemand etwas rausgestellt." — im Code gibt es nur `feed()` aus Jagd und Biss; nichts liest ein Blutfläschchen. **Blutfläschchen hat überhaupt keine Verwendung** (kein Rezept, kein Käufer).
5. **Himmelsweg-Freibrief** (`skywaywrit`) hat keinerlei Wirkung (MOD_SUMMARY behauptet „dauerhafte Aufwertung von Magpies Handelsmissionen"). → **BEHOBEN (461b173)** — MOD_SUMMARY nennt ihn jetzt „derzeit ohne Wirkung"; die Wirkung selbst bleibt ungebaut.
6. **Buch der unzustellbaren Post** (`postledger`): Tooltip „Magpie zahlt dafür" — Magpie kauft es nicht (sie kauft `skyparcel`), und es „benennt" nichts. → **BEHOBEN (461b173)** — Tooltip sagt jetzt, dass Magpie die Pakete bezahlt, nicht das Buch.
7. **Silberglöckchen**: nirgends erhältlich; MOD_SUMMARY §3 listet es noch in den Läden von Wächter und Magpie, `quests.md` noch als Belohnung von `swh_recruitwarden`. Der Zirkel-Tooltip `item.seancecircletip` spricht noch vom Läuten. → **TEILWEISE BEHOBEN (461b173)** — `item.seancecircletip` entfernt, MOD_SUMMARY-Läden korrigiert; `quests.md` (Quest-Bereich eines anderen Agenten) noch offen.
8. **Inselanker** (`skyanchor`) wird nie platziert — die Anker-Quest verankert sichtbar nichts.
9. **Seelenwebstuhl und Geisterschmiede haben kein Rezept und keinen Verkäufer.** Damit sind Seelenfaden (außer Drops), Geisterstahl (außer Drops/Läden) und **das ganze Geisterstahl-Rüstungsset** nicht herstellbar. (Ob sie in einem POI als abbaubares Objekt stehen, habe ich nicht geprüft → HYPOTHESE „unerreichbar".) → **BEHOBEN (5f45645)** — Werkbank-Rezepte für Geisterschmiede und Seelenwebstuhl.
10. **Materialien ohne jede Verwendung:** Edenholz, Schlangenschuppe, Giftzahn, Paradiesapfel, Paradieskokosnuss, Gestreifter Panzer, Wissenssteckling, Blutfläschchen. Die fünf Trophäen gehen nur an Magpie (Absicht). Steinfeld- und Krumm-Materialien haben kein einziges Rezept. → **TEILWEISE BEHOBEN (461b173)** — Edenholz trägt jetzt `anylog` (wie die fünf Himmelshölzer) und taugt für jedes Holzrezept. Die übrigen haben keine natürliche Verwendung ohne neue Inhalte; Blutfläschchen gehört zu Dorians Mechanik (anderer Agent).
11. **Wissenssteckling** heißt „lebender Steckling", ist aber ein reines Material (nicht pflanzbar) — HYPOTHESE aus der Klasse `GhostMatItem`.
12. **Hängende Schrumpfköpfe** und **Grinsende Vogelscheuche**: Locale-Namen vorhanden, PNG fehlt → Objekt wird nicht registriert, erscheint nie.
13. **Ungenutzte Wächter-Texte:** `wardenrecruit2`, `wardenrecruitconfirm`, `wardenrecruitwait`, `wardenrecruitdone1/2`, `wardenrecruitnohome`, `wardenfarewell`, `wardenshopclosed`. → **BEHOBEN (461b173)** — alle acht Schlüssel in beiden Sprachen entfernt (kein Leser, auch kein zur Laufzeit gebauter Schlüssel).
14. **Vermutlich ungenutzt:** `item.seancecircletip`, `item.ghostlanterntip`, `item.cloudberrytip` (im `[item]`-Abschnitt statt `[itemtooltip]`, kein Code liest sie) — HYPOTHESE. → **BEHOBEN (461b173)** — bestätigt ungelesen (`ItemDescription` liest nur `itemtooltip`, Vanilla-Nahrung hat keine eigene Zeile); entfernt.
15. `misc.bossportalsilent` („Was hier schlief, hat noch keinen Namen") kann nur noch bei einem Spawn-Fehler erscheinen, da jetzt jedes Reich einen Boss hat.

### 8.2 Texte, die Falsches behaupten

16. **Anwerbepreis des Wächters:** Code 30 000; `wardenrecruit2` sagt „Hunderttausend Münzen" (ungenutzt), Code-Kommentare in `SkyWardenMob.canTakeDamage` und `SkyMobs.java:31` sagen „100,000". → **BEHOBEN (461b173)** — `wardenrecruit2` entfernt, Kommentare in `SkyMobs` und `SkyWardenMob.canTakeDamage` sagen 30 000.
17. **Geisterführer:** `ghostguideintro` „Mit Münzen kann ich nichts anfangen." und MOD_SUMMARY „nimmt kein Geld" — sein Laden verkauft **gegen Münzen** (Schnitter 14 000–24 000 usw.). → **BEHOBEN (461b173)** — der Text folgt dem Laden, nicht umgekehrt: Münz-Laden ist die Entscheidung des Spielers („Auf garkeinen fall! dann lieber hohe münzbeträge und normaler shop.", `GhostGuideMob`). `ghostguideintro` neu, MOD_SUMMARY und FOGKEY A3 angepasst.
18. **Magpie/Halda/Ossian Siedlungs-Tipp** „an einer verlassenen Skywatch-Werkstatt" — sie stehen seit 2026-09-10 in Zollhaus/Keller/Testgelände. (Der alte Werkstatt-Weg existiert parallel noch, `SkyLevel.placeResident`.)
19. **Eveleen/Mortimer/Caspern-Tipps** verschweigen, dass die Anreise **den rekrutierten Wächter** voraussetzt.
20. **Dorians Tipp** „niemals euer Vieh": geschützt ist nur, was **innerhalb** der Siedlungsgrenze steht; Nutztiere außerhalb sind Beute.
21. **Katzen-Quest-Ziele** `swhcatblackhome/tabbyhome` „schläft im Korb im Turm des Wardens" — falsch, sobald ein eigener Katzenkorb steht. → **BEHOBEN (461b173)** — die Quest-Zeilen sagen jetzt „schläft im Katzenkorb".
22. **Eden-Kommentar vs. Welt** (`EdenBiome.java:127-131`): „Eden Copper, which is mined" — die Eden-Kupfer-Felsen sind Vanilla-`ivyoreswamp`, die Beerensträucher Vanilla-`blackberrybush`/`blueberrybush`, der Baum der Erkenntnis ein Vanilla-Dryadenbaum. Sie liefern **Vanilla-Beute** (HYPOTHESE, aus der Vanilla-Stand-in-Wahl gefolgert). Edenkupfer und die drei Quest-Früchte gibt es dann **nur aus Kisten/POIs/Himmelsfahrten**.
23. **`swh_edenreach`** gibt es nur über das Edentor; wer zu Fuß nach Eden kommt, bekommt die Quest nie (Eveleen gibt trotzdem `swh_edenplants`).
24. **Belohnungszeile `swh_edenplants`** erwähnt den Gebührenerlass nicht (die anderen drei tun es).
25. **Mr. Knotts** Gebühr wird trotz Quest **nicht** erlassen — inkonsistent zu den vier anderen Bewohner-Ketten.
26. **Séance-Zirkel/Kreide:** Kommentare sagen „die Kreide wird verbraucht", der Zirkel gibt beim Abbauen die Kreide zurück (`SeanceCircleObject.getLootTable`).

### 8.3 Veraltete Dokumente (Code gewinnt)

27. **MOD_SUMMARY §5 / §6:** Hölle „nicht gebaut", „0 Biome, 0 Feinde, 4 unerreichbar" — Code: 2 Biome, 4 Gegner, Mutantenhydra 528 000 LP per `swh_keyhell`. → **BEHOBEN (461b173)**
28. **MOD_SUMMARY §2** „Zehn benannte Menschen" — es gibt 11 benannte Bewohner (+ Dorian) plus vier Berufs-/Besucher-NPCs (Therapeut, Arzt, Veteran, Zwielichthändler), die dort fehlen. → **BEHOBEN (461b173)** — MOD_SUMMARY §2 zählt zehn Bewohner + Dorian und führt die vier Berufs-/Besucher-NPCs.
29. **MOD_SUMMARY §3 Wächter-Laden** (Silberglöckchen ×2 @ 5 000, Geisterkreide ×3) — Code: Kreide @ 1 200 + vier verdiente Kettenbelohnungen. **Magpie** kauft zusätzlich Himmelspost-Pakete und alle fünf Trophäen, verkauft Kurierkappe; kein Silberglöckchen. → **BEHOBEN (461b173)**
30. **MOD_SUMMARY §5 Wächter-Tabelle:** „Tollwright lässt fallen —" — Code: 1–2 Aetherium, 1–3 Sturmstahl, 3–6 Himmelsstein. → **BEHOBEN (461b173)**
31. **OVERVIEW §3** listet Mortimer/Caspern mit Quest „—", Anzahl „9 named humans"; **§8 Punkt 5** („Magpie, Halda, Ossian near-unfindable") ist durch die Landmarken überholt; **§8 Punkt 7** zählt noch Mortimer/Caspern ohne `interact`. → **BEHOBEN (461b173)**
32. **quests.md:** `swh_recruitwarden` belohnt angeblich mit Silberglöckchen; Registry-Tabelle „region keys 1 of 5", Hölle fehlt, ebenso Ives/Mortimer/Caspern.
33. **OVERVIEW §1** „Standing in a band you have not earned stacks a named debuff" — es gibt **nur eine** Wand (Tiefe 0,581); Eden und Steinfeld haben keine Sperre. → **BEHOBEN (461b173)**
34. **`SwhResetCommand.NAMED_RESIDENTS`** kennt Ives und Dorian nicht (Statusbericht zählt sie nie).
35. `SkywatchQuestData`-Felder `catsIntroShown`, `anchorIntroShown`, `finaleShown` werden gespeichert, aber nirgends mehr gesetzt.

### 8.4 Sprache, Tippfehler, Benennung

36. **Tippfehler:** `item.bloodvial` „Blutfaeschchen" (richtig „Blutfläschchen"); `swhmortimerritesreward`/`swhcaspernforgereward` „Geiststahlbarren" (sonst „Geisterstahlbarren"); `swhbeaconreward` „Flickerlicht-Girlande" (Item heißt „Flackerlicht-Girlande"); `catbasketready` „Wolkenpuff-Leckerli" (Item heißt „Wolkenzupf-Leckerli"). → **BEHOBEN (461b173)**
37. **ae/oe/ue statt Umlauten** in: allen Magpie-, Halda-, Ossian-Talkzeilen; allen Kriegsveteran-Zeilen und `swhveteranintro/doit/max`; `bloodvial`/`bloodvialtip`; allen `vampire*`-Zeilen und `vampiresettlertip`; `swhdoctorcure`, `swhdoctorcured`. → **BEHOBEN (461b173)** — 43 Wertzeilen, auch ß (saßen, draußen, außerhalb); Kommentarzeilen unverändert.
38. **Doppelte Locale-Schlüssel** (in DE und EN): `itemtooltip.cloudpufftreattip` (Z. 206 und 503) und `itemtooltip.silverbelltip` (Z. 207 und 504) mit unterschiedlichem Text. → **BEHOBEN (461b173)** — die Engine behält die spätere Zeile (`TranslationCategory.addTranslation` = `HashMap.put`), die früheren wurden entfernt.
39. **Grammatik** `misc.regionkeyunlocked` „Die <key> setzt sich …" → „Die Wachfeuer der Himmelsweite", „Die Knotts Krumme Tür", „Die Das Höllensiegel". Artikel muss in den Namen oder raus. → **BEHOBEN (461b173)** — „Das Schlüsselstück setzt sich in den Boden: <key>." (EN ebenso); „Höllensiegel" ohne Artikel.
40. **Gemischte Benennung Warden:** „Sky Warden" (`swhreturnwarden`), „Warden"/„Wardens Turm", „Himmelswächter", „Hüter" (Siedlername), „Wächter" (Intro). Ebenso „Skywatch" vs. „Himmelswacht" (`swhanchorreward` „Skywatch-Banner", Item heißt „Himmelswacht-Banner"). → **BEHOBEN (461b173)** — DE durchgehend „Wächter"/„Himmelswächter" und „Himmelswacht" (Siedlername ebenfalls). Ausnahme: die drei Siedlungs-Tipps aus Nr. 18 (Platzierungs-Umbau, anderer Agent) und die Beinamen „Hüter" in Itemnamen/Lore.
41. **Ortsnamen uneinheitlich:** Biom „Aschenöde", Tooltip „Aschenweite"; Biom „Aurorabänke", Tooltips „Aurora-Untiefen"/„Untiefen"; „Aetherium" vs. „Ätherium" (`aetherforgetip`), „Aetheresse" vs. „Ätherschmiede"; Reich „Jenseits der Krümmung"/„Krümmung"/„Krummes Jenseits". → **BEHOBEN (461b173)** — Aschenöde, Aurorabänke, Aetherium, Ätherschmiede. „Krümmung" bleibt als Kurzform von „Jenseits der Krümmung"; „Krummes Jenseits" kommt in `de.lang` nicht vor.
42. **Namen in MOD_SUMMARY ≠ Spiel:** Tollwright → im Spiel „Zollwerk"; „Verbundene Schließkassette" → „Verzollte Kassette"; „Skyway-Freibrief" → „Himmelsweg-Freibrief"; „Register unzustellbarer Post" → „Buch der unzustellbaren Post"; „Aetherwright-Gehäuse" → „Ätherwerker-Gehäuse"; „Skywatch-Siegel" → „Siegelring der Himmelswacht"; „Vatlinge" → „Bottichling"; „Trauerband" → „Trauerflor". → **BEHOBEN (461b173)**
43. `glimmerstrides` (Schimmerschritt-Stiefel) hat keinen Tooltip-Schlüssel. → **BEHOBEN (461b173)** — `itemtooltip.glimmerstridestip` + `getPreEnchantmentTooltips` wie bei Sturmstahl.

### 8.5 Balance-/Design-Auffälligkeiten (keine Fehler, aber zum Überdenken)

44. Knott erscheint auch im **Höllen-Ring** (`SkyLevel.placeRealmResidents` ruft `CrookedResidents` für CROOKED und HELL).
45. Die Himmelsweite-Bewohner können über den alten Werkstatt-Weg vor ihrer Landmarke „verbraucht" werden: steht eine Werkstation beim ersten Generieren einer Region, kann Magpie dort erscheinen und `residentsClaimed` setzen — dann steht die Landmarke ohne sie (HYPOTHESE, nicht ausgeführt).
46. Katzen lassen sich ohne Quest heimlocken; die Belohnung kommt trotzdem beim nächsten Wächter-Gespräch (Absicht laut Kommentar, aber für den Spieler unsichtbar).
47. Region-Schlüssel: **eine** Aktion pro Gespräch — nach einer Abgabe muss man den Wächter erneut ansprechen, um die nächste Frage zu bekommen.
48. Himmelsfahrten nach Eden & tiefer haben 0 % Erfolg, bis das jeweilige Schlüsselstück steht; der Grund steht nirgends im Spiel.

---

*Erstellt aus dem Code; nichts davon in dieser Sitzung im Spiel geprüft.*
