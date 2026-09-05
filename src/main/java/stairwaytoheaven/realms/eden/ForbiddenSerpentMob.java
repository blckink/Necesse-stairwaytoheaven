package stairwaytoheaven.realms.eden;

import java.awt.Color;
import java.awt.Rectangle;

import necesse.engine.registries.BuffRegistry;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedCollisionPlayerChaserWandererAI;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.particle.Particle;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;

/**
 * Forbidden Serpent — the elite, and the thing that lives near the Knowledge
 * Trees (§5: <i>"Forbidden Serpent — elite, near Knowledge Trees"</i>).
 *
 * <p>A3.3 makes it the realm's difficulty gradient rather than its boss:
 * <i>"Around the Knowledge Tree, snakes grow more common and rare resources lie
 * about — a soft difficulty gradient that needs no gate."</i> There is no gate
 * here and no arena; the ground around a Knowledge Tree simply spawns more
 * ({@link EdenPressure#KNOWLEDGE_TICKETS}) and the Knowledge Grove POI has one
 * of these standing over its cache
 * ({@link EdenCanopyBiome#getGuard()}).
 *
 * <p><b>Borrowed art:</b> vanilla {@code mobs/dragonwhelp} — a crimson winged
 * serpent, 448x320, i.e. seven 64px columns over five rows. Chosen because it
 * is the only vanilla sheet that reads as a serpent AND is unmistakably
 * different from the Eden Serpent's green crocodile at a glance; an elite that
 * looks like the standard enemy is a health bar, not an encounter. Drawn with
 * {@code PetDragonWhelpMob.addDrawables}' own offsets ({@code drawX - 32},
 * {@code drawY - 44}). NOT subclassed: that class is a player pet.
 *
 * <p><b>Tier: Eden floor, ELITE role</b> ({@link EdenTiers}) —
 * {@code 1500 x 1.40} = <b>2100 HP</b>, damage and armour unchanged at
 * <b>165 / 45</b> (the ladder gives an elite bulk, not a bigger hit; see
 * {@code SkyMobTiers}'s ROLES paragraph). Its poison lasts twice as long as the
 * common serpent's, which is the elite's real teeth.
 */
public class ForbiddenSerpentMob extends EdenHostileMob {

    /** Loaded in {@link EdenRealm#loadTextures()} from the GAME's own resources. */
    public static GameTexture texture;

    /** Eden floor 1500 x 1.40 (elite role) = 2100 on CLASSIC. */
    public static final MaxHealthGetter MAX_HEALTH =
            EdenTiers.health(stairwaytoheaven.mobs.SkyMobTiers.ROLE_ELITE_HP);
    /** Eden floor damage, unmodified: an elite is bulk, not a bigger hit. */
    public static final GameDamage DAMAGE = EdenTiers.damage();
    public static final int ARMOR = EdenTiers.EDEN_ARMOR;

    /**
     * Drops, at Eden's x1.3 drop value.
     *
     * <p>The Knowledge Cutting is the point of killing one: it is how a player
     * takes a Knowledge Tree home, and outside a cache at 6% this is the only
     * source in the realm. A4.5's rule — a resource is scarce because something
     * wants it — is satisfied by the sink rather than by the rate.
     */
    public static LootTable lootTable = new LootTable(
            new ChanceLootItemList(0.90F, LootItem.between("serpentscale",
                    EdenTiers.drop(3), EdenTiers.drop(5))),
            new ChanceLootItemList(0.60F, LootItem.between("venomfang",
                    EdenTiers.drop(1), EdenTiers.drop(2))),
            new ChanceLootItemList(0.25F, LootItem.between("knowledgecutting", 1, 1)));

    public ForbiddenSerpentMob() {
        super(EdenTiers.hp(stairwaytoheaven.mobs.SkyMobTiers.ROLE_ELITE_HP));
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
        this.setSpeed(52.0F);
        this.setFriction(3.0F);
        this.setKnockbackModifier(0.1F);
        this.collision = new Rectangle(-12, -8, 24, 16);
        this.hitBox = new Rectangle(-18, -20, 36, 34);
        this.selectBox = new Rectangle(-20, -46, 40, 52);
    }

    @Override
    public void init() {
        super.init();
        this.canDespawn = false;
        this.ai = new BehaviourTreeAI<>(this,
                new ConfusedCollisionPlayerChaserWandererAI<ForbiddenSerpentMob>(null, 640, DAMAGE, 150, 40000) {
                    @Override
                    public boolean attackTarget(ForbiddenSerpentMob mob, Mob target) {
                        boolean hit = super.attackTarget(mob, target);
                        if (hit) {
                            // Twelve seconds against the common serpent's six.
                            target.buffManager.addBuff(
                                    new ActiveBuff(BuffRegistry.Debuffs.GENERIC_POISON, target, 12.0F, mob),
                                    target.isServer());
                        }
                        return hit;
                    }
                });
    }

    /**
     * It glides. Low enough that it still reads as a ground fight — an elite
     * that floats over every obstacle would simply be unavoidable — and high
     * enough that the wings on the borrowed sheet are not a lie.
     */
    @Override
    public int getFlyingHeight() {
        return 12;
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
        return 64;
    }

    @Override
    protected int drawOffsetX() {
        return -32;
    }

    @Override
    protected int drawOffsetY() {
        // PetDragonWhelpMob.addDrawables:69 — `camera.getDrawY(y) - 44`.
        return -44;
    }

    @Override
    public void spawnDeathParticles(float knockbackX, float knockbackY) {
        for (int i = 0; i < 20; i++) {
            this.getLevel().entityManager
                    .addParticle(this.x, this.y, Particle.GType.IMPORTANT_COSMETIC)
                    .movesConstant(EdenSerpentMob.deathSpeed(knockbackX), EdenSerpentMob.deathSpeed(knockbackY))
                    .color(new Color(186, 42, 38));
        }
    }


    /**
     * Bestiary face: it wears mobs/dragonwhelp, which petdragonwhelp owns (EdenRealm.loadTextures), so it wears that creature's
     * face in the journal too. {@code Mob.getMobIcon()} is overridable and
     * {@code FormJournalEntryComponent} asks the MOB rather than the registry,
     * so this needs no PNG of its own -- see {@link stairwaytoheaven.mobs.BorrowedMobIcon}
     * for why borrowing the face is the right answer and not a shortcut.
     */
    @Override
    public necesse.gfx.gameTexture.GameTexture getMobIcon() {
        return stairwaytoheaven.mobs.BorrowedMobIcon.from("petdragonwhelp", super.getMobIcon());
    }

}
