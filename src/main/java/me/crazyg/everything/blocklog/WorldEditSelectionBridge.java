package me.crazyg.everything.blocklog;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.LocalSession;

import me.crazyg.everything.Everything;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Optional adapter for a WorldEdit WorldEdit installation. The class is only
 * referenced through static guards, so it can be compiled against the WorldEdit
 * API while remaining absent at runtime. See AGENTS.md.
 */
public final class WorldEditSelectionBridge {

    private WorldEditSelectionBridge() {}

    public static boolean isAvailable() {
        try {
            if (Everything.getInstance() == null
                    || Everything.getInstance().getServer().getPluginManager()
                            .getPlugin("WorldEdit") == null) {
                return false;
            }
            Class.forName("com.sk89q.worldedit.WorldEdit");
            return WorldEdit.getInstance() != null;
        } catch (Throwable e) {
            return false;
        }
    }

    public static SelectionManager.Selection fromWorldEdit(Player player) {
        if (player == null || !isAvailable()) {
            return null;
        }
        try {
            com.sk89q.worldedit.entity.Player wePlayer =
                BukkitAdapter.adapt(player);
            com.sk89q.worldedit.session.SessionManager manager =
                WorldEdit.getInstance().getSessionManager();
            com.sk89q.worldedit.LocalSession session =
                manager.getIfPresent(wePlayer);
            if (session == null) {
                return null;
            }
            com.sk89q.worldedit.world.World world =
                session.getSelectionWorld();
            if (world == null) {
                return null;
            }
            com.sk89q.worldedit.regions.Region region =
                session.getSelection(world);
            if (!(region instanceof CuboidRegion cuboid)) {
                return null;
            }
            BlockVector3 min = cuboid.getMinimumPoint();
            BlockVector3 max = cuboid.getMaximumPoint();
            World bukkitWorld = world == null ? player.getWorld()
                : (World) BukkitAdapter.adapt(world);
            return new SelectionManager.Selection(
                bukkitWorld,
                min.getX(), min.getY(), min.getZ(),
                max.getX(), max.getY(), max.getZ());
        } catch (Throwable e) {
            Everything.getInstance().getLogger().fine(
                "WorldEdit selection unavailable: " + e.getMessage());
            return null;
        }
    }
}
