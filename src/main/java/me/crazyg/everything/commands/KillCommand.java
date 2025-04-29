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
                String playername = args[0];
                Player target = Bukkit.getServer().getPlayerExact(playername);

                if (target == null) {
                    p.sendMessage(Component.text("This Player is not online")
                            .color(NamedTextColor.RED));
                } else {
                    target.setHealth(0);
                    target.sendMessage(Component.text("You Have been Opted To DIE by " + playername)
                            .color(NamedTextColor.DARK_RED)
                            .decorate(TextDecoration.BOLD));
                    p.sendMessage(Component.text("BRO DIED, UAHAHAH")
                            .color(NamedTextColor.DARK_RED)
                            .decorate(TextDecoration.BOLD));
                }
            }
        } else if (sender instanceof ConsoleCommandSender p) {
            p.sendMessage(Component.text("Command Cannot be runned by console, Silly")
                    .color(NamedTextColor.DARK_RED)
                    .decorate(TextDecoration.BOLD));
        } else if (sender instanceof BlockCommandSender p) {
            p.sendMessage(Component.text("Command Cannot be runned by command block, L")
                    .color(NamedTextColor.DARK_RED)
                    .decorate(TextDecoration.BOLD));
        }

        return true;
    }
}
