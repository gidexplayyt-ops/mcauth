package com.gidexplayyt.mcauth.bungee;

import com.gidexplayyt.mcauth.core.AuthManager;
import com.gidexplayyt.mcauth.core.BotIntegrationService;
import com.gidexplayyt.mcauth.core.JsonAuthStorage;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class BungeeAuthPlugin extends Plugin {
    private AuthManager authManager;

    @Override
    public void onEnable() {
        Map<String, String> config = new HashMap<>();
        config.put("telegram", "");
        config.put("vk", "");
        config.put("discord", "");
        config.put("google-issuer", "MCAuth");
        config.put("license-bypass", "true");
        authManager = new AuthManager(new JsonAuthStorage(getDataFolder().toPath()), BotIntegrationService.create(config, getLogger()::info));
        getProxy().getPluginManager().registerCommand(this, new Command("mcauth") {
            @Override
            public void execute(CommandSender sender, String[] args) {
                sender.sendMessage(new TextComponent("MCAuth BungeeCord служит мостом и использует ядро регистрации в Paper/Spigot."));
            }
        });
    }
}
