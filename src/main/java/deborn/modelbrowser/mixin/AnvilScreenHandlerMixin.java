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
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.equipment.Equippable;

@Mixin(AnvilMenu.class)
public abstract class AnvilScreenHandlerMixin {

    @Shadow private String itemName;
    @Shadow protected abstract void createResult();

    @Unique private Identifier pendingModelId = null;
    @Unique private Component savedCustomName = null;

    @Inject(method = "setItemName", at = @At("HEAD"))
    private void interceptRename(String newName, CallbackInfoReturnable<Boolean> cir) {
        if (newName == null) return;

        newName = validateName(newName);

        if (newName.matches("^[a-z0-9_.-]+:[a-z0-9_/.-]+$")) {
            Identifier id = Identifier.tryParse(newName);
            if (id != null) {
                this.pendingModelId = id;

                Slot inputSlot = ((AnvilMenu)(Object)this).getSlot(0);
                if (inputSlot.hasItem()) {
                    ItemStack in = inputSlot.getItem();
                    if (in.get(DataComponents.CUSTOM_NAME) == null) {
                        this.savedCustomName = null;
                    } else {
                        this.savedCustomName = in.get(DataComponents.CUSTOM_NAME);
                    }
                }
            }
        } else {
            this.pendingModelId = null;
            this.savedCustomName = null;
        }
    }

    @Shadow
    private static String validateName(String name) {
        return name;
    }

    @Inject(method = "createResult", at = @At("TAIL"))
    private void afterUpdateResult(CallbackInfo ci) {
        AnvilMenu self = (AnvilMenu)(Object)this;
        Identifier id = this.pendingModelId;
        if (id == null) return;                                                                                                                                                                              

        Slot output = self.getSlot(2);
        if (!output.hasItem()) return;

        ItemStack out = output.getItem();

        out.set(DataComponents.ITEM_MODEL, id);

        // Restore previous custom_name if there was one, otherwise remove it
        if (this.savedCustomName != null) {
            out.set(DataComponents.CUSTOM_NAME, this.savedCustomName);
        } else {
            out.remove(DataComponents.CUSTOM_NAME);
        }

        // make equippable
        Equippable equippable = Equippable.builder(EquipmentSlot.HEAD).build();
        if (out.get(DataComponents.EQUIPPABLE) == null)
            out.set(DataComponents.EQUIPPABLE, equippable);

        // remove glint
        out.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
        
        // custom data
        CompoundTag compound = new CompoundTag();
        compound.putBoolean("model_browser_data", true);
        CustomData customData = CustomData.of(compound);
        out.set(DataComponents.CUSTOM_DATA, customData);


        output.setByPlayer(out.copy());
    }

    @Inject(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/StringUtil;isBlank(Ljava/lang/String;)Z"))
    private void resetItemModel(
        CallbackInfo ci,
        @Local(ordinal = 0) LocalIntRef i, 
        @Local(ordinal = 1) LocalIntRef j, 
        @Local(ordinal = 0) LocalRef<ItemStack> itemStack, 
        @Local(ordinal = 1) LocalRef<ItemStack> itemStack2
    ) {
        if(!itemStack.get().has(DataComponents.CUSTOM_DATA)) return;
        CustomData customDataComp = itemStack.get().get(DataComponents.CUSTOM_DATA);
        if (customDataComp == null) return;
        CompoundTag tag = customDataComp.copyTag();
        if(!tag.getBooleanOr("model_browser", false)) {
            if (StringUtil.isBlank(this.itemName) || this.itemName == null) {
                if (!itemStack.get().has(DataComponents.CUSTOM_NAME)) {
                    j.set(1);
                    i.set(j.get()+i.get());
                }
                tag.remove("model_browser_data");
                if (tag.isEmpty()) {
                    itemStack2.get().remove(DataComponents.CUSTOM_DATA);
                } else {
                    itemStack2.get().set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                }
                itemStack2.get().set(DataComponents.ITEM_MODEL, itemStack2.get().getItem().getDefaultInstance().get(DataComponents.ITEM_MODEL));
                itemStack2.get().set(DataComponents.ITEM_NAME, itemStack2.get().getItem().getDefaultInstance().get(DataComponents.ITEM_NAME));
                itemStack2.get().set(DataComponents.EQUIPPABLE, itemStack2.get().getItem().getDefaultInstance().get(DataComponents.EQUIPPABLE));
                itemStack2.get().remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            }
        }
    }
}
