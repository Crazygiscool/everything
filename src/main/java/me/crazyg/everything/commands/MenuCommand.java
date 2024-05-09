package me.crazyg.everything.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MenuCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player p){
            ItemStack flower = new ItemStack(Material.FLOWERING_AZALEA, 2);
            p.getInventory().setItem(1, flower);

            ItemStack food = new ItemStack(Material.BEEF, 16);
            ItemMeta foodMeta = food.getItemMeta();
            foodMeta.setDisplayName(ChatColor.RED+"YUMMY!!");

            List<String> foodLore = new ArrayList<>();
            foodLore.add(ChatColor.DARK_AQUA+"BEST FOOD ON EARTH");
            foodLore.add(ChatColor.DARK_AQUA+"EAT IT AND YOU WONT REGRET IT");
            foodLore.add(ChatColor.DARK_AQUA+"COOKED BY CRAZYG");
            foodMeta.addEnchant(Enchantment.DURABILITY, 10000, true);
            foodMeta.addEnchant(Enchantment.KNOCKBACK, 10000, true);
            foodMeta.addEnchant(Enchantment.DAMAGE_ALL, 10000, true);

            //add the meta
            food.setItemMeta(foodMeta);
            p.getInventory().addItem(food);
        }
            return true;
    }
}