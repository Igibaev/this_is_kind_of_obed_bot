package kz.aday.bot.bot.handler.commandHamndlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.User;
import kz.aday.bot.util.LogFileHelper;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GetLogsCommandHandler extends AbstractHandler implements CommandHandler {
    @Override
    public boolean canHandle(String command) {
        return command.startsWith("/getlogs");
    }

    @Override
    public void handle(Update update, AbsSender sender) throws Exception {
        User user = userService.findById(getChatId(update).toString());
        String messageText = update.getMessage().getText();

        // 2. Парсинг количества строк из команды
        int linesCount = parseLinesCount(messageText);

        // 3. Получение файла логов
        File logFileToSend = null;
        try {
            logFileToSend = LogFileHelper.getLatestLogFileWithLastLines(linesCount);

            // 4. Отправка файла или сообщения об ошибке
            if (logFileToSend != null && logFileToSend.exists() && logFileToSend.isFile() && logFileToSend.length() > 0) {
                sendLogsFile(user, logFileToSend, linesCount, getMessageId(update), sender);
                log.info("Файл логов {} ({} строк) успешно отправлен в чат {}.",
                        logFileToSend.getName(), linesCount, user.getId());
            } else {
                sendMessage(user, "Не удалось найти или сформировать файл логов с запрошенными строками.", getMessageId(update), sender);
                log.warn("Файл логов не найден или пуст после запроса {} строк.", linesCount);
            }
        } catch (IOException e) {
            sendMessage(user, "Произошла ошибка при подготовке файла логов.", getMessageId(update), sender);
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
     * Отправляет файл логов пользователю.
     */
    private void sendLogsFile(User user, File file, int linesCount, Integer lastUserSendedMessageId, AbsSender sender) {
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(user.getChatId());
        sendDocument.setDocument(new InputFile(file));
        sendDocument.setCaption(String.format("Последние %d строк логов. Имя файла: %s", linesCount, file.getName()));

        try {
            List<Integer> messagesToDelete = new ArrayList<>();
            if (lastUserSendedMessageId != null) messagesToDelete.add(lastUserSendedMessageId);
            if (user.getLastMessageId() != null) messagesToDelete.add(user.getLastMessageId());
            Message message = sender.execute(sendDocument);
            user.setLastMessageId(message.getMessageId());
            userService.save(user);
            messageService.deleteMessage(user.getChatId(), messagesToDelete, sender);

        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке файла логов {} в чат {}: {}", file.getName(), user.getId(), e.getMessage(), e);
        }
    }


}
