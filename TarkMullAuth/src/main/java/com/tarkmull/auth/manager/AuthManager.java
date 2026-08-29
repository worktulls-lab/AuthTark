package com.tarkmull.auth.manager;

import com.tarkmull.auth.AuthPlugin;
import com.tarkmull.auth.model.PlayerAccount;
import com.tarkmull.auth.util.PasswordUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранит зарегистрированные аккаунты (players.yml) и то, кто из онлайн-игроков
 * прямо сейчас считается вошедшим в свой аккаунт.
 */
public class AuthManager {

    private final AuthPlugin plugin;
    private final File storageFile;
    private final YamlConfiguration storage;

    private final Map<UUID, PlayerAccount> accounts = new ConcurrentHashMap<>();
    private final Set<UUID> authenticated = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> loginAttempts = new ConcurrentHashMap<>();

    public AuthManager(AuthPlugin plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), plugin.getConfig().getString("storage-file", "players.yml"));
        this.storage = YamlConfiguration.loadConfiguration(storageFile);
        loadAll();
    }

    private void loadAll() {
        if (!storage.isConfigurationSection("accounts")) {
            return;
        }
        ConfigurationSection section = storage.getConfigurationSection("accounts");
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String path = "accounts." + key + ".";
                PlayerAccount acc = new PlayerAccount(uuid);
                acc.setName(storage.getString(path + "name"));
                acc.setPasswordHash(storage.getString(path + "hash"));
                acc.setSalt(storage.getString(path + "salt"));
                acc.setLastIp(storage.getString(path + "last-ip"));
                acc.setRegisteredAt(storage.getLong(path + "registered-at"));
                accounts.put(uuid, acc);
            } catch (IllegalArgumentException ignored) {
                // ключ не является валидным UUID — пропускаем
            }
        }
    }

    public void saveAll() {
        for (PlayerAccount acc : accounts.values()) {
            String path = "accounts." + acc.getUuid() + ".";
            storage.set(path + "name", acc.getName());
            storage.set(path + "hash", acc.getPasswordHash());
            storage.set(path + "salt", acc.getSalt());
            storage.set(path + "last-ip", acc.getLastIp());
            storage.set(path + "registered-at", acc.getRegisteredAt());
        }
        try {
            storage.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить players.yml: " + e.getMessage());
        }
    }

    public boolean isRegistered(UUID uuid) {
        return accounts.containsKey(uuid);
    }

    public boolean isAuthenticated(UUID uuid) {
        return authenticated.contains(uuid);
    }

    public void setAuthenticated(UUID uuid, boolean value) {
        if (value) {
            authenticated.add(uuid);
        } else {
            authenticated.remove(uuid);
        }
        loginAttempts.remove(uuid);
    }

    public PlayerAccount register(Player player, String password) {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hash(password, salt);

        PlayerAccount acc = new PlayerAccount(player.getUniqueId());
        acc.setName(player.getName());
        acc.setPasswordHash(hash);
        acc.setSalt(salt);
        acc.setLastIp(ip(player));
        acc.setRegisteredAt(System.currentTimeMillis());

        accounts.put(player.getUniqueId(), acc);
        saveAll();
        return acc;
    }

    public boolean checkPassword(Player player, String password) {
        PlayerAccount acc = accounts.get(player.getUniqueId());
        if (acc == null) {
            return false;
        }
        return PasswordUtil.verify(password, acc.getSalt(), acc.getPasswordHash());
    }

    /** Перехеширует пароль с новой солью и сохраняет. Ничего не делает, если аккаунта нет. */
    public void changePassword(Player player, String newPassword) {
        PlayerAccount acc = accounts.get(player.getUniqueId());
        if (acc == null) {
            return;
        }
        String salt = PasswordUtil.generateSalt();
        acc.setSalt(salt);
        acc.setPasswordHash(PasswordUtil.hash(newPassword, salt));
        saveAll();
    }

    public void updateLastIp(Player player) {
        PlayerAccount acc = accounts.get(player.getUniqueId());
        if (acc != null) {
            acc.setLastIp(ip(player));
            saveAll();
        }
    }

    public int registerFailedAttempt(UUID uuid) {
        return loginAttempts.merge(uuid, 1, Integer::sum);
    }

    public static String ip(Player player) {
        return (player.getAddress() != null && player.getAddress().getAddress() != null)
                ? player.getAddress().getAddress().getHostAddress()
                : "unknown";
    }
}
