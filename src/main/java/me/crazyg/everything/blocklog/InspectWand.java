package me.crazyg.everything.blocklog;

import me.crazyg.everything.Everything;
import me.crazyg.everything.utils.AdventureCompat;
import me.crazyg.everything.utils.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides the inspect wand. Left-click shows a block's history in chat;
 * right-click rolls back just that one block.
 */
public class InspectWand implements Listener {

    private final Everything plugin;
    private final BlockLogDatabase database;
    private final RollbackManager rollbackManager;

    private final Map<UUID, Boolean> inspectors = new ConcurrentHashMap<>();
    private final Map<UUID, InspectArea> areas = new ConcurrentHashMap<>();
    private ItemStack wandItem;

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static final class InspectArea {
        final Location center;
        final int size;

        InspectArea(Location center, int size) {
            this.center = center;
            this.size = size;
        }
    }

    public InspectWand(Everything plugin, BlockLogDatabase database,
                       RollbackManager rollbackManager) {
        this.plugin = plugin;
        this.database = database;
        this.rollbackManager = rollbackManager;
        buildWand();
    }

    private void buildWand() {
        String matName = plugin.getConfig()
            .getString("blocklog.inspect-wand-material", "STICK")
            .toUpperCase(java.util.Locale.ROOT);
        Material mat;
        try {
            mat = Material.matchMaterial(matName);
        } catch (Exception e) {
            mat = null;
        }
        if (mat == null) mat = Material.STICK;
        this.wandItem = ItemBuilder.builder(mat)
            .name("&bInspect Wand")
            .lore("&7Left-click a block to view its history.",
                "&7Right-click a block to set the inspect area.")
            .glowing()
            .unbreakable()
            .build();
    }

    public ItemStack getWandItem() {
        return wandItem.clone();
    }

    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() != wandItem.getType()) return false;
        return item.isSimilar(wandItem);
    }

    public void toggle(Player player) {
        UUID uuid = player.getUniqueId();
        if (inspectors.containsKey(uuid)) {
            inspectors.remove(uuid);
            AdventureCompat.sendMessage(player,
                Component.text("Inspect wand disabled.")
                    .color(NamedTextColor.GRAY));
        } else {
            inspectors.put(uuid, true);
            player.getInventory().addItem(getWandItem());
            AdventureCompat.sendMessage(player,
                Component.text("Inspect wand enabled! ")
                    .color(NamedTextColor.GREEN)
                    .append(Component.text(
                        "Left-click a block for history, right-click to set the inspect area.")
                        .color(NamedTextColor.GRAY)));
        }
    }

    public boolean isInspecting(UUID uuid) {
        return inspectors.containsKey(uuid);
    }

    // ---------------------------------------------------------
    // Per-player inspect area (configured via right-click GUI)
    // ---------------------------------------------------------

    public void setArea(UUID uuid, Location center, int size) {
        areas.put(uuid, new InspectArea(center, size));
    }

    public InspectArea getArea(UUID uuid) {
        return areas.get(uuid);
    }

    public void clearArea(UUID uuid) {
        areas.remove(uuid);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!inspectors.containsKey(player.getUniqueId())) return;
        if (!isWand(player.getInventory().getItemInMainHand())
            && !isWand(player.getInventory().getItemInOffHand())) {
            return;
        }
        if (event.getAction() != Action.LEFT_CLICK_BLOCK
            && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) return;
        event.setCancelled(true);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            showHistory(player, block.getLocation());
        } else {
            InspectArea current = areas.get(player.getUniqueId());
            int initial = current == null ? 10 : current.size;
            InspectAreaGUI gui = new InspectAreaGUI(
                player, this, block.getLocation(), initial);
            gui.open();
        }
    }

    private void showHistory(Player player, Location loc) {
        int limit = plugin.getConfig()
            .getInt("blocklog.max-history-per-block", 25);
        java.util.List<BlockChange> history = database.getHistory(loc, limit);
        if (history.isEmpty()) {
            AdventureCompat.sendMessage(player,
                Component.text("No logged changes at this block.")
                    .color(NamedTextColor.YELLOW));
            return;
        }

        AdventureCompat.sendMessage(player,
            Component.text("----- Block History -----")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
        AdventureCompat.sendMessage(player,
            Component.text("Location: ").color(NamedTextColor.GRAY)
                .append(Component.text(loc.getBlockX() + ", "
                    + loc.getBlockY() + ", " + loc.getBlockZ()
                    + " (" + loc.getWorld().getName() + ")")
                    .color(NamedTextColor.WHITE)));

        int index = 0;
        LocalDateTime now = LocalDateTime.now();
        for (BlockChange change : history) {
            String who = change.isNatural()
                ? change.getPlayerName() : change.getPlayerName();
            String ago = formatAgo(change.getTimestamp(), now);

            Component line = Component.text("")
                .append(Component.text("#" + index + " ").color(NamedTextColor.DARK_GRAY))
                .append(actionComponent(change.getAction()))
                .append(Component.text(" by ").color(NamedTextColor.GRAY))
                .append(Component.text(who == null ? "Natural" : who)
                    .color(NamedTextColor.AQUA))
                .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text(change.getOldData().split(":")[0]
                    + " -> " + change.getNewData().split(":")[0])
                    .color(NamedTextColor.WHITE))
                .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text(ago).color(NamedTextColor.YELLOW));

            AdventureCompat.sendMessage(player, line);
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
