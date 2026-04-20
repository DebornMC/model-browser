package deborn.modelbrowser.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import deborn.modelbrowser.ModelBrowser;
import deborn.modelbrowser.config.ModConfig;
import deborn.modelbrowser.gui.ModelBrowserWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;

@Mixin(value = AnvilScreen.class, remap = false)
public abstract class AnvilScreenMixin extends Screen {
    @Shadow
    private EditBox name;

    private ImageButton toggleButton;
    private ModelBrowserWidget modelBrowserWidget;
    private static final WidgetSprites RECIPE_BUTTON_TEXTURES = new WidgetSprites(
            Identifier.withDefaultNamespace("icon/search"));
    private static final int UI_SHIFT_AMOUNT = 77;

    protected AnvilScreenMixin(Component title) {
        super(title);
    }

    private int getTop() {
        return (this.height - 166) / 2;
    }

    private int getLeft() {
        return (this.width - 176) / 2;
    }

    @Shadow
    protected abstract void subInit();

    @Inject(method = "subInit", at = @At("TAIL"))
    private void setupUI(CallbackInfo ci) {
        if (ModConfig.INSTANCE != null && !ModConfig.INSTANCE.showAnvilScreenTab) {
            return;
        }
        modelBrowserWidget = new ModelBrowserWidget(minecraft, this.width, this.height);
        modelBrowserWidget.initialize();

        name.setWidth(86);
        toggleButton = new ImageButton(
                this.getLeft() + 154,
                this.getTop() + 22,
                12, 12,
                RECIPE_BUTTON_TEXTURES,
                b -> toggleModelBrowser(),
                Component.translatable("modelbrowser.open_menu"));
        addRenderableWidget(toggleButton);

        if (modelBrowserWidget.isOpen()) {
            shiftUI();
        }
    }

    private void toggleModelBrowser() {
        modelBrowserWidget.toggle();
        shiftUI();
    }

    private void shiftUI() {
        int dir = modelBrowserWidget.isOpen() ? UI_SHIFT_AMOUNT : -UI_SHIFT_AMOUNT;
        HandledScreenAccessor acc = (HandledScreenAccessor) (Object) this;
        acc.setX(acc.getX() + dir);
        name.setX(name.getX() + dir);
        toggleButton.setX(toggleButton.getX() + dir);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void interceptKeys(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if (!modelBrowserWidget.isOpen())
            return;
        if (input.isEscape()) {
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.player != null) {
                client.player.closeContainer();
            }
            cir.setReturnValue(true);
            return;
        }

        if (modelBrowserWidget.getSearchField().canConsumeInput()) {
            modelBrowserWidget.getSearchField().keyPressed(input);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void drawShiftedRecipeBook(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        modelBrowserWidget.drawBackground(ctx);
    }

    @Inject(method = "extractLabels", at = @At("TAIL"))
    private void drawModelGrid(GuiGraphicsExtractor ctx, int mouseX, int mouseY, CallbackInfo ci) {
        modelBrowserWidget.drawForeground(ctx, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (!modelBrowserWidget.isOpen())
            return super.mouseClicked(click, doubled);

        HandledScreenAccessor acc = (HandledScreenAccessor) (Object) this;
        AbstractContainerMenu handler = acc.getHandler();

        if (modelBrowserWidget.handleClick(click, doubled, handler, name, this)) {
            // ModelBrowser.LOGGER.info("Handled mouse click in model browser");
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (!modelBrowserWidget.isOpen())
            return super.mouseReleased(click);

        HandledScreenAccessor acc = (HandledScreenAccessor) (Object) this;
        AbstractContainerMenu handler = acc.getHandler();

        if (modelBrowserWidget.handleRelease(click)) {
            return true;
        }

        return super.mouseReleased(click);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float deltaTicks) {
        modelBrowserWidget.extractRenderState(ctx, mouseX, mouseY, deltaTicks);
        super.extractRenderState(ctx, mouseX, mouseY, deltaTicks);
    }
}
