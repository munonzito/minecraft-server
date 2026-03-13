package com.castlesiege.listener;

import com.castlesiege.CastleSiegePlugin;
import com.castlesiege.manager.CastleManager;
import com.castlesiege.manager.GameManager;
import com.castlesiege.manager.ShopManager;
import com.castlesiege.manager.WaveManager;
import com.castlesiege.model.GameState;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GameListener implements Listener {

    private final CastleSiegePlugin plugin;
    private final GameManager gameManager;
    private final CastleManager castleManager;
    private final WaveManager waveManager;
    private final ShopManager shopManager;

    public GameListener(CastleSiegePlugin plugin, GameManager gameManager,
                        CastleManager castleManager, WaveManager waveManager,
                        ShopManager shopManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.castleManager = castleManager;
        this.waveManager = waveManager;
        this.shopManager = shopManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!gameManager.isGameActive()) return;

        LivingEntity entity = event.getEntity();

        // Check if aldeano died (in any active state)
        if (castleManager.isAldeano(entity)) {
            gameManager.onVillagerDeath();
            return;
        }

        // Check if it's a wave mob
        if (gameManager.getState() != GameState.WAVE_ACTIVE) return;
        if (entity instanceof Mob && waveManager.isWaveMob(entity.getUniqueId())) {
            waveManager.onMobDeath(entity.getUniqueId());
            event.setDroppedExp(0);
            event.getDrops().clear();

            // Award coins to the killer
            Player killer = entity.getKiller();
            if (killer != null && gameManager.isPlayerInGame(killer.getUniqueId())) {
                boolean special = waveManager.isSpecialMob(entity.getType());
                int coins = special
                        ? plugin.getConfig().getInt("game.coins-per-special-kill", 3)
                        : plugin.getConfig().getInt("game.coins-per-kill", 1);

                shopManager.addCoins(killer.getUniqueId(), coins);
                int total = shopManager.getCoins(killer.getUniqueId());

                String msg = plugin.getMessage("player-kill")
                        .replace("%coins%", String.valueOf(coins))
                        .replace("%total%", String.valueOf(total));
                killer.sendMessage(CastleSiegePlugin.deserialize(plugin.getMessagePrefix() + msg));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!gameManager.isGameActive()) return;

        // Notify players when the aldeano is hit
        if (castleManager.isAldeano(event.getEntity())) {
            Villager aldeano = castleManager.getAldeano();
            double healthAfter = Math.max(0, aldeano.getHealth() - event.getFinalDamage());
            var maxHealthAttr = aldeano.getAttribute(Attribute.MAX_HEALTH);
            double maxHp = maxHealthAttr != null ? maxHealthAttr.getBaseValue() : 20.0;

            if (healthAfter > 0) {
                String msg = plugin.getMessage("villager-hit")
                        .replace("%health%", String.valueOf((int) healthAfter))
                        .replace("%max%", String.valueOf((int) maxHp));

                Component component = CastleSiegePlugin.deserialize(plugin.getMessagePrefix() + msg);
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    if (gameManager.isPlayerInGame(p.getUniqueId())) {
                        p.sendMessage(component);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!gameManager.isPlayerInGame(player.getUniqueId())) return;
        if (gameManager.getState() != GameState.WAVE_ACTIVE) return;

        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.deathMessage(null);

        gameManager.handlePlayerDeath(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (gameManager.isPlayerInGame(event.getPlayer().getUniqueId())) {
            gameManager.handlePlayerQuit(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!gameManager.isGameActive()) return;
        if (!gameManager.isPlayerInGame(event.getPlayer().getUniqueId())) return;

        if (castleManager.isShopKeeper(event.getRightClicked())) {
            event.setCancelled(true);
            shopManager.openShop(event.getPlayer());
        }

        // Prevent interacting with aldeano
        if (castleManager.isAldeano(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!gameManager.isPlayerInGame(player.getUniqueId())) return;

        if (shopManager.isShopInventory(event.getView().title())) {
            event.setCancelled(true);
            if (event.getRawSlot() >= 0 && event.getRawSlot() < 27) {
                shopManager.handleShopClick(player, event.getRawSlot());
                shopManager.openShop(player);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!gameManager.isGameActive()) return;

        // Prevent players from damaging the aldeano
        if (castleManager.isAldeano(event.getEntity()) && event.getDamager() instanceof Player) {
            event.setCancelled(true);
        }

        // Prevent players from damaging the shop keeper
        if (castleManager.isShopKeeper(event.getEntity())) {
            event.setCancelled(true);
        }

        // Prevent players from damaging ally golems
        if (castleManager.isAlly(event.getEntity()) && event.getDamager() instanceof Player) {
            event.setCancelled(true);
        }

        // Prevent friendly fire
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player attacker) {
            if (gameManager.isPlayerInGame(victim.getUniqueId()) && gameManager.isPlayerInGame(attacker.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (gameManager.isPlayerInGame(event.getPlayer().getUniqueId())
                && gameManager.getState() != GameState.IDLE) {
            event.setCancelled(true);
        }
    }
}
