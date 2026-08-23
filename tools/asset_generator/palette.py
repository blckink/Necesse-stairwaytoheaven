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
    # night-violet slate — the mod's gothic art direction: a purple night sky
    # made walkable. Same luminance ladder as before, hue shifted to violet.
    "deep":  (47, 42, 70),
    "base":  (66, 60, 95),
    "light": (89, 82, 122),
    "hi":    (115, 108, 150),
    "charge": (168, 150, 240),
}

MISTSEA = {
    "deep":  (156, 170, 186),
    "base":  (185, 198, 211),
    "light": (206, 217, 227),
    "hi":    (228, 236, 242),
    # sunlit cloud tops for the fluffy cloud-deck look (v0.2.3 mist rework)
    "top":   (247, 250, 253),
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
    # coat ramp lifted well above the outline color (34,34,46) — the old ramp
    # started at 36 luminance and the whole figure melted into a black cone
    "coat_deep":  (52, 47, 76),
    "coat":       (74, 68, 102),
    "coat_light": (100, 93, 132),
    "coat_hi":    (126, 119, 160),
    "feather":    (152, 148, 174),
    "feather_hi": (196, 194, 212),
    "skin":       (203, 192, 176),
    "skin_shade": (168, 155, 141),
    "hair":       (226, 228, 233),
    "hair_shade": (178, 182, 196),
    "eye":        (108, 196, 186),
    "staff":      (110, 86, 62),
    "staff_hi":   (148, 118, 86),
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


# --- v0.2.6: forage plants + critters ----------------------------------------

WINDWHEAT = {
    "deep":  (118, 124, 96),
    "base":  (168, 168, 120),
    "light": (204, 198, 148),
    "head":  (226, 214, 164),
}

CLOUDBERRY = {
    "berry":      (232, 186, 120),
    "berry_hi":   (250, 228, 186),
    "berry_deep": (188, 138, 84),
    "leaf":       (146, 168, 152),
    "leaf_deep":  (110, 132, 118),
}

LAMB = {
    "wool":       (236, 240, 246),
    "wool_shade": (204, 212, 224),
    "face":       (150, 150, 162),
    "face_dark":  (108, 110, 124),
}

MOTH = {
    "wing":       (240, 234, 242),
    "wing_shade": (206, 198, 216),
    "spot":       (108, 196, 186),
    "body":       (122, 112, 132),
}

BEETLE = {
    "shell":       (86, 78, 120),
    "shell_light": (120, 112, 158),
    "shell_deep":  (58, 52, 88),
    "charge":      (168, 150, 240),
}


# --- v0.3: The Veil -----------------------------------------------------------

MURKMOSS = {
    "deep":  (38, 46, 40),
    "base":  (56, 68, 58),
    "light": (76, 90, 76),
    "hi":    (98, 114, 96),
    "tuft":  (88, 118, 94),
}

BLACKPEAT = {
    "deep":  (26, 22, 28),
    "base":  (42, 36, 44),
    "light": (58, 50, 60),
    "hi":    (76, 66, 78),
}

ASHSAND = {
    "deep":  (72, 68, 68),
    "base":  (98, 94, 92),
    "light": (122, 118, 114),
    "hi":    (148, 143, 138),
}

MURKWATER = {
    "deep":  (22, 28, 30),
    "base":  (34, 44, 46),
    "light": (50, 64, 62),
    "hi":    (70, 92, 84),
    "glint": (122, 196, 160),
}

VEILROCK = {
    "deep":  (44, 40, 58),
    "base":  (66, 62, 82),
    "light": (88, 84, 106),
    "hi":    (112, 108, 132),
}

GHOSTFLAME = {
    "core":  (214, 255, 228),
    "glow":  (122, 214, 164),
    "deep":  (58, 128, 96),
}

SHADE = {
    "deep":  (30, 30, 44),
    "base":  (46, 46, 66),
    "light": (66, 66, 92),
    "hi":    (90, 90, 120),
    "eye":   (150, 235, 190),
}

BONEASH = {
    "deep":  (118, 112, 104),
    "base":  (164, 158, 148),
    "light": (198, 192, 180),
    "hi":    (226, 220, 208),
}
