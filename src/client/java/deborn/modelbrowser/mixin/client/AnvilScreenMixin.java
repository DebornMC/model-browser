package deborn.modelbrowser.mixin.client;

import deborn.modelbrowser.ModelBrowserScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
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

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends Screen {

    @Shadow
    private TextFieldWidget nameField; // Shadow the text input
    private static final Identifier RECIPE_BOOK_TEXTURE = Identifier.ofVanilla("textures/gui/recipe_book.png");
    private static final ButtonTextures RECIPE_BUTTON_TEXTURES = new ButtonTextures(Identifier.ofVanilla("icon/search"));
    private static final Identifier SLOT_CRAFTABLE_SPRITE = Identifier.ofVanilla("textures/gui/sprites/recipe_book/slot_craftable.png");
    private static final Text SEARCH_HINT_TEXT = Text.translatable("gui.recipebook.search_hint").fillStyle(TextFieldWidget.SEARCH_STYLE);

    private static final int ITEM_SIZE = 25;
    private static final int GRID_COLUMNS = 5;
    private static final int MAX_VISIBLE_ROWS = 4;
    private static final int SHIFT_AMOUNT = 77;
    

    private boolean uiShifted = false;
    private TexturedButtonWidget toggleButton;
    private TextFieldWidget modelSearchBox;
            
    private int guiLeft;
    private int guiTop;
    private int baseX;
    private int baseY;

    protected AnvilScreenMixin(Text title) {
        super(title);
    }

    private void getPositions() {
        guiLeft = (this.width - 176) / 2;
        guiTop = (this.height - 166) / 2;
        baseX = guiLeft - 124;
        baseY = guiTop + 13;
    }

    @Shadow
    protected abstract void setup();
    @Inject(method = "setup", at = @At("TAIL"))
    private void shrinkNameFieldWidth(CallbackInfo ci) {
        if (nameField != null) {
            nameField.setWidth(86); // shrink width from 103
        }
    }
    

    @Inject(method = "setup", at = @At("TAIL"))
    private void addSearchBar(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        getPositions();
        if (modelSearchBox == null) {
            modelSearchBox = new TextFieldWidget(this.textRenderer, baseX, baseY, 109, 14, Text.literal("Search Models"));
            modelSearchBox.setMaxLength(50);
            modelSearchBox.setChangedListener(this::filterModelStacks);
            modelSearchBox.setPlaceholder(SEARCH_HINT_TEXT);
        } else {
            // reposition on resize, but don't apply shift yet
            modelSearchBox.setX(baseX);
            modelSearchBox.setY(baseY);
        }

        // only apply shift if the GUI is currently toggled
        if (uiShifted) {
            modelSearchBox.setX(modelSearchBox.getX() + SHIFT_AMOUNT);
        }

        modelSearchBox.visible = uiShifted;
        modelSearchBox.active = uiShifted;

        // re-add after every setup rebuild
        this.addDrawableChild(modelSearchBox);

        modelSearchBox.setChangedListener(this::filterModelStacks);
        
        // Populate initially with all models
        filterModelStacks(modelSearchBox.getText());
    }



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
    }

    @Inject(method = "setup", at = @At("TAIL"))
    private void applyShiftAfterSetup(CallbackInfo ci) {
        if (uiShifted) {
            HandledScreenAccessor accessor = (HandledScreenAccessor) (Object) this;

            accessor.setX(accessor.getX() + SHIFT_AMOUNT);
            toggleButton.setX(toggleButton.getX() + SHIFT_AMOUNT);
            nameField.setX(nameField.getX() + SHIFT_AMOUNT);
        }
    }


    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void drawRecipeBookWhenShifted(DrawContext context, float deltaTicks, int mouseX, int mouseY, CallbackInfo ci) {
        if (uiShifted) {
            int i = (this.width - 176) / 2 - 72;
			int j = (this.height - 166) / 2;
            
            context.drawTexture(RenderPipelines.GUI_TEXTURED, RECIPE_BOOK_TEXTURE, i, j, 1.0F, 1.0F, 147, 166, 256, 256);
        }
    }

    @Inject(method = "setup", at = @At("TAIL"))
    private void addOrResetModelBrowserButton(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        int guiLeft = (this.width - 176) / 2;
        int guiTop = (this.height - 166) / 2;

        int buttonX = guiLeft + 154;
        int buttonY = guiTop + 22;

        // Recreate or reset the button
        toggleButton = new TexturedButtonWidget(
            buttonX,
            buttonY,
            12,
            12,
            RECIPE_BUTTON_TEXTURES,
            b -> toggleGuiShift(),
            Text.literal("⇄")
        );

        // Shift it if needed
        if (uiShifted) toggleButton.setX(toggleButton.getX() + SHIFT_AMOUNT);

        // Add to drawable children every setup
        this.addDrawableChild(toggleButton);
    }

    private void toggleGuiShift() {
        HandledScreenAccessor accessor = (HandledScreenAccessor) (Object) this;
        getPositions();

        if (uiShifted) {
            // shifting back
            accessor.setX(accessor.getX() - SHIFT_AMOUNT);
            toggleButton.setX(toggleButton.getX() - SHIFT_AMOUNT);
            nameField.setX(nameField.getX() - SHIFT_AMOUNT);

            modelSearchBox.visible = false;
            modelSearchBox.active = false;

            nameField.setFocused(true);
            nameField.setFocusUnlocked(false);
        } else {
            // shifting forward
            accessor.setX(accessor.getX() + SHIFT_AMOUNT);
            toggleButton.setX(toggleButton.getX() + SHIFT_AMOUNT);
            nameField.setX(nameField.getX() + SHIFT_AMOUNT);

            modelSearchBox.setX(baseX + SHIFT_AMOUNT);
            modelSearchBox.setY(baseY);
            modelSearchBox.visible = true;
            modelSearchBox.active = true;

            nameField.setFocusUnlocked(true);
            nameField.setFocused(false);
        }

        uiShifted = !uiShifted;
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void interceptKeys(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (input.isEscape()) {
            // Let the vanilla behavior close the screen
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                client.player.closeHandledScreen();
            }
            cir.setReturnValue(true);
            return;
        }

        if (modelSearchBox != null && modelSearchBox.isActive()) {
            // Block all keys except Escape while the search box is active
            boolean handled = modelSearchBox.keyPressed(input);
            cir.setReturnValue(true); // always return true to block hotbar number keys
        }
    }




    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void drawModelGridWhenShifted(DrawContext context, float deltaTicks, int mouseX, int mouseY, CallbackInfo ci) {
        if (!uiShifted) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        int guiLeft = (this.width - 176) / 2;
        int guiTop = (this.height - 166) / 2;

        // Scroll window area inside recipe book
        int gridX = guiLeft - 61;
        int gridY = guiTop + 30;

        int visibleHeight = MAX_VISIBLE_ROWS * ITEM_SIZE;

        // static scroll offset
        if (ModelBrowserScreen.scrollOffset < 0) ModelBrowserScreen.scrollOffset = 0;

        ModelBrowserScreen browser = ModelBrowserScreen.INSTANCE;
        if (browser == null) return;

        int totalRows;
        synchronized (browser.getModelStacks()) {
            totalRows = (int) Math.ceil(browser.getModelStacks().size() / (double)GRID_COLUMNS);
        }

        // limit scroll
        int maxScroll = Math.max(0, totalRows - MAX_VISIBLE_ROWS);
        if (ModelBrowserScreen.scrollOffset > maxScroll) {
            ModelBrowserScreen.scrollOffset = maxScroll;
        }

        int yOffset = -ModelBrowserScreen.scrollOffset * ITEM_SIZE;

        ItemStack hovered = null;
        int hoveredX = 0, hoveredY = 0;

        // scissor limits drawing to 6 rows
        context.enableScissor(gridX, gridY, gridX + GRID_COLUMNS * ITEM_SIZE, gridY + visibleHeight);

        int index = 0;
        synchronized (browser.getModelStacks()) {
            for (ItemStack stack : browser.getModelStacks()) {
                int row = index / GRID_COLUMNS;
                int col = index % GRID_COLUMNS;

                int x = gridX + col * ITEM_SIZE;
                int y = gridY + row * ITEM_SIZE + yOffset;

                // Only draw inside the clipped scroll region
                if (y >= gridY - ITEM_SIZE && y <= gridY + visibleHeight) {
                    context.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        SLOT_CRAFTABLE_SPRITE,
                        x, y,
                        0, 0,
                        25, 25,
                        25, 25
                    );
                    context.drawItem(stack, x + 4, y + 4);

                    // detect hover
                    if (mouseX >= x && mouseX <= x + ITEM_SIZE &&
                        mouseY >= y && mouseY <= y + ITEM_SIZE) {
                        hovered = stack;
                        hoveredX = x;
                        hoveredY = y;
                    }
                }
                index++;
            }
        }

        context.disableScissor();

        // Tooltip
        if (hovered != null) {
            Identifier modelId = hovered.get(DataComponentTypes.ITEM_MODEL);
            if (modelId != null) {
                context.drawTooltip(this.textRenderer, Text.literal(modelId.toString()), mouseX, mouseY);
            }
        }
    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (uiShifted) {
            // Only scroll when mouse is inside the grid area
            int gridX = guiLeft - 61;
            int gridY = guiTop + 30;

            int visibleHeight = MAX_VISIBLE_ROWS * ITEM_SIZE;

            if (mouseX >= gridX && mouseX <= gridX + GRID_COLUMNS * ITEM_SIZE &&
                mouseY >= gridY && mouseY <= gridY + visibleHeight) {

                int totalRows;
                ModelBrowserScreen browser = ModelBrowserScreen.INSTANCE;
                synchronized (browser.getModelStacks()) {
                    totalRows = (int)Math.ceil(browser.getModelStacks().size() / (double)GRID_COLUMNS);
                }

                int maxScroll = Math.max(0, totalRows - MAX_VISIBLE_ROWS);

                // scroll direction
                if (vertical < 0) ModelBrowserScreen.scrollOffset++;
                if (vertical > 0) ModelBrowserScreen.scrollOffset--;

                // clamp
                if (ModelBrowserScreen.scrollOffset < 0) ModelBrowserScreen.scrollOffset = 0;
                if (ModelBrowserScreen.scrollOffset > maxScroll) ModelBrowserScreen.scrollOffset = maxScroll;

                return true; // consume scroll event
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

}