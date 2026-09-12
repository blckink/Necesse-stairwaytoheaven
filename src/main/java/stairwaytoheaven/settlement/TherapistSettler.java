package stairwaytoheaven.settlement;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;

/**
 * The Therapist as a settlement settler type — i.e. as a PROFESSION, not as a
 * character.
 *
 * <h2>Why this is a {@link ProfessionSettler} and not a
 * {@code SkySettlers.SkyResident}</h2>
 *
 * Every settler this mod had before the Therapist was a named individual:
 * Magpie, Halda, Eveleen. {@code SkyResident} is built for exactly that — it
 * claims its name once per world ({@code SkywatchWorldData.residentsClaimed}),
 * refuses the free move-in roll, cannot be banished and never arrives twice. A
 * profession is the opposite of all four: there can be a Therapist in every
 * settlement, a second one can turn up if the first dies, and the player may
 * send them away again.
 *
 * <p>The shared half — 75 recruit tickets behind no story gate, the
 * COMPLETE_HOST opt-out, the icon and the clothes going on — lives in
 * {@link ProfessionSettler}. What is left here is the four things that make
 * this profession this one.
 *
 * @see SkyTherapy for the services
 */
public class TherapistSettler extends ProfessionSettler {

    public TherapistSettler() {
        super("therapisthuman");
    }

    @Override
    public GameMessage getAcquireTip() {
        return new LocalMessage("settlement", "therapisttip");
    }

    /**
     * Vanilla's Alchemist face, the closest thing vanilla's roster draws to
     * someone who tends to people rather than to ground. Row in
     * {@code docs/VANILLA_ASSET_MAP.md}.
     */
    @Override
    protected String iconPath() {
        return "mobs/icons/alchemisthuman";
    }

    /**
     * Consulting-room clothes. Two items rather than three: no vanilla headwear
     * reads as this job, and an arbitrary hat would make the profession look
     * like a costume.
     */
    @Override
    protected String[] wardrobe() {
        return new String[]{null, "blazer", "dressshoes"};
    }
}
