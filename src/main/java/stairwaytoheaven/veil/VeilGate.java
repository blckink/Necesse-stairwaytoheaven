package stairwaytoheaven.veil;

import necesse.engine.GameEventListener;
import necesse.engine.GameEvents;
import necesse.engine.commands.CommandsManager;
import necesse.engine.events.ServerStartEvent;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.WorldDataRegistry;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.staticBuffs.Buff;
import stairwaytoheaven.commands.VeilMarkCommand;

/**
 * The realm gate — one mechanic, one registration call, one line in the mod
 * entry class.
 *
 * <h2>One gate, two configurations</h2>
 *
 * {@code docs/WORLD_DESIGN.md} §42.4 is explicit that the mod has exactly one
 * gating mechanic and describes it twice:
 *
 * <blockquote>Soul Exposure (§8) and the Infernal Visa (§18) are the same
 * thing: a realmDepth debuff switched off by a permanent character unlock.
 * Build <b>one</b> system with two configurations. One implementation, two
 * quests, no second code path.</blockquote>
 *
 * <p>and §43's build order puts it third, "with Soul Exposure as its first
 * configuration". So the pieces are deliberately split along the line where the
 * second configuration will differ, and nowhere else:
 *
 * <ul>
 *   <li>{@link VeilRegion} — <b>where</b>. A realm-depth threshold and the
 *       levels that carry the field. Hell's gate is the same class with a
 *       second threshold.</li>
 *   <li>{@link SoulExposureBuff} — <b>what it costs</b>. §8's four bands. The
 *       Infernal Fringe gets its own buff with its own table; nothing else
 *       moves.</li>
 *   <li>{@link VeilWorldData} — <b>the clock and the unlock ledger</b>. The
 *       per-second region check and the set of characters that may cross. A
 *       second gate is a second set of authentications in the same record and
 *       one more branch in the same tick.</li>
 * </ul>
 *
 * <p>Nothing here is generalised past that. A configuration object with one
 * instance is a framework nobody asked for; the Infernal Visa lands when §18 is
 * built, and it lands as three small additions to these three files rather than
 * as a second implementation.
 *
 * <h2>Registration order</h2>
 *
 * Everything below happens inside {@code StairwayToHeavenMod.init()}, which is
 * where the buff, world-data and command registries are open.
 * {@code BuffRegistry.registerBuff} refuses only client-side-only mods
 * (BuffRegistry.java:972-977); ours is not one.
 */
public final class VeilGate {

    private static Buff exposure;
    private static Buff fog;

    /**
     * Who the Veil is, when it kills someone.
     *
     * <p>Without this the engine substitutes {@code Mob.TOO_BUFFED_ATTACKER}
     * (BuffManager.java:307) and the death message reads "&lt;victim&gt; was
     * too buffed", which is both wrong and funny in the wrong direction. The
     * attacker has no mob behind it — the fog is a place, not a creature — so
     * {@code getFirstAttackOwner} is null and no kill is credited to anyone.
     */
    public static final Attacker ATTACKER = new Attacker() {
        @Override
        public GameMessage getAttackerName() {
            return new LocalMessage("deaths", "swhveilname");
        }

        @Override
        public DeathMessageTable getDeathMessages() {
            return DeathMessageTable.fromRange("swhveil", 3);
        }

        @Override
        public Mob getFirstAttackOwner() {
            return null;
        }
    };

    private VeilGate() {
    }

    /**
     * Registers the whole gate. Called once, from
     * {@code StairwayToHeavenMod.init()}.
     */
    public static void register() {
        exposure = BuffRegistry.registerBuff(SoulExposureBuff.ID, new SoulExposureBuff());
        fog = BuffRegistry.registerBuff(VeilFogBuff.ID, new VeilFogBuff());
        WorldDataRegistry.registerWorldData(VeilWorldData.KEY, VeilWorldData.class);
        CommandsManager.registerServerCommand(new VeilMarkCommand());

        // A WorldData is created LAZILY, on first access -- so without this the
        // Veil's region check would only start ticking once something else
        // happened to ask for the record, and in a world where nobody ever runs
        // /veilmark that is never. The same listener, for the same reason, is
        // what starts surface/SkyfallWorldData. ServerStartEvent fires from
        // Server.markWorldInitialized, i.e. after world.init() has loaded the
        // world entity, so the record either comes back from the save with its
        // Veil Marks intact or is created empty here and saved from then on.
        GameEvents.addListener(ServerStartEvent.class, new GameEventListener<ServerStartEvent>() {
            @Override
            public void onEvent(ServerStartEvent event) {
                VeilWorldData.get(event.server);
            }
        });
    }

    /** §8's debuff. Null before {@link #register()} has run. */
    public static Buff exposure() {
        return exposure;
    }

    /** §8's fog. Null before {@link #register()} has run. */
    public static Buff fog() {
        return fog;
    }
}
