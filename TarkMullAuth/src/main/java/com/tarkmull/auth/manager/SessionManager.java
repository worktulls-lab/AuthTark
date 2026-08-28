package com.tarkmull.auth.manager;

import com.tarkmull.auth.AuthPlugin;
import com.tarkmull.auth.model.SessionData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Это и есть механизм "иногда пароль не нужен": пока у игрока есть свежая
 * сессия (обычно — тот же IP, что и в прошлый раз, в пределах N минут),
 * /login не требуется — вход происходит автоматически.
 * Сессии переживают перезапуск сервера (sessions.yml).
 */
public class SessionManager {

    private final AuthPlugin plugin;
    private final File file;
    private final Map<UUID, SessionData> sessions = new ConcurrentHashMap<>();

    public SessionManager(AuthPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "sessions.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (!yaml.isConfigurationSection("sessions")) {
            return;
        }
        ConfigurationSection section = yaml.getConfigurationSection("sessions");
        long now = System.currentTimeMillis();
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String path = "sessions." + key + ".";
                String ip = yaml.getString(path + "ip");
                long expires = yaml.getLong(path + "expires");
                if (expires > now) {
                    sessions.put(uuid, new SessionData(ip, expires));
                }
            } catch (IllegalArgumentException ignored) {
                // не UUID — пропускаем
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, SessionData> entry : sessions.entrySet()) {
            if (entry.getValue().isExpired()) {
                continue;
            }
            String path = "sessions." + entry.getKey() + ".";
            yaml.set(path + "ip", entry.getValue().getIp());
            yaml.set(path + "expires", entry.getValue().getExpiresAt());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить sessions.yml: " + e.getMessage());
        }
    }

    public boolean hasValidSession(UUID uuid, String ip) {
        SessionData data = sessions.get(uuid);
        if (data == null) {
            return false;
        }
        if (data.isExpired()) {
            sessions.remove(uuid);
            return false;
        }
        boolean requireSameIp = plugin.getConfig().getBoolean("session.require-same-ip", true);
        return !requireSameIp || data.getIp().equals(ip);
    }

    public void startSession(UUID uuid, String ip) {
        long minutes = Math.max(1, plugin.getConfig().getLong("session.duration-minutes", 15));
        long expires = System.currentTimeMillis() + minutes * 60_000L;
        sessions.put(uuid, new SessionData(ip, expires));
    }

    public void clearSession(UUID uuid) {
        sessions.remove(uuid);
    }
}
