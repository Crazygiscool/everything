package me.crazyg.everything.utils.particle;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class AnimatedEffect {
    private final BukkitTask task;

    public AnimatedEffect(JavaPlugin plugin, Player player, Particle particle,
                          int count, double spread, double speed, int intervalTicks) {
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stop();
                return;
            }
            player.getWorld().spawnParticle(particle, player.getLocation(),
                    count, spread, spread, spread, speed);
        }, 0L, intervalTicks);
    }

    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    public boolean isRunning() {
        return task != null && !task.isCancelled();
    }
}
