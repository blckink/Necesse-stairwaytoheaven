package stairwaytoheaven.mobs;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.gfx.HumanGender;
import necesse.gfx.HumanLook;
import necesse.inventory.container.mob.ContainerQuest;

/**
 * The Warden as a surface settler — the recruited form of {@link SkyWardenMob}.
 *
 * Created directly by the recruitment transaction (see
 * {@code SkyWardenMob.tryRecruit}): the server places him on the surface level
 * at the player's bound stairway using the Elder's placement recipe
 * ({@code setHome} → {@code Waystone.findTeleportLocation} →
 * {@code entityManager.addMob}). As a {@link HumanShop} subclass he is a real
 * Necesse settler: housing/bed assignment, room happiness, jobs and the
 * vanilla hire/interact machinery all come from the HumanMob branch — the same
 * bar the Elder sits on. The player then assigns his bed through the normal
 * settlement menu.
 *
 * RENDERING: he uses the vanilla human renderer exclusively — no bespoke
 * sprite overlay (an earlier draft drew a full-body sheet OVER the HumanMob
 * body, double-rendering him). His identity comes from the native channels
 * instead: a pinned Skywatch storm-blue robe + nightfell-dark shoes via
 * {@link #randomizeLook}, on top of an otherwise vanilla-rolled look.
 *
 * API note (verified against the 1.3.2 jar): HumanShop's only constructor is
 * {@code (int maxHealth, int maxHealthBase, String typeID)} — the Elder passes
 * {@code (500, 500, "elder")} and we mirror that with our own string ID.
 */
public class WardenSettlerMob extends HumanShop {

    public WardenSettlerMob() {
        super(500, 500, "wardensettler");
        this.canDespawn = false;
        // Elder-matching walk speed so settlement jobs/pathing behave like any
        // other human settler.
        this.setSpeed(30.0F);
    }

    /**
     * Vanilla ships the quest-giver hook dormant; the Elder overrides it to
     * return null and so do we — the Warden's tasks are handed out through his
     * own dialogue, not the dormant registry.
     */
    @Override
    public ArrayList<ContainerQuest> getQuests(ServerClient client) {
        return null;
    }

    /**
     * The recruitment fee was already paid, in coin, to his sky-side self —
     * so moving in costs nothing here. An EMPTY list is the vanilla idiom for
     * a free recruit (the Trader uses it after being freed from a trap): it
     * makes the shop's recruit button live and shows "recruit for free",
     * whereas the inherited {@code null} would leave the button permanently
     * dead and strand him outside the settlement forever.
     */
    @Override
    public List<necesse.inventory.InventoryItem> getRecruitItems(ServerClient client) {
        return Collections.emptyList();
    }

    /**
     * Open on the recruit page the first time the player talks to him at home:
     * "build me something with a view" is the next step, and the shop tab would
     * otherwise bury it.
     */
    @Override
    public boolean startInRecruitForm(ServerClient client) {
        return super.startInRecruitForm(client) || !this.isSettler();
    }

    /**
     * Fixed identity through the vanilla look system: any rolled face/hair,
     * but always the Skywatch storm-blue robe with dark shoes — recognizable
     * next to vanilla settlers without leaving the human renderer.
     */
    @Override
    public void randomizeLook(HumanLook look, HumanGender gender, GameRandom random) {
        look.randomizeLook(random, true, gender, true, true, true, true);
        look.setShirtColor(new Color(86, 96, 122));
        look.setShoesColor(new Color(46, 44, 60));
    }
}

