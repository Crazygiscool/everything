package me.crazyg.everything.gui;

import me.crazyg.everything.Everything;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class BaseGUI implements InventoryHolder {

    protected final Player player;
    protected final Inventory inv;
    protected final int size;

    private long lastClickTime = 0;
    protected long clickCooldown = 200;
    protected boolean clickCooldownEnabled = true;

    protected boolean animationEnabled = false;
    protected int animationInterval = 20;
    protected BukkitTask animationTask = null;
    protected int animationFrame = 0;

    protected boolean closeHandlerEnabled = false;
    protected Consumer<Player> onClose = null;

    protected ItemStack fillerItem = null;
    protected boolean fillerEnabled = true;

    private static final Map<Player, BaseGUI> openGUIs = new HashMap<>();

    public BaseGUI(Player player, int size, Component title) {
        this.player = player;
        this.size = size;
        this.inv = Bukkit.createInventory(this, size, title);
        this.fillerItem = createDefaultFiller();
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public abstract void build();

    public abstract void onClick(InventoryClickEvent e);

    public void open() {
        build();
        if (fillerEnabled && fillerItem != null) {
            fillEmptySlots();
        }
        player.openInventory(inv);
        openGUIs.put(player, this);

        if (animationEnabled) {
            startAnimation();
        }

        if (closeHandlerEnabled) {
            registerCloseHandler();
        }
    }

    public void close() {
        player.closeInventory();
        stopAnimation();
    }

    public void update() {
        build();
        if (fillerEnabled && fillerItem != null) {
            fillEmptySlots();
        }
        if (player.getOpenInventory().getTopInventory() == inv) {
            player.updateInventory();
        }
    }

    public void handleClick(InventoryClickEvent e) {
        e.setCancelled(true);

        if (isBlocked(e)) {
            return;
        }

        if (clickCooldownEnabled) {
            long now = System.currentTimeMillis();
            if (now - lastClickTime < clickCooldown) {
                playDenySound();
                return;
            }
            lastClickTime = now;
        }

        playClickSound();
        onClick(e);
    }

    protected boolean isBlocked(InventoryClickEvent e) {
        return e.getClickedInventory() == null
                || e.getClickedInventory() == player.getInventory()
                || e.isShiftClick()
                || e.getClick().isKeyboardClick()
                || e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || e.getAction() == InventoryAction.COLLECT_TO_CURSOR
                || e.getAction().toString().contains("DRAG")
                || e.getAction() == InventoryAction.SWAP_WITH_CURSOR
                || e.getClick() == ClickType.DROP
                || e.getClick() == ClickType.CONTROL_DROP
                || e.getClick() == ClickType.MIDDLE
                || e.getClick() == ClickType.NUMBER_KEY;
    }

    protected void set(int slot, ItemStack item) {
        inv.setItem(slot, item);
    }

    protected void set(int slot, ItemStack item, boolean applyFiller) {
        inv.setItem(slot, item);
        if (applyFiller && fillerEnabled && fillerItem != null) {
            updateNeighbors(slot);
        }
    }

    private void updateNeighbors(int slot) {
        int row = slot / 9;
        int col = slot % 9;

        int[] neighbors = {
            row * 9 + col,
            row * 9 + (col + 1) % 9,
            row * 9 + (col + 8) % 9,
            (row + 1) * 9 + col,
            (row - 1) * 9 + col
        };

        for (int i : neighbors) {
            if (i >= 0 && i < size && inv.getItem(i) == null) {
                inv.setItem(i, fillerItem);
            }
        }
    }

    protected void fillEmptySlots() {
        for (int i = 0; i < size; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, fillerItem);
            }
        }
    }

    protected ItemStack createDefaultFiller() {
        return new ItemStack(org.bukkit.Material.BLACK_STAINED_GLASS_PANE);
    }

    public void setFiller(ItemStack item) {
        this.fillerItem = item;
    }

    public void setFillerEnabled(boolean enabled) {
        this.fillerEnabled = enabled;
    }

    protected void setClickCooldown(long millis) {
        this.clickCooldown = millis;
    }

    protected void setClickCooldownEnabled(boolean enabled) {
        this.clickCooldownEnabled = enabled;
    }

    protected void enableAnimation(int tickInterval) {
        this.animationEnabled = true;
        this.animationInterval = tickInterval;
    }

    protected void disableAnimation() {
        this.animationEnabled = false;
        stopAnimation();
    }

    protected void startAnimation() {
        if (animationTask != null) return;
        animationTask = Bukkit.getScheduler().runTaskTimer(Everything.getInstance(), () -> {
            animationFrame++;
            onAnimation();
        }, animationInterval, animationInterval);
    }

    protected void stopAnimation() {
        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
        }
    }

    protected void onAnimation() {
    }

    public void setOnClose(Consumer<Player> handler) {
        this.onClose = handler;
        this.closeHandlerEnabled = true;
    }

    private void registerCloseHandler() {
    }

    public static BaseGUI getOpenGUI(Player player) {
        return openGUIs.get(player);
    }

    public static void handleInventoryClose(Player player) {
        BaseGUI gui = openGUIs.remove(player);
        if (gui != null) {
            gui.stopAnimation();
            if (gui.onClose != null) {
                gui.onClose.accept(player);
            }
        }
    }

    protected String replacePlaceholders(String text) {
        if (text == null) return "";
        
        text = text.replace("%player%", player.getName())
                   .replace("%player_name%", player.getName())
                   .replace("%player_displayname%", player.getDisplayName())
                   .replace("%player_world%", player.getWorld().getName())
                   .replace("%player_health%", String.format("%.1f", player.getHealth()))
                   .replace("%player_food%", String.valueOf(player.getFoodLevel()))
                   .replace("%player_xp%", String.valueOf(player.getExp()))
                   .replace("%player_level%", String.valueOf(player.getLevel()));

        try {
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
            }
        } catch (Exception ignored) {
        }

        return text;
    }

    protected Component replacePlaceholders(Component component) {
        String serialized = LegacyComponentSerializer.legacyAmpersand().serialize(component);
        serialized = replacePlaceholders(serialized);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(serialized);
    }

    protected void playClickSound() {
        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
        } catch (Exception ignored) {
        }
    }

    protected void playDenySound() {
        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 0.5f);
        } catch (Exception ignored) {
        }
    }

    protected void playSuccessSound() {
        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        } catch (Exception ignored) {
        }
    }

    public static class Pagination {
        private final List<?> items;
        private final int itemsPerPage;
        private final int currentPage;

        public Pagination(List<?> items, int itemsPerPage, int currentPage) {
            this.items = items;
            this.itemsPerPage = itemsPerPage;
            this.currentPage = Math.max(1, currentPage);
        }

        public int getTotalPages() {
            return (int) Math.ceil((double) items.size() / itemsPerPage);
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public boolean hasNext() {
            return currentPage < getTotalPages();
        }

        public boolean hasPrevious() {
            return currentPage > 1;
        }

        public List<?> getPageItems() {
            int start = (currentPage - 1) * itemsPerPage;
            int end = Math.min(start + itemsPerPage, items.size());
            if (start >= items.size()) return List.of();
            return items.subList(start, end);
        }

        public int getStartIndex() {
            return (currentPage - 1) * itemsPerPage;
        }
    }

    protected Pagination paginate(List<?> items, int itemsPerPage) {
        return new Pagination(items, itemsPerPage, 1);
    }

    protected Pagination paginate(List<?> items, int itemsPerPage, int page) {
        return new Pagination(items, itemsPerPage, page);
    }
}
