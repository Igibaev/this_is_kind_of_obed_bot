package kz.aday.bot.bot.handler.commandHamndlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class GetAllOrdersCommandHandler extends AbstractHandler implements CommandHandler {
    @Override
    public boolean canHandle(String command) {
        return command.startsWith("/getallorders");
    }

    @Override
    public void handle(Update update, AbsSender sender) throws Exception {
        User user = userService.findById(getChatId(update).toString());
        sendMessage(
                user,
                String.format(ATTENDANCE_SHEET, user.getCity().getValue(), orderService.getAllOrdersGropedByDate(user.getCity())),
                getMessageId(update),
                sender
        );

    }

    private final String ATTENDANCE_SHEET = "Отчет посещяемости офиса %s за 30дней\n%s";
}
