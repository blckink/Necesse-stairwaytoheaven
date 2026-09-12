package stairwaytoheaven.settlement;

import java.util.ArrayList;
import java.util.List;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.SettlerPersonalityRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.components.FormDialogueOption;
import necesse.gfx.forms.presets.containerComponent.mob.DialogueForm;
import necesse.gfx.forms.presets.containerComponent.mob.ShopContainerForm;
import necesse.inventory.container.customAction.PointCustomAction;
import necesse.inventory.container.mob.ShopContainer;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Recipe;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;
import necesse.level.maps.levelData.settlementData.settler.dialogues.SettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.personalities.SettlerPersonality;
import stairwaytoheaven.mobs.TherapistHumanMob;

/**
 * "Work on someone's character" — the paid trait swap.
 *
 * <p>Any settler of the therapist's settlement, whether or not they hold a
 * therapy place; one trait they actually have; 50 000 coins; and a replacement
 * the player does not get to choose and cannot see beforehand. The roll and the
 * charge both happen on the server — see {@code SkyTherapy.rollReplacement} for
 * why picking "any other personality" would be wrong — and the outcome comes
 * back as a {@link TraitSwapResultEvent}, which is the only thing that tells the
 * player what they bought.
 *
 * <p>The price is checked and taken through vanilla's own recipe machinery
 * ({@code Container.canCraftRecipe} / {@code Recipe.craft}), the same path
 * vanilla's Collector gift uses. Hand-rolled coin removal is how this mod once
 * shipped a settler who charged for being talked to.
 */
public class TraitTherapyDialogue extends SettlerDialogue {

    protected int[] settlerIDs = new int[0];
    protected String[] settlerNames = new String[0];
    /** Per settler, the IDs of the traits they hold right now. */
    protected int[][] settlerTraits = new int[0][];

    protected PointCustomAction rerollAction;
    protected DialogueForm chooseForm;
    protected DialogueForm traitForm;
    protected DialogueForm confirmForm;
    protected DialogueForm resultForm;
    protected FormDialogueOption confirmButton;
    protected TraitSwapResultEvent lastResult;

    /** Client-side reconstruction. Required by {@code SettlerDialogueRegistry}. */
    public TraitTherapyDialogue(HumanMob mob) {
        super(mob);
    }

    /** Server-side construction, with the settlement to read the roster from. */
    public TraitTherapyDialogue(TherapistHumanMob mob, ServerSettlementData data) {
        this(mob);
        List<Integer> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<int[]> traits = new ArrayList<>();
        for (LevelSettler levelSettler : data.getSettlers()) {
            SettlerMob settlerMob = levelSettler.getMob();
            Mob other = settlerMob == null ? null : settlerMob.getMob();
            if (!(other instanceof HumanMob)) {
                continue;
            }
            HumanMob human = (HumanMob) other;
            ArrayList<SettlerPersonality> personalities = human.getPersonalities();
            if (personalities == null || personalities.isEmpty()) {
                continue;
            }
            int[] traitIDs = new int[personalities.size()];
            for (int i = 0; i < traitIDs.length; i++) {
                traitIDs[i] = personalities.get(i).getID();
            }
            ids.add(levelSettler.mobUniqueID);
            names.add(human.getSettlerName());
            traits.add(traitIDs);
        }
        this.settlerIDs = new int[ids.size()];
        this.settlerNames = new String[names.size()];
        this.settlerTraits = new int[traits.size()][];
        for (int i = 0; i < ids.size(); i++) {
            this.settlerIDs[i] = ids.get(i);
            this.settlerNames[i] = names.get(i);
            this.settlerTraits[i] = traits.get(i);
        }
    }

    @Override
    protected void setupContainerPacket(PacketWriter writer) {
        super.setupContainerPacket(writer);
        writer.putNextShortUnsigned(this.settlerIDs.length);
        for (int i = 0; i < this.settlerIDs.length; i++) {
            writer.putNextInt(this.settlerIDs[i]);
            writer.putNextString(this.settlerNames[i] == null ? "" : this.settlerNames[i]);
            writer.putNextShortUnsigned(this.settlerTraits[i].length);
            for (int traitID : this.settlerTraits[i]) {
                writer.putNextShortUnsigned(traitID);
            }
        }
    }

    @Override
    protected void applyContainerPacket(PacketReader reader) {
        super.applyContainerPacket(reader);
        int size = reader.getNextShortUnsigned();
        this.settlerIDs = new int[size];
        this.settlerNames = new String[size];
        this.settlerTraits = new int[size][];
        for (int i = 0; i < size; i++) {
            this.settlerIDs[i] = reader.getNextInt();
            this.settlerNames[i] = reader.getNextString();
            int traitCount = reader.getNextShortUnsigned();
            this.settlerTraits[i] = new int[traitCount];
            for (int t = 0; t < traitCount; t++) {
                this.settlerTraits[i][t] = reader.getNextShortUnsigned();
            }
        }
    }

    @Override
    public void init(final ShopContainer container) {
        super.init(container);
        this.rerollAction = container.registerAction(new PointCustomAction() {
            @Override
            protected void run(int mobUniqueID, int personalityID) {
                if (!TraitTherapyDialogue.this.settlerMob.isServer()) {
                    // The client learns the outcome from the event, never by
                    // guessing it — that is the whole design of the service.
                    return;
                }
                TraitTherapyDialogue.this.runSwap(container, mobUniqueID, personalityID);
            }
        });
    }

    /**
     * The swap itself, server side.
     *
     * <p>Order matters: affordability is checked before the roll, the roll
     * before the swap, and the coins are taken only once the swap has actually
     * happened. A failure at any step leaves the settler and the purse exactly
     * as they were, and says which it was.
     */
    protected void runSwap(ShopContainer container, int mobUniqueID, int personalityID) {
        if (!container.hasSettlerAccess || !(this.settlerMob instanceof TherapistHumanMob)) {
            return;
        }
        TherapistHumanMob therapist = (TherapistHumanMob) this.settlerMob;
        HumanMob target = therapist.findSettlerMob(mobUniqueID);
        if (target == null) {
            return;
        }
        SettlerPersonality replace = null;
        for (SettlerPersonality personality : target.getPersonalities()) {
            if (personality.getID() == personalityID) {
                replace = personality;
                break;
            }
        }
        if (replace == null) {
            return;
        }
        Ingredient[] cost = cost();
        if (!container.canCraftRecipe(cost, container.getCraftInventories(), true, null).canCraft()) {
            this.sendResult(container, mobUniqueID, personalityID, 0,
                    TraitSwapResultEvent.FAILED_NO_COINS);
            return;
        }
        SettlerPersonality replacement = SkyTherapy.rollReplacement(target, replace, new GameRandom());
        if (replacement == null || !SkyTherapy.swapPersonality(target, replace, replacement)) {
            this.sendResult(container, mobUniqueID, personalityID, 0,
                    TraitSwapResultEvent.FAILED_NO_TRAIT);
            return;
        }
        Recipe.craft(cost, target.getLevel(), container.client.playerMob,
                container.getCraftInventories(), null);
        this.rememberSwap(mobUniqueID, personalityID, replacement.getID());
        this.sendResult(container, mobUniqueID, personalityID, replacement.getID(), 0);
    }

    protected void sendResult(ShopContainer container, int mobUniqueID, int oldID, int newID,
                              int failure) {
        if (container.client.getServerClient() == null) {
            return;
        }
        new TraitSwapResultEvent(mobUniqueID, oldID, newID, failure)
                .applyAndSendToClient(container.client.getServerClient());
    }

    /** 50 000 coins, as a recipe ingredient so vanilla can count and take them. */
    protected static Ingredient[] cost() {
        return new Ingredient[]{new Ingredient("coin", SkyTherapy.TRAIT_SWAP_PRICE)};
    }

    /** Keep the roster this dialogue was built with in step with the swap. */
    protected void rememberSwap(int mobUniqueID, int oldID, int newID) {
        for (int i = 0; i < this.settlerIDs.length; i++) {
            if (this.settlerIDs[i] != mobUniqueID) {
                continue;
            }
            for (int t = 0; t < this.settlerTraits[i].length; t++) {
                if (this.settlerTraits[i][t] == oldID) {
                    this.settlerTraits[i][t] = newID;
                    return;
                }
            }
        }
    }

    // --- the forms -------------------------------------------------------

    @Override
    public void initForm(final ShopContainer container, final ShopContainerForm<?> containerForm) {
        super.initForm(container, containerForm);
        this.chooseForm = containerForm.addComponent(new DialogueForm("swhtraitchoose",
                containerForm.width, containerForm.minHeight, containerForm.maxHeight, true));
        this.traitForm = containerForm.addComponent(new DialogueForm("swhtraitpick",
                containerForm.width, containerForm.minHeight, containerForm.maxHeight, true));
        this.confirmForm = containerForm.addComponent(new DialogueForm("swhtraitconfirm",
                containerForm.width, containerForm.minHeight, containerForm.maxHeight, true));
        this.resultForm = containerForm.addComponent(new DialogueForm("swhtraitresult",
                containerForm.width, containerForm.minHeight, containerForm.maxHeight, true));
        container.onEvent(TraitSwapResultEvent.class, event -> {
            this.lastResult = event;
            if (event.failure == 0) {
                this.rememberSwap(event.mobUniqueID, event.oldPersonalityID, event.newPersonalityID);
            }
            this.buildResultForm(container, containerForm);
            containerForm.makeCurrent(this.resultForm);
            containerForm.onWindowResized(WindowManager.getWindow());
        });
    }

    @Override
    public void setupDialogueOptions(final ShopContainer container,
                                     final ShopContainerForm<?> containerForm) {
        super.setupDialogueOptions(container, containerForm);
        if (!container.hasSettlerAccess || this.chooseForm == null || this.settlerIDs.length == 0) {
            return;
        }
        this.buildChooseForm(container, containerForm);
        containerForm.dialogueForm.addDialogueOption(
                new LocalMessage("misc", "swhtraitoption", "price",
                        Integer.toString(SkyTherapy.TRAIT_SWAP_PRICE)),
                () -> containerForm.makeCurrent(this.chooseForm));
    }

    protected void buildChooseForm(final ShopContainer container,
                                   final ShopContainerForm<?> containerForm) {
        this.chooseForm.reset(container.humanShop, true, container.romanceLevel,
                (contentBox, flow) -> {
                    Runnable chatBubble = DialogueForm.startChatBubble(contentBox, flow);
                    DialogueForm.addText(contentBox, flow,
                            new LocalMessage("misc", "swhtraitintro", "price",
                                    Integer.toString(SkyTherapy.TRAIT_SWAP_PRICE)), true);
                    chatBubble.run();
                });
        for (int i = 0; i < this.settlerIDs.length; i++) {
            final int index = i;
            this.chooseForm.addDialogueOption(new StaticMessage(this.settlerNames[i]), () -> {
                this.buildTraitForm(container, containerForm, index);
                containerForm.makeCurrent(this.traitForm);
                containerForm.onWindowResized(WindowManager.getWindow());
            });
        }
        this.chooseForm.addDialogueOption(new LocalMessage("ui", "backbutton"),
                () -> containerForm.makeCurrent(containerForm.dialogueForm));
    }

    protected void buildTraitForm(final ShopContainer container,
                                  final ShopContainerForm<?> containerForm, final int index) {
        final String name = this.settlerNames[index];
        this.traitForm.reset(container.humanShop, true, container.romanceLevel,
                (contentBox, flow) -> {
                    Runnable chatBubble = DialogueForm.startChatBubble(contentBox, flow);
                    DialogueForm.addText(contentBox, flow,
                            new LocalMessage("misc", "swhtraitwhich", "name", name), true);
                    chatBubble.run();
                });
        for (int traitID : this.settlerTraits[index]) {
            final int id = traitID;
            this.traitForm.addDialogueOption(traitName(id), () -> {
                this.buildConfirmForm(container, containerForm, index, id);
                containerForm.makeCurrent(this.confirmForm);
                containerForm.onWindowResized(WindowManager.getWindow());
            });
        }
        this.traitForm.addDialogueOption(new LocalMessage("ui", "backbutton"), () -> {
            this.buildChooseForm(container, containerForm);
            containerForm.makeCurrent(this.chooseForm);
            containerForm.onWindowResized(WindowManager.getWindow());
        });
    }

    protected void buildConfirmForm(final ShopContainer container,
                                    final ShopContainerForm<?> containerForm, final int index,
                                    final int traitID) {
        final int mobUniqueID = this.settlerIDs[index];
        final String name = this.settlerNames[index];
        this.confirmForm.reset(container.humanShop, true, container.romanceLevel,
                (contentBox, flow) -> {
                    Runnable chatBubble = DialogueForm.startChatBubble(contentBox, flow);
                    DialogueForm.addText(contentBox, flow,
                            new LocalMessage("misc", "swhtraitconfirm",
                                    "name", name,
                                    "trait", traitName(traitID),
                                    "price", Integer.toString(SkyTherapy.TRAIT_SWAP_PRICE)), true);
                    chatBubble.run();
                });
        this.confirmButton = this.confirmForm.addDialogueOption(
                new LocalMessage("misc", "swhtraitdoit"),
                () -> this.rerollAction.runAndSend(mobUniqueID, traitID));
        this.confirmButton.setActive(
                container.canCraftRecipe(cost(), container.getCraftInventories(), true, null)
                        .canCraft());
        this.confirmForm.addDialogueOption(new LocalMessage("ui", "backbutton"), () -> {
            this.buildTraitForm(container, containerForm, index);
            containerForm.makeCurrent(this.traitForm);
            containerForm.onWindowResized(WindowManager.getWindow());
        });
    }

    protected void buildResultForm(final ShopContainer container,
                                   final ShopContainerForm<?> containerForm) {
        final TraitSwapResultEvent result = this.lastResult;
        this.resultForm.reset(container.humanShop, true, container.romanceLevel,
                (contentBox, flow) -> {
                    Runnable chatBubble = DialogueForm.startChatBubble(contentBox, flow);
                    DialogueForm.addText(contentBox, flow, resultMessage(result), true);
                    chatBubble.run();
                });
        this.resultForm.addDialogueOption(new LocalMessage("ui", "backbutton"), () -> {
            this.buildChooseForm(container, containerForm);
            containerForm.makeCurrent(this.chooseForm);
            containerForm.onWindowResized(WindowManager.getWindow());
        });
    }

    protected GameMessage resultMessage(TraitSwapResultEvent result) {
        if (result == null) {
            return new LocalMessage("misc", "swhtraitnotrait");
        }
        if (result.failure == TraitSwapResultEvent.FAILED_NO_COINS) {
            return new LocalMessage("misc", "swhtraitnocoins", "price",
                    Integer.toString(SkyTherapy.TRAIT_SWAP_PRICE));
        }
        if (result.failure != 0) {
            return new LocalMessage("misc", "swhtraitnotrait");
        }
        return new LocalMessage("misc", "swhtraitdone",
                "name", this.nameOf(result.mobUniqueID),
                "old", traitName(result.oldPersonalityID),
                "new", traitName(result.newPersonalityID));
    }

    protected String nameOf(int mobUniqueID) {
        for (int i = 0; i < this.settlerIDs.length; i++) {
            if (this.settlerIDs[i] == mobUniqueID) {
                return this.settlerNames[i];
            }
        }
        return "";
    }

    /**
     * A trait's own name, exactly as the game writes it elsewhere.
     *
     * <p>Built through the registry rather than from a string of our own so
     * that the furniture- and wall-set traits — which override
     * {@code getName()} to read "Oak enthusiast" — come out right, and so no
     * translation of a vanilla trait has to be repeated in this mod's locale.
     * A null mob is what {@code SettlerPersonalityRegistry.onRegister} itself
     * constructs with, so every personality tolerates it.
     */
    protected static GameMessage traitName(int personalityID) {
        SettlerPersonality personality =
                SettlerPersonalityRegistry.getNewSettlerPersonality(personalityID, null);
        if (personality == null) {
            return new StaticMessage("?");
        }
        return personality.getName();
    }

    @Override
    public <T extends ShopContainer> void onWindowResized(GameWindow window, ShopContainer container,
                                                          ShopContainerForm<?> containerForm) {
        super.onWindowResized(window, container, containerForm);
        if (this.chooseForm != null) {
            ContainerComponent.setPosFocus(this.chooseForm);
        }
        if (this.traitForm != null) {
            ContainerComponent.setPosFocus(this.traitForm);
        }
        if (this.confirmForm != null) {
            ContainerComponent.setPosFocus(this.confirmForm);
        }
        if (this.resultForm != null) {
            ContainerComponent.setPosFocus(this.resultForm);
        }
    }
}
