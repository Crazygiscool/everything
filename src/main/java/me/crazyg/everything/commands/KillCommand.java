package me.crazyg.everything.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Stream;

public class KillCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player p) {

            // /kill
            if (args.length == 0) {
                p.setHealth(0);
                me.crazyg.everything.Everything.sendFancy(
                        p,
                        Component.text("You Have Opted To DIEEEE!")
                                .color(NamedTextColor.DARK_RED)
                                .decorate(TextDecoration.BOLD)
                );
                return true;
            }

            // /kill <selector|player>
            String selector = args[0];

            try {
                var entities = Bukkit.selectEntities(sender, selector);

                if (entities.isEmpty()) {
                    me.crazyg.everything.Everything.sendFancy(
                            p,
                            Component.text("No players/entities matched selector: " + selector)
                                    .color(NamedTextColor.RED)
                    );
                    return true;
                }

                int killed = 0;

                for (var entity : entities) {
                    if (entity instanceof Player target) {
                        target.setHealth(0);
                        me.crazyg.everything.Everything.sendFancy(
                                target,
                                Component.text("You have been killed by " + p.getName())
                                        .color(NamedTextColor.DARK_RED)
                                        .decorate(TextDecoration.BOLD)
                        );
                        killed++;
                    } else {
                        entity.remove();
                        killed++;
                    }
                }

                me.crazyg.everything.Everything.sendFancy(
                        p,
                        Component.text("Killed " + killed + " target(s) for selector: " + selector)
                                .color(NamedTextColor.DARK_RED)
                                .decorate(TextDecoration.BOLD)
                );

            } catch (IllegalArgumentException ex) {

                Player target = Bukkit.getPlayer(selector);

                if (target == null) {
                    me.crazyg.everything.Everything.sendFancy(
                            p,
                            Component.text("This Player is not online")
                                    .color(NamedTextColor.RED)
                    );
                } else {
                    target.setHealth(0);
                    me.crazyg.everything.Everything.sendFancy(
                            target,
                            Component.text("You have been killed by " + p.getName())
                                    .color(NamedTextColor.DARK_RED)
                                    .decorate(TextDecoration.BOLD)
                    );
                    me.crazyg.everything.Everything.sendFancy(
                            p,
                            Component.text("Killed player: " + target.getName())
                                    .color(NamedTextColor.DARK_RED)
                                    .decorate(TextDecoration.BOLD)
                    );
                }
            }

        } else if (sender instanceof ConsoleCommandSender p) {
            me.crazyg.everything.Everything.sendFancy(
                    p,
                    Component.text("Command cannot be run by console, Silly")
                            .color(NamedTextColor.DARK_RED)
                            .decorate(TextDecoration.BOLD)
            );

        } else if (sender instanceof BlockCommandSender p) {
            me.crazyg.everything.Everything.sendFancy(
                    p,
                    Component.text("Command cannot be run by command block, L")
                            .color(NamedTextColor.DARK_RED)
                            .decorate(TextDecoration.BOLD)
            );
        }

        return true;
    }

    // ----------------------------------------------------
    // TAB COMPLETION
    // ----------------------------------------------------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        // /kill <arg>
        if (args.length == 1) {

            String input = args[0].toLowerCase();

            // Suggest selectors + online players
            List<String> base = List.of("@p", "@a", "@r", "@e", "@s");

            List<String> players = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .toList();

            List<String> selectors = base.stream()
                    .filter(s -> s.startsWith(input))
                    .toList();

            return Stream.concat(selectors.stream(), players.stream()).toList();
        }

        return List.of();
    }
}