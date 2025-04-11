package quttor.lootchest;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;

public class LootChestListener implements Listener {
    private final LootChestPlugin plugin;

    public LootChestListener(LootChestPlugin plugin) {
        this.plugin = plugin;
    }

    // Prevent players from breaking loot chests
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();
        // Only consider chest or barrel type blocks
        if (type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL) {
            Location loc = block.getLocation();
            if (plugin.isLootChest(loc)) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                if (player != null) {
                    player.sendMessage("§cThis chest is protected as a loot chest. Use /lootchest unset to remove it.");
                }
            }
        }
    }

    // Prevent explosions from destroying loot chests (TNT, creepers, etc.)
    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        // Remove any protected loot chest blocks from the explosion's block list
        event.blockList().removeIf(block -> {
            Material type = block.getType();
            if ((type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL)
                    && plugin.isLootChest(block.getLocation())) {
                return true; // remove this block from explosion effect
            }
            return false;
        });
    }

    // Also handle non-entity explosions (e.g., beds in nether, etc.)
    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> {
            Material type = block.getType();
            if ((type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL)
                    && plugin.isLootChest(block.getLocation())) {
                return true;
            }
            return false;
        });
    }
}
