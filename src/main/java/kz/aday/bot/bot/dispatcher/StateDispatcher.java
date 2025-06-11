package kz.aday.bot.bot.dispatcher;

import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class StateDispatcher extends AbstractDispatcher<StateHandler>{
    private final UserService userService = ServiceContainer.getUserService();
    public StateDispatcher() {
        super(new HashSet<>());
    }

    public void dispatch(Update update, AbsSender sender) throws Exception {
        log.info("Dispatching state for update, chatId: {}", update != null && update.getMessage() != null ? update.getMessage().getChatId() : "unknown");
        if (update == null || update.getMessage() == null || update.getMessage().getText() == null || update.getMessage().getChatId() == null) {
            log.error("Invalid update or state text");
            throw new IllegalArgumentException("Некорректный state или текст команды");
        }

        String text = update.getMessage().getText().trim();
        log.debug("Processing state: [{}]", text);

        for (StateHandler handler : handlers) {
            if (handler.canHandle(text, userService.findById(update.getMessage().getChatId().toString()))) {
                log.info("State [{}] handler: [{}]", text, handler);
                handler.handle(update, sender);
                log.info("State handled successfully: [{}]", text);
                return; // Выходим после обработки команды
            }
        }
        log.warn("Unknown state: [{}]", text);
    }
}
