package stairwaytoheaven.quest;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.quest.DeliverItemsQuest;
import necesse.gfx.fairType.FairType;
import necesse.gfx.gameFont.FontOptions;

/**
 * The Cold Forge — Caspern's own chain, in the Ghost band.
 *
 * <h2>Why the Spirit Smith gets a quest</h2>
 * Same finding as {@link MortimerRitesQuest}: he sold Spiritsteel and had
 * nothing to say about it. He is the only source of the Ghost band's bar in a
 * mod whose other smithing — the Aether Forge — is a station the player builds
 * and staffs, so "the smith's fire has gone out and he needs what it takes to
 * relight it" is the one errand that explains his shop rather than sitting
 * beside it.
 *
 * <h2>Why these two materials</h2>
 * Spectral Ore is what he smelts (his own shelf sells it, the Ghost band's
 * spectral rock is the only place it comes from), and Veil Essence is what the
 * Gloom Shades of the Gloomfen and the Ashen Reach drop — the two ex-Veil
 * biomes that {@code WORLD_DESIGN} §41.5 moved into this band and that no quest
 * in the mod had yet given a player a reason to enter. Both are
 * Ghost-band-only, per the rule {@code SkyreachKeyQuest} states for the key
 * quests, and between them they send the player across both halves of the
 * realm rather than around one biome.
 *
 * <p>Veil Essence had three buyers and no consumer: Mortimer, Eleanor and the
 * Ghost Guide all pay coins for it, which makes it money rather than a
 * material. This is the first thing in the mod that spends it.
 *
 * <h2>Reward</h2>
 * His recruit fee (14 000 — the second-highest in the mod) waived, plus 6
 * Spiritsteel Bar, matching {@link MortimerRitesQuest} bar for bar. The two are
 * the same rung of the same realm and neither should read as the better one to
 * do first; what separates them is the fee, and his is nearly twice Mortimer's.
 */
public class CaspernForgeQuest extends DeliverItemsQuest {

    /** What he smelts. The Ghost band's spectral rock is the only source. */
    public static final int SPECTRAL_ORE = 12;
    /** What he quenches it in. The Gloomfen and Ashen Reach shades drop it. */
    public static final int VEIL_ESSENCE = 8;

    public CaspernForgeQuest() {
        super(new ItemObjective("spectralore", SPECTRAL_ORE),
                new ItemObjective("veilessence", VEIL_ESSENCE));
    }

    @Override
    public GameMessage getTitle() {
        return new LocalMessage("quests", "swhcaspernforgetitle");
    }

    @Override
    public GameMessage getDescription() {
        return new LocalMessage("quests", "swhcaspernforgedesc");
    }

    @Override
    public FairType getRewardType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhcaspernforgereward"));
    }

    @Override
    public FairType getHandInType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhspeaktocaspern"));
    }
}
