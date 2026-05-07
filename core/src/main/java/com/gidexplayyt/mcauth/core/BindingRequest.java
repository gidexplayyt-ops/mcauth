package com.gidexplayyt.mcauth.core;

import java.util.UUID;

public class BindingRequest {
    public UUID playerUuid;
    public BotType botType;
    public String identifier;
    public String code;
    public long createdAt;
    public long expiresAt;

    public BindingRequest() {
    }

    public BindingRequest(UUID playerUuid, BotType botType, String identifier, String code, long ttlSeconds) {
        this.playerUuid = playerUuid;
        this.botType = botType;
        this.identifier = identifier;
        this.code = code;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = this.createdAt + ttlSeconds * 1000L;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
