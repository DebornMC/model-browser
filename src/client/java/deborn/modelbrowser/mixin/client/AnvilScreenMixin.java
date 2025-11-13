package deborn.modelbrowser.mixin.client;

import deborn.modelbrowser.ModelBrowserScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends Screen {
    // Recipe button textures — matches vanilla
    private static final ButtonTextures RECIPE_BUTTON_TEXTURES = new ButtonTextures(
        Identifier.ofVanilla("recipe_book/button"),
        Identifier.ofVanilla("recipe_book/button_highlighted")
    );

    protected AnvilScreenMixin(Text title) {
        super(title);
    }

    /**
     * Adds a recipe-style model browser button to the Anvil screen.
     */
    @Inject(method = "setup", at = @At("TAIL"))
    private void addModelBrowserButton(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        // Center GUI coordinates (based on 176x166 default anvil size)
        int guiLeft = (this.width - 176) / 2;
        int guiTop = (this.height - 166) / 2;

        // Position button similarly to the recipe book button in InventoryScreen
        int buttonX = guiLeft + 149;
        int buttonY = guiTop + 64;

        // Create a textured button with vanilla recipe-book visuals
        TexturedButtonWidget button = new TexturedButtonWidget(
            buttonX,
            buttonY,
            20,
            18,
            RECIPE_BUTTON_TEXTURES,
            b -> client.setScreen(new ModelBrowserScreen()),
            Text.literal("Model Browser")
        );

        this.addDrawableChild(button);
    }
}
