/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import java.time.LocalDate;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseRepoKey {
  private String id;
  private LocalDate date;

  @Override
  public boolean equals(Object object) {
    if (object == null || getClass() != object.getClass()) return false;
    BaseRepoKey that = (BaseRepoKey) object;
    return Objects.equals(id, that.id) && Objects.equals(date, that.date);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, date);
  }
}
