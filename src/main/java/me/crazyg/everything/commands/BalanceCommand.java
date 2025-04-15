package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BalanceCommand implements CommandExecutor {
    private final Everything plugin;

    public BalanceCommand(Everything plugin) {
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

        Player player = (Player) sender;
        double balance = Everything.getEconomy().getBalance(player);
        player.sendMessage(ChatColor.GREEN + "Your balance: " + ChatColor.GOLD + Everything.getEconomy().format(balance));
        return true;
    }
}