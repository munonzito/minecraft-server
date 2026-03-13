package com.gtarace.model;

import org.bukkit.Material;

public enum PowerUpType {
    SPEED_BOOST("Speed Boost", "§b", Material.GOLD_BLOCK),
    SHIELD("Shield", "§9", Material.DIAMOND_BLOCK),
    MISSILE("Missile", "§c", Material.REDSTONE_BLOCK),
    OIL_SLICK("Oil Slick", "§8", Material.COAL_BLOCK);

    private final String displayName;
    private final String color;
    private final Material displayMaterial;

    PowerUpType(String displayName, String color, Material displayMaterial) {
        this.displayName = displayName;
        this.color = color;
        this.displayMaterial = displayMaterial;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    public String getColoredName() {
        return color + displayName;
    }

    public Material getDisplayMaterial() {
        return displayMaterial;
    }
}
