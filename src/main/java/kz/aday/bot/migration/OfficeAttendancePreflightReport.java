/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OfficeAttendancePreflightReport {
  private List<DuplicateGroup> duplicateGroups;
  private List<DateMismatch> folderDateMismatches;
  private List<String> orphanChatIds;

  public boolean hasBlockingIssues() {
    return duplicateGroups.stream().anyMatch(DuplicateGroup::isConflict)
        || !orphanChatIds.isEmpty();
  }
}
