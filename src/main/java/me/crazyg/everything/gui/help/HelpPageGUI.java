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

public class HelpPageGUI extends BaseGUI {

    private final HelpManager manager;
    private final String pageKey;
    private final List<ItemStack> dynamicItems = new ArrayList<>();
    private final Map<Integer, ConfigurationSection> staticItems = new LinkedHashMap<>();
    private Pagination pagination;
    private int currentPage = 1;
    private int dynamicStartSlot;
    private int dynamicPerPage;

    private static final TextColor[] COLORS = {
        TextColor.color(255, 85, 85),
        TextColor.color(85, 255, 85),
        TextColor.color(85, 85, 255),
        TextColor.color(255, 255, 85),
        TextColor.color(255, 85, 255),
        TextColor.color(85, 255, 255),
        TextColor.color(255, 170, 0)
    };

    public HelpPageGUI(Player player, HelpManager manager, String pageKey) {
        super(player, manager.getPageSize(pageKey),
            Component.text(HelpMainGUI.resolvePlaceholders(player,
                manager.getPageTitle(pageKey), manager))
                .color(NamedTextColor.GOLD));
        this.manager = manager;
        this.pageKey = pageKey;
        this.dynamicStartSlot = manager.getPageStartSlot(pageKey);
        this.dynamicPerPage = manager.getPageItemsPerPage(pageKey);

        loadContent();

        setClickCooldown(150);
        setFiller(ItemBuilder.glassPane(TextColor.color(30, 30, 30), " "));
        enableAnimation(15);

        setOnClose(p -> new HelpMainGUI(p, manager).open());
    }

    private void loadContent() {
        dynamicItems.clear();
        staticItems.clear();

        String load = manager.getPageLoad(pageKey);
        if (!load.isEmpty()) {
            loadDynamic(load);
        }

        staticItems.putAll(manager.getPageItems(pageKey));
    }

    private void loadDynamic(String loadType) {
        Everything plugin = manager.getPlugin();
        switch (loadType) {
            case "commands" -> loadCommands(plugin);
            case "warps" -> loadWarps(plugin);
            case "economy" -> loadEconomy(plugin);
        }
    }

    @SuppressWarnings("deprecation")
    private void loadCommands(Everything plugin) {
        for (Map.Entry<String, Map<String, Object>> entry :
                plugin.getDescription().getCommands().entrySet()) {
            String cmdName = entry.getKey();
            Map<String, Object> props = entry.getValue();

            Object permObj = (props != null) ? props.get("permission") : null;
            if (permObj instanceof String perm) {
                if (!player.hasPermission(perm)) continue;
            } else if (permObj instanceof Collection<?> perms) {
                boolean hasAny = false;
                for (Object p : perms) {
                    if (p instanceof String s && player.hasPermission(s)) {
                        hasAny = true;
                        break;
                    }
                }
                if (!hasAny) continue;
            }

            String desc = (props != null)
                ? (String) props.getOrDefault("description", "") : "";
            ItemStack item = ItemBuilder.builder(Material.PAPER)
                .name("&e/" + cmdName)
                .lore(desc.isEmpty() ? List.of() : List.of("&7" + desc))
                .build();
            dynamicItems.add(item);
        }
    }

    @SuppressWarnings("deprecation")
    private void loadWarps(Everything plugin) {
        WarpCommand warpCmd = plugin.getWarpCommand();
        if (warpCmd == null) return;

        for (String warpName : warpCmd.getWarpNames()) {
            dynamicItems.add(ItemBuilder.builder(Material.ENDER_PEARL)
                .name("&d" + warpName)
                .addLore("&7Click to teleport")
                .build());
        }
    }

    @SuppressWarnings("deprecation")
    private void loadEconomy(Everything plugin) {
        EcoStorage storage = plugin.getEcoStorage();
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
            String name = offPlayer.getName() != null
                ? offPlayer.getName() : uuid.toString().substring(0, 8);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setDisplayName(
                ItemBuilder.builder(Material.PAPER).name("&6" + name).build()
                    .getItemMeta().getDisplayName());
            meta.setOwningPlayer(offPlayer);
            meta.setLore(List.of(
                ItemBuilder.builder(Material.PAPER)
                    .name("&eBalance: &f" + String.format("%.2f", bal)).build()
                    .getItemMeta().getDisplayName()));
            head.setItemMeta(meta);
            dynamicItems.add(head);
        }
    }

    @Override
    public void build() {
        // Place static items at their configured slots
        for (Map.Entry<Integer, ConfigurationSection> entry : staticItems.entrySet()) {
            int slot = entry.getKey();
            ConfigurationSection itemConfig = entry.getValue();
            if (itemConfig == null) continue;

            String action = itemConfig.getString("action", "none");
            // Skip navigation items — they get placed below
            if (action.equals("prev-page") || action.equals("next-page")
                || action.equals("back")) {
                continue;
            }

            set(slot, buildConfigItem(itemConfig));
        }

        // Place dynamic items with pagination
        if (!dynamicItems.isEmpty()) {
            pagination = paginate(dynamicItems, dynamicPerPage, currentPage);
            List<?> pageItems = pagination.getPageItems();

            for (int i = 0; i < pageItems.size(); i++) {
                int slot = dynamicStartSlot + i;
                ItemStack item = ((ItemStack) pageItems.get(i)).clone();

                // Warp glow animation
                if ("warps".equals(manager.getPageLoad(pageKey))
                    && animationFrame % 30 < 15) {
                    var meta = item.getItemMeta();
                    if (meta != null) {
                        meta.addItemFlags(
                            org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                        meta.addEnchant(
                            org.bukkit.enchantments.Enchantment.DURABILITY,
                            1, true);
                        item.setItemMeta(meta);
                    }
                }

                set(slot, item);
            }
        }

        // Navigation buttons
        placeNavButtons();

        // Back button
        placeBackButton();
    }

    private void placeNavButtons() {
        if (dynamicItems.isEmpty()) {
            // No dynamic items, no pagination needed
            return;
        }

        int totalPages = pagination.getTotalPages();
        int navPrev = Math.min(36, size - 18);
        int navNext = Math.min(44, size - 10);
        int navPageInfo = (navPrev + navNext) / 2;

        // Previous page
        if (pagination.hasPrevious()) {
            set(navPrev, ItemBuilder.previousPageButton());
        } else {
            set(navPrev, ItemBuilder.builder(Material.GRAY_STAINED_GLASS_PANE)
                .name("No Previous").build());
        }

        // Next page
        if (pagination.hasNext()) {
            set(navNext, ItemBuilder.nextPageButton());
        } else {
            set(navNext, ItemBuilder.builder(Material.GRAY_STAINED_GLASS_PANE)
                .name("No Next").build());
        }

        // Page info
        set(navPageInfo, ItemBuilder.builder(Material.MAP)
            .name("&b&lPage &f" + currentPage + "&b&l/&f" + totalPages)
            .addLore("&7" + dynamicItems.size() + " items total")
            .build());
    }

    private void placeBackButton() {
        ConfigurationSection backItem = staticItems.get(getFirstSlotWithAction("back"));
        if (backItem != null) {
            set(backItem.getInt("slot"), buildConfigItem(backItem));
        } else {
            // Default back button
            int backSlot = size - 9;
            set(backSlot, ItemBuilder.backButton());
        }
    }

    private int getFirstSlotWithAction(String action) {
        for (Map.Entry<Integer, ConfigurationSection> entry : staticItems.entrySet()) {
            ConfigurationSection item = entry.getValue();
            if (item != null && action.equals(item.getString("action", ""))) {
                return entry.getKey();
            }
        }
        return -1;
    }

    private ItemStack buildConfigItem(ConfigurationSection itemConfig) {
        String name = itemConfig.getString("name", "");
        String icon = itemConfig.getString("icon", "PAPER");
        List<String> loreLines = itemConfig.getStringList("lore");

        name = HelpMainGUI.resolvePlaceholders(player,
            manager.replacePlaceholders(name), manager);

        List<String> processedLore = new ArrayList<>();
        for (String line : loreLines) {
            processedLore.add(HelpMainGUI.resolvePlaceholders(player,
                manager.replacePlaceholders(line), manager));
        }

        Material mat = Material.matchMaterial(icon);
        if (mat == null) mat = Material.PAPER;

        ItemBuilder builder = ItemBuilder.builder(mat).name(name);
        if (!processedLore.isEmpty()) {
            builder.lore(processedLore);
        }
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

        // Check static items first
        ConfigurationSection clickedStatic = staticItems.get(slot);
        if (clickedStatic != null) {
            String action = clickedStatic.getString("action", "none");
            if ("prev-page".equals(action) && pagination != null
                && pagination.hasPrevious()) {
                currentPage--;
                playClickSound();
                update();
                return;
            }
            if ("next-page".equals(action) && pagination != null
                && pagination.hasNext()) {
                currentPage++;
                playClickSound();
                update();
                return;
            }
            if (!"none".equals(action)) {
                HelpActionExecutor.execute(action, clickedStatic, player, manager);
                playSuccessSound();
            }
            return;
        }

        // Check for nav buttons at standard positions
        int navPrev = Math.min(36, size - 18);
        int navNext = Math.min(44, size - 10);

        if (slot == navPrev && pagination != null && pagination.hasPrevious()) {
            currentPage--;
            playClickSound();
            update();
            return;
        }

        if (slot == navNext && pagination != null && pagination.hasNext()) {
            currentPage++;
            playClickSound();
            update();
            return;
        }

        // Back button at default position
        int backSlot = size - 9;
        if (slot == backSlot) {
            new HelpMainGUI(player, manager).open();
            playClickSound();
            return;
        }

        // Dynamic item click (warps teleport)
        if (!dynamicItems.isEmpty() && pagination != null) {
            int localIndex = slot - dynamicStartSlot;
            List<?> pageItems = pagination.getPageItems();
            if (localIndex >= 0 && localIndex < pageItems.size()) {
                int globalIndex = (currentPage - 1) * dynamicPerPage + localIndex;
                if ("warps".equals(manager.getPageLoad(pageKey))) {
                    WarpCommand warpCmd = manager.getPlugin().getWarpCommand();
                    if (warpCmd != null) {
                        int i = 0;
                        for (String name : warpCmd.getWarpNames()) {
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
        }
    }

    @Override
    public void open() {
        loadContent();
        currentPage = 1;
        super.open();
    }
}
