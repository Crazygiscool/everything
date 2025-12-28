package me.crazyg.everything.utils.economy;

import me.crazyg.everything.Everything;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Simple YAML-backed storage for the EverythingEconomy provider.
 * Stores balances in eco.yml as:
 *
 * <uuid>: <balance>
 */
public class EcoStorage {

    private final Everything plugin;
    private final File file;
    private final FileConfiguration config;

    public EcoStorage(Everything plugin) {
        this.plugin = plugin;

        // Ensure /plugins/Everything/data/ exists
        File folder = new File(plugin.getDataFolder(), "data");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // Extract eco.yml from resources if missing
        File ecoResource = new File(folder, "eco.yml");
        if (!ecoResource.exists()) {
            // Path inside your JAR: src/main/resources/data/eco.yml
            plugin.saveResource("data/eco.yml", false);
        }

        this.file = ecoResource;
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    // ---------------------------------------------------------
    // Account Handling
    // ---------------------------------------------------------

    public boolean hasAccount(UUID uuid) {
        return config.contains(uuid.toString());
    }

    public boolean createAccount(UUID uuid) {
        if (hasAccount(uuid)) return false;

        config.set(uuid.toString(), 0.0);
        save();
        return true;
    }

    // ---------------------------------------------------------
    // Balance Handling
    // ---------------------------------------------------------

    public double getBalance(UUID uuid) {
        return config.getDouble(uuid.toString(), 0.0);
    }

    public void setBalance(UUID uuid, double amount) {
        config.set(uuid.toString(), amount);
        save();
    }

    // ---------------------------------------------------------
    // Save File
    // ---------------------------------------------------------

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}