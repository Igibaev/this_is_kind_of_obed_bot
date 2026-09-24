/* (C) 2024 Igibaev */
package kz.aday.bot.configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BotConfig {

  private BotConfig() {}

  private static String getProperty(String envVarName) {
    String value = System.getenv(envVarName);
    if (value == null || value.isBlank()) {
      value = System.getProperty(envVarName);
    }
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(
          "Required environment variable '" + envVarName + "' is not set or is empty.");
    }
    log.debug(
        "Successfully retrieved configuration for '{}' from environment variables.", envVarName);
    return value;
  }

  public static String getBotToken() {
    return getProperty("BOT_TOKEN");
  }

  public static String getBotName() {
    return getProperty("BOT_NAME");
  }

  public static String getBotTimeZone() {
    return getProperty("BOT_TIME_ZONE");
  }

  public static String getMainUserChatId() {
    return getProperty("BOT_MAIN_USER_CHAT_ID");
  }

  public static String getDatabaseUrl() {
    return getProperty("DB_URL");
  }

  public static String getDatabaseUsername() {
    return getProperty("DB_USERNAME");
  }

  public static String getDatabasePassword() {
    return getProperty("DB_PASSWORD");
  }
}
