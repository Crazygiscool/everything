package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TPCommand implements CommandExecutor {
    private final Everything plugin;

    public TPCommand(Everything plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length == 1) {
            // /tp <player>
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null || !target.isOnline()) {
                p.sendMessage(Component.text("Player not found or not online.").color(NamedTextColor.RED));
                return true;
            }
            if (target.equals(p)) {
                p.sendMessage(Component.text("You cannot teleport to yourself.").color(NamedTextColor.RED));
                return true;
            }
            p.teleport(target.getLocation());
            p.sendMessage(Component.text("Teleported to ").color(NamedTextColor.GREEN)
                    .append(Component.text(target.getName()).color(NamedTextColor.YELLOW)));
            return true;
        } else if (args.length == 2) {
            // /tp <player1> <player2>
            Player player1 = Bukkit.getPlayerExact(args[0]);
            Player player2 = Bukkit.getPlayerExact(args[1]);
            if (player1 == null || !player1.isOnline() || player2 == null || !player2.isOnline()) {
                p.sendMessage(Component.text("Player not found or not online.").color(NamedTextColor.RED));
                return true;
            }
            player1.teleport(player2.getLocation());
            player1.sendMessage(Component.text("Teleported to ").color(NamedTextColor.GREEN)
                    .append(Component.text(player2.getName()).color(NamedTextColor.YELLOW)));
            p.sendMessage(Component.text("Teleported ").color(NamedTextColor.GREEN)
                    .append(Component.text(player1.getName()).color(NamedTextColor.YELLOW))
                    .append(Component.text(" to ").color(NamedTextColor.GREEN))
                    .append(Component.text(player2.getName()).color(NamedTextColor.YELLOW)));
            return true;
        } else if (args.length == 3) {
            // /tp <x> <y> <z> (self)
            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);
                p.teleport(new Location(p.getWorld(), x, y, z, p.getLocation().getYaw(), p.getLocation().getPitch()));
                p.sendMessage(Component.text("Teleported to coordinates.").color(NamedTextColor.GREEN));
            } catch (NumberFormatException e) {
                p.sendMessage(Component.text("Invalid coordinates.").color(NamedTextColor.RED));
            }
            return true;
        } else if (args.length == 4) {
            // /tp <player> <x> <y> <z>
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null || !target.isOnline()) {
                p.sendMessage(Component.text("Player not found or not online.").color(NamedTextColor.RED));
                return true;
            }
            try {
                double x = Double.parseDouble(args[1]);
                double y = Double.parseDouble(args[2]);
                double z = Double.parseDouble(args[3]);
                target.teleport(new Location(target.getWorld(), x, y, z, target.getLocation().getYaw(), target.getLocation().getPitch()));
                target.sendMessage(Component.text("Teleported to coordinates.").color(NamedTextColor.GREEN));
                p.sendMessage(Component.text("Teleported ").color(NamedTextColor.GREEN)
                        .append(Component.text(target.getName()).color(NamedTextColor.YELLOW))
                        .append(Component.text(" to coordinates.").color(NamedTextColor.GREEN)));
            } catch (NumberFormatException e) {
                p.sendMessage(Component.text("Invalid coordinates.").color(NamedTextColor.RED));
            }
            return true;
        } else {
            p.sendMessage(Component.text("Usage: /tp <player> | /tp <player1> <player2> | /tp <x> <y> <z> | /tp <player> <x> <y> <z>").color(NamedTextColor.YELLOW));
            return true;
        }
    }
}
