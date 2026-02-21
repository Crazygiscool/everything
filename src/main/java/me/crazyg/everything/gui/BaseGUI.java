package me.crazyg.everything.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public abstract class BaseGUI implements InventoryHolder {

    protected final Player player;
    protected final Inventory inv;

    public BaseGUI(Player player, int size, Component title) {
        this.player = player;
        this.inv = Bukkit.createInventory(this, size, title);
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public abstract void build();

    public abstract void onClick(InventoryClickEvent e);

    public void open() {
        build();
        player.openInventory(inv);
    }

    public void handleClick(InventoryClickEvent e) {
        e.setCancelled(true);

        if (isBlocked(e)) {
            return;
        }

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
}
