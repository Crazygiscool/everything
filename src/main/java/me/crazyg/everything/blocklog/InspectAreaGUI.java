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
 * choose a uniform cube size (3-100) centered on the clicked block. Configure-
 * only: the selection is stored on the player and consumed by /lookup here and
 * /rollback here.
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
            Component.text("Inspect Area Settings")
                .color(NamedTextColor.AQUA)
                .decorate(TextDecoration.BOLD));
        this.wand = wand;
        this.center = center;
        this.size = Math.max(MIN_SIZE, Math.min(MAX_SIZE, initialSize));
    }

    @Override
    public void build() {
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

        ItemStack sizeItem = ItemBuilder.builder(Material.SLIME_BALL)
            .name("&aCube Size: &f" + size + "x" + size + "x" + size)
            .lore(
                "&7Half-extent: &f" + (size / 2) + " blocks",
                "&7Range: &f" + MIN_SIZE + " - " + MAX_SIZE)
            .build();
        set(13, sizeItem);

        ItemStack minus = ItemBuilder.builder(Material.RED_STAINED_GLASS_PANE)
            .name("&c-1")
            .lore("&7Decrease cube size (min " + MIN_SIZE + ")")
            .build();
        set(11, minus);

        ItemStack plus = ItemBuilder.builder(Material.GREEN_STAINED_GLASS_PANE)
            .name("&a+5")
            .lore("&7Increase cube size (max " + MAX_SIZE + ")")
            .build();
        set(15, plus);

        ItemStack finePlus = ItemBuilder.builder(Material.LIME_STAINED_GLASS_PANE)
            .name("&a+1")
            .lore("&7Fine increase by 1")
            .build();
        set(16, finePlus);

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
            case 11 -> size = Math.max(MIN_SIZE, size - 1);
            case 15 -> size = Math.min(MAX_SIZE, size + 5);
            case 16 -> size = Math.min(MAX_SIZE, size + 1);
            case 13 -> { /* display only */ }
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
        // Refresh size display for size-changing clicks.
        if (slot == 11 || slot == 15 || slot == 16) {
            update();
        }
    }
}
