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
 * The Sky Warden — the last keeper of the Skywatch and the quest giver of
 * "The Warden's Call". Stationary, invulnerable, save-persistent.
 *
 * All quest logic is interact-driven and server-authoritative: turn-ins use
 * the vanilla DeliverItemsQuest idiom (count what is in the player's own
 * inventory, then remove on success — never trusting the client). Progress
 * lives in the level's {@link SkywatchQuestData}, shared by all players;
 * whoever delivers receives the rewards.
 */
public class SkyWardenMob extends FriendlyMob {

    public static GameTexture texture;

    private static final int BEACON_SHARDS = 12;
    private static final int BEACON_SILK = 8;
    private static final int ANCHOR_BARS = 5;
    private static final int ANCHOR_STONE = 20;

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

        switch (quest.stage) {
            case 0:
                say(client, "wardenintro1");
                say(client, "wardenintro2");
                say(client, "wardenintro3");
                give(client, "windsilk", 6);
                quest.stage = 1;
                sayBeaconTask(client);
                break;
            case 1:
                if (tryTurnIn(client, "stormshard", BEACON_SHARDS, "windsilk", BEACON_SILK)) {
                    quest.stage = 2;
                    igniteBeacon(level, quest);
                    say(client, "wardenbeacondone");
                    give(client, "flickerlightgarland", 2);
                } else {
                    sayBeaconTask(client);
                    say(client, "wardenbeaconwait");
                }
                break;
            default:
                interactAfterBeacon(client, level, quest);
                break;
        }
    }

    private void sayBeaconTask(ServerClient client) {
        say(client, new LocalMessage("misc", "wardenbeacon1"));
        say(client, new LocalMessage("misc", "wardenbeacon2",
                "shards", String.valueOf(BEACON_SHARDS), "silk", String.valueOf(BEACON_SILK)));
    }

    private void interactAfterBeacon(ServerClient client, Level level, SkywatchQuestData quest) {
        // One-time intros for the two parallel quests
        if (!quest.catsIntroShown) {
            quest.catsIntroShown = true;
            say(client, "wardencats1");
            say(client, "wardencats2");
            say(client, "wardencats3");
            say(client, catDirectionsMessage("wardencats4", quest));
            give(client, "cloudpufftreat", 3);
            return;
        }
        if (!quest.anchorIntroShown) {
            quest.anchorIntroShown = true;
            say(client, "wardenanchor1");
            say(client, "wardenanchor2");
            return;
        }
        // Cats reward
        if (quest.blackHome && quest.tabbyHome && !quest.catsRewardGiven) {
            quest.catsRewardGiven = true;
            say(client, "wardencatsdone");
            give(client, "catbasket", 1);
            give(client, "silverbell", 1);
            placeBasket(level, quest);
            return;
        }
        // Anchor turn-in
        if (!quest.anchorDone) {
            if (tryTurnIn(client, "aetheriumbar", ANCHOR_BARS, "skystone", ANCHOR_STONE)) {
                quest.anchorDone = true;
                say(client, "wardenanchordone");
                give(client, "skywatchbanner", 1);
                give(client, "aurorapetal", 5);
                placeAnchor(level, quest);
                return;
            }
        }
        // Finale once everything is done
        if (quest.anchorDone && quest.catsRewardGiven && !quest.finaleShown) {
            quest.finaleShown = true;
            say(client, "wardenfinale");
            return;
        }
        // Idle lines reflecting what is still open
        if (!quest.blackHome || !quest.tabbyHome) {
            if (quest.blackHome != quest.tabbyHome) {
                say(client, "wardenonecat");
            } else {
                say(client, catDirectionsMessage("wardencatswait", quest));
            }
        } else if (!quest.anchorDone) {
            say(client, "wardenanchorwait");
        } else {
            say(client, "wardenanchordone");
        }
    }

    /** Server-authoritative delivery: both stacks must be present, then both are consumed. */
    private boolean tryTurnIn(ServerClient client, String itemA, int amountA, String itemB, int amountB) {
        PlayerMob player = client.playerMob;
        Level level = this.getLevel();
        boolean hasA = player.getInv().main.getAmount(level, player, ItemRegistry.getItem(itemA), "skywatch") >= amountA;
        boolean hasB = player.getInv().main.getAmount(level, player, ItemRegistry.getItem(itemB), "skywatch") >= amountB;
        if (!hasA || !hasB) {
            return false;
        }
        player.getInv().main.removeItems(level, player, ItemRegistry.getItem(itemA), amountA, "skywatch");
        player.getInv().main.removeItems(level, player, ItemRegistry.getItem(itemB), amountB, "skywatch");
        return true;
    }

    private void igniteBeacon(Level level, SkywatchQuestData quest) {
        swapObject(level, quest.beaconX, quest.beaconY, SkyRegistry.wardenBeaconOffID, SkyRegistry.wardenBeaconOnID);
    }

    private void placeBasket(Level level, SkywatchQuestData quest) {
        swapObject(level, quest.basketX, quest.basketY, 0, SkyRegistry.catBasketID);
    }

    private void placeAnchor(Level level, SkywatchQuestData quest) {
        // The anchor stands beside the beacon
        swapObject(level, quest.beaconX + 2, quest.beaconY + 1, 0, SkyRegistry.skyAnchorID);
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

    /** A misc line with <blackdir>/<tabbydir> filled with directions from the spire to each lair. */
    private GameMessage catDirectionsMessage(String miscKey, SkywatchQuestData quest) {
        return new LocalMessage("misc", miscKey,
                "blackdir", translatedDirection(quest.spireX, quest.spireY, quest.blackLairX, quest.blackLairY),
                "tabbydir", translatedDirection(quest.spireX, quest.spireY, quest.tabbyLairX, quest.tabbyLairY));
    }

    private static String translatedDirection(int fromX, int fromY, int toX, int toY) {
        return new LocalMessage("misc", SkywatchQuestData.directionKey(fromX, fromY, toX, toY)).translate();
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
