package me.crazyg.everything.listeners;

import me.crazyg.everything.Everything;
import me.clip.placeholderapi.PlaceholderAPI; // Import PlaceholderAPI
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class onJoinleaveListener implements Listener {

    private final Everything plugin;
    private final boolean papiEnabled; // Cache PAPI check for efficiency

    public onJoinleaveListener(Everything plugin) {
        this.plugin = plugin;
        // Check if PlaceholderAPI is enabled when the listener is created
        this.papiEnabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (!papiEnabled) {
            plugin.getLogger().info("PlaceholderAPI not found, using basic player name replacement for join/leave messages.");
        }
    }

    // No need for the ConsoleCommandSender field here anymore

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String format = plugin.getConfig().getString("messages.leave", ""); // Get format or empty string

        // If the format is empty in the config, disable the message
        if (format.isEmpty()) {
            event.setQuitMessage(null); // Setting to null uses the server default (or effectively disables if others do too)
            return;
        }

        String quitMessage = formatMessage(player, format);
        event.setQuitMessage(quitMessage);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String format;

        if (player.hasPlayedBefore()) {
            format = plugin.getConfig().getString("messages.join", ""); // Default to empty string
        } else {
            format = plugin.getConfig().getString("messages.first-join", ""); // Default to empty string
        }

        // If the format is empty in the config, disable the message
        if (format.isEmpty()) {
            event.setJoinMessage(null);
            return;
        }

        String joinMessage = formatMessage(player, format);
        event.setJoinMessage(joinMessage);
    }

    /**
     * Helper method to format messages, apply colors, and use PlaceholderAPI if available.
     *
     * @param player The player context for placeholders.
     * @param format The raw format string from the config.
     * @return The formatted message string, or null if the format was invalid/empty.
     */
    private String formatMessage(Player player, String format) {
        if (format == null || format.isEmpty()) {
            return null; // Should not happen if defaults are empty strings, but good practice
        }

        String message = format;

        // Apply PAPI placeholders if PAPI is enabled
        if (papiEnabled) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        } else {
            // Basic fallback if PAPI is not installed
            message = message.replace("%player_name%", player.getName())
                    .replace("%player_displayname%", player.getDisplayName());
            // Add more basic replacements here if needed
        }

        // Translate color codes AFTER PAPI has done its work
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}