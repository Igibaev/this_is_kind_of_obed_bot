/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import java.nio.file.Path;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DuplicateGroup {
  private String id;
  private Path winnerFile;
  private List<Path> loserFiles;
  private boolean conflict;
}
