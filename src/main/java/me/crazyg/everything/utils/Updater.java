package me.crazyg.everything.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;


public class Updater implements Listener {
    private final Everything plugin;
    private final String currentVersion;
    private String latestVersion;
    private boolean updateAvailable = false;
    private static final String GITHUB_API_URL = "https://api.github.com/repos/Crazygiscool/everything/releases";

    public Updater(Everything plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        checkForUpdates();
    }

    private void checkForUpdates() {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(GITHUB_API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JsonArray releases = JsonParser.parseString(response.toString()).getAsJsonArray();
                    if (releases.size() > 0) {
                        JsonObject latestRelease = releases.get(0).getAsJsonObject();
                        latestVersion = latestRelease.get("tag_name").getAsString();

                        // Compare versions
                        if (!currentVersion.equals(latestVersion)) {
                            updateAvailable = true;
                            plugin.getLogger().info("A new update is available! Current version: " + currentVersion + 
                                                  ", Latest version: " + latestVersion);
                        }
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (updateAvailable && player.hasPermission("everything.update")) {
            player.sendMessage(Component.text()
                .append(Component.text("A new version of Everything is available! ").color(NamedTextColor.GREEN))
                .append(Component.text("Current version: ").color(NamedTextColor.YELLOW))
                .append(Component.text(currentVersion).color(NamedTextColor.WHITE))
                .append(Component.text(", Latest version: ").color(NamedTextColor.YELLOW))
                .append(Component.text(latestVersion).color(NamedTextColor.WHITE)));
        }
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}