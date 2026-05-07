package com.gidexplayyt.mcauth.core;

import java.util.Map;
import java.util.function.Consumer;

public interface BotIntegrationService {
    boolean sendLoginRequest(AccountData account, LoginRequest request);
    void sendMessage(AccountData account, String message);
    boolean verifyGoogleCode(AccountData account, String code);
    boolean sendToBot(BotType botType, String identifier, String message);

    static BotIntegrationService create(Map<String, String> configuration, Consumer<String> logger) {
        return new SimpleBotIntegrationService(configuration, logger);
    }
}
