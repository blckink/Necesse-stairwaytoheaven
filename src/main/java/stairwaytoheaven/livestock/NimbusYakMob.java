package stairwaytoheaven.livestock;

import java.awt.Point;
import java.util.ArrayList;
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
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.CowMob;
import necesse.entity.mobs.friendly.HusbandryMob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
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
 * The Nimbus Yak — the Driftlands' milk animal, on vanilla's {@link CowMob}.
 *
 * <p>WHY THIS BASE. Of everything in {@code entity/mobs/friendly}, the cow is
 * the only archetype that carries the milking contract:
 * {@code HusbandryMob.canMilk}/{@code onMilk} are open hooks that only
 * {@code CowMob} implements, and {@code BucketItem.canMobInteract} is
 * {@code mob instanceof HusbandryMob && canMilk(item)} — so the vanilla bucket
 * milks a sky yak the same way it milks a cow, and the products list it hands
 * back is dropped as pickups by {@code BucketItem.onMobInteract} (jar 1.3.2).
 * Everything else about husbandry — roping, the feeding trough, hunger,
 * tameness, growing up, the settlement's milking job — comes with the base
 * class untouched.
 *
 * <p>WHAT IS OVERRIDDEN, AND WHY EACH ONE IS NEEDED:
 * <ul>
 *   <li><b>The product.</b> {@code CowMob.onMilk} hands out vanilla
 *       {@code milk}. Ours yields {@link SkyLivestock#NIMBUS_MILK} on exactly
 *       the same timer (super still arms {@code nextMilkTime} to the next
 *       morning, minimum two minutes).</li>
 *   <li><b>The calf.</b> {@code CowMob.getRandomChildMobStringID} is
 *       {@code getOneOf(getStringID(), "bull")}, so half of every calf born in
 *       the sky would be a plain vanilla bull. The herd breeds true.</li>
 *   <li><b>The name.</b> {@code CowMob.getLocalization} returns vanilla's
 *       {@code mob.calf} for anything not grown up, so a young yak would wear
 *       a vanilla display name. Ours is a Nimbus Yak at every age.</li>
 *   <li><b>Sex.</b> See {@link SkyBreed}: vanilla's bull answers only to the
 *       string {@code "cow"}, so without a male of our own the herd could
 *       never breed. Bulls are not milked.</li>
 *   <li><b>Spawning.</b> See {@link SkyBreed#validPastureSpawn}.</li>
 *   <li><b>Drawing.</b> {@code CowMob.getTexture()} is private, so the draw and
 *       death-particle methods are re-stated with our sheets, on the vanilla
 *       geometry (including the cow's extra {@code drawY -= 4}) and the vanilla
 *       cow/calf shadows.</li>
 * </ul>
 */
public class NimbusYakMob extends CowMob {

    /** Recoloured vanilla mobs/cow, mobs/bull and mobs/calf — see {@link SkyPelt}. */
    public static GameTexture cowTexture;
    public static GameTexture bullTexture;
    public static GameTexture calfTexture;

    public static final LootTable nimbusYakLoot = new LootTable(
            LootItem.between("beef", 1, 2), LootItem.between("leather", 2, 5));

    /** {@link SkyBreed#UNSET}/{@link SkyBreed#FEMALE}/{@link SkyBreed#MALE}. */
    protected int sex = SkyBreed.UNSET;

    @Override
    public void init() {
        super.init();
        // Guarded exactly like ChickenMob.init guards its own first egg timer:
        // whichever of init() and applyLoadData() runs second, a saved animal
        // keeps the sex it was born with.
        this.sex = SkyBreed.rollIfUnset(this.sex, this);
    }

    @Override
    public void addSaveData(SaveData save) {
        super.addSaveData(save);
        save.addInt("skySex", this.sex);
    }

    @Override
    public void applyLoadData(LoadData save) {
        super.applyLoadData(save);
        this.sex = save.getInt("skySex", this.sex, false);
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

    @Override
    public GameMessage getLocalization() {
        return new LocalMessage("mob", this.getStringID());
    }

    @Override
    public LootTable getLootTable() {
        return !this.isGrown() ? new LootTable() : nimbusYakLoot;
    }

    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkyBreed.validPastureSpawn(this, client, targetX, targetY);
    }

    /** A bull is not a dairy animal; vanilla's BullMob implements no milking either. */
    @Override
    public boolean canMilk(InventoryItem item) {
        return this.getGender() == HumanGender.FEMALE && super.canMilk(item);
    }

    /**
     * Vanilla's milking, our milk. {@code super.onMilk} is what re-arms
     * {@code nextMilkTime} and sends the movement packet, so the cooldown stays
     * the cow's; only the item in the bucket changes.
     */
    @Override
    public InventoryItem onMilk(InventoryItem item, List<InventoryItem> products) {
        ArrayList<InventoryItem> vanillaProducts = new ArrayList<>();
        InventoryItem result = super.onMilk(item, vanillaProducts);
        for (int i = 0; i < vanillaProducts.size(); i++) {
            products.add(new InventoryItem(SkyLivestock.NIMBUS_MILK));
        }
        return result;
    }

    @Override
    protected void addHoverTooltips(ListGameTooltips tooltips, boolean debug) {
        super.addHoverTooltips(tooltips, debug);
        tooltips.add(Localization.translate("misc", "nimbusyaktip"));
    }

    private GameTexture bodyTexture() {
        if (!this.isGrown()) {
            return calfTexture;
        }
        return SkyBreed.isMale(this.sex) && bullTexture != null ? bullTexture : cowTexture;
    }

    private GameTexture shadowTexture() {
        return this.isGrown() ? MobRegistry.Textures.cow_shadow : MobRegistry.Textures.calf_shadow;
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
        // CowMob draws its body four pixels above its shadow; a yak is a cow's
        // size, so it keeps the cow's offset.
        drawY -= 4;
        drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
        final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
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
