package kz.aday.bot.bot.handler.commandHamndlers;

import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.List;

@Slf4j
public class StartCommandHandler extends CommandHandler {

    @Override
    public boolean canHandle(String command) {
        return "/start".equalsIgnoreCase(command);
    }

    @Override
    public void handle(Update update, AbsSender sender) throws Exception {
        if (isUserExist(update)) {
            User user = userService.findById(getChatId(update).toString());
            if (isMenuReady(user.getCity())) {
                if (isOrderExist(user)) {
                    Order order = orderService.findById(user.getId());
                    if (order.getStatus() == Status.READY) {
                        sendMessage(
                                user,
                                CURRENT_ORDER,
                                order.getOrderItemList(),
                                List.of(
                                        new UserButton("Изменить заказ", CallbackState.CHANGE_ORDER),
                                        new UserButton("Вернуться", CallbackState.CANCEL)
                                ),
                                sender
                        );
                    } else {
                        sendMessage(
                                user,
                                CURRENT_PENDING_ORDER,
                                order.getOrderItemList(),
                                List.of(
                                        new UserButton("Потвердить", CallbackState.SUBMIT_ORDER),
                                        new UserButton("Изменить заказ", CallbackState.CHANGE_ORDER),
                                        new UserButton("Вернуться", CallbackState.CANCEL)
                                ),
                                sender
                        );
                    }
                } else {
                    Menu menu = menuService.findById(user.getCity().toString());
                    sendMessage(
                            user,
                            String.format(MENU_TODAY, user.getCity().getValue()),
                            menu.getItemList(),
                            List.of(new UserButton("Отправить", CallbackState.SUBMIT_ORDER)),
                            sender
                    );
                }
            } else {
//                sendMessage(user, String.format(MENU_NOT_READY, user.getCity().getValue()), sender);
            }
        } else {
            User createdUser = User.builder()
                    .chatId(getChatId(update))
                    .role(User.Role.USER)
                    .build();
            userService.save(createdUser);
            sendMessage(createdUser, START_MESSAGE, sender);
        }
    }

    private static final String CURRENT_PENDING_ORDER =
            "Ты не закончил заказ.";

    private static final String CURRENT_ORDER =
            "Твой заказ.";

    private static final String MENU_NOT_READY =
            "Город:%s. Меню на сегодня еще не готово.";

    private static final String MENU_TODAY =
            "Город:%s. Вот что сегодня в меню! \n" +
            "Чтобы отменить заказ нажми /cancel";

    private static final String START_MESSAGE = "Добро пожаловать! \n" +
            "Данный бот предназначен \n" +
            "для заказов еды в koronaTech.\n" +
            "Введите своё имя. \n" +
            "Чтобы отменить нажми /cancel";
}
