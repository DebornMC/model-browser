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
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends Screen {

    @Shadow
    private TextFieldWidget nameField; // Shadow the text input
    private static final Identifier RECIPE_BOOK_TEXTURE = Identifier.ofVanilla("textures/gui/recipe_book.png");
    private static final ButtonTextures RECIPE_BUTTON_TEXTURES = new ButtonTextures(
        Identifier.ofVanilla("icon/search")
    );

    private boolean uiShifted = false;
    private TexturedButtonWidget toggleButton;

    protected AnvilScreenMixin(Text title) {
        super(title);
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
    private void applyShiftAfterSetup(CallbackInfo ci) {
        if (uiShifted) {
            HandledScreenAccessor accessor = (HandledScreenAccessor) (Object) this;
            int shiftAmount = 80;

            accessor.setX(accessor.getX() + shiftAmount);
            toggleButton.setX(toggleButton.getX() + shiftAmount);
            nameField.setX(nameField.getX() + shiftAmount);
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
        if (uiShifted) toggleButton.setX(toggleButton.getX() + 80);

        // Add to drawable children every setup
        this.addDrawableChild(toggleButton);
    }


    private void toggleGuiShift() {
    HandledScreenAccessor accessor = (HandledScreenAccessor) (Object) this;
    int shiftAmount = 77;

    if (uiShifted) {
        accessor.setX(accessor.getX() - shiftAmount);
        toggleButton.setX(toggleButton.getX() - shiftAmount);
        nameField.setX(nameField.getX() - shiftAmount);
    } else {
        accessor.setX(accessor.getX() + shiftAmount);
        toggleButton.setX(toggleButton.getX() + shiftAmount);
        nameField.setX(nameField.getX() + shiftAmount);
    }

    uiShifted = !uiShifted;
}


}





