package stairwaytoheaven.pickupfilter;

import necesse.engine.GlobalData;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.client.Client;
import necesse.engine.state.MainGame;
import necesse.engine.Settings;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.MainGameFormManager;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.forms.position.FormRelativePosition;
import necesse.gfx.ui.ButtonColor;
import necesse.inventory.container.Container;
import net.bytebuddy.asm.Advice;

/**
 * Adds a gear button to the small bar under the equipment (restock, quick
 * stack, sort, trash) and the pickup filter menu it opens.
 *
 * <p>The bar is vanilla's {@code leftQuickbar}: 128 px wide, buttons at x 6,
 * 32, 58 and the trash slot at 84. It is widened by one button slot, every
 * vanilla component is shifted right, and the gear takes x 6 — so it sits left
 * of the sort buttons and is reached with the controller / arrow-key focus
 * like its neighbours. {@code setupInventoryForm} positions the bar before
 * this runs, so the bar is re-anchored left of the inventory here; the
 * vanilla resize path ({@code updateInventoryFormPositions}) reads the new
 * width on its own.
 */
@ModMethodPatch(target = MainGameFormManager.class, name = "setupInventoryForm", arguments = {Container.class})
public class InventoryFormPatch {

    /** One 24 px button plus vanilla's 2 px gap. */
    public static final int SLOT = 26;

    private static PickupFilterForm lastFilterForm;

    @Advice.OnMethodExit
    static void onExit(@Advice.This MainGameFormManager manager) {
        InventoryFormPatch.attach(manager);
    }

    public static void attach(MainGameFormManager manager) {
        Form bar = manager.leftQuickbar;
        if (manager.inventory == null || bar == null || !(GlobalData.getCurrentState() instanceof MainGame)) {
            return;
        }
        Client client = ((MainGame) GlobalData.getCurrentState()).getClient();

        bar.setWidth(bar.getWidth() + SLOT);
        for (FormComponent component : bar.getComponentList()) {
            if (component instanceof FormPositionContainer) {
                FormPositionContainer positioned = (FormPositionContainer) component;
                positioned.setX(positioned.getX() + SLOT);
            }
        }
        FormContentIconButton gear = bar.addComponent(new FormContentIconButton(6, 12, FormInputSize.SIZE_24, ButtonColor.BASE,
                Settings.UI.container_storage_config, new GameMessage[]{new LocalMessage("ui", "swh_pickupfiltertip")}));
        gear.controllerFocusHashcode = "quickbarPickupFilter";
        bar.setPosition(manager.inventory.getX() - bar.getWidth() - Settings.UI.formSpacing, bar.getY());

        // The inventory forms are rebuilt (e.g. when the inventory is
        // extended), so drop the menu that belonged to the previous build.
        if (lastFilterForm != null) {
            manager.removeComponent(lastFilterForm);
        }
        PickupFilterForm filterForm = manager.addComponent(new PickupFilterForm(client, manager.inventory));
        lastFilterForm = filterForm;
        filterForm.setHidden(true);
        filterForm.setPosition(new FormRelativePosition(manager.inventory,
                () -> (manager.inventory.getWidth() - PickupFilterForm.WIDTH) / 2,
                () -> -PickupFilterForm.HEIGHT - Settings.UI.formSpacing));

        gear.onClicked(e -> filterForm.setHidden(!filterForm.isHidden()));

        PickupFilter.sendToServer(client);
    }
}
