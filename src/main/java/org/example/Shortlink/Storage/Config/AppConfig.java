package org.example.Shortlink.Storage.Config;

import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private int ttlHours;
    private int defaultMaxClicks;
    private int cleanupIntervalSeconds;

    public static AppConfig load() {
        try (InputStream is =
                     AppConfig.class.getClassLoader()
                             .getResourceAsStream("application.properties")) {

            Properties props = new Properties();
            props.load(is);

            AppConfig config = new AppConfig(1, 3, 60);
            config.ttlHours = Integer.parseInt(props.getProperty("link.ttl.hours"));
            config.defaultMaxClicks = Integer.parseInt(props.getProperty("link.default.maxClicks"));
            config.cleanupIntervalSeconds = Integer.parseInt(props.getProperty("cleanup.interval.seconds"));

            return config;

        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки конфигурации", e);
        }
    }

    public AppConfig(int ttlHours, int defaultMaxClicks, int cleanupIntervalSeconds) {
        this.ttlHours = ttlHours;
        this.defaultMaxClicks = defaultMaxClicks;
        this.cleanupIntervalSeconds = cleanupIntervalSeconds;
    }

    public int getTtlHours() {
        return ttlHours;
    }

    public int getDefaultMaxClicks() {
        return defaultMaxClicks;
    }

    public int getCleanupIntervalSeconds() {
        return cleanupIntervalSeconds;
    }
}