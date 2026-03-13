package com.castledefense.model;

import org.bukkit.Material;

public enum Team {
    RED("Red", "§c", Material.RED_BANNER),
    BLUE("Blue", "§9", Material.BLUE_BANNER);

    private final String displayName;
    private final String color;
    private final Material bannerMaterial;

    Team(String displayName, String color, Material bannerMaterial) {
        this.displayName = displayName;
        this.color = color;
        this.bannerMaterial = bannerMaterial;
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

    public Material getBannerMaterial() {
        return bannerMaterial;
    }

    public Team getOpposite() {
        return this == RED ? BLUE : RED;
    }
}
