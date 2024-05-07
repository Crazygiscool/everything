package me.crazyg.everything.listeners;

import me.crazyg.everything.Everything;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class SpawnListener implements Listener {

    private final Everything plugin;

    public SpawnListener(Everything plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e){
        Player player = e.getPlayer();
        if(!player.hasPlayedBefore()){

            Location location = plugin.getConfig().getLocation("spawn");

            if(location != null){

                player.teleport(location);
                player.sendMessage(ChatColor.BLUE+"You have been teleported to spawn.");

            }

        }

    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e){
        Location location = plugin.getConfig().getLocation("spawn");
        if(location != null){
            e.setRespawnLocation(location);
        }


    }

}
