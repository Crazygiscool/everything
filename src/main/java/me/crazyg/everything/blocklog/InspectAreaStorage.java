package me.crazyg.everything.blocklog;

import me.crazyg.everything.Everything;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Persists per-player inspect areas to a YAML file so they survive restarts.
 */
public class InspectAreaStorage {

    private final File file;
    private YamlConfiguration config;

    public InspectAreaStorage(Everything plugin) {
        File folder = new File(plugin.getDataFolder(), "data");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        this.file = new File(folder, "inspect-areas.yml");
        reload();
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save(UUID uuid, InspectWand.InspectArea area) {
        String path = uuid.toString();
        config.set(path + ".world", area.center.getWorld().getName());
        config.set(path + ".x", area.center.getBlockX());
        config.set(path + ".y", area.center.getBlockY());
        config.set(path + ".z", area.center.getBlockZ());
        config.set(path + ".size", area.size);
        saveFile();
    }

    public void remove(UUID uuid) {
        config.set(uuid.toString(), null);
        saveFile();
    }

    public InspectWand.InspectArea load(UUID uuid) {
        String path = uuid.toString();
        if (!config.contains(path)) return null;
        String worldName = config.getString(path + ".world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        int x = config.getInt(path + ".x");
        int y = config.getInt(path + ".y");
        int z = config.getInt(path + ".z");
        int size = config.getInt(path + ".size", 10);
        Location center = new Location(world, x, y, z);
        return new InspectWand.InspectArea(center, size);
    }

    private void saveFile() {
        try {
            config.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.WARNING,
                "Failed to save inspect-areas.yml: " + e.getMessage());
        }
    }
}
