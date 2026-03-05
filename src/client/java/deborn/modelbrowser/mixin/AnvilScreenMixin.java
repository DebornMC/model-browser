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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Mixin(value = AnvilScreen.class, remap = false)
public abstract class AnvilScreenMixin extends Screen {
    @Shadow
    private TextFieldWidget nameField;

    private TexturedButtonWidget toggleButton;
    private ModelBrowserWidget modelBrowserWidget;
    private static final ButtonTextures RECIPE_BUTTON_TEXTURES = new ButtonTextures(
            Identifier.ofVanilla("icon/search"));
    private static final int UI_SHIFT_AMOUNT = 77;
    
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
        if (ModConfig.INSTANCE != null && !ModConfig.INSTANCE.showAnvilScreenTab) {
            return;
        }
        modelBrowserWidget = new ModelBrowserWidget(client, this.width, this.height);
        modelBrowserWidget.initialize();

        nameField.setWidth(86);
        toggleButton = new TexturedButtonWidget(
                this.getLeft() + 154,
                this.getTop() + 22,
                12, 12,
                RECIPE_BUTTON_TEXTURES,
                b -> toggleModelBrowser(),
                Text.translatable("modelbrowser.open_menu"));
        addDrawableChild(toggleButton);

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
        nameField.setX(nameField.getX() + dir);
        toggleButton.setX(toggleButton.getX() + dir);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void interceptKeys(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (!modelBrowserWidget.isOpen()) return;
        if (input.isEscape()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                client.player.closeHandledScreen();
            }
            cir.setReturnValue(true);
            return;
        }

        if (modelBrowserWidget.getSearchField().isActive()) {
            modelBrowserWidget.getSearchField().keyPressed(input);
            cir.setReturnValue(true);
        }
    }
    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void drawShiftedRecipeBook(DrawContext ctx, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        modelBrowserWidget.drawBackground(ctx);
    }

    @Inject(method = "drawForeground", at = @At("TAIL"))
    private void drawModelGrid(DrawContext ctx, int mouseX, int mouseY, CallbackInfo ci) {
        modelBrowserWidget.drawForeground(ctx, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (!modelBrowserWidget.isOpen()) return super.mouseClicked(click, doubled);

        HandledScreenAccessor acc = (HandledScreenAccessor) (Object) this;
        ScreenHandler handler = acc.getHandler();

        // let the widget deal with all of its own internal UI logic
        if (modelBrowserWidget.handleClick(click, doubled, handler, nameField, this)) {
            ModelBrowser.LOGGER.info("Handled mouse click in model browser");
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (!modelBrowserWidget.isOpen()) return super.mouseReleased(click);

        HandledScreenAccessor acc = (HandledScreenAccessor) (Object) this;
        ScreenHandler handler = acc.getHandler();

        if (modelBrowserWidget.handleRelease(click)) {
            return true;
        }

        return super.mouseReleased(click);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float deltaTicks) {
        modelBrowserWidget.render(ctx, mouseX, mouseY, deltaTicks);
        super.render(ctx, mouseX, mouseY, deltaTicks);
    }
}
