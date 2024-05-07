package me.crazyg.everything.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class GodCommand implements CommandExecutor{

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] strings) {

        if(sender instanceof Player p){
            if(p.isInvulnerable()){
                p.setInvulnerable(false);
                p.sendMessage(ChatColor.RED+"GOD MODE disabled");
            }else{
                p.setInvulnerable(true);
                p.sendMessage(ChatColor.GOLD+"GOD MODE Enabled");
            }
        }
        return true;
    }
}