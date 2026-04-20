package deborn.modelbrowser.gui;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import deborn.modelbrowser.ModelListData;
import deborn.modelbrowser.config.ModConfig;

public class ModelBrowserWidget {
    // UI Layout Constants
    private static final int ITEM_SIZE = 25;
    private static final int GRID_COLUMNS = 5;
    private static final int MAX_VISIBLE_ROWS = 4;
    private static final int SHIFT_LEFT_AMOUNT = 72;

    private static final int GRID_POSITION_X = 11;
    private static final int GRID_POSITION_Y = 32;

    private static final int PREV_PAGE_POSITION_X = 38;
    private static final int NEXT_PAGE_POSITION_X = 93;
    private static final int PAGE_BUTTONS_POSITION_Y = 137;

    private static final int PAGE_COUNT_POSITION_X = 64;
    private static final int PAGE_COUNT_POSITION_Y = 141;

    private static final int SEARCH_BOX_POSITION_X = 25;
    private static final int SEARCH_BOX_POSITION_Y = 13;

    // Textures and Sprites
    private static final Identifier RECIPE_BOOK_TEXTURE = Identifier
            .withDefaultNamespace("textures/gui/recipe_book.png");
    private static final Identifier SLOT_CRAFTABLE_SPRITE = Identifier
            .withDefaultNamespace("textures/gui/sprites/recipe_book/slot_craftable.png");
    private static final WidgetSprites PAGE_FORWARD_SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("recipe_book/page_forward"),
            Identifier.withDefaultNamespace("recipe_book/page_forward_highlighted"));
    private static final WidgetSprites PAGE_BACKWARD_SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("recipe_book/page_backward"),
            Identifier.withDefaultNamespace("recipe_book/page_backward_highlighted"));
    private static final Component NEXT_PAGE_TEXT = Component.translatable("gui.recipebook.next_page");
    private static final Component PREVIOUS_PAGE_TEXT = Component.translatable("gui.recipebook.previous_page");
    private static final Component SEARCH_HINT_TEXT = Component.translatable("gui.recipebook.search_hint")
            .withStyle(EditBox.SEARCH_HINT_STYLE);

    // State
    private String lastSearch = "";
    private int pageCount = 0;
    private int currentPage = 0;

    // UI Components
    private EditBox searchField;
    private ScreenRectangle searchFieldRect;
    private ImageButton nextPageButton;
    private ImageButton prevPageButton;

    // References
    private final Minecraft client;
    private int screenLeft;
    private int screenTop;
    private int screenWidth;
    private int screenHeight;
    private Font textRenderer;

    public ModelBrowserWidget(Minecraft client, int screenWidth, int screenHeight) {
        this.client = client;
        this.textRenderer = client.font;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.screenLeft = (screenWidth - 176) / 2;
        this.screenTop = (screenHeight - 166) / 2;
    }

    public void initialize() {
        int searchX = screenLeft + SEARCH_BOX_POSITION_X - SHIFT_LEFT_AMOUNT;
        int searchY = screenTop + SEARCH_BOX_POSITION_Y;

        searchField = new EditBox(textRenderer, searchX, searchY, 109, 14, Component.translatable("itemGroup.search"));
        searchField.setMaxLength(50);
        searchField.setResponder(this::filterModelStacks);
        searchField.setHint(SEARCH_HINT_TEXT);
        searchField.visible = ModConfig.INSTANCE.isModelBrowserOpen;
        updateSearchRect();

        int pagePrevX = screenLeft + PREV_PAGE_POSITION_X - SHIFT_LEFT_AMOUNT;
        int pageNextX = screenLeft + NEXT_PAGE_POSITION_X - SHIFT_LEFT_AMOUNT;
        int pageButtonY = screenTop + PAGE_BUTTONS_POSITION_Y;

        prevPageButton = new ImageButton(
                pagePrevX,
                pageButtonY,
                12,
                17,
                PAGE_BACKWARD_SPRITES,
                b -> previousPage(),
                PREVIOUS_PAGE_TEXT);

        nextPageButton = new ImageButton(
                pageNextX,
                pageButtonY,
                12,
                17,
                PAGE_FORWARD_SPRITES,
                b -> nextPage(),
                NEXT_PAGE_TEXT);

        prevPageButton.visible = false;
        nextPageButton.visible = false;

        // filterModelStacks("");
    }

    private void updateSearchRect() {
        searchFieldRect = new ScreenRectangle(
                searchField.getX() - 17,
                searchField.getY(),
                searchField.getWidth() + 17,
                searchField.getHeight());
    }

    public void toggle() {
        ModConfig.INSTANCE.isModelBrowserOpen = !ModConfig.INSTANCE.isModelBrowserOpen;

        searchField.visible = ModConfig.INSTANCE.isModelBrowserOpen;
        searchField.active = ModConfig.INSTANCE.isModelBrowserOpen;
    }

    public boolean handleClick(MouseButtonEvent click, boolean doubled, AbstractContainerMenu handler,
            EditBox nameField, Screen screen) {
        if (prevPageButton.mouseClicked(click, doubled)) {
            return true;
        }
        if (nextPageButton.mouseClicked(click, doubled)) {
            return true;
        }

        ItemStack clickedStack = getItemAtMouse((int) click.x(), (int) click.y());
        if (clickedStack != null) {
            Component name = clickedStack.get(DataComponents.CUSTOM_NAME);
            Identifier modelId = clickedStack.get(DataComponents.ITEM_MODEL);
            if (name != null) {
                AbstractWidget.playButtonClickSound(client.getSoundManager());
                int invSlot = findMatchingInventorySlot(handler, clickedStack);
                if (invSlot == -1) {
                    return true; // still consume the click even if no match
                }

                client.gameMode.handleContainerInput(
                        handler.containerId,
                        invSlot,
                        0,
                        ContainerInput.PICKUP,
                        client.player);
                client.gameMode.handleContainerInput(
                        handler.containerId,
                        invSlot,
                        0,
                        ContainerInput.PICKUP_ALL,
                        client.player);
                boolean anvilSlotHadItem = handler.getSlot(0).hasItem();
                client.gameMode.handleContainerInput(
                        handler.containerId,
                        0,
                        0,
                        ContainerInput.PICKUP,
                        client.player);
                if (anvilSlotHadItem) {
                    client.gameMode.handleContainerInput(
                            handler.containerId,
                            invSlot,
                            0,
                            ContainerInput.PICKUP,
                            client.player);
                }

                nameField.setValue("");
                nameField.setValue(name.getString());

                return true;
            } else if (modelId != null) {
                AbstractWidget.playButtonClickSound(client.getSoundManager());
                if (handler.getSlot(0).hasItem()) {
                    nameField.setValue("");
                    nameField.setValue(modelId.toString());
                    return true;
                }
            }
        }

        if (searchField != null) {
            boolean bl = searchFieldRect != null && searchFieldRect.containsPoint((int) click.x(), (int) click.y());
            if (bl) {
                screen.setFocused(searchField);
                searchField.setFocused(true);
                return true;
            }
            searchField.setFocused(false);
        }

        // Consume clicks within the widget bounds to prevent vanilla behavior
        if (isClickInWidgetBounds((int) click.x(), (int) click.y())) {
            return true;
        }

        return false;
    }

    public boolean handleRelease(MouseButtonEvent click) {
        return isClickInWidgetBounds((int) click.x(), (int) click.y());
    }

    private boolean isClickInWidgetBounds(int mouseX, int mouseY) {
        int x = screenLeft - SHIFT_LEFT_AMOUNT;
        int y = screenTop;
        return mouseX >= x && mouseX < x + 147 && mouseY >= y && mouseY < y + 166;
    }

    private int findMatchingInventorySlot(AbstractContainerMenu handler, ItemStack target) {
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.getSlot(i);
            if (slot.hasItem() && ItemStack.isSameItem(slot.getItem(), target)) {
                return i;
            }
        }
        return -1;
    }

    public void filterModelStacks(String text) {
        if (text.equals(lastSearch))
            return;
        lastSearch = text;

        ModelListData.filter(text);
        currentPage = 0;
    }

    private void previousPage() {
        if (currentPage > 0)
            currentPage--;
    }

    private void nextPage() {
        if (currentPage < pageCount - 1)
            currentPage++;
    }

    public ItemStack getItemAtMouse(int mouseX, int mouseY) {
        if (!this.isOpen())
            return null;

        List<ItemStack> stacks = ModelListData.getFiltered();

        int itemsPerPage = GRID_COLUMNS * MAX_VISIBLE_ROWS;
        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, stacks.size());

        for (int i = start; i < end; i++) {
            int index = i - start;
            int row = index / GRID_COLUMNS;
            int col = index % GRID_COLUMNS;

            int x = screenLeft + GRID_POSITION_X + col * ITEM_SIZE - SHIFT_LEFT_AMOUNT;
            int y = screenTop + GRID_POSITION_Y + row * ITEM_SIZE;

            if (mouseX >= x && mouseX <= x + ITEM_SIZE &&
                    mouseY >= y && mouseY <= y + ITEM_SIZE) {
                return stacks.get(i);
            }
        }

        return null;
    }

    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float deltaTicks) {
        if (this.isOpen()) {
            prevPageButton.extractRenderState(ctx, mouseX, mouseY, deltaTicks);
            nextPageButton.extractRenderState(ctx, mouseX, mouseY, deltaTicks);
            searchField.extractRenderState(ctx, mouseX, mouseY, deltaTicks);
            if (this.pageCount > 1) {
                Component text = Component.translatable("gui.recipebook.page",
                        new Object[] { this.currentPage + 1, this.pageCount });
                int x = screenLeft + PAGE_COUNT_POSITION_X - SHIFT_LEFT_AMOUNT;
                int y = screenTop + PAGE_COUNT_POSITION_Y;
                ctx.text(textRenderer, text, x, y, CommonColors.WHITE);
            }
        }
    }

    public void drawBackground(GuiGraphicsExtractor ctx) {
        if (!this.isOpen())
            return;

        int x = screenLeft - SHIFT_LEFT_AMOUNT;
        int y = screenTop;

        ctx.blit(RenderPipelines.GUI_TEXTURED, RECIPE_BOOK_TEXTURE, x, y,
                1.0F, 1.0F, 147, 166, 256, 256);
    }

    public void drawForeground(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        if (!this.isOpen())
            return;

        List<ItemStack> stacks = ModelListData.getFiltered();

        int itemsPerPage = GRID_COLUMNS * MAX_VISIBLE_ROWS;
        pageCount = (int) Math.ceil(stacks.size() / (double) itemsPerPage);

        currentPage = Math.min(currentPage, Math.max(0, pageCount - 1));

        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, stacks.size());

        for (int i = start; i < end; i++) {
            int index = i - start;
            int row = index / GRID_COLUMNS;
            int col = index % GRID_COLUMNS;

            int x = GRID_POSITION_X + col * ITEM_SIZE - SHIFT_LEFT_AMOUNT - 77;
            int y = GRID_POSITION_Y + row * ITEM_SIZE;

            ctx.blit(RenderPipelines.GUI_TEXTURED, SLOT_CRAFTABLE_SPRITE,
                    x, y, 0, 0, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE);

            ctx.item(stacks.get(i), x + 4, y + 4);
        }

        prevPageButton.visible = currentPage > 0;
        nextPageButton.visible = currentPage < pageCount - 1;

        ItemStack hovered = getItemAtMouse(mouseX, mouseY);
        if (hovered != null) {
            Component name = hovered.get(DataComponents.CUSTOM_NAME);
            Identifier modelId = hovered.get(DataComponents.ITEM_MODEL);
            if (name != null) {
                ctx.setTooltipForNextFrame(textRenderer, name, mouseX, mouseY);
            } else if (modelId != null) {
                ctx.setTooltipForNextFrame(textRenderer, Component.literal(modelId.toString()), mouseX, mouseY);
            }
        }
    }

    public EditBox getSearchField() {
        return searchField;
    }

    public ScreenRectangle getSearchFieldRect() {
        return searchFieldRect;
    }

    public ImageButton getNextPageButton() {
        return nextPageButton;
    }

    public ImageButton getPrevPageButton() {
        return prevPageButton;
    }

    public boolean isOpen() {
        return ModConfig.INSTANCE.isModelBrowserOpen;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int page) {
        this.currentPage = page;
    }
}
