package me.crazyg.everything.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class KillCommand implements CommandExecutor{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){

        if (sender instanceof Player p){
            if (args.length == 0) {
                p.setHealth(0);
                p.sendMessage(ChatColor.DARK_RED+""+ChatColor.BOLD+"You Have Opted To DIEEEE!");
            }else{
                //set the string playername to the first argument
                String playername = args[0];

                //get the target player name to store in to the target argument
                Player target = Bukkit.getServer().getPlayerExact(playername);

                if(target == null){
                    p.sendMessage("This Player is not online");
                }else{
                    target.setHealth(0);
                    target.sendMessage(ChatColor.DARK_RED+""+ChatColor.BOLD+"You Have been Opted To DIE by "+ playername);
                    p.sendMessage(ChatColor.DARK_RED+""+ChatColor.BOLD+"BRO DIED, UAHAHAH");
                }
            }
        } else if (sender instanceof ConsoleCommandSender p) {
            p.sendMessage(ChatColor.DARK_RED+""+ChatColor.BOLD+"Command Cannot be runned by console, Silly");
        } else if (sender instanceof BlockCommandSender p) {
            p.sendMessage(ChatColor.DARK_RED+""+ChatColor.BOLD+"Command Cannot be runned by command block, L");
        }

        return true;
    }
}
