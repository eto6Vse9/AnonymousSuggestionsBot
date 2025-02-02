package ru.mk3.suggestions.cache;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CachedUser {

    private Long telegramId;

    private boolean subscribed;
    private int subscriptionCheckCount;
    private LocalDateTime lastSubscriptionCheck;

    private int messageCount;
    private LocalDateTime lastMessageTime;

    public CachedUser(Long telegramId) {
        this.telegramId = telegramId;
    }

    public void incrementSubscriptionCheckCount() {
        this.subscriptionCheckCount++;
    }

    public void incrementMessageCount() {
        this.messageCount++;
    }

}