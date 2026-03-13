package com.gtarace.listener;

import com.gtarace.GTARacePlugin;
import com.gtarace.manager.PowerUpManager;
import com.gtarace.manager.RaceManager;
import com.gtarace.manager.VehicleManager;
import com.gtarace.model.RaceState;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class RaceListener implements Listener {

    private final GTARacePlugin plugin;
    private final RaceManager raceManager;
    private final VehicleManager vehicleManager;
    private final PowerUpManager powerUpManager;

    public RaceListener(GTARacePlugin plugin, RaceManager raceManager,
                        VehicleManager vehicleManager, PowerUpManager powerUpManager) {
        this.plugin = plugin;
        this.raceManager = raceManager;
        this.vehicleManager = vehicleManager;
        this.powerUpManager = powerUpManager;
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (raceManager.getState() != RaceState.ACTIVE && raceManager.getState() != RaceState.COUNTDOWN) return;
        if (!(event.getExited() instanceof Player player)) return;
        if (!raceManager.isPlayerInRace(player.getUniqueId())) return;

        if (event.getVehicle() instanceof Boat boat && vehicleManager.isRaceBoat(boat)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        if (!(event.getVehicle() instanceof Boat boat)) return;
        if (!vehicleManager.isRaceBoat(boat)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (raceManager.getState() != RaceState.ACTIVE) return;

        if (event.getEntity() instanceof Boat boat && vehicleManager.isRaceBoat(boat)) {
            event.setCancelled(true);

            if (event.getDamager() instanceof Fireball fireball) {
                if (fireball.getShooter() instanceof Player shooter) {
                    java.util.UUID ownerId = vehicleManager.getBoatOwner(boat);
                    if (ownerId != null && !ownerId.equals(shooter.getUniqueId())) {
                        vehicleManager.damageCar(ownerId, 35);

                        int health = vehicleManager.getCarHealth(ownerId);
                        if (health <= 0) {
                            raceManager.handleCarDestroyed(ownerId);
                        }

                        Player victim = org.bukkit.Bukkit.getPlayer(ownerId);
                        if (victim != null) {
                            victim.sendMessage(Component.text(
                                    plugin.getMessagePrefix() + "§c" + shooter.getName() + " hit you with a missile!"));
                        }
                        shooter.sendMessage(Component.text(
                                plugin.getMessagePrefix() + "§aDirect hit on " + (victim != null ? victim.getName() : "a racer") + "!"));
                    }
                }
                fireball.remove();
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Fireball fireball)) return;
        String name = fireball.getCustomName();
        if (name == null || !name.equals("RaceMissile")) return;

        event.setCancelled(true);
        fireball.remove();
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof Fireball fireball) {
            String name = fireball.getCustomName();
            if (name != null && name.equals("RaceMissile")) {
                event.setCancelled(true);
                event.blockList().clear();
            }
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (raceManager.getState() != RaceState.ACTIVE) return;

        Player player = event.getPlayer();
        if (!raceManager.isPlayerInRace(player.getUniqueId())) return;

        event.setCancelled(true);

        if (powerUpManager.hasPowerUp(player.getUniqueId())) {
            powerUpManager.usePowerUp(player);
        } else {
            player.sendMessage(Component.text(plugin.getMessagePrefix() + "§7No power-up! Drive through a pickup to collect one."));
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        if (raceManager.getState() != RaceState.ACTIVE) return;

        Player player = event.getPlayer();
        if (!raceManager.isPlayerInRace(player.getUniqueId())) return;

        event.setCancelled(true);

        if (powerUpManager.hasPowerUp(player.getUniqueId())) {
            powerUpManager.usePowerUp(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (raceManager.isPlayerInRace(event.getPlayer().getUniqueId())) {
            raceManager.handlePlayerQuit(event.getPlayer());
        }
    }
}
