package com.castledefense.manager;

import com.castledefense.CastleDefensePlugin;
import com.castledefense.model.Team;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class CastleBuilder {

    private final CastleDefensePlugin plugin;
    private final ArenaManager arenaManager;
    private BlueprintManager blueprintManager;

    private static final int WALL_LENGTH = 30;
    private static final int WALL_HEIGHT = 8;
    private static final int TOWER_SIZE = 5;
    private static final int TOWER_HEIGHT = 12;
    private static final int GATE_WIDTH = 5;
    private static final int GATE_HEIGHT = 5;
    private static final int CASTLE_DISTANCE = 80;

    public CastleBuilder(CastleDefensePlugin plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    public void setBlueprintManager(BlueprintManager blueprintManager) {
        this.blueprintManager = blueprintManager;
    }

    public void buildArena(Location center) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        int halfDist = CASTLE_DISTANCE / 2;

        int redCenterX = cx - halfDist;
        int blueCenterX = cx + halfDist;

        clearFullArea(world, cx, cy, cz);
        buildBattlefield(world, cx, cy, cz);

        buildCastle(world, redCenterX, cy, cz, Team.RED, 1);
        buildCastle(world, blueCenterX, cy, cz, Team.BLUE, -1);

        setupSpawnsAndTargets(world, redCenterX, blueCenterX, cy, cz);

        plugin.getLogger().info("Two-castle arena built at " + cx + ", " + cy + ", " + cz);
    }

    private void clearFullArea(World world, int cx, int cy, int cz) {
        int halfX = CASTLE_DISTANCE / 2 + WALL_LENGTH / 2 + TOWER_SIZE + 25;
        int halfZ = WALL_LENGTH / 2 + TOWER_SIZE + 15;
        for (int x = cx - halfX; x <= cx + halfX; x++) {
            for (int z = cz - halfZ; z <= cz + halfZ; z++) {
                for (int y = cy; y <= cy + TOWER_HEIGHT + 5; y++) {
                    setBlock(world, x, y, z, Material.AIR);
                }
            }
        }
    }

    private void buildBattlefield(World world, int cx, int cy, int cz) {
        int halfX = CASTLE_DISTANCE / 2 + WALL_LENGTH / 2 + TOWER_SIZE + 25;
        int halfZ = WALL_LENGTH / 2 + TOWER_SIZE + 15;
        for (int x = cx - halfX; x <= cx + halfX; x++) {
            for (int z = cz - halfZ; z <= cz + halfZ; z++) {
                setBlock(world, x, cy - 1, z, Material.GRASS_BLOCK);
            }
        }

        int pathHalf = GATE_WIDTH / 2;
        int halfDist = CASTLE_DISTANCE / 2;
        int half = WALL_LENGTH / 2;
        for (int x = cx - halfDist + half; x <= cx + halfDist - half; x++) {
            for (int z = cz - pathHalf; z <= cz + pathHalf; z++) {
                setBlock(world, x, cy, z, Material.COBBLESTONE);
                setBlock(world, x, cy - 1, z, Material.COBBLESTONE);
            }
        }
    }

    /**
     * Build one castle.
     * @param dir +1 means gate faces +X (red castle on the left, gate toward center),
     *            -1 means gate faces -X (blue castle on the right, gate toward center).
     */
    private void buildCastle(World world, int cx, int cy, int cz, Team team, int dir) {
        buildFloor(world, cx, cy, cz);
        buildWalls(world, cx, cy, cz, dir);
        buildCornerTowers(world, cx, cy, cz);
        buildGate(world, cx, cy, cz, dir);
        buildThroneRoom(world, cx, cy, cz, team, dir);
        buildBattlements(world, cx, cy, cz);
        buildInterior(world, cx, cy, cz, dir);
        buildStable(world, cx, cy, cz, team, dir);
        buildTowerCannons(world, cx, cy, cz);
        buildTowerWalkwayGates(world, cx, cy, cz);
        buildTowerAccess(world, cx, cy, cz);
    }

    private void buildFloor(World world, int cx, int cy, int cz) {
        int half = WALL_LENGTH / 2;
        for (int x = cx - half; x <= cx + half; x++) {
            for (int z = cz - half; z <= cz + half; z++) {
                setBlock(world, x, cy - 1, z, Material.STONE_BRICKS);
                setBlock(world, x, cy, z, Material.SMOOTH_STONE);
            }
        }
    }

    private void buildWalls(World world, int cx, int cy, int cz, int dir) {
        int half = WALL_LENGTH / 2;
        int gateHalf = GATE_WIDTH / 2;

        for (int y = cy + 1; y <= cy + WALL_HEIGHT; y++) {
            for (int x = cx - half; x <= cx + half; x++) {
                setBlock(world, x, y, cz - half, Material.STONE_BRICKS);
                setBlock(world, x, y, cz + half, Material.STONE_BRICKS);
            }
            for (int z = cz - half; z <= cz + half; z++) {
                // Back wall (opposite to gate)
                setBlock(world, cx - half * dir, y, z, Material.STONE_BRICKS);

                // Gate wall (with opening)
                if (z < cz - gateHalf || z > cz + gateHalf || y > cy + GATE_HEIGHT) {
                    setBlock(world, cx + half * dir, y, z, Material.STONE_BRICKS);
                }
            }
        }
    }

    private void buildCornerTowers(World world, int cx, int cy, int cz) {
        int half = WALL_LENGTH / 2;
        int halfT = TOWER_SIZE / 2;
        int[][] corners = {
                {cx - half + halfT, cz - half + halfT},
                {cx + half - halfT, cz - half + halfT},
                {cx - half + halfT, cz + half - halfT},
                {cx + half - halfT, cz + half - halfT}
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
    }

    private void buildGate(World world, int cx, int cy, int cz, int dir) {
        int half = WALL_LENGTH / 2;
        int gateHalf = GATE_WIDTH / 2;
        int gateX = cx + half * dir;

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

        int pathLength = 10;
        for (int dx = 1; dx <= pathLength; dx++) {
            for (int z = cz - gateHalf; z <= cz + gateHalf; z++) {
                setBlock(world, gateX + dx * dir, cy, z, Material.COBBLESTONE);
            }
        }
    }

    private void buildThroneRoom(World world, int cx, int cy, int cz, Team team, int dir) {
        int half = WALL_LENGTH / 2;
        int roomX = cx - (half - 5) * dir;
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

        // Entrance facing toward gate (dir side)
        for (int z = roomZ - 1; z <= roomZ + 1; z++) {
            for (int y = cy + 1; y <= cy + 4; y++) {
                setBlock(world, roomX + 4 * dir, y, z, Material.AIR);
            }
        }

        // Banner pedestal at the back of the throne room
        int bannerX = roomX - 2 * dir;
        setBlock(world, bannerX, cy + 1, roomZ, Material.POLISHED_DEEPSLATE);
        setBlock(world, bannerX, cy + 2, roomZ, Material.POLISHED_DEEPSLATE);
        setBlock(world, bannerX, cy + 3, roomZ, team.getBannerMaterial());

        setBlock(world, roomX - 3 * dir, cy + 1, roomZ - 2, Material.TORCH);
        setBlock(world, roomX - 3 * dir, cy + 1, roomZ + 2, Material.TORCH);
        setBlock(world, roomX + 3 * dir, cy + 1, roomZ - 4, Material.TORCH);
        setBlock(world, roomX + 3 * dir, cy + 1, roomZ + 4, Material.TORCH);

        setBlock(world, roomX, cy + 1, roomZ - 4, Material.DARK_OAK_FENCE);
        setBlock(world, roomX, cy + 1, roomZ + 4, Material.DARK_OAK_FENCE);

        arenaManager.setTargetBlock(team, new Location(world, bannerX, cy + 3, roomZ));
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

    private void buildInterior(World world, int cx, int cy, int cz, int dir) {
        int half = WALL_LENGTH / 2;

        for (int z = cz - 2; z <= cz + 2; z++) {
            setBlock(world, cx - (half - 1) * dir, cy + 1, z, Material.OAK_STAIRS);
            setBlock(world, cx - (half - 1) * dir, cy + 2, z, Material.AIR);
        }

        int offsetX = dir * 3;
        setBlock(world, cx + offsetX, cy + 1, cz + 5, Material.CRAFTING_TABLE);
        setBlock(world, cx + offsetX + dir, cy + 1, cz + 5, Material.CHEST);
        setBlock(world, cx + offsetX + 2 * dir, cy + 1, cz + 5, Material.FURNACE);

        setBlock(world, cx, cy + 1, cz - 8, Material.HAY_BLOCK);
        setBlock(world, cx + dir, cy + 1, cz - 8, Material.HAY_BLOCK);
        setBlock(world, cx, cy + 2, cz - 8, Material.HAY_BLOCK);

        int wellX = cx + 5 * dir;
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

    private void buildStableStructure(World world, int sx, int cy, int sz) {
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

    private void buildStable(World world, int cx, int cy, int cz, Team team, int dir) {
        int half = WALL_LENGTH / 2;
        int sx = cx + 5 * dir;
        int sz = cz + half - 10;
        buildStableStructure(world, sx, cy, sz);
        arenaManager.setStable(team, new Location(world, sx, cy + 1, sz));
    }

    private void buildTowerAccess(World world, int cx, int cy, int cz) {
        int half = WALL_LENGTH / 2;
        int halfT = TOWER_SIZE / 2;
        int platformY = cy + TOWER_HEIGHT + 1;

        int[][] towers = {
                {cx - half + halfT, cz - half + halfT},
                {cx + half - halfT, cz - half + halfT},
                {cx - half + halfT, cz + half - halfT},
                {cx + half - halfT, cz + half - halfT}
        };

        for (int[] tower : towers) {
            int tx = tower[0];
            int tz = tower[1];

            int ladderX = tx;
            int ladderZ = tz - 1;
            int wallZ = tz - 2;

            for (int y = cy + 1; y <= platformY + 2; y++) {
                setBlock(world, ladderX, y, wallZ, Material.STONE_BRICKS);
                setBlock(world, ladderX, y, ladderZ, Material.LADDER);
            }
            setBlock(world, ladderX, platformY + 3, ladderZ, Material.AIR);

            int doorY = cy + 1;

            if (tz < cz) {
                for (int y = doorY; y <= doorY + 2; y++) {
                    setBlock(world, tx, y, tz + halfT, Material.AIR);
                }
            }
            if (tz > cz) {
                for (int y = doorY; y <= doorY + 2; y++) {
                    setBlock(world, tx, y, tz - halfT, Material.AIR);
                }
            }
            if (tx < cx) {
                for (int y = doorY; y <= doorY + 2; y++) {
                    setBlock(world, tx + halfT, y, tz, Material.AIR);
                }
            }
            if (tx > cx) {
                for (int y = doorY; y <= doorY + 2; y++) {
                    setBlock(world, tx - halfT, y, tz, Material.AIR);
                }
            }
        }
    }

    private void buildTowerWalkwayGates(World world, int cx, int cy, int cz) {
        int half = WALL_LENGTH / 2;
        int halfT = TOWER_SIZE / 2;
        int walkwayY = cy + WALL_HEIGHT;

        int[][] towers = {
                {cx - half + halfT, cz - half + halfT},
                {cx + half - halfT, cz - half + halfT},
                {cx - half + halfT, cz + half - halfT},
                {cx + half - halfT, cz + half - halfT}
        };

        for (int[] tower : towers) {
            int tx = tower[0];
            int tz = tower[1];

            if (tz < cz) {
                int walkwayZ = cz - half + 1;
                if (tx < cx) {
                    for (int y = walkwayY; y <= walkwayY + 2; y++) {
                        setBlock(world, tx + halfT, y, walkwayZ, Material.AIR);
                    }
                }
                if (tx > cx) {
                    for (int y = walkwayY; y <= walkwayY + 2; y++) {
                        setBlock(world, tx - halfT, y, walkwayZ, Material.AIR);
                    }
                }
            }

            if (tz > cz) {
                int walkwayZ = cz + half - 1;
                if (tx < cx) {
                    for (int y = walkwayY; y <= walkwayY + 2; y++) {
                        setBlock(world, tx + halfT, y, walkwayZ, Material.AIR);
                    }
                }
                if (tx > cx) {
                    for (int y = walkwayY; y <= walkwayY + 2; y++) {
                        setBlock(world, tx - halfT, y, walkwayZ, Material.AIR);
                    }
                }
            }

            if (tx < cx) {
                int walkwayX = cx - half + 1;
                if (tz < cz) {
                    for (int y = walkwayY; y <= walkwayY + 2; y++) {
                        setBlock(world, walkwayX, y, tz + halfT, Material.AIR);
                    }
                }
                if (tz > cz) {
                    for (int y = walkwayY; y <= walkwayY + 2; y++) {
                        setBlock(world, walkwayX, y, tz - halfT, Material.AIR);
                    }
                }
            }

            if (tx > cx) {
                int walkwayX = cx + half - 1;
                if (tz < cz) {
                    for (int y = walkwayY; y <= walkwayY + 2; y++) {
                        setBlock(world, walkwayX, y, tz + halfT, Material.AIR);
                    }
                }
                if (tz > cz) {
                    for (int y = walkwayY; y <= walkwayY + 2; y++) {
                        setBlock(world, walkwayX, y, tz - halfT, Material.AIR);
                    }
                }
            }

            int ladderX = tx;
            int ladderZ = tz - 1;
            for (int x = tx - halfT + 1; x <= tx + halfT - 1; x++) {
                for (int z = tz - halfT + 1; z <= tz + halfT - 1; z++) {
                    if (x == ladderX && z == ladderZ) {
                        setBlock(world, x, walkwayY, z, Material.LADDER);
                        setBlock(world, x, walkwayY + 1, z, Material.AIR);
                        setBlock(world, x, walkwayY + 2, z, Material.AIR);
                    } else {
                        setBlock(world, x, walkwayY, z, Material.OAK_PLANKS);
                        setBlock(world, x, walkwayY + 1, z, Material.AIR);
                        setBlock(world, x, walkwayY + 2, z, Material.AIR);
                    }
                }
            }
        }
    }

    private void buildTowerCannons(World world, int cx, int cy, int cz) {
        if (blueprintManager == null) {
            plugin.getLogger().warning("BlueprintManager not set, skipping tower cannons.");
            return;
        }

        int half = WALL_LENGTH / 2;
        int halfT = TOWER_SIZE / 2;
        int platformY = cy + TOWER_HEIGHT + 1;

        int[][] towers = {
                {cx - half + halfT, cz - half + halfT},
                {cx + half - halfT, cz - half + halfT},
                {cx - half + halfT, cz + half - halfT},
                {cx + half - halfT, cz + half - halfT}
        };

        String[] facings = {"north", "east", "south", "east"};

        for (int i = 0; i < towers.length; i++) {
            int tx = towers[i][0];
            int tz = towers[i][1];
            String facing = facings[i];

            int platformExtend = 4;

            for (int x = tx - halfT; x <= tx + halfT; x++) {
                for (int z = tz - halfT; z <= tz + halfT; z++) {
                    setBlock(world, x, platformY, z, Material.OBSIDIAN);
                }
            }

            int extMinX = tx - halfT;
            int extMaxX = tx + halfT;
            int extMinZ = tz - halfT;
            int extMaxZ = tz + halfT;

            switch (facing) {
                case "east" -> extMaxX += platformExtend;
                case "west" -> extMinX -= platformExtend;
                case "north" -> extMinZ -= platformExtend;
                case "south" -> extMaxZ += platformExtend;
            }

            for (int x = extMinX; x <= extMaxX; x++) {
                for (int z = extMinZ; z <= extMaxZ; z++) {
                    setBlock(world, x, platformY, z, Material.OBSIDIAN);
                    for (int y = platformY + 1; y <= platformY + 3; y++) {
                        setBlock(world, x, y, z, Material.AIR);
                    }
                }
            }

            for (int x = extMinX; x <= extMaxX; x++) {
                setBlock(world, x, platformY + 1, extMinZ, Material.STONE_BRICK_WALL);
                setBlock(world, x, platformY + 1, extMaxZ, Material.STONE_BRICK_WALL);
            }
            for (int z = extMinZ; z <= extMaxZ; z++) {
                setBlock(world, extMinX, platformY + 1, z, Material.STONE_BRICK_WALL);
                setBlock(world, extMaxX, platformY + 1, z, Material.STONE_BRICK_WALL);
            }

            switch (facing) {
                case "east" -> {
                    for (int z = tz - 1; z <= tz + 1; z++) {
                        setBlock(world, extMaxX, platformY + 1, z, Material.AIR);
                    }
                }
                case "west" -> {
                    for (int z = tz - 1; z <= tz + 1; z++) {
                        setBlock(world, extMinX, platformY + 1, z, Material.AIR);
                    }
                }
                case "north" -> {
                    for (int x = tx - 1; x <= tx + 1; x++) {
                        setBlock(world, x, platformY + 1, extMinZ, Material.AIR);
                    }
                }
                case "south" -> {
                    for (int x = tx - 1; x <= tx + 1; x++) {
                        setBlock(world, x, platformY + 1, extMaxZ, Material.AIR);
                    }
                }
            }

            for (int x = tx - halfT + 1; x <= tx + halfT - 1; x++) {
                for (int z = tz - halfT + 1; z <= tz + halfT - 1; z++) {
                    setBlock(world, x, platformY + 1, z, Material.AIR);
                }
            }

            Location cannonOrigin = new Location(world, tx, platformY + 1, tz);
            blueprintManager.placeBlueprint("tnt_cannon", cannonOrigin, facing);
        }
    }

    private void setupSpawnsAndTargets(World world, int redCX, int blueCX, int cy, int cz) {
        int half = WALL_LENGTH / 2;

        // Red team spawns inside their castle courtyard
        Location redSpawn = new Location(world, redCX, cy + 1, cz, 90f, 0f);
        arenaManager.setSpawn(Team.RED, redSpawn);

        // Blue team spawns inside their castle courtyard
        Location blueSpawn = new Location(world, blueCX, cy + 1, cz, 270f, 0f);
        arenaManager.setSpawn(Team.BLUE, blueSpawn);
    }

    private void setBlock(World world, int x, int y, int z, Material material) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(material, false);
    }
}
