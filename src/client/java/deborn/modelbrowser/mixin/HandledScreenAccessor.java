package deborn.modelbrowser.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface HandledScreenAccessor {
    @Accessor("menu")
    AbstractContainerMenu getHandler();

    @Accessor("leftPos")
    void setX(int x);

    @Accessor("leftPos")
    int getX();

    @Accessor("topPos")
    void setY(int y);

    @Accessor("topPos")
    int getY();
}
