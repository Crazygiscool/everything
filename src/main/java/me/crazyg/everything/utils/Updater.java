package me.crazyg.everything.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
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
    private String downloadUrl;
    private boolean updateAvailable = false;
    private static final String GITHUB_API_URL = "https://api.github.com/repos/Crazygiscool/everything/releases";
    private static final String UPDATE_FOLDER = "updates";

    public Updater(Everything plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getPluginMeta().getVersion();
        checkForUpdates();
    }

    private void checkForUpdates() {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = URI.create(GITHUB_API_URL).toURL();
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

                        JsonArray assets = latestRelease.getAsJsonArray("assets");
                        if (assets != null && assets.size() > 0) {
                            downloadUrl = assets.get(0).getAsJsonObject().get("browser_download_url").getAsString();
                        }

                        if (!currentVersion.equals(latestVersion)) {
                            updateAvailable = true;
                            plugin.getLogger().info(Component.text()
                                .append(Component.text("A new update is available! ").color(NamedTextColor.GREEN))
                                .append(Component.text("Current version: ").color(NamedTextColor.YELLOW))
                                .append(Component.text(currentVersion).color(NamedTextColor.WHITE))
                                .append(Component.text(", Latest version: ").color(NamedTextColor.YELLOW))
                                .append(Component.text(latestVersion).color(NamedTextColor.WHITE))
                                .build().toString());
                            downloadUpdate();
                        }
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().severe(Component.text("Failed to check for updates: " + e.getMessage())
                    .color(NamedTextColor.RED)
                    .toString());
            }
        });
    }

    private void downloadUpdate() {
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            plugin.getLogger().severe(Component.text("No download URL available for the update!")
                .color(NamedTextColor.RED)
                .toString());
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                File updateFolder = new File(plugin.getDataFolder().getParentFile(), UPDATE_FOLDER);
                if (!updateFolder.exists()) {
                    updateFolder.mkdir();
                }

                String fileName = "everything-" + latestVersion + ".jar";
                File outputFile = new File(updateFolder, fileName);

                URL url = URI.create(downloadUrl).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Accept", "application/octet-stream");

                try (InputStream in = connection.getInputStream();
                     FileOutputStream out = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    long totalBytesRead = 0;
                    long fileSize = connection.getContentLengthLong();

                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        
                        if (fileSize > 0) {
                            int progress = (int) ((totalBytesRead * 100) / fileSize);
                            if (progress % 5 == 0) {
                                plugin.getLogger().info(Component.text("Download progress: " + progress + "%")
                                    .color(NamedTextColor.YELLOW)
                                    .toString());
                            }
                        }
                    }
                }

                plugin.getLogger().info(Component.text()
                    .append(Component.text("Update downloaded successfully to: ").color(NamedTextColor.GREEN))
                    .append(Component.text(outputFile.getAbsolutePath()).color(NamedTextColor.WHITE))
                    .build().toString());
                
                plugin.getLogger().info(Component.text("Please restart your server to apply the update.")
                    .color(NamedTextColor.YELLOW)
                    .toString());

            } catch (IOException e) {
                plugin.getLogger().severe(Component.text("Failed to download update: " + e.getMessage())
                    .color(NamedTextColor.RED)
                    .toString());
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
                .append(Component.text(latestVersion).color(NamedTextColor.WHITE))
                .build());
        }
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}