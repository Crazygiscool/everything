package me.crazyg.everything.listeners;

import me.crazyg.everything.gui.BaseGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GUIListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        // Only handle GUIs that use BaseGUI as the holder
        if (!(e.getInventory().getHolder() instanceof BaseGUI gui)) return;

        // Cancel ALL interactions by default
        e.setCancelled(true);

        // Block shift-clicking (moves items between inventories)
        if (e.isShiftClick()) return;

        // Block number-key hotbar swaps
        if (e.getClick().isKeyboardClick()) return;

        // Block dragging items
        if (e.getAction().toString().contains("DRAG")) return;

        // Block clicks in the player's own inventory
        if (e.getClickedInventory() != e.getInventory()) return;

        // Safe to pass the click to the GUI
        gui.onClick(e);
    }
}
