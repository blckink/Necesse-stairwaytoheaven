package stairwaytoheaven.worldgen.pois;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.hostile.MimicMob;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import necesse.level.maps.Level;
import stairwaytoheaven.bosses.BossScaling;

/**
 * Who stands in the six places of {@code docs/design/chapter-02-hoards-and-mimics.md},
 * and what their chests and their mimics carry.
 *
 * <h2>Why this is its own class</h2>
 * {@link RealmPoiPresets} holds the plans, because the plan interpreter lives
 * there; this holds everything a plan cannot: mobs, which are not objects, and
 * loot, which is rolled per placement. Both are read OFF the plan rows —
 * {@link RealmPoiPresets#hoardPlan} — so a mimic stands on exactly the tile the
 * dossier's map draws an {@code M} on and nowhere else.
 *
 * <h2>Once, and never again</h2>
 * {@link #placeInhabitants} runs from {@link RealmPoiWorldPreset}'s place
 * function, which the engine calls once, when the preset region generates the
 * rectangle — the same path the Dew-Keeper's snails and the Redoubt's
 * garrison take. Every mob here is {@code canDespawn = false} and on no spawn
 * table, so a place that has been cleared stays cleared: nothing respawns.
 *
 * <h2>The mimic mechanism</h2>
 * Vanilla's own, unmodified. <b>VERIFIED [jar]</b> (decompiled
 * {@code RandomCaveChestRoom.openingApply}, lines 109-120): the engine deletes
 * the {@code storagebox} a cave chest room would have had, and spawns a
 * {@code "mimic"} there with {@code canDespawn = false}, {@code setDir} of the
 * chest's rotation and the room's loot table rolled into {@code MimicMob.loot};
 * {@code MimicMob.getLootTable} drops that list plus a {@code mimicchest} when
 * it dies, and the list is saved with the mob. Here the {@code storagebox} is
 * never written at all — the plan draws {@code M} as floor — and the mimic is
 * {@code hoardmimic} (or the realm's own: {@code doormimic} in the Crooked
 * Beyond, whose loot is its own table).
 */
public final class RealmPoiHoards {

    private RealmPoiHoards() {
    }

    /** The six kinds, in dossier order. The census walks this. */
    public static final int[] KINDS = {
            RealmPoiPresets.SKY_COUNTERFEIT_TREASURY,
            RealmPoiPresets.SKY_FALLEN_OBSERVATORY,
            RealmPoiPresets.EDEN_HEDGE_LABYRINTH,
            RealmPoiPresets.STEINFELD_OSSUARY,
            RealmPoiPresets.GHOST_WEDDING_FEAST,
            RealmPoiPresets.CROOKED_HALL_OF_DOORS,
    };

    /**
     * The tier a guardian is lifted by, on top of its own realm row: x2.12
     * health and x1.52 damage ({@code SkyBossLadder.healthMultiplier(5)} and
     * {@code damageMultiplier(5)}, vanilla's incursion curve). A realm elite
     * becomes the thing the room is built around without becoming a ladder
     * boss — those are x3.18 and up, and a treasure room is not a boss fight.
     */
    public static final int GUARDIAN_TIER = 5;

    /** Whether a kind is one of the six. */
    public static boolean isHoard(int kind) {
        return indexOf(kind) >= 0;
    }

    private static int indexOf(int kind) {
        for (int i = 0; i < KINDS.length; i++) {
            if (KINDS[i] == kind) return i;
        }
        return -1;
    }

    // ------------------------------------------------------------------ cast

    /** The mimic each place uses, by {@link #KINDS} index. */
    private static final String[] MIMIC = {
            "hoardmimic", "hoardmimic", "hoardmimic", "hoardmimic", "hoardmimic", "doormimic"};

    /**
     * The tier {@code hoardmimic} is lifted by in each realm, so it sits on
     * that realm's row ({@code docs/BALANCE.md} §5, floor-relative): Eden x1.52
     * HP at tier 3, Steinfeld x2.12 at 5, the Ghost Realm x2.80 at 7. 0 = the
     * mob's own stats — the Skyreach row, and the Door Mimic, which is already
     * the Crooked row's elite.
     */
    private static final int[] MIMIC_TIER = {0, 0, 3, 5, 7, 0};

    /** The guardian ({@code W}) each place keeps. */
    private static final String[] GUARDIAN = {
            "skystonegolem", "dawnpiercer", "forbiddenserpent",
            "hollowangel", "mourningbride", "rarecrookedgolem"};

    /** The other guard letters of each plan, and the mob each one is. */
    private static final String[] GUARD_CHARS = {"a", "w", "vm", "n", "P", "a"};
    private static final String[][] GUARD_MOBS = {
            {"rimesentry"},
            {"stormwisp"},
            {"jealousvine", "bloommaw"},
            {"stonemourner"},
            {"possessedchair"},
            {"crookedarmadillo"},
    };

    /** The container letters besides {@code T} each plan fills with bait. */
    private static final String[] BAIT_CHARS = {"C", "kb", "C", "C", "C", "C"};

    /** The characters of each plan a player can stand on: its floors, and its mobs' tiles. */
    private static final String[] FLOOR_CHARS = {",+rnq", ",+=", ",", ",;p", ",;", "=;"};

    /**
     * Whether a plan character is open ground, the one test both a chest and
     * a mimic are turned by ({@link RealmPoiPresets#faceInward}) — so the two
     * face the same way and the direction gives nothing away.
     */
    static boolean isOpen(int kind, char c) {
        int index = indexOf(kind);
        return c == '.' || c == 'M' || c == 'W'
                || FLOOR_CHARS[index].indexOf(c) >= 0 || GUARD_CHARS[index].indexOf(c) >= 0;
    }

    public static String mimicOf(int kind) {
        return MIMIC[indexOf(kind)];
    }

    public static String guardianOf(int kind) {
        return GUARDIAN[indexOf(kind)];
    }

    // ------------------------------------------------------------------ loot

    /** An item ID, refused at load if it does not exist — a typo fails startup, not a chest. */
    private static String id(String itemID) {
        if (!ItemRegistry.itemExists(itemID)) {
            throw new IllegalStateException("Hoard loot names an unregistered item: " + itemID);
        }
        return itemID;
    }

    /**
     * The prize: the chest the guardian stands in front of. One guaranteed
     * stack of the realm's bar or best material, its secondary materials, coin
     * at the realm's drop value ({@code BALANCE.md} §5: x1.0 / 1.3 / 1.6 / 1.9
     * / 2.5), and a CHANCE at the realm's band trophy — the item Magpie buys
     * above broker — and in the Skyreach at one of the three arsenal weapons
     * the Nightfell Redoubt already rolls. Nothing here is a tier ahead of its
     * realm: the rule is "more of what the realm has", never "the next realm's".
     */
    public static LootTable prize(int kind) {
        switch (kind) {
            case RealmPoiPresets.SKY_COUNTERFEIT_TREASURY:
                return new LootTable(
                        LootItem.between(id("stormsteelbar"), 4, 8),
                        LootItem.between(id("aetheriumbar"), 3, 6),
                        LootItem.between(id("stormglass"), 3, 7),
                        LootItem.between(id("coin"), 900, 2200),
                        new ChanceLootItemList(0.30F, new OneOfLootItems(
                                new LootItem(id("stormdisc")),
                                new LootItem(id("skyreave")),
                                new LootItem(id("thunderhead")))),
                        ChanceLootItem.between(0.15F, id("skystoneheart"), 1, 1));
            case RealmPoiPresets.SKY_FALLEN_OBSERVATORY:
                return new LootTable(
                        LootItem.between(id("prismshard"), 3, 7),
                        LootItem.between(id("stormglass"), 4, 8),
                        LootItem.between(id("stormshard"), 2, 4),
                        LootItem.between(id("coin"), 500, 1400),
                        ChanceLootItem.between(0.20F, id("auroralocket"), 1, 1),
                        ChanceLootItem.between(0.15F, id("skystoneheart"), 1, 1));
            case RealmPoiPresets.EDEN_HEDGE_LABYRINTH:
                return new LootTable(
                        LootItem.between(id("edenbronzebar"), 4, 8),
                        LootItem.between(id("serpentscale"), 3, 6),
                        LootItem.between(id("venomfang"), 1, 3),
                        LootItem.between(id("goldenpollen"), 2, 5),
                        LootItem.between(id("coin"), 1100, 2800),
                        ChanceLootItem.between(0.50F, id("paradiseapple"), 2, 4),
                        ChanceLootItem.between(0.15F, id("bloomfang"), 1, 1));
            case RealmPoiPresets.STEINFELD_OSSUARY:
                return new LootTable(
                        LootItem.between(id("palestone"), 6, 12),
                        LootItem.between(id("gravesalt"), 4, 9),
                        LootItem.between(id("echoshard"), 2, 5),
                        LootItem.between(id("spiritmoss"), 3, 7),
                        LootItem.between(id("coin"), 1400, 3500),
                        ChanceLootItem.between(0.20F, id("mourningband"), 1, 1));
            case RealmPoiPresets.GHOST_WEDDING_FEAST:
                return new LootTable(
                        LootItem.between(id("spiritsteelbar"), 4, 8),
                        LootItem.between(id("soulthread"), 5, 10),
                        LootItem.between(id("spectralore"), 4, 8),
                        LootItem.between(id("bonewood"), 6, 12),
                        LootItem.between(id("coin"), 1700, 4200),
                        ChanceLootItem.between(0.20F, id("soulcollar"), 1, 1));
            case RealmPoiPresets.CROOKED_HALL_OF_DOORS:
                return new LootTable(
                        LootItem.between(id("realityshard"), 3, 6),
                        LootItem.between(id("warpresin"), 5, 10),
                        LootItem.between(id("strangefabric"), 5, 10),
                        LootItem.between(id("oddwood"), 6, 12),
                        LootItem.between(id("coin"), 2200, 5500),
                        ChanceLootItem.between(0.50F, id("eyeseed"), 2, 4),
                        ChanceLootItem.between(0.20F, id("stripedhorn"), 1, 1));
            default:
                throw new IllegalArgumentException("Not a hoard kind: " + kind);
        }
    }

    /**
     * What an honest box holds: little. The gamble the player takes on a
     * storage box is between this and a fight, and a fight that pays better
     * than the box is what makes the gamble worth taking (see {@link #hoard}).
     * One guaranteed stack each, so the census can tell "empty" from "rolled".
     */
    public static LootTable bait(int kind, char container) {
        switch (kind) {
            case RealmPoiPresets.SKY_COUNTERFEIT_TREASURY:
                return new LootTable(
                        LootItem.between(id("coin"), 60, 200),
                        ChanceLootItem.between(0.50F, id("stormglass"), 1, 3),
                        ChanceLootItem.between(0.30F, id("skystone"), 2, 5));
            case RealmPoiPresets.SKY_FALLEN_OBSERVATORY:
                // 'k' stands in the sealed cellar beside the prize; 'b' is the
                // ruin's surviving shelf of charts, open to anyone.
                return container == 'k'
                        ? new LootTable(
                                LootItem.between(id("aurorapetal"), 2, 5),
                                LootItem.between(id("prismwood"), 4, 8),
                                ChanceLootItem.between(0.40F, id("prismshard"), 1, 3))
                        : new LootTable(
                                LootItem.between(id("coin"), 40, 150),
                                ChanceLootItem.between(0.50F, id("cloudberry"), 2, 5));
            case RealmPoiPresets.EDEN_HEDGE_LABYRINTH:
                return new LootTable(
                        LootItem.between(id("edenberry"), 3, 8),
                        ChanceLootItem.between(0.50F, id("edencopperore"), 3, 8),
                        ChanceLootItem.between(0.60F, id("coin"), 80, 260));
            case RealmPoiPresets.STEINFELD_OSSUARY:
                return new LootTable(
                        LootItem.between(id("gravesalt"), 1, 3),
                        ChanceLootItem.between(0.60F, id("coin"), 100, 320));
            case RealmPoiPresets.GHOST_WEDDING_FEAST:
                return new LootTable(
                        LootItem.between(id("soulthread"), 2, 4),
                        ChanceLootItem.between(0.60F, id("coin"), 120, 380));
            case RealmPoiPresets.CROOKED_HALL_OF_DOORS:
                return new LootTable(
                        LootItem.between(id("oddwood"), 3, 6),
                        ChanceLootItem.between(0.60F, id("coin"), 180, 480));
            default:
                throw new IllegalArgumentException("Not a hoard kind: " + kind);
        }
    }

    /**
     * What a mimic carries in {@code MimicMob.loot}: roughly twice an honest
     * box, and a chance at the realm's bar. It is dropped together with
     * vanilla's own {@code mimicchest} when the mimic dies. Not used for the
     * Door Mimic, whose {@code getLootTable} is its own and ignores the list.
     */
    public static LootTable hoard(int kind) {
        switch (kind) {
            case RealmPoiPresets.SKY_COUNTERFEIT_TREASURY:
                return new LootTable(
                        LootItem.between(id("coin"), 150, 450),
                        ChanceLootItem.between(0.40F, id("aetheriumbar"), 1, 2),
                        ChanceLootItem.between(0.08F, id("silverbell"), 1, 1));
            case RealmPoiPresets.SKY_FALLEN_OBSERVATORY:
                return new LootTable(
                        LootItem.between(id("coin"), 120, 380),
                        ChanceLootItem.between(0.40F, id("prismshard"), 1, 3));
            case RealmPoiPresets.EDEN_HEDGE_LABYRINTH:
                return new LootTable(
                        LootItem.between(id("coin"), 200, 520),
                        ChanceLootItem.between(0.40F, id("edenbronzebar"), 1, 3));
            case RealmPoiPresets.STEINFELD_OSSUARY:
                return new LootTable(
                        LootItem.between(id("coin"), 250, 650),
                        ChanceLootItem.between(0.40F, id("echoshard"), 1, 2));
            case RealmPoiPresets.GHOST_WEDDING_FEAST:
                return new LootTable(
                        LootItem.between(id("coin"), 300, 800),
                        ChanceLootItem.between(0.35F, id("spiritsteelbar"), 1, 2));
            default:
                return new LootTable();
        }
    }

    // ------------------------------------------------------------ placement

    /** One mob a plan asks for: which, where, and in what role. */
    public static final class Resident {
        public final String mobID;
        public final int x;
        public final int y;
        /** 'M' mimic, 'W' guardian, or the guard letter. */
        public final char role;

        Resident(String mobID, int x, int y, char role) {
            this.mobID = mobID;
            this.x = x;
            this.y = y;
            this.role = role;
        }
    }

    /** Every mob one kind's plan draws, in reading order. */
    public static List<Resident> residents(int kind) {
        int index = indexOf(kind);
        String[] rows = RealmPoiPresets.hoardPlan(kind);
        List<Resident> out = new ArrayList<>();
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                char c = rows[y].charAt(x);
                if (c == 'M') {
                    out.add(new Resident(MIMIC[index], x, y, c));
                } else if (c == 'W') {
                    out.add(new Resident(GUARDIAN[index], x, y, c));
                } else {
                    int guard = GUARD_CHARS[index].indexOf(c);
                    if (guard >= 0) {
                        out.add(new Resident(GUARD_MOBS[index][guard], x, y, c));
                    }
                }
            }
        }
        return out;
    }

    /** Every container tile of one kind's plan: the prize first, then the bait. */
    public static List<Point> containers(int kind) {
        int index = indexOf(kind);
        String[] rows = RealmPoiPresets.hoardPlan(kind);
        List<Point> out = new ArrayList<>();
        for (int y = 0; y < rows.length; y++) {
            int x = rows[y].indexOf('T');
            if (x >= 0) out.add(0, new Point(x, y));
        }
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                if (BAIT_CHARS[index].indexOf(rows[y].charAt(x)) >= 0) {
                    out.add(new Point(x, y));
                }
            }
        }
        return out;
    }

    /**
     * Seats the whole cast of one hoard place, server side, once. See the
     * class header for why "once" is the engine's guarantee and not ours.
     */
    public static void placeInhabitants(int kind, Level level, int originX, int originY) {
        if (level.isClient() || !isHoard(kind)) {
            return;
        }
        int index = indexOf(kind);
        String[] rows = RealmPoiPresets.hoardPlan(kind);
        for (Resident resident : residents(kind)) {
            Mob mob = MobRegistry.getMob(resident.mobID, level);
            if (mob == null) {
                continue;
            }
            mob.canDespawn = false;
            int tileX = originX + resident.x;
            int tileY = originY + resident.y;
            int px = tileX * 32 + 16;
            int py = tileY * 32 + 16;
            if (resident.role == 'M') {
                // Vanilla's own order (RandomCaveChestRoom:113-118): loot,
                // canDespawn, setDir, onSpawned, addMob.
                if (mob instanceof MimicMob && !"doormimic".equals(resident.mobID)) {
                    GameRandom random = new GameRandom(
                            (long) tileX * 341873128712L + (long) tileY * 132897987541L + kind);
                    ((MimicMob) mob).loot.addAll(hoard(kind).getNewList(random, 1.0F, level));
                }
                mob.setDir(RealmPoiPresets.faceInward(rows, resident.x, resident.y,
                        c -> isOpen(kind, (char) c)));
                mob.onSpawned(px, py);
            }
            level.entityManager.addMob(mob, px, py);
            if (resident.role == 'M' && MIMIC_TIER[index] > 0) {
                BossScaling.applyTier(mob, MIMIC_TIER[index]);
            } else if (resident.role == 'W') {
                BossScaling.applyTier(mob, GUARDIAN_TIER);
            }
        }
    }
}
