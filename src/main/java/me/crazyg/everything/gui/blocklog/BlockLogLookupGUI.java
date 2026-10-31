package me.crazyg.everything.gui.blocklog;

import me.crazyg.everything.Everything;
import me.crazyg.everything.blocklog.BlockChange;
import me.crazyg.everything.gui.BaseGUI;
import me.crazyg.everything.utils.AdventureCompat;
import me.crazyg.everything.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class BlockLogLookupGUI extends BaseGUI {

    private static final int PAGE_SIZE = 25;

    private final Everything plugin;
    private final List<BlockChange> changes;
    private final String scope;
    private int page;

    public BlockLogLookupGUI(Player player, Everything plugin,
                             List<BlockChange> changes, String scope, int page) {
        super(player, 45,
            Component.text("Block Log Lookup").color(NamedTextColor.GOLD));
        this.plugin = plugin;
        this.changes = changes == null ? List.of() : changes;
        this.scope = scope == null || scope.isBlank() ? "near you" : scope;
        this.page = clampPage(page);
    }

    @Override
    public void build() {
        getInventory().clear();
        int totalPages = totalPages();

        if (changes.isEmpty()) {
            set(4, ItemBuilder.builder(Material.PAPER)
                .name("&6No matching logs")
                .lore("&7" + scope, "&7No block changes were found.").build());
        } else {
            int start = (page - 1) * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, changes.size());
            DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("MM-dd HH:mm");
            for (int slot = 0; slot < PAGE_SIZE; slot++) {
                int index = start + slot;
                if (index >= changes.size()) {
                    break;
                }
                BlockChange change = changes.get(index);
                String who = change.getPlayerName() == null
                    ? "Natural" : change.getPlayerName();
                Material material;
                try {
                    material = Material.matchMaterial(change.getNewMaterial());
                } catch (Exception ignored) {
                    material = null;
                }
                if (material == null) {
                    material = Material.PAPER;
                }

                ItemStack item = ItemBuilder.builder(material)
                    .name("&b#" + (index + 1) + " &8" + who)
                    .lore(
                        "&7Action: &f" + change.getAction().name(),
                        "&7Blocks: &f" + change.getOldMaterial() + " -> " + change.getNewMaterial(),
                        "&7Location: &f" + change.getX() + ", " + change.getY() + ", " + change.getZ(),
                        "&7Time: &f" + change.getTimestamp().format(formatter),
                        "&7Click: &aTeleport"
                    )
                    .build();
                set(slot, item);
            }
        }

        if (page > 1) {
            set(27, ItemBuilder.builder(Material.ARROW)
                .name("&aPrevious Page")
                .lore("&7Page " + (page - 1) + " of " + totalPages).build());
        }
        set(31, ItemBuilder.builder(Material.PAPER)
            .name("&6Page " + page + "/" + totalPages)
            .lore("&7Scope: " + scope,
                  "&7Found: " + changes.size()).build());
        if (page < totalPages) {
            set(35, ItemBuilder.builder(Material.ARROW)
                .name("&aNext Page")
                .lore("&7Page " + (page + 1) + " of " + totalPages).build());
        }
        set(40, ItemBuilder.builder(Material.BARRIER)
            .name("&cClose")
            .build());
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        int slot = e.getSlot();
        if (slot == 27 && page > 1) {
            setPage(page - 1);
            return;
        }
        if (slot == 35) {
            int totalPages = totalPages();
            if (page < totalPages) {
                setPage(page + 1);
            }
            return;
        }
        if (slot == 40) {
            close();
            return;
        }

        int start = (page - 1) * PAGE_SIZE;
        int index = start + slot;
        if (slot < PAGE_SIZE && index >= 0 && index < changes.size()) {
            teleport(changes.get(index));
        }
    }

    public void setPage(int page) {
        this.page = clampPage(page);
        update();
    }

    private int totalPages() {
        if (changes.isEmpty()) {
            return 1;
        }
        return (changes.size() + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    private int clampPage(int page) {
        int total = Math.max(1, totalPages());
        return Math.max(1, Math.min(page, total));
    }

    private void teleport(BlockChange change) {
        Player player = getPlayer();
        try {
            World world = org.bukkit.Bukkit.getWorld(change.getWorld());
            if (world == null) {
                throw new IllegalStateException("World unavailable");
            }
            org.bukkit.Location loc = new org.bukkit.Location(
                world,
                change.getX() + 0.5,
                change.getY() + 1.0,
                change.getZ() + 0.5
            );
            plugin.getTeleportManager().teleport(player, loc, "lookup");
            AdventureCompat.sendMessage(player, Component.text("Teleporting to logged block...")
                .color(NamedTextColor.AQUA));
        } catch (Exception ex) {
            AdventureCompat.sendMessage(player,
                Component.text("Could not teleport to that location: "
                    + ex.getMessage())
                    .color(NamedTextColor.RED));
        }
    }

    private Player getPlayer() {
        return player;
    }
}
