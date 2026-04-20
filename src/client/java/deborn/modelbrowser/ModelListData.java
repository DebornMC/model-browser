package deborn.modelbrowser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class ModelListData {
    private static boolean loaded = false;
    private static final List<ItemStack> MODEL_STACKS = new ArrayList<>();
    private static final List<ItemStack> FILTERED_STACKS = new ArrayList<>();

    public static boolean isLoaded() {
        return loaded;
    }

    public static synchronized void setStacks(List<ItemStack> stacks) {
        MODEL_STACKS.clear();
        MODEL_STACKS.addAll(stacks);
        FILTERED_STACKS.clear();
        FILTERED_STACKS.addAll(stacks);
        loaded = true;
    }

    public static synchronized void filter(String text) {
        FILTERED_STACKS.clear();
        String lower = text.toLowerCase();
        for (ItemStack stack : MODEL_STACKS) {
            Identifier id = stack.get(DataComponents.ITEM_MODEL);
            if (id != null && id.toString().toLowerCase().contains(lower)) {
                FILTERED_STACKS.add(stack);
            }
        }
    }

    public static synchronized List<ItemStack> getStacks() {
        return Collections.unmodifiableList(MODEL_STACKS);
    }

    public static synchronized List<ItemStack> getFiltered() {
        return List.copyOf(FILTERED_STACKS);
    }
}
