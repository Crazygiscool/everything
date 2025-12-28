package me.crazyg.everything.gui.help;

import me.crazyg.everything.Everything;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class HelpManager {

    private final Everything plugin;
    private final FileConfiguration config;

    public HelpManager(Everything plugin) {
        this.plugin = plugin;

        File helpFile = new File(plugin.getDataFolder(), "help.yml");
        if (!helpFile.exists()) {
            plugin.saveResource("help.yml", false);
        }

        this.config = YamlConfiguration.loadConfiguration(helpFile);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public ConfigurationSection getCategories() {
        return config.getConfigurationSection("categories");
    }

    public ConfigurationSection getCategory(String key) {
        return config.getConfigurationSection("categories." + key);
    }

    public String getServerName() {
        return config.getString("server-name", "Server");
    }

    public String getServerLink() {
        return config.getString("server-link", "");
    }

    public String getServerAuthor() {
        return config.getString("server-author", "");
    }
}
