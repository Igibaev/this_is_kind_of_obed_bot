/* (C) 2024 Igibaev */
package kz.aday.bot.scheduler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import kz.aday.bot.bot.TelegramFoodBot;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.messages.Messages;
import kz.aday.bot.model.*;
import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OrderService;
import kz.aday.bot.service.UserService;
import kz.aday.bot.util.KeyboardUtil;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
public class SchedulerService {
  private final MessageSender messageSender = new MessageSender();
  private final UserService userService = new UserService();
  private final MenuService menuService = ServiceContainer.getMenuService();
  private final OrderService orderService = ServiceContainer.getOrderService();
  private final TelegramFoodBot telegramFoodBot;

  private final Map<String, Boolean> handledNotifications = new ConcurrentHashMap<>();

  private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

  public SchedulerService(TelegramFoodBot telegramFoodBot) {
    this.telegramFoodBot = telegramFoodBot;
  }

  public void start() {
    planCleanTask();
    executorService.scheduleAtFixedRate(
        this::sendDeadlineIsNearNotification, 0, 1, TimeUnit.SECONDS);
    executorService.scheduleAtFixedRate(this::closeMenu, 0, 1, TimeUnit.SECONDS);
  }

  private void planCleanTask() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime next = now.with(LocalTime.MIDNIGHT);
    if (now.isAfter(next)) {
      next = next.plusDays(1);
    }

    Duration duration = Duration.between(now, next);
    long initialDelay = duration.getSeconds();

    long period = TimeUnit.DAYS.toSeconds(1);
    executorService.scheduleAtFixedRate(
        this::cleanAllStorages, initialDelay, period, TimeUnit.SECONDS);
  }

  /** Закрыть меню, закрыть все заказы */
  private void closeMenu() {
    log.debug("Closing Menu");
    for (Menu menu : menuService.findAll()) {
      if (menu.isDeadlinePassed() && menu.getStatus() != Status.DEADLINE) {
        menu.setStatus(Status.DEADLINE);
        menuService.save(menu);
        sendMenuIsClosedNotification(menu.getCity());
        sendReportToUsers(menu.getCity());
        handledNotifications.clear();
      }
    }
  }

    private void sendReportToUsers(City city) {
    for (User user : userService.findAll()) {
      if (user.getCity() == city) {
        List<Order> orders =
            orderService.findAll().stream()
                .filter(o -> o.getCity() == user.getCity())
                .filter(o -> o.getStatus() == Status.READY)
                .filter(o -> !o.getOrderItemList().isEmpty())
                .collect(Collectors.toList());
        if (orders.isEmpty()) {
          sendMessageToUser(EMPTY_ORDERS, user,  telegramFoodBot);
        } else {
          Report report = new Report(user.getCity(), orders);
          sendMessageToUser(REPORT_MESSAGE + report.printOrderReport(), user, telegramFoodBot);
        }
      }
    }
  }

  /** Отправить уведомления о том что дедлайн прошел меню закрыто */
  private void sendMenuIsClosedNotification(City city) {
    log.debug("send menu is closed notification");
    for (User user : userService.findAll()) {
      if (user.getCity() == city) {
        sendMessageToUser(Messages.MENU_IS_CLOSED, user, telegramFoodBot);
      }
    }
  }

  /** Отправить уведомления за 10 минут до дедлайна */
  private void sendDeadlineIsNearNotification() {
    log.debug("send deadline isNearNotification");
    for (Menu menu : menuService.findAll()) {
      if (menu.getStatus() == Status.READY && menu.isDeadlineNear() && !menu.isDeadlinePassed()) {
        for (User user : userService.findAll()) {
          if (user.getCity() != menu.getCity()) continue;
          if (handledNotifications.containsKey(user.getId())) {
            continue;
          }
          if (orderService.existsById(user.getId())) {
            Order order = orderService.findById(user.getId());
            if (order.getStatus() == Status.PENDING) {
              sendMessageWithMenuToUser(menu, order.getOrderItemList(), user, telegramFoodBot);
              handledNotifications.put(user.getId(), true);
            }
          } else {
            sendMessageWithMenuToUser(menu, user, telegramFoodBot);
            handledNotifications.put(user.getId(), true);
          }
        }
      }
    }
  }

  private void sendMessageToUser(String messageText, User user, AbsSender absSender) {
    List<Integer> messagesToDelete = new ArrayList<>();
    if (user.getLastMessageId() != null) messagesToDelete.add(user.getLastMessageId());
    SendMessage message = new SendMessage();
    message.setChatId(user.getChatId());
    message.setText(messageText);
	message.enableMarkdown(true);
    try {
      Message sendedMessage = messageSender.sendMessage(message, absSender);
      messageSender.deleteMessage(user.getChatId(), messagesToDelete, absSender);

      user.setLastMessageId(sendedMessage.getMessageId());
      userService.save(user);
    } catch (TelegramApiException e) {
      log.error("Skip sending deadline notification: {}\n {}", e.getMessage(), e);
    }
  }

  private static final String EMPTY_ORDERS = "Список заказов пуст.";

  private static final String REPORT_MESSAGE = "Список заказов.\n";

  private void sendMessageWithMenuToUser(Menu menu, User user, AbsSender absSender) {
    List<Integer> messagesToDelete = new ArrayList<>();
    if (user.getLastMessageId() != null) messagesToDelete.add(user.getLastMessageId());
    SendMessage message = new SendMessage();
    message.setChatId(user.getChatId());
    message.setText(Messages.DEADLINE_IS_NEAR_MAKE_AN_ORDER);
    message.setReplyMarkup(
        KeyboardUtil.createInlineKeyboard(menu.getItemList(), CallbackState.ADD_ITEM_TO_ORDER));
    message.enableMarkdown(true);
    try {
      Message sendedMessage = messageSender.sendMessage(message, absSender);
      messageSender.deleteMessage(user.getChatId(), messagesToDelete, absSender);

      user.setLastMessageId(sendedMessage.getMessageId());
      userService.save(user);
    } catch (TelegramApiException e) {
      log.error("Skip sending deadline notification: {}\n {}", e.getMessage(), e);
    }
  }

  private void sendMessageWithMenuToUser(
      Menu menu, Set<Item> orderItems, User user, AbsSender absSender) {
    List<Integer> messagesToDelete = new ArrayList<>();
    if (user.getLastMessageId() != null) messagesToDelete.add(user.getLastMessageId());
    SendMessage message = new SendMessage();
    message.setChatId(user.getChatId());
    message.setText(Messages.DEADLINE_IS_NEAR_MAKE_AN_ORDER);
    message.setReplyMarkup(
        KeyboardUtil.createInlineKeyboard(
            menu.getItemList(), orderItems, CallbackState.ADD_ITEM_TO_ORDER));
    message.enableMarkdown(true);
    try {
      Message sendedMessage = messageSender.sendMessage(message, absSender);
      messageSender.deleteMessage(user.getChatId(), messagesToDelete, absSender);

      user.setLastMessageId(sendedMessage.getMessageId());
      userService.save(user);
    } catch (TelegramApiException e) {
      log.error("Skip sending deadline notification: {}\n {}", e.getMessage(), e);
    }
  }

  /** Отчистить всю текстовую БД кроме юзеров */
  private void cleanAllStorages() {
    log.debug("cleaning all storages");
    menuService.deleteAll();
    orderService.deleteAll();
  }
}
