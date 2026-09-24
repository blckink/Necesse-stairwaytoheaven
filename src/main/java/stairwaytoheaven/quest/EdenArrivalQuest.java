package stairwaytoheaven.quest;

import stairwaytoheaven.worldgen.RealmDepth;

/**
 * {@code swh_edenreach} — "Into the Garden": the first step of the Garden of
 * Eden chapter of the quest ladder.
 *
 * <p>Eveleen hands it out in the Spire Village once the Skyreach chapter is
 * done; it completes when the player has stood in the Eden band
 * ({@link RealmVisitQuest}) and is turned in to her. The Eden Gate still hands
 * it out on first use, as it always did — a player standing in Eden has then
 * already reached it, and takes it back to her.
 *
 * <p>Was a pure signpost ("find the Knowledge Tree, Eveleen is beside it")
 * until 2026-09-24; with Eveleen living in the village that signpost pointed
 * at nobody. Its ID and class are unchanged, so a copy an older save holds in
 * its journal loads and simply starts tracking the visit.
 */
public class EdenArrivalQuest extends RealmVisitQuest {

    public EdenArrivalQuest() {
    }

    @Override
    protected int realm() {
        return RealmDepth.REALM_EDEN;
    }

    @Override
    protected String keyPrefix() {
        return "swhedenreach";
    }

    @Override
    protected String handInKey() {
        return "swhspeaktoeveleen";
    }
}
