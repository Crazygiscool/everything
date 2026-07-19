package me.crazyg.everything.blocklog;

import me.crazyg.everything.Everything;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Centralized blocklog config keys + defaults. Reading config through this
 * class avoids scattered string literals and keeps defaults consistent.
 */
public final class BlockLogConfig {

    private BlockLogConfig() {}

    public static final String ENABLED = "blocklog.enabled";
    public static final String LOG_NATURAL = "blocklog.log-natural";
    public static final String LOG_FLUID = "blocklog.log-fluid";
    public static final String MAX_RETENTION_DAYS = "blocklog.max-retention-days";
    public static final String PRUNE_ON_STARTUP = "blocklog.prune-on-startup";
    public static final String ASYNC = "blocklog.async";
    public static final String WAND_MATERIAL = "blocklog.inspect-wand-material";
    public static final String MAX_HISTORY_PER_BLOCK = "blocklog.max-history-per-block";
    public static final String MAX_ROLLBACK_BLOCKS = "blocklog.max-rollback-blocks";
    public static final String WORLDS_MODE = "blocklog.worlds.mode";
    public static final String WORLDS_LIST = "blocklog.worlds.list";

    public static boolean isEnabled(FileConfiguration config) {
        return config.getBoolean(ENABLED, true);
    }

    public static boolean logNatural(FileConfiguration config) {
        return config.getBoolean(LOG_NATURAL, true);
    }

    public static boolean logFluid(FileConfiguration config) {
        return config.getBoolean(LOG_FLUID, false);
    }

    public static boolean async(FileConfiguration config) {
        return config.getBoolean(ASYNC, true);
    }

    public static boolean pruneOnStartup(FileConfiguration config) {
        return config.getBoolean(PRUNE_ON_STARTUP, false);
    }

    public static int maxRetentionDays(FileConfiguration config) {
        return config.getInt(MAX_RETENTION_DAYS, 30);
    }

    public static int maxHistoryPerBlock(FileConfiguration config) {
        return config.getInt(MAX_HISTORY_PER_BLOCK, 25);
    }

    public static int maxRollbackBlocks(FileConfiguration config) {
        return config.getInt(MAX_ROLLBACK_BLOCKS, 10000);
    }

    public static boolean isWorldLogged(FileConfiguration config, String world) {
        String mode = config.getString(WORLDS_MODE, "blacklist");
        java.util.List<String> list = config.getStringList(WORLDS_LIST);
        if (list.isEmpty()) return true;
        boolean contains = list.contains(world);
        return "whitelist".equalsIgnoreCase(mode) ? contains : !contains;
    }
}
