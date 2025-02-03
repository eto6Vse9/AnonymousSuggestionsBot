package ru.mk3.suggestions.cache;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class UserCacheManager {

    private static final long SUBSCRIPTION_CHECK_TIMEOUT_MINUTES = 60;
    private static final int MAX_MESSAGES_PER_HOUR = 5;
    private static final int MAX_SUBSCRIPTION_CHECKS_PER_DAY = 3;

    @Cacheable(value = "users", key = "#telegramId")
    public CachedUser getCachedUserByTelegramId(Long telegramId) {
        return null;
    }

    @CachePut(value = "users", key = "#telegramId")
    public CachedUser createCachedUser(Long telegramId) {
        return new CachedUser(telegramId);
    }

    @CachePut(value = "users", key = "#user.telegramId")
    public CachedUser updateUser(CachedUser user) {
        return user;
    }

    public boolean canSendMessage(CachedUser user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastMessageTime = user.getLastMessageTime();

        if (lastMessageTime == null || ChronoUnit.HOURS.between(lastMessageTime, now) >= 1) {
            user.setMessageCount(0);
            user.setLastMessageTime(now);
            return true;
        }

        return user.getMessageCount() < MAX_MESSAGES_PER_HOUR;
    }

    public boolean shouldCheckSubscription(CachedUser user) {
        LocalDateTime lastCheck = user.getLastSubscriptionCheck();
        return lastCheck == null ||
                ChronoUnit.MINUTES.between(lastCheck, LocalDateTime.now()) >= SUBSCRIPTION_CHECK_TIMEOUT_MINUTES;
    }

    public boolean canCheckSubscription(CachedUser user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastCheck = user.getLastSubscriptionCheck();

        if (lastCheck == null || ChronoUnit.DAYS.between(lastCheck, now) >= 1) {
            user.setSubscriptionCheckCount(0);
            user.setLastSubscriptionCheck(now);
            return true;
        }

        return user.getSubscriptionCheckCount() < MAX_SUBSCRIPTION_CHECKS_PER_DAY;
    }

}