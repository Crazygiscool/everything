package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

public class PayCommand implements CommandExecutor, TabCompleter {

    private final Everything plugin;

    public PayCommand(Everything plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!plugin.isEconomyEnabled()) {
            sender.sendMessage(Component.text("Economy features are currently disabled.")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /pay <player> <amount>")
                    .color(NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);

        if (target == null) {
            player.sendMessage(Component.text("Player not found!")
                    .color(NamedTextColor.RED));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid amount!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(Component.text("Amount must be positive!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (!Everything.getEconomy().has(player, amount)) {
            player.sendMessage(Component.text("You don't have enough money!")
                    .color(NamedTextColor.RED));
            return true;
        }

        Everything.getEconomy().withdrawPlayer(player, amount);
        Everything.getEconomy().depositPlayer(target, amount);

        player.sendMessage(Component.text()
                .append(Component.text("You paid ").color(NamedTextColor.GREEN))
                .append(Component.text(Everything.getEconomy().format(amount)).color(NamedTextColor.GOLD))
                .append(Component.text(" to ").color(NamedTextColor.GREEN))
                .append(Component.text(target.getName()).color(NamedTextColor.GREEN))
                .build());

        target.sendMessage(Component.text()
                .append(Component.text("You received ").color(NamedTextColor.GREEN))
                .append(Component.text(Everything.getEconomy().format(amount)).color(NamedTextColor.GOLD))
                .append(Component.text(" from ").color(NamedTextColor.GREEN))
                .append(Component.text(player.getName()).color(NamedTextColor.GREEN))
                .build());

        return true;
    }

    // ----------------------------------------------------
    // TAB COMPLETION
    // ----------------------------------------------------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        // /pay <player>
        if (args.length == 1) {
            String input = args[0].toLowerCase();

            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .toList();
        }

        // /pay player <amount>
        if (args.length == 2) {
            return List.of("1", "10", "50", "100", "500", "1000")
                    .stream()
                    .filter(s -> s.startsWith(args[1]))
                    .toList();
        }

        return List.of();
    }
}
