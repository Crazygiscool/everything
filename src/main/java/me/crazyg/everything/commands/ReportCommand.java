package me.crazyg.everything.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import me.crazyg.everything.Everything; // Import your main plugin class
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReportCommand implements CommandExecutor{

    private final Everything plugin; // Store a reference to your main plugin
    private File reportsFile;
    private FileConfiguration reportsConfig;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // --- Constant Messages ---
    private static final String NEEDS_MORE_ARGS = ChatColor.DARK_RED + "" + ChatColor.BOLD + "You need to specify a player to report and the reason.";
    private static final String CONSOLE_DENIED = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Command cannot be run by console, silly!";
    private static final String COMMAND_BLOCK_DENIED = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Command cannot be run by command block, L!";
    private static final String PLAYER_NOT_FOUND = ChatColor.RED + "Player '";
    private static final String PLAYER_NOT_FOUND_SUFFIX = "' is not online.";
    private static final String REPORT_SENT_PREFIX = ChatColor.GREEN + "Report sent for player: " + ChatColor.YELLOW;
    private static final String BY_PREFIX = ChatColor.GREEN + " Reported by: " + ChatColor.AQUA;
    private static final String REPORT_REASON_PREFIX = ChatColor.GREEN + " Reason: " + ChatColor.WHITE;
    private static final String NO_REPORTS = ChatColor.YELLOW + "There are currently no pending reports.";
    private static final String REPORT_HEADER = ChatColor.GOLD + "----- Pending Reports -----";
    private static final String REPORT_FORMAT = ChatColor.GRAY + "- " +ChatColor.YELLOW + "Reported Player: " + ChatColor.WHITE + "%reported%" + ChatColor.YELLOW + ", Reporter: " + ChatColor.AQUA + "%reporter%" + ChatColor.YELLOW + ", Reason: " + ChatColor.WHITE + "%reason%";
    private static final String VIEW_REPORTS_PERMISSION = "everything.report.view";
    private static final String NO_VIEW_PERMISSION = ChatColor.RED + "You do not have permission to view reports.";

    public ReportCommand(Everything plugin) {
        this.plugin = plugin;
        createReportsFile();
        loadReports();
    }

    private void createReportsFile() {
        reportsFile = new File(plugin.getDataFolder(), "reports.yml");
        if (!reportsFile.exists()) {
            reportsFile.getParentFile().mkdirs(); // Ensure directory exists
            try {
                reportsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create reports.yml!");
                e.printStackTrace();
            }
        }
    }

    private void loadReports() {
        reportsConfig = YamlConfiguration.loadConfiguration(reportsFile);
    }

    private void saveReports() {
        try {
            reportsConfig.save(reportsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save reports.yml!");
            e.printStackTrace();
        }
    }

    private void addReportToConfig(String reporterName, String reportedName, String reason) {
        List<Map<?, ?>> reportsList = reportsConfig.getMapList("reports");
        Map<String, String> newReport = Map.of(
                "reporter", reporterName,
                "reported", reportedName,
                "reason", reason,
                "timestamp", TIMESTAMP_FORMAT.format(LocalDateTime.now())
        );
        reportsList.add(newReport);
        reportsConfig.set("reports", reportsList);
        saveReports();
    }

    private void displayReports(CommandSender sender) {
        List<Map<?, ?>> reportsList = reportsConfig.getMapList("reports");

        if (reportsList.isEmpty()) {
            sender.sendMessage(NO_REPORTS);
            return;
        }

        sender.sendMessage(REPORT_HEADER);
        for (Map<?, ?> reportData : reportsList) {
            String formattedReportLine = REPORT_FORMAT
                    .replace("%reporter%", String.valueOf(reportData.get("reporter")))
                    .replace("%reported%", String.valueOf(reportData.get("reported")))
                    .replace("%reason%", String.valueOf(reportData.get("reason")));
            sender.sendMessage(formattedReportLine);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if (sender instanceof Player p){
            if (args.length == 0) {
                // No arguments - Staff viewing reports
                if (p.hasPermission(VIEW_REPORTS_PERMISSION)) {
                    displayReports(sender);
                } else {
                    p.sendMessage(NO_VIEW_PERMISSION);
                }
            } else if (args.length < 2) {
                // One argument only (player name, but no reason)
                p.sendMessage(NEEDS_MORE_ARGS);
            } else {
                // Reporting a player
                String playerName = args[0];
                Player target = Bukkit.getServer().getPlayerExact(playerName);

                if (target == null) {
                    p.sendMessage(PLAYER_NOT_FOUND + playerName + PLAYER_NOT_FOUND_SUFFIX);
                    return true;
                }

                StringBuilder reportMessageBuilder = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    reportMessageBuilder.append(args[i]).append(" ");
                }
                String reportMessage = reportMessageBuilder.toString().trim();

                // Store the report in reports.yml
                addReportToConfig(p.getName(), target.getName(), reportMessage);

                // Send console notification (still keep this)
                String formattedReport = REPORT_SENT_PREFIX + target.getName() +
                        BY_PREFIX + p.getName() +
                        REPORT_REASON_PREFIX + reportMessage;
                Bukkit.getConsoleSender().sendMessage(formattedReport);

                p.sendMessage(ChatColor.GREEN + "Reported player " + ChatColor.YELLOW + target.getName() + ChatColor.GREEN + ". Thank you for your report.");
            }
        } else if (sender instanceof ConsoleCommandSender consoleSender) {
            if (args.length == 0) {
                // Console viewing reports
                displayReports(sender); // Console can view all reports directly
            } else {
                consoleSender.sendMessage(CONSOLE_DENIED); // Console can't *make* reports
            }
        } else if (sender instanceof BlockCommandSender blockSender) {
            blockSender.sendMessage(COMMAND_BLOCK_DENIED); // Command blocks can't use /report at all
        }
        return true;
    }
}