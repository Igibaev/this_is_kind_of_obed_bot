package kz.aday.bot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque; // Используем ArrayDeque как более эффективную очередь
import java.util.Comparator;
import java.util.Deque; // Deque (double-ended queue) позволяет добавлять/удалять с обоих концов
import java.util.Optional;
import java.util.zip.GZIPInputStream; // Для чтения .gz файлов

// Этот метод можно добавить в ваш класс MyTelegramBot
public class LogFileHelper {

    private static final Logger logger = LoggerFactory.getLogger(LogFileHelper.class);
    private static final String LOG_FILE_PREFIX = "telegram-bot"; // Префикс имени лог-файла из logback.xml
    private static final String LOG_DIR_NAME = "logs"; // Имя директории логов из logback.xml
    public static final int DEFAULT_LINES_COUNT = 500; // Количество строк по умолчанию

    /**
     * Находит самый свежий лог-файл (включая .gz архивы) и извлекает из него указанное количество последних строк.
     * Создает временный файл с этими строками.
     *
     * @param linesCount Количество последних строк для извлечения. Если <= 0, используется DEFAULT_LINES_COUNT.
     * @return Объект File с последними строками лога, или null, если файл не найден или произошла ошибка.
     * @throws IOException Если произошла ошибка ввода/вывода.
     */
    public static File getLatestLogFileWithLastLines(int linesCount) throws IOException {
        int actualLinesCount = (linesCount <= 0) ? DEFAULT_LINES_COUNT : linesCount;

        Path logsDir = Paths.get(LOG_DIR_NAME);

        if (!Files.exists(logsDir)) {
            logger.warn("Папка с логами '{}' не найдена.", logsDir.toAbsolutePath());
            return null;
        }

        // 1. Находим самый свежий лог-файл (текущий .log или самый свежий .log.gz)
        Optional<Path> latestLogPath = Files.list(logsDir)
                .filter(p -> p.getFileName().toString().startsWith(LOG_FILE_PREFIX) &&
                        (p.getFileName().toString().endsWith(".log") ||
                                p.getFileName().toString().endsWith(".log.gz")))
                .max(Comparator.comparingLong(p -> {
                    try {
                        return Files.getLastModifiedTime(p).toMillis();
                    } catch (IOException e) {
                        logger.error("Не удалось получить время модификации файла {}: {}", p, e.getMessage());
                        return 0L;
                    }
                }));

        if (!latestLogPath.isPresent()) {
            logger.warn("В папке '{}' не найдено подходящих файлов логов.", logsDir.toAbsolutePath());
            return null;
        }

        Path sourceLogPath = latestLogPath.get();
        logger.info("Найден самый свежий лог-файл: {}", sourceLogPath.toAbsolutePath());

        Deque<String> lastLines = new ArrayDeque<>(actualLinesCount); // Двусторонняя очередь для хранения последних строк
        BufferedReader reader = null;
        InputStream fileStream = null;

        try {
            // Определяем, нужно ли распаковывать GZIP
            if (sourceLogPath.toString().endsWith(".gz")) {
                fileStream = new GZIPInputStream(Files.newInputStream(sourceLogPath));
                reader = new BufferedReader(new java.io.InputStreamReader(fileStream));
                logger.debug("Чтение сжатого лог-файла: {}", sourceLogPath.getFileName());
            } else {
                reader = new BufferedReader(new FileReader(sourceLogPath.toFile()));
                logger.debug("Чтение обычного лог-файла: {}", sourceLogPath.getFileName());
            }

            String line;
            while ((line = reader.readLine()) != null) {
                lastLines.add(line); // Добавляем строку в конец очереди
                if (lastLines.size() > actualLinesCount) {
                    lastLines.removeFirst(); // Если превышен размер, удаляем старейшую строку
                }
            }

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.error("Ошибка при закрытии BufferedReader: {}", e.getMessage(), e);
                }
            }
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (IOException e) {
                    logger.error("Ошибка при закрытии InputStream: {}", e.getMessage(), e);
                }
            }
        }

        if (lastLines.isEmpty()) {
            logger.warn("Извлечено 0 строк из лог-файла {}.", sourceLogPath.getFileName());
            return null;
        }

        // 2. Создаем временный файл для отправки
        // Имя файла будет "логи_бота_2025-06-23_Nстрок.txt"
        String tempFileName = String.format("logs_bot_%s_%d_lines.txt",
                LocalDate.now().format(DateTimeFormatter.ISO_DATE), actualLinesCount);
        File tempFile = new File(System.getProperty("java.io.tmpdir"), tempFileName);
        // Убедимся, что временная папка существует
        Files.createDirectories(tempFile.toPath().getParent());

        try (OutputStream os = new FileOutputStream(tempFile)) {
            for (String line : lastLines) {
                os.write((line + System.lineSeparator()).getBytes()); // Записываем строки
            }
            logger.info("Временный файл логов создан: {} ({} строк)", tempFile.getAbsolutePath(), lastLines.size());
            return tempFile;
        } catch (IOException e) {
            logger.error("Ошибка при записи временного файла логов {}: {}", tempFile.getAbsolutePath(), e.getMessage(), e);
            // Попытаемся удалить временный файл в случае ошибки
            if (tempFile.exists()) {
                tempFile.delete();
            }
            throw e; // Пробрасываем исключение дальше
        }
    }
}