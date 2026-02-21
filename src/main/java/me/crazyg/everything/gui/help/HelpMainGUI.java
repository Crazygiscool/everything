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

public class HelpMainGUI extends BaseGUI {

    private final HelpManager manager;
    private final List<String> categoryKeys = new ArrayList<>();
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
        Pagination pagination = paginate(categoryKeys, ITEMS_PER_PAGE, currentPage);
        List<String> pageItems = new ArrayList<>();
        for (Object item : pagination.getPageItems()) {
            if (item instanceof String) {
                pageItems.add((String) item);
            }
        }

        int slot = 10;
        for (int i = 0; i < pageItems.size(); i++) {
            String key = pageItems.get(i);
            ConfigurationSection cat = manager.getCategory(key);
            
            if (cat == null) continue;
            
            String name = cat.getString("name", key);
            Material icon = Material.matchMaterial(cat.getString("icon", "BOOK"));
            
            TextColor color = COLORS[(pagination.getStartIndex() + i) % COLORS.length];
            
            set(slot, ItemBuilder.builder(icon == null ? Material.BOOK : icon)
                    .name(name, color)
                    .addLore("&7Click to view commands")
                    .addLore("&8Category: &f" + key)
                    .glowing(animationFrame % 20 < 10)
                    .build());
            
            slot += 2;
            if ((slot - 10) % 9 == 6) {
                slot += 3;
            }
        }

        set(4, ItemBuilder.builder(Material.PAPER)
                .name("&l&6" + manager.getServerName(), TextColor.color(255, 215, 0))
                .addLore("&7Welcome to the help menu!")
                .addLore("&7Select a category below")
                .addLore("&8Categories: &f" + categoryKeys.size())
                .build());

        if (pagination.hasPrevious()) {
            set(18, ItemBuilder.previousPageButton());
        } else {
            set(18, ItemBuilder.builder(Material.GRAY_STAINED_GLASS_PANE)
                    .name("No Previous")
                    .build());
        }

        set(22, ItemBuilder.builder(Material.MAP)
                .name("&lPage &f" + currentPage + "&l/&f" + pagination.getTotalPages(), TextColor.color(100, 200, 255))
                .addLore("&7Current page")
                .build());

        if (pagination.hasNext()) {
            set(26, ItemBuilder.nextPageButton());
        } else {
            set(26, ItemBuilder.builder(Material.GRAY_STAINED_GLASS_PANE)
                    .name("No Next")
                    .build());
        }

        set(36, ItemBuilder.builder(Material.BOOK)
                .name("&lAll Commands", TextColor.color(150, 255, 150))
                .addLore("&7View all server commands")
                .build());

        set(40, ItemBuilder.builder(Material.PLAYER_HEAD)
                .name("&l" + player.getName(), TextColor.color(200, 200, 200))
                .addLore("&7Your name")
                .addLore("&8World: &f" + player.getWorld().getName())
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

        String name = e.getCurrentItem().getItemMeta().displayName() != null 
                ? e.getCurrentItem().getItemMeta().displayName().toString() 
                : "";

        if (name.contains("Close")) {
            player.closeInventory();
            playSuccessSound();
            return;
        }

        if (name.contains("Previous")) {
            currentPage--;
            playClickSound();
            update();
            return;
        }

        if (name.contains("Next")) {
            currentPage++;
            playClickSound();
            update();
            return;
        }

        if (name.contains("All Commands")) {
            player.sendMessage(Component.text("Opening full command list...").color(NamedTextColor.GREEN));
            playSuccessSound();
            return;
        }

        for (String key : categoryKeys) {
            ConfigurationSection cat = manager.getCategory(key);
            if (cat != null && name.contains(cat.getString("name"))) {
                new HelpCategoryGUI(player, manager, key).open();
                playSuccessSound();
                return;
            }
        }
    }

    @Override
    public void open() {
        loadCategories();
        currentPage = 1;
        super.open();
    }
}
