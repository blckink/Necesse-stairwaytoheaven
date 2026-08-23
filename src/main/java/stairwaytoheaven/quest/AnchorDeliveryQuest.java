package stairwaytoheaven.quest;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.quest.DeliverItemsQuest;
import necesse.gfx.fairType.FairType;
import necesse.gfx.gameFont.FontOptions;

/**
 * HUD quest for the anchor finale: deliver 5 aetherium bars + 20 skystone so
 * the Warden can anchor the island.
 */
public class AnchorDeliveryQuest extends DeliverItemsQuest {

    public static final int BARS = 5;
    public static final int STONE = 20;

    public AnchorDeliveryQuest() {
        super(new ItemObjective("aetheriumbar", BARS), new ItemObjective("skystone", STONE));
    }

    @Override
    public GameMessage getTitle() {
        return new LocalMessage("quests", "swhanchortitle");
    }

    @Override
    public GameMessage getDescription() {
        return new LocalMessage("quests", "swhanchordesc");
    }

    @Override
    public FairType getRewardType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhanchorreward"));
    }

    @Override
    public FairType getHandInType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhreturnwarden"));
    }
}
