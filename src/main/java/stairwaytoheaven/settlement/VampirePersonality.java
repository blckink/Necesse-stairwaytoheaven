package stairwaytoheaven.settlement;

import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.settler.personalities.SettlerPersonality;

/**
 * Dorian's own trait. He lives on blood (the bowl, the hunt, a neighbour's
 * neck — {@code mobs.VampireSettlerMob}), so the settler mood view must not
 * judge him by the kitchen: without this he always showed "no food" and a
 * monotonous diet (Kevin, 2026-09-26). Vanilla's own switch for exactly that
 * is {@code SettlerPersonality.preventsLastFoodEatenThought} /
 * {@code preventsDietVarietyThought}, read in {@code HumanMob.getStaticThoughts}
 * (VERIFIED [jar], 1.3.3).
 */
public class VampirePersonality extends SettlerPersonality {

    public VampirePersonality(HumanMob mob) {
        super(mob);
    }

    @Override
    public boolean preventsLastFoodEatenThought() {
        return true;
    }

    @Override
    public boolean preventsDietVarietyThought() {
        return true;
    }
}
