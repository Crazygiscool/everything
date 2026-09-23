package me.crazyg.everything.blocklog;

import me.crazyg.everything.Everything;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Inspect mode. Once toggled via /inspect, every block the player tries to
 * break or place is intercepted (the event is cancelled) and the block's
 * logged change history is shown in chat instead. The event is cancelled at
 * HIGH priority so the block is left untouched and {@link BlockLogListener}
 * (MONITOR, ignoreCancelled) never logs the inspection attempt.
 */
public class InspectManager implements Listener {

    private final Everything plugin;
    private final BlockLogDatabase database;
    private final Set<UUID> inspecting = ConcurrentHashMap.newKeySet();

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public InspectManager(Everything plugin, BlockLogDatabase database) {
        this.plugin = plugin;
        this.database = database;
    }

    public boolean isInspecting(Player player) {
        return inspecting.contains(player.getUniqueId());
    }

    public void toggle(Player player) {
        if (isInspecting(player)) {
            setEnabled(player, false);
        } else {
            setEnabled(player, true);
        }
    }

    public void setEnabled(Player player, boolean enabled) {
        UUID uuid = player.getUniqueId();
        if (enabled) {
            inspecting.add(uuid);
            Everything.sendFancy(player, Component.text(
                "Inspect mode enabled. Block changes you try to make are shown as logs instead.")
                .color(NamedTextColor.GREEN));
        } else {
            inspecting.remove(uuid);
            Everything.sendFancy(player, Component.text("Inspect mode disabled.")
                .color(NamedTextColor.GRAY));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!isInspecting(player)) return;
        event.setCancelled(true);
        showHistory(player, event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!isInspecting(player)) return;
        event.setCancelled(true);
        showHistory(player, event.getBlock().getLocation());
    }

    private void showHistory(Player player, Location loc) {
        int limit = plugin.getConfig()
            .getInt("blocklog.max-history-per-block", 25);
        List<BlockChange> history = database.getHistory(loc, limit);
        if (history.isEmpty()) {
            Everything.sendFancy(player, Component.text("No logged changes at this block.")
                .color(NamedTextColor.YELLOW));
            return;
        }

        Everything.sendFancy(player, Component.text("----- Block History -----")
            .color(NamedTextColor.GOLD)
            .decorate(TextDecoration.BOLD));
        Everything.sendFancy(player, Component.text("Location: ").color(NamedTextColor.GRAY)
            .append(Component.text(loc.getBlockX() + ", "
                + loc.getBlockY() + ", " + loc.getBlockZ()
                + " (" + loc.getWorld().getName() + ")")
                .color(NamedTextColor.WHITE)));

        int index = 0;
        LocalDateTime now = LocalDateTime.now();
        for (BlockChange change : history) {
            String who = change.getPlayerName();
            String ago = formatAgo(change.getTimestamp(), now);

            Component line = Component.text("")
                .append(Component.text("#" + index + " ").color(NamedTextColor.DARK_GRAY))
                .append(actionComponent(change.getAction()))
                .append(Component.text(" by ").color(NamedTextColor.GRAY))
                .append(Component.text(who == null ? "Natural" : who)
                    .color(NamedTextColor.AQUA))
                .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text(change.getOldMaterial()
                    + " -> " + change.getNewMaterial())
                    .color(NamedTextColor.WHITE))
                .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text(ago).color(NamedTextColor.YELLOW));

            Everything.sendFancy(player, line);
            index++;
        }
    }

    private Component actionComponent(BlockChange.Action action) {
        NamedTextColor color = switch (action) {
            case PLACE -> NamedTextColor.GREEN;
            case BREAK -> NamedTextColor.RED;
            case EXPLODE -> NamedTextColor.GOLD;
            case BURN -> NamedTextColor.RED;
            case FADE -> NamedTextColor.YELLOW;
            case ENTITY -> NamedTextColor.LIGHT_PURPLE;
            case BUCKET -> NamedTextColor.AQUA;
            case FLUID -> NamedTextColor.BLUE;
            default -> NamedTextColor.GRAY;
        };
        return Component.text("[" + action.name() + "]").color(color);
    }

    private String formatAgo(LocalDateTime then, LocalDateTime now) {
        Duration d = Duration.between(then, now);
        long secs = d.getSeconds();
        if (secs < 60) return secs + "s ago";
        long mins = secs / 60;
        if (mins < 60) return mins + "m ago";
        long hours = mins / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        if (days < 30) return days + "d ago";
        return then.format(TIME_FMT);
    }
}