package stairwaytoheaven.quest;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.quest.DeliverItemsQuest;
import necesse.gfx.fairType.FairType;
import necesse.gfx.gameFont.FontOptions;

/**
 * Region key 1 of 5 — the Skyreach's Watchfire.
 *
 * <p>{@code docs/FOGKEY_AND_BOSSPORTALS.md} §B1: <i>"Each region's key piece is
 * the reward of an Elder quest tied to that region."</i> Handed out and turned
 * in by {@code mobs/SkyWardenMob.advanceRegionKeys} — see that method's own doc
 * for why the giver is the Warden and not the Elder. The reward is
 * {@code regionkeyskyreach}, the buildable Watchfire, which unlocks this
 * realm's Summoning Stones the moment it is stood up in a settlement.
 *
 * <h2>Why these two materials</h2>
 * §B1's rule, restated by this pass: what a region's quest asks for must only
 * be obtainable IN that region, so the quest is a reason to go there rather
 * than a shopping list. Storm Shard and Fulgurite are both Skyreach-only —
 * {@code biomes/StormveilBiome}'s own ground loot, {@code mobs/StormWispMob},
 * the {@code fulguriteore} rock and the Stormveil's Rime Sentries are the whole
 * supply, and nothing in Eden, Steinfeld, the Aftergarden or the Crooked Beyond
 * drops either.
 *
 * <h2>Reward, and why it looks like this</h2>
 * The key piece plus 6 Stormsteel Bar. Deliberately BELOW the Skyreach finale's
 * own 10 ({@link AnchorDeliveryQuest}, {@code docs/BALANCE.md}): this quest sits
 * immediately after that finale and its real payout is the boss it unlocks —
 * a tier-8 Cryo Queen at 57 240 HP (§B4) whose loot is the point. Paying the
 * finale's own number twice would make the bars, not the boss, the reason to do
 * it. The five keys then climb 6 - 8 - 10 Stormsteel, 10 - 12 Spiritsteel,
 * matching §B4's own monotone boss ladder rather than inventing a second one.
 */
public class SkyreachKeyQuest extends DeliverItemsQuest {

    public SkyreachKeyQuest() {
        super(new ItemObjective("stormshard", 10), new ItemObjective("fulgurite", 5));
    }

    @Override
    public GameMessage getTitle() {
        return new LocalMessage("quests", "swhkeyskyreachtitle");
    }

    @Override
    public GameMessage getDescription() {
        return new LocalMessage("quests", "swhkeyskyreachdesc");
    }

    @Override
    public FairType getRewardType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhkeyskyreachreward"));
    }

    @Override
    public FairType getHandInType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhreturnwarden"));
    }
}
