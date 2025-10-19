package me.org2.phantomPacts;

import java.util.UUID;

public class PactOffer {

    private final UUID sender;
    private final UUID target;
    private final PactType type;
    private final long createdAt;

    public PactOffer(UUID sender, UUID target, PactType type) {
        this.sender = sender;
        this.target = target;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getSender() {
        return sender;
    }

    public UUID getTarget() {
        return target;
    }

    public PactType getType() {
        return type;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // Проверка истечения предложения (60 секунд)
    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > 60000;
    }
}