package com.tarkmull.auth.util;

import com.tarkmull.auth.AuthPlugin;
import com.tarkmull.auth.manager.AuthManager;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.time.Duration;

/**
 * Общая логика для команд и слушателя: "заморозка" игрока до входа
 * и завершение входа/регистрации (снятие эффектов, сессия, короткий сигнал).
 */
public final class AuthFlow {

    private AuthFlow() {
    }

    /** Вызывается при заходе игрока, которому нужно ввести /login или /register. */
    public static void beginProtection(AuthPlugin plugin, Player player, boolean needsRegister) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 250, false, false, false));

        String titleKey = needsRegister ? "messages.title-register" : "messages.title-login";
        String subKey = needsRegister ? "messages.subtitle-register" : "messages.subtitle-login";
        Msg.title(player,
                plugin.getConfig().getString(titleKey),
                plugin.getConfig().getString(subKey),
                Duration.ofMillis(300), Duration.ofSeconds(4), Duration.ofMillis(500));

        int timeoutSeconds = Math.max(5, plugin.getConfig().getInt("login.timeout-seconds", 60));
        BossBar bar = Msg.bossBar(
                plugin.getConfig().getString("messages.bossbar").replace("%time%", String.valueOf(timeoutSeconds)),
                BossBar.Color.RED, 1.0f);
        player.showBossBar(bar);
        plugin.trackBossBar(player, bar);

        int[] remaining = {timeoutSeconds};
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            remaining[0]--;
            BossBar current = plugin.getBossBar(player);
            if (current == null) {
                return;
            }
            if (remaining[0] <= 0) {
                player.kick(Msg.parse(plugin.getConfig().getString("messages.kick-timeout")));
                return;
            }
            current.progress(Math.max(0f, remaining[0] / (float) timeoutSeconds));
            current.name(Msg.parse(plugin.getConfig().getString("messages.bossbar")
                    .replace("%time%", String.valueOf(remaining[0]))));
            if (remaining[0] % 15 == 0) {
                if (needsRegister && plugin.getCaptchaManager().isRequired(player.getUniqueId())) {
                    Msg.actionBar(player, plugin.getConfig().getString("messages.reminder-captcha")
                            .replace("%question%", plugin.getCaptchaManager().getQuestion(player.getUniqueId())));
                } else {
                    Msg.actionBar(player, needsRegister
                            ? plugin.getConfig().getString("messages.reminder-register")
                            : plugin.getConfig().getString("messages.reminder-login"));
                }
            }
        }, 20L, 20L);
        plugin.trackKickTask(player, task.getTaskId());

        if (plugin.getConfig().getBoolean("branding.enabled", true)) {
            showBranding(plugin, player);
        }
    }

    /**
     * Небольшая надпись-водяной знак, которая гаснет сама через несколько секунд.
     * У ванильного клиента нет настоящего "верхнего левого угла" под управлением плагина —
     * ближайший штатный аналог это боковая панель (scoreboard sidebar), которая рисуется
     * в верхнем ПРАВОМ углу экрана. Используем её и аккуратно возвращаем игроку
     * тот scoreboard, что был у него до этого (чтобы не сломать другие плагины).
     */
    private static void showBranding(AuthPlugin plugin, Player player) {
        Scoreboard previous = player.getScoreboard();

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("tarkmull_brand", Criteria.DUMMY,
                Msg.parse(plugin.getConfig().getString("branding.text", "by TarkMull")));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(board);

        int seconds = Math.max(1, plugin.getConfig().getInt("branding.duration-seconds", 4));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && player.getScoreboard() == board) {
                player.setScoreboard(previous != null ? previous : Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }, seconds * 20L);
    }

    /** Вызывается после успешного /register или /login. */
    public static void completeAuth(AuthPlugin plugin, Player player, boolean startSession) {
        AuthManager auth = plugin.getAuthManager();
        auth.setAuthenticated(player.getUniqueId(), true);
        auth.updateLastIp(player);

        plugin.cancelKickTask(player);
        plugin.hideBossBar(player);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.SLOWNESS);

        if (startSession && plugin.getConfig().getBoolean("session.enabled", true)) {
            plugin.getSessionManager().startSession(player.getUniqueId(), AuthManager.ip(player));
        }

        // Единственный сигнал успеха — тихий щелчок. Подтверждение уже есть в чате (register/login-success).
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
    }

    /** Автовход по действующей сессии — та самая ситуация "пароль не нужен". */
    public static void autoLoginBySession(AuthPlugin plugin, Player player) {
        plugin.getAuthManager().setAuthenticated(player.getUniqueId(), true);
        plugin.getAuthManager().updateLastIp(player);

        // Единственное место, где нужно объяснить игроку, почему пароль не спросили — короткая строка в action bar.
        Msg.actionBar(player, plugin.getConfig().getString("messages.session-actionbar"));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.35f, 1.6f);
    }
}
