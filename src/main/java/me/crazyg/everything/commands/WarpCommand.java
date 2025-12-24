package me.crazyg.everything.commands;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;


public class WarpCommand implements CommandExecutor {
    private final Everything plugin;
    private final File warpsFile;
    private FileConfiguration warpsConfig;

    public WarpCommand(Everything plugin) {
        this.plugin = plugin;
        this.warpsFile = new File(plugin.getDataFolder(), "locations.yml");
        loadWarps();
    }

    private void loadWarps() {
        if (!warpsFile.exists()) {
            plugin.saveResource("locations.yml", false);
        }
        warpsConfig = YamlConfiguration.loadConfiguration(warpsFile);
    }

    private void saveWarps() {
        try {
            warpsConfig.save(warpsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save warps file!");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            return listWarps(player);
        }

        switch (args[0].toLowerCase()) {
            case "set":
                if (!player.hasPermission("everything.warp.set")) {
                    player.sendMessage(Component.text("You don't have permission to set warps!")
                            .color(NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /warp set <name>")
                            .color(NamedTextColor.RED));
                    return true;
                }
                return setWarp(player, args[1]);

            case "delete":
                if (!player.hasPermission("everything.warp.delete")) {
                    player.sendMessage(Component.text("You don't have permission to delete warps!")
                            .color(NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /warp delete <name>")
                            .color(NamedTextColor.RED));
                    return true;
                }
                return deleteWarp(player, args[1]);

            default:
                return teleportToWarp(player, args[0]);
        }
    }

    private boolean listWarps(Player player) {
        Set<String> warps = warpsConfig.getKeys(false);
        if (warps.isEmpty()) {
            player.sendMessage(Component.text("There are no warps set!")
                    .color(NamedTextColor.RED));
            return true;
        }

        player.sendMessage(Component.text("Available warps:")
                .color(NamedTextColor.YELLOW));
        warps.forEach(warp -> player.sendMessage(Component.text("- " + warp)
                .color(NamedTextColor.GREEN)));
        return true;
    }

    private boolean setWarp(Player player, String name) {
        Location loc = player.getLocation();
        warpsConfig.set(name.toLowerCase() + ".world", loc.getWorld().getName());
        warpsConfig.set(name.toLowerCase() + ".x", loc.getX());
        warpsConfig.set(name.toLowerCase() + ".y", loc.getY());
        warpsConfig.set(name.toLowerCase() + ".z", loc.getZ());
        warpsConfig.set(name.toLowerCase() + ".yaw", loc.getYaw());
        warpsConfig.set(name.toLowerCase() + ".pitch", loc.getPitch());
        saveWarps();

        player.sendMessage(Component.text("Warp '" + name + "' has been set!")
                .color(NamedTextColor.GREEN));
        return true;
    }

    private boolean deleteWarp(Player player, String name) {
        if (!warpsConfig.contains(name.toLowerCase())) {
            player.sendMessage(Component.text("Warp '" + name + "' does not exist!")
                    .color(NamedTextColor.RED));
            return true;
        }

        warpsConfig.set(name.toLowerCase(), null);
        saveWarps();

        player.sendMessage(Component.text("Warp '" + name + "' has been deleted!")
                .color(NamedTextColor.GREEN));
        return true;
    }

    private boolean teleportToWarp(Player player, String name) {
        if (!warpsConfig.contains(name.toLowerCase())) {
            player.sendMessage(Component.text("Warp '" + name + "' does not exist!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("everything.warp.use." + name.toLowerCase()) &&
            !player.hasPermission("everything.warp.use.*")) {
            player.sendMessage(Component.text("You don't have permission to use this warp!")
                    .color(NamedTextColor.RED));
            return true;
        }

        String worldName = warpsConfig.getString(name.toLowerCase() + ".world");
        if (worldName == null) {
            player.sendMessage(Component.text("Warp '" + name + "' is missing a world!").color(NamedTextColor.RED));
            return true;
        }
        if (Bukkit.getWorld(worldName) == null) {
            player.sendMessage(Component.text("World '" + worldName + "' does not exist!").color(NamedTextColor.RED));
            return true;
        }

        double x = warpsConfig.getDouble(name.toLowerCase() + ".x");
        double y = warpsConfig.getDouble(name.toLowerCase() + ".y");
        double z = warpsConfig.getDouble(name.toLowerCase() + ".z");
        float yaw = (float) warpsConfig.getDouble(name.toLowerCase() + ".yaw");
        float pitch = (float) warpsConfig.getDouble(name.toLowerCase() + ".pitch");

        Location loc = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
        player.teleport(loc);

        player.sendMessage(Component.text("Teleported to warp '" + name + "'!")
                .color(NamedTextColor.GREEN));
        return true;
    }
}