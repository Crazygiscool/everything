package me.crazyg.everything.gui.help;

import me.crazyg.everything.gui.BaseGUI;
import me.crazyg.everything.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HelpMainGUI extends BaseGUI {

    private final HelpManager manager;

    public HelpMainGUI(Player player, HelpManager manager) {
        super(player, manager.getMainMenuSize(),
            Component.text(resolvePlaceholders(player,
                manager.getMainMenuTitle(), manager))
                .color(NamedTextColor.GOLD));
        this.manager = manager;

        setClickCooldown(150);
        setFiller(ItemBuilder.glassPane(TextColor.color(30, 30, 30), " "));
        enableAnimation(10);
    }

    @Override
    public void build() {
        Map<Integer, ConfigurationSection> items = manager.getMainMenuItems();

        for (Map.Entry<Integer, ConfigurationSection> entry : items.entrySet()) {
            int slot = entry.getKey();
            ConfigurationSection itemConfig = entry.getValue();
            if (itemConfig == null) continue;
            set(slot, buildItem(itemConfig));
        }
    }

    private org.bukkit.inventory.ItemStack buildItem(ConfigurationSection itemConfig) {
        String name = itemConfig.getString("name", "");
        String icon = itemConfig.getString("icon", "PAPER");
        List<String> loreLines = itemConfig.getStringList("lore");

        name = resolvePlaceholders(player, manager.replacePlaceholders(name), manager);

        List<String> processedLore = new ArrayList<>();
        for (String line : loreLines) {
            processedLore.add(resolvePlaceholders(player,
                manager.replacePlaceholders(line), manager));
        }

        Material mat = Material.matchMaterial(icon);
        if (mat == null) mat = Material.PAPER;

        ItemBuilder builder = ItemBuilder.builder(mat).name(name);
        if (!processedLore.isEmpty()) {
            builder.lore(processedLore);
        }
        builder.glowing(animationFrame % 20 < 10);
        return builder.build();
    }

    @Override
    public void onAnimation() {
        update();
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;
        int slot = e.getRawSlot();

        Map<Integer, ConfigurationSection> items = manager.getMainMenuItems();
        ConfigurationSection itemConfig = items.get(slot);

        if (itemConfig != null) {
            String action = itemConfig.getString("action", "none");
            if (!"none".equals(action)) {
                executeAction(action, itemConfig);
            }
            return;
        }

        // Check all config items for nav actions at this slot
        ConfigurationSection mainMenu = manager.getMainMenuSection();
        if (mainMenu == null) return;
        ConfigurationSection menuItems = mainMenu.getConfigurationSection("items");
        if (menuItems == null) return;

        for (String key : menuItems.getKeys(false)) {
            ConfigurationSection navItem = menuItems.getConfigurationSection(key);
            if (navItem != null && navItem.getInt("slot") == slot) {
                String action = navItem.getString("action", "none");
                if (!"none".equals(action)) {
                    executeAction(action, navItem);
                }
                return;
            }
        }
    }

    private void executeAction(String action, ConfigurationSection itemConfig) {
        HelpActionExecutor.execute(action, itemConfig, player, manager);
    }

    static String resolvePlaceholders(Player player, String text,
                                       HelpManager manager) {
        if (text == null) return "";
        text = text.replace("%player%", player.getName())
                   .replace("%player_name%", player.getName())
                   .replace("%player_displayname%", player.getDisplayName())
                   .replace("%player_world%", player.getWorld().getName())
                   .replace("%server_name%", manager.getServerName())
                   .replace("%server_link%", manager.getServerLink())
                   .replace("%server_author%", manager.getServerAuthor())
                   .replace("%server_description%", manager.getServerDescription());
        return text;
    }

    @Override
    public void open() {
        super.open();
    }
}
