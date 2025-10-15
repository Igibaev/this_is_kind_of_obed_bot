package kz.aday.bot.bot.handler.commandHamndlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.configuration.BotConfig;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class FeedBackCommandHandler extends AbstractHandler implements CommandHandler {
    private final String mainUserChatId;

    public FeedBackCommandHandler() {
        this.mainUserChatId = BotConfig.getMainUserChatId();
    }

    @Override
    public boolean canHandle(String command) {
        return command.startsWith("/feedback");
    }

    @Override
    public void handle(Update update, AbsSender sender) throws Exception {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            ForwardMessage forward = new ForwardMessage();
            forward.setChatId(mainUserChatId);                 // тебе
            forward.setFromChatId(message.getChatId().toString()); // от кого
            forward.setMessageId(message.getMessageId());

            sender.execute(forward);
        }
    }
}
