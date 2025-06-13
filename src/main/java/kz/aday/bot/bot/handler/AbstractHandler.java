/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler;

import static kz.aday.bot.model.User.Role.ADMIN;

import java.util.ArrayList;
import java.util.List;
import kz.aday.bot.bot.TelegramFoodBot;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackHandler;
import kz.aday.bot.bot.handler.commandHamndlers.CommandHandler;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OrderService;
import kz.aday.bot.service.ReportService;
import kz.aday.bot.service.UserService;
import kz.aday.bot.util.KeyboardUtil;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
public abstract class AbstractHandler {
  protected final UserService userService = ServiceContainer.getUserService();
  protected final MessageSender messageService = ServiceContainer.getMessageService();
  protected final MenuService menuService = ServiceContainer.getMenuService();
  protected final OrderService orderService = ServiceContainer.getOrderService();
  protected final ReportService reportService = ServiceContainer.getReportService();

  public Long getChatId(CallbackQuery update) {
    return update.getMessage().getChatId();
  }

  boolean isUserExistAndReady(CallbackQuery update) {
    return userService
        .findByIdOptional(getChatId(update).toString())
        .filter(user -> user.getStatus() == Status.READY)
        .isPresent();
  }

  public boolean isUserExist(CallbackQuery update) {
    return userService.findByIdOptional(getChatId(update).toString()).isPresent();
  }

  public Long getChatId(Update update) {
    return update.getMessage().getChatId();
  }

  public Integer getMessageId(Update update) {
    return update.getMessage().getMessageId();
  }

  public boolean isUserExistAndReady(Update update) {
    return userService
        .findByIdOptional(getChatId(update).toString())
        .filter(user -> user.getStatus() == Status.READY)
        .isPresent();
  }

  public boolean isUserExist(Update update) {
    return userService.findByIdOptional(getChatId(update).toString()).isPresent();
  }

  public boolean isMenuExist(City city) {
    return menuService.existsById(city.toString());
  }

  public boolean isMenuReady(City city) {
    if (isMenuExist(city)) {
      Menu menu = menuService.findById(city.toString());
      return menu.getStatus() == Status.READY;
    }
    return false;
  }

  public boolean isMenuPending(City city) {
    if (isMenuExist(city)) {
      Menu menu = menuService.findById(city.toString());
      return menu.getStatus() == Status.PENDING;
    }
    return false;
  }

  public boolean isOrderExist(User user) {
    return orderService.existsById(user.getId());
  }

  public boolean isOrderReady(User user) {
    if (isOrderExist(user)) {
      Order order = orderService.findById(user.getId());
      return order.getStatus() == Status.READY;
    }
    return false;
  }

  public boolean isOrderPending(User user) {
    if (isOrderExist(user)) {
      Order order = orderService.findById(user.getId());
      return order.getStatus() == Status.PENDING;
    }
    return false;
  }

  public ReplyKeyboard getUserMenuKeyboard(User user) {
    boolean isMenuExist = isMenuExist(user.getCity());
    boolean isMenuReady = isMenuReady(user.getCity());
    boolean isOrderExist = isOrderExist(user);
    boolean isOrderReady = isOrderReady(user);

    List<String> userMenuItems = new ArrayList<>();
    userMenuItems.add(State.PROFILE.getDisplayName());
    userMenuItems.add(State.EDIT_USERNAME.getDisplayName());
    userMenuItems.add(State.TEMP_ORDER_FOR_USER.getDisplayName());

    if (isMenuExist && isMenuReady) {
      if (isOrderExist) {
        if (isOrderReady) {
          userMenuItems.add(State.DELETE_ORDER.getDisplayName());
          userMenuItems.add(State.CHANGE_ORDER.getDisplayName());
          userMenuItems.add(State.GET_ORDER.getDisplayName());
        } else {
          userMenuItems.add(State.SUBMIT_ORDER.getDisplayName());
          userMenuItems.add(State.CHANGE_ORDER.getDisplayName());
          userMenuItems.add(State.GET_ORDER.getDisplayName());
        }
      } else {
        userMenuItems.add(State.CREATE_ORDER.getDisplayName());
      }
    }

    if (user.getRole() == ADMIN) {
      userMenuItems.add(State.SEND_MESSAGE_TO_ALL_USERS.getDisplayName());
      userMenuItems.add(State.GET_ALL_ORDERS.getDisplayName());
      if (isMenuExist) {
        if (isMenuReady) {
          userMenuItems.add(State.CLEAR_MENU.getDisplayName());
          userMenuItems.add(State.CHANGE_MENU.getDisplayName());
        } else {
          userMenuItems.add(State.PUBLISH_MENU.getDisplayName());
          userMenuItems.add(State.CHANGE_MENU.getDisplayName());
        }
      } else {
        userMenuItems.add(State.CREATE_MENU.getDisplayName());
      }
    }
    return KeyboardUtil.createReplyKeyboard(userMenuItems);
  }

  public void sendMessageWithKeyboard(
      User user,
      String text,
      ReplyKeyboard keyboard,
      Integer lastUserSendedMessageId,
      AbsSender sender)
      throws TelegramApiException {
    List<Integer> messagesToDelete = new ArrayList<>();
    if (lastUserSendedMessageId != null) messagesToDelete.add(lastUserSendedMessageId);
    if (user.getLastMessageId() != null) messagesToDelete.add(user.getLastMessageId());
    SendMessage message = new SendMessage();
    message.setChatId(user.getChatId());
    message.setText(text);
    message.setReplyMarkup(keyboard);
    message.enableMarkdown(true);
    Message sendedMessage = messageService.sendMessage(message, sender);
    messageService.deleteMessage(user.getChatId(), messagesToDelete, sender);

    user.setLastMessageId(sendedMessage.getMessageId());
    userService.save(user);
  }

  public void sendMessage(User user, String text, Integer lastUserSendedMessageId, AbsSender sender)
      throws TelegramApiException {
    List<Integer> messagesToDelete = new ArrayList<>();
    if (lastUserSendedMessageId != null) messagesToDelete.add(lastUserSendedMessageId);
    if (user.getLastMessageId() != null) messagesToDelete.add(user.getLastMessageId());
    SendMessage message = new SendMessage();
    message.setChatId(user.getChatId());
    message.setText(text);
    message.enableMarkdown(true);
    Message sendedMessage = messageService.sendMessage(message, sender);
    messageService.deleteMessage(user.getChatId(), messagesToDelete, sender);

    user.setLastMessageId(sendedMessage.getMessageId());
    userService.save(user);
  }

  public boolean register(TelegramFoodBot bot) {
    if (this instanceof StateHandler) {
      bot.addStateHandler((StateHandler) this);
    } else if (this instanceof CommandHandler) {
      bot.addCommandHandler((CommandHandler) this);
    } else if (this instanceof CallbackHandler) {
      bot.addCallbackHandler((CallbackHandler) this);
    } else {
      log.warn("Uknown handler: {}", this.getClass());
      return false;
    }
    return true;
  }
}
