package me.crazyg.everything.utils.storage;

import me.crazyg.everything.Everything;

public class PlayerSettingsStorage extends YamlRepository {

    public PlayerSettingsStorage(Everything plugin) {
        super(plugin, "data", "data/player-settings.yml", "player-settings.yml");
    }

    public void reloadSettings() {
        load();
    }

    public org.bukkit.configuration.file.FileConfiguration getStorageConfig() {
        return config;
    }

    public void saveSettings() {
        save();
    }
}
