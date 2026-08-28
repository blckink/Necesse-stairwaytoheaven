package stairwaytoheaven.commands;

import java.util.HashMap;
import java.util.Map;

import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyCloudmarbleSet;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.level.SkyLevel;

/**
 * Admin/debug command: forces the Skyreach to generate around the world origin
 * and reports terrain/biome statistics. This is what the headless integration
 * test drives (scripts/integration_test.sh) and it doubles as an in-game
 * diagnostics tool for bug reports.
 */
public class SkyreachStatusCommand extends ModularChatCommand {

    private static final int SCAN_RADIUS_TILES = 64;

    public SkyreachStatusCommand() {
        super("skyreachstatus", "Generates and inspects the Skyreach around the origin (debug)", PermissionLevel.ADMIN, false,
                // Optional mode. "cats" coaxes both spire cats home before
                // reporting, which is the only way to observe the travel-home
                // path headlessly -- and therefore the only way for
                // scripts/integration_test.sh to assert that a cat brought home
                // is still at its basket after a save/load round trip.
                // "dump" prints the stamped spire tile by tile (SPIREMAP lines)
                // so scripts can composite the real interior with real sprites:
                // the only way to actually LOOK at what the preset built.
                new necesse.engine.commands.CmdParameter("mode",
                        new necesse.engine.commands.parameterHandlers.StringParameterHandler("", "cats", "dump"), true));
    }

    @Override
    public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
        String mode = args.length > 0 && args[0] != null ? String.valueOf(args[0]) : "";
        Level level = server.world.getLevel(SkyRegistry.SKYREACH_IDENTIFIER);
        if (!(level instanceof SkyLevel)) {
            logs.add("FAIL: level for identifier \"" + SkyRegistry.SKYREACH_IDENTIFIER + "\" is " + level.getClass().getSimpleName()
                    + " (expected SkyLevel) - is the world generator registered?");
            return;
        }

        // Console commands run on the scanner thread while the server thread
        // ticks the level. The server thread's lock order is Level monitor ->
        // region locks (spawning, ensureWardenSpire); region generation from
        // THIS thread takes region locks first and then needs the Level
        // monitor inside generateRegion -> classic deadlock (reproduced once
        // critter spawning made server-side region activity constant). Taking
        // the Level monitor up front gives both threads the same lock order.
        synchronized (level) {
            runLocked(level, server, serverClient, mode, logs);
        }
    }

    private void runLocked(Level level, Server server, ServerClient serverClient, String mode, CommandLog logs) {
        int r = SCAN_RADIUS_TILES;
        // v0.5: the sky radiates from the canonical Old Warden Spire origin —
        // scan (and guarantee) the hub there, not around the world origin.
        java.awt.Point origin = stairwaytoheaven.worldgen.SkyOrigin.compute(((SkyLevel) level).getWorldGenSeed());
        level.regionManager.ensureTilesAreLoaded(origin.x - r, origin.y - r, origin.x + r, origin.y + r);
        // The spire is stamped at the canonical origin (idempotent); the
        // terrain painter guarantees solid Driftlands hub land around it.
        ((SkyLevel) level).ensureWardenSpire();

        if (mode.equals("dump")) {
            dumpSpire(level, origin, logs);
        }

        Map<String, Integer> tileCounts = new HashMap<>();
        Map<String, Integer> biomeCounts = new HashMap<>();
        Map<String, Integer> objectCounts = new HashMap<>();
        int total = 0;
        for (int x = origin.x - r; x <= origin.x + r; x++) {
            for (int y = origin.y - r; y <= origin.y + r; y++) {
                total++;
                tileCounts.merge(TileRegistry.getTileStringID(level.getTileID(x, y)), 1, Integer::sum);
                biomeCounts.merge(BiomeRegistry.getBiome(level.getBiomeID(x, y)).getStringID(), 1, Integer::sum);
                int objectID = level.getObjectID(x, y);
                if (objectID != 0) {
                    objectCounts.merge(level.getObject(x, y).getStringID(), 1, Integer::sum);
                }
            }
        }

        logs.add("Skyreach OK: class=" + level.getClass().getSimpleName()
                + " identifier=" + level.getIdentifier()
                + " dimension=" + level.getIdentifier().getOneWorldDimension()
                + " isCave=" + level.isCave);
        logs.add("Scanned " + total + " tiles in radius " + r + ":");
        tileCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(e -> logs.add("  tile " + e.getKey() + " x" + e.getValue()));
        biomeCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(e -> logs.add("  biome " + e.getKey() + " x" + e.getValue()));
        objectCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(e -> logs.add("  object " + e.getKey() + " x" + e.getValue()));

        diagnosePlacement(level, logs);
        diagnoseGeneration((SkyLevel) level, logs);
        if ("cats".equalsIgnoreCase(mode)) {
            coaxCatsHome((SkyLevel) level, logs);
        }
        diagnoseQuest((SkyLevel) level, logs);
        diagnoseCats((SkyLevel) level, logs);
        diagnoseQuestChain(logs);
        diagnoseHusbandry((SkyLevel) level, logs);
        diagnoseToolAudit(logs);
        diagnoseNetAudit(logs);
        diagnoseWorkstations(logs);
        locateFromPlayer((SkyLevel) level, serverClient, logs);
        logs.add("SKYREACH_STATUS_DONE");
    }

    /**
     * When a player runs the command while standing in the Skyreach, print their
     * position plus distance and compass direction to the quest landmarks.
     */
    private void locateFromPlayer(SkyLevel level, ServerClient serverClient, CommandLog logs) {
        if (serverClient == null || serverClient.playerMob == null
                || !SkyRegistry.SKYREACH_IDENTIFIER.equals(serverClient.getLevelIdentifier())) {
            logs.add("locator: run this while standing in the Skyreach to get directions to the spire and cats");
            return;
        }
        stairwaytoheaven.quest.SkywatchQuestData quest = stairwaytoheaven.quest.SkywatchQuestData.get(level);
        int px = serverClient.playerMob.getTileX();
        int py = serverClient.playerMob.getTileY();
        logs.add("you are at " + px + "," + py);
        if (quest.spirePlaced) {
            logs.add("  Warden's Spire: " + bearing(px, py, quest.spireX, quest.spireY));
        }
        // Rescue path: a lost player running the locator also gets the spire
        // pinned on their world map (once — the sent-set persists).
        stairwaytoheaven.quest.SkyMapMarkers.onLocator(serverClient, quest);
        if (quest.catsSpawned) {
            if (!quest.blackHome) {
                logs.add("  Siggi (black cat): " + bearing(px, py, quest.blackLairX, quest.blackLairY));
            }
            if (!quest.tabbyHome) {
                logs.add("  Peanut (tabby cat): " + bearing(px, py, quest.tabbyLairX, quest.tabbyLairY));
            }
        }
    }

    /** "312 tiles NW, at -42,-74" (screen directions: N = up, W = left). */
    private static String bearing(int px, int py, int tx, int ty) {
        int dx = tx - px;
        int dy = ty - py;
        int dist = (int) Math.round(Math.sqrt((double) dx * dx + (double) dy * dy));
        if (dist == 0) {
            return "right here (" + tx + "," + ty + ")";
        }
        String ns = dy < 0 ? "N" : "S";
        String ew = dx < 0 ? "W" : "E";
        String dir;
        if (Math.abs(dx) > 2 * Math.abs(dy)) {
            dir = ew;
        } else if (Math.abs(dy) > 2 * Math.abs(dx)) {
            dir = ns;
        } else {
            dir = ns + ew;
        }
        return dist + " tiles " + dir + ", at " + tx + "," + ty;
    }

    /**
     * Prints the effective tool type and HP of every custom deco/prop object,
     * so scripts/integration_test.sh can assert the harvest-tool audit (the
     * GameObject pickaxe default is wrong for soft flora and woody trunks).
     */
    private void diagnoseToolAudit(CommandLog logs) {
        String[] ids = {"gloomwillow", "gloomshroom", "ashbones", "deadtree",
                "skywatchtelescope", "skywatchastrolabe", "stormscreed", "skywatchrubble",
                "chargecrystal", "withershrub", "aurorashards", "starfall",
                "skyballoon", "aeronautwreck", "skyparcel",
                "wardenbeaconoff", "wardenbeaconon", "skyanchor"};
        for (String id : ids) {
            necesse.level.gameObject.GameObject object =
                    necesse.engine.registries.ObjectRegistry.getObject(
                            necesse.engine.registries.ObjectRegistry.getObjectID(id));
            if (object == null) {
                logs.add("tool " + id + "=MISSING");
                continue;
            }
            logs.add("tool " + id + "=" + object.toolType.name() + "/" + object.objectHealth);
        }
    }

    /**
     * Every reachable save state of "The Warden's Call", and the chapter each
     * one is owed.
     *
     * This is the answer to "warden gibt weiterhin keine quests die ich finden
     * kann" as a MEASUREMENT rather than a claim. The failure mode is not a
     * crash and not a missing registration: it is a world whose flags land in a
     * combination the hand-out code has no branch for, after which the journal
     * stays empty forever and nothing in the game says why. Enumerating the
     * states here means a future change that reintroduces such a hole fails the
     * integration test instead of shipping.
     *
     * Names are the historical bug, not the code path: each row is a save the
     * mod could actually produce, and the three marked (old build) are the ones
     * that used to hand out nothing at all.
     */
    private void diagnoseQuestChain(CommandLog logs) {
        String[][] states = {
                // label                            recruited settler black tabby reward anchor
                {"fresh",                           "0", "0", "0", "0", "0", "0"},
                {"met-him-old-build",               "0", "0", "0", "0", "0", "0"},
                {"recruited",                       "1", "1", "0", "0", "0", "0"},
                {"legacy-settler-no-record",        "0", "1", "0", "0", "0", "0"},
                {"one-cat-home",                    "1", "1", "1", "0", "0", "0"},
                {"both-cats-home-never-had-quest",  "1", "1", "1", "1", "0", "0"},
                {"cats-paid-out",                   "1", "1", "1", "1", "1", "0"},
                {"anchored",                        "1", "1", "1", "1", "1", "1"},
        };
        StringBuilder line = new StringBuilder("chain check:");
        boolean dead = false;
        for (String[] state : states) {
            stairwaytoheaven.quest.SkywatchQuestData probe = new stairwaytoheaven.quest.SkywatchQuestData();
            probe.recruited = state[1].equals("1");
            boolean isSettler = state[2].equals("1");
            probe.blackHome = state[3].equals("1");
            probe.tabbyHome = state[4].equals("1");
            probe.catsRewardGiven = state[5].equals("1");
            probe.anchorDone = state[6].equals("1");
            stairwaytoheaven.mobs.SkyWardenMob.Chapter chapter =
                    stairwaytoheaven.mobs.SkyWardenMob.chapterFor(probe, isSettler);
            // Only the fully finished chain may hand out nothing.
            boolean finished = probe.anchorDone;
            if (chapter == stairwaytoheaven.mobs.SkyWardenMob.Chapter.DONE && !finished) {
                dead = true;
            }
            line.append(' ').append(state[0]).append('=').append(chapter.name());
        }
        line.append(dead ? " DEAD_END_REACHABLE" : " no-dead-ends");
        logs.add(line.toString());
    }

    /**
     * Coaxes both spire cats home, exactly the way a Cloudpuff Treat does.
     *
     * "The cat is at the spire after being brought home" is the whole player
     * report ("Siggi ... Snack gegeben aber danach nie wieder gesehen") and it
     * is a claim about the SAVE, not about the source: the cats survive a
     * restart only because CritterMob.shouldSave() is
     * `shouldSave && !canDespawn()`, and after a coax they must survive it at
     * a DIFFERENT position, in a different region, than the one they were
     * written into at world generation. Driving the real path here is what lets
     * the integration test observe that across a server restart.
     */
    private void coaxCatsHome(SkyLevel level, CommandLog logs) {
        stairwaytoheaven.quest.SkywatchQuestData quest = stairwaytoheaven.quest.SkywatchQuestData.get(level);
        if (!quest.spirePlaced || !quest.catsSpawned) {
            logs.add("cat coax: SKIPPED (spirePlaced=" + quest.spirePlaced + " catsSpawned=" + quest.catsSpawned + ")");
            return;
        }
        level.regionManager.ensureTileIsLoaded(quest.blackLairX, quest.blackLairY);
        level.regionManager.ensureTileIsLoaded(quest.tabbyLairX, quest.tabbyLairY);
        quest.blackHome = true;
        quest.tabbyHome = true;
        int sent = 0;
        for (necesse.entity.mobs.Mob mob : level.entityManager.mobs) {
            if (mob instanceof stairwaytoheaven.mobs.SpireCatMob) {
                ((stairwaytoheaven.mobs.SpireCatMob) mob).sendHome(level, quest);
                sent++;
            }
        }
        if (level.getServer() != null) {
            stairwaytoheaven.quest.SkyQuests.syncCatQuests(level.getServer(), quest);
        }
        logs.add("cat coax: sent " + sent + " cat(s) home to " + quest.basketX + "," + quest.basketY);
    }

    /**
     * Where the cats actually are, and whether "home" is a real place.
     *
     * Three separate things have to be true for a coaxed cat to be findable,
     * and each of them used to be unobserved: the basket tile has to carry a
     * basket (WardenSpirePreset reserved the tile and placed nothing on it),
     * the cat has to BE there, and its homesick tether has to point at the
     * basket rather than at the lair it came from.
     */
    private void diagnoseCats(SkyLevel level, CommandLog logs) {
        stairwaytoheaven.quest.SkywatchQuestData quest = stairwaytoheaven.quest.SkywatchQuestData.get(level);
        if (!quest.spirePlaced) {
            logs.add("cat home check: SKIPPED (no spire)");
            return;
        }
        level.regionManager.ensureTileIsLoaded(quest.basketX, quest.basketY);
        String basketObject = level.getObject(quest.basketX, quest.basketY).getStringID();
        StringBuilder line = new StringBuilder("cat home check: basket=")
                .append(quest.basketX).append(',').append(quest.basketY)
                .append(" object=").append(basketObject)
                .append(" homeFlags black=").append(quest.blackHome)
                .append(" tabby=").append(quest.tabbyHome);
        for (necesse.entity.mobs.Mob mob : level.entityManager.mobs) {
            if (!(mob instanceof stairwaytoheaven.mobs.SpireCatMob)) {
                continue;
            }
            stairwaytoheaven.mobs.SpireCatMob cat = (stairwaytoheaven.mobs.SpireCatMob) mob;
            java.awt.Point tether = cat.getAiHomeTile();
            int dx = cat.getTileX() - quest.basketX;
            int dy = cat.getTileY() - quest.basketY;
            int dist = (int) Math.round(Math.sqrt((double) dx * dx + (double) dy * dy));
            // Two different claims, and the strict one is the TETHER.
            // HomesickCritterAI only pulls a critter back once it is more than
            // 96px (3 tiles) from home, and the wanderer keeps moving while it
            // does, so a snapshot legitimately catches a cat several tiles out
            // and walking back -- an exact position is not a property the AI
            // has. What must be exact is where the tether points: that is what
            // init() rebuilds on load, and pointing it at the old lair is the
            // way "brought home" would silently stop meaning anything.
            // SPIRE_RADIUS is the tower's own interior, so AT_BASKET reads as
            // "a player who walks into the spire finds this cat".
            final int spireRadius = 8;
            boolean tetherOk = tether != null && tether.x == quest.basketX && tether.y == quest.basketY;
            String state;
            if (!cat.isHomeFlag(quest)) {
                state = " STILL_WILD";
            } else if (!tetherOk) {
                state = " WRONG_TETHER";
            } else if (dist > spireRadius) {
                state = " AWAY_FROM_BASKET";
            } else {
                state = " AT_BASKET";
            }
            line.append(" | ").append(cat.getStringID())
                    .append(" at=").append(cat.getTileX()).append(',').append(cat.getTileY())
                    .append(" d=").append(dist)
                    .append(" tether=").append(tether == null ? "none" : tether.x + "," + tether.y)
                    .append(state);
        }
        logs.add(line.toString());
    }

    /**
     * The Cloud Lamb as a husbandry animal, measured rather than asserted.
     *
     * The player's three questions were "was bringen sie jetzt?", "es gibt halt
     * schon normale schafe" and "was muss in Trog bei wolkenschafen?". All three
     * are answered by values the engine reads off the mob and the item, so all
     * three are printed here: what shearing yields, what the trough accepts
     * (FeedingTroughObjectEntity's filter is `instanceof GrainItem` and nothing
     * else), and what a lamb's offspring is - vanilla SheepMob breeds a 50%
     * chance of a plain `ram`.
     */
    private void diagnoseHusbandry(SkyLevel level, CommandLog logs) {
        necesse.entity.mobs.Mob probe = necesse.engine.registries.MobRegistry.getMob("cloudlamb", level);
        if (!(probe instanceof necesse.entity.mobs.friendly.HusbandryMob)) {
            logs.add("husbandry check: cloudlamb is NOT a HusbandryMob");
            return;
        }
        necesse.entity.mobs.friendly.HusbandryMob lamb =
                (necesse.entity.mobs.friendly.HusbandryMob) probe;
        java.util.ArrayList<necesse.inventory.InventoryItem> products = new java.util.ArrayList<>();
        necesse.inventory.InventoryItem shears = new necesse.inventory.InventoryItem("shears");
        String shorn = "CANNOT SHEAR";
        if (lamb.canShear(shears)) {
            lamb.onShear(shears, products);
            StringBuilder sb = new StringBuilder();
            for (necesse.inventory.InventoryItem product : products) {
                sb.append(sb.length() == 0 ? "" : "+").append(product.item.getStringID())
                        .append('x').append(product.getAmount());
            }
            shorn = sb.length() == 0 ? "NOTHING" : sb.toString();
        }
        StringBuilder feed = new StringBuilder();
        for (String feedID : new String[]{"cloudberry", "wheat", "skystone"}) {
            necesse.inventory.item.Item item = necesse.engine.registries.ItemRegistry.getItem(feedID);
            boolean handFeed = item != null && lamb.canFeed(new necesse.inventory.InventoryItem(feedID));
            // Exactly the predicate FeedingTroughObjectEntity.isValidFeed uses.
            boolean trough = item instanceof necesse.inventory.item.placeableItem.consumableItem.food.GrainItem;
            feed.append(' ').append(feedID).append("=hand:").append(handFeed).append("/trough:").append(trough);
        }
        logs.add("husbandry check: cloudlamb shear=" + shorn
                + " child=" + lamb.getRandomChildMobStringID(lamb)
                + " name=" + lamb.getLocalization().translate()
                + " feed:" + feed);
    }

    /**
     * Reports which sky critters implement NetableMob — the marker the vanilla
     * net checks (NetToolItem.canHitMob) — so the catchability decision is
     * asserted by the integration test instead of living only in the source.
     */
    private void diagnoseNetAudit(CommandLog logs) {
        for (necesse.entity.mobs.Mob mob : new necesse.entity.mobs.Mob[]{
                new stairwaytoheaven.mobs.SkyCritterMob.DewSnail()}) {
            logs.add("net " + mob.getStringID() + "="
                    + (mob instanceof necesse.entity.mobs.misc.NetableMob ? "NETABLE" : "not netable"));
        }
    }

    /**
     * The three Skywatch workstations, measured rather than asserted.
     *
     * A settler can only be put on a station if two separate things are true at
     * once, and neither is visible from the source of the object alone.
     * `SettlementStorageManager.assignWorkstation` accepts a tile only when its
     * object passes `instanceof SettlementWorkstationObject`, and the station is
     * only worth assigning if its Tech actually carries recipes — a recipe
     * registered against the wrong Tech, or after the mod recipe registry
     * closed, leaves a station that is assignable and has nothing to do. Both
     * are silent failures in game, so both are printed here.
     */
    private void diagnoseWorkstations(CommandLog logs) {
        String[][] stations = {
                {"windsilkloom", "windsilkloom"},
                {"aetherforge", "aetherforge"},
                {"stormglasskiln", "stormglasskiln"},
        };
        for (String[] station : stations) {
            necesse.level.gameObject.GameObject object =
                    necesse.engine.registries.ObjectRegistry.getObject(
                            necesse.engine.registries.ObjectRegistry.getObjectID(station[0]));
            if (object == null) {
                logs.add("workstation " + station[0] + "=MISSING");
                continue;
            }
            boolean isStation = object instanceof necesse.level.maps.levelData
                    .settlementData.SettlementWorkstationObject;
            boolean processing = isStation && ((necesse.level.maps.levelData
                    .settlementData.SettlementWorkstationObject) object)
                    .isProcessingInventory(null, 0, 0);
            int recipes = 0;
            StringBuilder results = new StringBuilder();
            try {
                necesse.inventory.recipe.Tech tech =
                        necesse.engine.registries.RecipeTechRegistry.getTech(station[1]);
                for (necesse.inventory.recipe.Recipe recipe
                        : necesse.inventory.recipe.Recipes.getRecipes(tech)) {
                    recipes++;
                    if (results.length() > 0) {
                        results.append('+');
                    }
                    results.append(recipe.resultItem.item.getStringID())
                            .append('x').append(recipe.resultAmount);
                }
            } catch (java.util.NoSuchElementException e) {
                results.append("TECH_MISSING");
            }
            logs.add("workstation " + station[0]
                    + " settlementWorkstation=" + isStation
                    + " processing=" + processing
                    + " recipes=" + recipes
                    + " makes=" + (results.length() == 0 ? "NOTHING" : results));
        }
    }

    /** Verifies the Warden's Spire, the NPCs and the quest data integrity. */
    private void diagnoseQuest(SkyLevel level, CommandLog logs) {
        stairwaytoheaven.quest.SkywatchQuestData quest = stairwaytoheaven.quest.SkywatchQuestData.get(level);
        logs.add("quest: stage=" + quest.stage + " spirePlaced=" + quest.spirePlaced
                + " recruited=" + quest.recruited
                + " spire=" + quest.spireX + "," + quest.spireY
                + " beacon=" + quest.beaconX + "," + quest.beaconY
                + " catsSpawned=" + quest.catsSpawned
                + " blackLair=" + quest.blackLairX + "," + quest.blackLairY
                + " tabbyLair=" + quest.tabbyLairX + "," + quest.tabbyLairY);
        if (quest.spirePlaced) {
            level.regionManager.ensureTileIsLoaded(quest.beaconX, quest.beaconY);
            String beaconObj = level.getObject(quest.beaconX, quest.beaconY).getStringID();
            String spireFloor = TileRegistry.getTileStringID(level.getTileID(quest.spireX, quest.spireY));
            logs.add("spire check: beaconObject=" + beaconObj + " wardenFloor=" + spireFloor);

            // The grand door and the tiles a player walks in over. The Spire's
            // four doors sit on the axes through the origin, and the forecourt
            // lamp ring once put a candelabra on the south one -- so the way in
            // is asserted here rather than assumed. originY is the beacon row;
            // the south wall is +7 from it, and the approach runs out to the
            // arrival pad and past it to the lamp ring's radius.
            int ox = quest.beaconX;
            int oy = quest.beaconY;
            StringBuilder approach = new StringBuilder();
            String doorObj = "?";
            for (int dy = 7; dy <= 11; dy++) {
                level.regionManager.ensureTileIsLoaded(ox, oy + dy);
                String id = level.getObject(ox, oy + dy).getStringID();
                if (dy == 7) {
                    doorObj = id;
                } else {
                    approach.append(dy == 8 ? "" : " ").append(id);
                }
            }
            boolean doorIsDoor = doorObj.contains("door");
            boolean approachClear = approach.toString().replace("air", "").trim().isEmpty();
            logs.add("entrance check: door=" + doorObj + " isDoor=" + doorIsDoor
                    + " approach=[" + approach + "]"
                    + " clear=" + approachClear);
        }
        // Force the regions the NPCs are anchored to into memory before
        // counting. Without this the count measures which regions happen to be
        // streamed in — after a restart the spire loads but a cat lair 60 tiles
        // out may not, which reads as "a cat is missing" when it is only
        // asleep on disk.
        level.regionManager.ensureTileIsLoaded(quest.spireX, quest.spireY);
        level.regionManager.ensureTileIsLoaded(quest.blackLairX, quest.blackLairY);
        level.regionManager.ensureTileIsLoaded(quest.tabbyLairX, quest.tabbyLairY);
        if (quest.blackHome || quest.tabbyHome) {
            level.regionManager.ensureTileIsLoaded(quest.basketX, quest.basketY);
        }
        long wardens = 0, cats = 0, lambs = 0;
        for (necesse.entity.mobs.Mob mob : level.entityManager.mobs) {
            String id = mob.getStringID();
            if (id.equals("skywarden")) {
                wardens++;
            } else if (id.startsWith("spirecat")) {
                cats++;
            } else if (id.equals("cloudlamb")) {
                // Placed at region generation, not by a spawn table -- a sheep
                // can never satisfy Mob.isValidSpawnLocation. A zero here means
                // the flocks are gone again.
                lambs++;
            }
        }
        logs.add("npc check: wardens=" + wardens + " cats=" + cats + " cloudlambs=" + lambs
                + " (spire + lair regions forced loaded)");

        // Settler wiring for the recruited Warden. HumanMob.getSettler() resolves
        // settlerStringID through SettlerRegistry, and LevelSettler's constructor
        // does Objects.requireNonNull on the result — an unregistered settler can
        // never move into a settlement, so the 100,000-coin payoff would dead-end.
        necesse.level.maps.levelData.settlementData.settler.Settler wardenSettler =
                necesse.engine.registries.SettlerRegistry.getSettler("wardensettler");
        logs.add("settler check: wardensettler="
                + (wardenSettler == null ? "NOT REGISTERED" : wardenSettler.getClass().getSimpleName())
                + " mobRegistered=" + (necesse.engine.registries.MobRegistry.getMobID("wardensettler") >= 0));

        // The recruit path itself. PacketShopContainerUpdate.recruitSettler
        // fails with "notsettler" when container.humanShop.getSettler() is
        // null, and getSettler() resolves the mob's OWN settlerStringID -- not
        // the settler registry key we happen to have registered elsewhere. The
        // sky-side Warden shipped with an unregistered key for two releases,
        // which is why the mod hand-rolled its own payment and spawned a second
        // mob at home. Assert the live wiring, not the registration.
        for (String mobID : new String[]{"skywarden", "wardensettler"}) {
            necesse.entity.mobs.Mob probe = necesse.engine.registries.MobRegistry.getMob(mobID, level);
            String settlerName = "NOT A HUMAN";
            String recruitPrice = "n/a";
            if (probe instanceof necesse.entity.mobs.friendly.human.HumanMob) {
                necesse.entity.mobs.friendly.human.HumanMob human =
                        (necesse.entity.mobs.friendly.human.HumanMob) probe;
                necesse.level.maps.levelData.settlementData.settler.Settler s = human.getSettler();
                settlerName = s == null ? "NULL (recruit would fail: notsettler)" : s.getClass().getSimpleName();
            }
            if (probe instanceof necesse.entity.mobs.friendly.human.humanShop.HumanShop) {
                java.util.List<necesse.inventory.InventoryItem> items =
                        ((necesse.entity.mobs.friendly.human.humanShop.HumanShop) probe).getRecruitItems(null);
                recruitPrice = items == null ? "NULL (recruit button dead)"
                        : items.isEmpty() ? "free"
                        : items.stream().map(i -> i.item.getStringID() + "x" + i.getAmount())
                                .reduce((a, b) -> a + "+" + b).orElse("free");
            }
            logs.add("recruit check: " + mobID + " settler=" + settlerName + " price=" + recruitPrice);
        }
        // ---- why nothing spawns -----------------------------------------
        // MobChance.spawnMob calls mob.isValidSpawnLocation and drops the mob
        // when it answers false, and Mob's own implementation is `return
        // false`. So a spawn table entry whose class never overrides it is
        // dead weight: it is drawn, it is rejected, nothing appears. The
        // player report was "kein einziger Gegner ... nur Critter" and "Schafe
        // noch nirgends gesehen", which are two different versions of that.
        // Ask the live objects rather than reading class hierarchies.
        // Sample WELL AWAY from the spire. The first version of this probe swept
        // radius 6..30, which the v0.7 landscape pass filled with a lamp-lit
        // forecourt -- so it was measuring "standing next to a candelabra",
        // where nothing is supposed to spawn, and reported a working fix as
        // broken. Static light is printed per tile below so that mistake cannot
        // be made silently again.
        java.util.List<java.awt.Point> probeTiles = new java.util.ArrayList<>();
        for (int r = 60; r <= 160 && probeTiles.size() < 6; r += 20) {
            for (int a = 0; a < 8 && probeTiles.size() < 6; a++) {
                int px = quest.spireX + (int) Math.round(Math.cos(a * Math.PI / 4) * r);
                int py = quest.spireY + (int) Math.round(Math.sin(a * Math.PI / 4) * r);
                level.regionManager.ensureTileIsLoaded(px, py);
                if (!level.isSolidTile(px, py) && !level.getTile(px, py).isLiquid) {
                    probeTiles.add(new java.awt.Point(px, py));
                }
            }
        }
        // Report the world clock alongside the light. Without it the probe
        // cannot tell "this level never darkens" apart from "the time command
        // did not take effect", and those need opposite fixes.
        necesse.engine.world.WorldEntity we = level.getWorldEntity();
        // Recompute before measuring. Ambient light is refreshed once per
        // Level.serverTick, so a probe run in the same tick as a /time change
        // would report the previous value and look like "this level never
        // darkens". This is the same call the tick makes.
        level.lightManager.updateAmbientLight();
        logs.add(String.format(java.util.Locale.ROOT,
                "spawn check: dayTime=%.3f worldAmbient=%.1f levelAmbient=%.1f isCave=%s",
                we.getDayTime(), we.getAmbientLight(),
                level.lightManager.getAmbientLight(), level.isCave));
        StringBuilder lightLine = new StringBuilder("spawn check: ambient+static / static-only at ")
                .append(probeTiles.size()).append(" land tiles =");
        for (java.awt.Point t : probeTiles) {
            lightLine.append(' ').append(String.format(java.util.Locale.ROOT, "%.0f/%.0f",
                    level.lightManager.getAmbientAndStaticLightLevelFloat(t.x, t.y),
                    level.lightManager.getStaticLight(t.x, t.y).getLevel()));
        }
        logs.add(lightLine.toString());

        String[] probeMobs = {"zephyrray", "skystonegolem", "stormwisp", "galehound",
                "dawnpiercer", "gloomshade", "cloudlamb", "glowmoth", "sparkbeetle",
                "zephyrfinch", "dewsnail"};
        // Measure each mob twice: at the level's real light, and again with the
        // ambient forced to darkness. Two numbers separate the two causes that
        // both look like "nothing spawns" -- a mob rejected only by the light
        // threshold accepts in the dark column, and a mob that inherits Mob's
        // `return false` is 0 in BOTH. Guessing between those from the class
        // hierarchy alone is what this probe exists to stop.
        necesse.level.maps.light.GameLight savedOverride = level.lightManager.ambientLightOverride;
        for (String mobID : probeMobs) {
            necesse.entity.mobs.Mob probe = necesse.engine.registries.MobRegistry.getMob(mobID, level);
            if (probe == null) {
                logs.add("spawn check: " + mobID + " NOT REGISTERED");
                continue;
            }
            boolean overrides = overridesSpawnValidation(probe.getClass());
            int lit = countAcceptedSpawns(level, mobID, probeTiles);
            level.lightManager.ambientLightOverride = level.lightManager.newLight(0.0F);
            level.lightManager.updateAmbientLight();
            int dark = countAcceptedSpawns(level, mobID, probeTiles);
            level.lightManager.ambientLightOverride = savedOverride;
            level.lightManager.updateAmbientLight();
            logs.add("spawn check: " + mobID + " threshold=" + probe.spawnLightThreshold.value
                    + " validSpawnLocation=" + (overrides ? "implemented" : "INHERITS Mob's false")
                    + " accepted lit=" + lit + "/" + probeTiles.size()
                    + " dark=" + dark + "/" + probeTiles.size());
        }

                logs.add("name check: skywarden=" + necesse.engine.localization.Localization.translate("mob", "skywardenname", "name", "Test")
                + " | wardensettler=" + necesse.engine.localization.Localization.translate("mob", "wardensettlername", "name", "Test"));
    }

    /**
     * Compares the world the server actually generated against the pure
     * decision function, and reports the built landscape it contains.
     *
     * {@link stairwaytoheaven.worldgen.SkyTerrainPainter#describeTile} is the
     * single source of truth for every Skyreach tile, and the offline map
     * renderer (scripts/sky_map_render.sh) calibrates against the same
     * function. This block is what keeps those two honest: if the field and
     * the painted world ever disagree, every calibration render becomes
     * fiction. Ground tiles are compared strictly — nothing removes a floor
     * after generation — everywhere outside the spire preset's own footprint,
     * which legitimately overwrites what the painter wrote.
     */
    /** How many of the probe tiles this mob would accept as a spawn location. */
    private static int countAcceptedSpawns(SkyLevel level, String mobID, java.util.List<java.awt.Point> tiles) {
        int accepted = 0;
        for (java.awt.Point t : tiles) {
            necesse.entity.mobs.Mob m = necesse.engine.registries.MobRegistry.getMob(mobID, level);
            if (m == null) {
                return 0;
            }
            java.awt.Point off = m.getPathMoveOffset();
            if (m.isValidSpawnLocation(level.getServer(), null, t.x * 32 + off.x, t.y * 32 + off.y)) {
                accepted++;
            }
        }
        return accepted;
    }

    /**
     * Walks up from a mob class to find whoever implements
     * {@code isValidSpawnLocation}. Mob's own version returns false, so a class
     * whose chain stops there can never be placed by a spawn table.
     */
    private static boolean overridesSpawnValidation(Class<?> type) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            if (c == necesse.entity.mobs.Mob.class) {
                return false;
            }
            try {
                c.getDeclaredMethod("isValidSpawnLocation",
                        necesse.engine.network.server.Server.class,
                        necesse.engine.network.server.ServerClient.class, int.class, int.class);
                return true;
            } catch (NoSuchMethodException ignored) {
                // keep walking
            }
        }
        return false;
    }

    /**
     * Prints the stamped spire, one line per tile, as
     * {@code SPIREMAP <lx> <ly> <tile> <base>:<rot> <tileLayer> <wallDecor> <tableDecor>}
     * in preset-local coordinates. The offline compositor in
     * scripts reads this back and draws the real sprites at 1x, which is the
     * only way to judge an interior: an ASCII plan that looks fine can still
     * read as a maze in game.
     */
    private void dumpSpire(Level level, java.awt.Point origin, CommandLog logs) {
        int size = stairwaytoheaven.worldgen.WardenSpirePreset.SIZE;
        int half = size / 2;
        int pad = 3;
        logs.add("SPIREMAP_BEGIN size=" + size + " pad=" + pad
                + " origin=" + origin.x + "," + origin.y);
        for (int ly = -pad; ly < size + pad; ly++) {
            for (int lx = -pad; lx < size + pad; lx++) {
                int x = origin.x - half + lx;
                int y = origin.y - half + ly;
                level.regionManager.ensureTileIsLoaded(x, y);
                StringBuilder line = new StringBuilder("SPIREMAP ");
                line.append(lx).append(' ').append(ly).append(' ')
                        .append(TileRegistry.getTileStringID(level.getTileID(x, y)));
                for (int layer : new int[]{0,
                        necesse.engine.registries.ObjectLayerRegistry.TILE_LAYER,
                        necesse.engine.registries.ObjectLayerRegistry.WALL_DECOR,
                        necesse.engine.registries.ObjectLayerRegistry.FENCE_AND_TABLE_DECOR}) {
                    line.append(' ')
                            .append(level.getObject(layer, x, y).getStringID())
                            .append(':')
                            .append(level.getObjectRotation(layer, x, y));
                }
                logs.add(line.toString());
            }
        }
        logs.add("SPIREMAP_END");
    }

    private void diagnoseGeneration(SkyLevel level, CommandLog logs) {
        int seed = level.getWorldGenSeed();
        int r = SCAN_RADIUS_TILES;
        java.awt.Point origin = stairwaytoheaven.worldgen.SkyOrigin.compute(seed);
        Map<Integer, Integer> expected = new HashMap<>();
        Map<Integer, Integer> actualObjects = new HashMap<>();
        int tileMismatches = 0;
        int paved = 0;
        int chequer = 0;
        int skywayGround = 0;
        for (int x = origin.x - r; x <= origin.x + r; x++) {
            for (int y = origin.y - r; y <= origin.y + r; y++) {
                long desc = stairwaytoheaven.worldgen.SkyTerrainPainter.describeTile(seed, x, y, origin.x, origin.y);
                int wantTile = stairwaytoheaven.worldgen.SkyTerrainPainter.descTile(desc);
                int wantObject = stairwaytoheaven.worldgen.SkyTerrainPainter.descObject(desc);
                if (wantObject != 0) {
                    expected.merge(wantObject, 1, Integer::sum);
                }
                if (wantTile == SkyRegistry.skyroadTileID) {
                    paved++;
                } else if (wantTile == SkyRegistry.skyplinthTileID) {
                    chequer++;
                } else if (wantTile == SkyCloudmarbleSet.skywayTileID) {
                    skywayGround++;
                }
                // The spire preset is stamped on top of the painter's work,
                // so everything it writes is expected to differ. The box comes
                // from the preset itself rather than a hand-copied number, so
                // the two cannot drift apart when the hall changes size — and
                // it deliberately stops one tile short of the plot edge, which
                // is where the forecourt's lamp ring and railing live.
                int spireBox = stairwaytoheaven.worldgen.WardenSpirePreset.WRITTEN_RADIUS;
                if (Math.abs(x - origin.x) <= spireBox && Math.abs(y - origin.y) <= spireBox) {
                    continue;
                }
                if (level.getTileID(x, y) != wantTile) {
                    tileMismatches++;
                }
                int objectID = level.getObjectID(x, y);
                if (objectID != 0) {
                    actualObjects.merge(objectID, 1, Integer::sum);
                }
            }
        }
        logs.add("painter expectation (seed=" + seed + " origin=" + origin.x + "," + origin.y + "):");
        expected.forEach((id, n) -> logs.add("  expected " + necesse.engine.registries.ObjectRegistry.getObject(id).getStringID() + " x" + n));
        logs.add("painter oracle: tileMismatches=" + tileMismatches + " (scan radius " + r + ", spire footprint excluded)");

        // The built landscape, counted in the world rather than predicted:
        // this is the assertion that roads, lamps and gates really landed.
        //
        // Railings and gate piers are counted across BOTH material families.
        // The Skyway Passages build theirs out of Cloudmarble, so a count that
        // only knew about Skywatch iron would read as a drop in railings the
        // moment the hub happened to sit next to the passages — a false alarm
        // about a thing that got richer, not poorer.
        logs.add("skyroads: paved=" + paved + " chequer=" + chequer
                + " lamps=" + actualObjects.getOrDefault(SkyRegistry.wardenCandelabraID, 0)
                + " fences=" + (actualObjects.getOrDefault(SkyRegistry.skyironFenceID, 0)
                        + actualObjects.getOrDefault(SkyCloudmarbleSet.cloudmarbleFenceID, 0))
                + " gatewalls=" + (actualObjects.getOrDefault(SkyRegistry.skystoneBrickWallID, 0)
                        + actualObjects.getOrDefault(SkyRegistry.nightfellWallID, 0)
                        + actualObjects.getOrDefault(SkyCloudmarbleSet.cloudmarbleWallID, 0))
                + " roadtile=" + TileRegistry.getTileStringID(SkyRegistry.skyroadTileID));

        // The Skyway Passages, counted the same way. Everything this biome is
        // made of is registered and reachable, so the only thing that can go
        // wrong is that nothing generates it — which is exactly what a count
        // of zero here says, and what a source reading cannot.
        logs.add("skyway: ground=" + TileRegistry.getTileStringID(SkyCloudmarbleSet.skywayTileID)
                + " tiles=" + skywayGround
                + " seraphtrees=" + actualObjects.getOrDefault(SkyRegistry.skySeraphTreeID, 0)
                + " cloudtrees=" + actualObjects.getOrDefault(SkyRegistry.cloudTreeID, 0)
                + " trees=" + (actualObjects.getOrDefault(SkyRegistry.skySeraphTreeID, 0)
                        + actualObjects.getOrDefault(SkyRegistry.cloudTreeID, 0))
                + " seraphstatues=" + actualObjects.getOrDefault(SkyCloudmarbleSet.seraphStatueID, 0)
                + " rails=" + actualObjects.getOrDefault(SkyCloudmarbleSet.cloudmarbleFenceID, 0)
                + " railgates=" + actualObjects.getOrDefault(SkyCloudmarbleSet.cloudmarbleFenceGateID, 0));

        int[] place = stairwaytoheaven.worldgen.SkyLandscape.designedPlaceNear(
                seed, origin.x, origin.y, origin.x, origin.y, 3);
        if (place == null) {
            logs.add("designed place: NONE within 3 lattice cells of the hub");
        } else {
            logs.add("designed place: kind=" + place[2] + " radius=" + place[3]
                    + " at " + place[0] + "," + place[1]
                    + " (" + bearing(origin.x, origin.y, place[0], place[1]) + ")");
        }
    }

    /** Reports why natural objects would be rejected on sky ground, and the registered IDs. */
    private void diagnosePlacement(Level level, CommandLog logs) {
        logs.add("IDs: stormcrystal=" + SkyRegistry.stormCrystalID + " aurorabloom=" + SkyRegistry.auroraBloomID
                + " skyreeds=" + SkyRegistry.skyreedsID + " skystonerock=" + SkyRegistry.skystoneRockID
                + " aetheriumrock=" + SkyRegistry.aetheriumRockID);
        Map<String, Integer> crystalErrors = new HashMap<>();
        Map<String, Integer> reedErrors = new HashMap<>();
        int checked = 0;
        // v0.5: probe around the canonical spire origin (the hub the painter
        // guarantees), not the raw world origin.
        java.awt.Point center = stairwaytoheaven.worldgen.SkyOrigin.compute(((SkyLevel) level).getWorldGenSeed());
        int r = SCAN_RADIUS_TILES;
        level.regionManager.ensureTilesAreLoaded(center.x - r, center.y - r, center.x + r, center.y + r);
        for (int x = center.x - r; x <= center.x + r && checked < 300; x++) {
            for (int y = center.y - r; y <= center.y + r && checked < 300; y++) {
                if (level.getTileID(x, y) == SkyRegistry.mistseaID || level.getObjectID(x, y) != 0) {
                    continue;
                }
                checked++;
                String ce = necesse.engine.registries.ObjectRegistry.getObject(SkyRegistry.stormCrystalID)
                        .canPlace(level, 0, x, y, 0, false, false);
                crystalErrors.merge(ce == null ? "OK" : ce, 1, Integer::sum);
                String re = necesse.engine.registries.ObjectRegistry.getObject(SkyRegistry.skyreedsID)
                        .canPlace(level, 0, x, y, 0, false, false);
                reedErrors.merge(re == null ? "OK" : re, 1, Integer::sum);
            }
        }
        logs.add("placement check on " + checked + " free land tiles:");
        crystalErrors.forEach((k, v) -> logs.add("  stormcrystal: " + k + " x" + v));
        reedErrors.forEach((k, v) -> logs.add("  skyreeds: " + k + " x" + v));
    }
}
