package stairwaytoheaven.mobs;

import java.io.FileNotFoundException;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.engine.world.WorldEntity;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.Buff;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;

/**
 * Blood Fever / Blutfieber — what a bitten resident is left with.
 *
 * <p>The design keeps a bite deliberately SHORT of a conversion: the settler
 * stays himself, keeps his bed, his job and his schedule, and is merely pale,
 * slow and unwell for a while. Turning a resident into a second nocturnal
 * settler would mean replacing that settler's mob, which is a save-breaking
 * operation on somebody the player has already furnished a room for; the fever
 * is the honest, reversible half of the same idea.
 *
 * <p>It runs out on its own after a day, and the mod's Doctor can end it early
 * — see {@code settlement/DoctorHealDialogue}.
 *
 * <p>The icon is vanilla's own by literal path, the way {@link CatCuddleBuff}
 * borrows one: a debuff icon costs no new art, and a buff with a missing
 * texture is a crash on the client rather than a blank square.
 */
public class BloodFeverBuff extends Buff {

    /**
     * The registry key. Registered as a literal in
     * {@code StairwayToHeavenMod} so the locale audit can name-check it; this
     * constant exists for the code that APPLIES the buff and carries the same
     * string.
     */
    public static final String ID = "bloodfever";
    /** Vanilla's bleeding icon — the closest thing in its own set. */
    public static final String BORROWED_ICON = "buffs/bleeding";

    public BloodFeverBuff() {
        this.isVisible = true;
        this.canCancel = false;
        this.shouldSave = true;
        this.isImportant = true;
    }

    /** One in-game day. Long enough to notice, short enough to forgive. */
    public static int durationMs() {
        return WorldEntity.DEFAULT_DAY_TOTAL * 1000;
    }

    @Override
    public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
        buff.setModifier(BuffModifiers.SLOW, 0.20F);
        buff.setModifier(BuffModifiers.MINING_SPEED, -0.15F);
        buff.setModifier(BuffModifiers.BUILDING_SPEED, -0.15F);
        buff.setModifier(BuffModifiers.MAX_HEALTH, -0.10F);
    }

    @Override
    public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
        ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
        tooltips.add(Localization.translate("bufftooltip", "bloodfevertip"));
        return tooltips;
    }

    @Override
    public void loadTextures() {
        try {
            this.iconTexture = GameTexture.fromFileRaw(BORROWED_ICON);
        } catch (FileNotFoundException e) {
            super.loadTextures();
        }
    }
}
