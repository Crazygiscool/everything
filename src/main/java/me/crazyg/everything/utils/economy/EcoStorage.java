package me.crazyg.everything.utils.economy;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Simple YAML-backed storage for the EverythingEconomy provider.
 * Stores balances in balances.yml as:
 *
 * <uuid>: <balance>
 */
public class EcoStorage {

    private final File file;
    private final FileConfiguration config;

    public EcoStorage(File dataFolder) {
        this.file = new File(dataFolder, "balances.yml");

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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
