package me.crazyg.everything.gui.help;

import me.crazyg.everything.Everything;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HelpManager {

    private final Everything plugin;
    private FileConfiguration config;

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

    // ---------------------------------------------------------
    // Main Menu
    // ---------------------------------------------------------

    public ConfigurationSection getMainMenuSection() {
        return config.getConfigurationSection("main-menu");
    }

    public String getMainMenuTitle() {
        ConfigurationSection section = getMainMenuSection();
        return section != null ? section.getString("title", "&6Help Menu") : "&6Help Menu";
    }

    public int getMainMenuSize() {
        ConfigurationSection section = getMainMenuSection();
        return section != null ? section.getInt("size", 45) : 45;
    }

    /**
     * Returns main menu items as a map of slot -> item config section.
     * Excludes navigation items (prev-page, next-page) which are handled
     * by the GUI class itself.
     */
    public Map<Integer, ConfigurationSection> getMainMenuItems() {
        Map<Integer, ConfigurationSection> items = new LinkedHashMap<>();
        ConfigurationSection section = getMainMenuSection();
        if (section == null) return items;

        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        if (itemsSection == null) return items;

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection item = itemsSection.getConfigurationSection(key);
            if (item != null && item.contains("slot")) {
                items.put(item.getInt("slot"), item);
            }
        }
        return items;
    }

    // ---------------------------------------------------------
    // Pages
    // ---------------------------------------------------------

    public ConfigurationSection getPage(String pageKey) {
        return config.getConfigurationSection("pages." + pageKey);
    }

    /**
     * Returns static items for a page as a map of slot -> item config.
     */
    public Map<Integer, ConfigurationSection> getPageItems(String pageKey) {
        Map<Integer, ConfigurationSection> items = new LinkedHashMap<>();
        ConfigurationSection page = getPage(pageKey);
        if (page == null) return items;

        ConfigurationSection itemsSection = page.getConfigurationSection("items");
        if (itemsSection == null) return items;

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection item = itemsSection.getConfigurationSection(key);
            if (item != null && item.contains("slot")) {
                items.put(item.getInt("slot"), item);
            }
        }
        return items;
    }

    public String getPageTitle(String pageKey) {
        ConfigurationSection page = getPage(pageKey);
        return page != null ? page.getString("title", pageKey) : pageKey;
    }

    public int getPageSize(String pageKey) {
        ConfigurationSection page = getPage(pageKey);
        return page != null ? page.getInt("size", 54) : 54;
    }

    public String getPageLoad(String pageKey) {
        ConfigurationSection page = getPage(pageKey);
        return page != null ? page.getString("load", "") : "";
    }

    public int getPageStartSlot(String pageKey) {
        ConfigurationSection page = getPage(pageKey);
        return page != null ? page.getInt("start-slot", 10) : 10;
    }

    public int getPageItemsPerPage(String pageKey) {
        ConfigurationSection page = getPage(pageKey);
        return page != null ? page.getInt("items-per-page", 28) : 28;
    }

    /**
     * Placeholder replacement for strings from help.yml.
     */
    public String replacePlaceholders(String text) {
        if (text == null) return "";
        text = text.replace("%server_name%", getServerName())
                   .replace("%server_link%", getServerLink())
                   .replace("%server_author%", getServerAuthor())
                   .replace("%server_description%", getServerDescription());
        return text;
    }
}
