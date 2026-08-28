package stairwaytoheaven.surface;

import java.util.LinkedHashSet;
import java.util.Set;

import necesse.engine.GameEventListener;
import necesse.engine.GameEvents;
import necesse.engine.commands.CommandsManager;
import necesse.engine.events.ServerStartEvent;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.WorldDataRegistry;
import necesse.engine.registries.WorldEventRegistry;
import necesse.engine.registries.WorldPresetRegistry;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;

/**
 * The Surface half of the mod: three rare points of interest scattered across
 * vanilla's own surface, and one recurring night event above it.
 *
 * <p>This is the ONLY registration entry point for everything under
 * {@code stairwaytoheaven.surface}. {@code StairwayToHeavenMod.init()} calls
 * {@link #register()} and then {@link #registerItems()}, mirroring the way the
 * rest of the mod separates "register the things" from "everything that has to
 * come after the item registry".
 *
 * <h2>The design line this content stays behind</h2>
 * {@code docs/DESIGN_DECISIONS.md}: <i>the Surface stays the player's main
 * world.</i> Nothing here changes vanilla surface generation, replaces a
 * vanilla structure, or puts anything on the critical path — the POIs are three
 * more entries in the same weighted list vanilla fills with hunter cabins and
 * abandoned camps, and the world event is weather with loot in it.
 */
public final class SkySurface {

    /** The Fallen Skyshard a {@link SkyfallWorldEvent} scatters. */
    public static int skyfallShardID;

    /** The registered POI world preset, held so the probe can read its counters. */
    public static SkySurfacePresets poiPresets;

    /** {@code WorldEventRegistry} ID of the Skyfall, or −1 if it did not register. */
    public static int skyfallEventID = -1;

    /**
     * Item IDs that any of this package's loot tables names but that no item
     * registry entry backs. Filled by {@link #registerItems()}; must stay
     * empty. A loot table that names a missing item is not an error the player
     * ever sees as an error — {@code LootItem.getItem} prints to stderr and
     * hands back null, so the chest is simply emptier than it was meant to be.
     */
    public static final Set<String> UNRESOLVED_LOOT = new LinkedHashSet<>();

    /** Every item ID this package's loot tables name, resolved or not. */
    public static final Set<String> LOOT_ITEMS = new LinkedHashSet<>();

    private SkySurface() {
    }

    /**
     * Registry phase. Runs inside {@code ModEntry.init()}, while every registry
     * this touches is still open:
     * <ul>
     *   <li>{@code ObjectRegistry} — the shard the world event scatters.</li>
     *   <li>{@code WorldPresetRegistry} — the POI list. Its
     *       {@code onRegistryClosed} calls {@code addCorePresets}, which builds
     *       the three {@link necesse.level.maps.presets.Preset}s; that happens
     *       AFTER this method returns, so every object and tile ID they name is
     *       already resolved by then.</li>
     *   <li>{@code WorldEventRegistry} — the Skyfall. Registration is by CLASS:
     *       the registry instantiates it reflectively for the spawn packet and
     *       for the save, which is why the event needs a public no-argument
     *       constructor. It also refuses client-side-only mods outright
     *       ({@code WorldEventRegistry.registerEvent}); this mod is
     *       {@code clientside: false}.</li>
     *   <li>{@code WorldDataRegistry} — the schedule behind the event.</li>
     * </ul>
     * Plus the debug command ({@code CommandsManager} is opened before
     * {@code mod.init()} and closed after {@code postInit}) and a
     * {@link ServerStartEvent} listener, which is what guarantees the schedule
     * exists — and therefore ticks — on every world, including one that has
     * never had a Skyfall.
     */
    public static void register() {
        // The string ID is written out as a literal on purpose:
        // tools/locale_audit.py can only name-check an ID it can see at the
        // registration call site, and it fails loudly (rather than skipping)
        // when a registerObject argument is a variable. SkyfallShardObject
        // .STRING_ID holds the same value for code that needs to name it.
        skyfallShardID = ObjectRegistry.registerObject("skyfallshard",
                new SkyfallShardObject(), 0.0F, false);

        poiPresets = WorldPresetRegistry.registerPreset(SkySurfacePresets.STRING_ID, new SkySurfacePresets());

        skyfallEventID = WorldEventRegistry.registerEvent(SkyfallWorldEvent.STRING_ID, SkyfallWorldEvent.class);
        WorldDataRegistry.registerWorldData(SkyfallWorldData.KEY, SkyfallWorldData.class);

        CommandsManager.registerServerCommand(new SkySurfaceStatusCommand());

        // WorldData is created lazily on first access, so without this the
        // schedule would only start ticking once something else asked for it.
        // ServerStartEvent fires from Server.markWorldInitialized, i.e. after
        // world.init() has loaded the world entity, so the record either comes
        // back from the save or is created here and saved from then on.
        GameEvents.addListener(ServerStartEvent.class, new GameEventListener<ServerStartEvent>() {
            @Override
            public void onEvent(ServerStartEvent event) {
                SkyfallWorldData.get(event.server);
            }
        });
    }

    /**
     * Item phase. Runs after {@code SkyItems.register()}, and checks that every
     * item ID this package's loot tables name actually exists.
     *
     * <p>This is a real gate, not bookkeeping. {@code LootItem} resolves its
     * item lazily, in {@code getItem(random)} at drop time — so a typo in a
     * loot table survives the build, survives the integration test's log scan,
     * and shows up as a chest that is quietly missing an item, months later, in
     * somebody's save. Reading the IDs back out of the tables (rather than off
     * a hand-kept list) is what keeps this honest when a table is edited.
     */
    public static void registerItems() {
        LOOT_ITEMS.clear();
        UNRESOLVED_LOOT.clear();
        collect(SkyFragmentCraterPreset.CRATER_LOOT, LOOT_ITEMS);
        collect(AeronautCampPreset.CAMP_LOOT, LOOT_ITEMS);
        collect(SkyfallShardObject.SHARD_LOOT, LOOT_ITEMS);
        for (String itemID : LOOT_ITEMS) {
            if (!ItemRegistry.itemExists(itemID)) {
                UNRESOLVED_LOOT.add(itemID);
                System.err.println("[stairwaytoheaven] surface loot table names unknown item \"" + itemID + "\"");
            }
        }
    }

    /** Every item string ID a loot table names, including nested tables. */
    private static void collect(LootTable table, Set<String> out) {
        for (LootItemInterface entry : table.items) {
            if (entry instanceof LootItem) {
                out.add(((LootItem) entry).itemStringID);
            } else if (entry instanceof LootTable) {
                collect((LootTable) entry, out);
            }
        }
    }
}
