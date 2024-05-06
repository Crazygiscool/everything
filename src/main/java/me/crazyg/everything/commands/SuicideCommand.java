package me.crazyg.everything.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class SuicideCommand implements CommandExecutor{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] Strings){

        if (sender instanceof Player p){
            p.setHealth(0);
            p.sendMessage(ChatColor.DARK_RED+""+ChatColor.BOLD+"You Have Opted To DIEEEE!");
        } else if (sender instanceof ConsoleCommandSender p) {
            p.sendMessage(ChatColor.DARK_RED+""+ChatColor.BOLD+"Command Cannot be runned by console");
        } else if (sender instanceof BlockCommandSender p) {
            p.sendMessage(ChatColor.DARK_RED+""+ChatColor.BOLD+"Command Cannot be runned by command block");
        }

        return true;
    }
}
