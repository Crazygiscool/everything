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

    private final File dataFile;
    private FileConfiguration dataConfig;

    private final Map<UUID, PlayerStats> playerStats;

    public StatsCommand(Everything plugin) {
        this.plugin = plugin;

        this.dataFile = new File(plugin.getDataFolder(), "stats.yml");
        this.playerStats = new HashMap<>();

        loadData();

        // Save stats every 5 minutes
        Bukkit.getScheduler().runTaskTimer(plugin, this::saveAllStats, 6000L, 6000L);
    }

    // ---------------------------------------------------------
    // Load stats.yml
    // ---------------------------------------------------------
    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create stats.yml!");
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // Ensure stats section exists
        if (!dataConfig.contains("stats.players")) {
            dataConfig.set("stats.players", new HashMap<>());
            saveData();
        }
    }

    private void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save stats.yml!");
        }
    }

    // ---------------------------------------------------------
    // Save all stats
    // ---------------------------------------------------------
    private void saveAllStats() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerStats(player);
        }
        saveData();
    }

    // ---------------------------------------------------------
    // Update stats for one player
    // ---------------------------------------------------------
    private void updatePlayerStats(Player player) {
        PlayerStats stats = playerStats.computeIfAbsent(
                player.getUniqueId(),
                k -> new PlayerStats(player.getUniqueId())
        );

        stats.update(player);

        String path = "stats.players." + player.getUniqueId();

        dataConfig.set(path + ".name", player.getName());
        dataConfig.set(path + ".kills", stats.getKills());
        dataConfig.set(path + ".deaths", stats.getDeaths());
        dataConfig.set(path + ".playtime", stats.getPlayTime());
        dataConfig.set(path + ".blocksbroken", stats.getBlocksBroken());
        dataConfig.set(path + ".blocksplaced", stats.getBlocksPlaced());
        dataConfig.set(path + ".lastlogin", stats.getLastLogin());
    }

    // ---------------------------------------------------------
    // Command Handling
    // ---------------------------------------------------------
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player viewer)) {
            sender.sendMessage(Component.text("Only players can use this command!")
                    .color(NamedTextColor.RED));
            return true;
        }

        Player target;

        if (args.length > 0) {
            if (!viewer.hasPermission("everything.stats.others")) {
                viewer.sendMessage(Component.text("You don't have permission to view other players' stats!")
                        .color(NamedTextColor.RED));
                return true;
            }

            target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                viewer.sendMessage(Component.text("Player not found!")
                        .color(NamedTextColor.RED));
                return true;
            }

        } else {
            target = viewer;
        }

        showStats(viewer, target);
        return true;
    }

    // ---------------------------------------------------------
    // Display Stats
    // ---------------------------------------------------------
    private void showStats(Player viewer, Player target) {

        PlayerStats stats = playerStats.computeIfAbsent(
                target.getUniqueId(),
                k -> new PlayerStats(target.getUniqueId())
        );

        stats.update(target);

        viewer.sendMessage(Component.text("=== Stats for " + target.getName() + " ===")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));

        long hours = stats.getPlayTime() / 72000L;
        long minutes = (stats.getPlayTime() % 72000L) / 1200L;

        viewer.sendMessage(Component.text("Playtime: ").color(NamedTextColor.YELLOW)
                .append(Component.text(hours + "h " + minutes + "m").color(NamedTextColor.WHITE)));

        viewer.sendMessage(Component.text("Kills: ").color(NamedTextColor.YELLOW)
                .append(Component.text(stats.getKills()).color(NamedTextColor.WHITE)));

        viewer.sendMessage(Component.text("Deaths: ").color(NamedTextColor.YELLOW)
                .append(Component.text(stats.getDeaths()).color(NamedTextColor.WHITE)));

        viewer.sendMessage(Component.text("K/D Ratio: ").color(NamedTextColor.YELLOW)
                .append(Component.text(String.format("%.2f", stats.getKDRatio())).color(NamedTextColor.WHITE)));

        viewer.sendMessage(Component.text("Blocks Broken: ").color(NamedTextColor.YELLOW)
                .append(Component.text(stats.getBlocksBroken()).color(NamedTextColor.WHITE)));

        viewer.sendMessage(Component.text("Blocks Placed: ").color(NamedTextColor.YELLOW)
                .append(Component.text(stats.getBlocksPlaced()).color(NamedTextColor.WHITE)));
    }

    // ---------------------------------------------------------
    // PlayerStats Inner Class
    // ---------------------------------------------------------
    private static class PlayerStats {

        private int kills;
        private int deaths;
        private long playTime;
        private int blocksBroken;
        private int blocksPlaced;
        private long lastLogin;

        public PlayerStats(UUID uuid) {
            this.lastLogin = Instant.now().getEpochSecond();
        }

        public void update(Player player) {

            this.kills = player.getStatistic(Statistic.PLAYER_KILLS);
            this.deaths = player.getStatistic(Statistic.DEATHS);
            this.playTime = player.getStatistic(Statistic.PLAY_ONE_MINUTE);

            int broken = 0;
            int placed = 0;

            for (Material mat : Material.values()) {
                if (mat.isBlock()) {
                    try {
                        broken += player.getStatistic(Statistic.MINE_BLOCK, mat);
                        placed += player.getStatistic(Statistic.USE_ITEM, mat);
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            this.blocksBroken = broken;
            this.blocksPlaced = placed;
            this.lastLogin = Instant.now().getEpochSecond();
        }

        public int getKills() { return kills; }
        public int getDeaths() { return deaths; }
        public long getPlayTime() { return playTime; }
        public int getBlocksBroken() { return blocksBroken; }
        public int getBlocksPlaced() { return blocksPlaced; }
        public long getLastLogin() { return lastLogin; }

        public double getKDRatio() {
            return deaths == 0 ? kills : (double) kills / deaths;
        }
    }
}
