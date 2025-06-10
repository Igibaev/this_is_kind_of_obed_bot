package kz.aday.bot.bot.handler.commandHamndlers;

import kz.aday.bot.model.User;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Slf4j
public class StartCommandHandler extends CommandHandler {
    @Override
    public boolean canHandle(String command) {
        return "/start".equalsIgnoreCase(command);
    }

    @Override
    public void handle(Update update, AbsSender sender) throws Exception {
        if (isUserExist(update)) {
            
        } else {
            User createdUser = User.builder()
                    .chatId(getChatId(update))
                    .role(User.Role.USER)
                    .build();
            userService.saveUser(createdUser);
            sendMessage(START_MESSAGE, sender);
        }
    }

    private String START_MESSAGE = "Добро пожаловать! \n" +
            "Данный бот предназначен \n" +
            "для заказов еды в koronaTech.\n" +
            "Введите своё имя. \n" +
            "Чтобы отменить нажми /cancel";
}
