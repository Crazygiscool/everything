package me.crazyg.everything.blocklog;

import me.crazyg.everything.Everything;
import me.crazyg.everything.utils.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Built-in WorldEdit-style selection tool. Tracks a single cuboid selection
 * per player in memory (session-only), provides the selection wand item, and
 * renders the selection outline as particles. The selection scopes rollback
 * and lookup operations (see {@link BlockLogCommand}).
 */
public class SelectionManager implements Listener {

    /** A normalized cuboid selection (corners sorted to min/max). */
    public record Selection(World world, int minX, int minY, int minZ,
                            int maxX, int maxY, int maxZ) {

        public int getWidth() { return maxX - minX + 1; }
        public int getHeight() { return maxY - minY + 1; }
        public int getLength() { return maxZ - minZ + 1; }
        public long getVolume() {
            return (long) getWidth() * getHeight() * getLength();
        }

        public int getCenterX() { return (minX + maxX) / 2; }
        public int getCenterY() { return (minY + maxY) / 2; }
        public int getCenterZ() { return (minZ + maxZ) / 2; }
        public int getRadiusX() { return (maxX - minX) / 2; }
        public int getRadiusY() { return (maxY - minY) / 2; }
        public int getRadiusZ() { return (maxZ - minZ) / 2; }

        public boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
        }
    }

    private final Everything plugin;
    private final Map<UUID, int[]> pos1 = new ConcurrentHashMap<>();
    private final Map<UUID, int[]> pos2 = new ConcurrentHashMap<>();
    private final Map<UUID, String> selWorld = new ConcurrentHashMap<>();
    private final NamespacedKey wandKey;
    private final ItemStack selectionWand;

    public SelectionManager(Everything plugin) {
        this.plugin = plugin;
        this.wandKey = new NamespacedKey(plugin, "selection_wand");
        this.selectionWand = buildWand();
        Bukkit.getScheduler().runTaskTimer(plugin, this::spawnSelectionParticles, 0L, 10L);
    }

    // ---------------------------------------------------------
    // Wand item
    // ---------------------------------------------------------

    private ItemStack buildWand() {
        String matName = plugin.getConfig()
            .getString("blocklog.selection-wand-material", "WOODEN_AXE")
            .toUpperCase(java.util.Locale.ROOT);
        Material mat;
        try {
            mat = Material.matchMaterial(matName);
        } catch (Exception e) {
            mat = null;
        }
        if (mat == null) mat = Material.WOODEN_AXE;
        ItemStack wand = ItemBuilder.builder(mat)
            .name("&bSelection Wand")
            .lore("&7Left-click a block: set position 1",
                "&7Right-click a block: set position 2",
                "&7Use &f//size&7 to view the selection.")
            .glowing()
            .unbreakable()
            .build();
        ItemMeta meta = wand.getItemMeta();
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
        wand.setItemMeta(meta);
        return wand;
    }

    public ItemStack getSelectionWand() {
        return selectionWand.clone();
    }

    public boolean isSelectionWand(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
            .has(wandKey, PersistentDataType.BYTE);
    }

    private boolean hasWandInInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isSelectionWand(item)) return true;
        }
        return false;
    }

    /** Gives the player the selection wand, or removes it if they already have one. */
    public void toggleWand(Player player) {
        if (hasWandInInventory(player)) {
            player.getInventory().removeItem(getSelectionWand());
            sendFancy(player, "Selection wand removed.");
        } else {
            player.getInventory().addItem(getSelectionWand());
            sendFancy(player, "Selection wand added. Left-click for position 1, right-click for position 2.");
        }
    }

    // ---------------------------------------------------------
    // Selection state (session-only, in memory)
    // ---------------------------------------------------------

    public Selection getSelection(Player player) {
        return computeSelection(player.getUniqueId());
    }

    public void clear(Player player) {
        UUID uuid = player.getUniqueId();
        pos1.remove(uuid);
        pos2.remove(uuid);
        selWorld.remove(uuid);
        sendFancy(player, "Selection cleared.");
    }

    public void setPos1(Player player, Block block) {
        setPoint(player, true, block.getWorld(), block.getX(), block.getY(), block.getZ());
    }

    public void setPos2(Player player, Block block) {
        setPoint(player, false, block.getWorld(), block.getX(), block.getY(), block.getZ());
    }

    public void setPos1(Player player, int x, int y, int z) {
        setPoint(player, true, player.getWorld(), x, y, z);
    }

    public void setPos2(Player player, int x, int y, int z) {
        setPoint(player, false, player.getWorld(), x, y, z);
    }

    private void setPoint(Player player, boolean first, World world,
                          int x, int y, int z) {
        UUID uuid = player.getUniqueId();
        String worldName = world.getName();
        String prevWorld = selWorld.get(uuid);
        if (prevWorld != null && !prevWorld.equals(worldName)) {
            pos1.remove(uuid);
            pos2.remove(uuid);
        }
        selWorld.put(uuid, worldName);
        if (first) {
            pos1.put(uuid, new int[] {x, y, z});
        } else {
            pos2.put(uuid, new int[] {x, y, z});
        }

        Selection sel = computeSelection(uuid);
        if (sel != null && exceedsLimit(sel)) {
            if (first) pos1.remove(uuid); else pos2.remove(uuid);
            sendFancy(player, "Selection exceeds the maximum size of "
                + BlockLogConfig.selectionMaxSize(plugin.getConfig())
                + " blocks per side.", NamedTextColor.RED);
            return;
        }

        sendFancy(player, (first ? "Position 1" : "Position 2")
            + " set to (" + x + ", " + y + ", " + z + ").");
        if (sel != null) {
            sendFancy(player, "Selection: " + sel.getWidth() + "x"
                + sel.getHeight() + "x" + sel.getLength()
                + ". Run //size for details.");
        } else {
            sendFancy(player, "Now set the other position.");
        }
    }

    // ---------------------------------------------------------
    // Expand / contract / shift / outset / inset
    // ---------------------------------------------------------

    public void expand(Player player, int amount, BlockFace dir) {
        modify(player, amount, dir, true);
    }

    public void contract(Player player, int amount, BlockFace dir) {
        modify(player, amount, dir, false);
    }

    public void expandVert(Player player) {
        Selection sel = requireSelection(player);
        if (sel == null) return;
        World world = sel.world();
        Selection next = new Selection(world, sel.minX(), world.getMinHeight(),
            sel.minZ(), sel.maxX(), world.getMaxHeight() - 1, sel.maxZ());
        if (enforceLimit(player, next)) {
            setFromSelection(player, next);
            sendFancy(player, "Selection expanded vertically ("
                + next.getHeight() + " blocks tall).");
        }
    }

    public void shift(Player player, int amount, BlockFace dir) {
        Selection sel = requireSelection(player);
        if (sel == null) return;
        int dx = 0, dy = 0, dz = 0;
        switch (dir) {
            case UP -> dy = amount;
            case DOWN -> dy = -amount;
            case NORTH -> dz = -amount;
            case SOUTH -> dz = amount;
            case EAST -> dx = amount;
            case WEST -> dx = -amount;
            default -> {}
        }
        Selection next = new Selection(sel.world(),
            sel.minX() + dx, sel.minY() + dy, sel.minZ() + dz,
            sel.maxX() + dx, sel.maxY() + dy, sel.maxZ() + dz);
        if (enforceLimit(player, next)) {
            setFromSelection(player, next);
            sendFancy(player, "Selection shifted " + amount + " " + dir.name().toLowerCase()
                + ". Now " + next.getWidth() + "x" + next.getHeight() + "x"
                + next.getLength() + ".");
        }
    }

    public void outset(Player player, int amount) {
        Selection sel = requireSelection(player);
        if (sel == null) return;
        Selection next = new Selection(sel.world(),
            sel.minX() - amount, sel.minY() - amount, sel.minZ() - amount,
            sel.maxX() + amount, sel.maxY() + amount, sel.maxZ() + amount);
        if (enforceLimit(player, next)) {
            setFromSelection(player, next);
            sendFancy(player, "Selection outset by " + amount + " in all directions.");
        }
    }

    public void inset(Player player, int amount) {
        Selection sel = requireSelection(player);
        if (sel == null) return;
        int minX = sel.minX() + amount, minY = sel.minY() + amount, minZ = sel.minZ() + amount;
        int maxX = sel.maxX() - amount, maxY = sel.maxY() - amount, maxZ = sel.maxZ() - amount;
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            sendFancy(player, "Cannot inset that far; the selection would collapse.", NamedTextColor.RED);
            return;
        }
        Selection next = new Selection(sel.world(), minX, minY, minZ, maxX, maxY, maxZ);
        if (enforceLimit(player, next)) {
            setFromSelection(player, next);
            sendFancy(player, "Selection inset by " + amount + " in all directions.");
        }
    }

    private void modify(Player player, int amount, BlockFace dir,
                        boolean expand) {
        if (amount <= 0) {
            sendFancy(player, "Amount must be a positive number.", NamedTextColor.RED);
            return;
        }
        Selection sel = requireSelection(player);
        if (sel == null) return;
        int minX = sel.minX(), minY = sel.minY(), minZ = sel.minZ();
        int maxX = sel.maxX(), maxY = sel.maxY(), maxZ = sel.maxZ();
        switch (dir) {
            case UP -> maxY = expand ? maxY + amount : maxY - amount;
            case DOWN -> minY = expand ? minY - amount : minY + amount;
            case NORTH -> minZ = expand ? minZ - amount : minZ + amount;
            case SOUTH -> maxZ = expand ? maxZ + amount : maxZ - amount;
            case EAST -> maxX = expand ? maxX + amount : maxX - amount;
            case WEST -> minX = expand ? minX - amount : minX + amount;
            default -> {}
        }
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            sendFancy(player, "Cannot " + (expand ? "expand" : "contract")
                + " further; the selection would collapse.", NamedTextColor.RED);
            return;
        }
        Selection next = new Selection(sel.world(), minX, minY, minZ, maxX, maxY, maxZ);
        if (enforceLimit(player, next)) {
            setFromSelection(player, next);
            String side = dir.name().toLowerCase(java.util.Locale.ROOT);
            sendFancy(player, (expand ? "Expanded" : "Contracted") + " selection "
                + amount + " " + side + ". Now " + next.getWidth() + "x"
                + next.getHeight() + "x" + next.getLength() + ".");
        }
    }

    private Selection requireSelection(Player player) {
        Selection sel = getSelection(player);
        if (sel == null) {
            sendFancy(player, "Make a selection first (//wand, //pos1, //pos2).");
        }
        return sel;
    }

    private void setFromSelection(Player player, Selection sel) {
        UUID uuid = player.getUniqueId();
        selWorld.put(uuid, sel.world().getName());
        pos1.put(uuid, new int[] {sel.minX(), sel.minY(), sel.minZ()});
        pos2.put(uuid, new int[] {sel.maxX(), sel.maxY(), sel.maxZ()});
    }

    private Selection computeSelection(UUID uuid) {
        int[] p1 = pos1.get(uuid);
        int[] p2 = pos2.get(uuid);
        if (p1 == null || p2 == null) return null;
        String worldName = selWorld.get(uuid);
        World world = worldName == null ? null : Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Selection(world,
            Math.min(p1[0], p2[0]), Math.min(p1[1], p2[1]), Math.min(p1[2], p2[2]),
            Math.max(p1[0], p2[0]), Math.max(p1[1], p2[1]), Math.max(p1[2], p2[2]));
    }

    private boolean exceedsLimit(Selection sel) {
        int max = BlockLogConfig.selectionMaxSize(plugin.getConfig());
        return sel.getWidth() > max || sel.getHeight() > max || sel.getLength() > max;
    }

    private boolean enforceLimit(Player player, Selection sel) {
        if (exceedsLimit(sel)) {
            sendFancy(player, "Selection exceeds the maximum size of "
                + BlockLogConfig.selectionMaxSize(plugin.getConfig())
                + " blocks per side.", NamedTextColor.RED);
            return false;
        }
        return true;
    }

    // ---------------------------------------------------------
    // Wand interaction
    // ---------------------------------------------------------

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        boolean mainHand = isSelectionWand(player.getInventory().getItemInMainHand());
        boolean offHand = isSelectionWand(player.getInventory().getItemInOffHand());
        if (!mainHand && !offHand) return;
        if (!player.hasPermission("everything.blocklog.select")) {
            sendFancy(player, "You do not have permission to use the selection wand.", NamedTextColor.RED);
            event.setCancelled(true);
            return;
        }

        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        event.setCancelled(true);
        if (action == Action.LEFT_CLICK_BLOCK) {
            setPos1(player, block);
        } else {
            setPos2(player, block);
        }
    }

    // ---------------------------------------------------------
    // Particle outline
    // ---------------------------------------------------------

    private void spawnSelectionParticles() {
        for (UUID uuid : selWorld.keySet()) {
            Selection sel = computeSelection(uuid);
            if (sel == null) continue;
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            if (!player.getWorld().equals(sel.world())) continue;
            Location center = new Location(sel.world(), sel.getCenterX(),
                sel.getCenterY(), sel.getCenterZ());
            if (player.getLocation().distanceSquared(center) > 128 * 128) continue;
            spawnCuboidParticles(player, sel);
        }
    }

    private void spawnCuboidParticles(Player player, Selection sel) {
        World world = sel.world();
        double minX = sel.minX();
        double maxX = sel.maxX() + 1.0;
        double minY = sel.minY();
        double maxY = sel.maxY() + 1.0;
        double minZ = sel.minZ();
        double maxZ = sel.maxZ() + 1.0;

        drawLine(player, world, minX, minY, minZ, maxX, minY, minZ);
        drawLine(player, world, maxX, minY, minZ, maxX, minY, maxZ);
        drawLine(player, world, maxX, minY, maxZ, minX, minY, maxZ);
        drawLine(player, world, minX, minY, maxZ, minX, minY, minZ);

        drawLine(player, world, minX, maxY, minZ, maxX, maxY, minZ);
        drawLine(player, world, maxX, maxY, minZ, maxX, maxY, maxZ);
        drawLine(player, world, maxX, maxY, maxZ, minX, maxY, maxZ);
        drawLine(player, world, minX, maxY, maxZ, minX, maxY, minZ);

        drawLine(player, world, minX, minY, minZ, minX, maxY, minZ);
        drawLine(player, world, maxX, minY, minZ, maxX, maxY, minZ);
        drawLine(player, world, maxX, minY, maxZ, maxX, maxY, maxZ);
        drawLine(player, world, minX, minY, maxZ, minX, maxY, maxZ);
    }

    private void drawLine(Player player, World world, double x1, double y1,
                          double z1, double x2, double y2, double z2) {
        Location from = new Location(world, x1, y1, z1);
        Location to = new Location(world, x2, y2, z2);
        plugin.getParticleManager().drawLine(player, from, to,
            Particle.VILLAGER_HAPPY, 0.5);
    }

    private void sendFancy(Player player, String message) {
        Everything.sendFancy(player, Component.text(message));
    }

    private void sendFancy(Player player, String message, NamedTextColor color) {
        Everything.sendFancy(player, Component.text(message).color(color));
    }
}