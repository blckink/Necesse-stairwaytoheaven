package stairwaytoheaven.pickupfilter;

import java.awt.Rectangle;

import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.components.localComponents.FormLocalTextButton;
import necesse.gfx.forms.presets.ItemCategoriesFilterForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;
import necesse.inventory.item.Item;
import necesse.inventory.itemFilter.ItemCategoriesFilter;

/**
 * The pickup filter menu, laid out like a chest's storage settings
 * ({@code SettlementStorageConfigForm}): title, search, "Allow all" /
 * "Clear all", the category tree with checkboxes only, and "Back".
 *
 * <p>Every change is saved to the cfg file and pushed to the server at once.
 */
public class PickupFilterForm extends Form {

    public static final int WIDTH = 400;
    public static final int HEIGHT = 380;

    private final Client client;
    /** The inventory form; this menu closes whenever that one is closed. */
    private final Form owner;
    private final ItemCategoriesFilterForm filterForm;

    public PickupFilterForm(Client client, Form owner) {
        super("swhpickupfilter", WIDTH, HEIGHT);
        this.client = client;
        this.owner = owner;

        this.addComponent(new FormLocalLabel("ui", "swh_pickupfilter", new FontOptions(20), -1, 5, 5));

        int searchY = 34;
        FormTextInput search = this.addComponent(new FormTextInput(4, searchY, FormInputSize.SIZE_24, WIDTH - 8, -1, 500));
        search.placeHolder = new LocalMessage("ui", "searchtip");

        int contentY = searchY + 30;
        FormContentBox box = this.addComponent(new FormContentBox(0, contentY, WIDTH, HEIGHT - contentY - 32));
        this.filterForm = box.addComponent(new ItemCategoriesFilterForm(4, 28, PickupFilter.client(),
                ItemCategoriesFilterForm.Mode.ONLY_ALLOWED,
                Settings.getItemCategoryExpandedSetting("swhpickupfilter"),
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
        search.onChange(e -> this.filterForm.setSearch(search.getText()));

        box.addComponent(new FormLocalTextButton("ui", "allowallbutton", 4, 0, WIDTH / 2 - 6,
                FormInputSize.SIZE_24, ButtonColor.BASE)).onClicked(e -> {
            ItemCategoriesFilter filter = this.filterForm.filter;
            if (!filter.master.isAllAllowed() || !filter.master.isAllDefault()) {
                filter.master.setAllowed(true);
                this.filterForm.updateAllButtons();
                this.changed();
            }
        });
        box.addComponent(new FormLocalTextButton("ui", "clearallbutton", WIDTH / 2 + 2, 0, WIDTH / 2 - 6,
                FormInputSize.SIZE_24, ButtonColor.BASE)).onClicked(e -> {
            ItemCategoriesFilter filter = this.filterForm.filter;
            if (filter.master.isAnyAllowed()) {
                filter.master.setAllowed(false);
                this.filterForm.updateAllButtons();
                this.changed();
            }
        });

        this.addComponent(new FormLocalTextButton("ui", "backbutton", WIDTH / 2 - 4, HEIGHT - 28, WIDTH / 2,
                FormInputSize.SIZE_24, ButtonColor.BASE)).onClicked(e -> this.setHidden(true));
    }

    @Override
    public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
        if (this.owner != null && this.owner.isHidden()) {
            this.setHidden(true);
            return;
        }
        super.draw(tickManager, perspective, renderBox);
    }

    private void changed() {
        PickupFilter.saveClient();
        PickupFilter.sendToServer(this.client);
    }
}
