import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the shipped enemy statline out of the BUILT mod jar and holds it
 * against an expected table.
 *
 * <p>See scripts/balance_check.sh for what this proves and what it does not.
 * The short version: every number below is read out of the jar by reflection,
 * so the table cannot drift from the build silently. Rows are the mobs the
 * biome spawn tables actually spawn, plus the six boss rungs.
 */
public class BalanceCheck {

    // ===== the expected table =================================================
    // realm band | mob class | role | old HP | new HP | old dmg | new dmg
    //            | old armour | new armour | old aggro | new aggro
    //
    // "old" is what the mob shipped with before the §10 ascension uplift, read
    // off git revision 4a5cda0. "new" is floor x uplift, computed the same way
    // the classes compute it, so a wrong uplift fails here rather than in the
    // player's world. -1 in an aggro column means the mob has no aggression
    // range of its own to lift: it inherits a vanilla AI tree, or it is
    // MistserpentHead, whose circling chaser already reaches 80 tiles. An aggro
    // column where old EQUALS new is a deliberate hold, and the mob's own
    // javadoc has to say why.

    static final Row[] ROWS = {
        // --- Skyreach band, floor 1000 / 130 / 40, uplift 130 / 115 / 125 % ---
        r("Skyreach", "stairwaytoheaven.mobs.SkystoneGolemMob", "elite",
                1400, 1820, 130.0F, 149.5F, 40, 50, 384, 480),
        r("Skyreach", "stairwaytoheaven.mobs.GalehoundMob", "fast",
                600, 780, 104.0F, 119.6F, 40, 50, 512, 640),
        r("Skyreach", "stairwaytoheaven.mobs.ZephyrRayMob", "fast",
                600, 780, 104.0F, 119.6F, 40, 50, 512, 640),
        r("Skyreach", "stairwaytoheaven.mobs.DawnpiercerMob", "fast",
                600, 780, 104.0F, 119.6F, 40, 50, 512, 640),
        r("Skyreach", "stairwaytoheaven.mobs.StormWispMob", "ranged",
                700, 910, 110.5F, 127.075F, 40, 50, 448, 560),
        r("Skyreach", "stairwaytoheaven.arsenal.AuroraFlakeMob", "fast",
                600, 780, 105.0F, 119.6F, 40, 50, 448, 560),
        r("Skyreach", "stairwaytoheaven.arsenal.RimeSentryMob", "ranged",
                700, 910, 110.0F, 127.075F, 40, 50, 352, 440),
        r("Skyreach", "stairwaytoheaven.mobs.MistserpentHead", "elite",
                1400, 1820, 130.0F, 149.5F, 40, 50, -1, -1),

        // --- Eden, floor 1500 / 165 / 45, uplift 135 / 118 / 125 % ---
        r("Eden", "stairwaytoheaven.realms.eden.JealousVineMob", "standard",
                1500, 2025, 165.0F, 194.7F, 45, 56, 560, 728),
        r("Eden", "stairwaytoheaven.realms.eden.EdenSerpentMob", "standard",
                1500, 2025, 165.0F, 194.7F, 45, 56, 480, 624),
        r("Eden", "stairwaytoheaven.realms.eden.BloomMawMob", "standard",
                1500, 2025, 165.0F, 194.7F, 45, 56, 384, 499),
        r("Eden", "stairwaytoheaven.realms.eden.ForbiddenSerpentMob", "elite",
                2100, 2835, 165.0F, 194.7F, 45, 56, 640, 832),
        r("Eden", "stairwaytoheaven.realms.eden.GoldenHornetMob", "fast",
                900, 1215, 132.0F, 155.76F, 45, 56, 520, 676),

        // --- Steinfeld, floor 2100 / 200 / 50, uplift 140 / 121 / 125 % ---
        r("Steinfeld", "stairwaytoheaven.realms.steinfeld.mobs.StoneMournerMob", "standard",
                2100, 2940, 200.0F, 242.0F, 50, 62, 512, 691),
        r("Steinfeld", "stairwaytoheaven.realms.steinfeld.mobs.HollowAngelMob", "elite",
                2940, 4116, 200.0F, 242.0F, 50, 62, -1, -1),
        r("Steinfeld", "stairwaytoheaven.realms.steinfeld.mobs.GraveCrowMob", "ranged",
                1470, 2058, 170.0F, 205.7F, 50, 62, 480, 648),
        r("Steinfeld", "stairwaytoheaven.realms.steinfeld.mobs.LostPilgrimMob", "fast",
                1260, 1764, 160.0F, 193.6F, 50, 62, 448, 604),

        // --- Veil / Ghost Realm, floor 2800 / 230 / 55, uplift 145 / 124 / 125 % ---
        r("Veil", "stairwaytoheaven.mobs.GloomShadeMob", "standard",
                2800, 4060, 230.0F, 285.2F, 55, 68, 512, 716),
        r("Veil", "stairwaytoheaven.arsenal.FenWraithMob", "standard",
                2800, 4060, 230.0F, 285.2F, 55, 68, 768, 1075),
        r("Veil", "stairwaytoheaven.arsenal.CinderCantorMob", "ranged",
                1960, 2842, 195.0F, 242.42F, 55, 68, 640, 896),
        r("Ghost", "stairwaytoheaven.realms.ghost.CoffinCrawlerMob", "standard",
                2800, 4060, 230.0F, 285.2F, 55, 68, 512, 716),
        r("Ghost", "stairwaytoheaven.realms.ghost.DrifterMob", "standard",
                2800, 4060, 230.0F, 285.2F, 55, 68, 448, 627),
        r("Ghost", "stairwaytoheaven.realms.ghost.HeadlessButlerMob", "standard",
                2800, 4060, 230.0F, 285.2F, 55, 68, 512, 716),
        r("Ghost", "stairwaytoheaven.realms.ghost.MourningBrideMob", "elite",
                3920, 5684, 230.0F, 285.2F, 55, 68, 448, 627),
        r("Ghost", "stairwaytoheaven.realms.ghost.LanternWidowMob", "ranged",
                1960, 2842, 195.0F, 242.42F, 55, 68, 512, 716),
        r("Ghost", "stairwaytoheaven.realms.ghost.SoulHoundMob", "fast",
                1680, 2436, 184.0F, 228.16F, 55, 68, 512, 716),
        // A mimic rolls 14 dice instead of carrying one GameDamage, so its
        // damage column is the MEAN of MIN_DAMAGE_ROLL..MAX_DAMAGE_ROLL. Those
        // two were literals until 2026-09-07 and did NOT follow the realm row;
        // that is exactly the half-landed change this script exists to catch,
        // and it is why they are checked rather than skipped.
        r("Ghost", "stairwaytoheaven.realms.ghost.PossessedChairMob", "standard",
                2800, 4060, 230.0F, 285.0F, 55, 68, -1, -1),

        // --- Crooked Beyond, floor 4000 / 280 / 60, uplift 155 / 130 / 125 % ---
        r("Crooked", "stairwaytoheaven.realms.crooked.TonguePlantMob", "standard",
                4000, 6200, 280.0F, 364.0F, 60, 75, 960, 960),
        r("Crooked", "stairwaytoheaven.realms.crooked.DoorMimicMob", "elite",
                5600, 8680, 280.0F, 364.0F, 60, 75, -1, -1),

        // --- Hell, floor 4450 / 285 / 65, uplift 165 / 136 / 155 % ---
        // These four never shipped before, so there is no "old" value to read
        // off an earlier revision. The old column therefore records THE RUNG
        // THEY MUST EXCEED: Crooked Beyond's same-role value. That keeps every
        // assertion in check() meaningful rather than skipped -- "above the
        // old" reads as "above the realm below", which is exactly the claim a
        // new outermost band has to make. Crooked has no ranged and no fast
        // mob of its own, so those two rows use Crooked's floor with the role
        // applied (6200x70% = 4340, 364x85% = 309.4; 6200x60% = 3720,
        // 364x80% = 291.2) and its x1.50 aggro uplift on the same base range.
        r("Hell", "stairwaytoheaven.realms.hell.mobs.InfernalClerkMob", "standard",
                6200, 7342, 364.0F, 387.6F, 75, 81, 768, 793),
        r("Hell", "stairwaytoheaven.realms.hell.mobs.AshSpiritMob", "elite",
                8680, 10278, 364.0F, 387.6F, 75, 81, -1, -1),
        r("Hell", "stairwaytoheaven.realms.hell.mobs.TicketImpMob", "ranged",
                4340, 5139, 309.4F, 329.46F, 75, 81, 720, 744),
        r("Hell", "stairwaytoheaven.realms.hell.mobs.BoilerHoundMob", "fast",
                3720, 4405, 291.2F, 310.08F, 75, 81, 768, 793),
    };

    /** realm index | boss id | tier | old final HP | new final HP | old xdmg | new xdmg */
    static final Boss[] BOSSES = {
        b(0, "cryoqueen", 8, 57240, 74412, 1.87F, 2.1505F),
        b(1, "moonlightdancer", 8, 127200, 171720, 1.87F, 2.2066F),
        b(2, "ascendedwizard", 9, 157520, 220528, 2.0F, 2.42F),
        b(3, "pestwarden", 9, 161100, 233595, 2.0F, 2.48F),
        b(4, "crystaldragon", 10, 208000, 322400, 2.15F, 2.795F),
        // Hell. The old column is §B4's reservation -- 80 000 x the tier-10
        // curve of 4.00 -- because this rung had no portal before and so no
        // shipped value; what it must beat is the number the reservation
        // itself named.
        b(5, "mutanthydra", 10, 320000, 528000, 2.15F, 2.924F),
    };

    // ===== the check ==========================================================

    static final List<String> PROBLEMS = new ArrayList<>();
    static final float EPS = 0.01F;

    public static void main(String[] args) throws Exception {
        System.out.println("balance_check: reading the shipped statline out of the jar");
        System.out.println();
        System.out.printf("%-10s %-22s %-9s %13s %13s %9s %11s%n",
                "band", "mob", "role", "HP", "damage", "armour", "aggro");
        System.out.println("-".repeat(94));

        for (Row row : ROWS) {
            check(row);
        }
        System.out.println();
        checkBosses();
        System.out.println();
        checkMonotone();
        System.out.println();

        if (!PROBLEMS.isEmpty()) {
            System.out.println("FAIL -- " + PROBLEMS.size() + " problem(s):");
            for (String p : PROBLEMS) {
                System.out.println("  " + p);
            }
            System.exit(1);
        }
        System.out.println("OK -- every row in the jar matches the expected table, every");
        System.out.println("shipped value is above the value it replaced, and every role");
        System.out.println("column rises monotonically outwards across the six realm bands.");
    }

    static void check(Row row) throws Exception {
        Class<?> c = Class.forName(row.mobClass);
        int hp = classicHealth(c);
        float dmg = damage(c);
        int armor = intField(c, "ARMOR");
        int aggro = row.newAggro < 0 ? -1 : intField(c, "AGGRO_RANGE");

        System.out.printf("%-10s %-22s %-9s %6d->%-6d %6s->%-6s %4d->%-4d %5s->%-5s%n",
                row.band, simple(row.mobClass), row.role,
                row.oldHp, hp, num(row.oldDamage), num(dmg),
                row.oldArmor, armor, num(row.oldAggro), num(aggro));

        eqInt(row, "HP", row.newHp, hp);
        eqInt(row, "armour", row.newArmor, armor);
        if (row.newDamage >= 0.0F) {
            eqFloat(row, "damage", row.newDamage, dmg);
        }
        if (row.newAggro >= 0) {
            eqInt(row, "aggro range", row.newAggro, aggro);
        }

        // The point of the pass: harder than before, in every column that moved.
        gtInt(row, "HP", hp, row.oldHp);
        gtInt(row, "armour", armor, row.oldArmor);
        if (row.newDamage >= 0.0F) {
            if (dmg <= row.oldDamage) {
                PROBLEMS.add(simple(row.mobClass) + ": damage " + dmg
                        + " is not above the old " + row.oldDamage);
            }
        }
        // An aggro range where old == new is an EXPLICIT hold, not an
        // oversight: TonguePlantMob's 960 already reaches 30 tiles and its
        // javadoc says why lifting it would change nothing. Every other row
        // must have moved.
        if (row.newAggro >= 0 && row.newAggro != row.oldAggro) {
            gtInt(row, "aggro range", aggro, row.oldAggro);
        }
    }

    static void checkBosses() throws Exception {
        Class<?> ladder = Class.forName("stairwaytoheaven.bosses.SkyBossLadder");
        System.out.printf("%-18s %-6s %17s %17s%n", "boss", "tier", "final HP", "xdamage");
        System.out.println("-".repeat(62));
        for (Boss boss : BOSSES) {
            Object row = ladder.getMethod("forRealm", int.class).invoke(null, boss.realm);
            if (row == null) {
                PROBLEMS.add("boss " + boss.id + ": no ladder row for realm " + boss.realm);
                continue;
            }
            String id = (String) row.getClass().getField("mobStringID").get(row);
            int tier = (Integer) row.getClass().getField("tier").get(row);
            int finalHp = (Integer) row.getClass().getMethod("finalHealth").invoke(row);
            float xdmg = (Float) row.getClass().getMethod("damageMultiplier").invoke(row);

            System.out.printf("%-18s %-6d %7d->%-8d %7s->%-8s%n",
                    id, tier, boss.oldHealth, finalHp, num(boss.oldDamageMul), num(xdmg));

            if (!id.equals(boss.id)) {
                PROBLEMS.add("realm " + boss.realm + ": boss is " + id + ", expected " + boss.id);
            }
            if (tier != boss.tier) {
                PROBLEMS.add(boss.id + ": tier " + tier + ", expected " + boss.tier);
            }
            if (Math.abs(finalHp - boss.newHealth) > 1) {
                PROBLEMS.add(boss.id + ": final HP " + finalHp + ", expected " + boss.newHealth);
            }
            if (Math.abs(xdmg - boss.newDamageMul) > EPS) {
                PROBLEMS.add(boss.id + ": damage multiplier " + xdmg
                        + ", expected " + boss.newDamageMul);
            }
            if (finalHp <= boss.oldHealth) {
                PROBLEMS.add(boss.id + ": final HP " + finalHp
                        + " is not above the old " + boss.oldHealth);
            }
            if (xdmg <= boss.oldDamageMul) {
                PROBLEMS.add(boss.id + ": damage multiplier " + xdmg
                        + " is not above the old " + boss.oldDamageMul);
            }
        }
    }

    /**
     * "damit die Kurve nicht kippt" -- measured rather than asserted.
     *
     * <p>Per role, the six realm bands must rise strictly outwards. A uplift
     * that made, say, Eden's elite tougher than Steinfeld's elite would pass
     * every row check above and still have broken the ladder.
     */
    static void checkMonotone() {
        String[] bands = {"Skyreach", "Eden", "Steinfeld", "Veil|Ghost", "Crooked", "Hell"};
        System.out.println("monotone per role, outwards across the bands:");
        for (String role : new String[]{"standard", "elite", "ranged", "fast"}) {
            StringBuilder line = new StringBuilder();
            int prev = -1;
            boolean ok = true;
            for (String band : bands) {
                int max = -1;
                for (Row row : ROWS) {
                    if (!row.role.equals(role)) {
                        continue;
                    }
                    boolean hit = false;
                    for (String b : band.split("\\|")) {
                        hit |= row.band.equals(b);
                    }
                    if (hit) {
                        max = Math.max(max, row.newHp);
                    }
                }
                if (max < 0) {
                    continue;
                }
                line.append(prev < 0 ? "" : " < ").append(max);
                if (prev >= 0 && max <= prev) {
                    ok = false;
                }
                prev = max;
            }
            System.out.printf("  %-9s HP  %s   %s%n", role, line, ok ? "ok" : "BROKEN");
            if (!ok) {
                PROBLEMS.add("role " + role + ": HP does not rise outwards -- " + line);
            }
        }
    }

    // ===== reflection helpers =================================================

    /** CLASSIC health, whether the class holds an int or a MaxHealthGetter. */
    static int classicHealth(Class<?> c) throws Exception {
        Field f = findField(c, "MAX_HEALTH");
        if (f == null) {
            PROBLEMS.add(simple(c.getName()) + ": no MAX_HEALTH field");
            return -1;
        }
        f.setAccessible(true);
        Object v = f.get(null);
        if (v instanceof Integer) {
            return (Integer) v;
        }
        // MaxHealthGetter extends ProtectedDifficultyBasedGetter, whose `array`
        // is indexed by GameDifficulty ordinal; CLASSIC is 2. Read the field
        // rather than calling get(CLASSIC): GameDifficulty's own static init
        // pulls in localisation the dedicated server has not booted here.
        Field arr = instanceField(v.getClass(), "array");
        arr.setAccessible(true);
        Object[] values = (Object[]) arr.get(v);
        return (Integer) values[2];
    }

    /** The mob's primary GameDamage, or -1 where it has none (mimics roll theirs). */
    static float damage(Class<?> c) throws Exception {
        for (String name : new String[]{"DAMAGE", "damage", "HEAD_COLLISION"}) {
            Field f = findField(c, name);
            if (f == null) {
                continue;
            }
            f.setAccessible(true);
            Object v = f.get(null);
            if (v == null) {
                continue;
            }
            // GameDamage.damage is a public final INSTANCE field, so it does
            // not go through findField's static filter.
            Field d = v.getClass().getField("damage");
            d.setAccessible(true);
            return (Float) d.get(v);
        }
        // A mimic has no GameDamage: MimicMob rolls damageDiceCount dice
        // between two public non-final int fields. The mean is what the ladder
        // row is written against, so the mean is what is checked.
        Field lo = findField(c, "MIN_DAMAGE_ROLL");
        Field hi = findField(c, "MAX_DAMAGE_ROLL");
        if (lo != null && hi != null) {
            lo.setAccessible(true);
            hi.setAccessible(true);
            try {
                return ((Integer) lo.get(null) + (Integer) hi.get(null)) / 2.0F;
            } catch (IllegalAccessException e) {
                return -1.0F;
            }
        }
        return -1.0F;
    }

    static int intField(Class<?> c, String name) throws Exception {
        Field f = findField(c, name);
        if (f == null) {
            PROBLEMS.add(simple(c.getName()) + ": no " + name + " field");
            return -1;
        }
        f.setAccessible(true);
        return (Integer) f.get(null);
    }

    /** Like {@link #findField} but for an instance field, e.g. a getter's array. */
    static Field instanceField(Class<?> c, String name) throws NoSuchFieldException {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                return k.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // keep walking up
            }
        }
        throw new NoSuchFieldException(c.getName() + "." + name);
    }

    static Field findField(Class<?> c, String name) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                if (Modifier.isStatic(f.getModifiers())) {
                    return f;
                }
            } catch (NoSuchFieldException ignored) {
                // keep walking up
            }
        }
        return null;
    }

    // ===== assertions and plumbing ===========================================

    static void eqInt(Row row, String what, int expected, int actual) {
        if (expected != actual) {
            PROBLEMS.add(simple(row.mobClass) + ": " + what + " is " + actual
                    + ", table says " + expected);
        }
    }

    static void eqFloat(Row row, String what, float expected, float actual) {
        if (Math.abs(expected - actual) > EPS) {
            PROBLEMS.add(simple(row.mobClass) + ": " + what + " is " + actual
                    + ", table says " + expected);
        }
    }

    static void gtInt(Row row, String what, int now, int before) {
        if (now <= before) {
            PROBLEMS.add(simple(row.mobClass) + ": " + what + " " + now
                    + " is not above the old " + before);
        }
    }

    static String simple(String fqcn) {
        return fqcn.substring(fqcn.lastIndexOf('.') + 1);
    }

    static String num(float v) {
        if (v < 0.0F) {
            return "-";
        }
        return v == Math.rint(v) ? String.valueOf((int) v) : String.valueOf(v);
    }

    static String num(int v) {
        return v < 0 ? "-" : String.valueOf(v);
    }

    static Row r(String band, String mobClass, String role, int oldHp, int newHp,
            float oldDamage, float newDamage, int oldArmor, int newArmor,
            int oldAggro, int newAggro) {
        Row row = new Row();
        row.band = band;
        row.mobClass = mobClass;
        row.role = role;
        row.oldHp = oldHp;
        row.newHp = newHp;
        row.oldDamage = oldDamage;
        row.newDamage = newDamage;
        row.oldArmor = oldArmor;
        row.newArmor = newArmor;
        row.oldAggro = oldAggro;
        row.newAggro = newAggro;
        return row;
    }

    static Boss b(int realm, String id, int tier, int oldHealth, int newHealth,
            float oldDamageMul, float newDamageMul) {
        Boss boss = new Boss();
        boss.realm = realm;
        boss.id = id;
        boss.tier = tier;
        boss.oldHealth = oldHealth;
        boss.newHealth = newHealth;
        boss.oldDamageMul = oldDamageMul;
        boss.newDamageMul = newDamageMul;
        return boss;
    }

    static class Row {
        String band;
        String mobClass;
        String role;
        int oldHp;
        int newHp;
        float oldDamage;
        float newDamage;
        int oldArmor;
        int newArmor;
        int oldAggro;
        int newAggro;
    }

    static class Boss {
        int realm;
        String id;
        int tier;
        int oldHealth;
        int newHealth;
        float oldDamageMul;
        float newDamageMul;
    }
}
