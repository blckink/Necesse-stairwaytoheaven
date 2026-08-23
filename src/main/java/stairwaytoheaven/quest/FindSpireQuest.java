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
 * HUD quest for stage 0: find the Warden's Spire and talk to the Warden.
 * Pure signpost — no tracked state; completed by the Warden's first dialogue.
 */
public class FindSpireQuest extends Quest {

    public FindSpireQuest() {
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
        return new LocalMessage("quests", "swhfindspiretitle");
    }

    @Override
    public GameMessage getDescription() {
        return new LocalMessage("quests", "swhfindspiredesc");
    }

    @Override
    public DrawOptionsBox getProgressDrawBox(NetworkClient client, final int x, final int y, final int width,
            Color textColor, boolean outlined) {
        final DrawOptionsList drawOptions = new DrawOptionsList();
        FontOptions fo = new FontOptions(16).outline(outlined);
        if (textColor != null) {
            fo.color(textColor);
        }
        drawOptions.add(new StringDrawOptions(fo, Localization.translate("quests", "swhfindspireobj")).pos(x, y));
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
