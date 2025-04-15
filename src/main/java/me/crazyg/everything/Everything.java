package me.crazyg.everything;

import me.crazyg.everything.commands.*;
import me.crazyg.everything.listeners.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class Everything extends JavaPlugin {

    private static Economy econ = null;
    private boolean economyEnabled = false;
    private boolean vaultChatEnabled = false;

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

        // --- Economy & Vault Setup ---
        if (!setupEconomy()) {
            getLogger().warning("Vault or Economy provider not found! Economy features will be disabled.");
            economyEnabled = false;
        } else {
            getLogger().info("Vault economy hooked successfully!");
            economyEnabled = true;
        }

        // --- Command Manager ---
        CommandManager commandManager = new CommandManager();
        getCommand("suicide").setExecutor(commandManager);
        getCommand("god").setExecutor(commandManager);
        getCommand("report").setExecutor(commandManager);
        getCommand("everything").setExecutor(commandManager);
        getCommand("setspawn").setExecutor(commandManager);
        getCommand("spawn").setExecutor(commandManager);
        getCommand("gmc").setExecutor(commandManager);
        getCommand("gms").setExecutor(commandManager);
        getCommand("gmsp").setExecutor(commandManager);
        getCommand("gma").setExecutor(commandManager);
        getCommand("home").setExecutor(commandManager);
        getCommand("sethome").setExecutor(commandManager);
        getCommand("msg").setExecutor(commandManager);
        getCommand("reply").setExecutor(commandManager);

        // --- Command Registration ---
        commandManager.registerCommand("suicide", new KillCommand());
        commandManager.registerCommand("god", new GodCommand());
        commandManager.registerCommand("report", new ReportCommand(this));
        commandManager.registerCommand("everything", new EverythingCommand(this));
        commandManager.registerCommand("home", new HomeCommand(this));
        commandManager.registerCommand("sethome", new HomeCommand(this));
        commandManager.registerCommand("msg", new MessageCommand(this));
        commandManager.registerCommand("reply", new MessageCommand(this));

        // Only register economy commands if economy is enabled
        if (economyEnabled) {
            getCommand("balance").setExecutor(new BalanceCommand(this));
            getCommand("pay").setExecutor(new PayCommand(this));
            commandManager.registerCommand("pay", new PayCommand(this));
            commandManager.registerCommand("balance", new BalanceCommand(this));
        }

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
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        // --- PlaceholderAPI Check ---
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("PlaceholderAPI not found. Some placeholders in chat/messages may not work.");
        } else {
            getLogger().info("PlaceholderAPI found & Hooked!");
        }
        // --- Vault Check ---
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault not found. Prefixes in chat/messages may not work.");
        } else {
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

    public boolean isEconomyEnabled() {
        return economyEnabled;
    }

    // Add getter for economy
    public static Economy getEconomy() {
        return econ;
    }
}