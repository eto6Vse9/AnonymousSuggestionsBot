package ru.mk3.suggestions.properties;

import java.util.Properties;

public class MessageConfig {
    public static final String START_MESSAGE;
    public static final String CONFIRMATION_MESSAGE;
    public static final String MUST_BE_MEMBER;
    public static final String MESSAGE_LIMIT_EXCEEDED;
    public static final String SUBSCRIPTION_CHECK_LIMIT_EXCEEDED;

    static {
        Properties prop = ConfigLoader.loadProperties("messages.properties");

        START_MESSAGE = prop.getProperty("start");
        CONFIRMATION_MESSAGE = prop.getProperty("confirmation");
        MUST_BE_MEMBER = prop.getProperty("must-be-member");
        MESSAGE_LIMIT_EXCEEDED = prop.getProperty("message-limit-exceeded");
        SUBSCRIPTION_CHECK_LIMIT_EXCEEDED = prop.getProperty("subscription-check-limit-exceeded");
    }

    private MessageConfig() {
        throw new IllegalStateException("Utility class");
    }
}