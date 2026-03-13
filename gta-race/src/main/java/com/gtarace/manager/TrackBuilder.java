package com.gtarace.manager;

import com.gtarace.GTARacePlugin;
import com.gtarace.model.PowerUpType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class TrackBuilder {

    private final GTARacePlugin plugin;
    private final TrackManager trackManager;

    private static final int TRACK_WIDTH = 7;
    private static final int STRAIGHT_LENGTH = 60;
    private static final int CURVE_OUTER_RADIUS = 25;
    private static final int BARRIER_HEIGHT = 2;
    private static final Material ROAD_SURFACE = Material.BLUE_ICE;
    private static final Material ROAD_BASE = Material.PACKED_ICE;
    private static final Material BARRIER_MATERIAL = Material.STONE_BRICK_WALL;
    private static final Material BARRIER_BASE = Material.STONE_BRICKS;
    private static final Material START_LINE = Material.BLACK_CONCRETE;
    private static final Material START_LINE_ACCENT = Material.WHITE_CONCRETE;
    private static final Material PIT_FLOOR = Material.SMOOTH_STONE;
    private static final Material CURB_INNER = Material.RED_CONCRETE;
    private static final Material CURB_OUTER = Material.WHITE_CONCRETE;

    public TrackBuilder(GTARacePlugin plugin, TrackManager trackManager) {
        this.plugin = plugin;
        this.trackManager = trackManager;
    }

    /**
     * Builds an oval racetrack at the given location.
     * The track is an oval: two straight sections connected by two semicircular curves.
     * The player's location becomes the center of the start/finish straight.
     *
     * Layout (top-down, Z is forward from player):
     *
     *       ____________________
     *      /                    \
     *     |   CURVE (north)      |
     *     |                      |
     *     |  STRAIGHT (west)  STRAIGHT (east)  |
     *     |                      |
     *     |   CURVE (south)      |
     *      \____________________/
     *
     * Start/finish is on the east straight.
     */
    public void buildTrack(Location center) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        int innerRadius = CURVE_OUTER_RADIUS - TRACK_WIDTH;
        int halfStraight = STRAIGHT_LENGTH / 2;

        clearArea(world, cx, cy, cz, halfStraight);

        // Build east straight (start/finish side)
        int eastX = cx + CURVE_OUTER_RADIUS;
        for (int z = cz - halfStraight; z <= cz + halfStraight; z++) {
            for (int x = eastX - TRACK_WIDTH; x <= eastX; x++) {
                buildRoadSegment(world, x, cy, z);
            }
            buildBarrier(world, eastX + 1, cy, z);
            buildBarrier(world, eastX - TRACK_WIDTH - 1, cy, z);
        }

        // Build west straight
        int westX = cx - CURVE_OUTER_RADIUS;
        for (int z = cz - halfStraight; z <= cz + halfStraight; z++) {
            for (int x = westX; x <= westX + TRACK_WIDTH; x++) {
                buildRoadSegment(world, x, cy, z);
            }
            buildBarrier(world, westX - 1, cy, z);
            buildBarrier(world, westX + TRACK_WIDTH + 1, cy, z);
        }

        // Build north curve (z = cz - halfStraight)
        buildCurve(world, cx, cy, cz - halfStraight, CURVE_OUTER_RADIUS, innerRadius, true);

        // Build south curve (z = cz + halfStraight)
        buildCurve(world, cx, cy, cz + halfStraight, CURVE_OUTER_RADIUS, innerRadius, false);

        // Build start/finish line
        buildStartFinishLine(world, eastX, cy, cz);

        // Build pit area / grandstand decoration near start
        buildGrandstand(world, eastX + 3, cy, cz);

        // Add curbs at curve entries
        addCurbs(world, cx, cy, cz, halfStraight, CURVE_OUTER_RADIUS, innerRadius);

        // Add lighting
        addLighting(world, cx, cy, cz, halfStraight, eastX, westX);

        // Configure track points
        setupTrackConfig(world, cx, cy, cz, halfStraight, eastX, westX, CURVE_OUTER_RADIUS);

        plugin.getLogger().info("Race track built at " + cx + ", " + cy + ", " + cz);
    }

    private void buildCurve(World world, int cx, int cy, int cz, int outerR, int innerR, boolean north) {
        for (int angle = 0; angle <= 180; angle++) {
            double rad = Math.toRadians(angle);

            double cos = Math.cos(rad);
            double sin = north ? -Math.sin(rad) : Math.sin(rad);

            for (int r = innerR; r <= outerR; r++) {
                int bx = cx + (int) Math.round(cos * r);
                int bz = cz + (int) Math.round(sin * r);
                buildRoadSegment(world, bx, cy, bz);
            }

            // Outer barrier
            int ox = cx + (int) Math.round(cos * (outerR + 1));
            int oz = cz + (int) Math.round(sin * (outerR + 1));
            buildBarrier(world, ox, cy, oz);

            // Inner barrier
            int ix = cx + (int) Math.round(cos * (innerR - 1));
            int iz = cz + (int) Math.round(sin * (innerR - 1));
            buildBarrier(world, ix, cy, iz);
        }
    }

    private void buildRoadSegment(World world, int x, int cy, int z) {
        setBlock(world, x, cy - 2, z, Material.STONE);
        setBlock(world, x, cy - 1, z, ROAD_BASE);
        setBlock(world, x, cy, z, ROAD_SURFACE);
        // Clear above road
        for (int y = cy + 1; y <= cy + 5; y++) {
            setBlock(world, x, y, z, Material.AIR);
        }
    }

    private void buildBarrier(World world, int x, int cy, int z) {
        setBlock(world, x, cy - 1, z, Material.STONE);
        setBlock(world, x, cy, z, BARRIER_BASE);
        for (int y = cy + 1; y <= cy + BARRIER_HEIGHT; y++) {
            setBlock(world, x, y, z, BARRIER_MATERIAL);
        }
    }

    private void buildStartFinishLine(World world, int eastX, int cy, int cz) {
        for (int x = eastX - TRACK_WIDTH; x <= eastX; x++) {
            boolean isWhite = (x % 2 == 0);
            setBlock(world, x, cy, cz, isWhite ? START_LINE_ACCENT : START_LINE);
            setBlock(world, x, cy, cz - 1, isWhite ? START_LINE : START_LINE_ACCENT);
        }

        // Checkered pattern on adjacent rows for visibility
        for (int x = eastX - TRACK_WIDTH; x <= eastX; x++) {
            boolean isWhite = (x % 2 == 0);
            setBlock(world, x, cy, cz + 1, isWhite ? START_LINE : START_LINE_ACCENT);
        }
    }

    private void buildGrandstand(World world, int startX, int cy, int cz) {
        for (int row = 0; row < 4; row++) {
            int x = startX + row;
            for (int z = cz - 8; z <= cz + 8; z++) {
                setBlock(world, x, cy + row, z, Material.QUARTZ_STAIRS);
                setBlock(world, x, cy + row - 1, z, Material.QUARTZ_BLOCK);
            }
        }

        // Roof over grandstand
        for (int z = cz - 9; z <= cz + 9; z++) {
            setBlock(world, startX - 1, cy + 5, z, Material.QUARTZ_SLAB);
            for (int x = startX; x < startX + 5; x++) {
                setBlock(world, x, cy + 5, z, Material.QUARTZ_SLAB);
            }
        }

        // Support pillars
        for (int y = cy; y <= cy + 5; y++) {
            setBlock(world, startX, y, cz - 9, Material.QUARTZ_PILLAR);
            setBlock(world, startX, y, cz + 9, Material.QUARTZ_PILLAR);
            setBlock(world, startX + 3, y, cz - 9, Material.QUARTZ_PILLAR);
            setBlock(world, startX + 3, y, cz + 9, Material.QUARTZ_PILLAR);
        }
    }

    private void addCurbs(World world, int cx, int cy, int cz, int halfStraight,
                          int outerR, int innerR) {
        // Curbs at the inner edges of curves
        for (int angle = 0; angle <= 180; angle += 2) {
            double rad = Math.toRadians(angle);

            // North curve inner curb
            int nix = cx + (int) Math.round(Math.cos(rad) * innerR);
            int niz = cz - halfStraight + (int) Math.round(-Math.sin(rad) * innerR);
            boolean isRed = (angle / 2) % 2 == 0;
            setBlock(world, nix, cy, niz, isRed ? CURB_INNER : CURB_OUTER);

            // South curve inner curb
            int six = cx + (int) Math.round(Math.cos(rad) * innerR);
            int siz = cz + halfStraight + (int) Math.round(Math.sin(rad) * innerR);
            setBlock(world, six, cy, siz, isRed ? CURB_INNER : CURB_OUTER);
        }
    }

    private void addLighting(World world, int cx, int cy, int cz, int halfStraight,
                             int eastX, int westX) {
        // Lights along east straight
        for (int z = cz - halfStraight; z <= cz + halfStraight; z += 8) {
            buildLampPost(world, eastX + 2, cy, z);
        }
        // Lights along west straight
        for (int z = cz - halfStraight; z <= cz + halfStraight; z += 8) {
            buildLampPost(world, westX - 2, cy, z);
        }
    }

    private void buildLampPost(World world, int x, int cy, int z) {
        setBlock(world, x, cy, z, Material.STONE_BRICKS);
        setBlock(world, x, cy + 1, z, Material.STONE_BRICK_WALL);
        setBlock(world, x, cy + 2, z, Material.STONE_BRICK_WALL);
        setBlock(world, x, cy + 3, z, Material.STONE_BRICK_WALL);
        setBlock(world, x, cy + 4, z, Material.LANTERN);
    }

    private void clearArea(World world, int cx, int cy, int cz, int halfStraight) {
        int clearRadius = CURVE_OUTER_RADIUS + 10;
        for (int x = cx - clearRadius; x <= cx + clearRadius; x++) {
            for (int z = cz - halfStraight - clearRadius; z <= cz + halfStraight + clearRadius; z++) {
                for (int y = cy; y <= cy + 8; y++) {
                    setBlock(world, x, y, z, Material.AIR);
                }
                // Flatten ground
                setBlock(world, x, cy - 1, z, Material.GRASS_BLOCK);
                setBlock(world, x, cy - 2, z, Material.DIRT);
            }
        }
    }

    private void setupTrackConfig(World world, int cx, int cy, int cz,
                                   int halfStraight, int eastX, int westX, int outerR) {
        // Start: on the east straight, behind the start line, facing north (negative Z)
        Location start = new Location(world, eastX - TRACK_WIDTH / 2.0, cy + 1, cz + 4, 0f, 0f);
        trackManager.setStart(start);

        // Finish: same as start line position
        Location finish = new Location(world, eastX - TRACK_WIDTH / 2.0, cy + 1, cz);
        trackManager.setFinish(finish);

        // Checkpoints around the track (8 checkpoints for a good oval)
        // 1: End of east straight going north
        trackManager.addCheckpoint(new Location(world, eastX - TRACK_WIDTH / 2.0, cy + 1, cz - halfStraight + 5));
        // 2: Middle of north curve (top)
        trackManager.addCheckpoint(new Location(world, cx, cy + 1, cz - halfStraight - outerR / 2.0));
        // 3: Start of west straight from north
        trackManager.addCheckpoint(new Location(world, westX + TRACK_WIDTH / 2.0, cy + 1, cz - halfStraight + 5));
        // 4: Middle of west straight
        trackManager.addCheckpoint(new Location(world, westX + TRACK_WIDTH / 2.0, cy + 1, cz));
        // 5: End of west straight going south
        trackManager.addCheckpoint(new Location(world, westX + TRACK_WIDTH / 2.0, cy + 1, cz + halfStraight - 5));
        // 6: Middle of south curve (bottom)
        trackManager.addCheckpoint(new Location(world, cx, cy + 1, cz + halfStraight + outerR / 2.0));
        // 7: Back onto east straight from south
        trackManager.addCheckpoint(new Location(world, eastX - TRACK_WIDTH / 2.0, cy + 1, cz + halfStraight - 5));

        // Power-up spawns (4 around the track)
        // Speed boost on east straight
        trackManager.addPowerUpSpawn(
                new Location(world, eastX - TRACK_WIDTH / 2.0, cy + 1, cz - halfStraight / 2.0),
                PowerUpType.SPEED_BOOST);
        // Missile on west straight
        trackManager.addPowerUpSpawn(
                new Location(world, westX + TRACK_WIDTH / 2.0, cy + 1, cz + halfStraight / 2.0),
                PowerUpType.MISSILE);
        // Shield at north curve exit
        trackManager.addPowerUpSpawn(
                new Location(world, cx - outerR / 2.0, cy + 1, cz - halfStraight),
                PowerUpType.SHIELD);
        // Oil slick at south curve exit
        trackManager.addPowerUpSpawn(
                new Location(world, cx + outerR / 2.0, cy + 1, cz + halfStraight),
                PowerUpType.OIL_SLICK);

        plugin.getLogger().info("Track configured: 7 checkpoints, 4 power-up spawns, start/finish set.");
    }

    private void setBlock(World world, int x, int y, int z, Material material) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(material, false);
    }
}
