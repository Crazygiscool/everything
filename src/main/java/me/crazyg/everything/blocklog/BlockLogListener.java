package me.crazyg.everything.blocklog;

import me.crazyg.everything.Everything;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
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
 * Captures block changes (player + natural) and writes them to the block log
 * database. A rollback lock prevents logging blocks that are being reverted.
 */
public class BlockLogListener implements Listener {

    private final Everything plugin;
    private final BlockLogDatabase database;
    private final TileStateSerializer tileSerializer;

    public BlockLogListener(Everything plugin, BlockLogDatabase database) {
        this.plugin = plugin;
        this.database = database;
        this.tileSerializer = new TileStateSerializer(plugin);
    }

    // ---------------------------------------------------------
    // Rollback lock: blocks currently being reverted should not be re-logged.
    // ---------------------------------------------------------
    public boolean isLocked(Location loc) {
        return RollbackManager.isLocked(loc);
    }

    // ---------------------------------------------------------
    // Player actions
    // ---------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isEnabled()) return;
        Block block = event.getBlock();
        Player player = event.getPlayer();
        log(block, block.getType().name(),
            "AIR", serialize(block.getState()),
            BlockChange.Action.PLACE, player.getUniqueId(),
            player.getName(), null, block.getState());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isEnabled()) return;
        Block block = event.getBlock();
        Player player = event.getPlayer();
        log(block, block.getType().name(),
            serialize(block.getState()), "AIR",
            BlockChange.Action.BREAK, player.getUniqueId(),
            player.getName(), block.getState(), null);
    }

    // ---------------------------------------------------------
    // Natural / environmental changes
    // ---------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!isEnabled() || !logNatural()) return;
        for (Block block : event.blockList()) {
            if (isLocked(block.getLocation())) continue;
            log(block, block.getType().name(),
                serialize(block.getState()), "AIR",
                BlockChange.Action.EXPLODE, null, "Explosion", null, null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!isEnabled() || !logNatural()) return;
        Entity entity = event.getEntity();
        String name = entity == null ? "Explosion" : entity.getType().name();
        for (Block block : event.blockList()) {
            if (isLocked(block.getLocation())) continue;
            log(block, block.getType().name(),
                serialize(block.getState()), "AIR",
                BlockChange.Action.EXPLODE, null, name, null, null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (!isEnabled() || !logNatural()) return;
        Block block = event.getBlock();
        if (isLocked(block.getLocation())) return;
        log(block, block.getType().name(),
            serialize(block.getState()), "AIR",
            BlockChange.Action.BURN, null, "Fire", null, null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        if (!isEnabled() || !logNatural()) return;
        Block block = event.getBlock();
        BlockState newState = event.getNewState();
        if (isLocked(block.getLocation())) return;
        log(block, block.getType().name(),
            serialize(block.getState()),
            newState.getType().name(),
            BlockChange.Action.FADE, null, "Environment", null, null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!isEnabled() || !logNatural()) return;
        Block block = event.getBlock();
        if (isLocked(block.getLocation())) return;
        // Endermen picking up blocks, falling blocks, etc.
        log(block, block.getType().name(),
            serialize(block.getState()),
            event.getTo().name(),
            BlockChange.Action.ENTITY, null,
            event.getEntity().getType().name(), null, null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        if (!isEnabled() || !logNatural()) return;
        Block block = event.getBlock();
        if (isLocked(block.getLocation())) return;
        log(block, block.getType().name(),
            serialize(block.getState()),
            event.getNewState().getType().name(),
            BlockChange.Action.ENTITY, null,
            event.getEntity().getType().name(), null, null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (!isEnabled() || !logFluid()) return;
        Block to = event.getToBlock();
        if (isLocked(to.getLocation())) return;
        log(to, to.getType().name(),
            serialize(to.getState()),
            event.getBlock().getType().name(),
            BlockChange.Action.FLUID, null, "Fluid", null, null);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private void log(Block block, String blockType, String oldData,
                      String newData, BlockChange.Action action,
                      UUID uuid, String name, BlockState oldState,
                      BlockState newState) {
        if (isLocked(block.getLocation())) return;
        String oldTile = oldState == null ? null : tileSerializer.serialize(oldState);
        String newTile = newState == null ? null : tileSerializer.serialize(newState);
        database.logChange(block.getLocation(), blockType, oldData,
            newData, action, uuid, name, oldTile, newTile);
    }

    private String serialize(BlockState state) {
        Material type = state.getType();
        if (state instanceof ConfigurationSerializable serializable) {
            try {
                java.util.Map<String, Object> map = serializable.serialize();
                if (map != null && !map.isEmpty()) {
                    return type.name() + ":" + map.toString();
                }
            } catch (Exception e) {
                // fall through to material-only
            }
        }
        return type.name();
    }

    private boolean isEnabled() {
        return plugin.getConfig().getBoolean("blocklog.enabled", true);
    }

    private boolean logNatural() {
        return plugin.getConfig().getBoolean("blocklog.log-natural", true);
    }

    private boolean logFluid() {
        return plugin.getConfig().getBoolean("blocklog.log-fluid", false);
    }
}
