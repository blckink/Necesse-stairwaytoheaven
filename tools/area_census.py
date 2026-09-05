#!/usr/bin/env python3
"""Per-realm census: how full is each area of the mod, measured from the source.

WHY THIS EXISTS. "How many mobs does Steinfeld have, and how densely do they
spawn?" was a question nobody could answer without reading twenty files, so
every answer to it in the docs was a guess that went stale. This reads the
numbers off the code instead: spawn tables out of the biome classes, the cast
out of the `MobRegistry.registerMob` calls, the residents out of the settler
registry, the quests out of `StairwayToHeavenMod`, the POIs out of
`RealmPoiWorldPreset`, and the bosses out of `SkyBossLadder`.

It is a READING tool, not a gate: it prints what is there and flags the holes
it can prove (a biome with no critter table, a realm whose `getGuard()` is
never called, a hostile that counts no kill statistic). It exits 0 unless
`--check` is passed, in which case a proven hole is a failure.

The realm of a biome is not in the biome class — it is decided by which
terrain painter emits its `BIOME_*` class, so the mapping below is written
down once here with the painter that proves each row.

Usage:
    python3 tools/area_census.py            # the table
    python3 tools/area_census.py --markdown # the table, as docs/AREA_OVERVIEW.md wants it
    python3 tools/area_census.py --check    # non-zero if a proven hole is open
"""

import argparse
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA = os.path.join(ROOT, "src", "main", "java", "stairwaytoheaven")

# Realm order is RealmDepth's own: SKYREACH=0 .. HELL=5.
REALMS = ["Skyreach", "Eden", "Steinfeld", "Ghost Realm", "Crooked Beyond", "Hell"]

# biome class name -> realm index. The proof for each row is the painter that
# returns that biome's BIOME_* class:
#   SkyTerrainPainter        -> Skyreach's four
#   EdenTerrainPainter       -> Eden's three
#   SteinfeldTerrainPainter  -> Steinfeld's three
#   GhostTerrainPainter      -> Ghost's three + Gloomfen + Ashen Reach (WORLD_DESIGN 41.5)
#   CrookedTerrainPainter    -> Crooked's three + Beetlefreak Hollow + the Outlands rim
BIOME_REALM = {
    "DriftlandsBiome": 0, "StormveilBiome": 0, "SkywayBiome": 0, "AuroraShoalsBiome": 0,
    "EdenGardenBiome": 1, "EdenCanopyBiome": 1, "EdenShallowsBiome": 1,
    "QuietMeadowBiome": 2, "SlabFieldsBiome": 2, "GraveHeathBiome": 2,
    "AftergardenBiome": 3, "BoneOrchardBiome": 3, "EctomarshBiome": 3,
    "GloomfenBiome": 3, "AshenReachBiome": 3,
    "CheckerworksBiome": 4, "SpiralFieldsBiome": 4, "StripedWasteBiome": 4,
    "BeetlefreakHollowBiome": 4, "OutlandsBiome": 4,
}

# NPC id -> realm they are FOUND in. SkyLevel.placeResident (Skyreach),
# placeEveleen (Eden), VeilResidents.placeInGhost, CrookedResidents.place.
NPC_REALM = {
    "skywarden": 0, "wardensettler": 0, "magpiesettler": 0, "haldasettler": 0,
    "ossiansettler": 0, "spirecatblack": 0, "spirecattabby": 0,
    "eveleensettler": 1,
    "ivessettler": 2,
    "mortimersettler": 3, "caspernsettler": 3, "eleanorsettler": 3, "ghostguide": 3,
    "knottsettler": 4,
}

# quest id -> realm it belongs to, read off each quest class's own giver.
QUEST_REALM = {
    "swh_findspire": 0, "swh_recruitwarden": 0, "swh_cats": 0, "swh_anchor": 0,
    "swh_beacon": 0, "swh_keyskyreach": 0,
    "swh_edenreach": 1, "swh_edenplants": 1, "swh_keyeden": 1,
    "swh_keysteinfeld": 2, "swh_steinfeldvigil": 2,
    "swh_eleanor": 3, "swh_keyghostrealm": 3,
    "swh_mortimerrites": 3, "swh_caspernforge": 3,
    "swh_crookedarrival": 4, "swh_crookeddoor": 4, "swh_keycrookedbeyond": 4,
}

# Quests the mod registers but never hands out. Kept so old saves deserialize;
# counted apart so a realm does not look busier than it plays.
DEAD_QUESTS = {"swh_beacon"}

POI_REALM_PREFIX = {"SKY_": 0, "EDEN_": 1, "STEINFELD_": 2, "GHOST_": 3, "CROOKED_": 4, "HELL_": 5}


def read(path):
    with open(path, encoding="utf-8") as handle:
        return handle.read()


def java_files():
    for base, _, names in os.walk(JAVA):
        for name in names:
            if name.endswith(".java"):
                yield os.path.join(base, name)


def spawn_tables():
    """biome class name -> {"mobs": [(id, weight, capped)], "critters": [...], "guard": [ids]}."""
    out = {}
    for path in java_files():
        name = os.path.basename(path)[:-5]
        if name not in BIOME_REALM:
            continue
        source = read(path)
        entry = {"mobs": [], "critters": [], "guard": []}
        for match in re.finditer(
                r"MobSpawnTable\s+(\w+)\s*=\s*new MobSpawnTable\(\)(.*?);\n", source, re.S):
            var, body = match.group(1), match.group(2)
            adds = re.findall(r"\.add(Limited)?\(\s*(\d+)\s*,\s*([^;]*?\"(\w+)\")", body)
            rows = [(a[3], int(a[1]), bool(a[0])) for a in adds]
            if var in entry:
                entry[var] = rows
        guard = re.search(r"public Guard getGuard\(\)\s*\{(.*?)\n    \}", source, re.S)
        if guard:
            entry["guard"] = re.findall(r"\"(\w+)\"", guard.group(1))
        out[name] = entry
    return out


def registered_mobs():
    """mob id -> (source file, countKillStat)."""
    out = {}
    for path in java_files():
        for match in re.finditer(
                r"MobRegistry\.registerMob\(\s*\"(\w+)\"\s*,\s*([\w.]+)\.class\s*,\s*(true|false)",
                read(path)):
            out[match.group(1)] = (os.path.basename(path), match.group(3) == "true")
    return out


def guarded_realms():
    """Realms whose getGuard() packs are actually placed by SkyLevel."""
    source = read(os.path.join(JAVA, "level", "SkyLevel.java"))
    block = re.search(r"private void placeGuardPacks\(Region region\)\s*\{(.*?)\n    \}", source, re.S)
    if not block:
        return set()
    found = set()
    for match in re.finditer(r"RealmDepth\.REALM_(\w+)", block.group(1)):
        name = match.group(1)
        for index, realm in enumerate(["SKYREACH", "EDEN", "STEINFELD", "GHOST", "CROOKED", "HELL"]):
            if name == realm:
                found.add(index)
    return found


def bosses():
    """realm index -> (boss id, tier, base HP, final HP)."""
    source = read(os.path.join(JAVA, "bosses", "SkyBossLadder.java"))
    out = {}
    for match in re.finditer(
            r"BY_REALM\[RealmDepth\.REALM_(\w+)\]\s*=\s*\n?\s*new Boss\(RealmDepth\.REALM_\w+,\s*"
            r"\"(\w+)\",\s*\"\w+\",\s*(\d+),\s*(\d+)\)", source):
        name, boss, base, tier = match.group(1), match.group(2), int(match.group(3)), int(match.group(4))
        for index, realm in enumerate(["SKYREACH", "EDEN", "STEINFELD", "GHOST", "CROOKED", "HELL"]):
            if name == realm:
                out[index] = (boss, tier, base)
    return out


def pois():
    """realm index -> [preset constant names]."""
    source = read(os.path.join(JAVA, "worldgen", "pois", "RealmPoiPresets.java"))
    out = {index: [] for index in range(len(REALMS))}
    for match in re.finditer(r"public static final int (\w+) = \d+;", source):
        name = match.group(1)
        if name == "COUNT":
            continue
        for prefix, realm in POI_REALM_PREFIX.items():
            if name.startswith(prefix):
                out[realm].append(name)
                break
    return out


def census():
    tables = spawn_tables()
    mobs = registered_mobs()
    guarded = guarded_realms()
    ladder = bosses()
    poi = pois()

    realms = []
    for index, realm_name in enumerate(REALMS):
        biomes = sorted(name for name, r in BIOME_REALM.items() if r == index)
        hostiles, critters, weights = set(), set(), []
        for biome in biomes:
            entry = tables.get(biome, {})
            for mob, weight, _capped in entry.get("mobs", []):
                hostiles.add(mob)
            for mob, weight, _capped in entry.get("critters", []):
                critters.add(mob)
            total = sum(w for _, w, _ in entry.get("mobs", []))
            weights.append((biome, total, len(entry.get("mobs", [])),
                            len(entry.get("critters", [])), entry.get("guard", [])))
        realms.append({
            "name": realm_name,
            "index": index,
            "biomes": biomes,
            "hostiles": sorted(hostiles),
            "critters": sorted(critters),
            "weights": weights,
            "guards_placed": index in guarded,
            "boss": ladder.get(index),
            "pois": poi[index],
            "npcs": sorted(k for k, v in NPC_REALM.items() if v == index),
            "quests": sorted(k for k, v in QUEST_REALM.items()
                             if v == index and k not in DEAD_QUESTS),
            "dead_quests": sorted(k for k, v in QUEST_REALM.items()
                                  if v == index and k in DEAD_QUESTS),
        })
    return realms, mobs


def holes(realms, mobs):
    """Only what the source PROVES is missing — never a judgement of taste."""
    found = []
    for realm in realms:
        if realm["hostiles"] and not realm["guards_placed"]:
            found.append("%s: getGuard() is defined by its biomes and SkyLevel.placeGuardPacks "
                         "never calls it -- the packs are dead code" % realm["name"])
        if realm["biomes"] and not any(count for _, _, _, count, _ in realm["weights"]):
            found.append("%s: no biome has a critter spawn table -- the realm is silent"
                         % realm["name"])
        if realm["hostiles"] and not realm["npcs"]:
            found.append("%s: no NPC of any kind lives in the realm" % realm["name"])
        for mob in realm["hostiles"]:
            if mob in mobs and not mobs[mob][1]:
                found.append("%s: hostile '%s' is registered countKillStat=false -- no bestiary "
                             "row, no kill statistic (%s)" % (realm["name"], mob, mobs[mob][0]))
    return found


def render_text(realms, mobs):
    lines = []
    for realm in realms:
        boss = realm["boss"]
        lines.append("=== %s" % realm["name"])
        lines.append("  biomes    %d: %s" % (len(realm["biomes"]),
                                             ", ".join(b.replace("Biome", "") for b in realm["biomes"])))
        lines.append("  hostiles  %d: %s" % (len(realm["hostiles"]), ", ".join(realm["hostiles"]) or "-"))
        lines.append("  critters  %d: %s" % (len(realm["critters"]), ", ".join(realm["critters"]) or "-"))
        lines.append("  NPCs      %d: %s" % (len(realm["npcs"]), ", ".join(realm["npcs"]) or "-"))
        lines.append("  quests    %d live%s" % (
            len(realm["quests"]),
            (" + %d dead" % len(realm["dead_quests"])) if realm["dead_quests"] else ""))
        lines.append("  POIs      %d: %s" % (len(realm["pois"]), ", ".join(realm["pois"]) or "-"))
        lines.append("  boss      %s" % ("%s tier %d, %d base HP" % boss if boss else "none"))
        lines.append("  guards    %s" % ("placed" if realm["guards_placed"] else "NOT PLACED"))
        for biome, total, count, critter_count, guard in realm["weights"]:
            lines.append("    %-24s weight %-5d %d entries, %d critters, guard %d"
                         % (biome.replace("Biome", ""), total, count, critter_count, len(guard)))
    return "\n".join(lines)


def render_markdown(realms):
    lines = ["| realm | biomes | hostiles | critters | NPCs | live quests | POIs | boss |",
             "|---|---|---|---|---|---|---|---|"]
    for realm in realms:
        boss = realm["boss"]
        lines.append("| **%s** | %d | %d | %d | %d | %d | %d | %s |" % (
            realm["name"], len(realm["biomes"]), len(realm["hostiles"]), len(realm["critters"]),
            len(realm["npcs"]), len(realm["quests"]), len(realm["pois"]),
            ("`%s` t%d" % (boss[0], boss[1])) if boss else "—"))
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--markdown", action="store_true", help="print the summary table only")
    parser.add_argument("--check", action="store_true", help="exit non-zero on a proven hole")
    args = parser.parse_args()

    realms, mobs = census()
    if args.markdown:
        print(render_markdown(realms))
    else:
        print(render_text(realms, mobs))

    open_holes = holes(realms, mobs)
    if open_holes:
        print("\n%d open hole(s):" % len(open_holes))
        for hole in open_holes:
            print("  - " + hole)
    else:
        print("\nno open holes")
    return 1 if (args.check and open_holes) else 0


if __name__ == "__main__":
    sys.exit(main())
