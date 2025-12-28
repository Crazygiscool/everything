package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            sender.sendMessage(Component.text("Economy features are currently disabled! Check if you have a economy plugin installed.", NamedTextColor.RED));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        double balance = Everything.getEconomy().getBalance(player);
        player.sendMessage(Component.text("Your balance: ", NamedTextColor.GREEN)
                .append(Component.text(Everything.getEconomy().format(balance), NamedTextColor.GOLD)));
        return true;
    }
}