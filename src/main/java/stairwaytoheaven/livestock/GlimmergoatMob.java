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
import necesse.entity.mobs.friendly.HusbandryMob;
import necesse.entity.mobs.friendly.SheepMob;
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
 * The Glimmergoat — the Aurora Shoals' fibre animal, on vanilla's
 * {@link SheepMob}.
 *
 * <p>WHY A SECOND SHEARABLE. The Cloud Lamb already gives Windsilk, the mod's
 * workaday fibre, and it grazes the Driftlands where a player lands. The
 * Glimmergoat lives two biomes out in the rarest ground in the sky and gives
 * {@link SkyLivestock#AURORA_FLEECE} instead — the material the Glimmerstride
 * boots are made of. Vanilla does the same thing with the same base class
 * twice over (sheep and ram both shear wool); what makes this one worth the
 * climb is what comes off it, not the shearing.
 *
 * <p>Unlike the Cloud Lamb the herd can actually breed, because it has both
 * sexes: vanilla's {@code RamMob.canImpregnateMob} is
 * {@code other.getStringID().equals("sheep")}, a hard string test that no
 * modded animal can ever satisfy. See {@link SkyBreed}.
 *
 * <p>Bucks are drawn on vanilla's ram sheets and does on the sheep sheets, both
 * recoloured, so a sheared animal, a horned male and a kid all read correctly
 * without a single new frame being drawn.
 */
public class GlimmergoatMob extends SheepMob {

    /**
     * The player's own five sheets: {@code mobs/glimmergoat-doe},
     * {@code -doe_shorn}, {@code -ram}, {@code -ram_shorn}, {@code -lamb},
     * assigned in {@code SkyLivestock.loadTextures}.
     *
     * <p>NOT recoloured. Vanilla's sheep/ram/lamb were the stand-in until
     * 2026-09-02; all five real sheets have shipped since, shorn states
     * included. {@code extends SheepMob} is a BEHAVIOUR base — shearing,
     * breeding, regrowth — and says nothing about the art.
     */
    public static GameTexture doeTexture;
    public static GameTexture doeShornTexture;
    public static GameTexture buckTexture;
    public static GameTexture buckShornTexture;
    public static GameTexture kidTexture;

    public static final LootTable glimmergoatLoot = new LootTable(
            LootItem.between("rawmutton", 1, 2), LootItem.between(SkyLivestock.AURORA_FLEECE, 1, 2));

    protected int sex = SkyBreed.UNSET;

    @Override
    public void init() {
        super.init();
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

    /**
     * {@code SheepMob.getRandomChildMobStringID} is
     * {@code getOneOf(getStringID(), "ram")} — half of every kid would be a
     * plain vanilla ram. Ours breeds true, and the sex of the kid is rolled by
     * {@link SkyBreed} like any other.
     */
    @Override
    public String getRandomChildMobStringID(HusbandryMob father) {
        return this.getStringID();
    }

    /** {@code SheepMob.getLocalization} returns vanilla's {@code mob.lamb} for a kid. */
    @Override
    public GameMessage getLocalization() {
        return new LocalMessage("mob", this.getStringID());
    }

    @Override
    public LootTable getLootTable() {
        return !this.isGrown() ? new LootTable() : glimmergoatLoot;
    }

    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkyBreed.validPastureSpawn(this, client, targetX, targetY);
    }

    /**
     * Vanilla's shearing, our fleece. {@code super.onShear} rolls 1-3 items,
     * re-arms {@code nextShearTime} for 20-30 in-game minutes, plays the shears
     * sound and sends the movement packet that flips {@code hasWool()} on every
     * client; only the item changes.
     */
    @Override
    public InventoryItem onShear(InventoryItem item, List<InventoryItem> products) {
        ArrayList<InventoryItem> vanillaProducts = new ArrayList<>();
        InventoryItem result = super.onShear(item, vanillaProducts);
        for (int i = 0; i < vanillaProducts.size(); i++) {
            products.add(new InventoryItem(SkyLivestock.AURORA_FLEECE));
        }
        return result;
    }

    @Override
    protected void addHoverTooltips(ListGameTooltips tooltips, boolean debug) {
        super.addHoverTooltips(tooltips, debug);
        tooltips.add(Localization.translate("misc", "glimmergoattip"));
    }

    private GameTexture bodyTexture() {
        if (!this.isGrown()) {
            return kidTexture;
        }
        boolean male = SkyBreed.isMale(this.sex) && buckTexture != null;
        if (this.hasWool()) {
            return male ? buckTexture : doeTexture;
        }
        GameTexture shorn = male ? buckShornTexture : doeShornTexture;
        return shorn != null ? shorn : (male ? buckTexture : doeTexture);
    }

    private GameTexture shadowTexture() {
        return this.isGrown() ? MobRegistry.Textures.sheep_shadow : MobRegistry.Textures.lamb_shadow;
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
