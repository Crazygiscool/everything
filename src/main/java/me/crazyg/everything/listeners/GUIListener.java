package me.crazyg.everything.listeners;

import me.crazyg.everything.gui.BaseGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GUIListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        // Only handle GUIs that use BaseGUI as the holder
        if (!(e.getInventory().getHolder() instanceof BaseGUI gui)) return;

        // Cancel EVERYTHING by default
        e.setCancelled(true);

        // If they clicked outside the GUI, ignore
        if (e.getClickedInventory() == null) return;

        // If they clicked their own inventory, block it
        if (e.getClickedInventory() == p.getInventory()) return;

        // Block shift-click
        if (e.isShiftClick()) return;

        // Block number-key swaps
        if (e.getClick().isKeyboardClick()) return;

        // Block dragging items
        if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) return;
        if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR) return;
        if (e.getAction().toString().contains("DRAG")) return;

        // Block swapping with cursor
        if (e.getAction() == InventoryAction.SWAP_WITH_CURSOR) return;

        // Block drop actions
        if (e.getClick() == ClickType.DROP || e.getClick() == ClickType.CONTROL_DROP) return;

        // Block middle-click (creative mode)
        if (e.getClick() == ClickType.MIDDLE) return;

        // Block hotbar swaps
        if (e.getClick() == ClickType.NUMBER_KEY) return;

        // Now it's a safe GUI click
        gui.onClick(e);
    }
}
