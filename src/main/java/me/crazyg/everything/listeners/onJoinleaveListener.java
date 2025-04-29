package me.crazyg.everything.listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class onJoinleaveListener implements Listener {

    private final Everything plugin;
    private final boolean papiEnabled;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public onJoinleaveListener(Everything plugin) {
        this.plugin = plugin;
        this.papiEnabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (!papiEnabled) {
            plugin.getLogger().info("PlaceholderAPI not found, using basic player name replacement for join/leave messages.");
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String format = plugin.getConfig().getString("messages.leave", "");

        if (format.isEmpty()) {
            event.quitMessage(null);
            return;
        }

        event.quitMessage(formatMessage(player, format));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String format;

        if (player.hasPlayedBefore()) {
            format = plugin.getConfig().getString("messages.join", "");
        } else {
            format = plugin.getConfig().getString("messages.first-join", "");
        }

        if (format.isEmpty()) {
            event.joinMessage(null);
            return;
        }

        event.joinMessage(formatMessage(player, format));
    }

    /**
     * Helper method to format messages using Adventure API and PlaceholderAPI if available.
     *
     * @param player The player context for placeholders.
     * @param format The raw format string from the config.
     * @return The formatted Component, or null if the format was invalid/empty.
     */
    private Component formatMessage(Player player, String format) {
        if (format == null || format.isEmpty()) {
            return null;
        }

        String message = format;

        if (papiEnabled) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        } else {
            message = message.replace("%player_name%", player.getName())
                    .replace("%player_displayname%", player.displayName().toString());
        }

        // Use MiniMessage instead of LegacyComponentSerializer
        return miniMessage.deserialize(message);
    }
}