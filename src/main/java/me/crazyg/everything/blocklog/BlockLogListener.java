package me.crazyg.everything.blocklog;

import me.crazyg.everything.Everything;

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
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;

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
