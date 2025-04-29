package me.crazyg.everything.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeleportCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("everything.teleport")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        // Check if it's a player teleport or coordinate teleport
        if (args.length == 1) {
            // Player teleport
            Player target = player.getServer().getPlayer(args[0]);
            if (target == null) {
                player.sendMessage("§cPlayer not found!");
                return true;
            }

            // Check if player is trying to teleport themselves or has permission to teleport others
            if (!player.getName().equals(target.getName()) && !player.hasPermission("everything.teleport.other")) {
                player.sendMessage("§cYou don't have permission to teleport other players!");
                return true;
            }

            player.teleport(target);
            player.sendMessage("§aTeleported to " + target.getName());
            return true;
        } 
        // Coordinate teleport
        else if (args.length == 4) {
            Player target = player.getServer().getPlayer(args[0]);
            if (target == null) {
                player.sendMessage("§cPlayer not found!");
                return true;
            }

            // Check if player is trying to teleport themselves or has permission to teleport others
            if (!player.getName().equals(target.getName()) && !player.hasPermission("everything.teleport.other")) {
                player.sendMessage("§cYou don't have permission to teleport other players!");
                return true;
            }

            try {
                double x = Double.parseDouble(args[1]);
                double y = Double.parseDouble(args[2]);
                double z = Double.parseDouble(args[3]);

                Location location = new Location(target.getWorld(), x, y, z);
                target.teleport(location);
                player.sendMessage("§aTeleported " + target.getName() + " to X: " + x + " Y: " + y + " Z: " + z);
                return true;
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid coordinates! Please use numbers.");
                return true;
            }
        } else {
            player.sendMessage("§cUsage: /tp <player> OR /tp <player> <x> <y> <z>");
            return true;
        }
    }
}