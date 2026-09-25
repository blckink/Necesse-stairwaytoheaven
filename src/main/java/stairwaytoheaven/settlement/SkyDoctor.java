package stairwaytoheaven.settlement;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.SettlerDialogueRegistry;
import necesse.engine.registries.SettlerRegistry;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.container.settlement.events.SettlementSettlersChangedEvent;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.NetworkSettlementData;
import necesse.level.maps.levelData.settlementData.SavedSettlerSettings;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.settler.Settler;

/**
 * The Doctor — the mod's second settler profession, and the proof that a third
 * is cheap.
 *
 * <h2>What it costs to add one now</h2>
 *
 * The Therapist needed seven classes because it invented the shape: what a
 * profession is, how a service hangs off the settler talk menu, how a dialogue
 * carries state across the network. The Doctor reuses all of that and needs
 * <b>four</b>:
 *
 * <ul>
 * <li>{@link DoctorSettler} — about twenty lines on {@link ProfessionSettler},
 *     which holds the recruit ticket, the COMPLETE_HOST opt-out, the icon and
 *     the clothes;</li>
 * <li>{@code mobs.DoctorHumanMob} — the shop, i.e. the profession's actual
 *     identity;</li>
 * <li>{@link DoctorHealDialogue} — its one service;</li>
 * <li>this class, plus one line in {@code StairwayToHeavenMod.init} and the
 *     locale rows in both languages.</li>
 * </ul>
 *
 * <p>No packet and no container event this time: the Therapist needed one
 * because a trait swap's outcome is invisible until something says it out loud.
 * Being healed is a health bar filling up.
 *
 * <h2>What a Doctor does</h2>
 *
 * <ul>
 * <li><b>Patches you up on the spot</b> for {@link #HEAL_PRICE} coins — see
 *     {@link DoctorHealDialogue}.</li>
 * <li><b>Gets fallen settlers back up.</b> With settler death off, a settler
 *     at 0 health is downed and drops out of the settlement; the Doctor runs
 *     over and revives them for free ({@code mobs.DoctorReviveAINode},
 *     {@link #revive}).</li>
 * <li><b>Heals in a fight</b> — every {@code DoctorHumanMob.HEAL_INTERVAL}
 *     ticks, the most hurt ally within range who is in combat.</li>
 * <li><b>Sells the good consumables</b> — six Greater-tier potions and five
 *     gourmet dishes, all vanilla, all craftable by the player anyway, priced
 *     off each item's own registered broker value by vanilla's own Alchemist
 *     rule. The table and the arithmetic are in {@code mobs.DoctorHumanMob}.</li>
 * </ul>
 */
public final class SkyDoctor {

    private SkyDoctor() {
    }

    /** Mob string ID. Vanilla's own professions read {@code <job>human}. */
    public static final String MOB_ID = "doctorhuman";

    /** Settler string ID. Vanilla's own professions read {@code <job>}. */
    public static final String SETTLER_ID = "doctor";

    /**
     * Coins for a full heal.
     *
     * <p>Deliberately small. A Superior Health Potion off this same Doctor's
     * shelf costs 20-60 and does not heal you fully, so the visit is worth
     * making — but you have to walk home to a settlement that houses a Doctor
     * to get it, which is the whole trade.
     */
    public static final int HEAL_PRICE = 100;

    /**
     * Whether a downed settler was one of the Doctor's own people.
     *
     * <p>Keyed on {@code savedSettlerSettings.settlementUniqueID}, which
     * vanilla writes from the settler's {@code LevelSettler} just before it
     * removes the fallen one from the settlement. If that record is missing
     * (vanilla only writes it when a {@code LevelSettler} existed), a downed
     * settler lying inside the Doctor's own settlement counts too — only
     * settlers are ever downed, so that is somebody's resident, and here it
     * can only be ours.
     */
    public static boolean belongsTo(HumanMob downed, HumanMob doctor) {
        int ours = doctor.getSettlementUniqueID();
        if (ours == 0) {
            return false;
        }
        SavedSettlerSettings saved = downed.savedSettlerSettings;
        if (saved != null) {
            return saved.settlementUniqueID == ours;
        }
        NetworkSettlementData network = doctor.getSettlerSettlementNetworkData();
        return network != null && network.isTileWithinBounds(downed.getTileX(), downed.getTileY());
    }

    /**
     * Puts a downed settler back into the Doctor's settlement — vanilla's own
     * recruit path ({@code PacketShopContainerUpdate.recruitSettler}, 1.3.3)
     * with the Revival Potion taken out, in the same order and with the same
     * checks: settler cap, {@code canMoveIn}, {@code moveIn} (which clears the
     * downed state through {@code HumanSettlerMob.makeSettler}),
     * {@code onRecruited} (restores the saved settings, a quarter of max
     * health and five seconds of spawn invincibility), then the
     * settlers-changed event and a chat line to the settlement's team.
     *
     * @return false if the settlement is gone, full or refuses the settler
     */
    public static boolean revive(HumanMob doctor, HumanMob downed) {
        if (!doctor.isServer() || !downed.isDowned()) {
            return false;
        }
        ServerSettlementData data = doctor.getSettlerSettlementServerData();
        Settler settler = downed.getSettler();
        if (data == null || settler == null || doctor.getLevel() == null) {
            return false;
        }
        int max = doctor.getLevel().getServer().world.settings.maxSettlersPerSettlement;
        if (max >= 0 && data.countTotalSettlers() >= max) {
            return false;
        }
        LevelSettler levelSettler = new LevelSettler(data, settler, downed.getUniqueID(), downed.getSettlerSeed());
        if (!data.canMoveIn(levelSettler, -1)) {
            return false;
        }
        data.moveIn(levelSettler);
        downed.onRecruited(null, data, levelSettler);
        data.sendEvent(SettlementSettlersChangedEvent.class);
        LocalMessage message = new LocalMessage("misc", "swhdoctorrevived",
                "doctor", doctor.getLocalization(), "settler", downed.getLocalization());
        data.networkData.streamTeamMembers().forEach(c -> c.sendChatMessage(message));
        System.out.println("doctor revive: " + downed.getStringID() + " " + downed.getUniqueID()
                + " back in settlement " + data.uniqueID);
        return true;
    }

    /**
     * Three registries, one call, from {@code StairwayToHeavenMod.init} —
     * they all close right after it.
     *
     * <p>IDs are written as literals: {@code tools/locale_audit.py} follows
     * literals in the source, and an ID handed over as a constant is an ID the
     * audit cannot name-check.
     */
    public static void register() {
        MobRegistry.registerMob("doctorhuman", stairwaytoheaven.mobs.DoctorHumanMob.class, false);
        SettlerRegistry.registerSettler("doctor", new DoctorSettler());
        SettlerDialogueRegistry.registerSettlerDialogue("swh_doctorheal", DoctorHealDialogue.class);
    }
}
