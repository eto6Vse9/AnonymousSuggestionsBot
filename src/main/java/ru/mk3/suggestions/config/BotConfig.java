package ru.mk3.suggestions.config;

import java.util.List;
import java.util.Properties;

public class BotConfig {
    public static final String BOT_TOKEN;
    public static final String BOT_USERNAME;
    public static final List<String> BOT_ADMINS;
    public static final String TARGET_CHANNEL;

    static {
        Properties prop = ConfigLoader.loadProperties("config.properties");
        BOT_TOKEN = prop.getProperty("token");
        BOT_USERNAME = prop.getProperty("username");
        BOT_ADMINS = List.of(prop.getProperty("admins", "").split(","));
        TARGET_CHANNEL = prop.getProperty("target-channel");
    }

    private BotConfig() {
        throw new IllegalStateException("Utility class");
    }
}