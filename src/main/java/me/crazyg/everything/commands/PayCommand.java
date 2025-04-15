package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PayCommand implements CommandExecutor {
    private final Everything plugin;

    public PayCommand(Everything plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.isEconomyEnabled()) {
            sender.sendMessage(ChatColor.RED + "Economy features are currently disabled.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /pay <player> <amount>");
            return true;
        }

        Player player = (Player) sender;
        Player target = Bukkit.getPlayerExact(args[0]);
        
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount!");
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Amount must be positive!");
            return true;
        }

        if (!Everything.getEconomy().has(player, amount)) {
            player.sendMessage(ChatColor.RED + "You don't have enough money!");
            return true;
        }

        Everything.getEconomy().withdrawPlayer(player, amount);
        Everything.getEconomy().depositPlayer(target, amount);

        player.sendMessage(ChatColor.GREEN + "You paid " + ChatColor.GOLD + Everything.getEconomy().format(amount) + 
                         ChatColor.GREEN + " to " + target.getName());
        target.sendMessage(ChatColor.GREEN + "You received " + ChatColor.GOLD + Everything.getEconomy().format(amount) + 
                         ChatColor.GREEN + " from " + player.getName());
        return true;
    }
}