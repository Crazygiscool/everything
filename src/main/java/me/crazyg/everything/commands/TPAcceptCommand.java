package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TPAcceptCommand implements CommandExecutor {
    private final TPACommand tpaCommand;

    public TPAcceptCommand(Everything plugin, TPACommand tpaCommand) {
        this.tpaCommand = tpaCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        return tpaCommand.handleTpAccept(p);
    }
}
