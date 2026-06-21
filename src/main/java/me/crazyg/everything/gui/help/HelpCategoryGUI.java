package me.crazyg.everything.gui.help;

import me.crazyg.everything.Everything;
import me.crazyg.everything.commands.WarpCommand;
import me.crazyg.everything.gui.BaseGUI;
import me.crazyg.everything.utils.ItemBuilder;
import me.crazyg.everything.utils.economy.EcoStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class HelpCategoryGUI extends BaseGUI {

    private final HelpManager manager;
    private final String categoryKey;
    private final String type;
    private List<ItemStack> displayItems = new ArrayList<>();
    private final Map<Integer, Runnable> clickActions = new HashMap<>();
    private Pagination pagination;
    private int currentPage = 1;
    private int startSlot = 0;
    private static final int ITEMS_PER_PAGE = 36;

    public HelpCategoryGUI(Player player, HelpManager manager, String categoryKey) {
        super(player, 54, Component.text(manager.getCategory(categoryKey).getString("name", "Help"))
                .color(NamedTextColor.GOLD));
        this.manager = manager;
        this.categoryKey = categoryKey;

        ConfigurationSection cat = manager.getCategory(categoryKey);
        this.type = cat != null ? cat.getString("type", "") : "";

        loadContent();

        setClickCooldown(150);
        setFiller(ItemBuilder.glassPane(TextColor.color(30, 30, 30), " "));

        enableAnimation(15);

        setOnClose(p -> new HelpMainGUI(p, manager).open());
    }

    private void loadContent() {
        displayItems.clear();
        clickActions.clear();

        switch (type) {
            case "commands" -> loadCommands();
            case "warps" -> loadWarps();
            case "economy" -> loadEconomy();
            default -> loadDetails();
        }
    }

    private void loadCommands() {
        Everything plugin = manager.getPlugin();

        for (Map.Entry<String, Map<String, Object>> entry :
                plugin.getDescription().getCommands().entrySet()) {
            String cmdName = entry.getKey();
            Map<String, Object> props = entry.getValue();

            String perm = (props != null) ? (String) props.get("permission") : null;
            if (perm != null && !player.hasPermission(perm)) continue;

            String desc = (props != null) ? (String) props.getOrDefault("description", "") : "";

            ItemBuilder builder = ItemBuilder.builder(Material.PAPER)
                    .name("&e/" + cmdName);
            if (!desc.isEmpty()) {
                builder.addLore("&7" + desc);
            }
            displayItems.add(builder.build());
        }
    }

    @SuppressWarnings("deprecation")
    private void loadWarps() {
        WarpCommand warpCmd = manager.getPlugin().getWarpCommand();
        if (warpCmd == null) return;

        for (String warpName : warpCmd.getWarpNames()) {
            displayItems.add(ItemBuilder.builder(Material.ENDER_PEARL)
                    .name("&d" + warpName)
                    .addLore("&7Click to teleport")
                    .build());
        }
    }

    private void loadEconomy() {
        EcoStorage storage = manager.getPlugin().getEcoStorage();
        if (storage == null) return;

        List<Map.Entry<UUID, Double>> accounts = new ArrayList<>();
        for (String uuidStr : storage.getAllAccountUUIDs()) {
            UUID uuid = UUID.fromString(uuidStr);
            double bal = storage.getBalance(uuid);
            accounts.add(new AbstractMap.SimpleEntry<>(uuid, bal));
        }

        accounts.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        for (Map.Entry<UUID, Double> account : accounts) {
            UUID uuid = account.getKey();
            double bal = account.getValue();
            OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(uuid);
            String name = offPlayer.getName() != null ? offPlayer.getName() : uuid.toString().substring(0, 8);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.displayName(ItemBuilder.builder().name("&6" + name).build()
                    .getItemMeta().displayName());
            meta.setOwningPlayer(offPlayer);
            meta.lore(List.of(
                    ItemBuilder.builder().name("&eBalance: &f" + String.format("%.2f", bal)).build()
                            .getItemMeta().displayName()
            ));
            head.setItemMeta(meta);
            displayItems.add(head);
        }
    }

    private void loadDetails() {
        ConfigurationSection cat = manager.getCategory(categoryKey);
        if (cat == null) return;

        List<String> details = cat.getStringList("details");
        if (details == null) return;

        for (String detail : details) {
            displayItems.add(ItemBuilder.builder(Material.PAPER)
                    .name(detail)
                    .build());
        }
    }

    @Override
    public void build() {
        pagination = paginate(displayItems, ITEMS_PER_PAGE, currentPage);

        List<ItemStack> pageItems = new ArrayList<>();
        for (Object item : pagination.getPageItems()) {
            if (item instanceof ItemStack) {
                pageItems.add((ItemStack) item);
            }
        }

        int itemCount = pageItems.size();
        startSlot = itemCount >= 36 ? 0 : (36 - itemCount) / 2;

        for (int i = 0; i < itemCount; i++) {
            boolean isGlowing = animationFrame % 30 < 15;
            ItemStack item = pageItems.get(i).clone();
            if (isGlowing && type.equals("warps")) {
                var meta = item.getItemMeta();
                if (meta != null) {
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
                    item.setItemMeta(meta);
                }
            }
            set(startSlot + i, item);
        }

        ConfigurationSection cat = manager.getCategory(categoryKey);
        String categoryName = cat != null ? cat.getString("name", categoryKey) : categoryKey;
        String categoryDesc = cat != null ? cat.getString("description", "") : "";

        ItemBuilder catInfoItem = ItemBuilder.builder(Material.BOOK)
                .name("&l&6" + categoryName);
        if (!categoryDesc.isEmpty()) {
            catInfoItem.addLore(categoryDesc);
        }
        catInfoItem.addLore("&8" + displayItems.size() + (type.equals("economy") ? " accounts" : type.equals("warps") ? " warps" : " topics"));
        set(4, catInfoItem.build());

        if (pagination.hasPrevious()) {
            set(36, ItemBuilder.previousPageButton());
        } else {
            set(36, ItemBuilder.builder(Material.GRAY_STAINED_GLASS_PANE)
                    .name("No Previous")
                    .build());
        }

        if (pagination.hasNext()) {
            set(44, ItemBuilder.nextPageButton());
        } else {
            set(44, ItemBuilder.builder(Material.GRAY_STAINED_GLASS_PANE)
                    .name("No Next")
                    .build());
        }

        set(45, ItemBuilder.backButton());

        set(49, ItemBuilder.builder(Material.MAP)
                .name("&b&lPage &f" + currentPage + "&b&l/&f" + pagination.getTotalPages())
                .addLore("&7" + pageItems.size() + "&7/&f" + displayItems.size())
                .build());

        set(53, ItemBuilder.closeButton());
    }

    @Override
    public void onAnimation() {
        update();
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;

        int slot = e.getRawSlot();

        if (slot == 53) {
            player.closeInventory();
            playSuccessSound();
            return;
        }

        if (slot == 45) {
            new HelpMainGUI(player, manager).open();
            playClickSound();
            return;
        }

        if (slot == 36 && pagination.hasPrevious()) {
            currentPage--;
            playClickSound();
            update();
            return;
        }

        if (slot == 44 && pagination.hasNext()) {
            currentPage++;
            playClickSound();
            update();
            return;
        }

        if (type.equals("warps")) {
            int localIndex = slot - startSlot;
            List<?> pageItems = pagination.getPageItems();
            if (localIndex >= 0 && localIndex < pageItems.size()) {
                int globalIndex = (currentPage - 1) * ITEMS_PER_PAGE + localIndex;
                int i = 0;
                for (String name : manager.getPlugin().getWarpCommand().getWarpNames()) {
                    if (i == globalIndex) {
                        player.closeInventory();
                        player.performCommand("warp " + name);
                        playSuccessSound();
                        return;
                    }
                    i++;
                }
            }
        }
    }

    @Override
    public void open() {
        loadContent();
        currentPage = 1;
        super.open();
    }
}
