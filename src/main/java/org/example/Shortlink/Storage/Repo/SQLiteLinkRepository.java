package org.example.Shortlink.Storage.Repo;

import org.example.Shortlink.Core.Model.ShortLink;
import org.example.Shortlink.Core.Service.LinkRepository;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SQLiteLinkRepository implements LinkRepository {

    private final Connection connection;

    public SQLiteLinkRepository(String dbPath) {
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            initSchema();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка подключения к SQLite", e);
        }
    }

    private void initSchema() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS links (
                short_code TEXT PRIMARY KEY,
                original_url TEXT NOT NULL,
                owner_id TEXT NOT NULL,
                max_clicks INTEGER NOT NULL,
                current_clicks INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                expires_at INTEGER NOT NULL,
                active INTEGER NOT NULL
            )
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    @Override
    public void save(ShortLink link) {
        String sql = """
            INSERT INTO links (
                short_code, original_url, owner_id,
                max_clicks, current_clicks,
                created_at, expires_at, active
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, link.getShortCode());
            ps.setString(2, link.getOriginalUrl());
            ps.setString(3, link.getOwnerId().toString());
            ps.setInt(4, link.getMaxClicks());
            ps.setInt(5, link.getCurrentClicks());
            ps.setLong(6, link.getCreatedAt().getEpochSecond());
            ps.setLong(7, link.getExpiresAt().getEpochSecond());
            ps.setInt(8, link.isActive() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения ссылки", e);
        }
    }

    @Override
    public Optional<ShortLink> findByShortCode(String shortCode) {
        String sql = "SELECT * FROM links WHERE short_code = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, shortCode);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(map(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска ссылки", e);
        }
    }

    @Override
    public List<ShortLink> findAllByUser(UUID userId) {
        String sql = "SELECT * FROM links WHERE owner_id = ?";
        List<ShortLink> result = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId.toString());

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения ссылок пользователя", e);
        }
    }

    @Override
    public void update(ShortLink link) {
        String sql = """
        UPDATE links SET
            current_clicks = ?,
            max_clicks = ?,
            active = ?,
            expires_at = ?
        WHERE short_code = ?
    """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, link.getCurrentClicks());
            ps.setInt(2, link.getMaxClicks());
            ps.setInt(3, link.isActive() ? 1 : 0);
            ps.setLong(4, link.getExpiresAt().getEpochSecond());
            ps.setString(5, link.getShortCode());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления ссылки", e);
        }
    }


    @Override
    public void delete(String shortCode) {
        String sql = "DELETE FROM links WHERE short_code = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, shortCode);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка удаления ссылки", e);
        }
    }

    @Override
    public void deleteExpired() {
        String sql = "DELETE FROM links WHERE expires_at < ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            long now = Instant.now().getEpochSecond();
            stmt.setLong(1, now);
            int deleted = stmt.executeUpdate();
            System.out.println("Удалено протухших ссылок: " + deleted);
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при удалении протухших ссылок", e);
        }
    }


    private ShortLink map(ResultSet rs) throws SQLException {
        ShortLink link = new ShortLink(
                rs.getString("short_code"),
                rs.getString("original_url"),
                UUID.fromString(rs.getString("owner_id")),
                rs.getInt("max_clicks"),
                Instant.ofEpochSecond(rs.getLong("expires_at")) // OK
        );

        link.restoreState(
                rs.getInt("current_clicks"),
                rs.getInt("active") == 1,
                Instant.ofEpochSecond(rs.getLong("expires_at")) // ← правильно
        );

        return link;
    }

    @Override
    public void deleteAllLinks() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM links");
        }
    }

    @Override
    public void deleteAll() {
        String sql = "DELETE FROM links";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}