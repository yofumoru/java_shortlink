package org.example.Shortlink.App;

import org.example.Shortlink.Core.Model.ShortLink;
import org.example.Shortlink.Core.Service.LinkRepository;
import org.example.Shortlink.Core.Service.LinkService;
import org.example.Shortlink.Storage.Config.AppConfig;
import org.example.Shortlink.Storage.Repo.SQLiteLinkRepository;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

public class Main {

    public static void main(String[] args) throws Exception {
        // UTF-8 для консоли
        System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
        System.setErr(new java.io.PrintStream(System.err) {
            @Override
            public void println(String x) {
                if (x != null && x.startsWith("WARNING:")) return;
                super.println(x);
            }
        });

        // Конфигурация и сервисы
        AppConfig config = AppConfig.load();
        LinkRepository repository = new SQLiteLinkRepository("shortlinks.db");
        LinkService service = new LinkService(repository, config);

        Thread.sleep(3500);
        clearConsole();
        // UUID пользователя
        UUID userId = UUID.randomUUID();
        System.out.println("Ваш UUID: " + userId);

        Scanner scanner = new Scanner(System.in, "UTF-8");
        while (true) {
            printMenu();
            System.out.print("Выберите действие: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": // Создать ссылку
                    System.out.print("Введите URL: ");
                    String url = scanner.nextLine().trim();
                    try {
                        ShortLink link = service.createLink(userId, url, null);
                        System.out.println("Короткая ссылка: " + link.getShortCode());
                    } catch (IllegalArgumentException e) {
                        System.out.println("Ошибка: " + e.getMessage());
                    }
                    pause();
                    break;

                case "2": // Открыть ссылку
                    System.out.print("Введите короткий код: ");
                    String code = scanner.nextLine().trim();
                    service.openLink(userId, code);
                    pause();
                    break;

                case "3": // Список ссылок
                    List<ShortLink> links = service.listUserLinks(userId);
                    printLinks(links);
                    pause();
                    break;

                case "4": // Редактировать ссылку
                    System.out.print("Введите короткий код ссылки для редактирования: ");
                    String editCode = scanner.nextLine().trim();

                    Optional<ShortLink> optionalLink = repository.findByShortCode(editCode);
                    if (optionalLink.isEmpty()) {
                        System.out.println("✖ Ссылка не найдена.");
                        pause();
                        break;
                    }

                    ShortLink linkToEdit = optionalLink.get();

                    if (!linkToEdit.getOwnerId().equals(userId)) {
                        System.out.println("✖ Нет прав на редактирование этой ссылки");
                        pause();
                        break;
                    }

                    // Ввод нового лимита кликов
                    Integer newClicks = null;
                    while (true) {
                        System.out.print("Новый лимит кликов (оставьте пустым, если без изменений): ");
                        String clicksInput = scanner.nextLine().trim();
                        if (clicksInput.isEmpty()) {
                            break; // пропускаем изменение
                        }
                        try {
                            newClicks = Integer.parseInt(clicksInput);
                            if (newClicks <= 0) {
                                System.out.println("⚠ Лимит кликов должен быть положительным. Попробуйте ещё раз.");
                            } else {
                                break; // корректно введено
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Ошибка: введён некорректный лимит кликов. Попробуйте ещё раз.");
                        }
                    }

                    // Ввод нового TTL
                    Long newTtlHours = null;
                    while (true) {
                        System.out.print("Новый TTL в часах (оставьте пустым, если без изменений): ");
                        String ttlInput = scanner.nextLine().trim();
                        if (ttlInput.isEmpty()) {
                            break; // пропускаем изменение
                        }
                        try {
                            newTtlHours = Long.parseLong(ttlInput);
                            if (newTtlHours <= 0) {
                                System.out.println("⚠ TTL должен быть положительным. Попробуйте ещё раз.");
                            } else {
                                break; // корректно введено
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Ошибка: введён некорректный TTL. Попробуйте ещё раз.");
                        }
                    }

                    try {
                        service.editLink(userId, linkToEdit.getShortCode(), newTtlHours, newClicks);
                    } catch (IllegalArgumentException e) {
                        System.out.println("Ошибка при редактировании ссылки: " + e.getMessage());
                    }

                    pause();
                    break;

                case "5": // Удалить ссылку
                    System.out.print("Введите короткий код ссылки для удаления: ");
                    String deleteCode = scanner.nextLine().trim();
                    service.deleteLink(userId, deleteCode);
                    pause();
                    break;

                case "9": // Очистить консоль
                    clearConsole();
                    break;

                case "0": // Выход
                    System.out.println("Выход...");
                    repository.close();
                    return;

                default:
                    System.out.println("Неверный выбор");
                    pause();
            }
        }
    }

    private static void printMenu() {
        System.out.println("===== ShortLink Menu =====");
        System.out.println("1. Создать ссылку");
        System.out.println("2. Открыть ссылку");
        System.out.println("3. Список ссылок");
        System.out.println("4. Редактировать ссылку");
        System.out.println("5. Удалить ссылку");
        System.out.println("9. Очистить консоль");
        System.out.println("0. Выход");
        System.out.println("==========================");
    }

    private static void printLinks(List<ShortLink> links) {
        if (links.isEmpty()) {
            System.out.println("Ссылок нет.");
            return;
        }
        for (ShortLink l : links) {
            String maxClicks = (l.getMaxClicks() == null) ? "∞" : l.getMaxClicks().toString();
            System.out.printf("%s -> %s [Переходов: %d/%s, TTL: %s, Активна: %s]%n",
                    l.getShortCode(),
                    l.getOriginalUrl(),
                    l.getCurrentClicks(),
                    maxClicks,
                    l.getExpiresAt(),
                    l.isActive());
        }
    }

    private static void pause() {
        System.out.println("\nНажмите Enter для продолжения...");
        new Scanner(System.in).nextLine();
    }

    private static void clearConsole() {
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
    }
}