package me.crazyg.everything;

import java.io.File;
import me.crazyg.everything.blocklog.BlockLogCommand;
import me.crazyg.everything.blocklog.BlockLogDatabase;
import me.crazyg.everything.blocklog.BlockLogListener;
import me.crazyg.everything.blocklog.InspectWand;
import me.crazyg.everything.blocklog.RollbackManager;
import me.crazyg.everything.commands.*;
import me.crazyg.everything.listeners.*;
import me.crazyg.everything.utils.*;
import me.crazyg.everything.utils.chat.ChatStorage;
import me.crazyg.everything.utils.chat.EverythingChat;
import me.crazyg.everything.utils.economy.EcoProvider;
import me.crazyg.everything.utils.economy.EcoStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class Everything extends JavaPlugin {

    private static Everything instance;

    public static Everything getInstance() {
        return instance;
    }

    // Fancy prefix for all plugin messages
    public static final Component PLUGIN_PREFIX = Component.text("")
        .append(Component.text("❖ ").color(NamedTextColor.AQUA))
        .append(
            Component.text("Everything")
                .color(NamedTextColor.BLUE)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD)
                .decorate(net.kyori.adventure.text.format.TextDecoration.ITALIC)
        )
        .append(Component.text(" » ").color(NamedTextColor.AQUA));

    // Utility for sending a fancy message with prefix
    public static void sendFancy(CommandSender sender, Component message) {
        AdventureCompat.sendMessage(sender, PLUGIN_PREFIX.append(message));
    }

    private static Economy econ = null;
    private static net.milkbowl.vault.chat.Chat chat = null;
    private boolean economyEnabled = false;
    private boolean vaultChatEnabled = false;

    private WarpCommand warpCommand;
    private SetSpawnCommand spawnCommand;
    private ServerListListener serverListListener;
    private EcoStorage ecoStorage;
    private ChatStorage chatStorage;

    private BlockLogDatabase blockLogDatabase;
    private BlockLogListener blockLogListener;
    private InspectWand inspectWand;
    private RollbackManager rollbackManager;
    private BlockLogCommand blockLogCommand;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig(); // <-- Make sure this is first!
        // Now read config values, register listeners, etc.

        // --- bstats init ---
        int pluginId = 28514;
        Metrics metrics = new Metrics(this, pluginId);

        metrics.addCustomChart(
            new Metrics.SimplePie("plugin_version", () ->
                getDescription().getVersion()
            )
        );
        metrics.addCustomChart(
            new Metrics.SimplePie("cpu_cores", () ->
                String.valueOf(Runtime.getRuntime().availableProcessors())
            )
        );
        metrics.addCustomChart(
            new Metrics.SimplePie("minecraft_version", () -> {
                String v = Bukkit.getBukkitVersion();
                int dash = v.indexOf('-');
                return dash > 0 ? v.substring(0, dash) : v;
            })
        );
        metrics.addCustomChart(
            new Metrics.SimplePie("server_region", () ->
                java.util.TimeZone.getDefault().getDisplayName()
            )
        );
        metrics.addCustomChart(
            new Metrics.SimplePie("os_arch", () ->
                System.getProperty("os.arch")
            )
        );

        // --- Config Loading ---
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        // --- Logging ---
        getLogger().info("THIS PLUGIN IS WRITTEN BY CRAZYG");
        getLogger().info("THANK YOU FOR USING THE PLUGIN");

        // ASCII art with Adventure API
        Component asciiArt = Component.text(
            "\n███████╗██╗░░░██╗███████╗██████╗░██╗░░░██╗████████╗██╗░░██╗██╗███╗░░██╗░██████╗░\n██╔════╝██║░░░██║██╔════╝██╔══██╗╚██╗░██╔╝╚══██╔══╝██║░░██║██║████╗░██║██╔════╝░\n█████╗░░╚██╗░██╔╝█████╗░░██████╔╝░╚████╔╝░░░░██║░░░███████║██║██╔██╗██║██║░░██╗░\n██╔══╝░░░╚████╔╝░██╔══╝░░██╔══██╗░░╚██╔╝░░░░░██║░░░██╔══██║██║██║╚████║██║░░╚██╗\n███████╗░░╚██╔╝░░███████╗██║░░██║░░░██║░░░░░░██║░░░██║░░██║██║██║░╚███║╚██████╔╝\n╚══════╝░░░╚═╝░░░╚══════╝╚═╝░░╚═╝░░░╚═╝░░░░░░╚═╝░░░╚═╝░░╚═╝╚═╝╚═╝░░╚══╝░╚═════╝░  "
        )
            .color(NamedTextColor.GOLD)
            .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD);

        AdventureCompat.sendMessage(Bukkit.getConsoleSender(), asciiArt);

        // --- Economy & Vault Setup ---
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            // Register EverythingEconomy
            EcoStorage storage = new EcoStorage(this);
            this.ecoStorage = storage;
            EcoProvider ecoProvider = new EcoProvider(storage);

            Bukkit.getServicesManager().register(
                Economy.class,
                ecoProvider,
                this,
                org.bukkit.plugin.ServicePriority.Highest
            );

            getLogger().info("EverythingEconomy registered as Vault provider.");

            // Register EverythingChat
            ChatStorage chatStore = new ChatStorage(this);
            this.chatStorage = chatStore;
            EverythingChat chatProvider = new EverythingChat(this, chatStore);
            Bukkit.getServicesManager().register(
                Chat.class,
                chatProvider,
                this,
                org.bukkit.plugin.ServicePriority.Highest
            );
            getLogger().info("EverythingChat registered as Vault chat provider.");

            // Hook economy
            if (setupEconomy()) {
                getLogger().info("Vault economy hooked successfully!");
                economyEnabled = true;
            } else {
                getLogger().warning(
                    "Vault found but no economy provider detected!\n(Not sure how this happened, there is a Econ lib for everything, unless you modified to code, or I messed something up real bad)"
                );
            }

            // Hook chat
            if (setupChat()) {
                getLogger().info("Vault chat hooked successfully!");
                vaultChatEnabled = true;
            } else {
                getLogger().warning(
                    "Vault found but no chat provider detected!(LuckPerms)"
                );
            }
        } else {
            getLogger().warning(
                "Vault not found! Economy and chat features will be disabled."
            );
        }

        // --- Command Manager ---
        CommandManager commandManager = new CommandManager();
        getCommand("tp").setExecutor(commandManager);
        getCommand("tpa").setExecutor(commandManager);
        getCommand("tpaccept").setExecutor(commandManager);
        getCommand("tpdeny").setExecutor(commandManager);
        getCommand("kill").setExecutor(commandManager);
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
        getCommand("stats").setExecutor(commandManager);
        getCommand("namecolor").setExecutor(commandManager);
        getCommand("warp").setExecutor(commandManager);
        getCommand("eco").setExecutor(commandManager);
        getCommand("help").setExecutor(commandManager);
        getCommand("rtp").setExecutor(commandManager);

        // --- Command Registration ---
        TPACommand tpaCommand = new TPACommand(this);
        commandManager.registerCommand("tp", new TPCommand(this));
        commandManager.registerCommand("tpa", tpaCommand);
        commandManager.registerCommand(
            "tpaccept",
            new TPAcceptCommand(this, tpaCommand)
        );
        commandManager.registerCommand(
            "tpdeny",
            new TPDenyCommand(this, tpaCommand)
        );
        commandManager.registerCommand("kill", new KillCommand());
        commandManager.registerCommand("god", new GodCommand());
        commandManager.registerCommand("report", new ReportCommand(this));
        commandManager.registerCommand(
            "everything",
            new EverythingCommand(this)
        );
        commandManager.registerCommand("home", new HomeCommand(this));
        commandManager.registerCommand("sethome", new HomeCommand(this));
        commandManager.registerCommand("msg", new MessageCommand(this));
        commandManager.registerCommand("reply", new MessageCommand(this));
        commandManager.registerCommand(
            "maintenance",
            new MaintenanceCommand(this)
        );
        this.spawnCommand = new SetSpawnCommand(this);
        commandManager.registerCommand("setspawn", spawnCommand);
        commandManager.registerCommand("spawn", spawnCommand);
        commandManager.registerCommand("pay", new PayCommand(this));
        commandManager.registerCommand("balance", new BalanceCommand(this));
        commandManager.registerCommand("stats", new StatsCommand(this));
        commandManager.registerCommand("namecolor", new NameColorCommand(this));
        this.warpCommand = new WarpCommand(this);
        commandManager.registerCommand("warp", warpCommand);
        commandManager.registerCommand("eco", new EcoCommand(this));
        commandManager.registerCommand("help", new HelpCommand(this));
        commandManager.registerCommand("rtp", new RTPCommand(this));

        GamemodeCommand gamemodeExecutor = new GamemodeCommand();
        commandManager.registerCommand("gmc", gamemodeExecutor);
        commandManager.registerCommand("gms", gamemodeExecutor);
        commandManager.registerCommand("gmsp", gamemodeExecutor);
        commandManager.registerCommand("gma", gamemodeExecutor);

        // Remove duplicate direct setExecutor for setspawn, spawn, balance, pay

        // --- Block Logging & Rollback ---
        if (getConfig().getBoolean("blocklog.enabled", true)) {
            this.blockLogDatabase = new BlockLogDatabase(this);
            this.blockLogDatabase.init();
            this.rollbackManager = new RollbackManager(this, blockLogDatabase);
            this.inspectWand =
                new InspectWand(this, blockLogDatabase, rollbackManager);
            this.blockLogListener =
                new BlockLogListener(this, blockLogDatabase);
            this.blockLogCommand = new BlockLogCommand(
                this, blockLogDatabase, rollbackManager, inspectWand);

            getServer().getPluginManager().registerEvents(
                blockLogListener, this);
            getServer().getPluginManager().registerEvents(
                inspectWand, this);

            getCommand("rollback").setExecutor(blockLogCommand);
            getCommand("lookup").setExecutor(blockLogCommand);
            getCommand("inspect").setExecutor(blockLogCommand);
            getCommand("lb").setExecutor(blockLogCommand);

            getLogger().info("Block logging & rollback enabled.");
        } else {
            getLogger().info("Block logging & rollback disabled in config.yml");
        }

        // --- Listeners ---
        // Pass 'this' (the plugin instance) to the listeners if they need access to config etc.
        getServer()
            .getPluginManager()
            .registerEvents(new onJoinleaveListener(this), this);
        getServer()
            .getPluginManager()
            .registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        ServerListListener serverListListener = new ServerListListener(this);
        this.serverListListener = serverListListener;
        getServer()
            .getPluginManager()
            .registerEvents(serverListListener, this);

        // Initialize updater
        boolean autoUpdate = getConfig().getBoolean("auto-update", true);

        if (autoUpdate) {
            getServer()
                .getPluginManager()
                .registerEvents(new Updater(this), this);
        } else {
            getLogger().info("Auto-update is disabled in config.yml");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Everything plugin disabled.");

        if (blockLogDatabase != null) {
            blockLogDatabase.close();
        }

        applyUpdate();

        Component goodbyeArt = Component.text(
            "\n░██████╗░░█████╗░░█████╗░██████╗░██████╗░██╗░░░██╗███████╗\n██╔════╝░██╔══██╗██╔══██╗██╔══██╗██╔══██╗╚██╗░██╔╝██╔════╝\n██║░░██╗░██║░░██║██║░░██║██║░░██║██████╦╝░╚████╔╝░█████╗░░\n██║░░╚██╗██║░░██║██║░░██║██║░░██║██╔══██╗░░╚██╔╝░░██╔══╝░░\n╚██████╔╝╚█████╔╝╚█████╔╝██████╔╝██████╦╝░░░██║░░░███████╗\n░╚═════╝░░╚════╝░░╚════╝░╚═════╝░╚═════╝░░░░╚═╝░░░╚══════╝"
        ).color(NamedTextColor.RED);

        AdventureCompat.sendMessage(Bukkit.getConsoleSender(), goodbyeArt);
    }

    private void applyUpdate() {
        try {
            File updateFolder = new File(
                getDataFolder().getParentFile(),
                "update"
            );
            if (!updateFolder.isDirectory()) return;

            File[] updates = updateFolder.listFiles(
                (dir, name) -> name.endsWith(".jar") && !name.contains(".bak")
            );
            if (updates == null || updates.length == 0) return;

            File pluginsFolder = getDataFolder().getParentFile();

            // Delete all old Everything JARs from plugins folder
            File[] oldJars = pluginsFolder.listFiles(
                (dir, name) ->
                    name.toLowerCase().startsWith("everything") &&
                    name.endsWith(".jar")
            );
            if (oldJars != null) {
                for (File old : oldJars) {
                    old.delete();
                }
            }

            for (File update : updates) {
                File target = new File(pluginsFolder, update.getName());
                update.renameTo(target);
                getLogger().info(
                    "Applied update: " +
                        update.getName() +
                        ". Restart or reload to activate."
                );
            }

            // Clean up backup files
            File[] backups = updateFolder.listFiles((dir, name) ->
                name.contains(".bak-")
            );
            if (backups != null) {
                for (File bak : backups) {
                    bak.delete();
                }
            }
        } catch (Exception e) {
            getLogger().severe("Failed to apply update: " + e.getMessage());
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer()
            .getServicesManager()
            .getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    private boolean setupChat() {
        RegisteredServiceProvider<net.milkbowl.vault.chat.Chat> rsp =
            getServer()
                .getServicesManager()
                .getRegistration(net.milkbowl.vault.chat.Chat.class);
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

    public WarpCommand getWarpCommand() {
        return warpCommand;
    }

    public SetSpawnCommand getSpawnCommand() {
        return spawnCommand;
    }

    public ServerListListener getServerListListener() {
        return serverListListener;
    }

    public EcoStorage getEcoStorage() {
        return ecoStorage;
    }

    public ChatStorage getChatStorage() {
        return chatStorage;
    }

    public BlockLogDatabase getBlockLogDatabase() {
        return blockLogDatabase;
    }

    public InspectWand getInspectWand() {
        return inspectWand;
    }

    public RollbackManager getRollbackManager() {
        return rollbackManager;
    }
}
