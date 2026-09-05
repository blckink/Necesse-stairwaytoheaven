package stairwaytoheaven.quest;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.quest.DeliverItemsQuest;
import necesse.gfx.fairType.FairType;
import necesse.gfx.gameFont.FontOptions;

/**
 * The Vigil — Steinfeld's first quest that is not a key piece.
 *
 * <h2>Why Steinfeld gets one at all</h2>
 * {@code docs/AREA_OVERVIEW.md} measured it: a band 2280 tiles deep with four
 * hostiles, zero critters, <b>zero NPCs</b> and exactly one quest — the region
 * key, which the Warden only offers after the whole of "The Warden's Call" is
 * finished. {@code docs/WORLD_DESIGN.md} Part B says the same thing in its own
 * words: <i>"Steinfeld has no NPC, no boss and no station."</i> It has a boss
 * (§B4's Ascended Wizard) and now it has a person; this is what she asks for.
 *
 * <h2>Why these two materials</h2>
 * The rule {@code SkyreachKeyQuest} states for the key quests applies here too:
 * what a realm's quest asks for must only be obtainable IN that realm, so the
 * quest is a reason to go there rather than a shopping list. Grave Salt and
 * Spirit Moss are both Steinfeld-only — {@code SteinfeldRealm}'s gravesalt rock
 * and spirit-moss patch, the Slab Fields' and Grave Heath's ground loot, and
 * the Stone Mourner, Hollow Angel, Lost Pilgrim and Grave Crow drops are the
 * whole supply, and nothing in the Skyreach, Eden, the Aftergarden or the
 * Crooked Beyond drops either.
 *
 * <p>They were also, until this quest, <b>the two materials in the realm that
 * nothing consumed</b>. {@code SteinfeldKeyQuest} already takes Echo Shard and
 * Pale Stone; no recipe anywhere names Grave Salt or Spirit Moss, and
 * {@code docs/OVERVIEW.md} §8.9 lists both as droppable, sellable and pointless.
 * Asking for exactly the two that had no demand is the cheapest way to give a
 * realm's ground a reason to be dug.
 *
 * <h2>Reward, and where it sits on the ladder</h2>
 * Ives's recruit fee waived plus 10 Stormsteel Bar — deliberately the same
 * shape and the same bar count as {@link EdenPlantsQuest}, the mod's other
 * "found in a realm, waives the finder's fee" chain, because the two are the
 * same beat one band apart. It stays BELOW {@link SteinfeldKeyQuest}'s own
 * payout in what it unlocks rather than in what it pays: that one opens a
 * tier-9 boss, this one opens a shopkeeper.
 */
public class SteinfeldVigilQuest extends DeliverItemsQuest {

    /** Grave Salt, to lay the graves. Steinfeld's own rock and mob drop. */
    public static final int GRAVE_SALT = 14;
    /** Spirit Moss, to dress them. The Grave Heath's own ground cover. */
    public static final int SPIRIT_MOSS = 10;

    public SteinfeldVigilQuest() {
        super(new ItemObjective("gravesalt", GRAVE_SALT),
                new ItemObjective("spiritmoss", SPIRIT_MOSS));
    }

    @Override
    public GameMessage getTitle() {
        return new LocalMessage("quests", "swhsteinfeldvigiltitle");
    }

    @Override
    public GameMessage getDescription() {
        return new LocalMessage("quests", "swhsteinfeldvigildesc");
    }

    @Override
    public FairType getRewardType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhsteinfeldvigilreward"));
    }

    @Override
    public FairType getHandInType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhspeaktoives"));
    }
}
