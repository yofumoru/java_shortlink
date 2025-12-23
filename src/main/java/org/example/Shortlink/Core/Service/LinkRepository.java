package org.example.Shortlink.Core.Service;

import org.example.Shortlink.Core.Model.ShortLink;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinkRepository extends AutoCloseable {

    void save(ShortLink link);

    Optional<ShortLink> findByShortCode(String shortCode);

    List<ShortLink> findAllByUser(UUID userId);

    void delete(String shortCode);

    void deleteAllLinks() throws Exception;

    void deleteExpired();

    void deleteAll();

    void update(ShortLink link);
}