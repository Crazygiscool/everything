package me.crazyg.everything.gui.menu;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Locale;

public record MenuItemDefinition(
    String id,
    int slot,
    Material icon,
    Material iconEnabled,
    Material iconDisabled,
    String rawName,
    List<String> rawLore,
    String permission,
    MenuAction action,
    String target,
    String lockedName,
    String lockedLore,
    boolean chat
) {
    public static MenuItemDefinition from(String id,
                                         ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        int slot = section.getInt("slot", -1);
        if (slot < 0) {
            return null;
        }

        Material icon = parseMaterial(section.getString("icon", "STONE"));
        Material iconEnabled = parseOptional(section.getString(
            "icon-enabled", null));
        Material iconDisabled = parseOptional(section.getString(
            "icon-disabled", null));

        List<String> rawLore = section.getStringList("lore");
        if (rawLore.isEmpty() && section.contains("lore")) {
            String single = section.getString("lore");
            if (single != null && !single.trim().isEmpty()) {
                rawLore = List.of(single);
            }
        }

        return new MenuItemDefinition(id, slot, icon, iconEnabled, iconDisabled,
            section.getString("name", id),
            rawLore,
            section.getString("permission", "").trim(),
            MenuAction.from(section.getString("action", "none")),
            section.getString("target", "").trim(),
            section.getString("locked-name", "&cLocked: " + id).trim(),
            section.getString("locked-lore",
                "&7You do not have permission for this.").trim(),
            section.getBoolean("chat", false));
    }

    private static Material parseOptional(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        return parseMaterial(raw);
    }

    private static Material parseMaterial(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        return Material.getMaterial(raw.trim().toUpperCase(Locale.ROOT));
    }
}
