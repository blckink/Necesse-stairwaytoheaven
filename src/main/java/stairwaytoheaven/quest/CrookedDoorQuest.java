package stairwaytoheaven.quest;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.quest.DeliverItemsQuest;
import necesse.gfx.fairType.FairType;
import necesse.gfx.gameFont.FontOptions;

/**
 * HUD quest for the Crooked finale — {@code docs/WORLD_DESIGN.md} §13/A3.6:
 * "after death it is not only the landscape that decays. The rules of the
 * world decay." Mr. Knott has spent longer than anyone believing one of the
 * Door Yard's eleven doors can be convinced to lead somewhere; the player is
 * the proof.
 *
 * <h2>Why these three materials</h2>
 * Reality Shard, Warp Resin and Strange Fabric are the realm's own registered
 * economy ({@code CrookedRealm.registerItems}) — nothing invented — chosen
 * because they are the three {@code CrookedRealm}'s own doc names as the
 * Reality Stitcher's eventual first recipe: handing them to Mr. Knott now is a
 * small, honest down payment on a station this pass does not build.
 *
 * <h2>Reward, and why it looks like this</h2>
 * Crooked Beyond sits at incursion tier 10 on {@code docs/BALANCE.md}'s own
 * realm ladder — the same ceiling the Skyreach itself topped out at before this
 * mod's endgame rescale — so its chain is the largest of the three this pass
 * adds: Zephyr Harness (one of the mod's three EPIC trinkets, the same shape as
 * the Anchor's own Stormsteel Vambrace, but not the same one — Caspern's and
 * Eveleen's chains do not repeat each other's payout either), 12 Stormsteel
 * bars (above the Anchor's 10), and Reality Shards back, a small seed fund for
 * whichever future pass builds the Stitcher.
 */
public class CrookedDoorQuest extends DeliverItemsQuest {

    public CrookedDoorQuest() {
        super(new ItemObjective("realityshard", 5), new ItemObjective("warpresin", 8),
                new ItemObjective("strangefabric", 8));
    }

    @Override
    public GameMessage getTitle() {
        return new LocalMessage("quests", "swhcrookeddoortitle");
    }

    @Override
    public GameMessage getDescription() {
        return new LocalMessage("quests", "swhcrookeddoordesc");
    }

    @Override
    public FairType getRewardType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhcrookeddoorreward"));
    }

    @Override
    public FairType getHandInType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhspeaktoknott"));
    }
}
