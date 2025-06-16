package me.crazyg.everything.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

        // Get chat format from config, default to a basic format if not found
        String format = plugin.getConfig().getString("chat.format", "<color:#00b7ff>%player% <gray>» <white>%message%");

        // Get prefix and suffix from Vault if available
        String prefix = "";
        String suffix = "";
        if (vaultEnabled && vaultChat != null) {
            try {
                prefix = vaultChat.getPlayerPrefix(player);
                suffix = vaultChat.getPlayerSuffix(player);

                // Ensure prefix and suffix aren't null
                prefix = prefix != null ? prefix : "";
                suffix = suffix != null ? suffix : "";
            } catch (Exception e) {
                // If there's any error with Vault, just continue without prefix/suffix
            }
        }

        // Get name color from config
        String colorName = plugin.getConfig().getString("namecolors." + player.getUniqueId(), "WHITE");
        NamedTextColor color = NamedTextColor.NAMES.value(colorName.toLowerCase());
        if (color == null) {
            color = NamedTextColor.WHITE;
        }
        Component displayName = Component.text(player.getName()).color(color);

        // Create tag resolvers for placeholders
        TagResolver.Builder resolver = TagResolver.builder()
            .resolver(Placeholder.parsed("prefix", prefix))
            .resolver(Placeholder.parsed("suffix", suffix))
            .resolver(Placeholder.component("player", displayName))
            .resolver(Placeholder.component("message", message));

        // Apply PlaceholderAPI placeholders if available
        if (papiEnabled) {
            format = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, format);
        }

        // Parse the format with MiniMessage
        Component formattedMessage = miniMessage.deserialize(format, resolver.build());

        // Set the formatted message
        event.setCancelled(true);
        Bukkit.getServer().sendMessage(formattedMessage);
    }
}