package stairwaytoheaven.pickupfilter;

import java.awt.Rectangle;

import necesse.engine.GlobalData;
import necesse.engine.localization.Localization;
import necesse.engine.network.client.Client;
import necesse.engine.Settings;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.forms.presets.ItemCategoriesFilterForm;
import necesse.gfx.ui.ButtonColor;
import necesse.inventory.item.Item;
import necesse.inventory.itemFilter.ItemCategoriesFilter;

/**
 * The pickup filter menu: vanilla's storage filter tree, showing only the
 * allow/deny checkboxes (no amount limits), wrapped in a scroll box.
 *
 * <p>Every change is saved to the cfg file and pushed to the server at once.
 */
public class PickupFilterForm extends Form {

    public static final int WIDTH = 408;
    public static final int HEIGHT = 360;

    private final Client client;

    public PickupFilterForm(Client client) {
        super("swhpickupfilter", WIDTH, HEIGHT);
        this.client = client;
        FormContentBox box = this.addComponent(new FormContentBox(0, 0, WIDTH, HEIGHT - 32));
        box.addComponent(new ItemCategoriesFilterForm(4, 4, PickupFilter.client(), ItemCategoriesFilterForm.Mode.ONLY_ALLOWED,
                Settings.getItemCategoryExpandedSetting(PickupFilterForm.class),
                client == null || client.characterStats == null ? null : client.characterStats.items_obtained, true) {
            @Override
            public void onDimensionsChanged(int width, int height) {
                box.setContentBox(new Rectangle(0, 0, Math.max(WIDTH, width), this.getY() + height));
            }

            @Override
            public void onItemsChanged(Item[] items, boolean allowed) {
                PickupFilterForm.this.changed();
            }

            @Override
            public void onCategoryChanged(ItemCategoriesFilter.ItemCategoryFilter category, boolean allowed) {
                PickupFilterForm.this.changed();
            }
        });
        this.addComponent(new FormTextButton(Localization.translate("ui", "closebutton"), 4, HEIGHT - 28, WIDTH - 8,
                FormInputSize.SIZE_24, ButtonColor.BASE)).onClicked(e -> this.setHidden(true));
    }

    private void changed() {
        PickupFilter.saveClient();
        PickupFilter.sendToServer(this.client);
    }
}
