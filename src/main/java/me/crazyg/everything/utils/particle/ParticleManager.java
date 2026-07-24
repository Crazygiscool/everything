package me.crazyg.everything.utils.particle;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import me.crazyg.everything.Everything;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class ParticleManager {
    private final Everything plugin;
    private final Set<String> disabledCommands = new HashSet<>();
    private boolean masterEnabled = true;

    public ParticleManager(Everything plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        masterEnabled = plugin.getConfig().getBoolean("particles.enabled", true);
        disabledCommands.clear();

        ConfigurationSection commands = plugin.getConfig().getConfigurationSection("particles.commands");
        if (commands != null) {
            for (String key : commands.getKeys(false)) {
                if (!commands.getBoolean(key, true)) {
                    disabledCommands.add(key.toLowerCase(Locale.ROOT));
                }
            }
        }
    }

    public boolean isEnabled(String commandName) {
        if (!masterEnabled) return false;
        return !disabledCommands.contains(commandName.toLowerCase(Locale.ROOT));
    }

    public void playEffect(Player player, ParticleEffect effect) {
        if (player == null || !player.isOnline()) return;
        player.getWorld().spawnParticle(
                effect.getParticle(),
                player.getLocation(),
                effect.getCount(),
                effect.getSpread(),
                effect.getSpread(),
                effect.getSpread(),
                effect.getSpeed()
        );
    }

    public void playEffectAt(Player player, ParticleEffect effect, Location location) {
        if (player == null || !player.isOnline()) return;
        player.getWorld().spawnParticle(
                effect.getParticle(),
                location,
                effect.getCount(),
                effect.getSpread(),
                effect.getSpread(),
                effect.getSpread(),
                effect.getSpeed()
        );
    }

    public void playColoredEffect(Player player, Particle particle,
                                  Color color, int count, double spread) {
        if (player == null || !player.isOnline()) return;
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.0f);
        player.getWorld().spawnParticle(
                particle,
                player.getLocation(),
                count,
                spread, spread, spread,
                0, dust
        );
    }

    public void playNameColorEffect(Player player, String colorName) {
        if (player == null || !player.isOnline()) return;
        Color color = hexToBukkitColor(colorName);
        if (color == null) return;
        playColoredEffect(player, Particle.REDSTONE, color, 25, 0.4);
    }

    public AnimatedEffect startLoop(Player player, Particle particle,
                                    int count, double spread, double speed,
                                    int intervalTicks) {
        if (player == null || !player.isOnline()) return null;
        return new AnimatedEffect(plugin, player, particle, count, spread, speed, intervalTicks);
    }

    public void drawLine(Player player, Location from, Location to,
                         Particle particle, double spacing) {
        if (player == null || !player.isOnline()) return;
        World world = from.getWorld();
        if (world == null) return;

        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance == 0) return;

        double steps = distance / spacing;
        double xStep = dx / steps;
        double yStep = dy / steps;
        double zStep = dz / steps;

        for (double i = 0; i <= steps; i++) {
            double x = from.getX() + (xStep * i);
            double y = from.getY() + (yStep * i);
            double z = from.getZ() + (zStep * i);
            world.spawnParticle(particle, new Location(world, x, y, z), 1, 0, 0, 0, 0);
        }
    }

    private Color hexToBukkitColor(String colorName) {
        if (colorName == null) return null;
        String upper = colorName.toUpperCase(Locale.ROOT);
        switch (upper) {
            case "RED": return Color.RED;
            case "BLUE": return Color.BLUE;
            case "GREEN": return Color.GREEN;
            case "YELLOW": return Color.YELLOW;
            case "WHITE": return Color.WHITE;
            case "BLACK": return Color.BLACK;
            case "PURPLE": return Color.PURPLE;
            case "DARK_PURPLE": return Color.PURPLE;
            case "AQUA": return Color.AQUA;
            case "DARK_AQUA": return Color.TEAL;
            case "GRAY": return Color.GRAY;
            case "DARK_GRAY": return Color.GRAY;
            case "DARK_RED": return Color.MAROON;
            case "DARK_BLUE": return Color.NAVY;
            case "DARK_GREEN": return Color.GREEN;
            case "GOLD": return Color.ORANGE;
            case "LIGHT_PURPLE": return Color.FUCHSIA;
            case "MAGIC": return Color.ORANGE;
            case "ORANGE": return Color.ORANGE;
            case "PINK": return Color.FUCHSIA;
            case "TEAL": return Color.TEAL;
            case "NAVY": return Color.NAVY;
            case "MAROON": return Color.MAROON;
            case "FUCHSIA": return Color.FUCHSIA;
            case "LIME": return Color.LIME;
            case "CYAN": return Color.TEAL;
            case "INDIGO": return Color.NAVY;
            default: return null;
        }
    }
}
