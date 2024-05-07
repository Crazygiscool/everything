package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    private final Everything plugin;

    public SpawnCommand(Everything plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player p){

            Location location = plugin.getConfig().getLocation("spawn");

            if(location != null){

                p.teleport(location);
                p.sendMessage(ChatColor.BLUE+"You have been teleported to spawn.");

            }else{
                p.sendMessage(ChatColor.DARK_RED+"The spawn is not set, do /spawn");
            }

        }

        return true;
    }
}
