package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class HomeCommand implements CommandExecutor, TabCompleter {

    private final Everything plugin;
    private final File homesFile;
    private FileConfiguration homesConfig;
    private final Map<UUID, Location> playerHomes = new HashMap<>();

    public HomeCommand(Everything plugin) {
        this.plugin = plugin;
        this.homesFile = new File(plugin.getDataFolder(), "home.yml");

        loadHomes();

        // Save every 5 minutes
        Bukkit.getScheduler().runTaskTimer(plugin, this::saveAllHomes, 6000L, 6000L);
    }

    // ------------------------------
    // LOAD HOMES
    // ------------------------------
    private void loadHomes() {
        if (!homesFile.exists()) {
            try {
                homesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create home.yml!");
            }
        }

        homesConfig = YamlConfiguration.loadConfiguration(homesFile);

        if (!homesConfig.contains("homes")) return;

        for (String uuidStr : homesConfig.getConfigurationSection("homes").getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);

            String worldName = homesConfig.getString("homes." + uuidStr + ".world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            double x = homesConfig.getDouble("homes." + uuidStr + ".x");
            double y = homesConfig.getDouble("homes." + uuidStr + ".y");
            double z = homesConfig.getDouble("homes." + uuidStr + ".z");
            float yaw = (float) homesConfig.getDouble("homes." + uuidStr + ".yaw");
            float pitch = (float) homesConfig.getDouble("homes." + uuidStr + ".pitch");

            Location loc = new Location(world, x, y, z, yaw, pitch);
            playerHomes.put(uuid, loc);
        }
    }

    // ------------------------------
    // SAVE HOMES
    // ------------------------------
    private void saveAllHomes() {
        for (Map.Entry<UUID, Location> entry : playerHomes.entrySet()) {
            UUID uuid = entry.getKey();
            Location loc = entry.getValue();

            homesConfig.set("homes." + uuid + ".world", loc.getWorld().getName());
            homesConfig.set("homes." + uuid + ".x", loc.getX());
            homesConfig.set("homes." + uuid + ".y", loc.getY());
            homesConfig.set("homes." + uuid + ".z", loc.getZ());
            homesConfig.set("homes." + uuid + ".yaw", loc.getYaw());
            homesConfig.set("homes." + uuid + ".pitch", loc.getPitch());
        }

        try {
            homesConfig.save(homesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save home.yml!");
        }
    }

    // ------------------------------
    // COMMAND HANDLING
    // ------------------------------
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!")
                    .color(NamedTextColor.RED));
            return true;
        }

        UUID uuid = player.getUniqueId();

        // /sethome
        if (command.getName().equalsIgnoreCase("sethome")) {
            Location loc = player.getLocation();
            playerHomes.put(uuid, loc);
            saveAllHomes();

            player.sendMessage(Component.text("Home set!").color(NamedTextColor.GREEN));
            return true;
        }

        // /home
        if (command.getName().equalsIgnoreCase("home")) {
            if (!playerHomes.containsKey(uuid)) {
                player.sendMessage(Component.text("You haven't set a home yet!")
                        .color(NamedTextColor.RED));
                return true;
            }

            player.teleport(playerHomes.get(uuid));
            player.sendMessage(Component.text("Teleported to home!")
                    .color(NamedTextColor.GREEN));
            return true;
        }

        return false;
    }

    // ------------------------------
    // TAB COMPLETION
    // ------------------------------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        // /home → no args → no suggestions
        if (command.getName().equalsIgnoreCase("home")) {
            return List.of();
        }

        // /sethome → no args → no suggestions
        if (command.getName().equalsIgnoreCase("sethome")) {
            return List.of();
        }

        return List.of();
    }
}
