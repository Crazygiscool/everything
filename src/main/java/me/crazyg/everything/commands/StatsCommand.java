package me.crazyg.everything.commands;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;


public class StatsCommand implements CommandExecutor {
    private final Everything plugin;
    private final File statsFile;
    private FileConfiguration statsConfig;
    private final Map<UUID, PlayerStats> playerStats;

    public StatsCommand(Everything plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        this.playerStats = new HashMap<>();
        loadStats();

        // Schedule periodic stats saving
        Bukkit.getScheduler().runTaskTimer(plugin, this::saveAllStats, 6000L, 6000L); // Save every 5 minutes
    }

    private void loadStats() {
        if (!statsFile.exists()) {
            plugin.saveResource("stats.yml", false);
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
    }

    private void saveAllStats() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerStats(player);
        }
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save stats file!");
        }
    }

    private void updatePlayerStats(Player player) {
        PlayerStats stats = playerStats.computeIfAbsent(player.getUniqueId(), 
            k -> new PlayerStats(player.getUniqueId()));
        stats.update(player);
        
        // Save to config
        String path = "players." + player.getUniqueId();
        statsConfig.set(path + ".name", player.getName());
        statsConfig.set(path + ".kills", stats.getKills());
        statsConfig.set(path + ".deaths", stats.getDeaths());
        statsConfig.set(path + ".playtime", stats.getPlayTime());
        statsConfig.set(path + ".blocksbroken", stats.getBlocksBroken());
        statsConfig.set(path + ".blocksplaced", stats.getBlocksPlaced());
        statsConfig.set(path + ".lastlogin", stats.getLastLogin());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!")
                    .color(NamedTextColor.RED));
            return true;
        }

        Player target;
        if (args.length > 0) {
            if (!player.hasPermission("everything.stats.others")) {
                player.sendMessage(Component.text("You don't have permission to view other players' stats!")
                        .color(NamedTextColor.RED));
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(Component.text("Player not found!")
                        .color(NamedTextColor.RED));
                return true;
            }
        } else {
            target = player;
        }

        showStats(player, target);
        return true;
    }

    private void showStats(Player viewer, Player target) {
        PlayerStats stats = playerStats.computeIfAbsent(target.getUniqueId(), 
            k -> new PlayerStats(target.getUniqueId()));
        stats.update(target);

        viewer.sendMessage(Component.text("=== Stats for " + target.getName() + " ===")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));

        // Format playtime
        long hours = stats.getPlayTime() / 72000L; // Convert ticks to hours
        long minutes = (stats.getPlayTime() % 72000L) / 1200L; // Convert remaining ticks to minutes

        viewer.sendMessage(Component.text()
                .append(Component.text("Playtime: ").color(NamedTextColor.YELLOW))
                .append(Component.text(hours + "h " + minutes + "m").color(NamedTextColor.WHITE)));
        
        viewer.sendMessage(Component.text()
                .append(Component.text("Kills: ").color(NamedTextColor.YELLOW))
                .append(Component.text(stats.getKills()).color(NamedTextColor.WHITE)));
        
        viewer.sendMessage(Component.text()
                .append(Component.text("Deaths: ").color(NamedTextColor.YELLOW))
                .append(Component.text(stats.getDeaths()).color(NamedTextColor.WHITE)));
        
        viewer.sendMessage(Component.text()
                .append(Component.text("K/D Ratio: ").color(NamedTextColor.YELLOW))
                .append(Component.text(String.format("%.2f", stats.getKDRatio())).color(NamedTextColor.WHITE)));
        
        viewer.sendMessage(Component.text()
                .append(Component.text("Blocks Broken: ").color(NamedTextColor.YELLOW))
                .append(Component.text(stats.getBlocksBroken()).color(NamedTextColor.WHITE)));
        
        viewer.sendMessage(Component.text()
                .append(Component.text("Blocks Placed: ").color(NamedTextColor.YELLOW))
                .append(Component.text(stats.getBlocksPlaced()).color(NamedTextColor.WHITE)));
    }

    private static class PlayerStats {
        private int kills;
        private int deaths;
        private long playTime;
        private int blocksBroken;
        private int blocksPlaced;
        private long lastLogin;

        public PlayerStats(UUID playerUUID) {
            this.lastLogin = Instant.now().getEpochSecond();
        }

        public void update(Player player) {
            this.kills = player.getStatistic(Statistic.PLAYER_KILLS);
            this.deaths = player.getStatistic(Statistic.DEATHS);
            this.playTime = player.getStatistic(Statistic.PLAY_ONE_MINUTE);

            // Sum all blocks broken
            int totalBroken = 0;
            for (Material mat : Material.values()) {
                if (mat.isBlock()) {
                    try {
                        totalBroken += player.getStatistic(Statistic.MINE_BLOCK, mat);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            this.blocksBroken = totalBroken;

            // Sum all blocks placed
            int totalPlaced = 0;
            for (Material mat : Material.values()) {
                if (mat.isBlock()) {
                    try {
                        totalPlaced += player.getStatistic(Statistic.USE_ITEM, mat);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            this.blocksPlaced = totalPlaced;

            this.lastLogin = Instant.now().getEpochSecond();
        }

        public int getKills() { return kills; }
        public int getDeaths() { return deaths; }
        public long getPlayTime() { return playTime; }
        public int getBlocksBroken() { return blocksBroken; }
        public int getBlocksPlaced() { return blocksPlaced; }
        public long getLastLogin() { return lastLogin; }
        public double getKDRatio() { return deaths == 0 ? kills : (double) kills / deaths; }
    }
}