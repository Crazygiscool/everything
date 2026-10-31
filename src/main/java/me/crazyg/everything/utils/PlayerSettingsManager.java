package me.crazyg.everything.utils;

import java.util.UUID;
import me.crazyg.everything.Everything;
import me.crazyg.everything.utils.storage.PlayerSettingsStorage;
import org.bukkit.configuration.file.FileConfiguration;

public class PlayerSettingsManager {

    private final Everything plugin;
    private final PlayerSettingsStorage storage;

    public PlayerSettingsManager(Everything plugin) {
        this.plugin = plugin;
        this.storage = new PlayerSettingsStorage(plugin);
    }

    public PlayerSettingsStorage getStorage() {
        return storage;
    }

    public boolean getBoolean(UUID uuid,
                              String key,
                              boolean fallback) {
        FileConfiguration cfg = storage.getStorageConfig();
        String path = "settings." + uuid + "." + key;
        if (cfg.contains(path)) {
            return cfg.getBoolean(path);
        }
        return plugin.getConfig().getBoolean(
            "gui.player-settings.defaults." + key, fallback);
    }

    public void setBoolean(UUID uuid,
                           String key,
                           boolean value) {
        FileConfiguration cfg = storage.getStorageConfig();
        cfg.set("settings." + uuid + "." + key, value);
        storage.saveSettings();
    }

    public String getString(UUID uuid,
                           String key,
                           String fallback) {
        FileConfiguration cfg = storage.getStorageConfig();
        String path = "settings." + uuid + "." + key;
        if (cfg.contains(path)) {
            return cfg.getString(path, fallback);
        }
        return fallback;
    }

    public void setString(UUID uuid,
                          String key,
                          String value) {
        FileConfiguration cfg = storage.getStorageConfig();
        cfg.set("settings." + uuid + "." + key, value);
        storage.saveSettings();
    }

    /** Returns the per-player /help mode, falling back to the global config. */
    public String getHelpMode(UUID uuid) {
        FileConfiguration cfg = storage.getStorageConfig();
        String path = "settings." + uuid + ".help-mode";
        if (cfg.contains(path)) {
            String value = cfg.getString(path, "everything");
            if ("help".equalsIgnoreCase(value) || "disabled".equalsIgnoreCase(value)) {
                return value.toLowerCase();
            }
            return "everything";
        }
        String global = plugin.getConfig().getString("gui.help.mode", "everything");
        if ("command".equalsIgnoreCase(global) || "default".equalsIgnoreCase(global)) {
            return "everything";
        }
        return global == null ? "everything" : global.toLowerCase();
    }

    public void setHelpMode(UUID uuid, String mode) {
        String normalized;
        if (mode == null || mode.trim().isEmpty()) {
            normalized = "everything";
        } else {
            String lower = mode.trim().toLowerCase();
            if ("help".equals(lower) || "disabled".equals(lower)) {
                normalized = lower;
            } else {
                normalized = "everything";
            }
        }
        FileConfiguration cfg = storage.getStorageConfig();
        cfg.set("settings." + uuid + ".help-mode", normalized);
        storage.saveSettings();
    }
}
