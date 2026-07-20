package me.crazyg.everything.blocklog;

import me.crazyg.everything.Everything;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Performs block rollbacks in batches (per tick) and prevents re-logging of
 * reverted blocks via a transient lock set. Supports undo via session tracking.
 */
public class RollbackManager {

    private static final Set<String> LOCKED = ConcurrentHashMap.newKeySet();
    private static final long UNDO_WINDOW_SECONDS = 600; // 10 minutes
    private static final int MAX_SESSIONS = 10;

    private final Everything plugin;
    private final BlockLogDatabase database;
    private final TileStateSerializer tileSerializer;
    private final List<RollbackSession> sessions = new ArrayList<>();

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
        return rollback(changes, maxBlocks, -1);
    }

    /**
     * Rolls back with per-world limits. maxPerWorld = -1 means no per-world cap.
     */
    public int rollback(List<BlockChange> changes, int maxBlocks,
                        int maxPerWorld) {
        if (changes.isEmpty()) return 0;

        List<BlockChange> capped;
        if (maxPerWorld > 0) {
            capped = applyPerWorldLimit(changes, maxPerWorld);
        } else {
            capped = changes;
        }

        int limit = maxBlocks > 0
            ? Math.min(maxBlocks, capped.size()) : capped.size();

        int batchSize = 200;
        List<Integer> allMarked = new ArrayList<>();

        for (int i = 0; i < limit; i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, limit);
            final int batchIndex = i / batchSize;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                List<Integer> marked = new ArrayList<>();
                for (int j = start; j < end; j++) {
                    BlockChange change = capped.get(j);
                    if (applyRevert(change)) {
                        marked.add(change.getId());
                    }
                }
                database.markRolledBack(marked);
                synchronized (allMarked) {
                    allMarked.addAll(marked);
                }
            }, batchIndex);
        }

        // Store session for undo (use the original list for restoration)
        UUID sessionPlayer = null;
        for (BlockChange c : capped) {
            if (c.getPlayerUuid() != null) {
                sessionPlayer = c.getPlayerUuid();
                break;
            }
        }
        synchronized (sessions) {
            sessions.add(new RollbackSession(sessionPlayer, allMarked, capped));
            while (sessions.size() > MAX_SESSIONS) {
                sessions.remove(0);
            }
        }

        return limit;
    }

    /**
     * Undoes the most recent rollback within the undo window.
     * Applies the newData (what it was rolled back TO) back to the blocks.
     */
    public int undoLastRollback(UUID playerUuid) {
        RollbackSession session = null;
        synchronized (sessions) {
            for (int i = sessions.size() - 1; i >= 0; i--) {
                RollbackSession s = sessions.get(i);
                if (!s.isUndone() && s.isWithinUndoWindow(UNDO_WINDOW_SECONDS)) {
                    session = s;
                    break;
                }
            }
        }
        if (session == null) return 0;
        return undoSession(session);
    }

    private int undoSession(RollbackSession session) {
        List<BlockChange> changes = session.getOriginalChanges();
        int count = 0;
        for (BlockChange change : changes) {
            if (applyForward(change)) {
                count++;
            }
        }
        session.markUndone();
        return count;
    }

    /**
     * Applies the "new" state of a change (forward direction, for undo).
     */
    private boolean applyForward(BlockChange change) {
        World world = Bukkit.getWorld(change.getWorld());
        if (world == null) return false;
        Location loc = new Location(world, change.getX(),
            change.getY(), change.getZ());
        Block block = loc.getBlock();
        lock(loc);
        try {
            String newData = change.getNewData();
            Material newMat = parseMaterial(newData);
            if (newMat == null) return false;
            block.setType(newMat, false);
            applyBlockData(block, BlockChange.blockDataOf(newData));
            if (change.hasNewTile()) {
                BlockState newState = tileSerializer.deserialize(
                    change.getNewTile());
                if (newState != null) {
                    BlockState relocated = newState.copy(loc);
                    relocated.update(true, true);
                }
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning(
                "Failed to undo block at " + loc + ": " + e.getMessage());
            return false;
        } finally {
            unlock(loc);
        }
    }

    private List<BlockChange> applyPerWorldLimit(List<BlockChange> changes,
                                                 int maxPerWorld) {
        Map<String, Integer> worldCounts = new HashMap<>();
        List<BlockChange> result = new ArrayList<>();
        for (BlockChange c : changes) {
            int count = worldCounts.getOrDefault(c.getWorld(), 0);
            if (count < maxPerWorld) {
                result.add(c);
                worldCounts.put(c.getWorld(), count + 1);
            }
        }
        return result;
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
