"""Skyreach color palettes.

Design rules (docs/DESIGN.md §8): muted bases, few saturated accents, soft dark
outlines (never pure black), warm light from the top-left. Ramps are ordered
dark -> base -> light -> highlight.
"""

# Global outline tone: warm, very dark gray-blue (Necesse avoids pure black)
OUTLINE = (34, 34, 46)

# --- Terrain -----------------------------------------------------------------

CLOUDTURF = {
    "deep":  (128, 143, 138),
    "base":  (170, 184, 176),
    "light": (196, 208, 199),
    "hi":    (222, 231, 222),
    "tuft":  (146, 168, 152),
}

SKYSTONE = {
    "deep":  (84, 92, 108),
    "base":  (116, 126, 143),
    "light": (144, 154, 170),
    "hi":    (176, 185, 199),
}

STORMSLATE = {
    "deep":  (48, 51, 66),
    "base":  (68, 72, 90),
    "light": (88, 93, 112),
    "hi":    (112, 118, 138),
    "charge": (150, 140, 220),
}

MISTSEA = {
    "deep":  (156, 170, 186),
    "base":  (185, 198, 211),
    "light": (206, 217, 227),
    "hi":    (228, 236, 242),
}

# --- Materials ---------------------------------------------------------------

AETHERIUM = {
    "deep":  (44, 116, 124),
    "base":  (86, 178, 186),
    "light": (136, 216, 220),
    "hi":    (198, 244, 243),
}

STORMCRYSTAL = {
    "deep":  (66, 54, 130),
    "base":  (104, 88, 190),
    "light": (146, 130, 226),
    "hi":    (206, 196, 255),
}

AURORA = {
    "deep":  (140, 62, 104),
    "base":  (198, 106, 152),
    "light": (232, 154, 188),
    "hi":    (255, 212, 227),
    "teal":  (108, 196, 186),
}

WINDSILK = {
    "deep":  (150, 160, 172),
    "base":  (196, 206, 216),
    "light": (224, 231, 238),
    "hi":    (246, 249, 252),
}

GOLEM = {
    "deep":  (74, 82, 98),
    "base":  (108, 118, 135),
    "light": (140, 150, 166),
    "hi":    (172, 181, 196),
    "moss":  (122, 148, 132),
    "eye":   (136, 216, 220),
}

ZEPHYR = {
    "deep":  (96, 112, 128),
    "base":  (156, 174, 189),
    "light": (198, 212, 223),
    "hi":    (232, 240, 246),
    "belly": (226, 232, 238),
    "accent": (108, 196, 186),
}

WISP = {
    "core":  (240, 238, 255),
    "inner": (170, 156, 240),
    "base":  (116, 100, 205),
    "deep":  (74, 62, 148),
    "spark": (255, 250, 210),
}

WOOD = {
    "deep":  (86, 66, 50),
    "base":  (122, 96, 72),
    "light": (152, 124, 94),
}

STAIRLIGHT = {
    "deep":  (168, 178, 200),
    "base":  (206, 214, 230),
    "light": (232, 238, 248),
    "hi":    (250, 252, 255),
    "glow":  (186, 226, 230),
}


# --- v0.2: Warden, cats, Nightfell building set -----------------------------

NIGHTFELL = {
    "deep":  (30, 27, 41),
    "base":  (48, 44, 63),
    "light": (68, 63, 86),
    "hi":    (94, 88, 114),
}

WARDEN = {
    "coat_deep":  (36, 32, 50),
    "coat":       (54, 49, 72),
    "coat_light": (76, 70, 96),
    "feather":    (98, 92, 118),
    "skin":       (196, 186, 172),
    "skin_shade": (162, 150, 138),
    "hair":       (222, 224, 228),
    "eye":        (108, 196, 186),
    "staff":      (86, 66, 50),
    "lanternglow": (186, 226, 230),
}

CAT_BLACK = {
    "deep":  (16, 15, 20),
    "base":  (28, 27, 34),
    "light": (44, 43, 52),
    "eye":   (232, 176, 64),
    "nose":  (120, 90, 100),
}

CAT_TABBY = {
    "white":  (238, 236, 230),
    "shade":  (204, 200, 192),
    "tabby":  (196, 128, 66),
    "tabby_dark": (150, 92, 46),
    "eye":    (120, 186, 120),
    "nose":   (222, 150, 150),
}

MARBLE_DARK = (40, 38, 46)
MARBLE_LIGHT = (222, 220, 226)

GLOOMWOOD = {
    "deep":  (43, 34, 41),
    "base":  (66, 52, 60),
    "light": (88, 72, 78),
    "hi":    (110, 92, 96),
}

IRONWORK = {
    "deep":  (38, 40, 50),
    "base":  (62, 66, 80),
    "light": (92, 97, 112),
    "hi":    (128, 134, 150),
}

GARLAND_LIGHTS = [(235, 106, 106), (240, 200, 110), (120, 205, 130), (120, 160, 235), (200, 130, 220)]
