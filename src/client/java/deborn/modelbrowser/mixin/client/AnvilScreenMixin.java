package deborn.modelbrowser.mixin.client;

import deborn.modelbrowser.ModelBrowserScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
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

    // ---------- Static assets ----------
    private static final Identifier RECIPE_BOOK_TEXTURE = Identifier.ofVanilla("textures/gui/recipe_book.png");
    private static final ButtonTextures RECIPE_BUTTON_TEXTURES = new ButtonTextures(Identifier.ofVanilla("icon/search"));
    private static final Identifier SLOT_CRAFTABLE_SPRITE = Identifier.ofVanilla("textures/gui/sprites/recipe_book/slot_craftable.png");

    private static final ButtonTextures PAGE_FORWARD_TEXTURES = new ButtonTextures(
        Identifier.ofVanilla("recipe_book/page_forward"),
        Identifier.ofVanilla("recipe_book/page_forward_highlighted")
    );

    private static final ButtonTextures PAGE_BACKWARD_TEXTURES = new ButtonTextures(
        Identifier.ofVanilla("recipe_book/page_backward"),
        Identifier.ofVanilla("recipe_book/page_backward_highlighted")
    );

    private static final Text SEARCH_HINT_TEXT =
        Text.translatable("gui.recipebook.search_hint").fillStyle(TextFieldWidget.SEARCH_STYLE);

    // ---------- Constants ----------
    private static final int ITEM_SIZE = 25;
    private static final int GRID_COLUMNS = 5;
    private static final int MAX_VISIBLE_ROWS = 4;
    private static final int SHIFT_AMOUNT = 77;

    // ---------- Shadows ----------
    @Shadow private TextFieldWidget nameField;

    // ---------- UI State ----------
    private boolean uiShifted = false;

    private TexturedButtonWidget toggleButton;
    private TextFieldWidget modelSearchBox;
    private ToggleButtonWidget nextPageButton;
    private ToggleButtonWidget prevPageButton;

    private int currentPage = 0;

    // ---------- Cached GUI positions ----------
    private int guiLeft;
    private int guiTop;

    private int gridX;
    private int gridY;

    private int searchX;
    private int searchY;

    private int pagePrevX;
    private int pageNextX;
    private int pageButtonY;

    // ---------- Constructor ----------
    protected AnvilScreenMixin(Text title) {
        super(title);
    }

    // ====================================================================
    // Position Calculation — done once per setup() call
    // ====================================================================
    private void computePositions() {

        guiLeft = (this.width - 176) / 2;
        guiTop  = (this.height - 166) / 2;

        // Model search bar
        searchX = guiLeft - 124;
        searchY = guiTop + 13;

        // Grid base
        gridX = guiLeft - 61;
        gridY = guiTop + 30;

        // Paging buttons
        pageButtonY = gridY + MAX_VISIBLE_ROWS * ITEM_SIZE + 4;
        pagePrevX = gridX;
        pageNextX = gridX + GRID_COLUMNS * ITEM_SIZE - 12;
    }

    @Shadow protected abstract void setup();

    // ====================================================================
    // SETUP Sequence
    // ====================================================================

    @Inject(method = "setup", at = @At("TAIL"))
    private void setupUI(CallbackInfo ci) {

        computePositions();

        // --- Shrink name field ---
        if (nameField != null) {
            nameField.setWidth(86);
        }

        // --- Search box ---
        if (modelSearchBox == null) {
            modelSearchBox = new TextFieldWidget(textRenderer, searchX, searchY, 109, 14, Text.literal("Search Models"));
            modelSearchBox.setMaxLength(50);
            modelSearchBox.setChangedListener(this::filterModelStacks);
            modelSearchBox.setPlaceholder(SEARCH_HINT_TEXT);
        } else {
            modelSearchBox.setX(searchX);
            modelSearchBox.setY(searchY);
        }

        modelSearchBox.visible = uiShifted;
        modelSearchBox.active = uiShifted;
        addDrawableChild(modelSearchBox);

        // --- Toggle button ---
        toggleButton = new TexturedButtonWidget(
            guiLeft + 154,
            guiTop + 22,
            12, 12,
            RECIPE_BUTTON_TEXTURES,
            b -> toggleGuiShift(),
            Text.literal("⇄")
        );
        addDrawableChild(toggleButton);

        // --- Paging buttons ---
        prevPageButton = new ToggleButtonWidget(pagePrevX, pageButtonY, 12, 17, true);
        prevPageButton.setTextures(PAGE_BACKWARD_TEXTURES);

        nextPageButton = new ToggleButtonWidget(pageNextX, pageButtonY, 12, 17, false);
        nextPageButton.setTextures(PAGE_FORWARD_TEXTURES);

        addDrawableChild(prevPageButton);
        addDrawableChild(nextPageButton);

        prevPageButton.visible = false;
        nextPageButton.visible = false;

        // --- Apply shift if already active ---
        if (uiShifted) {
            shiftUI(+SHIFT_AMOUNT);
        }
        filterModelStacks("");
    }

    // ====================================================================
    // SHIFT LOGIC — clean and unified
    // ====================================================================
    private void shiftUI(int dx) {
        HandledScreenAccessor acc = (HandledScreenAccessor)(Object)this;

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
        modelSearchBox.active  = !uiShifted;

        if (uiShifted) {
            nameField.setFocusUnlocked(false);
            nameField.setFocused(true);
        } else {
            nameField.setFocusUnlocked(true);
            nameField.setFocused(false);
        }

        uiShifted = !uiShifted;
    }

    // ====================================================================
    // SEARCH FILTER
    // ====================================================================
    private void filterModelStacks(String text) {
        if (ModelBrowserScreen.INSTANCE == null) return;

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

        currentPage = 0;
    }

    // ====================================================================
    // KEY INPUT
    // ====================================================================
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

    // ====================================================================
    // DRAWING
    // ====================================================================

    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void drawShiftedRecipeBook(DrawContext ctx, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        if (!uiShifted) return;

        int x = guiLeft - 72;
        int y = guiTop;

        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, RECIPE_BOOK_TEXTURE, x, y,
            1.0F, 1.0F, 147, 166, 256, 256);
    }

    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void drawModelGrid(DrawContext ctx, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        if (!uiShifted) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        ModelBrowserScreen browser = ModelBrowserScreen.INSTANCE;
        if (browser == null) return;

        List<ItemStack> stacks;
        synchronized (browser.getModelStacks()) {
            stacks = List.copyOf(browser.getModelStacks());
        }

        int itemsPerPage = GRID_COLUMNS * MAX_VISIBLE_ROWS;
        int pageCount = (int)Math.ceil(stacks.size() / (double)itemsPerPage);

        if (currentPage >= pageCount)
            currentPage = Math.max(0, pageCount - 1);

        int start = currentPage * itemsPerPage;
        int end   = Math.min(start + itemsPerPage, stacks.size());

        ItemStack hovered = null;

        // Draw items
        for (int i = start; i < end; i++) {
            int index = i - start;
            int row = index / GRID_COLUMNS;
            int col = index % GRID_COLUMNS;

            int x = gridX + col * ITEM_SIZE;
            int y = gridY + row * ITEM_SIZE;

            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, SLOT_CRAFTABLE_SPRITE, x, y,
                0, 0, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE);

            ctx.drawItem(stacks.get(i), x + 4, y + 4);

            if (mouseX >= x && mouseX <= x + ITEM_SIZE &&
                mouseY >= y && mouseY <= y + ITEM_SIZE) {
                hovered = stacks.get(i);
            }
        }

        // Page buttons
        prevPageButton.visible = currentPage > 0;
        nextPageButton.visible = currentPage < pageCount - 1;

        // Tooltip
        if (hovered != null) {
            Identifier modelId = hovered.get(DataComponentTypes.ITEM_MODEL);
            if (modelId != null) {
                ctx.drawTooltip(textRenderer, Text.literal(modelId.toString()), mouseX, mouseY);
            }
        }
    }
}
