package com.mod.archetype.core;

public enum ClassCategory {
    DAMAGE("damage"),
    TANK("tank"),
    MOBILITY("mobility"),
    UTILITY("utility");

    private final String serializedName;

    ClassCategory(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public static ClassCategory fromString(String name) {
        for (ClassCategory cat : values()) {
            if (cat.serializedName.equalsIgnoreCase(name)) {
                return cat;
            }
        }
        return UTILITY;
    }
}
