package me.crazyg.everything.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class AdventureCompat {

    private static final LegacyComponentSerializer SERIALIZER =
        LegacyComponentSerializer.legacySection();

    private static boolean nativeAdventureChecked = false;
    private static boolean supportsNativeAdventure = false;
    private static Method sendMessageMethod;

    public static void sendMessage(CommandSender sender, Component component) {
        sender.sendMessage(SERIALIZER.serialize(component));
    }

    public static Component deserialize(String miniMessage) {
        return MiniMessage.miniMessage().deserialize(miniMessage);
    }

    /**
     * Sends a Component preserving click/hover events (for Paper servers).
     * Falls back to legacy serialization on Spigot.
     */
    public static void sendInteractiveMessage(Player player, Component component) {
        sendInteractiveMessage(player, component, null);
    }

    /**
     * Sends an interactive component to a player. On Paper, click and hover
     * events are preserved. On platform builds that serialize messages to
     * legacy text, {@code fallback} is appended as a plain command hint.
     */
    public static void sendInteractiveMessage(Player player,
                                            Component component,
                                            String fallback) {
        if (supportsInteractive(player) && tryNativeSend(player, component)) {
            return;
        }
        String legacy = SERIALIZER.serialize(component);
        if (fallback != null && !fallback.trim().isEmpty()) {
            legacy = legacy + " " + fallback.trim();
        }
        player.sendMessage(legacy);
    }

    /**
     * Whether the server's Adventure implementation can carry interactive
     * components reliably. Falls back to legacy messages otherwise.
     */
    public static boolean supportsInteractive(Player player) {
        if (!nativeAdventureChecked) {
            try {
                sendMessageMethod = player.getClass().getMethod(
                    "sendMessage", Component.class);
                supportsNativeAdventure = true;
            } catch (NoSuchMethodException e) {
                supportsNativeAdventure = false;
            }
            nativeAdventureChecked = true;
        }
        return supportsNativeAdventure && sendMessageMethod != null;
    }

    public static boolean tryNativeSend(Player player, Component component) {
        if (!nativeAdventureChecked) {
            try {
                sendMessageMethod = player.getClass().getMethod(
                    "sendMessage", Component.class);
                supportsNativeAdventure = true;
            } catch (NoSuchMethodException e) {
                supportsNativeAdventure = false;
            }
            nativeAdventureChecked = true;
        }
        if (!supportsNativeAdventure || sendMessageMethod == null) return false;
        try {
            sendMessageMethod.invoke(player, component);
            return true;
        } catch (Exception e) {
            supportsNativeAdventure = false;
            return false;
        }
    }

    private AdventureCompat() {}
}
