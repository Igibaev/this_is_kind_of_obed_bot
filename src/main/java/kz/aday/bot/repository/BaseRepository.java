/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
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
  public T getById(String id) {
    return database.get(createRepoKey(id));
  }

  @Override
  public boolean existById(String id) {
    return database.containsKey(createRepoKey(id));
  }

  @Override
  public Collection<T> getAll(LocalDate today) {
    if (!Files.exists(BASE_PATH)) {
      log.info("Storage not exist [{}]", BASE_PATH);
      return List.of();
    }
    List<T> items = new ArrayList<>();
    try (Stream<Path> dateFolders = Files.list(BASE_PATH)) {
      for (Path dateFolder : dateFolders.toList()) {
        if (Files.isDirectory(dateFolder)
                && dateFolder
                .getFileName()
                .toString()
                .equals(today.format(DATE_FOLDER_FORMATTER))) {
          try (Stream<Path> files = Files.list(dateFolder)) {
            files.forEach(
                    path -> {
                      if (Files.isRegularFile(path) && path.toString().endsWith(JSON)) {
                        try {
                          T item = objectMapper.readValue(path.toFile(), type);
                          items.add(item);
                        } catch (IOException e) {
                          log.warn("Failed to parse [{}], skip.", path);
                        }
                      }
                    });
          }
        }
      }
      return items;
    } catch (IOException e) {
      log.error("Error loading storage", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void save(T t) {
    saveToStorage(t);
    database.put(createRepoKey(t.getId()), t);
  }

  @Override
  public void deleteById(String id) {
    database.remove(createRepoKey(id));
    deleteFromStorage(id);
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
                      database.put(createRepoKey(item.getId()), item);
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

  private void saveToStorage(T t) {
    Path todayPath = getTodayFolderPath();
    JsonFileStorageSupport.createStorageIfNotExist(todayPath);

    Path file = todayPath.resolve(t.getId() + JSON);
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

  private void deleteFromStorage(String id) {
    Path todayPath = getTodayFolderPath();
    if (!Files.exists(todayPath)) {
      log.info("No folder for today [{}]", todayPath);
      return;
    }

    Path file = todayPath.resolve(id + JSON);
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

  private Path getTodayFolderPath() {
    String dateFolder = LocalDate.now().format(DATE_FOLDER_FORMATTER);
    return BASE_PATH.resolve(dateFolder);
  }

  private static BaseRepoKey createRepoKey(String id) {
    return new BaseRepoKey(id, LocalDate.now());
  }
}
