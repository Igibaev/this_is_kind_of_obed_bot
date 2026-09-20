/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import kz.aday.bot.model.City;
import kz.aday.bot.model.OfficeAttendance;
import kz.aday.bot.repository.JsonFileStorageSupport;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
final class OfficeAttendanceSourceScanner {

  private static final DateTimeFormatter DATE_FOLDER_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private OfficeAttendanceSourceScanner() {}

  static List<SourceEntry> scan(Path basePath) {
    if (!Files.exists(basePath)) {
      return List.of();
    }
    ObjectMapper objectMapper = JsonFileStorageSupport.createObjectMapper();
    List<SourceEntry> entries = new ArrayList<>();
    try (Stream<Path> dateFolders = Files.list(basePath)) {
      for (Path dateFolder : dateFolders.toList()) {
        if (!Files.isDirectory(dateFolder)) {
          continue;
        }
        LocalDate folderDate = parseFolderDate(dateFolder);
        if (folderDate == null) {
          continue;
        }
        readFolder(dateFolder, folderDate, objectMapper, entries);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return entries;
  }

  static SourceEntry pickWinner(List<SourceEntry> group) {
    return group.stream()
        .filter(sourceEntry -> sourceEntry.getFolderDate().equals(sourceEntry.getEffectiveDate()))
        .findFirst()
        .orElse(group.get(0));
  }

  private static void readFolder(
      Path dateFolder, LocalDate folderDate, ObjectMapper objectMapper, List<SourceEntry> entries)
      throws IOException {
    try (Stream<Path> files = Files.list(dateFolder)) {
      for (Path file : files.toList()) {
        if (!Files.isRegularFile(file) || !file.toString().endsWith(JsonFileStorageSupport.JSON)) {
          continue;
        }
        try {
          OfficeAttendance attendance =
              objectMapper.readValue(file.toFile(), OfficeAttendance.class);
          LocalDate fieldDate =
              attendance.getDate() != null ? LocalDate.parse(attendance.getDate()) : null;
          LocalDate effectiveDate = fieldDate != null ? fieldDate : folderDate;
          entries.add(
              new SourceEntry(
                  file,
                  attendance.getChatId(),
                  attendance.getCity(),
                  attendance.getWillCome(),
                  folderDate,
                  fieldDate,
                  effectiveDate));
        } catch (IOException e) {
          log.warn("Failed to parse [{}], skip.", file);
        }
      }
    }
  }

  private static LocalDate parseFolderDate(Path dateFolder) {
    try {
      return LocalDate.parse(dateFolder.getFileName().toString(), DATE_FOLDER_FORMATTER);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  @Data
  @AllArgsConstructor
  static class SourceEntry {
    private Path path;
    private String chatId;
    private City city;
    private Boolean willCome;
    private LocalDate folderDate;
    private LocalDate fieldDate;
    private LocalDate effectiveDate;

    String effectiveId() {
      return chatId + "_" + effectiveDate;
    }
  }
}
