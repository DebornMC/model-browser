package deborn.modelbrowser;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.resource.ResourceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ModelBrowserScreen extends Screen {
    public static int scrollOffset = 0;
    public TextFieldWidget searchBox;
    private final List<ItemStack> modelStacks = new ArrayList<>();
    public List<ItemStack> allModelStacks = new ArrayList<>();

    private static final int ITEM_SIZE = 20;
    private static final int GRID_COLUMNS = 9;

    public static ModelBrowserScreen INSTANCE;

    public ModelBrowserScreen() {
        super(Text.literal("Model Browser"));
        INSTANCE = this;
    }

    public List<ItemStack> getModelStacks() {
        return modelStacks;
    }

    @Override
    protected void init() {
        MinecraftClient client = this.client;
        if (client == null) return;

        this.searchBox = new TextFieldWidget(this.textRenderer, this.width / 2 - 80, 20, 160, 20, Text.literal("Search"));
        this.searchBox.setMaxLength(50);
        this.addDrawableChild(this.searchBox);
        this.setInitialFocus(this.searchBox);
        
        this.searchBox.setChangedListener(this::filterItems);
        
        new Thread(() -> loadResourcePackItemModels(client.getResourceManager())).start();

        
        filterItems("");
    }

    private void filterItems(String searchText) {
        String lower = searchText.toLowerCase();
        synchronized (modelStacks) {
            modelStacks.clear();
            for (ItemStack stack : allModelStacks) {
                Identifier id = stack.get(DataComponentTypes.ITEM_MODEL);
                if (id != null && id.toString().toLowerCase().contains(lower)) {
                    modelStacks.add(stack);
                }
            }
        }
    }

    public void loadResourcePackItemModels(ResourceManager manager) {
        allModelStacks.clear();

        try {
            for (String namespace : manager.getAllNamespaces()) {
                if (namespace.equals("minecraft")) continue;

                Map<Identifier, ?> resources = manager.findResources("items", path -> path.getPath().endsWith(".json"));

                for (Identifier resourceId : resources.keySet()) {
                    if (!resourceId.getNamespace().equals(namespace)) continue;

                    String path = resourceId.getPath();
                    if (!path.startsWith("items/") || !path.endsWith(".json")) continue;

                    String itemId = path.substring("items/".length(), path.length() - ".json".length());
                    Identifier itemIdentifier = Identifier.tryParse(namespace + ":" + itemId);
                    if (itemIdentifier == null) continue;

                    ItemStack stack = new ItemStack(Items.WHITE_STAINED_GLASS);
                    stack.set(DataComponentTypes.ITEM_MODEL, itemIdentifier);
                    allModelStacks.add(stack);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        MinecraftClient.getInstance().execute(() -> filterItems(this.searchBox.getText()));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        this.searchBox.render(context, mouseX, mouseY, delta);

        ItemRenderer renderer = this.client.getItemRenderer();
        int gridX = this.width / 2 - (GRID_COLUMNS * ITEM_SIZE / 2);
        int gridY = 50;

        int col = 0;
        int row = 0;
        ItemStack hovered = null;
        int hoveredX = 0, hoveredY = 0;

        synchronized (modelStacks) {
            for (ItemStack stack : modelStacks) {
                int x = gridX + col * ITEM_SIZE;
                int y = gridY + row * ITEM_SIZE;

                context.drawItem(stack, x, y);
                if (mouseX >= x && mouseX <= x + ITEM_SIZE && mouseY >= y && mouseY <= y + ITEM_SIZE) {
                    hovered = stack;
                    hoveredX = x;
                    hoveredY = y;
                }

                col++;
                if (col >= GRID_COLUMNS) {
                    col = 0;
                    row++;
                }
            }
        }

        
        if (hovered != null) {
            Identifier modelId = hovered.get(DataComponentTypes.ITEM_MODEL);
            if (modelId != null) {
                
                int overlayX = hoveredX;
                int overlayY = hoveredY;
                int overlaySize = 16;

                
                context.fill(overlayX, overlayY, overlayX + overlaySize, overlayY + overlaySize, 0x66FFFFFF);

                
                context.drawTooltip(this.textRenderer, Text.literal(modelId.toString()), mouseX, mouseY);
            }
        }


        super.render(context, mouseX, mouseY, delta);
    }
    
}
