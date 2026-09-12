package stairwaytoheaven.settlement;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;

/**
 * The Doctor as a settlement settler type — the mod's second PROFESSION, and
 * the one that shows what a third costs: this file.
 *
 * <p>Everything the Therapist and the Doctor do the same is in
 * {@link ProfessionSettler}: 75 recruit tickets behind no story gate (the
 * Blacksmith's and the Miner's terms), staying out of vanilla's COMPLETE_HOST
 * achievement, the settlement-screen face and the clothes. What is left is the
 * mob it stands up, that face, those clothes and its own acquire tip.
 *
 * @see SkyDoctor for the shop and the healing
 */
public class DoctorSettler extends ProfessionSettler {

    public DoctorSettler() {
        super("doctorhuman");
    }

    @Override
    public GameMessage getAcquireTip() {
        return new LocalMessage("settlement", "doctortip");
    }

    /**
     * Vanilla's Trader face — a plain one, deliberately.
     *
     * <p>The obvious pick was the Alchemist's hooded apothecary, and the
     * Therapist already wears it; two professions sharing one fallback face
     * would read as a mistake. Everything else vanilla draws comes with a hat
     * that belongs to another job (the Miner's lamp, the Angler's sou'wester,
     * the Mage's pointed hat), and this face has none — which is right for a
     * profession whose identity is the surgical mask and the lab coat the mob
     * itself wears. Row in {@code docs/VANILLA_ASSET_MAP.md}.
     */
    @Override
    protected String iconPath() {
        return "mobs/icons/traderhuman";
    }

    /** Mask, coat, boots — vanilla's own three, all sold by its Alchemist. */
    @Override
    protected String[] wardrobe() {
        return new String[]{"surgicalmask", "labcoat", "labboots"};
    }
}
