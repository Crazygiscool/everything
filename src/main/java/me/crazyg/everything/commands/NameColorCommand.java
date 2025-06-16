package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public class NameColorCommand implements CommandExecutor {
    private final Everything plugin;

    public NameColorCommand(Everything plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(Component.text("Usage: /namecolor <color>").color(NamedTextColor.YELLOW));
            return true;
        }

        String colorName = args[0].toUpperCase(Locale.ROOT);
        NamedTextColor color = NamedTextColor.NAMES.value(colorName.toLowerCase(Locale.ROOT));
        if (color == null) {
            player.sendMessage(Component.text("Invalid color! Use names like RED, BLUE, GREEN, etc.").color(NamedTextColor.RED));
            return true;
        }

        plugin.getConfig().set("namecolors." + player.getUniqueId(), colorName);
        plugin.saveConfig();

        player.sendMessage(Component.text("Your name color has been set to " + colorName + "!").color(color));
        return true;
    }
}