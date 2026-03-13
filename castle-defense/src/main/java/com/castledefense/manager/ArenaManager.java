package com.castledefense.manager;

import com.castledefense.CastleDefensePlugin;
import com.castledefense.model.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.Map;

public class ArenaManager {

    private final CastleDefensePlugin plugin;
    private final Map<Team, Location> spawns = new EnumMap<>(Team.class);
    private final Map<Team, Location> stables = new EnumMap<>(Team.class);
    private final Map<Team, Location> targetBlocks = new EnumMap<>(Team.class);

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

        for (Team team : Team.values()) {
            String key = team.name().toLowerCase();

            spawns.put(team, new Location(
                    world,
                    config.getDouble("arena." + key + "-spawn.x"),
                    config.getDouble("arena." + key + "-spawn.y"),
                    config.getDouble("arena." + key + "-spawn.z"),
                    (float) config.getDouble("arena." + key + "-spawn.yaw"),
                    (float) config.getDouble("arena." + key + "-spawn.pitch")
            ));

            targetBlocks.put(team, new Location(
                    world,
                    config.getInt("arena." + key + "-target.x"),
                    config.getInt("arena." + key + "-target.y"),
                    config.getInt("arena." + key + "-target.z")
            ));
        }
    }

    public void setSpawn(Team team, Location location) {
        String key = team.name().toLowerCase();
        String path = "arena." + key + "-spawn";
        plugin.getConfig().set(path + ".x", location.getX());
        plugin.getConfig().set(path + ".y", location.getY());
        plugin.getConfig().set(path + ".z", location.getZ());
        plugin.getConfig().set(path + ".yaw", location.getYaw());
        plugin.getConfig().set(path + ".pitch", location.getPitch());
        plugin.saveConfig();
        spawns.put(team, location);
    }

    public void setTargetBlock(Team team, Location location) {
        String key = team.name().toLowerCase();
        String path = "arena." + key + "-target";
        plugin.getConfig().set(path + ".x", location.getBlockX());
        plugin.getConfig().set(path + ".y", location.getBlockY());
        plugin.getConfig().set(path + ".z", location.getBlockZ());
        plugin.saveConfig();
        targetBlocks.put(team, location);
    }

    public void placeTargetBlocks() {
        for (Team team : Team.values()) {
            Location loc = targetBlocks.get(team);
            if (loc != null) {
                loc.getBlock().setType(team.getBannerMaterial());
            }
        }
    }

    public Team getTargetBlockTeam(Location location) {
        for (Team team : Team.values()) {
            Location target = targetBlocks.get(team);
            if (target != null
                    && location.getBlockX() == target.getBlockX()
                    && location.getBlockY() == target.getBlockY()
                    && location.getBlockZ() == target.getBlockZ()) {
                return team;
            }
        }
        return null;
    }

    public Location getSpawn(Team team) {
        Location loc = spawns.get(team);
        return loc != null ? loc.clone() : null;
    }

    public void setStable(Team team, Location location) {
        String key = team.name().toLowerCase();
        String path = "arena." + key + "-stable";
        plugin.getConfig().set(path + ".x", location.getX());
        plugin.getConfig().set(path + ".y", location.getY());
        plugin.getConfig().set(path + ".z", location.getZ());
        plugin.saveConfig();
        stables.put(team, location);
    }

    public Location getStable(Team team) {
        return stables.get(team);
    }
}
