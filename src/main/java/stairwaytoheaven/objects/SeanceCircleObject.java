package stairwaytoheaven.objects;

import java.awt.Color;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketChatMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameUtils;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.hudManager.floatText.ChatBubbleText;
import stairwaytoheaven.mobs.GhostGuideMob;

/**
 * The Séance Circle — a chalk ring you draw at home, and the only way to call
 * the Ghost Guide.
 *
 * <h2>It is not a teleporter any more</h2>
 *
 * It used to be one, with three branches, and its own comment called the last
 * of them "an honest dead end": on the Skyreach it turned itself into a Crooked
 * Door, past the Ghost band it said there was nowhere left to send you, and
 * anywhere else it opened a Veil Rift if you were carrying the Silver Bell. All
 * three are gone. {@code docs/PLAN_ONE_PLANE.md} removed the destination —
 * there is one level, so a door to another world cannot exist — and
 * {@code docs/FOGKEY_AND_BOSSPORTALS.md} A2 replaced the mechanic outright:
 * <blockquote>The circle stops being a teleporter. It becomes the thing you
 * draw at home.</blockquote>
 * Travel is the vanilla Portal Flask's job; passage through the fog is the
 * chalk's. Nothing here creates {@code crookeddoordown} any more — that object
 * keeps its own worldgen and its own owner.
 *
 * <h2>The three rules A2 gives it</h2>
 *
 * <ol>
 *   <li><b>Placeable only inside a settlement</b> — the player: <i>"zuhause in
 *       der basis (sonst geht es nicht)"</i>. See {@link #canPlace}.</li>
 *   <li><b>Minable with a pickaxe</b>, because it is furniture and not a
 *       fixture. See the constructor.</li>
 *   <li><b>Interacting summons the Ghost Guide.</b> See {@link #interact}.</li>
 * </ol>
 */
public class SeanceCircleObject extends SkyDecoObject {

    /**
     * How far from the ring the guide may already be standing before a second
     * use counts as "he is already here" rather than "call him".
     *
     * <p>320 = ten tiles. It has to be comfortably wider than the range the
     * guide's own {@code HumanAI} wanders (he is given a 320-unit home radius,
     * the same one every settler in this mod gets), so that a guide who has
     * drifted a few tiles off the chalk is still recognised as the guide this
     * circle called, and narrow enough that a second circle across the base
     * gets its own.
     */
    private static final int GUIDE_SEARCH_RANGE = 320;

    /** Where the guide appears, in pixels from the ring's centre. */
    private static final float SUMMON_OFFSET = 24.0F;

    public SeanceCircleObject() {
        super("seancecircle", 32, new Color(120, 150, 130), null, "objects", "misc");
        this.setLight(60, 0.38F, 0.45F);
        // A2: "Minable with a pickaxe -- it is furniture, not a fixture."
        // ToolType.PICKAXE is GameObject's own default (GameObject.java:117,
        // VERIFIED [jar]) and is stated here rather than inherited so that the
        // rule is visible where the rule is written down. 50 health is
        // vanilla's own number for placed furniture -- ChairObject.java:47 and
        // FlowerPotObject.java:32 both set exactly 50 -- against GameObject's
        // bare default of 100, which vanilla reserves for statues, pillars and
        // banners (AncientPillarObject.java:32, BannerObject.java:42).
        this.setTool(ToolType.PICKAXE);
        this.setObjectHealth(50);
    }

    /**
     * Breaking it hands the chalk back, not a circle.
     *
     * <p>{@code seancecircle} is registered UNOBTAINABLE
     * ({@code StairwayToHeavenMod.registerObjects}), so
     * {@code GameObject.getLootTable}'s default would drop nothing at all
     * (GameObject.java:285-289 returns an empty table unless the object's own
     * item is obtainable, VERIFIED [jar]) and a misplaced circle would cost the
     * player their chalk. One piece in, one piece out: the chalk is the only
     * item in the loop, which is what makes the Warden's restock the single
     * answer to "I lost it".
     */
    @Override
    public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
        return new LootTable(new LootItem("ghostchalk").preventLootMultiplier());
    }

    /**
     * Only inside a settlement.
     *
     * <p>Same shape and same seam vanilla uses for its own "not just anywhere"
     * objects: {@code SettlementFlagObject.canPlace} and
     * {@code LadderDownObject.canPlace} both call {@code super} first and then
     * return a non-null error key (SettlementFlagObject.java:163-169,
     * VERIFIED [jar]). A non-null answer is what stops the placement — on the
     * client it also greys the preview out, and on the server it is checked
     * again by {@code ObjectItem.canPlace}, so this is not a client-side
     * courtesy that a crafted packet could walk past.
     *
     * <p>The test is {@code SettlementsWorldData.hasSettlementAtTile}, which is
     * region-granular: a settlement claims whole 32x32-tile regions, so "inside
     * the base" means inside the flag's claimed regions rather than inside a
     * drawn border. That is the same claim the settlement machinery itself
     * works in, so a tile the player can build settler beds on is a tile they
     * can draw the ring on.
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
     * ...and say so. Without this the chalk simply refuses to go down and the
     * player is left guessing, which is exactly the complaint
     * {@code LadderDownObject.attemptPlace} exists to answer for its own
     * "notsurface" (LadderDownObject.java:140-144).
     */
    @Override
    public void attemptPlace(Level level, int x, int y, PlayerMob player, String error) {
        if (level.isClient() && "swhnotsettlement".equals(error)) {
            player.getLevel().hudManager.addElement(
                    new ChatBubbleText(player, Localization.translate("misc", "seanceneedsettlement")));
        }
    }

    @Override
    public boolean canInteract(Level level, int x, int y, PlayerMob player) {
        return true;
    }

    @Override
    public String getInteractTip(Level level, int x, int y, PlayerMob perspective, boolean debug) {
        return Localization.translate("controls", "usetip");
    }

    /**
     * The séance: one Ghost Guide, standing on the chalk.
     *
     * <p>{@code RoyalEggObject.spawnBoss} is the vanilla pattern for an object
     * that puts a mob on the ground — {@code MobRegistry.getMob(id, level)},
     * then {@code level.entityManager.addMob} at an offset
     * (RoyalEggObject.java:110-119, VERIFIED [jar]) — and it is level-agnostic,
     * which the one-plane world needs it to be.
     *
     * <p>A second use does not make a second guide. He is a person the circle
     * calls, not a spawner: if one is already within {@link #GUIDE_SEARCH_RANGE}
     * the ring says so and the player walks over and talks to him, which is
     * also where every conversation after the first happens (see
     * {@link GhostGuideMob}).
     */
    @Override
    public void interact(Level level, int x, int y, PlayerMob player) {
        if (!level.isServer() || !player.isServerClient()) {
            return;
        }
        ServerClient client = player.getServerClient();
        float centreX = x * 32 + 16;
        float centreY = y * 32 + 16;

        // A ring OUTSIDE a settlement answers nobody, and that is not the same
        // check as canPlace even though it asks the same question.
        //
        // WHY IT HAS TO BE ASKED TWICE. Worldgen stands a Seance Circle at every
        // hashed portal site in the Beetle Outlands
        // (CrookedTerrainPainter.describeTile -> SkyOutlands.isPortalSite) and
        // writes it into the level directly, which no canPlace ever sees. Those
        // rings are older than this design and they are the leftover of the
        // teleporter it replaced. Without this branch, finding one in the wild
        // would summon the guide for free -- and the chalk, the Warden and the
        // whole first-step-into-the-fog beat that FOGKEY A1 builds would be a
        // detour a player could simply walk around.
        //
        // They are deliberately left standing rather than removed from
        // worldgen: Part B's region boss portals are "scattered through
        // worldgen, in their own region only", and this is already that, hashed
        // and placed. Whoever builds them inherits the sites.
        SettlementsWorldData settlements = SettlementsWorldData.getSettlementsData(level);
        if (settlements == null || !settlements.hasSettlementAtTile(level, x, y)) {
            client.sendChatMessage(new LocalMessage("misc", "seancenobodyanswers"));
            return;
        }

        if (findGuide(level, centreX, centreY) != null) {
            client.sendChatMessage(new LocalMessage("misc", "seanceguidehere"));
            return;
        }

        Mob guide = MobRegistry.getMob(GhostGuideMob.STRING_ID, level);
        if (guide == null) {
            return;
        }
        level.entityManager.addMob(guide, centreX, centreY - SUMMON_OFFSET);
        GameMessage arrival = new LocalMessage("misc", "seanceguidearrives",
                "name", guide.getLocalization());
        level.getServer().network.sendToClientsWithEntity(new PacketChatMessage(arrival), guide);
    }

    /** The guide this circle already called, or null. */
    private static GhostGuideMob findGuide(Level level, float centreX, float centreY) {
        return level.entityManager.mobs
                .streamInRegionsShape(GameUtils.rangeBounds(centreX, centreY, GUIDE_SEARCH_RANGE), 0)
                .filter(m -> m instanceof GhostGuideMob && !m.removed())
                .map(m -> (GhostGuideMob) m)
                .findFirst()
                .orElse(null);
    }
}
