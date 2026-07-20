package me.crazyg.everything.blocklog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Tracks a rollback session for undo capability.
 */
public class RollbackSession {

    private final UUID playerUuid;
    private final LocalDateTime timestamp;
    private final List<Integer> rolledBackIds;
    private final List<BlockChange> originalChanges;
    private boolean undone;

    public RollbackSession(UUID playerUuid, List<Integer> rolledBackIds,
                           List<BlockChange> originalChanges) {
        this.playerUuid = playerUuid;
        this.timestamp = LocalDateTime.now();
        this.rolledBackIds = List.copyOf(rolledBackIds);
        this.originalChanges = List.copyOf(originalChanges);
        this.undone = false;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<Integer> getRolledBackIds() { return rolledBackIds; }
    public List<BlockChange> getOriginalChanges() { return originalChanges; }
    public boolean isUndone() { return undone; }
    public void markUndone() { this.undone = true; }

    public boolean isWithinUndoWindow(long seconds) {
        return !undone && java.time.Duration.between(
            timestamp, LocalDateTime.now()).getSeconds() <= seconds;
    }
}
