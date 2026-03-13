package com.castlesiege.manager;

import com.castlesiege.CastleSiegePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;



public class ShopManager {

    private final CastleSiegePlugin plugin;
    private CastleManager castleManager;
    private final Map<UUID, Integer> playerCoins = new HashMap<>();

    public static final String SHOP_TITLE_PLAIN = "Siege Shop";

    public ShopManager(CastleSiegePlugin plugin) {
        this.plugin = plugin;
    }

    public void setCastleManager(CastleManager castleManager) {
        this.castleManager = castleManager;
    }

    public void addCoins(UUID playerId, int amount) {
        playerCoins.merge(playerId, amount, Integer::sum);
    }

    public int getCoins(UUID playerId) {
        return playerCoins.getOrDefault(playerId, 0);
    }

    public boolean spendCoins(UUID playerId, int amount) {
        int current = getCoins(playerId);
        if (current < amount) return false;
        playerCoins.put(playerId, current - amount);
        return true;
    }

    public void openShop(Player player) {
        Inventory shop = Bukkit.createInventory(null, 27,
                Component.text(SHOP_TITLE_PLAIN, NamedTextColor.GOLD, TextDecoration.BOLD));
        int coins = getCoins(player.getUniqueId());

        shop.setItem(0, createShopItem(Material.COOKED_BEEF, 8, "§6Cooked Beef x8", getCost("cooked-beef-8"), coins));
        shop.setItem(1, createShopItem(Material.GOLDEN_APPLE, 2, "§6Golden Apple x2", getCost("golden-apple-2"), coins));
        shop.setItem(2, createShopItem(Material.SPLASH_POTION, 1, "§dSplash Healing Potion", getCost("splash-healing"), coins));
        shop.setItem(3, createShopItem(Material.ARROW, 16, "§7Arrow x16", getCost("arrow-16"), coins));
        shop.setItem(5, createShopItem(Material.DIAMOND_SWORD, 1, "§bDiamond Sword", getCost("diamond-sword"), coins));
        shop.setItem(6, createShopItem(Material.IRON_CHESTPLATE, 1, "§fIron Armor Set", getCost("iron-armor"), coins));
        shop.setItem(7, createShopItem(Material.DIAMOND_CHESTPLATE, 1, "§bDiamond Armor Set", getCost("diamond-armor"), coins));
        shop.setItem(8, createShopItem(Material.SHIELD, 1, "§eShield", getCost("shield"), coins));

        // Potions & Bow
        shop.setItem(9, createShopItem(Material.SPLASH_POTION, 2, "§cSplash Harming x2", getCost("splash-harming"), coins));
        shop.setItem(10, createShopItem(Material.SPLASH_POTION, 2, "§2Splash Poison x2", getCost("splash-poison"), coins));
        shop.setItem(11, createShopItem(Material.BOW, 1, "§ePower II Bow", getCost("power-bow"), coins));

        // Allies
        shop.setItem(18, createShopItem(Material.IRON_BLOCK, 1, "§f§lIron Golem Defender", getCost("iron-golem"), coins));

        // Coins display
        ItemStack coinDisplay = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta coinMeta = coinDisplay.getItemMeta();
        coinMeta.displayName(CastleSiegePlugin.deserialize("§e§lYour Coins: §b" + coins));
        coinDisplay.setItemMeta(coinMeta);
        shop.setItem(13, coinDisplay);

        player.openInventory(shop);
    }

    public boolean handleShopClick(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        switch (slot) {
            case 0 -> { return tryPurchase(player, "cooked-beef-8", new ItemStack(Material.COOKED_BEEF, 8), "Cooked Beef x8"); }
            case 1 -> { return tryPurchase(player, "golden-apple-2", new ItemStack(Material.GOLDEN_APPLE, 2), "Golden Apple x2"); }
            case 2 -> { return tryPurchaseSplashHealing(player); }
            case 3 -> { return tryPurchase(player, "arrow-16", new ItemStack(Material.ARROW, 16), "Arrow x16"); }
            case 5 -> { return tryPurchaseDiamondSword(player); }
            case 6 -> { return tryPurchaseIronArmor(player); }
            case 7 -> { return tryPurchaseDiamondArmor(player); }
            case 8 -> { return tryPurchase(player, "shield", new ItemStack(Material.SHIELD), "Shield"); }
            case 9 -> { return tryPurchaseSplashHarming(player); }
            case 10 -> { return tryPurchaseSplashPoison(player); }
            case 11 -> { return tryPurchasePowerBow(player); }
            case 18 -> { return tryPurchaseIronGolem(player); }
            default -> { return false; }
        }
    }

    private boolean tryPurchase(Player player, String configKey, ItemStack item, String displayName) {
        int cost = getCost(configKey);
        if (spendCoins(player.getUniqueId(), cost)) {
            player.getInventory().addItem(item);
            sendPurchaseMessage(player, displayName, cost);
            return true;
        } else {
            sendNotEnoughMessage(player, cost);
            return false;
        }
    }

    private boolean tryPurchaseSplashHealing(Player player) {
        int cost = getCost("splash-healing");
        if (spendCoins(player.getUniqueId(), cost)) {
            ItemStack potion = new ItemStack(Material.SPLASH_POTION);
            PotionMeta meta = (PotionMeta) potion.getItemMeta();
            meta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 1), true);
            meta.displayName(CastleSiegePlugin.deserialize("§dSplash Potion of Healing"));
            potion.setItemMeta(meta);
            player.getInventory().addItem(potion);
            sendPurchaseMessage(player, "Splash Healing Potion", cost);
            return true;
        } else {
            sendNotEnoughMessage(player, cost);
            return false;
        }
    }

    private boolean tryPurchaseDiamondSword(Player player) {
        int cost = getCost("diamond-sword");
        if (spendCoins(player.getUniqueId(), cost)) {
            ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
            sword.addEnchantment(Enchantment.SHARPNESS, 2);
            player.getInventory().addItem(sword);
            sendPurchaseMessage(player, "Diamond Sword", cost);
            return true;
        } else {
            sendNotEnoughMessage(player, cost);
            return false;
        }
    }

    private boolean tryPurchaseIronArmor(Player player) {
        int cost = getCost("iron-armor");
        if (spendCoins(player.getUniqueId(), cost)) {
            player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
            player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
            player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
            sendPurchaseMessage(player, "Iron Armor Set", cost);
            return true;
        } else {
            sendNotEnoughMessage(player, cost);
            return false;
        }
    }

    private boolean tryPurchaseSplashHarming(Player player) {
        int cost = getCost("splash-harming");
        if (spendCoins(player.getUniqueId(), cost)) {
            for (int i = 0; i < 2; i++) {
                ItemStack potion = new ItemStack(Material.SPLASH_POTION);
                PotionMeta meta = (PotionMeta) potion.getItemMeta();
                meta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 1), true);
                meta.displayName(CastleSiegePlugin.deserialize("§cSplash Potion of Harming"));
                potion.setItemMeta(meta);
                player.getInventory().addItem(potion);
            }
            sendPurchaseMessage(player, "Splash Harming x2", cost);
            return true;
        } else {
            sendNotEnoughMessage(player, cost);
            return false;
        }
    }

    private boolean tryPurchaseSplashPoison(Player player) {
        int cost = getCost("splash-poison");
        if (spendCoins(player.getUniqueId(), cost)) {
            for (int i = 0; i < 2; i++) {
                ItemStack potion = new ItemStack(Material.SPLASH_POTION);
                PotionMeta meta = (PotionMeta) potion.getItemMeta();
                meta.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 200, 1), true);
                meta.displayName(CastleSiegePlugin.deserialize("§2Splash Potion of Poison"));
                potion.setItemMeta(meta);
                player.getInventory().addItem(potion);
            }
            sendPurchaseMessage(player, "Splash Poison x2", cost);
            return true;
        } else {
            sendNotEnoughMessage(player, cost);
            return false;
        }
    }

    private boolean tryPurchasePowerBow(Player player) {
        int cost = getCost("power-bow");
        if (spendCoins(player.getUniqueId(), cost)) {
            ItemStack bow = new ItemStack(Material.BOW);
            bow.addEnchantment(Enchantment.POWER, 2);
            bow.addEnchantment(Enchantment.INFINITY, 1);
            player.getInventory().addItem(bow);
            player.getInventory().addItem(new ItemStack(Material.ARROW, 1));
            sendPurchaseMessage(player, "Power II Bow", cost);
            return true;
        } else {
            sendNotEnoughMessage(player, cost);
            return false;
        }
    }

    private boolean tryPurchaseIronGolem(Player player) {
        int cost = getCost("iron-golem");
        if (spendCoins(player.getUniqueId(), cost)) {
            if (castleManager != null) {
                castleManager.spawnAllyGolem(player.getLocation());
            }
            sendPurchaseMessage(player, "Iron Golem Defender", cost);
            return true;
        } else {
            sendNotEnoughMessage(player, cost);
            return false;
        }
    }

    private boolean tryPurchaseDiamondArmor(Player player) {
        int cost = getCost("diamond-armor");
        if (spendCoins(player.getUniqueId(), cost)) {
            player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
            player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
            player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            sendPurchaseMessage(player, "Diamond Armor Set", cost);
            return true;
        } else {
            sendNotEnoughMessage(player, cost);
            return false;
        }
    }

    private void sendPurchaseMessage(Player player, String item, int cost) {
        int remaining = getCoins(player.getUniqueId());
        String msg = plugin.getMessage("item-purchased")
                .replace("%item%", item)
                .replace("%cost%", String.valueOf(cost))
                .replace("%remaining%", String.valueOf(remaining));
        player.sendMessage(CastleSiegePlugin.deserialize(plugin.getMessagePrefix() + msg));
    }

    private void sendNotEnoughMessage(Player player, int cost) {
        String msg = plugin.getMessage("not-enough-coins")
                .replace("%cost%", String.valueOf(cost));
        player.sendMessage(CastleSiegePlugin.deserialize(plugin.getMessagePrefix() + msg));
    }

    private int getCost(String key) {
        return plugin.getConfig().getInt("shop." + key + ".cost", 5);
    }

    private ItemStack createShopItem(Material material, int amount, String name, int cost, int playerCoins) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        String affordable = playerCoins >= cost ? "§a" : "§c";
        meta.displayName(CastleSiegePlugin.deserialize(name));
        meta.lore(List.of(
                CastleSiegePlugin.deserialize(affordable + "Cost: §e" + cost + " coins"),
                CastleSiegePlugin.deserialize("§7You have: §e" + playerCoins + " coins")
        ));
        item.setItemMeta(meta);
        return item;
    }

    public boolean isShopInventory(Component title) {
        String plain = PlainTextComponentSerializer.plainText().serialize(title);
        return SHOP_TITLE_PLAIN.equals(plain);
    }

    public void reset() {
        playerCoins.clear();
    }
}
