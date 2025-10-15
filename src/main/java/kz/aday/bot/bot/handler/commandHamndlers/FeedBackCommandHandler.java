package kz.aday.bot.bot.handler.commandHamndlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.configuration.BotConfig;
import kz.aday.bot.model.User;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Slf4j
public class FeedBackCommandHandler extends AbstractHandler implements CommandHandler {
    @Override
    public boolean canHandle(String command) {
        return command.startsWith("/feedback");
    }

    @Override
    public void handle(Update update, AbsSender sender) throws Exception {
        User user = userService.findById(getChatId(update).toString());
        user.setState(State.SEND_FEEDBACK);
        sendMessage(user, SEND_FEEDBACK, user.getLastMessageId(), sender);
    }

    private final String SEND_FEEDBACK = "Это форма обратной связи. Приложите фотографии, аудио, видео или просто текстом опишите, ваши пожелания как сделать чат бот лучше и удобнее.";
}
