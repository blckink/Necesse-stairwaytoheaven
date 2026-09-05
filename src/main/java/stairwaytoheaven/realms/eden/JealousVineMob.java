package stairwaytoheaven.realms.eden;

import java.awt.Color;
import java.awt.Rectangle;

import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedCollisionPlayerChaserWandererAI;
import necesse.entity.particle.Particle;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;

/**
 * Jealous Vine — the thing that comes out of the undergrowth (§5: <i>"Jealous
 * Vine — attacks out of vegetation"</i>).
 *
 * <p>It belongs to the canopy, where the ground is roots and the light is thin,
 * and it is the reason walking between the giant trunks is not the same as
 * crossing a meadow.
 *
 * <p><b>Borrowed art:</b> vanilla {@code mobs/dryadsentinel} — an amber crown
 * over a mass of dark tendrils, which is a living plant seen from above and
 * needs no reinterpretation to be a vine. 768x896: six 128px columns over
 * seven rows. Drawn with {@code DryadSentinelMob.addDrawables}' own offsets
 * ({@code drawX - 64}, {@code drawY - 112 + 20}). NOT subclassed: the vanilla
 * sentinel's damage lives inside an anonymous AI it builds in {@code init()}
 * together with a root-spike ability chain, and rebuilding half of that to
 * change one number is more fragile than writing the chaser outright.
 *
 * <p><b>Tier: Eden floor, standard role</b> ({@link EdenTiers}) —
 * <b>1500 HP / 165 damage / 45 armour</b> on CLASSIC. Vanilla's own Dryad
 * Sentinel is 1000 HP / 60 damage / 25 armour and is untouched.
 *
 * <p><b>Why it is not the elite</b> even though its sheet is the biggest in the
 * roster: Eden's elite has to be the thing near the Knowledge Tree (A3.3), and
 * that is the Forbidden Serpent. A vine that hit elite numbers as well would
 * make the canopy two elites deep, which is exactly the "everything is a boss"
 * failure the balance pass is written against.
 */
public class JealousVineMob extends EdenHostileMob {

    /** Loaded in {@link EdenRealm#loadTextures()} from the GAME's own resources. */
    public static GameTexture texture;

    public static final MaxHealthGetter MAX_HEALTH = EdenTiers.health();
    public static final GameDamage DAMAGE = EdenTiers.damage();
    public static final int ARMOR = EdenTiers.EDEN_ARMOR;

    /**
     * Drops, at Eden's x1.3 drop value. Eden Sap is the plant material and the
     * vine is its most reliable source, which is what makes clearing the canopy
     * pay rather than merely cost.
     */
    public static LootTable lootTable = new LootTable(
            new ChanceLootItemList(0.85F, LootItem.between("edensap",
                    EdenTiers.drop(2), EdenTiers.drop(3))),
            new ChanceLootItemList(0.35F, LootItem.between("edenwood",
                    EdenTiers.drop(2), EdenTiers.drop(4))));

    public JealousVineMob() {
        super(EdenTiers.EDEN_HP);
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
        // Vanilla's sentinel walks at 45; a vine dragging itself along roots
        // is slower than a serpent and much harder to push.
        this.setSpeed(38.0F);
        this.setFriction(3.0F);
        this.setKnockbackModifier(0.15F);
        this.collision = new Rectangle(-14, -9, 28, 18);
        this.hitBox = new Rectangle(-22, -26, 44, 40);
        this.selectBox = new Rectangle(-24, -56, 48, 62);
    }

    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(this,
                new ConfusedCollisionPlayerChaserWandererAI<>(null, 560, DAMAGE, 140, 40000));
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    @Override
    protected GameTexture sheet() {
        return texture;
    }

    @Override
    protected int spriteSize() {
        return 128;
    }

    @Override
    protected int drawOffsetX() {
        return -64;
    }

    @Override
    protected int drawOffsetY() {
        // DryadSentinelMob.addDrawables:243 — `camera.getDrawY(y) - 112 + 20`.
        return -112 + 20;
    }

    @Override
    public void spawnDeathParticles(float knockbackX, float knockbackY) {
        for (int i = 0; i < 16; i++) {
            this.getLevel().entityManager
                    .addParticle(this.x, this.y, Particle.GType.IMPORTANT_COSMETIC)
                    .movesConstant(EdenSerpentMob.deathSpeed(knockbackX), EdenSerpentMob.deathSpeed(knockbackY))
                    .color(new Color(168, 106, 32));
        }
    }

    /**
     * Bestiary face: it wears mobs/dryadsentinel (EdenRealm.loadTextures), so it wears that creature's
     * face in the journal too. {@code Mob.getMobIcon()} is overridable and
     * {@code FormJournalEntryComponent} asks the MOB rather than the registry,
     * so this needs no PNG of its own -- see {@link stairwaytoheaven.mobs.BorrowedMobIcon}
     * for why borrowing the face is the right answer and not a shortcut.
     */
    @Override
    public necesse.gfx.gameTexture.GameTexture getMobIcon() {
        return stairwaytoheaven.mobs.BorrowedMobIcon.from("dryadsentinel", super.getMobIcon());
    }

}
