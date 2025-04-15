package me.crazyg.everything.listeners;

import me.crazyg.everything.Everything;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    
    private final Everything plugin;
    private final boolean papiEnabled;
    private final boolean vaultEnabled;
    private final net.milkbowl.vault.chat.Chat vaultChat;

    public ChatListener(Everything plugin) {
        this.plugin = plugin;
        this.papiEnabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        this.vaultEnabled = Bukkit.getPluginManager().isPluginEnabled("Vault");
        this.vaultChat = vaultEnabled ? plugin.getServer().getServicesManager()
            .getRegistration(net.milkbowl.vault.chat.Chat.class).getProvider() : null;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // Get chat format from config, default to a basic format if not found
        String format = plugin.getConfig().getString("chat.format", "&b%prefix%%player%%suffix% &7> &f%message%");
        
        // Get prefix and suffix from Vault if available
        String prefix = "";
        String suffix = "";
        if (vaultEnabled && vaultChat != null) {
            prefix = vaultChat.getPlayerPrefix(player);
            suffix = vaultChat.getPlayerSuffix(player);
            
            // Ensure prefix and suffix aren't null
            prefix = prefix != null ? prefix : "";
            suffix = suffix != null ? suffix : "";
        }
        
        // Replace placeholders
        String formattedMessage = format
            .replace("%prefix%", prefix)
            .replace("%suffix%", suffix)
            .replace("%player%", player.getName())
            .replace("%message%", message);
            
        // Apply PlaceholderAPI placeholders if available
        if (papiEnabled) {
            formattedMessage = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, formattedMessage);
        }
        
        // Translate color codes
        formattedMessage = ChatColor.translateAlternateColorCodes('&', formattedMessage);
        
        // Set the formatted message
        event.setFormat(formattedMessage.replace("%", "%%")); // Escape % for format string
    }
}