package stairwaytoheaven.bosses;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.registries.ObjectRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.worldgen.RealmDepth;

/**
 * A boss portal: one realm's summoning stone, standing where worldgen put it.
 *
 * <p>{@code docs/FOGKEY_AND_BOSSPORTALS.md} §B3, in full: <i>"Scattered through
 * worldgen, in their own region only. <b>Not minable.</b> Ever. They look like
 * the region's key piece, so a player recognises what they need. Using an
 * unlocked one spawns the region's boss, incursion-style, with valuable
 * loot."</i>
 *
 * <h2>Never minable</h2>
 * {@code ToolType.UNBREAKABLE}, the same way
 * {@code objects/SkySideStairwayObject} makes the Skywatch Gate unbreakable and
 * for the same class of reason: that one exists so mining your way home is
 * impossible, this one so mining away the fight is. A portal is a fact about
 * the world, not furniture, and there is no item behind it — every realm's
 * portal is registered {@code isObtainable = false}, so nothing can ever hold
 * one or place a second.
 *
 * <h2>Inert until its realm's key piece is built</h2>
 * The lock lives in {@code quest/SkywatchWorldData.bossPortalsUnlocked(realm)},
 * which is world-scoped rather than level-scoped because §B2 makes the key
 * piece something you stand in your BASE — a different place from the portal,
 * possibly a different level. {@link BossPortalObjectEntity} does the reading;
 * this class only routes the interaction to it.
 *
 * <h2>The sprites are borrowed, and that is deliberate</h2>
 * §B3 wants a portal to look like its realm's key piece. Those pieces are §B1's
 * work and do not exist yet, so each realm borrows the closest thing the mod
 * ALREADY draws — no new pixel art, per {@code docs/PLAN_ONE_PLANE.md} rule 1.
 * Every borrow is written down in {@code docs/VANILLA_ASSET_MAP.md} with its
 * exact pixel size, and swaps out by changing one constant here.
 */
public class BossPortalObject extends GameObject {

    // ------------------------------------------------------------------
    // the borrowed sheets, one per realm
    // ------------------------------------------------------------------

    /**
     * Skyreach: the Warden's beacon, lit. 32x96, mod art
     * ({@code src/main/resources/objects/wardenbeaconon.png}). The one thing
     * the Skyreach already draws that reads as "something happens here".
     */
    public static final String SPRITE_SKYREACH = "objects/wardenbeaconon";

    /**
     * Eden: the stairway sheet the Eden Gate itself wears. 32x96, mod art
     * ({@code objects/skystairwaydown.png}); {@code EdenGateObject} passes
     * {@code "skystairway"} to {@code LadderDownObject}, which reads exactly
     * this file, so Eden's doorway already looks like this.
     */
    public static final String SPRITE_EDEN = "objects/skystairwaydown";

    /**
     * Steinfeld: the seraph statue. 96x192, mod art
     * ({@code objects/statues/seraph.png}). §B1 names <i>"a statue for
     * Steinfeld"</i> and the realm already stands this exact sheet up as its
     * {@code brokenangel} ({@code SteinfeldRealm}: {@code StatueObject("seraph",
     * 32, 1)}), so the portal and the key piece are the same silhouette.
     */
    public static final String SPRITE_STEINFELD = "objects/statues/seraph";

    /**
     * Ghost Realm: the Gloom Raven statue. 64x96, mod art
     * ({@code objects/statues/gloomraven.png}) — the mod's own grave-marker
     * statue, which {@code SkyTerrainPainter} already scatters through the
     * realms the Skyway does not reach.
     */
    public static final String SPRITE_GHOST = "objects/statues/gloomraven";

    /**
     * Crooked Beyond: the door. 32x96, mod art
     * ({@code objects/veilriftdown.png}) — {@code CrookedDoorObject} passes
     * {@code "veilrift"} to {@code LadderDownObject}, so this file IS Mr.
     * Knott's door as far as the shipped mod is concerned, which is exactly the
     * key piece §B1 names for this realm.
     */
    public static final String SPRITE_CROOKED = "objects/veilriftdown";

    // ------------------------------------------------------------------
    // the worldgen lattice
    // ------------------------------------------------------------------

    /**
     * Side of one lattice cell, in tiles, and the chance a cell holds a site.
     *
     * <p>Deliberately rarer than anything else the sky scatters. The wreck
     * lattice — the rarest landmark before this — is
     * {@code SkyTerrainPainter.WRECK_CELL} 300 at {@code WRECK_CHANCE} 0.40,
     * i.e. one site per ~225 000 tiles. A portal is one per 600x600/0.35 ≈
     * 1 030 000 tiles of a realm's own band, before the Mistsea (about 61% of
     * the sky) and the realm gate throw sites away. That lands a handful in
     * each realm across a 6000-tile world: enough that a player who explores
     * will meet one, few enough that meeting one is an event.
     */
    public static final int PORTAL_CELL = 600;
    public static final float PORTAL_CHANCE = 0.35F;

    /**
     * Base salt for the portal lattice, and the stride between realms.
     *
     * <p>Each realm walks its own lattice at {@code SALT_PORTAL + realm *
     * SALT_STRIDE}, so two realms whose bands overlap cannot be handed the same
     * site and fight over it. The stride is 16 because
     * {@code SkyTerrainPainter.nearestSite} consumes {@code salt}, {@code +1},
     * {@code +2} and {@code +3}, and this leaves the same headroom the painter's
     * own lattices leave each other.
     */
    public static final int SALT_PORTAL = 971;
    public static final int SALT_STRIDE = 16;

    // ------------------------------------------------------------------
    // registration
    // ------------------------------------------------------------------

    /**
     * Registered object ID per realm; {@code 0} where a realm has no portal.
     *
     * <p>Zero is the empty object in Necesse, so "no portal" and "the id nobody
     * registered" are the same value and a caller cannot accidentally place the
     * Hell portal that does not exist.
     */
    private static final int[] BY_REALM = new int[RealmDepth.REALM_COUNT];

    /**
     * Registers all five portals and the scaling buff. Called once, from
     * {@code StairwayToHeavenMod.init()}.
     *
     * <p>The five IDs are spelled out as literals rather than built from
     * {@code RealmDepth.keyOf}: {@code tools/locale_audit.py} and
     * {@code tools/content_ledger.py} read registrations out of the SOURCE, and
     * an ID assembled at runtime is an ID neither tool can see — which is
     * exactly how content ships nameless. {@link #idFor} keeps the two spellings
     * from drifting and throws if they ever do.
     */
    public static void register() {
        BossScaling.register();
        registerPortal("bossportalskyreach", RealmDepth.REALM_SKYREACH,
                SPRITE_SKYREACH, new Color(196, 206, 219));
        registerPortal("bossportaleden", RealmDepth.REALM_EDEN,
                SPRITE_EDEN, new Color(120, 198, 132));
        registerPortal("bossportalsteinfeld", RealmDepth.REALM_STEINFELD,
                SPRITE_STEINFELD, new Color(172, 178, 188));
        registerPortal("bossportalghostrealm", RealmDepth.REALM_GHOST,
                SPRITE_GHOST, new Color(120, 150, 132));
        registerPortal("bossportalcrookedbeyond", RealmDepth.REALM_CROOKED,
                SPRITE_CROOKED, new Color(168, 96, 150));
        // Hell gets none: SkyBossLadder.forRealm returns null there and §B4
        // reserves its boss rather than placing it.
    }

    private static void registerPortal(String stringID, int realm, String sprite, Color mapColor) {
        if (!stringID.equals(idFor(realm))) {
            throw new IllegalStateException(
                    "boss portal ID drift: registered \"" + stringID
                            + "\" but idFor(" + realm + ") says \"" + idFor(realm) + "\"");
        }
        // 0.0F broker value and isObtainable=false: nothing can hold one, so
        // there is no item icon to draw and no way to place a second.
        BY_REALM[realm] = ObjectRegistry.registerObject(stringID,
                new BossPortalObject(realm, sprite, mapColor), 0.0F, false);
    }

    /**
     * Warms the five borrowed sheets, client-side. Called once, from
     * {@code StairwayToHeavenMod.initResources()} — the same hook
     * {@code GhostRealm.loadTextures} uses for the same reason.
     *
     * <p>Two reasons it exists rather than each object simply loading its own.
     * {@code GameTexture.fromFile} caches by path, so this makes every
     * instance's own load a cache hit. And more importantly:
     * {@code tools/locale_audit.py}'s {@code texture_load_sites} can only check
     * a texture path it can read as a STRING LITERAL at the call site. A sheet
     * loaded through a field — which {@link #loadTextures()} must do, because
     * the path differs per realm — is a sheet nothing checks, and a mistyped
     * one ships as the engine's red ERR tile standing in the world. Writing the
     * five literals here once puts them all back under the audit.
     */
    public static void loadBorrowedSheets() {
        GameTexture.fromFile("objects/wardenbeaconon");
        GameTexture.fromFile("objects/skystairwaydown");
        GameTexture.fromFile("objects/statues/seraph");
        GameTexture.fromFile("objects/statues/gloomraven");
        GameTexture.fromFile("objects/veilriftdown");
    }

    /** The registered string ID of a realm's portal, built the one way. */
    public static String idFor(int realm) {
        return "bossportal" + RealmDepth.keyOf(realm);
    }

    /** The registered object ID of a realm's portal, or 0 if it has none. */
    public static int portalID(int realm) {
        if (realm < 0 || realm >= BY_REALM.length) {
            return 0;
        }
        return BY_REALM[realm];
    }

    // ------------------------------------------------------------------
    // the object
    // ------------------------------------------------------------------

    /** Which realm's boss this portal answers to. */
    public final int realm;

    /** The sheet this portal borrows, by literal path. */
    public final String sprite;

    public GameTexture texture;

    public BossPortalObject(int realm, String sprite, Color mapColor) {
        this.realm = realm;
        this.sprite = sprite;
        this.mapColor = mapColor;
        // A landmark should be findable on the map, and its name is what tells
        // a player which realm's key piece they are missing.
        this.displayMapTooltip = true;
        this.toolType = ToolType.UNBREAKABLE;
        this.isLightTransparent = true;
        this.lightLevel = 50;
        // Fixed rather than derived from the sheet: loadTextures() never runs on
        // a dedicated server, so `texture` is null there and a hitbox computed
        // from it would be a crash on the one machine that matters. One tile
        // plus one tile of headroom covers every borrowed sheet's footprint.
        this.hoverHitbox = new Rectangle(0, -32, 32, 64);
        this.setItemCategory("objects", "misc");
        this.setCraftingCategory("objects", "misc");
    }

    @Override
    public void loadTextures() {
        super.loadTextures();
        this.texture = GameTexture.fromFile(this.sprite);
    }

    @Override
    public void addDrawables(List<LevelSortedDrawable> list, OrderableDrawables tileList, Level level,
            int tileX, int tileY, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        GameLight light = level.getLightLevel(tileX, tileY);
        // RoyalEggObject.addDrawables' own maths (:124-128): centre a sheet
        // wider than one tile, and stand it on the tile's bottom edge.
        int drawX = camera.getTileDrawX(tileX) - (this.texture.getWidth() - 32) / 2;
        int drawY = camera.getTileDrawY(tileY) - (this.texture.getHeight() - 32);
        final TextureDrawOptions options = this.texture.initDraw().light(light).pos(drawX, drawY);
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
    public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
        GameLight light = level.getLightLevel(tileX, tileY);
        int drawX = camera.getTileDrawX(tileX) - (this.texture.getWidth() - 32) / 2;
        int drawY = camera.getTileDrawY(tileY) - (this.texture.getHeight() - 32);
        this.texture.initDraw().light(light).alpha(alpha).draw(drawX, drawY);
    }

    /**
     * Only ever on the one plane. Nothing can hold a portal, so this is a belt
     * on top of the braces — but a creative-mode or admin placement on the
     * surface would stand a summoning stone somewhere its realm gate means
     * nothing.
     */
    @Override
    public String canPlace(Level level, int layerID, int x, int y, int rotation, boolean byPlayer, boolean ignoreOtherLayers) {
        return !level.getIdentifier().equals(SkyRegistry.SKYREACH_IDENTIFIER)
                ? "invalidlevel"
                : super.canPlace(level, layerID, x, y, rotation, byPlayer, ignoreOtherLayers);
    }

    @Override
    public boolean canInteract(Level level, int x, int y, PlayerMob player) {
        return true;
    }

    @Override
    public String getInteractTip(Level level, int x, int y, PlayerMob perspective, boolean debug) {
        return Localization.translate("controls", "usetip");
    }

    @Override
    public void interact(Level level, int x, int y, PlayerMob player) {
        if (level.isServer() && player.isServerClient()) {
            ObjectEntity objectEntity = level.entityManager.getObjectEntity(x, y);
            if (objectEntity instanceof BossPortalObjectEntity) {
                ((BossPortalObjectEntity) objectEntity).use(level.getServer(), player.getServerClient());
            }
        }
        super.interact(level, x, y, player);
    }

    @Override
    public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
        return new BossPortalObjectEntity(level, this.getStringID(), this.realm, x, y);
    }
}
