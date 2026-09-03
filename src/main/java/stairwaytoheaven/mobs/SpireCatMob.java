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
import stairwaytoheaven.util.TileText;

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
        this.rebuildHomeTether();
    }

    /**
     * A cat that changed level keeps its AI, so {@link #init()} is NOT run
     * again: {@code EntityList.addHidden} (jar 1.3.2, EntityList.java:205-209)
     * calls {@code init()} only for an entity that was never initialised and
     * {@code onLevelChanged()} for one that was. That is the hook a cross-level
     * travel arrives through, and without rebuilding here the cat would land in
     * its new home carrying a tether to the tile it left.
     */
    @Override
    public void onLevelChanged() {
        super.onLevelChanged();
        this.rebuildHomeTether();
    }

    /**
     * Points the homesick AI at wherever home currently is.
     *
     * <p>Only ever at a tile on the cat's OWN level: {@code HomesickCritterAI}
     * walks the critter toward {@code homeTile}, and a tile belonging to another
     * dimension is a place it can walk toward forever. Getting the cat to a home
     * on another level is {@link #sendHome} 's job, not the pathfinder's; when
     * the home is elsewhere the tether is left at the cat's own position (the
     * {@code HomesickCritterAI} default) so it simply wanders in place until it
     * is moved.
     */
    private void rebuildHomeTether() {
        HomesickCritterAI<SpireCatMob> ai = new HomesickCritterAI<>(this);
        Level level = this.getLevel();
        if (level != null && level.isServer()) {
            // mayLoadSkyreach = false: this runs inside mob load and level
            // change, and pulling a whole dimension into memory from there is
            // not something a tether is worth.
            stairwaytoheaven.quest.CatHome.Spot home = stairwaytoheaven.quest.CatHome
                    .resolve(level.getServer(), level, this.isBlackCat, false);
            if (home != null && home.isOn(level)) {
                ai.homeTile = new Point(home.tileX, home.tileY);
            }
            this.aiHomeTile = new Point(ai.homeTile);
        }
        this.ai = new BehaviourTreeAI<>(this, ai);
    }

    /** Diagnostics: where this cat's homesick tether currently points. */
    public Point getAiHomeTile() {
        return this.aiHomeTile;
    }

    /**
     * Has this cat been coaxed home with a Cloudpuff Treat?
     *
     * <p>Read from the WORLD record, not from the Skyreach's level data: a cat
     * living in a Surface town is nowhere near that level, and asking
     * {@code SkywatchQuestData.get} for it there would attach an empty copy to
     * the wrong level.
     */
    public boolean isCoaxedHome() {
        Level level = this.getLevel();
        if (level == null || !level.isServer()) {
            return false;
        }
        stairwaytoheaven.quest.SkywatchWorldData world =
                stairwaytoheaven.quest.SkywatchWorldData.get(level.getServer());
        return world != null && world.isCatCoaxed(this.isBlackCat);
    }

    @Override
    public void interact(PlayerMob player) {
        super.interact(player);
        if (!this.isServer() || !player.isServerClient()) {
            return;
        }
        ServerClient client = player.getServerClient();
        Level level = this.getLevel();

        if (this.isCoaxedHome()) {
            // He lives here now. Saying so matters: the whole reason a player
            // reported "Siggi gefunden und Snack gegeben aber danach nie wieder
            // gesehen" is that being brought home said nothing about WHERE home
            // is, and nothing at the spire showed that a cat lives there.
            this.bubble("wardencatpurr");
            stairwaytoheaven.quest.CatHome.Spot home = stairwaytoheaven.quest.CatHome
                    .placed(level.getServer());
            // The address floats over the cat itself, not into a chat log.
            // A cat says "Prrrrt"; the coordinates are the game answering for
            // it, so they go where the player is already looking.
            TileText.at(client, this.getTileX(), this.getTileY(), home == null
                    ? new LocalMessage("misc", "wardencatathome")
                    : new LocalMessage("misc", "wardencatathomebasket",
                            "x", String.valueOf(home.tileX), "y", String.valueOf(home.tileY)));
            return;
        }
        // A cat that has NOT been coaxed is still wild, and a wild cat is always
        // in its Skyreach lair -- nothing moves one off that level before the
        // treat. So this is the one place the sky's own level data is the right
        // thing to read, and reading it anywhere else would attach an empty
        // copy to a level it does not describe.
        if (!stairwaytoheaven.SkyRegistry.SKYREACH_IDENTIFIER.equals(level.getIdentifier())) {
            this.bubble("wardencatfound1");
            return;
        }
        SkywatchQuestData quest = SkywatchQuestData.get(level);
        int treats = player.getInv().main.getAmount(level, player, ItemRegistry.getItem("cloudpufftreat"), "skywatch");
        if (treats <= 0) {
            this.bubble("wardencatfound1");
            TileText.at(client, this.getTileX(), this.getTileY(),
                    new LocalMessage("misc", "wardencatnotreat"));
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
        stairwaytoheaven.quest.CatHome.Spot home = stairwaytoheaven.quest.CatHome
                .placed(level.getServer());
        if (home != null) {
            // A basket is down somewhere: that, not the spire, is where this cat
            // is about to go, and telling the player the spire would send them
            // to the wrong dimension to look. The level name goes in as a
            // GameMessage, not a translated String -- a String would be resolved
            // HERE, in the server's language, for every player.
            TileText.at(client, this.getTileX(), this.getTileY(),
                    new LocalMessage("misc", "wardencattreatbasket",
                            "x", String.valueOf(home.tileX), "y", String.valueOf(home.tileY))
                            .addReplacement("level", home.level.getDisplayName()));
        } else {
            TileText.at(client, this.getTileX(), this.getTileY(),
                    new LocalMessage("misc", "wardencattreat",
                            "dir", new LocalMessage("misc", SkywatchQuestData.directionKey(
                                    this.getTileX(), this.getTileY(), quest.spireX, quest.spireY)).translate(),
                            "dist", String.valueOf(tileDistance(this.getTileX(), this.getTileY(),
                                    quest.spireX, quest.spireY))));
        }
        stairwaytoheaven.quest.SkyMapMarkers.onLocator(client, quest);
        this.bubble("wardencatfound1");
        this.sendHome(level);
    }

    private static int tileDistance(int fromX, int fromY, int toX, int toY) {
        int dx = toX - fromX;
        int dy = toY - fromY;
        return (int) Math.round(Math.sqrt((double) dx * dx + (double) dy * dy));
    }

    /**
     * Vanishes in a puff of cloud and reappears at home -- the basket the player
     * put down if there is one, the spire's basket otherwise, on whatever level
     * that is.
     *
     * Public because {@code /skyreachstatus cats} drives it: "the cat is at the
     * basket after a save/load" is only worth anything as an observed fact, and
     * the only way to observe it headlessly is to actually send them home.
     */
    public void sendHome(Level level) {
        if (level == null || !level.isServer()) {
            return;
        }
        stairwaytoheaven.quest.CatHome.Spot home = stairwaytoheaven.quest.CatHome
                .resolve(level.getServer(), level, this.isBlackCat, true);
        if (home == null) {
            // No home is known yet (no basket, no stamped spire). Standing still
            // beats teleporting to 0,0, which is what the old spirePlaced guard
            // was protecting against.
            return;
        }
        if (home.isOn(level)) {
            this.spawnCloudPuff();
            level.regionManager.ensureTileIsLoaded(home.tileX, home.tileY);
            this.setPos(home.tileX * 32 + 16, home.tileY * 32 + 16, true);
            // Re-file the mob under its NEW region immediately instead of
            // waiting for the next EntityList tick. Region.onUnloaded (jar
            // 1.3.2, Region.java:407-417) walks getSaveToRegion and calls
            // limitWithinRegionBounds + remove on every mob still listed there,
            // so a lair region that unloads inside that one-tick window would
            // clamp the cat back into the lair it just left.
            if (level.entityManager.mobs.getRegionList() != null) {
                level.entityManager.mobs.getRegionList().updateRegion(this);
            }
            this.init();  // rebuild the AI so its home tether points at the basket
            this.spawnCloudPuff();
            return;
        }
        this.travelToLevel(level, home);
    }

    /**
     * Cross-level travel, through vanilla's own mechanism.
     *
     * <p>{@code setPos} cannot cross a dimension -- it moves a mob inside the
     * level it is filed under. {@code TeleportEvent} is what vanilla uses for a
     * mob that has to end up somewhere else entirely
     * ({@code SettlersWorldData.returnToSettlement} calls it on a settler in
     * exactly this shape), and it does the whole job: it resolves or GENERATES
     * the destination level ({@code World.getLevel}), asks the check for a
     * position, and hands the mob to {@code EntityManager.changeMobLevel}, which
     * re-files it and calls {@code onLevelChanged()} -- where the tether is
     * rebuilt around the new home.
     *
     * <p>The position in a {@code TeleportResult} is in PIXELS, not tiles:
     * {@code TeleportEvent.performTeleport} feeds it straight to
     * {@code setPos}/{@code changeMobLevel}, and {@code changeMobLevel} passes
     * it to {@code addMob(mob, (float) x, (float) y)}. Passing tiles here would
     * drop the cat 32x closer to the origin than the basket -- the same mistake
     * that once put a settler thousands of tiles into the wilderness.
     *
     * <p>Delay 0 means the move happens synchronously inside
     * {@code events.add} ({@code LevelEventsManager.addHidden} calls
     * {@code event.init()} directly), so by the time this returns the cat is
     * already there. Sickness time 0: teleport sickness is a player debuff and
     * a cat did not choose to travel.
     */
    private void travelToLevel(Level level, stairwaytoheaven.quest.CatHome.Spot home) {
        this.spawnCloudPuff();
        final int tileX = home.tileX;
        final int tileY = home.tileY;
        final necesse.engine.util.LevelIdentifier target = home.level;
        necesse.entity.levelEvent.TeleportEvent teleport = new necesse.entity.levelEvent.TeleportEvent(
                this, 0, target, 0.0F, null,
                destination -> {
                    if (destination == null) {
                        return new necesse.engine.util.TeleportResult(false, null);
                    }
                    destination.regionManager.ensureTileIsLoaded(tileX, tileY);
                    return new necesse.engine.util.TeleportResult(true, target,
                            tileX * 32 + 16, tileY * 32 + 16);
                });
        // No second puff here: TeleportEvent sends its own visual to every
        // client that can see the mob, at both ends.
        level.entityManager.events.add(teleport);
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
        // The cats are drawn on vanilla's DUCK sheet shape now -- 384x320, six
        // 64px columns over four direction rows -- not the 32px critter grid the
        // rest of SkyCritterMob uses. Offsets and the bobbing call are DuckMob's
        // own (VERIFIED [jar], DuckMob.java:108-118): a 64px cell centred on a
        // mob needs -30/-48, not -16/-26, or the cat stands a half tile
        // north-west of its own shadow.
        int drawX = camera.getDrawX(x) - 30;
        int drawY = camera.getDrawY(y) - 48;
        int dir = this.getDir();
        Point sprite = this.getAnimSprite(x, y, dir);
        drawY += this.getBobbing(x, y);
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
