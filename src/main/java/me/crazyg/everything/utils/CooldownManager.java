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
            player.sendMessage(Component.text("You must wait " + secondsLeft + " seconds before using this command again!")
                    .color(NamedTextColor.RED));
            return true;
        }
        
        commandCooldowns.remove(player.getUniqueId());
        return false;
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