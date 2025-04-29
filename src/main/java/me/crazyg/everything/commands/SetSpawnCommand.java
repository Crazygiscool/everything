package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {

    private final Everything plugin;

    public SetSpawnCommand(Everything plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("setspawn")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Only players can set the spawn location.")
                        .color(NamedTextColor.RED));
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

            player.sendMessage(Component.text("Spawn location set!")
                    .color(NamedTextColor.GREEN));
            return true;
        }

        if (command.getName().equalsIgnoreCase("spawn")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Only players can teleport to the spawn location.")
                        .color(NamedTextColor.RED));
                return true;
            }

            Player player = (Player) sender;

            // Retrieve the spawn location from the config
            String worldName = plugin.getConfig().getString("spawn.world");
            if (worldName == null) {
                player.sendMessage(Component.text("Spawn location is not set.")
                        .color(NamedTextColor.RED));
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
            player.sendMessage(Component.text("Teleported to spawn!")
                    .color(NamedTextColor.GREEN));
            return true;
        }

        return false;
    }
}