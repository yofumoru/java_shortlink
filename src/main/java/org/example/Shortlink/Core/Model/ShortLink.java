package org.example.Shortlink.Core.Model;

import java.time.Instant;
import java.util.UUID;

public class ShortLink {

    private final String shortCode;
    private final String originalUrl;
    private final UUID ownerId;

    private Integer maxClicks;
    private int currentClicks;

    private final Instant createdAt;
    private Instant expiresAt;

    private boolean active;

    /**
     * Конструктор для создания новой ссылки
     */
    public ShortLink(
            String shortCode,
            String originalUrl,
            UUID ownerId,
            int maxClicks,
            Instant expiresAt
    ) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.ownerId = ownerId;
        this.maxClicks = maxClicks;
        this.currentClicks = 0;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
        this.active = true;
    }

    /**
     * Используется только при восстановлении из БД
     */
    public ShortLink(
            String shortCode,
            String originalUrl,
            UUID ownerId,
            int maxClicks,
            int currentClicks,
            Instant createdAt,
            Instant expiresAt,
            boolean active
    ) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.ownerId = ownerId;
        this.maxClicks = maxClicks;
        this.currentClicks = currentClicks;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.active = active;
    }

    /* ===================== Бизнес-логика ===================== */

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isLimitReached() {
        return currentClicks >= maxClicks;
    }

    public boolean canBeUsed() {
        return active && !isExpired() && !isLimitReached();
    }

    /**
     * Регистрирует переход по ссылке
     * Блокирует ссылку при достижении лимита
     */
    public void registerClick() {
        if (!active) {
            return;
        }

        currentClicks++;

        if (currentClicks >= maxClicks) {
            active = false;
        }
    }

    /**
     * Принудительная деактивация (TTL, админ, и т.д.)
     */
    public void deactivate() {
        this.active = false;
    }

    /* =============== Восстановление из бд =============== */

    public void restoreState(int currentClicks, boolean active, Instant expiresAt) {
        this.currentClicks = currentClicks;
        this.active = active;
        this.expiresAt = expiresAt;
    }

    /* ===================== Сеттеры ===================== */

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setMaxClicks(int maxClicks) {
        this.maxClicks = maxClicks;
        if (currentClicks >= maxClicks) {
            active = false;
        }
    }

    /* ===================== Геттеры ===================== */

    public String getShortCode() {
        return shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Integer getMaxClicks() {
        return maxClicks;
    }

    public int getCurrentClicks() {
        return currentClicks;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    /* ===================== Для отладки и логов ===================== */

    @Override
    public String toString() {
        return "ShortLink{" +
                "shortCode='" + shortCode + '\'' +
                ", originalUrl='" + originalUrl + '\'' +
                ", ownerId=" + ownerId +
                ", currentClicks=" + currentClicks +
                "/" + maxClicks +
                ", expiresAt=" + expiresAt +
                ", active=" + active +
                '}';
    }
}