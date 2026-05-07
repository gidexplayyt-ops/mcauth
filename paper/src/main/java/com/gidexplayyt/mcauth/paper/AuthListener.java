package com.gidexplayyt.mcauth.paper;

import com.gidexplayyt.mcauth.core.AccountData;
import com.gidexplayyt.mcauth.core.AuthManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthListener implements Listener {
    private final JavaPlugin plugin;
    private final AuthManager authManager;
    private final Map<UUID, BossBarSession> bossBarSessions = new HashMap<>();

    public AuthListener(JavaPlugin plugin, AuthManager authManager) {
        this.plugin = plugin;
        this.authManager = authManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (authManager.isAuthenticated(uuid)) {
            return;
        }
        if (authManager.isLicensedBypass(uuid)) {
            authManager.markAuthenticated(uuid);
            player.sendMessage("§aЛицензионный аккаунт пропущен без регистрации.");
            return;
        }
        player.sendMessage("§eДобро пожаловать. Используйте /mcauth register <пароль> или /mcauth login <пароль>.");
        if (authManager.hasLinkedAccount(uuid)) {
            player.sendMessage("§eВам необходимо подтвердить вход в боте. Используйте /mcauth confirm <код>.");
            authManager.requestLogin(uuid);
        }
        startBossBar(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopBossBar(event.getPlayer().getUniqueId());
    }

    private void startBossBar(Player player) {
        stopBossBar(player.getUniqueId());
        BossBar bossBar = Bukkit.createBossBar(new NamespacedKey(plugin, "auth_bossbar"), "Вход в течение 5 минут", BarColor.BLUE, BarStyle.SOLID);
        bossBar.addPlayer(player);
        BossBarSession session = new BossBarSession(bossBar, 300);
        session.taskId = new BukkitRunnable() {
            @Override
            public void run() {
                session.tick();
                bossBar.setProgress(session.progress);
                if (session.isFinished()) {
                    bossBar.removePlayer(player);
                    player.kickPlayer("Время на авторизацию истекло.");
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
        bossBarSessions.put(player.getUniqueId(), session);
    }

    private void stopBossBar(UUID playerUuid) {
        BossBarSession session = bossBarSessions.remove(playerUuid);
        if (session != null) {
            session.bossBar.removeAll();
            Bukkit.getScheduler().cancelTask(session.taskId);
        }
    }

    private static class BossBarSession {
        private final BossBar bossBar;
        private final int totalSeconds;
        private int ticks;
        private int taskId;
        private double progress = 1.0;

        BossBarSession(BossBar bossBar, int totalSeconds) {
            this.bossBar = bossBar;
            this.totalSeconds = totalSeconds;
            this.ticks = 0;
        }

        void tick() {
            ticks++;
            progress = Math.max(0.0, 1.0 - ticks / (double) totalSeconds);
        }

        boolean isFinished() {
            return ticks >= totalSeconds;
        }
    }
}
