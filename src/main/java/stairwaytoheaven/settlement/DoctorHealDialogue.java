package stairwaytoheaven.settlement;

import java.awt.Rectangle;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.window.GameWindow;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MobHealthChangeEvent;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.components.FormDialogueOption;
import necesse.gfx.forms.presets.containerComponent.mob.DialogueForm;
import necesse.gfx.forms.presets.containerComponent.mob.ShopContainerForm;
import necesse.inventory.container.customAction.EmptyCustomAction;
import necesse.inventory.container.mob.ShopContainer;
import necesse.level.maps.levelData.settlementData.settler.dialogues.SettlerDialogue;

/**
 * "Patch me up" — the Doctor's one service.
 *
 * <p>A small fee, and the player is back to full health on the spot. No
 * cooldown: the fee is the throttle, and you have to be standing in front of a
 * Doctor who lives in your own settlement, which is limit enough. Health only —
 * not mana, not resilience, not hunger.
 *
 * <h2>How the healing is done</h2>
 *
 * Through {@code MobHealthChangeEvent}, added to the level's event manager on
 * the server. That is not a detour around {@code setHealth}: it is what
 * vanilla's own Health Potion does ({@code HealthPotionItem.onPlace}, 1.3.3),
 * and going through the event is what makes the number float up over the
 * player's head and the new health reach every client that can see them.
 *
 * <h2>How the fee is taken</h2>
 *
 * Straight out of the player's own inventory, the way
 * {@code ShopContainer.canPayForRecruit} / {@code payForRecruit} do it — NOT
 * through {@code Recipe.craft} over {@code getCraftInventories()}. Both would
 * work today, because a shop container's craft inventories are the player's own
 * slots. But "the doctor's fee comes out of your purse" is a rule that should
 * not quietly become "…or out of the settlement chest" if a future container
 * ever gains a storage slot.
 *
 * <p>There is no result packet, unlike the Therapist's trait swap: the outcome
 * of being healed is a health bar that fills up, which the player can already
 * see.
 */
public class DoctorHealDialogue extends SettlerDialogue {

    protected int price;
    protected EmptyCustomAction healAction;
    protected DialogueForm healForm;
    protected FormDialogueOption healButton;
    /** Set once the fee has been taken, so the menu can say thank you. */
    protected boolean healedThisVisit;

    /** Client-side reconstruction. Required by {@code SettlerDialogueRegistry}. */
    public DoctorHealDialogue(HumanMob mob) {
        super(mob);
    }

    /** Server-side construction. */
    public DoctorHealDialogue(HumanMob mob, int price) {
        this(mob);
        this.price = price;
    }

    @Override
    protected void setupContainerPacket(PacketWriter writer) {
        super.setupContainerPacket(writer);
        writer.putNextInt(this.price);
    }

    @Override
    protected void applyContainerPacket(PacketReader reader) {
        super.applyContainerPacket(reader);
        this.price = reader.getNextInt();
    }

    @Override
    public void init(final ShopContainer container) {
        super.init(container);
        this.healAction = container.registerAction(new EmptyCustomAction() {
            @Override
            protected void run() {
                if (DoctorHealDialogue.this.settlerMob.isServer()) {
                    DoctorHealDialogue.this.runHeal(container);
                }
                DoctorHealDialogue.this.healedThisVisit = true;
            }
        });
    }

    /**
     * Take the fee, then heal — in that order, and only if both can happen.
     *
     * <p>A player already at full health is refused rather than charged for
     * nothing; the button is greyed out for the same case, but a client can
     * click anything, so the server decides.
     */
    protected void runHeal(ShopContainer container) {
        if (!container.hasSettlerAccess) {
            return;
        }
        PlayerMob player = container.client.playerMob;
        if (player == null || player.getLevel() == null) {
            return;
        }
        int missing = player.getMaxHealth() - player.getHealth();
        if (missing <= 0 || !canPay(container)) {
            return;
        }
        player.getInv().main.removeItems(player.getLevel(), player,
                necesse.engine.registries.ItemRegistry.getItem("coin"), this.price, "doctorfee");
        player.getLevel().entityManager.events.add(new MobHealthChangeEvent((Mob) player, missing));
    }

    /** Coins in the player's own inventory, counted the way vanilla counts them. */
    protected boolean canPay(ShopContainer container) {
        PlayerMob player = container.client.playerMob;
        if (player == null || player.getLevel() == null) {
            return false;
        }
        int coins = player.getInv().main.getAmount(player.getLevel(), player,
                necesse.engine.registries.ItemRegistry.getItem("coin"), "buy");
        return coins >= this.price;
    }

    protected boolean needsHealing(ShopContainer container) {
        PlayerMob player = container.client.playerMob;
        return player != null && player.getHealth() < player.getMaxHealth();
    }

    @Override
    public void initForm(final ShopContainer container, ShopContainerForm<?> containerForm) {
        super.initForm(container, containerForm);
        this.healForm = containerForm.addComponent(new DialogueForm("swhdoctorheal",
                containerForm.width, containerForm.minHeight, containerForm.maxHeight, true) {
            /**
             * Coins get spent and health gets lost while this menu is open, so
             * the button's state is re-asked every frame rather than once when
             * the form was built. Vanilla's Collector gift button does the same
             * thing for the same reason.
             */
            @Override
            public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
                DoctorHealDialogue.this.updateHealButton(container);
                super.draw(tickManager, perspective, renderBox);
            }
        });
    }

    protected void updateHealButton(ShopContainer container) {
        if (this.healButton == null) {
            return;
        }
        this.healButton.setActive(this.canPay(container) && this.needsHealing(container));
    }

    @Override
    public void setupDialogueOptions(final ShopContainer container,
                                     final ShopContainerForm<?> containerForm) {
        super.setupDialogueOptions(container, containerForm);
        this.healButton = null;
        if (!container.hasSettlerAccess || this.healForm == null) {
            return;
        }
        this.buildHealForm(container, containerForm);
        containerForm.dialogueForm.addDialogueOption(
                new LocalMessage("misc", "swhdoctoroption", "price", Integer.toString(this.price)),
                () -> {
                    this.healedThisVisit = false;
                    this.buildHealForm(container, containerForm);
                    containerForm.makeCurrent(this.healForm);
                });
    }

    protected void buildHealForm(final ShopContainer container,
                                 final ShopContainerForm<?> containerForm) {
        final boolean healed = this.healedThisVisit;
        this.healForm.reset(container.humanShop, true, container.romanceLevel,
                (contentBox, flow) -> {
                    Runnable chatBubble = DialogueForm.startChatBubble(contentBox, flow);
                    if (healed) {
                        DialogueForm.addText(contentBox, flow,
                                new LocalMessage("misc", "swhdoctordone"), true);
                    } else if (!this.needsHealing(container)) {
                        DialogueForm.addText(contentBox, flow,
                                new LocalMessage("misc", "swhdoctorfine"), true);
                    } else {
                        DialogueForm.addText(contentBox, flow,
                                new LocalMessage("misc", "swhdoctorintro",
                                        "price", Integer.toString(this.price)), true);
                    }
                    chatBubble.run();
                });
        if (!healed) {
            this.healButton = this.healForm.addDialogueOption(
                    new LocalMessage("misc", "swhdoctordoit", "price", Integer.toString(this.price)),
                    () -> {
                        this.healAction.runAndSend();
                        this.buildHealForm(container, containerForm);
                    });
            this.updateHealButton(container);
        }
        this.healForm.addDialogueOption(new LocalMessage("ui", "backbutton"),
                () -> containerForm.makeCurrent(containerForm.dialogueForm));
    }

    @Override
    public <T extends ShopContainer> void onWindowResized(GameWindow window, ShopContainer container,
                                                          ShopContainerForm<?> containerForm) {
        super.onWindowResized(window, container, containerForm);
        if (this.healForm != null) {
            ContainerComponent.setPosFocus(this.healForm);
        }
    }
}
