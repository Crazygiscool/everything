package me.crazyg.everything.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.*;
import org.bukkit.entity.Player;


public class GmsCommand implements CommandExecutor{

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player p) {
            if (args.length == 0) {
                if (p.getGameMode() == GameMode.SURVIVAL) {
                    p.setGameMode(GameMode.SURVIVAL);
                    p.sendMessage(ChatColor.DARK_RED + "Gamemode set to" + ChatColor.GOLD + ChatColor.BOLD + "Survival Mode");
                }
            }else {

                //set the string playername to the first argument
                String playername = args[0];

                //get the target player name to store in to the target argument
                Player target = Bukkit.getServer().getPlayerExact(playername);

                if (target == null) {
                    p.sendMessage("This Player is not online");
                } else {
                        target.setGameMode(GameMode.SURVIVAL);
                        target.sendMessage(ChatColor.DARK_RED + "Set to " + ChatColor.GOLD + ChatColor.BOLD + "Survival Mode " + ChatColor.DARK_RED + "by " + ChatColor.DARK_AQUA + ChatColor.BOLD + p.getDisplayName());
                        p.sendMessage(ChatColor.BLUE + "Set " + ChatColor.DARK_AQUA + ChatColor.BOLD + p.getDisplayName() + ChatColor.BLUE + "to " + ChatColor.GOLD + ChatColor.BOLD + "Survival Mode");

                }
            }
        } else if (sender instanceof ConsoleCommandSender m) {
            m.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Command Cannot be runned by console");
        } else if (sender instanceof BlockCommandSender m) {
            m.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Command Cannot be runned by command block");
        }
        return true;
    }
}