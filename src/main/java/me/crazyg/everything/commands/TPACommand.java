package me.crazyg.everything.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.crazyg.everything.Everything;
import me.crazyg.everything.utils.AdventureCompat;
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
            AdventureCompat.sendMessage(sender, Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length == 1) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null || !target.isOnline()) {
                AdventureCompat.sendMessage(p, Component.text("Player not found or not online.").color(NamedTextColor.RED));
                return true;
            }
            if (target.equals(p)) {
                AdventureCompat.sendMessage(p, Component.text("You cannot send a TPA request to yourself.").color(NamedTextColor.RED));
                return true;
            }
            tpaRequests.put(target.getUniqueId(), p.getUniqueId());
            AdventureCompat.sendMessage(target, Component.text(p.getName() + " has requested to teleport to you. Type /tpaccept or /tpdeny.").color(NamedTextColor.AQUA));
            AdventureCompat.sendMessage(p, Component.text("TPA request sent to " + target.getName()).color(NamedTextColor.GREEN));
            return true;
        } else if (args.length == 0) {
            AdventureCompat.sendMessage(p, Component.text("Usage: /tpa <player>").color(NamedTextColor.YELLOW));
            return true;
        }
        return false;
    }

    public boolean handleTpAccept(Player target) {
        UUID requesterId = tpaRequests.remove(target.getUniqueId());
        if (requesterId == null) {
            AdventureCompat.sendMessage(target, Component.text("No pending TPA requests.").color(NamedTextColor.RED));
            return true;
        }
        Player requester = Bukkit.getPlayer(requesterId);
        if (requester == null || !requester.isOnline()) {
            AdventureCompat.sendMessage(target, Component.text("Requester is no longer online.").color(NamedTextColor.RED));
            return true;
        }
        requester.teleport(target.getLocation());
        AdventureCompat.sendMessage(requester, Component.text("Teleported to " + target.getName()).color(NamedTextColor.GREEN));
        AdventureCompat.sendMessage(target, Component.text(requester.getName() + " has teleported to you.").color(NamedTextColor.GREEN));
        return true;
    }

    public boolean handleTpDeny(Player target) {
        UUID requesterId = tpaRequests.remove(target.getUniqueId());
        if (requesterId == null) {
            AdventureCompat.sendMessage(target, Component.text("No pending TPA requests.").color(NamedTextColor.RED));
            return true;
        }
        Player requester = Bukkit.getPlayer(requesterId);
        if (requester != null && requester.isOnline()) {
            AdventureCompat.sendMessage(requester, Component.text(target.getName() + " denied your TPA request.").color(NamedTextColor.RED));
        }
        AdventureCompat.sendMessage(target, Component.text("TPA request denied.").color(NamedTextColor.RED));
        return true;
    }
}
