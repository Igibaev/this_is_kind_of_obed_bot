package kz.aday.bot.bot.handler.commandHamndlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class TempListOrderCommandHandler extends AbstractHandler implements CommandHandler {
    @Override
    public boolean canHandle(String command) {
        return "/templistorders".equals(command);
    }

    @Override
    public void handle(Update update, AbsSender sender) throws Exception {
        if (isUserExistAndReady(update)) {
            User user = userService.findById(getChatId(update).toString());
            if (user.getRole() == User.Role.USER) {
                sendMessage(user, PERMISSION_DENIED, getMessageId(update), sender);
                return;
            }
        }
    }

    private static final String PERMISSION_DENIED = "Нет доступа.";

}
