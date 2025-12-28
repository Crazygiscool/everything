package me.crazyg.everything.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ItemBuilder {

    public static ItemStack item(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(name).color(NamedTextColor.GOLD));

        if (lore.length > 0) {
            meta.lore(
                List.of(lore).stream()
                    .map(s -> Component.text(s).color(NamedTextColor.GRAY))
                    .toList()
            );
        }

        item.setItemMeta(meta);
        return item;
    }
}
