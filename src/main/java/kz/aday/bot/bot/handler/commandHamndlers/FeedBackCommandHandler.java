package kz.aday.bot.bot.handler.commandHamndlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.configuration.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Slf4j
public class FeedBackCommandHandler extends AbstractHandler implements StateHandler {
    private final String mainUserChatId;

    public FeedBackCommandHandler() {
        this.mainUserChatId = BotConfig.getMainUserChatId();
    }

    @Override
    public boolean canHandle(String command) {
        return false;
    }

    @Override
    public void handle(Update update, AbsSender sender) throws Exception {
        if (update.hasMessage()) {
            if (update.getMessage().hasText() && update.getMessage().getText().isBlank()) {
                log.info("Skip empty feedback command");
                return;
            }

            if (update.getMessage().hasText() || update.getMessage().hasAudio() || update.getMessage().hasVideo() || update.getMessage().hasPhoto()) {
                Message message = update.getMessage();

                ForwardMessage forward = new ForwardMessage();
                forward.setChatId(mainUserChatId);                 // тебе
                forward.setFromChatId(message.getChatId().toString()); // от кого
                forward.setMessageId(message.getMessageId());

                log.info("Send feedback to user");
                sender.execute(forward);
            }
        }
    }
}
