#!/usr/bin/env python3
"""Ein Übersichtsblatt je Gebiet: Boden, Blöcke, Gegner, NPCs — als Bild.

WOZU. "Was steckt eigentlich in Eden?" liess sich bisher nur beantworten,
indem man `tools/area_census.py` (Zahlen) neben `tools/sprite_gallery.py`
(jedes Sheet in voller Länge) legte. Beides beantwortet die Frage nicht, die
ein Mensch stellt: *zeig mir das Gebiet auf einer Seite*. Genau das erzeugt
dieses Werkzeug — ein PNG je Gebiet, 64px-Kacheln mit deutschem Namen.

WOHER DIE ZUORDNUNG KOMMT (gelesen, nicht erinnert):
  * Boden und Blöcke: jedes Gebiet hat einen Terrain-Painter bzw. ein eigenes
    Paket. Was dort als `*ID`-Feld auftaucht, wird dort gesetzt. Die Felder
    lösen sich über die `registerTile`/`registerObject`-Aufrufe im ganzen Baum
    zu String-IDs auf.
  * Gegner, Kritter, NPCs, Boss: aus `area_census.py` importiert, damit es
    genau eine Quelle für diese Zahlen gibt.
  * Bilder: eigenes Sheet unter `src/main/resources`, sonst das geliehene
    Vanilla-Sheet (docs/VANILLA_ASSET_MAP.md ist die Prosa dazu; hier wird der
    Texturname aus dem Konstruktor bzw. `BorrowedMobIcon.from(...)` gelesen).

Was kein Gebiet beansprucht, landet auf dem Blatt "Bau-Sets & Möbel"; was
gar kein Blatt findet, wird am Ende als "nicht zugeordnet" gedruckt. Damit
ist jedes registrierte Tile und Objekt genau einmal zu sehen.

Aufruf:
    PYTHONPATH=/home/blackoffset/dev/pylib python3 tools/area_sheets.py [ZIELORDNER]
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from PIL import Image, ImageDraw, ImageFont

import area_census

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA = os.path.join(REPO, "src", "main", "java", "stairwaytoheaven")
RES = os.path.join(REPO, "src", "main", "resources")
VANILLA = os.environ.get("NECESSE_VANILLA_SPRITES", "/home/blackoffset/dev/Necesse sprites")
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(REPO, "build", "area-sheets")

FONT_DIR = "/usr/share/fonts/truetype/dejavu"

# --- Die Gebiete -------------------------------------------------------------
# realm: Index in area_census.REALMS, oder None für alles, was kein Realm der
# RealmDepth-Leiter ist. sources: Dateien/Ordner, deren *ID-Verweise das Gebiet
# ausmachen (relativ zu JAVA).
AREAS = [
    {
        "key": "01-skyreach", "title": "Skyreach", "realm": 0,
        "sub": "Die dritte Weltschicht über der Wolkendecke — Ziel der Himmelstreppe.",
        "sources": ["worldgen/SkyTerrainPainter.java", "worldgen/SkyLandscape.java",
                    "worldgen/SkyOrigin.java"],
    },
    {
        "key": "02-eden", "title": "Eden", "realm": 1,
        "sub": "Erstes Realm-Band: Paradiesgarten, überwuchert und verboten.",
        "sources": ["realms/eden"],
    },
    {
        "key": "03-steinfeld", "title": "Steinfeld", "realm": 2,
        "sub": "Zweites Band: wo der Himmel aufhört, richtig zu funktionieren.",
        "sources": ["realms/steinfeld"],
    },
    {
        "key": "04-ghostrealm", "title": "Ghost Realm", "realm": 3,
        # PLAN_ONE_PLANE hat die veil2-Dimension eingezogen; WORLD_DESIGN 41.5
        # legt Gloomfen und Ashen Reach mitsamt dem Veil-Boden in dieses Band.
        # Deshalb ist VeilTerrainPainter hier eine Quelle und kein eigenes Blatt.
        "sub": "Drittes Band: Nachgarten, Knochenhain, Ektomarsch — und der ehemalige Veil (Gloomfen, Ashen Reach).",
        "sources": ["realms/ghost", "worldgen/VeilTerrainPainter.java"],
    },
    {
        "key": "05-crookedbeyond", "title": "Crooked Beyond", "realm": 4,
        "sub": "Viertes Band: alles ein bisschen falsch — Schachwerk, Spiralfelder, Streifenöde, Beetle-Outlands.",
        "sources": ["realms/crooked", "worldgen/SkyOutlands.java"],
    },
    {
        "key": "06-hell", "title": "Hell", "realm": 5,
        "sub": "Fünftes Band: Ofenreich und Infernaler Saum.",
        "sources": ["realms/hell"],
    },
]

# Blatt für alles Gebaute: registriert, aber von keinem Painter gesetzt.
BUILD_SHEET = {
    "key": "07-bausets", "title": "Bau-Sets & Möbel", "realm": None,
    "sub": "Registriert, aber von keinem Terrain-Painter gesetzt: gebaut, gecraftet, in POIs gestellt.",
}

ITEM_SHEET = {"key": "08-gegenstaende", "title": "Gegenstände — alle Item-Icons",
              "sub": "Jedes Icon aus src/main/resources/items, alphabetisch."}

# Sheets, die kein eigenes Wesen sind: Overlay-Arme, geschorene Varianten,
# Geschlechts- und Altersblätter derselben Herde.
MOB_SHEET_SKIP = re.compile(r"(arms_(left|right)|_shorn|_head|_shadow|_(bull|calf)|-(doe|ram|lamb))$")


# --- Quelle lesen ------------------------------------------------------------
def read(path):
    with open(path, encoding="utf-8") as handle:
        return handle.read()


def java_files(root=JAVA):
    for base, _, names in os.walk(root):
        for name in sorted(names):
            if name.endswith(".java"):
                yield os.path.join(base, name)


CLASS_FILES = {os.path.basename(p)[:-5]: p for p in java_files()}


# Wie ein `*ID`-Feld zu seinem Bild kommt. Der Mod schreibt seine IDs auf fünf
# Arten fest, und nur die erste ist die offensichtliche:
#   1. direkt          skystoneID = TileRegistry.registerTile("skystonetile", ...)
#   2. über Helfer     auroralilyID = registerPickable("auroralily", ...)
#   3. Vanilla-Leihe   goldenOrchidID = object("yellowflowerpatch")
#   4. Vanilla-Leihe   gravefenceID = ObjectRegistry.getObjectID("cryptfence")
#   5. Paar-Ableitung  skyironFenceGateID = FenceGateObject.registerGatePair(...)
# Wer nur 1. liest, verliert zwei Drittel von Eden und dem Ghost Realm.
# Der Schwanz steht im Vorausschau-Ausdruck, wird also NICHT mitverbraucht:
# sonst verschluckt eine Zuweisung die zwei, die in den nächsten 260 Zeichen
# stehen -- so sind der Skyreach vier ihrer Böden abhanden gekommen.
ASSIGN = re.compile(r"\b([A-Za-z_][\w.]*ID)\s*=\s*(?:\n\s*)?([\w.]+)\(\s*(?=(.{0,260}))", re.S)
PLAIN = re.compile(r"(TileRegistry\.registerTile|ObjectRegistry\.registerObject)"
                   r"\(\s*\"([\w/]+)\"\s*,\s*(?=(.{0,260}))", re.S)
# Wandsätze landen in einem int[], nicht in einem *ID-Feld:
#   int[] skystoneWall = WallObject.registerWallObjects("skystonebrick",
#                                                       "skystonebrickwall", ...)
# Der zweite Name ist das Objekt, der erste das Material.
WALLS = re.compile(r"registerWallObjects\(\s*\"(\w+)\"\s*,\s*\"(\w+)\"", re.S)
LITERAL = re.compile(r"\"([\w/]+)\"")
TILE_CALLS = ("TileRegistry.registerTile", "TileRegistry.getTileID")
OBJECT_CALLS = ("ObjectRegistry.registerObject", "ObjectRegistry.getObjectID", "object")
HELPER_CALLS = ("registerPickable", "registerMeadowGrass", "natural", "plant", "descObject")


def registrations():
    """Feldname -> (art, string-id, texturkandidaten, hinweis)."""
    out = {}
    for path in java_files():
        source = read(path)
        for match in ASSIGN.finditer(source):
            field = match.group(1).split(".")[-1]
            callee = match.group(2)
            # Nur bis zum Semikolon: sonst liest die Texturerkennung das
            # `new ...Tile()` der NÄCHSTEN Registrierung und hängt jedem Boden
            # das Bild seines Nachbarn an.
            args = match.group(3).split(";", 1)[0]
            literals = LITERAL.findall(args)
            if callee.endswith("registerGatePair"):
                # Das Tor trägt keinen eigenen Literal-Namen; der Feldname ist
                # der Name (skyironFenceGateID -> skyironfencegate).
                out[field] = ("object", field[:-2].lower(), [field[:-2].lower()], args)
                continue
            if not literals:
                continue
            if callee.endswith("registerWallObjects"):
                # ("skystonebrick", "skystonebrickwall", ...) -- der zweite
                # Name ist das Objekt, der erste das Material.
                name = literals[1] if len(literals) > 1 else literals[0]
                out[field] = ("object", name, literals, args)
            elif callee in TILE_CALLS or callee.endswith("registerTile"):
                out[field] = ("tile", literals[0], literals[1:], args)
            elif (callee in OBJECT_CALLS or callee in HELPER_CALLS
                    # BedObject.registerBed, BenchObject.registerBench, ... --
                    # Möbel, die die Engine paarweise anlegt.
                    or re.match(r"\w+Object\.register[A-Z]", callee)):
                out[field] = ("object", literals[0], literals[1:], args)
        # Registriert, aber an kein Feld gebunden: das halbe Bau-Set steht so
        # in der Quelle. Ohne diesen zweiten Durchgang fehlt es überall.
        claimed = {entry[1] for entry in out.values()}
        for match in PLAIN.finditer(source):
            kind = "tile" if "Tile" in match.group(1) else "object"
            string_id = match.group(2)
            if string_id in claimed:
                continue
            out.setdefault(string_id, (kind, string_id, LITERAL.findall(match.group(3)),
                                       match.group(3)))
        for match in WALLS.finditer(source):
            out.setdefault(match.group(2),
                           ("object", match.group(2), [match.group(1)], ""))
    return out


REGISTRY = registrations()


def field_words(field):
    """`goldenOrchidID` -> `Golden Orchid` — der Name, den der Mod im Kopf hat."""
    words = re.sub(r"([a-z0-9])([A-Z])", r"\1 \2", field[:-2] if field.endswith("ID") else field)
    return words[:1].upper() + words[1:]


def declared_types():
    """Variablenname -> Klassenname, für `registerTile(id, palegrassTile, ...)`.

    Die Felder heissen nicht wie ihre Klasse: `palegrassTile` ist eine
    `PaleGrassTile`. Aus dem Feldnamen die Klasse zu raten, trifft das grosse G
    nicht -- die Deklaration weiss es.
    """
    out = {}
    pattern = re.compile(r"\b(?:public|private|protected|static|final)\s+"
                         r"(?:[\w.]+\s+)*?([\w.]*[A-Z]\w*)\s+(\w+)\s*[;=]")
    for path in java_files():
        for match in pattern.finditer(read(path)):
            out.setdefault(match.group(2), match.group(1).split(".")[-1])
    return out


DECLARED = declared_types()


def variable_textures():
    """Variablenname -> erstes Literal seiner Zuweisung.

    `hauntedGrassTile = new GhostGroundTile("murkmoss", ...)`: die Textur steht
    weder im Registrierungsaufruf noch in der Klasse, sondern hier.
    """
    out = {}
    pattern = re.compile(r"\b(\w+)\s*=\s*new\s+[\w.]+\s*\(\s*\"([\w/]+)\"")
    for path in java_files():
        for match in pattern.finditer(read(path)):
            out.setdefault(match.group(1), match.group(2))
    return out


VAR_TEXTURE = variable_textures()
# Vanilla legt Biom-Varianten desselben Gegenstands unter einem Blatt ab:
# `ivyoreswamp` wird aus `ivyore.png` gezeichnet.
BIOME_SUFFIX = re.compile(r"(swamp|cave|desert|snow|forest|jungle)$")


def super_texture(class_name, depth=0):
    """Der Texturname, den eine Klasse trägt: aus super(...) oder fromFile(...)."""
    if depth > 4 or class_name not in CLASS_FILES:
        return None
    source = read(CLASS_FILES[class_name])
    ctor = re.search(r"super\(([^;]*?)\);", source)
    if ctor:
        literal = re.search(r"\"([\w/]+)\"", ctor.group(1))
        if literal:
            return literal.group(1)
    loaded = re.search(r"fromFile\(\s*\"(?:objects/|tiles/)?([\w/]+)\"", source)
    if loaded:
        return loaded.group(1)
    parent = re.search(r"class\s+%s\s+extends\s+([\w.]+)" % class_name, source)
    if parent:
        return super_texture(parent.group(1).split(".")[-1], depth + 1)
    return None


def texture_candidates(kind, string_id, tail, extras=()):
    """Mögliche Bildnamen für eine registrierte ID, beste zuerst.

    Die eigene ID steht vorn: das erste Literal im Konstruktor ist nicht
    verlässlich der Texturname (`new OreObject("oremask", "fulguriteore", ...)`
    fängt mit der Maske an, und die ist bei jedem Erz dieselbe).
    """
    names = [string_id, re.sub(r"tile$", "", string_id)]
    made = re.search(r"new\s+([\w.]+)\s*\(\s*(?:\"([\w/]+)\")?", tail or "")
    if made:
        if made.group(2):
            names.append(made.group(2))
        # Auch Objekte tragen ihre Textur oft in der Klasse statt im Aufruf
        # (SkyCacheObject, EdenGateObject): dann sagt es die Klasse.
        from_class = super_texture(made.group(1).split(".")[-1])
        if from_class:
            names.append(from_class)
    else:
        # `SkyRegistry.palegrassTile` statt `new PaleGrassTile()`.
        variable = re.search(r"\b(\w+(?:Tile|Object))\b", tail or "")
        if variable:
            if variable.group(1) in VAR_TEXTURE:
                names.append(VAR_TEXTURE[variable.group(1)])
            from_class = super_texture(DECLARED.get(variable.group(1), ""))
            if from_class:
                names.append(from_class)
    names.extend(extras)
    # Die untere Hälfte eines Übergangs trägt das Blatt der oberen, und
    # Vanillas Biom-Varianten teilen sich eines.
    if string_id.endswith("down"):
        names.append(string_id[:-4] + "up")
    names.append(BIOME_SUFFIX.sub("", string_id))
    seen, ordered = set(), []
    for name in names:
        # Masken sind kein Bild der Sache: `new OreObject("oremask",
        # "fulguriteore", ...)` fängt mit der Schablone an, die alle Erze teilen,
        # und die ist ein weisses Rechteck.
        if "mask" in name:
            continue
        if name and name not in seen:
            seen.add(name)
            ordered.append(name)
    return ordered


def find_png(relatives):
    """Erst der Mod, dann der Vanilla-Abzug."""
    for root, origin in ((RES, "mod"), (VANILLA, "vanilla")):
        for rel in relatives:
            path = os.path.join(root, rel)
            if os.path.isfile(path):
                return path, origin
    return None, None


def tile_image(string_id, tail, extras=()):
    for name in texture_candidates("tile", string_id, tail, extras):
        # Wasser liegt in zwei Blättern (tief/flach); das tiefe ist das Bild,
        # das ein Mensch mit dem Nebelmeer verbindet.
        path, origin = find_png(["tiles/%s_splat.png" % name, "tiles/%s.png" % name,
                                 "tiles/%s_deep_splat.png" % name])
        if path:
            return path, origin
    return None, None


def object_image(string_id, tail, extras=()):
    """Das Inventar-Icon zeigt die Sache; das Sheet zeigt das Blatt."""
    for name in texture_candidates("object", string_id, tail, extras):
        # Leitern hängen ihr Ziel an den Texturnamen: LadderDownObject liest
        # `objects/<name>down` (EdenGateObject.java:25).
        path, origin = find_png(["items/%s.png" % name, "objects/%s.png" % name,
                                 "objects/statues/%s.png" % name,
                                 "objects/carpets/%s.png" % name,
                                 "objects/%sdown.png" % name])
        if path:
            return path, origin
    return None, None


def mob_class_files():
    """mob-id -> (Klassendatei, Datei die ihn registriert)."""
    out = {}
    for path in java_files():
        for match in re.finditer(
                r"MobRegistry\.registerMob\(\s*\"(\w+)\"\s*,\s*([\w.]+)\.class", read(path)):
            out[match.group(1)] = (CLASS_FILES.get(match.group(2).split(".")[-1]), path)
    return out


MOB_SOURCES = mob_class_files()
MOB_CLASSES = {mob: files[0] for mob, files in MOB_SOURCES.items()}
# Segmente einer Schlange sind kein eigenes Wesen.
MOB_PARTS = re.compile(r"(body|tail)$")
# Wo ein Mob registriert wird, sagt, in welches Gebiet er gehört -- das ist die
# einzige Auskunft für die ortsgebundenen Gegner, die in keiner Spawn-Tabelle
# stehen (Tollwright, Sourvat Bloom, Prototype Nine ... aus 5n-f).
MOB_REALM_DIR = {"realms/eden": 1, "realms/steinfeld": 2, "realms/ghost": 3,
                 "realms/crooked": 4, "realms/hell": 5}


def mob_realm(mob_id):
    path = MOB_SOURCES.get(mob_id, (None, ""))[1] or ""
    relative = os.path.relpath(path, JAVA) if path else ""
    for prefix, realm in MOB_REALM_DIR.items():
        if relative.startswith(prefix):
            return realm
    return 0


def livestock():
    """Nutztiere zählt der Zensus nicht: sie sind weder Gegner noch Siedler."""
    path = os.path.join(JAVA, "livestock", "SkyLivestock.java")
    if not os.path.isfile(path):
        return []
    return sorted(set(re.findall(r"MobRegistry\.registerMob\(\s*\"(\w+)\"", read(path))))


LIVESTOCK = livestock()


def mob_image(mob_id):
    """Eigenes Icon, sonst das geliehene Gesicht, sonst das eigene Sheet."""
    path, origin = find_png(["mobs/icons/%s.png" % mob_id])
    if path:
        return path, origin
    source_file = MOB_CLASSES.get(mob_id)
    if source_file and os.path.isfile(source_file):
        borrowed = re.search(r"BorrowedMobIcon\.from\(\s*\"(\w+)\"", read(source_file))
        if borrowed:
            path, _ = find_png(["mobs/icons/%s.png" % borrowed.group(1)])
            if path:
                return path, "geliehen: " + borrowed.group(1)
    path, origin = find_png(["mobs/%s.png" % mob_id])
    if path:
        return path, origin
    # Nutztiere liegen nach Geschlecht und Alter getrennt (glimmergoat-doe,
    # nimbusyak_bull); irgendeins davon ist das Tier.
    folder = os.path.join(RES, "mobs")
    for name in sorted(os.listdir(folder)):
        if name.startswith(mob_id) and name.endswith(".png"):
            return os.path.join(folder, name), "mod"
    # Siedler sind HumanShops: die Engine baut ihr Gesicht aus Haar, Haut und
    # Kleidung zusammen, es gibt kein Blatt. Vanillas Menschen-Icon ist das,
    # was der Spieler im Journal sieht.
    if source_file and re.search(r"Human(Shop|Mob)|SkySettlerMob", read(source_file)):
        return find_png(["mobs/icons/human.png"])
    return None, None


# --- Namen -------------------------------------------------------------------
def locale(lang="de"):
    """Abschnitt -> {id: Name}. Ohne [abschnitt]-Kopf zählt eine Zeile nicht."""
    out, section = {}, None
    path = os.path.join(RES, "locale", "%s.lang" % lang)
    if not os.path.isfile(path):
        return out
    for line in read(path).splitlines():
        line = line.strip()
        if line.startswith("[") and line.endswith("]"):
            section = line[1:-1]
            out.setdefault(section, {})
        elif section and "=" in line and not line.startswith("//"):
            key, _, value = line.partition("=")
            out[section][key.strip()] = value.strip()
    return out


DE = locale("de")
EN = locale("en")


def name_of(section, string_id):
    for table in (DE, EN):
        found = table.get(section, {}).get(string_id)
        if found:
            return found
    return string_id


def any_name(string_id):
    """Der Name eines Icons steht mal unter [item], mal unter [object]."""
    for section in ("item", "object", "tile", "mob"):
        found = name_of(section, string_id)
        if found != string_id:
            return found
    return string_id


# --- Zeichnen ----------------------------------------------------------------
BG = (24, 26, 32)
CARD = (44, 48, 58)
CARD_ALT = (52, 56, 68)
INK = (232, 234, 240)
DIM = (150, 156, 170)
RULE = (78, 84, 100)

SPRITE = 64
PAD = 8
CELL_W = SPRITE + 34
LABEL_H = 26
CELL_H = SPRITE + LABEL_H + 6
COLS = 7
MARGIN = 18


def font(size, bold=False):
    name = "DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"
    return ImageFont.truetype(os.path.join(FONT_DIR, name), size)


F_TITLE = font(26, True)
F_SUB = font(13)
F_SECTION = font(15, True)
F_LABEL = font(10)
F_SMALL = font(9)


def sprite_cell(path, kind):
    """Ein 64x64-Ausschnitt, der die Sache zeigt statt das Blatt."""
    image = Image.open(path).convert("RGBA")
    width, height = image.size
    if kind == "tile":
        # Splat-Atlas: 7x3-Zellen à 32px je 96px-Block. Zelle (3,0) ist die
        # erste der vier vollflächigen Varianten und laut
        # docs/research/splat-format.md 5.3 auch die Quelle des Item-Icons.
        if width >= 224 and height >= 96 and width % 224 == 0:
            image = image.crop((96, 0, 128, 32))
        elif width == 224:
            image = image.crop((96, 0, 128, min(32, height)))
    elif kind == "mob" and width >= 128 and height >= 128:
        # Ein Mob-Blatt ist ein Raster aus Laufbildern; ganz verkleinert ist es
        # ein Punktemuster. Das erste Bild der ersten Reihe ist das Tier.
        frame = height // 5 if height % 5 == 0 and height // 5 in (32, 48, 64) else 64
        image = image.crop((0, 0, min(frame, width), min(frame, height)))
    box = Image.new("RGBA", (SPRITE, SPRITE), (0, 0, 0, 0))
    scale = min(SPRITE / image.width, SPRITE / image.height)
    if scale >= 1:
        scale = max(1, int(scale))
    size = (max(1, int(image.width * scale)), max(1, int(image.height * scale)))
    image = image.resize(size, Image.NEAREST)
    box.paste(image, ((SPRITE - size[0]) // 2, (SPRITE - size[1]) // 2), image)
    return box


def wrap(draw, text, width, use_font):
    """Zwei Zeilen, notfalls am Bindestrich getrennt.

    "Zerbrochener Himmelswacht-Statue" ist der Normalfall und nicht die
    Ausnahme: Necesse-Namen sind lang und zusammengesetzt. Wer nur an
    Leerzeichen trennt, schiebt die Hälfte davon in die Nachbarkachel.
    """
    words = [w for w in re.split(r"(?<=[-–/])|\s+", text) if w]
    lines, line = [], ""
    for word in words:
        probe = line + ("" if line.endswith(("-", "–", "/")) else " ") + word if line else word
        if draw.textlength(probe, font=use_font) <= width or not line:
            line = probe
        else:
            lines.append(line)
            line = word
    if line:
        lines.append(line)
    lines = lines[:2]
    for index, text_line in enumerate(lines):
        if draw.textlength(text_line, font=use_font) > width:
            while text_line and draw.textlength(text_line + "…", font=use_font) > width:
                text_line = text_line[:-1]
            lines[index] = text_line + "…"
    return lines


class Sheet:
    """Ein Blatt: Kopf, Abschnitte, Kacheln — in einem Rutsch gezeichnet."""

    def __init__(self, title, subtitle, facts):
        self.title, self.subtitle, self.facts = title, subtitle, facts
        self.sections = []

    def section(self, heading, cells, note=""):
        self.sections.append((heading, cells, note))

    def height(self):
        total = MARGIN + 34 + 20 + 18 * len(self.facts) + 10
        for heading, cells, note in self.sections:
            rows = max(1, -(-len(cells) // COLS))
            total += 30 + (12 if note else 0) + rows * CELL_H + 10
        return total + MARGIN

    def render(self, path):
        width = MARGIN * 2 + COLS * CELL_W
        image = Image.new("RGB", (width, self.height()), BG)
        draw = ImageDraw.Draw(image)
        y = MARGIN
        draw.text((MARGIN, y), self.title, font=F_TITLE, fill=INK)
        y += 34
        draw.text((MARGIN, y), self.subtitle, font=F_SUB, fill=DIM)
        y += 20
        for fact in self.facts:
            draw.text((MARGIN, y), fact, font=F_SUB, fill=DIM)
            y += 18
        y += 10
        for heading, cells, note in self.sections:
            draw.text((MARGIN, y), heading, font=F_SECTION, fill=INK)
            draw.line((MARGIN, y + 21, width - MARGIN, y + 21), fill=RULE)
            y += 30
            if note:
                draw.text((MARGIN, y - 4), note, font=F_SMALL, fill=DIM)
                y += 12
            if not cells:
                draw.text((MARGIN, y), "—", font=F_LABEL, fill=DIM)
                y += 20
                continue
            for index, (label, sub, sprite) in enumerate(cells):
                col, row = index % COLS, index // COLS
                x0 = MARGIN + col * CELL_W
                y0 = y + row * CELL_H
                draw.rectangle((x0, y0, x0 + CELL_W - 6, y0 + CELL_H - 6),
                               fill=CARD if (col + row) % 2 == 0 else CARD_ALT)
                if sprite is not None:
                    image.paste(sprite, (x0 + (CELL_W - 6 - SPRITE) // 2, y0 + 3), sprite)
                else:
                    draw.text((x0 + 24, y0 + 26), "kein Bild", font=F_SMALL, fill=DIM)
                text_y = y0 + SPRITE + 5
                for line in wrap(draw, label, CELL_W - 12, F_LABEL):
                    draw.text((x0 + 4, text_y), line, font=F_LABEL, fill=INK)
                    text_y += 11
                if sub and text_y <= y0 + SPRITE + 5 + 11:
                    draw.text((x0 + 4, text_y), sub[:22], font=F_SMALL, fill=DIM)
            y += max(1, -(-len(cells) // COLS)) * CELL_H + 10
        image.save(path)
        return path


# Was auf keinem Weg zu einem Bild führt -- am Ende gedruckt, nie verschwiegen.
BLIND = []


def cell_for(entry):
    kind, string_id, extras, tail, field = entry
    if kind == "tile":
        path, origin = tile_image(string_id, tail, extras)
        label = name_of("tile", string_id)
    else:
        path, origin = object_image(string_id, tail, extras)
        label = name_of("object", string_id)
    if label == string_id:
        # Keine Locale-Zeile: das ist eine Vanilla-Leihe, und der Feldname ist
        # der Name, den der Mod ihr gibt.
        label = field_words(field)
    if not path:
        BLIND.append("%s %s" % (kind, string_id))
    sprite = sprite_cell(path, kind) if path else None
    return (label, "" if origin == "mod" else "Vanilla-Leihe", sprite)


def mob_name(mob_id):
    name = name_of("mob", mob_id)
    return name if name != mob_id else mob_id.title()


RENDERED_MOBS = set()


def mob_cell(mob_id):
    RENDERED_MOBS.add(mob_id)
    path, origin = mob_image(mob_id)
    if not path:
        BLIND.append("mob %s" % mob_id)
    sprite = sprite_cell(path, "mob") if path else None
    sub = "" if origin == "mod" else (origin or "")
    return (mob_name(mob_id), "Vanilla-Leihe" if sub else "", sprite)


ALL_IDS = {entry[1] for entry in REGISTRY.values()}


def drop_right_halves(entries):
    """`stormcrystalr` ist die rechte Hälfte von `stormcrystal`, kein Ding.

    Geprüft wird gegen ALLE IDs, nicht nur gegen die des Blattes: sonst steht
    die rechte Hälfte allein auf dem Bau-Set-Blatt, weil die linke ein Gebiet
    für sich beansprucht hat.
    """
    return {i: e for i, e in entries.items() if not (i.endswith("r") and i[:-1] in ALL_IDS)}


def area_ids(area):
    """Die von diesem Gebiet gesetzten Tile- und Objekt-IDs."""
    paths = []
    for source in area["sources"]:
        full = os.path.join(JAVA, source)
        if os.path.isdir(full):
            paths.extend(java_files(full))
        elif os.path.isfile(full):
            paths.append(full)
    tiles, objects = {}, {}
    for path in paths:
        for field in sorted(set(re.findall(r"\b(\w+ID)\b", read(path)))):
            entry = REGISTRY.get(field)
            if not entry:
                continue
            kind, string_id, extras, tail = entry
            row = (kind, string_id, extras, tail, field)
            (tiles if kind == "tile" else objects)[string_id] = row
    return drop_right_halves(tiles), drop_right_halves(objects)


def site_bound_mobs(realms):
    """Registrierte Mobs, die in keiner Spawn-Tabelle stehen.

    Die Gegner aus 5n-f stehen in Orten statt im Gelände: Tollwright im
    Zollhaus, Sourvat Bloom im Grange Cellar, Prototype Nine auf dem Test
    Range. Der Zensus liest Spawn-Tabellen und kann sie nicht kennen -- ohne
    diesen Schritt fehlen sie auf jedem Blatt.
    """
    placed = set(LIVESTOCK)
    for realm in realms:
        placed |= set(realm["hostiles"]) | set(realm["critters"]) | set(realm["npcs"])
    rest = {}
    for mob_id in MOB_SOURCES:
        if mob_id in placed or MOB_PARTS.search(mob_id):
            continue
        rest.setdefault(mob_realm(mob_id), []).append(mob_id)
    return {realm: sorted(mobs) for realm, mobs in rest.items()}


def main():
    os.makedirs(OUT, exist_ok=True)
    realms, _ = area_census.census()
    site_bound = site_bound_mobs(realms)
    claimed_tiles, claimed_objects = set(), set()
    written = []

    for area in AREAS:
        tiles, objects = area_ids(area)
        claimed_tiles |= set(tiles)
        claimed_objects |= set(objects)
        facts = []
        hostiles, critters, npcs = [], [], []
        if area["realm"] is not None:
            realm = realms[area["realm"]]
            hostiles, critters, npcs = realm["hostiles"], realm["critters"], realm["npcs"]
            facts.append("Biome: " + ", ".join(b.replace("Biome", "") for b in realm["biomes"]))
            if realm["boss"]:
                boss, tier, base = realm["boss"]
                facts.append("Boss: %s — Stufe %d, %d Grund-TP" % (mob_name(boss), tier, base))
            facts.append("Orte: %d · Quests: %d" % (len(realm["pois"]), len(realm["quests"])))
        # Nimbus-Yak und Glimmergoat gehören der Skyreach; der Zensus kennt
        # nur Gegner, Kritter und Siedler und lässt Nutztiere aus.
        friends = sorted(set(npcs) | set(critters) | set(LIVESTOCK if area["realm"] == 0 else []))
        facts.append("%d Böden · %d Blöcke · %d Gegner · %d NPCs/Tiere"
                     % (len(tiles), len(objects), len(hostiles), len(friends)))

        sheet = Sheet(area["title"], area["sub"], facts)
        sheet.section("Böden (Tiles)", [cell_for(tiles[i]) for i in sorted(tiles)])
        sheet.section("Blöcke & Objekte", [cell_for(objects[i]) for i in sorted(objects)])
        sheet.section("Gegner", [mob_cell(m) for m in hostiles])
        bound = site_bound.get(area["realm"], []) if area["realm"] is not None else []
        if bound:
            sheet.section("Ortsgebundene Gegner", [mob_cell(m) for m in bound],
                          "Stehen in einem Ort statt im Gelände — in keiner Spawn-Tabelle.")
        sheet.section("NPCs & Tiere", [mob_cell(m) for m in friends])
        written.append(sheet.render(os.path.join(OUT, area["key"] + ".png")))
        print("%-18s %2d Böden %3d Blöcke %2d Gegner (+%d ortsgebunden) %2d NPCs/Tiere"
              % (area["title"], len(tiles), len(objects), len(hostiles), len(bound), len(friends)))

    # Alles Registrierte, das kein Painter setzt.
    rest_tiles = drop_right_halves(
        {i: (k, i, e, t, f) for f, (k, i, e, t) in REGISTRY.items()
         if k == "tile" and i not in claimed_tiles})
    rest_objects = drop_right_halves(
        {i: (k, i, e, t, f) for f, (k, i, e, t) in REGISTRY.items()
         if k == "object" and i not in claimed_objects})
    sheet = Sheet(BUILD_SHEET["title"], BUILD_SHEET["sub"],
                  ["%d Böden · %d Objekte" % (len(rest_tiles), len(rest_objects))])
    sheet.section("Böden (Tiles)", [cell_for(rest_tiles[i]) for i in sorted(rest_tiles)])
    sheet.section("Blöcke & Objekte", [cell_for(rest_objects[i]) for i in sorted(rest_objects)])
    written.append(sheet.render(os.path.join(OUT, BUILD_SHEET["key"] + ".png")))
    print("%-18s %2d Böden %3d Objekte" % (BUILD_SHEET["title"], len(rest_tiles), len(rest_objects)))

    # Item-Icons, vollständig.
    items = sorted(os.path.basename(p)[:-4]
                   for p in os.listdir(os.path.join(RES, "items")) if p.endswith(".png"))
    sheet = Sheet(ITEM_SHEET["title"], ITEM_SHEET["sub"], ["%d Icons" % len(items)])
    sheet.section("Item-Icons", [(any_name(i), "",
                                 sprite_cell(os.path.join(RES, "items", i + ".png"), "item"))
                                for i in items])
    written.append(sheet.render(os.path.join(OUT, ITEM_SHEET["key"] + ".png")))
    print("%-18s %3d Icons" % ("Gegenstände", len(items)))

    # Was kein Blatt zeigt: ein eigenes Sheet, das an keiner Registrierung
    # hängt. Ein Objekt kann Icon UND Sheet haben -- beide gelten als gezeigt,
    # sonst meldet der Bericht jedes Objekt-Sheet als verloren.
    shown = set()
    for kind, string_id, extras, tail in REGISTRY.values():
        for name in texture_candidates(kind, string_id, tail, extras):
            # Die Engine leitet aus einem Texturnamen weitere Blätter ab:
            # <name>debris beim Zerschlagen, <name>mask/_mask fürs Darüberliegen,
            # _deep/_shallow beim Wasser, _on/_off beim Schalten.
            for rel in ("tiles/%s_splat.png" % name, "tiles/%s.png" % name,
                        "tiles/%s_deep_splat.png" % name, "tiles/%s_shallow_splat.png" % name,
                        "objects/%s.png" % name, "objects/%s_on.png" % name,
                        "objects/%s_off.png" % name, "objects/%s_mask.png" % name,
                        "objects/%sdebris.png" % name, "objects/%smask.png" % name,
                        "objects/statues/%s.png" % name, "objects/carpets/%s.png" % name,
                        "objects/carpets/%smask.png" % name, "items/%s.png" % name):
                if os.path.isfile(os.path.join(RES, rel)):
                    shown.add(rel)
    for mob_id in MOB_CLASSES:
        for rel in ("mobs/icons/%s.png" % mob_id, "mobs/%s.png" % mob_id):
            if os.path.isfile(os.path.join(RES, rel)):
                shown.add(rel)
    missing = []
    for folder in ("tiles", "objects", "mobs"):
        for base, _, names in os.walk(os.path.join(RES, folder)):
            for name in names:
                if not name.endswith(".png"):
                    continue
                rel = os.path.relpath(os.path.join(base, name), RES)
                if rel not in shown and not MOB_SHEET_SKIP.search(name[:-4]):
                    missing.append(rel)
    # Jeder registrierte Mob steht auf genau einem Blatt -- oder hier.
    forgotten = [m for m in MOB_SOURCES
                 if m not in RENDERED_MOBS and not MOB_PARTS.search(m)]
    if forgotten:
        print("\nregistriert, aber auf keinem Blatt (%d):" % len(forgotten))
        for mob_id in sorted(forgotten):
            print("  mob " + mob_id)
    if BLIND:
        print("\nohne Bild (%d):" % len(BLIND))
        for row in sorted(set(BLIND)):
            print("  " + row)
    if missing:
        print("\nnicht zugeordnet (%d):" % len(missing))
        for rel in sorted(missing):
            print("  " + rel)
    print("\n%d Blätter in %s" % (len(written), OUT))


if __name__ == "__main__":
    main()
