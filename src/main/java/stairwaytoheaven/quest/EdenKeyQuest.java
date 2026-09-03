package stairwaytoheaven.quest;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.quest.DeliverItemsQuest;
import necesse.gfx.fairType.FairType;
import necesse.gfx.gameFont.FontOptions;

/**
 * Region key 2 of 5 — the Garden's Gate.
 *
 * <p>{@code docs/FOGKEY_AND_BOSSPORTALS.md} §B1-B2. Handed out and turned in by
 * {@code mobs/SkyWardenMob.advanceRegionKeys}; the reward is
 * {@code regionkeyeden}, which wears the Eden Gate's own stairway sheet — the
 * same picture Eden's Summoning Stones wear, so a player who has met one knows
 * on sight what this is for.
 *
 * <h2>Why these two materials</h2>
 * Eden Sap and Golden Pollen are drops of Eden's own living things and of
 * nothing else: the Jealous Vine and the Bloom Maw bleed the first
 * ({@code realms/eden/JealousVineMob}, {@code realms/eden/BloomMawMob}), the
 * Golden Hornet carries the second ({@code realms/eden/GoldenHornetMob}).
 * Neither is an ambient pickup, which is the difference between this and the
 * Eden chain's own {@link EdenPlantsQuest}: that one is a tour of the three
 * Eden biomes, this one asks the player to actually fight the garden.
 *
 * <h2>Reward</h2>
 * The key piece plus 8 Stormsteel Bar — one step up the 6 - 8 - 10 - 10 - 12
 * ladder these five quests walk, which is §B4's own monotone boss ladder read
 * back as a payout curve. Eden's stone wakes a tier-8 Moonlight Dancer.
 */
public class EdenKeyQuest extends DeliverItemsQuest {

    public EdenKeyQuest() {
        super(new ItemObjective("edensap", 8), new ItemObjective("goldenpollen", 6));
    }

    @Override
    public GameMessage getTitle() {
        return new LocalMessage("quests", "swhkeyedentitle");
    }

    @Override
    public GameMessage getDescription() {
        return new LocalMessage("quests", "swhkeyedendesc");
    }

    @Override
    public FairType getRewardType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhkeyedenreward"));
    }

    @Override
    public FairType getHandInType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhreturnwarden"));
    }
}
