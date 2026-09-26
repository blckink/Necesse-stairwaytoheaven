package stairwaytoheaven.journal;

import java.util.ArrayList;
import java.util.List;

import necesse.engine.journal.CraftItemJournalChallenge;
import necesse.engine.journal.DefeatMobJournalChallenge;
import necesse.engine.journal.ItemObtainedJournalChallenge;
import necesse.engine.journal.JournalChallenge;
import necesse.engine.journal.JournalEntry;
import necesse.engine.journal.MobsKilledJournalChallenge;
import necesse.engine.journal.MultiJournalChallenge;
import necesse.engine.journal.ObjectsPlacedJournalChallenge;
import necesse.engine.journal.PickupItemsJournalChallenge;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.JournalChallengeRegistry;
import necesse.engine.registries.JournalRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.biomes.Biome;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.worldgen.pois.RealmPoiHoards;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets;

/**
 * The mod's regions in vanilla's Adventure Journal (Abenteuertagebuch).
 *
 * <p>The player, 2026-09-26: <i>"unsere Gebiete fehlen in
 * Abenteurertagebuch, da trägt vanilla Herausforderungen ein für jedes Gebiet
 * und Bäume Blumen und Fische etc."</i> Vanilla keeps one
 * {@link JournalEntry} per biome and level (JournalRegistry.registerCore,
 * VERIFIED [jar] 1.3.3): what grows and can be mined there, who lives there
 * with their drops, the treasure tables, and one set of three challenges with
 * a reward. This is the same shape for each of the 22 sky biomes, registered
 * from the mod's init — which runs after every core registry and before they
 * close (GlobalData, VERIFIED [jar]).
 *
 * <h2>How an entry is found</h2>
 * {@code ServerClient.tickDiscoveredBiomes} reads the biome of the player's
 * tile from the region's biome layer, which {@code SkyTerrainPainter} fills
 * per tile, and discovers every entry registered for that biome whose level
 * identifier matches. So each entry names the sky plane
 * ({@link SkyRegistry#SKYREACH_IDENTIFIER}); the biome-only constructor would
 * mean the surface.
 *
 * <h2>Multiplayer</h2>
 * Vanilla's own model: discovery, challenge progress and the claimed reward
 * are character stats, so every player finds, works and claims each entry for
 * themself. Nothing here writes a world record.
 *
 * <h2>No fish</h2>
 * The sky has no fishing water (the Mistsea and the realm liquids are
 * {@code LiquidTile}, not {@code WaterTile}), so no entry lists a fish: an
 * entry promising one would be a lie.
 *
 * <p>Every ID is checked against the registries before it is used, and a
 * missing one is left out and counted in the start-up line
 * {@code swhjournal vanilla:} rather than shipped as a challenge nobody can
 * finish.
 */
public final class RealmJournalEntries {

    private static final List<String> MISSING = new ArrayList<>();
    private static int entries;
    private static int challenges;

    private RealmJournalEntries() {
    }

    public static void register() {
        // ---------------------------------------------------------------- Skyreach
        entry("driftlands",
                items("nimbuswood", "windwheat", "cloudberry", "cloudbell", "skytulip", "skystone", "aetheriumore"),
                mobs("zephyrfinch", "zephyrray", "galehound", "skystonegolem", "mistserpent",
                        "sourvatbloom", "vatling", "cryoqueen"),
                new LootTable[]{RealmPoiHoards.prize(RealmPoiPresets.SKY_COUNTERFEIT_TREASURY)},
                reward(item("skyballoon", 1), item("windsilk", 10)),
                challenge("swhwindwheat", new PickupItemsJournalChallenge(20, true, "windwheat"), "windwheat"),
                challenge("swhcloudpufftreat", new CraftItemJournalChallenge("cloudpufftreat"), "cloudpufftreat"),
                challenge("swhsourvatbloom", new DefeatMobJournalChallenge("sourvatbloom"), "sourvatbloom"));
        entry("stormveil",
                items("charwood", "thunderbloom", "staticmoss", "skystone", "fulgurite", "stormshard", "aetheriumore"),
                mobs("sparkbeetle", "stormwisp", "zephyrray", "skystonegolem", "rimesentry", "auroraflake",
                        "mistserpent", "prototypenine"),
                new LootTable[0],
                reward(item("stormdisc", 4)),
                challenge("swhstormwisps", new MobsKilledJournalChallenge(15, "stormwisp"), "stormwisp"),
                challenge("swhstormshards", new PickupItemsJournalChallenge(12, true, "stormshard"), "stormshard"),
                challenge("swhprototypenine", new DefeatMobJournalChallenge("prototypenine"), "prototypenine"));
        entry("aurorashoals",
                items("prismwood", "glowfern", "auroralily", "aurorapetal", "skystone", "prismshard", "aetheriumore"),
                mobs("glowmoth", "dewsnail", "skystonegolem", "zephyrray", "dawnpiercer", "auroraflake", "mistserpent"),
                new LootTable[]{RealmPoiHoards.prize(RealmPoiPresets.SKY_FALLEN_OBSERVATORY)},
                reward(item("skywatchastrolabe", 1), item("prismshard", 8)),
                challenge("swhaurorapetals", new PickupItemsJournalChallenge(15, true, "aurorapetal"), "aurorapetal"),
                challenge("swhdawnpiercer", new DefeatMobJournalChallenge("dawnpiercer"), "dawnpiercer"),
                challenge("swhprismcaller", new CraftItemJournalChallenge("prismcaller"), "prismcaller"));
        entry("skyway",
                items("seraphwood", "cloudwood", "skylichen", "cloudbell", "skytulip", "skystone", "fulgurite",
                        "prismshard", "aetheriumore"),
                mobs("zephyrfinch", "glowmoth", "skystonegolem", "galehound", "zephyrray", "rimesentry",
                        "mistserpent", "tollwright"),
                new LootTable[0],
                reward(item("skywatchtelescope", 1), item("aetheriumbar", 6)),
                challenge("swhtollwright", new DefeatMobJournalChallenge("tollwright"), "tollwright"),
                challenge("swhgalehounds", new MobsKilledJournalChallenge(10, "galehound"), "galehound"),
                challenge("swhskywatchfurniture", new CraftItemJournalChallenge(existing(
                        "skywatchbed", "skywatchbench", "skywatchbookshelf", "skywatchcabinet", "skywatchcandelabra",
                        "skywatchchair", "skywatchclock", "skywatchdesk", "skywatchdinnertable", "skywatchdisplay",
                        "skywatchdresser", "skywatchmodulartable")), "skywatchchair"));

        // ---------------------------------------------------------------- Eden
        entry("edengarden",
                items("palmlog", "dryadlog", "sprucelog", "paradiseapple", "sungrape", "edenberry", "moonmelon",
                        "blackberry", "blueberry", "stone", "ivyore"),
                mobs("edenserpent", "bloommaw", "goldenhornet", "moonlightdancer"),
                new LootTable[]{RealmPoiHoards.prize(RealmPoiPresets.EDEN_HEDGE_LABYRINTH)},
                reward(item("treeofplenty", 1), item("paradiseapple", 3)),
                challenge("swhparadiseapple", new ItemObtainedJournalChallenge("paradiseapple"), "paradiseapple"),
                challenge("swhbloommaws", new MobsKilledJournalChallenge(10, "bloommaw"), "bloommaw"),
                challenge("swhedenseedbasin", new CraftItemJournalChallenge("edenseedbasin"), "edenseedbasin"));
        entry("edencanopy",
                items("dryadlog", "palmlog", "edenwood", "edensap", "banana", "blueberry"),
                mobs("jealousvine", "edenserpent", "bloommaw", "forbiddenserpent"),
                new LootTable[0],
                reward(item("knowledgecutting", 3), item("edenwood", 10)),
                challenge("swhjealousvines", new MobsKilledJournalChallenge(8, "jealousvine"), "jealousvine"),
                challenge("swhforbiddenserpent", new DefeatMobJournalChallenge("forbiddenserpent"), "forbiddenserpent"),
                challenge("swhknowledgecutting", new ItemObtainedJournalChallenge("knowledgecutting"), "knowledgecutting"));
        entry("edenshallows",
                items("palmlog", "goldenpollen", "sungrape", "stone"),
                mobs("goldenhornet"),
                new LootTable[0],
                reward(item("edenbronzebar", 6), item("goldenpollen", 8)),
                challenge("swhgoldenhornets", new MobsKilledJournalChallenge(12, "goldenhornet"), "goldenhornet"),
                challenge("swhgoldenpollen", new PickupItemsJournalChallenge(10, true, "goldenpollen"), "goldenpollen"),
                challenge("swhedenbronze", new CraftItemJournalChallenge("edenbronzebar"), "edenbronzebar"));

        // ---------------------------------------------------------------- Steinfeld
        entry("quietmeadow",
                items("deadwoodlog", "spiritmoss", "palestone", "gravesalt"),
                mobs("lostpilgrim", "gravecrow", "stonemourner", "ascendedwizard"),
                new LootTable[]{stairwaytoheaven.realms.steinfeld.GraveyardPreset.LOOT},
                reward(item("mistglasslantern", 2), item("spiritmoss", 10)),
                challenge("swhlostpilgrims", new MobsKilledJournalChallenge(10, "lostpilgrim"), "lostpilgrim"),
                challenge("swhspiritmoss", new PickupItemsJournalChallenge(12, true, "spiritmoss"), "spiritmoss"),
                challenge("swhgravecrows", new MobsKilledJournalChallenge(10, "gravecrow"), "gravecrow"));
        entry("slabfields",
                items("deadwoodlog", "palestone", "gravesalt", "echoshard"),
                mobs("stonemourner", "lostpilgrim", "gravecrow", "hollowangel"),
                new LootTable[]{RealmPoiHoards.prize(RealmPoiPresets.STEINFELD_OSSUARY)},
                reward(item("seraphstatue", 1), item("echoshard", 6)),
                challenge("swhstonemourner", new DefeatMobJournalChallenge("stonemourner"), "stonemourner"),
                challenge("swhpalestone", new PickupItemsJournalChallenge(40, true, "palestone"), "palestone"),
                challenge("swhmourningband", new ItemObtainedJournalChallenge("mourningband"), "mourningband"));
        entry("graveheath",
                items("deadwoodlog", "spiritmoss", "gravesalt", "palestone"),
                mobs("lostpilgrim", "stonemourner", "gravecrow", "hollowangel"),
                new LootTable[]{stairwaytoheaven.realms.steinfeld.RuinedChapelPreset.LOOT},
                reward(item("gloomwillow", 1), item("gravesalt", 10)),
                challenge("swhhollowangel", new DefeatMobJournalChallenge("hollowangel"), "hollowangel"),
                challenge("swhgravesalt", new PickupItemsJournalChallenge(15, true, "gravesalt"), "gravesalt"),
                challenge("swhechoshards", new PickupItemsJournalChallenge(8, true, "echoshard"), "echoshard"));

        // ---------------------------------------------------------------- Ghost Realm
        entry("aftergarden",
                items("bonewood", "soulthread", "ectoplasm", "gloomshroom", "spectralore"),
                mobs("drifter", "headlessbutler", "lanternwidow", "soulhound", "veilbloom", "mourningbride",
                        "pestwarden"),
                new LootTable[]{RealmPoiHoards.prize(RealmPoiPresets.GHOST_WEDDING_FEAST)},
                reward(item("ghostlantern", 3), item("spiritsteelbar", 6)),
                challenge("swhdrifters", new MobsKilledJournalChallenge(12, "drifter"), "drifter"),
                challenge("swhlanternwidow", new DefeatMobJournalChallenge("lanternwidow"), "lanternwidow"),
                challenge("swhsoulbasin", new CraftItemJournalChallenge("soulbasin"), "soulbasin"));
        entry("boneorchard",
                items("bonewood", "soulthread", "gloomshroom", "spectralore"),
                mobs("lanternwidow", "headlessbutler", "drifter", "mourningbride", "coffincrawler"),
                new LootTable[0],
                reward(item("gloomravenstatue", 1), item("bonewood", 20)),
                challenge("swhbonewood", new PickupItemsJournalChallenge(30, true, "bonewood"), "bonewood"),
                challenge("swhheadlessbutler", new DefeatMobJournalChallenge("headlessbutler"), "headlessbutler"),
                challenge("swhmourningbride", new DefeatMobJournalChallenge("mourningbride"), "mourningbride"));
        entry("ectomarsh",
                items("bonewood", "soulthread", "ectoplasm", "spectralore"),
                mobs("drifter", "soulhound", "coffincrawler", "mourningbride", "veilbloom", "possessedchair"),
                new LootTable[0],
                reward(item("soulthread", 10), item("spiritsteelbar", 5)),
                challenge("swhsoulhounds", new MobsKilledJournalChallenge(10, "soulhound"), "soulhound"),
                challenge("swhsoulthread", new PickupItemsJournalChallenge(15, true, "soulthread"), "soulthread"),
                challenge("swhcoffincrawler", new DefeatMobJournalChallenge("coffincrawler"), "coffincrawler"));
        entry("gloomfen",
                items("deadwoodlog", "stone", "gloomshroom", "veilessence"),
                mobs("gloomshade", "fenwraith", "cindercantor"),
                new LootTable[0],
                reward(item("veilessence", 8), item("spiritsteelbar", 4)),
                challenge("swhgloomshades", new MobsKilledJournalChallenge(12, "gloomshade"), "gloomshade"),
                challenge("swhfenwraith", new DefeatMobJournalChallenge("fenwraith"), "fenwraith"),
                challenge("swhveilessence", new PickupItemsJournalChallenge(8, true, "veilessence"), "veilessence"));
        entry("ashenreach",
                items("deadwoodlog", "cinderpearl", "veilessence"),
                mobs("gloomshade", "cindercantor", "fenwraith"),
                new LootTable[0],
                reward(item("cinderpearl", 6), item("spiritsteelbar", 6)),
                challenge("swhcindercantor", new DefeatMobJournalChallenge("cindercantor"), "cindercantor"),
                challenge("swhcinderpearls", new PickupItemsJournalChallenge(6, true, "cinderpearl"), "cinderpearl"),
                challenge("swhspiritsteelarmor", new ItemObtainedJournalChallenge(existing(
                        "spiritsteelhelmet", "spiritsteelchestplate", "spiritsteelboots")), "spiritsteelchestplate"));

        // ---------------------------------------------------------------- Crooked Beyond
        entry("stripedwaste",
                items("oddwood", "realityshard", "stripedshell"),
                mobs("stripebeetle", "crookedgolem", "crookedarmadillo", "rarecrookedgolem", "doormimic",
                        "crystaldragon"),
                new LootTable[]{RealmPoiHoards.prize(RealmPoiPresets.CROOKED_HALL_OF_DOORS)},
                reward(item("realityshard", 8)),
                challenge("swhcrookedgolems", new MobsKilledJournalChallenge(10, "crookedgolem"), "crookedgolem"),
                challenge("swhstripedshells", new PickupItemsJournalChallenge(6, true, "stripedshell"), "stripedshell"),
                challenge("swhdoormimic", new DefeatMobJournalChallenge("doormimic"), "doormimic"));
        entry("spiralfields",
                items("oddwood", "eyeseed", "warpresin", "strangefabric", "realityshard"),
                mobs("tongueplant", "crookedarmadillo", "crookedgolem"),
                new LootTable[]{stairwaytoheaven.realms.crooked.DoorYardPreset.LOOT},
                reward(item("oddwood", 20), item("warpresin", 8)),
                challenge("swhtongueplants", new MobsKilledJournalChallenge(8, "tongueplant"), "tongueplant"),
                challenge("swhoddwood", new PickupItemsJournalChallenge(30, true, "oddwood"), "oddwood"),
                challenge("swhwarpresin", new PickupItemsJournalChallenge(10, true, "warpresin"), "warpresin"));
        entry("checkerworks",
                items("oddwood", "realityshard", "strangefabric", "crystalstone"),
                mobs("crookedgolem", "doormimic", "crookedarmadillo", "rarecrookedgolem"),
                new LootTable[]{stairwaytoheaven.realms.crooked.LongTablePreset.LOOT},
                reward(item("strangefabric", 10), item("realityshard", 4)),
                challenge("swhrarecrookedgolem", new DefeatMobJournalChallenge("rarecrookedgolem"), "rarecrookedgolem"),
                challenge("swhrealityshards", new PickupItemsJournalChallenge(8, true, "realityshard"), "realityshard"),
                challenge("swhstrangefabric", new PickupItemsJournalChallenge(10, true, "strangefabric"), "strangefabric"));
        entry("outlands",
                items("oddwood", "eyeseed", "realityshard"),
                mobs("crookedgolem", "rarecrookedgolem", "crookedarmadillo", "gloomshade", "fenwraith", "cindercantor"),
                new LootTable[]{stairwaytoheaven.realms.crooked.InvertedHousePreset.LOOT},
                reward(item("eyeseed", 5), item("spiritsteelbar", 6)),
                challenge("swhcrookedarmadillos", new MobsKilledJournalChallenge(10, "crookedarmadillo"), "crookedarmadillo"),
                challenge("swheyeseeds", new PickupItemsJournalChallenge(5, true, "eyeseed"), "eyeseed"),
                challenge("swhfenwraiths", new MobsKilledJournalChallenge(8, "fenwraith"), "fenwraith"));
        entry("beetlefreakhollow",
                items("veilessence", "deadwoodlog"),
                mobs("gloomshade", "fenwraith", "cindercantor"),
                new LootTable[0],
                reward(item("veilessence", 6), item("spiritsteelbar", 8)),
                challenge("swhbeetlewalls", new ObjectsPlacedJournalChallenge(20, "beetlewall"), "beetlewall"),
                challenge("swhbeetledoor", new CraftItemJournalChallenge("beetledoor"), "beetledoor"),
                challenge("swhcindercantors", new MobsKilledJournalChallenge(5, "cindercantor"), "cindercantor"));

        // ---------------------------------------------------------------- Hell
        entry("infernalfringe",
                items("cinderpearl", "charwood"),
                mobs("infernalclerk", "boilerhound", "ticketimp", "ashspirit", "mutanthydra"),
                new LootTable[0],
                reward(item("spiritsteelbar", 16)),
                challenge("swhinfernalclerks", new MobsKilledJournalChallenge(10, "infernalclerk"), "infernalclerk"),
                challenge("swhticketimps", new MobsKilledJournalChallenge(10, "ticketimp"), "ticketimp"),
                challenge("swhinfernalseal", new ItemObtainedJournalChallenge("regionkeyhell"), "regionkeyhell"));
        entry("furnacereach",
                items("cinderpearl", "charwood"),
                mobs("infernalclerk", "boilerhound", "ticketimp", "ashspirit"),
                new LootTable[0],
                reward(item("zephyrharness", 1)),
                challenge("swhboilerhounds", new MobsKilledJournalChallenge(10, "boilerhound"), "boilerhound"),
                challenge("swhashspirits", new MobsKilledJournalChallenge(10, "ashspirit"), "ashspirit"),
                challenge("swhhellpearls", new PickupItemsJournalChallenge(10, true, "cinderpearl"), "cinderpearl"));

        System.out.println("swhjournal vanilla: entries=" + entries + " challenges=" + challenges
                + " missing=" + MISSING.size() + (MISSING.isEmpty() ? "" : " " + MISSING));
    }

    // ------------------------------------------------------------------
    // building blocks

    /** One challenge of an entry's three, with the one ID it depends on checked. */
    private static final class Challenge {
        final String id;
        final JournalChallenge challenge;
        final String dependsOn;

        Challenge(String id, JournalChallenge challenge, String dependsOn) {
            this.id = id;
            this.challenge = challenge;
            this.dependsOn = dependsOn;
        }
    }

    private static Challenge challenge(String id, JournalChallenge challenge, String dependsOn) {
        return new Challenge(id, challenge, dependsOn);
    }

    private static LootItem item(String id, int amount) {
        return new LootItem(id, amount);
    }

    private static LootItem[] reward(LootItem... items) {
        return items;
    }

    private static String[] items(String... ids) {
        return existing(ids);
    }

    private static String[] mobs(String... ids) {
        List<String> out = new ArrayList<>();
        for (String id : ids) {
            if (MobRegistry.mobExists(id)) {
                out.add(id);
            } else {
                MISSING.add("mob:" + id);
            }
        }
        return out.toArray(new String[0]);
    }

    /** The item IDs that exist; the others are recorded as missing. */
    private static String[] existing(String... ids) {
        List<String> out = new ArrayList<>();
        for (String id : ids) {
            if (ItemRegistry.itemExists(id)) {
                out.add(id);
            } else {
                MISSING.add("item:" + id);
            }
        }
        return out.toArray(new String[0]);
    }

    private static boolean exists(String id) {
        return ItemRegistry.itemExists(id) || MobRegistry.mobExists(id);
    }

    private static void entry(String biomeID, String[] loot, String[] mobIDs, LootTable[] treasures,
            LootItem[] rewardItems, Challenge... set) {
        Biome biome = BiomeRegistry.getBiome(biomeID);
        if (biome == null || biome == BiomeRegistry.UNKNOWN) {
            MISSING.add("biome:" + biomeID);
            return;
        }
        JournalEntry entry = JournalRegistry.registerJournalEntry(biomeID,
                new JournalEntry(biome, SkyRegistry.SKYREACH_IDENTIFIER));
        entry.addBiomeLootEntry(loot);
        entry.addMobEntries(mobIDs);
        for (LootTable table : treasures) {
            entry.addTreasureEntry(table);
        }

        List<Integer> ids = new ArrayList<>();
        for (Challenge c : set) {
            if (!exists(c.dependsOn)) {
                MISSING.add("challenge:" + c.id + "(" + c.dependsOn + ")");
                continue;
            }
            ids.add(JournalChallengeRegistry.registerChallenge(c.id, c.challenge));
            challenges++;
        }
        List<LootItem> rewards = new ArrayList<>();
        for (LootItem item : rewardItems) {
            if (ItemRegistry.itemExists(item.itemStringID)) {
                rewards.add(item);
            } else {
                MISSING.add("reward:" + item.itemStringID);
            }
        }
        if (!ids.isEmpty()) {
            int multi = JournalChallengeRegistry.registerChallenge(biomeID,
                    new MultiJournalChallenge(ids.toArray(new Integer[0]))
                            .setReward(new LootTable(rewards.toArray(new LootItem[0]))));
            entry.addEntryChallenges(multi);
        }
        entries++;
    }
}
