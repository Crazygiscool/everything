package me.crazyg.everything;

import me.crazyg.everything.commands.*;
import me.crazyg.everything.listeners.ChatListener;
import me.crazyg.everything.listeners.onJoinleaveListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor; // Import ChatColor
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class Everything extends JavaPlugin {

    private static Economy econ = null;

    @Override
    public void onEnable() {
        // --- Config Loading ---
        // Ensure default config exists and copies new defaults if config is updated
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        // --- Logging ---
        getLogger().info("THIS PLUGIN IS WRITTENED BY CRAZYG");
        getLogger().info("THANKS FOR USING THE PLUGIN");
        // Consider making the ASCII art optional or configurable
        Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "\n" + // Example of adding color
                "███████╗██╗░░░██╗███████╗██████╗░██╗░░░██╗████████╗██╗░░██╗██╗███╗░░██╗░██████╗░\n" +
                "██╔════╝██║░░░██║██╔════╝██╔══██╗╚██╗░██╔╝╚══██╔══╝██║░░██║██║████╗░██║██╔════╝░\n" +
                "█████╗░░╚██╗░██╔╝█████╗░░██████╔╝░╚████╔╝░░░░██║░░░███████║██║██╔██╗██║██║░░██╗░\n" +
                "██╔══╝░░░╚████╔╝░██╔══╝░░██╔══██╗░░╚██╔╝░░░░░██║░░░██╔══██║██║██║╚████║██║░░╚██╗\n" +
                "███████╗░░╚██╔╝░░███████╗██║░░██║░░░██║░░░░░░██║░░░██║░░██║██║██║░╚███║╚██████╔╝\n" +
                "╚══════╝░░░╚═╝░░░╚══════╝╚═╝░░╚═╝░░░╚═╝░░░░░░╚═╝░░░╚═╝░░╚═╝╚═╝╚═╝░░╚══╝░╚═════╝░" +
                ChatColor.RESET); // Reset color after

        // --- Command Manager ---
        CommandManager commandManager = new CommandManager();
        getCommand("suicide").setExecutor(commandManager);
        getCommand("god").setExecutor(commandManager);
        getCommand("report").setExecutor(commandManager);
        getCommand("reload").setExecutor(commandManager);
        getCommand("setspawn").setExecutor(commandManager);
        getCommand("spawn").setExecutor(commandManager);
        getCommand("balance").setExecutor(commandManager);
        getCommand("pay").setExecutor(commandManager);
        getCommand("gmc").setExecutor(commandManager);
        getCommand("gms").setExecutor(commandManager);
        getCommand("gmsp").setExecutor(commandManager);
        getCommand("gma").setExecutor(commandManager);
        // --- Command Registration ---
        commandManager.registerCommand("suicide", new KillCommand());
        commandManager.registerCommand("god", new GodCommand());
        commandManager.registerCommand("report", new ReportCommand(this));
        commandManager.registerCommand("reload", new ReloadCommand(this));
        commandManager.registerCommand("balance", new BalanceCommand(this));
        commandManager.registerCommand("pay", new PayCommand(this));
        GamemodeCommand gamemodeExecutor = new GamemodeCommand();
        commandManager.registerCommand("gmc", gamemodeExecutor);
        commandManager.registerCommand("gms", gamemodeExecutor);
        commandManager.registerCommand("gmsp", gamemodeExecutor);
        commandManager.registerCommand("gma", gamemodeExecutor);

        // Register SetSpawnCommand
        SetSpawnCommand setSpawnCommand = new SetSpawnCommand(this);
        getCommand("setspawn").setExecutor(setSpawnCommand);
        getCommand("spawn").setExecutor(setSpawnCommand);

        // --- Listeners ---
        // Pass 'this' (the plugin instance) to the listeners if they need access to config etc.
        getServer().getPluginManager().registerEvents(new onJoinleaveListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(), this);

        // --- PlaceholderAPI Check ---
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("PlaceholderAPI not found. Some placeholders in chat/messages may not work.");
        } else {
            getLogger().info("PlaceholderAPI found & Hooked!");
        }

        // Hook into Vault's economy
        if (!setupEconomy()) {
            getLogger().severe("Vault dependency not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }else{
            getLogger().info("Vault found & Hooked!");
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Everything plugin disabled.");
        getLogger().info("\n" +
                "░██████╗░░█████╗░░█████╗░██████╗░██████╗░██╗░░░██╗███████╗\n" +
                "██╔════╝░██╔══██╗██╔══██╗██╔══██╗██╔══██╗╚██╗░██╔╝██╔════╝\n" +
                "██║░░██╗░██║░░██║██║░░██║██║░░██║██████╦╝░╚████╔╝░█████╗░░\n" +
                "██║░░╚██╗██║░░██║██║░░██║██║░░██║██╔══██╗░░╚██╔╝░░██╔══╝░░\n" +
                "╚██████╔╝╚█████╔╝╚█████╔╝██████╔╝██████╦╝░░░██║░░░███████╗\n" +
                "░╚═════╝░░╚════╝░░╚════╝░╚═════╝░╚═════╝░░░░╚═╝░░░╚══════╝");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEconomy() {
        return econ;
    }
}