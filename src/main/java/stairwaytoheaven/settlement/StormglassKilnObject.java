package stairwaytoheaven.settlement;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.registries.ContainerRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.ProcessingInventoryObjectEntity;
import necesse.entity.objectEntity.ProcessingTechInventoryObjectEntity;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.InventoryRange;
import necesse.inventory.container.object.OEInventoryContainer;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.SettlementWorkstationObject;
import necesse.level.maps.light.GameLight;

/**
 * The Stormglass Kiln: fulgurite and skystone fired into stormglass panes.
 *
 * <p>Archetype: vanilla's {@code CheesePressObject} — {@code GameObject
 * implements SettlementWorkstationObject} over a
 * {@link ProcessingTechInventoryObjectEntity}, i.e. a processing station with
 * NO fuel: the settler drops the ingredients in and the kiln works on its own
 * clock. {@code CheesePressObject} itself cannot be subclassed usefully (it
 * hard codes {@code objects/cheesepress} and the vanilla cheese tech), so this
 * is that class's shape pointed at our own texture and
 * {@link StormglassKilnObjectEntity}.
 *
 * <p>One deliberate difference from the cheese press. Vanilla writes its sprite
 * index as {@code rotation % texture.getWidth() / 32}, which Java groups as
 * {@code (rotation % width) / 32} — always 0 for rotations 0-3, whatever the
 * sheet width. That is harmless on a 32px-wide sheet like {@code cheesepress}
 * and would silently pin a four-column sheet to its first column. This kiln has
 * four real rotations, so it indexes {@code rotation % 4} the way every other
 * vanilla station (forge, alchemy table) does.
 *
 * <p>A settler is assigned exactly as for the other two stations: the player
 * marks the tile as a workstation in the settlement screen and adds recipes;
 * {@code ServerSettlementData.tickJobs} publishes a
 * {@code UseWorkstationLevelJob} for it, which is registered under vanilla's
 * <b>crafting</b> work priority. No settlement work zone is involved.
 */
public class StormglassKilnObject extends GameObject implements SettlementWorkstationObject {

    public GameTexture texture;
    public GameTexture onTexture;

    public StormglassKilnObject() {
        super(new Rectangle(4, 6, 24, 20));
        this.setItemCategory(new String[]{"objects", "craftingstations"});
        this.setCraftingCategory(new String[]{"craftingstations"});
        this.mapColor = new Color(168, 150, 118);
        this.displayMapTooltip = true;
        this.toolType = ToolType.ALL;
        this.rarity = Item.Rarity.COMMON;
        this.objectHealth = 50;
        this.isLightTransparent = true;
        // Amber, the colour of the glass melt in the stoke hole.
        this.lightHue = 35.0F;
        this.lightSat = 0.30F;
        this.hoverHitbox = new Rectangle(0, -16, 32, 48);
        this.replaceCategories.add("workstation");
        this.canReplaceCategories.add("workstation");
        this.canReplaceCategories.add("wall");
        this.canReplaceCategories.add("furniture");
    }

    @Override
    public void loadTextures() {
        super.loadTextures();
        this.texture = GameTexture.fromFile("objects/stormglasskiln");
        // Same optional-lit-sheet handshake the cheese press uses: fromFileRaw
        // throws instead of handing back the ERR texture, so a missing _on
        // degrades to the cold sheet rather than to a red tile.
        try {
            this.onTexture = GameTexture.fromFileRaw("objects/stormglasskiln_on");
        } catch (FileNotFoundException e) {
            this.onTexture = this.texture;
        }
    }

    @Override
    public int getLightLevel(Level level, int layerID, int tileX, int tileY) {
        ProcessingTechInventoryObjectEntity kiln = this.getProcessingObjectEntity(level, tileX, tileY);
        return kiln != null && kiln.isProcessing() ? 80 : 0;
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
        int rotation = level.getObjectRotation(tileX, tileY) % 4;
        int drawX = camera.getTileDrawX(tileX);
        int drawY = camera.getTileDrawY(tileY);
        ObjectEntity objectEntity = level.entityManager.getObjectEntity(tileX, tileY);
        GameTexture sheet = this.texture;
        if (objectEntity instanceof ProcessingInventoryObjectEntity
                && ((ProcessingInventoryObjectEntity) objectEntity).isProcessing()) {
            sheet = this.onTexture;
        }

        final TextureDrawOptions options = sheet.initDraw()
                .sprite(rotation, 0, 32, sheet.getHeight())
                .addObjectDamageOverlay(this, level, tileX, tileY)
                .light(light)
                .pos(drawX, drawY - (sheet.getHeight() - 32));
        list.add(new LevelSortedDrawable(this, tileX, tileY) {
            @Override
            public int getSortY() {
                return 16;
            }

            @Override
            public void draw(TickManager tickManager) {
                options.draw();
            }
        });
    }

    @Override
    public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha,
                            PlayerMob player, GameCamera camera) {
        int drawX = camera.getTileDrawX(tileX);
        int drawY = camera.getTileDrawY(tileY);
        this.texture.initDraw()
                .sprite(rotation % 4, 0, 32, this.texture.getHeight())
                .alpha(alpha)
                .draw(drawX, drawY - (this.texture.getHeight() - 32));
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
            OEInventoryContainer.openAndSendContainer(
                    ContainerRegistry.PROCESSING_INVENTORY_CONTAINER,
                    player.getServerClient(), level, x, y);
        }
    }

    @Override
    public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
        return new StormglassKilnObjectEntity(level, x, y);
    }

    @Override
    public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
        ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
        tooltips.add(Localization.translate("itemtooltip", "stormglasskilntip"));
        return tooltips;
    }

    public ProcessingTechInventoryObjectEntity getProcessingObjectEntity(Level level, int tileX, int tileY) {
        ObjectEntity objectEntity = level.entityManager.getObjectEntity(tileX, tileY);
        return objectEntity instanceof ProcessingTechInventoryObjectEntity
                ? (ProcessingTechInventoryObjectEntity) objectEntity : null;
    }

    // ---- SettlementWorkstationObject -------------------------------------

    @Override
    public Stream<Recipe> streamSettlementRecipes(Level level, int tileX, int tileY) {
        ProcessingTechInventoryObjectEntity kiln = this.getProcessingObjectEntity(level, tileX, tileY);
        return kiln != null ? Recipes.streamRecipes(kiln.techs) : Stream.empty();
    }

    @Override
    public boolean isProcessingInventory(Level level, int tileX, int tileY) {
        return true;
    }

    @Override
    public boolean canCurrentlyCraft(Level level, int tileX, int tileY, Recipe recipe) {
        ProcessingTechInventoryObjectEntity kiln = this.getProcessingObjectEntity(level, tileX, tileY);
        return kiln != null && kiln.getExpectedResults().crafts < 10;
    }

    @Override
    public int getMaxCraftsAtOnce(Level level, int tileX, int tileY, Recipe recipe) {
        return 10;
    }

    @Override
    public InventoryRange getProcessingInputRange(Level level, int tileX, int tileY) {
        ProcessingTechInventoryObjectEntity kiln = this.getProcessingObjectEntity(level, tileX, tileY);
        return kiln != null ? kiln.getInputInventoryRange() : null;
    }

    @Override
    public InventoryRange getProcessingOutputRange(Level level, int tileX, int tileY) {
        ProcessingTechInventoryObjectEntity kiln = this.getProcessingObjectEntity(level, tileX, tileY);
        return kiln != null ? kiln.getOutputInventoryRange() : null;
    }

    @Override
    public ArrayList<InventoryItem> getCurrentAndFutureProcessingOutputs(Level level, int tileX, int tileY) {
        ProcessingTechInventoryObjectEntity kiln = this.getProcessingObjectEntity(level, tileX, tileY);
        return kiln != null ? kiln.getCurrentAndExpectedResults().items : new ArrayList<>();
    }

    @Override
    protected boolean shouldPlayInteractSound(Level level, int tileX, int tileY) {
        return true;
    }
}
