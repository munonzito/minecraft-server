package com.castledefense.listener;

import com.castledefense.CastleDefensePlugin;
import com.castledefense.manager.ArenaManager;
import com.castledefense.manager.GameManager;
import com.castledefense.model.GameState;
import com.castledefense.model.Team;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class GameListener implements Listener {

    private final CastleDefensePlugin plugin;
    private final GameManager gameManager;
    private final ArenaManager arenaManager;

    public GameListener(CastleDefensePlugin plugin, GameManager gameManager, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.arenaManager = arenaManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (gameManager.getState() != GameState.ACTIVE) return;

        Player player = event.getPlayer();
        if (!gameManager.isPlayerInGame(player.getUniqueId())) return;

        if (arenaManager.isTargetBlock(event.getBlock().getLocation())) {
            Team team = gameManager.getPlayerTeam(player.getUniqueId());
            if (team == Team.ATTACKERS) {
                event.setDropItems(false);
                gameManager.onTargetBlockBroken(player);
            } else {
                event.setCancelled(true);
                player.sendMessage(Component.text(plugin.getMessagePrefix() + "§cYou can't break your own banner!"));
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!gameManager.isPlayerInGame(player.getUniqueId())) return;
        if (gameManager.getState() != GameState.ACTIVE) return;

        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.deathMessage(null);

        gameManager.handlePlayerDeath(player);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!gameManager.isPlayerInGame(player.getUniqueId())) return;
        if (gameManager.getState() != GameState.ACTIVE) return;

        Team team = gameManager.getPlayerTeam(player.getUniqueId());
        if (team != null) {
            event.setRespawnLocation(arenaManager.getSpawn(team));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (gameManager.isPlayerInGame(event.getPlayer().getUniqueId())) {
            gameManager.handlePlayerQuit(event.getPlayer());
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (gameManager.getState() != GameState.ACTIVE) return;

        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player attacker) {
            if (!gameManager.isPlayerInGame(victim.getUniqueId())) return;
            if (!gameManager.isPlayerInGame(attacker.getUniqueId())) return;

            Team victimTeam = gameManager.getPlayerTeam(victim.getUniqueId());
            Team attackerTeam = gameManager.getPlayerTeam(attacker.getUniqueId());

            if (victimTeam == attackerTeam) {
                event.setCancelled(true);
                attacker.sendMessage(Component.text(plugin.getMessagePrefix() + "§cYou can't attack your teammate!"));
            }
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (gameManager.isPlayerInGame(event.getPlayer().getUniqueId()) && gameManager.getState() == GameState.ACTIVE) {
            event.setCancelled(true);
        }
    }
}
