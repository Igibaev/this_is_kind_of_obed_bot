package kz.aday.bot.bot.handler;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
public class ErrorHandler {
    public void handle(Exception e, Update update, AbsSender sender) {
        log.warn("Caught exception [{}]",e.getMessage(), e);
    }
}
