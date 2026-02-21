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

public class HelpCategoryGUI extends BaseGUI {

    private final HelpManager manager;
    private final String categoryKey;
    private final List<String> commands = new ArrayList<>();
    private int currentPage = 1;
    private static final int ITEMS_PER_PAGE = 36;

    public HelpCategoryGUI(Player player, HelpManager manager, String categoryKey) {
        super(player, 54, Component.text(manager.getCategory(categoryKey).getString("name", "Help"))
                .color(NamedTextColor.GOLD));
        this.manager = manager;
        this.categoryKey = categoryKey;
        
        loadCommands();
        
        setClickCooldown(150);
        setFiller(ItemBuilder.glassPane(TextColor.color(30, 30, 30), " "));
        
        enableAnimation(15);
        
        setOnClose(p -> new HelpMainGUI(p, manager).open());
    }

    private void loadCommands() {
        commands.clear();
        ConfigurationSection cat = manager.getCategory(categoryKey);
        if (cat != null) {
            List<String> cmds = cat.getStringList("commands");
            if (cmds != null) {
                commands.addAll(cmds);
            }
        }
    }

    @Override
    public void build() {
        Pagination pagination = paginate(commands, ITEMS_PER_PAGE, currentPage);
        List<String> pageItems = new ArrayList<>();
        for (Object item : pagination.getPageItems()) {
            if (item instanceof String) {
                pageItems.add((String) item);
            }
        }

        for (int i = 0; i < pageItems.size(); i++) {
            String cmd = pageItems.get(i);
            int slot = i;
            
            boolean isGlowing = animationFrame % 30 < 15;
            
            set(slot, ItemBuilder.builder(Material.PAPER)
                    .name("&l" + cmd, isGlowing ? TextColor.color(255, 255, 100) : TextColor.color(255, 215, 0))
                    .addLore("&7&m------------------------------")
                    .addLore("&aLeft Click: &fSuggest command")
                    .addLore("&eRight Click: &fExecute command")
                    .addLore("&bShift Click: &fCopy to clipboard")
                    .addLore("&7&m------------------------------")
                    .glowing(isGlowing)
                    .build());
        }

        ConfigurationSection cat = manager.getCategory(categoryKey);
        String categoryName = cat != null ? cat.getString("name", categoryKey) : categoryKey;

        set(4, ItemBuilder.builder(Material.BOOK)
                .name("&l&6" + categoryName, TextColor.color(255, 215, 0))
                .addLore("&7Commands in this category")
                .addLore("&8Total: &f" + commands.size())
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
                .addLore("&7Commands: &f" + pageItems.size() + "&7/&f" + commands.size())
                .build());

        if (pagination.hasNext()) {
            set(26, ItemBuilder.nextPageButton());
        } else {
            set(26, ItemBuilder.builder(Material.GRAY_STAINED_GLASS_PANE)
                    .name("No Next")
                    .build());
        }

        set(36, ItemBuilder.builder(Material.KNOWLEDGE_BOOK)
                .name("&l&bCommand Help", TextColor.color(100, 200, 255))
                .addLore("&7Learn how to use commands")
                .build());

        set(40, ItemBuilder.builder(Material.COMPARATOR)
                .name("&l&eTips & Tricks", TextColor.color(255, 200, 50))
                .addLore("&7• Use &f/&7 before commands")
                .addLore("&7• Some commands need arguments")
                .addLore("&7• Ask staff for help!")
                .build());

        set(44, ItemBuilder.closeButton());

        set(53, ItemBuilder.backButton());
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

        if (name.contains("Back")) {
            new HelpMainGUI(player, manager).open();
            playClickSound();
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

        if (name.contains("Tips")) {
            player.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("Command Tips:").color(NamedTextColor.GOLD));
            player.sendMessage(Component.text("• Use / before all commands").color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("• Tab completion helps!").color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("• Ask staff for help").color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GRAY));
            playSuccessSound();
            return;
        }

        if (name.contains("Help")) {
            player.sendMessage(Component.text("Left-click to suggest a command").color(NamedTextColor.GREEN));
            player.sendMessage(Component.text("Right-click to run the command").color(NamedTextColor.YELLOW));
            playClickSound();
            return;
        }

        for (String cmd : commands) {
            if (name.contains(cmd)) {
                switch (e.getClick()) {
                    case LEFT -> {
                        player.closeInventory();
                        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GRAY));
                        player.sendMessage(Component.text("Suggested: ").color(NamedTextColor.GREEN)
                                .append(Component.text(cmd).color(NamedTextColor.YELLOW)));
                        player.sendMessage(Component.text("Type it in chat to use").color(NamedTextColor.WHITE));
                        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GRAY));
                        playSuccessSound();
                    }
                    case RIGHT -> {
                        player.closeInventory();
                        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GRAY));
                        player.sendMessage(Component.text("Executing: ").color(NamedTextColor.YELLOW)
                                .append(Component.text(cmd).color(NamedTextColor.AQUA)));
                        player.performCommand(cmd.replace("/", ""));
                        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GRAY));
                        playSuccessSound();
                    }
                    case SHIFT_LEFT, SHIFT_RIGHT -> {
                        player.closeInventory();
                        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GRAY));
                        player.sendMessage(Component.text("Copied to clipboard: ").color(NamedTextColor.GREEN)
                                .append(Component.text(cmd).color(NamedTextColor.AQUA)));
                        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GRAY));
                        playSuccessSound();
                    }
                }
                return;
            }
        }
    }

    @Override
    public void open() {
        loadCommands();
        currentPage = 1;
        super.open();
    }
}
