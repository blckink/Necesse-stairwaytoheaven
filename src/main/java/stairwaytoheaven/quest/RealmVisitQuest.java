package stairwaytoheaven.quest;

import java.awt.Color;
import java.awt.Rectangle;

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
 * "Set foot in that realm, then come back and tell me" — the arrival step of
 * a realm, turned in to the resident who sent the player there.
 *
 * <h2>Why the two arrival quests became this</h2>
 * {@code swh_edenreach} and {@code swh_crookedarrival} were signposts: "find
 * whoever lives out there", completed the moment the player met Eveleen beside
 * her Knowledge Tree or Mr. Knott at his Door Yard. Since the Spire Village
 * (2026-09-24) nobody lives out there any more — they live around the spire —
 * so "find her" would be finished before it began. The step keeps its ID and
 * its place at the start of each realm's chapter, and asks for the thing the
 * village cannot give: to have actually been there.
 *
 * <p>It also mends {@code swh_crookedarrival}, which was unreachable: only the
 * Crooked Door ever handed it out, and nothing in the world places that door
 * ({@code KOMPLETTUEBERSICHT.md} §8.1 no. 1). Mr. Knott hands it out now.
 *
 * <h2>How the visit is noticed</h2>
 * {@code ServerClient} ticks every quest it holds, every server tick
 * (ServerClient.java:811, VERIFIED [jar]); {@link #tick} asks the sky level
 * which realm the player's tile belongs to — the same
 * {@code SkyLevel.realmAt} every realm rule in the mod reads — and flips
 * {@link #reached} once. The flag is saved, and synced to the journal through
 * the quest's own packet so the objective line can show it done.
 */
public abstract class RealmVisitQuest extends Quest {

    /** Whether the player has stood in the realm since taking the quest. */
    public boolean reached;

    protected abstract int realm();

    /** {@code quests.<prefix>title/desc/obj}. */
    protected abstract String keyPrefix();

    /** {@code quests.<key>}: "Speak with <giver>." */
    protected abstract String handInKey();

    @Override
    public void tick(ServerClient client) {
        if (this.reached || client == null || client.playerMob == null) {
            return;
        }
        necesse.level.maps.Level level = client.playerMob.getLevel();
        if (level instanceof stairwaytoheaven.level.SkyLevel
                && ((stairwaytoheaven.level.SkyLevel) level).realmAt(
                        client.playerMob.getTileX(), client.playerMob.getTileY()) == this.realm()) {
            this.reached = true;
            this.markDirty();
        }
    }

    @Override
    public boolean canComplete(NetworkClient client) {
        return this.reached;
    }

    @Override
    public void addSaveData(SaveData save) {
        super.addSaveData(save);
        save.addBoolean("reached", this.reached);
    }

    @Override
    public void applyLoadData(LoadData save) {
        super.applyLoadData(save);
        this.reached = save.getBoolean("reached", false, false);
    }

    @Override
    public void setupSpawnPacket(PacketWriter writer) {
        super.setupSpawnPacket(writer);
        writer.putNextBoolean(this.reached);
    }

    @Override
    public void applySpawnPacket(PacketReader reader) {
        super.applySpawnPacket(reader);
        this.reached = reader.getNextBoolean();
    }

    @Override
    public void setupPacket(PacketWriter writer) {
        super.setupPacket(writer);
        writer.putNextBoolean(this.reached);
    }

    @Override
    public void applyPacket(PacketReader reader) {
        super.applyPacket(reader);
        this.reached = reader.getNextBoolean();
    }

    @Override
    public GameMessage getTitle() {
        return new LocalMessage("quests", this.keyPrefix() + "title");
    }

    @Override
    public GameMessage getDescription() {
        return new LocalMessage("quests", this.keyPrefix() + "desc");
    }

    @Override
    public DrawOptionsBox getProgressDrawBox(NetworkClient client, final int x, final int y, final int width,
            Color textColor, boolean outlined) {
        final DrawOptionsList drawOptions = new DrawOptionsList();
        FontOptions fo = new FontOptions(16).outline(outlined);
        if (textColor != null) {
            fo.color(textColor);
        }
        String line = Localization.translate("quests", this.keyPrefix() + "obj");
        if (this.reached) {
            line = line + " " + Localization.translate("quests", "swhladderreached");
        }
        drawOptions.add(new StringDrawOptions(fo, line).pos(x, y));
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
        String line = Localization.translate("quests", this.keyPrefix() + "reward", false);
        if (line == null || line.isEmpty() || line.endsWith(this.keyPrefix() + "reward")) {
            return null;
        }
        return new FairType().append(new FontOptions(12).outline(outlined), line);
    }

    @Override
    public FairType getHandInType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", this.handInKey()));
    }
}
