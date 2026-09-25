# Preview tools — Ausstellung, Screenshots, Sprite-Dump, Offline-Renderer

Four tools, one question: *what is actually built, and how does it look
together in the real game?* Deutsch zuerst, English below.

| Werkzeug | wo es läuft | Stand |
|---|---|---|
| `/swhshowroom` — die Ausstellung | Server + Einzelspieler | **VERIFIED [run]** auf dem Dedicated Server (Integrationstest) |
| `/swhshots` — automatische Screenshots | nur Client (dein PC) | **VERIFIED [jar]** aus dem dekompilierten Client gelesen; Ausführen = **HYPOTHESIS**, bis du es einmal laufen lässt |
| `/swhdumpsprites` — Sprite-Dump | nur Client (dein PC) | **VERIFIED [jar]**; Ausführen = **HYPOTHESIS** |
| `tools/preset_render.py` — Offline-Draufsicht | Python, überall | **VERIFIED [run]** hier in der Cloud (ohne Vanilla-Sprites) |

---

## Deutsch — Schritt für Schritt

### 1. Die Ausstellung bauen und ablaufen

In einer Welt mit der Mod (Einzelspieler reicht; du bist dort Admin), im Chat:

```
/swhshowroom build      baut alles (ca. 5–70 s, das Spiel steht so lange)
/swhshowroom list       alle Exponat-IDs
/swhshowroom goto skytower     vor ein Exponat teleportieren
/swhshowroom            Status: gebaut ja/nein, wo
/swhshowroom check      jedes Exponat gegen sein Preset zählen
/swhshowroom clear      die Ausstellungsfläche wieder leer machen
```

**Was drin steht** (70 Exponate, darunter seit 2026-09-25 `spirevillage`, das ganze Spire-Dorf, je Reich eine Reihe — Skyreach, Eden,
Steinfeld, Geisterreich, Crooked Beyond, Hölle, Oberfläche):

* `realm-<reich>` — ein Bildschirm (48×28) echter Boden und echte Natur des
  Reichs, genau so, wie der Weltgenerator ihn an der Landestelle des Reichs malt.
* `tiles-<reich>` — jede Mod-Bodenkachel als 3×3-Fleck mit Schild.
* `walls-<reich>` — jede Mod-Wandfamilie als kleiner Raum: zwei Fenster, eine Tür.
* `objects-<reich>[-2,-3]` — jedes Mod-Objekt einmal, Vorderansicht, Schild mit
  der ID darunter. Wanddeko hängt an einer Steinwand, Tischdeko steht auf einem
  Eichentisch, Teppiche liegen auf dem Boden.
* jedes Gebäude: alle 33 `RealmPoiPresets` (Sky Tower, Sky Town, …, Hall of
  Doors) und alle eigenständigen Presets (Warden-Spire, Haunted Manor,
  Mausoleum, Crooked House, Graveyard, Aeronaut Camp, …).

**Wo:** Level `skyreach2`, Kacheln **20000,20000** bis ca. 20330,20500 — rund
27.000 Kacheln hinter dem Ende der Hölle. Da kommt man im Spiel nie hin; die
Befehle schreiben nie außerhalb dieses Rechtecks.

**Keine Monster:** Der Boden bekommt das Biom `swhshowroom` (Spawn-Tabellen
leer, Spawn-Cap 0). Wachen und Bewohner setzt nur der Weltgenerator, nie ein
Preset; was beim Generieren dort stand, wird entfernt. `check` zählt `mobs=0`.

**Licht:** An jeder Ecke jedes Exponats und neben jedem Schild steht eine
eiserne Straßenlaterne. Für Tageslicht: `/time noon`.

**Zweimal bauen ist harmlos:** `build` schreibt jedes Mal exakt dasselbe (feste
Seeds). Truhen werden dabei neu befüllt.

### 2. Screenshots automatisch (auf deinem PC)

```
/swhshots               alle Exponate der Ausstellung (erst /swhshowroom build)
/swhshots day           dasselbe, vorher /time noon
/swhshots eden          nur eine Reihe (skyreach, eden, steinfeld, ghostrealm, crookedbeyond, hell, surface)
/swhshots skytower      nur ein Exponat
/swhshots here 64 36    die Gegend um dich herum, 64×36 Kacheln
/swhshots world 8       Spire + die 8 nächsten POI-Plätze der echten Welt (Einzelspieler/selbst gehostet)
/swhshots stop          abbrechen
```

Für jedes Bild: teleportieren, warten bis du angekommen bist und alle Regionen
geladen sind, 2,5 s setzen lassen, dann rendert das Spiel genau das Rechteck des
Exponats in einen Offscreen-Puffer — **ohne HUD**, 32 Pixel pro Kachel, mit
Licht, Schatten und Splatting wie auf dem Bildschirm. Das ist Vanillas eigener
Karten-Screenshot (`Renderer.takeMapshot`), nur mit Namen statt Zeitstempel.

Die Bilder landen in:

```
<Necesse-Datenordner>\swh-screenshots\<Datum_Uhrzeit>\<exponat>.png
```

Der Datenordner ist der, in dem auch `screenshots\` und `saves\` liegen
(`GlobalData.appDataPath()`; unter Windows normalerweise `%APPDATA%\Necesse`).
Der genaue Pfad steht beim Start und am Ende der Tour im Chat.

Am Ende steht der Ordner im Chat. Falls die eigene Aufnahme scheitert, nimmt die
Tour Vanillas Karten-Screenshot (landet dann mit Zeitstempel in
`Necesse\screenshots\`) und sagt das im Chat.

### 3. Sprites für die Offline-Werkzeuge

```
/swhdumpsprites         alle PNGs unter objects/ tiles/ items/ mobs/ player/ buffs/ particles/
/swhdumpsprites all     wirklich alle PNGs des Spiels
```

Landet in `%APPDATA%\Necesse\swh-sprites\` (Pfade wie im Spiel,
`objects\stonewall.png`); die Mod-eigenen PNGs in `swh-sprites\_mod\`.
`MANIFEST.txt` listet alles. Das ist genau das Verzeichnis, das die
Offline-Werkzeuge als `NECESSE_SPRITES` / `--vanilla` erwarten.

### 4. Offline-Draufsicht jedes Gebäudes (Python)

Einmal: Python 3 und Pillow installieren (`py -m pip install pillow`). Dann im
Spiel `/swhshowroom build` und `/swhshowroom export` — das schreibt
`%APPDATA%\Necesse\swh-export\`. Im Repo-Ordner:

```
py tools\preset_render.py --data "%APPDATA%\Necesse\swh-export" --sprites "%APPDATA%\Necesse\swh-sprites" --out build\qa\presets
py tools\preset_render.py --data ... --sprites ... skytower hauntedmanor     (nur diese)
```

Die übrigen Werkzeuge mit demselben Dump:

```
set NECESSE_SPRITES=%APPDATA%\Necesse\swh-sprites
py tools\size_audit.py --vanilla "%NECESSE_SPRITES%"
py tools\wall_render_preview.py
py tools\scene_preview.py
```

(`scripts/sky_map_render.sh` braucht zusätzlich den Dedicated Server; unter
WSL/Git-Bash: `NECESSE_SPRITES=... scripts/sky_map_render.sh`.)

**Was die Draufsicht kann und was nicht** (steht auch im Bild):
Boden = echte Textur, aber harte Kanten statt Übergängen; Wände/Türen/Fenster =
Nachbau der Engine-Zeichenregeln; Zäune, Bäume, Felsen = dieselben Regeln wie
`sky_map_render.py`; Möbel = erste Zelle der Drehung, Pro-Klassen-Versatz
fehlt; kein Licht. Was keine Sprite-Datei hat, ist ein beschrifteter Farbblock
und steht in der Legende. **Das echte Bild ist `/swhshots`.**

---

## English — short

* **`/swhshowroom build|goto <id>|list|check|clear|export [dir]|tp x y`** —
  ADMIN. Stamps every `RealmPoiPresets` kind, every standalone preset, a wall /
  tile / object gallery per realm and a real-ground sample per realm on flat,
  lit, spawn-free ground at sky-level tile 20000,20000. `build` is idempotent
  and ends with a `check` (`SHOWROOM_CHECK exhibits=69 … missing=0 signs=69/69
  mobs=0`, VERIFIED [run] on the dedicated server in `scripts/integration_test.sh`).
* **`/swhshots [showroom|<id>|<group>|here [w h]|world [n]|stop] [day]`** —
  client. Tours the exhibits (or the spire and nearby POI sites) and renders each
  rectangle off-screen with the game's own map-screenshot path, no HUD, into
  `<settings>/swh-screenshots/<timestamp>/`. VERIFIED [jar]; running is
  HYPOTHESIS until run on a client.
* **`/swhdumpsprites [all]`** — client. Writes the game's PNGs to
  `<settings>/swh-sprites/` with their resource paths (and the mod's own to
  `_mod/`): the dump `NECESSE_SPRITES` / `--vanilla` expect.
* **`scripts/preset_render.sh`** (cloud/CI: boots the dedicated server, builds,
  exports, renders) and **`tools/preset_render.py --data <export> --sprites
  <dump>`** (anywhere) — top-down 32 px/tile renders of every exhibit into
  `build/qa/presets/`, drawn from what the game actually wrote.

Implementation: `src/main/java/stairwaytoheaven/showroom/`.
