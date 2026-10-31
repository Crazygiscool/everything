package me.crazyg.everything.utils;

import me.crazyg.everything.Everything;
import me.crazyg.everything.gui.menu.MenuAction;
import me.crazyg.everything.gui.menu.MenuItemDefinition;
import me.crazyg.everything.gui.menu.MenuDefinition;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public final class HybridChatMenu {

    private HybridChatMenu() {}

    public static void send(Everything plugin, CommandSender sender) {
        send(plugin, sender, plugin.getConfig().getString("gui.primary-menu", "primary"));
    }

    public static void send(Everything plugin, CommandSender sender, String menuId) {
        if (!(sender instanceof org.bukkit.entity.Player player)) {
            AdventureCompat.sendMessage(sender,
                Component.text("Use /everything menu in-game.")
                    .color(NamedTextColor.RED));
            return;
        }

        MenuDefinition definition =
            MenuDefinition.load(plugin, menuId, plugin.getConfig());
        if (definition == null) {
            Everything.sendFancy(player,
                Component.text("Chat launcher unavailable.")
                    .color(NamedTextColor.RED));
            return;
        }

        Component message = Component.text("❖ ").color(NamedTextColor.AQUA)
            .append(Component.text("Everything").color(NamedTextColor.BLUE))
            .append(Component.text(" » ").color(NamedTextColor.AQUA));
        boolean first = true;
        for (MenuItemDefinition item : definition.items()) {
            if (!item.chat()) {
                continue;
            }
            if (!hasPermission(player, item)) {
                continue;
            }
            String command = commandFor(player, plugin, item);
            if (command == null) {
                continue;
            }
            if (!first) {
                message = message.append(Component.text(" | ").color(NamedTextColor.DARK_GRAY));
            }
            first = false;
            Component label = AdventureCompat.deserialize(item.rawName())
                .colorIfAbsent(NamedTextColor.AQUA);
            message = message.append(label)
                .append(Component.text(" [" + command + "]")
                    .color(NamedTextColor.DARK_GRAY));
        }

        if (first) {
            AdventureCompat.sendMessage(player, Component.text("No chat shortcuts configured.")
                .color(NamedTextColor.RED));
            AdventureCompat.sendMessage(player, Component.text("You can still use /everything menu.")
                .color(NamedTextColor.GRAY));
            return;
        }

        Everything.sendFancy(player, message);
    }

    private static String commandFor(org.bukkit.entity.Player player,
                                      Everything plugin,
                                      MenuItemDefinition item) {
        String target = item.target() == null ? "" : item.target().trim();
        return switch (item.action()) {
            case OPEN_MENU -> target.isEmpty() ? "/everything menu" : "/everything menu " + target;
            case OPEN_PLAYER_SETTINGS -> "/everything settings";
            case OPEN_ADMIN -> "/everything admin";
            case OPEN_HELP -> "/help";
            case OPEN_PAGE -> "/help";
            case PERFORM_COMMAND -> {
                if (target.isEmpty()) {
                    yield null;
                }
                yield target.startsWith("/") ? target : "/" + target;
            }
            case OPEN_CHAT -> "/everything chat";
            default -> null;
        };
    }

    private static boolean hasPermission(org.bukkit.entity.Player player,
                                         MenuItemDefinition item) {
        return item.permission() == null
            || item.permission().trim().isEmpty()
            || player.hasPermission(item.permission());
    }

    private static Component infoComponent(MenuItemDefinition item, String command) {
        return Component.text(item.action() + ": " + command)
            .color(NamedTextColor.GRAY);
    }
}
