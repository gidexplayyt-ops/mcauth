package com.gidexplayyt.mcauth.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public class AuthManager {
    private final AuthStorage storage;
    private final BotIntegrationService botService;
    private final Map<UUID, AccountData> accountsByUuid = new HashMap<>();
    private final Map<UUID, LoginRequest> pendingRequests = new HashMap<>();
    private final Map<UUID, BindingRequest> pendingBindings = new HashMap<>();
    private final Set<UUID> authenticated = new HashSet<>();

    public AuthManager(AuthStorage storage, BotIntegrationService botService) {
        this.storage = storage;
        this.botService = botService;
        loadAccounts();
    }

    private void loadAccounts() {
        try {
            for (AccountData account : storage.loadAccounts()) {
                accountsByUuid.put(account.uuid, account);
            }
        } catch (IOException exception) {
            botService.sendMessage(new AccountData(null, "system", null), "Не удалось загрузить данные аккаунтов: " + exception.getMessage());
        }
    }

    private void saveAccounts() {
        try {
            storage.saveAccounts(new ArrayList<>(accountsByUuid.values()));
        } catch (IOException exception) {
            botService.sendMessage(new AccountData(null, "system", null), "Не удалось сохранить данные аккаунтов: " + exception.getMessage());
        }
    }

    public synchronized AccountData register(UUID uuid, String username, String password) {
        if (findByUsername(username).isPresent()) {
            return null;
        }
        String hash = hashPassword(password);
        AccountData account = new AccountData(uuid, username, hash);
        accountsByUuid.put(uuid, account);
        saveAccounts();
        return account;
    }

    public synchronized Optional<AccountData> login(String username, String password) {
        Optional<AccountData> account = findByUsername(username);
        if (account.isEmpty()) {
            return Optional.empty();
        }
        if (!account.get().passwordHash.equals(hashPassword(password))) {
            return Optional.empty();
        }
        markAuthenticated(account.get().uuid);
        return account;
    }

    public synchronized boolean markAuthenticated(UUID uuid) {
        authenticated.add(uuid);
        AccountData account = accountsByUuid.get(uuid);
        if (account != null) {
            account.authenticated = true;
            account.lastLogin = System.currentTimeMillis();
            saveAccounts();
        }
        return true;
    }

    public synchronized boolean isAuthenticated(UUID uuid) {
        return authenticated.contains(uuid);
    }

    public synchronized boolean requestLogin(UUID uuid) {
        AccountData account = accountsByUuid.get(uuid);
        if (account == null) {
            return false;
        }
        if (!hasLinkedAccount(uuid) && (account.googleSecret == null || account.googleSecret.isEmpty())) {
            return false;
        }
        BotType botType = account.linkedAccounts.containsKey(BotType.TELEGRAM) ? BotType.TELEGRAM :
                account.linkedAccounts.containsKey(BotType.VK) ? BotType.VK :
                account.linkedAccounts.containsKey(BotType.DISCORD) ? BotType.DISCORD :
                BotType.GOOGLE_AUTHENTICATOR;
        String code = botType == BotType.GOOGLE_AUTHENTICATOR
                ? GoogleAuthenticatorUtil.getCode(account.googleSecret)
                : String.format("%06d", new Random().nextInt(1_000_000));
        LoginRequest request = new LoginRequest(uuid, botType, code, 300);
        pendingRequests.put(uuid, request);
        botService.sendLoginRequest(account, request);
        saveAccounts();
        return true;
    }

    public synchronized boolean confirmLogin(UUID uuid, String code) {
        cleanupExpiredRequests();
        LoginRequest request = pendingRequests.get(uuid);
        if (request == null || request.isExpired()) {
            pendingRequests.remove(uuid);
            return false;
        }
        AccountData account = accountsByUuid.get(uuid);
        if (account == null) {
            return false;
        }
        if (request.botType == BotType.GOOGLE_AUTHENTICATOR) {
            if (!botService.verifyGoogleCode(account, code)) {
                return false;
            }
        } else if (!request.code.equals(code)) {
            return false;
        }
        request.confirmed = true;
        markAuthenticated(uuid);
        pendingRequests.remove(uuid);
        return true;
    }

    public synchronized String createBindingCode(UUID uuid, BotType botType, String identifier) {
        AccountData account = accountsByUuid.get(uuid);
        if (account == null || botType == null || identifier == null || identifier.isBlank()) {
            return null;
        }
        String code = String.format("%06d", new Random().nextInt(1_000_000));
        BindingRequest bindingRequest = new BindingRequest(uuid, botType, identifier, code, 600);
        pendingBindings.put(uuid, bindingRequest);
        String message = String.format("[MCAuth] Код привязки для %s: %s. Используйте /mcauth code confirm %s на сервере.", account.username, code, code);
        if (!botService.sendToBot(botType, identifier, message)) {
            pendingBindings.remove(uuid);
            return null;
        }
        return code;
    }

    public synchronized boolean confirmBinding(UUID uuid, String code) {
        cleanupExpiredRequests();
        BindingRequest bindingRequest = pendingBindings.get(uuid);
        if (bindingRequest == null || bindingRequest.isExpired()) {
            pendingBindings.remove(uuid);
            return false;
        }
        if (!bindingRequest.code.equals(code)) {
            return false;
        }
        boolean linked = linkBot(uuid, bindingRequest.botType, bindingRequest.identifier);
        pendingBindings.remove(uuid);
        return linked;
    }

    public synchronized boolean hasLinkedAccount(UUID uuid) {
        AccountData account = accountsByUuid.get(uuid);
        if (account == null) {
            return false;
        }
        return account.linkedAccounts.values().stream().anyMatch(value -> value != null && !value.isBlank());
    }

    public synchronized boolean isLicensedBypass(UUID uuid) {
        AccountData account = accountsByUuid.get(uuid);
        return account != null && account.licensed && !hasLinkedAccount(uuid);
    }

    public synchronized boolean linkBot(UUID uuid, BotType botType, String identifier) {
        AccountData account = accountsByUuid.get(uuid);
        if (account == null || botType == null || identifier == null || identifier.isBlank()) {
            return false;
        }
        account.linkedAccounts.put(botType, identifier);
        if (botType == BotType.GOOGLE_AUTHENTICATOR && (account.googleSecret == null || account.googleSecret.isEmpty())) {
            account.googleSecret = GoogleAuthenticatorUtil.generateSecret();
        }
        saveAccounts();
        return true;
    }

    public synchronized boolean markLicensed(UUID uuid, String licenseKey) {
        AccountData account = accountsByUuid.get(uuid);
        if (account == null) {
            account = new AccountData(uuid, "unknown", null);
            accountsByUuid.put(uuid, account);
        }
        account.licensed = true;
        account.licenseKey = licenseKey;
        saveAccounts();
        return true;
    }

    public synchronized Optional<AccountData> findByUsername(String username) {
        return accountsByUuid.values().stream()
                .filter(account -> account.username != null && account.username.equalsIgnoreCase(username))
                .findFirst();
    }

    public synchronized int getAccountCount() {
        return accountsByUuid.size();
    }

    public synchronized int getLinkedAccountCount(UUID uuid) {
        AccountData account = accountsByUuid.get(uuid);
        if (account == null) {
            return 0;
        }
        return (int) account.linkedAccounts.values().stream().filter(value -> value != null && !value.isBlank()).count();
    }

    public synchronized void cleanupExpiredRequests() {
        pendingRequests.values().removeIf(LoginRequest::isExpired);
        pendingBindings.values().removeIf(BindingRequest::isExpired);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 не поддерживается", exception);
        }
    }
}
