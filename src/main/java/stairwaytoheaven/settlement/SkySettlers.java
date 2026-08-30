package stairwaytoheaven.settlement;

import necesse.engine.playerStats.PlayerStats;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.SettlerRegistry;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.settler.Settler;

/**
 * The Skyreach's three hireable residents, as real settlement settlers.
 *
 * A mob alone is not a settler. {@code HumanMob.getSettler()} resolves the
 * mob's settler key through {@link SettlerRegistry}, and {@code LevelSettler}
 * runs {@code Objects.requireNonNull} on the result — so without a registered
 * {@link Settler} the vanilla recruit path answers "notsettler", the recruit
 * button can never work, and the NPC can never take a bed. That is exactly the
 * bug the Warden shipped with, and it is why every one of these gets its type
 * here in the same breath as its mob.
 *
 * {@code Settler} objects must be constructed while the registry is OPEN, i.e.
 * during {@code init()}. {@code onSettlerRegistryClosed} then validates that
 * each mobStringID resolves to a mob implementing SettlerMob, so a bad
 * registration fails the server boot loudly instead of silently.
 */
public final class SkySettlers {

    private SkySettlers() {
    }

    public static void register() {
        MobRegistry.registerMob("magpiesettler", stairwaytoheaven.mobs.MagpieMob.class, false);
        MobRegistry.registerMob("haldasettler", stairwaytoheaven.mobs.HaldaMob.class, false);
        MobRegistry.registerMob("ossiansettler", stairwaytoheaven.mobs.OssianMob.class, false);

        SettlerRegistry.registerSettler("magpiesettler", new SkyResident("magpiesettler"));
        SettlerRegistry.registerSettler("haldasettler", new SkyResident("haldasettler"));
        SettlerRegistry.registerSettler("ossiansettler", new SkyResident("ossiansettler"));
    }

    /**
     * One settler type, shared by all three: they differ in what they sell, not
     * in how they move in.
     */
    public static class SkyResident extends Settler {

        public SkyResident(String mobStringID) {
            super(mobStringID);
            // Vanilla's COMPLETE_HOST achievement wants one of every settler
            // type in a settlement. A modded settler must stay out of that set
            // or installing this mod makes the achievement unreachable.
            this.isPartOfCompleteHost = false;
        }

        @Override
        public void loadTextures() {
            this.texture = GameTexture.fromFile("mobs/icons/" + this.mobStringID);
        }

        /** Found in the Skyreach and hired there — never rolled into a town. */
        @Override
        public boolean canSpawnInSettlement(ServerSettlementData settlement, PlayerStats stats) {
            return false;
        }

        /** Paid for, and a named character: they do not wander off again. */
        @Override
        public boolean canMoveOut(LevelSettler settler, ServerSettlementData settlement) {
            return false;
        }

        @Override
        public boolean canBanish(LevelSettler settler, ServerSettlementData settlement) {
            return false;
        }

        /** No random stand-in arrives if one dies. They are individuals. */
        @Override
        public float getArriveAsRecruitAfterDeathChance(ServerSettlementData settlement) {
            return 0.0F;
        }
    }
}
