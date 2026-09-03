package stairwaytoheaven.quest;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.quest.DeliverItemsQuest;
import necesse.gfx.fairType.FairType;
import necesse.gfx.gameFont.FontOptions;

/**
 * Region key 5 of 5 — a door of your own.
 *
 * <p>{@code docs/FOGKEY_AND_BOSSPORTALS.md} §B1 names this one too:
 * <i>"Mr. Knott's red door for Crooked"</i>. The reward,
 * {@code regionkeycrookedbeyond}, is that door, on the very sheet
 * {@code realms/crooked/CrookedDoorObject} passes to {@code LadderDownObject}
 * and the very sheet the Crooked Beyond's Summoning Stones wear. Handed out and
 * turned in by {@code mobs/SkyWardenMob.advanceRegionKeys}.
 *
 * <h2>Why these two materials, and how they differ from Knott's own ask</h2>
 * Oddwood and Reality Shard both come only out of the Crooked Beyond
 * ({@code CrookedBiome}, {@code CheckerworksBiome}, the Tongue Plant, and the
 * Long Table, Inverted House and Door Yard presets). {@link CrookedDoorQuest} —
 * Mr. Knott's own chain — asks for Reality Shard, Warp Resin and Strange
 * Fabric, the three the Reality Stitcher will eventually want. This one asks
 * for the two you would build a DOOR out of instead: a lot of the realm's wood,
 * and enough shard to make it lead somewhere. The overlap on Reality Shard is
 * deliberate and small; the realm has one currency, and pretending otherwise
 * would mean inventing an item.
 *
 * <h2>Reward</h2>
 * The key piece plus 12 Spiritsteel Bar, the top of the five-key ladder.
 * {@code docs/BALANCE.md} puts the Crooked Beyond at incursion tier 10, and
 * §B4's own table agrees: its stone wakes the Crystal Dragon at 208 000 HP,
 * the heaviest fight the mod currently has a portal for.
 */
public class CrookedKeyQuest extends DeliverItemsQuest {

    public CrookedKeyQuest() {
        super(new ItemObjective("oddwood", 16), new ItemObjective("realityshard", 8));
    }

    @Override
    public GameMessage getTitle() {
        return new LocalMessage("quests", "swhkeycrookedbeyondtitle");
    }

    @Override
    public GameMessage getDescription() {
        return new LocalMessage("quests", "swhkeycrookedbeyonddesc");
    }

    @Override
    public FairType getRewardType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhkeycrookedbeyondreward"));
    }

    @Override
    public FairType getHandInType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhreturnwarden"));
    }
}
