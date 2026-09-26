package stairwaytoheaven.settlement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import necesse.engine.registries.SettlerPersonalityRegistry;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.settler.personalities.SettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.SimplePersonalityFilter;

/**
 * Fixed traits for the story people, the way vanilla fixes them for its own
 * (the Elder always "elder", the Friendly Witch always "friendlywitch").
 * Rolled at random, the Undertaker came out a beach lover and the vampire a
 * gourmet (Kevin, 2026-09-26: "sinnvolle Eigenschaften, nicht Bambus-Liebhaber").
 *
 * <p>The saved list wins over {@code setupPersonalities} on load
 * ({@code SkyTherapy}), so {@link #enforce} is also called after
 * {@code applyLoadData}: a save from before this commit gets the table too.
 * These people are who they are — the trait therapy cannot change them.
 */
public final class SkyPersonalities {

    public static final String VAMPIRE = "vampirenature";

    private static final Map<String, String[]> TABLE = new HashMap<>();

    static {
        TABLE.put(SkySettlers.MAGPIE, new String[] {"jogger", "adventurer"});        // the courier
        TABLE.put(SkySettlers.HALDA, new String[] {"favoritedish", "orderly"});      // the cellarer
        TABLE.put(SkySettlers.OSSIAN, new String[] {"indoorsy", "artconnoisseur"});  // the last reader
        TABLE.put(SkySettlers.EVELEEN, new String[] {"gardener", "flowercollector", "ecologist"}); // botanist
        TABLE.put(SkySettlers.MORTIMER, new String[] {"orderly", "persistent"});     // the undertaker
        TABLE.put(SkySettlers.CASPERN, new String[] {"maker", "fastworker"});        // the spirit smith
        TABLE.put(SkySettlers.ELEANOR, new String[] {"indoorsy", "audiophile"});     // the ghost who stayed
        TABLE.put(SkySettlers.KNOTT, new String[] {"orderly", "persistent"});        // the doorman
        TABLE.put(SkySettlers.IVES, new String[] {"orderly", "audiophile"});         // the verger
        TABLE.put(SkySettlers.VAMPIRE, new String[] {VAMPIRE, "bloodthirsty"});      // the nightbound
    }

    private SkyPersonalities() {
    }

    /** Registers the mod's own traits; call before any settler is built. */
    public static void register() {
        SettlerPersonalityRegistry.registerSettlerPersonality(VAMPIRE, VampirePersonality.class,
                new SimplePersonalityFilter(100).makeSettlerStringIDsWhitelist()
                        .filterSettlerStringID(SkySettlers.VAMPIRE),
                false);
    }

    /** Whether this mob's traits are fixed by the table. */
    public static boolean isFixed(String settlerStringID) {
        return settlerStringID != null && TABLE.containsKey(settlerStringID);
    }

    /** The fixed list for this mob, or null when it rolls like anyone else. */
    public static ArrayList<SettlerPersonality> fixed(HumanMob mob, String settlerStringID) {
        String[] ids = TABLE.get(settlerStringID);
        if (ids == null) {
            return null;
        }
        ArrayList<SettlerPersonality> list = new ArrayList<>();
        for (String id : ids) {
            SettlerPersonality p = SettlerPersonalityRegistry.getNewSettlerPersonality(id, mob);
            if (p != null) {
                list.add(p);
            }
        }
        return list.isEmpty() ? null : list;
    }

    /** Whether the list already holds exactly the table's traits, in order. */
    public static boolean matches(ArrayList<SettlerPersonality> current, String settlerStringID) {
        String[] ids = TABLE.get(settlerStringID);
        if (ids == null || current == null || current.size() != ids.length) {
            return ids == null;
        }
        for (int i = 0; i < ids.length; i++) {
            if (!ids[i].equals(current.get(i).getStringID())) {
                return false;
            }
        }
        return true;
    }
}
