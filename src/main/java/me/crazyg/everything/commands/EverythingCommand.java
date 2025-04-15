package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class EverythingCommand implements CommandExecutor {

    private final Everything plugin;

    public EverythingCommand(Everything plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Display help/info page if no sub-command is provided
            sender.sendMessage(Component.text("Everything Plugin - Help").color(NamedTextColor.GOLD));
            sender.sendMessage(Component.text()
                .append(Component.text("/everything reload").color(NamedTextColor.YELLOW))
                .append(Component.text(" - Reload the plugin configuration.").color(NamedTextColor.WHITE)));
            sender.sendMessage(Component.text()
                .append(Component.text("/everything info").color(NamedTextColor.YELLOW))
                .append(Component.text(" - Display plugin information.").color(NamedTextColor.WHITE)));
            sender.sendMessage(Component.text()
                .append(Component.text("/gmc").color(NamedTextColor.YELLOW))
                .append(Component.text(" - Changes your gamemode to creative mode").color(NamedTextColor.WHITE)));
            sender.sendMessage(Component.text()
                .append(Component.text("/gms").color(NamedTextColor.YELLOW))
                .append(Component.text(" - Changes your gamemode to survival mode").color(NamedTextColor.WHITE)));
            sender.sendMessage(Component.text()
                .append(Component.text("/gmsp").color(NamedTextColor.YELLOW))
                .append(Component.text(" - Changes your gamemode to spectator mode").color(NamedTextColor.WHITE)));
            sender.sendMessage(Component.text()
                .append(Component.text("/gma").color(NamedTextColor.YELLOW))
                .append(Component.text(" - Changes your gamemode to adventure mode").color(NamedTextColor.WHITE)));
            sender.sendMessage(Component.text()
                .append(Component.text("/setspawn").color(NamedTextColor.YELLOW))
                .append(Component.text(" - Set the spawn point for the world.").color(NamedTextColor.WHITE)));
            sender.sendMessage(Component.text()
                .append(Component.text("/spawn").color(NamedTextColor.YELLOW))
                .append(Component.text(" - Teleport to the spawn point.").color(NamedTextColor.WHITE)));
            sender.sendMessage(Component.text()
                .append(Component.text("/report").color(NamedTextColor.YELLOW))
                .append(Component.text(" - Report a player to the server staff.").color(NamedTextColor.WHITE)));
            sender.sendMessage(Component.text()
                .append(Component.text("/god").color(NamedTextColor.YELLOW))
                .append(Component.text(" - Toggle god mode on/off.").color(NamedTextColor.WHITE)));
            return true;
        }

        // Handle sub-commands
        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("everything.reload")) {
                    sender.sendMessage(Component.text("You do not have permission to use this command.")
                        .color(NamedTextColor.RED));
                    return true;
                }
                plugin.reloadConfig();
                sender.sendMessage(Component.text("Plugin configuration reloaded successfully!")
                    .color(NamedTextColor.GREEN));
                return true;

            case "info":
                sender.sendMessage(Component.text("Everything Plugin - Info")
                    .color(NamedTextColor.GOLD));
                sender.sendMessage(Component.text()
                    .append(Component.text("Version: ").color(NamedTextColor.YELLOW))
                    .append(Component.text(plugin.getPluginMeta().getVersion()).color(NamedTextColor.WHITE)));
                sender.sendMessage(Component.text()
                    .append(Component.text("Author: ").color(NamedTextColor.YELLOW))
                    .append(Component.text(String.join(", ", plugin.getPluginMeta().getAuthors())).color(NamedTextColor.WHITE)));
                sender.sendMessage(Component.text()
                    .append(Component.text("Description: ").color(NamedTextColor.YELLOW))
                    .append(Component.text(plugin.getPluginMeta().getDescription()).color(NamedTextColor.WHITE)));
                return true;

            default:
                sender.sendMessage(Component.text("Unknown sub-command. Use /everything for help.")
                    .color(NamedTextColor.RED));
                return true;
        }
    }
}