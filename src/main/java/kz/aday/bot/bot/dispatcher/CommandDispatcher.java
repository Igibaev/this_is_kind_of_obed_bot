package kz.aday.bot.bot.dispatcher;

import java.util.HashSet;
import kz.aday.bot.bot.handler.commandHamndlers.CommandHandler;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Slf4j
public class CommandDispatcher extends AbstractDispatcher<CommandHandler> {

  public CommandDispatcher() {
    super(new HashSet<>());
  }

  public void dispatch(Update update, AbsSender sender) throws Exception {
    log.info(
        "Dispatching command for update, chatId: {}",
        update != null && update.getMessage() != null
            ? update.getMessage().getChatId()
            : "unknown");
    if (update == null
        || update.getMessage() == null
        || update.getMessage().getText() == null
        || update.getMessage().getChatId() == null) {
      log.error("Invalid update or command text");
      throw new IllegalArgumentException("Некорректный update или текст команды");
    }

    String text = update.getMessage().getText().trim();
    log.debug("Processing command: [{}]", text);

    for (CommandHandler handler : handlers) {
      if (handler.canHandle(text)) {
        log.info("Command [{}] handler: [{}]", text, handler);
        handler.handle(update, sender);
        log.info("Command handled successfully: [{}]", text);
        return; // Выходим после обработки команды
      }
    }
    log.warn("Unknown command: [{}]", text);
  }
}
