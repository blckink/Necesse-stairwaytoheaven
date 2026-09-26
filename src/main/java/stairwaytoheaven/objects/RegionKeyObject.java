package stairwaytoheaven.objects;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.TeleportResult;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;
import necesse.level.maps.hudManager.floatText.ChatBubbleText;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.bosses.BossPortalSites;
import stairwaytoheaven.journal.JournalWorldData;
import stairwaytoheaven.quest.SkywatchWorldData;
import stairwaytoheaven.worldgen.SkyOrigin;
import stairwaytoheaven.worldgen.RealmDepth;
import stairwaytoheaven.util.TileText;

/**
 * A region key piece — the thing the player stands up at home to wake one
 * realm's boss portals.
 *
 * <p>{@code docs/FOGKEY_AND_BOSSPORTALS.md} §B1-B2, in full: <i>"Each region's
 * key piece is the reward of an Elder quest tied to that region... The key
 * piece is a buildable object: Mr. Knott's red door for Crooked, a statue for
 * Steinfeld, and so on. Stand the key piece in your base and that region's boss
 * portals unlock. Before that they are inert."</i> The player's own wording for
 * what it should look like: <i>"die können in den welten einfach aussehen wie
 * die jew. station bzw vom elder erhaltene teil dass man aufbaut in base
 * (crooked door, besondere statue o.Ä.)"</i>
 *
 * <h2>It wears its realm's portal sheet, and that is the whole point</h2>
 * §B3 asks that a boss portal <i>"look like the region's key piece, so a player
 * recognises what they need"</i>. That is a statement about TWO objects, and it
 * is only true if both read from the same file — so every constant here is the
 * matching {@code bosses/BossPortalObject.SPRITE_*} path, spelled out again
 * rather than imported so that {@code tools/locale_audit.py} can see it as a
 * literal (the same reason {@code BossPortalObject.loadBorrowedSheets} exists).
 * A player who meets a Summoning Stone in the Aftergarden and a Raven's Perch
 * in their own base is looking at one picture twice; that recognition IS the
 * signpost, and there is no tooltip that would do the job as well.
 *
 * <p>No new pixel art was drawn for any of this. Every sheet and every icon
 * below already exists in this repository, and each borrow has a row in
 * {@code docs/VANILLA_ASSET_MAP.md} §1.6 with its exact pixel size.
 *
 * <h2>Only inside a settlement</h2>
 * §B2 says the key piece stands in your BASE. {@link #canPlace} enforces that
 * exactly the way {@code objects/SeanceCircleObject} already does — the same
 * seam vanilla's own {@code SettlementFlagObject.canPlace} uses — and
 * {@link #placeObject} does the unlock the way vanilla's
 * {@code HomestoneObject.placeObject} does its own settlement bookkeeping
 * (HomestoneObject.java:101-105, VERIFIED [jar]): call {@code super} first,
 * then act, server-side only.
 *
 * <p>It stays minable, because it is furniture and not a fixture — but mining
 * one does NOT re-lock the realm. {@code SkywatchWorldData.unlockBossPortals}
 * is documented as never un-recording, and a player who tidies their base
 * should not lose a realm they already earned.
 */
public class RegionKeyObject extends SkyDecoObject {

    // ------------------------------------------------------------------
    // the sheets, one per realm — each one its summoning stone's own arch
    // ------------------------------------------------------------------

    /*
     * 2026-09-26: every key piece wears its realm's summoning-stone arch
     * (objects/bossportals/<realm>.png, 64x96, drawn centred on its tile).
     * The stones moved to those arches on 2026-09-15 and the keys were left on
     * the old borrowed sheets, so the Eden key still wore the Skyward Stairway
     * - the player: "was soll Gartenstiege von Eden machen? ... Ich hab keine
     * Ahnung". §B3's "a stone looks like its key piece" holds again, and a
     * placed key IS a small gate to its stone (see interact).
     */
    public static final String SHEET_SKYREACH = "bossportals/skyreach";
    public static final String SHEET_EDEN = "bossportals/eden";
    public static final String SHEET_STEINFELD = "bossportals/steinfeld";
    public static final String SHEET_GHOST = "bossportals/ghost";
    public static final String SHEET_CROOKED = "bossportals/crooked";
    public static final String SHEET_HELL = "bossportals/hell";

    /**
     * Inventory icons: the arch itself, box-downscaled to 32x32 once from the
     * sheet above (items/regionkey<realm>.png, 2026-09-26) - a reduction of
     * the existing art, nothing redrawn.
     */
    public static final String ICON_SKYREACH = "items/regionkeyskyreach";
    public static final String ICON_EDEN = "items/regionkeyeden";
    public static final String ICON_STEINFELD = "items/regionkeysteinfeld";
    public static final String ICON_GHOST = "items/regionkeyghostrealm";
    public static final String ICON_CROOKED = "items/regionkeycrookedbeyond";
    public static final String ICON_HELL = "items/regionkeyhell";

    // ------------------------------------------------------------------
    // registration
    // ------------------------------------------------------------------

    /** Registered object ID per realm; {@code 0} where a realm has none. */
    private static final int[] BY_REALM = new int[RealmDepth.REALM_COUNT];

    /**
     * Registers the six key pieces. Called once, from
     * {@code StairwayToHeavenMod.registerObjects()}.
     *
     * <p>Six literals rather than a loop over {@code RealmDepth.keyOf}, for
     * the reason {@link stairwaytoheaven.bosses.BossPortalObject#register}
     * gives for its own five: {@code tools/locale_audit.py} and
     * {@code tools/content_ledger.py} read registrations out of the SOURCE, and
     * an ID assembled at runtime is an ID neither tool can see. {@link #idFor}
     * keeps the two spellings from drifting and throws if they ever do.
     *
     * <p>Hell used to get none, because it had no boss portal and a key for
     * it would have unlocked nothing. It has one now
     * ({@link stairwaytoheaven.bosses.BossPortalObject#SPRITE_HELL}), so it
     * has a key.
     */
    public static void register() {
        registerKey("regionkeyskyreach", RealmDepth.REALM_SKYREACH,
                SHEET_SKYREACH, 64, ICON_SKYREACH, new Color(196, 206, 219));
        registerKey("regionkeyeden", RealmDepth.REALM_EDEN,
                SHEET_EDEN, 64, ICON_EDEN, new Color(120, 198, 132));
        registerKey("regionkeysteinfeld", RealmDepth.REALM_STEINFELD,
                SHEET_STEINFELD, 64, ICON_STEINFELD, new Color(172, 178, 188));
        registerKey("regionkeyghostrealm", RealmDepth.REALM_GHOST,
                SHEET_GHOST, 64, ICON_GHOST, new Color(120, 150, 132));
        registerKey("regionkeycrookedbeyond", RealmDepth.REALM_CROOKED,
                SHEET_CROOKED, 64, ICON_CROOKED, new Color(168, 96, 150));
        registerKey("regionkeyhell", RealmDepth.REALM_HELL,
                SHEET_HELL, 64, ICON_HELL, new Color(186, 84, 52));
    }

    private static void registerKey(String stringID, int realm, String worldSheet,
            int sheetWidth, String iconPath, Color mapColor) {
        if (!stringID.equals(idFor(realm))) {
            throw new IllegalStateException(
                    "region key ID drift: registered \"" + stringID
                            + "\" but idFor(" + realm + ") says \"" + idFor(realm) + "\"");
        }
        // Obtainable: it IS the quest reward, so the player has to be able to
        // hold one, drop one and pick it back up. 40.0F is the broker value the
        // mod's own seraphstatue carries (SkyCloudmarbleSet), and this is the
        // same statue.
        BY_REALM[realm] = ObjectRegistry.registerObject(stringID,
                new RegionKeyObject(worldSheet, sheetWidth, realm, iconPath, mapColor), 40.0F, true);
    }

    /**
     * Warms every sheet these five read, client-side. Called once, from
     * {@code StairwayToHeavenMod.initResources()}.
     *
     * <p>Exists for the same two reasons {@code BossPortalObject
     * .loadBorrowedSheets} does: {@code GameTexture.fromFile} caches by path so
     * every later load is a cache hit, and — the important one —
     * {@code tools/locale_audit.py}'s {@code texture_load_sites} can only check
     * a path it reads as a STRING LITERAL at the call site. Both halves of this
     * class load through a field, because the path differs per realm:
     * {@code SkyDecoObject.loadTextures} builds {@code "objects/" +
     * textureName} for the world sheet, and {@link #generateItemTexture()}
     * reads {@link #iconPath} for the icon. Without these eight lines a
     * mistyped path would ship as the engine's red ERR tile — standing in the
     * world, or sitting in the player's bag — and nothing would have caught it.
     *
     * <p>Twelve paths: six arches, six icons.
     */
    public static void loadBorrowedArt() {
        // world sheets -- each one its realm's summoning stone's own arch
        GameTexture.fromFile("objects/bossportals/skyreach");
        GameTexture.fromFile("objects/bossportals/eden");
        GameTexture.fromFile("objects/bossportals/steinfeld");
        GameTexture.fromFile("objects/bossportals/ghost");
        GameTexture.fromFile("objects/bossportals/crooked");
        GameTexture.fromFile("objects/bossportals/hell");
        // inventory icons
        GameTexture.fromFile("items/regionkeyskyreach");
        GameTexture.fromFile("items/regionkeyeden");
        GameTexture.fromFile("items/regionkeysteinfeld");
        GameTexture.fromFile("items/regionkeyghostrealm");
        GameTexture.fromFile("items/regionkeycrookedbeyond");
        GameTexture.fromFile("items/regionkeyhell");
    }

    /** The registered string ID of a realm's key piece, built the one way. */
    public static String idFor(int realm) {
        return "regionkey" + RealmDepth.keyOf(realm);
    }

    /** The registered object ID of a realm's key piece, or 0 if it has none. */
    public static int keyID(int realm) {
        if (realm < 0 || realm >= BY_REALM.length) {
            return 0;
        }
        return BY_REALM[realm];
    }

    // ------------------------------------------------------------------
    // the object
    // ------------------------------------------------------------------

    /** Which realm's portals this piece wakes. */
    public final int realm;

    /** The icon sheet this piece borrows, by literal path. */
    private final String iconPath;

    public RegionKeyObject(String worldSheet, int sheetWidth, int realm, String iconPath, Color mapColor) {
        // collision: vanilla's own statue box (StatueObject.java:35,
        // VERIFIED [jar]) -- these are statues, doors and standing beacons, and
        // a player should not walk through one.
        super(worldSheet, sheetWidth, mapColor, new Rectangle(2, 10, 28, 18), "objects", "misc");
        this.realm = realm;
        this.iconPath = iconPath;
        // It is the Elder's quest reward and the key to a realm: in the
        // inventory it sorts with the other quest items (the Ghost Chalk sets
        // the same precedent), not in "objects.misc" with the gates and
        // crates. Still an object -- it is placed, and the ID is in saves.
        // The crafting tree has no questitems node, so that side stays
        // objects.misc (it has no recipe either way). docs/ITEM_CATEGORIES.md.
        this.setItemCategory("misc", "questitems");
        this.displayMapTooltip = true;
        this.isLightTransparent = true;
        // Furniture, not a fixture: minable, and vanilla's 100 health -- the
        // number it reserves for statues, pillars and banners
        // (AncientPillarObject.java:32, BannerObject.java:42) rather than the
        // 50 it gives chairs and flower pots.
        this.setTool(ToolType.PICKAXE);
        this.setObjectHealth(100);
        // The arch is 64x96 on one tile, drawn centred and standing on the
        // tile's bottom edge - BossPortalObject's own hover box.
        this.hoverHitbox = new Rectangle(-16, -64, 64, 96);
    }

    /** {@code journal.realm<N>}, in {@code RealmDepth.REALM_*} order: the realm's name as the Chronicle writes it. */
    private static final String[] REALM_NAME_KEYS = {"realm0", "realm1", "realm2", "realm3", "realm4", "realm5"};

    private LocalMessage realmName() {
        return new LocalMessage("journal", REALM_NAME_KEYS[this.realm]);
    }

    /** {@code mob.<boss>}: the boss its stones call. */
    private LocalMessage bossName() {
        return new LocalMessage("mob", stairwaytoheaven.journal.JournalBuilder.bossOf(this.realm));
    }

    /**
     * What the piece is for, in the bag. Until 2026-09-26 the tooltip said
     * nothing but vanilla's "can be placed", and the player asked what the Eden
     * one was supposed to do.
     */
    @Override
    public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
        ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
        tooltips.add(new StringTooltips(new LocalMessage("itemtooltip", "regionkeytip",
                "realm", this.realmName(), "boss", this.bossName()).translate(), 400));
        tooltips.add(new StringTooltips(new LocalMessage("itemtooltip", "regionkeytraveltip",
                "realm", this.realmName()).translate(), 400));
        return tooltips;
    }

    @Override
    public boolean canInteract(Level level, int x, int y, PlayerMob player) {
        return true;
    }

    @Override
    public String getInteractTip(Level level, int x, int y, PlayerMob perspective, boolean debug) {
        return Localization.translate("misc", "regionkeyusetip");
    }

    /**
     * A standing key piece is a gate to its realm's nearest summoning stone.
     *
     * <p>Only once the realm's stones are awake (a piece set down outside a
     * settlement wakes nothing and leads nowhere), and only for a player who
     * has been in that realm themself ({@code JournalWorldData.hasVisited}) -
     * in co-op the piece somebody else earned does not carry a newcomer past
     * the Veil. Per player: everyone who uses it travels alone. The way back is
     * vanilla's: walk, or a Recall Scroll.
     */
    @Override
    public void interact(Level level, int x, int y, PlayerMob player) {
        if (level.isServer() && player != null && player.isServerClient()) {
            this.travel(level, x, y, player.getServerClient());
        }
        super.interact(level, x, y, player);
    }

    private void travel(Level level, int x, int y, ServerClient client) {
        Server server = level.getServer();
        if (!SkywatchWorldData.bossPortalsUnlocked(server, this.realm)) {
            TileText.at(client, x, y, new LocalMessage("misc", "regionkeyasleep", "realm", this.realmName()));
            return;
        }
        JournalWorldData seen = JournalWorldData.get(server);
        if (seen != null && !seen.hasVisited(client.authentication, this.realm)) {
            TileText.at(client, x, y, new LocalMessage("misc", "regionkeynotvisited", "realm", this.realmName()));
            return;
        }
        int seed = SkyOrigin.worldGenSeed(level.getWorldEntity());
        Point from = level.getIdentifier().equals(SkyRegistry.SKYREACH_IDENTIFIER) ? new Point(x, y) : null;
        final Point landing = BossPortalSites.landingNearestTo(seed, this.realm, from);
        if (landing == null) {
            TileText.at(client, x, y, new LocalMessage("misc", "regionkeynostone", "realm", this.realmName()));
            return;
        }
        client.changeLevelCheck(SkyRegistry.SKYREACH_IDENTIFIER, dest -> {
            dest.regionManager.ensureTileIsLoaded(landing.x, landing.y);
            Point pos = GameUtils.getValidMobLocationAroundObject(GameRandom.globalRandom, dest,
                    client.playerMob, landing.x, landing.y, true);
            if (pos == null) {
                pos = new Point(landing.x * 32 + 16, landing.y * 32 + 16);
            }
            return new TeleportResult(true, pos);
        }, true);
    }

    /**
     * The inventory icon: cell (0,0) of {@link #iconPath}, 32x32.
     *
     * <p>{@code GameObject.generateItemTexture} (GameObject.java:791) is hard
     * coded to {@code items/<stringID>.png}, and {@code GameTexture.fromFile}
     * swallows a miss and hands back the red ERR tile — so an obtainable object
     * whose icon was never drawn puts that tile in the player's bag. Reading
     * the path from a field instead is the same seam
     * {@code RockObject.generateItemTexture} (RockObject.java:116) uses, and
     * the one {@code realms/ghost/GhostDecoObject} already uses in this mod.
     */
    @Override
    public GameTexture generateItemTexture() {
        return new GameTexture(GameTexture.fromFile(this.iconPath), 0, 0, 32);
    }

    /**
     * Only inside a settlement.
     *
     * <p>§B2's <i>"stand the key piece in your base"</i>, enforced exactly the
     * way {@code SeanceCircleObject.canPlace} enforces A2's <i>"zuhause in der
     * basis (sonst geht es nicht)"</i>: call {@code super} first, then return a
     * non-null error key. A non-null answer greys the preview out on the client
     * AND is re-checked on the server by {@code ObjectItem.canPlace}, so this
     * is not a courtesy a crafted packet could walk past.
     *
     * <p>{@code SettlementsWorldData.hasSettlementAtTile} is region-granular —
     * a settlement claims whole 32x32-tile regions — so "in the base" means the
     * same tiles the settlement machinery itself works in. Any level: a base on
     * the surface is still a base, and the unlock it writes is world-scoped.
     */
    @Override
    public String canPlace(Level level, int layerID, int x, int y, int rotation, boolean byPlayer,
            boolean ignoreOtherLayers) {
        String error = super.canPlace(level, layerID, x, y, rotation, byPlayer, ignoreOtherLayers);
        if (error != null) {
            return error;
        }
        SettlementsWorldData settlements = SettlementsWorldData.getSettlementsData(level);
        if (settlements == null || !settlements.hasSettlementAtTile(level, x, y)) {
            return "swhnotsettlement";
        }
        return null;
    }

    /**
     * ...and say why, instead of leaving the player to guess why the preview
     * will not go down. Same shape as {@code SeanceCircleObject.attemptPlace},
     * which follows {@code LadderDownObject.attemptPlace}'s own "notsurface"
     * (LadderDownObject.java:140-144).
     */
    @Override
    public void attemptPlace(Level level, int x, int y, PlayerMob player, String error) {
        if (level.isClient() && "swhnotsettlement".equals(error)) {
            player.getLevel().hudManager.addElement(
                    new ChatBubbleText(player, Localization.translate("misc", "regionkeyneedsettlement")));
        }
    }

    /**
     * Standing it up wakes the realm.
     *
     * <p>The WRITE side of §B2, and the one call the whole boss-portal slice was
     * waiting for. Shaped after {@code HomestoneObject.placeObject}
     * (HomestoneObject.java:101-105, VERIFIED [jar]), which is vanilla's own
     * example of an object that writes settlement-scoped truth when it is put
     * down: {@code super} first, then {@code if (level.isServer())}.
     *
     * <p>The settlement test is asked AGAIN here rather than trusted from
     * {@link #canPlace}, for the reason {@code SeanceCircleObject.interact}
     * gives for asking it twice: {@code canPlace} only runs on the player
     * placement path, and an object written straight into a level — by worldgen,
     * by a preset, by an admin command — never sees it. A key piece stamped into
     * the world by something that is not a player must not unlock a realm.
     */
    @Override
    public void placeObject(Level level, int layerID, int x, int y, int rotation, boolean byPlayer) {
        super.placeObject(level, layerID, x, y, rotation, byPlayer);
        if (!byPlayer || !level.isServer()) {
            return;
        }
        SettlementsWorldData settlements = SettlementsWorldData.getSettlementsData(level);
        if (settlements == null || !settlements.hasSettlementAtTile(level, x, y)) {
            return;
        }
        if (SkywatchWorldData.bossPortalsUnlocked(level.getServer(), this.realm)) {
            return; // already earned; a second statue is decoration.
        }
        SkywatchWorldData.unlockBossPortals(level.getServer(), this.realm);
        // Over the piece that was just stood up, not in a chat log -- the same
        // place onPlaceFail above already answers from, and the same mechanism
        // vanilla's own EggNestObject uses (see stairwaytoheaven.util.TileText).
        TileText.atAll(level.getServer(), level, x, y,
                new LocalMessage("misc", "regionkeyunlocked", "key", this.getDisplayName()));
    }
}
