package stairwaytoheaven.quest;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.quest.DeliverItemsQuest;
import necesse.gfx.fairType.FairType;
import necesse.gfx.gameFont.FontOptions;

/**
 * HUD quest for the anchor finale: deliver aetherium, skystone and the mod's
 * own rare bar so the Warden can anchor the island.
 *
 * <h2>Endgame rescale</h2>
 * Shipped at 5 aetheriumbar + 20 skystone (CHANGELOG "Anchor of the Sky"),
 * from before the mod became endgame-only content. docs/BALANCE.md exists
 * precisely because that framing changed and this ask did not follow it --
 * the player's own verdict on the finale as shipped was "das sind keine
 * endgame belohnungen tbh", and he was right. See each field for what its
 * number is benchmarked against.
 */
public class AnchorDeliveryQuest extends DeliverItemsQuest {

    /**
     * VERIFIED [jar] benchmark: docs/BALANCE.md §1's derived tier table --
     * summing {@code BiomeMissionIncursionData}'s cumulative per-tier arrays
     * puts incursion tier 10 at enemy HP x4.00 over tier 1, the steepest
     * multiplier the mod's own difficulty curve defines. The finale's
     * original tier-1-era ask is scaled by that same x4 (5 -> 20): the
     * chain's capstone toll grows by the largest number the mod's own
     * arithmetic already uses elsewhere, rather than by one picked fresh.
     */
    public static final int BARS = 20;

    /** Same x4 benchmark as {@link #BARS} (docs/BALANCE.md §1, tier-10 HP x4.00): 20 -> 80. */
    public static final int STONE = 80;

    /**
     * New ingredient. Aetheriumbar above is the mod's mid UNCOMMON bar; this
     * is the actual endgame one -- RARE, made only at the Aether Forge from
     * aetheriumore + stormshard ({@code SkyProfessions.registerItems}' 58.0F
     * broker-value comment spells out why). Set to
     * {@code StormsteelArmor.Helmet}'s own recipe cost
     * ({@code SkyItems.registerGearRecipes}: {@code stormsteelbar, 8}) -- the
     * finale asks for as much of the mod's rarest bar as crafting one full
     * endgame armour piece would, so paying this ask means the player is
     * already at the wealth level Stormsteel gear itself demands.
     */
    public static final int STORMSTEEL = 8;

    public AnchorDeliveryQuest() {
        super(new ItemObjective("aetheriumbar", BARS), new ItemObjective("skystone", STONE),
                new ItemObjective("stormsteelbar", STORMSTEEL));
    }

    @Override
    public GameMessage getTitle() {
        return new LocalMessage("quests", "swhanchortitle");
    }

    @Override
    public GameMessage getDescription() {
        return new LocalMessage("quests", "swhanchordesc");
    }

    @Override
    public FairType getRewardType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhanchorreward"));
    }

    @Override
    public FairType getHandInType(NetworkClient client, boolean outlined) {
        return new FairType().append(new FontOptions(12).outline(outlined),
                Localization.translate("quests", "swhreturnwarden"));
    }
}
