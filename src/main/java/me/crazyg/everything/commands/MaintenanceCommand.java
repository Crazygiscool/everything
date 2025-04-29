package me.crazyg.everything.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;


public class MaintenanceCommand implements CommandExecutor, Listener {
    private final Everything plugin;
    private boolean maintenanceMode;
    private final List<UUID> allowedPlayers;
    private Component kickMessage;

    public MaintenanceCommand(Everything plugin) {
        this.plugin = plugin;
        this.maintenanceMode = plugin.getConfig().getBoolean("maintenance-mode", false);
        this.allowedPlayers = new ArrayList<>();
        this.kickMessage = Component.text("Server is currently in maintenance mode!")
                .color(NamedTextColor.RED);
        
        // Register the listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Load allowed players from config
        List<String> allowed = plugin.getConfig().getStringList("maintenance-allowed-players");
        allowed.forEach(uuid -> allowedPlayers.add(UUID.fromString(uuid)));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("everything.maintenance")) {
            sender.sendMessage(Component.text("You don't have permission to use this command!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            toggleMaintenance(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "on":
                enableMaintenance(sender);
                break;
            case "off":
                disableMaintenance(sender);
                break;
            case "add":
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /maintenance add <player>")
                            .color(NamedTextColor.RED));
                    return true;
                }
                addAllowedPlayer(sender, args[1]);
                break;
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /maintenance remove <player>")
                            .color(NamedTextColor.RED));
                    return true;
                }
                removeAllowedPlayer(sender, args[1]);
                break;
            case "list":
                listAllowedPlayers(sender);
                break;
            default:
                sender.sendMessage(Component.text("Usage: /maintenance [on|off|add|remove|list]")
                        .color(NamedTextColor.RED));
        }
        return true;
    }

    private void toggleMaintenance(CommandSender sender) {
        maintenanceMode = !maintenanceMode;
        if (maintenanceMode) {
            enableMaintenance(sender);
        } else {
            disableMaintenance(sender);
        }
    }

    private void enableMaintenance(CommandSender sender) {
        maintenanceMode = true;
        plugin.getConfig().set("maintenance-mode", true);
        plugin.saveConfig();
        
        Bukkit.broadcast(Component.text("Server entering maintenance mode!")
                .color(NamedTextColor.RED));
        
        // Kick non-allowed players
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("everything.maintenance.bypass") && 
                !allowedPlayers.contains(player.getUniqueId())) {
                player.kick(kickMessage);
            }
        }
        
        sender.sendMessage(Component.text("Maintenance mode enabled!")
                .color(NamedTextColor.GREEN));
    }

    private void disableMaintenance(CommandSender sender) {
        maintenanceMode = false;
        plugin.getConfig().set("maintenance-mode", false);
        plugin.saveConfig();
        
        Bukkit.broadcast(Component.text("Server maintenance mode disabled!")
                .color(NamedTextColor.GREEN));
        
        sender.sendMessage(Component.text("Maintenance mode disabled!")
                .color(NamedTextColor.GREEN));
    }

    private void addAllowedPlayer(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found!")
                    .color(NamedTextColor.RED));
            return;
        }

        UUID targetUUID = target.getUniqueId();
        if (allowedPlayers.contains(targetUUID)) {
            sender.sendMessage(Component.text("Player is already on the allowed list!")
                    .color(NamedTextColor.RED));
            return;
        }

        allowedPlayers.add(targetUUID);
        saveAllowedPlayers();
        
        sender.sendMessage(Component.text("Added " + playerName + " to the allowed players list!")
                .color(NamedTextColor.GREEN));
    }

    private void removeAllowedPlayer(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found!")
                    .color(NamedTextColor.RED));
            return;
        }

        UUID targetUUID = target.getUniqueId();
        if (!allowedPlayers.contains(targetUUID)) {
            sender.sendMessage(Component.text("Player is not on the allowed list!")
                    .color(NamedTextColor.RED));
            return;
        }

        allowedPlayers.remove(targetUUID);
        saveAllowedPlayers();
        
        sender.sendMessage(Component.text("Removed " + playerName + " from the allowed players list!")
                .color(NamedTextColor.GREEN));
        
        if (maintenanceMode && target.isOnline()) {
            target.kick(kickMessage);
        }
    }

    private void listAllowedPlayers(CommandSender sender) {
        if (allowedPlayers.isEmpty()) {
            sender.sendMessage(Component.text("No players are on the allowed list!")
                    .color(NamedTextColor.YELLOW));
            return;
        }

        sender.sendMessage(Component.text("Allowed players:")
                .color(NamedTextColor.YELLOW));
        
        allowedPlayers.forEach(uuid -> {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name != null) {
                sender.sendMessage(Component.text("- " + name)
                        .color(NamedTextColor.GREEN));
            }
        });
    }

    private void saveAllowedPlayers() {
        List<String> uuidStrings = allowedPlayers.stream()
                .map(UUID::toString)
                .toList();
        plugin.getConfig().set("maintenance-allowed-players", uuidStrings);
        plugin.saveConfig();
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (maintenanceMode && 
            !event.getPlayer().hasPermission("everything.maintenance.bypass") &&
            !allowedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickMessage);
        }
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }
}