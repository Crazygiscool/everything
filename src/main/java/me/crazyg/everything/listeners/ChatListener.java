package me.crazyg.everything.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {
    
    private final Everything plugin;
    private final boolean papiEnabled;
    private final boolean vaultEnabled;
    private final net.milkbowl.vault.chat.Chat vaultChat;
    private final MiniMessage miniMessage;

    public ChatListener(Everything plugin) {
        this.plugin = plugin;
        this.papiEnabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        this.vaultEnabled = plugin.isVaultChatEnabled();
        this.vaultChat = Everything.getChat();
        this.miniMessage = MiniMessage.miniMessage();
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Component message = event.message();

        // Get chat format from config
        String format = plugin.getConfig().getString("chat.format", 
            "<color:#00b7ff>%player_name% <gray>» <white>%message%");

        // Get prefix and suffix from Vault if available
        String prefix = "";
        String suffix = "";
        if (vaultEnabled && vaultChat != null) {
            try {
                prefix = vaultChat.getPlayerPrefix(player);
                suffix = vaultChat.getPlayerSuffix(player);
                prefix = prefix != null ? prefix : "";
                suffix = suffix != null ? suffix : "";
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to get Vault prefix/suffix: " + e.getMessage());
            }
        }

        // Process PlaceholderAPI placeholders first if available
        if (papiEnabled) {
            format = format.replace("%player%", "%player_name%"); // Fix common placeholder
            format = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, format);
            prefix = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, prefix);
            suffix = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, suffix);
        }

        // Create tag resolvers for MiniMessage
        TagResolver.Builder resolver = TagResolver.builder()
            .resolver(Placeholder.parsed("prefix", prefix))
            .resolver(Placeholder.parsed("suffix", suffix))
            .resolver(Placeholder.parsed("player_name", player.getName()))
            .resolver(Placeholder.component("message", message));

        // Parse the format with MiniMessage
        Component formattedMessage = miniMessage.deserialize(format, resolver.build());

        // Set the formatted message
        event.setCancelled(true);
        Bukkit.getServer().sendMessage(formattedMessage);
    }
}