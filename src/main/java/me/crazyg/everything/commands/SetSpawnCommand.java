package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import me.crazyg.everything.utils.AdventureCompat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class SetSpawnCommand implements CommandExecutor {

    private final Everything plugin;
    private final File locationsFile;
    private FileConfiguration locationsConfig;

    public SetSpawnCommand(Everything plugin) {
        this.plugin = plugin;

        // Ensure /plugins/Everything/location/ exists
        File folder = new File(plugin.getDataFolder(), "location");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // Extract spawn.yml from resources if missing
        File spawnResource = new File(folder, "spawn.yml");
        if (!spawnResource.exists()) {
            plugin.saveResource("location/spawn.yml", false);
        }

        // Load the actual file
        this.locationsFile = spawnResource;

        loadLocations();
    }

    private void loadLocations() {
        if (!locationsFile.exists()) {
            try {
                locationsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create spawn.yml!");
            }
        }
        locationsConfig = YamlConfiguration.loadConfiguration(locationsFile);
    }

    public void reload() {
        loadLocations();
    }

    private void saveLocations() {
        try {
            locationsConfig.save(locationsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save spawn.yml!");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("setspawn")) {
            if (!(sender instanceof Player player)) {
                AdventureCompat.sendMessage(sender, Component.text("Only players can set the spawn location.")
                        .color(NamedTextColor.RED));
                return true;
            }

            Location loc = player.getLocation();

            locationsConfig.set("spawn.world", loc.getWorld().getName());
            locationsConfig.set("spawn.x", loc.getX());
            locationsConfig.set("spawn.y", loc.getY());
            locationsConfig.set("spawn.z", loc.getZ());
            locationsConfig.set("spawn.yaw", loc.getYaw());
            locationsConfig.set("spawn.pitch", loc.getPitch());
            saveLocations();
            
            World world = loc.getWorld();

            int x = (int) locationsConfig.getDouble("spawn.x");
            int y = (int) locationsConfig.getDouble("spawn.y");
            int z = (int) locationsConfig.getDouble("spawn.z");

            world.setSpawnLocation(x, y, z);

            if (plugin.getParticleManager().isEnabled("setspawn")) {
                plugin.getParticleManager().playEffect(player, me.crazyg.everything.utils.particle.ParticleEffect.SPAWN_SET);
            }
            AdventureCompat.sendMessage(player, Component.text("Spawn location set!")
                    .color(NamedTextColor.GREEN));
            return true;
        }

        if (command.getName().equalsIgnoreCase("spawn")) {
            if (!(sender instanceof Player player)) {
                AdventureCompat.sendMessage(sender, Component.text("Only players can teleport to the spawn location.")
                        .color(NamedTextColor.RED));
                return true;
            }

            String worldName = locationsConfig.getString("spawn.world");
            Location spawnLoc;
            if (worldName != null && Bukkit.getWorld(worldName) != null) {
                org.bukkit.World spawnWorld = Bukkit.getWorld(worldName);
                spawnLoc = new Location(
                        spawnWorld,
                        locationsConfig.getDouble("spawn.x"),
                        locationsConfig.getDouble("spawn.y"),
                        locationsConfig.getDouble("spawn.z"),
                        (float) locationsConfig.getDouble("spawn.yaw"),
                        (float) locationsConfig.getDouble("spawn.pitch")
                );
            } else {
                World fallbackWorld = Bukkit.getWorlds().isEmpty()
                    ? player.getWorld() : Bukkit.getWorlds().get(0);
                spawnLoc = fallbackWorld.getSpawnLocation().clone();
                AdventureCompat.sendMessage(player, Component.text(
                    "No custom spawn is set; using the world spawn.")
                    .color(NamedTextColor.YELLOW));
            }

            plugin.getTeleportManager().teleport(player, spawnLoc, "spawn");
            return true;
        }

        return false;
    }
}
