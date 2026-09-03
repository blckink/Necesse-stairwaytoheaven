package stairwaytoheaven.quest;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.quest.DeliverItemsQuest;
import necesse.gfx.fairType.FairType;
import necesse.gfx.gameFont.FontOptions;

/**
 * Region key 3 of 5 — Steinfeld's Broken Angel.
 *
 * <p>{@code docs/FOGKEY_AND_BOSSPORTALS.md} §B1 names this one outright:
 * <i>"a statue for Steinfeld"</i>. The reward, {@code regionkeysteinfeld},
 * stands the realm's own seraph up in the player's base on the exact sheet its
 * Summoning Stones wear. Handed out and turned in by
 * {@code mobs/SkyWardenMob.advanceRegionKeys}.
 *
 * <h2>Why these two materials</h2>
 * Echo Shard and Pale Stone are Steinfeld's whole ground economy and appear
 * nowhere else: {@code SteinfeldBiome}, {@code SlabFieldsBiome},
 * {@code GraveHeathBiome} and {@code QuietMeadowBiome} drop them, as do the
 * Hollow Angel, the Lost Pilgrim, the Grave Crow and the Stone Mourner. Pale
 * Stone is the bulk half of the ask (a rock yields 5-16) and Echo Shard the
 * scarce half (1-3 on a chance roll), so the quest is a walk across the realm
 * rather than a stand at one rock.
 *
 * <h2>Reward</h2>
 * The key piece plus 10 Stormsteel Bar — level with the Skyreach finale's own
 * payout ({@link AnchorDeliveryQuest}, {@code docs/BALANCE.md}), and the top of
 * the Stormsteel half of the five-key ladder. Steinfeld's stone wakes a tier-9
 * Ascended Wizard.
 */
public class SteinfeldKeyQuest extends DeliverItemsQuest {

    public SteinfeldKeyQuest() {
        super(new ItemObjective("echoshard", 8), new ItemObjective("palestone", 20));
    }

    @Override
    public GameMessage getTitle() {
        return new LocalMessage("quests", "swhkeysteinfeldtitle");
    }

    @Override
    public GameMessage getDescription() {
        return new LocalMessage("quests", "swhkeysteinfelddesc");
    }

    @Override
    public FairType getRewardType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhkeysteinfeldreward"));
    }

    @Override
    public FairType getHandInType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhreturnwarden"));
    }
}
