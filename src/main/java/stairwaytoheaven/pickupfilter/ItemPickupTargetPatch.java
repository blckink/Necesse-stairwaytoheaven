package stairwaytoheaven.pickupfilter;

import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.server.ServerClient;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.entity.pickup.QuestItemPickupEntity;
import net.bytebuddy.asm.Advice;

/**
 * Keeps filtered items from flying to the player.
 *
 * <p>{@code ItemPickupEntity.isValidTarget} is the only gate
 * {@code PickupEntity.serverTick} asks before it targets a player, so saying
 * no here leaves the item lying on the ground for others (and for settlers).
 * The player's own death drops and quest items are never filtered.
 */
@ModMethodPatch(target = ItemPickupEntity.class, name = "isValidTarget", arguments = {ServerClient.class})
public class ItemPickupTargetPatch {

    @Advice.OnMethodExit
    static void onExit(@Advice.This ItemPickupEntity self, @Advice.Argument(0) ServerClient client,
                       @Advice.Return(readOnly = false) boolean valid) {
        if (valid && self.playerDeathAuth == 0L && !(self instanceof QuestItemPickupEntity)
                && !PickupFilter.allows(client, self.item)) {
            valid = false;
        }
    }
}
