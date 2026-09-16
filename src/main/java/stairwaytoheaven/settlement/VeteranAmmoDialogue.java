package stairwaytoheaven.settlement;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.ItemRegistry;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.components.FormDialogueOption;
import necesse.gfx.forms.presets.containerComponent.mob.DialogueForm;
import necesse.gfx.forms.presets.containerComponent.mob.ShopContainerForm;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.customAction.EmptyCustomAction;
import necesse.inventory.container.mob.ShopContainer;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Recipe;
import necesse.level.maps.levelData.settlementData.settler.dialogues.SettlerDialogue;

/**
 * "Hand over ammunition" — raises {@link VeteranDefense#level} by consuming a
 * stack of the next tier's arrows and explosives, straight out of the
 * player's own inventory.
 *
 * <h2>Shape</h2>
 *
 * Built the same way as {@code DoctorHealDialogue}: one form, one button, no
 * result packet needed because the outcome is a number this same dialogue
 * already displays. Unlike the Doctor's fee (plain coins taken by hand), the
 * cost here is two real vanilla items, so it is taken through
 * {@code Ingredient}/{@code Recipe.craft} — the same vanilla-machinery path
 * {@code TraitTherapyDialogue} uses for its coin price, generalised to
 * non-coin ingredients.
 *
 * <h2>Why the level travels in the packet</h2>
 *
 * {@link VeteranDefense} lives in {@code WorldEntity} on the server only; the
 * client's copy of this dialogue has no way to read it directly. Exactly like
 * {@code DoctorHealDialogue} carries its price and {@code TherapySlotsDialogue}
 * carries its slots, {@link #setupContainerPacket}/{@link #applyContainerPacket}
 * carry the current level across, so the client can show "Level 2/5" and the
 * correct next requirement without guessing.
 *
 * <h2>Client-side number after a click</h2>
 *
 * The server is the only side that actually removes items and calls
 * {@link VeteranDefense#raiseLevel(int)}. But the button's own
 * {@code EmptyCustomAction} runs once locally the instant it is clicked (that
 * is how vanilla's own dialogue actions give instant feedback) and once again
 * when the packet reaches the server. On the client side that first run only
 * predicts the same afford-check the server will make and, if it agrees,
 * bumps this dialogue's own {@link #level} field so the menu redraws with the
 * new number immediately; the server's own instance does the real work
 * against the authoritative inventory and {@link VeteranDefense}. If the
 * prediction and the server ever disagree (e.g. another player just spent the
 * last bomb), the next time this dialogue is reopened it is rebuilt from the
 * server's real number again.
 */
public class VeteranAmmoDialogue extends SettlerDialogue {

    /** One step of the ladder: what {@link VeteranDefense#raiseLevel(int)} costs to climb it. */
    private static final class Tier {
        final String arrowID;
        final int arrowAmount;
        final String bombID;
        final int bombAmount;

        Tier(String arrowID, int arrowAmount, String bombID, int bombAmount) {
            this.arrowID = arrowID;
            this.arrowAmount = arrowAmount;
            this.bombID = bombID;
            this.bombAmount = bombAmount;
        }

        Ingredient[] cost() {
            return new Ingredient[]{
                    new Ingredient(this.arrowID, this.arrowAmount),
                    new Ingredient(this.bombID, this.bombAmount)
            };
        }

        /** "100x Iron Arrow + 5x Iron Bomb", using vanilla's own item names. */
        String describe() {
            String arrowName = ItemRegistry.getItem(this.arrowID)
                    .getDisplayName(new InventoryItem(this.arrowID, this.arrowAmount));
            String bombName = ItemRegistry.getItem(this.bombID)
                    .getDisplayName(new InventoryItem(this.bombID, this.bombAmount));
            return this.arrowAmount + "x " + arrowName + " + " + this.bombAmount + "x " + bombName;
        }
    }

    /**
     * Turret line: stone -> iron -> fire -> frost -> poison arrows.
     * Catapult line: iron bombs, then dynamite once the turret arrows run out
     * of vanilla tiers. Every ID here was verified against the 1.3.3
     * {@code Server.jar} ({@code ItemRegistry.class} / arrow item classes)
     * before use.
     */
    private static final Tier[] TIERS = new Tier[]{
            new Tier("stonearrow", 100, "ironbomb", 5),
            new Tier("ironarrow", 150, "ironbomb", 8),
            new Tier("firearrow", 200, "ironbomb", 12),
            new Tier("frostarrow", 250, "dynamitestick", 6),
            new Tier("poisonarrow", 300, "dynamitestick", 10)
    };

    /** Synced from the server; 0-{@link VeteranDefense#MAX_LEVEL}. */
    protected int level;

    protected EmptyCustomAction handOverAction;
    protected DialogueForm ammoForm;
    protected FormDialogueOption handOverButton;

    /** Client-side reconstruction. Required by {@code SettlerDialogueRegistry}. */
    public VeteranAmmoDialogue(HumanMob mob) {
        super(mob);
    }

    /** Server-side construction, with the current defense level to show. */
    public VeteranAmmoDialogue(HumanMob mob, int level) {
        this(mob);
        this.level = level;
    }

    @Override
    protected void setupContainerPacket(PacketWriter writer) {
        super.setupContainerPacket(writer);
        writer.putNextInt(this.level);
    }

    @Override
    protected void applyContainerPacket(PacketReader reader) {
        super.applyContainerPacket(reader);
        this.level = reader.getNextInt();
    }

    @Override
    public void init(final ShopContainer container) {
        super.init(container);
        this.handOverAction = container.registerAction(new EmptyCustomAction() {
            @Override
            protected void run() {
                if (VeteranAmmoDialogue.this.settlerMob.isServer()) {
                    VeteranAmmoDialogue.this.runHandOver(container);
                } else {
                    VeteranAmmoDialogue.this.predictHandOver(container);
                }
            }
        });
    }

    /** The real thing: take the items, raise the level, remember the new number. */
    protected void runHandOver(ShopContainer container) {
        if (!container.hasSettlerAccess || this.level >= VeteranDefense.MAX_LEVEL) {
            return;
        }
        Tier tier = TIERS[this.level];
        Ingredient[] cost = tier.cost();
        if (!container.canCraftRecipe(cost, container.getCraftInventories(), true, null).canCraft()) {
            return;
        }
        Recipe.craft(cost, this.settlerMob.getLevel(), container.client.playerMob,
                container.getCraftInventories(), null);
        VeteranDefense defense = this.settlerMob.getLevel() == null ? null
                : VeteranDefense.get(this.settlerMob.getLevel().getServer());
        if (defense != null) {
            defense.raiseLevel(1);
            this.level = defense.level;
        } else {
            this.level = Math.min(VeteranDefense.MAX_LEVEL, this.level + 1);
        }
    }

    /** Cosmetic only: guesses the same outcome so the menu updates without a round trip. */
    protected void predictHandOver(ShopContainer container) {
        if (this.level >= VeteranDefense.MAX_LEVEL) {
            return;
        }
        Tier tier = TIERS[this.level];
        if (container.canCraftRecipe(tier.cost(), container.getCraftInventories(), true, null).canCraft()) {
            this.level = Math.min(VeteranDefense.MAX_LEVEL, this.level + 1);
        }
    }

    @Override
    public void initForm(final ShopContainer container, ShopContainerForm<?> containerForm) {
        super.initForm(container, containerForm);
        this.ammoForm = containerForm.addComponent(new DialogueForm("swhveteranammo",
                containerForm.width, containerForm.minHeight, containerForm.maxHeight, true));
    }

    @Override
    public void setupDialogueOptions(final ShopContainer container,
                                     final ShopContainerForm<?> containerForm) {
        super.setupDialogueOptions(container, containerForm);
        if (!container.hasSettlerAccess || this.ammoForm == null) {
            return;
        }
        this.buildAmmoForm(container, containerForm);
        containerForm.dialogueForm.addDialogueOption(
                new LocalMessage("misc", "swhveteranoption", "level", Integer.toString(this.level)),
                () -> {
                    this.buildAmmoForm(container, containerForm);
                    containerForm.makeCurrent(this.ammoForm);
                });
    }

    protected void buildAmmoForm(final ShopContainer container,
                                 final ShopContainerForm<?> containerForm) {
        final boolean maxed = this.level >= VeteranDefense.MAX_LEVEL;
        this.ammoForm.reset(container.humanShop, true, container.romanceLevel,
                (contentBox, flow) -> {
                    Runnable chatBubble = DialogueForm.startChatBubble(contentBox, flow);
                    if (maxed) {
                        DialogueForm.addText(contentBox, flow,
                                new LocalMessage("misc", "swhveteranmax"), true);
                    } else {
                        DialogueForm.addText(contentBox, flow,
                                new LocalMessage("misc", "swhveteranintro",
                                        "level", Integer.toString(this.level),
                                        "max", Integer.toString(VeteranDefense.MAX_LEVEL),
                                        "req", TIERS[this.level].describe()), true);
                    }
                    chatBubble.run();
                });
        if (!maxed) {
            this.handOverButton = this.ammoForm.addDialogueOption(
                    new LocalMessage("misc", "swhveterandoit"),
                    () -> {
                        this.handOverAction.runAndSend();
                        this.buildAmmoForm(container, containerForm);
                    });
            this.handOverButton.setActive(
                    container.canCraftRecipe(TIERS[this.level].cost(),
                            container.getCraftInventories(), true, null).canCraft());
        }
        this.ammoForm.addDialogueOption(new LocalMessage("ui", "backbutton"),
                () -> containerForm.makeCurrent(containerForm.dialogueForm));
    }

    @Override
    public <T extends ShopContainer> void onWindowResized(necesse.engine.window.GameWindow window,
                                                          ShopContainer container,
                                                          ShopContainerForm<?> containerForm) {
        super.onWindowResized(window, container, containerForm);
        if (this.ammoForm != null) {
            ContainerComponent.setPosFocus(this.ammoForm);
        }
    }
}
