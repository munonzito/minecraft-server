package com.castledefense.model;

public enum Kit {
    KNIGHT("Knight", "§6"),
    ARCHER("Archer", "§a"),
    TANK("Tank", "§7");

    private final String displayName;
    private final String color;

    Kit(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
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
}
