package com.soulstealer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

public class SwordManager {

    public static ItemStack createSword(int souls) {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&5&lSoulstealer Sword"));

        List<String> lore = Arrays.asList(
            ChatColor.GRAY + "Souls: " + souls + "/8",
            ChatColor.DARK_PURPLE + "Sharpness: " + getSharpnessName(souls)
        );
        meta.setLore(lore);

        if (souls > 0) {
            Enchantment sharpness = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("sharpness"));
            if (sharpness != null) {
                meta.addEnchant(sharpness, souls, true);
            }
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(getPlugin().swordKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(getPlugin().soulsKey, PersistentDataType.INTEGER, souls);

        sword.setItemMeta(meta);
        return sword;
    }

    public static void registerRecipe(SoulstealerPlugin plugin) {
        ShapedRecipe recipe = new ShapedRecipe(plugin.recipeKey, createSword(0));
        // 8 Wither Skeleton Skulls surrounding a Netherite Sword
        recipe.shape("WWW", "WSW", "WWW");
        recipe.setIngredient('W', Material.WITHER_SKELETON_SKULL);
        recipe.setIngredient('S', Material.NETHERITE_SWORD);
        Bukkit.addRecipe(recipe);
    }

    public static boolean isSoulstealerSword(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(getPlugin().swordKey, PersistentDataType.BYTE);
    }

    public static int getSoulCount(ItemStack item) {
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.getOrDefault(getPlugin().soulsKey, PersistentDataType.INTEGER, 0);
    }

    public static void setSoulCount(ItemStack item, int souls) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(getPlugin().soulsKey, PersistentDataType.INTEGER, souls);

        List<String> lore = Arrays.asList(
            ChatColor.GRAY + "Souls: " + souls + "/8",
            ChatColor.DARK_PURPLE + "Sharpness: " + getSharpnessName(souls)
        );
        meta.setLore(lore);

        Enchantment sharpness = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("sharpness"));
        if (sharpness != null) {
            meta.removeEnchant(sharpness);
            if (souls > 0) {
                meta.addEnchant(sharpness, souls, true);
            }
        }

        item.setItemMeta(meta);
    }

    private static String getSharpnessName(int level) {
        if (level == 0) return "None";
        String[] romans = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII"};
        return level <= 8 ? romans[level - 1] : String.valueOf(level);
    }

    private static SoulstealerPlugin getPlugin() {
        return (SoulstealerPlugin) Bukkit.getPluginManager().getPlugin("Soulstealer");
    }
}
