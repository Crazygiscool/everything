package me.crazyg.everything.blocklog;

import me.crazyg.everything.Everything;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Captures block changes (player + natural + growth + pistons + portals +
 * buckets + sign edits) and queues them for asynchronous persistence. A
 * rollback lock prevents logging blocks that are being reverted.
 */
public class BlockLogListener implements Listener {

    private final Everything plugin;
    private final BlockLogDatabase database;

    // Cached config flags (refreshed on reload).
    private volatile boolean enabled;
    private volatile boolean logNatural;
    private volatile boolean logFluid;
    private volatile boolean logGrowth;
    private volatile boolean logPiston;
    private volatile boolean logPortal;
    private volatile boolean logBucket;
    private volatile boolean logSignEdit;
    private volatile boolean logInteract;
    private volatile boolean logContainer;
    private volatile boolean logCraft;
    private volatile boolean logEntityDeath;

    private static final Set<Material> INTERACTIVE_MATERIALS = EnumSet.of(
        Material.OAK_DOOR, Material.SPRUCE_DOOR, Material.BIRCH_DOOR,
        Material.JUNGLE_DOOR, Material.ACACIA_DOOR, Material.DARK_OAK_DOOR,
        Material.MANGROVE_DOOR, Material.CHERRY_DOOR, Material.BAMBOO_DOOR,
        Material.CRIMSON_DOOR, Material.WARPED_DOOR,
        Material.OAK_TRAPDOOR, Material.SPRUCE_TRAPDOOR, Material.BIRCH_TRAPDOOR,
        Material.JUNGLE_TRAPDOOR, Material.ACACIA_TRAPDOOR, Material.DARK_OAK_TRAPDOOR,
        Material.MANGROVE_TRAPDOOR, Material.CHERRY_TRAPDOOR, Material.BAMBOO_TRAPDOOR,
        Material.CRIMSON_TRAPDOOR, Material.WARPED_TRAPDOOR,
        Material.OAK_FENCE_GATE, Material.SPRUCE_FENCE_GATE, Material.BIRCH_FENCE_GATE,
        Material.JUNGLE_FENCE_GATE, Material.ACACIA_FENCE_GATE, Material.DARK_OAK_FENCE_GATE,
        Material.MANGROVE_FENCE_GATE, Material.CHERRY_FENCE_GATE, Material.BAMBOO_FENCE_GATE,
        Material.CRIMSON_FENCE_GATE, Material.WARPED_FENCE_GATE,
        Material.STONE_BUTTON, Material.OAK_BUTTON, Material.SPRUCE_BUTTON,
        Material.BIRCH_BUTTON, Material.JUNGLE_BUTTON, Material.ACACIA_BUTTON,
        Material.DARK_OAK_BUTTON, Material.MANGROVE_BUTTON, Material.CHERRY_BUTTON,
        Material.BAMBOO_BUTTON, Material.CRIMSON_BUTTON, Material.WARPED_BUTTON,
        Material.POLISHED_BLACKSTONE_BUTTON,
        Material.LEVER,
        Material.JUKEBOX, Material.NOTE_BLOCK,
        Material.CRAFTING_TABLE, Material.ENCHANTING_TABLE, Material.LOOM,
        Material.STONECUTTER, Material.GRINDSTONE, Material.ANVIL,
        Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL, Material.SMITHING_TABLE,
        Material.BARREL, Material.BLAST_FURNACE, Material.SMOKER,
        Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
        Material.LECTERN
    );

    public BlockLogListener(Everything plugin, BlockLogDatabase database) {
        this.plugin = plugin;
        this.database = database;
        reloadConfig();
    }

    /** Refresh cached config flags (call on plugin config reload). */
    public void reloadConfig() {
        this.enabled = BlockLogConfig.isEnabled(plugin.getConfig());
        this.logNatural = BlockLogConfig.logNatural(plugin.getConfig());
        this.logFluid = BlockLogConfig.logFluid(plugin.getConfig());
        this.logGrowth = BlockLogConfig.getBoolean(plugin.getConfig(),
            "blocklog.log-growth", true);
        this.logPiston = BlockLogConfig.getBoolean(plugin.getConfig(),
            "blocklog.log-piston", true);
        this.logPortal = BlockLogConfig.getBoolean(plugin.getConfig(),
            "blocklog.log-portal", true);
        this.logBucket = BlockLogConfig.getBoolean(plugin.getConfig(),
            "blocklog.log-bucket", true);
        this.logSignEdit = BlockLogConfig.getBoolean(plugin.getConfig(),
            "blocklog.log-sign-edit", true);
        this.logInteract = BlockLogConfig.logInteract(plugin.getConfig());
        this.logContainer = BlockLogConfig.logContainer(plugin.getConfig());
        this.logCraft = BlockLogConfig.logCraft(plugin.getConfig());
        this.logEntityDeath = BlockLogConfig.logEntityDeath(plugin.getConfig());
    }

    // ---------------------------------------------------------
    // Rollback lock: blocks currently being reverted should not be re-logged.
    // ---------------------------------------------------------
    public boolean isLocked(Location loc) {
        return RollbackManager.isLocked(loc);
    }

    private boolean isWorldLogged(Location loc) {
        return loc.getWorld() != null
            && BlockLogConfig.isWorldLogged(
                plugin.getConfig(), loc.getWorld().getName());
    }

    // ---------------------------------------------------------
    // Player actions
    // ---------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!enabled) return;
        Block block = event.getBlock();
        if (!isWorldLogged(block.getLocation())) return;
        Player player = event.getPlayer();
        log(block, BlockChange.Action.PLACE, player.getUniqueId(),
            player.getName(), null, block.getState());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!enabled) return;
        Block block = event.getBlock();
        if (!isWorldLogged(block.getLocation())) return;
        Player player = event.getPlayer();
        log(block, BlockChange.Action.BREAK, player.getUniqueId(),
            player.getName(), block.getState(), null);
    }

    // ---------------------------------------------------------
    // Natural / environmental changes
    // ---------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!enabled || !logNatural) return;
        for (Block block : event.blockList()) {
            if (isLocked(block.getLocation())) continue;
            if (!isWorldLogged(block.getLocation())) continue;
            log(block, BlockChange.Action.EXPLODE, null, "Explosion",
                block.getState(), null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!enabled || !logNatural) return;
        Entity entity = event.getEntity();
        String name = entity == null ? "Explosion" : entity.getType().name();
        for (Block block : event.blockList()) {
            if (isLocked(block.getLocation())) continue;
            if (!isWorldLogged(block.getLocation())) continue;
            log(block, BlockChange.Action.EXPLODE, null, name,
                block.getState(), null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (!enabled || !logNatural) return;
        Block block = event.getBlock();
        if (isLocked(block.getLocation())) return;
        if (!isWorldLogged(block.getLocation())) return;
        log(block, BlockChange.Action.BURN, null, "Fire",
            block.getState(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        if (!enabled || !logNatural) return;
        Block block = event.getBlock();
        BlockState newState = event.getNewState();
        if (isLocked(block.getLocation())) return;
        if (!isWorldLogged(block.getLocation())) return;
        log(block, BlockChange.Action.FADE, null, "Environment",
            block.getState(), newState);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!enabled || !logNatural) return;
        Block block = event.getBlock();
        if (isLocked(block.getLocation())) return;
        if (!isWorldLogged(block.getLocation())) return;
        log(block, BlockChange.Action.ENTITY, null,
            event.getEntity().getType().name(),
            block.getState(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        if (!enabled || !logNatural) return;
        Block block = event.getBlock();
        if (isLocked(block.getLocation())) return;
        if (!isWorldLogged(block.getLocation())) return;
        log(block, BlockChange.Action.ENTITY, null,
            event.getEntity().getType().name(), block.getState(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (!enabled || !logFluid) return;
        Block to = event.getToBlock();
        if (isLocked(to.getLocation())) return;
        if (!isWorldLogged(to.getLocation())) return;
        log(to, BlockChange.Action.FLUID, null, "Fluid",
            to.getState(), event.getBlock().getState());
    }

    // ---------------------------------------------------------
    // Growth / decay (Phase 1)
    // ---------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (!enabled || !logNatural) return;
        Block block = event.getBlock();
        if (isLocked(block.getLocation())) return;
        if (!isWorldLogged(block.getLocation())) return;
        log(block, BlockChange.Action.LEAF_DECAY, null, "Decay",
            block.getState(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        if (!enabled || !logGrowth) return;
        Block block = event.getBlock();
        if (isLocked(block.getLocation())) return;
        if (!isWorldLogged(block.getLocation())) return;
        log(block, BlockChange.Action.GROWTH, null, "Growth",
            block.getState(), event.getNewState());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        if (!enabled || !logGrowth) return;
        for (org.bukkit.block.BlockState state : event.getBlocks()) {
            Block block = state.getBlock();
            if (isLocked(block.getLocation())) continue;
            if (!isWorldLogged(block.getLocation())) continue;
            log(block, BlockChange.Action.GROWTH, null, "Growth",
                null, state);
        }
    }

    // ---------------------------------------------------------
    // Pistons (Phase 1)
    // ---------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!enabled || !logPiston) return;
        for (Block block : event.getBlocks()) {
            if (isLocked(block.getLocation())) continue;
            if (!isWorldLogged(block.getLocation())) continue;
            log(block, BlockChange.Action.PISTON, null,
                "Piston (extend)", block.getState(), null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!enabled || !logPiston) return;
        for (Block block : event.getBlocks()) {
            if (isLocked(block.getLocation())) continue;
            if (!isWorldLogged(block.getLocation())) continue;
            log(block, BlockChange.Action.PISTON, null,
                "Piston (retract)", block.getState(), null);
        }
    }

    // ---------------------------------------------------------
    // Portals (Phase 1)
    // ---------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        if (!enabled || !logPortal) return;
        for (BlockState state : event.getBlocks()) {
            Block block = state.getBlock();
            if (isLocked(block.getLocation())) continue;
            if (!isWorldLogged(block.getLocation())) continue;
            log(block, BlockChange.Action.PORTAL, null, "Portal",
                null, state);
        }
    }

    // ---------------------------------------------------------
    // Buckets (Phase 1)
    // ---------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketFill(org.bukkit.event.player.PlayerBucketFillEvent event) {
        if (!enabled || !logBucket) return;
        Player player = event.getPlayer();
        Block block = event.getBlockClicked().getRelative(
            event.getBlockFace());
        if (isLocked(block.getLocation())) return;
        if (!isWorldLogged(block.getLocation())) return;
        log(block, BlockChange.Action.BUCKET, player.getUniqueId(),
            player.getName(), block.getState(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketEmpty(org.bukkit.event.player.PlayerBucketEmptyEvent event) {
        if (!enabled || !logBucket) return;
        Player player = event.getPlayer();
        Block block = event.getBlockClicked().getRelative(
            event.getBlockFace());
        if (isLocked(block.getLocation())) return;
        if (!isWorldLogged(block.getLocation())) return;
        log(block, BlockChange.Action.BUCKET, player.getUniqueId(),
            player.getName(), null, block.getState());
    }

    // ---------------------------------------------------------
    // Sign edits (Phase 1)
    // ---------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (!enabled || !logSignEdit) return;
        Block block = event.getBlock();
        if (isLocked(block.getLocation())) return;
        if (!isWorldLogged(block.getLocation())) return;
        Player player = event.getPlayer();
        log(block, BlockChange.Action.SIGN_EDIT,
            player == null ? null : player.getUniqueId(),
            player == null ? "Sign" : player.getName(),
            null, block.getState());
    }

    // ---------------------------------------------------------
    // Player interactions (Phase 3)
    // ---------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!enabled || !logInteract) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!INTERACTIVE_MATERIALS.contains(block.getType())) return;
        if (isLocked(block.getLocation())) return;
        if (!isWorldLogged(block.getLocation())) return;
        Player player = event.getPlayer();
        log(block, BlockChange.Action.INTERACT, player.getUniqueId(),
            player.getName(), block.getState(), block.getState());
    }

    // ---------------------------------------------------------
    // Container / inventory logging (Phase 4)
    // ---------------------------------------------------------

    private static final Set<InventoryType> CONTAINER_TYPES = EnumSet.of(
        InventoryType.CHEST, InventoryType.BARREL,
        InventoryType.BLAST_FURNACE, InventoryType.SMOKER,
        InventoryType.FURNACE, InventoryType.HOPPER,
        InventoryType.DROPPER, InventoryType.DISPENSER,
        InventoryType.ENDER_CHEST, InventoryType.SHULKER_BOX
    );

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!enabled || !logContainer) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory topInv = event.getView().getTopInventory();
        if (topInv == null) return;
        InventoryHolder holder = topInv.getHolder();
        if (holder == null) return;
        if (!CONTAINER_TYPES.contains(topInv.getType())) return;
        Block block = null;
        if (holder instanceof BlockState bs) {
            block = bs.getBlock();
        }
        if (block == null) return;
        if (!isWorldLogged(block.getLocation())) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= topInv.getSize()) return;

        ItemStack oldItem = event.getView().getItem(rawSlot);
        ItemStack newItem = computeNewItem(event, topInv, rawSlot);

        String oldItemStr = InventoryLogEntry.itemString(oldItem);
        String newItemStr = InventoryLogEntry.itemString(newItem);

        if (oldItemStr.equals(newItemStr)) return;

        database.logInventoryChange(
            block.getLocation(), player.getUniqueId(), player.getName(),
            rawSlot, BlockChange.Action.CONTAINER, oldItemStr, newItemStr);
    }

    private ItemStack computeNewItem(InventoryClickEvent event,
                                     Inventory topInv, int rawSlot) {
        InventoryAction action = event.getAction();
        return switch (action) {
            case PICKUP_ALL, PICKUP_SOME, PICKUP_HALF, PICKUP_ONE ->
                null;
            case PLACE_ALL, PLACE_SOME, PLACE_ONE ->
                event.getCursor();
            case SWAP_WITH_CURSOR ->
                event.getCursor();
            case HOTBAR_SWAP ->
                event.getView().getBottomInventory().getItem(event.getHotbarButton());
            case MOVE_TO_OTHER_INVENTORY ->
                null;
            default -> event.getCursor();
        };
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!enabled || !logCraft) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack result = event.getRecipe().getResult();
        Location loc = player.getLocation();
        if (!isWorldLogged(loc)) return;
        String resultStr = InventoryLogEntry.itemString(result);
        database.logInventoryChange(
            loc, player.getUniqueId(), player.getName(),
            -1, BlockChange.Action.CRAFT, "AIR|0|", resultStr);
    }

    // ---------------------------------------------------------
    // Entity kill/death logging (Phase 5)
    // ---------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!enabled || !logEntityDeath) return;
        org.bukkit.entity.LivingEntity entity = event.getEntity();
        Location loc = entity.getLocation();
        if (!isWorldLogged(loc)) return;

        Player killer = entity.getKiller();
        if (killer != null) {
            database.logEntityChange(loc, entity.getType().name(),
                killer.getUniqueId(), killer.getName(),
                BlockChange.Action.ENTITY_KILL, null);
        } else {
            String cause = entity.getLastDamageCause() != null
                ? entity.getLastDamageCause().getCause().name() : "Unknown";
            database.logEntityChange(loc, entity.getType().name(),
                null, entity.getType().name(),
                BlockChange.Action.ENTITY_DEATH, cause);
        }
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private void log(Block block, BlockChange.Action action, UUID uuid,
                      String name, BlockState oldState, BlockState newState) {
        if (isLocked(block.getLocation())) return;
        String oldData = oldState == null ? null : dataString(oldState);
        String newData = newState == null ? null : dataString(newState);
        database.logChange(block.getLocation(),
            block.getType().name(), oldData, newData,
            action, uuid, name, oldState, newState);
    }

    private String dataString(BlockState state) {
        BlockData bd = state.getBlockData();
        return state.getType().name() + "|" + bd.getAsString();
    }
}
