package deborn.modelbrowser.mixin.client;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {

    @Accessor("x")
    void setX(int x);

    @Accessor("x")
    int getX();

    @Accessor("y")
    void setY(int y);

    @Accessor("y")
    int getY();
}
