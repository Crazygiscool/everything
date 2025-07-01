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


public class TPACommand implements CommandExecutor {
    private final Everything plugin;
    private final Map<UUID, UUID> tpaRequests = new HashMap<>();

    public TPACommand(Everything plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length == 1) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null || !target.isOnline()) {
                p.sendMessage(Component.text("Player not found or not online.").color(NamedTextColor.RED));
                return true;
            }
            if (target.equals(p)) {
                p.sendMessage(Component.text("You cannot send a TPA request to yourself.").color(NamedTextColor.RED));
                return true;
            }
            tpaRequests.put(target.getUniqueId(), p.getUniqueId());
            target.sendMessage(Component.text(p.getName() + " has requested to teleport to you. Type /tpaccept or /tpdeny.").color(NamedTextColor.AQUA));
            p.sendMessage(Component.text("TPA request sent to " + target.getName()).color(NamedTextColor.GREEN));
            return true;
        } else if (args.length == 0) {
            p.sendMessage(Component.text("Usage: /tpa <player>").color(NamedTextColor.YELLOW));
            return true;
        }
        return false;
    }

    public boolean handleTpAccept(Player target) {
        UUID requesterId = tpaRequests.remove(target.getUniqueId());
        if (requesterId == null) {
            target.sendMessage(Component.text("No pending TPA requests.").color(NamedTextColor.RED));
            return true;
        }
        Player requester = Bukkit.getPlayer(requesterId);
        if (requester == null || !requester.isOnline()) {
            target.sendMessage(Component.text("Requester is no longer online.").color(NamedTextColor.RED));
            return true;
        }
        requester.teleport(target.getLocation());
        requester.sendMessage(Component.text("Teleported to " + target.getName()).color(NamedTextColor.GREEN));
        target.sendMessage(Component.text(requester.getName() + " has teleported to you.").color(NamedTextColor.GREEN));
        return true;
    }

    public boolean handleTpDeny(Player target) {
        UUID requesterId = tpaRequests.remove(target.getUniqueId());
        if (requesterId == null) {
            target.sendMessage(Component.text("No pending TPA requests.").color(NamedTextColor.RED));
            return true;
        }
        Player requester = Bukkit.getPlayer(requesterId);
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(Component.text(target.getName() + " denied your TPA request.").color(NamedTextColor.RED));
        }
        target.sendMessage(Component.text("TPA request denied.").color(NamedTextColor.RED));
        return true;
    }
}
