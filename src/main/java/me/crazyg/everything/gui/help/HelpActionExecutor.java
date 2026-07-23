package me.crazyg.everything.gui.help;

import me.crazyg.everything.utils.AdventureCompat;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Shared action executor for help GUI items. Used by both the main menu
 * and page GUIs so click behavior is defined in one place.
 */
public final class HelpActionExecutor {

    private HelpActionExecutor() {}

    public static void execute(String action, ConfigurationSection itemConfig,
                                Player player, HelpManager manager) {
        if (action == null) return;
        switch (action) {
            case "open_page" -> {
                String target = itemConfig.getString("target-page", "");
                if (target.equals("_main") || target.isEmpty()) {
                    player.closeInventory();
                    if (target.equals("_main") && manager != null) {
                        new HelpMainGUI(player, manager).open();
                    }
                } else if (manager != null) {
                    new HelpPageGUI(player, manager, target).open();
                }
            }
            case "command" -> {
                String cmd = itemConfig.getString("command", "");
                if (!cmd.isEmpty()) {
                    player.closeInventory();
                    boolean asConsole = itemConfig.getBoolean("as-console", false);
                    if (asConsole) {
                        player.getServer().dispatchCommand(
                            player.getServer().getConsoleSender(), cmd);
                    } else {
                        player.performCommand(cmd);
                    }
                }
            }
            case "message" -> {
                String msg = itemConfig.getString("message", "");
                if (!msg.isEmpty()) {
                    msg = manager != null ? manager.replacePlaceholders(msg) : msg;
                    AdventureCompat.sendMessage(player,
                        LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
                }
            }
            case "close" -> player.closeInventory();
            default -> {}
        }
    }
}
