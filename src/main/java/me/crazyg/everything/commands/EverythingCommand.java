package me.crazyg.everything.commands;
import java.util.*;
import me.crazyg.everything.Everything;
import me.crazyg.everything.utils.Updater;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;


public class EverythingCommand implements CommandExecutor {

    private final Everything plugin;
    private final Updater updater;

    public EverythingCommand(Everything plugin) {
        this.plugin = plugin;
        this.updater = new Updater(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Everything Plugin - Help").color(NamedTextColor.GOLD));
            sender.sendMessage(Component.text("Available Commands:").color(NamedTextColor.YELLOW));

            // Dynamically list all plugin commands and their usage
            PluginDescriptionFile desc = plugin.getDescription();
            Map<String, Map<String, Object>> commands = desc.getCommands();

            if (commands != null) {
                for (Map.Entry<String, Map<String, Object>> entry : commands.entrySet()) {
                    String cmd = entry.getKey();
                    Map<String, Object> meta = entry.getValue();
                    String usage = meta.getOrDefault("usage", "").toString();
                    String descText = meta.getOrDefault("description", "").toString();

                    StringBuilder line = new StringBuilder();
                    line.append(" /").append(cmd);
                    if (!usage.isEmpty()) {
                        // Only show the first line of usage for brevity
                        String usageLine = usage.split("\n")[0].trim();
                        if (!usageLine.startsWith("/")) {
                            line.append(" ").append(usageLine);
                        }
                    }
                    if (!descText.isEmpty()) {
                        line.append(" - ").append(descText);
                    }
                    sender.sendMessage(Component.text(line.toString()).color(NamedTextColor.WHITE));
                }
            } else {
                sender.sendMessage(Component.text("No commands found in plugin.yml.").color(NamedTextColor.RED));
            }
            return true;
        }

        // Handle sub-commands
        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("everything.reload")) {
                    sender.sendMessage(Component.text("You do not have permission to use this command.")
                        .color(NamedTextColor.RED));
                    return true;
                }
                plugin.reloadConfig();
                sender.sendMessage(Component.text("Plugin configuration reloaded successfully!")
                    .color(NamedTextColor.GREEN));
                return true;

            case "info":
                sender.sendMessage(Component.text("Everything Plugin - Info")
                    .color(NamedTextColor.GOLD));
                
                // Use modern methods to get plugin info
                sender.sendMessage(Component.text()
                    .append(Component.text("Version: ").color(NamedTextColor.YELLOW))
                    .append(Component.text(plugin.getPluginMeta().getVersion()).color(NamedTextColor.WHITE)));
                sender.sendMessage(Component.text()
                    .append(Component.text("Author: ").color(NamedTextColor.YELLOW))
                    .append(Component.text(String.join(", ", plugin.getPluginMeta().getAuthors())).color(NamedTextColor.WHITE)));
                sender.sendMessage(Component.text()
                    .append(Component.text("Description: ").color(NamedTextColor.YELLOW))
                    .append(Component.text(plugin.getPluginMeta().getDescription()).color(NamedTextColor.WHITE)));
                return true;

            case "checkupdate":
                if (!sender.hasPermission("everything.admin")) {
                    sender.sendMessage(Component.text("You don't have permission to check for updates!")
                        .color(NamedTextColor.RED));
                    return true;
                }
                
                if (updater.isUpdateAvailable()) {
                    sender.sendMessage(Component.text()
                        .append(Component.text("A new version is available! ").color(NamedTextColor.GREEN))
                        .append(Component.text("Current: ").color(NamedTextColor.YELLOW))
                        .append(Component.text(plugin.getPluginMeta().getVersion()).color(NamedTextColor.WHITE))
                        .append(Component.text(" Latest: ").color(NamedTextColor.YELLOW))
                        .append(Component.text(updater.getLatestVersion()).color(NamedTextColor.WHITE)));
                } else {
                    sender.sendMessage(Component.text("You are running the latest version!")
                        .color(NamedTextColor.GREEN));
                }
                return true;

            case "test":
                PluginDescriptionFile desc = plugin.getDescription();
                Map<String, Map<String, Object>> commands = desc.getCommands();
                sender.sendMessage(Component.text("[TEST] Listing and attempting to run all commands:").color(NamedTextColor.AQUA));
                if (commands != null) {
                    for (Map.Entry<String, Map<String, Object>> entry : commands.entrySet()) {
                        String cmd = entry.getKey();
                        Map<String, Object> meta = entry.getValue();
                        String usage = meta.getOrDefault("usage", "").toString();
                        String descText = meta.getOrDefault("description", "").toString();
                        StringBuilder line = new StringBuilder();
                        line.append("/" + cmd);
                        if (!usage.isEmpty()) {
                            String usageLine = usage.split("\n")[0].trim();
                            if (!usageLine.startsWith("/")) {
                                line.append(" ").append(usageLine);
                            }
                        }
                        if (!descText.isEmpty()) {
                            line.append(" - ").append(descText);
                        }
                        sender.sendMessage(Component.text(line.toString()).color(NamedTextColor.GRAY));
                        // Try to run the command with the sender as the player if possible
                        try {
                            String[] testArgs = new String[0];
                            if (usage.contains("<player>") || usage.contains("[player]")) {
                                if (sender instanceof org.bukkit.entity.Player) {
                                    testArgs = new String[] { ((org.bukkit.entity.Player)sender).getName() };
                                } else {
                                    sender.sendMessage(Component.text("(Skipped: Needs player context)").color(NamedTextColor.DARK_GRAY));
                                    continue;
                                }
                            }
                            plugin.getCommand(cmd).execute(sender, cmd, testArgs);
                            sender.sendMessage(Component.text("(Executed)").color(NamedTextColor.DARK_GREEN));
                        } catch (Exception ex) {
                            sender.sendMessage(Component.text("(Error executing: " + ex.getMessage() + ")").color(NamedTextColor.RED));
                        }
                    }
                } else {
                    sender.sendMessage(Component.text("No commands found in plugin.yml.").color(NamedTextColor.RED));
                }
                return true;

            default:
                sender.sendMessage(Component.text("Unknown sub-command. Use /everything for help.")
                    .color(NamedTextColor.RED));
                return true;
        }
    }
}