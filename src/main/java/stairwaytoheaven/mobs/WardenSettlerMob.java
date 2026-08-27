package stairwaytoheaven.mobs;

import java.util.Collections;
import java.util.List;

import necesse.engine.network.server.ServerClient;
import necesse.inventory.InventoryItem;

/**
 * The Warden's settled form — kept ONLY for save compatibility.
 *
 * Worlds built with v0.5.0/v0.5.1 have one of these standing in or near the
 * player's settlement. Those builds could not use vanilla's recruitment (see
 * {@link SkyWardenMob} for why), so they took the 100,000 coins in the sky and
 * hand-spawned this second mob at home, where it then had to be recruited a
 * second time. Deleting the class would make those saves fail to load the mob.
 *
 * So it stays, as a thin subclass of the real Warden: same face, same clothes,
 * same dialogue, same settler type, same display name — the player cannot tell
 * the two apart, which is the point. The one difference is the price: this
 * world already paid, so moving in is free.
 *
 * Newly generated worlds never create one. There, the single {@code skywarden}
 * mob at the spire is the mob that moves into the settlement.
 */
public class WardenSettlerMob extends SkyWardenMob {

    /**
     * Free. An EMPTY list is vanilla's idiom for a free recruit (the Trader
     * uses it after being freed from a trap): it makes the recruit button live
     * and reads "recruit for free". The inherited {@code null} would leave the
     * button permanently dead and strand him outside the settlement forever —
     * which is precisely what a playtester hit.
     */
    @Override
    public List<InventoryItem> getRecruitItems(ServerClient client) {
        return Collections.emptyList();
    }

}
