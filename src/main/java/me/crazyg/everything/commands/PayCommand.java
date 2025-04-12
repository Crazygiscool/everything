// filepath: vscode-vfs://github/Crazygiscool/everything/src/main/java/me/crazyg/everything/commands/PayCommand.java
package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PayCommand implements CommandExecutor {

    private final Economy econ;

    public PayCommand(Everything plugin) {
        this.econ = Everything.getEconomy();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can send money.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /pay <player> <amount>");
            return true;
        }

        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "Player not found or not online.");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount.");
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Amount must be greater than zero.");
            return true;
        }

        if (econ.getBalance(player) < amount) {
            player.sendMessage(ChatColor.RED + "You don't have enough money.");
            return true;
        }

        econ.withdrawPlayer(player, amount);
        econ.depositPlayer(target, amount);

        player.sendMessage(ChatColor.GREEN + "You sent " + ChatColor.GOLD + econ.format(amount) + ChatColor.GREEN + " to " + target.getName() + ".");
        target.sendMessage(ChatColor.GREEN + "You received " + ChatColor.GOLD + econ.format(amount) + ChatColor.GREEN + " from " + player.getName() + ".");
        return true;
    }
}