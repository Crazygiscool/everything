package me.crazyg.everything;

import java.io.File;
import me.crazyg.everything.blocklog.BlockLogCommand;
import me.crazyg.everything.blocklog.BlockLogConfig;
import me.crazyg.everything.blocklog.BlockLogDatabase;
import me.crazyg.everything.blocklog.BlockLogListener;
import me.crazyg.everything.blocklog.InspectManager;
import me.crazyg.everything.blocklog.RollbackManager;
import me.crazyg.everything.blocklog.SelectionCommand;
import me.crazyg.everything.blocklog.SelectionManager;
import me.crazyg.everything.commands.*;
import me.crazyg.everything.listeners.*;
import me.crazyg.everything.utils.*;
import me.crazyg.everything.utils.chat.ChatStorage;
import me.crazyg.everything.utils.chat.EverythingChat;
import me.crazyg.everything.utils.economy.EcoProvider;
import me.crazyg.everything.utils.economy.EcoStorage;
import me.crazyg.everything.utils.FeatureManager;
import me.crazyg.everything.utils.PlayerSettingsManager;
import me.crazyg.everything.utils.particle.ParticleManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Everything extends JavaPlugin {

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
        Component fancy = PLUGIN_PREFIX.append(message);
        if (!(sender instanceof Player player)) {
            AdventureCompat.sendMessage(sender, fancy);
            return;
        }

        if (AdventureCompat.supportsInteractive(player)
                && AdventureCompat.tryNativeSend(player, fancy)) {
            return;
        }

        Component fallback = fancy;
        java.util.Set<String> commands = collectRunCommands(fancy);
        if (!commands.isEmpty()) {
            fallback = fancy.append(
                net.kyori.adventure.text.Component.text(
                    " (Run: " + String.join(", ", commands) + ")")
                    .color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
        }
        AdventureCompat.sendMessage(sender, fallback);
    }

    private static java.util.Set<String> collectRunCommands(
            net.kyori.adventure.text.Component component) {
        java.util.LinkedHashSet<String> commands = new java.util.LinkedHashSet<>();
        collectRunCommands(component, commands);
        return commands;
    }

    private static void collectRunCommands(
            net.kyori.adventure.text.Component component,
            java.util.LinkedHashSet<String> commands) {
        net.kyori.adventure.text.event.ClickEvent click =
            component.clickEvent();
        if (click != null
                && click.action() == net.kyori.adventure.text.event.ClickEvent.Action.RUN_COMMAND
                && click.value() != null && !click.value().trim().isEmpty()) {
            commands.add(click.value().trim());
        }

        for (net.kyori.adventure.text.Component child : component.children()) {
            collectRunCommands(child, commands);
        }
    }

    private static EcoProvider econ = null;
    private Object externalChatProvider;
    private boolean economyEnabled = false;
    private boolean vaultChatEnabled = false;

    private WarpCommand warpCommand;
    private SetSpawnCommand spawnCommand;
    private ServerListListener serverListListener;
    private EcoStorage ecoStorage;
    private ChatStorage chatStorage;

private BlockLogDatabase blockLogDatabase;
    private BlockLogListener blockLogListener;
    private SelectionManager selectionManager;
    private InspectManager inspectManager;
    private BlockLogCommand blockLogCommand;
    private SelectionCommand selectionCommand;
    private RollbackManager rollbackManager;

    private ParticleManager particleManager;
    private TeleportManager teleportManager;
    private PlayerSettingsManager playerSettingsManager;
    private FeatureManager featureManager;

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
        // copyDefaults merges missing keys from the embedded default config
        // into memory; saveConfig() persists them to the user's file without
        // wiping their custom values. (saveDefaultConfig() alone would not
        // persist newly-added default keys on upgrade.)
        getConfig().options().copyDefaults(true);
        saveConfig();

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

        // --- Economy setup (internal first) ---
        // Everything keeps its own economy and appearance preferences even
        // when Vault is not installed. Vault is used only as an optional
        // compatibility service, and the plugin never requires its classes on
        // the classpath.
        this.ecoStorage = new EcoStorage(this);
        this.econ = new EcoProvider(this.ecoStorage);
        this.economyEnabled = true;

        this.chatStorage = new ChatStorage(this);
        this.vaultChatEnabled = false;
        this.externalChatProvider = null;

        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            try {
                Class<?> economyType = Class.forName("net.milkbowl.vault.economy.Economy");
                Class<?> bridgeType = Class.forName("me.crazyg.everything.utils.economy.VaultEconomyBridge");
                Object bridge = bridgeType.getDeclaredConstructor(
                    me.crazyg.everything.utils.economy.EcoProvider.class)
                    .newInstance(this.econ);
                java.lang.reflect.Method register = Bukkit.getServicesManager().getClass()
                    .getMethod("register", Class.class, Object.class,
                               org.bukkit.plugin.Plugin.class,
                               org.bukkit.plugin.ServicePriority.class);
                register.invoke(Bukkit.getServicesManager(), economyType, bridge,
                    this, org.bukkit.plugin.ServicePriority.Highest);
            } catch (Exception e) {
                getLogger().warning("Vault economy compatibility could not be registered: " + e.getMessage());
            }

            try {
                Class<?> chatType = Class.forName("net.milkbowl.vault.chat.Chat");
                Class<?> providerType = Class.forName("me.crazyg.everything.utils.chat.EverythingChat");
                Object provider = providerType.getDeclaredConstructor(Everything.class, ChatStorage.class)
                    .newInstance(this, this.chatStorage);
                java.lang.reflect.Method register = Bukkit.getServicesManager().getClass()
                    .getMethod("register", Class.class, Object.class,
                               org.bukkit.plugin.Plugin.class,
                               org.bukkit.plugin.ServicePriority.class);
                register.invoke(Bukkit.getServicesManager(), chatType, provider, this,
                    org.bukkit.plugin.ServicePriority.Highest);
                this.externalChatProvider = provider;
                this.vaultChatEnabled = true;
                if (setupChat()) {
                    getLogger().info("Vault chat compatibility is available.");
                }
            } catch (Exception e) {
                getLogger().warning("Vault chat compatibility could not be enabled: " + e.getMessage());
            }
            getLogger().info("Vault compatibility is active; Everything continues to use its internal economy provider.");
        } else {
            getLogger().info("Vault is not installed; Everything is using its internal economy and chat provider.");
        }

        // --- Particle & Teleport Managers ---
        this.featureManager = new FeatureManager(this);
        this.particleManager = new ParticleManager(this);
        this.teleportManager = new TeleportManager(this, particleManager);
        this.playerSettingsManager = new PlayerSettingsManager(this);

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
        commandManager.registerCommand("kill", new KillCommand(this));
        commandManager.registerCommand("god", new GodCommand(this));
        commandManager.registerCommand("report", new ReportCommand(this));
        commandManager.registerCommand(
            "everything",
            new EverythingCommand(this)
        );
        HomeCommand homeCommand = new HomeCommand(this);
        commandManager.registerCommand("home", homeCommand);
        commandManager.registerCommand("sethome", homeCommand);
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

        GamemodeCommand gamemodeExecutor = new GamemodeCommand(this);
        commandManager.registerCommand("gmc", gamemodeExecutor);
        commandManager.registerCommand("gms", gamemodeExecutor);
        commandManager.registerCommand("gmsp", gamemodeExecutor);
        commandManager.registerCommand("gma", gamemodeExecutor);

        // Remove duplicate direct setExecutor for setspawn, spawn, balance, pay

        // --- Block Logging & Rollback ---
        if (BlockLogConfig.isEnabled(getConfig())) {
            this.blockLogDatabase = new BlockLogDatabase(this);
            this.blockLogDatabase.init();
            if (BlockLogConfig.pruneOnStartup(getConfig())) {
                int removed = this.blockLogDatabase.pruneOlderThan(
                    BlockLogConfig.maxRetentionDays(getConfig()),
                    null, null, null, true);
                getLogger().info("Pruned " + removed
                    + " old block-log entries on startup.");
            }
            this.rollbackManager = new RollbackManager(this, blockLogDatabase);
            this.selectionManager = new SelectionManager(this);
            this.inspectManager = new InspectManager(this, blockLogDatabase);
            this.blockLogListener =
                new BlockLogListener(this, blockLogDatabase);
            this.blockLogCommand = new BlockLogCommand(
                this, blockLogDatabase, rollbackManager,
                selectionManager, inspectManager);
            this.selectionCommand = new SelectionCommand(
                this, selectionManager);

            getServer().getPluginManager().registerEvents(
                blockLogListener, this);
            getServer().getPluginManager().registerEvents(
                inspectManager, this);
            getServer().getPluginManager().registerEvents(
                selectionManager, this);
            getServer().getPluginManager().registerEvents(
                new SlashPrefixListener(this, selectionCommand), this);
            getServer().getPluginManager().registerEvents(
                new SlashPrefixTabCompleteListener(this, selectionCommand), this);

            getCommand("rollback").setExecutor(blockLogCommand);
            getCommand("lookup").setExecutor(blockLogCommand);
            getCommand("inspect").setExecutor(blockLogCommand);
            getCommand("lb").setExecutor(blockLogCommand);

            // Selection commands (getCommand("//x") resolves via plugin.yml keys)
            for (String selCmd : new String[] {
                    "wand", "pos1", "pos2", "hpos1", "hpos2",
                    "expand", "contract", "outset", "inset", "shift",
                    "sel", "desel", "size", "count"}) {
                org.bukkit.command.PluginCommand pc = getCommand(selCmd);
                if (pc != null) {
                    pc.setExecutor(selectionCommand);
                    pc.setTabCompleter(selectionCommand);
                }
            }

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
        boolean autoUpdate = getFeatureManager() != null
            ? getFeatureManager().getBoolean("auto-update",
                getConfig().getBoolean("auto-update", true))
            : getConfig().getBoolean("auto-update", true);

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
            "\n░██████╗░░█████╗░░█████╗░██████╗░██████╗░██╗░░░██╗███████╗\n██╔════╝░██╔══██╗██╔══██╗██╔══██╗██╔══██╗╚██╗░██╔╝██╔════╝\n██║░░██╗░██║░░██║██║░░██║██║░░██║██████╦╝░╚████╔╝░█████╗░░\n██║░░╚██╗██║░░██║██║░░██║██║░░██║██╔══██╗░░╚██╔╝░░██╔══╝░░\n╚██████╔╝╚█████╔╝╚█████╔╝██████╔╝██████╦╝░░░██║░░░███████╗\n░╚═════╝░░╚════╝░░╚════╝░░╚════╝░╚═════╝░░░░╚═╝░░░╚══════╝").color(NamedTextColor.RED);

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
        return getServer().getPluginManager().getPlugin("Vault") != null;
    }

    private boolean setupChat() {
        return externalChatProvider != null;
    }

    public boolean isEconomyEnabled() {
        return economyEnabled;
    }

    public static Object getChatProvider() {
        return getInstance() == null ? null : getInstance().externalChatProvider;
    }

    public Object getExternalChatProvider() {
        return externalChatProvider;
    }

    public static EcoProvider getEconomy() {
        return getInstance() == null ? null : getInstance().econ;
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

    public SelectionManager getSelectionManager() {
        return selectionManager;
    }

    public InspectManager getInspectManager() {
        return inspectManager;
    }

    public RollbackManager getRollbackManager() {
        return rollbackManager;
    }

    public BlockLogListener getBlockLogListener() {
        return blockLogListener;
    }

    public ParticleManager getParticleManager() {
        return particleManager;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public PlayerSettingsManager getPlayerSettingsManager() {
        return playerSettingsManager;
    }

    public FeatureManager getFeatureManager() {
        return featureManager;
    }

    /**
     * Persists and applies a config-driven feature toggle. Returns what the
     * caller can expect after the method returns.
     */
    public FeatureManager.FeatureApplyOutcome applyFeatureToggle(String path,
                                                                boolean value) {
        if (path == null || path.trim().isEmpty()
                || getFeatureManager() == null) {
            return FeatureManager.FeatureApplyOutcome.INVALID_PATH;
        }
        String cleanPath = path.trim();
        if (!isSupportedFeaturePath(cleanPath)) {
            getLogger().warning("Rejected unknown feature toggle: " + cleanPath);
            return FeatureManager.FeatureApplyOutcome.INVALID_PATH;
        }

        if (!getFeatureManager().setBooleanSilently(cleanPath, value)) {
            return FeatureManager.FeatureApplyOutcome.NOT_APPLIED;
        }

        if (cleanPath.startsWith("particles.")) {
            getParticleManager().loadConfig();
            return FeatureManager.FeatureApplyOutcome.LIVE;
        }

        if (cleanPath.startsWith("blocklog.log-")
                || cleanPath.equals("blocklog.log-natural")
                || cleanPath.equals("blocklog.log-fluid")) {
            if (getBlockLogListener() != null) {
                getBlockLogListener().reloadConfig();
            }
            return FeatureManager.FeatureApplyOutcome.LIVE;
        }

        if (cleanPath.equals("blocklog.enabled")) {
            if (getBlockLogListener() != null) {
                getBlockLogListener().reloadConfig();
            }
            return FeatureManager.FeatureApplyOutcome.RESTART_REQUIRED;
        }

        if (cleanPath.equals("cosmetics.join-messages")) {
            return FeatureManager.FeatureApplyOutcome.LIVE;
        }

        if (cleanPath.equals("auto-update")
                || cleanPath.equals("automation.update")
                || cleanPath.startsWith("automation.")) {
            return FeatureManager.FeatureApplyOutcome.RESTART_REQUIRED;
        }

        return FeatureManager.FeatureApplyOutcome.LIVE;
    }

    private boolean isSupportedFeaturePath(String path) {
        return path.equals("particles.master")
            || path.startsWith("particles.commands.")
            || path.equals("blocklog.enabled")
            || path.startsWith("blocklog.log-")
            || path.equals("cosmetics.join-messages")
            || path.equals("auto-update")
            || path.equals("automation.update")
            || path.startsWith("automation.");
    }
}
