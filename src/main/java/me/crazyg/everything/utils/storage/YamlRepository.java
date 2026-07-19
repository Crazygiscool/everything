package me.crazyg.everything.utils.storage;

import me.crazyg.everything.Everything;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * Shared base for the plugin's flat YAML data stores (eco, chat, reports,
 * homes, warps, ...). Centralizes the "ensure folder -> extract resource
 * -> load -> save" boilerplate that was previously copy-pasted across every
 * storage class.
 */
public abstract class YamlRepository {

    protected final Everything plugin;
    protected final File folder;
    protected final File file;
    protected FileConfiguration config;

    protected YamlRepository(Everything plugin, String subfolder,
                              String resourcePath, String fileName) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), subfolder);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        this.file = new File(folder, fileName);
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    /** Reload the in-memory config from disk. */
    protected void load() {
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    /** Persist the in-memory config to disk. */
    protected void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe(
                "Failed to save " + file.getName() + ": " + e.getMessage());
        }
    }
}
