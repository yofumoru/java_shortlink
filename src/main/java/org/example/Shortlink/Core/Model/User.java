package org.example.Shortlink.Core.Model;

import java.time.Instant;
import java.util.UUID;

public class User {

    private final UUID id;
    private final Instant createdAt;

    public User(UUID id) {
        this.id = id;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}