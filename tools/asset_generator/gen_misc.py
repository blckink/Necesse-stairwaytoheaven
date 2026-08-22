"""Mod preview image (268x268, matches the template's preview.png size)."""

from px import Canvas, Rng, with_alpha, mix
import palette


def gen_preview(path):
    W = H = 268
    c = Canvas(W, H)
    sky_top = (52, 66, 96)
    sky_mid = (96, 118, 150)
    sky_low = (176, 192, 208)
    # vertical sky gradient, banded like pixel art (16px bands)
    bands = H // 16
    for b in range(bands + 1):
        t = b / bands
        col = mix(sky_top, sky_mid, min(1.0, t * 1.6)) if t < 0.62 else mix(sky_mid, sky_low, (t - 0.62) / 0.38)
        c.rect(0, b * 16, W, 16, col)
    rng = Rng(0x9E77)
    # stars in the upper sky
    for _ in range(40):
        x = rng.range(4, W - 5)
        y = rng.range(4, 110)
        c.put(x, y, (232, 238, 250))
        if rng.chance(0.25):
            c.put(x + 1, y, (180, 194, 218))
    # mist sea at the bottom
    mist = palette.MISTSEA
    c.rect(0, 212, W, 56, mist["base"])
    for _ in range(60):
        x = rng.range(0, W - 20)
        y = rng.range(214, 262)
        ln = rng.range(8, 22)
        tone = rng.pick((mist["light"], mist["hi"], mist["deep"]))
        for i in range(ln):
            c.put(x + i, y, tone)
    # floating islands
    def island(cx, cy, w, hh):
        turf = palette.CLOUDTURF
        stone = palette.SKYSTONE
        c.ellipse(cx, cy, w, hh, turf["base"])
        c.ellipse(cx - w * 0.2, cy - 1, w * 0.6, hh * 0.6, turf["light"])
        # rocky underside taper
        for i in range(int(hh * 2.2)):
            ww = max(2, w * (1.0 - i / (hh * 2.4)))
            c.ellipse(cx + (i % 3) - 1, cy + hh * 0.4 + i, ww, 1.6, stone["deep"] if i > hh else stone["base"])
    island(70, 150, 42, 12)
    island(196, 120, 34, 10)
    island(150, 196, 30, 9)
    # the stairway of light rising through the middle
    ramp = palette.STAIRLIGHT
    x, y = 128, 236
    for i in range(14):
        wstep = 22 - i
        for dx in range(wstep):
            c.put(x + dx, y, ramp["base"])
            c.put(x + dx, y - 1, ramp["light"])
        c.put(x, y, palette.OUTLINE)
        c.put(x + wstep - 1, y, palette.OUTLINE)
        if i % 2 == 0:
            c.put(x + wstep // 2, y - 3, ramp["glow"])
        x += (-1) ** i * 3 + 2
        y -= 15
    # glow at the vanishing point
    for rad, alpha in ((26, 40), (18, 70), (10, 110)):
        for px_ in range(-rad, rad + 1):
            for py_ in range(-rad, rad + 1):
                if px_ * px_ + py_ * py_ <= rad * rad:
                    old = c.get(min(max(x + px_, 0), W - 1), min(max(y + py_, 0), H - 1))
                    c.put(x + px_, y + py_, mix(old[:3], ramp["hi"], alpha / 255.0))
    # zephyr ray silhouettes
    for (rx, ry, s) in ((60, 90, 1.0), (210, 70, 0.7), (180, 170, 0.8)):
        z = palette.ZEPHYR
        c.ellipse(rx, ry, 10 * s, 3 * s, z["deep"])
        c.ellipse(rx, ry - 1, 4 * s, 2 * s, z["base"])
    c.save(path)
