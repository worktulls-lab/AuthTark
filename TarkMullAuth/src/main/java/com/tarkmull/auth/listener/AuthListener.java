package com.tarkmull.auth.listener;

import com.tarkmull.auth.AuthPlugin;
import com.tarkmull.auth.manager.AuthManager;
import com.tarkmull.auth.util.AuthFlow;
import com.tarkmull.auth.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class AuthListener implements Listener {

    private final AuthPlugin plugin;

    public AuthListener(AuthPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        AuthManager auth = plugin.getAuthManager();

        // На входе игрок всегда считается неавторизованным, пока не пройдёт проверку ниже
        auth.setAuthenticated(player.getUniqueId(), false);

        boolean registered = auth.isRegistered(player.getUniqueId());
        String ip = AuthManager.ip(player);

        boolean sessionsEnabled = plugin.getConfig().getBoolean("session.enabled", true);
        if (registered && sessionsEnabled && plugin.getSessionManager().hasValidSession(player.getUniqueId(), ip)) {
            // Тот самый случай: недавно уже входил с этого IP — пароль не спрашиваем
            AuthFlow.autoLoginBySession(plugin, player);
            return;
        }

        if (!registered && plugin.getCaptchaManager().isEnabled()) {
            // Новому игроку сперва нужно решить пример — только потом /register примет пароль
            String question = plugin.getCaptchaManager().generate(player);
            Msg.send(player, plugin.getConfig().getString("messages.captcha-prompt").replace("%question%", question));
        }

        AuthFlow.beginProtection(plugin, player, !registered);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.cancelKickTask(player);
        plugin.hideBossBar(player);
        plugin.getAuthManager().setAuthenticated(player.getUniqueId(), false);
        plugin.getCaptchaManager().clear(player.getUniqueId());
    }
}
