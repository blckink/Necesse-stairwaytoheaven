package stairwaytoheaven.settlement;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.recipe.Tech;
import necesse.level.gameObject.container.CraftingStationObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

/**
 * The Windsilk Loom: the Skywatch's weaving bench, where windsilk becomes
 * Skyweave cloth.
 *
 * <p>Archetype: {@link CraftingStationObject}. That class is
 * {@code FurnitureObject implements SettlementWorkstationObject}, and its
 * {@code streamSettlementRecipes} already answers with every recipe matching
 * {@link #getCraftingTechs()} — so a settler assigned this workstation in the
 * settlement screen fetches windsilk out of settlement storage, walks here,
 * weaves, and hauls the cloth back. No object entity and no work zone are
 * involved: a workstation is discovered by
 * {@code SettlementStorageManager.assignWorkstation}, which accepts any object
 * that {@code instanceof SettlementWorkstationObject}, and the job it produces
 * ({@code UseWorkstationLevelJob}) is registered under vanilla's
 * <b>crafting</b> work priority.
 *
 * <p>Drawing follows {@code AlchemyTableObject}, the vanilla 128x64 station on
 * the same base class: four 32px rotation columns, {@code sprite(rotation % 4,
 * 0, 32, height)} anchored at {@code drawY - height + 32}.
 */
public class WindsilkLoomObject extends CraftingStationObject {

    public GameTexture texture;

    public WindsilkLoomObject() {
        super(new Rectangle(32, 32));
        this.mapColor = new Color(186, 196, 208);
        this.toolType = ToolType.ALL;
        this.rarity = Item.Rarity.COMMON;
        this.objectHealth = 50;
        this.isLightTransparent = true;
        // Same reach vanilla gives every one-tile station that rises a tile
        // above its floor tile (forge, cheese press, alchemy table).
        this.hoverHitbox = new Rectangle(0, -16, 32, 48);
    }

    @Override
    public void loadTextures() {
        super.loadTextures();
        this.texture = GameTexture.fromFile("objects/windsilkloom");
    }

    @Override
    public Rectangle getCollision(Level level, int x, int y, int rotation) {
        // The loom stands on a wide base seen head on and a narrow one edge on,
        // exactly the split AlchemyTableObject uses.
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
        int rotation = level.getObjectRotation(tileX, tileY) % 4;
        final TextureDrawOptions options = this.texture
                .initDraw()
                .sprite(rotation, 0, 32, this.texture.getHeight())
                .addObjectDamageOverlay(this, level, tileX, tileY)
                .light(light)
                .pos(drawX, drawY - this.texture.getHeight() + 32);
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
                .draw(drawX, drawY - this.texture.getHeight() + 32);
    }

    @Override
    public Tech[] getCraftingTechs() {
        return new Tech[]{SkyProfessions.LOOM};
    }

    @Override
    public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
        ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
        tooltips.add(Localization.translate("itemtooltip", "windsilkloomtip"));
        return tooltips;
    }
}
