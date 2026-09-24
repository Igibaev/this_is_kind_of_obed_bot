/* (C) 2024 Igibaev */
package kz.aday.bot.testsupport;

import javax.sql.DataSource;
import kz.aday.bot.model.User;
import kz.aday.bot.repository.UserRepository;

public final class TestUsers {

  private TestUsers() {}

  public static void ensureExists(DataSource dataSource, long chatId, String preferedName) {
    new UserRepository(dataSource)
        .save(User.builder().chatId(chatId).preferedName(preferedName).build());
  }
}
