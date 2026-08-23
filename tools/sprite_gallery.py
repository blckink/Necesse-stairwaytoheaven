#!/usr/bin/env python3
"""Builds a single-file HTML sprite atlas of every shipped texture.

Usage:  python3 tools/sprite_gallery.py [out.html]

The page embeds every PNG from src/main/resources as a data URI, upscales
them with CSS pixelated rendering, and groups them by category with notes on
each sheet's layout. Sprites sit on a checkerboard "ground" that can be
toggled between Stormslate-dark and Cloudturf-light — the same two grounds
the style guide demands every sprite must read against. Regenerate after
every art batch; the result is meant to be published/shared for review.
"""
import base64
import html
import os
import sys

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "src", "main", "resources")
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(REPO, "build", "sprite-gallery.html")

# (label, note) per resource-relative path; missing files fall back to filename.
META = {
    "tiles/cloudturf_splat.png": ("Cloudturf", "Boden der Driftlande. Splat-Atlas: Spalten 3–6 der obersten Reihe sind die 4 Voll-Varianten, die übrigen 17 Zellen sind Übergangs-Blends."),
    "tiles/skystone_splat.png": ("Skystone", "Nackter Fels aller Biome. Gleicher Splat-Aufbau."),
    "tiles/stormslate_splat.png": ("Stormslate", "Boden des Sturmschleiers, mit Ladungs-Violett. Gleicher Splat-Aufbau."),
    "tiles/gloomwoodfloor_splat.png": ("Gloomwood-Dielen", "Baubarer Holzboden (Bau-Set). Splat mit 2 Varianten."),
    "tiles/marblechecker.png": ("Schachbrett-Marmor", "Welt-verankertes 2x2-Muster - bewusst OHNE Splat, damit das Schachbrett beim Bauen durchlaeuft."),
    "tiles/mistsea_deep_splat.png": ("Mistsea (offen)", "Die offene Wolkendecke - 8 Animations-Frames, grosse Ballen driften nach Osten, Schleier nach Westen."),
    "tiles/mistsea_shallow_splat.png": ("Mistsea (Ufer)", "Duenneres, helleres Uferband - gleiche 8-Frame-Schleife."),
    "objects/skystonerock.png": ("Skystone-Fels", "Abbaubarer Fels. Autotile: Spalten = Varianten, 13 Reihen aus 16-px-Vierteln."),
    "objects/aetheriumore.png": ("Aetherium-Erz (Overlay)", "2 Muster-Varianten, die die Engine ueber den Fels maskiert."),
    "objects/stormcrystal.png": ("Sturmkristall", "2x1-Cluster: gerade Spalten = linke Haelfte, ungerade = rechte."),
    "objects/aurorabloom.png": ("Aurorabluete", "2x1-Cluster wie der Sturmkristall, Rose/Teal-Akzent der Aurorabaenke."),
    "objects/skyreeds.png": ("Himmelsschilf", "4 Gras-Varianten, wachsen nur auf organischem Cloudturf."),
    "objects/skystairwaydown.png": ("Himmelstreppe (Oberflaeche)", "Der Aufgang. Oberste 32x32 = Bodenteil, darunter der Aufbau."),
    "objects/skystairwayup.png": ("Rueckweg-Treppe (Himmel)", "Wird oben automatisch platziert."),
    "objects/skystonebrickwall.png": ("Skystone-Ziegelwand", "Wand-Set 352x128: Autotile-Blob + Fenster-Einsatz + 8 Tuer-Rahmen. Tuer + Fenster entstehen automatisch daraus."),
    "objects/nightfellwall.png": ("Nightfell-Wand", "Zweites Wand-Set, dunkles Thema."),
    "objects/skyironfence.png": ("Schmiedeeisen-Zaun", "5 Verbindungs-Spalten."),
    "objects/skyironfencegate.png": ("Zauntor", "6 Spalten inkl. offen/zu."),
    "objects/wardencandelabra.png": ("Wardens Kandelaber", "Strassenlampe: obere Haelfte an, untere aus (per Schalter/Draht)."),
    "objects/mistglasslantern.png": ("Mistglas-Laterne", "Wandlicht: 2 Spalten an/aus x 4 Wand-Ausrichtungen."),
    "objects/flickerlightgarland.png": ("Flickerlicht-Girlande", "Bunte Wand-Lichterkette (Quest-Belohnung), gleiches Layout."),
    "objects/statues/gloomraven.png": ("Gloomraben-Statue", "Gotische Statue, 1 Pose."),
    "objects/gloomwillow.png": ("Gloomweide", "Kahler Deko-Baum, 2 Varianten."),
    "objects/catbasket.png": ("Katzenkorb", "Haustier-Bett (Quest-Belohnung)."),
    "objects/skywatchbanner.png": ("Skywatch-Banner", "Wandbild: 4 Rotations-Reihen."),
    "objects/wardenbeaconoff.png": ("Leuchtfeuer (erloschen)", "Der Zustand vor Quest-Stufe 2."),
    "objects/wardenbeaconon.png": ("Leuchtfeuer (entzuendet)", "Nach der Abgabe - leuchtet kalt-tuerkis."),
    "objects/skyanchor.png": ("Inselanker", "Erscheint nach der Anker-Quest neben dem Leuchtfeuer."),
    "mobs/zephyrray.png": ("Zephyrrochen", "Schneller Nahkampf-Flieger. 6 Spalten (Idle, Lauf x4, Schwimmen) x 4 Reihen (Oben/Rechts/Unten/Links)."),
    "mobs/stormwisp.png": ("Sturmirrlicht", "Fernkampf-Geist: Reihe 1 Koerper, Reihe 2 Gluehen."),
    "mobs/skystonegolem.png": ("Himmelsstein-Golem", "Gepanzerter Brocken, gleiches Lauf-Layout."),
    "mobs/skywarden.png": ("Der Sky Warden", "Quest-NPC: hagerer Waechter mit Laternenstab."),
    "mobs/spirecatblack.png": ("Siggi", "Die schwarze Turmkatze (versteckt im Sturmschleier)."),
    "mobs/spirecattabby.png": ("Peanut", "Die weiss-getigerte Turmkatze (Aurorabaenke)."),
    "player/weapons/tempestedge.png": ("Sturmklinge (gehalten)", "Haltegrafik der Schwert-Hand."),
    "player/weapons/galehowl.png": ("Windheuler (gehalten)", "Haltegrafik des Bogens."),
    "preview.png": ("Mod-Preview", "268x268-Vorschaubild fuer Mod-Liste/Workshop."),
}

ITEM_NAMES = {
    "skystone": "Skystone", "aetheriumore": "Aetherium-Erz", "aetheriumbar": "Aetherium-Barren",
    "stormshard": "Sturmsplitter", "windsilk": "Windseide", "aurorapetal": "Aurorablatt",
    "tempestedge": "Sturmklinge", "galehowl": "Windheuler", "cloudpufftreat": "Wolkenzupf-Leckerli",
    "silverbell": "Silbergloeckchen", "skystairwaydown": "Himmelstreppe", "skyreeds": "Himmelsschilf",
    "skystonerock": "Skystone-Fels", "stormcrystal": "Sturmkristall", "aurorabloom": "Aurorabluete",
    "skystonebrickwall": "Ziegelwand", "skystonebrickdoor": "Ziegeltuer", "nightfellwall": "Nightfell-Wand",
    "nightfelldoor": "Nightfell-Tuer", "skyironfence": "Zaun", "skyironfencegate": "Zauntor",
    "wardencandelabra": "Kandelaber", "mistglasslantern": "Mistglas-Laterne",
    "flickerlightgarland": "Girlande", "gloomravenstatue": "Raben-Statue", "gloomwillow": "Gloomweide",
    "catbasket": "Katzenkorb", "skywatchbanner": "Banner",
}

SECTIONS = [
    ("terrain", "Terrain & Böden", "Die Splat-Atlanten, aus denen die Engine alle Bodenübergänge mischt.",
     ["tiles/cloudturf_splat.png", "tiles/skystone_splat.png", "tiles/stormslate_splat.png",
      "tiles/gloomwoodfloor_splat.png", "tiles/marblechecker.png"]),
    ("natur", "Natur & Ressourcen", "Alles, was in den drei Biomen wächst und abgebaut wird.",
     ["objects/skystonerock.png", "objects/aetheriumore.png", "objects/stormcrystal.png",
      "objects/aurorabloom.png", "objects/skyreeds.png"]),
    ("bauset", "Bau-Set „Nightfell & Skylight“", "Wände, Böden, Licht und Deko — größtenteils beim Warden craftbar.",
     ["objects/skystonebrickwall.png", "objects/nightfellwall.png", "objects/skyironfence.png",
      "objects/skyironfencegate.png", "objects/wardencandelabra.png", "objects/mistglasslantern.png",
      "objects/flickerlightgarland.png", "objects/statues/gloomraven.png", "objects/gloomwillow.png",
      "objects/catbasket.png", "objects/skywatchbanner.png", "objects/wardenbeaconoff.png",
      "objects/wardenbeaconon.png", "objects/skyanchor.png"]),
    ("bewohner", "Bewohner & Gegner", "Lauf-Sheets: 6 Spalten (Idle, Lauf ×4, Schwimmen) × 4 Blickrichtungen.",
     ["mobs/skywarden.png", "mobs/spirecatblack.png", "mobs/spirecattabby.png",
      "mobs/zephyrray.png", "mobs/stormwisp.png", "mobs/skystonegolem.png"]),
    ("treppen", "Die Treppen", "Das namensgebende Objekt-Paar.",
     ["objects/skystairwaydown.png", "objects/skystairwayup.png",
      "player/weapons/tempestedge.png", "player/weapons/galehowl.png", "preview.png"]),
]


def data_uri(path):
    with open(path, "rb") as f:
        return "data:image/png;base64," + base64.b64encode(f.read()).decode()


def scale_for(w, h):
    if max(w, h) <= 64:
        return 4
    if h <= 128 and w <= 224:
        return 3
    return 2


def card(rel):
    p = os.path.join(RES, rel)
    im = Image.open(p)
    w, h = im.size
    label, note = META.get(rel, (os.path.basename(rel)[:-4], ""))
    s = scale_for(w, h)
    esc = html.escape
    return (
        f'<figure class="card">'
        f'<figcaption><span class="nm">{esc(label)}</span>'
        f'<span class="meta">{esc(rel)} · {w}×{h} · {s}×</span></figcaption>'
        f'<div class="vp"><img src="{data_uri(p)}" width="{w * s}" height="{h * s}" alt="{esc(label)}"></div>'
        + (f'<p class="note">{esc(note)}</p>' if note else "")
        + "</figure>"
    )


def mist_block():
    out = ['<figure class="card"><figcaption><span class="nm">Mistsea — live</span>'
           '<span class="meta">tiles/mistsea_*_splat.png · 8 Frames · 2×</span></figcaption>']
    for rel, cap in (("tiles/mistsea_deep_splat.png", "offene Wolkendecke"),
                     ("tiles/mistsea_shallow_splat.png", "Uferband")):
        p = os.path.join(RES, rel)
        out.append(
            f'<div class="mistcap">{cap}</div>'
            f'<div class="mist"><img class="mistroll" src="{data_uri(p)}" width="3584" height="192" alt="{cap}"></div>'
        )
    out.append('<p class="note">So rollt die Wolkendecke im Spiel — die Animation hier ist die echte '
               '8-Frame-Schleife aus der Datei.</p></figure>')
    return "".join(out)


def mini_grid(subdir, names):
    cells = []
    d = os.path.join(RES, subdir)
    for f in sorted(os.listdir(d)):
        if not f.endswith(".png"):
            continue
        key = f[:-4]
        name = names.get(key, key)
        cells.append(f'<div class="it"><div class="ivp"><img src="{data_uri(os.path.join(d, f))}" '
                     f'width="64" height="64" alt="{html.escape(name)}"></div>'
                     f'<span>{html.escape(name)}</span></div>')
    return '<div class="itemgrid">' + "".join(cells) + "</div>"


def items_grid():
    return mini_grid("items", ITEM_NAMES)


def build():
    chips = "".join(f'<a href="#{sid}">{title}</a>' for sid, title, _, _ in SECTIONS) + '<a href="#items">Items</a>'
    body = []
    for sid, title, intro, files in SECTIONS:
        body.append(f'<section id="{sid}"><h2>{title}</h2><p class="intro">{intro}</p>')
        if sid == "terrain":
            body.append(mist_block())
        body.extend(card(rel) for rel in files)
        if sid == "bewohner":
            body.append('<p class="intro">Bestiarium-Icons (32×32):</p>')
            body.append(mini_grid("mobs/icons", {
                "skywarden": "Sky Warden", "spirecatblack": "Siggi", "spirecattabby": "Peanut",
                "zephyrray": "Zephyrrochen", "stormwisp": "Sturmirrlicht", "skystonegolem": "Golem"}))
        body.append("</section>")
    body.append('<section id="items"><h2>Items</h2>'
                '<p class="intro">Alle Inventar-Icons (32×32, hier 2×).</p>' + items_grid() + "</section>")

    n = sum(len(f) for _, _, _, f in SECTIONS) + len(os.listdir(os.path.join(RES, "items"))) + 2
    page = PAGE.replace("__CHIPS__", chips).replace("__BODY__", "".join(body)).replace("__COUNT__", str(n))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        f.write(page)
    print(f"{OUT}  ({os.path.getsize(OUT) // 1024} KB)")


PAGE = """<title>Skyreach Sprite-Atlas</title>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Pixelify+Sans:wght@500;600&family=Atkinson+Hyperlegible:ital,wght@0,400;0,700&family=IBM+Plex+Mono:wght@400;500&display=swap">
<style>
:root{
  --bg:#14161f; --panel:#1c1f2b; --line:#2b2f40; --ink:#dfe4ee; --mut:#8b93a8;
  --acc:#7adfd8; --accink:#0e2724;
  --chk-a:#262a38; --chk-b:#2e3345;
}
@media (prefers-color-scheme: light){
  :root:not([data-theme="dark"]){
    --bg:#eef1f6; --panel:#ffffff; --line:#d7dce8; --ink:#232735; --mut:#5d6577;
    --acc:#127c74; --accink:#eafffd;
  }
}
:root[data-theme="light"]{
  --bg:#eef1f6; --panel:#ffffff; --line:#d7dce8; --ink:#232735; --mut:#5d6577;
  --acc:#127c74; --accink:#eafffd;
}
:root[data-theme="dark"]{
  --bg:#14161f; --panel:#1c1f2b; --line:#2b2f40; --ink:#dfe4ee; --mut:#8b93a8;
  --acc:#7adfd8; --accink:#0e2724;
}
.cloudbg{ --chk-a:#c7d2ca; --chk-b:#d9e2da; }
*{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--ink);
  font:16px/1.55 "Atkinson Hyperlegible",system-ui,sans-serif;}
header{position:sticky;top:0;z-index:5;background:var(--bg);
  border-bottom:1px solid var(--line);padding:10px 14px 0;}
h1{font:600 26px/1.1 "Pixelify Sans","Atkinson Hyperlegible",sans-serif;
  margin:4px 0 2px;letter-spacing:.5px;}
.sub{color:var(--mut);font-size:13px;margin:0 0 8px;}
.sub b{color:var(--acc);font-weight:700;}
.bar{display:flex;gap:8px;align-items:center;padding-bottom:10px;flex-wrap:wrap;}
button#ground{font:14px "Atkinson Hyperlegible",sans-serif;border:1px solid var(--line);
  background:var(--panel);color:var(--ink);border-radius:6px;padding:6px 10px;cursor:pointer;}
button#ground .dot{display:inline-block;width:10px;height:10px;border-radius:2px;
  background:conic-gradient(var(--chk-a) 25%,var(--chk-b) 0 50%,var(--chk-a) 0 75%,var(--chk-b) 0);
  margin-right:6px;vertical-align:-1px;}
nav{display:flex;gap:6px;overflow-x:auto;padding-bottom:2px;scrollbar-width:none;}
nav a{flex:none;font-size:13px;color:var(--ink);text-decoration:none;
  border:1px solid var(--line);background:var(--panel);border-radius:99px;padding:4px 11px;}
nav a:focus-visible,button#ground:focus-visible{outline:2px solid var(--acc);outline-offset:2px;}
main{max-width:720px;margin:0 auto;padding:8px 14px 60px;}
section{margin-top:30px;}
h2{font:500 20px/1.2 "Pixelify Sans",sans-serif;color:var(--acc);
  border-bottom:1px solid var(--line);padding-bottom:6px;margin:0 0 4px;}
.intro{color:var(--mut);font-size:14px;margin:4px 0 14px;max-width:62ch;}
.card{margin:0 0 18px;background:var(--panel);border:1px solid var(--line);
  border-radius:8px;padding:12px;}
figcaption{display:flex;justify-content:space-between;gap:10px;align-items:baseline;
  flex-wrap:wrap;margin-bottom:8px;}
.nm{font-weight:700;}
.meta{font:12px "IBM Plex Mono",monospace;color:var(--mut);}
.vp,.mist,.ivp{background:
  conic-gradient(var(--chk-a) 25%,var(--chk-b) 0 50%,var(--chk-a) 0 75%,var(--chk-b) 0)
  0 0/16px 16px;border-radius:4px;}
.vp{overflow-x:auto;padding:10px;}
.vp img,.ivp img,.mistroll{image-rendering:pixelated;display:block;}
.note{color:var(--mut);font-size:14px;margin:8px 0 0;max-width:62ch;}
.mistcap{font:12px "IBM Plex Mono",monospace;color:var(--mut);margin:8px 0 4px;}
.mist{overflow:hidden;width:448px;max-width:100%;height:192px;position:relative;}
.mistroll{animation:roll 1.9s steps(8) infinite;}
@keyframes roll{to{transform:translateX(-3584px)}}
@media (prefers-reduced-motion: reduce){.mistroll{animation:none}}
.itemgrid{display:grid;grid-template-columns:repeat(auto-fill,minmax(88px,1fr));gap:10px;}
.it{background:var(--panel);border:1px solid var(--line);border-radius:8px;
  padding:8px;text-align:center;font-size:12px;color:var(--mut);}
.it .ivp{display:flex;justify-content:center;padding:6px;margin-bottom:6px;}
</style>
<header>
  <h1>Skyreach Sprite-Atlas</h1>
  <p class="sub">Stairway to Heaven <b>v0.2.3</b> · __COUNT__ Assets · alle Größen in Original-Pixeln, hochskaliert ohne Glättung</p>
  <div class="bar">
    <button id="ground" type="button"><span class="dot"></span><span id="glabel">Untergrund: Stormslate</span></button>
    <nav>__CHIPS__</nav>
  </div>
</header>
<main>
<p class="intro" style="margin-top:16px">Jedes Sprite liegt auf dem Prüf-Untergrund aus dem Style-Guide:
dunkler Stormslate oder heller Cloudturf — auf beiden muss jedes Sprite lesbar sein. Mit dem Knopf oben wechselst du den Untergrund.</p>
__BODY__
</main>
<script>
(function(){
  var b=document.getElementById('ground'),l=document.getElementById('glabel'),cloud=false;
  try{cloud=localStorage.getItem('skyreach-ground')==='cloud';}catch(e){}
  function apply(){document.body.classList.toggle('cloudbg',cloud);
    l.textContent='Untergrund: '+(cloud?'Cloudturf':'Stormslate');}
  b.addEventListener('click',function(){cloud=!cloud;apply();
    try{localStorage.setItem('skyreach-ground',cloud?'cloud':'slate');}catch(e){}});
  apply();
})();
</script>
"""

if __name__ == "__main__":
    build()
