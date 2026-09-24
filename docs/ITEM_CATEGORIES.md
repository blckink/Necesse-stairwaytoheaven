# Item-Kategorien — wo jedes Ding der Mod landet

Auslöser (Spieler, 2026-09-24): *"alle Items mal richtig zugeordnet, nicht
Großteil als Objekte."*

Dieses Dokument ist die vollständige Bestandsaufnahme aller 454 Items, die die
Mod registriert (jedes Objekt und jeder Boden bekommt vom Spiel automatisch ein
Item, jeder Kill-Stat-Mob ein Spawn-Item), mit Inventar-Kategorie,
Crafting-Tab, Erhältlichkeit, Broker-Wert, Seltenheit und Rezept — vorher und
nachher. Die Tabelle am Ende ist **nicht von Hand geschrieben**, sondern aus dem
Server-Log erzeugt (`CategoryCensus` → `tools/category_table.py`).

## 1. Wie Necesse 1.3.2 Kategorien vergibt (VERIFIED [jar])

Gelesen im dekompilierten 1.3.2-Code:

- **Drei Bäume.** `ItemCategory.masterManager` (Inventar-Sortierung, Kreativ-
  menü, Item-Browser), `ItemCategory.craftingManager` (Tabs im Crafting-Menü)
  und `equipmentManager`. Alle Knoten werden in `ItemCategory`'s static-Block
  angelegt (ItemCategory.java:33-208). Der Crafting-Baum ist flacher: `tiles`
  hat dort **keine** Unterkategorien, `misc.questitems` oder `objects.traps`
  gibt es dort nicht — eine nicht existierende Pfadangabe wirft beim
  Registrieren `IllegalStateException` (ItemCategoryManager.getCategory).
- **Defaults.** `Item` startet mit `{"misc"}` in beiden Bäumen (Item.java:180);
  `GameObject.itemCategoryTree` und `craftingCategoryTree` starten mit dem
  nackten `{"objects"}` (GameObject.java:113-114); `GameTile` mit `{"tiles"}`
  und setzt selbst `tiles.floors/terrain/liquids` (GameTile.java:128-132).
  Vanilla-Basisklassen setzen ihre Kategorie im Konstruktor (MatItem →
  `materials`, GrainItem/FoodMatItem → `consumable.rawfood`, ArmorItem →
  `equipment.armor`, GrassObject → `objects.landscaping.plants`,
  FlowerObject → `materials.flowers`, CraftingStationObject →
  `objects.craftingstations`, …).
- **Objekt → Item.** `ObjectRegistry.registerObject` erzeugt für JEDES Objekt
  ein `ObjectItem`, und dessen Konstruktor kopiert
  `object.itemCategoryTree`/`craftingCategoryTree` in diesem Moment
  (ObjectItem.java:55-56). Ein `setItemCategory` am Objekt NACH
  `registerObject` bewirkt nichts; es muss vorher passieren (oder direkt am
  Item). `itemObtainable` (4. Argument) steuert Statistik und — über
  `GameObject.getLootTable` — ob das Objekt beim Abbauen sich selbst droppt
  (GameObject.java:285-288).
- **Natürlich vs. gebaut.** Vanilla-Szenerie, die auch baubar ist, fragt
  `level.objectLayer.isPlayerPlaced(x, y)`: vom Spieler gesetzt → gibt sich
  selbst zurück; von der Welt gesetzt → Material oder nichts
  (SurfaceGrassObject.java:22-30, CrystalClusterObject.java:101,
  CowSkeletonObject.java:43, CobwebObject.java:39). Das Flag wird pro Kachel
  gespeichert (`objectIsPlayerPlaced`, ArrayObjectLayer.java:90) und von
  `GameObject.placeObject(..., byPlayer)` gesetzt (GameObject.java:607).

## 2. Befund

Gemessen mit dem neuen Server-Zensus (VERIFIED [run], Seed-unabhängig, weil
Registrierungsdaten):

```
vorher:  swhcat census: items=454 obtainable=267 bad=7 objectsroot=5 miscroot=1 tilesroot=0 craftroot=6
nachher: swhcat census: items=454 obtainable=267 bad=0 objectsroot=5 miscroot=0 tilesroot=0 craftroot=0
```

(`bad=7` vorher zählte noch mit der ersten Regel, die den Crafting-Tab `tiles`
fälschlich anmahnte — dort ist er Vanilla-korrekt; mit der endgültigen Regel
war es vorher `bad=1`: `overgrownedenseed` im nackten `misc`. Die fünf
`objectsroot` sind Multi-Tile-Teilstücke — `…2`-Hälften von Gemälden und
Kristallen —, die in keiner Liste erscheinen und nicht erhältlich sind.)

Die Kategorien waren also **nicht** überwiegend falsch. Was der Spieler als
"Großteil als Objekte" erlebt, hat eine andere Ursache — und die war echt:

1. **Abbauen von Welt-Szenerie füllte die Tasche mit Objekten.** Zehn
   natürliche Dinge waren `obtainable=true` und hatten keine eigene
   Loot-Tabelle, also gab `GameObject.getLootTable` beim Abbauen das Objekt
   selbst zurück: Wolkenriede, Flüsterriede, Eden-Gras (jedes geschnittene
   Grasbüschel!), der Tote Baum (eines der häufigsten Streu-Objekte in Veil und Outlands),
   Sturmschutt, Wächter-Trümmer, Ladungskristall, Aurorascherben, Sternfall,
   Welkstrauch. Vanilla gibt hier Material bzw. Wurmköder/Saat.
2. **Material, das als Objekt implementiert ist, sortierte als Deko/Pflanze.**
   Windweizen (eine Ernte, die zwei Händler kaufen) stand unter
   *Landschaft › Pflanzen*, der Düsterpilz (Sammel-Blume) unter *Dekoration*.
3. **Unscharfe Sammelbecken.** Natürliche Kristalle, Schutt und Sträucher unter
   *Dekoration* statt neben Vanillas eigenen Kristallen/Steinen/Pflanzen; die
   sechs Regionsschlüssel unter *Objekte › Sonstiges* zwischen Toren und
   Kisten; Katapult und Geschützturm des Veteranen unter *Dekoration*;
   Eden-Obst, Steinfeld-Materialien und Eden-Saft/-Steckling im nackten
   `materials`; der Eden-Grassamen im nackten `misc`.
4. **Erhältlich, aber ohne Quelle.** Seelenwebstuhl und Geisterschmiede:
   registriert als erhältlich, aber kein Rezept, kein Händler, keine Beute,
   kein Preset — und damit Seelenfaden-Rezept, Geisterstahlbarren und die
   ganze Geisterstahl-Rüstung unerreichbar.

## 3. Änderungen, nach Gruppen

Keine String-ID wurde umbenannt, kein Objekt wurde zu einem Item oder
umgekehrt. Alle Änderungen sind Kategorie, Loot-Tabelle oder Rezept — nichts
davon steht im Spielstand.

| Gruppe | IDs | vorher → nachher | Begründung (Vanilla-Analogon) |
|---|---|---|---|
| A. Natürliche Szenerie droppt Material | `deadtree`, `stormscreed`, `skywatchrubble`, `chargecrystal`, `aurorashards`, `starfall`, `withershrub` | Welt-platziert: gab sich selbst → gibt `deadwoodlog` 2-4 / `skystone` 1-2 / `skystone` 2-4 / `stormshard` 1-2 / `aurorapetal` 1-2 / `prismshard` 1 / nichts. Spieler-platziert: unverändert sich selbst. | CrystalClusterObject, CowSkeletonObject, Vanilla-Totholz. Neu: `SkyDecoObject.setNaturalLoot`. |
| B. Wildes Gras droppt wie Vanilla-Gras | `skyreeds`, `whisperreeds`, `overgrowngrass` | Welt-gewachsen: gab sich selbst → 1/35 `wormbait`; Eden-Gras zusätzlich 1 % `overgrownedenseed`. Gepflanzt: unverändert. | SurfaceGrassObject (Wurmköder + 1 % Grassamen). Neu: `objects/NaturalGrassObject`. |
| C. Natürliche Szenerie in Vanillas Landschafts-Baum | `chargecrystal`, `aurorashards`, `starfall` → `objects.landscaping.crystals`; `stormscreed` → `…rocksandores`; `skywatchrubble` → `…masonry`; `withershrub`, `deadtree` → `…plants` (Inventar und Crafting) | `objects.decorations` → Landschaft | Vanillas Kristalle, Steine, Statuen, Pflanzen sortieren genau dort. |
| D. Material als Objekt → Material-Kategorie | `windwheat` → `consumable.rawfood` (Crafting `materials`); `gloomshroom` → `materials.flowers` (Crafting `materials`) | Pflanze/Deko → Rohkost/Blumen | Vanilla-Weizen ist GrainItem (`consumable.rawfood`), Vanilla-Blumen/Pilz sind FlowerObject (`materials.flowers`). Beide bleiben Objekte (sie werden gepflanzt, IDs stehen in Welten). |
| E. Materialien in echte Unterkategorien | `palestone` → `materials.stone`; `gravesalt`, `echoshard` → `materials.minerals`; `spiritmoss`, `edensap`, `knowledgecutting` → `materials.flowers`; `paradiseapple`, `paradisecoconut`, `edenberry`, `moonmelon`, `sungrape` → `consumable.rawfood` | nacktes `materials` → Familie | Vanilla vergibt fast jedem MatItem eine Familie (ItemRegistry.java:846-933); Rohe Früchte/Honig/Weizen sind `consumable.rawfood`. |
| F. Schlüssel zu den Quest-Items | `regionkeyskyreach`, `…eden`, `…steinfeld`, `…ghostrealm`, `…crookedbeyond`, `…hell` → `misc.questitems` (Crafting-Seite bleibt `objects.misc`) | `objects.misc` → Quest-Items | Belohnung einer Elder-Quest; die Geisterkreide (`ghostchalk`) ist dasselbe Muster. Bleibt ein Objekt: FOGKEY §B1 legt fest, dass der Schlüssel in der Basis aufgestellt wird. |
| G. Verteidigung zu den Fallen | `veteranturret`, `veterancatapult` (+ 8 Teilstücke) → `objects.traps` (Crafting `objects.misc`, dort gibt es kein `traps`) | `objects.decorations` → Fallen | Vanilla sortiert seine Verteidigungsobjekte unter `objects.traps`. |
| H. Saat zu den Saaten | `overgrownedenseed` → `objects.seeds` | nacktes `misc` → Saaten | Vanillas `GrassSeedItem` setzt keine Kategorie und fällt ins `misc`-Default; für den Spieler gehört eine Saat zu den Saaten. |
| I. Quelle für quellenlose Stationen | `spiritforge` (Werkbank: 20 Knochenholz, 8 Spektralerz, 6 Ektoplasma), `soulloom` (Werkbank: 12 Knochenholz, 8 Ektoplasma, 4 Seelenfaden) | kein Rezept → Rezept | Wie die drei Himmelsstationen (SkyProfessions). Die Zutaten gibt es nur im Geisterreich, das ist WORLD_DESIGNs "Geisterreich betreten → Geisterschmiede". **Mengen sind ein Vorschlag — awaiting player confirmation.** |

Zusammen: **37 Einträge mit geänderter Kategorie** (29 für den Spieler
sichtbar + 8 unsichtbare Katapult-Teilstücke), **10 Objekte mit geändertem
Natur-Drop** (Gruppen A, B), **2 Stationen mit neuem Rezept**.

### Bewusst NICHT geändert

- `skyweave` und `cloudpufftreat` bleiben im nackten `materials` — beide haben
  einen dokumentierten Grund im Code (Vanillas `glass`/`glassbottle`-Muster
  für Zwischenprodukte ohne Familie).
- Von Menschen gebaute POI-Requisiten (`skywatchtelescope`,
  `skywatchastrolabe`, `skyballoon`, `aeronautwreck`) geben sich auch aus der
  Welt heraus selbst zurück — wie Vanilla-Dungeonmöbel. Das Päckchen
  (`skyparcel`) hatte die Natur/gebaut-Weiche schon.
- `windwheat` und `gloomshroom` droppen weiter sich selbst: das IST die Ernte
  (Händler kaufen Windweizen; der Düsterpilz wird durch Platzieren gepflanzt).
- Terrain-Böden, Bäume, Setzlinge, Büsche, Felsen: schon in Vanillas Bäumen,
  Drops schon korrekt (Felsen → Stein, Bäume → Holz).
- Nicht erhältliche Welt-Objekte der Reiche (Krumm, Geist, Steinfeld, Hölle)
  mit `objects.decorations`: unsichtbar (weder erhältlich noch im
  Kreativmenü) und mit eigener Loot-Tabelle; ohne Wirkung für den Spieler.
- Die ausgemusterten Gemälde (`eyepainting`, `shrunkenheadtrophy`) sind
  erhältlich ohne Quelle — absichtlich, damit Exemplare in alten Spielständen
  laden (TwilightWares `RETIRED_PAINTINGS`).
- ERR-Icons: `tools/locale_audit.py` prüft jedes haltbare ID gegen die Datei,
  die die Engine anfordert, und meldet OK. Die Client-Darstellung ist mangels
  Client nicht angesehen worden.

## 4. Spielstand-Kompatibilität

- Keine ID umbenannt, kein Registrierungstyp gewechselt → platzierte Objekte,
  Inventare und Truhen alter Welten laden unverändert.
- Kategorien stehen nicht im Spielstand; alte Stapel sortieren sich einfach neu.
- Die Natur-Drop-Weiche liest `objectIsPlayerPlaced`, das der Spielstand
  schon immer pro Kachel speichert. Ein Stück, das der Spieler in einer alten
  Welt gebaut hat, gibt sich also weiter selbst zurück; nur von der Welt
  generierte Stücke droppen Material. (VERIFIED [jar]:
  ArrayObjectLayer.addSaveData/applyLoadData.)
- Kein Konvertierungspfad nötig. Items, die Spieler vorher als Objekt
  aufgesammelt haben (z. B. ein Stapel `deadtree`), bleiben gültige,
  platzierbare Items.

## 5. Das Gate

- `src/main/java/stairwaytoheaven/CategoryCensus.java` druckt bei jedem
  Serverstart eine Zeile pro Mod-Item und eine Summe
  (`swhcat census: … bad=N …`). `bad` zählt haltbare, gelistete Items im
  nackten Fallback (`objects`, `misc`, `tiles`, keine) und Rezept-Ergebnisse,
  deren Crafting-Tab `misc`/`objects`/keiner ist. Jede Fundstelle steht als
  `swhcat BAD <id> …` im Log.
- Außerdem fragt es auf einer Wegwerf-Ebene (8×8, nie gespeichert) die echte
  `getLootTable` einiger Objekte natürlich und gebaut ab:
  `swhcat loot deadtree natural=[deadwoodlog] placed=[deadtree]`.
- `scripts/integration_test.sh` verlangt `bad=0` und sechs dieser Loot-Zeilen.
  Eine künftige Registrierung ohne Kategorie lässt das Gate mit ihrem Namen
  scheitern.
- `python3 tools/category_table.py <log> [--before <log>] --write` erzeugt die
  Tabelle unten neu.

## 6. Vollständige Tabelle

Legende: **fett** = in diesem Durchgang geändert, "Kategorie vorher" nur
gefüllt, wenn sie sich geändert hat. "Teilstück" = Multi-Tile-Hälfte, erscheint
in keiner Liste. Rezept-Station ist die Tech-ID (`workstation` = Werkbank,
`none` = Inventar-Crafting).

<!-- TABLE:START (tools/category_table.py) -->
| ID | Art | Klasse | Kategorie vorher | Kategorie (Inventar) | Crafting-Tab | erhältlich / Kreativ | Broker | Seltenheit | Rezept an |
|---|---|---|---|---|---|---|---|---|---|
| `cloudcustard` | Item | LivestockFood |  | consumable.food.fine | consumable.fine | ja / ja | 16.0 | NORMAL | cookingpot |
| `nimbusdraught` | Item | LivestockFood |  | consumable.food.fine | consumable.fine | ja / ja | 24.0 | UNCOMMON | alchemy |
| `skycurd` | Item | LivestockFood |  | consumable.food.fine | consumable.fine | ja / ja | 9.0 | NORMAL | cheesepress |
| `wardensround` | Item | WardensRoundItem |  | consumable.food.fine | consumable.fine | ja / ja | 150.0 | RARE |  |
| `cloudberry` | Item | GrainItem |  | consumable.rawfood | materials | ja / ja | 3.0 | NORMAL |  |
| `edenberry` | Item | GhostMatItem | materials | **consumable.rawfood** | materials | ja / ja | 10.0 | UNCOMMON |  |
| `moonmelon` | Item | GhostMatItem | materials | **consumable.rawfood** | materials | ja / ja | 10.0 | UNCOMMON |  |
| `nimbusmilk` | Item | LivestockFood |  | consumable.rawfood | consumable.simple | ja / ja | 4.0 | COMMON |  |
| `paradiseapple` | Item | GhostMatItem | materials | **consumable.rawfood** | materials | ja / ja | 10.0 | UNCOMMON |  |
| `paradisecoconut` | Item | GhostMatItem | materials | **consumable.rawfood** | materials | ja / ja | 10.0 | UNCOMMON |  |
| `sungrape` | Item | GhostMatItem | materials | **consumable.rawfood** | materials | ja / ja | 10.0 | UNCOMMON |  |
| `glimmerstrides` | Item | GlimmerstrideBoots |  | equipment.armor | equipment.armor | ja / ja | 60.0 | UNCOMMON | tungstenanvil |
| `spiritsteelboots` | Item | SpiritsteelBoots |  | equipment.armor | equipment.armor | ja / ja | 180.0 | EPIC | spiritforge |
| `spiritsteelchestplate` | Item | SpiritsteelChestplate |  | equipment.armor | equipment.armor | ja / ja | 300.0 | EPIC | spiritforge |
| `spiritsteelhelmet` | Item | SpiritsteelHelmet |  | equipment.armor | equipment.armor | ja / ja | 220.0 | EPIC | spiritforge |
| `stormsteelboots` | Item | Boots |  | equipment.armor | equipment.armor | ja / ja | 95.0 | EPIC | tungstenanvil |
| `stormsteelchestplate` | Item | Chestplate |  | equipment.armor | equipment.armor | ja / ja | 190.0 | EPIC | tungstenanvil |
| `stormsteelhelmet` | Item | Helmet |  | equipment.armor | equipment.armor | ja / ja | 130.0 | EPIC | tungstenanvil |
| `dreamstalkerboots` | Item | Boots |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `dreamstalkerchest` | Item | Chest |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `dreamstalkerhead` | Item | Head |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `grinclownboots` | Item | Boots |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `grinclownchest` | Item | Chest |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `grinclownhead` | Item | Head |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `haldakerchief` | Item | HaldaKerchief |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | UNCOMMON |  |
| `hauntedpuppetboots` | Item | Boots |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `hauntedpuppetchest` | Item | Chest |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `hauntedpuppethead` | Item | Head |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `hockeyslasherboots` | Item | Boots |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `hockeyslasherchest` | Item | Chest |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `hockeyslasherhead` | Item | Head |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `magpiecap` | Item | MagpieCap |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | UNCOMMON |  |
| `pincushionboots` | Item | Boots |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `pincushionchest` | Item | Chest |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `pincushionhead` | Item | Head |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `pumpkinscarecrowboots` | Item | Boots |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `pumpkinscarecrowchest` | Item | Chest |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `pumpkinscarecrowhead` | Item | Head |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `screamrobeboots` | Item | Boots |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `screamrobechest` | Item | Chest |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `screamrobehead` | Item | Head |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `skeletonboots` | Item | Boots |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `skeletonchest` | Item | Chest |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `skeletonhead` | Item | Head |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `skywatchhood` | Item | Hood |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | RARE |  |
| `stitchedmonsterboots` | Item | Boots |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `stitchedmonsterchest` | Item | Chest |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `stitchedmonsterhead` | Item | Head |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `twilightsuitboots` | Item | Boots |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `twilightsuitchest` | Item | Chest |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `twilightsuithead` | Item | Head |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `vanecowl` | Item | VaneCowl |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | UNCOMMON |  |
| `wardenboots` | Item | Boots |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | RARE |  |
| `wardenmantle` | Item | Mantle |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | RARE |  |
| `widowgownboots` | Item | Boots |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `widowgownchest` | Item | Chest |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `widowgownhead` | Item | Head |  | equipment.cosmetics | equipment.cosmetics | ja / ja | 0.0 | EPIC |  |
| `auroralocket` | Item | SkyTrinketItem |  | equipment.trinkets | equipment.trinkets | ja / ja | 260.0 | EPIC | tungstenworkstation |
| `skywatchsignet` | Item | SkywatchSignetItem |  | equipment.trinkets | equipment.trinkets | ja / ja | 280.0 | EPIC |  |
| `stormsteelvambrace` | Item | SkyTrinketItem |  | equipment.trinkets | equipment.trinkets | ja / ja | 180.0 | EPIC | tungstenworkstation |
| `zephyrharness` | Item | SkyTrinketItem |  | equipment.trinkets | equipment.trinkets | ja / ja | 160.0 | EPIC | tungstenworkstation |
| `prismcaller` | Item | PrismcallerStaffToolItem |  | equipment.weapons.magicweapons | equipment.weapons.magicweapons | ja / ja | 300.0 | EPIC | tungstenworkstation |
| `skylance` | Item | SkyLanceToolItem |  | equipment.weapons.magicweapons | equipment.weapons.magicweapons | ja / ja | 580.0 | EPIC | tungstenworkstation |
| `cloudglaive` | Item | CloudGlaiveToolItem |  | equipment.weapons.meleeweapons | equipment.weapons.meleeweapons | ja / ja | 450.0 | EPIC |  |
| `skyreave` | Item | SkyreaveGlaiveToolItem |  | equipment.weapons.meleeweapons | equipment.weapons.meleeweapons | ja / ja | 280.0 | EPIC | tungstenworkstation |
| `spiritsteelreaver` | Item | SpiritsteelReaver |  | equipment.weapons.meleeweapons | equipment.weapons.meleeweapons | ja / ja | 430.0 | EPIC |  |
| `stormdisc` | Item | StormdiscToolItem |  | equipment.weapons.meleeweapons | equipment.weapons.meleeweapons | ja / ja | 100.0 | EPIC | tungstenworkstation |
| `tempestedge` | Item | TempestEdgeSwordToolItem |  | equipment.weapons.meleeweapons | equipment.weapons.meleeweapons | ja / ja | 220.0 | EPIC | tungstenworkstation |
| `galehowl` | Item | GalehowlProjectileToolItem |  | equipment.weapons.rangedweapons | equipment.weapons.rangedweapons | ja / ja | 220.0 | EPIC | tungstenworkstation |
| `gravewindbow` | Item | GravewindBow |  | equipment.weapons.rangedweapons | equipment.weapons.rangedweapons | ja / ja | 400.0 | EPIC |  |
| `thunderhead` | Item | ThunderheadGreatbowToolItem |  | equipment.weapons.rangedweapons | equipment.weapons.rangedweapons | ja / ja | 400.0 | EPIC | tungstenworkstation |
| `skywatchwhistle` | Item | SkywatchWhistleSummonToolItem |  | equipment.weapons.summonweapons | equipment.weapons.summonweapons | ja / ja | 310.0 | EPIC | tungstenworkstation |
| `cloudpufftreat` | Item | SkyMatItem |  | materials | materials | ja / ja | 5.0 | UNCOMMON | none+none+none |
| `skyweave` | Item | SkyMatItem |  | materials | materials | ja / ja | 20.0 | UNCOMMON | windsilkloom |
| `aetheriumbar` | Item | SkyMatItem |  | materials.bars | materials | ja / ja | 25.0 | UNCOMMON | forge+aetherforge |
| `edenbronzebar` | Item | GhostMatItem |  | materials.bars | materials | ja / ja | 10.0 | EPIC | tungstenworkstation |
| `spiritsteelbar` | Item | GhostMatItem |  | materials.bars | materials | ja / ja | 55.0 | EPIC | spiritforge |
| `stormsteelbar` | Item | SkyMatItem |  | materials.bars | materials | ja / ja | 58.0 | RARE | aetherforge |
| `aurorapetal` | Item | SkyMatItem |  | materials.flowers | materials | ja / ja | 15.0 | UNCOMMON |  |
| `edensap` | Item | GhostMatItem | materials | **materials.flowers** | materials | ja / ja | 10.0 | UNCOMMON |  |
| `goldenpollen` | Item | GhostMatItem |  | materials.flowers | materials | ja / ja | 10.0 | RARE |  |
| `knowledgecutting` | Item | GhostMatItem | materials | **materials.flowers** | materials | ja / ja | 10.0 | EPIC |  |
| `spiritmoss` | Item | SteinfeldMatItem | materials | **materials.flowers** | materials | ja / ja | 32.0 | RARE |  |
| `bonewood` | Item | GhostMatItem |  | materials.logs | materials | ja / ja | 4.0 | UNCOMMON |  |
| `charwood` | Item | SkyMatItem |  | materials.logs | materials | ja / ja | 2.0 | NORMAL |  |
| `cloudwood` | Item | SkyMatItem |  | materials.logs | materials | ja / ja | 2.0 | NORMAL |  |
| `edenwood` | Item | GhostMatItem |  | materials.logs | materials | ja / ja | 10.0 | UNCOMMON |  |
| `nimbuswood` | Item | SkyMatItem |  | materials.logs | materials | ja / ja | 2.0 | NORMAL |  |
| `prismwood` | Item | SkyMatItem |  | materials.logs | materials | ja / ja | 2.0 | NORMAL |  |
| `seraphwood` | Item | SkyMatItem |  | materials.logs | materials | ja / ja | 2.0 | NORMAL |  |
| `cinderpearl` | Item | SkyMatItem |  | materials.minerals | materials | ja / ja | 30.0 | UNCOMMON |  |
| `echoshard` | Item | SteinfeldMatItem | materials | **materials.minerals** | materials | ja / ja | 45.0 | RARE |  |
| `fulgurite` | Item | SkyMatItem |  | materials.minerals | materials | ja / ja | 25.0 | UNCOMMON |  |
| `gravesalt` | Item | SteinfeldMatItem | materials | **materials.minerals** | materials | ja / ja | 18.0 | UNCOMMON |  |
| `prismshard` | Item | SkyMatItem |  | materials.minerals | materials | ja / ja | 25.0 | UNCOMMON |  |
| `skystone` | Item | SkyMatItem |  | materials.minerals | materials | ja / ja | 2.0 | NORMAL |  |
| `stormglass` | Item | SkyMatItem |  | materials.minerals | materials | ja / ja | 32.0 | UNCOMMON | stormglasskiln |
| `stormshard` | Item | SkyMatItem |  | materials.minerals | materials | ja / ja | 25.0 | UNCOMMON |  |
| `aetherwrightcasing` | Item | SkyRewardItem |  | materials.mobdrops | materials | ja / ja | 90.0 | RARE |  |
| `aurorafleece` | Item | LivestockProduce |  | materials.mobdrops | materials | ja / ja | 16.0 | UNCOMMON |  |
| `bloodvial` | Item | BloodVialItem |  | materials.mobdrops | materials | ja / ja | 22.0 | UNCOMMON |  |
| `bloomfang` | Item | SkyMatItem |  | materials.mobdrops | materials | ja / ja | 45.0 | UNCOMMON |  |
| `dewsnail` | Item | SkyMatItem |  | materials.mobdrops | materials | ja / ja | 20.0 | UNCOMMON |  |
| `eyeseed` | Item | CrookedMatItem |  | materials.mobdrops | materials | ja / ja | 45.0 | RARE |  |
| `mourningband` | Item | SkyMatItem |  | materials.mobdrops | materials | ja / ja | 60.0 | UNCOMMON |  |
| `oddwood` | Item | CrookedMatItem |  | materials.mobdrops | materials | ja / ja | 30.0 | UNCOMMON |  |
| `realityshard` | Item | CrookedMatItem |  | materials.mobdrops | materials | ja / ja | 90.0 | EPIC |  |
| `serpentscale` | Item | GhostMatItem |  | materials.mobdrops | materials | ja / ja | 10.0 | RARE |  |
| `skystoneheart` | Item | SkyMatItem |  | materials.mobdrops | materials | ja / ja | 30.0 | UNCOMMON |  |
| `soulcollar` | Item | SkyMatItem |  | materials.mobdrops | materials | ja / ja | 85.0 | RARE |  |
| `soulthread` | Item | GhostMatItem |  | materials.mobdrops | materials | ja / ja | 30.0 | RARE | soulloom |
| `strangefabric` | Item | CrookedMatItem |  | materials.mobdrops | materials | ja / ja | 36.0 | UNCOMMON |  |
| `stripedhorn` | Item | SkyMatItem |  | materials.mobdrops | materials | ja / ja | 120.0 | RARE |  |
| `stripedshell` | Item | CrookedMatItem |  | materials.mobdrops | materials | ja / ja | 55.0 | RARE |  |
| `veilessence` | Item | SkyMatItem |  | materials.mobdrops | materials | ja / ja | 34.0 | RARE |  |
| `venomfang` | Item | GhostMatItem |  | materials.mobdrops | materials | ja / ja | 10.0 | RARE |  |
| `warpresin` | Item | CrookedMatItem |  | materials.mobdrops | materials | ja / ja | 34.0 | UNCOMMON |  |
| `windsilk` | Item | SkyMatItem |  | materials.mobdrops | materials | ja / ja | 12.0 | UNCOMMON | none+windsilkloom |
| `aetheriumore` | Item | SkyMatItem |  | materials.ore | materials | ja / ja | 8.0 | UNCOMMON |  |
| `edencopperore` | Item | GhostMatItem |  | materials.ore | materials | ja / ja | 10.0 | RARE |  |
| `spectralore` | Item | GhostMatItem |  | materials.ore | materials | ja / ja | 35.0 | RARE |  |
| `palestone` | Item | SteinfeldMatItem | materials | **materials.stone** | materials | ja / ja | 6.0 | UNCOMMON |  |
| `bondedlockbox` | Item | SkyRewardItem |  | misc.questitems | materials | ja / ja | 300.0 | RARE |  |
| `postledger` | Item | SkyRewardItem |  | misc.questitems | materials | ja / ja | 220.0 | RARE |  |
| `silverbell` | Item | SkyMatItem |  | misc.questitems | materials | ja / ja | 250.0 | EPIC |  |
| `skywaywrit` | Item | SkyRewardItem |  | misc.questitems | materials | ja / ja | 260.0 | RARE |  |
| `stormlenscore` | Item | SkyRewardItem |  | misc.questitems | materials | ja / ja | 300.0 | RARE |  |
| `themother` | Item | SkyRewardItem |  | misc.questitems | materials | ja / ja | 300.0 | RARE |  |
| `overgrownedenseed` | Item | OvergrownEdenSeedItem | misc | **objects.seeds** | objects | ja / ja | 2.0 | NORMAL |  |
| `windwheat` | Objekt | GrassObject | objects.landscaping.plants | **consumable.rawfood** | materials | ja / ja | 1.0 | NORMAL |  |
| `gloomshroom` | Objekt | SkyDecoObject | objects.decorations | **materials.flowers** | materials | ja / ja | 5.0 | NORMAL |  |
| `ghostchalk` | Objekt | SeanceCircleObject |  | misc.questitems | objects.misc | ja / ja | 120.0 | NORMAL |  |
| `regionkeycrookedbeyond` | Objekt | RegionKeyObject | objects.misc | **misc.questitems** | objects.misc | ja / ja | 40.0 | NORMAL |  |
| `regionkeyeden` | Objekt | RegionKeyObject | objects.misc | **misc.questitems** | objects.misc | ja / ja | 40.0 | NORMAL |  |
| `regionkeyghostrealm` | Objekt | RegionKeyObject | objects.misc | **misc.questitems** | objects.misc | ja / ja | 40.0 | NORMAL |  |
| `regionkeyhell` | Objekt | RegionKeyObject | objects.misc | **misc.questitems** | objects.misc | ja / ja | 40.0 | NORMAL |  |
| `regionkeyskyreach` | Objekt | RegionKeyObject | objects.misc | **misc.questitems** | objects.misc | ja / ja | 40.0 | NORMAL |  |
| `regionkeysteinfeld` | Objekt | RegionKeyObject | objects.misc | **misc.questitems** | objects.misc | ja / ja | 40.0 | NORMAL |  |
| `aurorabloomr` | Objekt | CrystalClusterRObject |  | objects | objects | nein / nein (Teilstück) | 0.0 | NORMAL |  |
| `hauntedwallclock2` | Objekt | LargePaintingObject2 |  | objects | objects | nein / nein (Teilstück) | 0.0 | EPIC |  |
| `magicmirror2` | Objekt | LargePaintingObject2 |  | objects | objects | nein / nein (Teilstück) | 0.0 | EPIC |  |
| `stormcrystalr` | Objekt | CrystalClusterRObject |  | objects | objects | nein / nein (Teilstück) | 0.0 | NORMAL |  |
| `walleye2` | Objekt | LargePaintingObject2 |  | objects | objects | nein / nein (Teilstück) | 0.0 | EPIC |  |
| `chapelcolumn` | Objekt | ColumnObject |  | objects.columns | objects.columns | nein / nein | 0.0 | NORMAL |  |
| `aetherforge` | Objekt | AetherForgeObject |  | objects.craftingstations | craftingstations | ja / ja | 20.0 | COMMON | workstation |
| `soulloom` | Objekt | GhostStationObject |  | objects.craftingstations | craftingstations | ja / ja | 30.0 | COMMON | workstation |
| `spiritforge` | Objekt | GhostStationObject |  | objects.craftingstations | craftingstations | ja / ja | 35.0 | COMMON | workstation |
| `stormglasskiln` | Objekt | StormglassKilnObject |  | objects.craftingstations | craftingstations | ja / ja | 20.0 | COMMON | workstation |
| `windsilkloom` | Objekt | WindsilkLoomObject |  | objects.craftingstations | craftingstations | ja / ja | 20.0 | COMMON | workstation |
| `aeronautwreck` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | ja / ja | 8.0 | NORMAL | workstation |
| `ashbones` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `bentgrass` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `bentlantern` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `bonewoodtree` | Objekt | GhostDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `brimstonecrag` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `charredgallows` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `crookedclock` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `crookeddeadtree` | Objekt | GhostDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `eyeballshrub` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `ghostgravestone` | Objekt | GhostDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `ghostrock` | Objekt | GhostDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `groundwindow` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `heavenslab` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `hellbones` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `lanterntree` | Objekt | GhostDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `longchair` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `screamingflower` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `skyanchor` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `skyballoon` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | ja / ja | 2.0 | NORMAL | workstation |
| `skyfallshard` | Objekt | SkyfallShardObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `skyparcel` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | ja / ja | 2.0 | NORMAL | workstation |
| `skywatchastrolabe` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | ja / ja | 25.0 | NORMAL | workstation |
| `skywatchtelescope` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | ja / ja | 25.0 | NORMAL | workstation |
| `spectralorerock` | Objekt | GhostDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `spiraltree` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `spiritwillow` | Objekt | GhostDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `stripedmushroom` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `teethrock` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `wardenbeaconoff` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `wardenbeaconon` | Objekt | SkyDecoObject |  | objects.decorations | objects.decorations | nein / nein | 0.0 | NORMAL |  |
| `skywatchcarpet` | Objekt | ModularCarpetObject |  | objects.decorations.carpets | objects.decorations.carpets | ja / ja | 25.0 | NORMAL | carpenter+workstation |
| `eyepainting` | Objekt | PaintingObject |  | objects.decorations.paintings | objects.decorations.paintings | ja / ja | 20.0 | RARE |  |
| `hauntedwallclock` | Objekt | LargePaintingObject |  | objects.decorations.paintings | objects.decorations.paintings | ja / ja | 20.0 | EPIC |  |
| `magicmirror` | Objekt | LargePaintingObject |  | objects.decorations.paintings | objects.decorations.paintings | ja / ja | 20.0 | EPIC |  |
| `salonmirror` | Objekt | SalonMirrorObject |  | objects.decorations.paintings | objects.decorations.paintings | ja / ja | 30.0 | EPIC |  |
| `shrunkenheadtrophy` | Objekt | PaintingObject |  | objects.decorations.paintings | objects.decorations.paintings | ja / ja | 20.0 | RARE |  |
| `skywatchbanner` | Objekt | PaintingObject |  | objects.decorations.paintings | objects.decorations.paintings | ja / ja | 80.0 | RARE |  |
| `walleye` | Objekt | LargePaintingObject |  | objects.decorations.paintings | objects.decorations.paintings | ja / ja | 20.0 | EPIC |  |
| `pottedcloudberry` | Objekt | PotTableDecorationObject |  | objects.decorations.pots | objects.decorations.pots | ja / ja | 20.0 | NORMAL | workstation |
| `barbedwirefence` | Objekt | BarbedWireFenceObject |  | objects.fencesandgates | objects.fencesandgates | ja / ja | 35.0 | NORMAL |  |
| `cloudmarblefence` | Objekt | FenceObject |  | objects.fencesandgates | objects.fencesandgates | ja / ja | 2.0 | NORMAL | workstation |
| `cloudmarblefencegate` | Objekt | FenceGateObject |  | objects.fencesandgates | objects.fencesandgates | ja / ja | 4.0 | NORMAL | workstation |
| `cloudmarblefencegateopen` | Objekt | FenceGateOpenObject |  | objects.fencesandgates | objects.fencesandgates | nein / nein (Teilstück) | 0.0 | NORMAL |  |
| `skyironfence` | Objekt | FenceObject |  | objects.fencesandgates | objects.fencesandgates | ja / ja | 2.0 | NORMAL | workstation |
| `skyironfencegate` | Objekt | FenceGateObject |  | objects.fencesandgates | objects.fencesandgates | ja / ja | 4.0 | NORMAL | workstation |
| `skyironfencegateopen` | Objekt | FenceGateOpenObject |  | objects.fencesandgates | objects.fencesandgates | nein / nein (Teilstück) | 0.0 | NORMAL |  |
| `catbasket` | Objekt | CatBasketObject |  | objects.furniture | objects.furniture | ja / ja | 50.0 | NORMAL |  |
| `salonbarberpole` | Objekt | SalonPoleObject |  | objects.furniture.salon | objects.furniture.salon | ja / ja | 25.0 | RARE |  |
| `salonchair` | Objekt | SalonChairObject |  | objects.furniture.salon | objects.furniture.salon | ja / ja | 30.0 | RARE |  |
| `salonsign` | Objekt | SalonSignObject |  | objects.furniture.salon | objects.furniture.salon | ja / ja | 30.0 | RARE |  |
| `skywatchbed` | Objekt | BedObject |  | objects.furniture.skywatch | objects.furniture.skywatch | ja / ja | 100.0 | NORMAL | workstation |
| `skywatchbed2` | Objekt | Bed2Object |  | objects.furniture.skywatch | objects.furniture.skywatch | nein / nein (Teilstück) | 0.0 | NORMAL |  |
| `skywatchbench` | Objekt | BenchObject |  | objects.furniture.skywatch | objects.furniture.skywatch | ja / ja | 10.0 | NORMAL | workstation |
| `skywatchbench2` | Objekt | Bench2Object |  | objects.furniture.skywatch | objects.furniture.skywatch | nein / nein (Teilstück) | 0.0 | NORMAL |  |
| `skywatchbookshelf` | Objekt | BookshelfObject |  | objects.furniture.skywatch | objects.furniture.skywatch | ja / ja | 10.0 | NORMAL | workstation |
| `skywatchcabinet` | Objekt | CabinetObject |  | objects.furniture.skywatch | objects.furniture.skywatch | ja / ja | 10.0 | NORMAL | workstation |
| `skywatchcandelabra` | Objekt | CandelabraObject |  | objects.furniture.skywatch | objects.furniture.skywatch | ja / ja | 10.0 | NORMAL | workstation |
| `skywatchchair` | Objekt | ChairObject |  | objects.furniture.skywatch | objects.furniture.skywatch | ja / ja | 5.0 | NORMAL | workstation |
| `skywatchclock` | Objekt | ClockObject |  | objects.furniture.skywatch | objects.furniture.skywatch | ja / ja | 10.0 | NORMAL | workstation |
| `skywatchdesk` | Objekt | DeskObject |  | objects.furniture.skywatch | objects.furniture.skywatch | ja / ja | 10.0 | NORMAL | workstation |
| `skywatchdinnertable` | Objekt | DinnerTableObject |  | objects.furniture.skywatch | objects.furniture.skywatch | ja / ja | 20.0 | NORMAL | workstation |
| `skywatchdinnertable2` | Objekt | DinnerTable2Object |  | objects.furniture.skywatch | objects.furniture.skywatch | nein / nein (Teilstück) | 0.0 | NORMAL |  |
| `skywatchdisplay` | Objekt | DisplayStandObject |  | objects.furniture.skywatch | objects.furniture.skywatch | ja / ja | 20.0 | NORMAL | workstation |
| `skywatchdresser` | Objekt | DresserObject |  | objects.furniture.skywatch | objects.furniture.skywatch | ja / ja | 10.0 | NORMAL | workstation |
| `skywatchmodulartable` | Objekt | ModularTableObject |  | objects.furniture.skywatch | objects.furniture.skywatch | ja / ja | 10.0 | NORMAL | workstation |
| `coffinbed` | Objekt | BedObject |  | objects.furniture.twilight | objects.furniture.twilight | ja / ja | 100.0 | NORMAL |  |
| `coffinbed2` | Objekt | Bed2Object |  | objects.furniture.twilight | objects.furniture.twilight | nein / nein (Teilstück) | 0.0 | NORMAL |  |
| `electricchair` | Objekt | ChairObject |  | objects.furniture.twilight | objects.furniture.twilight | ja / ja | 5.0 | NORMAL |  |
| `hangingtree` | Objekt | SkyDecoObject |  | objects.furniture.twilight | objects.furniture.twilight | ja / ja | 20.0 | NORMAL |  |
| `hauntedclock` | Objekt | ClockObject |  | objects.furniture.twilight | objects.furniture.twilight | ja / ja | 10.0 | NORMAL |  |
| `sandwormtombstone` | Objekt | SkyDecoObject |  | objects.furniture.twilight | objects.furniture.twilight | ja / ja | 20.0 | NORMAL |  |
| `skullcandelabra` | Objekt | CandelabraObject |  | objects.furniture.twilight | objects.furniture.twilight | ja / ja | 10.0 | NORMAL |  |
| `twilightcauldron` | Objekt | SkyDecoObject |  | objects.furniture.twilight | objects.furniture.twilight | ja / ja | 20.0 | NORMAL |  |
| `twilightsarcophagus` | Objekt | SarcophagusObject |  | objects.furniture.twilight | objects | ja / ja | 50.0 | EPIC |  |
| `aurorashards` | Objekt | SkyDecoObject | objects.decorations | **objects.landscaping.crystals** | objects.landscaping.crystals | ja / ja | 5.0 | NORMAL | workstation |
| `chargecrystal` | Objekt | SkyDecoObject | objects.decorations | **objects.landscaping.crystals** | objects.landscaping.crystals | ja / ja | 5.0 | NORMAL | workstation |
| `starfall` | Objekt | SkyDecoObject | objects.decorations | **objects.landscaping.crystals** | objects.landscaping.crystals | ja / ja | 10.0 | NORMAL | workstation |
| `brokenangel` | Objekt | StatueObject |  | objects.landscaping.masonry | objects.landscaping.masonry | nein / nein | 0.0 | NORMAL |  |
| `gloomravenstatue` | Objekt | StatueObject |  | objects.landscaping.masonry | objects.landscaping.masonry | ja / ja | 20.0 | NORMAL | workstation |
| `mournerstatue` | Objekt | StatueObject |  | objects.landscaping.masonry | objects.landscaping.masonry | nein / nein | 0.0 | NORMAL |  |
| `seraphstatue` | Objekt | StatueObject |  | objects.landscaping.masonry | objects.landscaping.masonry | ja / ja | 40.0 | NORMAL | workstation |
| `skywatchrubble` | Objekt | SkyDecoObject | objects.decorations | **objects.landscaping.masonry** | objects.landscaping.masonry | ja / ja | 2.0 | NORMAL | workstation |
| `auroralily` | Objekt | GrassObject |  | objects.landscaping.plants | objects.landscaping.plants | ja / ja | 1.0 | NORMAL |  |
| `cloudbell` | Objekt | GrassObject |  | objects.landscaping.plants | objects.landscaping.plants | ja / ja | 1.0 | NORMAL |  |
| `cloudberrybush` | Objekt | FruitBushObject |  | objects.landscaping.plants | objects | nein / ja | 0.0 | NORMAL |  |
| `cloudberrysapling` | Objekt | SaplingObject |  | objects.landscaping.plants | objects | ja / ja | 30.0 | NORMAL |  |
| `cloudsapling` | Objekt | TreeSaplingObject |  | objects.landscaping.plants | objects | ja / ja | 5.0 | NORMAL |  |
| `cloudtree` | Objekt | SkyTreeObject |  | objects.landscaping.plants | objects | nein / ja | 0.0 | NORMAL |  |
| `cragbloom` | Objekt | GrassObject |  | objects.landscaping.plants | objects.landscaping.plants | ja / ja | 1.0 | NORMAL |  |
| `deadheavenbloom` | Objekt | SteinfeldPlantObject |  | objects.landscaping.plants | objects.landscaping.plants | nein / nein | 0.0 | NORMAL |  |
| `deadtree` | Objekt | SkyDecoObject | objects.decorations | **objects.landscaping.plants** | objects.landscaping.plants | ja / ja | 4.0 | NORMAL |  |
| `ectoplasmfern` | Objekt | GhostPlantObject |  | objects.landscaping.plants | objects.landscaping.plants | nein / nein | 0.0 | NORMAL |  |
| `fulgurpine` | Objekt | TreeObject |  | objects.landscaping.plants | objects | nein / ja | 0.0 | NORMAL |  |
| `fulgursapling` | Objekt | TreeSaplingObject |  | objects.landscaping.plants | objects | ja / ja | 5.0 | NORMAL |  |
| `ghostlily` | Objekt | GhostPlantObject |  | objects.landscaping.plants | objects.landscaping.plants | nein / nein | 0.0 | NORMAL |  |
| `ghostmushroom` | Objekt | SteinfeldPlantObject |  | objects.landscaping.plants | objects.landscaping.plants | nein / nein | 0.0 | NORMAL |  |
| `giantfigtree` | Objekt | FruitTreeObject |  | objects.landscaping.plants | objects | ja / ja | 100.0 | NORMAL |  |
| `gloomwillow` | Objekt | SkyDecoObject |  | objects.landscaping.plants | objects.landscaping.plants | ja / ja | 15.0 | NORMAL | workstation |
| `glowfern` | Objekt | GrassObject |  | objects.landscaping.plants | objects.landscaping.plants | ja / ja | 1.0 | NORMAL |  |
| `knowledgetree` | Objekt | DryadTreeObject |  | objects.landscaping.plants | objects | ja / ja | 100.0 | NORMAL |  |
| `mourningrose` | Objekt | GhostPlantObject |  | objects.landscaping.plants | objects.landscaping.plants | nein / nein | 0.0 | NORMAL |  |
| `nimbussapling` | Objekt | TreeSaplingObject |  | objects.landscaping.plants | objects | ja / ja | 5.0 | NORMAL |  |
| `nimbuswillow` | Objekt | TreeObject |  | objects.landscaping.plants | objects | nein / ja | 0.0 | NORMAL |  |
| `overgrowngrass` | Objekt | NaturalGrassObject |  | objects.landscaping.plants | objects.landscaping.plants | ja / ja | 1.0 | NORMAL |  |
| `palereed` | Objekt | SteinfeldPlantObject |  | objects.landscaping.plants | objects.landscaping.plants | nein / nein | 0.0 | NORMAL |  |
| `paradisefern` | Objekt | GrassObject |  | objects.landscaping.plants | objects.landscaping.plants | nein / nein | 0.0 | NORMAL |  |
| `paradisepalm` | Objekt | TreeObject |  | objects.landscaping.plants | objects | nein / ja | 0.0 | NORMAL |  |
| `prismabirch` | Objekt | TreeObject |  | objects.landscaping.plants | objects | nein / ja | 0.0 | NORMAL |  |
| `prismasapling` | Objekt | TreeSaplingObject |  | objects.landscaping.plants | objects | ja / ja | 5.0 | NORMAL |  |
| `prismgrass` | Objekt | GrassObject |  | objects.landscaping.plants | objects.landscaping.plants | nein / nein | 0.0 | NORMAL |  |
| `skylichen` | Objekt | GrassObject |  | objects.landscaping.plants | objects.landscaping.plants | ja / ja | 1.0 | NORMAL |  |
| `skyreeds` | Objekt | NaturalGrassObject |  | objects.landscaping.plants | objects.landscaping.plants | ja / ja | 1.0 | NORMAL |  |
| `skyscree` | Objekt | GrassObject |  | objects.landscaping.plants | objects.landscaping.plants | ja / ja | 1.0 | NORMAL |  |
| `skyseraphsapling` | Objekt | TreeSaplingObject |  | objects.landscaping.plants | objects | ja / ja | 5.0 | NORMAL |  |
| `skyseraphtree` | Objekt | SkyTreeObject |  | objects.landscaping.plants | objects | nein / ja | 0.0 | NORMAL |  |
| `skytulip` | Objekt | GrassObject |  | objects.landscaping.plants | objects.landscaping.plants | ja / ja | 1.0 | NORMAL |  |
| `spiritmosspatch` | Objekt | SteinfeldPlantObject |  | objects.landscaping.plants | objects.landscaping.plants | nein / nein | 0.0 | NORMAL |  |
| `spiritmushroom` | Objekt | GhostPlantObject |  | objects.landscaping.plants | objects.landscaping.plants | nein / nein | 0.0 | NORMAL |  |
| `staticmoss` | Objekt | GrassObject |  | objects.landscaping.plants | objects.landscaping.plants | ja / ja | 1.0 | NORMAL |  |
| `stormsedge` | Objekt | GrassObject |  | objects.landscaping.plants | objects.landscaping.plants | nein / nein | 0.0 | NORMAL |  |
| `tallcloudgrass` | Objekt | GrassObject |  | objects.landscaping.plants | objects.landscaping.plants | nein / nein | 0.0 | NORMAL |  |
| `thunderbloom` | Objekt | GrassObject |  | objects.landscaping.plants | objects.landscaping.plants | ja / ja | 1.0 | NORMAL |  |
| `treeofplenty` | Objekt | FruitTreeObject |  | objects.landscaping.plants | objects | ja / ja | 100.0 | NORMAL |  |
| `whisperreeds` | Objekt | NaturalGrassObject |  | objects.landscaping.plants | objects.landscaping.plants | ja / ja | 1.0 | NORMAL |  |
| `widowflower` | Objekt | SteinfeldPlantObject |  | objects.landscaping.plants | objects.landscaping.plants | nein / nein | 0.0 | NORMAL |  |
| `widowvine` | Objekt | GhostPlantObject |  | objects.landscaping.plants | objects.landscaping.plants | nein / nein | 0.0 | NORMAL |  |
| `witheredtuft` | Objekt | SteinfeldPlantObject |  | objects.landscaping.plants | objects.landscaping.plants | nein / nein | 0.0 | NORMAL |  |
| `withershrub` | Objekt | SkyDecoObject | objects.decorations | **objects.landscaping.plants** | objects.landscaping.plants | ja / ja | 2.0 | NORMAL | workstation |
| `aetheriumrock` | Objekt | RockOreObject |  | objects.landscaping.rocksandores | objects.landscaping.rocksandores | ja / ja | 0.0 | NORMAL |  |
| `aurorabloom` | Objekt | CrystalClusterObject |  | objects.landscaping.rocksandores | objects.landscaping.rocksandores | ja / ja | 30.0 | NORMAL |  |
| `evilwall` | Objekt | RockObject |  | objects.landscaping.rocksandores | objects.landscaping.rocksandores | ja / ja | 0.0 | NORMAL |  |
| `fulguriterock` | Objekt | RockOreObject |  | objects.landscaping.rocksandores | objects.landscaping.rocksandores | ja / ja | 0.0 | NORMAL |  |
| `gravesaltrock` | Objekt | RockObject |  | objects.landscaping.rocksandores | objects.landscaping.rocksandores | ja / ja | 0.0 | NORMAL |  |
| `palestonerock` | Objekt | RockObject |  | objects.landscaping.rocksandores | objects.landscaping.rocksandores | ja / ja | 0.0 | NORMAL |  |
| `prismshardrock` | Objekt | RockOreObject |  | objects.landscaping.rocksandores | objects.landscaping.rocksandores | ja / ja | 0.0 | NORMAL |  |
| `skystonerock` | Objekt | RockObject |  | objects.landscaping.rocksandores | objects.landscaping.rocksandores | ja / ja | 0.0 | NORMAL |  |
| `stormcrystal` | Objekt | CrystalClusterObject |  | objects.landscaping.rocksandores | objects.landscaping.rocksandores | ja / ja | 0.0 | NORMAL |  |
| `stormscreed` | Objekt | SkyDecoObject | objects.decorations | **objects.landscaping.rocksandores** | objects.landscaping.rocksandores | ja / ja | 2.0 | NORMAL | workstation |
| `veilrock` | Objekt | RockObject |  | objects.landscaping.rocksandores | objects.landscaping.rocksandores | ja / ja | 0.0 | NORMAL |  |
| `saloncashregister` | Objekt | SalonTableObject |  | objects.landscaping.tabledecorations | objects.landscaping.tabledecorations | ja / ja | 30.0 | RARE |  |
| `salonproducts` | Objekt | SalonTableObject |  | objects.landscaping.tabledecorations | objects.landscaping.tabledecorations | ja / ja | 20.0 | RARE |  |
| `skywatchcandle` | Objekt | TableDecorationObject |  | objects.landscaping.tabledecorations | objects.landscaping.tabledecorations | ja / ja | 20.0 | NORMAL | workstation |
| `skywatchchalice` | Objekt | TableDecorationObject |  | objects.landscaping.tabledecorations | objects.landscaping.tabledecorations | ja / ja | 20.0 | NORMAL | workstation |
| `skywatchtome` | Objekt | TableDecorationObject |  | objects.landscaping.tabledecorations | objects.landscaping.tabledecorations | ja / ja | 20.0 | NORMAL | workstation |
| `thingbox` | Objekt | TableDecorationObject |  | objects.landscaping.tabledecorations | objects.landscaping.tabledecorations | ja / ja | 20.0 | NORMAL |  |
| `flickerlightgarland` | Objekt | SkyWallLightObject |  | objects.lighting | objects.lighting | ja / ja | 50.0 | NORMAL |  |
| `ghostlantern` | Objekt | StreetlampObject |  | objects.lighting | objects.lighting | ja / ja | 30.0 | NORMAL | workstation |
| `mistglasslantern` | Objekt | SkyWallLightObject |  | objects.lighting | objects.lighting | ja / ja | 10.0 | NORMAL | workstation |
| `wardencandelabra` | Objekt | StreetlampObject |  | objects.lighting | objects.lighting | ja / ja | 30.0 | NORMAL | workstation |
| `bossportalcrookedbeyond` | Objekt | BossPortalObject |  | objects.misc | objects.misc | nein / nein | 0.0 | NORMAL |  |
| `bossportaleden` | Objekt | BossPortalObject |  | objects.misc | objects.misc | nein / nein | 0.0 | NORMAL |  |
| `bossportalghostrealm` | Objekt | BossPortalObject |  | objects.misc | objects.misc | nein / nein | 0.0 | NORMAL |  |
| `bossportalhell` | Objekt | BossPortalObject |  | objects.misc | objects.misc | nein / nein | 0.0 | NORMAL |  |
| `bossportalskyreach` | Objekt | BossPortalObject |  | objects.misc | objects.misc | nein / nein | 0.0 | NORMAL |  |
| `bossportalsteinfeld` | Objekt | BossPortalObject |  | objects.misc | objects.misc | nein / nein | 0.0 | NORMAL |  |
| `crookedcrate` | Objekt | RandomCrateObject |  | objects.misc | objects.landscaping.misc | nein / nein | 0.0 | NORMAL |  |
| `crookeddoordown` | Objekt | CrookedDoorObject |  | objects.misc | objects.misc | nein / nein | 0.0 | EPIC |  |
| `crookeddoorup` | Objekt | CrookedSideDoorObject |  | objects.misc | objects.misc | nein / nein | 0.0 | NORMAL |  |
| `edengatedown` | Objekt | EdenGateObject |  | objects.misc | objects.misc | nein / nein | 0.0 | RARE |  |
| `edengateup` | Objekt | EdenSideGateObject |  | objects.misc | objects.misc | nein / nein | 0.0 | NORMAL |  |
| `edenseedbasin` | Objekt | EdenSeedBasinObject |  | objects.misc | objects.misc | ja / ja | 20.0 | NORMAL | tungstenworkstation |
| `ghostgatedown` | Objekt | GhostGateObject |  | objects.misc | objects.misc | nein / nein | 0.0 | EPIC |  |
| `ghostgateup` | Objekt | GhostSideGateObject |  | objects.misc | objects.misc | nein / nein | 0.0 | NORMAL |  |
| `seancecircle` | Objekt | SeanceCircleObject |  | objects.misc | objects.misc | nein / nein | 15.0 | NORMAL |  |
| `skycache` | Objekt | SkyCacheObject |  | objects.misc | objects.landscaping.misc | nein / nein | 0.0 | NORMAL |  |
| `skycrate` | Objekt | RandomCrateObject |  | objects.misc | objects.landscaping.misc | nein / nein | 0.0 | NORMAL |  |
| `skystairwaydown` | Objekt | SkywardStairwayObject |  | objects.misc | objects.misc | ja / ja | 20.0 | UNCOMMON | tungstenworkstation |
| `skystairwayup` | Objekt | SkySideStairwayObject |  | objects.misc | objects.misc | nein / nein | 0.0 | NORMAL |  |
| `soulbasin` | Objekt | SoulBasinObject |  | objects.misc | objects.misc | ja / ja | 25.0 | NORMAL | tungstenworkstation |
| `veilriftdown` | Objekt | VeilRiftObject |  | objects.misc | objects.misc | nein / nein | 0.0 | EPIC |  |
| `veilriftup` | Objekt | VeilSideRiftObject |  | objects.misc | objects.misc | nein / nein | 0.0 | NORMAL |  |
| `veterancatapult` | Objekt | VeteranCatapultObject | objects.decorations | **objects.traps** | objects.misc | ja / ja | 150.0 | NORMAL |  |
| `veterancatapult2` | Objekt | VeteranCatapultObject | objects.decorations | **objects.traps** | objects.misc | nein / nein (Teilstück) | 0.0 | NORMAL |  |
| `veterancatapult3` | Objekt | VeteranCatapultObject | objects.decorations | **objects.traps** | objects.misc | nein / nein (Teilstück) | 0.0 | NORMAL |  |
| `veterancatapult4` | Objekt | VeteranCatapultObject | objects.decorations | **objects.traps** | objects.misc | nein / nein (Teilstück) | 0.0 | NORMAL |  |
| `veterancatapult5` | Objekt | VeteranCatapultObject | objects.decorations | **objects.traps** | objects.misc | nein / nein (Teilstück) | 0.0 | NORMAL |  |
| `veterancatapult6` | Objekt | VeteranCatapultObject | objects.decorations | **objects.traps** | objects.misc | nein / nein (Teilstück) | 0.0 | NORMAL |  |
| `veterancatapult7` | Objekt | VeteranCatapultObject | objects.decorations | **objects.traps** | objects.misc | nein / nein (Teilstück) | 0.0 | NORMAL |  |
| `veterancatapult8` | Objekt | VeteranCatapultObject | objects.decorations | **objects.traps** | objects.misc | nein / nein (Teilstück) | 0.0 | NORMAL |  |
| `veterancatapult9` | Objekt | VeteranCatapultObject | objects.decorations | **objects.traps** | objects.misc | nein / nein (Teilstück) | 0.0 | NORMAL |  |
| `veteranturret` | Objekt | VeteranTurretObject | objects.decorations | **objects.traps** | objects.misc | ja / ja | 60.0 | NORMAL |  |
| `beetledoor` | Objekt | WallDoorObject |  | objects.wallsanddoors | objects.wallsanddoors | ja / ja | 60.0 | NORMAL | workstation |
| `beetledoorlocked` | Objekt | WallDoorLockedObject |  | objects.wallsanddoors | objects.wallsanddoors | nein / nein | 0.0 | NORMAL |  |
| `beetledooropen` | Objekt | WallDoorOpenObject |  | objects.wallsanddoors | objects.wallsanddoors | nein / nein (Teilstück) | 0.0 | NORMAL |  |
| `beetledoorunlocked` | Objekt | WallDoorUnlockedObject |  | objects.wallsanddoors | objects.wallsanddoors | nein / nein | 0.0 | NORMAL |  |
| `beetlewall` | Objekt | WallObject |  | objects.wallsanddoors | objects.wallsanddoors | ja / ja | 7.5 | NORMAL | workstation |
| `beetlewindow` | Objekt | WallWindowObject |  | objects.wallsanddoors | objects.wallsanddoors | nein / nein | 0.0 | NORMAL |  |
| `cloudmarbledoor` | Objekt | WallDoorObject |  | objects.wallsanddoors | objects.wallsanddoors | ja / ja | 46.0 | NORMAL | workstation |
| `cloudmarbledoorlocked` | Objekt | WallDoorLockedObject |  | objects.wallsanddoors | objects.wallsanddoors | nein / nein | 0.0 | NORMAL |  |
| `cloudmarbledooropen` | Objekt | WallDoorOpenObject |  | objects.wallsanddoors | objects.wallsanddoors | nein / nein (Teilstück) | 0.0 | NORMAL |  |
| `cloudmarbledoorunlocked` | Objekt | WallDoorUnlockedObject |  | objects.wallsanddoors | objects.wallsanddoors | nein / nein | 0.0 | NORMAL |  |
| `cloudmarblewall` | Objekt | WallObject |  | objects.wallsanddoors | objects.wallsanddoors | ja / ja | 7.5 | NORMAL | workstation |
| `cloudmarblewindow` | Objekt | WallWindowObject |  | objects.wallsanddoors | objects.wallsanddoors | nein / nein | 0.0 | NORMAL |  |
| `hellbrickdoor` | Objekt | WallDoorObject |  | objects.wallsanddoors | objects.wallsanddoors | ja / ja | 0.0 | NORMAL |  |
| `hellbrickdoorlocked` | Objekt | WallDoorLockedObject |  | objects.wallsanddoors | objects.wallsanddoors | nein / nein | 0.0 | NORMAL |  |
| `hellbrickdooropen` | Objekt | WallDoorOpenObject |  | objects.wallsanddoors | objects.wallsanddoors | nein / nein (Teilstück) | 0.0 | NORMAL |  |
| `hellbrickdoorunlocked` | Objekt | WallDoorUnlockedObject |  | objects.wallsanddoors | objects.wallsanddoors | nein / nein | 0.0 | NORMAL |  |
| `hellbrickwall` | Objekt | WallObject |  | objects.wallsanddoors | objects.wallsanddoors | ja / ja | 0.0 | NORMAL |  |
| `hellbrickwindow` | Objekt | WallWindowObject |  | objects.wallsanddoors | objects.wallsanddoors | nein / nein | 0.0 | NORMAL |  |
| `nightfelldoor` | Objekt | WallDoorObject |  | objects.wallsanddoors | objects.wallsanddoors | ja / ja | 29.0 | NORMAL | workstation |
| `nightfelldoorlocked` | Objekt | WallDoorLockedObject |  | objects.wallsanddoors | objects.wallsanddoors | nein / nein | 0.0 | NORMAL |  |
| `nightfelldooropen` | Objekt | WallDoorOpenObject |  | objects.wallsanddoors | objects.wallsanddoors | nein / nein (Teilstück) | 0.0 | NORMAL |  |
| `nightfelldoorunlocked` | Objekt | WallDoorUnlockedObject |  | objects.wallsanddoors | objects.wallsanddoors | nein / nein | 0.0 | NORMAL |  |
| `nightfellwall` | Objekt | WallObject |  | objects.wallsanddoors | objects.wallsanddoors | ja / ja | 7.3 | NORMAL | workstation |
| `nightfellwindow` | Objekt | WallWindowObject |  | objects.wallsanddoors | objects.wallsanddoors | nein / nein | 0.0 | NORMAL |  |
| `skystonebrickdoor` | Objekt | WallDoorObject |  | objects.wallsanddoors | objects.wallsanddoors | ja / ja | 6.0 | NORMAL | workstation |
| `skystonebrickdoorlocked` | Objekt | WallDoorLockedObject |  | objects.wallsanddoors | objects.wallsanddoors | nein / nein | 0.0 | NORMAL |  |
| `skystonebrickdooropen` | Objekt | WallDoorOpenObject |  | objects.wallsanddoors | objects.wallsanddoors | nein / nein (Teilstück) | 0.0 | NORMAL |  |
| `skystonebrickdoorunlocked` | Objekt | WallDoorUnlockedObject |  | objects.wallsanddoors | objects.wallsanddoors | nein / nein | 0.0 | NORMAL |  |
| `skystonebrickwall` | Objekt | WallObject |  | objects.wallsanddoors | objects.wallsanddoors | ja / ja | 1.0 | NORMAL | workstation |
| `skystonebrickwindow` | Objekt | WallWindowObject |  | objects.wallsanddoors | objects.wallsanddoors | nein / nein | 0.0 | NORMAL |  |
| `veteranbarricade` | Objekt | WallObject |  | objects.wallsanddoors | objects.wallsanddoors | ja / ja | 40.0 | NORMAL |  |
| `charfloortile` | Boden | SimpleFloorTile |  | tiles.floors | tiles | ja / ja | 1.0 | NORMAL | workstation |
| `gloomwoodfloortile` | Boden | SimpleFloorTile |  | tiles.floors | tiles | ja / ja | 1.0 | NORMAL | workstation |
| `hellbrickfloortile` | Boden | CheckerFloorTile |  | tiles.floors | tiles | nein / nein | 0.0 | NORMAL |  |
| `marblecheckertile` | Boden | CheckerFloorTile |  | tiles.floors | tiles | ja / ja | 1.0 | NORMAL | workstation |
| `nimbusfloortile` | Boden | SimpleFloorTile |  | tiles.floors | tiles | ja / ja | 1.0 | NORMAL | workstation |
| `prismfloortile` | Boden | SimpleFloorTile |  | tiles.floors | tiles | ja / ja | 1.0 | NORMAL | workstation |
| `skywaypathtile` | Boden | PathTiledTile |  | tiles.floors | tiles | ja / ja | 1.0 | NORMAL |  |
| `ectoplasmtile` | Boden | EctoplasmTile |  | tiles.liquids | tiles | nein / nein | 0.0 | NORMAL |  |
| `edenshallowstile` | Boden | EdenShallowsTile |  | tiles.liquids | tiles | nein / nein | 0.0 | NORMAL |  |
| `mistseatile` | Boden | MistseaTile |  | tiles.liquids | tiles | nein / nein | 0.0 | NORMAL |  |
| `murkwatertile` | Boden | MurkwaterTile |  | tiles.liquids | tiles | nein / nein | 0.0 | NORMAL |  |
| `spilltile` | Boden | SpillTile |  | tiles.liquids | tiles | nein / nein | 0.0 | NORMAL |  |
| `ashgrasstile` | Boden | AshGrassTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `ashsandtile` | Boden | AshsandTile |  | tiles.terrain | tiles | ja / ja | 1.0 | NORMAL |  |
| `aurorashoaltile` | Boden | AuroraShoalTile |  | tiles.terrain | tiles | ja / ja | 1.0 | NORMAL |  |
| `beetlefreaktile` | Boden | BeetlefreakTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL | tungstenlandscaping |
| `blackcobbletile` | Boden | GhostGroundTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `blackpeattile` | Boden | BlackpeatTile |  | tiles.terrain | tiles | ja / ja | 1.0 | NORMAL |  |
| `bonegraveltile` | Boden | BoneGravelTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `brimstonecrusttile` | Boden | BrimstoneCrustTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `checkerstonetile` | Boden | CheckerStoneTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `cinderashtile` | Boden | CinderAshTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `cloudturftile` | Boden | CloudturfTile |  | tiles.terrain | tiles | ja / ja | 1.0 | NORMAL |  |
| `crackedmarbletile` | Boden | CrackedMarbleTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `crookedstripetile` | Boden | CrookedStripeTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `deadsoiltile` | Boden | DeadSoilTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `edenmosstile` | Boden | EdenMossTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `edenrootfloortile` | Boden | EdenRootFloorTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `edensoiltile` | Boden | EdenSoilTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `furnaceslagtile` | Boden | FurnaceSlagTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `ghostmosstile` | Boden | GhostGroundTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `gravesoiltile` | Boden | GraveSoilTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `graveyardsoiltile` | Boden | GhostGroundTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `hauntedgrasstile` | Boden | GhostGroundTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `miststonetile` | Boden | MistStoneTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `murkmosstile` | Boden | MurkmossTile |  | tiles.terrain | tiles | ja / ja | 1.0 | NORMAL |  |
| `overgrownedentile` | Boden | OvergrownEdenTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `palegrasstile` | Boden | PaleGrassTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `paradisesandtile` | Boden | ParadiseSandTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `skystonetile` | Boden | SkystoneTile |  | tiles.terrain | tiles | ja / ja | 1.0 | NORMAL |  |
| `skywaytile` | Boden | SkywayTile |  | tiles.terrain | tiles | ja / ja | 1.0 | NORMAL |  |
| `spiralsoiltile` | Boden | SpiralSoilTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `spiritstonetile` | Boden | GhostGroundTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `stormslatetile` | Boden | StormslateTile |  | tiles.terrain | tiles | ja / ja | 1.0 | NORMAL |  |
| `violetdirttile` | Boden | GhostGroundTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `violetmudtile` | Boden | VioletMudTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `weatheredstonetile` | Boden | WeatheredStoneTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `wrongwaytile` | Boden | WrongWayTile |  | tiles.terrain | tiles | nein / ja | 0.0 | NORMAL |  |
| `ashspiritspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `auroraflakespawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `bloodthrallspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `bloommawspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `boilerhoundspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `cindercantorspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `coffincrawlerspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `crookedarmadillospawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `crookedgolemspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `dawnpiercerspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `doormimicspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `drifterspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `edenserpentspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `fenwraithspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `forbiddenserpentspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `galehoundspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `gloomshadespawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `goldenhornetspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `gravecrowspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `headlessbutlerspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `hoardmimicspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `hollowangelspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `infernalclerkspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `jealousvinespawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `lanternwidowspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `lostpilgrimspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `mistserpentspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `mourningbridespawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `possessedchairspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `prototypeninespawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `rarecrookedgolemspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `rimesentryspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `skystonegolemspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `soulhoundspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `sourvatbloomspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `stonemournerspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `stormwispspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `stripedmegasharkspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `ticketimpspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `tollwrightspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `tongueplantspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `vatlingspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `veilbloomspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
| `zephyrrayspawnitem` | Spawn-Item | MobSpawnItem |  | mobs.hostile | misc | nein / ja | 50.0 | UNIQUE |  |
<!-- TABLE:END -->
