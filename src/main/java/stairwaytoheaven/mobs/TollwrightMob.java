package stairwaytoheaven.mobs;

import java.util.stream.Stream;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.util.TargetValidity;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.hostile.AshGolemMob;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;

/**
 * The Tollwright — the customs golem of the Skyway Toll-House, still doing its
 * job with nobody left to bill.
 *
 * <p>{@code chapter-01-skyreach-cast.md} §2: <i>"bruiser — slow, armoured,
 * telegraphed slam, owns a room rather than chasing"</i>, built on
 * <i>"vanilla's armoured golem melee archetype ({@code AshGolemMob}), the same
 * family the Skystone Golem answers to"</i>. One of the three POI-exclusive
 * enemies; it never spawns on the open map, and
 * {@code SkyLandmarkPois.seatGuards} is the only thing that places it.
 *
 * <h2>The art: none</h2>
 * The dossier's §1.3 pattern — subclass the vanilla mob whose behaviour you
 * want and wear that mob's own sheet. Nothing here overrides
 * {@code addDrawables} or {@code spawnDeathParticles}, so the Tollwright is
 * drawn and shattered by {@code AshGolemMob}'s own code out of
 * {@code MobRegistry.Textures.ashGolem}, and it costs zero new pixels.
 * {@link #getMobIcon} then gives the bestiary the ash golem's face rather than
 * the engine's ERR tile — see {@link BorrowedMobIcon} for why that is the right
 * icon and not a missing one. The row is in {@code docs/VANILLA_ASSET_MAP.md}.
 *
 * <h2>Two deliberate departures from the archetype</h2>
 * <ul>
 *   <li><b>It cannot break objects.</b> VERIFIED [jar]:
 *       {@code AshGolemMob.getDefaultModifiers} returns
 *       {@code CAN_BREAK_OBJECTS true} plus a fire immunity, and
 *       {@code getPathBreakDownDamage} multiplies the base by twelve — an ash
 *       golem walks through walls on its way to you. That is correct in the
 *       open Ashen Reach and wrong inside a 19x13 building that also holds
 *       Magpie and two display stands: the first player to lure it across the
 *       hall would flatten the POI. The fire immunity is kept.</li>
 *   <li><b>It does not shoot.</b> {@code AshGolemAI}'s first constructor
 *       argument is the shoot distance and passing 0 leaves the
 *       {@code AshenWaveProjectile} node out entirely (AshGolemMob.java:201).
 *       §2 asks for a slam, not a wave, and an ash wave in a customs office
 *       reads as the wrong realm.</li>
 * </ul>
 *
 * <p>Everything that makes it own a room instead of chasing is vanilla's and
 * is inherited: {@code AshGolemAI} bases both its target finder and its
 * wanderer on {@code spawnTilePosition}, which is why the placer sets that
 * field before adding the mob.
 *
 * <h2>Tier</h2>
 * The Skyreach's ELITE rung, the same one {@link SkystoneGolemMob} stands on:
 * {@code SkyMobTiers.hp(SKYREACH_HP, ROLE_ELITE_HP)} health at the band's
 * unreduced damage and armour. Vanilla's ash golem is a Hell-tier 3000 HP /
 * 160 collision damage / 100 armour and stays exactly that; only this subclass
 * moves.
 */
public class TollwrightMob extends AshGolemMob {

    /** Skyreach elite rung on Classic, with vanilla's own five-difficulty ratios. */
    public static final MaxHealthGetter MAX_HEALTH = SkyMobTiers.scaled(
            SkyMobTiers.hp(SkyMobTiers.SKYREACH_HP, SkyMobTiers.ROLE_ELITE_HP));

    /** The elite role takes the band's damage unchanged. Vanilla's slam is 160. */
    public static final GameDamage DAMAGE = new GameDamage(SkyMobTiers.SKYREACH_DAMAGE);

    /** The band's armour. Vanilla's ash golem wears 100. */
    public static final int ARMOR = SkyMobTiers.SKYREACH_ARMOR;

    /**
     * How far it notices somebody, i.e. {@code AshGolemAI}'s search distance:
     * the measured 512 x1.25 = 640 (docs/BALANCE.md §10). Melee reach stays
     * vanilla's 64.
     */
    public static final int AGGRO_RANGE = SkyMobTiers.aggro(512, SkyMobTiers.UPLIFT_SKYREACH_AGGRO);

    /**
     * Stormsteel-band scrap and tollplate, as §2's drop column asks. The
     * Bonded Lockbox is NOT here: §2.12's object table puts it on the display
     * stand at (16,11), on the vault floor the Tollwright is standing on, and
     * {@code SkyLandmarkPois} fills that stand.
     */
    public static LootTable lootTable = new LootTable(
            LootItem.between("skystone", 3, 6),
            new ChanceLootItemList(0.65F, LootItem.between("stormsteelbar", 1, 3)),
            new ChanceLootItemList(0.40F, LootItem.between("aetheriumbar", 1, 2)),
            ChanceLootItem.between(0.80F, "coin", 400, 1200));

    public TollwrightMob() {
        super();
        // In the constructor, like every other rung of the ladder:
        // MobDifficultyChanges throws if it is touched after init().
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
        // Public instance fields on AshGolemMob, so the slam is simply
        // reassigned; the ash wave is switched off in init() instead.
        this.collisionDamage = DAMAGE;
    }

    @Override
    public void init() {
        super.init();
        AshGolemMob.AshGolemAI<TollwrightMob> tree =
                new AshGolemMob.AshGolemAI<>(0, 64, AGGRO_RANGE);
        // It bills PLAYERS. VERIFIED [jar]:
        // TargetFinderAINode.streamPlayersAndHumans (:253) accepts any visible
        // human on a team, and Magpie is standing 7 tiles away in the ledger
        // room — so out of the box the Tollwright leaves the vault, chases
        // her, and cannot ever finish, because SkySettlerMob.canTakeDamage is
        // false. What the player would then find is a golem in the wrong room
        // and a courier who has been knocked across it. `validity` is a public
        // mutable field on the node (TargetFinderAINode.java:24), which is the
        // one seam that does not need vanilla's tree rebuilt around it.
        tree.targetFinderNode.validity = new TargetValidity<TollwrightMob>() {
            @Override
            public boolean isValidTarget(AINode<TollwrightMob> node, TollwrightMob mob,
                    Mob target, boolean isNewTarget) {
                return target != null && target.isPlayer
                        && super.isValidTarget(node, mob, target, isNewTarget);
            }
        };
        this.ai = new BehaviourTreeAI<>(this, tree);
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    /**
     * Vanilla's fire immunity, without vanilla's licence to demolish the
     * building it is standing in. See the class comment.
     */
    @Override
    public Stream<ModifierValue<?>> getDefaultModifiers() {
        return Stream.of(new ModifierValue<>(BuffModifiers.FIRE_DAMAGE, 0.0F).max(0.0F));
    }

    /** The face of the creature whose body it wears. See {@link BorrowedMobIcon}. */
    @Override
    public GameTexture getMobIcon() {
        return BorrowedMobIcon.from("ashgolem", super.getMobIcon());
    }
}
