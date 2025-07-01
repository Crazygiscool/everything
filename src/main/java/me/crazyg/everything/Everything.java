package me.crazyg.everything;

import me.crazyg.everything.commands.*;
import me.crazyg.everything.listeners.*;
import me.crazyg.everything.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class Everything extends JavaPlugin {

    private static Economy econ = null;
    private static net.milkbowl.vault.chat.Chat chat = null;
    private boolean economyEnabled = false;
    private boolean vaultChatEnabled = false;
    private Updater updater;

    @Override
    public void onEnable() {
        // --- Teleport Commands ---
        TPCommand tpCommand = new TPCommand(this);
        TPACommand tpaCommand = new TPACommand(this);
        TPAcceptCommand tpAcceptCommand = new TPAcceptCommand(this, tpaCommand);
        TPDenyCommand tpDenyCommand = new TPDenyCommand(this, tpaCommand);

        getCommand("tp").setExecutor(tpCommand);
        getCommand("tpa").setExecutor(tpaCommand);
        getCommand("tpaccept").setExecutor(tpAcceptCommand);
        getCommand("tpdeny").setExecutor(tpDenyCommand);
        saveDefaultConfig(); // <-- Make sure this is first!
        // Now read config values, register listeners, etc.

        // --- Config Loading ---
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        // --- Logging ---
        getLogger().info("THIS PLUGIN IS WRITTEN BY CRAZYG");
        getLogger().info("THANKS FOR USING THE PLUGIN");

        // ASCII art with Adventure API
        Component asciiArt = Component.text()
            .append(Component.newline())
            .append(Component.text("███████╗██╗░░░██╗███████╗██████╗░██╗░░░██╗████████╗██╗░░██╗██╗███╗░░██╗░██████╗░\n").color(NamedTextColor.GOLD))
            .append(Component.text("██╔════╝██║░░░██║██╔════╝██╔══██╗╚██╗░██╔╝╚══██╔══╝██║░░██║██║████╗░██║██╔════╝░\n").color(NamedTextColor.GOLD))
            .append(Component.text("█████╗░░╚██╗░██╔╝█████╗░░██████╔╝░╚████╔╝░░░░██║░░░███████║██║██╔██╗██║██║░░██╗░\n").color(NamedTextColor.GOLD))
            .append(Component.text("██╔══╝░░░╚████╔╝░██╔══╝░░██╔══██╗░░╚██╔╝░░░░░██║░░░██╔══██║██║██║╚████║██║░░╚██╗\n").color(NamedTextColor.GOLD))
            .append(Component.text("███████╗░░╚██╔╝░░███████╗██║░░██║░░░██║░░░░░░██║░░░██║░░██║██║██║░╚███║╚██████╔╝\n").color(NamedTextColor.GOLD))
            .append(Component.text("╚══════╝░░░╚═╝░░░╚══════╝╚═╝░░╚═╝░░░╚═╝░░░░░░╚═╝░░░╚═╝░░╚═╝╚═╝╚═╝░░╚══╝░╚═════╝░").color(NamedTextColor.GOLD))
            .build();

        Bukkit.getConsoleSender().sendMessage(asciiArt);

        // --- Economy & Vault Setup ---
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            // Setup economy
            if (setupEconomy()) {
                getLogger().info("Vault economy hooked successfully!");
                economyEnabled = true;
            } else {
                getLogger().warning("Vault found but no economy provider detected!");
            }
            
            // Setup chat
            if (setupChat()) {
                getLogger().info("Vault chat hooked successfully!");
                vaultChatEnabled = true;
            } else {
                getLogger().warning("Vault found but no chat provider detected!");
            }
        } else {
            getLogger().warning("Vault not found! Economy and chat features will be disabled.");
        }

        // --- Command Manager ---
        CommandManager commandManager = new CommandManager();
        commandManager.registerCommand("tp", tpCommand);
        commandManager.registerCommand("tpa", tpaCommand);
        commandManager.registerCommand("tpaccept", tpAcceptCommand);
        commandManager.registerCommand("tpdeny", tpDenyCommand);
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
        getCommand("balance").setExecutor(commandManager);
        getCommand("pay").setExecutor(commandManager);
        getCommand("maintenance").setExecutor(commandManager);
        // Removed teleport commands as per suggestion
        getCommand("stats").setExecutor(commandManager);
        getCommand("namecolor").setExecutor(commandManager);
        getCommand("warp").setExecutor(commandManager);

        // --- Command Registration ---
        commandManager.registerCommand("suicide", new KillCommand());
        commandManager.registerCommand("god", new GodCommand());
        commandManager.registerCommand("report", new ReportCommand(this));
        commandManager.registerCommand("everything", new EverythingCommand(this));
        commandManager.registerCommand("home", new HomeCommand(this));
        commandManager.registerCommand("sethome", new HomeCommand(this));
        commandManager.registerCommand("msg", new MessageCommand(this));
        commandManager.registerCommand("reply", new MessageCommand(this));
        commandManager.registerCommand("maintenance", new MaintenanceCommand(this));
        commandManager.registerCommand("setspawn", new SetSpawnCommand(this));
        commandManager.registerCommand("spawn", new SetSpawnCommand(this));
        commandManager.registerCommand("pay", new PayCommand(this));
        commandManager.registerCommand("balance", new BalanceCommand(this));
        // Removed teleport command registration as per suggestion
        commandManager.registerCommand("stats", new StatsCommand(this));
        commandManager.registerCommand("namecolor", new NameColorCommand(this));
        commandManager.registerCommand("warp", new WarpCommand(this));

        GamemodeCommand gamemodeExecutor = new GamemodeCommand();
        commandManager.registerCommand("gmc", gamemodeExecutor);
        commandManager.registerCommand("gms", gamemodeExecutor);
        commandManager.registerCommand("gmsp", gamemodeExecutor);
        commandManager.registerCommand("gma", gamemodeExecutor);

        // Remove duplicate direct setExecutor for setspawn, spawn, balance, pay

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

        // Initialize updater
        updater = new Updater(this);
        getServer().getPluginManager().registerEvents(updater, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Everything plugin disabled.");
        
        Component goodbyeArt = Component.text()
            .append(Component.newline())
            .append(Component.text("░██████╗░░█████╗░░█████╗░██████╗░██████╗░██╗░░░██╗███████╗\n").color(NamedTextColor.RED))
            .append(Component.text("██╔════╝░██╔══██╗██╔══██╗██╔══██╗██╔══██╗╚██╗░██╔╝██╔════╝\n").color(NamedTextColor.RED))
            .append(Component.text("██║░░██╗░██║░░██║██║░░██║██║░░██║██████╦╝░╚████╔╝░█████╗░░\n").color(NamedTextColor.RED))
            .append(Component.text("██║░░╚██╗██║░░██║██║░░██║██║░░██║██╔══██╗░░╚██╔╝░░██╔══╝░░\n").color(NamedTextColor.RED))
            .append(Component.text("╚██████╔╝╚█████╔╝╚█████╔╝██████╔╝██████╦╝░░░██║░░░███████╗\n").color(NamedTextColor.RED))
            .append(Component.text("░╚═════╝░░╚════╝░░╚════╝░╚═════╝░╚═════╝░░░░╚═╝░░░╚══════╝").color(NamedTextColor.RED))
            .build();
            
        Bukkit.getConsoleSender().sendMessage(goodbyeArt);
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

    private boolean setupChat() {
        RegisteredServiceProvider<net.milkbowl.vault.chat.Chat> rsp = 
            getServer().getServicesManager().getRegistration(net.milkbowl.vault.chat.Chat.class);
        if (rsp == null) {
            return false;
        }
        chat = rsp.getProvider();
        return chat != null;
    }

    public boolean isEconomyEnabled() {
        return economyEnabled;
    }

    // Add getter for economy
    public static Economy getEconomy() {
        return econ;
    }

    public static net.milkbowl.vault.chat.Chat getChat() {
        return chat;
    }

    public boolean isVaultChatEnabled() {
        return vaultChatEnabled;
    }
}