package me.crazyg.everything.commands;

import java.util.Arrays;
import java.util.List;
import me.crazyg.everything.Everything;
import me.crazyg.everything.kits.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class KitCommand implements CommandExecutor {
    private final Everything plugin;

    public KitCommand(Everything plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            showAvailableKits(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (!player.hasPermission("everything.kit.create")) {
                    player.sendMessage(Component.text("You don't have permission to create kits!")
                            .color(NamedTextColor.RED));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(Component.text("Usage: /kit create <name> <cooldown>")
                            .color(NamedTextColor.RED));
                    return true;
                }
                return createKit(player, args[1], args[2]);

            case "delete":
                if (!player.hasPermission("everything.kit.delete")) {
                    player.sendMessage(Component.text("You don't have permission to delete kits!")
                            .color(NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /kit delete <name>")
                            .color(NamedTextColor.RED));
                    return true;
                }
                return deleteKit(player, args[1]);

            default:
                return giveKit(player, args[0]);
        }
    }

    private boolean createKit(Player player, String name, String cooldownStr) {
        long cooldown;
        try {
            cooldown = Long.parseLong(cooldownStr);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Cooldown must be a number!")
                    .color(NamedTextColor.RED));
            return true;
        }

        List<ItemStack> items = Arrays.asList(player.getInventory().getContents());
        if (plugin.getKitManager().createKit(name, items, cooldown)) {
            player.sendMessage(Component.text("Kit '" + name + "' created successfully!")
                    .color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("A kit with that name already exists!")
                    .color(NamedTextColor.RED));
        }
        return true;
    }

    private boolean deleteKit(Player player, String name) {
        if (plugin.getKitManager().deleteKit(name)) {
            player.sendMessage(Component.text("Kit '" + name + "' deleted successfully!")
                    .color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Kit '" + name + "' does not exist!")
                    .color(NamedTextColor.RED));
        }
        return true;
    }

    private boolean giveKit(Player player, String name) {
        Kit kit = plugin.getKitManager().getKit(name);
        if (kit == null) {
            player.sendMessage(Component.text("Kit '" + name + "' does not exist!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission(kit.getPermission())) {
            player.sendMessage(Component.text("You don't have permission to use this kit!")
                    .color(NamedTextColor.RED));
            return true;
        }

        long remaining = plugin.getKitManager().getRemainingCooldown(player, name);
        if (remaining > 0) {
            player.sendMessage(Component.text("You must wait " + remaining + " seconds before using this kit again!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (plugin.getKitManager().giveKit(player, name)) {
            player.sendMessage(Component.text("Kit '" + name + "' received!")
                    .color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to give kit!")
                    .color(NamedTextColor.RED));
        }
        return true;
    }

    private void showAvailableKits(Player player) {
        player.sendMessage(Component.text("Available Kits:")
                .color(NamedTextColor.YELLOW));
        
        for (Kit kit : plugin.getKitManager().getKits().values()) {
            if (player.hasPermission(kit.getPermission())) {
                long cooldown = plugin.getKitManager().getRemainingCooldown(player, kit.getName());
                Component message = Component.text()
                    .append(Component.text("- " + kit.getName()).color(NamedTextColor.GREEN))
                    .append(Component.text(cooldown > 0 ? " (Cooldown: " + cooldown + "s)" : "")
                            .color(NamedTextColor.GRAY))
                    .build();
                player.sendMessage(message);
            }
        }
    }
}