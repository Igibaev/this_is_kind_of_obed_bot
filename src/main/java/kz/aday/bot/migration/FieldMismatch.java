/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FieldMismatch {
  private String id;
  private String field;
  private String sourceValue;
  private String destinationValue;
}
