package me.crazyg.everything.listeners;

import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    
    private final Everything plugin;
    private final boolean vaultEnabled;
    private final net.milkbowl.vault.chat.Chat vaultChat;
    private final MiniMessage miniMessage;

    public ChatListener(Everything plugin) {
        this.plugin = plugin;
        this.vaultEnabled = plugin.isVaultChatEnabled();
        this.vaultChat = Everything.getChat();
        this.miniMessage = MiniMessage.miniMessage();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Component message = Component.text(event.getMessage());

        event.setCancelled(true);

        String format = plugin.getConfig().getString("chat.format", "<color:#00b7ff><player> <gray>» <white><message>");

        String prefix = "";
        String suffix = "";
        if (vaultEnabled && vaultChat != null) {
            try {
                prefix = vaultChat.getPlayerPrefix(player);
                suffix = vaultChat.getPlayerSuffix(player);

                prefix = prefix != null ? prefix : "";
                suffix = suffix != null ? suffix : "";
            } catch (Exception ignored) {}
        }

        Component prefixComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(prefix);
        Component suffixComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(suffix);

        String colorName = plugin.getConfig().getString("namecolors." + player.getUniqueId(), "WHITE");
        NamedTextColor color = NamedTextColor.NAMES.value(colorName.toLowerCase());
        if (color == null) {
            color = NamedTextColor.WHITE;
        }
        Component displayName = Component.text(player.getName()).color(color);

        format = format.replace("%player_name%", player.getName())
                       .replace("%player_displayname%", player.getDisplayName())
                       .replace("%player_world%", player.getWorld().getName())
                       .replace("%player_health%", String.format("%.1f", player.getHealth()))
                       .replace("%player_food%", String.valueOf(player.getFoodLevel()))
                       .replace("%player_level%", String.valueOf(player.getLevel()))
                       .replace("%player_xp%", String.valueOf(player.getExp()))
                       .replace("%player_ping%", String.valueOf(player.getPing()))
                       .replace("%player_gamemode%", player.getGameMode().name());

        TagResolver.Builder resolver = TagResolver.builder()
            .resolver(Placeholder.component("prefix", prefixComponent))
            .resolver(Placeholder.component("suffix", suffixComponent))
            .resolver(Placeholder.component("player", displayName))
            .resolver(Placeholder.component("message", message));

        Component formattedMessage = miniMessage.deserialize(format, resolver.build());
        Bukkit.broadcastMessage(LegacyComponentSerializer.legacySection().serialize(formattedMessage));
    }
}
