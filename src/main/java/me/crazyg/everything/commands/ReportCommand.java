package me.crazyg.everything.commands;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;


public class ReportCommand implements CommandExecutor {

    private final Everything plugin;
    private File reportsFile;
    private FileConfiguration reportsConfig;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // --- Constant Messages ---
    private static final Component NEEDS_MORE_ARGS = Component.text("You need to specify a player to report and the reason.")
            .color(NamedTextColor.DARK_RED)
            .decorate(TextDecoration.BOLD);

    private static final Component CONSOLE_DENIED = Component.text("Command cannot be run by console, silly!")
            .color(NamedTextColor.DARK_RED)
            .decorate(TextDecoration.BOLD);

    private static final Component COMMAND_BLOCK_DENIED = Component.text("Command cannot be run by command block, L!")
            .color(NamedTextColor.DARK_RED)
            .decorate(TextDecoration.BOLD);

    private static final Component NO_REPORTS = Component.text("There are currently no pending reports.")
            .color(NamedTextColor.YELLOW);

    private static final Component REPORT_HEADER = Component.text("----- Pending Reports -----")
            .color(NamedTextColor.GOLD);

    private static final Component NO_VIEW_PERMISSION = Component.text("You do not have permission to view reports.")
            .color(NamedTextColor.RED);

    private static final String VIEW_REPORTS_PERMISSION = "everything.report.view";

    public ReportCommand(Everything plugin) {
        this.plugin = plugin;
        createReportsFile();
        loadReports();
    }

    private void createReportsFile() {
        reportsFile = new File(plugin.getDataFolder(), "reports.yml");
        if (!reportsFile.exists()) {
            reportsFile.getParentFile().mkdirs();
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
        int index = 0;
        for (Map<?, ?> reportData : reportsList) {
            Component reportLine = Component.text()
                .append(Component.text("- ").color(NamedTextColor.GRAY))
                .append(Component.text("Reported Player: ").color(NamedTextColor.YELLOW))
                .append(Component.text(String.valueOf(reportData.get("reported"))).color(NamedTextColor.WHITE))
                .append(Component.text(", Reporter: ").color(NamedTextColor.YELLOW))
                .append(Component.text(String.valueOf(reportData.get("reporter"))).color(NamedTextColor.AQUA))
                .append(Component.text(", Reason: ").color(NamedTextColor.YELLOW))
                .append(Component.text(String.valueOf(reportData.get("reason"))).color(NamedTextColor.WHITE))
                .build();

            // Only show buttons to players with permission
            if (sender instanceof Player p && p.hasPermission(VIEW_REPORTS_PERMISSION)) {
                Component acceptBtn = Component.text(" [Accept]")
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/report accept " + index))
                        .hoverEvent(HoverEvent.showText(Component.text("Accept this report")));
                Component denyBtn = Component.text(" [Deny]")
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/report deny " + index))
                        .hoverEvent(HoverEvent.showText(Component.text("Deny this report")));
                reportLine = Component.text().append(reportLine).append(acceptBtn).append(denyBtn).build();
            }
            sender.sendMessage(reportLine);
            index++;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player p) {
            if (args.length == 0) {
                if (p.hasPermission(VIEW_REPORTS_PERMISSION)) {
                    displayReports(sender);
                } else {
                    p.sendMessage(NO_VIEW_PERMISSION);
                }
            } else if (args.length == 2 && (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("deny"))) {
                // Accept or deny a report by index
                if (!p.hasPermission(VIEW_REPORTS_PERMISSION)) {
                    p.sendMessage(NO_VIEW_PERMISSION);
                    return true;
                }
                int index;
                try {
                    index = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    p.sendMessage(Component.text("Invalid report index.").color(NamedTextColor.RED));
                    return true;
                }
                List<Map<?, ?>> reportsList = reportsConfig.getMapList("reports");
                if (index < 0 || index >= reportsList.size()) {
                    p.sendMessage(Component.text("No report at that index.").color(NamedTextColor.RED));
                    return true;
                }
                Map<?, ?> report = reportsList.get(index);
                String reported = String.valueOf(report.get("reported"));
                String reporter = String.valueOf(report.get("reporter"));
                String reason = String.valueOf(report.get("reason"));
                String action = args[0].equalsIgnoreCase("accept") ? "accepted" : "denied";
                // Remove the report
                reportsList.remove(index);
                reportsConfig.set("reports", reportsList);
                saveReports();
                // Notify admin
                p.sendMessage(Component.text("Report for player '").color(NamedTextColor.GREEN)
                        .append(Component.text(reported).color(NamedTextColor.YELLOW))
                        .append(Component.text("' has been ").color(NamedTextColor.GREEN))
                        .append(Component.text(action).color(args[0].equalsIgnoreCase("accept") ? NamedTextColor.GREEN : NamedTextColor.RED))
                        .append(Component.text(".").color(NamedTextColor.GREEN)));
                // Optionally notify reporter if online
                Player reporterPlayer = Bukkit.getPlayerExact(reporter);
                if (reporterPlayer != null && reporterPlayer.isOnline()) {
                    reporterPlayer.sendMessage(Component.text("Your report against '").color(NamedTextColor.GOLD)
                        .append(Component.text(reported).color(NamedTextColor.YELLOW))
                        .append(Component.text("' was ").color(NamedTextColor.GOLD))
                        .append(Component.text(action).color(args[0].equalsIgnoreCase("accept") ? NamedTextColor.GREEN : NamedTextColor.RED))
                        .append(Component.text(" by an admin.").color(NamedTextColor.GOLD)));
                }
            } else if (args.length < 2) {
                p.sendMessage(NEEDS_MORE_ARGS);
            } else {
                String playerName = args[0];
                Player target = Bukkit.getServer().getPlayerExact(playerName);
                if (target == null) {
                    p.sendMessage(Component.text()
                        .append(Component.text("Player '").color(NamedTextColor.RED))
                        .append(Component.text(playerName))
                        .append(Component.text("' is not online.").color(NamedTextColor.RED))
                        .build());
                    return true;
                }
                StringBuilder reason = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    reason.append(args[i]).append(" ");
                }
                String reportMessage = reason.toString().trim();
                addReportToConfig(p.getName(), target.getName(), reportMessage);
                // Console notification
                Component consoleMessage = Component.text()
                    .append(Component.text("Report sent for player: ").color(NamedTextColor.GREEN))
                    .append(Component.text(target.getName()).color(NamedTextColor.YELLOW))
                    .append(Component.text(" Reported by: ").color(NamedTextColor.GREEN))
                    .append(Component.text(p.getName()).color(NamedTextColor.AQUA))
                    .append(Component.text(" Reason: ").color(NamedTextColor.GREEN))
                    .append(Component.text(reportMessage).color(NamedTextColor.WHITE))
                    .build();
                Bukkit.getConsoleSender().sendMessage(consoleMessage);
                p.sendMessage(Component.text()
                    .append(Component.text("Reported player ").color(NamedTextColor.GREEN))
                    .append(Component.text(target.getName()).color(NamedTextColor.YELLOW))
                    .append(Component.text(". Thank you for your report.").color(NamedTextColor.GREEN))
                    .build());
            }
        } else if (sender instanceof ConsoleCommandSender) {
            if (args.length == 0) {
                displayReports(sender);
            } else {
                sender.sendMessage(CONSOLE_DENIED);
            }
        } else if (sender instanceof BlockCommandSender) {
            sender.sendMessage(COMMAND_BLOCK_DENIED);
        }
        return true;
    }
}