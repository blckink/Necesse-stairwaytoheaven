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
 * HUD quest for stage 1: hire the Sky Warden into your settlement.
 *
 * WHY THIS EXISTS: FindSpireQuest was the only quest the mod ever handed out.
 * Meeting the Warden removed it and gave nothing back, so from first contact
 * onwards the player's journal was empty and the 100,000-coin price existed
 * only inside a speech bubble that scrolled away. Reported from a playtest as
 * "wie gibt man dem Warden denn das Geld? seh keinen Dialog o.Ä." and "die
 * quests von ihm seh ich auch nirgends im Journal".
 *
 * The objective line names the price, because the journal is the one place a
 * player can go back and re-read it.
 */
public class RecruitWardenQuest extends Quest {

    public RecruitWardenQuest() {
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
        return new LocalMessage("quests", "swhrecruitwardentitle");
    }

    @Override
    public GameMessage getDescription() {
        return new LocalMessage("quests", "swhrecruitwardendesc");
    }

    @Override
    public DrawOptionsBox getProgressDrawBox(NetworkClient client, final int x, final int y, final int width,
            Color textColor, boolean outlined) {
        final DrawOptionsList drawOptions = new DrawOptionsList();
        FontOptions fo = new FontOptions(16).outline(outlined);
        if (textColor != null) {
            fo.color(textColor);
        }
        drawOptions.add(new StringDrawOptions(fo, Localization.translate("quests", "swhrecruitwardenobj",
                "cost", String.valueOf(stairwaytoheaven.mobs.SkyWardenMob.RECRUIT_COST))).pos(x, y));
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
        return null;
    }

    @Override
    public FairType getHandInType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhreturnwarden"));
    }
}
