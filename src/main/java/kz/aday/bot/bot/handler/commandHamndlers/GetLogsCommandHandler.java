package kz.aday.bot.bot.handler.commandHamndlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.util.LogFileHelper;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;

@Slf4j
public class GetLogsCommandHandler extends AbstractHandler implements CommandHandler {
    @Override
    public boolean canHandle(String command) {
        return command.startsWith("/getlogs");
    }

    @Override
    public void handle(Update update, AbsSender sender) throws Exception {
        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();

        // 2. Парсинг количества строк из команды
        int linesCount = parseLinesCount(messageText);

        // 3. Получение файла логов
        File logFileToSend = null;
        try {
            logFileToSend = LogFileHelper.getLatestLogFileWithLastLines(linesCount);

            // 4. Отправка файла или сообщения об ошибке
            if (logFileToSend != null && logFileToSend.exists() && logFileToSend.isFile() && logFileToSend.length() > 0) {
                sendLogsFile(chatId, logFileToSend, linesCount, sender);
                log.info("Файл логов {} ({} строк) успешно отправлен в чат {}.",
                        logFileToSend.getName(), linesCount, chatId);
            } else {
                sendMessage(chatId, "Не удалось найти или сформировать файл логов с запрошенными строками.", sender);
                log.warn("Файл логов не найден или пуст после запроса {} строк.", linesCount);
            }
        } catch (IOException e) {
            sendMessage(chatId, "Произошла ошибка при подготовке файла логов.", sender);
            log.error("Ошибка при чтении или обработке файла логов: {}", e.getMessage(), e);
        } finally {
            // 5. Обязательно удаляем временный файл после использования
            if (logFileToSend != null && logFileToSend.exists()) {
                if (logFileToSend.delete()) {
                    log.debug("Временный файл логов удален: {}", logFileToSend.getAbsolutePath());
                } else {
                    log.warn("Не удалось удалить временный файл логов: {}", logFileToSend.getAbsolutePath());
                    // Можно добавить логику для периодической очистки временной директории,
                    // если файлы не удаляются сразу (например, из-за блокировок).
                }
            }
        }
    }

    /**
     * Парсит количество строк из команды /getlogs.
     * Например, для "/getlogs 100" вернет 100. Для "/getlogs" вернет дефолтное значение.
     */
    private int parseLinesCount(String command) {
        String[] parts = command.split(" ");
        if (parts.length > 1) {
            try {
                int count = Integer.parseInt(parts[1]);
                if (count > 0) { // Проверяем, что число положительное
                    return count;
                }
            } catch (NumberFormatException e) {
                log.warn("Неверный формат числа строк в команде /getlogs: {}", parts[1]);
            }
        }
        return LogFileHelper.DEFAULT_LINES_COUNT; // Возвращаем дефолт, если не указано или ошибка
    }

    /**
     * Отправляет текстовое сообщение пользователю.
     */
    private void sendMessage(long chatId, String text, AbsSender sender) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            sender.execute(message);
            log.info("Сообщение отправлено в чат {}: {}", chatId, text);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке текстового сообщения в чат {}: {}", chatId, e.getMessage(), e);
        }
    }

    /**
     * Отправляет файл логов пользователю.
     */
    private void sendLogsFile(long chatId, File file, int linesCount, AbsSender sender) {
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);
        sendDocument.setDocument(new InputFile(file));
        sendDocument.setCaption(String.format("Последние %d строк логов. Имя файла: %s", linesCount, file.getName()));

        try {
            sender.execute(sendDocument);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке файла логов {} в чат {}: {}", file.getName(), chatId, e.getMessage(), e);
        }
    }


}
