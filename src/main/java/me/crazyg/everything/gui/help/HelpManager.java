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

        // Always overwrite help.yml from JAR resources
        plugin.saveResource("gui/help.yml", true);

        // Move from gui/help.yml to help.yml
        File helpFile = new File(plugin.getDataFolder(), "help.yml");
        File extracted = new File(plugin.getDataFolder(), "gui/help.yml");
        if (extracted.exists()) {
            if (helpFile.exists()) helpFile.delete();
            extracted.renameTo(helpFile);
        }

        // Clean up leftover gui folder
        File guiDir = new File(plugin.getDataFolder(), "gui");
        if (guiDir.isDirectory()) guiDir.delete();

        this.config = YamlConfiguration.loadConfiguration(helpFile);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public Everything getPlugin() {
        return plugin;
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

    public String getServerDescription() {
        return config.getString("server-description", "");
    }
}
