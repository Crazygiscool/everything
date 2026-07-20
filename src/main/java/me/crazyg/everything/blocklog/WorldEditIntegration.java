package me.crazyg.everything.blocklog;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Utility class for WorldEdit/FAWE integration.
 * All methods gracefully return null/false if WorldEdit is not installed.
 * Uses pure reflection to avoid compile-time dependency on WorldEdit classes.
 */
public class WorldEditIntegration {

    private static boolean checked = false;
    private static boolean available = false;

    /**
     * Returns true if WorldEdit or FAWE is available on the server.
     */
    public static boolean isAvailable() {
        if (!checked) {
            try {
                Class.forName("com.sk89q.worldedit.bukkit.WorldEditPlugin");
                available = true;
            } catch (ClassNotFoundException e) {
                available = false;
            }
            checked = true;
        }
        return available;
    }

    /**
     * Returns the selection bounds for a player, or null if no selection
     * or WorldEdit is not installed.
     */
    public static SelectionBounds getSelection(Player player) {
        if (!isAvailable()) return null;
        try {
            Object wePlugin = getWorldEditPlugin();
            if (wePlugin == null) return null;

            Method wrapPlayer = wePlugin.getClass().getMethod(
                "wrapPlayer", Player.class);
            Object wePlayer = wrapPlayer.invoke(wePlugin, player);
            if (wePlayer == null) return null;

            Method getSessionManager = wePlugin.getClass().getMethod(
                "getSessionManager");
            Object sessionManager = getSessionManager.invoke(wePlugin);

            Method getSession = sessionManager.getClass().getMethod(
                "get", wePlayer.getClass());
            Object session = getSession.invoke(sessionManager, wePlayer);
            if (session == null) return null;

            Method hasSelection = session.getClass().getMethod(
                "hasSelection");
            Boolean hasSel = (Boolean) hasSelection.invoke(session);
            if (hasSel == null || !hasSel) return null;

            Method getSelection = session.getClass().getMethod(
                "getSelection");
            Object region = getSelection.invoke(session);
            if (region == null) return null;

            Method getMin = region.getClass().getMethod(
                "getMinimumPoint");
            Method getMax = region.getClass().getMethod(
                "getMaximumPoint");
            Object min = getMin.invoke(region);
            Object max = getMax.invoke(region);

            Method getX = min.getClass().getMethod("getX");
            Method getY = min.getClass().getMethod("getY");
            Method getZ = min.getClass().getMethod("getZ");

            int minX = ((Number) getX.invoke(min)).intValue();
            int minY = ((Number) getY.invoke(min)).intValue();
            int minZ = ((Number) getZ.invoke(min)).intValue();
            int maxX = ((Number) getX.invoke(max)).intValue();
            int maxY = ((Number) getY.invoke(max)).intValue();
            int maxZ = ((Number) getZ.invoke(max)).intValue();

            World world = player.getWorld();
            return new SelectionBounds(world, minX, minY, minZ,
                maxX, maxY, maxZ);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object getWorldEditPlugin() {
        try {
            Object pm = Bukkit.getPluginManager();
            Method getPlugin = pm.getClass().getMethod("getPlugin", String.class);
            Object we = getPlugin.invoke(pm, "WorldEdit");
            if (we != null) return we;
            return getPlugin.invoke(pm, "FastAsyncWorldEdit");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Holds the min/max corners of a WorldEdit selection.
     */
    public record SelectionBounds(World world, int minX, int minY, int minZ,
                                   int maxX, int maxY, int maxZ) {
        public int getRadiusX() { return (maxX - minX) / 2; }
        public int getRadiusY() { return (maxY - minY) / 2; }
        public int getRadiusZ() { return (maxZ - minZ) / 2; }
        public int getCenterX() { return (minX + maxX) / 2; }
        public int getCenterY() { return (minY + maxY) / 2; }
        public int getCenterZ() { return (minZ + maxZ) / 2; }
    }
}
