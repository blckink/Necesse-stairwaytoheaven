package stairwaytoheaven.objects;

import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.gameObject.RandomCrateObject;
import necesse.level.maps.Level;

/**
 * The rich tier of sky container: a sealed aeronaut cache, found only at the
 * heart of a wreck site.
 *
 * WHY A SUBCLASS. {@link RandomCrateObject} always asks
 * {@code Level.getCrateLootTable}, which resolves through the biome — so every
 * crate in a biome would hold the same thing and there could be no tiers. The
 * player asked for exactly that distinction ("verschiedene Wertigkeiten"), so
 * this one overrides {@code getLootTable} directly and keeps its own table.
 *
 * It wears vanilla's own {@code arcaniccrates} sheet. That is deliberate and
 * the player invited it: an incursion-tier strongbox already reads as "this is
 * worth more than the wooden one" to anyone who has seen one underground, and
 * reusing it means the tier is legible without a single new pixel.
 *
 * The incursion materials in the table were verified to exist in the game's
 * ItemRegistry before being named here — {@code arcanicbar} and
 * {@code voidcrystal}, which would have been the obvious guesses, do not.
 */
public class SkyCacheObject extends RandomCrateObject {

    public SkyCacheObject() {
        super("arcaniccrates");
    }

    @Override
    public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
        if (level.objectLayer.isPlayerPlaced(tileX, tileY)) {
            return super.getLootTable(level, layerID, tileX, tileY);
        }
        return new LootTable(
                LootItem.between("aetheriumbar", 2, 5),
                ChanceLootItem.between(0.55F, "stormsteelbar", 1, 3),
                ChanceLootItem.between(0.45F, "stormglass", 2, 5),
                ChanceLootItem.between(0.30F, "skyweave", 1, 3),
                // Incursion-tier salvage: what a sky freighter was carrying
                // that the player cannot yet make.
                ChanceLootItem.between(0.22F, "crystalessence", 1, 2),
                ChanceLootItem.between(0.10F, "ascendedshard", 1, 1),
                ChanceLootItem.between(0.03F, "eyeofthevoid", 1, 1)
        );
    }
}
