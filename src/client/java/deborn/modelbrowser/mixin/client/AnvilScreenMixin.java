package deborn.modelbrowser.mixin.client;

import deborn.modelbrowser.ModelBrowserScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.navigation.NavigationAxis;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.gui.widget.ToggleButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

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

    private static final int SEARCH_BOX_POSITION_X = 25;
    private static final int SEARCH_BOX_POSITION_Y = 13;

    private String lastSearch;
    private boolean uiShifted = false;
    private int pageCount;

    private TexturedButtonWidget toggleButton;
    private TextFieldWidget searchField;
    private ScreenRect searchFieldRect;
    
    private ToggleButtonWidget nextPageButton;
    private ToggleButtonWidget prevPageButton;

    private int currentPage = 0;

    protected AnvilScreenMixin(Text title) {
        super(title);
    }

    private int getTop() {
		return (this.height - 166) / 2;
	}

	private int getLeft() {
		return (this.width - 176) / 2;
	}

    @Shadow
    protected abstract void setup();

    @Inject(method = "setup", at = @At("TAIL"))
    private void setupUI(CallbackInfo ci) {

        nameField.setWidth(86);

        int searchX = this.getLeft() + SEARCH_BOX_POSITION_X - SHIFT_LEFT_AMOUNT - SHIFT_AMOUNT;
        int searchY = this.getTop() + SEARCH_BOX_POSITION_Y;    

        searchField = new TextFieldWidget(textRenderer, searchX, searchY, 109, 14, Text.literal("Search Models"));
        searchField.setMaxLength(50);
        searchField.setChangedListener(this::filterModelStacks);
        searchField.setPlaceholder(SEARCH_HINT_TEXT);
        searchField.visible = uiShifted;
        updateSearchRect();

        if (ModelBrowserScreen.INSTANCE != null) {
            ModelBrowserScreen.INSTANCE.searchBox = searchField;
        }
        toggleButton = new TexturedButtonWidget(
                this.getLeft() + 154,
                this.getTop() + 22,
                12, 12,
                RECIPE_BUTTON_TEXTURES,
                b -> toggleGuiShift(),
                Text.literal("⇄"));
        addDrawableChild(toggleButton);
        
        int pagePrevX = this.getLeft() + PREV_PAGE_POSITION_X - SHIFT_LEFT_AMOUNT - SHIFT_AMOUNT;
        int pageNextX = this.getLeft() + NEXT_PAGE_POSITION_X - SHIFT_LEFT_AMOUNT - SHIFT_AMOUNT;
        int pageButtonY = this.getTop() + PAGE_BUTTONS_POSITION_Y;
        
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

        searchField.setX(searchField.getX() + dx);
        updateSearchRect();
    }

    private void updateSearchRect() {
        searchFieldRect = new ScreenRect(
            searchField.getX() - 17, 
            searchField.getY(), 
            searchField.getWidth() + 17, 
            searchField.getHeight()
        );
    }

    private void toggleGuiShift() {
        int dir = uiShifted ? -SHIFT_AMOUNT : SHIFT_AMOUNT;
        shiftUI(dir);

        searchField.visible = !uiShifted;
        searchField.active = !uiShifted;

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

        if (searchField.isActive()) {
            searchField.keyPressed(input);
            cir.setReturnValue(true);
        }
    }
    private ItemStack getItemAtMouse(int mouseX, int mouseY) {
        if (!uiShifted || ModelBrowserScreen.INSTANCE == null) return null;

        List<ItemStack> stacks;
        synchronized (ModelBrowserScreen.INSTANCE.getModelStacks()) {
            stacks = List.copyOf(ModelBrowserScreen.INSTANCE.getModelStacks());
        }

        int itemsPerPage = GRID_COLUMNS * MAX_VISIBLE_ROWS;
        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, stacks.size());

        for (int i = start; i < end; i++) {
            int index = i - start;
            int row = index / GRID_COLUMNS;
            int col = index % GRID_COLUMNS;

            int x = this.getLeft() + GRID_POSITION_X + col * ITEM_SIZE - SHIFT_LEFT_AMOUNT;
            int y = this.getTop() + GRID_POSITION_Y + row * ITEM_SIZE;

            if (mouseX >= x && mouseX <= x + ITEM_SIZE &&
                mouseY >= y && mouseY <= y + ITEM_SIZE) {
                return stacks.get(i);
            }
        }

        return null;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        super.mouseClicked(click, doubled);

        if (!uiShifted) return false;

        if (prevPageButton.mouseClicked(click, doubled)) { currentPage--; return true; }
        if (nextPageButton.mouseClicked(click, doubled)) { currentPage++; return true; }

        HandledScreenAccessor acc = (HandledScreenAccessor) (Object) this;
        ScreenHandler handler = acc.getHandler();

        ItemStack clickedStack = getItemAtMouse((int) click.x(), (int) click.y());
        if (clickedStack != null) {
            Identifier modelId = clickedStack.get(DataComponentTypes.ITEM_MODEL);
            if (modelId != null) {
                ClickableWidget.playClickSound(MinecraftClient.getInstance().getSoundManager());
                if (handler.getSlot(0).hasStack()) {
                    nameField.setText("");  
                    nameField.setText(modelId.toString());
                    return true;
                }
            }
        }		
        if (this.searchField != null) {
            boolean bl = this.searchFieldRect != null && this.searchFieldRect.contains((int) click.x(), (int) click.y());
            if (bl) {
                this.setFocused(searchField);
                this.searchField.setFocused(true);
                return true;
            }
            this.searchField.setFocused(false);
        }
        return false;
    }

    // @Inject(method = "onSlotUpdate", at = @At("HEAD"), cancellable = true)
    // private void dontUpdateWhenNameEntered(ScreenHandler handler, int slotId, ItemStack stack, CallbackInfo ci) {
    //     if (slotId == 0) {
    //         if (!stack.isEmpty() && !nameField.getText().isEmpty()) {
    //             this.setFocused(this.nameField);
    //             ci.cancel();
    //         }
    //     }
    // }
    
    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void drawShiftedRecipeBook(DrawContext ctx, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        if (!uiShifted)
            return;

        int x = this.getLeft() - SHIFT_LEFT_AMOUNT;
        int y = this.getTop();

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

        for (int i = start; i < end; i++) {
            int index = i - start;
            int row = index / GRID_COLUMNS;
            int col = index % GRID_COLUMNS;

            int x = GRID_POSITION_X + col * ITEM_SIZE - SHIFT_LEFT_AMOUNT - SHIFT_AMOUNT;
            int y = GRID_POSITION_Y + row * ITEM_SIZE;

            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, SLOT_CRAFTABLE_SPRITE,
                    x, y, 0, 0, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE);

            ctx.drawItem(stacks.get(i), x + 4, y + 4);

        }

        prevPageButton.visible = currentPage > 0;
        nextPageButton.visible = currentPage < pageCount - 1;

        ItemStack hovered = getItemAtMouse(mouseX, mouseY);
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
            searchField.render(ctx, mouseX, mouseY, deltaTicks);
            if (this.pageCount > 1) {
                Text text = Text.translatable("gui.recipebook.page", new Object[]{this.currentPage + 1, this.pageCount});
                int x = this.getLeft() + PAGE_COUNT_POSITION_X - SHIFT_LEFT_AMOUNT;
                int y = this.getTop() + PAGE_COUNT_POSITION_Y;
                ctx.drawTextWithShadow(this.client.textRenderer, text, x, y, Colors.WHITE);
            }
        }
    }

}
