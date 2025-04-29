package me.crazyg.everything.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class TpaCommand implements CommandExecutor {
    private final Everything plugin;
    private final Map<UUID, UUID> tpaRequests = new HashMap<>();
    private final Map<UUID, Long> tpaCooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 30000; // 30 seconds

    public TpaCommand(Everything plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("everything.tpa")) {
            player.sendMessage(Component.text("You don't have permission to use this command!")
                    .color(NamedTextColor.RED));
            return true;
        }

        // Handle different TPA commands
        switch (label.toLowerCase()) {
            case "tpa":
                return handleTpaRequest(player, args);
            case "tpaccept":
                return handleTpaAccept(player);
            case "tpdeny":
                return handleTpaDeny(player);
            default:
                return false;
        }
    }

    private boolean handleTpaRequest(Player sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /tpa <player>")
                    .color(NamedTextColor.RED));
            return true;
        }

        // Check cooldown
        if (tpaCooldowns.containsKey(sender.getUniqueId())) {
            long timeLeft = (tpaCooldowns.get(sender.getUniqueId()) - System.currentTimeMillis()) / 1000;
            if (timeLeft > 0) {
                sender.sendMessage(Component.text("You must wait " + timeLeft + " seconds before sending another request!")
                        .color(NamedTextColor.RED));
                return true;
            }
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (target.equals(sender)) {
            sender.sendMessage(Component.text("You cannot send a teleport request to yourself!")
                    .color(NamedTextColor.RED));
            return true;
        }

        tpaRequests.put(target.getUniqueId(), sender.getUniqueId());
        tpaCooldowns.put(sender.getUniqueId(), System.currentTimeMillis() + COOLDOWN_TIME);

        sender.sendMessage(Component.text("Teleport request sent to " + target.getName())
                .color(NamedTextColor.GREEN));
        target.sendMessage(Component.text()
                .append(Component.text(sender.getName() + " has requested to teleport to you. ")
                        .color(NamedTextColor.YELLOW))
                .append(Component.text("/tpaccept").color(NamedTextColor.GREEN))
                .append(Component.text(" to accept or ").color(NamedTextColor.YELLOW))
                .append(Component.text("/tpdeny").color(NamedTextColor.RED))
                .append(Component.text(" to deny.").color(NamedTextColor.YELLOW)));

        // Remove request after 60 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (tpaRequests.containsKey(target.getUniqueId())) {
                tpaRequests.remove(target.getUniqueId());
                sender.sendMessage(Component.text("Your teleport request to " + target.getName() + " has expired.")
                        .color(NamedTextColor.RED));
            }
        }, 1200L);

        return true;
    }

    private boolean handleTpaAccept(Player player) {
        UUID requesterUUID = tpaRequests.get(player.getUniqueId());
        if (requesterUUID == null) {
            player.sendMessage(Component.text("You have no pending teleport requests!")
                    .color(NamedTextColor.RED));
            return true;
        }

        Player requester = Bukkit.getPlayer(requesterUUID);
        if (requester == null) {
            player.sendMessage(Component.text("The requesting player is no longer online!")
                    .color(NamedTextColor.RED));
            tpaRequests.remove(player.getUniqueId());
            return true;
        }

        requester.teleport(player.getLocation());
        requester.sendMessage(Component.text("Teleporting to " + player.getName())
                .color(NamedTextColor.GREEN));
        player.sendMessage(Component.text(requester.getName() + " has been teleported to you.")
                .color(NamedTextColor.GREEN));
        tpaRequests.remove(player.getUniqueId());

        return true;
    }

    private boolean handleTpaDeny(Player player) {
        UUID requesterUUID = tpaRequests.get(player.getUniqueId());
        if (requesterUUID == null) {
            player.sendMessage(Component.text("You have no pending teleport requests!")
                    .color(NamedTextColor.RED));
            return true;
        }

        Player requester = Bukkit.getPlayer(requesterUUID);
        tpaRequests.remove(player.getUniqueId());

        player.sendMessage(Component.text("You denied the teleport request.")
                .color(NamedTextColor.YELLOW));
        
        if (requester != null) {
            requester.sendMessage(Component.text(player.getName() + " denied your teleport request.")
                    .color(NamedTextColor.RED));
        }

        return true;
    }
}