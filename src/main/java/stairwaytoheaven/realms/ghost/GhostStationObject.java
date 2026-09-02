package stairwaytoheaven.realms.ghost;

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
 * The Aftergarden's two machines: the <b>Soul Loom</b> and the <b>Spirit
 * Forge</b>.
 *
 * <h2>Archetype, and why a settler can work one</h2>
 * {@link CraftingStationObject} — the same class the mod's Windsilk Loom uses
 * and the same one vanilla's alchemy table and workbench use. It is
 * {@code FurnitureObject implements SettlementWorkstationObject}, and its
 * {@code streamSettlementRecipes} already answers with every recipe matching
 * {@link #getCraftingTechs()}. So a settler assigned this station in the
 * settlement screen's work tab fetches the ingredients out of settlement
 * storage, walks over, works and hauls the result back. No object entity and no
 * work ZONE are involved: {@code SettlementStorageManager.assignWorkstation}
 * accepts any object that {@code instanceof SettlementWorkstationObject} — that
 * check is the whole gate — and the {@code UseWorkstationLevelJob} it produces
 * is filed under vanilla's existing <b>crafting</b> work priority, exactly as
 * the mod's other three stations are.
 *
 * <p><b>Why not the Aether Forge's shape.</b> The mod's Aether Forge is a
 * {@code GameObject} with its own {@code ObjectEntity}, because it is a FUELED
 * processing station: it burns logs, runs while nobody is standing at it, and
 * carries a save schema of its own. The Spirit Forge deliberately does not burn
 * anything — a forge in the land of the dead has no fire to feed — so a
 * crafting station is not a simplification here, it is the right archetype, and
 * it avoids adding a second save schema for no gameplay difference. If a fuel
 * loop is ever wanted, {@code AetherForgeObjectEntity} is the template.
 *
 * <h2>Both sheets are borrowed</h2>
 * The world sheet and the item icon are constructor arguments, so the loom
 * draws on the mod's existing loom sheet and the forge on its existing forge
 * sheet, and the icons come from the game's own resources. See
 * {@code docs/realms/ghost.md} for the table. Drawing follows
 * {@code AlchemyTableObject}, the vanilla 128x64 station on this same base
 * class: four 32px rotation columns, {@code sprite(rotation % 4, 0, 32,
 * height)} anchored at {@code drawY - height + 32}.
 */
public class GhostStationObject extends CraftingStationObject {

    private final String textureName;
    private final String iconName;
    private final String tooltipKey;
    private final Tech[] techs;

    public GameTexture texture;

    /**
     * @param textureName world sheet under {@code objects/}
     * @param iconName    inventory icon under {@code items/}
     * @param tooltipKey  {@code [itemtooltip]} key for the "what is this" line
     * @param tech        the recipe tech this station runs
     */
    public GhostStationObject(String textureName, String iconName, String tooltipKey, Tech tech) {
        super(new Rectangle(32, 32));
        this.textureName = textureName;
        this.iconName = iconName;
        this.tooltipKey = tooltipKey;
        this.techs = new Tech[]{tech};
        this.mapColor = new Color(96, 176, 168);
        this.toolType = ToolType.ALL;
        this.rarity = Item.Rarity.COMMON;
        this.objectHealth = 50;
        this.isLightTransparent = true;
        this.setItemCategory("objects", "craftingstations");
        this.setCraftingCategory("craftingstations");
        this.roomProperties.add("metalwork");
        // Same reach vanilla gives every one-tile station that rises a tile
        // above its floor tile (forge, cheese press, alchemy table).
        this.hoverHitbox = new Rectangle(0, -16, 32, 48);
    }

    @Override
    public void loadTextures() {
        super.loadTextures();
        this.texture = GameTexture.fromFile("objects/" + this.textureName);
    }

    @Override
    public GameTexture generateItemTexture() {
        return new GameTexture(GameTexture.fromFile("items/" + this.iconName));
    }

    @Override
    public Rectangle getCollision(Level level, int x, int y, int rotation) {
        // Wide base seen head on, narrow one edge on — the split
        // AlchemyTableObject uses.
        return rotation % 2 == 0
                ? new Rectangle(x * 32 + 2, y * 32 + 6, 28, 20)
                : new Rectangle(x * 32 + 6, y * 32 + 2, 20, 28);
    }

    @Override
    public void addDrawables(List<LevelSortedDrawable> list, OrderableDrawables tileList,
            Level level, int tileX, int tileY, TickManager tickManager, GameCamera camera,
            PlayerMob perspective) {
        if (this.texture == null) {
            return;
        }
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
        if (this.texture == null) {
            return;
        }
        int drawX = camera.getTileDrawX(tileX);
        int drawY = camera.getTileDrawY(tileY);
        this.texture.initDraw()
                .sprite(rotation % 4, 0, 32, this.texture.getHeight())
                .alpha(alpha)
                .draw(drawX, drawY - this.texture.getHeight() + 32);
    }

    @Override
    public Tech[] getCraftingTechs() {
        return this.techs;
    }

    @Override
    public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
        ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
        tooltips.add(Localization.translate("itemtooltip", this.tooltipKey));
        return tooltips;
    }
}
