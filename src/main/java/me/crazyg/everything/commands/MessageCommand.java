package me.crazyg.everything.commands;

import java.util.HashMap;
import java.util.UUID;
import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageCommand implements CommandExecutor {
    private static final HashMap<UUID, UUID> lastMessage = new HashMap<>();

    public MessageCommand(Everything plugin) {
        // Empty constructor or remove it if not needed
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players!")
                    .color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("msg")) {
            if (args.length < 2) {
                player.sendMessage(Component.text("Usage: /msg <player> <message>")
                        .color(NamedTextColor.RED));
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(Component.text("Player not found!")
                        .color(NamedTextColor.RED));
                return true;
            }

            StringBuilder message = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                message.append(args[i]).append(" ");
            }

            player.sendMessage(Component.text()
                    .append(Component.text("To " + target.getName() + ": ").color(NamedTextColor.GRAY))
                    .append(Component.text(message.toString()).color(NamedTextColor.WHITE)));
            
            target.sendMessage(Component.text()
                    .append(Component.text("From " + player.getName() + ": ").color(NamedTextColor.GRAY))
                    .append(Component.text(message.toString()).color(NamedTextColor.WHITE)));

            lastMessage.put(target.getUniqueId(), player.getUniqueId());
            return true;
        }

        if (command.getName().equalsIgnoreCase("reply") || command.getName().equalsIgnoreCase("r")) {
            if (args.length < 1) {
                player.sendMessage(Component.text("Usage: /reply <message>")
                        .color(NamedTextColor.RED));
                return true;
            }

            UUID lastUUID = lastMessage.get(player.getUniqueId());
            if (lastUUID == null) {
                player.sendMessage(Component.text("Nobody to reply to!")
                        .color(NamedTextColor.RED));
                return true;
            }

            Player target = Bukkit.getPlayer(lastUUID);
            if (target == null) {
                player.sendMessage(Component.text("Player is offline!")
                        .color(NamedTextColor.RED));
                return true;
            }

            StringBuilder message = new StringBuilder();
            for (String arg : args) {
                message.append(arg).append(" ");
            }

            player.sendMessage(Component.text()
                    .append(Component.text("To " + target.getName() + ": ").color(NamedTextColor.GRAY))
                    .append(Component.text(message.toString()).color(NamedTextColor.WHITE)));
            
            target.sendMessage(Component.text()
                    .append(Component.text("From " + player.getName() + ": ").color(NamedTextColor.GRAY))
                    .append(Component.text(message.toString()).color(NamedTextColor.WHITE)));

            lastMessage.put(target.getUniqueId(), player.getUniqueId());
            return true;
        }

        return false;
    }
}