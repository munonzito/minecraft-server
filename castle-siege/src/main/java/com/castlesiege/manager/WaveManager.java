package com.castlesiege.manager;

import com.castlesiege.CastleSiegePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class WaveManager {

    private final CastleSiegePlugin plugin;
    private final Set<UUID> aliveMobs = new HashSet<>();
    private int currentWave = 0;

    private static final Set<EntityType> SPECIAL_MOBS = Set.of(
            EntityType.RAVAGER, EntityType.EVOKER
    );

    public WaveManager(CastleSiegePlugin plugin) {
        this.plugin = plugin;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public int spawnWave(Location baseSpawn, Villager target) {
        currentWave++;
        aliveMobs.clear();

        int baseMobs = plugin.getConfig().getInt("game.base-mobs-per-wave", 8);
        int increase = plugin.getConfig().getInt("game.mobs-increase-per-wave", 5);
        int totalMobs = baseMobs + (currentWave - 1) * increase;
        double healthScale = plugin.getConfig().getDouble("game.health-scale-per-wave", 1.10);
        double healthMultiplier = Math.pow(healthScale, currentWave - 1);

        List<EntityType> availableTypes = getMobTypesForWave(currentWave);
        Random random = new Random();
        World world = baseSpawn.getWorld();

        for (int i = 0; i < totalMobs; i++) {
            EntityType type = availableTypes.get(random.nextInt(availableTypes.size()));
            double offsetX = (random.nextDouble() - 0.5) * 20;
            double offsetZ = (random.nextDouble() - 0.5) * 20;
            Location spawnLoc = baseSpawn.clone().add(offsetX, 0, offsetZ);
            spawnLoc.setY(world.getHighestBlockYAt(spawnLoc.getBlockX(), spawnLoc.getBlockZ()) + 1);

            // Ensure mobs spawn on the ground, not on trees/structures
            int groundY = world.getHighestBlockYAt(spawnLoc.getBlockX(), spawnLoc.getBlockZ());
            // Walk down from the highest block to find a non-leaf/non-log surface
            while (groundY > spawnLoc.getBlockY() - 10) {
                Material groundType = world.getBlockAt(spawnLoc.getBlockX(), groundY, spawnLoc.getBlockZ()).getType();
                if (!groundType.name().contains("LEAVES") && !groundType.name().contains("LOG")
                        && !groundType.name().contains("VINE") && groundType != Material.AIR) {
                    break;
                }
                groundY--;
            }
            spawnLoc.setY(groundY + 1);

            Mob mob = (Mob) world.spawnEntity(spawnLoc, type);
            mob.setRemoveWhenFarAway(false);

            AttributeInstance maxHealthAttr = mob.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double baseHealth = maxHealthAttr.getBaseValue();
                double newHealth = baseHealth * healthMultiplier;
                maxHealthAttr.setBaseValue(newHealth);
                mob.setHealth(newHealth);
            }

            if (currentWave >= 5 && random.nextInt(3) == 0) {
                equipMobArmor(mob, random);
            }

            if (target != null && !target.isDead()) {
                mob.setTarget(target);
            }

            aliveMobs.add(mob.getUniqueId());
        }

        return totalMobs;
    }

    private List<EntityType> getMobTypesForWave(int wave) {
        List<EntityType> types = new ArrayList<>();
        // Illager-only waves
        types.add(EntityType.PILLAGER);

        if (wave >= 3) {
            types.add(EntityType.VINDICATOR);
        }
        if (wave >= 5) {
            types.add(EntityType.WITCH);
        }
        if (wave >= 7) {
            types.add(EntityType.EVOKER);
        }
        if (wave >= 10) {
            types.add(EntityType.RAVAGER);
        }
        return types;
    }

    private void equipMobArmor(Mob mob, Random random) {
        if (!(mob instanceof LivingEntity living)) return;
        if (living.getEquipment() == null) return;

        if (random.nextBoolean()) {
            living.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
        }
        if (random.nextBoolean()) {
            living.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        }
    }

    public void onMobDeath(UUID mobId) {
        aliveMobs.remove(mobId);
    }

    public boolean isWaveMob(UUID entityId) {
        return aliveMobs.contains(entityId);
    }

    public boolean isSpecialMob(EntityType type) {
        return SPECIAL_MOBS.contains(type);
    }

    public boolean isWaveCleared() {
        return aliveMobs.isEmpty();
    }

    public int getAliveCount() {
        return aliveMobs.size();
    }

    public void retargetMobs(World world, Villager target) {
        if (target == null || target.isDead()) return;

        Iterator<UUID> it = aliveMobs.iterator();
        while (it.hasNext()) {
            UUID mobId = it.next();
            Entity entity = Bukkit.getEntity(mobId);
            if (entity == null || entity.isDead() || !entity.isValid()) {
                it.remove();
                continue;
            }
            if (entity instanceof Mob mob) {
                if (mob.getTarget() == null || mob.getTarget().isDead()) {
                    mob.setTarget(target);
                }
            }
        }

        // Retarget Vexes (summoned by Evokers) to nearest player instead of Aldeano
        for (Entity entity : world.getEntitiesByClass(Vex.class)) {
            Vex vex = (Vex) entity;
            if (vex.isDead()) continue;
            if (vex.getTarget() != null && vex.getTarget() instanceof Player && !vex.getTarget().isDead()) continue;
            Player nearest = null;
            double nearestDist = Double.MAX_VALUE;
            for (Player p : world.getPlayers()) {
                if (p.isDead() || p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
                double dist = p.getLocation().distanceSquared(vex.getLocation());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = p;
                }
            }
            if (nearest != null) {
                vex.setTarget(nearest);
            }
        }
    }

    public void removeAllMobs(World world) {
        for (UUID mobId : aliveMobs) {
            Entity entity = Bukkit.getEntity(mobId);
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
        aliveMobs.clear();
    }

    public void reset() {
        aliveMobs.clear();
        currentWave = 0;
    }
}
