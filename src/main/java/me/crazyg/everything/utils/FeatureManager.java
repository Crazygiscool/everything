package me.crazyg.everything.utils;

import me.crazyg.everything.Everything;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class FeatureManager {

    public enum FeatureApplyOutcome {
        NOT_APPLIED,
        LIVE,
        RELOAD_REQUIRED,
        RESTART_REQUIRED,
        INVALID_PATH
    }

    private final Everything plugin;
    private File file;
    private FileConfiguration config;

    public FeatureManager(Everything plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        if (plugin.getDataFolder() != null && !plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        this.file = new File(plugin.getDataFolder(), "features.yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
                new YamlConfiguration().save(file);
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create features.yml: " + e.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public boolean getBoolean(String path, boolean fallback) {
        if (config == null || path == null) {
            return fallback;
        }
        return config.contains(path) ? config.getBoolean(path) : fallback;
    }

    public void setBoolean(String path, boolean value) {
        setBooleanSilently(path, value);
    }

    public boolean setBooleanSilently(String path, boolean value) {
        if (config == null || path == null || path.trim().isEmpty()) {
            return false;
        }
        try {
            config.set(path, value);
            save();
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Could not save feature '" + path + "': " + e.getMessage());
            return false;
        }
    }

    public void save() {
        try {
            if (file != null && config != null) {
                config.save(file);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save features.yml: " + e.getMessage());
        }
    }

    public void reload() {
        load();
    }
}
