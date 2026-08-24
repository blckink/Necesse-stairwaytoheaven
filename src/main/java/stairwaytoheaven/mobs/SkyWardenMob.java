package stairwaytoheaven.mobs;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketMobChat;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ItemRegistry;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.FriendlyMob;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.quest.SkywatchQuestData;

/**
 * The Sky Warden — the last keeper of the Skywatch, resident of the Old
 * Warden Spire and the entry point of the whole Skyreach progression.
 *
 * v0.5 DESIGN: the Warden is the first major goal. The old four-stage fetch
 * chain is gone; his flow is now:
 *   stage 0 — first meeting: intro dialogue, "find the spire" journal quest
 *             completes, he opens up about the Skywatch.
 *   stage 1 — recruitment: he offers to leave the sky and join the player's
 *             surface settlement for {@link #RECRUIT_COST} coins (server-
 *             authoritative inventory check, vanilla DeliverItems idiom).
 *             On payment he lights the beacon (the Skywatch wakes up), hands
 *             over his contract (a vanilla mob spawn item) and departs.
 *   stage 2 — he is gone: the player uses the contract at home and builds his
 *             new tower with the unlocked Skywatch building set. The settler
 *             Warden (see {@link WardenSettlerMob}) takes over as the
 *             progression interface.
 *
 * All interaction is interact-driven and server-authoritative: turn-ins use
 * the vanilla DeliverItemsQuest idiom (count what is in the player's own
 * inventory, then remove on success — never trusting the client). Progress
 * lives in the level's {@link SkywatchQuestData}, shared by all players.
 */
public class SkyWardenMob extends FriendlyMob {

    public static GameTexture texture;

    /**
     * The recruitment price. Intentional design value: it equals the top
     * vanilla settlement expansion tier (100,000 coins), so an endgame player
     * who has finished incursion-tier content pays a meaningful but achievable
     * lump sum — the single largest NPC purchase in the mod, benchmarked
     * against the wiki economy (Elder's priciest stock item is ~6,000).
     */
    public static final int RECRUIT_COST = 100_000;

    public SkyWardenMob() {
        super(500);
        this.canDespawn = false;
        this.setSpeed(0.0F);
        this.collision = new Rectangle(-10, -7, 20, 14);
        this.hitBox = new Rectangle(-14, -12, 28, 24);
        this.selectBox = new Rectangle(-16, -48, 32, 58);
    }

    @Override
    public boolean canTakeDamage() {
        return false;
    }

    /**
     * Mob.canInteract defaults to FALSE — without this override the client
     * never offers the interact prompt and PacketPlayerMobInteract drops the
     * request server-side, so interact() would be dead code.
     */
    @Override
    public boolean canInteract(necesse.entity.mobs.Mob mob) {
        return mob != null && mob.isPlayer;
    }

    @Override
    public void interact(PlayerMob player) {
        super.interact(player);
        if (!this.isServer() || !player.isServerClient()) {
            return;
        }
        ServerClient client = player.getServerClient();
        Level level = this.getLevel();
        SkywatchQuestData quest = SkywatchQuestData.get(level);

        necesse.engine.network.server.Server server = level.getServer();
        switch (quest.stage) {
            case 0:
                // First meeting: intro, then he opens up. The "find the spire"
                // journal quest completes here for everyone.
                say(client, "wardenintro1");
                say(client, "wardenintro2");
                say(client, "wardenintro3");
                give(client, "windsilk", 6);
                quest.stage = 1;
                stairwaytoheaven.quest.SkyQuests.removeAllOfType(server, stairwaytoheaven.quest.FindSpireQuest.class);
                sayRecruitmentOffer(client);
                break;
            case 1:
                if (tryRecruit(client, level, quest)) {
                    say(client, "wardenrecruitdone1");
                    say(client, "wardenrecruitdone2");
                } else {
                    sayRecruitmentOffer(client);
                    say(client, "wardenrecruitwait");
                }
                break;
            default:
                // Recruited worlds should not still have him here; a stale mob
                // (e.g. from a hand-edited save) politely says goodbye.
                say(client, "wardenfarewell");
                break;
        }
    }

    /** The recruitment pitch: what he offers and what it costs. */
    private void sayRecruitmentOffer(ServerClient client) {
        say(client, "wardenrecruit1");
        say(client, "wardenrecruit2");
    }

    /**
     * Server-authoritative recruitment — the 100,000-coin payment IS the
     * recruitment transaction, exactly like vanilla's coin hiring of world
     * NPCs ("most NPCs can be hired with coins ... and become settlers in the
     * player's settlement" — wiki). There is no item: the server consumes the
     * coins, then performs the transfer itself:
     *
     *   1. consume {@link #RECRUIT_COST} coins from the paying player's own
     *      inventory (vanilla DeliverItems idiom, never trusting the client),
     *   2. light the beacon — the Skywatch wakes up,
     *   3. create the settler Warden (a real {@link WardenSettlerMob}, i.e. a
     *      HumanShop settler) on the SURFACE level, placed with the Elder's
     *      own placement recipe (setHome → Waystone.findTeleportLocation →
     *      entityManager.addMob) at the stairway this player ascended from —
     *      the verified, persisted spot at the heart of their base,
     *   4. remove this sky-side warden; the player assigns his bed/home
     *      through the normal settlement menu afterwards.
     */
    private boolean tryRecruit(ServerClient client, Level level, SkywatchQuestData quest) {
        PlayerMob player = client.playerMob;
        // The transfer target: this player's bound surface stairway. Everyone
        // who reached the spire through a stairway has one; without it we
        // refuse before taking any coins rather than guessing a destination.
        long[] homeTile = quest.getReturnStairway(client.authentication);
        if (homeTile == null) {
            say(client, "wardenrecruitnohome");
            return false;
        }
        necesse.inventory.item.Item coin = ItemRegistry.getItem("coin");
        if (player.getInv().main.getAmount(level, player, coin, "skywatch") < RECRUIT_COST) {
            return false;
        }
        player.getInv().main.removeItems(level, player, coin, RECRUIT_COST, "skywatch");

        quest.stage = 2;
        quest.recruited = true;
        quest.recruitedAuth = client.authentication;
        igniteBeacon(level, quest);

        // The transfer: spawn the settler on the surface level (always loaded)
        // at the player's home-side stairway, Elder-preset placement recipe.
        Level surface = level.getServer().world.getLevel(necesse.engine.util.LevelIdentifier.SURFACE_IDENTIFIER);
        WardenSettlerMob settler = new WardenSettlerMob();
        int homeX = (int) homeTile[0];
        int homeY = (int) homeTile[1] + 1; // just below the stairway pad
        settler.setHome(new java.awt.Point(homeX, homeY));
        java.awt.Point spot = necesse.level.maps.levelData.settlementData.Waystone
                .findTeleportLocation(surface, homeX, homeY, settler);
        surface.entityManager.addMob(settler, spot.x * 32 + 16, spot.y * 32 + 16);

        // This sky-side keeper departs; the settler at home takes over.
        this.remove();
        return true;
    }

    private void igniteBeacon(Level level, SkywatchQuestData quest) {
        swapObject(level, quest.beaconX, quest.beaconY, SkyRegistry.wardenBeaconOffID, SkyRegistry.wardenBeaconOnID);
    }

    private void swapObject(Level level, int tileX, int tileY, int expectedID, int newID) {
        level.regionManager.ensureTileIsLoaded(tileX, tileY);
        if (expectedID == 0 && level.getObjectID(tileX, tileY) != 0) {
            level.setObject(tileX, tileY, 0);
        }
        if (expectedID == 0 || level.getObjectID(tileX, tileY) == expectedID) {
            level.setObject(tileX, tileY, newID);
            level.getServer().network.sendToClientsWithTile(
                    new necesse.engine.network.packet.PacketChangeObject(level, 0, tileX, tileY, newID),
                    level, tileX, tileY);
        }
    }

    private void say(ServerClient client, String miscKey) {
        say(client, new LocalMessage("misc", miscKey));
    }

    /** Speech bubble for everyone nearby + chat line for the interacting player. */
    private void say(ServerClient client, GameMessage message) {
        this.getLevel().getServer().network.sendToClientsWithEntity(
                new PacketMobChat(this.getUniqueID(), message), this);
        client.sendChatMessage(new LocalMessage("misc", "wardenchatformat", "name",
                new LocalMessage("misc", "wardenname").translate(), "line", message.translate()));
    }

    /** Give items to the delivering player; anything that does not fit drops at their feet. */
    private void give(ServerClient client, String itemStringID, int amount) {
        PlayerMob player = client.playerMob;
        Level level = this.getLevel();
        InventoryItem item = new InventoryItem(itemStringID, amount);
        boolean added = player.getInv().main.addItem(level, player, item, "skywatchreward", null);
        if (!added && item.getAmount() > 0) {
            level.entityManager.pickups.add(new ItemPickupEntity(level, item, player.x, player.y, 0.0F, 0.0F));
        }
    }

    @Override
    public void addDrawables(List<MobDrawable> list, OrderableDrawables tileList, OrderableDrawables topList,
            Level level, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
        if (texture == null) {
            return;
        }
        GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
        int drawX = camera.getDrawX(x) - 32;
        int drawY = camera.getDrawY(y) - 54;
        int dir = this.getDir();
        Point sprite = this.getAnimSprite(x, y, dir);
        drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
        final TextureDrawOptionsEnd drawOptions = texture
                .initDraw()
                .sprite(sprite.x, sprite.y, 64)
                .light(light)
                .pos(drawX, drawY);
        list.add(new MobDrawable() {
            @Override
            public void draw(TickManager tickManager) {
                drawOptions.draw();
            }
        });
        this.addShadowDrawables(tileList, level, x, y, light, camera);
    }
}
