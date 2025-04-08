package me.crazyg.everything;

import me.crazyg.everything.commands.*;
import me.crazyg.everything.listeners.onJoinleaveListener;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class Everything extends JavaPlugin {
    ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("THIS PLUGIN IS WRITTENED BY CRAZYG");
        getLogger().info("THANKS FOR USING THE PLUGIN");
        getLogger().info("\n" +
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
        getCommand("gmc").setExecutor(new GmcCommand());
        getCommand("gms").setExecutor(new GmsCommand());
        // Listeners
        getServer().getPluginManager().registerEvents((Listener) new onJoinleaveListener(this), (Plugin) this);
        //config.yml
        getConfig().options().copyDefaults();
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("[everything]\n" +
                "░██████╗░░█████╗░░█████╗░██████╗░██████╗░██╗░░░██╗███████╗\n" +
                "██╔════╝░██╔══██╗██╔══██╗██╔══██╗██╔══██╗╚██╗░██╔╝██╔════╝\n" +
                "██║░░██╗░██║░░██║██║░░██║██║░░██║██████╦╝░╚████╔╝░█████╗░░\n" +
                "██║░░╚██╗██║░░██║██║░░██║██║░░██║██╔══██╗░░╚██╔╝░░██╔══╝░░\n" +
                "╚██████╔╝╚█████╔╝╚█████╔╝██████╔╝██████╦╝░░░██║░░░███████╗\n" +
                "░╚═════╝░░╚════╝░░╚════╝░╚═════╝░╚═════╝░░░░╚═╝░░░╚══════╝");
    }
}
