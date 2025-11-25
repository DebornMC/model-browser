package deborn.modelbrowser.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {

    @Shadow private String newItemName;
    @Shadow protected abstract void updateResult();

    @Unique private Identifier pendingModelId = null;
    @Unique private Text savedCustomName = null;

    @Inject(method = "setNewItemName", at = @At("HEAD"))
    private void interceptRename(String newName, CallbackInfoReturnable<Boolean> cir) {
        if (newName == null) return;

        newName = sanitize(newName);

        if (newName.matches("^[a-z0-9_.-]+:[a-z0-9_/.-]+$")) {
            Identifier id = Identifier.tryParse(newName);
            if (id != null) {
                this.pendingModelId = id;

                Slot inputSlot = ((AnvilScreenHandler)(Object)this).getSlot(0);
                if (inputSlot.hasStack()) {
                    ItemStack in = inputSlot.getStack();
                    if (in.get(DataComponentTypes.CUSTOM_NAME) == null) {
                        this.savedCustomName = null;
                    } else {
                        this.savedCustomName = in.get(DataComponentTypes.CUSTOM_NAME);
                    }
                }
            }
        } else {
            this.pendingModelId = null;
            this.savedCustomName = null;
        }
    }

    @Shadow
    private static String sanitize(String name) {
        return name;
    }

    @Inject(method = "updateResult", at = @At("TAIL"))
    private void afterUpdateResult(CallbackInfo ci) {
        AnvilScreenHandler self = (AnvilScreenHandler)(Object)this;
        Identifier id = this.pendingModelId;
        if (id == null) return;

        Slot output = self.getSlot(2);
        if (!output.hasStack()) return;

        ItemStack out = output.getStack();

        out.set(DataComponentTypes.ITEM_MODEL, id);

        // Restore previous custom_name if there was one
        if (this.savedCustomName != null) {
            out.set(DataComponentTypes.CUSTOM_NAME, this.savedCustomName);
        } else {
            out.remove(DataComponentTypes.CUSTOM_NAME);
        }

        // make equippable
        EquippableComponent equippable = EquippableComponent.builder(EquipmentSlot.HEAD).build();
        if (out.get(DataComponentTypes.EQUIPPABLE) == null)
            out.set(DataComponentTypes.EQUIPPABLE, equippable);

        // remove glint
        out.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false);
        output.setStack(out.copy());
    }
}
