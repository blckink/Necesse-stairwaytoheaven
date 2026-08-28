package stairwaytoheaven.mobs;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketMobChat;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.HomesickCritterAI;
import necesse.entity.mobs.friendly.critters.CritterMob;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import stairwaytoheaven.quest.SkywatchQuestData;

/**
 * The Warden's runaway spire cats. Each world has exactly two — the black
 * one hiding in the Stormveil and the white-tabby one in the Aurora Shoals.
 * Invulnerable, never despawn, tethered to a home point (lair before, the
 * spire's basket spot after being brought home with a Cloudpuff Treat).
 *
 * Subclasses only choose which cat this is (MobRegistry needs distinct
 * no-arg-constructible classes per stringID).
 */
public abstract class SpireCatMob extends CritterMob {

    public static GameTexture blackTexture;
    public static GameTexture tabbyTexture;

    protected final boolean isBlackCat;

    protected SpireCatMob(boolean isBlackCat) {
        super(50);
        this.isBlackCat = isBlackCat;
        // DESIGN INVARIANT — Siggi and Peanut must never be permanently lost.
        // Three native mechanisms carry that, and they are coupled:
        //   1. canTakeDamage() == false below gates EVERY damage entry point in
        //      Mob (verified against the 1.3.2 jar: isHit/addHealth/setHealth
        //      all check it; setHealth only ever allows increases when false).
        //   2. canDespawn = false stops CritterMob's distance despawn.
        //   3. CritterMob.shouldSave() is `shouldSave && !canDespawn()` — so
        //      (2) is ALSO what makes them save-persistent. Flipping canDespawn
        //      back to true would silently make them despawn AND stop being
        //      written to the save, and since SkywatchQuestData.catsSpawned
        //      stays true they would never respawn. Do not change this line.
        this.canDespawn = false;
        this.setSpeed(24.0F);
        this.setFriction(2.5F);
        this.collision = new Rectangle(-6, -4, 12, 8);
        this.hitBox = new Rectangle(-8, -8, 16, 14);
        this.selectBox = new Rectangle(-9, -18, 18, 22);
    }

    @Override
    public boolean canTakeDamage() {
        return false;
    }

    /** Mob.canInteract defaults to false — required for the treat/feeding interaction to fire at all. */
    @Override
    public boolean canInteract(necesse.entity.mobs.Mob mob) {
        return mob != null && mob.isPlayer;
    }

    /**
     * The tile init() last tethered this cat to. Kept so /skyreachstatus can
     * report the LIVE tether rather than recomputing what it ought to be — the
     * whole question after a coax is whether the AI was actually rebuilt around
     * the basket, and a recomputation cannot answer that.
     */
    private Point aiHomeTile;

    @Override
    public void init() {
        super.init();
        HomesickCritterAI<SpireCatMob> ai = new HomesickCritterAI<>(this);
        Level level = this.getLevel();
        if (level != null && level.isServer()) {
            SkywatchQuestData quest = SkywatchQuestData.get(level);
            ai.homeTile = new Point(this.homeX(quest), this.homeY(quest));
            this.aiHomeTile = new Point(ai.homeTile);
        }
        this.ai = new BehaviourTreeAI<>(this, ai);
    }

    /** Diagnostics: where this cat's homesick tether currently points. */
    public Point getAiHomeTile() {
        return this.aiHomeTile;
    }

    private boolean isHome(SkywatchQuestData quest) {
        return this.isBlackCat ? quest.blackHome : quest.tabbyHome;
    }

    private int homeX(SkywatchQuestData quest) {
        if (this.isHome(quest)) {
            return quest.basketX;
        }
        return this.isBlackCat ? quest.blackLairX : quest.tabbyLairX;
    }

    private int homeY(SkywatchQuestData quest) {
        if (this.isHome(quest)) {
            return quest.basketY;
        }
        return this.isBlackCat ? quest.blackLairY : quest.tabbyLairY;
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

        if (this.isHome(quest)) {
            // He lives here now. Saying so matters: the whole reason a player
            // reported "Siggi gefunden und Snack gegeben aber danach nie wieder
            // gesehen" is that being brought home said nothing about WHERE home
            // is, and nothing at the spire showed that a cat lives there.
            this.bubble("wardencatpurr");
            client.sendChatMessage(new LocalMessage("misc", "wardencatathome"));
            return;
        }
        int treats = player.getInv().main.getAmount(level, player, ItemRegistry.getItem("cloudpufftreat"), "skywatch");
        if (treats <= 0) {
            this.bubble("wardencatfound1");
            client.sendChatMessage(new LocalMessage("misc", "wardencatnotreat"));
            return;
        }
        player.getInv().main.removeItems(level, player, ItemRegistry.getItem("cloudpufftreat"), 1, "skywatch");
        if (this.isBlackCat) {
            quest.blackHome = true;
        } else {
            quest.tabbyHome = true;
        }
        // ...and in the world record, which no level unload can drop. See
        // SkywatchWorldData: the level copy went missing across a restart on
        // some world seeds, and this is the write that cannot.
        stairwaytoheaven.quest.SkywatchWorldData world =
                stairwaytoheaven.quest.SkywatchWorldData.get(level.getServer());
        if (world != null) {
            world.markCatHome(this.isBlackCat);
        }
        // Push the new state into every player's journal copy (auto-syncs)
        if (level.getServer() != null) {
            stairwaytoheaven.quest.SkyQuests.syncCatQuests(level.getServer(), quest);
        }
        // Name the destination and put it back on the map. "vanishes homeward"
        // is not an address, and the spire marker may never have been delivered
        // (or may have been deleted); onLocator is idempotent per player.
        client.sendChatMessage(new LocalMessage("misc", "wardencattreat",
                "dir", new LocalMessage("misc", SkywatchQuestData.directionKey(
                        this.getTileX(), this.getTileY(), quest.spireX, quest.spireY)).translate(),
                "dist", String.valueOf(tileDistance(this.getTileX(), this.getTileY(),
                        quest.spireX, quest.spireY))));
        stairwaytoheaven.quest.SkyMapMarkers.onLocator(client, quest);
        this.bubble("wardencatfound1");
        this.sendHome(level, quest);
    }

    private static int tileDistance(int fromX, int fromY, int toX, int toY) {
        int dx = toX - fromX;
        int dy = toY - fromY;
        return (int) Math.round(Math.sqrt((double) dx * dx + (double) dy * dy));
    }

    /**
     * Vanishes in a puff of cloud and reappears at the spire basket spot.
     *
     * Public because {@code /skyreachstatus cats} drives it: "the cat is at the
     * basket after a save/load" is only worth anything as an observed fact, and
     * the only way to observe it headlessly is to actually send them home.
     */
    public void sendHome(Level level, SkywatchQuestData quest) {
        if (!quest.spirePlaced) {
            return;  // no basket tile exists yet; teleporting to 0,0 would lose them
        }
        this.spawnCloudPuff();
        level.regionManager.ensureTileIsLoaded(quest.basketX, quest.basketY);
        this.setPos(quest.basketX * 32 + 16, quest.basketY * 32 + 16, true);
        // Re-file the mob under its NEW region immediately instead of waiting
        // for the next EntityList tick. Region.onUnloaded (jar 1.3.2,
        // Region.java:407-417) walks getSaveToRegion and calls
        // limitWithinRegionBounds + remove on every mob still listed there, so
        // a lair region that unloads inside that one-tick window would clamp
        // the cat back into the lair it just left.
        if (level.entityManager.mobs.getRegionList() != null) {
            level.entityManager.mobs.getRegionList().updateRegion(this);
        }
        this.init();  // rebuild the AI so its home tether points at the basket
        this.spawnCloudPuff();
    }

    /** True once this cat has been coaxed home (world state, not position). */
    public boolean isHomeFlag(SkywatchQuestData quest) {
        return this.isHome(quest);
    }

    private void spawnCloudPuff() {
        for (int i = 0; i < 10; i++) {
            this.getLevel().entityManager
                    .addParticle(this.x, this.y, Particle.GType.IMPORTANT_COSMETIC)
                    .movesConstant(
                            (float) (GameRandom.globalRandom.getIntBetween(4, 14) * (GameRandom.globalRandom.nextBoolean() ? -1 : 1)),
                            (float) (GameRandom.globalRandom.getIntBetween(4, 14) * (GameRandom.globalRandom.nextBoolean() ? -1 : 1)))
                    .color(new Color(228, 236, 242));
        }
    }

    private void bubble(String miscKey) {
        this.getLevel().getServer().network.sendToClientsWithEntity(
                new PacketMobChat(this.getUniqueID(), new LocalMessage("misc", miscKey)), this);
    }

    private GameTexture texture() {
        return this.isBlackCat ? blackTexture : tabbyTexture;
    }

    @Override
    public void addDrawables(List<MobDrawable> list, OrderableDrawables tileList, OrderableDrawables topList,
            Level level, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
        GameTexture texture = this.texture();
        if (texture == null) {
            return;
        }
        GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
        int drawX = camera.getDrawX(x) - 16;
        int drawY = camera.getDrawY(y) - 26;
        int dir = this.getDir();
        Point sprite = this.getAnimSprite(x, y, dir);
        drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
        final TextureDrawOptionsEnd drawOptions = texture
                .initDraw()
                .sprite(sprite.x, sprite.y, 32)
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

    /** Siggi — the Warden's black cat, sulking in the Stormveil. */
    public static class Black extends SpireCatMob {
        public Black() {
            super(true);
        }
    }

    /** Peanut — the white-tabby, chasing glowmoths in the Aurora Shoals. */
    public static class Tabby extends SpireCatMob {
        public Tabby() {
            super(false);
        }
    }
}
