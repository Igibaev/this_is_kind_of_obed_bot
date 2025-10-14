/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import kz.aday.bot.configuration.BotConfig;
import kz.aday.bot.model.Id;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseRepository<T extends Id> implements Repository<T> {

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final DateTimeFormatter DATE_FOLDER_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final String JSON = ".json";

  private final Path BASE_PATH;
  private final ObjectMapper objectMapper;
  private final Class<T> type;
  private final Map<BaseRepoKey, T> database;

  public BaseRepository(Map<BaseRepoKey, T> database, Class<T> type, String storagePath) {
    this.BASE_PATH = Path.of(BotConfig.getBotStorePath()).resolve(storagePath);
    this.objectMapper = getObjectMapper();
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
  public Collection<T> getAll() {
    return database.entrySet().stream()
        .filter(e -> e.getKey().getDate().isEqual(LocalDate.now()))
        .map(Map.Entry::getValue)
        .collect(Collectors.toList());
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
    createStorageIfNotExist(todayPath);

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

  private void createStorageIfNotExist(Path storage) {
    try {
      if (!Files.exists(storage)) {
        Files.createDirectories(storage);
        log.info("Created storage folder [{}]", storage);
      }
    } catch (IOException e) {
      log.error("Failure create storage [{}]", storage, e);
      throw new RuntimeException(e);
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
                  if (folderDate.isBefore(today.minusDays(7))) {
                    deleteRecursively(folder);
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

  private void deleteRecursively(Path path) throws IOException {
    if (Files.isDirectory(path)) {
      try (Stream<Path> entries = Files.list(path)) {
        for (Path entry : entries.toList()) {
          deleteRecursively(entry);
        }
      }
    }
    Files.deleteIfExists(path);
  }

  private Path getTodayFolderPath() {
    String dateFolder = LocalDate.now().format(DATE_FOLDER_FORMATTER);
    return BASE_PATH.resolve(dateFolder);
  }

  private static BaseRepoKey createRepoKey(String id) {
    return new BaseRepoKey(id, LocalDate.now());
  }

  private ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
    module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());
    objectMapper.registerModule(module);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return objectMapper;
  }

  public static class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      gen.writeString(value.format(FORMATTER));
    }
  }

  public static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      return LocalDateTime.parse(p.getText(), FORMATTER);
    }
  }
}
