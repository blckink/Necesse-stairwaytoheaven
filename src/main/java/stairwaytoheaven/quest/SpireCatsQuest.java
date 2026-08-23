package stairwaytoheaven.quest;

import java.awt.Color;
import java.awt.Rectangle;

import necesse.engine.Settings;
import necesse.engine.achievements.Achievement;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.engine.quest.Quest;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.gfx.drawOptions.DrawOptionsBox;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawOptions.StringDrawOptions;
import necesse.gfx.fairType.FairType;
import necesse.gfx.gameFont.FontOptions;

/**
 * HUD quest for the cats: coax both spire cats home with cloud puff treats.
 * The two booleans mirror SkywatchQuestData (world truth) and are pushed into
 * the live instance by mod code + markDirty() when a cat travels home.
 */
public class SpireCatsQuest extends Quest {

    public boolean blackHome;
    public boolean tabbyHome;

    public SpireCatsQuest() {
    }

    @Override
    public void tick(ServerClient client) {
    }

    @Override
    public boolean canComplete(NetworkClient client) {
        return this.blackHome && this.tabbyHome;
    }

    @Override
    public void setupPacket(PacketWriter writer) {
        super.setupPacket(writer);
        writer.putNextBoolean(this.blackHome);
        writer.putNextBoolean(this.tabbyHome);
    }

    @Override
    public void applyPacket(PacketReader reader) {
        super.applyPacket(reader);
        this.blackHome = reader.getNextBoolean();
        this.tabbyHome = reader.getNextBoolean();
    }

    @Override
    public void addSaveData(SaveData save) {
        super.addSaveData(save);
        save.addBoolean("blackHome", this.blackHome);
        save.addBoolean("tabbyHome", this.tabbyHome);
    }

    @Override
    public void applyLoadData(LoadData save) {
        super.applyLoadData(save);
        this.blackHome = save.getBoolean("blackHome", false, false);
        this.tabbyHome = save.getBoolean("tabbyHome", false, false);
    }

    @Override
    public GameMessage getTitle() {
        return new LocalMessage("quests", "swhcatstitle");
    }

    @Override
    public GameMessage getDescription() {
        return new LocalMessage("quests", "swhcatsdesc");
    }

    @Override
    public DrawOptionsBox getProgressDrawBox(NetworkClient client, final int x, final int y, final int width,
            Color textColor, boolean outlined) {
        final DrawOptionsList drawOptions = new DrawOptionsList();
        int currentHeight = 0;
        currentHeight += addCatLine(drawOptions, x, y + currentHeight, "swhcatblack", this.blackHome, textColor, outlined);
        currentHeight += addCatLine(drawOptions, x, y + currentHeight, "swhcattabby", this.tabbyHome, textColor, outlined);
        int home = (this.blackHome ? 1 : 0) + (this.tabbyHome ? 1 : 0);
        float progress = home / 2.0F;
        Color barColor = progress == 1.0F ? Settings.UI.successTextColor : Settings.UI.errorTextColor;
        DrawOptionsBox progressBox = Achievement.getProgressbarTextDrawBox(
                x, y + currentHeight, width, 5, progress,
                Settings.UI.progressBarOutline, Settings.UI.progressBarFill,
                home + "/2", new FontOptions(16).outline(outlined).color(barColor));
        drawOptions.add(progressBox);
        final int finalHeight = currentHeight + progressBox.getBoundingBox().height;
        return new DrawOptionsBox() {
            @Override
            public Rectangle getBoundingBox() {
                return new Rectangle(x, y, width, finalHeight);
            }

            @Override
            public void draw() {
                drawOptions.draw();
            }
        };
    }

    private static int addCatLine(DrawOptionsList drawOptions, int x, int y, String key, boolean home,
            Color textColor, boolean outlined) {
        FontOptions fo = new FontOptions(16).outline(outlined);
        if (home) {
            fo.color(Settings.UI.successTextColor);
        } else if (textColor != null) {
            fo.color(textColor);
        }
        drawOptions.add(new StringDrawOptions(fo, Localization.translate("quests", key)).pos(x, y));
        return 16;
    }

    @Override
    public FairType getRewardType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhcatsreward"));
    }

    @Override
    public FairType getHandInType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhreturnwarden"));
    }
}
