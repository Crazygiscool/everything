package me.crazyg.everything.commands;

import java.util.HashMap;
import java.util.UUID;
import me.crazyg.everything.Everything;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class MessageCommand implements CommandExecutor {
    private final Everything plugin;
    private static final HashMap<UUID, UUID> lastMessage = new HashMap<>();

    public MessageCommand(Everything plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("msg")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /msg <player> <message>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }

            StringBuilder message = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                message.append(args[i]).append(" ");
            }

            player.sendMessage(ChatColor.GRAY + "To " + target.getName() + ": " + ChatColor.WHITE + message);
            target.sendMessage(ChatColor.GRAY + "From " + player.getName() + ": " + ChatColor.WHITE + message);

            lastMessage.put(target.getUniqueId(), player.getUniqueId());
            return true;
        }

        if (command.getName().equalsIgnoreCase("reply") || command.getName().equalsIgnoreCase("r")) {
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Usage: /reply <message>");
                return true;
            }

            UUID lastUUID = lastMessage.get(player.getUniqueId());
            if (lastUUID == null) {
                player.sendMessage(ChatColor.RED + "Nobody to reply to!");
                return true;
            }

            Player target = Bukkit.getPlayer(lastUUID);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player is offline!");
                return true;
            }

            StringBuilder message = new StringBuilder();
            for (String arg : args) {
                message.append(arg).append(" ");
            }

            player.sendMessage(ChatColor.GRAY + "To " + target.getName() + ": " + ChatColor.WHITE + message);
            target.sendMessage(ChatColor.GRAY + "From " + player.getName() + ": " + ChatColor.WHITE + message);

            lastMessage.put(target.getUniqueId(), player.getUniqueId());
            return true;
        }

        return false;
    }
}