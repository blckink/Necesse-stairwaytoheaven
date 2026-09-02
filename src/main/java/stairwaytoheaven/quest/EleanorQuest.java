package stairwaytoheaven.quest;

import java.awt.Color;
import java.awt.Rectangle;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.server.ServerClient;
import necesse.engine.quest.Quest;
import necesse.gfx.drawOptions.DrawOptionsBox;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawOptions.StringDrawOptions;
import necesse.gfx.fairType.FairType;
import necesse.gfx.gameFont.FontOptions;

/**
 * HUD quest for Eleanor's story — {@code docs/WORLD_DESIGN.md} §11.
 *
 * <p>Pure signpost, exactly like {@link FindSpireQuest}: no tracked state, no
 * progress to poll. Everything that actually decides her ending already lived
 * in {@code EleanorMob} before this class existed — holding
 * {@code veilessence} and talking to her is PASS ON, talking without it opens
 * the ordinary recruit page for STAY — and still does; this only wraps that
 * encounter in a journal entry so the player has something to read that says
 * "there is a choice here" and, later, which one they made.
 *
 * <p>Handed out by {@code EleanorMob.interact} the first time she is found
 * (while she is neither settler nor visitor) and removed by whichever ending
 * actually happens: {@code EleanorMob.interact}'s PASS ON branch, or
 * {@code EleanorMob.onRecruited} for STAY.
 */
public class EleanorQuest extends Quest {

    public EleanorQuest() {
    }

    @Override
    public void tick(ServerClient client) {
    }

    @Override
    public boolean canComplete(NetworkClient client) {
        return true;
    }

    @Override
    public GameMessage getTitle() {
        return new LocalMessage("quests", "swheleanortitle");
    }

    @Override
    public GameMessage getDescription() {
        return new LocalMessage("quests", "swheleanordesc");
    }

    @Override
    public DrawOptionsBox getProgressDrawBox(NetworkClient client, final int x, final int y, final int width,
            Color textColor, boolean outlined) {
        final DrawOptionsList drawOptions = new DrawOptionsList();
        FontOptions fo = new FontOptions(16).outline(outlined);
        if (textColor != null) {
            fo.color(textColor);
        }
        drawOptions.add(new StringDrawOptions(fo, Localization.translate("quests", "swheleanorobj")).pos(x, y));
        return new DrawOptionsBox() {
            @Override
            public Rectangle getBoundingBox() {
                return new Rectangle(x, y, width, 16);
            }

            @Override
            public void draw() {
                drawOptions.draw();
            }
        };
    }

    @Override
    public FairType getRewardType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swheleanorreward"));
    }

    @Override
    public FairType getHandInType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhspeaktoeleanor"));
    }
}
