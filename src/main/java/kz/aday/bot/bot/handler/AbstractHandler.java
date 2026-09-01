/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler;

import static kz.aday.bot.model.User.Role.ADMIN;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kz.aday.bot.bot.TelegramFoodBot;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.commandHandlers.CommandHandler;
import kz.aday.bot.bot.handler.stateHandlers.InputMessageToAllUsersStateHandler;
import kz.aday.bot.bot.handler.stateHandlers.SendFeedbackStateHandler;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.util.Messages;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OfficeAttendanceService;
import kz.aday.bot.service.OrderService;
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
  protected final OfficeAttendanceService officeAttendanceService =
      ServiceContainer.getOfficeAttendanceService();

  public boolean canHandle(CallbackQuery callback, CallbackState state) {
    String[] data = callback.getData().split(":");
    if (data.length == 0) {
      throw new IllegalArgumentException("There is no callback");
    }
    return state.name().equals(data[0].trim());
  }

  public Long getChatId(CallbackQuery update) {
    return update.getMessage().getChatId();
  }

  public Long getChatId(Update update) {
    return update.getMessage().getChatId();
  }

  public boolean isUserExistAndReady(CallbackQuery update) {
    return userService
        .findByIdOptional(getChatId(update).toString())
        .filter(this::isUserReady)
        .isPresent();
  }

  public Optional<User> findReadyUserByChatId(CallbackQuery update) {
    return userService.findByIdOptional(getChatId(update).toString()).filter(this::isUserReady);
  }

  public Optional<User> findReadyUserByChatId(Update update) {
    return userService.findByIdOptional(getChatId(update).toString()).filter(this::isUserReady);
  }

  public boolean isUserReady(User user) {
    return user.getStatus() == Status.READY;
  }

  public boolean checkAdminRole(User user, Integer messageId, AbsSender sender)
      throws TelegramApiException {
    if (user.getRole() == User.Role.USER) {
      sendMessage(user, Messages.PERMISSION_DENIED.getText(), messageId, sender);
      return true;
    }
    return false;
  }

  public Integer getMessageId(Update update) {
    return update.getMessage().getMessageId();
  }

  public Integer getMessageId(CallbackQuery callbackQuery) {
    return callbackQuery.getMessage().getMessageId();
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

  public boolean isDeadLinePassed(City city) {
    if (isMenuExist(city)) {
      Menu menu = menuService.findById(city.toString());
      return menu.isDeadlinePassed();
    }
    return false;
  }

  public boolean isOrderExist(User user) {
    return orderService.existsById(user.getId());
  }

  public ReplyKeyboard getUserMenuKeyboard(User user) {
    List<String> items = new ArrayList<>();
    boolean isAdmin = user.getRole() == ADMIN;
    addBaseMenuItems(isAdmin, items);

    Optional<Menu> menu = menuService.findByIdOptional(user.getCity().toString());
    if (menu.isEmpty()) {
      items.add(State.CREATE_ORDER.getDisplayName());
      if (isAdmin) {
        items.add(State.CREATE_MENU.getDisplayName());
      }
      return KeyboardUtil.createReplyKeyboard(items);
    }

    switch (menu.get().getStatus()) {
      case READY -> addReadyMenuItems(user.getId(), isAdmin, items);
      case DEADLINE -> {
        items.add(State.GET_ORDER.getDisplayName());
        if (isAdmin) {
          items.add(State.CHANGE_MENU.getDisplayName());
        }
      }
      case PENDING -> {
        if (isAdmin) {
          items.add(State.PUBLISH_MENU.getDisplayName());
          items.add(State.CHANGE_MENU.getDisplayName());
        }
      }
      default -> {}
    }
    return KeyboardUtil.createReplyKeyboard(items);
  }

  private void addBaseMenuItems(boolean isAdmin, List<String> items) {
    items.add(State.PROFILE.getDisplayName());
    items.add(State.EDIT_USERNAME.getDisplayName());
    items.add(State.WHO_WILL_COME_TO_OFFICE.getDisplayName());
    items.add(State.SET_OFFICE_ATTENDANCE.getDisplayName());

    if (isAdmin) {
      items.add(State.SEND_MESSAGE_TO_ALL_USERS.getDisplayName());
      items.add(State.GET_TODAY_ORDERS.getDisplayName());
      items.add(State.GET_ATTENDANCE_STATS.getDisplayName());
      items.add(State.GET_ATTENDANCE_STATS_MONTH.getDisplayName());
    }
  }

  private void addReadyMenuItems(String userId, boolean isAdmin, List<String> items) {
    if (isAdmin) {
      items.add(State.CLEAR_MENU.getDisplayName());
      items.add(State.CHANGE_MENU.getDisplayName());
    }
    Optional<Order> order = orderService.findByIdOptional(userId);
    if (order.isEmpty()) {
      items.add(State.CREATE_ORDER.getDisplayName());
      items.add(State.RANDOM_ORDER.getDisplayName());
      return;
    }
    items.add(
        order.get().getStatus() == Status.READY
            ? State.DELETE_ORDER.getDisplayName()
            : State.SUBMIT_ORDER.getDisplayName());
    items.add(State.CHANGE_ORDER.getDisplayName());
    items.add(State.GET_ORDER.getDisplayName());
  }

  public void sendMessageWithKeyboard(
      User user,
      String text,
      ReplyKeyboard keyboard,
      Integer lastUserSentMessageId,
      AbsSender sender)
      throws TelegramApiException {
    List<Integer> messagesToDelete = new ArrayList<>();
    if (lastUserSentMessageId != null) messagesToDelete.add(lastUserSentMessageId);
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
    if (this instanceof SendFeedbackStateHandler) {
      bot.addStateWithContentHandler((StateHandler) this);
    }
    if (this instanceof InputMessageToAllUsersStateHandler) {
      bot.addStateWithContentHandler((StateHandler) this);
    }
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
