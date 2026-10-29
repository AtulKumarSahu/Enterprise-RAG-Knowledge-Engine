package com.enterprise.ai.model;

public enum AccessLevel {
    PUBLIC(1),
    ENGINEERING(2),
    FINANCE(3),
    HR(4),
    ADMIN(5);

    private final int clearanceRank;

    AccessLevel(int clearanceRank) {
        this.clearanceRank = clearanceRank;
    }

    public int getClearanceRank() {
        return clearanceRank;
    }

    public boolean canAccess(AccessLevel documentAccessLevel) {
        return this.clearanceRank >= documentAccessLevel.clearanceRank;
    }

    public static AccessLevel fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return PUBLIC;
        }
        try {
            return AccessLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PUBLIC;
        }
    }
}

