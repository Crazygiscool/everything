package me.crazyg.everything.gui.help;

import me.crazyg.everything.gui.BaseGUI;
import me.crazyg.everything.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class HelpCategoryGUI extends BaseGUI {

    private final HelpManager manager;
    private final String categoryKey;

    public HelpCategoryGUI(Player player, HelpManager manager, String categoryKey) {
        super(
            player,
            54,
            Component.text(manager.getCategory(categoryKey).getString("name"))
                    .color(NamedTextColor.GOLD)
        );
        this.manager = manager;
        this.categoryKey = categoryKey;
    }

    @Override
    public void build() {
        ConfigurationSection cat = manager.getCategory(categoryKey);
        if (cat == null) return;

        List<String> commands = cat.getStringList("commands");

        int slot = 0;
        for (String cmd : commands) {
            set(slot++, ItemBuilder.item(
                    Material.PAPER,
                    cmd,
                    "Left-click: Suggest",
                    "Right-click: Run"
            ));
        }

        // Optional: Back button
        set(53, ItemBuilder.item(
                Material.ARROW,
                "Back",
                "Return to categories"
        ));
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;

        String name = e.getCurrentItem().getItemMeta().displayName().toString();

        // Back button
        if (name.contains("Back")) {
            new HelpMainGUI(player, manager).open();
            return;
        }

        // Command clicked
        String cmd = name;

        switch (e.getClick()) {
            case LEFT -> {
                player.closeInventory();
                player.sendMessage(
                        Component.text("Suggested: ").color(NamedTextColor.GREEN)
                                .append(Component.text(cmd).color(NamedTextColor.YELLOW))
                );
                player.sendMessage(Component.text(cmd));
            }

            case RIGHT -> {
                player.closeInventory();
                player.performCommand(cmd.replace("/", ""));
            }
        }
    }
}
