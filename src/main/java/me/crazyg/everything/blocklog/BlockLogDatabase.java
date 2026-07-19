package me.crazyg.everything.blocklog;

import me.crazyg.everything.Everything;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * SQLite-backed storage for block change logging and rollbacks.
 *
 * <p>Writes are buffered in memory and flushed asynchronously in batches
 * (one transaction per flush) so block-change events never block the server
 * main thread. Reads remain synchronous (command/rollback context) but all
 * connection access is guarded by a single lock since SQLite connections are
 * not thread-safe.
 *
 * <p>Table {@code block_log}:
 * id, world, x, y, z, block_type, old_data, new_data, action,
 * player_uuid, player_name, timestamp, old_tile, new_tile
 */
public class BlockLogDatabase {

    private static final DateTimeFormatter DB_FORMAT =
        DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final int FLUSH_INTERVAL_TICKS = 20;
    private static final int MAX_BATCH = 500;

    private final Everything plugin;
    private final File dbFile;
    private Connection connection;
    private final Object lock = new Object();

    private final ConcurrentLinkedQueue<QueuedChange> writeQueue =
        new ConcurrentLinkedQueue<>();
    private int flushTaskId = -1;

    private static final class QueuedChange {
        final String world;
        final int x, y, z;
        final String blockType;
        final String oldData;
        final String newData;
        final BlockChange.Action action;
        final UUID playerUuid;
        final String playerName;
        final BlockState oldState;
        final BlockState newState;
        final LocalDateTime timestamp;

        QueuedChange(String world, int x, int y, int z, String blockType,
                     String oldData, String newData,
                     BlockChange.Action action, UUID playerUuid,
                     String playerName, BlockState oldState,
                     BlockState newState) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockType = blockType;
            this.oldData = oldData;
            this.newData = newData;
            this.action = action;
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.oldState = oldState;
            this.newState = newState;
            this.timestamp = LocalDateTime.now();
        }
    }

    public BlockLogDatabase(Everything plugin) {
        this.plugin = plugin;
        File folder = new File(plugin.getDataFolder(), "data");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        this.dbFile = new File(folder, "blocklog.db");
    }

    public void init() {
        try {
            if (!dbFile.exists()) {
                try {
                    boolean created = dbFile.createNewFile();
                    if (!created) {
                        plugin.getLogger().warning(
                            "Could not create blocklog.db file.");
                    }
                } catch (java.io.IOException e) {
                    plugin.getLogger().warning(
                        "Could not create blocklog.db file: "
                            + e.getMessage());
                }
            }
            Class.forName("org.sqlite.JDBC");
            synchronized (lock) {
                this.connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + dbFile.getAbsolutePath());
                createTable();
            }
            // Asynchronous batched writer — runs off the main thread unless
            // blocklog.async is disabled in config.
            if (BlockLogConfig.async(plugin.getConfig())) {
                flushTaskId = Bukkit.getScheduler()
                    .runTaskTimerAsynchronously(plugin, this::flush,
                        FLUSH_INTERVAL_TICKS, FLUSH_INTERVAL_TICKS)
                    .getTaskId();
            } else {
                flushTaskId = Bukkit.getScheduler()
                    .runTaskTimer(plugin, this::flush,
                        FLUSH_INTERVAL_TICKS, FLUSH_INTERVAL_TICKS)
                    .getTaskId();
            }
            plugin.getLogger().info("BlockLog database initialized.");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe(
                "SQLite JDBC driver not found: " + e.getMessage());
        } catch (SQLException e) {
            plugin.getLogger().severe(
                "Failed to initialize blocklog database: " + e.getMessage());
        }
    }

    public void close() {
        flush(); // drain remaining queued entries
        if (flushTaskId != -1) {
            Bukkit.getScheduler().cancelTask(flushTaskId);
            flushTaskId = -1;
        }
        synchronized (lock) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    plugin.getLogger().warning(
                        "Error closing blocklog database: " + e.getMessage());
                }
            }
        }
    }

    private void createTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS block_log ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "world TEXT NOT NULL, "
                    + "x INTEGER NOT NULL, "
                    + "y INTEGER NOT NULL, "
                    + "z INTEGER NOT NULL, "
                    + "block_type TEXT NOT NULL, "
                    + "old_data TEXT NOT NULL, "
                    + "new_data TEXT NOT NULL, "
                    + "action TEXT NOT NULL, "
                    + "player_uuid TEXT, "
                    + "player_name TEXT, "
                    + "timestamp TEXT NOT NULL)");
            stmt.execute(
                "CREATE INDEX IF NOT EXISTS idx_block_pos "
                    + "ON block_log(world, x, y, z)");
            stmt.execute(
                "CREATE INDEX IF NOT EXISTS idx_block_time "
                    + "ON block_log(timestamp)");
            migrateColumns(stmt);
        }
    }

    /**
     * Adds tile-state columns if they are missing (supports existing DBs
     * created before tile data was introduced).
     */
    private void migrateColumns(Statement stmt) {
        addColumnIfMissing(stmt, "old_tile", "TEXT");
        addColumnIfMissing(stmt, "new_tile", "TEXT");
        addColumnIfMissing(stmt, "rolled_back", "INTEGER DEFAULT 0");
    }

    private void addColumnIfMissing(Statement stmt, String column,
                                     String type) {
        try {
            stmt.execute("ALTER TABLE block_log ADD COLUMN "
                + column + " " + type);
        } catch (SQLException e) {
            // Column already exists - ignore.
        }
    }

    // ---------------------------------------------------------
    // Logging (main-thread safe: only enqueues)
    // ---------------------------------------------------------

    /**
     * Queues a block change for asynchronous, batched persistence.
     * Tile-state serialization is deferred to the flush thread so it never
     * runs on the server main thread.
     */
    public void logChange(Location loc, String blockType, String oldData,
                          String newData, BlockChange.Action action,
                          UUID playerUuid, String playerName,
                          BlockState oldState, BlockState newState) {
        String world = loc.getWorld() == null
            ? "world" : loc.getWorld().getName();
        writeQueue.add(new QueuedChange(
            world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
            blockType, oldData, newData, action, playerUuid,
            playerName, oldState, newState));
    }

    /** Drains the queue and writes entries in a single transaction. */
    private void flush() {
        if (writeQueue.isEmpty()) return;
        List<QueuedChange> batch = new ArrayList<>();
        QueuedChange item;
        while ((item = writeQueue.poll()) != null && batch.size() < MAX_BATCH) {
            batch.add(item);
        }
        if (batch.isEmpty()) return;

        synchronized (lock) {
            if (connection == null) return;
            String sql = "INSERT INTO block_log(world, x, y, z, block_type, "
                + "old_data, new_data, action, player_uuid, player_name, "
                + "timestamp, old_tile, new_tile) "
                + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                connection.setAutoCommit(false);
                TileStateSerializer serializer =
                    new TileStateSerializer(plugin);
                for (QueuedChange c : batch) {
                    String oldTile = c.oldState == null ? null
                        : serializer.serialize(c.oldState);
                    String newTile = c.newState == null ? null
                        : serializer.serialize(c.newState);
                    ps.setString(1, c.world);
                    ps.setInt(2, c.x);
                    ps.setInt(3, c.y);
                    ps.setInt(4, c.z);
                    ps.setString(5, c.blockType);
                    ps.setString(6, c.oldData == null ? "" : c.oldData);
                    ps.setString(7, c.newData == null ? "" : c.newData);
                    ps.setString(8, c.action.name());
                    ps.setString(9, c.playerUuid == null
                        ? null : c.playerUuid.toString());
                    ps.setString(10, c.playerName);
                    ps.setString(11, DB_FORMAT.format(c.timestamp));
                    ps.setString(12, oldTile == null ? "" : oldTile);
                    ps.setString(13, newTile == null ? "" : newTile);
                    ps.addBatch();
                }
                ps.executeBatch();
                connection.commit();
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                plugin.getLogger().warning(
                    "Failed to flush block log batch: " + e.getMessage());
                try {
                    connection.rollback();
                    connection.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    // ---------------------------------------------------------
    // Queries
    // ---------------------------------------------------------

    /**
     * Returns the full history for a single block coordinate, most recent first.
     */
    public List<BlockChange> getHistory(Location loc, int limit) {
        List<BlockChange> result = new ArrayList<>();
        if (connection == null) return result;
        String sql = "SELECT * FROM block_log WHERE world = ? AND x = ? AND y = ? AND z = ? "
            + "AND rolled_back = 0 ORDER BY id DESC LIMIT ?";
        synchronized (lock) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, loc.getWorld() == null
                    ? "world" : loc.getWorld().getName());
                ps.setInt(2, loc.getBlockX());
                ps.setInt(3, loc.getBlockY());
                ps.setInt(4, loc.getBlockZ());
                ps.setInt(5, limit <= 0 ? Integer.MAX_VALUE : limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(mapRow(rs));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning(
                    "Failed to query block history: " + e.getMessage());
            }
        }
        return result;
    }

    /**
     * Query changes within a radius of a center, optionally filtered by player
     * and/or a cutoff time. Most recent first.
     */
    public List<BlockChange> query(World world, int cx, int cy, int cz,
                                    int radius, UUID playerUuid,
                                    LocalDateTime since) {
        List<BlockChange> result = new ArrayList<>();
        if (connection == null) return result;
        StringBuilder sql = new StringBuilder(
            "SELECT * FROM block_log WHERE world = ? AND rolled_back = 0 ");
        if (radius >= 0) {
            sql.append("AND x BETWEEN ? AND ? AND y BETWEEN ? AND ? AND z BETWEEN ? AND ? ");
        }
        if (playerUuid != null) {
            sql.append("AND player_uuid = ? ");
        }
        if (since != null) {
            sql.append("AND timestamp >= ? ");
        }
        sql.append("ORDER BY id DESC");

        synchronized (lock) {
            try (PreparedStatement ps =
                     connection.prepareStatement(sql.toString())) {
                int idx = 1;
                ps.setString(idx++, world == null ? "world" : world.getName());
                if (radius >= 0) {
                    ps.setInt(idx++, cx - radius);
                    ps.setInt(idx++, cx + radius);
                    ps.setInt(idx++, cy - radius);
                    ps.setInt(idx++, cy + radius);
                    ps.setInt(idx++, cz - radius);
                    ps.setInt(idx++, cz + radius);
                }
                if (playerUuid != null) {
                    ps.setString(idx++, playerUuid.toString());
                }
                if (since != null) {
                    ps.setString(idx++, DB_FORMAT.format(since));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(mapRow(rs));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning(
                    "Failed to query block log: " + e.getMessage());
            }
        }
        return result;
    }

    // ---------------------------------------------------------
    // Rollback
    // ---------------------------------------------------------

    /**
     * Marks the given log entries as rolled back instead of deleting them, so
     * the change can be undone later (re-applied) via the inverse delta.
     */
    public void markRolledBack(List<Integer> ids) {
        if (ids.isEmpty()) return;
        String placeholders = String.join(",",
            java.util.Collections.nCopies(ids.size(), "?"));
        String sql = "UPDATE block_log SET rolled_back = 1 "
            + "WHERE id IN (" + placeholders + ")";
        synchronized (lock) {
            if (connection == null) return;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                for (int i = 0; i < ids.size(); i++) {
                    ps.setInt(i + 1, ids.get(i));
                }
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning(
                    "Failed to mark block log entries as rolled back: "
                        + e.getMessage());
            }
        }
    }

    /** Returns the ids of the most recently rolled-back entries (for undo). */
    public List<Integer> getRolledBackIds(int limit) {
        List<Integer> ids = new ArrayList<>();
        if (connection == null) return ids;
        String sql = "SELECT id FROM block_log WHERE rolled_back = 1 "
            + "ORDER BY id DESC LIMIT ?";
        synchronized (lock) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, limit <= 0 ? Integer.MAX_VALUE : limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ids.add(rs.getInt("id"));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning(
                    "Failed to query rolled-back entries: " + e.getMessage());
            }
        }
        return ids;
    }

    public int pruneOlderThan(int days) {
        if (days <= 0) return 0;
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        String sql = "DELETE FROM block_log WHERE timestamp < ?";
        synchronized (lock) {
            if (connection == null) return 0;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, DB_FORMAT.format(cutoff));
                return ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning(
                    "Failed to prune block log: " + e.getMessage());
                return 0;
            }
        }
    }

    public int countEntries() {
        synchronized (lock) {
            if (connection == null) return 0;
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM block_log")) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning(
                    "Failed to count block log entries: " + e.getMessage());
            }
            return 0;
        }
    }

    // ---------------------------------------------------------
    // Mapping
    // ---------------------------------------------------------

    private BlockChange mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String world = rs.getString("world");
        int x = rs.getInt("x");
        int y = rs.getInt("y");
        int z = rs.getInt("z");
        String blockType = rs.getString("block_type");
        String oldData = rs.getString("old_data");
        String newData = rs.getString("new_data");
        BlockChange.Action action =
            BlockChange.Action.fromString(rs.getString("action"));
        String uuidStr = rs.getString("player_uuid");
        UUID uuid = uuidStr == null ? null : parseUuid(uuidStr);
        String playerName = rs.getString("player_name");
        String tsStr = rs.getString("timestamp");
        LocalDateTime ts;
        try {
            ts = LocalDateTime.parse(tsStr, DB_FORMAT);
        } catch (Exception e) {
            ts = LocalDateTime.now();
        }
        String oldTile = rs.getString("old_tile");
        String newTile = rs.getString("new_tile");
        return new BlockChange(id, world, x, y, z, blockType, oldData,
            newData, action, uuid, playerName, ts, oldTile, newTile);
    }

    private UUID parseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
