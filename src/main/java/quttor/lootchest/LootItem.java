package quttor.lootchest;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class LootItem {
    private final Material type;
    private final int minAmount;
    private final int maxAmount;
    private final Map<String, Integer> enchantments; // enchantment name -> level

    public LootItem(Material type, int minAmount, int maxAmount, Map<String, Integer> enchantments) {
        this.type = type;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.enchantments = enchantments;
    }

    /**
     * Generate an ItemStack based on this loot item definition.
     * The quantity is randomized between minAmount and maxAmount (inclusive).
     * Enchantments (if any) are applied to the item.
     * If the random amount turns out to be zero, this returns null (nothing to add).
     */
    public ItemStack generateItem() {
        // Determine amount (random between min and max inclusive)
        int amt;
        if (minAmount == maxAmount) {
            amt = minAmount;
        } else {
            int low = Math.min(minAmount, maxAmount);
            int high = Math.max(minAmount, maxAmount);
            amt = ThreadLocalRandom.current().nextInt(high - low + 1) + low;
        }
        if (amt <= 0) {
            return null; // no item to generate
        }
        ItemStack item = new ItemStack(type, amt);
        // Apply enchantments if any
        if (enchantments != null && !enchantments.isEmpty()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                for (Map.Entry<String, Integer> enchEntry : enchantments.entrySet()) {
                    Enchantment enchantment = Enchantment.getByName(enchEntry.getKey());
                    if (enchantment == null) {
                        // Unknown enchantment name, skip
                        continue;
                    }
                    int level = enchEntry.getValue();
                    // Use addEnchant with ignoreLevelRestriction to allow levels beyond normal max
                    meta.addEnchant(enchantment, level, true);
                }
                item.setItemMeta(meta);
            }
        }
        return item;
    }
}
