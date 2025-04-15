package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import org.bukkit.ChatColor;
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
            sender.sendMessage(ChatColor.GOLD + "Everything Plugin - Help");
            sender.sendMessage(ChatColor.YELLOW + "/everything reload" + ChatColor.WHITE + " - Reload the plugin configuration.");
            sender.sendMessage(ChatColor.YELLOW + "/everything info" + ChatColor.WHITE + " - Display plugin information.");
            sender.sendMessage(ChatColor.YELLOW + "/gmc" + ChatColor.WHITE + " - Changes your gamemode to creative mode");
            sender.sendMessage(ChatColor.YELLOW + "/gms" + ChatColor.WHITE + " - Changes your gamemode to survival mode");
            sender.sendMessage(ChatColor.YELLOW + "/gmsp" + ChatColor.WHITE + " - Changes your gamemode to spectator mode");
            sender.sendMessage(ChatColor.YELLOW + "/gma" + ChatColor.WHITE + " - Changes your gamemode to adventure mode");
            sender.sendMessage(ChatColor.YELLOW + "/setspawn" + ChatColor.WHITE + " - Set the spawn point for the world.");
            sender.sendMessage(ChatColor.YELLOW + "/spawn" + ChatColor.WHITE + " - Teleport to the spawn point.");
            sender.sendMessage(ChatColor.YELLOW + "/report" + ChatColor.WHITE + " - Report a player to the server staff.");
            sender.sendMessage(ChatColor.YELLOW + "/god" + ChatColor.WHITE + " - Toggle god mode on/off.");
            return true;
        }

        // Handle sub-commands
        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("everything.reload")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }
                plugin.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "Plugin configuration reloaded successfully!");
                return true;

            case "info":
                sender.sendMessage(ChatColor.GOLD + "Everything Plugin - Info");
                sender.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
                sender.sendMessage(ChatColor.YELLOW + "Author: " + ChatColor.WHITE + String.join(", ", plugin.getDescription().getAuthors()));
                sender.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.WHITE + plugin.getDescription().getDescription());
                return true;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown sub-command. Use /everything for help.");
                return true;
        }
    }
}