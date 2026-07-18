package me.crazyg.everything.blocklog;

import me.crazyg.everything.Everything;

import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.inventory.InventoryHolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

/**
 * Serializes {@link BlockState}s that carry extra data (chest inventories,
 * sign text, skull owners, etc.) to a compact Base64 string for storage, and
 * restores them on rollback. Plain blocks (no tile entity) return null.
 */
public class TileStateSerializer {

    private final Everything plugin;

    public TileStateSerializer(Everything plugin) {
        this.plugin = plugin;
    }

    public boolean hasData(BlockState state) {
        return state instanceof TileState
            || state instanceof InventoryHolder;
    }

    public String serialize(BlockState state) {
        if (!hasData(state)) {
            return null;
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(baos)) {
            out.writeObject(state);
            out.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning(
                "Failed to serialize tile state: " + e.getMessage());
            return null;
        }
    }

    public BlockState deserialize(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try (ByteArrayInputStream bais =
                 new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream in = new BukkitObjectInputStream(bais)) {
            Object obj = in.readObject();
            if (obj instanceof BlockState state) {
                return state;
            }
            return null;
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().warning(
                "Failed to deserialize tile state: " + e.getMessage());
            return null;
        }
    }
}
