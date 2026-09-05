/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.commandHandlers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.configuration.BotConfig;
import kz.aday.bot.model.User;
import kz.aday.bot.util.Messages;
import kz.aday.bot.util.ZipUtil;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
public class ExportDataCommandHandler extends AbstractHandler implements CommandHandler {
  private static final long TELEGRAM_MAX_UPLOAD_BYTES = 50L * 1024 * 1024;

  @Override
  public boolean canHandle(String command) {
    return command.startsWith("/exportdata");
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    User user = userService.findById(getChatId(update).toString());
    if (checkAdminRole(user, getMessageId(update), sender)) {
      return;
    }

    Path dataDir = Paths.get(BotConfig.getBotStorePath());
    File zipFile = null;
    try {
      String zipName =
          String.format(
              "data_backup_%s.zip",
              LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")));
      zipFile = new File(System.getProperty("java.io.tmpdir"), zipName);

      int filesZipped = ZipUtil.zipDirectory(dataDir, zipFile.toPath());

      if (filesZipped == 0) {
        sendMessage(user, Messages.DATA_EXPORT_EMPTY.getText(), getMessageId(update), sender);
        return;
      }
      if (zipFile.length() > TELEGRAM_MAX_UPLOAD_BYTES) {
        sendMessage(user, Messages.DATA_EXPORT_TOO_LARGE.getText(), getMessageId(update), sender);
        return;
      }

      sendZipFile(user, zipFile, filesZipped, getMessageId(update), sender);
      log.info("Data export ({} files) sent to chat {}.", filesZipped, user.getId());
    } catch (IOException e) {
      sendMessage(user, Messages.DATA_EXPORT_ERROR.getText(), getMessageId(update), sender);
      log.error("Error building data export zip: {}", e.getMessage(), e);
    } finally {
      if (zipFile != null && zipFile.exists() && !zipFile.delete()) {
        log.warn("Could not delete temp export zip: {}", zipFile.getAbsolutePath());
      }
    }
  }

  private void sendZipFile(
      User user, File file, int fileCount, Integer lastUserSendedMessageId, AbsSender sender) {
    SendDocument sendDocument = new SendDocument();
    sendDocument.setChatId(user.getChatId());
    sendDocument.setDocument(new InputFile(file));
    sendDocument.setCaption(
        String.format("Бэкап данных бота. Файлов: %d. Имя архива: %s", fileCount, file.getName()));

    try {
      List<Integer> messagesToDelete = new ArrayList<>();
      if (lastUserSendedMessageId != null) messagesToDelete.add(lastUserSendedMessageId);
      if (user.getLastMessageId() != null) messagesToDelete.add(user.getLastMessageId());
      Message message = sender.execute(sendDocument);
      user.setLastMessageId(message.getMessageId());
      userService.save(user);
      messageService.deleteMessage(user.getChatId(), messagesToDelete, sender);
    } catch (TelegramApiException e) {
      log.error(
          "Error sending data export zip to chat {}: {}", user.getId(), e.getMessage(), e);
    }
  }
}
