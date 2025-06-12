package kz.aday.bot.bot.handler.commandHamndlers;

import kz.aday.bot.bot.TelegramFoodBot;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class ReturnCommandHandler extends AbstractHandler implements CommandHandler {
    @Override
    public boolean canHandle(String command) {
        return "/return".equals(command);
    }

    @Override
    public void handle(Update update, AbsSender sender) throws Exception {
        User user = userService.findById(getChatId(update).toString());
        user.setState(State.NONE);
        if (user.getStatus() == Status.READY) {
            sendMessageWithKeyboard(user, NAVIGATION_MENU, getUserMenuKeyboard(user), getMessageId(update), sender);
        }
    }

    private static final String NAVIGATION_MENU = "Возвращаемся. Меню навигации по боту.";
}
