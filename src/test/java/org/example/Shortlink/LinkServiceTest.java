package org.example.Shortlink;

import org.example.Shortlink.Core.Model.ShortLink;
import org.example.Shortlink.Core.Service.LinkRepository;
import org.example.Shortlink.Core.Service.LinkService;
import org.example.Shortlink.Storage.Repo.SQLiteLinkRepository;
import org.example.Shortlink.Storage.Config.AppConfig;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LinkServiceTest {

    static LinkRepository repo;
    static LinkService service;
    static UUID userId;

    @BeforeAll
    static void setup() {
        repo = new SQLiteLinkRepository("test.db");
        AppConfig config = new AppConfig(1, 3, 60);
        service = new LinkService(repo, config);
        userId = UUID.randomUUID();
    }

    @BeforeEach
    void clearDatabase() {
        repo.deleteAll();
    }

    @AfterAll
    static void cleanup() throws Exception {
        repo.close();
    }

    @Test
    void createLink_generatesUniqueLink() {
        System.out.println("\nТест 1: createLink_generatesUniqueLink — проверка уникальности");

        ShortLink link1 = service.createLink(userId, "https://example.com", null);
        System.out.println("Ссылка 1 создана: " + link1.getShortCode() + ", TTL: " + link1.getExpiresAt());

        ShortLink link2 = service.createLink(userId, "https://example.com", null);
        System.out.println("Ссылка 2 создана: " + link2.getShortCode() + ", TTL: " + link2.getExpiresAt());

        assertNotEquals(link1.getShortCode(), link2.getShortCode(), "Ссылки должны быть уникальны");
        System.out.println("Проверка уникальности пройдена: " + !link1.getShortCode().equals(link2.getShortCode()));
    }

    @Test
    void clickLimit_blocksAfterMaxClicks() {
        System.out.println("\nТест 2: clickLimit_blocksAfterMaxClicks — проверка лимита переходов");

        ShortLink link = service.createLink(userId, "https://example.com", 2);
        System.out.println("Ссылка создана: " + link.getShortCode() + ", Лимит: 2, TTL: " + link.getExpiresAt());

        System.out.println("Первый клик...");
        assertTrue(link.canBeUsed());
        link.registerClick();
        System.out.println("Текущее количество кликов: " + link.getCurrentClicks() + ", доступна? " + link.canBeUsed());

        System.out.println("Второй клик...");
        assertTrue(link.canBeUsed());
        link.registerClick();
        System.out.println("Текущее количество кликов: " + link.getCurrentClicks() + ", доступна? " + link.canBeUsed());

        System.out.println("Третий клик — должна заблокироваться");
        assertFalse(link.canBeUsed(), "Ссылка должна блокироваться после достижения лимита");
        System.out.println("Ссылка заблокирована: " + !link.canBeUsed());
    }

    @Test
    void expiredLink_isDeleted() {
        System.out.println("\nТест 3: expiredLink_isDeleted — проверка удаления \"протухшей\" ссылки");

        ShortLink link = service.createLink(userId, "https://example.com", null);
        System.out.println("Ссылка создана: " + link.getShortCode() + ", TTL: " + link.getExpiresAt());

        // Принудительно делаем ссылку протухшей
        link.setExpiresAt(Instant.now().minusSeconds(10));
        service.updateLink(link);
        System.out.println("Ссылка обновлена с новым expires_at: " + link.getExpiresAt());

        // Очистка протухших
        service.cleanupExpiredLinks();

        // Проверка
        List<ShortLink> links = service.listUserLinks(userId);
        boolean deleted = links.stream().noneMatch(l -> l.getShortCode().equals(link.getShortCode()));
        System.out.println("Удалена ли ссылка? " + deleted);

        assertTrue(deleted, "Протухшая ссылка должна быть удалена");
    }

    @Test
    void linkOwnership_isRespected() {
        System.out.println("\nТест 4: linkOwnership_isRespected — проверка прав доступа");

        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        // Пользователь 1 создаёт ссылку
        ShortLink link = service.createLink(user1, "https://example.com", 5);
        System.out.println("Ссылка создана пользователем 1: " + link.getShortCode());

        // Попытка пользователя 2 удалить ссылку
        System.out.println("Пользователь 2 пытается удалить ссылку...");
        service.deleteLink(user2, link.getShortCode());
        List<ShortLink> linksAfterDeleteAttempt = service.listUserLinks(user1);
        assertFalse(linksAfterDeleteAttempt.isEmpty(), "Ссылка не должна быть удалена чужим пользователем");

        // Попытка пользователя 2 редактировать ссылку
        System.out.println("Пользователь 2 пытается редактировать ссылку...");
        service.editLink(user2, link.getShortCode(), 3600L, 10);
        ShortLink linkAfterEditAttempt = service.listUserLinks(user1).get(0);
        assertEquals(5, linkAfterEditAttempt.getMaxClicks(), "Лимит кликов не должен измениться чужим пользователем");

        // Пользователь 1 редактирует ссылку — должно сработать
        System.out.println("Пользователь 1 редактирует ссылку...");
        service.editLink(user1, link.getShortCode(), 7200L, 15);
        ShortLink linkAfterOwnerEdit = service.listUserLinks(user1).get(0);
        assertEquals(15, linkAfterOwnerEdit.getMaxClicks(), "Лимит кликов должен обновиться владельцем");

        System.out.println("Проверка прав доступа пройдена");
    }

    @Test
    void sameUrlDifferentUsers_generatesUniqueShortCodes() {
        System.out.println("\nТест 5: sameUrlDifferentUsers_generatesUniqueShortCodes — проверка уникальности по пользователю");

        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        // Оба пользователя создают ссылку на один и тот же URL
        ShortLink linkUser1 = service.createLink(user1, "https://example.com", null);
        System.out.println("Ссылка пользователя 1: " + linkUser1.getShortCode());

        ShortLink linkUser2 = service.createLink(user2, "https://example.com", null);
        System.out.println("Ссылка пользователя 2: " + linkUser2.getShortCode());

        // Коды должны различаться
        assertNotEquals(linkUser1.getShortCode(), linkUser2.getShortCode(),
                "Ссылки разных пользователей на один и тот же URL должны иметь разные короткие коды");

        System.out.println("Проверка уникальности для разных пользователей пройдена");
    }
}
