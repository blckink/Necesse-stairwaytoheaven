package stairwaytoheaven.realms.ghost;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedCollisionPlayerChaserWandererAI;
import necesse.entity.mobs.hostile.JackalMob;
import necesse.inventory.lootTable.LootTable;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * Soul Hound — somebody's dog, still waiting, and it does not know the
 * difference between you and the person it is waiting for.
 *
 * <p><b>Vanilla base:</b> {@link JackalMob}, art {@code mobs/jackal}: a lean
 * four-legged runner at speed 40 with low knockback resistance and a tight
 * collision box. It is the fastest ordinary land mob in the game's own roster,
 * which is exactly the role the Aftergarden's table needs and the role
 * {@code GloomfenBiome} had to admit the Veil could not fill at all.
 *
 * <h2>Tier</h2>
 * Ghost Realm row with the FAST discount ({@code docs/BALANCE.md} §6: HP x0.6,
 * damage x0.8): 2800 x 0.6 = <b>1680 HP</b> and 230 x 0.8 = <b>184 damage</b>,
 * at the row's full <b>55 armour</b>. Vanilla's jackal is a desert mob at 200
 * HP / 44 damage / 10 armour and stays exactly that.
 *
 * <p>The discount is the whole point of the role table: something that closes
 * distance at speed 40 and hits for 184 is a different problem from something
 * that walks at 25 and hits for 230, and giving the runner the full row would
 * make it strictly better than the bruiser instead of different from it.
 * Vanilla's own shape behind that — {@code CrystalGolemMob} is the slow
 * 130-damage anchor at speed 20 while {@code AscendedBatMob} (speed 175) and
 * the rolling {@code CrystalArmadillo} (speed 200) both drop to 90.
 */
public class SoulHoundMob extends JackalMob {

    /**
     * Ghost Realm row x0.6 (fast) = <b>1680 HP</b> on Classic, spread across
     * the difficulties on {@code AscendedGolemMob.MAX_HEALTH}'s measured
     * ratios (VERIFIED [jar]). Vanilla's jackal is 200.
     */
    public static final MaxHealthGetter MAX_HEALTH =
            new MaxHealthGetter(672, 1260, 1680, 2184, 3024);

    /** Ghost Realm row x0.8 (fast) = <b>184 damage</b>. Vanilla's is 44. */
    public static final GameDamage DAMAGE = new GameDamage(184.0F);

    /** Ghost Realm row = <b>55 armour</b>. Vanilla's jackal wears 10. */
    public static final int ARMOR = 55;

    /**
     * Vanilla's jackal drops NOTHING at all — its loot table is empty, because
     * on a desert island it is scenery with teeth. In the Aftergarden it is a
     * tier-7 kill, so it pays like one.
     */
    public static LootTable lootTable = GhostLoot.standard();

    public SoulHoundMob() {
        super();
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
    }

    @Override
    public void init() {
        super.init();
        // Vanilla's own tree (512 search, 100 knockback, 40s wander) against our
        // damage; JackalMob.init builds `new GameDamage(44.0F)` as a local
        // inside the constructor call, so there is no field to write through.
        this.ai = new BehaviourTreeAI<>(this,
                new ConfusedCollisionPlayerChaserWandererAI<>(null, 512, DAMAGE, 100, 40000));
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkySpawnRules.daylightSpawn(this, server, client, targetX, targetY);
    }

    /**
     * Bestiary face: it subclasses JackalMob, so it wears that creature's
     * face in the journal too. {@code Mob.getMobIcon()} is overridable and
     * {@code FormJournalEntryComponent} asks the MOB rather than the registry,
     * so this needs no PNG of its own -- see {@link stairwaytoheaven.mobs.BorrowedMobIcon}
     * for why borrowing the face is the right answer and not a shortcut.
     */
    @Override
    public necesse.gfx.gameTexture.GameTexture getMobIcon() {
        return stairwaytoheaven.mobs.BorrowedMobIcon.from("jackal", super.getMobIcon());
    }

}
