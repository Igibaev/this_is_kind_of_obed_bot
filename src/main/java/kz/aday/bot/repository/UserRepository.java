package kz.aday.bot.repository;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import kz.aday.bot.configuration.BotConfig;
import kz.aday.bot.model.User;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class UserRepository implements Repository<User> {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String JSON = ".json";

    private final Path BASE_PATH;
    private final ObjectMapper objectMapper;
    private final Map<String, User> database;

    public UserRepository(Map<String, User> database, String storagePath) {
        this.BASE_PATH = Path.of(BotConfig.getBotStorePath()).resolve(storagePath);
        this.objectMapper = getObjectMapper();
        this.database = database;
        loadFromStorage();
    }

    @Override
    public User getById(String id) {
        return database.get(id);
    }

    @Override
    public boolean existById(String id) {
        return database.containsKey(id);
    }

    @Override
    public Collection<User> getAll() {
        return database.values();
    }

    @Override
    public void save(User user) {
        saveToStorage(user);
        database.put(user.getId(), user);
    }

    @Override
    public void clearLastWeek() {
        log.info("Clearing user storage is skiped");
    }

    @Override
    public void deleteById(String id) {
        deleteFromStorage(id);
        database.remove(id);
    }

    @Override
    public void clearStorage() {
        log.info("Clearing user storage is skiped");
    }

    private void loadFromStorage() {
        if (!Files.exists(BASE_PATH)) {
            log.info("Storage not exist [{}]", BASE_PATH);
            return;
        }

        log.info("Load storage [{}]", BASE_PATH);
        try (Stream<Path> jsonUsers = Files.list(BASE_PATH)) {
            for (Path filePath : jsonUsers.toList()) {
                if (Files.isRegularFile(filePath) && filePath.toString().endsWith(JSON)) {
                    try {
                        User user = objectMapper.readValue(filePath.toFile(), User.class);
                        database.put(user.getId(), user);
                    } catch (IOException e) {
                        log.warn("Failed to parse [{}], skip.", filePath);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error loading storage", e);
            throw new RuntimeException(e);
        }
    }

    private void saveToStorage(User user) {
        createStorageIfNotExist(BASE_PATH);

        Path file = BASE_PATH.resolve(user.getId() + JSON);
        try {
            if (Files.exists(file)) {
                Files.delete(file);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), user);
            log.info("Saved [{}]", file);
        } catch (Exception e) {
            log.error("Failure saving file [{}]", file, e);
            throw new RuntimeException(e);
        }
    }

    private void deleteFromStorage(String id) {
        Path file = BASE_PATH.resolve(id + JSON);
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

    private ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new BaseRepository.LocalDateTimeSerializer());
        module.addDeserializer(LocalDateTime.class, new BaseRepository.LocalDateTimeDeserializer());
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
