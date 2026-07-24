package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import me.crazyg.everything.utils.AdventureCompat;
import me.crazyg.everything.utils.storage.YamlRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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
import org.bukkit.entity.Player;

import java.util.List;

public class HomeCommand extends YamlRepository
        implements CommandExecutor, TabCompleter {

    private final Everything plugin;
    private final Map<UUID, Location> playerHomes = new HashMap<>();

    public HomeCommand(Everything plugin) {
        super(plugin, "location", "location/home.yml", "home.yml");
        this.plugin = plugin;

        loadHomes();

        // Save every 5 minutes
        Bukkit.getScheduler().runTaskTimer(plugin, this::saveAllHomes, 6000L, 6000L);
    }

    // ------------------------------
    // LOAD HOMES
    // ------------------------------
    private void loadHomes() {
        if (!config.contains("homes")) return;

        for (String uuidStr : config.getConfigurationSection("homes").getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);

            String worldName = config.getString("homes." + uuidStr + ".world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            double x = config.getDouble("homes." + uuidStr + ".x");
            double y = config.getDouble("homes." + uuidStr + ".y");
            double z = config.getDouble("homes." + uuidStr + ".z");
            float yaw = (float) config.getDouble("homes." + uuidStr + ".yaw");
            float pitch = (float) config.getDouble("homes." + uuidStr + ".pitch");

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

            config.set("homes." + uuid + ".world", loc.getWorld().getName());
            config.set("homes." + uuid + ".x", loc.getX());
            config.set("homes." + uuid + ".y", loc.getY());
            config.set("homes." + uuid + ".z", loc.getZ());
            config.set("homes." + uuid + ".yaw", loc.getYaw());
            config.set("homes." + uuid + ".pitch", loc.getPitch());
        }

        save();
    }

    // ------------------------------
    // COMMAND HANDLING
    // ------------------------------
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            AdventureCompat.sendMessage(sender, Component.text("This command can only be used by players!")
                    .color(NamedTextColor.RED));
            return true;
        }

        UUID uuid = player.getUniqueId();

        // /sethome
        if (command.getName().equalsIgnoreCase("sethome")) {
            Location loc = player.getLocation();
            playerHomes.put(uuid, loc);
            saveAllHomes();

            if (plugin.getParticleManager().isEnabled("sethome")) {
                plugin.getParticleManager().playEffect(player, me.crazyg.everything.utils.particle.ParticleEffect.HOME_SET);
            }
            AdventureCompat.sendMessage(player, Component.text("Home set!").color(NamedTextColor.GREEN));
            return true;
        }

        // /home
        if (command.getName().equalsIgnoreCase("home")) {
            if (!playerHomes.containsKey(uuid)) {
                AdventureCompat.sendMessage(player, Component.text("You haven't set a home yet!")
                        .color(NamedTextColor.RED));
                return true;
            }

            plugin.getTeleportManager().teleport(player, playerHomes.get(uuid), "home");
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
