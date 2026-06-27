package me.crazyg.everything.commands;

import java.util.Random;
import me.crazyg.everything.Everything;
import me.crazyg.everything.utils.AdventureCompat;
import me.crazyg.everything.utils.CooldownManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RTPCommand implements CommandExecutor {

    private final Everything plugin;
    private final CooldownManager cooldownManager;

    public RTPCommand(Everything plugin) {
        this.plugin = plugin;
        this.cooldownManager = new CooldownManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            AdventureCompat.sendMessage(sender, Component.text("Only players can use this command!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("everything.rtp")) {
            AdventureCompat.sendMessage(player, Component.text("You don't have permission to random teleport!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("everything.rtp.bypasscooldown")) {
            if (cooldownManager.hasCooldown("rtp", player)) {
                return true;
            }
        }

        World world = player.getWorld();

        double centerX = plugin.getConfig().getDouble("rtp.center-x", 0);
        double centerZ = plugin.getConfig().getDouble("rtp.center-z", 0);
        double minRadius = plugin.getConfig().getDouble("rtp.min-radius", 500);
        double maxRadius = plugin.getConfig().getDouble("rtp.max-radius", 5000);
        int maxAttempts = plugin.getConfig().getInt("rtp.max-attempts", 20);
        int cooldownSeconds = plugin.getConfig().getInt("rtp.cooldown-seconds", 60);

        if (minRadius > maxRadius) {
            double temp = minRadius;
            minRadius = maxRadius;
            maxRadius = temp;
        }

        Location safeLocation = findSafeLocation(world, centerX, centerZ, minRadius, maxRadius, maxAttempts);

        if (safeLocation == null) {
            AdventureCompat.sendMessage(player, Component.text("Could not find a safe location after " + maxAttempts + " attempts. Try again.")
                    .color(NamedTextColor.RED));
            return true;
        }

        player.teleport(safeLocation);
        cooldownManager.setCooldown("rtp", player.getUniqueId(), cooldownSeconds);

        AdventureCompat.sendMessage(player, Component.text("Teleported to random location: ")
                .color(NamedTextColor.GREEN)
                .append(Component.text(safeLocation.getBlockX() + ", " + safeLocation.getBlockZ())
                        .color(NamedTextColor.YELLOW)));

        return true;
    }

    private Location findSafeLocation(World world, double centerX, double centerZ,
                                       double minRadius, double maxRadius, int maxAttempts) {
        Random random = new Random();
        org.bukkit.WorldBorder border = world.getWorldBorder();
        double borderCenterX = border.getCenter().getX();
        double borderCenterZ = border.getCenter().getZ();
        double borderHalfSize = border.getSize() / 2.0;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = minRadius + random.nextDouble() * (maxRadius - minRadius);

            double x = centerX + distance * Math.cos(angle);
            double z = centerZ + distance * Math.sin(angle);

            x = Math.max(borderCenterX - borderHalfSize + 1,
                    Math.min(borderCenterX + borderHalfSize - 1, x));
            z = Math.max(borderCenterZ - borderHalfSize + 1,
                    Math.min(borderCenterZ + borderHalfSize - 1, z));

            int blockX = (int) Math.floor(x);
            int blockZ = (int) Math.floor(z);

            int safeY = findSafeY(world, blockX, blockZ);
            if (safeY != -1) {
                return new Location(world, blockX + 0.5, safeY + 1, blockZ + 0.5,
                        random.nextFloat() * 360, 0);
            }
        }

        return null;
    }

    private int findSafeY(World world, int x, int z) {
        int maxY = world.getMaxHeight() - 2;
        int minY = world.getMinHeight();

        for (int y = maxY; y >= minY; y--) {
            Block ground = world.getBlockAt(x, y, z);
            Block feet = world.getBlockAt(x, y + 1, z);
            Block head = world.getBlockAt(x, y + 2, z);

            if (isSafeGround(ground) && isPassable(feet) && isPassable(head)) {
                return y;
            }
        }

        return -1;
    }

    private boolean isSafeGround(Block block) {
        Material type = block.getType();
        return type.isSolid()
                && type != Material.CACTUS
                && type != Material.MAGMA_BLOCK
                && type != Material.FIRE;
    }

    private boolean isPassable(Block block) {
        Material type = block.getType();
        return !type.isSolid()
                || type == Material.TALL_GRASS
                || type == Material.FERN
                || type == Material.DEAD_BUSH;
    }
}
