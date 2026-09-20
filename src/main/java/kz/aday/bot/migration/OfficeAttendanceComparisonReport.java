/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OfficeAttendanceComparisonReport {
  private int sourceCount;
  private int destinationCount;
  private Map<LocalDate, CountMismatch> perDateCountMismatches;
  private List<String> missingIds;
  private List<String> extraIds;
  private List<FieldMismatch> fieldMismatches;

  public boolean isMatch() {
    return sourceCount == destinationCount
        && perDateCountMismatches.isEmpty()
        && missingIds.isEmpty()
        && extraIds.isEmpty()
        && fieldMismatches.isEmpty();
  }
}
