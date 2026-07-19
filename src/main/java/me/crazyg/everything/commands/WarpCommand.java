package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import me.crazyg.everything.utils.AdventureCompat;
import me.crazyg.everything.utils.storage.YamlRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

public class WarpCommand extends YamlRepository
        implements CommandExecutor, TabCompleter {

    private final Everything plugin;

    public WarpCommand(Everything plugin) {
        super(plugin, "location", "location/warp.yml", "warp.yml");
        this.plugin = plugin;

        loadWarps();
    }

    public Set<String> getWarpNames() {
        return config.getKeys(false);
    }

    private void loadWarps() {
        if (!config.contains("warps")) {
            config.createSection("warps");
            save();
        }
    }

    private void saveWarps() {
        save();
    }

    // ----------------------------------------------------
    // COMMAND EXECUTION
    // ----------------------------------------------------
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            AdventureCompat.sendMessage(sender, Component.text("Only players can use this command!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            return listWarps(player);
        }

        switch (args[0].toLowerCase()) {
            case "set" -> {
                if (!player.hasPermission("everything.warp.set")) {
                    AdventureCompat.sendMessage(player, Component.text("You don't have permission to set warps!")
                            .color(NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    AdventureCompat.sendMessage(player, Component.text("Usage: /warp set <name>")
                            .color(NamedTextColor.RED));
                    return true;
                }
                return setWarp(player, args[1]);
            }

            case "delete" -> {
                if (!player.hasPermission("everything.warp.delete")) {
                    AdventureCompat.sendMessage(player, Component.text("You don't have permission to delete warps!")
                            .color(NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    AdventureCompat.sendMessage(player, Component.text("Usage: /warp delete <name>")
                            .color(NamedTextColor.RED));
                    return true;
                }
                return deleteWarp(player, args[1]);
            }

            default -> {
                return teleportToWarp(player, args[0]);
            }
        }
    }

    private boolean listWarps(Player player) {
        Set<String> warps = config.getKeys(false);
        if (warps.isEmpty()) {
            AdventureCompat.sendMessage(player, Component.text("There are no warps set!")
                    .color(NamedTextColor.RED));
            return true;
        }

        AdventureCompat.sendMessage(player, Component.text("Available warps:")
                .color(NamedTextColor.YELLOW));
        warps.forEach(warp -> AdventureCompat.sendMessage(player, Component.text("- " + warp)
                .color(NamedTextColor.GREEN)));
        return true;
    }

    private boolean setWarp(Player player, String name) {
        Location loc = player.getLocation();
        config.set(name.toLowerCase() + ".world", loc.getWorld().getName());
        config.set(name.toLowerCase() + ".x", loc.getX());
        config.set(name.toLowerCase() + ".y", loc.getY());
        config.set(name.toLowerCase() + ".z", loc.getZ());
        config.set(name.toLowerCase() + ".yaw", loc.getYaw());
        config.set(name.toLowerCase() + ".pitch", loc.getPitch());
        saveWarps();

        AdventureCompat.sendMessage(player, Component.text("Warp '" + name + "' has been set!")
                .color(NamedTextColor.GREEN));
        return true;
    }

    private boolean deleteWarp(Player player, String name) {
        if (!config.contains(name.toLowerCase())) {
            AdventureCompat.sendMessage(player, Component.text("Warp '" + name + "' does not exist!")
                    .color(NamedTextColor.RED));
            return true;
        }

        config.set(name.toLowerCase(), null);
        saveWarps();

        AdventureCompat.sendMessage(player, Component.text("Warp '" + name + "' has been deleted!")
                .color(NamedTextColor.GREEN));
        return true;
    }

    private boolean teleportToWarp(Player player, String name) {
        if (!config.contains(name.toLowerCase())) {
            AdventureCompat.sendMessage(player, Component.text("Warp '" + name + "' does not exist!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("everything.warp.use." + name.toLowerCase())
                && !player.hasPermission("everything.warp.use.*")) {
            AdventureCompat.sendMessage(player, Component.text("You don't have permission to use this warp!")
                    .color(NamedTextColor.RED));
            return true;
        }

        String worldName = config.getString(name.toLowerCase() + ".world");
        if (worldName == null || Bukkit.getWorld(worldName) == null) {
            AdventureCompat.sendMessage(player, Component.text("Warp '" + name + "' has an invalid world!")
                    .color(NamedTextColor.RED));
            return true;
        }

        double x = config.getDouble(name.toLowerCase() + ".x");
        double y = config.getDouble(name.toLowerCase() + ".y");
        double z = config.getDouble(name.toLowerCase() + ".z");
        float yaw = (float) config.getDouble(name.toLowerCase() + ".yaw");
        float pitch = (float) config.getDouble(name.toLowerCase() + ".pitch");

        Location loc = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
        player.teleport(loc);

        AdventureCompat.sendMessage(player, Component.text("Teleported to warp '" + name + "'!")
                .color(NamedTextColor.GREEN));
        return true;
    }

    // ----------------------------------------------------
    // TAB COMPLETION
    // ----------------------------------------------------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        Set<String> warps = config.getKeys(false);

        // /warp <tab>
        if (args.length == 1) {
            String input = args[0].toLowerCase();

            // Base subcommands
            List<String> base = List.of("set", "delete");

            // Filter base commands
            List<String> baseMatches = base.stream()
                    .filter(s -> s.startsWith(input))
                    .toList();

            // Filter warp names
            List<String> warpMatches = warps.stream()
                    .filter(w -> w.toLowerCase().startsWith(input))
                    .toList();

            // Merge both
            List<String> result = new java.util.ArrayList<>();
            result.addAll(baseMatches);
            result.addAll(warpMatches);
            return result;
        }

        // /warp delete <warp>
        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            String input = args[1].toLowerCase();

            return warps.stream()
                    .filter(w -> w.toLowerCase().startsWith(input))
                    .toList();
        }

        // /warp set <name> → no suggestions
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return List.of();
        }

        return List.of();
    }
}
