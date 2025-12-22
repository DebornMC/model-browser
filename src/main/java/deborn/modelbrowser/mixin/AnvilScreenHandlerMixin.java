package deborn.modelbrowser.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringHelper;

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

        // Restore previous custom_name if there was one, otherwise remove it
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
        
        // custom data
        NbtCompound compound = new NbtCompound();
        compound.putBoolean("model_browser_data", true);
        NbtComponent customData = NbtComponent.of(compound);
        out.set(DataComponentTypes.CUSTOM_DATA, customData);


        output.setStack(out.copy());
    }

    @Inject(method = "updateResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/StringHelper;isBlank(Ljava/lang/String;)Z"))
    private void resetItemModel(
        CallbackInfo ci,
        @Local(ordinal = 0) LocalIntRef i, 
        @Local(ordinal = 1) LocalIntRef j, 
        @Local(ordinal = 0) LocalRef<ItemStack> itemStack, 
        @Local(ordinal = 1) LocalRef<ItemStack> itemStack2
    ) {
        if(!itemStack.get().contains(DataComponentTypes.CUSTOM_DATA)) return;
        NbtComponent customDataComp = itemStack.get().get(DataComponentTypes.CUSTOM_DATA);
        if (customDataComp == null) return;
        NbtCompound tag = customDataComp.copyNbt();
        if(!tag.getBoolean("model_browser", false)) {
            if (StringHelper.isBlank(this.newItemName) || this.newItemName == null) {
                if (!itemStack.get().contains(DataComponentTypes.CUSTOM_NAME)) {
                    j.set(1);
                    i.set(j.get()+i.get());
                }
                tag.remove("model_browser_data");
                if (tag.isEmpty()) {
                    itemStack2.get().remove(DataComponentTypes.CUSTOM_DATA);
                } else {
                    itemStack2.get().set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
                }
                itemStack2.get().set(DataComponentTypes.ITEM_MODEL, itemStack2.get().getItem().getDefaultStack().get(DataComponentTypes.ITEM_MODEL));
                itemStack2.get().set(DataComponentTypes.ITEM_NAME, itemStack2.get().getItem().getDefaultStack().get(DataComponentTypes.ITEM_NAME));
                itemStack2.get().set(DataComponentTypes.EQUIPPABLE, itemStack2.get().getItem().getDefaultStack().get(DataComponentTypes.EQUIPPABLE));
                itemStack2.get().remove(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
            }
        }
    }
}
