package com.castlesiege.manager;

import com.castlesiege.CastleSiegePlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Villager;

import java.util.ArrayList;
import java.util.List;

public class CastleManager {

    private final CastleSiegePlugin plugin;

    private static final int WALL_LENGTH = 30;
    private static final int WALL_HEIGHT = 8;
    private static final int TOWER_SIZE = 5;
    private static final int TOWER_HEIGHT = 12;
    private static final int GATE_WIDTH = 5;
    private static final int GATE_HEIGHT = 5;

    private Location castleCenter;
    private Location villagerLocation;
    private Location spawnLocation;
    private Location mobSpawnLocation;
    private Location shopVillagerLocation;
    private Villager aldeano;
    private Villager shopKeeper;
    private final List<Entity> allyEntities = new ArrayList<>();

    public CastleManager(CastleSiegePlugin plugin) {
        this.plugin = plugin;
    }

    public void buildCastle(Location center) {
        this.castleCenter = center.clone();
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
        buildTowerAccess(world, cx, cy, cz);

        int half = WALL_LENGTH / 2;
        int mobDist = plugin.getConfig().getInt("game.mob-spawn-distance", 30);

        spawnLocation = new Location(world, cx, cy + 1, cz, 270f, 0f);
        mobSpawnLocation = new Location(world, cx + half + mobDist, cy + 1, cz);

        int roomX = cx - half + 5;
        // Aldeano is on the second floor of the throne room
        int secondFloorY = cy + 5;
        villagerLocation = new Location(world, roomX, secondFloorY + 1, cz);

        shopVillagerLocation = new Location(world, cx + 3, cy + 1, cz + 5);

        // Ensure villager spawn areas are clear of blocks
        clearSpawnArea(world, villagerLocation);
        clearSpawnArea(world, shopVillagerLocation);

        plugin.getLogger().info("Siege castle built at " + cx + ", " + cy + ", " + cz);
    }

    public void spawnVillagers() {
        if (villagerLocation == null) return;

        double maxHealth = plugin.getConfig().getDouble("game.villager-max-health", 40);

        aldeano = villagerLocation.getWorld().spawn(villagerLocation, Villager.class, v -> {
            v.setCustomName("§6§lAldeano");
            v.setCustomNameVisible(true);
            v.setAI(false);
            v.setInvulnerable(false);
            v.setGlowing(true);
            v.setProfession(Villager.Profession.NITWIT);
            v.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
            v.setHealth(maxHealth);
            v.setRemoveWhenFarAway(false);
        });

        shopKeeper = shopVillagerLocation.getWorld().spawn(shopVillagerLocation, Villager.class, v -> {
            v.setCustomName("§e§lShop");
            v.setCustomNameVisible(true);
            v.setAI(false);
            v.setInvulnerable(true);
            v.setProfession(Villager.Profession.WEAPONSMITH);
            v.setRemoveWhenFarAway(false);
        });
    }

    public void cleanup() {
        if (aldeano != null && !aldeano.isDead()) {
            aldeano.remove();
        }
        if (shopKeeper != null && !shopKeeper.isDead()) {
            shopKeeper.remove();
        }
        aldeano = null;
        shopKeeper = null;

        for (Entity ally : allyEntities) {
            if (ally != null && !ally.isDead()) {
                ally.remove();
            }
        }
        allyEntities.clear();

        demolishCastle();
    }

    private void demolishCastle() {
        if (castleCenter == null) return;
        World world = castleCenter.getWorld();
        if (world == null) return;

        int cx = castleCenter.getBlockX();
        int cy = castleCenter.getBlockY();
        int cz = castleCenter.getBlockZ();

        int halfX = WALL_LENGTH / 2 + TOWER_SIZE + 35;
        int halfZ = WALL_LENGTH / 2 + TOWER_SIZE + 15;
        for (int x = cx - halfX; x <= cx + halfX; x++) {
            for (int z = cz - halfZ; z <= cz + halfZ; z++) {
                for (int y = cy - FOUNDATION_DEPTH; y <= cy + TOWER_HEIGHT + 6; y++) {
                    setBlock(world, x, y, z, Material.AIR);
                }
            }
        }
        castleCenter = null;
    }

    public IronGolem spawnAllyGolem(Location location) {
        if (location == null || location.getWorld() == null) return null;
        IronGolem golem = location.getWorld().spawn(location, IronGolem.class, g -> {
            g.setCustomName("§a§lDefender");
            g.setCustomNameVisible(true);
            g.setPlayerCreated(true);
            g.setRemoveWhenFarAway(false);
        });
        allyEntities.add(golem);
        return golem;
    }

    public boolean isAlly(Entity entity) {
        return allyEntities.contains(entity);
    }

    public boolean isAldeano(org.bukkit.entity.Entity entity) {
        return aldeano != null && entity.equals(aldeano);
    }

    public boolean isShopKeeper(org.bukkit.entity.Entity entity) {
        return shopKeeper != null && entity.equals(shopKeeper);
    }

    public Villager getAldeano() {
        return aldeano;
    }

    public Location getSpawnLocation() {
        return spawnLocation != null ? spawnLocation.clone() : null;
    }

    public Location getMobSpawnLocation() {
        return mobSpawnLocation != null ? mobSpawnLocation.clone() : null;
    }

    public Location getCastleCenter() {
        return castleCenter != null ? castleCenter.clone() : null;
    }

    // --- Castle building methods (adapted from CastleDefense CastleBuilder) ---

    private void clearArea(World world, int cx, int cy, int cz) {
        int halfX = WALL_LENGTH / 2 + TOWER_SIZE + 35;
        int halfZ = WALL_LENGTH / 2 + TOWER_SIZE + 15;
        for (int x = cx - halfX; x <= cx + halfX; x++) {
            for (int z = cz - halfZ; z <= cz + halfZ; z++) {
                for (int y = cy; y <= cy + TOWER_HEIGHT + 5; y++) {
                    setBlock(world, x, y, z, Material.AIR);
                }
            }
        }
    }

    private static final int FOUNDATION_DEPTH = 5;

    private void buildFloor(World world, int cx, int cy, int cz) {
        int half = WALL_LENGTH / 2;
        for (int x = cx - half; x <= cx + half; x++) {
            for (int z = cz - half; z <= cz + half; z++) {
                setBlock(world, x, cy, z, Material.SMOOTH_STONE);
                setBlock(world, x, cy - 1, z, Material.STONE_BRICKS);
                for (int d = 2; d <= FOUNDATION_DEPTH; d++) {
                    setBlock(world, x, cy - d, z, Material.STONE);
                }
            }
        }
        int outerX = half + 35;
        int outerZ = half + 15;
        for (int x = cx - outerX; x <= cx + outerX; x++) {
            for (int z = cz - outerZ; z <= cz + outerZ; z++) {
                if (x < cx - half || x > cx + half || z < cz - half || z > cz + half) {
                    setBlock(world, x, cy, z, Material.AIR);
                    setBlock(world, x, cy - 1, z, Material.GRASS_BLOCK);
                    for (int d = 2; d <= FOUNDATION_DEPTH; d++) {
                        setBlock(world, x, cy - d, z, Material.DIRT);
                    }
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
                setBlock(world, cx - half, y, z, Material.STONE_BRICKS);
                if (z < cz - gateHalf || z > cz + gateHalf || y > cy + GATE_HEIGHT) {
                    setBlock(world, cx + half, y, z, Material.STONE_BRICKS);
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
                boolean isEdge = x == tx - halfT || x == tx + halfT || z == tz - halfT || z == tz + halfT;
                if (isEdge && (x + z) % 2 == 0) {
                    setBlock(world, x, topY + 1, z, Material.STONE_BRICK_WALL);
                }
            }
        }
        setBlock(world, tx, topY + 1, tz, Material.TORCH);
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
        for (int y = cy + 1; y <= cy + 3; y++) {
            setBlock(world, gateX, y, cz - gateHalf, Material.DARK_OAK_FENCE);
            setBlock(world, gateX, y, cz + gateHalf, Material.DARK_OAK_FENCE);
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
        int secondFloorY = cy + 5;

        // Ground floor
        for (int x = roomX - 5; x <= roomX + 5; x++) {
            for (int z = roomZ - 6; z <= roomZ + 6; z++) {
                setBlock(world, x, cy, z, Material.POLISHED_DEEPSLATE);
            }
        }

        // Walls - extend up to cover both floors
        for (int y = cy + 1; y <= cy + 11; y++) {
            for (int x = roomX - 6; x <= roomX + 6; x++) {
                for (int z = roomZ - 7; z <= roomZ + 7; z++) {
                    boolean isEdge = x == roomX - 6 || x == roomX + 6 || z == roomZ - 7 || z == roomZ + 7;
                    if (isEdge) {
                        setBlock(world, x, y, z, Material.DEEPSLATE_BRICKS);
                    }
                }
            }
        }

        // Clear entire interior (both floors) to start fresh
        for (int y = cy + 1; y <= cy + 11; y++) {
            for (int x = roomX - 5; x <= roomX + 5; x++) {
                for (int z = roomZ - 6; z <= roomZ + 6; z++) {
                    setBlock(world, x, y, z, Material.AIR);
                }
            }
        }

        // Second floor platform - leave a 3x5 opening for the staircase on the +z, +x corner
        // Opening: x from roomX+3 to roomX+5, z from roomZ+3 to roomZ+6
        for (int x = roomX - 5; x <= roomX + 5; x++) {
            for (int z = roomZ - 6; z <= roomZ + 6; z++) {
                boolean isStairOpening = (x >= roomX + 3 && x <= roomX + 5)
                        && (z >= roomZ + 3 && z <= roomZ + 6);
                if (!isStairOpening) {
                    setBlock(world, x, secondFloorY, z, Material.POLISHED_DEEPSLATE);
                }
            }
        }

        // Roof
        for (int x = roomX - 6; x <= roomX + 6; x++) {
            for (int z = roomZ - 7; z <= roomZ + 7; z++) {
                setBlock(world, x, cy + 12, z, Material.DEEPSLATE_BRICK_SLAB);
            }
        }

        // Ground floor entrance (from castle interior into throne room) - 3 wide, 3 tall
        for (int z = roomZ - 1; z <= roomZ + 1; z++) {
            for (int y = cy + 1; y <= cy + 3; y++) {
                setBlock(world, roomX + 6, y, z, Material.AIR);
            }
        }

        // === STRAIGHT STAIRCASE ===
        // 3 blocks wide (z = roomZ+4 to roomZ+6), going in the -x direction.
        // 5 steps: each step is 1 block in -x, 1 block up.
        // Step 0 (bottom): x=roomX+5, y=cy+1
        // Step 1:           x=roomX+4, y=cy+2
        // Step 2:           x=roomX+3, y=cy+3
        // Step 3:           x=roomX+2, y=cy+4
        // Step 4 (top):     x=roomX+1, y=cy+5 = secondFloorY (merges with second floor)
        //
        // The second floor opening at x=roomX+3..+5, z=roomZ+3..+6 lets us
        // walk up without hitting our head on the floor above.

        for (int i = 0; i < 5; i++) {
            int stepX = roomX + 5 - i;
            int stepY = cy + 1 + i;
            for (int z = roomZ + 4; z <= roomZ + 6; z++) {
                // Place the step block
                setBlock(world, stepX, stepY, z, Material.POLISHED_DEEPSLATE);
                // Fill solid below (so steps aren't floating)
                for (int fillY = cy + 1; fillY < stepY; fillY++) {
                    setBlock(world, stepX, fillY, z, Material.DEEPSLATE_BRICKS);
                }
                // Ensure at least 3 blocks of headroom above each step
                for (int h = 1; h <= 3; h++) {
                    setBlock(world, stepX, stepY + h, z, Material.AIR);
                }
            }
        }

        // The top step (i=4) is at x=roomX+1, y=secondFloorY.
        // Connect it to the main second floor by making sure there's no gap.
        // The second floor already has solid blocks at x=roomX+1, z=roomZ+4..+6
        // at y=secondFloorY, but let's also clear any floor block that would
        // double up and ensure smooth walkway from step onto the main floor.
        // Clear the second floor blocks right where the top step merges:
        for (int z = roomZ + 3; z <= roomZ + 6; z++) {
            setBlock(world, roomX + 1, secondFloorY + 1, z, Material.AIR);
            setBlock(world, roomX + 1, secondFloorY + 2, z, Material.AIR);
            setBlock(world, roomX + 1, secondFloorY + 3, z, Material.AIR);
        }

        // Fence railing: only on the SIDE of the opening, not blocking the walkway
        // Place along z=roomZ+3 edge (the inner side facing the room)
        // but NOT at x where you walk onto the second floor
        for (int x = roomX + 3; x <= roomX + 5; x++) {
            setBlock(world, x, secondFloorY + 1, roomZ + 3, Material.DARK_OAK_FENCE);
        }
        // Place along x=roomX+2 edge (the side wall of the opening)
        // but leave z=roomZ+4..+6 open so you can walk off the top step
        // Only fence the corner
        setBlock(world, roomX + 2, secondFloorY + 1, roomZ + 3, Material.DARK_OAK_FENCE);

        // Ground floor torches
        setBlock(world, roomX - 5, cy + 3, roomZ - 4, Material.TORCH);
        setBlock(world, roomX - 5, cy + 3, roomZ + 4, Material.TORCH);
        setBlock(world, roomX + 5, cy + 3, roomZ - 4, Material.TORCH);

        // Second floor torches
        setBlock(world, roomX - 5, secondFloorY + 3, roomZ - 6, Material.TORCH);
        setBlock(world, roomX - 5, secondFloorY + 3, roomZ + 6, Material.TORCH);
        setBlock(world, roomX + 5, secondFloorY + 3, roomZ - 6, Material.TORCH);
        setBlock(world, roomX + 5, secondFloorY + 3, roomZ + 6, Material.TORCH);

        // Throne on second floor behind the aldeano
        setBlock(world, roomX - 3, secondFloorY + 1, roomZ, Material.POLISHED_DEEPSLATE);
        setBlock(world, roomX - 3, secondFloorY + 2, roomZ, Material.POLISHED_DEEPSLATE);
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
            int ladderZ = tz - 1;
            int wallZ = tz - 2;
            for (int y = cy + 1; y <= platformY + 2; y++) {
                setBlock(world, tx, y, wallZ, Material.STONE_BRICKS);
                setBlock(world, tx, y, ladderZ, Material.LADDER);
            }
            setBlock(world, tx, platformY + 3, ladderZ, Material.AIR);
            int doorY = cy + 1;
            if (tz < cz) {
                for (int y = doorY; y <= doorY + 2; y++) setBlock(world, tx, y, tz + halfT, Material.AIR);
            }
            if (tz > cz) {
                for (int y = doorY; y <= doorY + 2; y++) setBlock(world, tx, y, tz - halfT, Material.AIR);
            }
            if (tx < cx) {
                for (int y = doorY; y <= doorY + 2; y++) setBlock(world, tx + halfT, y, tz, Material.AIR);
            }
            if (tx > cx) {
                for (int y = doorY; y <= doorY + 2; y++) setBlock(world, tx - halfT, y, tz, Material.AIR);
            }
        }
    }

    private void clearSpawnArea(World world, Location loc) {
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        // Clear a 1-block radius around the spawn at foot and head height
        for (int x = bx - 1; x <= bx + 1; x++) {
            for (int z = bz - 1; z <= bz + 1; z++) {
                setBlock(world, x, by, z, Material.AIR);
                setBlock(world, x, by + 1, z, Material.AIR);
            }
        }
    }

    private void setBlock(World world, int x, int y, int z, Material material) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(material, false);
    }
}
