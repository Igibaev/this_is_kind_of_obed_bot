/* (C) 2024 Igibaev */
package kz.aday.bot.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class IdTest {

  private static final String OWNER = "123456";
  private static final LocalDate DATE = LocalDate.of(2031, 1, 2);

  @Test
  void composeId_joinsOwnerAndDateWithSeparator() {
    assertEquals(OWNER + Id.COMPOSITE_ID_SEPARATOR + DATE, Id.composeId(OWNER, DATE));
  }

  @Test
  void ownerOf_returnsOwnerPart_ofComposedId() {
    assertEquals(OWNER, Id.ownerOf(Id.composeId(OWNER, DATE)));
  }

  @Test
  void ownerOf_returnsEnumOwner_ofComposedCityId() {
    assertEquals(City.ALMATA.name(), Id.ownerOf(Id.composeId(City.ALMATA, DATE)));
  }
}
