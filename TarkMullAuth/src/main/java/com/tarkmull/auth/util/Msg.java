package com.tarkmull.auth.util;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * Мелкие помощники, чтобы весь плагин говорил красиво (MiniMessage, титры, boss bar),
 * а не голым white-текстом в чат.
 */
public final class Msg {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Msg() {
    }

    public static Component parse(String raw) {
        return MM.deserialize(raw == null ? "" : raw);
    }

    public static void send(Player player, String raw) {
        player.sendMessage(parse(raw));
    }

    public static void actionBar(Player player, String raw) {
        player.sendActionBar(parse(raw));
    }

    public static void title(Player player, String titleRaw, String subtitleRaw,
                              Duration fadeIn, Duration stay, Duration fadeOut) {
        Title.Times times = Title.Times.times(fadeIn, stay, fadeOut);
        Title title = Title.title(parse(titleRaw), parse(subtitleRaw), times);
        player.showTitle(title);
    }

    public static BossBar bossBar(String raw, BossBar.Color color, float progress) {
        return BossBar.bossBar(parse(raw), progress, color, BossBar.Overlay.PROGRESS);
    }
}
