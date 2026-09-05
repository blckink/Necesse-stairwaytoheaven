package stairwaytoheaven.realms.steinfeld.mobs;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedCollisionPlayerChaserWandererAI;
import necesse.entity.mobs.ai.behaviourTree.util.FlyingAIMover;
import necesse.entity.mobs.hostile.DeepCaveSpiritMob;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import stairwaytoheaven.mobs.SkyMobTiers;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * Lost Pilgrim — a ghost fragment, {@code docs/WORLD_DESIGN.md} §7's first
 * named resident of the Reach. It drifts through the Quiet Meadow before
 * anything else does, which is deliberate: this is the first thing that tells
 * the player these are PEOPLE, not monsters.
 *
 * <h2>Vanilla base</h2>
 * {@link DeepCaveSpiritMob}, art {@code mobs/deepcavespirit} — a small hooded,
 * translucent, floating figure with two lit eyes. It is the same body the
 * Aftergarden's Drifter wears one rung further up the stairway, and that is
 * not a coincidence: both realms' ordinary dead are the mod's one "person who
 * has stopped being alive but kept the shape" sprite, and the Reach is where
 * the player meets it first. Subclassing keeps the flight, the translucency
 * and vanilla's own ectoplasm drop unchanged; only the numbers move.
 *
 * <h2>Tier: Steinfeld row, FAST role</h2>
 * {@link SteinfeldTier}: realm row 2100 / 200 / 50, FAST role
 * {@code x0.60} HP / {@code x0.80} damage = <b>1260 HP / 160 damage / 50
 * armour</b> — the exact line {@code SteinfeldTier}'s own class comment
 * prints for this mob. Vanilla's spirit is 225 HP / 65 damage / 20 armour and
 * is left untouched; only this subclass is retuned.
 */
public class LostPilgrimMob extends DeepCaveSpiritMob {

    /** Steinfeld row 2100 x 0.60 (fast role) = 1260 on Classic. */
    public static final MaxHealthGetter MAX_HEALTH =
            SteinfeldTier.health(SkyMobTiers.ROLE_FAST_HP);
    /** Steinfeld row 200 x 0.80 (fast role) = 160. */
    public static final GameDamage DAMAGE =
            SteinfeldTier.damage(SkyMobTiers.ROLE_FAST_DAMAGE);
    /** The realm's armour, unreduced — armour carries no role modifier. */
    public static final int ARMOR = SteinfeldTier.ARMOR;

    /**
     * A pilgrim is what an Echo Shard comes from — {@code docs/WORLD_DESIGN.md}
     * §7 names the source directly ("Echo Shard (from ghost apparitions)") —
     * so this is the one guaranteed place to find them rather than a crate
     * rumour. Spirit Moss is the rarer half, foreshadowing the Grave Heath's
     * own resource before the player has walked that far.
     */
    public static LootTable lootTable = new LootTable(
            ChanceLootItem.between(0.40F, "echoshard", 1, 1),
            ChanceLootItem.between(0.15F, "spiritmoss", 1, 2));

    public LostPilgrimMob() {
        super();
        // Registered in construction, exactly like AscendedGolemMob registers
        // its own MAX_HEALTH: MobDifficultyChanges throws if touched after
        // init().
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
        // Faster than the Drifter's shared 35 base speed -- the fast role is
        // meant to be felt, not only priced.
        this.setSpeed(58.0F);
    }

    @Override
    public void init() {
        super.init();
        // DeepCaveSpiritMob.init() rebuilds an incursion-conditional AI of its
        // own that this replaces outright, the same way DrifterMob does: same
        // 448 search, same 100 knockback, same 40s wander, same FlyingAIMover,
        // against OUR damage rather than vanilla's shared baseDamage field.
        this.ai = new BehaviourTreeAI<>(this,
                new ConfusedCollisionPlayerChaserWandererAI<>(null, 448, DAMAGE, 100, 40000),
                new FlyingAIMover());
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    /**
     * Same rule the rest of the mod's residents use: a torch-lit camp still
     * protects the player, but the Reach does not go quiet just because the
     * sun is up. See {@link SkySpawnRules}.
     */
    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkySpawnRules.daylightSpawn(this, server, client, targetX, targetY);
    }

    /**
     * Bestiary face: it subclasses DeepCaveSpiritMob, so it wears that creature's
     * face in the journal too. {@code Mob.getMobIcon()} is overridable and
     * {@code FormJournalEntryComponent} asks the MOB rather than the registry,
     * so this needs no PNG of its own -- see {@link stairwaytoheaven.mobs.BorrowedMobIcon}
     * for why borrowing the face is the right answer and not a shortcut.
     */
    @Override
    public necesse.gfx.gameTexture.GameTexture getMobIcon() {
        return stairwaytoheaven.mobs.BorrowedMobIcon.from("deepcavespirit", super.getMobIcon());
    }

}
