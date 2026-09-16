package stairwaytoheaven.settlement;

import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.settler.personalities.SettlerPersonality;

/**
 * The Twilight Merchant's own personality, after vanilla's
 * {@code ExoticMerchantSettlerPersonality}. It only exists for its name: the
 * dialogue shows the personality as the subtitle under the merchant's name,
 * and borrowing the Exotic Merchant's showed "Exotic Merchant" there.
 */
public class TwilightMerchantPersonality extends SettlerPersonality {

    public TwilightMerchantPersonality(HumanMob mob) {
        super(mob);
    }
}
