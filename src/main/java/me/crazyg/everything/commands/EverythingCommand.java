package me.crazyg.everything.commands;
import java.util.*;
import me.crazyg.everything.Everything;
import me.crazyg.everything.utils.Updater;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;


public class EverythingCommand implements CommandExecutor {

    // Dynamically load command names from plugin.yml at runtime
    private List<String> getPluginCommands() {
        List<String> commandNames = new ArrayList<>();
        try {
            org.bukkit.configuration.file.YamlConfiguration yml = new org.bukkit.configuration.file.YamlConfiguration();
            java.io.InputStream in = plugin.getResource("plugin.yml");
            if (in != null) {
                java.io.InputStreamReader reader = new java.io.InputStreamReader(in);
                yml.load(reader);
                reader.close();
                in.close();
                if (yml.contains("commands")) {
                    org.bukkit.configuration.ConfigurationSection section = yml.getConfigurationSection("commands");
                    if (section != null) {
                        commandNames.addAll(section.getKeys(false));
                    }
                }
            }
        } catch (Exception e) {
            // fallback: do nothing, return empty list
        }
        return commandNames;
    }

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

            List<String> commandNames = getPluginCommands();
            if (!commandNames.isEmpty()) {
                for (String cmd : commandNames) {
                    org.bukkit.command.PluginCommand pluginCmd = plugin.getCommand(cmd);
                    String usage = pluginCmd != null ? pluginCmd.getUsage() : "";
                    String descText = pluginCmd != null ? pluginCmd.getDescription() : "";
                    StringBuilder line = new StringBuilder();
                    line.append(" /").append(cmd);
                    if (usage != null && !usage.isEmpty() && !usage.equalsIgnoreCase("/" + cmd)) {
                        String usageLine = usage.split("\n")[0].trim();
                        if (!usageLine.startsWith("/")) {
                            line.append(" ").append(usageLine);
                        }
                    }
                    if (descText != null && !descText.isEmpty()) {
                        line.append(" - ").append(descText);
                    }
                    sender.sendMessage(Component.text(line.toString()).color(NamedTextColor.WHITE));
                }
            } else {
                sender.sendMessage(Component.text("No commands found for this plugin.").color(NamedTextColor.RED));
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
                sender.sendMessage(Component.text("[TEST] Listing and attempting to run all commands:").color(NamedTextColor.AQUA));
                List<String> testCommands = getPluginCommands();
                for (String cmd : testCommands) {
                    org.bukkit.command.PluginCommand pluginCmd = plugin.getCommand(cmd);
                    if (pluginCmd == null) {
                        sender.sendMessage(Component.text("/" + cmd + " (Not registered)").color(NamedTextColor.RED));
                        continue;
                    }
                    String usage = pluginCmd.getUsage();
                    String descText = pluginCmd.getDescription();
                    StringBuilder line = new StringBuilder();
                    line.append("/").append(cmd);
                    if (usage != null && !usage.isEmpty() && !usage.equalsIgnoreCase("/" + cmd)) {
                        String usageLine = usage.split("\n")[0].trim();
                        if (!usageLine.startsWith("/")) {
                            line.append(" ").append(usageLine);
                        }
                    }
                    if (descText != null && !descText.isEmpty()) {
                        line.append(" - ").append(descText);
                    }
                    sender.sendMessage(Component.text(line.toString()).color(NamedTextColor.GRAY));
                    // Try to run the command with the sender as the player if possible
                    try {
                        String[] testArgs = new String[0];
                        // Only add player argument if <player> or [player] is a standalone argument
                        boolean needsPlayer = false;
                        if (usage != null && !usage.isEmpty()) {
                            String[] usageParts = usage.replace("/" + cmd, "").trim().split(" ");
                            for (String part : usageParts) {
                                if (part.equalsIgnoreCase("<player>") || part.equalsIgnoreCase("[player]")) {
                                    needsPlayer = true;
                                    break;
                                }
                            }
                        }
                        if (needsPlayer) {
                            if (sender instanceof org.bukkit.entity.Player) {
                                testArgs = new String[] { ((org.bukkit.entity.Player)sender).getName() };
                            } else {
                                sender.sendMessage(Component.text("(Skipped: Needs player context)").color(NamedTextColor.DARK_GRAY));
                                continue;
                            }
                        }
                        pluginCmd.execute(sender, cmd, testArgs);
                        sender.sendMessage(Component.text("(Executed)").color(NamedTextColor.DARK_GREEN));
                    } catch (Exception ex) {
                        sender.sendMessage(Component.text("(Error executing: " + ex.getMessage() + ")").color(NamedTextColor.RED));
                    }
                }
                return true;

            default:
                sender.sendMessage(Component.text("Unknown sub-command. Use /everything for help.")
                    .color(NamedTextColor.RED));
                return true;
        }
    }
}