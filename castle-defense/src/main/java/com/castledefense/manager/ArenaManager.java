package com.castledefense.manager;

import com.castledefense.CastleDefensePlugin;
import com.castledefense.model.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

public class ArenaManager {

    private final CastleDefensePlugin plugin;
    private Location attackersSpawn;
    private Location defendersSpawn;
    private Location targetBlockLocation;
    private Material targetBlockMaterial;

    public ArenaManager(CastleDefensePlugin plugin) {
        this.plugin = plugin;
        loadFromConfig();
    }

    private void loadFromConfig() {
        FileConfiguration config = plugin.getConfig();
        String worldName = config.getString("arena.world", "world");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            plugin.getLogger().warning("Arena world '" + worldName + "' not found! Using default world.");
            world = Bukkit.getWorlds().get(0);
        }

        attackersSpawn = new Location(
                world,
                config.getDouble("arena.attackers-spawn.x"),
                config.getDouble("arena.attackers-spawn.y"),
                config.getDouble("arena.attackers-spawn.z"),
                (float) config.getDouble("arena.attackers-spawn.yaw"),
                (float) config.getDouble("arena.attackers-spawn.pitch")
        );

        defendersSpawn = new Location(
                world,
                config.getDouble("arena.defenders-spawn.x"),
                config.getDouble("arena.defenders-spawn.y"),
                config.getDouble("arena.defenders-spawn.z"),
                (float) config.getDouble("arena.defenders-spawn.yaw"),
                (float) config.getDouble("arena.defenders-spawn.pitch")
        );

        targetBlockLocation = new Location(
                world,
                config.getInt("arena.target-block.x"),
                config.getInt("arena.target-block.y"),
                config.getInt("arena.target-block.z")
        );

        String materialName = config.getString("arena.target-block.material", "ORANGE_BANNER");
        try {
            targetBlockMaterial = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid target block material: " + materialName + ". Using ORANGE_BANNER.");
            targetBlockMaterial = Material.ORANGE_BANNER;
        }
    }

    public void setSpawn(Team team, Location location) {
        String path = team == Team.ATTACKERS ? "arena.attackers-spawn" : "arena.defenders-spawn";
        plugin.getConfig().set(path + ".x", location.getX());
        plugin.getConfig().set(path + ".y", location.getY());
        plugin.getConfig().set(path + ".z", location.getZ());
        plugin.getConfig().set(path + ".yaw", location.getYaw());
        plugin.getConfig().set(path + ".pitch", location.getPitch());
        plugin.saveConfig();

        if (team == Team.ATTACKERS) {
            attackersSpawn = location;
        } else {
            defendersSpawn = location;
        }
    }

    public void setTargetBlock(Location location) {
        plugin.getConfig().set("arena.target-block.x", location.getBlockX());
        plugin.getConfig().set("arena.target-block.y", location.getBlockY());
        plugin.getConfig().set("arena.target-block.z", location.getBlockZ());
        plugin.saveConfig();
        targetBlockLocation = location;
    }

    public void placeTargetBlock() {
        if (targetBlockLocation != null && targetBlockMaterial != null) {
            targetBlockLocation.getBlock().setType(targetBlockMaterial);
        }
    }

    public boolean isTargetBlock(Location location) {
        if (targetBlockLocation == null) return false;
        return location.getBlockX() == targetBlockLocation.getBlockX()
                && location.getBlockY() == targetBlockLocation.getBlockY()
                && location.getBlockZ() == targetBlockLocation.getBlockZ();
    }

    public Location getSpawn(Team team) {
        return team == Team.ATTACKERS ? attackersSpawn.clone() : defendersSpawn.clone();
    }

    public Location getTargetBlockLocation() {
        return targetBlockLocation;
    }

    public Material getTargetBlockMaterial() {
        return targetBlockMaterial;
    }
}
