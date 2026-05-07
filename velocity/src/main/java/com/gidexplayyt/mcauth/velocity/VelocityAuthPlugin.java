package com.gidexplayyt.mcauth.velocity;

import com.gidexplayyt.mcauth.core.AuthManager;
import com.gidexplayyt.mcauth.core.BotIntegrationService;
import com.gidexplayyt.mcauth.core.JsonAuthStorage;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.google.inject.Inject;
import net.kyori.adventure.text.Component;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Plugin(id = "mcauth", name = "MCAuth", version = "1.0.0")
public class VelocityAuthPlugin {
    private final ProxyServer server;
    private AuthManager authManager;

    @Inject
    public VelocityAuthPlugin(ProxyServer server) {
        this.server = server;
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        Map<String, String> config = new HashMap<>();
        config.put("telegram", "");
        config.put("vk", "");
        config.put("discord", "");
        config.put("google-issuer", "MCAuth");
        config.put("license-bypass", "true");
        authManager = new AuthManager(new JsonAuthStorage(Path.of("./plugins/mcauth")), BotIntegrationService.create(config, this::logInfo));

        server.getCommandManager().register("mcauth", new SimpleCommand() {
            @Override
            public void execute(Invocation invocation) {
                CommandSource source = invocation.source();
                source.sendMessage(Component.text("MCAuth пока поддерживает только регистрацию на сервере через ядро Paper/Bukkit."));
            }

            @Override
            public boolean hasPermission(Invocation invocation) {
                return true;
            }
        });
        logInfo("Velocity MCAuth инициализирован.");
    }

    private void logInfo(String message) {
        server.getConsoleCommandSource().sendMessage(Component.text(message));
    }
}
