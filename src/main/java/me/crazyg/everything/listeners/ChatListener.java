package me.crazyg.everything.listeners;

import me.crazyg.everything.Everything;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final Everything plugin;

    // Constructor to pass the plugin instance
    public ChatListener(Everything plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Get the player and the message
        String playerName = event.getPlayer().getDisplayName();
        String message = event.getMessage();

        // Get the chat format from the config
        String chatFormat = plugin.getConfig().getString("chat-format", "&e%player%: &f%message%");

        if (chatFormat != null) {
            // Replace placeholders in the chat format
            chatFormat = chatFormat.replace("%player%", playerName);
            chatFormat = chatFormat.replace("%message%", message);

            // Send the formatted message
            event.setFormat(ChatColor.translateAlternateColorCodes('&', chatFormat));
        } else {
            // Fallback - Send the default chat format if no format is defined in config.yml
            event.setFormat(ChatColor.YELLOW + playerName + ChatColor.WHITE + ": " + message);
        }
    }
}