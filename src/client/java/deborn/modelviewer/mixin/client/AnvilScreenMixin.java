package deborn.modelviewer.mixin.client;

import deborn.modelviewer.ModelViewerScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends Screen {
    protected AnvilScreenMixin(Text title) {
        super(title);
    }

    /**
     * Injects a custom button into the Anvil GUI after it sets up its widgets.
     */
    @Inject(method = "setup", at = @At("TAIL"))
    private void addModelViewerButton(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        // Place near top-right corner of the anvil GUI
        int buttonX = this.width / 2 + 100;
        int buttonY = this.height / 2 - 70;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Models"), button -> {
            client.setScreen(new ModelViewerScreen());
        }).dimensions(buttonX, buttonY, 60, 20).build());
    }
}
