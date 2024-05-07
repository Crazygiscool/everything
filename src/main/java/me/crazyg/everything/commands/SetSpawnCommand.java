package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {

    private final Everything plugin;

    public SetSpawnCommand(Everything plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if(sender instanceof Player p){

            Location location = p.getLocation();

            //method 1
            //plugin.getConfig().set("spawn.x", location.getX());
            //plugin.getConfig().set("spawn.y", location.getY());
            //plugin.getConfig().set("spawn.z", location.getZ());
            //plugin.getConfig().set("spawn.worldName", location.getWorld().getName());

            //method 2
            plugin.getConfig().set("spawn", location);

            plugin.saveConfig();

            p.sendMessage(ChatColor.DARK_RED+"SUCCESSFULLY set the spawnpoint");

        }else{
            System.out.println("NOPE, PLAYER ONLY");
        }

        return true;
    }
}
