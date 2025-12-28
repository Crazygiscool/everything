package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class NameColorCommand implements CommandExecutor, TabCompleter {

    private final Everything plugin;

    public NameColorCommand(Everything plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(Component.text("Usage: /namecolor <color>")
                    .color(NamedTextColor.YELLOW));
            return true;
        }

        String colorName = args[0].toUpperCase(Locale.ROOT);
        NamedTextColor color = NamedTextColor.NAMES.value(colorName.toLowerCase(Locale.ROOT));

        if (color == null) {
            player.sendMessage(Component.text("Invalid color! Use names like RED, BLUE, GREEN, etc.")
                    .color(NamedTextColor.RED));
            return true;
        }

        plugin.getConfig().set("namecolors." + player.getUniqueId(), colorName);
        plugin.saveConfig();

        player.sendMessage(
            Component.text("Your name color has been set to ")
                .append(Component.text(colorName).color(color).decorate(TextDecoration.BOLD))
                .append(Component.text("!").color(NamedTextColor.WHITE))
        );

        return true;
    }

    // ----------------------------------------------------
    // TAB COMPLETION
    // ----------------------------------------------------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        // /namecolor <color>
        if (args.length == 1) {

            String input = args[0].toLowerCase(Locale.ROOT);

            return NamedTextColor.NAMES.keys().stream()
                    .filter(name -> name.startsWith(input))
                    .toList();
        }

        return List.of();
    }
}
