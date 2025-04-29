package me.crazyg.everything.kits;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.crazyg.everything.Everything;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class KitManager {
    private final Everything plugin;
    private final Map<String, Kit> kits = new HashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final File kitsFile;
    private FileConfiguration kitsConfig;

    public KitManager(Everything plugin) {
        this.plugin = plugin;
        this.kitsFile = new File(plugin.getDataFolder(), "kits.yml");
        loadKits();
    }

    private void loadKits() {
        if (!kitsFile.exists()) {
            plugin.saveResource("kits.yml", false);
        }
        kitsConfig = YamlConfiguration.loadConfiguration(kitsFile);
        
        for (String key : kitsConfig.getKeys(false)) {
            List<ItemStack> items = (List<ItemStack>) kitsConfig.getList(key + ".items");
            long cooldown = kitsConfig.getLong(key + ".cooldown");
            String permission = kitsConfig.getString(key + ".permission", "everything.kit." + key);
            kits.put(key, new Kit(key, items, cooldown, permission));
        }
    }

    private void saveKits() {
        for (Kit kit : kits.values()) {
            String path = kit.getName() + ".";
            kitsConfig.set(path + "items", kit.getItems());
            kitsConfig.set(path + "cooldown", kit.getCooldown());
            kitsConfig.set(path + "permission", kit.getPermission());
        }
        try {
            kitsConfig.save(kitsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save kits file!");
        }
    }

    public boolean createKit(String name, List<ItemStack> items, long cooldown) {
        if (kits.containsKey(name)) {
            return false;
        }
        Kit kit = new Kit(name, items, cooldown, "everything.kit." + name);
        kits.put(name, kit);
        saveKits();
        return true;
    }

    public boolean deleteKit(String name) {
        if (!kits.containsKey(name)) {
            return false;
        }
        kits.remove(name);
        kitsConfig.set(name, null);
        saveKits();
        return true;
    }

    public boolean giveKit(Player player, String kitName) {
        Kit kit = kits.get(kitName);
        if (kit == null) {
            return false;
        }

        if (!player.hasPermission(kit.getPermission())) {
            return false;
        }

        // Check cooldown
        Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        long lastUsed = playerCooldowns.getOrDefault(kitName, 0L);
        long now = System.currentTimeMillis();
        
        if (lastUsed + (kit.getCooldown() * 1000) > now) {
            return false;
        }

        // Give items
        for (ItemStack item : kit.getItems()) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
            leftover.values().forEach(leftoverItem -> 
                player.getWorld().dropItemNaturally(player.getLocation(), leftoverItem));
        }

        // Update cooldown
        playerCooldowns.put(kitName, now);
        return true;
    }

    public Kit getKit(String name) {
        return kits.get(name);
    }

    public Map<String, Kit> getKits() {
        return Collections.unmodifiableMap(kits);
    }

    public long getRemainingCooldown(Player player, String kitName) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) {
            return 0;
        }
        
        Long lastUsed = playerCooldowns.get(kitName);
        if (lastUsed == null) {
            return 0;
        }

        Kit kit = kits.get(kitName);
        if (kit == null) {
            return 0;
        }

        long now = System.currentTimeMillis();
        long cooldownEnd = lastUsed + (kit.getCooldown() * 1000);
        return Math.max(0, (cooldownEnd - now) / 1000);
    }
}