package me.crazyg.everything.listeners;

import me.crazyg.everything.Everything;
import me.crazyg.everything.blocklog.SelectionCommand;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class SlashPrefixTabCompleteListener implements Listener {

    private static final Map<String, String> COMMANDS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    static {
        COMMANDS.put("wand", "wand");
        COMMANDS.put("pos1", "pos1");
        COMMANDS.put("pos2", "pos2");
        COMMANDS.put("hpos1", "hpos1");
        COMMANDS.put("hpos2", "hpos2");
        COMMANDS.put("expand", "expand");
        COMMANDS.put("contract", "contract");
        COMMANDS.put("outset", "outset");
        COMMANDS.put("inset", "inset");
        COMMANDS.put("shift", "shift");
        COMMANDS.put("sel", "sel");
        COMMANDS.put("desel", "sel");
        COMMANDS.put("size", "size");
        COMMANDS.put("count", "count");
    }

    private final Everything plugin;
    private final SelectionCommand selectionCommand;

    public SlashPrefixTabCompleteListener(Everything plugin,
                                           SelectionCommand selectionCommand) {
        this.plugin = plugin;
        this.selectionCommand = selectionCommand;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player player)) {
            return;
        }
        if (!player.hasPermission("everything.blocklog.select")) {
            return;
        }

        String buffer = event.getBuffer();
        if (buffer == null) {
            return;
        }

        List<String> completions = completionsFor(plugin, selectionCommand, player, buffer);
        if (completions != null) {
            event.setCompletions(completions);
        }
    }

    public static List<String> completionsFor(Everything plugin,
                                            SelectionCommand selectionCommand,
                                            Player player,
                                            String buffer) {
        // Explicitly leave three-slash input alone.
        if (buffer == null || buffer.startsWith("///")) {
            return null;
        }

        String raw = buffer.trim();
        boolean doubleSlash = raw.startsWith("//");
        String content = doubleSlash ? raw.substring(2) : raw.startsWith("/") ? raw.substring(1) : raw;
        String[] parts = content.split("\\s+", -1);
        String commandLabel = parts[0].toLowerCase(Locale.ROOT);

        String canonical = COMMANDS.get(commandLabel);
        if (canonical != null) {
            if (parts.length > 1) {
                PluginCommand command = plugin.getCommand(canonical);
                if (command != null) {
                    String[] args = java.util.Arrays.copyOfRange(parts, 1, parts.length);
                    try {
                        List<String> choices = command.tabComplete(player, canonical, args);
                        if (choices != null) {
                            return choices;
                        }
                    } catch (Exception ignored) {
                    }
                }
                return List.of();
            }
            return List.of("//" + commandLabel);
        }

        // Turn typed prefixes into a concrete double-slash suggestion. This
        // prevents the generic command map from revealing single-slash names.
        List<String> suggestions = new ArrayList<>();
        for (String name : COMMANDS.keySet()) {
            if (name.startsWith(commandLabel)) {
                suggestions.add("//" + name);
            }
        }
        if (!suggestions.isEmpty()) {
            return suggestions;
        }
        return null;
    }
}
