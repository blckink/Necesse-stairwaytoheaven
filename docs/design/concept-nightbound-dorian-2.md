# Konzept 2: Dorian spürbar machen — Blutkreislauf der Siedlung

**Status: ENTWURF, wartet auf Kevins Entscheidungen (D1–D5 unten).** Nichts
davon ist gebaut. Stand der Recherche: 2026-09-26. Gelesen wurden der Code auf
`drive/spielstand-20260907` (89248b5) und das dekompilierte Vanilla-Jar 1.3.3.
Anlass ist Kevins Spieltest am 2026-09-26 mit dem Screenshot von „Dorian Ma[son]“
(Krieger, Bambus-Architekt, Hersteller, Blutrünstig, Edelsteinsammler).

Das erste Konzept (`concept-nightbound-dorian.md`) ist seit 6f4a71c/f9201ae
**fast vollständig gebaut**. Das Problem ist nicht, dass etwas fehlt. **Man sieht
es im Spiel nicht.** Deshalb beginnt dieses Konzept mit der Frage „warum sieht
man es nicht?“.

---

## 1. Kevins Punkte: was gebaut ist und warum man es nicht merkt

| Kevins Punkt | Was gebaut ist | Warum er es nicht merkt |
|---|---|---|
| „Man kriegt nicht mit, dass er Tiere killt.“ | Nachtjagd außerhalb der Grenzen. Die Beute (Blutfläschchen, 40 % Hammel) geht **still ins Siedlungslager** (Kevins Entscheidung 2026-09-25). Den Nachtbericht gibt es nur in **Dorians Dialog** („Was war letzte Nacht?“). | Keine Spur im Spiel. Das Fläschchen landet in irgendeiner Kiste, die Sprechblase sieht man nur daneben, nachts, draußen. Den Nachtbericht muss man kennen, um ihn zu öffnen. |
| „Noch keines der Vampir-Tiere gesehen.“ | 1 von 6 ausgesaugten Tieren steht als **Blutknecht** (`bloodthrall`) wieder auf, feindlich. | Er ist **1:1 die Vanilla-Krypta-Fledermaus**: gleiches Aussehen, entsteht draußen an der Jagdstelle, nachts. Wer ihn sieht, hält ihn für eine normale Fledermaus. |
| „Man muss merken, wenn jemand gebissen wurde … Ausrufezeichen wie bei Hunger.“ | **Gibt es schon**: die Siedlungsmeldung `swhbloodfever`, am selben Ort und mit demselben Ausrufezeichen wie „Hunger“ und „kein Bett“. Der Tooltip nennt die Gebissenen mit Namen. | Sie erscheint **nur nach einem Biss**. Dein Dorian ist satt, er jagt („Blutrünstig: Gegnern wehgetan x4“). Also hat er vermutlich noch nie gebissen, und die Meldung kam nie. Das ist richtig so, aber es fehlt die **Vorwarnung**. |
| „Jemand fällt paar Tage aus.“ | Blutfieber hält **einen Spieltag**: −20 % Tempo, −15 % Abbau/Bauen, −10 % Leben. | Kurz und mild. Man merkt es kaum, und „fällt aus“ stimmt nicht. → **D4**. |
| „Blutflaschen-Teil durch Siedler befüllbar wie Kiste/Trog, Vorrat einstellbar.“ | Die Blutschale ist ein **Klick-Objekt**: Man klickt mit Fläschchen im Inventar, der Bestand steht in `NightboundWorldData`. | Siedler können sie **nicht** befüllen, es gibt keine Vorratsgrenze. → **Abschnitt 3**, sicher umsetzbar. |
| „Eigenschaften zugeschnitten auf Vampir, nicht random.“ | Keine Eigenschaften-Logik für Dorian. Vanilla würfelt sie aus seinem Siedler-Seed. | Deshalb „Bambus-Architekt“, „Hersteller“, „Edelsteinsammler“. → **Abschnitt 4**. |
| „Dorian Mason? Was ist der Name?“ | **Seit 89248b5 (2026-09-26 02:35) behoben** durch einen anderen Auftrag. Story-Bewohner haben keinen Zufallsnamen mehr: Siedlerliste „Dorian“, Dialogkopf „Dorian, Nachtgebundener“ (Vanilla-Muster „Gloria, Schmied“). Alte Spielstände übernehmen den Namen beim Laden. | **Noch nicht in deinem Spiel**: Das ausgelieferte Jar ist von 00:24, der Build mit dem Fix von 02:34 liegt noch nicht im Mods-Ordner. |
| „Der Therapeut — wieso Der?“ | Ebenfalls in 89248b5: Berufs-NPCs zeigen „<Name>, Therapeut“ ohne Artikel („Greta, der Arzt“ ist weg). | Ebenfalls noch nicht ausgeliefert. |

**Glätten (in jedem Fall):** „Nachtgebundener“ ist als Berufsbezeichnung
schwer lesbar. Vanilla schreibt hinter das Komma, **was jemand tut**: Schmied,
Jäger. Vorschlag **D5**: „Dorian, Vampir“. „Der Nachtgebundene“ bleibt als
Beiname in seinen Texten.

---

## 2. Leitidee: ein Blutkreislauf, der sich automatisieren lässt

Necesse belohnt das Automatisieren: Siedler befüllen Kisten, Tröge und Öfen,
und man stellt nur ein, wie viel wohin soll. Dorian wird genau so ein Kreislauf.
Solange er läuft, **ist er nie Thema**. Er meldet sich erst, wenn etwas stockt:

```
  Jagd (Dorian, nachts)  ─┐
  Alchemietisch          ─┼─► Siedlungslager ──(Träger)──► Blutschale ──► Dorian trinkt
  (Fleisch + Glasflasche) ┘                                (Vorrat z.B. 5)   (und später
                                                                             die Gewandelten)
```

- **Läuft der Kreislauf**, gibt es keine Meldung und keinen Biss. Einmal
  einrichten, dann Ruhe, wie beim Futtertrog.
- **Stockt er** (Schale leer, Dorian durstig), kommt **eine** Vorwarnung als
  Siedlungsmeldung. Man hat dann noch eine Nacht Zeit zum Reagieren.
- **Ignoriert man sie**, beißt er. Die Meldung „gebissen“ kommt mit Namen, und
  der Gebissene fällt aus (D4).
- **Der Twist** (D1): Wer gebissen wurde, kann beim Arzt **verwandelt** statt
  geheilt werden. Dann arbeitet er Tag und Nacht, braucht aber selbst Blut aus
  der Schale. Am Ende steht eine Siedlung, die rund um die Uhr läuft und an
  einer Blutversorgung hängt, die man selbst gebaut hat.

---

## 3. Blutschale als Lager wie der Futtertrog (sicher umsetzbar)

**Vanilla-Vorbild, VERIFIED [jar] 1.3.3:** `FeedingTroughObject` +
`FeedingTroughObjectEntity`.

- Die Objekt-Entität implementiert `OEInventory` (10 Plätze).
- `isSettlementStorageItemDisabled(item)` sperrt alles außer Futter. Deshalb
  kann man den Trog im Siedlungsmenü als Lager markieren, und die Träger bringen
  nur Passendes.
- Quickstack, Auffüllen, Sortieren und Crafting in der Nähe sind aus (`false`).
- `interact` öffnet `ContainerRegistry.OE_INVENTORY_CONTAINER`, also das normale
  Kistenfenster.

**Umbau der Blutschale:**

1. `BloodBowlObject.getNewObjectEntity` → neue `BloodBowlObjectEntity implements OEInventory`
   - 10 Plätze, Filter nur `bloodvial`;
   - `isSettlementStorageItemDisabled = item != bloodvial`;
   - Quickstack/Auffüllen/Sortieren/Crafting aus.
2. Klick öffnet das Kistenfenster. Das Klick-Nachfüllen entfällt, denn
   Schieben per Hand geht im Fenster ganz normal.
3. **„Wie viele Flaschen auf Vorrat“** ist Vanillas Lagerfilter, ohne eigene
   Oberfläche. Im Siedlungsmenü die Schale als Lager markieren, dann „Limit“
   (`ItemCategoriesFilter.maxAmount` / Einzel-Limits, VERIFIED [jar]), z. B. 5.
   Die Träger halten diesen Stand von selbst.
   - **Vorschlag:** Mit `setupDefaultSettlementStorage` bekommt die Schale beim
     Markieren gleich hohe Priorität, so wie die Versandkiste
     (`ShippingChestObjectEntity`) ihre Voreinstellung setzt.
4. `drinkNear` liest die Inventare der Schalen statt `NightboundWorldData`.
5. **Übernahme alter Spielstände:** Beim ersten Tick bzw. Klick wandert der alte
   Bestand aus `NightboundWorldData` in die Entität.
   - **HYPOTHESE:** Ob eine schon platzierte Schale beim Laden überhaupt eine
     Entität bekommt, wenn die Klasse neu `getNewObjectEntity` hat, ist nicht
     geprüft. Vor dem Bau im Ladepfad von `Level` nachlesen. Notfalls die
     Entität beim ersten Klick anlegen.
6. Aussehen: weiter `spiritbasin`, keine neue Grafik.
   - **Optional:** eine gefüllte Schale dunkler zeichnen (Tönung), so wie der
     Trog „hasFeed“ zeigt. Dann sieht man, ob sie leer ist.

---

## 4. Eigenschaften: fest und vampirisch statt gewürfelt

**Vanilla, VERIFIED [jar] 1.3.3:**

- `HumanMob.setupPersonalities()` ist `protected` (HumanMob.java:1787) und
  würfelt über `SettlerPersonalityRegistry.getNewRandomSettlerPersonalities`.
- `registerSettlerPersonality(..., new SimplePersonalityFilter(n).makeSettlerStringIDsWhitelist().filterSettlerStringID(...), bonus)`
  ist öffentlich. So bekommt nur der Älteste „elder“.

**Vorschlag D2: Dorians feste Eigenschaften**

| Eigenschaft | Art | Warum sie passt |
|---|---|---|
| **Krieger** (`warrior`) | Bonus (Kampf) | Er jagt, ist in der Abenteuergruppe nützlich. Hat er schon. |
| **Blutrünstig** (`bloodthirsty`) | normal | Freut sich, wenn er Gegnern wehtut. Passt genau. Hat er schon. |
| **Sargschläfer** *(neu, nur Dorian)* | normal | Mag es, wenn in seinem Zimmer ein Sarkophag oder Sargbett steht. Das entspricht Vanillas „Bambus-Architekt“, nur vampirisch, und gibt einen Grund, ihm einen Sarg ins Zimmer zu stellen. |
| **Nachtgebunden** *(neu, nur Dorian und Gewandelte)* | normal | Gedanke in der Stimmungsliste: „Letzte Nacht 3 Tiere ausgesaugt, 1 stand wieder auf“ (+) bzw. „Die Schale war leer“ (−). **Damit wird die Jagd sichtbar, genau dort, wo du im Screenshot hinschaust.** |

Weg fallen: Bambus-Architekt, Hersteller, Edelsteinsammler.

Technik:

- `VampireSettlerMob.setupPersonalities()` überschreiben.
- **Gotcha:** Bei alten Spielständen gewinnt die gespeicherte Liste beim Laden
  (siehe `SkyTherapy`). Deshalb die Liste in `applyLoadData` ersetzen, so wie es
  89248b5 mit dem Namen macht, und die Clients per `PacketSettlerPersonalities`
  informieren.
- Die Whitelist prüft den **SettlerRegistry-Schlüssel**, nicht die Mob-ID
  (`SkySettlerMob.java:60`).
- Der Therapeut darf Dorians feste Eigenschaften nicht tauschen, sonst entsteht
  der Zufall wieder.

---

## 5. Sichtbarkeit ohne neues HUD

| Was | Bordmittel | Wie oft sieht man es |
|---|---|---|
| Jagd | Gedanke „Nachtgebunden“ in Dorians Stimmungsliste (4.) | nur, wenn man ihn anklickt |
| **Vorwarnung** | **zweite Siedlungsmeldung** `swhbloodthirst`: „Dorian hat Durst, und keine Blutschale ist gefüllt“. Kommt bei `thirstStage() >= 2` und leerer Schale, verschwindet, sobald er getrunken hat. Schwächere Stufe als die Biss-Meldung. | nur, wenn der Kreislauf stockt |
| Biss | bestehende Meldung `swhbloodfever` mit Namen | nur nach einem Biss |
| Blutknecht | **ein Blutknecht pro Nacht höchstens**, und er **folgt Dorian heim**: Er entsteht nicht draußen an der Jagdstelle, sondern im Morgengrauen am Siedlungsrand, wenn Dorian heimkehrt. Dazu eine rote Tönung beim Zeichnen, damit er nicht wie eine Vanilla-Fledermaus aussieht. | selten, etwa jede zweite bis dritte Nacht |

Zum Blutknecht: Ist er am Siedlungsrand zu lästig, ist die Alternative, dass
er friedlich bei Dorian bleibt und ihm folgt, eine Art Haustier-Fledermaus.
→ **D3**.

---

## 6. Der Twist: Biss → Arzt → rund um die Uhr (D1)

Kevins Bild: *„ist ja eigentlich geil, wenn er alle beißt und man Trank vom Arzt
hat, dass sie tags und nachts arbeiten am Schluss.“*

**Ablauf:**

1. **Gebissen:** Blutfieber, die Person fällt aus (D4), die Meldung kommt.
2. **Beim Arzt zwei Wege:**
   - *Heilen* (wie heute, 100 Münzen oder die Tinktur): zurück zu normal.
   - *Wandeln*, neu: **Nachtblut-Trank** vom Arzt, nur an Gebissenen
     anwendbar. Kein Ausfall mehr, dafür wird die Person **rastlos**: arbeitet
     tags und nachts und schläft nur kurz, wenn sie müde ist. Das ist dieselbe
     Mechanik wie Dorians Stufe 2, gebaut in f9201ae.
3. **Der Preis:** Gewandelte essen nicht mehr, sie **trinken aus der
   Blutschale**, etwa ein Fläschchen pro Tag. Je mehr Gewandelte, desto mehr
   Blut braucht die Siedlung, desto wichtiger werden Dorians Jagd, der
   Alchemietisch und die Träger. Ist die Schale leer, schlafen sie wieder nachts
   (keine Strafe außer dem Verlust des Vorteils) und bekommen „Nachtgebunden: die
   Schale war leer“ als Stimmungsminus.

**Konflikt, den du entscheiden musst:**

- **Entscheidung 2026-09-25 („Zuschnitt C“):** Heute wandelt Dorian selbst,
  über seinen Dialog, für 3 Fläschchen, und nur **Bewohner dieses Mods** (die
  neun Benannten). Der Vorschlag hier verlegt das Wandeln zum Arzt und koppelt es
  an den Biss.
- **Technische Grenze, VERIFIED [jar] 1.3.3, HumanMob.java:2737:**
  `findJob` gibt bei **jedem Vanilla-Siedler** nachts `null` zurück, außer in
  der Abenteuergruppe oder unter Befehl. Es gibt keinen Haken, den eine Mod
  pro Siedler umlegen kann.
  - Bei Mod-Bewohnern geht es: Das Merkmal `swhnightbound` gibt es schon.
  - Bei Vanilla-Siedlern (Schmied, Bauer …) geht es nur mit einem **Umbau ihres
    KI-Baums zur Laufzeit**. Getragen würde das von einem gespeicherten
    Dauer-Buff „Nachtblut“, dessen Tick den Baum tauscht. Das ist machbar, aber
    **HYPOTHESE** und das riskanteste Stück hier.

**Vorschlag:**

- **Phase 1:** Mod-Bewohner über den Biss und den Arzt-Trank, auf dem
  bestehenden Merkmal. Dorians Dialog-Wandeln fällt weg bzw. wird zum Hinweis
  „Geh zum Arzt“.
- **Phase 2:** Vanilla-Siedler, erst nach einem eigenen Machbarkeitstest.

---

## 7. Entscheidungen

| # | Frage | Vorschlag |
|---|---|---|
| **D1** | Wandeln über Biss + Arzt-Trank statt über Dorians Dialog? Und Vanilla-Siedler auch (Phase 2)? | Ja, Phase 1 jetzt; Phase 2 nach Machbarkeitstest |
| **D2** | Feste Eigenschaften: Krieger, Blutrünstig, Sargschläfer (neu), Nachtgebunden (neu) | Ja |
| **D3** | Blutknecht: feindlich am Siedlungsrand (max. 1/Nacht, rot getönt) **oder** zahme Fledermaus, die Dorian folgt? | feindlich, max. 1/Nacht |
| **D4** | Blutfieber-Dauer: heute 1 Tag. „Paar Tage ausfallen“ → **2 Tage**, und in der Zeit **arbeitet die Person nicht** (liegt im Bett)? | 2 Tage, arbeitet nicht; Arzt heilt sofort |
| **D5** | Berufsbezeichnung: „Dorian, Vampir“ statt „Dorian, Nachtgebundener“? | Ja |

**Ohne Entscheidung umsetzbar** (Kevins eigene Anweisung): **Blutschale als Lager
wie der Futtertrog** (Abschnitt 3) und die **Vorwarnung** (Abschnitt 5).

## 8. Reihenfolge beim Bau

1. Blutschale als Lager (3), mit Übernahme alter Bestände
2. Vorwarnmeldung `swhbloodthirst` (5)
3. Feste Eigenschaften + Gedanke „Nachtgebunden“ (4) — nach D2
4. Blutknecht sichtbar (5) — nach D3
5. Blutfieber-Dauer/Ausfall (D4)
6. Wandeln über den Arzt, Phase 1 (6) — nach D1

Jeder Schritt einzeln bauen, Integrationstest, `tools/registry_gate.py` (neue
Meldung/Eigenschaft sind Registry-Klassen!), dann ausliefern.
