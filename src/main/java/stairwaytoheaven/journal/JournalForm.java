package stairwaytoheaven.journal;

import java.awt.Color;
import java.awt.Rectangle;

import necesse.engine.GlobalData;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.engine.state.MainGame;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.MainGameFormManager;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;
import necesse.gfx.ui.GameInterfaceStyle;

/**
 * The journal window: realm chapters down the left, the open chapter on the
 * right, scrolling.
 *
 * <p>Built only from vanilla form parts this mod already uses elsewhere
 * ({@code Form}, {@code FormContentBox}, {@code FormLabel},
 * {@code FormTextButton} — {@code pickupfilter/PickupFilterForm} ships the
 * same set), laid out like vanilla's own settlement config pages: title, a
 * column of buttons, a scrolling content box. It holds no state of its own
 * beyond which chapter is open; everything it shows came in one
 * {@link PacketJournalOpen}, and the refresh button asks for a new one.
 *
 * <p>Every line carries its status as a WORD ("[Done]", "[Active]") as well
 * as a colour, so the page reads the same in any interface style and for a
 * colour-blind player.
 *
 * <p>HYPOTHESIS until a player opens it: the layout, sizes and colours are
 * written against the decompiled 1.3.2 form classes, and the server-side gate
 * ({@code /swhjournal}) only proves the book it shows is complete.
 */
public class JournalForm extends Form {

    public static final int WIDTH = 660;
    public static final int HEIGHT = 470;
    private static final int COLUMN = 180;
    private static final int TOP = 38;

    /** The open window, so a second packet refreshes it instead of stacking a second one. */
    private static JournalForm open;
    /** Remembered for the session, so reopening lands where the player was reading. */
    private static int lastRealm = -1;

    private final MainGameFormManager manager;
    private JournalBook book;
    private int realm;
    private FormContentBox content;

    private JournalForm(MainGameFormManager manager, JournalBook book) {
        super("swhjournal", WIDTH, HEIGHT);
        this.manager = manager;
        this.book = book;
        this.realm = lastRealm >= 0 && book.chapter(lastRealm) != null ? lastRealm : firstOpenChapter(book);
        this.rebuild();
    }

    // ------------------------------------------------------------------
    // opening

    /** Client side: show this book, reusing the window if it is already open. */
    public static void show(Client client, JournalBook book) {
        if (!(GlobalData.getCurrentState() instanceof MainGame) || book == null) {
            return;
        }
        MainGameFormManager manager = ((MainGame) GlobalData.getCurrentState()).formManager;
        if (manager == null) {
            return;
        }
        if (open != null && open.manager == manager && manager.hasComponent(open)) {
            open.book = book;
            if (book.chapter(open.realm) == null) {
                open.realm = firstOpenChapter(book);
            }
            open.setHidden(false);
            open.rebuild();
            return;
        }
        open = manager.addComponent(new JournalForm(manager, book));
        open.onWindowResized(WindowManager.getWindow());
    }

    /**
     * Hides the window. It is kept (and reused by the next {@link #show}) rather
     * than removed from the manager from inside its own input handler — hiding
     * is the path {@code PickupFilterForm} already takes, and it leaves the
     * manager's component list alone while that list is being walked.
     */
    private void close() {
        this.setHidden(true);
    }

    /** The first chapter the player can still do something in; the first chapter otherwise. */
    private static int firstOpenChapter(JournalBook book) {
        for (JournalChapter chapter : book.chapters) {
            for (JournalStep step : chapter.steps) {
                if (step.status == JournalStatus.ACTIVE || step.status == JournalStatus.AVAILABLE) {
                    return chapter.realm;
                }
            }
        }
        return book.chapters.isEmpty() ? 0 : book.chapters.get(0).realm;
    }

    // ------------------------------------------------------------------
    // layout

    private void rebuild() {
        this.clearComponents();
        lastRealm = this.realm;

        this.addComponent(new FormLabel(text(new LocalMessage("journal", "title")), new FontOptions(20), -1, 10, 8));

        int y = TOP;
        for (JournalChapter chapter : this.book.chapters) {
            String label = text(chapter.name) + "  " + chapter.doneSteps() + "/" + chapter.steps.size();
            boolean selected = chapter.realm == this.realm;
            FormTextButton button = this.addComponent(new FormTextButton(label, 8, y, COLUMN - 12,
                    FormInputSize.SIZE_24, selected ? ButtonColor.YELLOW : ButtonColor.BASE));
            final int target = chapter.realm;
            button.onClicked(e -> {
                this.realm = target;
                this.rebuild();
            });
            y += 28;
        }

        this.addComponent(new FormTextButton(text(new LocalMessage("journal", "refresh")), 8, HEIGHT - 64,
                COLUMN - 12, FormInputSize.SIZE_24, ButtonColor.BASE))
                .onClicked(e -> {
                    if (GlobalData.getCurrentState() instanceof MainGame) {
                        Client client = ((MainGame) GlobalData.getCurrentState()).getClient();
                        if (client != null) {
                            client.network.sendPacket(new PacketJournalRequest());
                        }
                    }
                });
        this.addComponent(new FormTextButton(text(new LocalMessage("journal", "close")), 8, HEIGHT - 34,
                COLUMN - 12, FormInputSize.SIZE_24, ButtonColor.BASE))
                .onClicked(e -> this.close());

        this.content = this.addComponent(new FormContentBox(COLUMN, TOP, WIDTH - COLUMN - 6, HEIGHT - TOP - 6));
        JournalChapter chapter = this.book.chapter(this.realm);
        int bottom = chapter == null ? 0 : this.layoutChapter(chapter);
        this.content.setContentBox(new Rectangle(0, 0, WIDTH - COLUMN - 6, bottom + 8));
    }

    /** Lays one chapter into the content box; returns the height used. */
    private int layoutChapter(JournalChapter chapter) {
        GameInterfaceStyle style = this.getInterfaceStyle();
        int width = WIDTH - COLUMN - 6 - 24;
        int y = 4;
        y = this.line(text(chapter.name), 20, null, 4, y, width) + 2;
        y = this.line(text(chapter.intro), 12, style.inactiveTextColor, 4, y, width) + 10;

        y = this.heading("secsteps", y, width);
        if (chapter.steps.isEmpty()) {
            y = this.line(text(new LocalMessage("journal", "nosteps")), 12, style.inactiveTextColor, 12, y, width);
        }
        for (JournalStep step : chapter.steps) {
            y = this.layoutStep(step, y, width, style) + 8;
        }

        y = this.heading("secfacts", y + 2, width);
        y = this.lines(chapter.facts, y, width, style);
        y = this.heading("secresidents", y + 6, width);
        y = this.lines(chapter.residents, y, width, style);
        y = this.heading("seclandmarks", y + 6, width);
        y = this.lines(chapter.landmarks, y, width, style);

        y = this.heading("seclore", y + 6, width);
        for (JournalLine entry : chapter.lore) {
            boolean unlocked = entry.status == JournalStatus.DONE;
            y = this.line(text(entry.title), 16, unlocked ? null : style.inactiveTextColor, 12, y, width - 8);
            y = this.line(text(entry.detail), 12, unlocked ? null : style.inactiveTextColor, 20, y, width - 16) + 6;
        }
        return y;
    }

    private int layoutStep(JournalStep step, int y, int width, GameInterfaceStyle style) {
        Color colour = colourOf(step.status, style);
        y = this.line(statusWord(step.status) + " " + text(step.title), 16, colour, 8, y, width - 4);
        int x = 20;
        int w = width - 16;
        if (step.giver != null || step.where != null) {
            String who = step.giver == null ? text(step.where)
                    : step.where == null ? text(step.giver)
                    : text(step.giver) + " - " + text(step.where);
            y = this.line(text(new LocalMessage("journal", "givenby")) + " " + who, 12, style.inactiveTextColor, x, y, w);
        }
        if (step.worldScoped) {
            y = this.line(text(new LocalMessage("journal", "worldscoped")), 12, style.inactiveTextColor, x, y, w);
        }
        switch (step.status) {
            case DONE:
                if (step.reward != null) {
                    y = this.line(text(new LocalMessage("journal", "rewardreceived")) + " " + text(step.reward),
                            12, style.successTextColor, x, y, w);
                }
                break;
            case LOCKED:
                break;
            default:
                if (step.description != null) {
                    y = this.line(text(step.description), 12, null, x, y, w);
                }
                if (step.why != null) {
                    y = this.line(text(new LocalMessage("journal", "whylabel")) + " " + text(step.why), 12, null, x, y, w);
                }
                for (GameMessage objective : step.objectives) {
                    y = this.line(text(objective), 12, null, x + 6, y, w - 6);
                }
                if (step.opens != null) {
                    y = this.line(text(new LocalMessage("journal", "openslabel")) + " " + text(step.opens),
                            12, style.highlightTextColor, x, y, w);
                }
                if (step.reward != null) {
                    y = this.line(text(new LocalMessage("journal", "rewardlabel")) + " " + text(step.reward),
                            12, style.highlightTextColor, x, y, w);
                }
                break;
        }
        if (step.hint != null) {
            y = this.line(text(step.hint), 12, style.inactiveTextColor, x, y, w);
        }
        return y;
    }

    private int lines(java.util.List<JournalLine> lines, int y, int width, GameInterfaceStyle style) {
        for (JournalLine entry : lines) {
            y = this.line(statusMark(entry.status) + " " + text(entry.title), 12, colourOf(entry.status, style),
                    12, y, width - 8);
            if (entry.detail != null) {
                y = this.line(text(entry.detail), 12, style.inactiveTextColor, 26, y, width - 22);
            }
            y += 3;
        }
        return y;
    }

    private int heading(String key, int y, int width) {
        return this.line(text(new LocalMessage("journal", key)), 16, this.getInterfaceStyle().highlightTextColor,
                4, y, width) + 4;
    }

    /** Adds one wrapped label to the content box; returns the y below it. */
    private int line(String text, int size, Color colour, int x, int y, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return y;
        }
        FontOptions font = new FontOptions(size);
        if (colour != null) {
            font = font.color(colour);
        }
        FormLabel label = this.content.addComponent(new FormLabel(text, font, -1, x, y, maxWidth));
        return y + Math.max(size + 2, label.getHeight());
    }

    // ------------------------------------------------------------------
    // words and colours

    private static String text(GameMessage message) {
        return message == null ? "" : message.translate();
    }

    private static String statusWord(JournalStatus status) {
        switch (status) {
            case DONE: return Localization.translate("journal", "statusdone");
            case ACTIVE: return Localization.translate("journal", "statusactive");
            case AVAILABLE: return Localization.translate("journal", "statusavailable");
            default: return Localization.translate("journal", "statuslocked");
        }
    }

    /** The short form for fact / resident / landmark lines. */
    private static String statusMark(JournalStatus status) {
        switch (status) {
            case DONE: return "[+]";
            case ACTIVE: return "[~]";
            case AVAILABLE: return "[o]";
            default: return "[-]";
        }
    }

    private static Color colourOf(JournalStatus status, GameInterfaceStyle style) {
        switch (status) {
            case DONE: return style.successTextColor;
            case ACTIVE: return style.highlightTextColor;
            case AVAILABLE: return style.activeTextColor;
            default: return style.inactiveTextColor;
        }
    }

    // ------------------------------------------------------------------
    // window behaviour

    @Override
    public void onWindowResized(GameWindow window) {
        super.onWindowResized(window);
        this.setPosMiddle(window.getHudWidth() / 2, window.getHudHeight() / 2);
    }

    /** Escape closes the journal (and is used, so it does not also open the pause menu). */
    @Override
    public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
        if (!this.isHidden() && event.state && !event.isUsed() && event.getID() == 256) {
            event.use();
            this.close();
            return;
        }
        super.handleInputEvent(event, tickManager, perspective);
    }
}
