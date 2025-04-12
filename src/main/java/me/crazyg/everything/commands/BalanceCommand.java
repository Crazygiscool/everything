// filepath: vscode-vfs://github/Crazygiscool/everything/src/main/java/me/crazyg/everything/commands/BalanceCommand.java
package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BalanceCommand implements CommandExecutor {

    private final Economy econ;

    public BalanceCommand(Everything plugin) {
        this.econ = Everything.getEconomy();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can check their balance.");
            return true;
        }

        Player player = (Player) sender;
        double balance = econ.getBalance(player);
        player.sendMessage(ChatColor.GREEN + "Your balance is: " + ChatColor.GOLD + econ.format(balance));
        return true;
    }
}