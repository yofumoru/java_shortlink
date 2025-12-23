package org.example.Shortlink.Core.Service;

import org.example.Shortlink.Core.Model.ShortLink;
import org.example.Shortlink.Storage.Config.AppConfig;
import org.example.Shortlink.Util.ShortCodeGenerator;

import java.awt.Desktop;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class LinkService {

    private final LinkRepository repository;
    private final AppConfig config;

    public LinkService(LinkRepository repository, AppConfig config) {
        this.repository = repository;
        this.config = config;
    }

    /* ===================== CREATE ===================== */

    public ShortLink createLink(UUID userId, String originalUrl, Integer maxClicks) {
        validateUrl(originalUrl);

        int clicksLimit = maxClicks != null
                ? maxClicks
                : config.getDefaultMaxClicks();

        Instant expiresAt = Instant.now()
                .plus(config.getTtlHours(), ChronoUnit.HOURS);

        String shortCode = ShortCodeGenerator.generate(userId, originalUrl);

        ShortLink link = new ShortLink(
                shortCode,
                originalUrl,
                userId,
                clicksLimit,
                expiresAt
        );

        repository.save(link);

        System.out.println("✔ Ссылка создана");
        System.out.println("  TTL до: " + expiresAt);
        System.out.println("  Лимит переходов: " + clicksLimit);

        return link;
    }

    /* ===================== OPEN ===================== */

    public void openLink(UUID userId, String shortCode) {
        Optional<ShortLink> optional = repository.findByShortCode(shortCode);

        if (optional.isEmpty()) {
            System.out.println("✖ Ссылка не найдена");
            return;
        }

        ShortLink link = optional.get();

        // Проверка владельца
        if (!link.getOwnerId().equals(userId)) {
            System.out.println("✖ Нет прав на открытие этой ссылки");
            return;
        }

        if (link.isExpired()) {
            repository.delete(shortCode);
            System.out.println("⚠ Ссылка устарела и была удалена");
            return;
        }

        if (!link.isActive()) {
            System.out.println("⚠ Ссылка недоступна (лимит исчерпан)");
            return;
        }

        if (!link.canBeUsed()) {
            System.out.println("⚠ Ссылка недоступна");
            return;
        }

        try {
            Desktop.getDesktop().browse(new URI(link.getOriginalUrl()));
            link.registerClick();
            repository.update(link);

            System.out.println("⮕ Переход выполнен (" +
                    link.getCurrentClicks() + "/" + link.getMaxClicks() + ")");

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при открытии ссылки", e);
        }
    }

    /* ===================== LIST ===================== */

    public List<ShortLink> listUserLinks(UUID userId) {
        return repository.findAllByUser(userId);
    }

    /* ===================== EDIT ===================== */

    public void editLink(UUID userId, String shortCode, Long newTtlHours, Integer newMaxClicks) {
        Optional<ShortLink> optional = repository.findByShortCode(shortCode);

        if (optional.isEmpty()) {
            System.out.println("✖ Ссылка не найдена");
            return;
        }

        ShortLink link = optional.get();

        if (!link.getOwnerId().equals(userId)) {
            System.out.println("✖ Нет прав на редактирование этой ссылки");
            return;
        }

        // Обновление TTL
        if (newTtlHours != null) {
            Instant oldTtl = link.getExpiresAt();
            link.setExpiresAt(Instant.now().plus(newTtlHours, ChronoUnit.HOURS));
            System.out.println("✔ TTL обновлён: " + oldTtl + " → " + link.getExpiresAt());
        }

        // Обновление лимита кликов
        if (newMaxClicks != null) {
            int oldLimit = link.getMaxClicks();
            link.setMaxClicks(newMaxClicks);
            System.out.println("✔ Лимит кликов обновлён: " + oldLimit + " → " + newMaxClicks);
        }

        repository.update(link);
        System.out.println("✔ Ссылка обновлена");
    }

    /* ===================== DELETE ===================== */

    public void deleteLink(UUID userId, String shortCode) {
        Optional<ShortLink> optional = repository.findByShortCode(shortCode);

        if (optional.isEmpty()) {
            System.out.println("✖ Ссылка не найдена");
            return;
        }

        ShortLink link = optional.get();

        if (!link.getOwnerId().equals(userId)) {
            System.out.println("✖ Нет прав на удаление этой ссылки");
            return;
        }

        repository.delete(shortCode);
        System.out.println("✔ Ссылка удалена");
    }

    /* ===================== CLEANUP ===================== */

    public void cleanupExpiredLinks() {
        repository.deleteExpired();
    }

    /* ===================== VALIDATION ===================== */

    private void validateUrl(String url) {
        try {
            URI uri = new URI(url);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Некорректный URL");
        }
    }

    /* ===================== UPDATE ===================== */

    public void updateLink(ShortLink link) {
        repository.update(link);
    }
}