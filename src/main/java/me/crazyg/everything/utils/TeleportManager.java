package me.crazyg.everything.utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.crazyg.everything.Everything;
import me.crazyg.everything.utils.particle.AnimatedEffect;
import me.crazyg.everything.utils.particle.ParticleEffect;
import me.crazyg.everything.utils.particle.ParticleManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

public class TeleportManager implements Listener {
    private final Everything plugin;
    private final CooldownManager cooldownManager;
    private final ParticleManager particleManager;
    private final Map<UUID, BukkitTask> pendingWarmups = new HashMap<>();
    private final Map<UUID, Location> warmupOrigins = new ConcurrentHashMap<>();
    private final Map<UUID, AnimatedEffect> warmupParticles = new HashMap<>();

    public TeleportManager(Everything plugin, ParticleManager particleManager) {
        this.plugin = plugin;
        this.cooldownManager = new CooldownManager();
        this.particleManager = particleManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public void teleport(Player player, Location destination, String commandName) {
        if (player == null || destination == null) return;

        String cmd = commandName.toLowerCase(Locale.ROOT);
        int warmup = getConfigInt("teleport.warmup-seconds." + cmd, 0);
        int cooldown = getConfigInt("teleport.cooldown-seconds." + cmd, 0);
        boolean cancelOnMove = getConfigBoolean("teleport.cancel-on-move." + cmd, false);
        boolean checkSafety = getConfigBoolean("teleport.check-safety." + cmd, false);
        boolean useParticles = particleManager.isEnabled(cmd);
        UUID uuid = player.getUniqueId();

        if (!player.hasPermission("everything." + cmd + ".bypasscooldown")
                && !player.hasPermission("everything.rtp.bypasscooldown")) {
            if (cooldownManager.hasCooldownRaw(cmd, uuid)) {
                long remaining = cooldownManager.getRemainingSeconds(cmd, uuid);
                String cooldownMsg = getConfigString("teleport.messages.cooldown",
                        "<red>You must wait <gold>%time%<red> seconds before using this again.");
                AdventureCompat.sendMessage(player,
                        AdventureCompat.deserialize(cooldownMsg.replace("%time%", String.valueOf(remaining))));
                return;
            }
        }

        if (checkSafety && !isLocationSafe(destination)) {
            String msg = getConfigString("teleport.messages.safety-failed",
                    "<red>Destination is not safe (inside block or hazard). Try again.");
            AdventureCompat.sendMessage(player, AdventureCompat.deserialize(msg));
            return;
        }

        if (warmup > 0) {
            startWarmup(player, destination, cmd, warmup, cooldown, cancelOnMove, useParticles);
        } else {
            completeTeleport(player, destination, cmd, cooldown, useParticles);
        }
    }

    private void startWarmup(Player player, Location destination, String cmd,
                             int warmupSeconds, int cooldownSeconds,
                             boolean cancelOnMove, boolean useParticles) {
        UUID uuid = player.getUniqueId();

        cancelWarmup(uuid);

        warmupOrigins.put(uuid, player.getLocation());

        if (useParticles) {
            AnimatedEffect effect = particleManager.startLoop(
                    player, ParticleEffect.TELEPORT_WARMUP.getParticle(),
                    ParticleEffect.TELEPORT_WARMUP.getCount(),
                    ParticleEffect.TELEPORT_WARMUP.getSpread(),
                    ParticleEffect.TELEPORT_WARMUP.getSpeed(), 5
            );
            warmupParticles.put(uuid, effect);
        }

        String startMsg = getConfigString("teleport.messages.warmup-start",
                "<yellow>Teleporting in <gold>%time%<yellow> seconds... Don't move!");
        AdventureCompat.sendMessage(player,
                AdventureCompat.deserialize(startMsg.replace("%time%", String.valueOf(warmupSeconds))));

        final int[] remaining = {warmupSeconds};

        BukkitTask tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                cancelWarmup(uuid);
                return;
            }

            remaining[0]--;

            if (remaining[0] <= 0) {
                cancelWarmup(uuid);
                completeTeleport(player, destination, cmd, cooldownSeconds, useParticles);
                return;
            }

            String tickMsg = getConfigString("teleport.messages.warmup-tick",
                    "<yellow>Teleporting in <gold>%time%<yellow> seconds...");
            AdventureCompat.sendMessage(player,
                    AdventureCompat.deserialize(tickMsg.replace("%time%", String.valueOf(remaining[0]))));

        }, 20L, 20L);

        pendingWarmups.put(uuid, tickTask);
    }

    private void completeTeleport(Player player, Location destination, String cmd,
                                  int cooldownSeconds, boolean useParticles) {
        UUID uuid = player.getUniqueId();

        cancelWarmup(uuid);

        player.teleport(destination);

        if (cooldownSeconds > 0) {
            cooldownManager.setCooldown(cmd, uuid, cooldownSeconds);
        }

        if (useParticles) {
            particleManager.playEffect(player, ParticleEffect.TELEPORT_ARRIVE);
        }

        String completeMsg = getConfigString("teleport.messages.warmup-complete",
                "<green>Teleported!");
        AdventureCompat.sendMessage(player, AdventureCompat.deserialize(completeMsg));
    }

    public void cancelWarmup(UUID uuid) {
        BukkitTask task = pendingWarmups.remove(uuid);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
        warmupOrigins.remove(uuid);
        AnimatedEffect effect = warmupParticles.remove(uuid);
        if (effect != null) {
            effect.stop();
        }
    }

    public boolean isLocationSafe(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        World world = loc.getWorld();

        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);

        return isPassable(feet) && isPassable(head) && !isInHazard(feet) && !isInHazard(head);
    }

    private boolean isPassable(Block block) {
        Material type = block.getType();
        return type == Material.AIR
                || type == Material.CAVE_AIR
                || type == Material.VOID_AIR
                || type == Material.TALL_GRASS
                || type == Material.FERN
                || type == Material.DEAD_BUSH
                || type == Material.SHORT_GRASS
                || type == Material.SNOW;
    }

    private boolean isInHazard(Block block) {
        Material type = block.getType();
        return type == Material.LAVA
                || type == Material.FIRE
                || type == Material.SOUL_FIRE
                || type == Material.CACTUS
                || type == Material.MAGMA_BLOCK;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!pendingWarmups.containsKey(uuid)) return;

        Location from = warmupOrigins.get(uuid);
        if (from == null) return;

        Location to = event.getTo();
        if (to == null) return;

        if (from.getWorld() != to.getWorld()
                || from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()) {

            String cmd = getCommandForWarmup(uuid);
            boolean cancelOnMove = getConfigBoolean("teleport.cancel-on-move." + cmd, false);

            if (cancelOnMove) {
                cancelWarmup(uuid);

                Player player = event.getPlayer();
                particleManager.playEffect(player, ParticleEffect.TELEPORT_CANCEL);

                String cancelMsg = getConfigString("teleport.messages.warmup-cancel",
                        "<red>Teleport cancelled — you moved!");
                AdventureCompat.sendMessage(player, AdventureCompat.deserialize(cancelMsg));
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cancelWarmup(event.getPlayer().getUniqueId());
    }

    private String getCommandForWarmup(UUID uuid) {
        for (Map.Entry<UUID, BukkitTask> entry : pendingWarmups.entrySet()) {
            if (entry.getKey().equals(uuid)) {
                return "unknown";
            }
        }
        return "unknown";
    }

    private int getConfigInt(String path, int def) {
        return plugin.getConfig().getInt(path, def);
    }

    private boolean getConfigBoolean(String path, boolean def) {
        return plugin.getConfig().getBoolean(path, def);
    }

    private String getConfigString(String path, String def) {
        return plugin.getConfig().getString(path, def);
    }
}
