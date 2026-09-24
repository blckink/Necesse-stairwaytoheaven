package stairwaytoheaven.settlement;

import java.awt.Rectangle;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.window.GameWindow;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.components.FormDialogueOption;
import necesse.gfx.forms.presets.containerComponent.mob.DialogueForm;
import necesse.gfx.forms.presets.containerComponent.mob.ShopContainerForm;
import necesse.inventory.container.customAction.EmptyCustomAction;
import necesse.inventory.container.mob.ShopContainer;
import necesse.level.maps.levelData.settlementData.settler.dialogues.SettlerDialogue;
import stairwaytoheaven.mobs.VampireSettlerMob;

/**
 * Talking to Dorian — §3.2 of {@code docs/design/concept-nightbound-dorian.md}:
 * <i>"Ein eigenes Dialogmenü, wie beim Arzt oder Therapeuten"</i>, with the
 * three things the concept asks for on one page:
 * <ol>
 *   <li><b>How thirsty is he?</b> His invisible 0..1 thirst, in one of four
 *       words (sated / thirsty / very thirsty / "tonight I have to bite
 *       someone"), so the meter is readable without a new HUD bar.</li>
 *   <li><b>Give him a Blood Vial</b> — the promise his fourth talk line made
 *       since he shipped and nothing ever kept: +0.5 thirst, one vial, only
 *       offered while the player carries one.</li>
 *   <li><b>What happened last night?</b> How many animals he drained, whether
 *       one got up again as a Blood Thrall, and whom he bit, by name.</li>
 * </ol>
 * The shape is {@code DoctorHealDialogue}'s: the numbers are written into the
 * container packet on the server and read back on the client, and the one
 * action is a registered {@code EmptyCustomAction} that the server re-checks.
 * Only for the settlement's own people ({@code hasSettlerAccess}).
 */
public class DorianDialogue extends SettlerDialogue {

    protected int thirstStage;
    protected int drained;
    protected int thralls;
    protected String bitten = "";

    protected EmptyCustomAction feedAction;
    protected DialogueForm form;
    protected FormDialogueOption feedButton;
    protected boolean fedThisVisit;

    public DorianDialogue(HumanMob mob) {
        super(mob);
        if (mob instanceof VampireSettlerMob && mob.isServer()) {
            VampireSettlerMob dorian = (VampireSettlerMob) mob;
            this.thirstStage = dorian.thirstStage();
            this.drained = dorian.lastNightDrained();
            this.thralls = dorian.lastNightThralls();
            this.bitten = dorian.lastNightBitten();
        }
    }

    @Override
    protected void setupContainerPacket(PacketWriter writer) {
        super.setupContainerPacket(writer);
        writer.putNextByteUnsigned(this.thirstStage);
        writer.putNextShortUnsigned(Math.min(65535, this.drained));
        writer.putNextShortUnsigned(Math.min(65535, this.thralls));
        writer.putNextString(this.bitten == null ? "" : this.bitten);
    }

    @Override
    protected void applyContainerPacket(PacketReader reader) {
        super.applyContainerPacket(reader);
        this.thirstStage = reader.getNextByteUnsigned();
        this.drained = reader.getNextShortUnsigned();
        this.thralls = reader.getNextShortUnsigned();
        this.bitten = reader.getNextString();
    }

    @Override
    public void init(final ShopContainer container) {
        super.init(container);
        this.feedAction = container.registerAction(new EmptyCustomAction() {
            @Override
            protected void run() {
                if (DorianDialogue.this.settlerMob.isServer()) {
                    DorianDialogue.this.runFeed(container);
                }
                DorianDialogue.this.fedThisVisit = true;
            }
        });
    }

    /** Server: one vial out of the player's bag, +0.5 thirst. */
    protected void runFeed(ShopContainer container) {
        if (!container.hasSettlerAccess || !(this.settlerMob instanceof VampireSettlerMob)) {
            return;
        }
        PlayerMob player = container.client.playerMob;
        if (player == null || player.getLevel() == null || carried(player) <= 0) {
            return;
        }
        player.getInv().main.removeItems(player.getLevel(), player, ItemRegistry.getItem("bloodvial"),
                1, "dorian");
        ((VampireSettlerMob) this.settlerMob).feed(0.5F);
        this.thirstStage = ((VampireSettlerMob) this.settlerMob).thirstStage();
    }

    protected static int carried(PlayerMob player) {
        if (player == null || player.getLevel() == null) {
            return 0;
        }
        return player.getInv().main.getAmount(player.getLevel(), player,
                ItemRegistry.getItem("bloodvial"), "dorian");
    }

    @Override
    public void initForm(final ShopContainer container, ShopContainerForm<?> containerForm) {
        super.initForm(container, containerForm);
        this.form = containerForm.addComponent(new DialogueForm("swhdorian",
                containerForm.width, containerForm.minHeight, containerForm.maxHeight, true) {
            @Override
            public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
                if (DorianDialogue.this.feedButton != null) {
                    DorianDialogue.this.feedButton.setActive(carried(container.client.playerMob) > 0);
                }
                super.draw(tickManager, perspective, renderBox);
            }
        });
    }

    @Override
    public void setupDialogueOptions(final ShopContainer container,
                                     final ShopContainerForm<?> containerForm) {
        super.setupDialogueOptions(container, containerForm);
        if (!container.hasSettlerAccess || this.form == null) {
            return;
        }
        containerForm.dialogueForm.addDialogueOption(new LocalMessage("misc", "swhdorianoption"), () -> {
            this.fedThisVisit = false;
            this.build(container, containerForm);
            containerForm.makeCurrent(this.form);
        });
    }

    protected void build(final ShopContainer container, final ShopContainerForm<?> containerForm) {
        this.form.reset(container.humanShop, true, container.romanceLevel, (contentBox, flow) -> {
            Runnable chatBubble = DialogueForm.startChatBubble(contentBox, flow);
            if (this.fedThisVisit) {
                DialogueForm.addText(contentBox, flow, new LocalMessage("misc", "swhdorianfed"), true);
            } else {
                DialogueForm.addText(contentBox, flow,
                        new LocalMessage("misc", thirstKey(this.thirstStage)), true);
            }
            DialogueForm.addText(contentBox, flow, this.report(), true);
            chatBubble.run();
        });
        this.feedButton = null;
        if (!this.fedThisVisit) {
            this.feedButton = this.form.addDialogueOption(new LocalMessage("misc", "swhdorianfeed"), () -> {
                this.feedAction.runAndSend();
                this.build(container, containerForm);
            });
            this.feedButton.setActive(carried(container.client.playerMob) > 0);
        }
        this.form.addDialogueOption(new LocalMessage("ui", "backbutton"),
                () -> containerForm.makeCurrent(containerForm.dialogueForm));
    }

    /** "Last night": what he drained, whether one got up again, whom he bit. */
    protected LocalMessage report() {
        if (this.drained == 0 && this.thralls == 0 && (this.bitten == null || this.bitten.isEmpty())) {
            return new LocalMessage("misc", "swhdorianreportquiet");
        }
        LocalMessage line = new LocalMessage("misc", "swhdorianreport",
                "drained", String.valueOf(this.drained),
                "thralls", String.valueOf(this.thralls));
        if (this.bitten != null && !this.bitten.isEmpty()) {
            return new LocalMessage("misc", "swhdorianreportbit", "report", line, "names", this.bitten);
        }
        return line;
    }

    /** Four words for the thirst meter. Literal keys for the locale audit. */
    public static String thirstKey(int stage) {
        switch (stage) {
            case 0: return "swhdorianthirst0";
            case 1: return "swhdorianthirst1";
            case 2: return "swhdorianthirst2";
            default: return "swhdorianthirst3";
        }
    }

    @Override
    public <T extends ShopContainer> void onWindowResized(GameWindow window, ShopContainer container,
                                                          ShopContainerForm<?> containerForm) {
        super.onWindowResized(window, container, containerForm);
        if (this.form != null) {
            ContainerComponent.setPosFocus(this.form);
        }
    }
}
