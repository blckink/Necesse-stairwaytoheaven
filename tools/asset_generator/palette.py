"""Skyreach color palettes.

Design rules (docs/DESIGN.md §8): muted bases, few saturated accents, soft dark
outlines (never pure black), warm light from the top-left. Ramps are ordered
dark -> base -> light -> highlight.
"""

# Global outline tone: warm, very dark gray-blue (Necesse avoids pure black)
OUTLINE = (34, 34, 46)

# --- Terrain -----------------------------------------------------------------

CLOUDTURF = {
    # v0.5 art sprint: chroma pushed further toward living silver-green so the
    # Driftlands floor reads as MEADOW against the pale Mistsea at 1x zoom
    # (measured sat was 0.14 vs vanilla grass ~0.9 on its full cells).
    "deep":  (104, 138, 108),
    "base":  (148, 184, 144),
    "light": (180, 210, 170),
    "hi":    (212, 233, 198),
    "tuft":  (110, 152, 112),
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
    # v0.4.1 quality pass: deeper plate shadows for readable boulder separation,
    # greener moss, brighter eye glow.
    "deep":  (54, 62, 80),
    "base":  (104, 114, 132),
    "light": (144, 154, 170),
    "hi":    (184, 193, 206),
    "moss":  (106, 160, 122),
    "eye":   (116, 236, 226),
}

ZEPHYR = {
    # v0.4.1 quality pass: back ramp deepened ~2 steps — the old base sat at
    # the Mistsea's luminance and the ray vanished against open cloud; the
    # belly stays pale for counter-shading, the accent teal is strengthened.
    "deep":  (58, 78, 100),
    "base":  (120, 142, 162),
    "light": (176, 196, 210),
    "hi":    (226, 238, 244),
    "belly": (230, 236, 240),
    "accent": (62, 210, 190),
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
    # v0.6 identity pass: the coat ramp moved from lavender to STORM-BLUE
    # (the Skywatch livery the settler pins in WardenSettlerMob), so the
    # Warden stops reading as a generic purple wizard. Trim stays the single
    # warm brass accent; "patch" is the pale weathered mend tone.
    "coat_deep":  (48, 52, 76),
    "coat":       (68, 76, 104),
    "coat_light": (94, 104, 136),
    "coat_hi":    (120, 132, 166),
    "patch":      (134, 142, 160),
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
    # v0.4.1 quality pass: warm gold trim — one warm accent so the warden
    # stops reading as a grey-lavender mass among the grey mobs.
    "trim":       (204, 160, 82),
    "trim_hi":    (238, 202, 124),
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
    # v0.6 readability redesign: the old sage-grey leaf tone helped the bush
    # read as pebbles; the ramp now sits in the Driftlands green family with
    # a lit step, and the berries stay amber for contrast against it.
    "berry":      (232, 186, 120),
    "berry_hi":   (250, 228, 186),
    "berry_deep": (188, 138, 84),
    "leaf":       (110, 152, 106),
    "leaf_light": (150, 190, 134),
    "leaf_deep":  (76, 114, 80),
    "wood":       (86, 66, 50),
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


# --- v0.4 "The Living Sky" fill -----------------------------------------------

NIMBUSWOOD = {
    "deep":  (108, 96, 88),
    "base":  (150, 136, 124),
    "light": (186, 172, 158),
    "hi":    (214, 202, 188),
}

NIMBUSLEAF = {
    # v0.5 art sprint: the old "pale blue-grey leaf" ramp measured sat 0.11 —
    # the willow read as fog, not foliage. Now a true silver-green (sat ~0.30,
    # vanilla willow-cell reference 0.37) while staying cool-toned.
    "deep":  (92, 132, 106),
    "base":  (138, 178, 142),
    "light": (176, 208, 172),
    "hi":    (216, 236, 204),
}

CHARWOOD = {
    "deep":  (38, 36, 46),
    "base":  (62, 58, 72),
    "light": (88, 82, 98),
    "hi":    (118, 110, 128),
    "ember": (255, 176, 92),
}

FULGURPINE_NEEDLE = {
    # v0.5 art sprint: measured sat 0.27 vs vanilla pine-cell 0.72 — the pine
    # read as grey scrub. Now a cold electric violet-blue ramp (Stormveil
    # identity) with bright charge-tipped highlights.
    "deep":  (48, 54, 96),
    "base":  (74, 84, 140),
    "light": (110, 124, 184),
    "hi":    (176, 200, 244),
}

PRISMWOOD = {
    "deep":  (168, 152, 170),
    "base":  (210, 196, 210),
    "light": (234, 224, 235),
    "hi":    (250, 245, 250),
}

PRISMLEAF = {
    "deep":  (148, 120, 168),
    "base":  (188, 158, 204),
    "light": (222, 196, 228),
    "hi":    (246, 228, 244),
    "teal":  (128, 208, 198),
    "rose":  (238, 160, 190),
}

CLOUDBELL = {
    "deep":  (74, 96, 158),
    "base":  (112, 138, 204),
    "light": (156, 180, 232),
    "hi":    (206, 220, 248),
}

SKYTULIP = {
    "rose":  (226, 130, 162),
    "gold":  (232, 190, 110),
    "white": (238, 240, 244),
    "stem":  (122, 150, 122),
    "stem_deep": (88, 112, 92),
}

STATICMOSS = {
    "deep":  (58, 74, 84),
    "base":  (86, 108, 116),
    "light": (120, 146, 148),
    "spark": (196, 232, 236),
}

THUNDERBLOOM = {
    "deep":  (98, 78, 148),
    "base":  (140, 116, 198),
    "light": (178, 156, 226),
    "spark": (255, 250, 210),
    "stem":  (74, 84, 94),
}

GLOWFERN = {
    "deep":  (70, 128, 118),
    "base":  (104, 172, 156),
    "light": (150, 212, 192),
    "hi":    (208, 246, 228),
}

AURORALILY = {
    "deep":  (170, 108, 150),
    "base":  (214, 150, 190),
    "light": (240, 192, 220),
    "core":  (255, 240, 250),
    "stem":  (110, 140, 128),
}

FULGURITE = {
    "deep":  (120, 96, 60),
    "base":  (176, 146, 96),
    "light": (222, 196, 140),
    "hi":    (255, 240, 196),
}

PRISMSHARD = {
    "deep":  (140, 110, 170),
    "base":  (186, 156, 214),
    "light": (224, 200, 240),
    "hi":    (250, 240, 255),
    "teal":  (136, 216, 206),
}

FINCH = {
    "deep":  (96, 118, 152),
    "base":  (140, 164, 198),
    "light": (186, 206, 230),
    "belly": (238, 230, 208),
    "beak":  (232, 186, 100),
}

GALEHOUND = {
    # v0.4.1 quality pass: darker storm back / brighter highlights for value
    # contrast, stronger eye glow, new "wind" cyan for detached mane flecks.
    "deep":  (58, 70, 92),
    "base":  (114, 128, 150),
    "light": (166, 182, 200),
    "hi":    (216, 228, 238),
    "eye":   (108, 242, 228),
    "wind":  (148, 234, 222),
}

DEWSNAIL = {
    "deep":  (98, 140, 132),
    "base":  (142, 186, 172),
    "light": (188, 224, 206),
    "shell": (222, 206, 170),
    "shell_deep": (170, 150, 116),
    "glow":  (232, 255, 240),
}

DAWNPIERCER = {
    "deep":  (168, 120, 96),
    "base":  (214, 164, 128),
    "light": (240, 204, 162),
    "crystal": (140, 220, 226),
    "crystal_deep": (86, 164, 176),
    "beak":  (94, 86, 104),
}
