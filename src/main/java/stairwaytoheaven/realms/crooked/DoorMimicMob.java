package stairwaytoheaven.realms.crooked;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.hostile.MimicMob;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import stairwaytoheaven.mobs.SkyMobTiers;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * Door Mimic — {@code WORLD_DESIGN.md} §14's <i>"not livestock; looks like a
 * door; enemy"</i>, and the only guard in Crooked Beyond that you walk up to on
 * purpose.
 *
 * <h2>Vanilla base: {@link MimicMob}</h2>
 * Every behaviour is inherited: it sits {@code isDisguised} and completely
 * inert, shakes the camera when you get close, drops the disguise when you are
 * within reach, and chases on collision. That IS the design §14 describes, and
 * it is the reason this realm's guard packs can put an anchor right on top of
 * the loot without the player seeing it coming.
 *
 * <h2>Where its numbers come from</h2>
 * The realm row, {@link SkyMobTiers}: Crooked Beyond is incursion tier ~10, i.e.
 * <b>4000 HP / 280 damage / 60 armour</b>, and this is the realm's ELITE, so it
 * takes {@code ROLE_ELITE_HP} x1.40 = <b>5600 HP</b> at the row's full damage
 * and armour. <b>VERIFIED [jar]</b> for the floor those multipliers act on:
 * {@code AscendedGolemMob.MAX_HEALTH} is {@code MaxHealthGetter(400, 750, 1000,
 * 1300, 1800)} — 1000 on Classic — and {@code CrystalGolemMob} is
 * {@code GameDamage(130)} behind {@code setArmor(40)}; the tier-10 curves summed
 * out of {@code BiomeMissionIncursionData} are x4.00 health and x2.15 damage.
 *
 * <p>Vanilla's own mimic is {@code super(600)}, {@code setSpeed(40)},
 * {@code setArmor(20)} and stays exactly that — only this subclass moves.
 *
 * <h2>The damage is dice, not a number</h2>
 * {@code MimicMob} does not carry a {@code GameDamage}. It rolls
 * {@code damageDiceCount} = 14 dice between {@code minDamageRoll} and
 * {@code maxDamageRoll} (vanilla 14..112, i.e. an average of 63 per hit,
 * VERIFIED [jar] — the fields are public and non-final, the dice count is
 * {@code final}). Scaling that onto 280 average means multiplying both ends by
 * 280/63 = 4.444: 14 -&gt; 62 and 112 -&gt; 498, which averages 280.0 exactly.
 * Keeping the SPREAD rather than flattening it to a constant is deliberate —
 * the wide roll is what makes a mimic feel like a gamble instead of a number.
 *
 * <h2>Art</h2>
 * <b>Borrowed sheet:</b> vanilla {@code mobs/mimic.png}, inherited untouched
 * because nothing here overrides {@code addDrawables}. Looked at rather than
 * assumed: a container that unfolds into a red mouth full of teeth. It reads as
 * "a piece of furniture that was waiting", which is the joke; it does <b>not</b>
 * yet read as a DOOR, and until door art exists that is the honest state of it.
 * Recorded as a stand-in in {@code docs/realms/crooked.md}.
 */
public class DoorMimicMob extends MimicMob {

    /**
     * Crooked row 4000 HP x 1.40 (elite role) = 5600 on Classic, spread across
     * the five difficulties with the ratios of the getter the floor itself was
     * measured from ({@code AscendedGolemMob.MAX_HEALTH}: 0.40 / 0.75 / 1.00 /
     * 1.30 / 1.80). Vanilla's mimic is 600.
     */
    public static final MaxHealthGetter MAX_HEALTH = SkyMobTiers.scaled(
            SkyMobTiers.hp(SkyMobTiers.CROOKED_HP, SkyMobTiers.ROLE_ELITE_HP));

    /** Crooked row armour, unreduced by the role. Vanilla's mimic wears 20. */
    public static final int ARMOR = SkyMobTiers.CROOKED_ARMOR;

    /** Low end of the damage dice — see the class comment for the arithmetic. */
    public static final int MIN_DAMAGE_ROLL = 62;
    /** High end of the damage dice. 14 dice of 62..498 average 280. */
    public static final int MAX_DAMAGE_ROLL = 498;

    /**
     * What a mimic is carrying.
     *
     * <p>Vanilla's own {@code getLootTable()} hands out a {@code mimicchest}
     * plus a generated inventory, which is a vanilla-progression reward in a
     * realm that has nothing to do with it. This is the realm's own economy at
     * its drop value ({@code CROOKED_DROP_VALUE} = 2.5, the measured tier-10
     * loot figure) — and the Strange Fabric is the point: the mimic was
     * pretending to be furniture, and furniture is what it is made of.
     */
    public static final LootTable lootTable = new LootTable(
            LootItem.between("strangefabric", 4, 9),
            ChanceLootItem.between(0.60F, "realityshard", 1, 3),
            ChanceLootItem.between(0.45F, "warpresin", 3, 7),
            ChanceLootItem.between(0.25F, "eyeseed", 1, 3));

    public DoorMimicMob() {
        super();
        // MobDifficultyChanges throws if it is touched after init(), so the
        // health curve has to be set here rather than in init().
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
        this.minDamageRoll = MIN_DAMAGE_ROLL;
        this.maxDamageRoll = MAX_DAMAGE_ROLL;
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    /**
     * Daylight spawning, like every mod hostile that is meant to belong to a
     * PLACE rather than to the night.
     *
     * <p>{@code HostileMob.isValidSpawnLocation} measures ambient + static light
     * against a threshold of 0, and on a non-cave level the ambient is 150 in
     * daylight — so not one hostile can be placed anywhere while the sun is up.
     * {@link SkySpawnRules#daylightSpawn} is vanilla's own chain with the
     * ambient check swapped for the STATIC one, so the realm stays dangerous at
     * noon and a lit camp is still safe. A mimic in particular has to be able to
     * spawn by day: it is a thing standing still in a room, and a room is
     * somewhere you go when you can see.
     */
    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkySpawnRules.daylightSpawn(this, server, client, targetX, targetY);
    }
}
