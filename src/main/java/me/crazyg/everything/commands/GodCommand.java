package me.crazyg.everything.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;


public class GodCommand implements CommandExecutor{

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] strings) {

        if(sender instanceof Player p){
            if(p.isInvulnerable()){
                p.setInvulnerable(false);
                p.sendMessage(ChatColor.DARK_RED+"GOD MODE disabled");
            }else{
                p.setInvulnerable(true);
                p.sendMessage(ChatColor.GOLD+"GOD MODE Enabled");
            }
        }else if (sender instanceof ConsoleCommandSender p) {
            p.sendMessage(ChatColor.DARK_RED+""+ChatColor.BOLD+"Command Cannot be runned by console");
        } else if (sender instanceof BlockCommandSender p) {
            p.sendMessage(ChatColor.DARK_RED+""+ChatColor.BOLD+"Command Cannot be runned by command block");
        }
        return true;
    }
}