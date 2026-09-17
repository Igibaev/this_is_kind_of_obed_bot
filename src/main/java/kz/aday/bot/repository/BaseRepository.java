/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;
import kz.aday.bot.configuration.BotConfig;
import kz.aday.bot.model.Id;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseRepository<T extends Id> implements Repository<T> {

  private static final DateTimeFormatter DATE_FOLDER_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final String JSON = JsonFileStorageSupport.JSON;

  private final Path BASE_PATH;
  private final ObjectMapper objectMapper;
  private final Class<T> type;
  private final Map<BaseRepoKey, T> database;

  public BaseRepository(Map<BaseRepoKey, T> database, Class<T> type, String storagePath) {
    this.BASE_PATH = Path.of(BotConfig.getBotStorePath()).resolve(storagePath);
    this.objectMapper = JsonFileStorageSupport.createObjectMapper();
    this.database = database;
    this.type = type;
    loadFromStorage();
  }

  @Override
  public T getById(String id, LocalDate date) {
    BaseRepoKey key = createRepoKey(id, date);
    T cached = database.get(key);
    if (cached != null) {
      return cached;
    }
    T fromDisk = readFromDisk(id, date);
    if (fromDisk != null) {
      database.put(key, fromDisk);
    }
    return fromDisk;
  }

  @Override
  public boolean existById(String id, LocalDate date) {
    return getById(id, date) != null;
  }

  private T readFromDisk(String id, LocalDate date) {
    Path file = getFolderPath(date).resolve(id + JSON);
    if (!Files.exists(file)) {
      return null;
    }
    try {
      return objectMapper.readValue(file.toFile(), type);
    } catch (IOException e) {
      log.warn("Failed to parse [{}], skip.", file);
      return null;
    }
  }

  @Override
  public Collection<T> getAll(LocalDate date) {
    if (!Files.exists(BASE_PATH)) {
      log.info("Storage not exist [{}]", BASE_PATH);
      return List.of();
    }
    List<T> items = new ArrayList<>();
    try (Stream<Path> dateFolders = Files.list(BASE_PATH)) {
      for (Path dateFolder : dateFolders.toList()) {
        if (Files.isDirectory(dateFolder)
            && dateFolder.getFileName().toString().equals(date.format(DATE_FOLDER_FORMATTER))) {
          readFolder(dateFolder, items);
        }
      }
      return items;
    } catch (IOException e) {
      log.error("Error loading storage", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public Collection<T> getAll() {
    if (!Files.exists(BASE_PATH)) {
      log.info("Storage not exist [{}]", BASE_PATH);
      return List.of();
    }
    List<T> items = new ArrayList<>();
    try (Stream<Path> dateFolders = Files.list(BASE_PATH)) {
      for (Path dateFolder : dateFolders.toList()) {
        if (Files.isDirectory(dateFolder)) {
          readFolder(dateFolder, items);
        }
      }
      return items;
    } catch (IOException e) {
      log.error("Error loading storage", e);
      throw new RuntimeException(e);
    }
  }

  private void readFolder(Path dateFolder, List<T> items) throws IOException {
    LocalDate folderDate = parseFolderDate(dateFolder);
    try (Stream<Path> files = Files.list(dateFolder)) {
      files.forEach(
          path -> {
            if (Files.isRegularFile(path) && path.toString().endsWith(JSON)) {
              try {
                T item = objectMapper.readValue(path.toFile(), type);
                if (folderDate != null) {
                  item.backfillDateIfMissing(folderDate);
                }
                items.add(item);
              } catch (IOException e) {
                log.warn("Failed to parse [{}], skip.", path);
              }
            }
          });
    }
  }

  private LocalDate parseFolderDate(Path dateFolder) {
    try {
      return LocalDate.parse(dateFolder.getFileName().toString(), DATE_FOLDER_FORMATTER);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  @Override
  public void save(T t) {
    LocalDate date = t.getStorageDate();
    saveToStorage(t, date);
    database.put(createRepoKey(t.getId(), date), t);
  }

  @Override
  public void deleteById(String id, LocalDate date) {
    database.remove(createRepoKey(id, date));
    deleteFromStorage(id, date);
  }

  @Override
  public void clearLastWeek() {
    clearStorage();
  }

  @Override
  public void clearStorage() {
    clearOldFolders();
  }

  private void loadFromStorage() {
    if (!Files.exists(BASE_PATH)) {
      log.info("Storage not exist [{}]", BASE_PATH);
      return;
    }

    log.info("Load storage [{}]", BASE_PATH);
    try (Stream<Path> dateFolders = Files.list(BASE_PATH)) {
      for (Path dateFolder : dateFolders.toList()) {
        if (Files.isDirectory(dateFolder)
            && dateFolder
                .getFileName()
                .toString()
                .equals(LocalDate.now().format(DATE_FOLDER_FORMATTER))) {
          try (Stream<Path> files = Files.list(dateFolder)) {
            files.forEach(
                path -> {
                  if (Files.isRegularFile(path) && path.toString().endsWith(JSON)) {
                    try {
                      T item = objectMapper.readValue(path.toFile(), type);
                      database.put(createRepoKey(item.getId(), item.getStorageDate()), item);
                    } catch (IOException e) {
                      log.warn("Failed to parse [{}], skip.", path);
                    }
                  }
                });
          }
        }
      }
    } catch (IOException e) {
      log.error("Error loading storage", e);
      throw new RuntimeException(e);
    }
  }

  private void saveToStorage(T t, LocalDate date) {
    Path folderPath = getFolderPath(date);
    JsonFileStorageSupport.createStorageIfNotExist(folderPath);

    Path file = folderPath.resolve(t.getId() + JSON);
    try {
      if (Files.exists(file)) {
        Files.delete(file);
      }
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), t);
      log.info("Saved [{}]", file);
    } catch (Exception e) {
      log.error("Failure saving file [{}]", file, e);
      throw new RuntimeException(e);
    }
  }

  private void deleteFromStorage(String id, LocalDate date) {
    Path folderPath = getFolderPath(date);
    if (!Files.exists(folderPath)) {
      log.info("No folder for [{}]", folderPath);
      return;
    }

    Path file = folderPath.resolve(id + JSON);
    try {
      if (Files.exists(file)) {
        Files.delete(file);
        log.info("Deleted [{}]", file);
      } else {
        log.info("File [{}] not found", file);
      }
    } catch (IOException e) {
      log.error("Failure deleting file [{}]", file, e);
    }
  }

  private void clearOldFolders() {
    if (!Files.exists(BASE_PATH)) return;

    LocalDate today = LocalDate.now();
    try (Stream<Path> folders = Files.list(BASE_PATH)) {
      folders
          .filter(Files::isDirectory)
          .forEach(
              folder -> {
                String name = folder.getFileName().toString();
                try {
                  LocalDate folderDate = LocalDate.parse(name, DATE_FOLDER_FORMATTER);
                  if (folderDate.isBefore(today.minusDays(30))) {
                    JsonFileStorageSupport.deleteRecursively(folder);
                    log.info("Deleted old folder [{}]", folder);
                  }
                } catch (Exception e) {
                  log.warn("Skip non-date folder [{}]", folder);
                }
              });
    } catch (IOException e) {
      log.error("Failure clearing old storage", e);
      throw new RuntimeException(e);
    }
  }

  private Path getFolderPath(LocalDate date) {
    String dateFolder = date.format(DATE_FOLDER_FORMATTER);
    return BASE_PATH.resolve(dateFolder);
  }

  private static BaseRepoKey createRepoKey(String id, LocalDate date) {
    return new BaseRepoKey(id, date);
  }
}
