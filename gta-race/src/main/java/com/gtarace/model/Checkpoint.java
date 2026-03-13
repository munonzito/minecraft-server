package com.gtarace.model;

import org.bukkit.Location;

public class Checkpoint {

    private final int index;
    private final Location location;
    private final double radius;

    public Checkpoint(int index, Location location, double radius) {
        this.index = index;
        this.location = location;
        this.radius = radius;
    }

    public int getIndex() {
        return index;
    }

    public Location getLocation() {
        return location.clone();
    }

    public double getRadius() {
        return radius;
    }

    public boolean isInside(Location loc) {
        if (!loc.getWorld().equals(location.getWorld())) return false;
        double dx = loc.getX() - location.getX();
        double dz = loc.getZ() - location.getZ();
        return (dx * dx + dz * dz) <= (radius * radius);
    }
}
