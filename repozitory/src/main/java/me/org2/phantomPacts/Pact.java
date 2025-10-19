package me.org2.phantomPacts;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Pact {

    private final UUID player1;
    private final UUID player2;
    private final PactType type;
    private final long createdAt;
    private final long expiresAt;
    private boolean active;

    public Pact(UUID player1, UUID player2, PactType type, long duration) {
        this.player1 = player1;
        this.player2 = player2;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = createdAt + (duration * 1000); // duration в секундах
        this.active = true;
    }

    // Конструктор для загрузки из файла
    public Pact(UUID player1, UUID player2, PactType type, long createdAt, long expiresAt, boolean active) {
        this.player1 = player1;
        this.player2 = player2;
        this.type = type;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.active = active;
    }

    public UUID getPlayer1() {
        return player1;
    }

    public UUID getPlayer2() {
        return player2;
    }

    public PactType getType() {
        return type;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // Проверка истечения пакта
    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }

    // Получить оставшееся время в секундах
    public long getRemainingTime() {
        long remaining = (expiresAt - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    // Проверка, участвует ли игрок в этом пакте
    public boolean involves(UUID playerUuid) {
        return player1.equals(playerUuid) || player2.equals(playerUuid);
    }

    // Проверка, является ли пара игроков участниками пакта
    public boolean involves(UUID p1, UUID p2) {
        return (player1.equals(p1) && player2.equals(p2)) ||
                (player1.equals(p2) && player2.equals(p1));
    }

    // Получить партнёра по пакту
    public UUID getPartner(UUID playerUuid) {
        if (player1.equals(playerUuid)) {
            return player2;
        } else if (player2.equals(playerUuid)) {
            return player1;
        }
        return null;
    }

    // Получить имя партнёра
    public String getPartnerName(UUID playerUuid) {
        UUID partnerUuid = getPartner(playerUuid);
        if (partnerUuid == null) return "Unknown";

        Player partner = Bukkit.getPlayer(partnerUuid);
        if (partner != null) {
            return partner.getName();
        }
        return Bukkit.getOfflinePlayer(partnerUuid).getName();
    }

    // Форматированное оставшееся время
    public String getFormattedRemainingTime() {
        long seconds = getRemainingTime();

        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("д ");
        if (hours > 0) sb.append(hours).append("ч ");
        if (minutes > 0) sb.append(minutes).append("м ");
        if (secs > 0 || sb.length() == 0) sb.append(secs).append("с");

        return sb.toString().trim();
    }

    @Override
    public String toString() {
        return String.format("Pact[%s <-> %s, Type: %s, Active: %s, Expires: %s]",
                Bukkit.getOfflinePlayer(player1).getName(),
                Bukkit.getOfflinePlayer(player2).getName(),
                type.getName(),
                active,
                getFormattedRemainingTime());
    }
}