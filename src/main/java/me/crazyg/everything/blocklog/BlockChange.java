package me.crazyg.everything.blocklog;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a single logged block change.
 */
public class BlockChange {

    public enum Action {
        PLACE,
        BREAK,
        EXPLODE,
        BURN,
        FADE,
        ENTITY,
        BUCKET,
        FLUID,
        LEAF_DECAY,
        GROWTH,
        PISTON,
        PORTAL,
        SIGN_EDIT,
        UNKNOWN;

        public static Action fromString(String s) {
            if (s == null) return UNKNOWN;
            try {
                return Action.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                return UNKNOWN;
            }
        }
    }

    private final int id;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final String blockType;
    private final String oldData;
    private final String newData;
    private final Action action;
    private final UUID playerUuid;
    private final String playerName;
    private final LocalDateTime timestamp;
    private final String oldTile;
    private final String newTile;

    public BlockChange(int id, String world, int x, int y, int z,
                       String blockType, String oldData, String newData,
                       Action action, UUID playerUuid, String playerName,
                       LocalDateTime timestamp) {
        this(id, world, x, y, z, blockType, oldData, newData, action,
            playerUuid, playerName, timestamp, null, null);
    }

    public BlockChange(int id, String world, int x, int y, int z,
                       String blockType, String oldData, String newData,
                       Action action, UUID playerUuid, String playerName,
                       LocalDateTime timestamp, String oldTile, String newTile) {
        this.id = id;
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
        this.timestamp = timestamp;
        this.oldTile = oldTile;
        this.newTile = newTile;
    }

    public int getId() {
        return id;
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public String getBlockType() {
        return blockType;
    }

    public String getOldData() {
        return oldData;
    }

    public String getNewData() {
        return newData;
    }

    public Action getAction() {
        return action;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getOldTile() {
        return oldTile;
    }

    public String getNewTile() {
        return newTile;
    }

    public boolean hasOldTile() {
        return oldTile != null && !oldTile.isEmpty();
    }

    public boolean hasNewTile() {
        return newTile != null && !newTile.isEmpty();
    }

    public boolean isNatural() {
        return playerUuid == null;
    }

    /**
     * Returns the stored data string in the format
     * {@code MATERIAL|blockdata} (blockdata may be empty). This helper returns
     * just the material token, used for rollback material resolution and
     * display.
     */
    public static String materialOf(String data) {
        if (data == null) return "";
        int sep = data.indexOf('|');
        return (sep >= 0 ? data.substring(0, sep) : data).trim();
    }

    public String getOldMaterial() {
        return materialOf(oldData);
    }

    public String getNewMaterial() {
        return materialOf(newData);
    }

    /** Returns the block-data portion (after '|'), or empty if none. */
    public static String blockDataOf(String data) {
        if (data == null) return "";
        int sep = data.indexOf('|');
        return sep >= 0 ? data.substring(sep + 1) : "";
    }
}
