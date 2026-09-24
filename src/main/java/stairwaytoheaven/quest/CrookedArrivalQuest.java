package stairwaytoheaven.quest;

import stairwaytoheaven.worldgen.RealmDepth;

/**
 * {@code swh_crookedarrival} — "A Door That Leads Somewhere": the first step of
 * the Crooked Beyond chapter of the quest ladder.
 *
 * <p>Until 2026-09-24 this quest was DEAD: the only thing that handed it out
 * was {@code CrookedDoorObjectEntity.use}, and nothing in the world places a
 * Crooked Door ({@code KOMPLETTUEBERSICHT.md} §8.1 no. 1). Mr. Knott now hands
 * it out himself in the Spire Village, and it completes when the player has
 * stood in the Crooked band ({@link RealmVisitQuest}) — the Door Yard he keeps
 * talking about is out there.
 */
public class CrookedArrivalQuest extends RealmVisitQuest {

    public CrookedArrivalQuest() {
    }

    @Override
    protected int realm() {
        return RealmDepth.REALM_CROOKED;
    }

    @Override
    protected String keyPrefix() {
        return "swhcrookedarrival";
    }

    @Override
    protected String handInKey() {
        return "swhspeaktoknott";
    }
}
