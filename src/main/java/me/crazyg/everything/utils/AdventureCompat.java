package me.crazyg.everything.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public final class AdventureCompat {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();

    public static void sendMessage(CommandSender sender, Component component) {
        sender.sendMessage(SERIALIZER.serialize(component));
    }

    private AdventureCompat() {}
}
