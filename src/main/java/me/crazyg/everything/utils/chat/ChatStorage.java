package me.crazyg.everything.utils.chat;

import me.crazyg.everything.Everything;
import me.crazyg.everything.utils.storage.YamlRepository;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.UUID;

/**
 * Simple YAML-backed storage for the EverythingChat provider fallback.
 * Stores per-player prefix/suffix in chat.yml as:
 *
 * <uuid>:
 *   prefix: "..."
 *   suffix: "..."
 */
public class ChatStorage extends YamlRepository {

    public ChatStorage(Everything plugin) {
        super(plugin, "data", "data/chat.yml", "chat.yml");
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
}
