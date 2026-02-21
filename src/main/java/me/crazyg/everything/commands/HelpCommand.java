package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import me.crazyg.everything.gui.help.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HelpCommand implements CommandExecutor {

    private final HelpManager manager;

    public HelpCommand(Everything plugin) {
        this.manager = new HelpManager(plugin);
    }

    @Override
    public boolean onCommand(
        CommandSender sender,
        Command cmd,
        String label,
        String[] args
    ) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(
                Component.text(
                    "This command can only be used by players!"
                ).color(NamedTextColor.RED)
            );
            return true;
        }

        new HelpMainGUI(p, manager).open();
        return true;
    }
}
