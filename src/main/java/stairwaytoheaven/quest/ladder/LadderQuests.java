package stairwaytoheaven.quest.ladder;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.quest.DeliverItemsQuest;
import necesse.gfx.fairType.FairType;
import necesse.gfx.gameFont.FontOptions;

/**
 * The twelve delivery quests the Spire Village's ladder added
 * ({@code docs/design/chapter-03-spire-village.md} §4).
 *
 * <p>Each is a vanilla {@link DeliverItemsQuest}: the journal lists what is
 * still missing and how much, turning in removes the items
 * ({@code DeliverItemsQuest.complete}), and the objectives are saved with the
 * quest. {@code QuestRegistry} builds a quest from its class with the no-arg
 * constructor when a save loads, so every step is its own public class — the
 * constructor IS the step's ask. Texts are {@code quests.<step>title/desc/
 * reward} and the hand-in line is the giver's {@code swhspeakto<name>}.
 *
 * <p><b>What they ask for, and the rule behind it.</b> Every ask is renewable —
 * mob drops, rocks, plants, the realm's own ground — or has a way back if the
 * one-of-a-kind item was lost: Magpie keeps a copy of the Ledger and the Writ
 * for a player who holds her quest ({@code MagpieMob}). A ladder whose gate can
 * be locked for ever by one sold book is the {@code swh_beacon} dead end
 * again. Where a step names a landmark or a boss, the ask is what that place
 * or its guard drops, so the quest is a reason to go there.
 */
public final class LadderQuests {

    private LadderQuests() {
    }

    /** Shared texts: title, description, reward line, "speak with". */
    public abstract static class Step extends DeliverItemsQuest {

        protected Step() {
        }

        protected Step(ItemObjective first, ItemObjective... more) {
            super(first, more);
        }

        /** The quest's registry ID, which is also its locale stem. */
        protected abstract String stepID();

        /** {@code quests.swhspeakto<giver>}. */
        protected abstract String handIn();

        private String stem() {
            return this.stepID().replace("_", "");
        }

        @Override
        public GameMessage getTitle() {
            return new LocalMessage("quests", this.stem() + "title");
        }

        @Override
        public GameMessage getDescription() {
            return new LocalMessage("quests", this.stem() + "desc");
        }

        @Override
        public FairType getRewardType(NetworkClient client, boolean outlined) {
            return new FairType().append(new FontOptions(12).outline(outlined),
                    Localization.translate("quests", this.stem() + "reward"));
        }

        @Override
        public FairType getHandInType(NetworkClient client, boolean outlined) {
            return new FairType().append(new FontOptions(12).outline(outlined),
                    Localization.translate("quests", this.handIn()));
        }
    }

    // ---------------------------------------------------------------- I --

    /** Magpie: the Ledger of Undelivered Post, out of the Skyway Toll-House. */
    public static class Post extends Step {
        public Post() {
            super(new ItemObjective("postledger", 1));
        }
        @Override protected String stepID() { return "swh_ladder_post"; }
        @Override protected String handIn() { return "swhspeaktomagpie"; }
    }

    /** Halda: what a cellar ferments — Driftlands berries and wheat, Aurora petals. */
    public static class Round extends Step {
        public Round() {
            super(new ItemObjective("cloudberry", 16),
                    new ItemObjective("windwheat", 8),
                    new ItemObjective("aurorapetal", 4));
        }
        @Override protected String stepID() { return "swh_ladder_round"; }
        @Override protected String handIn() { return "swhspeaktohalda"; }
    }

    /** Ossian: what Prototype Nine was built from — the Stormveil's glass and shards. */
    public static class Prototype extends Step {
        public Prototype() {
            super(new ItemObjective("stormglass", 6),
                    new ItemObjective("stormshard", 8),
                    new ItemObjective("fulgurite", 4));
        }
        @Override protected String stepID() { return "swh_ladder_prototype"; }
        @Override protected String handIn() { return "swhspeaktoossian"; }
    }

    // --------------------------------------------------------------- II --

    /** Halda: Paradise must — Eden's apples, coconuts and sap. */
    public static class Cider extends Step {
        public Cider() {
            super(new ItemObjective("paradiseapple", 4),
                    new ItemObjective("paradisecoconut", 2),
                    new ItemObjective("edensap", 4));
        }
        @Override protected String stepID() { return "swh_ladder_cider"; }
        @Override protected String handIn() { return "swhspeaktohalda"; }
    }

    /** Magpie: contraband out of the Garden, and the Writ that lets it through. */
    public static class Contraband extends Step {
        public Contraband() {
            super(new ItemObjective("serpentscale", 6),
                    new ItemObjective("venomfang", 4),
                    new ItemObjective("skywaywrit", 1));
        }
        @Override protected String stepID() { return "swh_ladder_contraband"; }
        @Override protected String handIn() { return "swhspeaktomagpie"; }
    }

    // -------------------------------------------------------------- III --

    /** Ossian: an echo for the archive, and a mourning band off a Stone Mourner. */
    public static class Echo extends Step {
        public Echo() {
            super(new ItemObjective("echoshard", 10),
                    new ItemObjective("mourningband", 1));
        }
        @Override protected String stepID() { return "swh_ladder_echo"; }
        @Override protected String handIn() { return "swhspeaktoossian"; }
    }

    /** Ives: eleven of each, for the one who walks the same eleven steps. */
    public static class Steps extends Step {
        public Steps() {
            super(new ItemObjective("gravesalt", 11),
                    new ItemObjective("spiritmoss", 11),
                    new ItemObjective("palestone", 11));
        }
        @Override protected String stepID() { return "swh_ladder_steps"; }
        @Override protected String handIn() { return "swhspeaktoives"; }
    }

    // --------------------------------------------------------------- IV --

    /** Caspern: a Soul Collar off a Mourning Bride, and the Aftergarden's wood. */
    public static class Memory extends Step {
        public Memory() {
            super(new ItemObjective("soulcollar", 1),
                    new ItemObjective("bonewood", 10),
                    new ItemObjective("ectoplasm", 8));
        }
        @Override protected String stepID() { return "swh_ladder_memory"; }
        @Override protected String handIn() { return "swhspeaktocaspern"; }
    }

    /** Mortimer: shrouds for the Wedding Feast's guests. */
    public static class Shrouds extends Step {
        public Shrouds() {
            super(new ItemObjective("soulthread", 10),
                    new ItemObjective("veilessence", 6));
        }
        @Override protected String stepID() { return "swh_ladder_shrouds"; }
        @Override protected String handIn() { return "swhspeaktomortimer"; }
    }

    // ---------------------------------------------------------------- V --

    /** Eveleen: the three seeds she never planted, and the Crooked eyes to ask them. */
    public static class Seeds extends Step {
        public Seeds() {
            super(new ItemObjective("eyeseed", 6),
                    new ItemObjective("knowledgecutting", 3));
        }
        @Override protected String stepID() { return "swh_ladder_seeds"; }
        @Override protected String handIn() { return "swhspeaktoeveleen"; }
    }

    /** Mr. Knott: striped shells, and a Door Mimic's horn. */
    public static class Shells extends Step {
        public Shells() {
            super(new ItemObjective("stripedshell", 8),
                    new ItemObjective("stripedhorn", 1));
        }
        @Override protected String stepID() { return "swh_ladder_shells"; }
        @Override protected String handIn() { return "swhspeaktoknott"; }
    }

    // --------------------------------------------------------------- VI --

    /** Ossian: Form 666-B, filled in with Hell's own paperwork. */
    public static class Form extends Step {
        public Form() {
            super(new ItemObjective("realityshard", 16),
                    new ItemObjective("oddwood", 24),
                    new ItemObjective("charwood", 30));
        }
        @Override protected String stepID() { return "swh_ladder_form"; }
        @Override protected String handIn() { return "swhspeaktoossian"; }
    }
}
