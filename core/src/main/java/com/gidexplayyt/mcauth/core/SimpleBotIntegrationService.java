package com.gidexplayyt.mcauth.core;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

public class SimpleBotIntegrationService implements BotIntegrationService {
    private final Map<String, String> configuration;
    private final Consumer<String> logger;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SimpleBotIntegrationService(Map<String, String> configuration, Consumer<String> logger) {
        this.configuration = configuration;
        this.logger = logger;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean sendLoginRequest(AccountData account, LoginRequest request) {
        String linkedId = account.linkedAccounts.get(request.botType);
        if (linkedId == null || linkedId.isEmpty()) {
            logger.accept("[MCAuth] Попытка отправить запрос на вход, но аккаунт не привязан к " + request.botType);
            return false;
        }
        String message = String.format("[MCAuth] Запрос входа для %s (%s). Код: %s. Срок 5 минут.", account.username, request.botType, request.code);
        if (request.botType == BotType.GOOGLE_AUTHENTICATOR) {
            logger.accept("[MCAuth] Google Authenticator код для " + account.username + ": " + request.code);
            return true;
        }
        boolean result = sendToBot(request.botType, linkedId, message);
        if (!result) {
            logger.accept("[MCAuth] Не удалось отправить запрос на вход пользователю " + account.username + " через " + request.botType);
        }
        return result;
    }

    @Override
    public void sendMessage(AccountData account, String message) {
        if (account == null || account.linkedAccounts == null) {
            return;
        }
        account.linkedAccounts.forEach((type, id) -> {
            if (id == null || id.isBlank()) {
                return;
            }
            sendToBot(type, id, message);
        });
    }

    @Override
    public boolean sendToBot(BotType botType, String identifier, String message) {
        switch (botType) {
            case TELEGRAM:
                return sendTelegramMessage(identifier, message);
            case VK:
                return sendVkMessage(identifier, message);
            case DISCORD:
                return sendDiscordMessage(identifier, message);
            default:
                logger.accept("[MCAuth] Неизвестный тип бота при отправке сообщения: " + botType);
                return false;
        }
    }

    @Override
    public boolean verifyGoogleCode(AccountData account, String code) {
        if (account.googleSecret == null || account.googleSecret.isEmpty()) {
            return false;
        }
        String expected = GoogleAuthenticatorUtil.getCode(account.googleSecret);
        return expected.equals(code);
    }

    private boolean sendTelegramMessage(String chatId, String message) {
        String token = configuration.getOrDefault("telegram-token", "");
        if (token.isBlank()) {
            logger.accept("[MCAuth] Telegram token не настроен.");
            return false;
        }
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "chat_id", chatId,
                    "text", message,
                    "parse_mode", "HTML"
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.telegram.org/bot" + token + "/sendMessage"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (IOException | InterruptedException exception) {
            logger.accept("[MCAuth] Ошибка отправки Telegram-сообщения: " + exception.getMessage());
            return false;
        }
    }

    private boolean sendVkMessage(String peerId, String message) {
        String token = configuration.getOrDefault("vk-token", "");
        String apiVersion = configuration.getOrDefault("vk-api-version", "5.131");
        if (token.isBlank()) {
            logger.accept("[MCAuth] VK token не настроен.");
            return false;
        }
        try {
            String url = String.format(
                    "https://api.vk.com/method/messages.send?access_token=%s&v=%s&peer_id=%s&random_id=%d&message=%s",
                    URLEncoder.encode(token, StandardCharsets.UTF_8),
                    URLEncoder.encode(apiVersion, StandardCharsets.UTF_8),
                    URLEncoder.encode(peerId, StandardCharsets.UTF_8),
                    System.currentTimeMillis() / 1000,
                    URLEncoder.encode(message, StandardCharsets.UTF_8)
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 && response.body().contains("response");
        } catch (IOException | InterruptedException exception) {
            logger.accept("[MCAuth] Ошибка отправки VK-сообщения: " + exception.getMessage());
            return false;
        }
    }

    private boolean sendDiscordMessage(String recipientId, String message) {
        String token = configuration.getOrDefault("discord-token", "");
        if (token.isBlank()) {
            logger.accept("[MCAuth] Discord token не настроен.");
            return false;
        }
        try {
            String channelJson = objectMapper.writeValueAsString(Map.of("recipient_id", recipientId));
            HttpRequest channelRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://discord.com/api/v10/users/@me/channels"))
                    .header("Authorization", "Bot " + token)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(channelJson))
                    .build();
            HttpResponse<String> channelResponse = httpClient.send(channelRequest, HttpResponse.BodyHandlers.ofString());
            if (channelResponse.statusCode() != 200) {
                logger.accept("[MCAuth] Не удалось создать Discord DM-канал: " + channelResponse.body());
                return false;
            }
            Map<?, ?> channelData = objectMapper.readValue(channelResponse.body(), Map.class);
            String channelId = String.valueOf(channelData.get("id"));
            if (channelId == null || channelId.isEmpty()) {
                logger.accept("[MCAuth] Discord канал не возвращен.");
                return false;
            }
            String messageJson = objectMapper.writeValueAsString(Map.of("content", message));
            HttpRequest messageRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://discord.com/api/v10/channels/" + channelId + "/messages"))
                    .header("Authorization", "Bot " + token)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(messageJson))
                    .build();
            HttpResponse<String> messageResponse = httpClient.send(messageRequest, HttpResponse.BodyHandlers.ofString());
            return messageResponse.statusCode() == 200;
        } catch (IOException | InterruptedException exception) {
            logger.accept("[MCAuth] Ошибка отправки Discord-сообщения: " + exception.getMessage());
            return false;
        }
    }
}
