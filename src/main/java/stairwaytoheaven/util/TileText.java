package stairwaytoheaven.util;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.packet.PacketUniqueFloatText;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.level.maps.Level;

/**
 * Floating text at a tile — the mod's only way for a THING to say something.
 *
 * <p>The chat log is gone. The player's words were <i>"und keine chat
 * nachrichten! generell.. die sind total kacke lesbar"</i>, and every line this
 * mod used to post into chat now either floats over the thing that said it or
 * sits in a speech bubble over the head of the person who said it
 * ({@code PacketMobChat}, see {@code mobs/SkySettlerMob.bubble}).
 *
 * <p>This is vanilla's own mechanism, not an invention.
 * {@code EggNestObject.interact} (EggNestObject.java:113-114 and :145,
 * VERIFIED [jar]) is the pattern copied here exactly, offsets included:
 *
 * <blockquote><pre>
 * int textX = x * 32 + 16;
 * int textY = y * 32 + 32;
 * serverClient.sendUniqueFloatText(textX, textY, new LocalMessage("ui", "egghatchtip"), "inspect", 6000);
 * </pre></blockquote>
 *
 * <p>{@code "inspect"} is vanilla's own unique type and it is deliberately
 * shared by everything here: {@code UniqueFloatText.init}
 * (UniqueFloatText.java:24-33, VERIFIED [jar]) removes every other floating
 * text with the same type the moment a new one appears, so a player who mashes
 * a locked portal reads one sentence rather than a stack of them.
 *
 * <p><b>Long lines break in the .lang file, not here.</b>
 * {@code FloatTextFade.setText} (FloatTextFade.java:100-101, VERIFIED [jar])
 * splits on {@code \n} and centres each line, and {@code Translation}
 * (Translation.java:181, VERIFIED [jar]) turns a literal {@code \n} in a
 * {@code .lang} entry into a real newline. Nothing here wraps text on its own,
 * so a sentence with no break in it draws as one very wide line.
 */
public final class TileText {

    private TileText() {
    }

    /** Vanilla's own hover time for an inspect line (EggNestObject.java:145). */
    public static final int HOVER_MS = 6000;

    /** Vanilla's own unique type, so a second line replaces the first. */
    public static final String INSPECT = "inspect";

    /** One player is told something by the thing standing at {@code tileX, tileY}. */
    public static void at(ServerClient client, int tileX, int tileY, GameMessage message) {
        if (client == null || message == null) {
            return;
        }
        client.sendUniqueFloatText(tileX * 32 + 16, tileY * 32 + 32, message, INSPECT, HOVER_MS);
    }

    /**
     * Everyone who can see {@code tileX, tileY} is told — the float-text
     * equivalent of the level-wide chat announcement it replaces.
     */
    public static void atAll(Server server, Level level, int tileX, int tileY, GameMessage message) {
        if (server == null || server.network == null || level == null || message == null) {
            return;
        }
        server.network.sendToClientsWithTile(
                new PacketUniqueFloatText(tileX * 32 + 16, tileY * 32 + 32, message, INSPECT, HOVER_MS),
                level, tileX, tileY);
    }
}
