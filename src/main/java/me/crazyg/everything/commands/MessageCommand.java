package me.crazyg.everything.commands;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class MessageCommand implements CommandExecutor, TabCompleter {

    private static final HashMap<UUID, UUID> lastMessage = new HashMap<>();

    public MessageCommand(Everything plugin) {
        // Empty constructor or remove if unused
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!")
                    .color(NamedTextColor.RED));
            return true;
        }

        // ----------------------------------------------------
        // /msg <player> <message>
        // ----------------------------------------------------
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

            String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

            player.sendMessage(
                Component.text("To " + target.getName() + ": ").color(NamedTextColor.GRAY)
                    .append(Component.text(message).color(NamedTextColor.WHITE))
            );

            target.sendMessage(
                Component.text("From " + player.getName() + ": ").color(NamedTextColor.GRAY)
                    .append(Component.text(message).color(NamedTextColor.WHITE))
            );

            lastMessage.put(target.getUniqueId(), player.getUniqueId());
            return true;
        }

        // ----------------------------------------------------
        // /reply <message> or /r <message>
        // ----------------------------------------------------
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

            String message = String.join(" ", args);

            player.sendMessage(
                Component.text("To " + target.getName() + ": ").color(NamedTextColor.GRAY)
                    .append(Component.text(message).color(NamedTextColor.WHITE))
            );

            target.sendMessage(
                Component.text("From " + player.getName() + ": ").color(NamedTextColor.GRAY)
                    .append(Component.text(message).color(NamedTextColor.WHITE))
            );

            lastMessage.put(target.getUniqueId(), player.getUniqueId());
            return true;
        }

        return false;
    }

    // ----------------------------------------------------
    // TAB COMPLETION
    // ----------------------------------------------------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        // /msg <player> <message>
        if (command.getName().equalsIgnoreCase("msg")) {

            // Suggest players for first argument
            if (args.length == 1) {
                String input = args[0].toLowerCase();

                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(input))
                        .toList();
            }

            // No suggestions for message text
            return List.of();
        }

        // /reply or /r
        if (command.getName().equalsIgnoreCase("reply") || command.getName().equalsIgnoreCase("r")) {

            // No suggestions for message text
            return List.of();
        }

        return List.of();
    }
}
