package me.crazyg.everything.blocklog;

import org.bukkit.inventory.ItemStack;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a single logged inventory/container change.
 */
public class InventoryLogEntry {

    private final int id;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final UUID playerUuid;
    private final String playerName;
    private final LocalDateTime timestamp;
    private final int slot;
    private final BlockChange.Action action;
    private final String oldItem;
    private final String newItem;

    public InventoryLogEntry(int id, String world, int x, int y, int z,
                             UUID playerUuid, String playerName,
                             LocalDateTime timestamp, int slot,
                             BlockChange.Action action,
                             String oldItem, String newItem) {
        this.id = id;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.timestamp = timestamp;
        this.slot = slot;
        this.action = action;
        this.oldItem = oldItem;
        this.newItem = newItem;
    }

    public int getId() { return id; }
    public String getWorld() { return world; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public int getSlot() { return slot; }
    public BlockChange.Action getAction() { return action; }
    public String getOldItem() { return oldItem; }
    public String getNewItem() { return newItem; }

    /**
     * Serializes an ItemStack to "MATERIAL|count|meta" format.
     * Meta is a simplified string of enchantments and display name.
     */
    public static String itemString(ItemStack item) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) {
            return "AIR|0|";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(item.getType().name());
        sb.append("|").append(item.getAmount());
        sb.append("|");
        if (item.hasItemMeta()) {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                sb.append("name:").append(meta.getDisplayName());
            }
            if (meta.hasEnchants()) {
                if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != '|') {
                    sb.append(",");
                }
                sb.append("ench:");
                meta.getEnchants().forEach((ench, lvl) ->
                    sb.append(ench.getName()).append(":").append(lvl).append(";"));
            }
        }
        return sb.toString();
    }
}
