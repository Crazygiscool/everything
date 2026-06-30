package me.crazyg.everything.listeners;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.util.CachedServerIcon;

public class ServerListListener implements Listener {

    private final Everything plugin;
    private final MiniMessage miniMessage;
    private final Random random;
    private final long startTime;

    private File motdFile;
    private FileConfiguration motdConfig;
    private List<String> motdLines;
    private List<String> maintenanceMotdLines;
    private CachedServerIcon normalIcon;
    private CachedServerIcon maintenanceIcon;

    public ServerListListener(Everything plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.random = new Random();
        this.startTime = System.currentTimeMillis();

        File resource = new File(plugin.getDataFolder(), "motd.yml");
        if (!resource.exists()) {
            plugin.saveResource("motd.yml", false);
        }

        this.motdFile = resource;
        load();
    }

    public void reload() {
        load();
    }

    private void load() {
        if (!motdFile.exists()) {
            try {
                motdFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create motd.yml!");
            }
        }
        motdConfig = YamlConfiguration.loadConfiguration(motdFile);

        motdLines = motdConfig.getStringList("lines").stream()
                .map(this::legacyToMiniMessage)
                .collect(Collectors.toList());

        maintenanceMotdLines = motdConfig.getStringList("maintenance-lines").stream()
                .map(this::legacyToMiniMessage)
                .collect(Collectors.toList());

        normalIcon = loadIcon(motdConfig.getString("icon", "server-icon.png"));
        maintenanceIcon = loadIcon(motdConfig.getString("icon-maintenance", "server-icon-maintenance.png"));
    }

    private String legacyToMiniMessage(String text) {
        return text.replace("&0", "<black>").replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>").replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
                .replace("&6", "<gold>").replace("&7", "<gray>")
                .replace("&8", "<dark_gray>").replace("&9", "<blue>")
                .replace("&a", "<green>").replace("&b", "<aqua>")
                .replace("&c", "<red>").replace("&d", "<light_purple>")
                .replace("&e", "<yellow>").replace("&f", "<white>")
                .replace("&k", "<obfuscated>").replace("&l", "<bold>")
                .replace("&m", "<strikethrough>").replace("&n", "<underline>")
                .replace("&o", "<italic>").replace("&r", "<reset>");
    }

    private String replacePlaceholders(String text) {
        int online = Bukkit.getOnlinePlayers().size();
        long uptime = (System.currentTimeMillis() - startTime) / 1000;
        long hours = uptime / 3600;
        long minutes = (uptime % 3600) / 60;

        return text.replace("%player_count%", String.valueOf(online))
                .replace("%max_players%", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("%server_name%", Bukkit.getServer().getName())
                .replace("%server_version%", Bukkit.getBukkitVersion())
                .replace("%server_uptime%", hours + "h " + minutes + "m");
    }

    private CachedServerIcon loadIcon(String path) {
        try {
            File iconFile = new File(plugin.getDataFolder(), path);
            if (iconFile.exists()) {
                BufferedImage image = ImageIO.read(iconFile);
                if (image != null) {
                    Method method = Bukkit.class.getMethod("createServerIcon", BufferedImage.class);
                    return (CachedServerIcon) method.invoke(null, image);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not load server icon: " + path);
        }
        return null;
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        boolean maintenance = plugin.getConfig().getBoolean("maintenance-mode", false);

        // MOTD
        List<String> pool = maintenance ? maintenanceMotdLines : motdLines;
        if (!pool.isEmpty()) {
            String line = pool.get(random.nextInt(pool.size()));
            line = replacePlaceholders(line);
            Component component = miniMessage.deserialize(line);
            event.setMotd(LegacyComponentSerializer.legacySection().serialize(component));
        }

        // Player count
        if (maintenance && motdConfig.getBoolean("hide-during-maintenance", true)) {
            event.setMaxPlayers(0);
        } else {
            int realOnline = Bukkit.getOnlinePlayers().size();
            int persist = motdConfig.getInt("persist-online", 10);
            int displayMax = Math.max(realOnline, persist);

            int customMax = motdConfig.getInt("custom-max", -1);
            if (customMax > 0) {
                displayMax = Math.min(displayMax, customMax);
            }

            event.setMaxPlayers(displayMax);
        }

        // Server icon
        CachedServerIcon icon = maintenance ? maintenanceIcon : normalIcon;
        if (icon != null) {
            try {
                event.setServerIcon(icon);
            } catch (Exception ignored) {
            }
        }
    }
}
