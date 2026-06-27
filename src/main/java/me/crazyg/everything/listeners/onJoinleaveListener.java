package me.crazyg.everything.listeners;

import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class onJoinleaveListener implements Listener {

    private final Everything plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public onJoinleaveListener(Everything plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String format = plugin.getConfig().getString("messages.leave", "");

        if (format.isEmpty()) {
            event.setQuitMessage("");
            return;
        }

        event.setQuitMessage(formatMessage(player, format));
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
            event.setJoinMessage("");
            return;
        }

        event.setJoinMessage(formatMessage(player, format));
    }

    private String formatMessage(Player player, String format) {
        if (format == null || format.isEmpty()) {
            return "";
        }

        String message = format;
        message = message.replace("%player_name%", player.getName())
                         .replace("%player_displayname%", player.getDisplayName())
                         .replace("%player_world%", player.getWorld().getName())
                         .replace("%player_uuid%", player.getUniqueId().toString())
                         .replace("%player_ping%", String.valueOf(player.getPing()))
                         .replace("%player_gamemode%", player.getGameMode().name());

        Component component = miniMessage.deserialize(message);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}
