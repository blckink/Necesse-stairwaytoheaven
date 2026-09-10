package stairwaytoheaven.realms.hell;

import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.TileRegistry;
import stairwaytoheaven.realms.hell.mobs.AshSpiritMob;
import stairwaytoheaven.realms.hell.mobs.BoilerHoundMob;
import stairwaytoheaven.realms.hell.mobs.InfernalClerkMob;
import stairwaytoheaven.realms.hell.mobs.TicketImpMob;

/**
 * Hell — Tier 5 of {@code docs/WORLD_DESIGN.md} §17-23, and everything it
 * registers, in one place.
 *
 * <h2>What was missing, and what this fixes</h2>
 * Hell has been a real realm band since {@code RealmDepth} was written — depth
 * 0.80-1.00, its own waterline, its own four POIs, its own residents — and the
 * only band with <b>no cast of its own</b>. The CHANGELOG's boss-portal
 * section records the same finding from the other side: Hell was the one realm
 * {@code SkyBossLadder} answered {@code null} for. This class is the glue that
 * ends both: two biomes with a roster, four hostiles on the rung past Crooked
 * Beyond, and a painter to put them on ground that is Hell's rather than
 * Crooked's.
 *
 * <h2>Where the actual content lives</h2>
 * This class only REGISTERS. The generation itself is
 * {@link HellTerrainPainter} (the two bands, the transition ratio, the ground
 * mix and the prop scatter), and the ladder rung is
 * {@link stairwaytoheaven.bosses.SkyBossLadder} plus
 * {@link stairwaytoheaven.bosses.BossPortalObject} and
 * {@link stairwaytoheaven.objects.RegionKeyObject}, which now carry a sixth
 * row each.
 *
 * <h2>Wiring</h2>
 * <pre>
 *   StairwayToHeavenMod.init()          -&gt; HellRealm.register()
 *   StairwayToHeavenMod.initResources() -&gt; HellRealm.loadTextures()
 * </pre>
 *
 * <h2>String IDs are literals at every registration, on purpose</h2>
 * {@code tools/locale_audit.py} finds registered IDs by matching a quoted
 * first argument to the registry calls, and {@code tools/content_ledger.py}
 * reuses that scan. An ID hidden behind a constant is an ID neither gate can
 * name-check, so every call below spells its string out.
 *
 * <h2>Two grounds, no objects, no items</h2>
 * The realm owns exactly two sheets: {@code cinderash} and {@code furnaceslag},
 * the pale and the dark half of its floor. That is the whole of {@code
 * docs/STATUS.md} 7n, the pass 6n deferred when it let Hell paint the Veil's
 * {@code ashsand} and the Gloomfen's {@code blackpeat} as stand-ins. Every
 * PROP Hell scatters is still an object the mod already registers, reached by
 * its existing ID field (see {@link HellTerrainPainter}'s own header), and
 * nothing here needs an inventory icon, because neither ground is obtainable.
 * What Hell is still missing is therefore NAMED rather than faked:
 *
 * <ul>
 * <li><b>§20's six materials</b> — Hellsteel Ore, Brimstone, Infernal Brass,
 *     Demon Hide, Hellglass, Furnace Heart. Hell's loot is Crooked Beyond's
 *     currency in Hell's quantities until they exist; see
 *     {@link HellBiome#getCrateLootTable}.</li>
 * <li><b>§19's three friendly demons</b> (Brim, Moxie, Vex) and §18's Clerk
 *     666-B. Hell's residents are still {@code CrookedResidents}', which
 *     {@code SkyLevel.placeRealmResidents} places in both bands.</li>
 * <li><b>§21's four machines</b> and §23's crops, both of which are economy
 *     work rather than area work.</li>
 * <li><b>§25's Auditor.</b> The realm's boss is vanilla's Mutant Hydra, which
 *     is what {@code docs/FOGKEY_AND_BOSSPORTALS.md} §B4 reserved for Hell
 *     from the start; the Auditor is a bespoke four-phase fight and is its own
 *     piece of work.</li>
 * </ul>
 */
public final class HellRealm {

    private HellRealm() {
    }

    // ===== The two bands =====

    /** §17's transition: still Crooked Beyond, with the first hell elements. */
    public static InfernalFringeBiome infernalFringe;
    /** A3.8's Furnace: black ground at the far edge of the plane. */
    public static FurnaceReachBiome furnaceReach;

    // ===== The two grounds =====

    /** Burnt grit and cinder dust — the pale half of Hell's floor. */
    public static int cinderAshID;
    /** Cooled slag crust with the embers showing — the dark half. */
    public static int furnaceSlagID;

    /**
     * Everything the realm puts into the registries.
     *
     * <p>Biomes and tiles before mobs, the order every other realm uses,
     * because the terrain painter needs both sets of IDs and the registry
     * closes at the end of the {@code init()} loop.
     */
    public static void register() {
        registerBiomes();
        registerTiles();
        registerMobs();
    }

    /**
     * The two bands. {@code countInStats = false}, the flag vanilla uses for
     * every biome that is not a surface island: these are painted into the sky
     * level's own biome layer by {@link HellTerrainPainter} and must never turn
     * up in the world's surface-biome statistics.
     */
    public static void registerBiomes() {
        infernalFringe = BiomeRegistry.registerBiome(
                "infernalfringe", new InfernalFringeBiome(), false);
        furnaceReach = BiomeRegistry.registerBiome(
                "furnacereach", new FurnaceReachBiome(), false);
    }

    /**
     * The two grounds §17 and A3.8 describe, and the only sheets this realm
     * owns.
     *
     * <p>Registered the way Crooked Beyond registers its six and the way
     * vanilla registers {@code spidernesttile}: {@code brokerValue 0}, not
     * obtainable, not obtainable in creative, on the terrain tile layer. Hell
     * has no floor the player crafts — §21's machines and §23's crops are the
     * economy pass, and it has not happened — so an obtainable ground would put
     * two tiles in a building menu with no recipe behind them.
     *
     * <p>They take over exactly the share {@code ashsand} and {@code blackpeat}
     * held in {@link HellTerrainPainter#groundAt}; those two are the Veil's and
     * the Gloomfen's and also floor the Ghost band, Crooked Beyond and the
     * Outlands, so they are left untouched rather than repainted.
     */
    public static void registerTiles() {
        cinderAshID = TileRegistry.registerTile("cinderashtile",
                new stairwaytoheaven.realms.hell.tiles.CinderAshTile(),
                0.0F, false, false, true);
        furnaceSlagID = TileRegistry.registerTile("furnaceslagtile",
                new stairwaytoheaven.realms.hell.tiles.FurnaceSlagTile(),
                0.0F, false, false, true);
    }

    /**
     * The realm's four residents — §17's own list, one of each spawn-table
     * role (see {@link stairwaytoheaven.realms.hell.mobs.HellTier}). Kill
     * statistics ON for all four, the mod's convention for every hostile —
     * nothing here is a critter.
     */
    private static void registerMobs() {
        MobRegistry.registerMob("infernalclerk", InfernalClerkMob.class, true);
        MobRegistry.registerMob("ashspirit", AshSpiritMob.class, true);
        MobRegistry.registerMob("ticketimp", TicketImpMob.class, true);
        MobRegistry.registerMob("boilerhound", BoilerHoundMob.class, true);
    }

    /**
     * Client-side texture loading. Never called on a dedicated server.
     *
     * <p>Empty on purpose, exactly as {@code SteinfeldRealm.loadTextures} is:
     * all four mobs subclass a real vanilla hostile body and inherit that
     * body's own texture handling unchanged, and the realm registers no tile,
     * object or item that would carry a sheet of its own.
     */
    public static void loadTextures() {
    }
}
