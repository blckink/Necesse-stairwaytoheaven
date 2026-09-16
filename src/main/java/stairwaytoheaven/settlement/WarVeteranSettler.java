package stairwaytoheaven.settlement;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;

/**
 * The War Veteran as a settlement settler type — a {@link ProfessionSettler}
 * like the Therapist and Doctor: any settlement may hold one.
 *
 * <p>Always dressed as a soldier: a fixed vanilla iron armor set, regardless
 * of the settler's own randomized look, so he reads as "the settlement's
 * soldier" at a glance.
 */
public class WarVeteranSettler extends ProfessionSettler {

    public WarVeteranSettler() {
        super("warveteranhuman");
    }

    @Override
    public GameMessage getAcquireTip() {
        return new LocalMessage("settlement", "warveterantip");
    }

    /**
     * Vanilla's Guard face — the closest thing vanilla's roster draws to a
     * soldier. Row in {@code docs/VANILLA_ASSET_MAP.md}.
     */
    @Override
    protected String iconPath() {
        return "mobs/icons/guardhuman";
    }

    /** Fixed soldier look: vanilla iron helmet, chestplate and boots. */
    @Override
    protected String[] wardrobe() {
        return new String[]{"ironhelmet", "ironchestplate", "ironboots"};
    }
}
