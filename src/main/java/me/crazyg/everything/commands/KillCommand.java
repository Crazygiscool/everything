package me.crazyg.everything.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class KillCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player p) {
            if (args.length == 0) {
                p.setHealth(0);
                p.sendMessage(Component.text("You Have Opted To DIEEEE!")
                        .color(NamedTextColor.DARK_RED)
                        .decorate(TextDecoration.BOLD));
            } else {
                String selector = args[0];
                // Try to resolve selector (e.g. @p, @a, @r, @e, @s)
                try {
                    // Bukkit.selectEntities supports selectors for 1.13+
                    java.util.List<org.bukkit.entity.Entity> entities = Bukkit.selectEntities(sender, selector);
                    if (entities.isEmpty()) {
                        p.sendMessage(Component.text("No players/entities matched selector: " + selector)
                                .color(NamedTextColor.RED));
                        return true;
                    }
                    int killed = 0;
                    for (org.bukkit.entity.Entity entity : entities) {
                        if (entity instanceof Player target) {
                            target.setHealth(0);
                            target.sendMessage(Component.text("You have been killed by " + p.getName())
                                    .color(NamedTextColor.DARK_RED)
                                    .decorate(TextDecoration.BOLD));
                            killed++;
                        } else {
                            entity.remove(); // For non-player entities, just remove
                            killed++;
                        }
                    }
                    p.sendMessage(Component.text("Killed " + killed + " target(s) for selector: " + selector)
                            .color(NamedTextColor.DARK_RED)
                            .decorate(TextDecoration.BOLD));
                } catch (IllegalArgumentException ex) {
                    // Not a selector, try as player name
                    Player target = Bukkit.getServer().getPlayerExact(selector);
                    if (target == null) {
                        p.sendMessage(Component.text("This Player is not online")
                                .color(NamedTextColor.RED));
                    } else {
                        target.setHealth(0);
                        target.sendMessage(Component.text("You have been killed by " + p.getName())
                                .color(NamedTextColor.DARK_RED)
                                .decorate(TextDecoration.BOLD));
                        p.sendMessage(Component.text("Killed player: " + target.getName())
                                .color(NamedTextColor.DARK_RED)
                                .decorate(TextDecoration.BOLD));
                    }
                }
            }
        } else if (sender instanceof ConsoleCommandSender p) {
            p.sendMessage(Component.text("Command cannot be run by console, Silly")
                    .color(NamedTextColor.DARK_RED)
                    .decorate(TextDecoration.BOLD));
        } else if (sender instanceof BlockCommandSender p) {
            p.sendMessage(Component.text("Command cannot be run by command block, L")
                    .color(NamedTextColor.DARK_RED)
                    .decorate(TextDecoration.BOLD));
        }
        return true;
    }
}
