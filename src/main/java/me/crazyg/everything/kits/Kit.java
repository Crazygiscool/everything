package me.crazyg.everything.kits;

import java.util.*;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;

public class Kit implements ConfigurationSerializable {
    private final String name;
    private final List<ItemStack> items;
    private final long cooldown;
    private final String permission;

    public Kit(String name, List<ItemStack> items, long cooldown, String permission) {
        this.name = name;
        this.items = items;
        this.cooldown = cooldown;
        this.permission = permission;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("items", items);
        map.put("cooldown", cooldown);
        map.put("permission", permission);
        return map;
    }

    public static Kit deserialize(Map<String, Object> map) {
        String name = (String) map.get("name");
        @SuppressWarnings("unchecked")
        List<ItemStack> items = (List<ItemStack>) map.get("items");
        long cooldown = ((Number) map.get("cooldown")).longValue();
        String permission = (String) map.get("permission");
        return new Kit(name, items, cooldown, permission);
    }

    // Getters
    public String getName() { return name; }
    public List<ItemStack> getItems() { return Collections.unmodifiableList(items); }
    public long getCooldown() { return cooldown; }
    public String getPermission() { return permission; }
}