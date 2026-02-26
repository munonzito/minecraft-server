package com.castledefense.manager;

import com.castledefense.CastleDefensePlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlueprintManager {

    private final CastleDefensePlugin plugin;

    public BlueprintManager(CastleDefensePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean placeBlueprint(String name, Location origin, String facing) {
        String path = "blueprints/" + name + ".yml";
        InputStream stream = plugin.getResource(path);
        if (stream == null) {
            plugin.getLogger().warning("Blueprint not found: " + path);
            return false;
        }

        YamlConfiguration blueprint = YamlConfiguration.loadConfiguration(new InputStreamReader(stream));
        World world = origin.getWorld();
        int ox = origin.getBlockX();
        int oy = origin.getBlockY();
        int oz = origin.getBlockZ();

        List<Map<?, ?>> allBlocks = new ArrayList<>();
        if (blueprint.contains("base")) {
            allBlocks.addAll(blueprint.getMapList("base"));
        }
        if (blueprint.contains("top")) {
            allBlocks.addAll(blueprint.getMapList("top"));
        }

        // Separate TNT and water blocks to place them last
        List<Map<?, ?>> solidBlocks = new ArrayList<>();
        List<Map<?, ?>> waterBlocks = new ArrayList<>();
        List<Map<?, ?>> tntBlocks = new ArrayList<>();
        List<Map<?, ?>> redstoneBlocks = new ArrayList<>();

        for (Map<?, ?> entry : allBlocks) {
            String block = (String) entry.get("block");
            if (block.equals("water")) {
                waterBlocks.add(entry);
            } else if (block.equals("tnt")) {
                tntBlocks.add(entry);
            } else if (block.contains("redstone") || block.contains("repeater") || block.contains("button")) {
                redstoneBlocks.add(entry);
            } else {
                solidBlocks.add(entry);
            }
        }

        // Place in order: solid -> water -> tnt -> redstone
        for (Map<?, ?> entry : solidBlocks) {
            placeBlock(world, ox, oy, oz, entry, facing);
        }
        for (Map<?, ?> entry : waterBlocks) {
            placeBlock(world, ox, oy, oz, entry, facing);
        }
        for (Map<?, ?> entry : tntBlocks) {
            placeBlock(world, ox, oy, oz, entry, facing);
        }
        for (Map<?, ?> entry : redstoneBlocks) {
            placeBlock(world, ox, oy, oz, entry, facing);
        }

        return true;
    }

    private void placeBlock(World world, int ox, int oy, int oz, Map<?, ?> entry, String facing) {
        int dx = ((Number) entry.get("dx")).intValue();
        int dy = ((Number) entry.get("dy")).intValue();
        int dz = ((Number) entry.get("dz")).intValue();
        String blockStr = (String) entry.get("block");

        // Rotate offsets based on facing direction
        int rx, rz;
        String rotatedBlock = blockStr;
        switch (facing.toLowerCase()) {
            case "east" -> {
                rx = -dx; rz = -dz;
                rotatedBlock = rotateBlock(blockStr, "east");
            }
            case "north" -> {
                rx = dz; rz = dx;
                rotatedBlock = rotateBlock(blockStr, "north");
            }
            case "south" -> {
                rx = -dz; rz = -dx;
                rotatedBlock = rotateBlock(blockStr, "south");
            }
            default -> { // west (default, no rotation)
                rx = dx; rz = dz;
            }
        }

        int finalX = ox + rx;
        int finalY = oy + dy;
        int finalZ = oz + rz;

        try {
            Block block = world.getBlockAt(finalX, finalY, finalZ);
            BlockData data = plugin.getServer().createBlockData("minecraft:" + rotatedBlock);
            block.setBlockData(data, false);
        } catch (Exception e) {
            try {
                Block block = world.getBlockAt(finalX, finalY, finalZ);
                Material mat = Material.valueOf(rotatedBlock.toUpperCase().split("\\[")[0]);
                block.setType(mat, false);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to place block: " + rotatedBlock + " at " + finalX + "," + finalY + "," + finalZ);
            }
        }
    }

    private String rotateBlock(String block, String facing) {
        if (!block.contains("facing=")) return block;

        return switch (facing) {
            case "east" -> block.replace("facing=east", "facing=west")
                               .replace("facing=west", "facing=east");
            case "north" -> block.replace("facing=east", "facing=south")
                                .replace("facing=west", "facing=north");
            case "south" -> block.replace("facing=east", "facing=north")
                                .replace("facing=west", "facing=south");
            default -> block;
        };
    }
}
