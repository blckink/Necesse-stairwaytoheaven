package stairwaytoheaven.settlement;

import java.util.function.Supplier;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.HumanLook;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.InventoryItem;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.settler.Settler;

/**
 * The Therapist as a settlement settler type — i.e. as a PROFESSION, not as a
 * character.
 *
 * <h2>Why this is a plain Settler and not a {@code SkySettlers.SkyResident}</h2>
 *
 * Every settler this mod had before was a named individual: Magpie, Halda,
 * Eveleen. {@code SkyResident} is built for exactly that — it claims its name
 * once per world ({@code SkywatchWorldData.residentsClaimed}), refuses the free
 * move-in roll, cannot be banished and never arrives twice. A profession is the
 * opposite of all four: there can be a Therapist in every settlement, a second
 * one can turn up if the first dies, and the player may send them away again.
 * So this mirrors {@code MinerSettler} and {@code BlacksmithSettler} instead,
 * which is what the request asks for — "as common as the Stylist, the Miner,
 * the Angler".
 *
 * <h2>The recruit ticket, read off vanilla</h2>
 *
 * {@code ServerSettlementData} asks every registered settler for tickets when
 * it rolls the next person to walk into town. Vanilla's own weights (VERIFIED
 * against 1.3.3):
 *
 * <ul>
 * <li>Blacksmith 75, Miner 75, ungated;</li>
 * <li>Angler 100, ungated;</li>
 * <li>Stylist 75 but only after {@code defeatreaper}; Trader 100 after
 *     {@code defeatchieftain}; Alchemist 75 after
 *     {@code defeatevilsprotector}.</li>
 * </ul>
 *
 * <p>This takes <b>75, ungated</b> — the Blacksmith's and the Miner's exact
 * terms. A story gate was considered and rejected: the request is that a
 * Therapist is simply mixed in among all the others, and a gate would make them
 * rarer than the two professions named as the benchmark.
 */
public class TherapistSettler extends Settler {

    public TherapistSettler() {
        // Names the MOB, not this settler's own key: Settler resolves the mob
        // it stands up through MobRegistry with this string. Vanilla's own
        // pairing is the same shape ("stylist" the settler, "stylisthuman" the
        // mob).
        super("therapisthuman");
        // Vanilla's COMPLETE_HOST achievement wants one of every settler type
        // in a settlement. A modded settler must stay out of that set or
        // installing this mod makes the achievement unreachable — the same call
        // SkyResident makes, and for the same reason.
        this.isPartOfCompleteHost = false;
    }

    /**
     * The settlement screen's "where do I get this settler" line.
     *
     * <p>Section {@code [settlement]}, the same one vanilla's {@code minertip}
     * and {@code stylisttip} live in.
     */
    @Override
    public GameMessage getAcquireTip() {
        return new LocalMessage("settlement", "therapisttip");
    }

    /**
     * The face on the settlement screen.
     *
     * <p>Borrowed by literal path from vanilla's Alchemist, the closest thing
     * vanilla's roster draws to someone who tends to people rather than to
     * ground. {@code GameTexture.fromFile} reads one flat resource map with the
     * mod's own files merged in, so a vanilla path resolves from mod code
     * exactly as our own icons do. Row in {@code docs/VANILLA_ASSET_MAP.md}.
     */
    @Override
    public void loadTextures() {
        this.texture = ICON.get();
    }

    private static final Supplier<GameTexture> ICON =
            () -> GameTexture.fromFile("mobs/icons/alchemisthuman");

    /**
     * What they wear when the settlement screen draws them, and what
     * {@code HumanMob} falls back to when the mob itself has no equipment.
     * Vanilla clothing items only — no new art, the same trade every settler in
     * this mod makes.
     */
    @Override
    public void setDefaultArmor(HumanDrawOptions drawOptions, int seed, HumanLook look,
                                boolean isSettler) {
        drawOptions.chestplate(new InventoryItem("blazer"));
        drawOptions.boots(new InventoryItem("dressshoes"));
    }

    /**
     * The ticket that makes a Therapist turn up.
     *
     * <p>{@code isRandomEvent} true is vanilla's "a settler arrives out of the
     * blue" pass, and vanilla lets that one bypass the already-have-this-type
     * check; the normal recruit draw does not. Copied verbatim from
     * {@code BlacksmithSettler} so a Therapist arrives on exactly the terms a
     * Blacksmith does.
     */
    @Override
    public void addNewRecruitSettler(ServerSettlementData data, boolean isRandomEvent,
                                     TicketSystemList<Supplier<HumanMob>> ticketSystem) {
        if (isRandomEvent || !this.doesSettlementHaveThisSettler(data)) {
            ticketSystem.addObject(75, this.getNewRecruitMob(data));
        }
    }
}
