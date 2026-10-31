package me.crazyg.everything.gui.menu;

import me.crazyg.everything.Everything;
import me.crazyg.everything.gui.BaseGUI;
import me.crazyg.everything.gui.help.HelpMainGUI;
import me.crazyg.everything.gui.help.HelpManager;
import me.crazyg.everything.gui.menu.DynamicMenuGUI;
import me.crazyg.everything.utils.AdventureCompat;
import me.crazyg.everything.utils.HybridChatMenu;
import me.crazyg.everything.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Map;

public class DynamicMenuGUI extends BaseGUI {

    private static final int MAX_SLOTS = 54;

    private final Everything plugin;
    private final String menuId;
    private final MenuDefinition definition;
    private final Map<Integer, MenuItemDefinition> actions = new HashMap<>();
    private static final Set<String> SAFE_SETTINGS = Set.of("particles", "join-messages", "teleport-warmup-messages");

    public DynamicMenuGUI(Player player, Everything plugin, String requestedMenuId) {
        super(player, MenuDefinition.clampSize(
                MenuDefinition.load(plugin,
                        requestedMenuId == null || requestedMenuId.trim().isEmpty()
                            ? plugin.getConfig().getString("gui.primary-menu", "primary")
                            : requestedMenuId.trim(),
                        plugin.getConfig()).size()),
            AdventureCompat.deserialize(MenuDefinition.load(plugin,
                    requestedMenuId == null || requestedMenuId.trim().isEmpty()
                        ? plugin.getConfig().getString("gui.primary-menu", "primary")
                        : requestedMenuId.trim(),
                    plugin.getConfig()).titleRaw()));
        this.plugin = plugin;
        String fallbackId = plugin.getConfig().getString("gui.primary-menu", "primary");
        String id = (requestedMenuId == null || requestedMenuId.trim().isEmpty())
            ? fallbackId : requestedMenuId.trim();
        this.menuId = id;
        this.definition = MenuDefinition.load(plugin, id, plugin.getConfig());
    }

    @Override
    public void build() {
        actions.clear();
        if (definition == null) {
            return;
        }

        int size = definition.size();
        for (int slot = 0; slot < size; slot++) {
            inv.setItem(slot, null);
        }

        if (definition.filler() != null
                && definition.filler() != org.bukkit.Material.AIR) {
            org.bukkit.inventory.ItemStack filler = ItemBuilder.builder(definition.filler())
                .name(" ")
                .build();
            for (int slot = 0; slot < size; slot++) {
                if (inv.getItem(slot) == null) {
                    inv.setItem(slot, filler);
                }
            }
        }

        for (MenuItemDefinition item : definition.items()) {
            int slot = item.slot();
            if (slot < 0 || slot >= size) {
                continue;
            }
            if (!isVisible(item)) {
                continue;
            }
            set(slot, createItem(item));
            actions.put(slot, item);
        }
    }

    private boolean isVisible(MenuItemDefinition item) {
        if (item.permission() == null || item.permission().trim().isEmpty()) {
            return true;
        }
        if (player.hasPermission(item.permission())) {
            return true;
        }
        return !"hide".equalsIgnoreCase(definition.unauthorizedMode());
    }

    private ItemStack createItem(MenuItemDefinition item) {
        if (item.permission() != null && !item.permission().trim().isEmpty()
                && !player.hasPermission(item.permission())) {
            return ItemBuilder.builder(org.bukkit.Material.BARRIER)
                .name(item.lockedName())
                .lore(item.lockedLore())
                .build();
        }

        List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
        for (String line : item.rawLore()) {
            lore.add(AdventureCompat.deserialize(replace(line)));
        }
        if (lore.isEmpty()) {
            lore = java.util.List.of();
        }

        boolean toggle = item.action() == MenuAction.TOGGLE_FEATURE
            || item.action() == MenuAction.TOGGLE_SETTING
            || item.action() == MenuAction.CYCLE_HELP_MODE;
        boolean active = toggleState(item);

        org.bukkit.Material icon = item.icon();
        if (icon == null) {
            icon = org.bukkit.Material.BARRIER;
        }
        if (toggle) {
            if (active && item.iconEnabled() != null) {
                icon = item.iconEnabled();
            } else if (!active && item.iconDisabled() != null) {
                icon = item.iconDisabled();
            }
        }

        ItemBuilder itemBuilder = ItemBuilder.builder(icon)
            .name(AdventureCompat.deserialize(replace(item.rawName())))
            .lore(lore.toArray(new net.kyori.adventure.text.Component[0]));
        if (toggle && active) {
            itemBuilder = itemBuilder.glowing();
        }
        return itemBuilder.build();
    }

    private String replace(String raw) {
        if (raw == null) {
            return "";
        }
        String result = raw;

        result = result.replace("%player_name%", player.getName());
        result = result.replace("%player_world%",
            player.getWorld().getName());
        result = result.replace("%version%",
            plugin.getDescription().getVersion());
        result = result.replace("%server_name%",
            Bukkit.getServer().getName());
        result = result.replace("%server_version%",
            Bukkit.getBukkitVersion());

        // Feature-state placeholders.
        java.util.regex.Matcher featureMatcher =
            java.util.regex.Pattern.compile(
                "%feature:([^%]+)%").matcher(result);
        while (featureMatcher.find()) {
            String path = featureMatcher.group(1).trim();
            String value = featureState(path);
            result = result.replace(featureMatcher.group(0), value);
            featureMatcher = java.util.regex.Pattern.compile(
                "%feature:([^%]+)%").matcher(result);
        }

        // Player-setting placeholders.
        java.util.regex.Matcher settingMatcher =
            java.util.regex.Pattern.compile(
                "%setting:([^%]+)%").matcher(result);
        while (settingMatcher.find()) {
            String key = settingMatcher.group(1).trim();
            String value = settingState(key);
            result = result.replace(settingMatcher.group(0), value);
            settingMatcher = java.util.regex.Pattern.compile(
                "%setting:([^%]+)%").matcher(result);
        }
        return result;
    }

    private String featureState(String path) {
        if (path == null || path.trim().isEmpty()) return "Unknown";
        boolean value = plugin.getFeatureManager().getBoolean(path, false);
        return (value ? "<green>Enabled" : "<red>Disabled") + "</gray>";
    }

    private String settingState(String key) {
        if (key == null || key.trim().isEmpty()) return "Unknown";
        if (key.equals("help-mode")) {
            String mode = plugin.getPlayerSettingsManager().getHelpMode(player.getUniqueId());
            return mode == null ? "Unknown" : mode;
        }
        boolean value = plugin.getPlayerSettingsManager().getBoolean(player.getUniqueId(), key, false);
        return value ? "<green>Enabled" : "<red>Disabled";
    }

    private boolean isToggleSetting(String key) {
        return key != null && SAFE_SETTINGS.contains(key);
    }

    private boolean toggleState(MenuItemDefinition item) {
        String target = item.target() == null ? "" : item.target().trim();
        return switch (item.action()) {
            case TOGGLE_FEATURE -> plugin.getFeatureManager().getBoolean(target, true);
            case TOGGLE_SETTING ->
                plugin.getPlayerSettingsManager().getBoolean(player.getUniqueId(), target, true);
            case CYCLE_HELP_MODE ->
                !"disabled".equalsIgnoreCase(
                    plugin.getPlayerSettingsManager().getHelpMode(player.getUniqueId()));
            default -> true;
        };
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        MenuItemDefinition item = actions.get(e.getSlot());
        if (item == null) {
            return;
        }
        if (item.permission() != null && !item.permission().trim().isEmpty()
                && !player.hasPermission(item.permission())) {
            player.sendMessage("You do not have permission: " + item.permission());
            return;
        }
        try {
            execute(item);
        } catch (Exception ex) {
            player.sendMessage("Menu action failed: " + ex.getMessage());
        }
    }

    private void execute(MenuItemDefinition item) {
        switch (item.action()) {
            case OPEN_MENU -> {
                String target = item.target();
                if (target == null || target.trim().isEmpty()) {
                    target = menuId;
                }
                new DynamicMenuGUI(player, plugin, target).open();
            }
            case OPEN_PLAYER_SETTINGS ->
                new DynamicMenuGUI(player, plugin,
                    plugin.getConfig().getString("gui.player-settings-menu", "player-settings")).open();
            case OPEN_ADMIN ->
                new DynamicMenuGUI(player, plugin, "admin").open();
            case OPEN_HELP ->
                new HelpMainGUI(player,
                    new me.crazyg.everything.gui.help.HelpManager(plugin)).open();
            case OPEN_PAGE -> {
                String page = item.target();
                new me.crazyg.everything.gui.help.HelpPageGUI(player,
                    new me.crazyg.everything.gui.help.HelpManager(plugin), page).open();
            }
            case OPEN_CHAT ->
                me.crazyg.everything.utils.HybridChatMenu.send(plugin, player);
            case PERFORM_COMMAND -> {
                String command = item.target();
                if (command != null && !command.trim().isEmpty()) {
                    if (command.startsWith("/")) {
                        command = command.substring(1);
                    }
                    if (!command.isBlank()) {
                        player.performCommand(command);
                    }
                }
            }
            case TOGGLE_FEATURE -> {
                String path = item.target() == null ? "" : item.target().trim();
                if (path.isEmpty()) {
                    player.sendMessage("Feature toggle has no target.");
                    break;
                }
                boolean value = !plugin.getFeatureManager().getBoolean(path, true);
                me.crazyg.everything.utils.FeatureManager.FeatureApplyOutcome outcome = plugin.applyFeatureToggle(path, value);
                player.sendMessage(path + " -> " + (value ? "enabled" : "disabled")
                    + " (" + outcome.name().toLowerCase() + ")");
                if (outcome != me.crazyg.everything.utils.FeatureManager.FeatureApplyOutcome.INVALID_PATH
                        && outcome != me.crazyg.everything.utils.FeatureManager.FeatureApplyOutcome.NOT_APPLIED) {
                    update();
                }
            }
            case CLOSE -> close();
            case TOGGLE_SETTING -> {
                String key = item.target();
                if (key == null || key.trim().isEmpty() || !isToggleSetting(key)) {
                    player.sendMessage("Unknown setting: " + key);
                    return;
                }
                boolean current = plugin.getPlayerSettingsManager().getBoolean(
                    player.getUniqueId(), key, true);
                plugin.getPlayerSettingsManager().setBoolean(
                    player.getUniqueId(), key, !current);
                player.sendMessage(key + " -> "
                    + (!current ? "enabled" : "disabled"));
                update();
            }
            case CYCLE_HELP_MODE -> {
                if (player.hasPermission("everything.settings.help")) {
                    String current = plugin.getPlayerSettingsManager().getHelpMode(player.getUniqueId());
                    String next = switch (current.toLowerCase()) {
                        case "everything" -> "help";
                        case "help" -> "disabled";
                        default -> "everything";
                    };
                    plugin.getPlayerSettingsManager().setHelpMode(player.getUniqueId(), next);
                    player.sendMessage("/help is now set to: " + next);
                    update();
                } else {
                    player.sendMessage("You do not have permission to change help behavior.");
                }
            }
            case NONE -> {}
        }
    }

    private void itemMessage(String message) {
        player.sendMessage(message);
    }
}
