/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MenuServiceTest {

  private Repository<Menu> repository;
  private MenuService service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    repository = mock(Repository.class);
    service = new MenuService(repository);
  }

  @Test
  void save_setsDateFromCityCurrentOrderDate_whenDateNotAlreadySet() {
    Menu menu = new Menu();
    menu.setCity(City.ALMATA);

    service.save(menu);

    assertEquals(City.ALMATA.getCurrentOrderDate().toString(), menu.getDate());
  }

  @Test
  void save_preservesAlreadySetDate_insteadOfRecomputingIt() {
    Menu menu = new Menu();
    menu.setCity(City.ALMATA);
    menu.setDate("2031-03-01");

    service.save(menu);

    assertEquals("2031-03-01", menu.getDate());
  }

  @Test
  void save_deletesExistingDeadlineMenu_beforeSavingReplacement() {
    Menu existing = new Menu();
    existing.setCity(City.ASTANA);
    existing.setStatus(Status.DEADLINE);
    when(repository.getById(eq(City.ASTANA.toString()), any(LocalDate.class))).thenReturn(existing);

    Menu replacement = new Menu();
    replacement.setCity(City.ASTANA);
    service.save(replacement);

    verify(repository, times(1)).deleteById(eq(City.ASTANA.toString()), any(LocalDate.class));
    verify(repository).save(replacement);
  }

  @Test
  void save_doesNotDelete_whenExistingMenuStatusIsNotDeadline() {
    Menu existing = new Menu();
    existing.setCity(City.ASTANA);
    existing.setStatus(Status.READY);
    when(repository.getById(eq(City.ASTANA.toString()), any(LocalDate.class))).thenReturn(existing);

    Menu replacement = new Menu();
    replacement.setCity(City.ASTANA);
    service.save(replacement);

    verify(repository, never()).deleteById(any(), any());
    verify(repository).save(replacement);
  }

  @Test
  void findAll_returnsOnlyCitiesWithACurrentMenu_skippingCitiesWithNone() {
    Menu almatyMenu = new Menu();
    almatyMenu.setCity(City.ALMATA);
    when(repository.getById(eq(City.ALMATA.toString()), any(LocalDate.class)))
        .thenReturn(almatyMenu);
    when(repository.getById(eq(City.ASTANA.toString()), any(LocalDate.class))).thenReturn(null);
    when(repository.getById(eq(City.KARAGANDA.toString()), any(LocalDate.class))).thenReturn(null);

    assertEquals(List.of(almatyMenu), List.copyOf(service.findAll()));
  }

  @Test
  void findById_returnsNull_whenRepositoryHasNoMenuForCurrentOrderDate() {
    when(repository.getById(eq(City.ALMATA.toString()), any(LocalDate.class))).thenReturn(null);

    assertNull(service.findById(City.ALMATA.toString()));
  }
}
