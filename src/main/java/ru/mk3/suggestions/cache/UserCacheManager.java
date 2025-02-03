package ru.mk3.suggestions.cache;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class UserCacheManager {

    private static final int MAX_MESSAGES_PER_HOUR = 5;
    private static final int MAX_SUBSCRIPTION_CHECKS_PER_DAY = 4;

    @Cacheable(value = "users", key = "#telegramId")
    public CachedUser getCachedUserByTelegramId(Long telegramId) {
        return new CachedUser(telegramId);
    }

    @CachePut(value = "users", key = "#user.telegramId")
    public CachedUser updateUser(CachedUser user) {
        return user;
    }

    public boolean canSendMessage(CachedUser user) {
        LocalDateTime lastMessageTime = user.getLastMessageTime();

        if (lastMessageTime == null) {
            return true;
        }

        if (ChronoUnit.HOURS.between(lastMessageTime, LocalDateTime.now()) >= 1) {
            user.setMessageCount(0);
            updateUser(user);

            return true;
        }

        return user.getMessageCount() < MAX_MESSAGES_PER_HOUR;
    }

    public boolean canCheckSubscription(CachedUser user) {
        LocalDateTime lastCheck = user.getLastSubscriptionCheck();

        if (lastCheck == null) {
            return true;
        }

        if (ChronoUnit.DAYS.between(lastCheck, LocalDateTime.now()) >= 1) {
            user.setSubscriptionCheckCount(0);
            updateUser(user);

            return true;
        }

        return user.getSubscriptionCheckCount() < MAX_SUBSCRIPTION_CHECKS_PER_DAY;
    }

}