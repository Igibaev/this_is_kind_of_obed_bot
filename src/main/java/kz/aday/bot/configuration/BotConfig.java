package kz.aday.bot.configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BotConfig {
    private static final String CONFIG_FILE = "application.properties";
    private static final String BOT_TOKEN = "bot.token";
    private static final String BOT_NAME = "bot.name";
    private static final String BOT_TIME_ZONE = "bot.time-zone";
    private static final String BOT_STORE_PATH = "bot.database.store";
    private static final Properties properties = new Properties();

    public BotConfig() {
        try (InputStream input = BotConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new RuntimeException("Failed to load properties file: " + CONFIG_FILE);
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load properties file: " + CONFIG_FILE, e);
        }
    }

    public static String getBotToken() {
        String token = properties.getProperty(BOT_TOKEN);
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("Token is not configured in " + CONFIG_FILE);
        }
        return token;
    }

    public static String getBotName() {
        String token = properties.getProperty(BOT_NAME);
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("Name is not configured in " + CONFIG_FILE);
        }
        return token;
    }

    public static String getBotTimeZone() {
        String token = properties.getProperty(BOT_TIME_ZONE);
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("Timezone is not configured in " + CONFIG_FILE);
        }
        return token;
    }

    public static String getBotStorePath() {
        String storage = properties.getProperty(BOT_STORE_PATH);
        if (storage == null || storage.isEmpty()) {
            throw new IllegalStateException("Timezone is not configured in " + BOT_STORE_PATH);
        }
        return storage;
    }
}
