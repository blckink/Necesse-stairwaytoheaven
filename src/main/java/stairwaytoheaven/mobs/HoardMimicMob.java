package stairwaytoheaven.mobs;

import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.hostile.MimicMob;

/**
 * Hoard Mimic — the chest in a treasure room that is not a chest.
 *
 * <p>The player asked for <i>"Mimic-Truhen die geheimen Loot haben"</i>, and
 * the engine already ships exactly that mechanic. <b>VERIFIED [jar]</b>, read
 * in the decompiled {@code RandomCaveChestRoom.openingApply} (lines 109-120):
 * vanilla's own cave chest rooms take the {@code storagebox} they would have
 * placed, delete it, and put a {@code "mimic"} mob on that tile instead —
 * {@code canDespawn = false}, turned the way the chest was
 * ({@code setDir(rotation)}), with the room's loot table rolled straight into
 * {@code MimicMob.loot}. {@code MimicMob.getLootTable} (lines 196-219) then
 * drops a {@code mimicchest} plus every item in that list when it dies, and
 * {@code addSaveData}/{@code applyLoadData} (80-110) save the list with the
 * mob, so the hoard survives a restart. The places in
 * {@code docs/design/chapter-02-hoards-and-mimics.md} do the same thing, with
 * the same {@code storagebox} standing beside it as the honest version.
 *
 * <h2>Why a subclass and not vanilla's "mimic"</h2>
 * Vanilla's mimic is a deep-cave mob: {@code super(600)}, {@code setArmor(20)},
 * 14..112 damage dice. The player one-shots the Skyreach's ordinary cast
 * ({@code WORLD_DESIGN.md} §A4.1); a 600-HP chest in the room the whole POI
 * was built around would not be an ambush, it would be a speed bump. So this
 * is the Skyreach row's ELITE ({@code docs/BALANCE.md} §5/§6): 1300 x 1.40 =
 * <b>1820 HP</b> on Classic, <b>50 armour</b>, and the dice scaled to the row's
 * 149.5 average exactly the way {@code DoorMimicMob} and
 * {@code PossessedChairMob} already do it for their realms (14/63 and 112/63
 * of the row). Deeper realms put the realm's tier on top with
 * {@code BossScaling.applyTier} when the place is stamped, so there is one
 * mimic class and not five.
 *
 * <h2>What it does NOT change</h2>
 * The disguise, the three-tile wake radius ({@code MimicAI}'s 96-pixel
 * filter), the camera shake, the save format and the loot mechanism are all
 * vanilla's and are inherited untouched. It is on no spawn table: the only
 * thing in the game that places one is {@code RealmPoiHoards}.
 *
 * <h2>Art</h2>
 * Borrowed: vanilla {@code mobs/mimic.png}, inherited because nothing here
 * overrides {@code addDrawables}, and vanilla's mimic face in the bestiary via
 * {@link BorrowedMobIcon}. Row in {@code docs/VANILLA_ASSET_MAP.md}.
 */
public class HoardMimicMob extends MimicMob {

    /** Skyreach row x the elite role: 1300 x 1.40 = 1820 on Classic. */
    public static final MaxHealthGetter MAX_HEALTH = SkyMobTiers.scaled(
            SkyMobTiers.hp(SkyMobTiers.SKYREACH_HP, SkyMobTiers.ROLE_ELITE_HP));

    /** Skyreach row armour. Vanilla's mimic wears 20. */
    public static final int ARMOR = SkyMobTiers.SKYREACH_ARMOR;

    /** Low end of the 14 damage dice: the row's average x 14/63. */
    public static final int MIN_DAMAGE_ROLL = Math.round(SkyMobTiers.SKYREACH_DAMAGE * 14 / 63.0F);

    /** High end of the dice: the row's average x 112/63, so the mean is the row. */
    public static final int MAX_DAMAGE_ROLL = Math.round(SkyMobTiers.SKYREACH_DAMAGE * 112 / 63.0F);

    public HoardMimicMob() {
        super();
        // MobDifficultyChanges refuses changes after init(), so here.
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
        this.minDamageRoll = MIN_DAMAGE_ROLL;
        this.maxDamageRoll = MAX_DAMAGE_ROLL;
    }

    /** Wears the vanilla mimic's face in the journal (see {@link BorrowedMobIcon}). */
    @Override
    public necesse.gfx.gameTexture.GameTexture getMobIcon() {
        return BorrowedMobIcon.from("mimic", super.getMobIcon());
    }
}
