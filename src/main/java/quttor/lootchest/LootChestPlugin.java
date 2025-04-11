package quttor.lootchest;

import java.io.File;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class LootChestPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {

    private final Map<Location, String> lootChests = new HashMap<>();
    private final Map<String, LootTable> lootTables = new HashMap<>();
    private String scheduleMode;
    private int intervalSeconds;
    private List<LocalTime> scheduleTimes;
    private BukkitTask refillTask;
    private long nextRefillTimeMillis;
    private String refillMessage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("schedule.yml", false);
        loadLootTables();
        loadChests();
        loadSchedule();

        refillMessage = getConfig().getString("refill-message", "§eLoot chests have been refilled!");

        Bukkit.getPluginManager().registerEvents(new LootChestListener(this), this);

        if (getCommand("lootchest") != null) {
            getCommand("lootchest").setExecutor(this);
            getCommand("lootchest").setTabCompleter(this);
        } else {
            getLogger().severe("Command /lootchest not found in plugin.yml!");
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new LootChestExpansion(this).register();
            getLogger().info("PlaceholderAPI detected. Registered %lootchest_nextrefill% placeholder.");
        }
    }

    @Override
    public void onDisable() {
        if (refillTask != null) refillTask.cancel();
        saveChestData();
        HandlerList.unregisterAll(this);
    }

    private void loadLootTables() {
        FileConfiguration config = getConfig();
        if (!config.isConfigurationSection("loot-tables")) return;
        for (String tableId : config.getConfigurationSection("loot-tables").getKeys(false)) {
            LootTable table = new LootTable(tableId);
            List<Map<?, ?>> itemList = config.getMapList("loot-tables." + tableId + ".items");
            for (Map<?, ?> itemMap : itemList) {
                try {
                    String typeName = Objects.toString(itemMap.get("type"), "").toUpperCase();
                    Material material = Material.matchMaterial(typeName);
                    if (material == null) continue;
                    int min = itemMap.get("min") != null ? ((Number) itemMap.get("min")).intValue() : 1;
                    int max = itemMap.get("max") != null ? ((Number) itemMap.get("max")).intValue() : 1;
                    Map<String, Integer> enchantments = new HashMap<>();
                    if (itemMap.get("enchantments") instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> enchMap = (Map<String, Object>) itemMap.get("enchantments");
                        for (Map.Entry<String, Object> enchEntry : enchMap.entrySet()) {
                            enchantments.put(enchEntry.getKey().toUpperCase(), ((Number) enchEntry.getValue()).intValue());
                        }
                    }
                    table.addItem(new LootItem(material, min, max, enchantments));
                } catch (Exception ignored) {}
            }
            lootTables.put(tableId, table);
        }
    }

    private void loadChests() {
        FileConfiguration config = getConfig();
        List<Map<?, ?>> chestList = config.getMapList("chests");
        for (Map<?, ?> entry : chestList) {
            String worldName = Objects.toString(entry.get("world"), "");
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;
            try {
                int x = ((Number) entry.get("x")).intValue();
                int y = ((Number) entry.get("y")).intValue();
                int z = ((Number) entry.get("z")).intValue();
                String tableId = Objects.toString(entry.get("table"), "");
                if (!lootTables.containsKey(tableId)) continue;
                lootChests.put(new Location(world, x, y, z), tableId);
            } catch (Exception ignored) {}
        }
    }

    public boolean isLootChest(Location loc) {
        Location key = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return lootChests.containsKey(key);
    }

    private void loadSchedule() {
        File scheduleFile = new File(getDataFolder(), "schedule.yml");
        FileConfiguration scheduleConfig = YamlConfiguration.loadConfiguration(scheduleFile);
        scheduleMode = scheduleConfig.getString("mode", "interval").toLowerCase(Locale.ROOT);
        if (scheduleMode.equals("times")) {
            scheduleTimes = new ArrayList<>();
            List<String> timesList = scheduleConfig.getStringList("times");
            DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("H:mm");
            for (String timeStr : timesList) {
                try {
                    scheduleTimes.add(LocalTime.parse(timeStr, timeFormat));
                } catch (Exception ignored) {}
            }
            Collections.sort(scheduleTimes);
        } else {
            scheduleMode = "interval";
            intervalSeconds = scheduleConfig.getInt("interval", 3600);
            if (intervalSeconds < 1) intervalSeconds = 3600;
        }
        refillTask = null;
        nextRefillTimeMillis = 0;
    }

    private void startRefillTask() {
        if (scheduleMode.equals("interval")) {
            long intervalTicks = intervalSeconds * 20L;
            nextRefillTimeMillis = System.currentTimeMillis() + intervalSeconds * 1000L;
            refillTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
                doRefill();
                nextRefillTimeMillis = System.currentTimeMillis() + intervalSeconds * 1000L;
            }, intervalTicks, intervalTicks);
        } else {
            scheduleNextTimeRefill();
        }
    }

    private void scheduleNextTimeRefill() {
        if (scheduleTimes == null || scheduleTimes.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextDateTime = scheduleTimes.stream()
                .map(time -> LocalDateTime.of(now.toLocalDate(), time))
                .filter(dt -> !dt.isBefore(now))
                .findFirst()
                .orElse(LocalDateTime.of(now.toLocalDate().plusDays(1), scheduleTimes.get(0)));
        Duration wait = Duration.between(now, nextDateTime);
        long delayTicks = wait.getSeconds() * 20L;
        nextRefillTimeMillis = nextDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        refillTask = Bukkit.getScheduler().runTaskLater(this, () -> {
            doRefill();
            scheduleNextTimeRefill();
        }, delayTicks);
    }

    private void stopRefillTask() {
        if (refillTask != null) {
            refillTask.cancel();
            refillTask = null;
        }
        nextRefillTimeMillis = 0;
    }

    private void doRefill() {
        for (Map.Entry<Location, String> entry : lootChests.entrySet()) {
            Location loc = entry.getKey();
            String tableId = entry.getValue();
            LootTable table = lootTables.get(tableId);
            if (table == null) continue;
            Block block = loc.getBlock();
            if (!(block.getState() instanceof Container container)) continue;
            if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                loc.getWorld().loadChunk(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
            }
            container.getInventory().clear();
            table.fillInventory(container.getInventory());
        }

        Bukkit.broadcastMessage(refillMessage);
    }

    private String formatLocation(Location loc) {
        return loc.getWorld().getName() + "(" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + ")";
    }

    void saveChestData() {
        List<Map<String, Object>> chestList = new ArrayList<>();
        for (Map.Entry<Location, String> entry : lootChests.entrySet()) {
            Location loc = entry.getKey();
            String tableId = entry.getValue();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("world", loc.getWorld().getName());
            data.put("x", loc.getBlockX());
            data.put("y", loc.getBlockY());
            data.put("z", loc.getBlockZ());
            data.put("table", tableId);
            chestList.add(data);
        }
        getConfig().set("chests", chestList);
        saveConfig();
    }

    String formatTimeUntil(long futureMillis) {
        long diff = futureMillis - System.currentTimeMillis();
        if (diff < 0) return "0s";
        long seconds = diff / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return (hours > 0 ? hours + "h " : "") +
                (minutes > 0 ? minutes + "m " : "") +
                (hours == 0 && secs >= 0 ? secs + "s" : "").trim();
    }

    String getNextRefillPlaceholder() {
        if (refillTask == null || nextRefillTimeMillis <= 0) return "Paused";
        return formatTimeUntil(nextRefillTimeMillis);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lootchest.admin")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§eUsage: /lootchest <set|unset|begin|pause> [...]");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "begin" -> {
                if (refillTask != null) {
                    sender.sendMessage("§eLoot chest refills are already running.");
                } else {
                    startRefillTask();
                    sender.sendMessage("§aLoot chest refills started.");
                    if (nextRefillTimeMillis > 0) {
                        sender.sendMessage("§7Next refill scheduled in " + formatTimeUntil(nextRefillTimeMillis) + ".");
                    }
                }
                return true;
            }

            case "pause" -> {
                if (refillTask == null) {
                    sender.sendMessage("§eLoot chest refills are not running.");
                } else {
                    stopRefillTask();
                    sender.sendMessage("§aLoot chest refills paused.");
                }
                return true;
            }

            case "set" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can use this command.");
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /lootchest set <lootTableId>");
                    return true;
                }

                String tableId = args[1];
                if (!lootTables.containsKey(tableId)) {
                    player.sendMessage("§cLoot table '" + tableId + "' does not exist.");
                    return true;
                }

                Block target = player.getTargetBlockExact(5);
                if (target == null || !(target.getState() instanceof Container)) {
                    player.sendMessage("§cYou must be looking at a chest or barrel within 5 blocks.");
                    return true;
                }

                Location loc = target.getLocation();
                if (lootChests.containsKey(loc)) {
                    player.sendMessage("§cThat container is already set as a loot chest.");
                    return true;
                }

                lootChests.put(loc, tableId);
                saveChestData();
                player.sendMessage("§aMarked this container as a loot chest with table '" + tableId + "'.");
                return true;
            }

            case "unset" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can use this command.");
                    return true;
                }

                Block target = player.getTargetBlockExact(5);
                if (target == null || !(target.getState() instanceof Container)) {
                    player.sendMessage("§cYou must be looking at a loot chest (chest or barrel) within 5 blocks.");
                    return true;
                }

                Location loc = target.getLocation();
                if (!lootChests.containsKey(loc)) {
                    player.sendMessage("§cThat container is not marked as a loot chest.");
                    return true;
                }

                lootChests.remove(loc);
                saveChestData();
                player.sendMessage("§aThis container is no longer a loot chest.");
                return true;
            }

            default -> {
                sender.sendMessage("§eUsage: /lootchest <set|unset|begin|pause> [...]");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("lootchest.admin")) return Collections.emptyList();
        if (args.length == 1) return Arrays.asList("set", "unset", "begin", "pause").stream()
                .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                .toList();
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) return lootTables.keySet().stream()
                .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                .toList();
        return Collections.emptyList();
    }
}
