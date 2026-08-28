package com.tarkmull.auth;

import com.tarkmull.auth.command.CaptchaCommand;
import com.tarkmull.auth.command.ChangePasswordCommand;
import com.tarkmull.auth.command.LoginCommand;
import com.tarkmull.auth.command.RegisterCommand;
import com.tarkmull.auth.listener.AuthListener;
import com.tarkmull.auth.listener.ProtectionListener;
import com.tarkmull.auth.manager.AuthManager;
import com.tarkmull.auth.manager.CaptchaManager;
import com.tarkmull.auth.manager.SessionManager;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthPlugin extends JavaPlugin {

    private AuthManager authManager;
    private SessionManager sessionManager;
    private CaptchaManager captchaManager;

    private final Map<UUID, BossBar> activeBossBars = new HashMap<>();
    private final Map<UUID, Integer> kickTasks = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getDataFolder().mkdirs();

        this.authManager = new AuthManager(this);
        this.sessionManager = new SessionManager(this);
        this.captchaManager = new CaptchaManager(this);

        getCommand("register").setExecutor(new RegisterCommand(this));
        getCommand("login").setExecutor(new LoginCommand(this));
        getCommand("changepassword").setExecutor(new ChangePasswordCommand(this));
        getCommand("captcha").setExecutor(new CaptchaCommand(this));

        getServer().getPluginManager().registerEvents(new AuthListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);

        getLogger().info("TarkMullAuth включён — регистрация и вход готовы.");
    }

    @Override
    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            hideBossBar(player);
        }
        sessionManager.save();
        authManager.saveAll();
        getLogger().info("TarkMullAuth выключен, данные сохранены.");
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public CaptchaManager getCaptchaManager() {
        return captchaManager;
    }

    public void trackBossBar(Player player, BossBar bar) {
        activeBossBars.put(player.getUniqueId(), bar);
    }

    public BossBar getBossBar(Player player) {
        return activeBossBars.get(player.getUniqueId());
    }

    public void hideBossBar(Player player) {
        BossBar bar = activeBossBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    public void trackKickTask(Player player, int taskId) {
        cancelKickTask(player);
        kickTasks.put(player.getUniqueId(), taskId);
    }

    public void cancelKickTask(Player player) {
        Integer id = kickTasks.remove(player.getUniqueId());
        if (id != null) {
            getServer().getScheduler().cancelTask(id);
        }
    }
}
