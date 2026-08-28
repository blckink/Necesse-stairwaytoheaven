package stairwaytoheaven.livestock;

import java.awt.Point;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.ChickenMob;
import necesse.entity.mobs.friendly.HusbandryMob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.HumanGender;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

/**
 * The Thunderquill Fowl — the Stormveil's bird, on vanilla's {@link ChickenMob}.
 *
 * <p>WHY THIS BASE. It is the game's only fowl archetype, and it brings the
 * whole egg loop with it: the {@code ChickenAI} lay-egg node, the nest search,
 * the laying animation and particles, the chick growth timer. A Thunderquill
 * hen therefore lays real eggs in a Skyreach coop, which is the only egg source
 * above the clouds.
 *
 * <p>TWO THINGS THE BASE CLASS CANNOT GIVE A MOD, AND WHAT IS DONE ABOUT THEM:
 *
 * <ul>
 *   <li><b>The egg is hardcoded.</b> [jar 1.3.2]
 *       {@code ChickenMob.ChickenLayEggAINode.tickNode} builds its
 *       {@code ProcessObjectHandler} inline and its {@code process()} is
 *       {@code new InventoryItem("egg")}; {@code EggNestObject.getLayEggHandler}
 *       does the same. There is no hook between them, so a mod bird lays
 *       vanilla eggs and nothing else. That is kept — a renewable vanilla egg
 *       in the sky is worth having — and the bird's OWN product is taken with
 *       shears instead: {@code ShearsItem.canMobInteract} is
 *       {@code mob instanceof HusbandryMob && canShear(item)}, and
 *       {@code canShear}/{@code onShear} are open hooks on
 *       {@code HusbandryMob} itself, not on SheepMob. So plucking is the
 *       vanilla shearing mechanism applied to a bird, on the same
 *       grow-back-over-20-to-30-minutes timer vanilla's fleece uses.</li>
 *   <li><b>Chicken breeding cannot breed a mod bird.</b>
 *       {@code ChickenMob.onImpregnated} does not give birth — it sets
 *       {@code nextEggIsFertilized}, the AI then lays that egg in an
 *       {@code EggNestObjectInterface}, and the nest hatches whatever
 *       {@code EggItemInterface.getHatchMobStringID} says. For vanilla's egg
 *       that is {@code "chicken"} or {@code "rooster"}
 *       ({@code EggFoodConsumableItem}), so a Thunderquill pair would breed
 *       ordinary chickens. Ours gives birth live instead, which is what
 *       {@code HusbandryMob.onImpregnated} does for every other animal in the
 *       game — see {@link SkyBreed#birthLiveYoung}.</li>
 * </ul>
 */
public class ThunderquillMob extends ChickenMob {

    /** Recoloured vanilla mobs/chicken, mobs/rooster, mobs/chick. */
    public static GameTexture henTexture;
    public static GameTexture cockTexture;
    public static GameTexture chickTexture;
    /** Washed-out copies of the two adult sheets: vanilla has no plucked bird. */
    public static GameTexture henPluckedTexture;
    public static GameTexture cockPluckedTexture;

    /** Same window vanilla's fleece uses (SheepMob.onShear): 20-30 in-game minutes. */
    public static final int PLUCK_COOLDOWN_MIN = 1200000;
    public static final int PLUCK_COOLDOWN_MAX = 1800000;

    public static final LootTable thunderquillLoot = new LootTable(
            LootItem.between("rawchickenleg", 1, 2), LootItem.between(SkyLivestock.STORM_DOWN, 1, 2));

    protected int sex = SkyBreed.UNSET;
    /** World time at which the plumage has grown back. Mirrors SheepMob.nextShearTime. */
    public long nextPluckTime;

    @Override
    public void init() {
        super.init();
        this.sex = SkyBreed.rollIfUnset(this.sex, this);
    }

    @Override
    public void addSaveData(SaveData save) {
        super.addSaveData(save);
        save.addInt("skySex", this.sex);
        save.addLong("nextPluckTime", this.nextPluckTime);
    }

    @Override
    public void applyLoadData(LoadData save) {
        super.applyLoadData(save);
        this.sex = save.getInt("skySex", this.sex, false);
        this.nextPluckTime = save.getLong("nextPluckTime", 0L, false);
    }

    @Override
    public void setupSpawnPacket(PacketWriter writer) {
        super.setupSpawnPacket(writer);
        writer.putNextByteUnsigned(this.sex);
    }

    @Override
    public void applySpawnPacket(PacketReader reader) {
        super.applySpawnPacket(reader);
        this.sex = reader.getNextByteUnsigned();
    }

    /** The plumage state has to reach every client, or the plucked sprite never shows. */
    @Override
    public void setupMovementPacket(PacketWriter writer) {
        super.setupMovementPacket(writer);
        writer.putNextLong(this.nextPluckTime);
    }

    @Override
    public void applyMovementPacket(PacketReader reader, boolean isDirect) {
        super.applyMovementPacket(reader, isDirect);
        this.nextPluckTime = reader.getNextLong();
    }

    @Override
    public HumanGender getGender() {
        return SkyBreed.gender(this.sex);
    }

    @Override
    public boolean canImpregnateMob(HusbandryMob other) {
        return other.getStringID().equals(this.getStringID());
    }

    @Override
    public String getRandomChildMobStringID(HusbandryMob father) {
        return this.getStringID();
    }

    /** {@code ChickenMob.getLocalization} returns vanilla's {@code mob.chick} for a young bird. */
    @Override
    public GameMessage getLocalization() {
        return new LocalMessage("mob", this.getStringID());
    }

    @Override
    public LootTable getLootTable() {
        return !this.isGrown() ? new LootTable() : thunderquillLoot;
    }

    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkyBreed.validPastureSpawn(this, client, targetX, targetY);
    }

    /** Live birth instead of vanilla's fertilized-egg-in-a-nest path (see the class comment). */
    @Override
    public void onImpregnated(HusbandryMob father) {
        SkyBreed.birthLiveYoung(this, father);
    }

    public boolean hasPlumage() {
        return this.nextPluckTime <= this.getWorldEntity().getWorldTime();
    }

    /**
     * Same three conditions vanilla puts on shearing a sheep: grown, the crop
     * has grown back, and the animal is not still on a trader's stand
     * ({@code buyPrice == null}).
     */
    @Override
    public boolean canShear(InventoryItem item) {
        return this.isGrown() && this.hasPlumage() && this.buyPrice == null;
    }

    @Override
    public InventoryItem onShear(InventoryItem item, List<InventoryItem> products) {
        this.nextPluckTime = this.getWorldEntity().getWorldTime()
                + (long) GameRandom.globalRandom.getIntBetween(PLUCK_COOLDOWN_MIN, PLUCK_COOLDOWN_MAX);
        int amount = GameRandom.globalRandom.getIntBetween(1, 3);
        for (int i = 0; i < amount; i++) {
            products.add(new InventoryItem(SkyLivestock.STORM_DOWN));
        }
        if (this.isClient()) {
            SoundManager.playSound(GameResources.shears, SoundEffect.effect(this).volume(0.4F));
        }
        this.sendMovementPacket(false);
        return item;
    }

    @Override
    protected void addHoverTooltips(ListGameTooltips tooltips, boolean debug) {
        super.addHoverTooltips(tooltips, debug);
        tooltips.add(Localization.translate("misc", "thunderquilltip"));
    }

    @Override
    protected void addDebugTooltips(ListGameTooltips tooltips) {
        super.addDebugTooltips(tooltips);
        if (this.isGrown()) {
            tooltips.add("Plumage grown in: "
                    + GameUtils.getTimeStringMillis(this.nextPluckTime - this.getWorldTime()));
        }
    }

    private GameTexture bodyTexture() {
        if (!this.isGrown()) {
            return chickTexture;
        }
        boolean male = SkyBreed.isMale(this.sex) && cockTexture != null;
        if (this.hasPlumage()) {
            return male ? cockTexture : henTexture;
        }
        GameTexture plucked = male ? cockPluckedTexture : henPluckedTexture;
        return plucked != null ? plucked : (male ? cockTexture : henTexture);
    }

    private GameTexture shadowTexture() {
        if (!this.isGrown()) {
            return MobRegistry.Textures.chick_shadow;
        }
        return SkyBreed.isMale(this.sex) ? MobRegistry.Textures.rooster_shadow
                : MobRegistry.Textures.chicken_shadow;
    }

    @Override
    public void spawnDeathParticles(float knockbackX, float knockbackY) {
        GameTexture texture = this.bodyTexture();
        if (texture == null) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            this.getLevel().entityManager.addParticle(
                    new FleshParticle(this.getLevel(), texture, GameRandom.globalRandom.nextInt(5), 8, 32,
                            this.x, this.y, 10.0F, knockbackX, knockbackY),
                    Particle.GType.IMPORTANT_COSMETIC);
        }
    }

    @Override
    public void addDrawables(List<MobDrawable> list, OrderableDrawables tileList, OrderableDrawables topList,
            Level level, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        GameTexture texture = this.bodyTexture();
        if (texture == null) {
            return;
        }
        GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
        int drawX = camera.getDrawX(x) - 22 - 10;
        int drawY = camera.getDrawY(y) - 44 - 7;
        int dir = this.getDir();
        Point sprite = this.getAnimSprite(x, y, dir);
        drawY += this.getBobbing(x, y);
        TextureDrawOptions shadow = this.shadowTexture().initDraw().sprite(0, dir, 64).light(light).pos(drawX, drawY);
        tileList.add(tm -> shadow.draw());
        drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
        // Vanilla reuses the swim mask to make a laying hen sink into her nest;
        // keeping it is what makes the inherited egg animation still read.
        float eggProgress = this.layEggHandler.getProgressPercent();
        final MaskShaderOptions swimMask = eggProgress > 0.0F
                ? this.getSwimMaskShaderOptions(Math.min(eggProgress * 2.0F, 0.7F))
                : this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
        final DrawOptions options = texture
                .initDraw()
                .sprite(sprite.x, sprite.y, 64)
                .startGlowOptions(level, (long) this.getID())
                .light(light)
                .applyEnemyTracker(this, perspective)
                .addMaskShader(swimMask)
                .pos(drawX, drawY);
        list.add(new MobDrawable() {
            @Override
            public void draw(TickManager tickManager) {
                swimMask.use();
                options.draw();
                swimMask.stop();
            }
        });
    }
}
