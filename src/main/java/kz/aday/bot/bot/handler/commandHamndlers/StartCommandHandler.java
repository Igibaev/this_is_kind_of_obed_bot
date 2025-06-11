package kz.aday.bot.bot.handler.commandHamndlers;

import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.util.KeyboardUtil;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.ArrayList;
import java.util.List;

import static kz.aday.bot.bot.handler.stateHandlers.State.SET_USERNAME_THEN_CHOOSE_CITY;
import static kz.aday.bot.model.User.Role.ADMIN;

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
                    Menu menu = menuService.findById(user.getCity().toString());
                    if (order.getStatus() == Status.READY) {
                        sendMessage(
                                user,
                                CURRENT_ORDER,
                                getMenuKeyboard(
                                        menu.getItemList(),
                                        order,
                                        List.of(
                                                new UserButton("Изменить заказ", CallbackState.CHANGE_ORDER.toString()),
                                                new UserButton("Удалить", CallbackState.DELETE_ORDER.toString()),
                                                new UserButton("Вернуться", CallbackState.CANCEL.toString())
                                        )
                                ),
                                sender
                        );
                    } else {
                        sendMessage(
                                user,
                                CURRENT_PENDING_ORDER,
                                getMenuKeyboard(
                                        menu.getItemList(),
                                        order,
                                        List.of(
                                                new UserButton("Потвердить", CallbackState.SUBMIT_ORDER.toString()),
                                                new UserButton("Изменить заказ", CallbackState.CHANGE_ORDER.toString()),
                                                new UserButton("Удалить", CallbackState.DELETE_ORDER.toString()),
                                                new UserButton("Вернуться", CallbackState.CANCEL.toString())
                                        )
                                ),
                                sender
                        );
                    }
                } else {
                    Menu menu = menuService.findById(user.getCity().toString());
                    sendMessage(
                            user,
                            String.format(MENU_TODAY, user.getCity().getValue()),
                            getMenuKeyboard(
                                    menu.getItemList(),
                                    List.of(new UserButton("Отправить", CallbackState.SUBMIT_ORDER.name()))
                            ),
                            sender
                    );
                }
            } else {
                user.setState(State.DEFAULT);
                sendMessage(
                        user,
                        NAVIGATION_MENU,
                        getUserMenuKeyboard(user),
                        sender
                );
            }
        } else {
            User createdUser = User.builder()
                    .chatId(getChatId(update))
                    .role(User.Role.USER)
                    .build();
            createdUser.setState(SET_USERNAME_THEN_CHOOSE_CITY);
            userService.save(createdUser);
            sendMessage(createdUser, START_MESSAGE_INPUT_NAME, sender);
        }
    }

    private ReplyKeyboard getUserMenuKeyboard(User user) {
        boolean isMenuExist = isMenuExist(user.getCity());
        boolean isMenuReady = isMenuReady(user.getCity());
        boolean isOrderExist = isOrderExist(user);
        boolean isOrderReady = isOrderReady(user);

        List<String> userMenuItems = new ArrayList<>();
        userMenuItems.add(CallbackState.PROFILE.getDisplayName());
        userMenuItems.add(CallbackState.EDIT_USERNAME.getDisplayName());
        userMenuItems.add(CallbackState.TEMP_ORDER_FOR_USER.getDisplayName());

        if (isMenuExist && isMenuReady) {
            if (isOrderExist) {
                if (isOrderReady) {
                    userMenuItems.add(CallbackState.DELETE_ORDER.getDisplayName());
                    userMenuItems.add(CallbackState.CHANGE_ORDER.getDisplayName());
                    userMenuItems.add(CallbackState.GET_ORDER.getDisplayName());
                } else {
                    userMenuItems.add(CallbackState.SUBMIT_ORDER.getDisplayName());
                    userMenuItems.add(CallbackState.CHANGE_ORDER.getDisplayName());
                    userMenuItems.add(CallbackState.GET_ORDER.getDisplayName());
                }
            } else {
                userMenuItems.add(CallbackState.CREATE_ORDER.getDisplayName());
            }
        }

        if (user.getRole() == ADMIN) {
            userMenuItems.add(CallbackState.INPUT_MESSAGE_TO_ALL_USERS.getDisplayName());
            userMenuItems.add(CallbackState.GET_ALL_ORDERS.getDisplayName());
            if (isMenuExist) {
                if (isMenuReady) {
                    userMenuItems.add(CallbackState.CLEAR_MENU.getDisplayName());
                    userMenuItems.add(CallbackState.CHANGE_MENU.getDisplayName());
                } else {
                    userMenuItems.add(CallbackState.PUBLISH_MENU.getDisplayName());
                }
            } else {
                userMenuItems.add(CallbackState.CREATE_MENU.getDisplayName());
            }
        }
        return KeyboardUtil.createReplyKeyboard(userMenuItems);
    }

    private ReplyKeyboard getMenuKeyboard(List<Item> itemList, Order order, List<UserButton> buttons) {
        InlineKeyboardMarkup markup = KeyboardUtil.createInlineKeyboard(itemList, order.getOrderItemList(), CallbackState.ADD_ITEM_TO_ORDER);
        KeyboardUtil.addButton(buttons, markup);
        return markup;
    }

    private ReplyKeyboard getMenuKeyboard(List<Item> itemList, List<UserButton> buttons) {
        InlineKeyboardMarkup markup = KeyboardUtil.createInlineKeyboard(itemList, CallbackState.ADD_ITEM_TO_ORDER);
        KeyboardUtil.addButton(buttons, markup);
        return markup;
    }

    private static final String NAVIGATION_MENU =
            "Меню навигации по боту.";

    private static final String CURRENT_PENDING_ORDER =
            "Ты не закончил заказ.";

    private static final String CURRENT_ORDER =
            "Твой заказ.";

    private static final String MENU_NOT_READY =
            "Город:%s. Меню на сегодня еще не готово.";

    private static final String MENU_TODAY =
            "Город:%s. Вот что сегодня в меню! \n" +
            "Чтобы отменить заказ нажми /cancel";

    private static final String START_MESSAGE_INPUT_NAME =
            "Добро пожаловать! \n" +
            "Данный бот предназначен \n" +
            "для заказов еды в koronaTech.\n" +
            "Введите своё имя. \n" +
            "Чтобы отменить нажми /cancel";
}
