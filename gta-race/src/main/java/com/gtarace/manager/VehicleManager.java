package com.gtarace.manager;

import com.gtarace.GTARacePlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.entity.boat.OakBoat;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class VehicleManager {

    private final GTARacePlugin plugin;
    private final Map<UUID, Boat> playerBoats = new HashMap<>();
    private final Map<UUID, Integer> carHealth = new HashMap<>();
    private final Map<UUID, Long> speedBoostEndTime = new HashMap<>();
    private final Map<UUID, Long> shieldEndTime = new HashMap<>();
    private BukkitTask speedTask;
    private BukkitTask freezeTask;
    private boolean frozen = false;

    private int maxHealth;
    private double speedMultiplier;

    public VehicleManager(GTARacePlugin plugin) {
        this.plugin = plugin;
        this.maxHealth = plugin.getConfig().getInt("race.car-max-health", 100);
        this.speedMultiplier = plugin.getConfig().getDouble("race.vehicle-speed-multiplier", 1.8);
    }

    public void spawnVehicle(Player player, Location location) {
        Location spawnLoc = location.clone();
        spawnLoc.setY(spawnLoc.getY() + 0.5);

        Boat boat = spawnLoc.getWorld().spawn(spawnLoc, OakBoat.class, b -> {
            b.setCustomName(player.getName() + "'s Car");
            b.setCustomNameVisible(true);
            b.setInvulnerable(true);
        });

        boat.addPassenger(player);
        playerBoats.put(player.getUniqueId(), boat);
        carHealth.put(player.getUniqueId(), maxHealth);
    }

    public void removeVehicle(UUID playerId) {
        Boat boat = playerBoats.remove(playerId);
        if (boat != null && !boat.isDead()) {
            boat.getPassengers().forEach(boat::removePassenger);
            boat.remove();
        }
        carHealth.remove(playerId);
        speedBoostEndTime.remove(playerId);
        shieldEndTime.remove(playerId);
    }

    public void removeAllVehicles() {
        for (UUID uuid : new HashSet<>(playerBoats.keySet())) {
            removeVehicle(uuid);
        }
    }

    public void startFreezeTask() {
        frozen = true;
        if (freezeTask != null) freezeTask.cancel();
        freezeTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!frozen) {
                    cancel();
                    return;
                }
                for (Boat boat : playerBoats.values()) {
                    if (boat != null && !boat.isDead()) {
                        boat.setVelocity(new Vector(0, 0, 0));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void stopFreezeTask() {
        frozen = false;
        if (freezeTask != null) {
            freezeTask.cancel();
            freezeTask = null;
        }
    }

    public void startSpeedBoostTask() {
        if (speedTask != null) speedTask.cancel();

        speedTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Map.Entry<UUID, Boat> entry : playerBoats.entrySet()) {
                    Boat boat = entry.getValue();
                    if (boat == null || boat.isDead()) continue;
                    if (boat.getPassengers().isEmpty()) continue;

                    Vector velocity = boat.getVelocity();
                    double horizontalSpeed = Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ());

                    if (horizontalSpeed > 0.01) {
                        double multiplier = speedMultiplier;

                        Long boostEnd = speedBoostEndTime.get(entry.getKey());
                        if (boostEnd != null && now < boostEnd) {
                            multiplier *= 1.8;
                        }

                        velocity.setX(velocity.getX() * multiplier);
                        velocity.setZ(velocity.getZ() * multiplier);

                        double maxSpeed = 2.0;
                        double newSpeed = Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ());
                        if (newSpeed > maxSpeed) {
                            double scale = maxSpeed / newSpeed;
                            velocity.setX(velocity.getX() * scale);
                            velocity.setZ(velocity.getZ() * scale);
                        }

                        boat.setVelocity(velocity);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void stopSpeedBoostTask() {
        if (speedTask != null) {
            speedTask.cancel();
            speedTask = null;
        }
    }

    public void applySpeedBoost(UUID playerId, int durationMs) {
        speedBoostEndTime.put(playerId, System.currentTimeMillis() + durationMs);
    }

    public void applyShield(UUID playerId, int durationMs) {
        shieldEndTime.put(playerId, System.currentTimeMillis() + durationMs);
    }

    public boolean hasShield(UUID playerId) {
        Long end = shieldEndTime.get(playerId);
        return end != null && System.currentTimeMillis() < end;
    }

    public boolean hasSpeedBoost(UUID playerId) {
        Long end = speedBoostEndTime.get(playerId);
        return end != null && System.currentTimeMillis() < end;
    }

    public void damageCar(UUID playerId, int amount) {
        if (hasShield(playerId)) {
            shieldEndTime.remove(playerId);
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage(Component.text(plugin.getMessagePrefix() + "§9Shield absorbed the hit!"));
            }
            return;
        }

        int health = carHealth.getOrDefault(playerId, maxHealth);
        health = Math.max(0, health - amount);
        carHealth.put(playerId, health);

        if (health <= 0) {
            destroyCar(playerId);
        }
    }

    private void destroyCar(UUID playerId) {
        Boat boat = playerBoats.get(playerId);
        Player player = Bukkit.getPlayer(playerId);

        if (boat != null && !boat.isDead()) {
            Location loc = boat.getLocation();
            loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 3, 1, 1, 1, 0);
            loc.getWorld().spawnParticle(Particle.FLAME, loc, 30, 1, 0.5, 1, 0.1);
            loc.getWorld().spawnParticle(Particle.SMOKE, loc, 20, 1, 1, 1, 0.05);
            loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
        }

        removeVehicle(playerId);

        if (player != null) {
            player.sendMessage(Component.text(plugin.getMessagePrefix() + plugin.getMessage("car-destroyed")));
        }
    }

    public void respawnVehicle(Player player, Location location) {
        removeVehicle(player.getUniqueId());
        spawnVehicle(player, location);
    }

    public void fireMissile(Player shooter) {
        Boat boat = playerBoats.get(shooter.getUniqueId());
        if (boat == null) return;

        Location loc = boat.getLocation().add(0, 1, 0);
        Vector direction = loc.getDirection().normalize().multiply(1.5);

        loc.getWorld().spawn(loc, org.bukkit.entity.Fireball.class, fireball -> {
            fireball.setDirection(direction);
            fireball.setShooter(shooter);
            fireball.setYield(0);
            fireball.setIsIncendiary(false);
            fireball.setCustomName("RaceMissile");
        });

        loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.5f);
    }

    public int getCarHealth(UUID playerId) {
        return carHealth.getOrDefault(playerId, maxHealth);
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public Boat getPlayerBoat(UUID playerId) {
        return playerBoats.get(playerId);
    }

    public boolean isRaceBoat(Boat boat) {
        return playerBoats.containsValue(boat);
    }

    public UUID getBoatOwner(Boat boat) {
        for (Map.Entry<UUID, Boat> entry : playerBoats.entrySet()) {
            if (entry.getValue().equals(boat)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public String getHealthBar(UUID playerId) {
        int health = getCarHealth(playerId);
        int bars = (int) Math.ceil((health / (double) maxHealth) * 10);
        StringBuilder sb = new StringBuilder("§7[Car: ");
        for (int i = 0; i < 10; i++) {
            if (i < bars) {
                if (bars > 6) sb.append("§a");
                else if (bars > 3) sb.append("§e");
                else sb.append("§c");
                sb.append("|");
            } else {
                sb.append("§8_");
            }
        }
        sb.append("§7] ").append(health).append("%");
        return sb.toString();
    }
}
