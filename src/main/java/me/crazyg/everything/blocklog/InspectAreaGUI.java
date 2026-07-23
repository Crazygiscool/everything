package me.crazyg.everything.blocklog;

import me.crazyg.everything.gui.BaseGUI;
import me.crazyg.everything.utils.AdventureCompat;
import me.crazyg.everything.utils.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Settings GUI opened by right-clicking with the inspect wand. Lets the player
 * choose block log commands, adjust cube size (3-100) via left/right click, and confirm area.
 */
public class InspectAreaGUI extends BaseGUI {

    private static final int MIN_SIZE = 3;
    private static final int MAX_SIZE = 100;

    private final InspectWand wand;
    private final Location center;
    private int size;

    public InspectAreaGUI(Player player, InspectWand wand,
                           Location center, int initialSize) {
        super(player, 27,
            Component.text("Inspector Wand Menu")
                .color(NamedTextColor.AQUA)
                .decorate(TextDecoration.BOLD));
        this.wand = wand;
        this.center = center;
        this.size = Math.max(MIN_SIZE, Math.min(MAX_SIZE, initialSize));
    }

    @Override
    public void build() {
        // Block Log Commands (slots 0-3)
        ItemStack inspectCmd = ItemBuilder.builder(Material.COMPASS)
            .name("&b/inspect")
            .lore(
                "&7Toggle the inspect wand.",
                "",
                "&eClick to execute &b/inspect")
            .build();
        set(0, inspectCmd);

        ItemStack lookupCmd = ItemBuilder.builder(Material.BOOK)
            .name("&b/lookup")
            .lore(
                "&7Look up block changes near you.",
                "",
                "&eClick to execute &b/lookup here")
            .build();
        set(1, lookupCmd);

        ItemStack rollbackCmd = ItemBuilder.builder(Material.CLOCK)
            .name("&c/rollback")
            .lore(
                "&7Roll back block changes near you.",
                "",
                "&eClick to execute &c/rollback here")
            .build();
        set(2, rollbackCmd);

        ItemStack lbCmd = ItemBuilder.builder(Material.WRITABLE_BOOK)
            .name("&e/lb")
            .lore(
                "&7Block log utility (prune logs).",
                "",
                "&eClick to execute &e/lb")
            .build();
        set(3, lbCmd);

        // Area Center (slot 4)
        ItemStack centerInfo = ItemBuilder.builder(Material.PAPER)
            .name("&bArea Center")
            .lore(
                "&7World: &f" + center.getWorld().getName(),
                "&7X: &f" + center.getBlockX(),
                "&7Y: &f" + center.getBlockY(),
                "&7Z: &f" + center.getBlockZ(),
                "",
                "&7Cube is centered here.")
            .build();
        set(4, centerInfo);

        // Size item (slot 13) - adjusted via left/right click
        ItemStack sizeItem = ItemBuilder.builder(Material.SLIME_BALL)
            .name("&aCube Size: &f" + size + "x" + size + "x" + size)
            .lore(
                "&7Half-extent: &f" + (size / 2) + " blocks",
                "&7Range: &f" + MIN_SIZE + " - " + MAX_SIZE,
                "",
                "&eLeft-click: &fDecrease size by 1",
                "&eRight-click: &fIncrease size by 1")
            .build();
        set(13, sizeItem);

        ItemStack confirm = ItemBuilder.builder(Material.EMERALD_BLOCK)
            .name("&aConfirm Area")
            .lore(
                "&7Save this " + size + "x" + size + "x" + size + " cube",
                "&7then use /lookup here or /rollback here")
            .build();
        set(22, confirm);
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        int slot = e.getSlot();
        switch (slot) {
            case 0 -> {
                player.closeInventory();
                player.performCommand("inspect");
            }
            case 1 -> {
                player.closeInventory();
                player.performCommand("lookup here");
            }
            case 2 -> {
                player.closeInventory();
                player.performCommand("rollback here");
            }
            case 3 -> {
                player.closeInventory();
                player.performCommand("lb");
            }
            case 4 -> { /* display only */ }
            case 13 -> {
                if (e.isLeftClick()) {
                    size = Math.max(MIN_SIZE, size - 1);
                    playClickSound();
                    update();
                } else if (e.isRightClick()) {
                    size = Math.min(MAX_SIZE, size + 1);
                    playClickSound();
                    update();
                }
            }
            case 22 -> {
                wand.setArea(player.getUniqueId(), center, size);
                playSuccessSound();
                AdventureCompat.sendMessage(player,
                    Component.text("Inspect area set to ")
                        .color(NamedTextColor.GREEN)
                        .append(Component.text(size + "x" + size + "x" + size)
                            .color(NamedTextColor.AQUA))
                        .append(Component.text(" centered at ")
                            .color(NamedTextColor.GREEN))
                        .append(Component.text(center.getBlockX() + ", "
                            + center.getBlockY() + ", "
                            + center.getBlockZ())
                            .color(NamedTextColor.WHITE))
                        .append(Component.text(". Use ")
                            .color(NamedTextColor.GREEN))
                        .append(Component.text("/lookup here")
                            .color(NamedTextColor.AQUA))
                        .append(Component.text(" or ")
                            .color(NamedTextColor.GREEN))
                        .append(Component.text("/rollback here -y")
                            .color(NamedTextColor.AQUA))
                        .append(Component.text(".")
                            .color(NamedTextColor.GREEN)));
                close();
                return;
            }
            default -> { /* filler / ignored */ }
        }
    }
}
