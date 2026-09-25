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
 * <p>Region-only turned out not to be enough: both lie on the surface, and a
 * player had the ask together in minutes (11n, 2026-09-25). Two Skystone
 * Hearts — the band's trophy, off its golems and hoard chests — are the part
 * that needs the Skyreach explored rather than mined. docs/BALANCE.md §11.
 *
 * <h2>Reward, and why it looks like this</h2>
 * The key piece, 4 Stormsteel Bar, a full set of four Stormdiscs and the
 * Skywatch Hood. The table lives in ONE place, {@code SkyWardenMob.RegionKey};
 * this paragraph only explains it. Deliberately BELOW the Skyreach finale that
 * comes immediately before it ({@link AnchorDeliveryQuest}, which pays the
 * Wolkengleve): the key's real payout is the boss it unlocks, a tier-8 Cryo
 * Queen (§B4), and paying the finale's number twice would make the bars, not
 * the boss, the reason to do it. The six keys then climb 4 - 5 - 6 Stormsteel,
 * 6 - 8 - 16 Spiritsteel, with the weapon rising alongside (see
 * {@code RegionKey.specialItemIDs}).
 */
public class SkyreachKeyQuest extends DeliverItemsQuest {

    public SkyreachKeyQuest() {
        // 11n: plus the band trophy (docs/BALANCE.md §11) -- the surface
        // materials alone were farmable in minutes; the trophy needs the band's
        // own elite or its hoard chest.
        super(new ItemObjective("stormshard", 10), new ItemObjective("fulgurite", 5),
                new ItemObjective("skystoneheart", 2));
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
