package stairwaytoheaven.settlement;

import java.util.ArrayList;
import java.util.List;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.presets.containerComponent.mob.DialogueForm;
import necesse.gfx.forms.presets.containerComponent.mob.ShopContainerForm;
import necesse.inventory.container.customAction.PointCustomAction;
import necesse.inventory.container.mob.ShopContainer;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;
import necesse.level.maps.levelData.settlementData.settler.dialogues.SettlerDialogue;
import stairwaytoheaven.mobs.TherapistHumanMob;

/**
 * "Who is in therapy?" — the four places, in the settler talk menu.
 *
 * <h2>Shape</h2>
 *
 * Built on {@code CollectorSettlerDialogue}, which is vanilla's own worked
 * example of a dialogue that carries state and buttons:
 *
 * <ul>
 * <li>the SERVER constructs this with the settlement in hand and writes what
 *     the player may choose from into {@link #setupContainerPacket};</li>
 * <li>the CLIENT re-creates it through {@code SettlerDialogueRegistry} using
 *     the {@code (HumanMob)} constructor and reads that list back — which is
 *     why the second constructor exists and must stay;</li>
 * <li>clicks travel back as a registered custom action, never as a direct
 *     call.</li>
 * </ul>
 *
 * <p>Two sub-forms rather than one: the places, and the roster you pick from
 * for one place. A single list would have to be sixteen buttons wide.
 */
public class TherapySlotsDialogue extends SettlerDialogue {

    /** What is in each place, by mob unique ID; 0 is free. */
    protected final int[] slots = new int[SkyTherapy.SLOTS];

    /** Everyone in this settlement who could take a place. */
    protected int[] candidateIDs = new int[0];
    protected String[] candidateNames = new String[0];

    protected PointCustomAction assignAction;
    protected DialogueForm slotsForm;
    protected DialogueForm pickForm;

    /** Client-side reconstruction. Required by {@code SettlerDialogueRegistry}. */
    public TherapySlotsDialogue(HumanMob mob) {
        super(mob);
    }

    /** Server-side construction, with the settlement to read the roster from. */
    public TherapySlotsDialogue(TherapistHumanMob mob, ServerSettlementData data) {
        this(mob);
        for (int slot = 0; slot < this.slots.length; slot++) {
            this.slots[slot] = mob.getPatient(slot);
        }
        List<Integer> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (LevelSettler levelSettler : data.getSettlers()) {
            SettlerMob settlerMob = levelSettler.getMob();
            Mob other = settlerMob == null ? null : settlerMob.getMob();
            if (!(other instanceof HumanMob) || other == mob) {
                continue;
            }
            ids.add(levelSettler.mobUniqueID);
            names.add(((HumanMob) other).getSettlerName());
        }
        this.candidateIDs = new int[ids.size()];
        this.candidateNames = new String[names.size()];
        for (int i = 0; i < ids.size(); i++) {
            this.candidateIDs[i] = ids.get(i);
            this.candidateNames[i] = names.get(i);
        }
    }

    @Override
    protected void setupContainerPacket(PacketWriter writer) {
        super.setupContainerPacket(writer);
        for (int slot : this.slots) {
            writer.putNextInt(slot);
        }
        writer.putNextShortUnsigned(this.candidateIDs.length);
        for (int i = 0; i < this.candidateIDs.length; i++) {
            writer.putNextInt(this.candidateIDs[i]);
            writer.putNextString(this.candidateNames[i] == null ? "" : this.candidateNames[i]);
        }
    }

    @Override
    protected void applyContainerPacket(PacketReader reader) {
        super.applyContainerPacket(reader);
        for (int slot = 0; slot < this.slots.length; slot++) {
            this.slots[slot] = reader.getNextInt();
        }
        int size = reader.getNextShortUnsigned();
        this.candidateIDs = new int[size];
        this.candidateNames = new String[size];
        for (int i = 0; i < size; i++) {
            this.candidateIDs[i] = reader.getNextInt();
            this.candidateNames[i] = reader.getNextString();
        }
    }

    @Override
    public void init(final ShopContainer container) {
        super.init(container);
        this.assignAction = container.registerAction(new PointCustomAction() {
            @Override
            protected void run(int slot, int mobUniqueID) {
                if (slot < 0 || slot >= TherapySlotsDialogue.this.slots.length) {
                    return;
                }
                if (TherapySlotsDialogue.this.settlerMob.isServer()) {
                    // Server has the last word on WHO may be treated: a client
                    // could otherwise name any mob in the world.
                    if (!container.hasSettlerAccess
                            || !(TherapySlotsDialogue.this.settlerMob instanceof TherapistHumanMob)) {
                        return;
                    }
                    TherapistHumanMob therapist = (TherapistHumanMob) TherapySlotsDialogue.this.settlerMob;
                    if (mobUniqueID != 0 && therapist.findSettlerMob(mobUniqueID) == null) {
                        return;
                    }
                    therapist.setPatient(slot, mobUniqueID);
                }
                // Both sides keep the same picture of the four places, so the
                // menu redraws correctly without waiting for a round trip.
                if (mobUniqueID != 0) {
                    for (int i = 0; i < TherapySlotsDialogue.this.slots.length; i++) {
                        if (i != slot && TherapySlotsDialogue.this.slots[i] == mobUniqueID) {
                            TherapySlotsDialogue.this.slots[i] = 0;
                        }
                    }
                }
                TherapySlotsDialogue.this.slots[slot] = mobUniqueID;
            }
        });
    }

    @Override
    public void initForm(ShopContainer container, ShopContainerForm<?> containerForm) {
        super.initForm(container, containerForm);
        this.slotsForm = containerForm.addComponent(new DialogueForm("swhtherapyslots",
                containerForm.width, containerForm.minHeight, containerForm.maxHeight, true));
        this.pickForm = containerForm.addComponent(new DialogueForm("swhtherapypick",
                containerForm.width, containerForm.minHeight, containerForm.maxHeight, true));
    }

    @Override
    public void setupDialogueOptions(final ShopContainer container,
                                     final ShopContainerForm<?> containerForm) {
        super.setupDialogueOptions(container, containerForm);
        if (!container.hasSettlerAccess || this.slotsForm == null) {
            return;
        }
        this.buildSlotsForm(container, containerForm);
        containerForm.dialogueForm.addDialogueOption(
                new LocalMessage("misc", "swhtherapyoption"),
                () -> containerForm.makeCurrent(this.slotsForm));
    }

    /** The four places, one line each, rebuilt whenever one of them changes. */
    protected void buildSlotsForm(final ShopContainer container,
                                  final ShopContainerForm<?> containerForm) {
        this.slotsForm.reset(container.humanShop, true, container.romanceLevel,
                (contentBox, flow) -> {
                    Runnable chatBubble = DialogueForm.startChatBubble(contentBox, flow);
                    DialogueForm.addText(contentBox, flow,
                            new LocalMessage("misc", "swhtherapyintro", "percent",
                                    Integer.toString(SkyTherapy.BONUS_PERCENT)), true);
                    chatBubble.run();
                });
        for (int slot = 0; slot < this.slots.length; slot++) {
            final int index = slot;
            this.slotsForm.addDialogueOption(this.slotLabel(slot), () -> {
                this.buildPickForm(container, containerForm, index);
                containerForm.makeCurrent(this.pickForm);
                containerForm.onWindowResized(WindowManager.getWindow());
            });
        }
        this.slotsForm.addDialogueOption(new LocalMessage("ui", "backbutton"),
                () -> containerForm.makeCurrent(containerForm.dialogueForm));
    }

    /**
     * "Place 1: Ada" or "Place 1: free".
     *
     * <p>Two keys rather than one with an empty replacement: an empty chair is
     * the state the player meets FIRST, and "Place 1: " with nothing after the
     * colon reads as a bug rather than as an invitation.
     */
    protected GameMessage slotLabel(int slot) {
        String name = this.nameOf(this.slots[slot]);
        if (name == null || name.isEmpty()) {
            return new LocalMessage("misc", "swhtherapyslotfree",
                    "number", Integer.toString(slot + 1));
        }
        return new LocalMessage("misc", "swhtherapyslot",
                "number", Integer.toString(slot + 1),
                "name", name);
    }

    /** The roster, for one place. */
    protected void buildPickForm(final ShopContainer container,
                                 final ShopContainerForm<?> containerForm, final int slot) {
        this.pickForm.reset(container.humanShop, true, container.romanceLevel,
                (contentBox, flow) -> {
                    Runnable chatBubble = DialogueForm.startChatBubble(contentBox, flow);
                    DialogueForm.addText(contentBox, flow,
                            new LocalMessage("misc", "swhtherapypick"), true);
                    chatBubble.run();
                });
        if (this.slots[slot] != 0) {
            this.pickForm.addDialogueOption(new LocalMessage("misc", "swhtherapyclear"), () -> {
                this.assignAction.runAndSend(slot, 0);
                this.buildSlotsForm(container, containerForm);
                containerForm.makeCurrent(this.slotsForm);
                containerForm.onWindowResized(WindowManager.getWindow());
            });
        }
        for (int i = 0; i < this.candidateIDs.length; i++) {
            final int mobUniqueID = this.candidateIDs[i];
            GameMessage label = new StaticMessage(this.candidateNames[i]);
            this.pickForm.addDialogueOption(label, () -> {
                this.assignAction.runAndSend(slot, mobUniqueID);
                this.buildSlotsForm(container, containerForm);
                containerForm.makeCurrent(this.slotsForm);
                containerForm.onWindowResized(WindowManager.getWindow());
            });
        }
        this.pickForm.addDialogueOption(new LocalMessage("ui", "backbutton"), () -> {
            this.buildSlotsForm(container, containerForm);
            containerForm.makeCurrent(this.slotsForm);
            containerForm.onWindowResized(WindowManager.getWindow());
        });
    }

    protected String nameOf(int mobUniqueID) {
        if (mobUniqueID == 0) {
            return null;
        }
        for (int i = 0; i < this.candidateIDs.length; i++) {
            if (this.candidateIDs[i] == mobUniqueID) {
                return this.candidateNames[i];
            }
        }
        return null;
    }

    @Override
    public <T extends ShopContainer> void onWindowResized(GameWindow window, ShopContainer container,
                                                          ShopContainerForm<?> containerForm) {
        super.onWindowResized(window, container, containerForm);
        if (this.slotsForm != null) {
            ContainerComponent.setPosFocus(this.slotsForm);
        }
        if (this.pickForm != null) {
            ContainerComponent.setPosFocus(this.pickForm);
        }
    }
}
