package deborn.modelbrowser.gui;

import java.util.List;

import deborn.modelbrowser.ModelListData;
import deborn.modelbrowser.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;

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
    private static final Identifier RECIPE_BOOK_TEXTURE = Identifier.ofVanilla("textures/gui/recipe_book.png");
    private static final Identifier SLOT_CRAFTABLE_SPRITE = Identifier
            .ofVanilla("textures/gui/sprites/recipe_book/slot_craftable.png");
    private static final ButtonTextures PAGE_FORWARD_TEXTURES = new ButtonTextures(
            Identifier.ofVanilla("recipe_book/page_forward"),
            Identifier.ofVanilla("recipe_book/page_forward_highlighted"));
    private static final ButtonTextures PAGE_BACKWARD_TEXTURES = new ButtonTextures(
            Identifier.ofVanilla("recipe_book/page_backward"),
            Identifier.ofVanilla("recipe_book/page_backward_highlighted"));
    private static final Text SEARCH_HINT_TEXT = Text.translatable("gui.recipebook.search_hint")
            .fillStyle(TextFieldWidget.SEARCH_STYLE);

    // State
    private String lastSearch = "";
    private int pageCount = 0;
    private int currentPage = 0;

    // UI Components
    private TextFieldWidget searchField;
    private ScreenRect searchFieldRect;
    private TexturedButtonWidget nextPageButton;
    private TexturedButtonWidget prevPageButton;

    // References
    private int screenLeft;
    private int screenTop;
    private int screenWidth;
    private int screenHeight;
    private TextRenderer textRenderer;

    public ModelBrowserWidget(MinecraftClient client, int screenWidth, int screenHeight) {
        this.textRenderer = client.textRenderer;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.screenLeft = (screenWidth - 176) / 2;
        this.screenTop = (screenHeight - 166) / 2;
    }

    public void initialize() {
        int searchX = screenLeft + SEARCH_BOX_POSITION_X - SHIFT_LEFT_AMOUNT;
        int searchY = screenTop + SEARCH_BOX_POSITION_Y;

        searchField = new TextFieldWidget(textRenderer, searchX, searchY, 109, 14, Text.translatable("itemGroup.search"));
        searchField.setMaxLength(50);
        searchField.setChangedListener(this::filterModelStacks);
        searchField.setPlaceholder(SEARCH_HINT_TEXT);
        searchField.visible = ModConfig.INSTANCE.isModelBrowserOpen;
        updateSearchRect();

        int pagePrevX = screenLeft + PREV_PAGE_POSITION_X - SHIFT_LEFT_AMOUNT;
        int pageNextX = screenLeft + NEXT_PAGE_POSITION_X - SHIFT_LEFT_AMOUNT;
        int pageButtonY = screenTop + PAGE_BUTTONS_POSITION_Y;

        prevPageButton = new TexturedButtonWidget(
            pagePrevX,
            pageButtonY,
            12,
            17,
            PAGE_BACKWARD_TEXTURES,
            b -> previousPage(),
            Text.empty()
        );

        nextPageButton = new TexturedButtonWidget(
            pageNextX,
            pageButtonY,
            12,
            17,
            PAGE_FORWARD_TEXTURES,
            b -> nextPage(),
            Text.empty()
        );

        prevPageButton.visible = false;
        nextPageButton.visible = false;

        filterModelStacks("");
    }

    private void updateSearchRect() {
        searchFieldRect = new ScreenRect(
            searchField.getX() - 17,
            searchField.getY(),
            searchField.getWidth() + 17,
            searchField.getHeight()
        );
    }

    public void toggle() {
        ModConfig.INSTANCE.isModelBrowserOpen = !ModConfig.INSTANCE.isModelBrowserOpen;

        searchField.visible = ModConfig.INSTANCE.isModelBrowserOpen;
        searchField.active = ModConfig.INSTANCE.isModelBrowserOpen;
    }

    public void filterModelStacks(String text) {
        if (text.equals(lastSearch)) return;
        lastSearch = text;

        ModelListData.filter(text);
        currentPage = 0;
    }

    private void previousPage() {
        if (currentPage > 0) currentPage--;
    }

    private void nextPage() {
        if (currentPage < pageCount - 1) currentPage++;
    }

    public ItemStack getItemAtMouse(int mouseX, int mouseY) {
        if (!this.isOpen()) return null;

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

    public void render(DrawContext ctx, int mouseX, int mouseY, float deltaTicks) {
        if (this.isOpen()) {
            prevPageButton.render(ctx, mouseX, mouseY, deltaTicks);
            nextPageButton.render(ctx, mouseX, mouseY, deltaTicks);
            searchField.render(ctx, mouseX, mouseY, deltaTicks);
            if (this.pageCount > 1) {
                Text text = Text.translatable("gui.recipebook.page", new Object[]{this.currentPage + 1, this.pageCount});
                int x = screenLeft + PAGE_COUNT_POSITION_X - SHIFT_LEFT_AMOUNT;
                int y = screenTop + PAGE_COUNT_POSITION_Y;
                ctx.drawTextWithShadow(textRenderer, text, x, y, Colors.WHITE);
            }
        }
    }

    public void drawBackground(DrawContext ctx) {
        if (!this.isOpen()) return;

        int x = screenLeft - SHIFT_LEFT_AMOUNT;
        int y = screenTop;

        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, RECIPE_BOOK_TEXTURE, x, y,
                1.0F, 1.0F, 147, 166, 256, 256);
    }

    public void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        if (!this.isOpen()) return;

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

            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, SLOT_CRAFTABLE_SPRITE,
                    x, y, 0, 0, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE);

            ctx.drawItem(stacks.get(i), x + 4, y + 4);
        }

        prevPageButton.visible = currentPage > 0;
        nextPageButton.visible = currentPage < pageCount - 1;

        ItemStack hovered = getItemAtMouse(mouseX, mouseY);
        if (hovered != null) {
            Text name = hovered.get(DataComponentTypes.CUSTOM_NAME);
            Identifier modelId = hovered.get(DataComponentTypes.ITEM_MODEL);
            if (name != null) {
                ctx.drawTooltip(textRenderer, name, mouseX, mouseY);
            } else if (modelId != null) {
                ctx.drawTooltip(textRenderer, Text.literal(modelId.toString()), mouseX, mouseY);
            }
        }
    }

    public TextFieldWidget getSearchField() {
        return searchField;
    }

    public ScreenRect getSearchFieldRect() {
        return searchFieldRect;
    }

    public TexturedButtonWidget getNextPageButton() {
        return nextPageButton;
    }

    public TexturedButtonWidget getPrevPageButton() {
        return prevPageButton;
    }

    public boolean isOpen() {
        return ModConfig.INSTANCE.isModelBrowserOpen;
    }

    public void toggleOpen(boolean shifted) {
        ModConfig.INSTANCE.isModelBrowserOpen = !ModConfig.INSTANCE.isModelBrowserOpen;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int page) {
        this.currentPage = page;
    }
}
