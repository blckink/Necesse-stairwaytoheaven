package stairwaytoheaven.quest;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.quest.DeliverItemsQuest;
import necesse.gfx.fairType.FairType;
import necesse.gfx.gameFont.FontOptions;

/**
 * Region key 6 of 6 — the paperwork.
 *
 * <p>{@code docs/FOGKEY_AND_BOSSPORTALS.md} §B1 names a key piece for each
 * realm and stopped at five, because Hell had no boss portal to unlock. It has
 * one now, so it has a key: {@code regionkeyhell}, which wears the Aether
 * Forge — the same furnace mouth its boss portal wears, and the shape §21's
 * Infernal Forge will eventually take.
 *
 * <h2>Why these two materials</h2>
 * §18's INFERNAL PAPERWORK asks for three absurdly literal documents — Proof
 * of Death, Proof of Life, Proof of Unreasonable Intent — and none of the
 * three is built, because each is a quest object with its own icon and its own
 * chain. Rather than fake them, this quest asks for what a player standing in
 * Hell actually has: the Crooked Beyond's own two currencies, at the counts
 * Hell's own drop value produces. Reality Shard 16 and Oddwood 24 are double
 * {@link CrookedKeyQuest}'s 8 and 16 — the realm one rung further out costs
 * one more trip, which is §40's central rule made arithmetic.
 *
 * <p>The overlap with the Crooked key is deliberate and is the same call
 * {@link CrookedKeyQuest} already documents: a realm has one currency, and
 * pretending otherwise would mean inventing an item. When §20's six Hell
 * materials land, this is the first quest that should ask for them.
 *
 * <h2>Reward</h2>
 * The key piece plus 16 Spiritsteel Bar, the top of the six-key ladder — over
 * the Crooked key's 12. Its stone wakes the Mutant Hydra at 528 000 HP, the
 * heaviest fight the mod has a portal for.
 */
public class HellKeyQuest extends DeliverItemsQuest {

    public HellKeyQuest() {
        super(new ItemObjective("realityshard", 16), new ItemObjective("oddwood", 24));
    }

    @Override
    public GameMessage getTitle() {
        return new LocalMessage("quests", "swhkeyhelltitle");
    }

    @Override
    public GameMessage getDescription() {
        return new LocalMessage("quests", "swhkeyhelldesc");
    }

    @Override
    public FairType getRewardType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhkeyhellreward"));
    }

    @Override
    public FairType getHandInType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhreturnwarden"));
    }
}
