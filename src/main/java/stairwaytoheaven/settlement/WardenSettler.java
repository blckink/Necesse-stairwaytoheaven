package stairwaytoheaven.settlement;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.playerStats.PlayerStats;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.settler.Settler;

/**
 * The settlement-side registration of the recruited Warden.
 *
 * WHY THIS EXISTS: {@code HumanMob.getSettler()} resolves a mob's
 * {@code settlerStringID} through {@link necesse.engine.registries.SettlerRegistry},
 * and {@code LevelSettler}'s constructor runs {@code Objects.requireNonNull} on
 * the result. Without an entry here the Warden mob exists but is not a settler
 * type at all: the vanilla recruit path answers "notsettler", he can never take
 * a bed, and the 100,000-coin payment leads nowhere. Verified at runtime before
 * this class existed — {@code /skyreachstatus} reported
 * {@code wardensettler=NOT REGISTERED mobRegistered=true}.
 *
 * He is modelled on {@code ElderSettler}: a unique, story-bound resident rather
 * than a settler type the world can roll. He never spawns on his own, never
 * moves out, cannot be banished, and never returns as a random recruit — the
 * only way he joins a settlement is the recruitment in the sky.
 */
public class WardenSettler extends Settler {

    public WardenSettler() {
        super("wardensettler");
        // Vanilla's COMPLETE_HOST achievement wants one of every settler type
        // in the settlement. A modded settler must stay out of that set, or
        // installing the mod would make the achievement unreachable.
        this.isPartOfCompleteHost = false;
    }

    @Override
    public void loadTextures() {
        this.texture = GameTexture.fromFile("mobs/icons/wardensettler");
    }

    /** He is recruited in the Skyreach, never rolled into a settlement. */
    @Override
    public boolean canSpawnInSettlement(ServerSettlementData settlement, PlayerStats stats) {
        return false;
    }

    /** Bought once, for a fortune — he does not wander off again (Elder rule). */
    @Override
    public boolean canMoveOut(LevelSettler settler, ServerSettlementData settlement) {
        return false;
    }

    @Override
    public boolean canBanish(LevelSettler settler, ServerSettlementData settlement) {
        return false;
    }

    /** No random replacement Warden after a death — there is exactly one. */
    @Override
    public float getArriveAsRecruitAfterDeathChance(ServerSettlementData settlement) {
        return 0.0F;
    }

    @Override
    public GameMessage getAcquireTip() {
        return new LocalMessage("misc", "wardensettlertip");
    }
}
