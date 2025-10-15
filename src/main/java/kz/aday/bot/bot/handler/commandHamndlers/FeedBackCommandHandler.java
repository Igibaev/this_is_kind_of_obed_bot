package kz.aday.bot.bot.handler.commandHamndlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.configuration.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Slf4j
public class FeedBackCommandHandler extends AbstractHandler implements CommandHandler {
    private final String mainUserChatId;

    public FeedBackCommandHandler() {
        this.mainUserChatId = BotConfig.getMainUserChatId();
    }

    @Override
    public boolean canHandle(String command) {
        // всегда false потому что удобнее любое сообщение которое написали боту чтобы оно пересылалось мне
        if (command.trim().equalsIgnoreCase("/feedback")) {
            return false;
        }
        return command.startsWith("/feedback");
    }

    @Override
    public void handle(Update update, AbsSender sender) throws Exception {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            log.info("Message received: [{}]", message);
            ForwardMessage forward = new ForwardMessage();
            forward.setChatId(mainUserChatId);                 // тебе
            forward.setFromChatId(message.getChatId().toString()); // от кого
            forward.setMessageId(message.getMessageId());

            try {
                log.info("Send feedback to user");
                sender.executeAsync(forward);
            } catch (Exception e) {
                log.error("Error while send feedback to user", e);
            }

        }
    }
}
