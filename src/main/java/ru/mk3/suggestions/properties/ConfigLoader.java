package ru.mk3.suggestions.properties;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class ConfigLoader {

    public static Properties loadProperties(String fileName) {
        Properties prop = new Properties();

        try (InputStreamReader reader = new InputStreamReader(
                ConfigLoader.class.getClassLoader().getResourceAsStream(fileName),
                StandardCharsets.UTF_8)) {

            prop.load(reader);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load configuration from " + fileName, ex);
        }

        return prop;
    }

    private ConfigLoader() {
        throw new IllegalStateException("Utility class");
    }
}