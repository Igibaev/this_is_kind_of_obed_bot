/* (C) 2024 Igibaev */
package kz.aday.bot.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TimeFormatterExtractor {
  private static final Pattern TIME_PATTERN =
      Pattern.compile("\\b(?:[0-1]?[0-9]|2[0-3]):[0-5][0-9]\\b");
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("[H:mm][HH:mm]");

  public static LocalTime extractTimes(String input) {
    if (input == null || input.isEmpty()) {
      return null;
    }

    Matcher matcher = TIME_PATTERN.matcher(input);
    if (matcher.find()) {
      String time = matcher.group();
      // Нормализуем формат: добавляем ведущий ноль для H:mm
      if (time.length() == 4 && time.charAt(1) == ':') {
        time = "0" + time;
      }
      return LocalTime.parse(time, FORMATTER);
    }
    return null;
  }
}
