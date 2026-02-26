package com.castledefense.model;

public enum Team {
    ATTACKERS("Attackers", "§c"),
    DEFENDERS("Defenders", "§9");

    private final String displayName;
    private final String color;

    Team(String displayName, String color) {
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
