"""Tiny pixel-art toolkit used by the Stairway to Heaven asset generator.

Style contract (matches vanilla Necesse pixel language, see
docs/assets-style-guide.md):
- 32 px tile grid, sprites drawn at 1:1 pixel scale
- soft dark outline on silhouettes (darkest ramp step, never pure black)
- 2-4 flat shade steps per material, light from the top-left
- sparse single-pixel dithering only at shade borders

Everything is deterministic: every drawing helper takes an explicit `Rng`.
"""

from PIL import Image


class Rng:
    """Tiny deterministic PRNG (xorshift*), independent of Python's seeding."""

    def __init__(self, seed):
        self.state = (seed ^ 0x9E3779B97F4A7C15) & 0xFFFFFFFFFFFFFFFF
        if self.state == 0:
            self.state = 0x2545F4914F6CDD1D

    def next(self):
        x = self.state
        x ^= (x >> 12) & 0xFFFFFFFFFFFFFFFF
        x ^= (x << 25) & 0xFFFFFFFFFFFFFFFF
        x ^= (x >> 27) & 0xFFFFFFFFFFFFFFFF
        self.state = x & 0xFFFFFFFFFFFFFFFF
        return (x * 0x2545F4914F6CDD1D) & 0xFFFFFFFFFFFFFFFF

    def float(self):
        return (self.next() >> 40) / float(1 << 24)

    def range(self, a, b):
        """Integer in [a, b] inclusive."""
        return a + self.next() % (b - a + 1)

    def chance(self, p):
        return self.float() < p

    def pick(self, seq):
        return seq[self.next() % len(seq)]


class Canvas:
    """RGBA canvas with pixel-art helpers."""

    def __init__(self, width, height):
        self.img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        self.px = self.img.load()
        self.width = width
        self.height = height

    # ----- primitives -----

    def put(self, x, y, color):
        if 0 <= x < self.width and 0 <= y < self.height:
            self.px[x, y] = color if len(color) == 4 else (*color, 255)

    def get(self, x, y):
        if 0 <= x < self.width and 0 <= y < self.height:
            return self.px[x, y]
        return (0, 0, 0, 0)

    def filled(self, x, y):
        return self.get(x, y)[3] > 0

    def rect(self, x, y, w, h, color):
        for i in range(x, x + w):
            for j in range(y, y + h):
                self.put(i, j, color)

    def ellipse(self, cx, cy, rx, ry, color):
        for i in range(int(cx - rx), int(cx + rx) + 1):
            for j in range(int(cy - ry), int(cy + ry) + 1):
                dx = (i - cx) / max(rx, 0.001)
                dy = (j - cy) / max(ry, 0.001)
                if dx * dx + dy * dy <= 1.0:
                    self.put(i, j, color)

    def line(self, x0, y0, x1, y1, color):
        dx = abs(x1 - x0)
        dy = -abs(y1 - y0)
        sx = 1 if x0 < x1 else -1
        sy = 1 if y0 < y1 else -1
        err = dx + dy
        while True:
            self.put(x0, y0, color)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x0 += sx
            if e2 <= dx:
                err += dx
                y0 += sy

    def blob(self, cx, cy, r, color, rng, lumps=4):
        """Organic roundish shape: a core ellipse plus a few jittered lumps."""
        self.ellipse(cx, cy, r, r * 0.85, color)
        for _ in range(lumps):
            ox = rng.range(-int(r * 0.7), int(r * 0.7))
            oy = rng.range(-int(r * 0.5), int(r * 0.5))
            lr = max(1.0, r * (0.35 + rng.float() * 0.35))
            self.ellipse(cx + ox, cy + oy, lr, lr * 0.8, color)

    # ----- style passes -----

    def outline(self, color, inside=True):
        """Soft dark outline around every filled region (silhouette only)."""
        edges = []
        for x in range(self.width):
            for y in range(self.height):
                if inside:
                    if not self.filled(x, y):
                        continue
                    for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                        if not self.filled(nx, ny):
                            edges.append((x, y))
                            break
                else:
                    if self.filled(x, y):
                        continue
                    for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                        if self.filled(nx, ny):
                            edges.append((x, y))
                            break
        for x, y in edges:
            self.put(x, y, color)

    def shade_topleft(self, light, dark, strength=1.0):
        """Cheap flat shading: brighten top-left rim pixels, darken bottom-right."""
        for x in range(self.width):
            for y in range(self.height):
                if not self.filled(x, y):
                    continue
                up_free = not self.filled(x, y - 1)
                left_free = not self.filled(x - 1, y)
                down_free = not self.filled(x, y + 1)
                right_free = not self.filled(x + 1, y)
                if (up_free or left_free) and not (down_free or right_free):
                    self.put(x, y, light)
                elif (down_free or right_free) and not (up_free or left_free):
                    self.put(x, y, dark)

    def dither_border(self, color_a, color_b, rng, chance=0.4):
        """Sparse checker dithering where color_a touches color_b."""
        swaps = []
        for x in range(self.width):
            for y in range(self.height):
                if self.get(x, y)[:3] != color_a[:3]:
                    continue
                touches = any(self.get(nx, ny)[:3] == color_b[:3]
                              for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)))
                if touches and (x + y) % 2 == 0 and rng.chance(chance):
                    swaps.append((x, y))
        for x, y in swaps:
            self.put(x, y, color_b)

    # ----- composition -----

    def paste(self, other, x, y):
        self.img.alpha_composite(other.img, (x, y))
        self.px = self.img.load()

    def mirrored(self):
        out = Canvas(self.width, self.height)
        out.img = self.img.transpose(Image.FLIP_LEFT_RIGHT)
        out.px = out.img.load()
        return out

    def save(self, path):
        self.img.save(path)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def with_alpha(color, alpha):
    return (color[0], color[1], color[2], alpha)
