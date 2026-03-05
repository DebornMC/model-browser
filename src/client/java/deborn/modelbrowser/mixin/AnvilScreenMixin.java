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
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
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
    private int findMatchingInventorySlot(PlayerInventory inv, ItemStack target) {
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            ModelBrowser.LOGGER.info("inventory " + i + ": " + stack + " | clicked: " + target);
            if (!stack.isEmpty() && ItemStack.areItemsEqual(stack, target)) {
                return i;
            }
        }
        return -1;
    }

    private int playerInvIndexToHandlerSlot(ScreenHandler handler, int invSlot) {
        int playerInvStart = handler.slots.size() - 36;

        if (invSlot < 9) {
            return playerInvStart + 27 + invSlot;
        } else {
            return playerInvStart + (invSlot - 9);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (!modelBrowserWidget.isOpen()) return super.mouseClicked(click, doubled);

        if (modelBrowserWidget.getPrevPageButton().mouseClicked(click, doubled)) {
            return true;
        }
        if (modelBrowserWidget.getNextPageButton().mouseClicked(click, doubled)) {
            return true;
        }

        HandledScreenAccessor acc = (HandledScreenAccessor) (Object) this;
        ScreenHandler handler = acc.getHandler();

        ItemStack clickedStack = modelBrowserWidget.getItemAtMouse((int) click.x(), (int) click.y());
        if (clickedStack != null) {
            Text name = clickedStack.get(DataComponentTypes.CUSTOM_NAME);
            Identifier modelId = clickedStack.get(DataComponentTypes.ITEM_MODEL);
            if (name != null) {
                ClickableWidget.playClickSound(MinecraftClient.getInstance().getSoundManager());
                PlayerInventory inv = client.player.getInventory();
                int invSlot = findMatchingInventorySlot(inv, clickedStack);
                if (invSlot == -1) {
                    return true; // no matching item in inventory
                }
                int handlerSlot = playerInvIndexToHandlerSlot(handler, invSlot);
                
                client.interactionManager.clickSlot(
                    handler.syncId,
                    handlerSlot,
                    0,
                    SlotActionType.PICKUP_ALL,
                    client.player
                );
                client.interactionManager.clickSlot(
                    handler.syncId,
                    0,
                    0,
                    SlotActionType.PICKUP,
                    client.player
                );
                if (handler.getSlot(0).hasStack()) {
                    client.interactionManager.clickSlot(
                        handler.syncId,
                        handlerSlot,
                        0,
                        SlotActionType.PICKUP_ALL,
                        client.player
                    );
                }

                nameField.setText("");
                nameField.setText(name.getString());

                return true;
            }
            
            else if (modelId != null) {
                ClickableWidget.playClickSound(MinecraftClient.getInstance().getSoundManager());
                if (handler.getSlot(0).hasStack()) {
                    nameField.setText("");  
                    nameField.setText(modelId.toString());
                    return true;
                }
            }
        }		
        if (modelBrowserWidget.getSearchField() != null) {
            boolean bl = modelBrowserWidget.getSearchFieldRect() != null && modelBrowserWidget.getSearchFieldRect().contains((int) click.x(), (int) click.y());
            if (bl) {
                this.setFocused(modelBrowserWidget.getSearchField());
                modelBrowserWidget.getSearchField().setFocused(true);
                return true;
            }
            modelBrowserWidget.getSearchField().setFocused(false);
        }
        return super.mouseClicked(click, doubled);
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY, float deltaTicks) {
        modelBrowserWidget.render(ctx, mouseX, mouseY, deltaTicks);
        super.render(ctx, mouseX, mouseY, deltaTicks);
    }
}
