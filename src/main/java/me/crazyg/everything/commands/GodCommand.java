package me.crazyg.everything.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class GodCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player p) {
            if (args.length == 0) {
                if (p.isInvulnerable()) {
                    p.setInvulnerable(false);
                    p.sendMessage(Component.text("GOD MODE disabled")
                            .color(NamedTextColor.DARK_RED));
                } else {
                    p.setInvulnerable(true);
                    p.sendMessage(Component.text("GOD MODE Enabled")
                            .color(NamedTextColor.GOLD));
                }
            } else {
                String playername = args[0];
                Player target = Bukkit.getServer().getPlayerExact(playername);

                if (target == null) {
                    p.sendMessage(Component.text("This Player is not online")
                            .color(NamedTextColor.RED));
                } else {
                    if (p.isInvulnerable()) {
                        target.setInvulnerable(false);
                        target.sendMessage(Component.text()
                                .append(Component.text("GOD MODE disabled by ").color(NamedTextColor.DARK_RED))
                                .append(Component.text(p.displayName().toString()).color(NamedTextColor.DARK_RED)));
                        p.sendMessage(Component.text("MADE HIM NO LONGER GOD")
                                .color(NamedTextColor.BLUE));
                    } else {
                        target.setInvulnerable(true);
                        target.sendMessage(Component.text()
                                .append(Component.text("GOD MODE Enabled by ").color(NamedTextColor.GOLD))
                                .append(Component.text(p.displayName().toString()).color(NamedTextColor.GOLD)));
                        p.sendMessage(Component.text("MADE HIM GOD")
                                .color(NamedTextColor.BLUE));
                    }
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