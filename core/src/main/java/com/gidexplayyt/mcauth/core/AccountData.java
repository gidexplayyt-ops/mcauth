package com.gidexplayyt.mcauth.core;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class AccountData {
    public UUID uuid;
    public String username;
    public String passwordHash;
    public boolean licensed;
    public String licenseKey;
    public Map<BotType, String> linkedAccounts = new EnumMap<>(BotType.class);
    public String googleSecret;
    public long lastLogin;
    public boolean authenticated;

    public AccountData() {
    }

    public AccountData(UUID uuid, String username, String passwordHash) {
        this.uuid = uuid;
        this.username = username;
        this.passwordHash = passwordHash;
    }
}
