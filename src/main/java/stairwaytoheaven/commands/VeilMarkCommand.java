package stairwaytoheaven.commands;

import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.commands.parameterHandlers.BoolParameterHandler;
import necesse.engine.commands.parameterHandlers.ServerClientParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.level.maps.Level;
import stairwaytoheaven.veil.SoulExposureBuff;
import stairwaytoheaven.veil.VeilGate;
import stairwaytoheaven.veil.VeilRegion;
import stairwaytoheaven.veil.VeilWorldData;
import stairwaytoheaven.worldgen.RealmDepth;

/**
 * {@code /veilmark [player] [1/0]} — the Veil Mark, and where the fog is.
 *
 * <h2>Why this exists</h2>
 *
 * {@code docs/WORLD_DESIGN.md} §9 gives the Mark out at the end of the séance
 * questline: Madame Orla, the Séance Table, the Ferryman, five quest
 * ingredients. <b>None of that is built.</b> Until it is, this command is the
 * only thing in the world that writes the unlock, which means the gate can be
 * played from both sides today — walk out to the wall and be turned back, take
 * the Mark, walk through — instead of being a feature nobody can finish.
 *
 * <p>When §9 lands, the Ferryman calls {@code VeilWorldData.grantMark} and this
 * command stays exactly as it is: an admin tool, on the same footing as
 * {@code /skyreachstatus} and {@code /veilstatus}.
 *
 * <p>With no arguments it reports rather than changes: where the fog starts, how
 * deep into the realm field you are standing, and whether you may cross. That
 * readout is the answer to "why is nothing happening" for anyone testing the
 * gate, because the honest answer is almost always "you are 3000 tiles short".
 */
public class VeilMarkCommand extends ModularChatCommand {

    public VeilMarkCommand() {
        super("veilmark",
                "Grants, revokes or reports the Veil Mark (debug)",
                PermissionLevel.ADMIN,
                true,
                new CmdParameter("player", new ServerClientParameterHandler(true, false), true),
                new CmdParameter("1/0", new BoolParameterHandler(null), true));
    }

    @Override
    public void runModular(Client client, Server server, ServerClient serverClient,
                           Object[] args, String[] errors, CommandLog logs) {
        VeilWorldData data = VeilWorldData.get(server);
        if (data == null) {
            logs.add("FAIL: no world to record the Veil Mark in");
            return;
        }

        ServerClient target = (ServerClient) args[0];
        if (target == null) {
            target = serverClient;
        }
        Boolean value = (Boolean) args[1];

        if (target == null) {
            // Console with no player named: still worth answering the half of
            // the question that is about the world rather than about a person.
            logs.add(worldLine(data));
            logs.add("Specify <player> to grant or revoke.");
            return;
        }

        if (value != null) {
            boolean changed = value ? data.grantMark(target.authentication)
                                    : data.revokeMark(target.authentication);
            logs.add((value ? "Veil Mark granted to " : "Veil Mark revoked from ")
                    + target.getName() + (changed ? "" : " (no change)"));
        }

        logs.add(worldLine(data));
        logs.add(playerLine(data, target));
    }

    private static String worldLine(VeilWorldData data) {
        return String.format(
                "Veil gate: depth>=%.3f of scale %d = %.0f tiles from the spire; marks held=%d",
                VeilRegion.VEIL_DEPTH,
                (int) RealmDepth.DEPTH_SCALE,
                VeilRegion.wallDistanceTiles(),
                data.markCount());
    }

    private static String playerLine(VeilWorldData data, ServerClient target) {
        boolean marked = data.hasMark(target.authentication);
        PlayerMob player = target.playerMob;
        if (player == null) {
            return target.getName() + ": mark=" + marked + " (not spawned)";
        }
        Level level = player.getLevel();
        float depth = VeilRegion.depthAt(level, player.getTileX(), player.getTileY());
        if (depth < 0.0F) {
            return String.format("%s: mark=%s level=%s carries no realm field, so no fog here",
                    target.getName(), marked,
                    level == null ? "null" : level.getIdentifier().stringID);
        }
        boolean inside = depth >= VeilRegion.VEIL_DEPTH;
        ActiveBuff exposure = VeilGate.exposure() == null
                ? null : player.buffManager.getBuff(VeilGate.exposure());
        int seconds = exposure == null ? 0 : exposure.getStacks();
        return String.format("%s: mark=%s depth=%.3f realm=%s inFog=%s exposure=%ds band=%s",
                target.getName(), marked, depth,
                RealmDepth.keyOf(RealmDepth.realmForDepth(
                        VeilRegion.seedOf(level), player.getTileX(), player.getTileY(), depth)),
                inside, seconds,
                seconds == 0 ? "none" : SoulExposureBuff.bandKey(seconds));
    }
}
