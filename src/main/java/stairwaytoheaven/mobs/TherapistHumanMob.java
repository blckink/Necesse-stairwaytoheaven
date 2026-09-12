package stairwaytoheaven.mobs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanShop.BuyingShopItem;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.inventory.InventoryItem;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;
import necesse.level.maps.levelData.settlementData.settler.dialogues.SettlerDialogue;
import stairwaytoheaven.settlement.SkyTherapy;
import stairwaytoheaven.settlement.TherapySlotsDialogue;
import stairwaytoheaven.settlement.TraitTherapyDialogue;

/**
 * The Therapist — the person behind the profession.
 *
 * <p>Deliberately NOT a {@code SkySettlerMob}: that base is for the mod's named
 * residents and hands out {@code canTakeDamage() == false},
 * {@code canDespawn == false} and a fixed face, none of which belong on a
 * profession that a world may hold any number of. This extends
 * {@link HumanShop} directly, the same as vanilla's own Stylist and Miner, and
 * keeps every default they keep.
 *
 * <p>What it adds over a plain shop is the four therapy places. They live here,
 * on the mob, because that is what is being assigned TO — a settlement may hold
 * two Therapists and each keeps their own four patients — and because
 * {@code addSaveData}/{@code applyLoadData} on the mob is the one store that
 * travels with the settler through a move, a save and a dimension change.
 *
 * @see SkyTherapy for the mood arithmetic and the trait swap
 */
public class TherapistHumanMob extends HumanShop {

    /**
     * Patients, by {@code Mob.getUniqueID()} — the same handle
     * {@code LevelSettler.mobUniqueID} uses, which vanilla writes into the
     * settlement save. 0 means the place is free.
     */
    protected final int[] patients = new int[SkyTherapy.SLOTS];

    /** Wall-clock guard so the therapy refresh runs seconds apart, not ticks. */
    private long nextTherapyCheck;

    public TherapistHumanMob() {
        // (health, speed, settlerStringID). The third argument is the
        // SettlerRegistry key, NOT the mob's own ID — getting it wrong makes
        // getSettler() null and silently disables recruitment. Vanilla's
        // Stylist passes "stylist" while its mob is "stylisthuman"; ours is
        // "therapist" against mob "therapisthuman".
        super(500, 200, "therapist");
        this.attackCooldown = 500;
        this.attackAnimTime = 500;

        // A small, on-theme shop. Every string ID here is one vanilla already
        // sells through a shop of its own, so none of them can be a
        // registration that does not exist.
        this.shop.addSellingItem("prettyflower", new SellingShopItem())
                .setStaticPriceBasedOnHappiness(600, 950, 50);
        this.shop.addSellingItem("book", new SellingShopItem())
                .setStaticPriceBasedOnHappiness(300, 550, 40);
        this.shop.addSellingItem("recipebook", new SellingShopItem())
                .setStaticPriceBasedOnHappiness(400, 700, 50);
        this.shop.addSellingItem("musicplayer", new SellingShopItem())
                .setStaticPriceBasedOnHappiness(1200, 2000, 120);
        this.shop.addSellingItem("homevinyl", new SellingShopItem())
                .setStaticPriceBasedOnHappiness(400, 750, 50);
        this.shop.addSellingItem("healthregenpotion", new SellingShopItem())
                .setStaticPriceBasedOnHappiness(150, 300, 25);
        this.shop.addSellingItem("manaregenpotion", new SellingShopItem())
                .setStaticPriceBasedOnHappiness(150, 300, 25);
        this.shop.addSellingItem("blazer", new SellingShopItem())
                .setStaticPriceBasedOnHappiness(75, 150, 20);
        this.shop.addSellingItem("dressshoes", new SellingShopItem())
                .setStaticPriceBasedOnHappiness(75, 150, 20);
        this.shop.addBuyingItem("sunflower", new BuyingShopItem())
                .setPriceBasedOnHappiness(18, 11, 3);
        this.shop.addBuyingItem("silk", new BuyingShopItem())
                .setPriceBasedOnHappiness(20, 12, 3);
    }

    // --- what the player pays to move them in ---------------------------

    /**
     * Vanilla's own recruit page takes this; never hand-roll a payment in
     * {@code interact()}. Priced between the Stylist's visitor fee and the
     * settled professions: the trait swap they unlock is expensive on its own.
     */
    @Override
    public List<InventoryItem> getRecruitItems(ServerClient client) {
        GameRandom random = new GameRandom((long) this.getSettlerSeed() * 271L);
        return Collections.singletonList(
                new InventoryItem("coin", random.getIntBetween(900, 1400)));
    }

    @Override
    protected ArrayList<GameMessage> getMessages(ServerClient client) {
        return getLocalMessages("mobmsg", "therapisttalk", 5);
    }

    // --- the two services ------------------------------------------------

    /**
     * The two extra lines in the talk menu, beside vanilla's own.
     *
     * <p>{@code clientHasAccess} is vanilla's "this player owns the
     * settlement"; both services change other people's settlers permanently, so
     * neither is offered to a visiting player. A Therapist who has not moved in
     * yet has no settlement to work on and offers nothing either.
     */
    @Override
    public ArrayList<SettlerDialogue> getDialogues(ServerClient client, ServerSettlementData data,
                                                   boolean clientHasAccess) {
        ArrayList<SettlerDialogue> out = super.getDialogues(client, data, clientHasAccess);
        if (clientHasAccess && this.isSettler() && data != null) {
            out.add(new TherapySlotsDialogue(this, data));
            out.add(new TraitTherapyDialogue(this, data));
        }
        return out;
    }

    /** The patient in a place, or 0. */
    public int getPatient(int slot) {
        return slot < 0 || slot >= this.patients.length ? 0 : this.patients[slot];
    }

    /**
     * Put a settler in a therapy place — or clear it with
     * {@code mobUniqueID == 0}.
     *
     * <p>Server side this also lifts the effect off whoever was there, because
     * a bonus that outlives its assignment is a bonus the player cannot take
     * away again.
     */
    public void setPatient(int slot, int mobUniqueID) {
        if (slot < 0 || slot >= this.patients.length) {
            return;
        }
        // The same person may not occupy two places: four slots would then buy
        // one settler four times the mood.
        if (mobUniqueID != 0) {
            for (int i = 0; i < this.patients.length; i++) {
                if (i != slot && this.patients[i] == mobUniqueID) {
                    this.patients[i] = 0;
                }
            }
        }
        int previous = this.patients[slot];
        this.patients[slot] = mobUniqueID;
        if (this.isServer() && previous != 0 && previous != mobUniqueID) {
            HumanMob old = this.findSettlerMob(previous);
            if (old != null) {
                SkyTherapy.clearTherapy(old);
            }
        }
    }

    /**
     * Refresh the therapy every two seconds.
     *
     * <p>Two seconds rather than every tick because the effect is a mood line,
     * not a buff, and because the refresh walks the settlement's settler list.
     * The thought itself is added with a 60 second life, so it survives a
     * missed pass and expires on its own if this Therapist dies, is sold off or
     * leaves the level — which is what makes "only while assigned" true without
     * a teardown path that could be missed.
     */
    @Override
    public void serverTick() {
        super.serverTick();
        long now = System.currentTimeMillis();
        if (now < this.nextTherapyCheck) {
            return;
        }
        this.nextTherapyCheck = now + 2000L;
        if (!this.isSettler()) {
            return;
        }
        ServerSettlementData data = this.getSettlerSettlementServerData();
        if (data == null) {
            return;
        }
        for (int slot = 0; slot < this.patients.length; slot++) {
            int uniqueID = this.patients[slot];
            if (uniqueID == 0) {
                continue;
            }
            HumanMob patient = this.findSettlerMob(uniqueID);
            if (patient == null || patient == this) {
                // Moved out, banished or dead: the place falls free, and the
                // thought on them (if they still exist anywhere) runs out.
                this.patients[slot] = 0;
                continue;
            }
            SkyTherapy.applyTherapy(patient);
        }
    }

    /**
     * Find one of this Therapist's own settlement's settlers by unique ID.
     *
     * <p>Goes through {@code ServerSettlementData} rather than the level's mob
     * list on purpose: it is the settlement membership that decides who may be
     * treated, and a settler who has wandered onto another level is still a
     * member ({@code LevelSettler.getMob} falls back to
     * {@code SettlersWorldData}).
     */
    public HumanMob findSettlerMob(int mobUniqueID) {
        ServerSettlementData data = this.getSettlerSettlementServerData();
        if (data == null) {
            return null;
        }
        for (LevelSettler levelSettler : data.getSettlers()) {
            if (levelSettler.mobUniqueID != mobUniqueID) {
                continue;
            }
            SettlerMob settlerMob = levelSettler.getMob();
            Mob mob = settlerMob == null ? null : settlerMob.getMob();
            return mob instanceof HumanMob ? (HumanMob) mob : null;
        }
        return null;
    }

    // --- persistence -----------------------------------------------------

    @Override
    public void addSaveData(SaveData save) {
        super.addSaveData(save);
        for (int slot = 0; slot < this.patients.length; slot++) {
            if (this.patients[slot] != 0) {
                save.addInt("therapypatient" + slot, this.patients[slot]);
            }
        }
    }

    @Override
    public void applyLoadData(LoadData save) {
        super.applyLoadData(save);
        for (int slot = 0; slot < this.patients.length; slot++) {
            this.patients[slot] = save.getInt("therapypatient" + slot, 0, false);
        }
    }
}
