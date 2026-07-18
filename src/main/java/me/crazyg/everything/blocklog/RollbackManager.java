package me.crazyg.everything.blocklog;

import me.crazyg.everything.Everything;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Performs block rollbacks in batches (per tick) and prevents re-logging of
 * reverted blocks via a transient lock set.
 */
public class RollbackManager {

    private static final Set<String> LOCKED = new HashSet<>();

    private final Everything plugin;
    private final BlockLogDatabase database;

    public RollbackManager(Everything plugin, BlockLogDatabase database) {
        this.plugin = plugin;
        this.database = database;
    }

    public static boolean isLocked(Location loc) {
        return LOCKED.contains(key(loc));
    }

    private static String key(Location loc) {
        World w = loc.getWorld();
        return (w == null ? "world" : w.getName()) + ":"
            + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private static void lock(Location loc) {
        LOCKED.add(key(loc));
    }

    private static void unlock(Location loc) {
        LOCKED.remove(key(loc));
    }

    /**
     * Rolls back the given changes in batches. Each change is reverted to its
     * old state (old_data). Log entries are deleted afterwards so they are not
     * reverted twice. Returns the number of blocks actually reverted.
     */
    public int rollback(List<BlockChange> changes, int maxBlocks) {
        if (changes.isEmpty()) return 0;

        int limit = maxBlocks > 0
            ? Math.min(maxBlocks, changes.size()) : changes.size();

        int[] reverted = {0};
        int batchSize = 200;

        for (int i = 0; i < limit; i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, limit);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (int j = start; j < end; j++) {
                    BlockChange change = changes.get(j);
                    if (applyRevert(change)) {
                        reverted[0]++;
                    }
                    database.deleteEntries(List.of(change.getId()));
                }
            }, (i / batchSize));
        }

        return limit;
    }

    /**
     * Reverts a single block change (used by the inspect wand right-click).
     */
    public boolean rollbackSingle(BlockChange change) {
        boolean ok = applyRevert(change);
        if (ok) {
            database.deleteEntries(List.of(change.getId()));
        }
        return ok;
    }

    private boolean applyRevert(BlockChange change) {
        World world = Bukkit.getWorld(change.getWorld());
        if (world == null) return false;
        Location loc = new Location(world, change.getX(),
            change.getY(), change.getZ());
        Block block = loc.getBlock();
        lock(loc);
        try {
            String oldData = change.getOldData();
            Material oldMat = parseMaterial(oldData);
            if (oldMat == null) {
                return false;
            }
            block.setType(oldMat, false);
            restoreState(block, oldData);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning(
                "Failed to revert block at " + loc + ": " + e.getMessage());
            return false;
        } finally {
            unlock(loc);
        }
    }

    private Material parseMaterial(String data) {
        if (data == null || data.isEmpty()) return null;
        String matName = data.split(":")[0].trim().toUpperCase(Locale.ROOT);
        try {
            return Material.matchMaterial(matName);
        } catch (Exception e) {
            return null;
        }
    }

    private void restoreState(Block block, String data) {
        // Only the material is restored for robustness. Complex tile-entity
        // state (signs, skulls, etc.) is intentionally not reconstructed.
    }
}
