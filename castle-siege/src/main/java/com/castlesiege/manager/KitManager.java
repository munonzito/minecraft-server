package com.castlesiege.manager;

import com.castlesiege.CastleSiegePlugin;
import com.castlesiege.model.SiegeKit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class KitManager {

    public void applyKit(Player player, SiegeKit kit) {
        player.getInventory().clear();
        var maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(20.0);
        }
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20f);

        switch (kit) {
            case KNIGHT -> applyKnight(player);
            case ARCHER -> applyArcher(player);
            case MAGICIAN -> applyMagician(player);
            case TANK -> applyTank(player);
        }
    }

    private void applyKnight(Player player) {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(Enchantment.SHARPNESS, 1);
        player.getInventory().addItem(sword);
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 2));
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 8));

        player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
    }

    private void applyArcher(Player player) {
        ItemStack bow = new ItemStack(Material.BOW);
        bow.addEnchantment(Enchantment.POWER, 2);
        bow.addEnchantment(Enchantment.INFINITY, 1);
        player.getInventory().addItem(bow);
        player.getInventory().addItem(new ItemStack(Material.ARROW, 1));
        player.getInventory().addItem(new ItemStack(Material.STONE_SWORD));
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 8));

        Color armorColor = Color.GREEN;
        player.getInventory().setHelmet(coloredLeather(Material.LEATHER_HELMET, armorColor));
        player.getInventory().setChestplate(coloredLeather(Material.LEATHER_CHESTPLATE, armorColor));
        player.getInventory().setLeggings(coloredLeather(Material.LEATHER_LEGGINGS, armorColor));
        player.getInventory().setBoots(coloredLeather(Material.LEATHER_BOOTS, armorColor));
    }

    private void applyMagician(Player player) {
        player.getInventory().addItem(new ItemStack(Material.IRON_SWORD));

        for (int i = 0; i < 4; i++) {
            player.getInventory().addItem(makeSplashPotion(PotionEffectType.INSTANT_DAMAGE, 1, 0, "Splash Potion of Harming"));
        }
        for (int i = 0; i < 4; i++) {
            player.getInventory().addItem(makeSplashPotion(PotionEffectType.SLOWNESS, 1, 200, "Splash Potion of Slowness"));
        }
        for (int i = 0; i < 4; i++) {
            player.getInventory().addItem(makeSplashPotion(PotionEffectType.POISON, 1, 200, "Splash Potion of Poison"));
        }

        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 8));

        player.getInventory().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
    }

    private void applyTank(Player player) {
        player.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
        player.getInventory().addItem(new ItemStack(Material.SHIELD));
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 4));
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));

        ItemStack chestplate = new ItemStack(Material.DIAMOND_CHESTPLATE);
        chestplate.addEnchantment(Enchantment.PROTECTION, 2);

        player.getInventory().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));

        var tankHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (tankHealthAttr != null) {
            tankHealthAttr.setBaseValue(30.0);
        }
        player.setHealth(30.0);
    }

    private ItemStack coloredLeather(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeSplashPotion(PotionEffectType type, int amplifier, int duration, String name) {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.addCustomEffect(new PotionEffect(type, duration, amplifier), true);
        meta.displayName(CastleSiegePlugin.deserialize("§d" + name));
        potion.setItemMeta(meta);
        return potion;
    }
}
