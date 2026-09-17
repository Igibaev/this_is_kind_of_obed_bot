/* (C) 2024 Igibaev */
package kz.aday.bot.util;

import javax.inject.Singleton;

@Singleton
public class StringUtils {

  public static String escapeMarkdown(String text) {
    if (text == null) {
      return "";
    }

    return text.replace("\\", "\\\\")
        .replace("_", "\\_")
        .replace("*", "\\*")
        .replace("`", "\\`")
        .replace("[", "\\[");
  }
}
