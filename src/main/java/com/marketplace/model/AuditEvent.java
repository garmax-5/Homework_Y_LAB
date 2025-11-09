package com.marketplace.model;

import java.time.Instant;

public class AuditEvent {
    public enum Level {INFO, ERROR}

    private final Instant timestamp;
    private final Long userId;
    private final String action;
    private final String details;
    private final Level level;

    public AuditEvent(Long userId, String action, String details, Level level) {
        this.timestamp = Instant.now();
        this.userId = userId;
        this.action = action;
        this.details = details;
        this.level = level;
    }

    public Instant getTimestamp() { return timestamp; }
    public Long getUserId() { return userId; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
    public Level getLevel() { return level; }

    @Override
    public String toString() {
        return String.format("[%s] %s user=%s action=%s details=%s",
                timestamp, level, userId, action, details);
    }
}

