# Kapitel 03 — Himmelsdorf (Spire Village) und die Questleiter

> *"Ich denke wir müssen alle NPCs, die man auf Himmelsebene irgendwo finden
> kann, zentral um den Spire in Häusern wohnen lassen und Stück für Stück Quests
> von ihnen kriegen, für die man immer weiter in tiefe Gebiete ziehen muss,
> schön übersichtlich ergänzt in ein zentrales Abenteurer-Tagebuch ...
> Welt-Aufbau sinnvoller, insbesondere POIs. Häuser mehr Details bzgl. Items,
> Einrichtung, dort lebenden NPCs, Händlern die dort herumziehen, Dialogen,
> Arenen, Dörfer, Einsiedler, böse NPCs, gute NPCs, Quests mit uniquen
> Belohnungen etc."* — der Spieler, 2026-09-24

Dieses Dokument ist die Entwurfsgrundlage **und** die Quelle der Grundrisse:
`tools/plan_transcription_audit.py` liest jeden `y0 …`-Block unten und
vergleicht ihn Zeichen für Zeichen mit `SpireVillagePlans.java`. Wer einen
Grundriss ändert, ändert beide in einem Commit.

Die Entscheidung selbst steht in `docs/DESIGN_DECISIONS.md` (Eintrag
2026-09-24, „Himmelsdorf“). Was davon wirklich läuft, steht in §7 mit
Nachweisstatus nach `docs/IMPLEMENTATION_RULES.md` §14.

---

## 1. Worum es geht

Bisher stand jede benannte Figur der Himmelsebene irgendwo in „ihrem“ Reich:
Magpie im Skyway-Zollhaus, Halda im Grange-Keller, Ossian auf dem
Sturmschleier-Testgelände, Ives, Mortimer, Caspern, Eveleen, Eleanor und
Knott als Einzelgänger in Eden, Steinfeld, Geisterreich und Krummem Jenseits.
Die Folgen, im Spiel und in der Integrationsprüfung beobachtet:

* Man fand sie nicht, oder erst Stunden später, und wusste dann nicht, was sie
  wollten.
* Sie liefen weg: `ossiansettler is not standing in stormveiltestrange` war ein
  wiederkehrender, saatabhängiger Fehlschlag, weil nichts sie an einen Ort band.
* Die Quests hingen lose nebeneinander; niemand sagte „als Nächstes geh
  tiefer“.

Das neue Bild: **ein Dorf um den Wächterturm**, ein Haus pro Bewohner, und eine
**Questleiter** von 20 Stufen in sechs Kapiteln — Himmelsreich, Eden,
Steinfeld, Geisterreich, Krummes Jenseits, Hölle. Jede Stufe hat genau einen
Auftraggeber im Dorf, schickt den Spieler an einen benannten Ort und zahlt eine
Belohnung, die es sonst nirgends gibt. Das Abenteurer-Tagebuch zeigt die
Leiter (`QuestLadderSource`).

## 2. Das Dorf

### 2.1 Lage und Maße

* Mittelpunkt ist der Wächterturm (`SkyOrigin`, 33 × 33 Turm-Preset). Um ihn
  liegt der bestehende Vorplatz (Radius 21,5) mit seinen vier Achsenstraßen.
* Das Dorf füllt den Ring von Tschebyschow-Abstand 22 bis 38 — also ein
  Quadrat von 77 × 77 Kacheln. `HUB_RADIUS` ist 56, dort ist Land garantiert,
  das Dorf passt also auf jeder Saat.
* **Ringgasse** bei Abstand 23–25, **Außengasse** bei 36–37, **Saum** mit
  Laternen alle 8 Kacheln bei 38. Die vier **Alleen** (|x| ≤ 1 oder |y| ≤ 1)
  laufen vom Turmtor bis zum Saum durch.
* Zwölf Grundstücke: acht Seitenhäuser (23 × 10) zwischen Ring- und Außengasse
  und vier Eckgrundstücke (10 × 10). Jedes Haus ist mit der Front (Tür unten
  im Grundriss) zur Gasse gedreht, an der es steht; gedreht wird mit
  Vanillas eigenem `Preset.rotate`.

### 2.2 Gesamtplan

Norden ist oben. `+` Turm, `D` Turmtore, `|` Vorplatzgeländer, `,` Gasse,
`.` Rasen, `L` Laterne; die Häuser mit den Zeichen ihrer Grundrisse (§3).
Erzeugt aus den Java-Arrays, nicht von Hand gezeichnet.

```
  y-038 ......L.......L.......L.......L......,,,......L.......L.......L.......L......
  y-037 .,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,.
  y-036 .,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,.
  y-035 .,,.####D###..###O#####O###..........,,,..##O#O#O#O#O#O###O#O##...........,,.
  y-034 .,,.#S====e#..#RE=Sxh==C=Kc#FFFFFFF..,,,..#_fgyf_=_ygfy_#RE=SC#..FFFFgFFF.,,.
  y-033 .,,.#=====E#..#=e=========>#.G.H.GF..,,,..#_gyfg_=_fgyf_#=e===#..F%r%%y%F.,,.
  y-032 L,,.O===@==O..######D#######......F..,,,..O=============#<===cO..F%%%%%%F.,,L
  y-031 .,,.#c==ht=#..O==Q==V==Q===O.H.G.HF..,,,..#B_B_B_=_B_B_B#=xh==#..F%%%T%%F.,,.
  y-030 .,,.##D###D#..#=nN==@==nN==#......F..,,,..#=ppp==@===ss=D=====#..F%%%%%%F.,,.
  y-029 .,,.#===#==#..#=nN=====nN==#......F..,,,..O=h===========#<=th=O..F%B%%B%F.,,.
  y-028 .,,.#KR=#=P#..O============O......F..,,,..#_fyg__=_gyf__#=====#..F%%%%%%F.,,.
  y-027 .,,.##O##O##..#<==========>#FFFgFFF..,,,..#<===========>#Nn=c=#..FFFFFFFF.,,.
  y-026 .,,...........###O##D##O####.........,,,..##O#O##D##O#O###O#O##...........,,.
  y-025 .,,..........,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,..........,,.
  y-024 L,,######O###,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,#####O####,,L
  y-023 .,,#C=c#c=>=#,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,#<A=====S#,,.
  y-022 .,,O===#====#,,,..........L..........,,,..........L..........,,,#===h=S=S#,,.
  y-021 .,,#===#====O,,,..................|||||||||..................,,,O===t=S=S#,,.
  y-020 .,,#K==#U===#,,,...............|||         |||...............,,,#===t=S==O,,.
  y-019 .,,#W==D=@==#,,,...L........|||               |||........L...,,,#===h===S#,,.
  y-018 .,,O==h#H=b=#,,,...........||                   ||...........,,,#=======S#,,.
  y-017 .,,#==t#==b=O,,,.........||                       ||.........,,,D==@====S#,,.
  y-016 L,,#Eeh#G=b=#,,,........||                         ||........,,,#===h====#,,L
  y-015 .,,#R<=#====#,,,.......+++++++++++++++D+++++++++++++++.......,,,#===t=S==O,,.
  y-014 .,,#####====#,,,......|+++++++++++++++++++++++++++++++|......,,,#===t=S=S#,,.
  y-013 .,,#l>=#====#,,,.....||+++++++++++++++++++++++++++++++||.....,,,O===h=S=S#,,.
  y-012 .,,#l==#Yy==D,,,L....| +++++++++++++++++++++++++++++++ |....L,,,#=======S#,,.
  y-011 .,,O===#Q===#,,,....|  +++++++++++++++++++++++++++++++  |....,,,#>X===>=K#,,.
  y-010 .,,#jh=#====#,,,...||  +++++++++++++++++++++++++++++++  ||...,,,###D######,,.
  y-009 .,,#jh=DYy==#,,,...|   +++++++++++++++++++++++++++++++   |...,,,#=====<=R#,,.
  y-008 L,,#===#Q===O,,,...|   +++++++++++++++++++++++++++++++   |...,,,#hj==x=eE#,,L
  y-007 .,,Os==#====#,,,..|    +++++++++++++++++++++++++++++++    |..,,,Ohj==h===O,,.
  y-006 .,,#s==#Yy=n#,,,..|    +++++++++++++++++++++++++++++++    |..,,,#=======S#,,.
  y-005 .,,#l=<#==<N#,,,..|    +++++++++++++++++++++++++++++++    |..,,,#c=PP=c=C#,,.
  y-004 .,,######O###,,,.|     +++++++++++++++++++++++++++++++     |.,,,#####O####,,.
  y-003 .,,..........,,,.|     +++++++++++++++++++++++++++++++     |.,,,..........,,.
  y-002 .,,..........,,,.|     +++++++++++++++++++++++++++++++     |.,,,..........,,.
  y-001 ,,,,,,,,,,,,,,,,,|     +++++++++++++++++++++++++++++++     |,,,,,,,,,,,,,,,,,
  y+000 ,,,,,,,,,,,,,,,,,|     D++++++++++++++*++++++++++++++D     |,,,,,,,,,,,,,,,,,
  y+001 ,,,,,,,,,,,,,,,,,|     +++++++++++++++++++++++++++++++     |,,,,,,,,,,,,,,,,,
  y+002 .,,..........,,,.|     +++++++++++++++++++++++++++++++     |.,,,..........,,.
  y+003 .,,..........,,,.|     +++++++++++++++++++++++++++++++     |.,,,..........,,.
  y+004 .,,###O##O###,,,.|     +++++++++++++++++++++++++++++++     |.,,,###O######,,.
  y+005 .,,#C=c==P==#,,,..|    +++++++++++++++++++++++++++++++    |..,,,#N<=g#<=g#,,.
  y+006 .,,#S======c#,,,..|    +++++++++++++++++++++++++++++++    |..,,,#n=bg#==g#,,.
  y+007 .,,O===h==h=O,,,..|    +++++++++++++++++++++++++++++++    |..,,,#==b=#==gO,,.
  y+008 L,,#Ee=x==tn#,,,...|   +++++++++++++++++++++++++++++++   |...,,,O==b@#===#,,L
  y+009 .,,#R=<===<N#,,,...|   +++++++++++++++++++++++++++++++   |...,,,#==b=D==o#,,.
  y+010 .,,######D###,,,...||  +++++++++++++++++++++++++++++++  ||...,,,#====#===#,,.
  y+011 .,,#==>===>=#,,,....|  +++++++++++++++++++++++++++++++  |....,,,#====#==lO,,.
  y+012 .,,#C=======#,,,L....| +++++++++++++++++++++++++++++++ |....L,,,D====#==l#,,.
  y+013 .,,#s==W====O,,,.....||+++++++++++++++++++++++++++++++||.....,,,#==h=#>ss#,,.
  y+014 .,,#l=======#,,,......|+++++++++++++++++++++++++++++++|......,,,#=hth#####,,.
  y+015 .,,Ol==mh===#,,,.......+++++++++++++++D+++++++++++++++.......,,,#=hth#<=R#,,.
  y+016 L,,#===mh===#,,,........||                         ||........,,,#==h=#=eE#,,L
  y+017 .,,#J=======D,,,.........||                       ||.........,,,O====#===#,,.
  y+018 .,,#===@====#,,,...........||                   ||...........,,,#====#===O,,.
  y+019 .,,#I=======#,,,...L........|||               |||........L...,,,#==h=D===#,,.
  y+020 .,,O========#,,,...............|||         |||...............,,,#=hth#=h=#,,.
  y+021 .,,#A==Z====O,,,..................|||||||||..................,,,O=hth#=m=#,,.
  y+022 .,,#======l=#,,,..........L..........,,,..........L..........,,,#==h=#===O,,.
  y+023 .,,#F==Z==l<#,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,#=>=c#c=C#,,.
  y+024 L,,###O##O###,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,###O######,,L
  y+025 .,,..........,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,..........,,.
  y+026 .,,...........,,,,,,,,,,,,,,,,,,,,,..,,,..####O#####O###D##O###...........,,.
  y+027 .,,.##O##O##..L,,,,,nN,,,nN,,,,,,,L..,,,..#=c====#===========<#..||||||||.,,.
  y+028 .,,.#okkkC=#..,**,,,,,,,,,,,,,,,**,..,,,..#===hth#=========nN=O..|,,,,,,|.,,.
  y+029 .,,.#==@===#..,,,,,,,,,,,,,,,,,,,,,..,,,..#C====<#>====ttt===l#..|,W,,W,|.,,.
  y+030 .,,.O=htth=O..,,,,,,,,||g|,,,,,,,,,..,,,..#======D=PP===@====P#..|,,,,,,|.,,.
  y+031 .,,.#======#..,,bbb,,,|~~|,,,,aaa,,..,,,..#c==hx=#########D####..|,,W,,,|.,,.
  y+032 L,,.#E=R=c>#..,,l,l,,,|~~|,,,,s,X,,..,,,..O=::===#============O..|,,,,,,|.,,L
  y+033 .,,.#e=====#..,,,,,,,,||||,,,,,,,,,..,,,..#=::=e=#>====p=====s#..|,,,,,,|.,,.
  y+034 .,,.###D####..L,,,,,,,,,,,,,Z,,,,,L..,,,..#KSS=ER#=CC=ppp==sll#..|||g||||.,,.
  y+035 .,,.f.f..f.f..,,,,,,,,,,,,,,,,,,,,,..,,,..####O#######O####O###.L........L,,.
  y+036 .,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,.
  y+037 .,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,.
  y+038 ......L.......L.......L.......L......,,,......L.......L.......L.......L......
```

| Grundstück | Bewohner | Ursprung (x, y) | Drehung | Front zeigt nach |
|---|---|---|---|---|
| Kontor der Elster | Magpie | (3, 26) | HALF_180 | Norden (Ringgasse) |
| Kellerschänke | Halda | (26, 3) | CLOCKWISE | Westen |
| Das kleine Archiv | Ossian | (26, −25) | CLOCKWISE | Westen |
| Gewächshaus | Eveleen | (3, −35) | — | Süden |
| Küsterei | Ives | (−25, −35) | — | Süden |
| Bestattungshaus | Mortimer | (−35, −25) | ANTI_CLOCKWISE | Osten |
| Kalte Schmiede | Caspern | (−35, 3) | ANTI_CLOCKWISE | Osten |
| Marktplatz | (Stände) | (−25, 26) | HALF_180 | Norden |
| Haus der vielen Türen | Knott | (−35, −35) | HALF_180 | Norden |
| Küchenhaus | Eleanor | (−35, 26) | — | Süden |
| Edenbeet | (Eveleens Garten) | (26, −35) | HALF_180 | Norden |
| Übungsring | — | (26, 26) | — | Süden |

### 2.3 Stempeln, Wiederholung, alte Spielstände

* `SpireVillage.ensure(level)` läuft in `SkyLevel.ensureWardenSpire`, direkt
  nach dem Turm und vor den Wahrzeichen. Das Flag
  `SkywatchQuestData.villagePlaced` verhindert doppeltes Stempeln.
* `villagePlaced` steht **nicht** in `copyProgressFrom`: `/swhreset
  regenerate` baut die Himmelsebene neu, und das Dorf muss dann mit neu
  entstehen. `SpireVillage.prepareRegeneration` setzt das Flag zurück und gibt
  die Ansprüche der Dorfbewohner frei.
* **Alte Spielstände** bekommen das Dorf beim nächsten Laden automatisch —
  außer, eine Spielersiedlung liegt im Grundriss. Dann wird nicht gestempelt,
  `/swhvillage` meldet `blocked=settlement`, und nichts vom Spieler Gebaute wird
  überschrieben.
* Ein Bewohner, der in einem alten Spielstand noch irgendwo in der Himmelsebene
  steht, wird von `SpireVillage.bringHome` an seinen Sitzplatz im Haus gesetzt —
  keine Doppelten, weil `residentsClaimed` pro Welt genau einen Anspruch hält.

### 2.4 Bewohner wohnen, sie wandern nicht davon

Jeder Bewohner bekommt `home` = sein `@`-Platz, `canDespawn = false`. Das
vanilla `HumanAI` nimmt `mob.home` bei Nicht-Siedlern als Basis: tagsüber
streifen sie etwa zehn Kacheln um ihr Haus (Markt, Nachbarn), nachts bleiben
sie drinnen. `home` wird vom Spiel selbst gespeichert. Zieht einer in die
Siedlung des Spielers (Ives, Mortimer, Caspern, Eleanor), löst `onMoveIn` die
Dorfmarke — das Haus bleibt als Erinnerung stehen.

## 3. Die Häuser

Gemeinsame Zeichen aller Hausgrundrisse:

| Zeichen | Bedeutung |
|---|---|
| `.` | nichts — der Rasen des Dorfs scheint durch |
| `=` | Boden des Hauses |
| `:` | Teppich |
| `@` | hier wohnt der Bewohner (Boden) |
| `#` `O` `D` | Wand, Fenster, Tür |
| `^ v < >` | Wandlaterne (Nebelglas), Wand oben / unten / links / rechts |
| `h` | Stuhl, vom Interpreter zum Tisch gedreht |
| `E e` | Bett, Kopf und Fuß |
| `N n` | Bank, beide Hälften |

Jeder Grundriss ist in Grundstellung gezeichnet: Front unten. Alles weitere
steht in der Legende unter dem Plan.

### 3.1 Das Kontor der Elster — Magpie

Laden, Lager und Büro. Unzugestellte Post (`p`, Himmelspakete) stapelt sich im
Lager, wo nie jemand sie abgeholt hat; das ist ihre erste Frage an den Spieler.

```
       0         1         2  
       01234567890123456789012
  y0    .###O####O#######O####.
  y1    .#lls==ppp=CC=#RE=SSK#.
  y2    .#s=====p====>#=e=::=#.
  y3    .O============#===::=O.
  y4    .####D#########=xh==c#.
  y5    .#P====@===PP=D======#.
  y6    .#l===ttt====>#<====C#.
  y7    .O=Nn=========#hth===#.
  y8    .#<===========#====c=#.
  y9    .###O##D###O#####O####.
```

Boden Dunkelholz, Wände Himmelsstein. `x` Schreibtisch, `t` Tisch mit Papier,
Tischuhr, Feder, Teekanne, `R` Kommode, `S` Bücherregal, `K` Standuhr,
`C` Schrank, `P` Vitrine, `c` Kandelaber, `l` Fass, `s` Sack, `p` Himmelspaket.

### 3.2 Die Kellerschänke — Halda

Schankraum mit zwei Vierertischen, Braukammer mit Fässern, ihr eigenes Zimmer.
Sie verkauft hier Sturmbräu und Paradiesmost, sobald die zugehörigen Stufen
erledigt sind.

```
       0         1         2  
       01234567890123456789012
  y0    .###O###O######O###O##.
  y1    .#ggg=o=lls#RE======C#.
  y2    .#========s#=e===hm==#.
  y3    .#<=======>#<=======c#.
  y4    .#####D#########D#####.
  y5    .#gg=@=====hh====hh=c#.
  y6    .O=bbbb===htth==htth=O.
  y7    .#<========hh====hh=>#.
  y8    .#Nn=================#.
  y9    .####O###D####O###O###.
```

`b` Theke (Krüge, Teller, Eintopf), `t` Gasttische (Krüge, Brettchen),
`m` ihr Teetisch (Topfmoltebeere, Kerze), `g` großes Fass, `o` Kochtopf,
`l` Fass, `s` Sack, `R` Kommode, `C` Schrank, `c` Kandelaber.

### 3.3 Das kleine Archiv — Ossian

Regalreihen, zwei Lesetische, Werkstatt mit Astrolabium und Teleskop.

```
       0         1         2  
       01234567890123456789012
  y0    .####O####O#######O###.
  y1    .#SSS=SSS==SSSK#RE=SC#.
  y2    .#=============#=e===#.
  y3    .#=SSS====SSS=>#<===c#.
  y4    .O=============#=xh==O.
  y5    .#=htth==htth==#====P#.
  y6    .#======@======D====P#.
  y7    .#A===========X#=jj==#.
  y8    .#<===========>#=hh=c#.
  y9    .###O###D###O#####O###.
```

`t` Lesetisch (Bücher, Foliant, Feder, Papier), `j` Werktisch (Globus,
Seherkugel), `x` Schreibtisch, `S` Bücherregal, `K` Standuhr, `A` Astrolabium,
`X` Teleskop, `P` Vitrine, `R` Kommode, `C` Schrank, `c` Kandelaber.

### 3.4 Das Gewächshaus — Eveleen

Beete unter einer Glasfront — die Nordwand ist fast nur Fenster.

```
       0         1         2  
       01234567890123456789012
  y0    .##O#O#O#O#O#O###O#O##.
  y1    .#_fgyf_=_ygfy_#RE=SC#.
  y2    .#_gyfg_=_fgyf_#=e===#.
  y3    .O=============#<===cO.
  y4    .#B_B_B_=_B_B_B#=xh==#.
  y5    .#=ppp==@===ss=D=====#.
  y6    .O=h===========#<=th=O.
  y7    .#_fyg__=_gyf__#=====#.
  y8    .#<===========>#Nn=c=#.
  y9    .##O#O##D##O#O###O#O##.
```

Boden Edenwurzel, Wände Palme. `_` Ackerboden, `f g y` rote / blaue / gelbe
Blumen auf Acker, `B` Brombeerstrauch auf Acker, `p` Pflanztisch (drei Töpfe),
`t` Tisch mit Topfmoltebeere, `x` Schreibtisch, `s` Sack, `R S C c` Palmmöbel.

### 3.5 Die Küsterei — Ives

Kapelle mit Bänken zum Altar, seine Zelle, und ein Steinmetzhof mit den
Grabsteinen, die er verkauft.

```
       0         1         2  
       01234567890123456789012
  y0    .###O#####O###.........
  y1    .#RE=Sxh==C=Kc#FFFFFFF.
  y2    .#=e=========>#.G.H.GF.
  y3    .######D#######......F.
  y4    .O==Q==V==Q===O.H.G.HF.
  y5    .#=nN==@==nN==#......F.
  y6    .#=nN=====nN==#......F.
  y7    .O============O......F.
  y8    .#<==========>#FFFgFFF.
  y9    .###O##D##O####........
```

Boden rissiger Marmor. `N n` Birkenbänke (Blick zum Altar), `Q` Kerzensockel,
`V` Vase, `x` Schreibtisch, `G H` Grabsteine im Hof, `F` Steinzaun,
`g` Steintor, `R S C K c` Möbel.

### 3.6 Das Bestattungshaus — Mortimer

Ausstellungsraum mit drei Särgen und Grabsteinen, Werkstatt, Salon.

```
       0         1         2  
       01234567890123456789012
  y0    .###O###O######O###O##.
  y1    .#lss=jj=ll#RE==WK==C#.
  y2    .#====hh==>#<e=======#.
  y3    .#<========#=hth====c#.
  y4    .#####D#########D#####.
  y5    .#=Y=QY=QY===G=H=U==c#.
  y6    .O=y==y==y======@====O.
  y7    .#<==========bbb====>#.
  y8    .#Nn=================#.
  y9    .####O###D####O###O###.
```

Boden Gruftweg, Knochenmöbel. `Y y` Gruftsarg, `G H U` Grabsteine, `Q`
Kerzensockel, `j` Werktisch (Kerze, Schädel), `b` Theke (Schädel, Papier,
alte Kelche), `t` Teetisch, `W` Bücherregal, `K` Uhr, `l s` Fass, Sack.

### 3.7 Die kalte Schmiede — Caspern

Die Schmiedehalle mit Esse, Ätheresse, zwei Ambossen, Rüstständern und einer
Übungspuppe; dahinter sein Zimmer.

```
       0         1         2  
       01234567890123456789012
  y0    .####O####O#######O###.
  y1    .#F=A=I=J=llsC=#RE=SC#.
  y2    .#=============#=e===#.
  y3    .O============>#<===cO.
  y4    .#Z=Z==@=mm=W==#=xh==#.
  y5    .#=======hh====#=====#.
  y6    .O=============D====PO.
  y7    .#ll==========>#<th==#.
  y8    .#<============#Nn=c=#.
  y9    .###O###D###O#####O###.
```

`F` Esse, `A` Ätheresse, `I` Eisenamboss, `J` Wolframamboss, `Z` Rüstungsständer,
`W` Übungspuppe, `m` Werktisch (vergessene Klinge, Papier), `t` Tisch mit Krug,
`x` Schreibtisch, `P` Vitrine, `l s` Fass, Sack.

### 3.8 Das Haus der vielen Türen — Mr. Knott (Eckgrundstück)

Klein, arkan, zu viele Türen für so wenig Haus.

```
       0         
       0123456789
  y0    ..........
  y1    .##O##O##.
  y2    .#P=#=RK#.
  y3    .#==#===#.
  y4    .#D###D##.
  y5    .#=th==c#.
  y6    .O==@===O.
  y7    .#E=====#.
  y8    .#e====S#.
  y9    .###D####.
```

Boden Krummstreifen, Wände arkan. `t` Tisch mit Leerwürfel, `P` Vitrine,
`S` Bücherregal (verkehrt herum, Blick nach oben), `R K c` Knochenmöbel.

### 3.9 Das Küchenhaus — Eleanor (Eckgrundstück)

Die Küche, an die sie sich erinnert, und der Tisch für zwei.

```
       0         
       0123456789
  y0    ..........
  y1    .##O##O##.
  y2    .#okkkC=#.
  y3    .#==@===#.
  y4    .O=htth=O.
  y5    .#======#.
  y6    .#E=R=c>#.
  y7    .#e=====#.
  y8    .###D####.
  y9    .f.f..f.f.
```

`k` Küchentheke (Brett, Teller, Teekanne), `o` Kochtopf, `t` Tisch für zwei,
`f` rote Blumen vor der Tür, `C R c` Möbel.

### 3.10 Der Marktplatz

Brunnen, zwei Stände (Eveleens Blumen, Haldas Getränke), das Schwarze Brett
(`Z`, Text `misc.swhsignvillage`), Bänke und Tulpen.

```
       0         1         2  
       01234567890123456789012
  y0    .,,,,,,,,,,,,,,,,,,,,,.
  y1    .L,,,,,Z,,,,,,,,,,,,,L.
  y2    .,,,,,,,,,||||,,,,,,,,.
  y3    .,,X,s,,,,|~~|,,,l,l,,.
  y4    .,,aaa,,,,|~~|,,,bbb,,.
  y5    .,,,,,,,,,|g||,,,,,,,,.
  y6    .,,,,,,,,,,,,,,,,,,,,,.
  y7    .,**,,,,,,,,,,,,,,,**,.
  y8    .L,,,,,,,Nn,,,Nn,,,,,L.
  y9    .,,,,,,,,,,,,,,,,,,,,,.
```

Boden Skyway-Platten, `,` Gasse. `~` Nebelwasser, `|` Wolkenmarmorzaun,
`g` Tor, `L` Wächterkandelaber, `a` Blumenstand, `b` Getränkestand,
`X s` Säcke, `l` Fass, `*` Himmelstulpe auf Wolkenrasen.

### 3.11 Der Übungsring (Eckgrundstück)

Ein Sparringsring mit drei Übungspuppen — das „Arena“-Stichwort des Spielers
im kleinsten Maßstab; die großen Arenen stehen im Rückstand (§6).

```
       0         
       0123456789
  y0    ..........
  y1    .||||||||.
  y2    .|,,,,,,|.
  y3    .|,W,,W,|.
  y4    .|,,,,,,|.
  y5    .|,,W,,,|.
  y6    .|,,,,,,|.
  y7    .|,,,,,,|.
  y8    .|||g||||.
  y9    L........L
```

`|` Wolkenmarmorzaun, `g` Tor, `W` Übungspuppe, `L` Wächterkandelaber.

### 3.12 Das Edenbeet (Eckgrundstück)

Eveleens eingezäuntes Stück Eden mit einem Wissensbaum.

```
       0         
       0123456789
  y0    ..........
  y1    .FFFFFFFF.
  y2    .F%%%%%%F.
  y3    .F%B%%B%F.
  y4    .F%%%%%%F.
  y5    .F%%T%%%F.
  y6    .F%%%%%%F.
  y7    .F%y%%r%F.
  y8    .FFFgFFFF.
  y9    ..........
```

Boden überwucherte Edenplatte, `F` Holzzaun, `g` Tor, `B` Brombeere,
`T` Wissensbaum, `y r` gelbe / rote Blumen.

## 4. Die Questleiter

Sechs Kapitel, eines pro Reich. Ein Kapitel öffnet sich erst, wenn alle
**tragenden** Stufen des vorigen erledigt sind; Eleanors Stufe ist die einzige
nicht tragende (man darf sie gehen lassen oder behalten, aber auch
aufschieben). Jeder Auftraggeber bietet immer nur seine **nächste** Stufe an;
wer nichts hat, sagt eine Wartezeile (`swhladderwait<name>`). Beim Annehmen
setzt der Server eine Kartenmarke auf den Zielort (`PacketAddMapMarker`).

Abgabe: im Gespräch mit dem Auftraggeber — vollständige Anforderungen werden
zuerst abgegeben, erst dann wird die nächste Stufe angeboten.

| # | Kap. | Stufe | Auftraggeber | Verlangt | Ziel | Belohnung |
|---|---|---|---|---|---|---|
| 1 | I Himmelsreich | Unzustellbare Post | Magpie | 1 Postbuch | Skyway-Zollhaus | Elsterbeutel |
| 2 | I | Die Kellerhefe | Halda | 16 Wolkenbeeren, 8 Windweizen, 4 Aurorablüten | Driftlande, Aurorabänke | 3 Sturmbräu, danach im Laden |
| 3 | I | Woraus Prototyp Neun gebaut ist | Ossian | 6 Sturmglas, 8 Sturmsplitter, 4 Fulgurit | Sturmschleier-Testgelände | Leserlupe |
| 4 | II Eden | Hinein in den Garten | Eveleen | Eden betreten | Garten Eden | 6 Eden-Grassamen |
| 5 | II | Ein Geschmack von Eden | Eveleen | (bestehende Quest) | Garten Eden | 3 Wissenssteckling + 10 Sturmstahl |
| 6 | II | Paradiesmost | Halda | 4 Paradiesäpfel, 2 Kokosnüsse, 4 Edensaft | Heckenlabyrinth, Blütenrachen | 3 Paradiesmost, danach im Laden |
| 7 | II | Schmuggelware aus dem Garten | Magpie | 6 Schlangenschuppen, 4 Giftzähne, 1 Wegebrief | Schlangen in Eden, Zollhaus | Himmelsfahrten +15 % Erfolg |
| 8 | III Steinfeld | Die Totenwache | Ives | (bestehende Quest) | Steinfeld | Ives zieht ein + 12 Sturmstahl |
| 9 | III | Das Echo im Archiv | Ossian | 10 Echosplitter, 1 Trauerband | Steinerne Trauernde | Echomuschel |
| 10 | III | Die elf Schritte | Ives | je 11 Grabsalz, Geistermoos, Blassstein | Plattenfelder, Grabheide | Küsterlaterne |
| 11 | IV Geisterreich | Die letzte Ehre | Mortimer | (bestehende Quest) | Geisterreich | Mortimer zieht ein + 6 Geisterstahl |
| 12 | IV | Die kalte Schmiede | Caspern | (bestehende Quest) | Geisterreich | Caspern zieht ein + 6 Geisterstahl |
| 13 | IV | Warum sie blieb | Eleanor | (bestehende Quest, nicht tragend) | Küchenhaus | Loslassen oder Bleiben |
| 14 | IV | Metall, das sich erinnert | Caspern | 1 Seelenhalsband, 10 Knochenholz, 8 Ektoplasma | Trauerbräute, Hochzeitsmahl | Die Erinnernde Klinge |
| 15 | IV | Leichentücher für das Fest | Mortimer | 10 Seelenfaden, 6 Schleieressenz | Hochzeitsmahl, Düsterschemen | Trauerbrosche |
| 16 | V Krummes Jenseits | Eine Tür, die irgendwohin führt | Knott | das Krumme Jenseits betreten | Krummes Jenseits | 4 Realitätssplitter |
| 17 | V | Überzeug die Tür | Knott | (bestehende Quest) | Krummes Jenseits | Zephyr-Gurtzeug + 12 Geisterstahl + 6 Realitätssplitter |
| 18 | V | Samen, die ich nie gepflanzt habe | Eveleen | 6 Augensamen, 3 Wissenssteckling | Zungenpflanzen | Wurzelkranz |
| 19 | V | Eine Tür mit Manieren | Knott | 8 Streifenmuscheln, 1 Streifenhorn | Streifenkäfer, Türmimiken | Knotts Schlüsselbund |
| 20 | VI Hölle | Formular 666-B | Ossian | 16 Realitätssplitter, 24 Wunderholz, 30 Kohleholz | Höllensaum, Ofenweite | Siegel des Prüfers |

Stufe 16 (`swh_crookedarrival`) war vorher tot: sie hatte keine Abgabe und
keinen Auslöser. Jetzt ist sie wie Stufe 4 eine `RealmVisitQuest` — sie ist
erfüllt, sobald der Spieler das Krumme Jenseits betritt, und wird bei Knott
abgegeben.

### 4.1 Die Belohnungen gegen `BALANCE.md`

Grundregel aus `BALANCE.md` §7: die Ausrüstungsleiter läuft Sturmstahl
(1900, EPIC) → Geisterstahl (2400) → Krummes Set (3000) → Höllenstahl (3600,
LEGENDARY). Eine Leiterbelohnung ist **kein** Rüstungsersatz, sondern ein
Accessoire oder Verbrauchsgut, dessen Stärke zur Tiefe passt, in der man es
erhält. Die Verzauberungskosten folgen deshalb der Stufe des Reichs.

| Belohnung | Werte | Einordnung |
|---|---|---|
| Elsterbeutel | +5 Aufsammelradius, +25 % Ausdauer; Verz. 600 | Komfort, kein Kampfwert — Kapitel I ist Einstieg. Schließt den Gegenstandsmagneten aus (gleicher Zweck). |
| Sturmbräu | +10 % Tempo, +5 % Angriffstempo, 12 min | Fein-Essen, unter vanilla Tier-1-Tränken; danach kaufbar (150–220). |
| Leserlupe | Höhlenblick (Erze) + 20 % Werkzeugschaden | Nutzen-Accessoire; ersetzt einen Höhlenforschertrank dauerhaft, kostet aber einen Slot. |
| Paradiesmost | +40 Leben, +0,5 Regeneration, +3 % Krit, 15 min | Gourmet-Essen auf Eden-Stufe (×1,3 Beute). |
| Wegebrief-Flag | +15 % Erfolg jeder Himmelsfahrt, gedeckelt bei 100 % | Kein Kampfwert; beschleunigt Handel, nicht Fortschritt. |
| Echomuschel | +25 % Kritschaden, +3 % Krit; Verz. 1400 | Steinfeld (×1,6). Schließt Frostwelle aus, damit Krit-Stapel gedeckelt bleibt. |
| Küsterlaterne | Gegnerblick + 30 Leben | Nutzen + etwas Überleben; Steinfeld-Niveau. |
| Erinnernde Klinge | Großschwert 186 → 232 Schaden, Verz. 2600, Ladestufen 160/320/480 | Geisterreich; zwischen Geisterstahl (2400) und Krummem Set (3000). Leiht die Grafik des Hexklingen-Großschwerts. |
| Trauerbrosche | +8 Rüstung, +25 Leben | Entspricht etwa einem Viertel eines Geisterstahl-Brustteils (34) — ein Accessoire, kein Setersatz. |
| Wurzelkranz | +60 Leben, +1,0 Regeneration | Krummes Jenseits (×2,5). Schließt Auroramedaillon, Frostherz, Lebensanhänger und Regenerationsanhänger aus — kein Stapeln von Heilung. |
| Knotts Schlüsselbund | −25 % Ausweichabklingzeit, +10 % Tempo | Bewegung, Krummes Jenseits. Schließt den Tempelanhänger aus. |
| Siegel des Prüfers | +10 % Schaden, +5 % Krit; Verz. 3600, LEGENDARY | Hölle: gleiche Verzauberungskosten und Seltenheit wie Höllenstahl (§7). Die einzige LEGENDARY-Belohnung der Leiter. |

## 5. Dorian — lesbar gemacht

Aus `docs/design/concept-nightbound-dorian.md`, Entscheidungen E1–E5, jetzt
gebaut:

* **E1 — er sagt, was er ist.** Siedlertipp, Werbezeile und Gesprächszeilen
  sagen „Vampir“ und „ich beiße nachts“, statt es zu verschleiern.
* **E2 — Benachrichtigung statt Chat.** Ein Biss meldet sich als
  Siedlungsbenachrichtigung (`swhbloodfever`, Warnstufe) mit den Namen der
  Gebissenen; sie verschwindet von selbst, sobald das Blutfieber weg ist.
* **E3 — die Blutschale.** Dorian verkauft sie (3200–4800). Man legt
  Blutfläschchen hinein (bis 12); nachts trinkt er daraus (Reichweite 40
  Kacheln), **bevor** er beißt. Wer die Schale füllt, findet keinen Gebissenen.
  Leiht das Seelenbecken-Blatt (dritte Nutzung, in `VANILLA_ASSET_MAP.md`).
* **E4 — sein Dialog.** Ein eigenes Menü wie beim Arzt: Durst in vier Worten,
  „letzte Nacht“ (ausgesaugte Tiere, erhobene Blutknechte, Gebissene mit
  Namen) und „Blutfläschchen geben“ (+0,5 Durst).
* **E5 — die Heilung.** Der Arzt verkauft die Blutfiebertinktur (250); sie
  heilt Blutfieber bei allen Menschen im Umkreis von 10 Kacheln. Der Arzt
  nennt im Gespräch, wie viele gerade gebissen sind.

## 6. Gute und böse NPCs anderswo — Entwurf und Rückstand

Das Dorf zieht die benannten Figuren zusammen; die Welt darf dadurch nicht
leer werden. Der Plan dafür, **noch nicht gebaut**:

### 6.1 Gute NPCs

| Figur | Reich | Ort | Rolle | Stand |
|---|---|---|---|---|
| Der Wolkenhirte (Einsiedler) | Himmelsreich | Hirtenpferch | tauscht Wolle gegen Himmelswetter-Vorhersagen (Buff) | Rückstand |
| Die Tauhüterin | Himmelsreich | Tauhüter-Hütte | Tränke gegen Aurorablüten | Rückstand |
| Pilger von Steinfeld | Steinfeld | Pilgerossarium | kleine Lieferaufträge, Lore zu den Stufen 8–10 | Rückstand |
| Der Fährmann | Geisterreich | ein Steg am Nebelsee | bringt den Spieler einmal pro Tag zu einem Geisterreich-POI | Rückstand |
| Wandernder Händler „Pim“ | alle | wandert Markt → Zollhaus → Eden → Steinfeld | Rotationssortiment je Reich, kommt jede dritte Nacht auf den Dorfmarkt | Rückstand |

### 6.2 Böse NPCs

| Figur | Reich | Ort | Rolle | Stand |
|---|---|---|---|---|
| Die Himmelsräuber | Himmelsreich | ein Lager im Driftland | Banditenlager, Anführer mit Beute­tabelle, „bewacht“ Pakete aus Stufe 1 | Rückstand |
| Die Heckenwirtin | Eden | im Heckenlabyrinth | freundlich, bis man isst; dann Kampf | Rückstand |
| Der Grabräuber | Steinfeld | Plattenfelder | stiehlt aus Ives' Hof, flieht; Kopfgeld bei Ives | Rückstand |
| Die Brautjungfern | Geisterreich | Hochzeitsmahl | Arena-Welle vor dem Bossraum | Rückstand |
| Der Tür-Anwalt | Krummes Jenseits | Saal der vielen Türen | Rätselgegner; verliert nur gegen Knotts Schlüsselbund | Rückstand |

### 6.3 Dörfer und Arenen in der Tiefe

* **Ein zweites Dorf pro Tiefe** (Eden-Siedlung, Steinfelder Kloster,
  Geisterreich-Totenstadt) — jeweils drei bis fünf namenlose Bewohner mit
  Sortimenten, kein Questgeber. Rückstand; braucht ein Preset pro Reich.
* **Arenen:** WORLD_DESIGN §42.5 lässt offen, ob Bossarenen Weltgenerierung
  oder Beschwörung sind. Der Übungsring im Dorf ist bewusst klein; große
  Arenen erst nach dieser Entscheidung.

## 7. Nachweisstand

| Aussage | Stand |
|---|---|
| Das Dorf stempelt vollständig, 12 Häuser, kein fehlendes Objekt | VERIFIED [run] — `/swhvillage` in `scripts/integration_test.sh` |
| Neun Bewohner sitzen zu Hause, keine Doppelten, auch nach Neustart | VERIFIED [run] — ebenda, zweiter Serverstart |
| 20 Leiterstufen registriert und baubar, 19 als Angebot auflösbar | VERIFIED [run] — `village ladder:`-Zeile |
| Grundrisse im Code = Grundrisse hier | VERIFIED [run] — `tools/plan_transcription_audit.py` |
| Gespräch vergibt/nimmt Stufen, Kartenmarke, Belohnung kommt an | HYPOTHESIS — kein Client; der Befehl prüft nur den Auflöser |
| Drehung von Wanddeko und Möbeln sieht auf dem Client richtig aus | HYPOTHESIS — `Preset.rotate` im Quelltext gelesen (VERIFIED [jar] für den Mechanismus), nicht gesehen |
| Dorian-Dialog, Blutschale, Benachrichtigung, Tinktur | kompiliert und registriert; im Spiel HYPOTHESIS |
| Bewohner streifen tagsüber ~10 Kacheln, nachts im Haus | Mechanismus VERIFIED [jar] (`HumanAI`); Verhalten über einen Tag HYPOTHESIS |
