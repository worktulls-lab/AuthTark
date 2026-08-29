package com.tarkmull.auth.model;

/**
 * Активная сессия игрока — тот самый механизм "иногда пароль не нужен".
 * Пока сессия жива и (опционально) IP совпадает — игрок входит без /login.
 */
public class SessionData {

    private final String ip;
    private final long expiresAt;

    public SessionData(String ip, long expiresAt) {
        this.ip = ip;
        this.expiresAt = expiresAt;
    }

    public String getIp() {
        return ip;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
