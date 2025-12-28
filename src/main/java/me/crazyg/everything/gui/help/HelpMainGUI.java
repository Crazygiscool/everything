package me.crazyg.everything.gui.help;

import me.crazyg.everything.gui.BaseGUI;
import me.crazyg.everything.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class HelpMainGUI extends BaseGUI {

    private final HelpManager manager;

    public HelpMainGUI(Player player, HelpManager manager) {
        super(player, 27, Component.text(manager.getServerName() + " Help").color(NamedTextColor.GOLD));
        this.manager = manager;
    }

    @Override
    public void build() {
        ConfigurationSection categories = manager.getCategories();
        if (categories == null) return;

        int slot = 10;

        for (String key : categories.getKeys(false)) {
            ConfigurationSection cat = categories.getConfigurationSection(key);

            String name = cat.getString("name", key);
            Material icon = Material.matchMaterial(cat.getString("icon", "BOOK"));

            set(slot++, ItemBuilder.item(
                    icon == null ? Material.BOOK : icon,
                    name,
                    "Click to view commands"
            ));
        }
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;

        String clickedName = e.getCurrentItem().getItemMeta().displayName().toString();

        for (String key : manager.getCategories().getKeys(false)) {
            ConfigurationSection cat = manager.getCategory(key);
            if (clickedName.contains(cat.getString("name"))) {
                new HelpCategoryGUI(player, manager, key).open();
                return;
            }
        }
    }
}
