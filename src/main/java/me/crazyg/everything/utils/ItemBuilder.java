package me.crazyg.everything.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ItemBuilder {

    private Material material;
    private int amount = 1;
    private short durability = 0;
    private Component displayName;
    private List<Component> lore = new ArrayList<>();
    private boolean glowing = false;
    private boolean unbreakable = false;
    private int customModelData = -1;
    private TextColor armorColor = null;
    private PotionEffectType potionEffect = null;
    private int potionDuration = 0;
    private int potionAmplifier = 0;

    public static ItemBuilder builder() {
        return new ItemBuilder();
    }

    public static ItemBuilder builder(Material mat) {
        return new ItemBuilder().material(mat);
    }

    public static ItemStack item(Material mat, String name, String... lore) {
        return builder(mat).name(name).lore(Arrays.asList(lore)).build();
    }

    public ItemBuilder material(Material mat) {
        this.material = mat;
        return this;
    }

    public ItemBuilder amount(int amount) {
        this.amount = Math.max(1, Math.min(64, amount));
        return this;
    }

    public ItemBuilder durability(short durability) {
        this.durability = durability;
        return this;
    }

    public ItemBuilder name(String name) {
        this.displayName = parseComponent(name);
        return this;
    }

    public ItemBuilder name(Component component) {
        this.displayName = component;
        return this;
    }

    public ItemBuilder name(String name, TextColor color) {
        this.displayName = Component.text(name).color(color);
        return this;
    }

    public ItemBuilder lore(String... lines) {
        this.lore = Arrays.stream(lines)
                .map(this::parseComponent)
                .toList();
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        this.lore = lines.stream()
                .map(this::parseComponent)
                .toList();
        return this;
    }

    public ItemBuilder lore(Component... lines) {
        this.lore = Arrays.asList(lines);
        return this;
    }

    public ItemBuilder addLore(String line) {
        this.lore.add(parseComponent(line));
        return this;
    }

    public ItemBuilder glowing(boolean glowing) {
        this.glowing = glowing;
        return this;
    }

    public ItemBuilder glowing() {
        return glowing(true);
    }

    public ItemBuilder unbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
        return this;
    }

    public ItemBuilder unbreakable() {
        return unbreakable(true);
    }

    public ItemBuilder customModelData(int data) {
        this.customModelData = data;
        return this;
    }

    public ItemBuilder armorColor(TextColor color) {
        this.armorColor = color;
        return this;
    }

    public ItemBuilder armorColor(int r, int g, int b) {
        this.armorColor = TextColor.color(r, g, b);
        return this;
    }

    public ItemBuilder potion(PotionEffectType type, int duration, int amplifier) {
        this.potionEffect = type;
        this.potionDuration = duration;
        this.potionAmplifier = amplifier;
        return this;
    }

    public ItemBuilder transform(Consumer<ItemMeta> transformer) {
        return this;
    }

    public ItemStack build() {
        ItemStack item = new ItemStack(material, amount, durability);
        ItemMeta meta = item.getItemMeta();

        if (displayName != null) {
            meta.displayName(displayName);
        }

        if (!lore.isEmpty()) {
            meta.lore(lore);
        }

        if (glowing) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            org.bukkit.enchantments.Enchantment glow = org.bukkit.enchantments.Enchantment.DURABILITY;
            meta.addEnchant(glow, 1, true);
        }

        if (unbreakable) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }

        if (customModelData >= 0) {
            meta.setCustomModelData(customModelData);
        }

        if (armorColor != null && meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(org.bukkit.Color.fromRGB(
                    armorColor.red(),
                    armorColor.green(),
                    armorColor.blue()
            ));
        }

        if (potionEffect != null && meta instanceof PotionMeta potionMeta) {
            potionMeta.addCustomEffect(
                    new PotionEffect(potionEffect, potionDuration, potionAmplifier),
                    true
            );
        }

        item.setItemMeta(meta);
        return item;
    }

    private Component parseComponent(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacyAmpersand()
                .deserialize(text);
    }

    public static ItemStack skull(String owner, String name, String... lore) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) skull.getItemMeta();
        if (owner != null && !owner.isEmpty()) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
        }
        skullMeta.displayName(Component.text(name).color(NamedTextColor.GOLD));
        if (lore.length > 0) {
            skullMeta.lore(
                    Arrays.stream(lore)
                            .map(s -> Component.text(s).color(NamedTextColor.GRAY))
                            .toList()
            );
        }
        skull.setItemMeta(skullMeta);
        return skull;
    }

    public static ItemStack glassPane(TextColor color, String name) {
        Material pane = switch (color.toString()) {
            case "red" -> Material.RED_STAINED_GLASS_PANE;
            case "blue" -> Material.BLUE_STAINED_GLASS_PANE;
            case "green" -> Material.GREEN_STAINED_GLASS_PANE;
            case "yellow" -> Material.YELLOW_STAINED_GLASS_PANE;
            case "purple" -> Material.PURPLE_STAINED_GLASS_PANE;
            case "pink" -> Material.PINK_STAINED_GLASS_PANE;
            case "cyan" -> Material.CYAN_STAINED_GLASS_PANE;
            case "white" -> Material.WHITE_STAINED_GLASS_PANE;
            default -> Material.BLACK_STAINED_GLASS_PANE;
        };
        return builder(pane).name(name).build();
    }

    public static ItemStack decoration(Material mat, String name) {
        return builder(mat).name(name).build();
    }

    public static ItemStack border(Material mat, TextColor color) {
        return glassPane(color, " ");
    }

    public static ItemStack closeButton() {
        return builder(Material.BARRIER)
                .name("Close", NamedTextColor.RED)
                .addLore("Click to close")
                .build();
    }

    public static ItemStack backButton() {
        return builder(Material.ARROW)
                .name("Back", NamedTextColor.GREEN)
                .addLore("Return to previous menu")
                .build();
    }

    public static ItemStack nextPageButton() {
        return builder(Material.ARROW)
                .name("Next Page", NamedTextColor.GREEN)
                .addLore("Go to next page")
                .build();
    }

    public static ItemStack previousPageButton() {
        return builder(Material.ARROW)
                .name("Previous Page", NamedTextColor.GREEN)
                .addLore("Go to previous page")
                .build();
    }

    public static ItemStack info(String title, String... lines) {
        return builder(Material.PAPER)
                .name(title, NamedTextColor.AQUA)
                .lore(lines)
                .build();
    }
}
