package stairwaytoheaven.realms.ghost;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedCollisionPlayerChaserWandererAI;
import necesse.entity.mobs.hostile.DesertCrawlerMob;
import necesse.inventory.lootTable.LootTable;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * Coffin Crawler — a box that was buried and has changed its mind, dragging
 * itself through the grave soil with the earth heaping up behind it.
 *
 * <p><b>Vanilla base:</b> {@link DesertCrawlerMob}, art
 * {@code mobs/desertcrawler} plus the {@code mound1..3} sheets. Subclassing
 * keeps the one thing that makes it read as something coming up out of the
 * ground rather than walking across it: the crawler carries a
 * {@code GroundPillarList} of mounds and adds a new one every time it moves, so
 * it leaves a churned trail. {@code WORLD_DESIGN} §10 asks for "a coffin with
 * legs", and this is the game's own body for a long thing that scuttles and
 * disturbs the soil.
 *
 * <h2>Tier</h2>
 * Ghost Realm row, no role discount: <b>2800 HP</b>, <b>230 damage</b>,
 * <b>55 armour</b> ({@code docs/BALANCE.md} §5). Vanilla's crawler is a desert
 * mob at 350 HP / 90 damage / 20 armour and stays exactly that.
 *
 * <h2>Why super.init() is called before the tree is replaced</h2>
 * Vanilla's {@code init()} does two separable things: it builds the AI against
 * a static {@code baseDamage} shared with every desert crawler in the game, and
 * it seeds the mob's first three mounds through {@code addNewMound}, which is
 * <b>private</b> and therefore unreachable from a subclass. Rebuilding
 * {@code init()} from scratch would silently ship a crawler with no mounds
 * until it had moved. So {@code super.init()} runs whole — mounds and all —
 * and only {@code this.ai} is replaced afterwards, with vanilla's own tree
 * (512 search, 100 knockback, 40s wander) rebuilt against our damage.
 */
public class CoffinCrawlerMob extends DesertCrawlerMob {

    /**
     * Ghost Realm row = <b>2800 HP</b> on Classic, spread on
     * {@code AscendedGolemMob.MAX_HEALTH}'s measured ratios (VERIFIED [jar]).
     * Vanilla's crawler is 350.
     */
    public static final MaxHealthGetter MAX_HEALTH =
            new MaxHealthGetter(1120, 2100, 2800, 3640, 5040);

    /** Ghost Realm row = <b>230 damage</b>. Vanilla's crawler hits for 90. */
    public static final GameDamage DAMAGE = new GameDamage(230.0F);

    /** Ghost Realm row = <b>55 armour</b>. Vanilla's crawler wears 20. */
    public static final int ARMOR = 55;

    /** Vanilla's crawler drops nothing; this one was buried with grave goods. */
    public static LootTable lootTable = GhostLoot.ambusher();

    public CoffinCrawlerMob() {
        super();
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
    }

    @Override
    public void init() {
        super.init();
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
}
