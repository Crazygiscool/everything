package me.crazyg.everything.utils.chat;

import me.crazyg.everything.Everything;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/**
 * Vault Chat provider for Everything.
 *
 * Resolution order for prefix/suffix:
 *   1. LuckPerms (if installed) - reads the player's cached meta.
 *   2. Everything's own ChatStorage (data/chat.yml).
 *   3. config.yml chat.default-prefix / chat.default-suffix.
 *
 * All other Vault Chat methods are minimal/no-op since the plugin's
 * ChatListener only consumes player prefix/suffix.
 */
public class EverythingChat extends Chat {

    private final Everything plugin;
    private final ChatStorage storage;
    private final net.luckperms.api.LuckPerms luckPerms;

    public EverythingChat(Everything plugin, ChatStorage storage) {
        super(resolvePermission(plugin));
        this.plugin = plugin;
        this.storage = storage;
        this.luckPerms = resolveLuckPerms();
    }

    private static Permission resolvePermission(Everything plugin) {
        try {
            return Bukkit.getServicesManager().getRegistration(Permission.class) != null
                ? Bukkit.getServicesManager().load(Permission.class)
                : null;
        } catch (Exception e) {
            return null;
        }
    }

    private net.luckperms.api.LuckPerms resolveLuckPerms() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return null;
        }
        try {
            return Bukkit.getServicesManager().load(net.luckperms.api.LuckPerms.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String fromLuckPerms(UUID uuid, boolean prefix) {
        if (luckPerms == null) return null;
        try {
            net.luckperms.api.model.user.User user = luckPerms.getUserManager().getUser(uuid);
            if (user == null) return null;
            net.luckperms.api.cacheddata.CachedMetaData meta = user.getCachedData().getMetaData();
            String value = prefix ? meta.getPrefix() : meta.getSuffix();
            if (value == null) return null;
            // LuckPerms returns legacy '&' color codes; Vault convention is the same.
            return value;
        } catch (Exception e) {
            return null;
        }
    }

    private String resolvePrefix(UUID uuid) {
        String value = fromLuckPerms(uuid, true);
        if (value != null && !value.isEmpty()) return value;

        value = storage.getPlayerPrefix(uuid);
        if (value != null && !value.isEmpty()) return value;

        return plugin.getConfig().getString("chat.default-prefix", "");
    }

    private String resolveSuffix(UUID uuid) {
        String value = fromLuckPerms(uuid, false);
        if (value != null && !value.isEmpty()) return value;

        value = storage.getPlayerSuffix(uuid);
        if (value != null && !value.isEmpty()) return value;

        return plugin.getConfig().getString("chat.default-suffix", "");
    }

    @Override
    public String getName() {
        return "EverythingChat";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // ---------------------------------------------------------
    // Player prefix / suffix (abstract in VaultAPI 1.7)
    // ---------------------------------------------------------

    @Override
    @Deprecated
    public String getPlayerPrefix(String world, String player) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(player);
        return resolvePrefix(offline.getUniqueId());
    }

    @Override
    @Deprecated
    public void setPlayerPrefix(String world, String player, String prefix) {
        storage.setPlayerPrefix(Bukkit.getOfflinePlayer(player).getUniqueId(), prefix);
    }

    @Override
    @Deprecated
    public String getPlayerSuffix(String world, String player) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(player);
        return resolveSuffix(offline.getUniqueId());
    }

    @Override
    @Deprecated
    public void setPlayerSuffix(String world, String player, String suffix) {
        storage.setPlayerSuffix(Bukkit.getOfflinePlayer(player).getUniqueId(), suffix);
    }

    // ---------------------------------------------------------
    // Group methods (not supported - return empty / no-op)
    // ---------------------------------------------------------

    @Override
    public String getGroupPrefix(String world, String group) {
        return "";
    }

    @Override
    public String getGroupSuffix(String world, String group) {
        return "";
    }

    @Override
    public void setGroupPrefix(String world, String group, String prefix) {}

    @Override
    public void setGroupSuffix(String world, String group, String suffix) {}

    // ---------------------------------------------------------
    // Player info methods (not supported - return default / no-op)
    // ---------------------------------------------------------

    @Override
    @Deprecated
    public String getPlayerInfoString(String world, String player, String node, String defaultValue) {
        return defaultValue;
    }

    @Override
    @Deprecated
    public void setPlayerInfoString(String world, String player, String node, String value) {}

    @Override
    @Deprecated
    public int getPlayerInfoInteger(String world, String player, String node, int defaultValue) {
        return defaultValue;
    }

    @Override
    @Deprecated
    public void setPlayerInfoInteger(String world, String player, String node, int value) {}

    @Override
    @Deprecated
    public double getPlayerInfoDouble(String world, String player, String node, double defaultValue) {
        return defaultValue;
    }

    @Override
    @Deprecated
    public void setPlayerInfoDouble(String world, String player, String node, double value) {}

    @Override
    @Deprecated
    public boolean getPlayerInfoBoolean(String world, String player, String node, boolean defaultValue) {
        return defaultValue;
    }

    @Override
    @Deprecated
    public void setPlayerInfoBoolean(String world, String player, String node, boolean value) {}

    // ---------------------------------------------------------
    // Group info methods (not supported - return default / no-op)
    // ---------------------------------------------------------

    @Override
    public String getGroupInfoString(String world, String group, String node, String defaultValue) {
        return defaultValue;
    }

    @Override
    public void setGroupInfoString(String world, String group, String node, String value) {}

    @Override
    public int getGroupInfoInteger(String world, String group, String node, int defaultValue) {
        return defaultValue;
    }

    @Override
    public void setGroupInfoInteger(String world, String group, String node, int value) {}

    @Override
    public double getGroupInfoDouble(String world, String group, String node, double defaultValue) {
        return defaultValue;
    }

    @Override
    public void setGroupInfoDouble(String world, String group, String node, double value) {}

    @Override
    public boolean getGroupInfoBoolean(String world, String group, String node, boolean defaultValue) {
        return defaultValue;
    }

    @Override
    public void setGroupInfoBoolean(String world, String group, String node, boolean value) {}
}
