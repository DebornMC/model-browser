package deborn.modelbrowser.mixin.client;

import deborn.modelbrowser.ModelBrowserScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.gui.widget.ToggleButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends Screen {
    @Shadow
    private TextFieldWidget nameField;
    private static final Identifier RECIPE_BOOK_TEXTURE = Identifier.ofVanilla("textures/gui/recipe_book.png");
    private static final ButtonTextures RECIPE_BUTTON_TEXTURES = new ButtonTextures(
            Identifier.ofVanilla("icon/search"));
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

    private static final int ITEM_SIZE = 25;
    private static final int GRID_COLUMNS = 5;
    private static final int MAX_VISIBLE_ROWS = 4;
    private static final int SHIFT_AMOUNT = 77;
    private static final int SHIFT_LEFT_AMOUNT = 72;

    private static final int GRID_POSITION_X = 11;
    private static final int GRID_POSITION_Y = 32;
    private static final int PREV_PAGE_POSITION_X = 38;
    private static final int NEXT_PAGE_POSITION_X = 93;
    private static final int PAGE_BUTTONS_POSITION_Y = 137;
    private static final int PAGE_COUNT_POSITION_X = 64;
    private static final int PAGE_COUNT_POSITION_Y = 141;

    private String lastSearch;
    private boolean uiShifted = false;
    private int pageCount;

    private TexturedButtonWidget toggleButton;
    private TextFieldWidget modelSearchBox;
    private ToggleButtonWidget nextPageButton;
    private ToggleButtonWidget prevPageButton;

    private int currentPage = 0;

    private int guiLeft;
    private int guiTop;

    private int gridX;
    private int gridY;

    private int searchX;
    private int searchY;

    protected AnvilScreenMixin(Text title) {
        super(title);
    }

    private void computePositions() {

        guiLeft = (this.width - 176) / 2;
        guiTop = (this.height - 166) / 2;

        searchX = guiLeft - 124;
        searchY = guiTop + 13;

        gridX = guiLeft - 61;
        gridY = guiTop + 30;
    }

    @Shadow
    protected abstract void setup();

    @Inject(method = "setup", at = @At("TAIL"))
    private void setupUI(CallbackInfo ci) {

        computePositions();
        nameField.setWidth(86);

        modelSearchBox = new TextFieldWidget(textRenderer, searchX, searchY, 109, 14, Text.literal("Search Models"));
        modelSearchBox.setMaxLength(50);
        modelSearchBox.setChangedListener(this::filterModelStacks);
        modelSearchBox.setPlaceholder(SEARCH_HINT_TEXT);
        modelSearchBox.visible = uiShifted;

        addDrawableChild(modelSearchBox);
        if (ModelBrowserScreen.INSTANCE != null) {
            ModelBrowserScreen.INSTANCE.searchBox = modelSearchBox;
        }
        toggleButton = new TexturedButtonWidget(
                guiLeft + 154,
                guiTop + 22,
                12, 12,
                RECIPE_BUTTON_TEXTURES,
                b -> toggleGuiShift(),
                Text.literal("⇄"));
        addDrawableChild(toggleButton);
        
        int pagePrevX = guiLeft + PREV_PAGE_POSITION_X - SHIFT_LEFT_AMOUNT - SHIFT_AMOUNT;
        int pageNextX = guiLeft + NEXT_PAGE_POSITION_X - SHIFT_LEFT_AMOUNT - SHIFT_AMOUNT;
        int pageButtonY = guiTop + PAGE_BUTTONS_POSITION_Y;
        
        prevPageButton = new ToggleButtonWidget(pagePrevX, pageButtonY, 12, 17, true);
        prevPageButton.setTextures(PAGE_BACKWARD_TEXTURES);
        nextPageButton = new ToggleButtonWidget(pageNextX, pageButtonY, 12, 17, false);
        nextPageButton.setTextures(PAGE_FORWARD_TEXTURES);

        // addDrawableChild(prevPageButton);
        // addDrawableChild(nextPageButton);

        prevPageButton.visible = false;
        nextPageButton.visible = false;

        if (uiShifted) {
            shiftUI(+SHIFT_AMOUNT);
        }
        filterModelStacks("");
    }

    private void shiftUI(int dx) {
        HandledScreenAccessor acc = (HandledScreenAccessor) (Object) this;

        acc.setX(acc.getX() + dx);
        toggleButton.setX(toggleButton.getX() + dx);
        nameField.setX(nameField.getX() + dx);

        prevPageButton.setX(prevPageButton.getX() + dx);
        nextPageButton.setX(nextPageButton.getX() + dx);

        modelSearchBox.setX(modelSearchBox.getX() + dx);
    }

    private void toggleGuiShift() {
        int dir = uiShifted ? -SHIFT_AMOUNT : SHIFT_AMOUNT;
        shiftUI(dir);

        modelSearchBox.visible = !uiShifted;
        modelSearchBox.active = !uiShifted;

        if (uiShifted) {
            nameField.setFocusUnlocked(false);
            nameField.setFocused(true);
        } else {
            nameField.setFocusUnlocked(true);
            nameField.setFocused(false);
        }

        uiShifted = !uiShifted;
    }

    private void filterModelStacks(String text) {
        if (ModelBrowserScreen.INSTANCE == null)
            return;

        // If the text did not change, do NOT reset the page
        if (text.equals(lastSearch)) {
            return;
        }
        lastSearch = text;

        String lower = text.toLowerCase();

        synchronized (ModelBrowserScreen.INSTANCE.getModelStacks()) {
            ModelBrowserScreen.INSTANCE.getModelStacks().clear();

            for (ItemStack stack : ModelBrowserScreen.INSTANCE.allModelStacks) {
                Identifier id = stack.get(DataComponentTypes.ITEM_MODEL);
                if (id != null && id.toString().toLowerCase().contains(lower)) {
                    ModelBrowserScreen.INSTANCE.getModelStacks().add(stack);
                }
            }
        }

        // Now this only runs when the query actually changes
        currentPage = 0;
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void interceptKeys(KeyInput input, CallbackInfoReturnable<Boolean> cir) {

        if (input.isEscape()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                client.player.closeHandledScreen();
            }
            cir.setReturnValue(true);
            return;
        }

        if (modelSearchBox.isActive()) {
            modelSearchBox.keyPressed(input);
            cir.setReturnValue(true);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        super.mouseClicked(click, doubled);
        if (uiShifted) {
            if (this.prevPageButton.mouseClicked(click, doubled)) {
                currentPage--;
                return true;
            } else if (this.nextPageButton.mouseClicked(click, doubled)) {
                currentPage++;
                return true; 
            }
        }
        return false;
    }

    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void drawShiftedRecipeBook(DrawContext ctx, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        if (!uiShifted)
            return;

        int x = guiLeft - SHIFT_LEFT_AMOUNT;
        int y = guiTop;

        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, RECIPE_BOOK_TEXTURE, x, y,
                1.0F, 1.0F, 147, 166, 256, 256);
    }

    @Inject(method = "drawForeground", at = @At("TAIL"))
    private void drawModelGrid(DrawContext ctx, int mouseX, int mouseY, CallbackInfo ci) {
        if (!uiShifted)
            return;

        ModelBrowserScreen browser = ModelBrowserScreen.INSTANCE;
        if (browser == null)
            return;

        List<ItemStack> stacks;
        synchronized (browser.getModelStacks()) {
            stacks = List.copyOf(browser.getModelStacks());
        }

        int itemsPerPage = GRID_COLUMNS * MAX_VISIBLE_ROWS;
        pageCount = (int) Math.ceil(stacks.size() / (double) itemsPerPage);

        currentPage = Math.min(currentPage, Math.max(0, pageCount - 1));

        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, stacks.size());

        ItemStack hovered = null;

        for (int i = start; i < end; i++) {
            int index = i - start;
            int row = index / GRID_COLUMNS;
            int col = index % GRID_COLUMNS;

            int localMouseX = mouseX - guiLeft;
            int localMouseY = mouseY - guiTop;


            int x = GRID_POSITION_X + col * ITEM_SIZE - SHIFT_LEFT_AMOUNT - SHIFT_AMOUNT;
            int y = GRID_POSITION_Y + row * ITEM_SIZE;

            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, SLOT_CRAFTABLE_SPRITE,
                    x, y, 0, 0, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE);

            ctx.drawItem(stacks.get(i), x + 4, y + 4);

            if (localMouseX >= x + SHIFT_AMOUNT && localMouseX <= x + SHIFT_AMOUNT + ITEM_SIZE &&
                    localMouseY >= y && localMouseY <= y + ITEM_SIZE) {
                hovered = stacks.get(i);
            }
        }

        prevPageButton.visible = currentPage > 0;
        nextPageButton.visible = currentPage < pageCount - 1;

        if (hovered != null) {
            Identifier modelId = hovered.get(DataComponentTypes.ITEM_MODEL);
            if (modelId != null) {
                ctx.drawTooltip(textRenderer, Text.literal(modelId.toString()), mouseX, mouseY);
            }
        }
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY, float deltaTicks) {
        super.render(ctx, mouseX, mouseY, deltaTicks);
        if (uiShifted) {
            prevPageButton.render(ctx, mouseX, mouseY, deltaTicks);
            nextPageButton.render(ctx, mouseX, mouseY, deltaTicks);
            if (this.pageCount > 1) {
                Text text = Text.translatable("gui.recipebook.page", new Object[]{this.currentPage + 1, this.pageCount});
                int x = guiLeft + PAGE_COUNT_POSITION_X - SHIFT_LEFT_AMOUNT;
                int y = guiTop + PAGE_COUNT_POSITION_Y;
                ctx.drawTextWithShadow(this.client.textRenderer, text, x, y, Colors.WHITE);
            }
        }
    }

}
