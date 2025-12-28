package me.crazyg.everything.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public abstract class BaseGUI {

    protected final Player player;
    protected final Inventory inv;

    public BaseGUI(Player player, int size, Component title) {
        this.player = player;
        this.inv = Bukkit.createInventory(null, size, title);
    }

    public abstract void build();

    public abstract void onClick(InventoryClickEvent e);

    public void open() {
        build();
        player.openInventory(inv);
    }

    protected void set(int slot, ItemStack item) {
        inv.setItem(slot, item);
    }
}
