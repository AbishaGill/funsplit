package com.finsplit.app.models;

public enum Category {
    FOOD,
    TRANSPORT,
    RENT,
    UTILITIES,
    OTHER;

    /** Display label shown in chips and list items. */
    public String label() {
        switch (this) {
            case FOOD:      return "Food";
            case TRANSPORT: return "Transport";
            case RENT:      return "Rent";
            case UTILITIES: return "Utilities";
            case OTHER:     return "Other";
            default:        return name();
        }
    }

    public static Category fromString(String value) {
        if (value == null) return OTHER;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}
