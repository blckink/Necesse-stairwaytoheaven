package stairwaytoheaven.quest;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.quest.DeliverItemsQuest;
import necesse.gfx.fairType.FairType;
import necesse.gfx.gameFont.FontOptions;

/**
 * HUD quest for the Eden finale: bring Eveleen three Eden plants.
 *
 * <h2>What "three Eden plants" means</h2>
 * {@code docs/WORLD_DESIGN.md} §5's own unlock line — "collecting three Eden
 * plants" — is read as one of each of the realm's three named farmable fruits
 * (Eden Berry, Moon Melon, Sun Grape) rather than three of one kind: it doubles
 * as a light tour of all three Eden biomes (the berry and the grape are
 * {@code EdenBiome}'s shared table, the melon is the Garden's own, the grape
 * again the Shallows') and every one of the three is already an ordinary
 * ambient drop — nothing here is gated behind a recipe or a rare roll.
 *
 * <h2>Reward, and why it looks like this</h2>
 * Handed out by {@code EveleenMob.interact}, not by this class: "she joins" is
 * a free recruit ({@code EveleenMob.getRecruitItems} waives her fee once
 * {@code SkywatchWorldData.edenPlantsGiven} is set), and the material payout —
 * a Knowledge Cutting for the tree the chain opened with, 10 Stormsteel bars —
 * is benchmarked the same way {@link AnchorDeliveryQuest}'s own reward is:
 * {@code docs/BALANCE.md}, at or above what the Skyreach finale already pays.
 */
public class EdenPlantsQuest extends DeliverItemsQuest {

    public EdenPlantsQuest() {
        super(new ItemObjective("edenberry", 1), new ItemObjective("moonmelon", 1),
                new ItemObjective("sungrape", 1));
    }

    @Override
    public GameMessage getTitle() {
        return new LocalMessage("quests", "swhedenplantstitle");
    }

    @Override
    public GameMessage getDescription() {
        return new LocalMessage("quests", "swhedenplantsdesc");
    }

    @Override
    public FairType getRewardType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhedenplantsreward"));
    }

    @Override
    public FairType getHandInType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhspeaktoeveleen"));
    }
}
