package stairwaytoheaven.quest;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.quest.DeliverItemsQuest;
import necesse.gfx.fairType.FairType;
import necesse.gfx.gameFont.FontOptions;

/**
 * The Last Rites — Mortimer's own chain, in the Aftergarden.
 *
 * <h2>Why the Undertaker gets a quest</h2>
 * {@code docs/AREA_OVERVIEW.md}: the Ghost band holds four of the mod's named
 * people and exactly two live quests, and one of those is a region key. Mortimer
 * and Caspern were shops with a greeting line and nothing to do —
 * {@code docs/OVERVIEW.md} §8.7 lists both. An NPC a player can only buy from
 * is a vending machine with a face; a realm with three of them is a shopping
 * street, not a place.
 *
 * <h2>Why these two materials</h2>
 * Soul Thread and Bonewood, both Ghost-band-only ({@code GhostRealm}'s ghost
 * lily, its dead trees, {@code GhostLoot}'s crate tables and the Sunken
 * Graveyard's own chest are the whole supply), so the quest is a reason to walk
 * the Bone Orchard rather than a list to buy. The undertaker's trade decides
 * which two: a shroud is thread and a coffin is wood, and he is the only person
 * in the mod who would think of them in that order.
 *
 * <p>Bonewood is also what {@link GhostKeyQuest} asks for, and that overlap is
 * deliberate rather than an oversight. A realm whose materials have exactly one
 * buyer each is a realm you farm once; the Skyreach's Storm Shard already
 * carries three separate demands, and the Ghost band's ought to carry more than
 * one. Soul Thread had none at all before this — {@code docs/OVERVIEW.md} §8.9's
 * list of drops no recipe names.
 *
 * <h2>Reward</h2>
 * His recruit fee (8 000) waived, plus 6 Spiritsteel Bar. The bar is the Ghost
 * band's own, and six sits below {@link GhostKeyQuest}'s ten for the reason
 * {@code SkyreachKeyQuest} states about its own curve: the key's real payout is
 * the tier-9 boss it unlocks, and a side chain that matched it would make the
 * key look like the smaller errand.
 */
public class MortimerRitesQuest extends DeliverItemsQuest {

    /** Shrouds. The Ghost band's own cloth, and nothing else consumed it. */
    public static final int SOUL_THREAD = 12;
    /** Coffins. The Bone Orchard's dead wood. */
    public static final int BONEWOOD = 10;

    public MortimerRitesQuest() {
        super(new ItemObjective("soulthread", SOUL_THREAD),
                new ItemObjective("bonewood", BONEWOOD));
    }

    @Override
    public GameMessage getTitle() {
        return new LocalMessage("quests", "swhmortimerritestitle");
    }

    @Override
    public GameMessage getDescription() {
        return new LocalMessage("quests", "swhmortimerritesdesc");
    }

    @Override
    public FairType getRewardType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhmortimerritesreward"));
    }

    @Override
    public FairType getHandInType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhspeaktomortimer"));
    }
}
