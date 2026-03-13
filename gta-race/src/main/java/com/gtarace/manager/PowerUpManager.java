package com.gtarace.manager;

import com.gtarace.GTARacePlugin;
import com.gtarace.model.PowerUpType;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class PowerUpManager {

    private final GTARacePlugin plugin;
    private final VehicleManager vehicleManager;

    private final Map<ArmorStand, PowerUpType> activePickups = new HashMap<>();
    private final Map<Location, Long> cooldowns = new HashMap<>();
    private final Map<UUID, PowerUpType> heldPowerUps = new HashMap<>();
    private BukkitTask particleTask;
    private BukkitTask pickupCheckTask;

    private int respawnSeconds;

    public PowerUpManager(GTARacePlugin plugin, VehicleManager vehicleManager) {
        this.plugin = plugin;
        this.vehicleManager = vehicleManager;
        this.respawnSeconds = plugin.getConfig().getInt("race.powerup-respawn-seconds", 10);
    }

    public void spawnAllPickups(List<TrackManager.PowerUpSpawn> spawns) {
        removeAllPickups();
        for (TrackManager.PowerUpSpawn spawn : spawns) {
            spawnPickup(spawn.location(), spawn.type());
        }
    }

    private void spawnPickup(Location location, PowerUpType type) {
        Location spawnLoc = location.clone().add(0, 1, 0);
        ArmorStand stand = spawnLoc.getWorld().spawn(spawnLoc, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setSmall(true);
            as.setMarker(true);
            as.setCustomName(type.getColor() + "? " + type.getDisplayName() + " ?");
            as.setCustomNameVisible(true);
            as.getEquipment().setHelmet(new ItemStack(type.getDisplayMaterial()));
        });
        activePickups.put(stand, type);
    }

    public void startTasks(Set<UUID> racePlayers) {
        stopTasks();

        particleTask = new BukkitRunnable() {
            double angle = 0;
            @Override
            public void run() {
                angle += 0.3;
                for (Map.Entry<ArmorStand, PowerUpType> entry : activePickups.entrySet()) {
                    ArmorStand stand = entry.getKey();
                    if (stand.isDead()) continue;
                    Location loc = stand.getLocation();
                    double x = Math.cos(angle) * 0.5;
                    double z = Math.sin(angle) * 0.5;
                    Particle particle = switch (entry.getValue()) {
                        case SPEED_BOOST -> Particle.HAPPY_VILLAGER;
                        case SHIELD -> Particle.DRIPPING_WATER;
                        case MISSILE -> Particle.FLAME;
                        case OIL_SLICK -> Particle.SMOKE;
                    };
                    loc.getWorld().spawnParticle(particle, loc.getX() + x, loc.getY() + 0.5, loc.getZ() + z, 1, 0, 0, 0, 0);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);

        pickupCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                Iterator<Map.Entry<Location, Long>> cooldownIt = cooldowns.entrySet().iterator();
                while (cooldownIt.hasNext()) {
                    Map.Entry<Location, Long> entry = cooldownIt.next();
                    if (now >= entry.getValue()) {
                        cooldownIt.remove();
                    }
                }

                for (UUID uuid : racePlayers) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null) continue;
                    if (heldPowerUps.containsKey(uuid)) continue;

                    // Use boat location for pickup detection
                    Location playerLoc;
                    Boat boat = vehicleManager.getPlayerBoat(uuid);
                    if (boat != null && !boat.isDead()) {
                        playerLoc = boat.getLocation();
                    } else {
                        playerLoc = player.getLocation();
                    }

                    Iterator<Map.Entry<ArmorStand, PowerUpType>> it = activePickups.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<ArmorStand, PowerUpType> entry = it.next();
                        ArmorStand stand = entry.getKey();
                        if (stand.isDead()) {
                            it.remove();
                            continue;
                        }

                        if (stand.getLocation().distanceSquared(playerLoc) < 16.0) {
                            PowerUpType type = entry.getValue();
                            Location standLoc = stand.getLocation().clone();

                            stand.remove();
                            it.remove();

                            heldPowerUps.put(uuid, type);
                            givePowerUpItem(player, type);
                            player.sendMessage(Component.text(plugin.getMessagePrefix() + "§aPicked up " + type.getColoredName() + "§a! Press §bF §aor §bQ §ato use."));
                            player.playSound(playerLoc, Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f);

                            cooldowns.put(standLoc, now + (respawnSeconds * 1000L));

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    spawnPickup(standLoc.subtract(0, 1, 0), type);
                                }
                            }.runTaskLater(plugin, respawnSeconds * 20L);

                            break;
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void givePowerUpItem(Player player, PowerUpType type) {
        ItemStack item = new ItemStack(type.getDisplayMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(type.getColor() + type.getDisplayName() + " §7[Press F or Q]"));
        item.setItemMeta(meta);
        player.getInventory().setItemInMainHand(item);
    }

    private void removePowerUpItem(Player player) {
        player.getInventory().setItemInMainHand(null);
    }

    public void usePowerUp(Player player) {
        UUID uuid = player.getUniqueId();
        PowerUpType type = heldPowerUps.remove(uuid);
        if (type == null) return;

        removePowerUpItem(player);

        switch (type) {
            case SPEED_BOOST -> {
                vehicleManager.applySpeedBoost(uuid, 3000);
                player.sendMessage(Component.text(plugin.getMessagePrefix() + plugin.getMessage("powerup-speed")));
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.5f);
            }
            case SHIELD -> {
                vehicleManager.applyShield(uuid, 5000);
                player.sendMessage(Component.text(plugin.getMessagePrefix() + plugin.getMessage("powerup-shield")));
                player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.5f, 1.5f);
            }
            case MISSILE -> {
                vehicleManager.fireMissile(player);
                player.sendMessage(Component.text(plugin.getMessagePrefix() + plugin.getMessage("powerup-missile")));
            }
            case OIL_SLICK -> {
                dropOilSlick(player);
                player.sendMessage(Component.text(plugin.getMessagePrefix() + plugin.getMessage("powerup-oil")));
                player.playSound(player.getLocation(), Sound.BLOCK_SLIME_BLOCK_PLACE, 1.0f, 0.5f);
            }
        }
    }

    private void dropOilSlick(Player player) {
        Location loc = player.getLocation().clone();
        loc.getWorld().spawn(loc.add(0, 0.1, 0), ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setSmall(true);
            as.setMarker(true);
            as.setCustomName("§8Oil Slick");
            as.setCustomNameVisible(false);

            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (as.isDead() || ticks > 200) {
                        as.remove();
                        cancel();
                        return;
                    }
                    Location slickLoc = as.getLocation();
                    slickLoc.getWorld().spawnParticle(Particle.SMOKE, slickLoc, 5, 0.8, 0.1, 0.8, 0);

                    for (Map.Entry<UUID, org.bukkit.entity.Boat> entry :
                            getActiveBoats().entrySet()) {
                        if (entry.getKey().equals(player.getUniqueId())) continue;
                        org.bukkit.entity.Boat boat = entry.getValue();
                        if (boat.getLocation().distanceSquared(slickLoc) < 4.0) {
                            vehicleManager.damageCar(entry.getKey(), 15);
                            org.bukkit.util.Vector spin = boat.getVelocity();
                            spin.setX(spin.getX() * -0.5 + (Math.random() - 0.5));
                            spin.setZ(spin.getZ() * -0.5 + (Math.random() - 0.5));
                            boat.setVelocity(spin);
                            Player victim = Bukkit.getPlayer(entry.getKey());
                            if (victim != null) {
                                victim.sendMessage(Component.text(plugin.getMessagePrefix() + "§8You hit an oil slick!"));
                                victim.playSound(victim.getLocation(), Sound.BLOCK_SLIME_BLOCK_STEP, 1.0f, 0.5f);
                            }
                        }
                    }
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 5L);
        });
    }

    private Map<UUID, org.bukkit.entity.Boat> getActiveBoats() {
        Map<UUID, org.bukkit.entity.Boat> boats = new HashMap<>();
        for (UUID uuid : heldPowerUps.keySet()) {
            org.bukkit.entity.Boat boat = vehicleManager.getPlayerBoat(uuid);
            if (boat != null) boats.put(uuid, boat);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            org.bukkit.entity.Boat boat = vehicleManager.getPlayerBoat(player.getUniqueId());
            if (boat != null) boats.put(player.getUniqueId(), boat);
        }
        return boats;
    }

    public boolean hasPowerUp(UUID playerId) {
        return heldPowerUps.containsKey(playerId);
    }

    public PowerUpType getHeldPowerUp(UUID playerId) {
        return heldPowerUps.get(playerId);
    }

    public void stopTasks() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
        if (pickupCheckTask != null) {
            pickupCheckTask.cancel();
            pickupCheckTask = null;
        }
    }

    public void removeAllPickups() {
        for (ArmorStand stand : activePickups.keySet()) {
            if (!stand.isDead()) stand.remove();
        }
        activePickups.clear();
        heldPowerUps.clear();
        cooldowns.clear();
    }

    public void cleanup() {
        stopTasks();
        removeAllPickups();
    }
}
