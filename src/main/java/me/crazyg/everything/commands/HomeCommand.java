package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HomeCommand implements CommandExecutor {
    private final Everything plugin;

    public HomeCommand(Everything plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players!")
                    .color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        String uuid = player.getUniqueId().toString();

        if (command.getName().equalsIgnoreCase("sethome")) {
            Location loc = player.getLocation();
            plugin.getConfig().set("homes." + uuid, loc);
            plugin.saveConfig();
            player.sendMessage(Component.text("Home location set!")
                    .color(NamedTextColor.GREEN));
            return true;
        }

        if (command.getName().equalsIgnoreCase("home")) {
            if (!plugin.getConfig().contains("homes." + uuid)) {
                player.sendMessage(Component.text("You haven't set a home yet!")
                        .color(NamedTextColor.RED));
                return true;
            }
            String worldName = plugin.getConfig().getString("homes." + uuid + ".world");
            double x = plugin.getConfig().getDouble("homes." + uuid + ".x");
            double y = plugin.getConfig().getDouble("homes." + uuid + ".y");
            double z = plugin.getConfig().getDouble("homes." + uuid + ".z");
            float yaw = (float) plugin.getConfig().getDouble("homes." + uuid + ".yaw");
            float pitch = (float) plugin.getConfig().getDouble("homes." + uuid + ".pitch");
            Location loc = new Location(plugin.getServer().getWorld(worldName), x, y, z, yaw, pitch);
            player.teleport(loc);
            player.sendMessage(Component.text("Teleported to home!")
                    .color(NamedTextColor.GREEN));
            return true;
        }

        return false;
    }
}