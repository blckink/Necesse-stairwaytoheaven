package stairwaytoheaven.settlement;

import necesse.engine.localization.Localization;
import necesse.engine.network.client.Client;
import necesse.gfx.GameBackground;
import necesse.gfx.GameColor;
import necesse.gfx.gameTooltips.BackgroundedGameTooltips;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.SpacerGameTooltip;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.notifications.SettlementNotification;
import necesse.level.maps.levelData.settlementData.notifications.SettlementNotificationManager;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;
import stairwaytoheaven.mobs.BloodFeverBuff;

/**
 * "Dorian hat heute Nacht jemanden gebissen" — decision E2 of
 * {@code docs/design/concept-nightbound-dorian.md}: a settlement notification,
 * in the same place as vanilla's "no bed" and "inventory full", not a chat
 * line (the player's rule: no chat messages, see {@code SkySettlerMob}).
 *
 * <p>The concept marked this a HYPOTHESIS until the engine was checked. It is
 * supported, VERIFIED [jar] in the decompiled 1.3.2 source:
 * {@code SettlementNotificationRegistry.registerNotification} is public and
 * refuses only client-side-only mods; a notification is one subclass of
 * {@link SettlementNotification} (vanilla's own {@code
 * NoBedSettlementNotification} is 30 lines); the icon is the severity's, so no
 * art is needed; and {@code SettlementNotificationManager.submitNotification(
 * id, settlerMob, severity)} files one per settler and drops it by itself as
 * soon as {@link #isStillValid(SettlerMob)} says no — here, the moment the
 * blood fever is cured or wears off. The tooltip lists the bitten by name
 * ({@code addSettlerSubmissionsTooltips}).
 */
public class BloodFeverNotification extends SettlementNotification {

    public static final String ID = "swhbloodfever";

    public BloodFeverNotification() {
    }

    @Override
    public GameTooltips getTooltip(SettlementNotificationManager.ActiveNotification notification) {
        ListGameTooltips tooltips = new ListGameTooltips();
        tooltips.add(Localization.translate("ui", "swhnotificationbloodfever"), 400);
        tooltips.add(new StringTooltips(Localization.translate("ui", "swhnotificationbloodfeverhelp"),
                GameColor.GRAY, 400));
        tooltips.add(new SpacerGameTooltip(10));
        this.addSettlerSubmissionsTooltips(tooltips, notification);
        return new BackgroundedGameTooltips(tooltips, GameBackground.getItemTooltipBackground());
    }

    @Override
    public void onClicked(Client client, SettlementNotificationManager.ActiveNotification data) {
        // Nothing to open: the tooltip already says who and what helps.
    }

    @Override
    public boolean isStillValid(SettlerMob settlerMob) {
        return settlerMob != null && settlerMob.getMob() != null
                && settlerMob.getMob().buffManager.hasBuff(BloodFeverBuff.ID);
    }

    @Override
    public boolean isStillValid(ServerSettlementData data) {
        return false;
    }
}
