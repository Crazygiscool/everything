package me.crazyg.everything.blocklog;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a single logged entity death/kill event.
 */
public class EntityLogEntry {

    private final int id;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final String entityType;
    private final UUID playerUuid;
    private final String playerName;
    private final LocalDateTime timestamp;
    private final BlockChange.Action action;
    private final String deathCause;

    public EntityLogEntry(int id, String world, int x, int y, int z,
                          String entityType, UUID playerUuid, String playerName,
                          LocalDateTime timestamp, BlockChange.Action action,
                          String deathCause) {
        this.id = id;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.entityType = entityType;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.timestamp = timestamp;
        this.action = action;
        this.deathCause = deathCause;
    }

    public int getId() { return id; }
    public String getWorld() { return world; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public String getEntityType() { return entityType; }
    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public BlockChange.Action getAction() { return action; }
    public String getDeathCause() { return deathCause; }
}
