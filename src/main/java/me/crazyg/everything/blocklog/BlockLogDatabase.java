package me.crazyg.everything.blocklog;

import me.crazyg.everything.Everything;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

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

/**
 * SQLite-backed storage for block change logging and rollbacks.
 *
 * <p>Table {@code block_log}:
 * id, world, x, y, z, block_type, old_data, new_data, action,
 * player_uuid, player_name, timestamp
 */
public class BlockLogDatabase {

    private static final DateTimeFormatter DB_FORMAT =
        DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Everything plugin;
    private final File dbFile;
    private Connection connection;

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
            this.connection = DriverManager.getConnection(
                "jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTable();
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
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().warning(
                    "Error closing blocklog database: " + e.getMessage());
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
        }
    }

    // ---------------------------------------------------------
    // Logging
    // ---------------------------------------------------------

    public void logChange(Location loc, String blockType, String oldData,
                          String newData, BlockChange.Action action,
                          UUID playerUuid, String playerName) {
        if (connection == null) return;
        String sql = "INSERT INTO block_log(world, x, y, z, block_type, "
            + "old_data, new_data, action, player_uuid, player_name, timestamp) "
            + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, loc.getWorld() == null
                ? "world" : loc.getWorld().getName());
            ps.setInt(2, loc.getBlockX());
            ps.setInt(3, loc.getBlockY());
            ps.setInt(4, loc.getBlockZ());
            ps.setString(5, blockType);
            ps.setString(6, oldData == null ? "" : oldData);
            ps.setString(7, newData == null ? "" : newData);
            ps.setString(8, action.name());
            ps.setString(9, playerUuid == null
                ? null : playerUuid.toString());
            ps.setString(10, playerName);
            ps.setString(11, DB_FORMAT.format(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning(
                "Failed to log block change: " + e.getMessage());
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
            + "ORDER BY id DESC LIMIT ?";
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
            "SELECT * FROM block_log WHERE world = ? ");
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
        return result;
    }

    // ---------------------------------------------------------
    // Rollback
    // ---------------------------------------------------------

    /**
     * Deletes log entries that match the given parameters. Used internally to
     * remove log rows that have been rolled back so they are not reverted again.
     */
    public void deleteEntries(List<Integer> ids) {
        if (connection == null || ids.isEmpty()) return;
        String placeholders = String.join(",",
            java.util.Collections.nCopies(ids.size(), "?"));
        String sql = "DELETE FROM block_log WHERE id IN (" + placeholders + ")";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) {
                ps.setInt(i + 1, ids.get(i));
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning(
                "Failed to delete block log entries: " + e.getMessage());
        }
    }

    public int pruneOlderThan(int days) {
        if (connection == null || days <= 0) return 0;
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        String sql = "DELETE FROM block_log WHERE timestamp < ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, DB_FORMAT.format(cutoff));
            return ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning(
                "Failed to prune block log: " + e.getMessage());
            return 0;
        }
    }

    public int countEntries() {
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
        return new BlockChange(id, world, x, y, z, blockType, oldData,
            newData, action, uuid, playerName, ts);
    }

    private UUID parseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
