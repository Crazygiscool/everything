package me.crazyg.everything.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Get the player who sent the message
        String playerName = event.getPlayer().getName();

        // Get the original message
        String message = event.getMessage();

        // Format the chat message
        String formattedMessage = ChatColor.GOLD + playerName + ChatColor.WHITE + ": " + message;

        // Broadcast the custom formatted message to all players
        Bukkit.broadcastMessage(formattedMessage);

        // Cancel the default chat format (if you only want your custom format)
        event.setCancelled(true);
    }
}