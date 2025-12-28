package me.crazyg.everything.commands;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class ReportCommand implements CommandExecutor, TabCompleter{

    private final Everything plugin;

    private final File dataFile;
    private FileConfiguration dataConfig;

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

        // Ensure /plugins/Everything/data/ exists
        File folder = new File(plugin.getDataFolder(), "data");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // Extract report.yml from resources if missing
        File reportResource = new File(folder, "report.yml");
        if (!reportResource.exists()) {
            // Path inside your JAR: src/main/resources/data/report.yml
            plugin.saveResource("data/report.yml", false);
        }

        this.dataFile = reportResource;
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // Ensure "reports" list exists
        if (!dataConfig.contains("reports")) {
            dataConfig.set("reports", new java.util.ArrayList<>());
            saveData();
        }
    }

    // -----------------------------
    // Save report.yml
    // -----------------------------
    private void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save report.yml!");
        }
    }

    // -----------------------------
    // Add Report
    // -----------------------------
    private void addReportToConfig(String reporterName, String reportedName, String reason) {
        List<Map<?, ?>> reportsList = dataConfig.getMapList("reports");

        Map<String, String> newReport = Map.of(
                "reporter", reporterName,
                "reported", reportedName,
                "reason", reason,
                "timestamp", TIMESTAMP_FORMAT.format(LocalDateTime.now())
        );

        reportsList.add(newReport);
        dataConfig.set("reports", reportsList);
        saveData();
    }

    // -----------------------------
    // Display Reports
    // -----------------------------
    private void displayReports(CommandSender sender) {
        List<Map<?, ?>> reportsList = dataConfig.getMapList("reports");

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

    // -----------------------------
    // Command Handling
    // -----------------------------
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player p) {

            if (args.length == 0) {
                if (p.hasPermission(VIEW_REPORTS_PERMISSION)) {
                    displayReports(sender);
                } else {
                    p.sendMessage(NO_VIEW_PERMISSION);
                }
                return true;
            }

            // Accept / Deny
            if (args.length == 2 && (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("deny"))) {

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

                List<Map<?, ?>> reportsList = dataConfig.getMapList("reports");

                if (index < 0 || index >= reportsList.size()) {
                    p.sendMessage(Component.text("No report at that index.").color(NamedTextColor.RED));
                    return true;
                }

                Map<?, ?> report = reportsList.get(index);
                String reported = String.valueOf(report.get("reported"));
                String reporter = String.valueOf(report.get("reporter"));
                String reason = String.valueOf(report.get("reason"));
                String action = args[0].equalsIgnoreCase("accept") ? "accepted" : "denied";

                // Remove report
                reportsList.remove(index);
                dataConfig.set("reports", reportsList);
                saveData();

                // Notify admin
                p.sendMessage(Component.text("Report for player '").color(NamedTextColor.GREEN)
                        .append(Component.text(reported).color(NamedTextColor.YELLOW))
                        .append(Component.text("' has been ").color(NamedTextColor.GREEN))
                        .append(Component.text(action).color(args[0].equalsIgnoreCase("accept") ? NamedTextColor.GREEN : NamedTextColor.RED))
                        .append(Component.text(".").color(NamedTextColor.GREEN)));

                // Notify reporter if online
                Player reporterPlayer = Bukkit.getPlayerExact(reporter);
                if (reporterPlayer != null && reporterPlayer.isOnline()) {
                    reporterPlayer.sendMessage(Component.text("Your report against '").color(NamedTextColor.GOLD)
                            .append(Component.text(reported).color(NamedTextColor.YELLOW))
                            .append(Component.text("' was ").color(NamedTextColor.GOLD))
                            .append(Component.text(action).color(args[0].equalsIgnoreCase("accept") ? NamedTextColor.GREEN : NamedTextColor.RED))
                            .append(Component.text(" by an admin.").color(NamedTextColor.GOLD)));
                }

                return true;
            }

            // Not enough args
            if (args.length < 2) {
                p.sendMessage(NEEDS_MORE_ARGS);
                return true;
            }

            // Create report
            String playerName = args[0];
            OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);

            if (!target.hasPlayedBefore() && !target.isOnline()) {
                p.sendMessage(Component.text("Player '" + playerName + "' has never joined before.")
                        .color(NamedTextColor.RED));
                return true;
            }

            String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

            addReportToConfig(p.getName(), target.getName(), reason);

            // Console log
            Bukkit.getConsoleSender().sendMessage(
                    Component.text("Report sent for player: ").color(NamedTextColor.GREEN)
                            .append(Component.text(target.getName()).color(NamedTextColor.YELLOW))
                            .append(Component.text(" Reported by: ").color(NamedTextColor.GREEN))
                            .append(Component.text(p.getName()).color(NamedTextColor.AQUA))
                            .append(Component.text(" Reason: ").color(NamedTextColor.GREEN))
                            .append(Component.text(reason).color(NamedTextColor.WHITE))
            );

            p.sendMessage(Component.text("Reported player ")
                    .color(NamedTextColor.GREEN)
                    .append(Component.text(target.getName()).color(NamedTextColor.YELLOW))
                    .append(Component.text(". Thank you for your report.").color(NamedTextColor.GREEN)));

            return true;

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

    // ----------------------------------------------------
    // TAB COMPLETION
    // ----------------------------------------------------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        // /report
        if (args.length == 1) {
            String input = args[0].toLowerCase();

            // Base subcommands
            List<String> base = List.of("accept", "deny", "list");

            // Player names (for creating a report)
            List<String> players = Arrays.stream(Bukkit.getOfflinePlayers())
                    .map(OfflinePlayer::getName)
                    .filter(Objects::nonNull)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .toList();


            // Subcommands that match
            List<String> matches = base.stream()
                    .filter(s -> s.startsWith(input))
                    .toList();

            // Merge both
            List<String> result = new java.util.ArrayList<>();
            result.addAll(matches);
            result.addAll(players);
            return result;
        }

        // /report accept <index>
        if (args.length == 2 && args[0].equalsIgnoreCase("accept")) {
            return getReportIndexSuggestions(args[1]);
        }

        // /report deny <index>
        if (args.length == 2 && args[0].equalsIgnoreCase("deny")) {
            return getReportIndexSuggestions(args[1]);
        }

        // /report <player> <reason...>
        if (args.length >= 2) {
            return List.of(); // free‑form reason
        }

        return List.of();
    }

    // Helper to list report indices
    private List<String> getReportIndexSuggestions(String input) {
        List<Map<?, ?>> reports = dataConfig.getMapList("reports");

        return java.util.stream.IntStream.range(0, reports.size())
                .mapToObj(String::valueOf)
                .filter(s -> s.startsWith(input))
                .toList();
    }
}
