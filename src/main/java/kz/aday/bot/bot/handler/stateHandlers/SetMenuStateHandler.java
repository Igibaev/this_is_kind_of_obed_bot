package kz.aday.bot.bot.handler.stateHandlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.service.MenuTextParser;
import kz.aday.bot.util.KeyboardUtil;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.time.LocalDateTime;
import java.util.List;

public class SetMenuStateHandler extends AbstractHandler implements StateHandler {
    @Override
    public boolean canHandle(String state) {
        return State.SET_MENU.getDisplayName().equals(state);
    }

    @Override
    public void handle(Update update, AbsSender sender) throws Exception {
        if (isUserExistAndReady(update)) {
            User user = userService.findById(getChatId(update).toString());
            if (user.getRole() == User.Role.USER) {
                sendMessage(user, PERMISSION_DENIED, sender);
            } else {
                LocalDateTime deadline = MenuTextParser.parseDeadline(update.getMessage().getText());
                Menu menu = MenuTextParser.parseMenu(update.getMessage().getText());
                menu.setDeadline(deadline);
                menu.setStatus(Status.PENDING);
                menu.setCity(user.getCity());
                menuService.save(menu);

                user.setState(State.SET_MENU);
                InlineKeyboardMarkup markup = KeyboardUtil.createInlineKeyboard(menu.getItemList(), CallbackState.NONE);
                KeyboardUtil.addButton(
                        List.of(
                                new UserButton("Опубликовать", CallbackState.SUBMIT_MENU.toString()),
                                new UserButton("Изменить", CallbackState.CHANGE_MENU.toString())
                        ),
                        markup
                );
                sendMessageWithKeyboard(
                        user,
                        String.format(MENU_PENDING, user.getCity().getValue()),
                        markup,
                        sender
                );
            }
        }
    }

    private static final String PERMISSION_DENIED = "Нет доступа к созданию меню";

    private static final String MENU_PENDING = "Проверьте корректность меню для города %s.\nЧтобы отменить нажми /cancel";
}
