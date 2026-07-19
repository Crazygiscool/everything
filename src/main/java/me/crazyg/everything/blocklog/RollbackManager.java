package me.crazyg.everything.blocklog;

import me.crazyg.everything.Everything;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import java.util.ArrayList;
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
    private final TileStateSerializer tileSerializer;

    public RollbackManager(Everything plugin, BlockLogDatabase database) {
        this.plugin = plugin;
        this.database = database;
        this.tileSerializer = new TileStateSerializer(plugin);
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
     * old state (old_data). Entries are marked rolled_back (not deleted) so the
     * operation can be undone later. Returns the number of blocks reverted.
     */
    public int rollback(List<BlockChange> changes, int maxBlocks) {
        if (changes.isEmpty()) return 0;

        int limit = maxBlocks > 0
            ? Math.min(maxBlocks, changes.size()) : changes.size();

        int batchSize = 200;

        for (int i = 0; i < limit; i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, limit);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                List<Integer> marked = new ArrayList<>();
                for (int j = start; j < end; j++) {
                    BlockChange change = changes.get(j);
                    if (applyRevert(change)) {
                        marked.add(change.getId());
                    }
                }
                database.markRolledBack(marked);
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
            database.markRolledBack(List.of(change.getId()));
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
            applyBlockData(block, BlockChange.blockDataOf(oldData));

            // Restore tile-entity data (sign text, skull owner, chest
            // inventory, etc.) when available.
            if (change.hasOldTile()) {
                BlockState oldState = tileSerializer.deserialize(
                    change.getOldTile());
                if (oldState != null) {
                    BlockState relocated = oldState.copy(loc);
                    relocated.update(true, true);
                }
            }
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
        String matName = BlockChange.materialOf(data)
            .toUpperCase(Locale.ROOT);
        try {
            return Material.matchMaterial(matName);
        } catch (Exception e) {
            return null;
        }
    }

    /** Applies the serialized BlockData portion (after '|') if present. */
    private void applyBlockData(Block block, String blockData) {
        if (blockData == null || blockData.isEmpty()) return;
        try {
            org.bukkit.block.data.BlockData bd =
                org.bukkit.Bukkit.createBlockData(blockData);
            if (bd != null) {
                block.setBlockData(bd, false);
            }
        } catch (Exception e) {
            // Fall back to material-only restoration.
        }
    }
}
