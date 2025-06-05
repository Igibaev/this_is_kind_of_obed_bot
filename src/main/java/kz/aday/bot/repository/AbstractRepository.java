package kz.aday.bot.repository;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import kz.aday.bot.model.Id;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public abstract class AbstractRepository<T extends Id> implements Repository<T> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final String BASE_PATH = System.getProperty("database.store"); // Путь к файлам
    private static final String JSON = ".json";
    private final ObjectMapper objectMapper;
    private final Class<T> type;
    private final Map<String, T> database;

    protected AbstractRepository(Map<String, T> database, Class<T> type) {
        objectMapper = getObjectMapper();
        this.database = database;
        this.type = type;
        loadFromStorage();
    }

    public abstract String getStoragePath();

    @Override
    public T getById(String id) {
        return database.get(id);
    }

    @Override
    public Collection<T> getAll() {
        return database.values();
    }

    @Override
    public void save(T t) {
        saveToStorage(t);
        database.put(t.getId(), t);
    }

    @Override
    public void clearAll() {
        database.clear();
        clearStorage();
    }

    private void loadFromStorage() {
        Path storage = Path.of(BASE_PATH + File.separator + getStoragePath());
        if (Files.exists(storage)) {
            log.info("Load storage [{}]", getStoragePath());
            try (Stream<Path> pathStorage = Files.list(storage)) {
                pathStorage.forEach(path -> {
                    if (path.toFile().isFile()) {
                        try {
                            T item = objectMapper.readValue(path.toFile(), type);
                            database.put(item.getId(), item);
                        } catch (IOException e) {
                            log.warn("Failed to parse json file [{}], skip.", path);
                            throw new RuntimeException(e);
                        }
                    } else {
                        log.warn("Path [{}] is not file, skip.", path);
                    }
                });
            } catch (IOException e) {
                log.error("Error apeard when loading storage ", e);
                throw new RuntimeException(e);
            }

        } else {
            log.info("Storage not exist [{}]", getStoragePath());
        }
    }

    private void saveToStorage(T t) {
        Path storage = Path.of(BASE_PATH + File.separator + getStoragePath());
        createStorageIfNotExist(storage);
        Path path = storage.resolve(Path.of(t.getId() + JSON));
        try {
            if (Files.exists(path)) {
                log.info("File exist [{}]", path);
                Files.delete(path);
                log.info("Delete file [{}]", path);
            }
            log.info("Create file and save [{}]", path);
            Files.createFile(path);
            objectMapper.writer()
                    .withDefaultPrettyPrinter()
                    .writeValue(path.toFile(), t);
        }  catch (Exception e) {
            log.error("Failure when save to storage ", e);
            throw new RuntimeException(e);
        }
    }

    private void createStorageIfNotExist(Path storage) {
        if (Files.exists(storage)) {
            log.info("Storage [{}] exist", storage);
        } else {
            log.info("Create storage [{}]", storage);
            try {
                Files.createDirectories(storage);
            } catch (IOException e) {
                log.error("Failure create storage", e);
                throw new RuntimeException(e);
            }
        }
    }


    private void clearStorage() {
        Path storage = Path.of(BASE_PATH + File.separator + getStoragePath());
        try {
            Files.deleteIfExists(storage);
            log.info("Storage was cleared [{}]", storage);
        } catch (IOException e) {
            log.error("Failure when clear storage ", e);
            throw new RuntimeException(e);
        }
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
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
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
