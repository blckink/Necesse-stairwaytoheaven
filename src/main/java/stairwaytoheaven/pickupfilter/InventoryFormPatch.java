package stairwaytoheaven.pickupfilter;

import necesse.engine.GlobalData;
import necesse.engine.localization.Localization;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.client.Client;
import necesse.engine.state.MainGame;
import necesse.gfx.forms.MainGameFormManager;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.forms.position.FormRelativePosition;
import necesse.gfx.ui.ButtonColor;
import necesse.inventory.container.Container;
import net.bytebuddy.asm.Advice;

/**
 * Adds the "Pickup filter" button under the player inventory and the filter
 * menu it opens.
 *
 * <p>The button is a normal form button inside the inventory form, so it
 * hides with the inventory and is reachable with the controller / arrow-key
 * focus navigation like every other inventory control. Building the inventory
 * form happens on every join, which is also when the saved filter is pushed
 * to the server.
 */
@ModMethodPatch(target = MainGameFormManager.class, name = "setupInventoryForm", arguments = {Container.class})
public class InventoryFormPatch {

    public static final int BUTTON_HEIGHT = 28;

    private static PickupFilterForm lastFilterForm;

    @Advice.OnMethodExit
    static void onExit(@Advice.This MainGameFormManager manager) {
        InventoryFormPatch.attach(manager);
    }

    public static void attach(MainGameFormManager manager) {
        if (manager.inventory == null || !(GlobalData.getCurrentState() instanceof MainGame)) {
            return;
        }
        Client client = ((MainGame) GlobalData.getCurrentState()).getClient();
        int oldHeight = manager.inventory.getHeight();
        manager.inventory.setHeight(oldHeight + BUTTON_HEIGHT);
        FormTextButton button = manager.inventory.addComponent(new FormTextButton(
                Localization.translate("ui", "swh_pickupfilter"),
                Localization.translate("ui", "swh_pickupfiltertip"),
                4, oldHeight, manager.inventory.getWidth() - 8, FormInputSize.SIZE_24, ButtonColor.BASE));

        // The inventory form is rebuilt (e.g. when the inventory is extended),
        // so drop the menu that belonged to the previous one.
        if (lastFilterForm != null) {
            manager.removeComponent(lastFilterForm);
        }
        PickupFilterForm filterForm = manager.addComponent(new PickupFilterForm(client, manager.inventory));
        lastFilterForm = filterForm;
        filterForm.setHidden(true);
        filterForm.setPosition(new FormRelativePosition(manager.inventory, 0, -PickupFilterForm.HEIGHT - 4));

        button.onClicked(e -> filterForm.setHidden(!filterForm.isHidden()));

        PickupFilter.sendToServer(client);
    }
}
