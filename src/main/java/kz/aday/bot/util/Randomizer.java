/* (C) 2024 Igibaev */
package kz.aday.bot.util;

import java.util.Random;
import javax.inject.Singleton;

@Singleton
public class Randomizer {
  private static final Random random = new Random();

  private Randomizer() {}

  public static Random getRandom() {
    return random;
  }
}
