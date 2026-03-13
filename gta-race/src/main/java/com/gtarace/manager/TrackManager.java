package com.gtarace.manager;

import com.gtarace.GTARacePlugin;
import com.gtarace.model.Checkpoint;
import com.gtarace.model.PowerUpType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class TrackManager {

    private final GTARacePlugin plugin;
    private Location startLocation;
    private Location finishLocation;
    private final List<Checkpoint> checkpoints = new ArrayList<>();
    private double checkpointRadius;
    private double finishRadius;

    public record PowerUpSpawn(Location location, PowerUpType type) {}
    private final List<PowerUpSpawn> powerUpSpawns = new ArrayList<>();

    public TrackManager(GTARacePlugin plugin) {
        this.plugin = plugin;
        loadFromConfig();
    }

    private void loadFromConfig() {
        FileConfiguration config = plugin.getConfig();
        String worldName = config.getString("track.world", "world");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            plugin.getLogger().warning("Track world '" + worldName + "' not found! Using default world.");
            world = Bukkit.getWorlds().get(0);
        }

        checkpointRadius = config.getDouble("track.checkpoint-radius", 5.0);
        finishRadius = config.getDouble("track.finish-radius", 5.0);

        startLocation = new Location(
                world,
                config.getDouble("track.start.x"),
                config.getDouble("track.start.y"),
                config.getDouble("track.start.z"),
                (float) config.getDouble("track.start.yaw"),
                (float) config.getDouble("track.start.pitch")
        );

        finishLocation = new Location(
                world,
                config.getDouble("track.finish.x"),
                config.getDouble("track.finish.y"),
                config.getDouble("track.finish.z")
        );

        checkpoints.clear();
        List<?> cpList = config.getList("track.checkpoints");
        if (cpList != null) {
            ConfigurationSection cpSection = config.getConfigurationSection("track.checkpoints");
            if (cpSection != null) {
                for (String key : cpSection.getKeys(false)) {
                    ConfigurationSection cp = cpSection.getConfigurationSection(key);
                    if (cp == null) continue;
                    int index = checkpoints.size();
                    Location loc = new Location(
                            world,
                            cp.getDouble("x"),
                            cp.getDouble("y"),
                            cp.getDouble("z")
                    );
                    double radius = cp.getDouble("radius", checkpointRadius);
                    checkpoints.add(new Checkpoint(index, loc, radius));
                }
            }
        }

        powerUpSpawns.clear();
        ConfigurationSection puSection = config.getConfigurationSection("track.powerup-spawns");
        if (puSection != null) {
            for (String key : puSection.getKeys(false)) {
                ConfigurationSection pu = puSection.getConfigurationSection(key);
                if (pu == null) continue;
                Location loc = new Location(
                        world,
                        pu.getDouble("x"),
                        pu.getDouble("y"),
                        pu.getDouble("z")
                );
                PowerUpType type;
                try {
                    type = PowerUpType.valueOf(pu.getString("type", "SPEED_BOOST"));
                } catch (IllegalArgumentException e) {
                    type = PowerUpType.SPEED_BOOST;
                }
                powerUpSpawns.add(new PowerUpSpawn(loc, type));
            }
        }
    }

    public void setStart(Location location) {
        startLocation = location.clone();
        plugin.getConfig().set("track.start.x", location.getX());
        plugin.getConfig().set("track.start.y", location.getY());
        plugin.getConfig().set("track.start.z", location.getZ());
        plugin.getConfig().set("track.start.yaw", location.getYaw());
        plugin.getConfig().set("track.start.pitch", location.getPitch());
        plugin.getConfig().set("track.world", location.getWorld().getName());
        plugin.saveConfig();
    }

    public void setFinish(Location location) {
        finishLocation = location.clone();
        plugin.getConfig().set("track.finish.x", location.getX());
        plugin.getConfig().set("track.finish.y", location.getY());
        plugin.getConfig().set("track.finish.z", location.getZ());
        plugin.saveConfig();
    }

    public int addCheckpoint(Location location) {
        int index = checkpoints.size();
        checkpoints.add(new Checkpoint(index, location.clone(), checkpointRadius));
        saveCheckpoints();
        return index;
    }

    public boolean setCheckpoint(int index, Location location) {
        if (index < 0 || index >= checkpoints.size()) return false;
        checkpoints.set(index, new Checkpoint(index, location.clone(), checkpointRadius));
        saveCheckpoints();
        return true;
    }

    public boolean removeCheckpoint(int index) {
        if (index < 0 || index >= checkpoints.size()) return false;
        checkpoints.remove(index);
        for (int i = 0; i < checkpoints.size(); i++) {
            Checkpoint old = checkpoints.get(i);
            checkpoints.set(i, new Checkpoint(i, old.getLocation(), old.getRadius()));
        }
        saveCheckpoints();
        return true;
    }

    private void saveCheckpoints() {
        plugin.getConfig().set("track.checkpoints", null);
        for (int i = 0; i < checkpoints.size(); i++) {
            Checkpoint cp = checkpoints.get(i);
            String path = "track.checkpoints." + i;
            plugin.getConfig().set(path + ".x", cp.getLocation().getX());
            plugin.getConfig().set(path + ".y", cp.getLocation().getY());
            plugin.getConfig().set(path + ".z", cp.getLocation().getZ());
            plugin.getConfig().set(path + ".radius", cp.getRadius());
        }
        plugin.saveConfig();
    }

    public int addPowerUpSpawn(Location location, PowerUpType type) {
        int index = powerUpSpawns.size();
        powerUpSpawns.add(new PowerUpSpawn(location.clone(), type));
        savePowerUpSpawns();
        return index;
    }

    public boolean removePowerUpSpawn(int index) {
        if (index < 0 || index >= powerUpSpawns.size()) return false;
        powerUpSpawns.remove(index);
        savePowerUpSpawns();
        return true;
    }

    private void savePowerUpSpawns() {
        plugin.getConfig().set("track.powerup-spawns", null);
        for (int i = 0; i < powerUpSpawns.size(); i++) {
            PowerUpSpawn ps = powerUpSpawns.get(i);
            String path = "track.powerup-spawns." + i;
            plugin.getConfig().set(path + ".x", ps.location().getX());
            plugin.getConfig().set(path + ".y", ps.location().getY());
            plugin.getConfig().set(path + ".z", ps.location().getZ());
            plugin.getConfig().set(path + ".type", ps.type().name());
        }
        plugin.saveConfig();
    }

    public Location getStartLocation() {
        return startLocation != null ? startLocation.clone() : null;
    }

    public Location getFinishLocation() {
        return finishLocation != null ? finishLocation.clone() : null;
    }

    public List<Checkpoint> getCheckpoints() {
        return new ArrayList<>(checkpoints);
    }

    public int getCheckpointCount() {
        return checkpoints.size();
    }

    public Checkpoint getCheckpoint(int index) {
        if (index < 0 || index >= checkpoints.size()) return null;
        return checkpoints.get(index);
    }

    public double getFinishRadius() {
        return finishRadius;
    }

    public boolean isAtFinish(Location loc) {
        if (finishLocation == null) return false;
        if (!loc.getWorld().equals(finishLocation.getWorld())) return false;
        double dx = loc.getX() - finishLocation.getX();
        double dz = loc.getZ() - finishLocation.getZ();
        return (dx * dx + dz * dz) <= (finishRadius * finishRadius);
    }

    public List<PowerUpSpawn> getPowerUpSpawns() {
        return new ArrayList<>(powerUpSpawns);
    }
}
