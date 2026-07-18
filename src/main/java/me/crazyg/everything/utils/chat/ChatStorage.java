package me.crazyg.everything.utils.chat;

import me.crazyg.everything.Everything;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Simple YAML-backed storage for the EverythingChat provider fallback.
 * Stores per-player prefix/suffix in chat.yml as:
 *
 * <uuid>:
 *   prefix: "..."
 *   suffix: "..."
 */
public class ChatStorage {

    private final Everything plugin;
    private final File file;
    private final FileConfiguration config;

    public ChatStorage(Everything plugin) {
        this.plugin = plugin;

        File folder = new File(plugin.getDataFolder(), "data");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File chatResource = new File(folder, "chat.yml");
        if (!chatResource.exists()) {
            plugin.saveResource("data/chat.yml", false);
        }

        this.file = chatResource;
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public String getPlayerPrefix(UUID uuid) {
        return config.getString(uuid.toString() + ".prefix", "");
    }

    public String getPlayerSuffix(UUID uuid) {
        return config.getString(uuid.toString() + ".suffix", "");
    }

    public void setPlayerPrefix(UUID uuid, String prefix) {
        config.set(uuid.toString() + ".prefix", prefix);
        save();
    }

    public void setPlayerSuffix(UUID uuid, String suffix) {
        config.set(uuid.toString() + ".suffix", suffix);
        save();
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save chat.yml: " + e.getMessage());
        }
    }
}
