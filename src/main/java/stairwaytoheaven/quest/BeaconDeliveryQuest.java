package stairwaytoheaven.quest;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.quest.DeliverItemsQuest;
import necesse.gfx.fairType.FairType;
import necesse.gfx.gameFont.FontOptions;

/**
 * HUD quest for stage 1: deliver 12 storm shards + 8 windsilk to light the
 * beacon. Counting, per-item progress bars and consumption on completion are
 * the vanilla DeliverItemsQuest machinery.
 */
public class BeaconDeliveryQuest extends DeliverItemsQuest {

    public static final int SHARDS = 12;
    public static final int SILK = 8;

    public BeaconDeliveryQuest() {
        // Fixed objectives per type; save/packet application clears + refills.
        super(new ItemObjective("stormshard", SHARDS), new ItemObjective("windsilk", SILK));
    }

    @Override
    public GameMessage getTitle() {
        return new LocalMessage("quests", "swhbeacontitle");
    }

    @Override
    public GameMessage getDescription() {
        return new LocalMessage("quests", "swhbeacondesc");
    }

    @Override
    public FairType getRewardType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhbeaconreward"));
    }

    @Override
    public FairType getHandInType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhreturnwarden"));
    }
}
