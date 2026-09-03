package stairwaytoheaven.quest;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.quest.DeliverItemsQuest;
import necesse.gfx.fairType.FairType;
import necesse.gfx.gameFont.FontOptions;

/**
 * Region key 4 of 5 — the Aftergarden's Raven Perch.
 *
 * <p>{@code docs/FOGKEY_AND_BOSSPORTALS.md} §B1-B2. The reward,
 * {@code regionkeyghostrealm}, is the Gloom Raven on its plinth — the mod's own
 * grave marker, and the sheet the Aftergarden's Summoning Stones already wear.
 * Handed out and turned in by {@code mobs/SkyWardenMob.advanceRegionKeys}.
 *
 * <h2>Why these two materials, and what the quest quietly requires</h2>
 * Bonewood and Spectral Ore come out of the Ghost band and nowhere else: every
 * drop site for either is under {@code realms/ghost/} — {@code GhostLoot}'s four
 * tables, {@code GhostBiome}, the realm's own trees and rocks, and the Haunted
 * Manor, Mausoleum and Sunken Graveyard presets — and no recipe makes either.
 * Ectoplasm would have been the obvious ask and is deliberately NOT used: it is
 * a VANILLA item (ItemRegistry.java:929, VERIFIED [jar]) that vanilla's own
 * swamp hands out, so a quest for it could be finished without ever entering
 * the Aftergarden, which is exactly what §B1's "tied to that region" forbids.
 *
 * <p>Reaching either at all means walking through the Ghost band's Soul
 * Exposure fog, which is Part A's whole job: the chalk, the Séance Circle and
 * the Ghost Guide. That makes this the one region key with a real
 * prerequisite, and it is the prerequisite the spec already built rather than a
 * new one invented here.
 *
 * <h2>Reward</h2>
 * The key piece plus 10 Spiritsteel Bar. Spiritsteel sits one tier above
 * Stormsteel on {@code docs/BALANCE.md}'s gear ladder (34 chest armour / 2400
 * enchant vs. 29 / 1900), so the payout steps up in KIND here rather than only
 * in count — the same reasoning {@link EleanorQuest}'s own reward is built on.
 * The Aftergarden's stone wakes a tier-9 Pest Warden.
 */
public class GhostKeyQuest extends DeliverItemsQuest {

    public GhostKeyQuest() {
        super(new ItemObjective("bonewood", 12), new ItemObjective("spectralore", 8));
    }

    @Override
    public GameMessage getTitle() {
        return new LocalMessage("quests", "swhkeyghostrealmtitle");
    }

    @Override
    public GameMessage getDescription() {
        return new LocalMessage("quests", "swhkeyghostrealmdesc");
    }

    @Override
    public FairType getRewardType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhkeyghostrealmreward"));
    }

    @Override
    public FairType getHandInType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhreturnwarden"));
    }
}
