package me.crazyg.everything.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.crazyg.everything.Everything;

public class SetSpawnCommand implements CommandExecutor {

    private final Everything plugin;

    public SetSpawnCommand(Everything plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("setspawn")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can set the spawn location.");
                return true;
            }

            Player player = (Player) sender;
            Location location = player.getLocation();

            // Save the spawn location to the config
            plugin.getConfig().set("spawn.world", location.getWorld().getName());
            plugin.getConfig().set("spawn.x", location.getX());
            plugin.getConfig().set("spawn.y", location.getY());
            plugin.getConfig().set("spawn.z", location.getZ());
            plugin.getConfig().set("spawn.yaw", location.getYaw());
            plugin.getConfig().set("spawn.pitch", location.getPitch());
            plugin.saveConfig();

            player.sendMessage(ChatColor.GREEN + "Spawn location set!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("spawn")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can teleport to the spawn location.");
                return true;
            }

            Player player = (Player) sender;

            // Retrieve the spawn location from the config
            String worldName = plugin.getConfig().getString("spawn.world");
            if (worldName == null) {
                player.sendMessage(ChatColor.RED + "Spawn location is not set.");
                return true;
            }

            Location spawnLocation = new Location(
                    Bukkit.getWorld(worldName),
                    plugin.getConfig().getDouble("spawn.x"),
                    plugin.getConfig().getDouble("spawn.y"),
                    plugin.getConfig().getDouble("spawn.z"),
                    (float) plugin.getConfig().getDouble("spawn.yaw"),
                    (float) plugin.getConfig().getDouble("spawn.pitch")
            );

            player.teleport(spawnLocation);
            player.sendMessage(ChatColor.GREEN + "Teleported to spawn!");
            return true;
        }

        return false;
    }
}