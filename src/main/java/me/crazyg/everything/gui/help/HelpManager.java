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

        // Ensure plugin folder exists
        File helpFile = new File(plugin.getDataFolder(), "help.yml");

        // Extract from /gui/help.yml inside the JAR
        if (!helpFile.exists()) {
            plugin.saveResource("gui/help.yml", false);

            // Move it to the root data folder as help.yml
            File extracted = new File(plugin.getDataFolder(), "gui/help.yml");
            extracted.renameTo(helpFile);

            // Delete leftover folder if needed
            new File(plugin.getDataFolder(), "gui").delete();
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
