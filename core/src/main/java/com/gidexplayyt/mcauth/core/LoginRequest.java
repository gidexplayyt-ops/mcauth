package com.gidexplayyt.mcauth.core;

import java.util.UUID;

public class LoginRequest {
    public UUID requestId;
    public UUID playerUuid;
    public BotType botType;
    public long createdAt;
    public long expiresAt;
    public String code;
    public boolean confirmed;

    public LoginRequest() {
    }

    public LoginRequest(UUID playerUuid, BotType botType, String code, long ttlSeconds) {
        this.requestId = UUID.randomUUID();
        this.playerUuid = playerUuid;
        this.botType = botType;
        this.code = code;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = this.createdAt + ttlSeconds * 1000L;
        this.confirmed = false;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
