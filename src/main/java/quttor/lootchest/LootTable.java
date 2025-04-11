package quttor.lootchest;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LootTable {
    private final String id;
    private final List<LootItem> items = new ArrayList<>();

    public LootTable(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void addItem(LootItem item) {
        items.add(item);
    }

    /**
     * Fill the given inventory with items from this loot table.
     * All existing items in the inventory should be cleared before calling this.
     * Items are placed into random empty slots.
     */
    public void fillInventory(Inventory inventory) {
        int size = inventory.getSize();
        List<Integer> availableSlots = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            availableSlots.add(i);
        }
        Random random = ThreadLocalRandom.current();
        for (LootItem lootItem : items) {
            ItemStack stack = lootItem.generateItem();
            if (stack == null || stack.getAmount() <= 0) {
                continue; // no item generated (e.g., random 0 amount)
            }
            if (availableSlots.isEmpty()) {
                // Inventory is full, cannot place more items
                break;
            }
            // Pick a random available slot for this item
            int slotIndex = random.nextInt(availableSlots.size());
            int slot = availableSlots.remove(slotIndex);
            inventory.setItem(slot, stack);
        }
    }
}
