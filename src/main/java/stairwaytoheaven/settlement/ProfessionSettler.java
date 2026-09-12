package stairwaytoheaven.settlement;

import java.util.function.Supplier;

import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.HumanLook;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.InventoryItem;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.settler.Settler;

/**
 * The shared half of a settler PROFESSION — everything two of them do the same.
 *
 * <h2>Why this exists</h2>
 *
 * The Therapist was the mod's first profession in vanilla's own sense, as
 * opposed to the nine named residents on {@code SkySettlers.SkyResident}. The
 * Doctor is the second, and the two settler types turned out to differ in
 * exactly four things: which mob they stand up, which face the settlement
 * screen falls back to, what they wear, and their acquire tip. Everything else
 * — the recruit ticket, staying out of the COMPLETE_HOST achievement, the
 * clothes going on through {@code setDefaultArmor} — was the same code twice.
 *
 * <p>So a third profession is now one subclass of this (about twenty lines), one
 * {@code HumanShop} for the shop and whatever services it offers, one line in
 * a {@code register()}, and its locale rows. That is the answer to "is it
 * easier now": the settler half is, the shop and the dialogues are the part
 * that is genuinely new each time and are deliberately NOT shared.
 *
 * <h2>What is not shared, and why</h2>
 *
 * <ul>
 * <li>{@link #getAcquireTip()} stays abstract so every subclass writes its own
 *     key as a LITERAL. Passing it through a constructor would read better and
 *     would blind {@code tools/locale_audit.py}, which follows literals in the
 *     source — {@code SkySettlers.SkyResident} takes its tip key as a field and
 *     the audit reports it as "built at runtime; cannot check". One class doing
 *     that is a known cost; making it the pattern is not.</li>
 * <li>The mob. Two shops with two sets of services have nothing to share.</li>
 * </ul>
 *
 * <h2>The recruit ticket</h2>
 *
 * {@link #recruitTickets()} defaults to <b>75, behind no story gate</b> — the
 * terms vanilla gives the Blacksmith and the Miner, which is what "as common as
 * the professions you already know" means in code. (The Angler takes 100; the
 * Stylist, the Trader and the Alchemist take theirs only after a story
 * objective.) A profession that should be rarer overrides the number; one that
 * should be gated overrides {@code addNewRecruitSettler} outright.
 */
public abstract class ProfessionSettler extends Settler {

    protected ProfessionSettler(String mobStringID) {
        // Names the MOB, not this settler's own key: Settler resolves the mob
        // it stands up through MobRegistry with this string. Vanilla's own
        // pairing is the same shape ("stylist" the settler, "stylisthuman" the
        // mob).
        super(mobStringID);
        // Vanilla's COMPLETE_HOST achievement wants one of every settler type
        // in a settlement. A modded settler must stay out of that set or
        // installing this mod makes the achievement unreachable.
        this.isPartOfCompleteHost = false;
    }

    /**
     * The settlement screen's "where do I get this settler" line.
     *
     * <p>Section {@code [settlement]}, the same one vanilla's {@code minertip}
     * and {@code stylisttip} live in. Write the key as a literal — see the
     * class note.
     */
    @Override
    public abstract necesse.engine.localization.message.GameMessage getAcquireTip();

    /**
     * The vanilla icon path this profession borrows for the settlement screen.
     *
     * <p>In practice a fallback: {@code Settler.getSettlerFaceDrawOptions} draws
     * the settler's own human face from the live mob whenever there is one.
     * Every path used here has a row in {@code docs/VANILLA_ASSET_MAP.md}.
     */
    protected abstract String iconPath();

    /**
     * helmet / chestplate / boots item string IDs, or null for a bare slot.
     *
     * <p>Vanilla clothing items only — no new art, the same trade every settler
     * in this mod makes.
     */
    protected abstract String[] wardrobe();

    /** Tickets in the settlement's recruit draw. See the class note. */
    protected int recruitTickets() {
        return 75;
    }

    @Override
    public void loadTextures() {
        this.texture = GameTexture.fromFile(this.iconPath());
    }

    @Override
    public void setDefaultArmor(HumanDrawOptions drawOptions, int seed, HumanLook look,
                                boolean isSettler) {
        String[] worn = this.wardrobe();
        if (worn == null) {
            return;
        }
        if (worn.length > 0 && worn[0] != null) {
            drawOptions.helmet(new InventoryItem(worn[0]));
        }
        if (worn.length > 1 && worn[1] != null) {
            drawOptions.chestplate(new InventoryItem(worn[1]));
        }
        if (worn.length > 2 && worn[2] != null) {
            drawOptions.boots(new InventoryItem(worn[2]));
        }
    }

    /**
     * The ticket that makes one of these turn up.
     *
     * <p>{@code isRandomEvent} true is vanilla's "a settler arrives out of the
     * blue" pass, and vanilla lets that one bypass the already-have-this-type
     * check; the normal recruit draw does not. Copied verbatim from
     * {@code BlacksmithSettler}.
     */
    @Override
    public void addNewRecruitSettler(ServerSettlementData data, boolean isRandomEvent,
                                     TicketSystemList<Supplier<HumanMob>> ticketSystem) {
        if (isRandomEvent || !this.doesSettlementHaveThisSettler(data)) {
            ticketSystem.addObject(this.recruitTickets(), this.getNewRecruitMob(data));
        }
    }
}
