package me.crazyg.everything.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class CooldownManager {
    private final Map<String, Map<UUID, Long>> cooldowns = new HashMap<>();
    
    public void setCooldown(String command, UUID player, long seconds) {
        if (seconds <= 0) return;
        cooldowns.computeIfAbsent(command, k -> new HashMap<>())
                .put(player, System.currentTimeMillis() + (seconds * 1000));
    }
    
    public boolean hasCooldown(String command, Player player) {
        Map<UUID, Long> commandCooldowns = cooldowns.get(command);
        if (commandCooldowns == null) return false;
        
        Long cooldownTime = commandCooldowns.get(player.getUniqueId());
        if (cooldownTime == null) return false;
        
        if (cooldownTime > System.currentTimeMillis()) {
            long secondsLeft = (cooldownTime - System.currentTimeMillis()) / 1000;
            AdventureCompat.sendMessage(player, Component.text("You must wait " + secondsLeft + " seconds before using this command again!")
                    .color(NamedTextColor.RED));
            return true;
        }
        
        commandCooldowns.remove(player.getUniqueId());
        return false;
    }

    public boolean hasCooldownRaw(String command, UUID uuid) {
        Map<UUID, Long> commandCooldowns = cooldowns.get(command);
        if (commandCooldowns == null) return false;

        Long cooldownTime = commandCooldowns.get(uuid);
        if (cooldownTime == null) return false;

        if (cooldownTime > System.currentTimeMillis()) {
            return true;
        }

        commandCooldowns.remove(uuid);
        return false;
    }

    public long getRemainingSeconds(String command, UUID uuid) {
        Map<UUID, Long> commandCooldowns = cooldowns.get(command);
        if (commandCooldowns == null) return 0;

        Long cooldownTime = commandCooldowns.get(uuid);
        if (cooldownTime == null) return 0;

        long remaining = (cooldownTime - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }
    
    public void removeCooldown(String command, UUID player) {
        Map<UUID, Long> commandCooldowns = cooldowns.get(command);
        if (commandCooldowns != null) {
            commandCooldowns.remove(player);
        }
    }
    
    public void clearAllCooldowns() {
        cooldowns.clear();
    }
}