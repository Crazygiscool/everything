package me.crazyg.everything.blocklog;

import me.crazyg.everything.Everything;

import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrushableBlock;
import org.bukkit.block.TileState;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

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
 *
 * <p>States that cannot be round-tripped via Bukkit's object stream (currently
 * brushable suspicious sand/gravel, GitHub issue #93) use a dedicated marker
 * payload so their {@code item} field can still be restored without relying
 * on serialization of the whole state.
 */
public class TileStateSerializer {

    public static final String BRUSHABLE_PREFIX = "brushable:";

    private final Everything plugin;

    public TileStateSerializer(Everything plugin) {
        this.plugin = plugin;
    }

    public boolean hasData(BlockState state) {
        if (state == null) {
            return false;
        }
        // Brushable states cannot be serialized wholesale (see #93), but we can
        // handle the part that matters: the discovered item.
        if (state instanceof BrushableBlock brushable) {
            ItemStack item = brushable.getItem();
            return item != null && !item.getType().isAir();
        }
        return state instanceof TileState
            || state instanceof InventoryHolder;
    }

    public String serialize(BlockState state) {
        if (state == null || !hasData(state)) {
            return null;
        }
        if (state instanceof BrushableBlock brushable) {
            String itemPayload = encode(brushable.getItem());
            return itemPayload == null ? null
                : BRUSHABLE_PREFIX + itemPayload;
        }
        return encode(state);
    }

    public BlockState deserialize(String data) {
        if (data == null || data.isEmpty()
                || data.startsWith(BRUSHABLE_PREFIX)) {
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

    /**
     * Restores the tile state recorded in {@code data} at {@code loc}. Returns
     * {@code true} if a state was successfully applied. Unlike
     * {@link #deserialize(String)}, this also handles the dedicated brushable
     * payload which is not a {@link BlockState}.
     */
    public boolean restore(Location loc, String data) {
        if (loc == null || data == null || data.isEmpty()) {
            return false;
        }
        if (data.startsWith(BRUSHABLE_PREFIX)) {
            return restoreBrushable(loc,
                data.substring(BRUSHABLE_PREFIX.length()));
        }
        try {
            BlockState state = deserialize(data);
            if (state == null) {
                return false;
            }
            BlockState relocated = state.copy(loc);
            relocated.update(true, true);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning(
                "Failed to restore tile state at " + loc + ": "
                    + e.getMessage());
            return false;
        }
    }

    private boolean restoreBrushable(Location loc, String itemPayload) {
        Object itemObj = decode(itemPayload);
        if (!(itemObj instanceof ItemStack)) {
            return false;
        }
        ItemStack item = (ItemStack) itemObj;
        BlockState current = loc.getBlock().getState();
        if (!(current instanceof BrushableBlock brushable)) {
            return false;
        }
        brushable.setItem(item);
        current.update(true, true);
        return true;
    }

    private String encode(Object value) {
        if (value == null) {
            return null;
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(baos)) {
            out.writeObject(value);
            out.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning(
                "Failed to serialize tile state: " + e.getMessage());
            return null;
        }
    }

    private Object decode(String payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try (ByteArrayInputStream bais =
                 new ByteArrayInputStream(Base64.getDecoder().decode(payload));
             BukkitObjectInputStream in = new BukkitObjectInputStream(bais)) {
            return in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().warning(
                "Failed to deserialize tile state: " + e.getMessage());
            return null;
        }
    }
}
