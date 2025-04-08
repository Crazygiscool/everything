package me.crazyg.everything.listeners;

import me.crazyg.everything.Everything;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class onJoinleaveListener implements Listener {

    private final Everything plugin;

    public onJoinleaveListener(Everything plugin) {
        this.plugin = plugin;
    }
    ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
    @EventHandler
    public void onLeave(PlayerQuitEvent e){

        Player player = e.getPlayer();

        String leavemsg = this.plugin.getConfig().getString("leave-message");

        if(leavemsg != null){
            leavemsg = leavemsg.replace("%player%", e.getPlayer().getDisplayName());
            e.setQuitMessage(ChatColor.translateAlternateColorCodes('&', leavemsg));

        }else{
            console.sendMessage("SET THE LEAVE-MESSAGE IN CONFIG.YML");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e){

        Player player = e.getPlayer();

        String joinmsg = this.plugin.getConfig().getString("join-message");

        String firstjoinmsg = this.plugin.getConfig().getString("first-join-message");
        if (player.hasPlayedBefore()){

            if(joinmsg != null){
                joinmsg = joinmsg.replace("%player%", e.getPlayer().getDisplayName());
                e.setJoinMessage(ChatColor.translateAlternateColorCodes('&', joinmsg));

            }else{
                console.sendMessage("SET THE JOIN-MESSAGE IN CONFIG.YML");
            }

        }else{

            if(firstjoinmsg != null){
                firstjoinmsg = firstjoinmsg.replace("%player%", e.getPlayer().getDisplayName());
                e.setJoinMessage(ChatColor.translateAlternateColorCodes('&', firstjoinmsg));

            }else{
                console.sendMessage("SET THE FIRST-JOIN-MESSAGE IN CONFIG.YML");
            }

        }

    }
}
