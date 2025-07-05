package me.crazyg.everything.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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
    // Only notify once per update/download
    private static boolean notifiedUpdate = false;
    private static boolean notifiedDownload = false;
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

                        int cmp = compareVersions(currentVersion, latestVersion);
                        if (cmp < 0) {
                            // Current version is older than latest
                            updateAvailable = true;
                            // Log a simple, readable string to the console
                            plugin.getLogger().info("A new update is available! Current version: " + currentVersion + ", Latest version: " + latestVersion);
                            // Send a beautiful Adventure message to all online players (once)
                            Component updateMsg = me.crazyg.everything.Everything.PLUGIN_PREFIX.append(
                                Component.text()
                                    .append(Component.text("A new update is available! ").color(NamedTextColor.GREEN))
                                    .append(Component.text("Current: ").color(NamedTextColor.YELLOW))
                                    .append(Component.text(currentVersion).color(NamedTextColor.WHITE))
                                    .append(Component.text(" → ").color(NamedTextColor.GRAY))
                                    .append(Component.text(latestVersion).color(NamedTextColor.AQUA).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD))
                                    .append(Component.text(". Download: ").color(NamedTextColor.YELLOW))
                                    .append(Component.text(downloadUrl != null ? downloadUrl : "(no link)").color(NamedTextColor.BLUE).clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl(downloadUrl != null ? downloadUrl : "")))
                                    .build()
                            );
                            if (!notifiedUpdate) {
                                notifiedUpdate = true;
                                org.bukkit.Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(updateMsg));
                                org.bukkit.Bukkit.getConsoleSender().sendMessage(updateMsg);
                            }
                            downloadUpdate();
                        } else if (cmp > 0) {
                            // Current version is newer than latest (test server)
                            plugin.getLogger().info("This server is running a test/development version (" + currentVersion + ") ahead of the latest public release (" + latestVersion + ").");
                        }
                        // If cmp == 0, do nothing (up to date)
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to check for updates: " + e.getMessage());
            }
        });
    }

    private void downloadUpdate() {
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            plugin.getLogger().severe("No download URL available for the update!");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                // Prepare update folder
                File updateFolder = new File(plugin.getDataFolder().getParentFile(), UPDATE_FOLDER);
                if (!updateFolder.exists()) {
                    updateFolder.mkdir();
                }

                // Get the current plugin jar file
                File currentJar = null;
                try {
                    currentJar = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
                } catch (Exception e) {
                    plugin.getLogger().severe("Could not determine the current plugin JAR file: " + e.getMessage());
                    return;
                }
                if (currentJar == null || !currentJar.exists()) {
                    plugin.getLogger().severe("Could not determine the current plugin JAR file!");
                    return;
                }

                // Backup the current jar to the update folder
                File backupFile = new File(updateFolder, currentJar.getName() + ".bak-" + currentVersion);
                try (FileInputStream fis = new FileInputStream(currentJar); FileOutputStream fos = new FileOutputStream(backupFile)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }

                // Download the new jar and overwrite the current one
                URL url = URI.create(downloadUrl).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Accept", "application/octet-stream");

                long fileSize = connection.getContentLengthLong();
                long totalBytesRead = 0;
                int bytesRead;
                byte[] buffer = new byte[1024];

                try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(currentJar)) {
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        if (fileSize > 0) {
                            int progress = (int) ((totalBytesRead * 100) / fileSize);
                            if (progress % 5 == 0) {
                                plugin.getLogger().info("Download progress: " + progress + "%");
                            }
                        }
                    }
                }

                if (!notifiedDownload) {
                    notifiedDownload = true;
                    plugin.getLogger().info("Update downloaded and replaced plugin jar: " + currentJar.getAbsolutePath());
                    plugin.getLogger().info("Backup of old jar saved to: " + backupFile.getAbsolutePath());
                    plugin.getLogger().info("Please restart your server to apply the update.");
                    Component doneMsg = me.crazyg.everything.Everything.PLUGIN_PREFIX.append(
                        Component.text()
                            .append(Component.text("Update downloaded! ").color(NamedTextColor.GREEN))
                            .append(Component.text("Please restart your server to apply the update.").color(NamedTextColor.YELLOW))
                            .build()
                    );
                    org.bukkit.Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(doneMsg));
                    org.bukkit.Bukkit.getConsoleSender().sendMessage(doneMsg);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to download or replace update: " + e.getMessage());
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

    // Compare two version strings (e.g. 1.2.0-BETA, 1.2.0, 2.0.1)
    private int compareVersions(String v1, String v2) {
        String[] a1 = v1.split("[.-]");
        String[] a2 = v2.split("[.-]");
        int len = Math.max(a1.length, a2.length);
        for (int i = 0; i < len; i++) {
            String s1 = i < a1.length ? a1[i] : "0";
            String s2 = i < a2.length ? a2[i] : "0";
            boolean s1Num = s1.matches("\\d+");
            boolean s2Num = s2.matches("\\d+");
            if (s1Num && s2Num) {
                int cmp = Integer.compare(Integer.parseInt(s1), Integer.parseInt(s2));
                if (cmp != 0) return cmp;
            } else if (s1Num) {
                // Numeric is always newer than non-numeric (e.g. 1.2.0 > 1.2.0-BETA)
                return 1;
            } else if (s2Num) {
                return -1;
            } else {
                int cmp = s1.compareToIgnoreCase(s2);
                if (cmp != 0) return cmp;
            }
        }
        return 0;
    }
}