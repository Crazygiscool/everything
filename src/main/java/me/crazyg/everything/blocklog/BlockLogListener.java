package me.crazyg.everything.blocklog;

import me.crazyg.everything.Everything;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
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
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.UUID;

/**
 * Captures block changes (player + natural) and queues them for asynchronous
 * persistence. A rollback lock prevents logging blocks that are being reverted.
 */
public class BlockLogListener implements Listener {

    private final Everything plugin;
    private final BlockLogDatabase database;

    // Cached config flags (refreshed on reload).
    private volatile boolean enabled;
    private volatile boolean logNatural;
    private volatile boolean logFluid;

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
        log(block, block.getType().name(),
            "AIR", BlockChange.Action.PLACE, player.getUniqueId(),
            player.getName(), null, block.getState());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!enabled) return;
        Block block = event.getBlock();
        if (!isWorldLogged(block.getLocation())) return;
        Player player = event.getPlayer();
        log(block, block.getType().name(),
            "AIR", BlockChange.Action.BREAK, player.getUniqueId(),
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
            log(block, block.getType().name(),
                "AIR", BlockChange.Action.EXPLODE, null, "Explosion",
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
            log(block, block.getType().name(),
                "AIR", BlockChange.Action.EXPLODE, null, name,
                block.getState(), null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (!enabled || !logNatural) return;
        Block block = event.getBlock();
        if (isLocked(block.getLocation())) return;
        if (!isWorldLogged(block.getLocation())) return;
        log(block, block.getType().name(),
            "AIR", BlockChange.Action.BURN, null, "Fire",
            block.getState(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        if (!enabled || !logNatural) return;
        Block block = event.getBlock();
        BlockState newState = event.getNewState();
        if (isLocked(block.getLocation())) return;
        if (!isWorldLogged(block.getLocation())) return;
        log(block, block.getType().name(),
            newState.getType().name(), BlockChange.Action.FADE, null,
            "Environment", block.getState(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!enabled || !logNatural) return;
        Block block = event.getBlock();
        if (isLocked(block.getLocation())) return;
        if (!isWorldLogged(block.getLocation())) return;
        // Endermen picking up blocks, falling blocks, etc.
        log(block, block.getType().name(),
            event.getTo().name(), BlockChange.Action.ENTITY, null,
            event.getEntity().getType().name(), block.getState(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        if (!enabled || !logNatural) return;
        Block block = event.getBlock();
        if (isLocked(block.getLocation())) return;
        if (!isWorldLogged(block.getLocation())) return;
        log(block, block.getType().name(),
            event.getNewState().getType().name(),
            BlockChange.Action.ENTITY, null,
            event.getEntity().getType().name(), block.getState(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (!enabled || !logFluid) return;
        Block to = event.getToBlock();
        if (isLocked(to.getLocation())) return;
        if (!isWorldLogged(to.getLocation())) return;
        log(to, to.getType().name(),
            event.getBlock().getType().name(),
            BlockChange.Action.FLUID, null, "Fluid",
            to.getState(), null);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private void log(Block block, String blockType, String newData,
                      BlockChange.Action action, UUID uuid, String name,
                      BlockState oldState, BlockState newState) {
        if (isLocked(block.getLocation())) return;
        database.logChange(block.getLocation(), blockType, "AIR", newData,
            action, uuid, name, oldState, newState);
    }
}
