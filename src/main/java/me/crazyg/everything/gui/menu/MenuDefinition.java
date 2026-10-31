package me.crazyg.everything.gui.menu;

import me.crazyg.everything.Everything;
import me.crazyg.everything.utils.AdventureCompat;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MenuDefinition {

    private final String menuId;
    private final String titleRaw;
    private final int size;
    private final org.bukkit.Material filler;
    private final String unauthorizedMode;
    private final Map<Integer, MenuItemDefinition> itemsBySlot;
    private final List<MenuItemDefinition> items;

    public MenuDefinition(String menuId,
                          String titleRaw,
                          int size,
                          org.bukkit.Material filler,
                          String unauthorizedMode,
                          List<MenuItemDefinition> items) {
        this.menuId = menuId;
        this.titleRaw = titleRaw;
        this.size = size;
        this.filler = filler == null ? org.bukkit.Material.AIR : filler;
        this.unauthorizedMode = unauthorizedMode;
        this.items = List.copyOf(items);
        Map<Integer, MenuItemDefinition> bySlot = new LinkedHashMap<>();
        for (MenuItemDefinition item :
                this.items) {
            bySlot.put(item.slot(), item);
        }
        this.itemsBySlot = Map.copyOf(bySlot);
    }

    public String menuId() {
        return menuId;
    }

    public String titleRaw() {
        return titleRaw;
    }

    public int size() {
        return size;
    }

    public org.bukkit.Material filler() {
        return filler;
    }

    public String unauthorizedMode() {
        return unauthorizedMode;
    }

    public List<MenuItemDefinition> items() {
        return items;
    }

    public Map<Integer, MenuItemDefinition> itemsBySlot() {
        return itemsBySlot;
    }

    public static MenuDefinition load(Everything plugin,
                                     String menuId,
                                     FileConfiguration config) {
        String path = "menus." + menuId;
        ConfigurationSection root = config.getConfigurationSection(path);
        if (root == null) {
            if (plugin != null) {
                plugin.getLogger().warning("Menu definition not found: " + menuId);
            }
            return empty(menuId);
        }

        String title = root.getString("title", "<gray>" + menuId + "</gray>");
        int size = clampSize(root.getInt("size", 27));
        org.bukkit.Material filler = parseMaterial(
            plugin, "menus." + menuId + ".filler",
            root.getString("filler", null));

        String unauthorized = root.getString("unauthorized-mode", "lock");
        if (!"hide".equalsIgnoreCase(unauthorized)
                && !"lock".equalsIgnoreCase(unauthorized)) {
            unauthorized = "lock";
        }

        ConfigurationSection items = root.getConfigurationSection("items");
        List<MenuItemDefinition> parsed = new ArrayList<>();
        Set<Integer> seen = new java.util.HashSet<>();
        if (items != null) {
            for (String id : items.getKeys(false)) {
                ConfigurationSection child = items.getConfigurationSection(id);
                MenuItemDefinition item = MenuItemDefinition.from(id, child);
                if (item == null) continue;
                if (!seen.add(item.slot())) {
                    if (plugin != null) {
                        plugin.getLogger().warning(
                            "Duplicate slot " + item.slot()
                                + " in menu '" + menuId + "'");
                    }
                    continue;
                }
                if (item.slot() < 0 || item.slot() >= size) {
                    if (plugin != null) {
                        plugin.getLogger().warning(
                            "Invalid slot " + item.slot()
                                + " in menu '" + menuId + "'");
                    }
                    continue;
                }
                parsed.add(item);
            }
        }
        parsed.sort(Comparator.comparingInt(MenuItemDefinition::slot));
        return new MenuDefinition(menuId, title, size, filler, unauthorized, parsed);
    }

    public static int clampSize(int value) {
        if (value < 9) {
            return 9;
        }
        if (value > 54) {
            return 54;
        }
        int nearest = 9;
        for (int size = 9; size <= 54; size += 9) {
            if (Math.abs(size - value) < Math.abs(nearest - value)) {
                nearest = size;
            }
        }
        return nearest;
    }

    public static MenuDefinition empty(String menuId) {
        List<MenuItemDefinition> items = List.of();
        return new MenuDefinition(menuId, "<gray>Menu: " + menuId + "</gray>",
            27, null, "lock", items);
    }

    private static org.bukkit.Material parseMaterial(Everything plugin,
                                                     String path,
                                                     String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        org.bukkit.Material material = raw == null ? null
            : org.bukkit.Material.getMaterial(raw.toUpperCase(Locale.ROOT));
        if (material == null) {
            if (plugin != null) {
                plugin.getLogger().warning(
                    "Unknown material '" + raw + "' in " + path);
            }
            return org.bukkit.Material.BARRIER;
        }
        return material;
    }
}
