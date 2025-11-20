package deborn.modelbrowser;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModelListData {
    private static boolean loaded = false;
    private static final List<ItemStack> MODEL_STACKS = new ArrayList<>();

    public static boolean isLoaded() {
        return loaded;
    }

    public static synchronized void setStacks(List<ItemStack> stacks) {
        MODEL_STACKS.clear();
        MODEL_STACKS.addAll(stacks);
        loaded = true;
    }

    public static synchronized List<ItemStack> getStacks() {
        return Collections.unmodifiableList(MODEL_STACKS);
    }
}
