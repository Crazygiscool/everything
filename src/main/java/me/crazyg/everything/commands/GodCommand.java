package me.crazyg.everything.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;


public class GodCommand implements CommandExecutor{

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if(sender instanceof Player p){
            if (args.length == 0){
                if(p.isInvulnerable()){
                    p.setInvulnerable(false);
                    p.sendMessage(ChatColor.DARK_RED+"GOD MODE disabled");
                }else{
                    p.setInvulnerable(true);
                    p.sendMessage(ChatColor.GOLD+"GOD MODE Enabled");
                }
            }else{

                //set the string playername to the first argument
                String playername = args[0];

                //get the target player name to store in to the target argument
                Player target = Bukkit.getServer().getPlayerExact(playername);

                if(target == null){
                    p.sendMessage("This Player is not online");
                }else{
                    if(p.isInvulnerable()){
                        target.setInvulnerable(false);
                        target.sendMessage(ChatColor.DARK_RED+"GOD MODE disabled by "+p.getDisplayName());
                        p.sendMessage(ChatColor.BLUE+"MADE HIM UNGOD");
                    }else {
                        target.setInvulnerable(true);
                        target.sendMessage(ChatColor.GOLD + "GOD MODE Enabled by "+p.getDisplayName());
                        p.sendMessage(ChatColor.BLUE+"MADE HIM GOD");
                    }
                }

            }
        }else if (sender instanceof ConsoleCommandSender p) {
            p.sendMessage(ChatColor.DARK_RED+""+ChatColor.BOLD+"Command Cannot be runned by console");
        } else if (sender instanceof BlockCommandSender p) {
            p.sendMessage(ChatColor.DARK_RED+""+ChatColor.BOLD+"Command Cannot be runned by command block");
        }
        return true;
    }
}