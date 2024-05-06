package me.crazyg.everything.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class onJoinleaveListener implements Listener {
    @EventHandler
    public void onLeave(PlayerQuitEvent e){

        Player player = e.getPlayer();
        e.setQuitMessage(ChatColor.BOLD+""+ ChatColor.DARK_AQUA+player.getDisplayName()+ChatColor.BOLD+""+ChatColor.RED+"Has Left the Server");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e){

        Player player = e.getPlayer();
        if (player.hasPlayedBefore()){
            e.setJoinMessage(ChatColor.BOLD+""+ChatColor.RED+"Welcome "+ChatColor.BOLD+""+ChatColor.DARK_AQUA+player.getDisplayName()+ChatColor.BOLD+""+ChatColor.RED+" Back to the server!");
        }else{
            e.setJoinMessage(ChatColor.BOLD+""+ChatColor.RED+"Welcome "+ChatColor.BOLD+""+ChatColor.DARK_AQUA+player.getDisplayName()+ChatColor.BOLD+""+ChatColor.RED+" to the server! he Has Joined For the First Time!");
        }

    }
}
