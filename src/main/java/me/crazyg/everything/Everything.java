package me.crazyg.everything;

import me.crazyg.everything.commands.*;
import me.crazyg.everything.listeners.onJoinleaveListener;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class Everything extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        System.out.println("THIS PLUGIN IS WRITTENED BY CRAZYG");
        System.out.println("THANKS FOR USING THE PLUGIN");
        System.out.println("\n" +
                "███████╗██╗░░░██╗███████╗██████╗░██╗░░░██╗████████╗██╗░░██╗██╗███╗░░██╗░██████╗░\n" +
                "██╔════╝██║░░░██║██╔════╝██╔══██╗╚██╗░██╔╝╚══██╔══╝██║░░██║██║████╗░██║██╔════╝░\n" +
                "█████╗░░╚██╗░██╔╝█████╗░░██████╔╝░╚████╔╝░░░░██║░░░███████║██║██╔██╗██║██║░░██╗░\n" +
                "██╔══╝░░░╚████╔╝░██╔══╝░░██╔══██╗░░╚██╔╝░░░░░██║░░░██╔══██║██║██║╚████║██║░░╚██╗\n" +
                "███████╗░░╚██╔╝░░███████╗██║░░██║░░░██║░░░░░░██║░░░██║░░██║██║██║░╚███║╚██████╔╝\n" +
                "╚══════╝░░░╚═╝░░░╚══════╝╚═╝░░╚═╝░░░╚═╝░░░░░░╚═╝░░░╚═╝░░╚═╝╚═╝╚═╝░░╚══╝░╚═════╝░");
        // Commands
        getCommand("suicide").setExecutor(new KillCommand());
        getCommand("god").setExecutor(new GodCommand());
        getCommand("repeat").setExecutor(new RepeatCommand());
        // Listeners
        getServer().getPluginManager().registerEvents((Listener) new onJoinleaveListener(this), (Plugin) this);
        //config.yml
        getConfig().options().copyDefaults();
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        System.out.println("\n" +
                "░██████╗░░█████╗░░█████╗░██████╗░██████╗░██╗░░░██╗███████╗\n" +
                "██╔════╝░██╔══██╗██╔══██╗██╔══██╗██╔══██╗╚██╗░██╔╝██╔════╝\n" +
                "██║░░██╗░██║░░██║██║░░██║██║░░██║██████╦╝░╚████╔╝░█████╗░░\n" +
                "██║░░╚██╗██║░░██║██║░░██║██║░░██║██╔══██╗░░╚██╔╝░░██╔══╝░░\n" +
                "╚██████╔╝╚█████╔╝╚█████╔╝██████╔╝██████╦╝░░░██║░░░███████╗\n" +
                "░╚═════╝░░╚════╝░░╚════╝░╚═════╝░╚═════╝░░░░╚═╝░░░╚══════╝");
    }
}
