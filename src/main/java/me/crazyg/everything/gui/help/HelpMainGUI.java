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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelpMainGUI extends BaseGUI {

    private final HelpManager manager;
    private final List<String> categoryKeys = new ArrayList<>();
    private final Map<Integer, String> slotToCategory = new HashMap<>();
    private Pagination pagination;
    private int currentPage = 1;
    private static final int ITEMS_PER_PAGE = 7;
    private static final TextColor[] COLORS = {
            TextColor.color(255, 85, 85),
            TextColor.color(85, 255, 85),
            TextColor.color(85, 85, 255),
            TextColor.color(255, 255, 85),
            TextColor.color(255, 85, 255),
            TextColor.color(85, 255, 255),
            TextColor.color(255, 170, 0)
    };

    public HelpMainGUI(Player player, HelpManager manager) {
        super(player, 45, Component.text(manager.getServerName() + " Help Menu").color(NamedTextColor.GOLD));
        this.manager = manager;
        loadCategories();

        setClickCooldown(150);
        setFiller(ItemBuilder.glassPane(TextColor.color(30, 30, 30), " "));

        enableAnimation(10);
    }

    private void loadCategories() {
        categoryKeys.clear();
        ConfigurationSection categories = manager.getCategories();
        if (categories != null) {
            categoryKeys.addAll(categories.getKeys(false));
        }
    }

    @Override
    public void build() {
        slotToCategory.clear();

        pagination = paginate(categoryKeys, ITEMS_PER_PAGE, currentPage);
        List<String> pageItems = new ArrayList<>();
        for (Object item : pagination.getPageItems()) {
            if (item instanceof String) {
                pageItems.add((String) item);
            }
        }

        int slot = 19;
        for (int i = 0; i < pageItems.size(); i++) {
            String key = pageItems.get(i);
            ConfigurationSection cat = manager.getCategory(key);

            if (cat == null) continue;

            String name = cat.getString("name", key);
            String desc = cat.getString("description", "");
            Material icon = Material.matchMaterial(cat.getString("icon", "BOOK"));

            TextColor color = COLORS[(pagination.getStartIndex() + i) % COLORS.length];

            set(slot, ItemBuilder.builder(icon == null ? Material.BOOK : icon)
                    .name(name, color)
                    .lore(desc)
                    .glowing(animationFrame % 20 < 10)
                    .build());

            slotToCategory.put(slot, key);

            slot += 2;
            if ((slot - 19) % 9 == 6) {
                slot += 3;
            }
        }

        String serverDesc = manager.getServerDescription();
        ItemBuilder serverInfoItem = ItemBuilder.builder(Material.NETHER_STAR)
                .name("&l&6" + manager.getServerName());
        if (!serverDesc.isEmpty()) {
            serverInfoItem.addLore(serverDesc);
        }
        serverInfoItem
                .addLore("&8" + manager.getServerLink())
                .addLore("&7Created by " + manager.getServerAuthor());
        set(4, serverInfoItem.build());

        if (pagination.hasPrevious()) {
            set(27, ItemBuilder.previousPageButton());
        } else {
            set(27, ItemBuilder.builder(Material.GRAY_STAINED_GLASS_PANE)
                    .name("No Previous")
                    .build());
        }

        if (pagination.hasNext()) {
            set(35, ItemBuilder.nextPageButton());
        } else {
            set(35, ItemBuilder.builder(Material.GRAY_STAINED_GLASS_PANE)
                    .name("No Next")
                    .build());
        }

        set(36, ItemBuilder.builder(Material.PLAYER_HEAD)
                .name("&7&l" + player.getName())
                .addLore("&7Your name")
                .addLore("&8World: &f" + player.getWorld().getName())
                .build());

        set(40, ItemBuilder.builder(Material.MAP)
                .name("&b&lPage &f" + currentPage + "&b&l/&f" + pagination.getTotalPages())
                .addLore("&7Current page")
                .build());

        set(44, ItemBuilder.closeButton());
    }

    @Override
    public void onAnimation() {
        update();
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;

        int slot = e.getRawSlot();

        if (slot == 44) {
            player.closeInventory();
            playSuccessSound();
            return;
        }

        if (slot == 27 && pagination.hasPrevious()) {
            currentPage--;
            playClickSound();
            update();
            return;
        }

        if (slot == 35 && pagination.hasNext()) {
            currentPage++;
            playClickSound();
            update();
            return;
        }

        String categoryKey = slotToCategory.get(slot);
        if (categoryKey != null) {
            new HelpCategoryGUI(player, manager, categoryKey).open();
            playSuccessSound();
        }
    }

    @Override
    public void open() {
        loadCategories();
        currentPage = 1;
        super.open();
    }
}
