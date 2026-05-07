package com.gidexplayyt.mcauth.core;

public enum BotType {
    TELEGRAM,
    VK,
    DISCORD,
    GOOGLE_AUTHENTICATOR;

    public static BotType parse(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        for (BotType type : values()) {
            if (type.name().equals(normalized) || type.name().replace("_", "").equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
