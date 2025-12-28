package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class EcoCommand implements CommandExecutor, TabCompleter {

    private final Everything plugin;
    private final Economy econ;

    public EcoCommand(Everything plugin) {
        this.plugin = plugin;
        this.econ = Everything.getEconomy();
    }

    // ----------------------------------------------------
    // COMMAND EXECUTION
    // ----------------------------------------------------
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("everything.eco")) {
            sender.sendMessage(
                Everything.PLUGIN_PREFIX.append(
                    Component.text("You do not have permission to use this command.")
                        .color(NamedTextColor.RED)
                )
            );
            return true;
        }

        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String action = args[0].toLowerCase();
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        if (target == null) {
            sender.sendMessage(
                Everything.PLUGIN_PREFIX.append(
                    Component.text("Player not found.").color(NamedTextColor.RED)
                )
            );
            return true;
        }

        switch (action) {
            case "reset":
                econ.withdrawPlayer(target, econ.getBalance(target));
                sender.sendMessage(
                    Everything.PLUGIN_PREFIX.append(
                        Component.text("Reset balance for ").color(NamedTextColor.GREEN)
                            .append(Component.text(target.getName()).color(NamedTextColor.AQUA))
                    )
                );
                return true;

            case "give":
            case "take":
            case "set":
                if (args.length < 3) {
                    sendUsage(sender);
                    return true;
                }

                double amount;
                try {
                    amount = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(
                        Everything.PLUGIN_PREFIX.append(
                            Component.text("Invalid amount.").color(NamedTextColor.RED)
                        )
                    );
                    return true;
                }

                if (amount < 0) {
                    sender.sendMessage(
                        Everything.PLUGIN_PREFIX.append(
                            Component.text("Amount cannot be negative.").color(NamedTextColor.RED)
                        )
                    );
                    return true;
                }

                return handleMoneyAction(sender, target, action, amount);

            default:
                sendUsage(sender);
                return true;
        }
    }

    private boolean handleMoneyAction(CommandSender sender, OfflinePlayer target, String action, double amount) {
        switch (action) {
            case "give":
                econ.depositPlayer(target, amount);
                sender.sendMessage(
                    Everything.PLUGIN_PREFIX.append(
                        Component.text("Gave ").color(NamedTextColor.GREEN)
                            .append(Component.text(amount).color(NamedTextColor.YELLOW))
                            .append(Component.text(" to ").color(NamedTextColor.GREEN))
                            .append(Component.text(target.getName()).color(NamedTextColor.AQUA))
                    )
                );
                return true;

            case "take":
                econ.withdrawPlayer(target, amount);
                sender.sendMessage(
                    Everything.PLUGIN_PREFIX.append(
                        Component.text("Took ").color(NamedTextColor.RED)
                            .append(Component.text(amount).color(NamedTextColor.YELLOW))
                            .append(Component.text(" from ").color(NamedTextColor.RED))
                            .append(Component.text(target.getName()).color(NamedTextColor.AQUA))
                    )
                );
                return true;

            case "set":
                double current = econ.getBalance(target);
                if (current > amount) {
                    econ.withdrawPlayer(target, current - amount);
                } else {
                    econ.depositPlayer(target, amount - current);
                }

                sender.sendMessage(
                    Everything.PLUGIN_PREFIX.append(
                        Component.text("Set ").color(NamedTextColor.GREEN)
                            .append(Component.text(target.getName()).color(NamedTextColor.AQUA))
                            .append(Component.text("'s balance to ").color(NamedTextColor.GREEN))
                            .append(Component.text(amount).color(NamedTextColor.YELLOW))
                    )
                );
                return true;
        }
        return false;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(
            Everything.PLUGIN_PREFIX.append(
                Component.text("Usage: /eco <give|take|set|reset> <player> [amount]")
                    .color(NamedTextColor.YELLOW)
            )
        );
    }

    // ----------------------------------------------------
    // TAB COMPLETION
    // ----------------------------------------------------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (!sender.hasPermission("everything.eco")) return List.of();

        // /eco <action>
        if (args.length == 1) {
            return List.of("give", "take", "set", "reset")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        // /eco give <player>
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(p -> p.getName())
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        // /eco give player <amount>
        if (args.length == 3 && !args[0].equalsIgnoreCase("reset")) {
            return List.of("1", "10", "100", "1000")
                    .stream()
                    .filter(s -> s.startsWith(args[2]))
                    .toList();
        }

        return List.of();
    }
}
