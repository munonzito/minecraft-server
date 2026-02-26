package com.castledefense.manager;

import com.castledefense.CastleDefensePlugin;
import com.castledefense.model.Team;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.Wall;
import org.bukkit.block.data.type.Wall.Height;

public class CastleBuilder {

    private final CastleDefensePlugin plugin;
    private final ArenaManager arenaManager;

    private static final int WALL_LENGTH = 30;
    private static final int WALL_HEIGHT = 8;
    private static final int TOWER_SIZE = 5;
    private static final int TOWER_HEIGHT = 12;
    private static final int GATE_WIDTH = 5;
    private static final int GATE_HEIGHT = 5;

    public CastleBuilder(CastleDefensePlugin plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    public void buildCastle(Location center) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        clearArea(world, cx, cy, cz);
        buildFloor(world, cx, cy, cz);
        buildWalls(world, cx, cy, cz);
        buildCornerTowers(world, cx, cy, cz);
        buildGate(world, cx, cy, cz);
        buildThroneRoom(world, cx, cy, cz);
        buildBattlements(world, cx, cy, cz);
        buildInterior(world, cx, cy, cz);
        buildDefenderStable(world, cx, cy, cz);
        buildAttackerStable(world, cx, cy, cz);
        buildTowerAccess(world, cx, cy, cz);
        setupSpawnsAndTarget(center);

        plugin.getLogger().info("Castle built at " + cx + ", " + cy + ", " + cz);
    }

    private void clearArea(World world, int cx, int cy, int cz) {
        int halfX = WALL_LENGTH / 2 + TOWER_SIZE + 25;
        int halfZ = WALL_LENGTH / 2 + TOWER_SIZE + 15;
        for (int x = cx - halfX; x <= cx + halfX; x++) {
            for (int z = cz - halfZ; z <= cz + halfZ; z++) {
                for (int y = cy; y <= cy + TOWER_HEIGHT + 5; y++) {
                    setBlock(world, x, y, z, Material.AIR);
                }
            }
        }
    }

    private void buildFloor(World world, int cx, int cy, int cz) {
        int half = WALL_LENGTH / 2;
        for (int x = cx - half; x <= cx + half; x++) {
            for (int z = cz - half; z <= cz + half; z++) {
                setBlock(world, x, cy - 1, z, Material.STONE_BRICKS);
                setBlock(world, x, cy, z, Material.SMOOTH_STONE);
            }
        }

        int outerX = half + 25;
        int outerZ = half + 15;
        for (int x = cx - outerX; x <= cx + outerX; x++) {
            for (int z = cz - outerZ; z <= cz + outerZ; z++) {
                if (x < cx - half || x > cx + half || z < cz - half || z > cz + half) {
                    setBlock(world, x, cy - 1, z, Material.GRASS_BLOCK);
                    setBlock(world, x, cy, z, Material.AIR);
                }
            }
        }
    }

    private void buildWalls(World world, int cx, int cy, int cz) {
        int half = WALL_LENGTH / 2;
        int gateHalf = GATE_WIDTH / 2;

        for (int y = cy + 1; y <= cy + WALL_HEIGHT; y++) {
            for (int x = cx - half; x <= cx + half; x++) {
                setBlock(world, x, y, cz - half, Material.STONE_BRICKS);
                setBlock(world, x, y, cz + half, Material.STONE_BRICKS);
            }
            for (int z = cz - half; z <= cz + half; z++) {
                setBlock(world, x(cx - half), y, z, Material.STONE_BRICKS);

                if (z < cz - gateHalf || z > cz + gateHalf || y > cy + GATE_HEIGHT) {
                    setBlock(world, cx + half, y, z, Material.STONE_BRICKS);
                }
            }
        }
    }

    private int x(int val) {
        return val;
    }

    private void buildCornerTowers(World world, int cx, int cy, int cz) {
        int half = WALL_LENGTH / 2;
        int[][] corners = {
                {cx - half - 1, cz - half - 1},
                {cx + half + 1, cz - half - 1},
                {cx - half - 1, cz + half + 1},
                {cx + half + 1, cz + half + 1}
        };

        for (int[] corner : corners) {
            buildTower(world, corner[0], cy, corner[1]);
        }
    }

    private void buildTower(World world, int tx, int cy, int tz) {
        int halfT = TOWER_SIZE / 2;

        for (int y = cy + 1; y <= cy + TOWER_HEIGHT; y++) {
            for (int x = tx - halfT; x <= tx + halfT; x++) {
                for (int z = tz - halfT; z <= tz + halfT; z++) {
                    boolean isEdge = x == tx - halfT || x == tx + halfT || z == tz - halfT || z == tz + halfT;
                    if (isEdge) {
                        setBlock(world, x, y, z, Material.STONE_BRICKS);
                    } else if (y == cy + 1) {
                        setBlock(world, x, y, z, Material.OAK_PLANKS);
                    } else {
                        setBlock(world, x, y, z, Material.AIR);
                    }
                }
            }
        }

        int topY = cy + TOWER_HEIGHT + 1;
        for (int x = tx - halfT; x <= tx + halfT; x++) {
            for (int z = tz - halfT; z <= tz + halfT; z++) {
                setBlock(world, x, topY, z, Material.STONE_BRICK_SLAB);
            }
        }

        for (int x = tx - halfT; x <= tx + halfT; x++) {
            for (int z = tz - halfT; z <= tz + halfT; z++) {
                boolean isEdge = x == tx - halfT || x == tx + halfT || z == tz - halfT || z == tz + halfT;
                if (isEdge) {
                    if ((x + z) % 2 == 0) {
                        setBlock(world, x, topY + 1, z, Material.STONE_BRICK_WALL);
                    }
                }
            }
        }

        setBlock(world, tx, topY + 1, tz, Material.TORCH);

        for (int y = cy + 1; y <= cy + TOWER_HEIGHT; y++) {
            setBlock(world, tx, y, tz - halfT, Material.STONE_BRICK_STAIRS);
        }
    }

    private void buildGate(World world, int cx, int cy, int cz) {
        int half = WALL_LENGTH / 2;
        int gateHalf = GATE_WIDTH / 2;
        int gateX = cx + half;

        for (int z = cz - gateHalf; z <= cz + gateHalf; z++) {
            for (int y = cy + 1; y <= cy + GATE_HEIGHT; y++) {
                setBlock(world, gateX, y, z, Material.AIR);
            }
        }

        for (int z = cz - gateHalf; z <= cz + gateHalf; z++) {
            setBlock(world, gateX, cy + GATE_HEIGHT + 1, z, Material.DARK_OAK_SLAB);
        }

        setBlock(world, gateX, cy + 1, cz - gateHalf - 1, Material.DEEPSLATE_BRICK_WALL);
        setBlock(world, gateX, cy + 2, cz - gateHalf - 1, Material.DEEPSLATE_BRICK_WALL);
        setBlock(world, gateX, cy + 3, cz - gateHalf - 1, Material.TORCH);

        setBlock(world, gateX, cy + 1, cz + gateHalf + 1, Material.DEEPSLATE_BRICK_WALL);
        setBlock(world, gateX, cy + 2, cz + gateHalf + 1, Material.DEEPSLATE_BRICK_WALL);
        setBlock(world, gateX, cy + 3, cz + gateHalf + 1, Material.TORCH);

        for (int y = cy + 1; y <= cy + GATE_HEIGHT; y++) {
            if (y <= cy + 3) {
                setBlock(world, gateX, y, cz - gateHalf, Material.DARK_OAK_FENCE);
                setBlock(world, gateX, y, cz + gateHalf, Material.DARK_OAK_FENCE);
            }
        }

        int pathLength = 18;
        for (int dx = 1; dx <= pathLength; dx++) {
            for (int z = cz - gateHalf; z <= cz + gateHalf; z++) {
                setBlock(world, gateX + dx, cy, z, Material.COBBLESTONE);
            }
        }
    }

    private void buildThroneRoom(World world, int cx, int cy, int cz) {
        int half = WALL_LENGTH / 2;
        int roomX = cx - half + 5;
        int roomZ = cz;

        for (int x = roomX - 3; x <= roomX + 3; x++) {
            for (int z = roomZ - 4; z <= roomZ + 4; z++) {
                setBlock(world, x, cy, z, Material.POLISHED_DEEPSLATE);
            }
        }

        for (int y = cy + 1; y <= cy + 5; y++) {
            for (int x = roomX - 4; x <= roomX + 4; x++) {
                for (int z = roomZ - 5; z <= roomZ + 5; z++) {
                    boolean isEdge = x == roomX - 4 || x == roomX + 4 || z == roomZ - 5 || z == roomZ + 5;
                    if (isEdge) {
                        setBlock(world, x, y, z, Material.DEEPSLATE_BRICKS);
                    }
                }
            }
        }

        for (int x = roomX - 4; x <= roomX + 4; x++) {
            for (int z = roomZ - 5; z <= roomZ + 5; z++) {
                setBlock(world, x, cy + 6, z, Material.DEEPSLATE_BRICK_SLAB);
            }
        }

        for (int z = roomZ - 1; z <= roomZ + 1; z++) {
            for (int y = cy + 1; y <= cy + 4; y++) {
                setBlock(world, roomX + 4, y, z, Material.AIR);
            }
        }

        setBlock(world, roomX - 2, cy + 1, roomZ, Material.POLISHED_DEEPSLATE);
        setBlock(world, roomX - 2, cy + 2, roomZ, Material.POLISHED_DEEPSLATE);

        setBlock(world, roomX - 2, cy + 3, roomZ, Material.ORANGE_BANNER);

        setBlock(world, roomX - 3, cy + 1, roomZ - 2, Material.TORCH);
        setBlock(world, roomX - 3, cy + 1, roomZ + 2, Material.TORCH);
        setBlock(world, roomX + 3, cy + 1, roomZ - 4, Material.TORCH);
        setBlock(world, roomX + 3, cy + 1, roomZ + 4, Material.TORCH);

        setBlock(world, roomX, cy + 1, roomZ - 4, Material.DARK_OAK_FENCE);
        setBlock(world, roomX, cy + 1, roomZ + 4, Material.DARK_OAK_FENCE);

        arenaManager.setTargetBlock(new Location(world, roomX - 2, cy + 3, roomZ));
    }

    private void buildBattlements(World world, int cx, int cy, int cz) {
        int half = WALL_LENGTH / 2;
        int topY = cy + WALL_HEIGHT + 1;

        for (int x = cx - half; x <= cx + half; x++) {
            if ((x + cz) % 2 == 0) {
                setBlock(world, x, topY, cz - half, Material.STONE_BRICK_WALL);
                setBlock(world, x, topY, cz + half, Material.STONE_BRICK_WALL);
            } else {
                setBlock(world, x, topY, cz - half, Material.STONE_BRICK_SLAB);
                setBlock(world, x, topY, cz + half, Material.STONE_BRICK_SLAB);
            }
        }
        for (int z = cz - half; z <= cz + half; z++) {
            if ((cx + z) % 2 == 0) {
                setBlock(world, cx - half, topY, z, Material.STONE_BRICK_WALL);
                setBlock(world, cx + half, topY, z, Material.STONE_BRICK_WALL);
            } else {
                setBlock(world, cx - half, topY, z, Material.STONE_BRICK_SLAB);
                setBlock(world, cx + half, topY, z, Material.STONE_BRICK_SLAB);
            }
        }

        for (int x = cx - half + 1; x <= cx + half - 1; x++) {
            setBlock(world, x, cy + WALL_HEIGHT, cz - half + 1, Material.OAK_PLANKS);
            setBlock(world, x, cy + WALL_HEIGHT, cz + half - 1, Material.OAK_PLANKS);
        }
        for (int z = cz - half + 1; z <= cz + half - 1; z++) {
            setBlock(world, cx - half + 1, cy + WALL_HEIGHT, z, Material.OAK_PLANKS);
            setBlock(world, cx + half - 1, cy + WALL_HEIGHT, z, Material.OAK_PLANKS);
        }
    }

    private void buildInterior(World world, int cx, int cy, int cz) {
        int half = WALL_LENGTH / 2;

        for (int z = cz - 2; z <= cz + 2; z++) {
            setBlock(world, cx - half + 1, cy + 1, z, Material.OAK_STAIRS);
            setBlock(world, cx - half + 1, cy + 2, z, Material.AIR);
        }

        setBlock(world, cx + 3, cy + 1, cz + 5, Material.CRAFTING_TABLE);
        setBlock(world, cx + 4, cy + 1, cz + 5, Material.CHEST);
        setBlock(world, cx + 5, cy + 1, cz + 5, Material.FURNACE);

        setBlock(world, cx, cy + 1, cz - 8, Material.HAY_BLOCK);
        setBlock(world, cx + 1, cy + 1, cz - 8, Material.HAY_BLOCK);
        setBlock(world, cx, cy + 2, cz - 8, Material.HAY_BLOCK);

        int wellX = cx + 5;
        int wellZ = cz - 5;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    setBlock(world, wellX, cy, wellZ, Material.WATER);
                } else {
                    setBlock(world, wellX + dx, cy + 1, wellZ + dz, Material.COBBLESTONE_WALL);
                }
            }
        }

        for (int i = 0; i < 6; i++) {
            int torchX = cx - half + 2 + (i * 5);
            if (torchX < cx + half - 1) {
                setBlock(world, torchX, cy + 4, cz - half + 1, Material.TORCH);
                setBlock(world, torchX, cy + 4, cz + half - 1, Material.TORCH);
            }
        }
        for (int i = 0; i < 6; i++) {
            int torchZ = cz - half + 2 + (i * 5);
            if (torchZ < cz + half - 1) {
                setBlock(world, cx - half + 1, cy + 4, torchZ, Material.TORCH);
                setBlock(world, cx + half - 1, cy + 4, torchZ, Material.TORCH);
            }
        }
    }

    private void buildStable(World world, int sx, int cy, int sz) {
        int halfX = 5;
        int halfZ = 4;

        for (int x = sx - halfX; x <= sx + halfX; x++) {
            for (int z = sz - halfZ; z <= sz + halfZ; z++) {
                setBlock(world, x, cy - 1, z, Material.SPRUCE_PLANKS);
                setBlock(world, x, cy, z, Material.SPRUCE_PLANKS);
            }
        }

        for (int y = cy + 1; y <= cy + 4; y++) {
            for (int x = sx - halfX; x <= sx + halfX; x++) {
                setBlock(world, x, y, sz - halfZ, Material.SPRUCE_LOG);
                setBlock(world, x, y, sz + halfZ, Material.SPRUCE_LOG);
            }
            for (int z = sz - halfZ; z <= sz + halfZ; z++) {
                setBlock(world, sx - halfX, y, z, Material.SPRUCE_PLANKS);
            }
        }

        for (int y = cy + 1; y <= cy + 4; y++) {
            for (int x = sx - halfX + 1; x <= sx + halfX; x++) {
                for (int z = sz - halfZ + 1; z <= sz + halfZ - 1; z++) {
                    setBlock(world, x, y, z, Material.AIR);
                }
            }
        }

        for (int x = sx - halfX; x <= sx + halfX; x++) {
            for (int z = sz - halfZ; z <= sz + halfZ; z++) {
                setBlock(world, x, cy + 5, z, Material.SPRUCE_SLAB);
            }
        }

        for (int i = 0; i < 3; i++) {
            int stallZ = sz - 3 + (i * 3);
            setBlock(world, sx - 3, cy + 1, stallZ, Material.OAK_FENCE);
            setBlock(world, sx + 1, cy + 1, stallZ, Material.OAK_FENCE);
        }

        setBlock(world, sx - 4, cy, sz, Material.HAY_BLOCK);
        setBlock(world, sx - 4, cy + 1, sz, Material.HAY_BLOCK);
        setBlock(world, sx - 4, cy, sz - 2, Material.HAY_BLOCK);
        setBlock(world, sx + 4, cy, sz, Material.HAY_BLOCK);
        setBlock(world, sx + 4, cy + 1, sz, Material.HAY_BLOCK);

        setBlock(world, sx - halfX + 1, cy + 3, sz - halfZ + 1, Material.TORCH);
        setBlock(world, sx - halfX + 1, cy + 3, sz + halfZ - 1, Material.TORCH);
        setBlock(world, sx + halfX - 1, cy + 3, sz - halfZ + 1, Material.TORCH);
        setBlock(world, sx + halfX - 1, cy + 3, sz + halfZ - 1, Material.TORCH);

        setBlock(world, sx, cy, sz - 2, Material.WATER);
        setBlock(world, sx - 1, cy + 1, sz - 2, Material.COBBLESTONE_WALL);
        setBlock(world, sx + 1, cy + 1, sz - 2, Material.COBBLESTONE_WALL);
        setBlock(world, sx, cy + 1, sz - 3, Material.COBBLESTONE_WALL);
    }

    private void buildDefenderStable(World world, int cx, int cy, int cz) {
        int half = WALL_LENGTH / 2;
        int sx = cx + 5;
        int sz = cz + half - 10;
        buildStable(world, sx, cy, sz);
    }

    private void buildAttackerStable(World world, int cx, int cy, int cz) {
        int half = WALL_LENGTH / 2;
        int sx = cx + half + 18;
        int sz = cz + 6;
        buildStable(world, sx, cy, sz);
    }

    private void buildTowerAccess(World world, int cx, int cy, int cz) {
        int half = WALL_LENGTH / 2;
        int halfT = TOWER_SIZE / 2;

        // Tower positions: {towerX, towerZ, ladderX, ladderZ, wallX (solid behind ladder), doorX, doorZ, towerWallX, towerWallZ}
        // For each tower, place ladder against the outer wall (facing inward) and carve a doorway through the castle wall + tower wall

        // Bottom-left tower (cx - half - 1, cz - half - 1) -> ladder on +X wall, door on +X side
        buildSingleTowerAccess(world, cx, cy, cz,
                cx - half - 1, cz - half - 1,
                cx - half - 1 + halfT - 1, cz - half - 1,  // ladder position (inside, against +X wall)
                cx - half - 1 + halfT, cz - half - 1,       // wall behind ladder
                cx - half, cz - half - 1,                    // castle wall doorway
                cx - half - 1 + halfT, cz - half - 1);      // tower wall doorway

        // Bottom-right tower (cx + half + 1, cz - half - 1) -> ladder on -X wall, door on -X side
        buildSingleTowerAccess(world, cx, cy, cz,
                cx + half + 1, cz - half - 1,
                cx + half + 1 - halfT + 1, cz - half - 1,
                cx + half + 1 - halfT, cz - half - 1,
                cx + half, cz - half - 1,
                cx + half + 1 - halfT, cz - half - 1);

        // Top-left tower (cx - half - 1, cz + half + 1) -> ladder on +X wall, door on +X side
        buildSingleTowerAccess(world, cx, cy, cz,
                cx - half - 1, cz + half + 1,
                cx - half - 1 + halfT - 1, cz + half + 1,
                cx - half - 1 + halfT, cz + half + 1,
                cx - half, cz + half + 1,
                cx - half - 1 + halfT, cz + half + 1);

        // Top-right tower (cx + half + 1, cz + half + 1) -> ladder on -X wall, door on -X side
        buildSingleTowerAccess(world, cx, cy, cz,
                cx + half + 1, cz + half + 1,
                cx + half + 1 - halfT + 1, cz + half + 1,
                cx + half + 1 - halfT, cz + half + 1,
                cx + half, cz + half + 1,
                cx + half + 1 - halfT, cz + half + 1);
    }

    private void buildSingleTowerAccess(World world, int cx, int cy, int cz,
                                         int tx, int tz,
                                         int ladderX, int ladderZ,
                                         int wallBehindX, int wallBehindZ,
                                         int doorX, int doorZ,
                                         int towerDoorX, int towerDoorZ) {
        // Ensure wall behind ladder is solid
        for (int y = cy + 1; y <= cy + TOWER_HEIGHT; y++) {
            setBlock(world, wallBehindX, y, wallBehindZ, Material.STONE_BRICKS);
            setBlock(world, ladderX, y, ladderZ, Material.LADDER);
        }

        // Carve doorway through castle wall
        for (int y = cy + 1; y <= cy + 3; y++) {
            setBlock(world, doorX, y, doorZ, Material.AIR);
        }

        // Carve doorway through tower wall
        for (int y = cy + 1; y <= cy + 3; y++) {
            setBlock(world, towerDoorX, y, towerDoorZ, Material.AIR);
        }
    }

    private void setupSpawnsAndTarget(Location center) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        int half = WALL_LENGTH / 2;

        Location attackerSpawn = new Location(world, cx + half + 20, cy + 1, cz, 270f, 0f);
        arenaManager.setSpawn(Team.ATTACKERS, attackerSpawn);

        Location defenderSpawn = new Location(world, cx, cy + 1, cz, 270f, 0f);
        arenaManager.setSpawn(Team.DEFENDERS, defenderSpawn);

        Location defenderStable = new Location(world, cx + 5, cy + 1, cz + half - 10);
        arenaManager.setStable(Team.DEFENDERS, defenderStable);

        Location attackerStable = new Location(world, cx + half + 18, cy + 1, cz + 6);
        arenaManager.setStable(Team.ATTACKERS, attackerStable);
    }

    private void setBlock(World world, int x, int y, int z, Material material) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(material, false);
    }
}
