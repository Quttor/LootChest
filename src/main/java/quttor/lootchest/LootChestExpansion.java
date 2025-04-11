package quttor.lootchest;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;


public class LootChestExpansion extends PlaceholderExpansion {
    private final LootChestPlugin plugin;

    public LootChestExpansion(LootChestPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "lootchest";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        if (identifier.equalsIgnoreCase("nextrefill")) {
            return plugin.getNextRefillPlaceholder();
        }
        return null;
    }
}
