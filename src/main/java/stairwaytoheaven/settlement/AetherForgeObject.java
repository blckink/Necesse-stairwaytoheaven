package stairwaytoheaven.settlement;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.registries.GlobalIngredientRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.sound.SoundSettingsRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.InventoryRange;
import necesse.inventory.container.object.CraftingStationContainer;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.SettlementRequestOptions;
import necesse.level.maps.levelData.settlementData.SettlementWorkstationObject;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageGlobalIngredientIDIndex;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageRecords;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageRecordsRegionData;
import necesse.level.maps.light.GameLight;

/**
 * The Aether Forge: a log-fired crucible that refines aetherium ore.
 *
 * <p>Archetype: vanilla's {@code ProcessingForgeObject}, method for method.
 * That class is not designed to be subclassed — its {@code loadTextures} hard
 * codes {@code objects/forge} and its {@code getNewObjectEntity} hard codes the
 * vanilla forge's tech — so this is the same {@code GameObject implements
 * SettlementWorkstationObject} shape pointed at our own texture and our own
 * {@link AetherForgeObjectEntity}.
 *
 * <p>How a settler works it: the player marks the tile as a workstation in the
 * settlement screen and adds recipes to it. {@code ServerSettlementData.tickJobs}
 * then publishes a {@code UseWorkstationLevelJob} for it every tick, and any
 * settler with the vanilla <b>crafting</b> work priority takes it. Because
 * {@link #isProcessingInventory} is true, the settler hauls ore into the input
 * slots and the fuel request pulls logs in on its own — the forge runs while
 * nobody is standing at it, and a second settler carries the bars back to
 * storage. No settlement work ZONE exists or is needed: zones
 * ({@code SettlementWorkZoneRegistry}: forestry, husbandry, fertilize) cover
 * area tasks, and workstations are found by
 * {@code SettlementStorageManager.assignWorkstation} instead.
 *
 * <p>Sheet layout is the vanilla forge's: {@code objects/aetherforge.png} is
 * 128x96 — four 32x64 rotation columns of body drawn at {@code drawY - 32},
 * plus a four-frame 32x32 fire strip on row 2 drawn at {@code drawY} while the
 * fuel is running.
 */
public class AetherForgeObject extends GameObject implements SettlementWorkstationObject {

    public GameTexture texture;

    public AetherForgeObject() {
        super(new Rectangle(32, 32));
        this.setItemCategory(new String[]{"objects", "craftingstations"});
        this.setCraftingCategory(new String[]{"craftingstations"});
        this.mapColor = new Color(96, 132, 146);
        this.displayMapTooltip = true;
        this.toolType = ToolType.ALL;
        this.rarity = Item.Rarity.COMMON;
        this.objectHealth = 50;
        this.isLightTransparent = true;
        this.roomProperties.add("metalwork");
        // Vanilla's forge tints its light warm (hue 50). Aetherium burns teal.
        this.lightHue = 180.0F;
        this.lightSat = 0.35F;
        this.hoverHitbox = new Rectangle(0, -16, 32, 48);
        this.replaceCategories.add("workstation");
        this.canReplaceCategories.add("workstation");
        this.canReplaceCategories.add("wall");
        this.canReplaceCategories.add("furniture");
    }

    @Override
    public void loadTextures() {
        super.loadTextures();
        this.texture = GameTexture.fromFile("objects/aetherforge");
    }

    @Override
    public int getLightLevel(Level level, int layerID, int tileX, int tileY) {
        AetherForgeObjectEntity forge = this.getForgeObjectEntity(level, tileX, tileY);
        return forge != null && forge.isFuelRunning() ? 100 : 0;
    }

    @Override
    public void tickEffect(Level level, int layerID, int tileX, int tileY) {
        super.tickEffect(level, layerID, tileX, tileY);
        if (GameRandom.globalRandom.nextInt(10) == 0) {
            AetherForgeObjectEntity forge = this.getForgeObjectEntity(level, tileX, tileY);
            if (forge != null && forge.isFuelRunning()) {
                int startHeight = 16 + GameRandom.globalRandom.nextInt(16);
                level.entityManager
                        .addParticle((float) (tileX * 32 + GameRandom.globalRandom.getIntBetween(8, 24)),
                                (float) (tileY * 32 + 32), Particle.GType.COSMETIC)
                        .smokeColor()
                        .heightMoves((float) startHeight, (float) (startHeight + 20))
                        .lifeTime(1000);
            }
        }
    }

    @Override
    public Rectangle getCollision(Level level, int x, int y, int rotation) {
        return rotation % 2 == 0
                ? new Rectangle(x * 32 + 2, y * 32 + 6, 28, 20)
                : new Rectangle(x * 32 + 6, y * 32 + 2, 20, 28);
    }

    @Override
    public void addDrawables(
            List<LevelSortedDrawable> list,
            OrderableDrawables tileList,
            Level level,
            int tileX,
            int tileY,
            TickManager tickManager,
            GameCamera camera,
            PlayerMob perspective
    ) {
        GameLight light = level.getLightLevel(tileX, tileY);
        int drawX = camera.getTileDrawX(tileX);
        int drawY = camera.getTileDrawY(tileY);
        byte rotation = level.getObjectRotation(tileX, tileY);
        boolean isFueled = false;
        AetherForgeObjectEntity objectEntity = this.getForgeObjectEntity(level, tileX, tileY);
        if (objectEntity != null) {
            isFueled = objectEntity.isFuelRunning();
        }

        // The sheet is one tile taller than the body: the last 32px row is the
        // fire strip, so the body is everything above it.
        int spriteHeight = this.texture.getHeight() - 32;
        final TextureDrawOptions options = this.texture
                .initDraw()
                .sprite(rotation % 4, 0, 32, spriteHeight)
                .addObjectDamageOverlay(this, level, tileX, tileY)
                .light(light)
                .pos(drawX, drawY - (spriteHeight - 32));
        final TextureDrawOptions fire;
        if (isFueled && rotation == 2) {
            // Only the south-facing forge shows its mouth to the camera, which
            // is exactly why vanilla animates the flame for rotation 2 alone.
            int spriteX = (int) (level.getWorldEntity().getWorldTime() % 1200L / 300L);
            fire = this.texture.initDraw()
                    .sprite(spriteX, spriteHeight / 32, 32)
                    .light(light)
                    .pos(drawX, drawY);
        } else {
            fire = null;
        }

        list.add(new LevelSortedDrawable(this, tileX, tileY) {
            @Override
            public int getSortY() {
                return 16;
            }

            @Override
            public void draw(TickManager tickManager) {
                options.draw();
                if (fire != null) {
                    fire.draw();
                }
            }
        });
    }

    @Override
    public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha,
                            PlayerMob player, GameCamera camera) {
        int drawX = camera.getTileDrawX(tileX);
        int drawY = camera.getTileDrawY(tileY);
        int spriteHeight = this.texture.getHeight() - 32;
        this.texture.initDraw()
                .sprite(rotation % 4, 0, 32, spriteHeight)
                .alpha(alpha)
                .draw(drawX, drawY - (spriteHeight - 32));
    }

    @Override
    public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
        return new AetherForgeObjectEntity(level, x, y);
    }

    @Override
    public String getInteractTip(Level level, int x, int y, PlayerMob perspective, boolean debug) {
        return Localization.translate("controls", "opentip");
    }

    @Override
    public boolean canInteract(Level level, int x, int y, PlayerMob player) {
        return true;
    }

    @Override
    public void interact(Level level, int x, int y, PlayerMob player) {
        super.interact(level, x, y, player);
        if (level.isServer()) {
            CraftingStationContainer.openAndSendContainer(
                    ContainerRegistry.FUELED_PROCESSING_STATION_CONTAINER,
                    player.getServerClient(), level, x, y);
        }
    }

    @Override
    public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
        ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
        tooltips.add(Localization.translate("itemtooltip", "aetherforgetip"));
        return tooltips;
    }

    public AetherForgeObjectEntity getForgeObjectEntity(Level level, int tileX, int tileY) {
        ObjectEntity objectEntity = level.entityManager.getObjectEntity(tileX, tileY);
        return objectEntity instanceof AetherForgeObjectEntity
                ? (AetherForgeObjectEntity) objectEntity : null;
    }

    // ---- SettlementWorkstationObject -------------------------------------

    @Override
    public Stream<Recipe> streamSettlementRecipes(Level level, int tileX, int tileY) {
        AetherForgeObjectEntity forge = this.getForgeObjectEntity(level, tileX, tileY);
        return forge != null ? Recipes.streamRecipes(forge.techs) : Stream.empty();
    }

    @Override
    public boolean isProcessingInventory(Level level, int tileX, int tileY) {
        return true;
    }

    @Override
    public boolean canCurrentlyCraft(Level level, int tileX, int tileY, Recipe recipe) {
        AetherForgeObjectEntity forge = this.getForgeObjectEntity(level, tileX, tileY);
        return forge != null
                && forge.getExpectedResults().crafts < 10
                && (forge.isFuelRunning() || forge.canUseFuel());
    }

    @Override
    public int getMaxCraftsAtOnce(Level level, int tileX, int tileY, Recipe recipe) {
        return 5;
    }

    @Override
    public InventoryRange getProcessingInputRange(Level level, int tileX, int tileY) {
        AetherForgeObjectEntity forge = this.getForgeObjectEntity(level, tileX, tileY);
        return forge != null ? forge.getInputInventoryRange() : null;
    }

    @Override
    public InventoryRange getProcessingOutputRange(Level level, int tileX, int tileY) {
        AetherForgeObjectEntity forge = this.getForgeObjectEntity(level, tileX, tileY);
        return forge != null ? forge.getOutputInventoryRange() : null;
    }

    @Override
    public ArrayList<InventoryItem> getCurrentAndFutureProcessingOutputs(Level level, int tileX, int tileY) {
        AetherForgeObjectEntity forge = this.getForgeObjectEntity(level, tileX, tileY);
        return forge != null ? forge.getCurrentAndExpectedResults().items : new ArrayList<>();
    }

    @Override
    public SettlementRequestOptions getFuelRequestOptions(Level level, int tileX, int tileY) {
        // "Keep between 5 and 10 logs here" - the standing order that makes the
        // settlement's haulers refuel the forge without the player asking.
        return new SettlementRequestOptions(5, 10) {
            @Override
            public SettlementStorageRecordsRegionData getRequestStorageData(SettlementStorageRecords records) {
                return records.getIndex(SettlementStorageGlobalIngredientIDIndex.class)
                        .getGlobalIngredient(GlobalIngredientRegistry.getGlobalIngredientID("anylog"));
            }
        };
    }

    @Override
    public InventoryRange getFuelInventoryRange(Level level, int tileX, int tileY) {
        AetherForgeObjectEntity forge = this.getForgeObjectEntity(level, tileX, tileY);
        if (forge != null) {
            Inventory inventory = forge.getInventory();
            if (inventory != null && forge.fuelSlots > 0) {
                return new InventoryRange(inventory, 0, forge.fuelSlots - 1);
            }
        }
        return null;
    }

    @Override
    protected boolean shouldPlayInteractSound(Level level, int tileX, int tileY) {
        return true;
    }

    @Override
    protected SoundSettings getInteractSoundOpen() {
        return SoundSettingsRegistry.defaultOpen;
    }
}
