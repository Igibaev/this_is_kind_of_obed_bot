package kz.aday.bot.configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class BotConfig {
    private static final String CONFIG_FILE = "application.properties";
    private static final String BOT_TOKEN = "bot.token";
    private static final String BOT_NAME = "bot.name";
    private static final String BOT_TIME_ZONE = "bot.time-zone";
    private final Properties properties;

    public BotConfig() {
        properties = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load properties file: " + CONFIG_FILE, e);
        }
    }

    public String getBotToken() {
        String token = properties.getProperty(BOT_TOKEN);
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("Token is not configured in " + CONFIG_FILE);
        }
        return token;
    }

    public String getBotName() {
        String token = properties.getProperty(BOT_NAME);
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("Name is not configured in " + CONFIG_FILE);
        }
        return token;
    }

    public String getBotTimeZone() {
        String token = properties.getProperty(BOT_TIME_ZONE);
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("Timezone is not configured in " + CONFIG_FILE);
        }
        return token;
    }
}
