"""Terrain tile strips (32 x 32*variants) and Mistsea liquid strips.

Terrain tiles use the legacy strip format: the engine cuts 32x32 variants from
top to bottom and auto-generates all blending/splatting (see
docs/research/asset-formats.md).
"""

from px import Canvas, Rng, mix
import palette


def _speckle(canvas, x0, y0, ramp, rng_salt, density=0.10, seed=1):
    """Deterministic per-pixel speckle, phase-locked so tiles repeat seamlessly."""
    for x in range(32):
        for y in range(32):
            r = Rng((x * 7349 + y * 12611) ^ rng_salt ^ (seed * 0x51ED))
            v = r.float()
            if v < density * 0.45:
                canvas.put(x0 + x, y0 + y, ramp["deep"])
            elif v < density:
                canvas.put(x0 + x, y0 + y, ramp["light"])


def _base_variant(canvas, y0, ramp, salt, variant):
    canvas.rect(0, y0, 32, 32, ramp["base"])
    _speckle(canvas, 0, y0, ramp, salt, seed=variant + 1)


def gen_cloudturf(path):
    ramp = palette.CLOUDTURF
    c = Canvas(32, 96)
    for v in range(3):
        y0 = v * 32
        _base_variant(c, y0, ramp, 0xC10D, v)
        rng = Rng(0xC10D + v * 977)
        # short grass tufts: 2-3 px strokes leaning slightly
        for _ in range(rng.range(5, 7)):
            x = rng.range(1, 30)
            y = y0 + rng.range(2, 29)
            lean = rng.pick((-1, 0, 1))
            c.put(x, y, ramp["tuft"])
            c.put(x + lean, y - 1, ramp["tuft"])
            if rng.chance(0.5):
                c.put(x + lean, y - 2, ramp["hi"])
        # a few bright cloud-motes
        for _ in range(rng.range(2, 4)):
            x = rng.range(2, 29)
            y = y0 + rng.range(2, 29)
            c.put(x, y, ramp["hi"])
            c.put(x + 1, y, ramp["light"])
    c.save(path)


def gen_skystone(path):
    ramp = palette.SKYSTONE
    c = Canvas(32, 96)
    for v in range(3):
        y0 = v * 32
        _base_variant(c, y0, ramp, 0x51A9, v)
        rng = Rng(0x51A9 + v * 977)
        # hairline cracks
        for _ in range(rng.range(2, 3)):
            x = rng.range(3, 28)
            y = y0 + rng.range(3, 28)
            length = rng.range(3, 6)
            step = rng.pick((-1, 1))
            for i in range(length):
                c.put(x + i, y + (i // 2) * step, ramp["deep"])
        # light chips
        for _ in range(rng.range(3, 5)):
            x = rng.range(2, 29)
            y = y0 + rng.range(2, 29)
            c.put(x, y, ramp["light"])
            c.put(x + 1, y + 1, ramp["hi"])
    c.save(path)


def gen_stormslate(path):
    ramp = palette.STORMSLATE
    c = Canvas(32, 96)
    for v in range(3):
        y0 = v * 32
        _base_variant(c, y0, ramp, 0x570A, v)
        rng = Rng(0x570A + v * 977)
        # diagonal fracture lines
        for _ in range(rng.range(2, 4)):
            x = rng.range(2, 24)
            y = y0 + rng.range(2, 24)
            length = rng.range(4, 8)
            for i in range(length):
                c.put(x + i, y + i, ramp["deep"])
        # faint static charge, very sparse
        if rng.chance(0.8):
            x = rng.range(4, 27)
            y = y0 + rng.range(4, 27)
            c.put(x, y, ramp["charge"])
    c.save(path)


def _mist_variant(canvas, y0, salt, variant):
    ramp = palette.MISTSEA
    canvas.rect(0, y0, 32, 32, ramp["base"])
    rng = Rng(salt + variant * 977)
    # soft horizontal drift streaks
    for _ in range(rng.range(4, 6)):
        x = rng.range(0, 20)
        y = y0 + rng.range(2, 29)
        length = rng.range(6, 14)
        tone = rng.pick((ramp["light"], ramp["hi"], ramp["deep"]))
        for i in range(length):
            canvas.put((x + i) % 32, y, tone)
    # curl dots
    for _ in range(rng.range(2, 3)):
        x = rng.range(2, 29)
        y = y0 + rng.range(2, 29)
        canvas.put(x, y, ramp["hi"])
        canvas.put(x - 1, y + 1, ramp["light"])


def gen_mistsea(shallow_path, deep_path):
    ramp = palette.MISTSEA
    shallow = Canvas(32, 64)
    for v in range(2):
        _mist_variant(shallow, v * 32, 0x315EA, v)
    shallow.save(shallow_path)

    deep = Canvas(32, 64)
    for v in range(2):
        _mist_variant(deep, v * 32, 0xD1EE9, v + 7)
        # deep mist is dimmer: overlay darker wash
        for x in range(32):
            for y in range(v * 32, v * 32 + 32):
                col = deep.get(x, y)
                deep.put(x, y, mix(col, ramp["deep"], 0.45))
    deep.save(deep_path)
