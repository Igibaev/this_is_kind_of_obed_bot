/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import java.util.function.Function;

public final class JdbcMappingSupport {

  private JdbcMappingSupport() {}

  public static <T> T mapEnum(String value, Function<String, T> parser) {
    return value == null ? null : parser.apply(value);
  }
}
