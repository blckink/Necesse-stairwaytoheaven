package stairwaytoheaven.mobs;

import java.awt.Point;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.SheepMob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

/**
 * The Cloudlamb — a REAL sky sheep, not a critter: extends the vanilla
 * SheepMob so ropes, feeding troughs, breeding, growing up and shearing all
 * work exactly like surface livestock. What makes it worth keeping instead of
 * a surface sheep is what comes off it.
 *
 * WHAT IT IS FOR (the playtest question was literally "was bringen sie jetzt?
 * und es gibt halt schon normale schafe"):
 *
 *  · Shearing yields WINDSILK, not vanilla wool. Windsilk is the mod's fibre:
 *    the Galehowl bow, the Seance Circle that opens the Veil, the Sky Balloon
 *    and — the one that matters most — the Cloudpuff Treats the spire cats
 *    want are all made of it. Before this, every gram of windsilk in a world
 *    came off a corpse or a windwheat harvest; a sheared flock is the only
 *    renewable source there is, and a vanilla sheep can never produce any.
 *  · It eats CLOUDBERRIES (see SkyItems: the berry is a GrainItem, so vanilla's
 *    feeding trough accepts it and right-clicking with one feeds by hand), which
 *    grow on cloudberry bushes in the Driftlands. Vanilla wheat still works —
 *    every HusbandryMob eats grain — so a lamb is at home in a surface pen too.
 *  · Which is the reason to haul one down: they are placed at Skyreach region
 *    generation and can never be spawned any other way (see
 *    SkyLevel.placeCloudLambFlock), so a breeding pair on a rope is how a
 *    player turns a sky animal into a farm. FriendlyRopableMob drags a roped
 *    animal along when the roper changes level (jar 1.3.2,
 *    FriendlyRopableMob.java:45-52), so the stairway brings them home.
 *
 * Vanilla SheepMob draws through a private getTexture(), so the draw and
 * death-particle methods are overridden 1:1 with our textures (same sheet
 * layout as vanilla sheep.png: 6x4 walk grid + a 5th row of fleece chunks).
 * The vanilla sheep/lamb shadows are reused.
 */
public class CloudLambMob extends SheepMob {

    public static GameTexture texture;
    public static GameTexture shearedTexture;

    /**
     * Which sex this individual is.
     *
     * Without it the flock could not breed at all. Vanilla drives breeding from
     * the MALE: {@code HusbandryMob.canImpregnateMob} is {@code return false},
     * and every vanilla male hard-tests a vanilla string
     * ({@code RamMob} -> {@code "sheep"}), so no vanilla ram will ever accept a
     * Cloudlamb and a Cloudlamb had no way to accept one either. It also had no
     * sex of its own -- {@code SheepMob.getGender} answers for a vanilla sheep --
     * so {@code canBirth}/{@code canImpregnate} and the husbandry zone's job
     * tick had nothing to pair up.
     *
     * The three v1.0 sky animals hit exactly this and solved it in
     * {@link stairwaytoheaven.livestock.SkyBreed}; the Cloudlamb shipped first
     * and was left behind. Same fix, same helper: roll a sex on the server,
     * persist it, send it in the spawn packet, and accept our own kind.
     */
    protected int sex = stairwaytoheaven.livestock.SkyBreed.UNSET;

    @Override
    public void init() {
        super.init();
        this.sex = stairwaytoheaven.livestock.SkyBreed.rollIfUnset(this.sex, this);
    }

    @Override
    public void addSaveData(necesse.engine.save.SaveData save) {
        super.addSaveData(save);
        save.addInt("skySex", this.sex);
    }

    @Override
    public void applyLoadData(necesse.engine.save.LoadData save) {
        super.applyLoadData(save);
        this.sex = save.getInt("skySex", this.sex, false);
    }

    @Override
    public void setupSpawnPacket(necesse.engine.network.PacketWriter writer) {
        super.setupSpawnPacket(writer);
        writer.putNextByteUnsigned(this.sex);
    }

    @Override
    public void applySpawnPacket(necesse.engine.network.PacketReader reader) {
        super.applySpawnPacket(reader);
        this.sex = reader.getNextByteUnsigned();
    }

    @Override
    public necesse.gfx.HumanGender getGender() {
        return stairwaytoheaven.livestock.SkyBreed.gender(this.sex);
    }

    @Override
    public boolean canImpregnateMob(necesse.entity.mobs.friendly.HusbandryMob other) {
        return other.getStringID().equals(this.getStringID());
    }

    /**
     * A husbandry animal inherits {@code Mob.isValidSpawnLocation}, which is
     * {@code return false} -- vanilla places its livestock through island
     * generators, never through a spawn table. So the Cloudlamb's entry in the
     * Driftlands table could never place one: the flock existed only where
     * worldgen had already put it.
     */
    @Override
    public boolean isValidSpawnLocation(necesse.engine.network.server.Server server,
                                        necesse.engine.network.server.ServerClient client,
                                        int targetX, int targetY) {
        return stairwaytoheaven.livestock.SkyBreed.validPastureSpawn(this, client, targetX, targetY);
    }

    /** What a sheared fleece is worth, and what one drops when killed. */
    public static final String FLEECE_ITEM = "windsilk";
    public static final LootTable cloudLambLoot = new LootTable(
            LootItem.between("rawmutton", 1, 2), LootItem.between(FLEECE_ITEM, 1, 2));

    private GameTexture bodyTexture() {
        GameTexture sheared = shearedTexture;
        return (this.hasWool() || sheared == null) ? texture : sheared;
    }

    @Override
    public LootTable getLootTable() {
        return !this.isGrown() ? new LootTable() : cloudLambLoot;
    }

    /**
     * The product. SheepMob.onShear hands out vanilla {@code wool}; ours hands
     * out windsilk on the same timer (vanilla rolls 1-3 items and re-arms
     * nextShearTime for 20-30 in-game minutes, and hasWool() is what picks the
     * sheared sprite — all of that is inherited unchanged).
     */
    @Override
    public InventoryItem onShear(InventoryItem item, java.util.List<InventoryItem> products) {
        java.util.ArrayList<InventoryItem> vanillaProducts = new java.util.ArrayList<>();
        InventoryItem result = super.onShear(item, vanillaProducts);
        for (InventoryItem ignored : vanillaProducts) {
            products.add(new InventoryItem(FLEECE_ITEM));
        }
        return result;
    }

    /**
     * SheepMob.getRandomChildMobStringID is
     * {@code getOneOf(this.getStringID(), "ram")} — so half of every Cloudlamb
     * born in the sky was a VANILLA RAM. There is no sky ram; the flock breeds
     * true.
     */
    @Override
    public String getRandomChildMobStringID(necesse.entity.mobs.friendly.HusbandryMob father) {
        return this.getStringID();
    }

    /**
     * SheepMob.getLocalization returns vanilla's {@code mob.lamb} for anything
     * not grown up, so a Cloudlamb kid was labelled "Lamb" — another mod mob
     * wearing a vanilla name, which is the class of bug this project has
     * shipped twice. Ours is a Cloudlamb at every age.
     */
    @Override
    public necesse.engine.localization.message.GameMessage getLocalization() {
        return new necesse.engine.localization.message.LocalMessage("mob", this.getStringID());
    }

    /**
     * Say what it eats and what it gives, on the animal itself. Neither fact is
     * discoverable otherwise: vanilla's feeding trough silently refuses
     * anything that is not a GrainItem, and shears only tell you afterwards.
     */
    @Override
    protected void addHoverTooltips(necesse.gfx.gameTooltips.ListGameTooltips tooltips, boolean debug) {
        super.addHoverTooltips(tooltips, debug);
        tooltips.add(necesse.engine.localization.Localization.translate("misc", "cloudlambtip"));
    }

    @Override
    public void spawnDeathParticles(float knockbackX, float knockbackY) {
        GameTexture t = bodyTexture();
        if (t == null) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            this.getLevel().entityManager.addParticle(
                    new FleshParticle(this.getLevel(), t, GameRandom.globalRandom.nextInt(5), 8, 32,
                            this.x, this.y, 10.0F, knockbackX, knockbackY),
                    Particle.GType.IMPORTANT_COSMETIC);
        }
    }

    @Override
    public void addDrawables(List<MobDrawable> list, OrderableDrawables tileList, OrderableDrawables topList,
            Level level, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        GameTexture t = bodyTexture();
        if (t == null) {
            return;
        }
        GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
        int drawX = camera.getDrawX(x) - 22 - 10;
        int drawY = camera.getDrawY(y) - 44 - 7;
        int dir = this.getDir();
        Point sprite = this.getAnimSprite(x, y, dir);
        drawY += this.getBobbing(x, y);
        GameTexture shadowTexture = this.isGrown() ? MobRegistry.Textures.sheep_shadow : MobRegistry.Textures.lamb_shadow;
        TextureDrawOptions shadow = shadowTexture.initDraw().sprite(0, dir, 64).light(light).pos(drawX, drawY);
        tileList.add(tm -> shadow.draw());
        drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
        final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
        final DrawOptions options = t
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
