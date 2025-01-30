package ru.mk3.suggestions.config;

import java.util.Properties;

public class MessageConfig {
    public static final String START_MESSAGE;
    public static final String CONFIRMATION_MESSAGE;
    public static final String MUST_BE_MEMBER;

    static {
        Properties prop = ConfigLoader.loadProperties("messages.properties");
        START_MESSAGE = prop.getProperty("start");
        CONFIRMATION_MESSAGE = prop.getProperty("confirmation");
        MUST_BE_MEMBER = prop.getProperty("must-be-member");
    }

    private MessageConfig() {
        throw new IllegalStateException("Utility class");
    }
}