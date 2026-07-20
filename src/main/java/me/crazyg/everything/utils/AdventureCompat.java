package me.crazyg.everything.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

    /**
     * Sends a Component preserving click/hover events (for Paper servers).
     * Falls back to legacy serialization on Spigot.
     */
    public static void sendInteractiveMessage(Player player, Component component) {
        if (tryNativeSend(player, component)) return;
        player.sendMessage(SERIALIZER.serialize(component));
    }

    private static boolean tryNativeSend(Player player, Component component) {
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
