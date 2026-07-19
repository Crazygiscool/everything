package me.crazyg.everything.utils.economy;

import me.crazyg.everything.Everything;
import me.crazyg.everything.utils.storage.YamlRepository;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Set;
import java.util.UUID;

/**
 * Simple YAML-backed storage for the EverythingEconomy provider.
 * Stores balances in eco.yml as:
 *
 * <uuid>: <balance>
 */
public class EcoStorage extends YamlRepository {

    public EcoStorage(Everything plugin) {
        super(plugin, "data", "data/eco.yml", "eco.yml");
    }

    // ---------------------------------------------------------
    // Account Handling
    // ---------------------------------------------------------

    public boolean hasAccount(UUID uuid) {
        return config.contains(uuid.toString());
    }

    public Set<String> getAllAccountUUIDs() {
        return config.getKeys(false);
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
}
