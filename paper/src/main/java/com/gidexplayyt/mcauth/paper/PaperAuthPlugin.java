package com.gidexplayyt.mcauth.paper;

import com.gidexplayyt.mcauth.core.AuthManager;
import com.gidexplayyt.mcauth.core.BotIntegrationService;
import com.gidexplayyt.mcauth.core.JsonAuthStorage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class PaperAuthPlugin extends JavaPlugin {
    private AuthManager authManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        authManager = new AuthManager(new JsonAuthStorage(getDataFolder().toPath()),
                BotIntegrationService.create(loadBotConfig(config), this::logInfo));

        getServer().getPluginManager().registerEvents(new AuthListener(this, authManager), this);
        if (getCommand("mcauth") != null) {
            getCommand("mcauth").setExecutor(new AuthCommand(authManager));
        }
        getServer().getScheduler().runTaskTimer(this, authManager::cleanupExpiredRequests, 20L, 20L);
        logInfo("MCAuth запущен. Поддержка регистрации, входа и привязки ботов.");
    }

    private Map<String, String> loadBotConfig(FileConfiguration config) {
        Map<String, String> result = new HashMap<>();
        result.put("telegram-token", config.getString("telegram-token", ""));
        result.put("vk-token", config.getString("vk-token", ""));
        result.put("vk-api-version", config.getString("vk-api-version", "5.131"));
        result.put("discord-token", config.getString("discord-token", ""));
        result.put("google-issuer", config.getString("google-authenticator-issuer", "MCAuth"));
        result.put("license-bypass", String.valueOf(config.getBoolean("license-bypass", true)));
        return result;
    }

    private void logInfo(String message) {
        getLogger().info(message);
    }
}
