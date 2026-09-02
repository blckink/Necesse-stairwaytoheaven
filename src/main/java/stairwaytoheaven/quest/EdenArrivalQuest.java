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
 * HUD quest for the first step of the Eden chain: find the Knowledge Tree.
 *
 * Pure signpost, exactly like {@link FindSpireQuest} — no tracked state.
 * Handed out on a player's first step through the Eden Gate
 * ({@code EdenGateObjectEntity.use}); completed by Eveleen's first dialogue,
 * who stands beside a Knowledge Tree ({@code EdenLevel.placeResident}), so
 * finding her IS finding the tree.
 */
public class EdenArrivalQuest extends Quest {

    public EdenArrivalQuest() {
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
        return new LocalMessage("quests", "swhedenreachtitle");
    }

    @Override
    public GameMessage getDescription() {
        return new LocalMessage("quests", "swhedenreachdesc");
    }

    @Override
    public DrawOptionsBox getProgressDrawBox(NetworkClient client, final int x, final int y, final int width,
            Color textColor, boolean outlined) {
        final DrawOptionsList drawOptions = new DrawOptionsList();
        FontOptions fo = new FontOptions(16).outline(outlined);
        if (textColor != null) {
            fo.color(textColor);
        }
        drawOptions.add(new StringDrawOptions(fo, Localization.translate("quests", "swhedenreachobj")).pos(x, y));
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
                Localization.translate("quests", "swhspeaktoeveleen"));
    }
}
