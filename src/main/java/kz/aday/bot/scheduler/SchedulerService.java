/* (C) 2024 Igibaev */
package kz.aday.bot.scheduler;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import kz.aday.bot.bot.TelegramFoodBot;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.model.*;
import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OfficeAttendanceService;
import kz.aday.bot.service.OrderService;
import kz.aday.bot.service.SharedOrderItemPoolService;
import kz.aday.bot.service.UserService;
import kz.aday.bot.util.KeyboardUtil;
import kz.aday.bot.util.Messages;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
public class SchedulerService {
  private static final String EMPTY_ORDERS = "Список заказов пуст.";
  private static final String REPORT_MESSAGE = "Список заказов.\n";
  private static final long INITIAL_DELAY = 0;
  private static final long NOTIFICATION_PERIOD_SECONDS = 1;
  private static final long CLEANUP_PERIOD_DAYS = 1;
  private static final String ATTENDANCE_CONSOLIDATION_TASK = "office attendance consolidation";
  private static final String OUTDATED_MENUS_DELETION_TASK = "outdated menus deletion";
  private static final String OUTDATED_ORDERS_DELETION_TASK = "outdated orders deletion";
  private static final String OUTDATED_POOLS_DELETION_TASK = "outdated shared order items deletion";

  private final MessageSender messageSender = new MessageSender();
  private final UserService userService = ServiceContainer.getUserService();
  private final MenuService menuService = ServiceContainer.getMenuService();
  private final OrderService orderService = ServiceContainer.getOrderService();
  private final OfficeAttendanceService officeAttendanceService =
      ServiceContainer.getOfficeAttendanceService();
  private final SharedOrderItemPoolService poolService = ServiceContainer.getPoolService();
  private final TelegramFoodBot telegramFoodBot;

  private final Map<String, Boolean> handledNotifications = new ConcurrentHashMap<>();

  private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

  public SchedulerService(TelegramFoodBot telegramFoodBot) {
    this.telegramFoodBot = telegramFoodBot;
  }

  public void start() {
    executorService.scheduleAtFixedRate(
        this::sendDeadlineIsNearNotification,
        INITIAL_DELAY,
        NOTIFICATION_PERIOD_SECONDS,
        TimeUnit.SECONDS);
    executorService.scheduleAtFixedRate(
        this::closeMenu, INITIAL_DELAY, NOTIFICATION_PERIOD_SECONDS, TimeUnit.SECONDS);
    executorService.scheduleAtFixedRate(
        this::cleanUpStorage, INITIAL_DELAY, CLEANUP_PERIOD_DAYS, TimeUnit.DAYS);
  }

  void cleanUpStorage() {
    runSafely(ATTENDANCE_CONSOLIDATION_TASK, officeAttendanceService::consolidatePastMonths);
    runSafely(OUTDATED_ORDERS_DELETION_TASK, orderService::deleteOutdated);
    runSafely(OUTDATED_POOLS_DELETION_TASK, poolService::deleteOutdated);
    runSafely(OUTDATED_MENUS_DELETION_TASK, menuService::deleteOutdated);
  }

  private static void runSafely(String taskName, Runnable task) {
    log.debug("Running [{}]", taskName);
    try {
      task.run();
    } catch (RuntimeException e) {
      log.error("Failed to run [{}]: {}", taskName, e.getMessage(), e);
    }
  }

  void closeMenu() {
    log.debug("Closing Menu");
    for (Menu menu : menuService.findAll()) {
      if (menu.isDeadlinePassed() && menu.getStatus() != Status.DEADLINE) {
        menu.setStatus(Status.DEADLINE);
        menuService.save(menu);
        LocalDate orderDate = menu.getCity().getCurrentOrderDate();
        orderService.markOrdersAsSubmitted(menu.getCity(), orderDate);
        sendMenuIsClosedNotification(menu.getCity());
        sendReportToUsers(menu.getCity(), orderDate);
        handledNotifications.clear();
      }
    }
  }

  private void sendReportToUsers(City city, LocalDate orderDate) {
    for (User user : userService.findAll()) {
      if (user.getCity() == city) {
        List<Order> orders =
            orderService.findAllOnDate(orderDate).stream()
                .filter(o -> o.getCity() == user.getCity())
                .filter(o -> o.getStatus() == Status.READY)
                .filter(o -> !o.getOrderItemList().isEmpty())
                .collect(Collectors.toList());
        if (orders.isEmpty()) {
          sendMessageToUser(EMPTY_ORDERS, user);
        } else {
          Report report = new Report(user.getCity(), orders);
          sendMessageToUser(REPORT_MESSAGE + report.printOrderReport(), user);
        }
      }
    }
  }

  private void sendMenuIsClosedNotification(City city) {
    log.debug("send menu is closed notification");
    for (User user : userService.findAll()) {
      if (user.getCity() == city) {
        sendMessageToUser(Messages.MENU_IS_CLOSED.getText(), user);
      }
    }
  }

  private void sendDeadlineIsNearNotification() {
    log.debug("send deadline isNearNotification");
    for (Menu menu : menuService.findAll()) {
      if (menu.getStatus() == Status.READY && menu.isDeadlineNear() && !menu.isDeadlinePassed()) {
        for (User user : userService.findAll()) {
          if (user.getCity() != menu.getCity()) continue;
          if (handledNotifications.containsKey(user.getId())) {
            continue;
          }
          if (orderService.existsByChatId(user.getId(), user.getCity().getCurrentOrderDate())) {
            Order order =
                orderService.findByChatId(user.getId(), user.getCity().getCurrentOrderDate());
            if (order.getStatus() == Status.PENDING) {
              sendMessageWithMenuToUser(menu, order.getOrderItemList(), user);
              handledNotifications.put(user.getId(), true);
            }
          } else {
            sendMessageWithMenuToUser(menu, user);
            handledNotifications.put(user.getId(), true);
          }
        }
      }
    }
  }

  private void sendMessageToUser(String messageText, User user) {
    send(buildMessage(user, messageText), user);
  }

  private void sendMessageWithMenuToUser(Menu menu, User user) {
    sendDeadlineIsNearMessage(
        KeyboardUtil.createInlineKeyboard(menu.getItemList(), CallbackState.ADD_ITEM_TO_ORDER),
        user);
  }

  private void sendMessageWithMenuToUser(Menu menu, Set<Item> orderItems, User user) {
    sendDeadlineIsNearMessage(
        KeyboardUtil.createInlineKeyboard(
            menu.getItemList(), orderItems, CallbackState.ADD_ITEM_TO_ORDER),
        user);
  }

  private void sendDeadlineIsNearMessage(ReplyKeyboard keyboard, User user) {
    SendMessage message = buildMessage(user, Messages.DEADLINE_IS_NEAR_MAKE_AN_ORDER.getText());
    message.setReplyMarkup(keyboard);
    send(message, user);
  }

  private static SendMessage buildMessage(User user, String messageText) {
    SendMessage message = new SendMessage();
    message.setChatId(user.getChatId());
    message.setText(messageText);
    message.enableMarkdown(true);
    return message;
  }

  private void send(SendMessage message, User user) {
    List<Integer> messagesToDelete = new ArrayList<>();
    if (user.getLastMessageId() != null) messagesToDelete.add(user.getLastMessageId());
    try {
      Message sendedMessage = messageSender.sendMessage(message, telegramFoodBot);
      messageSender.deleteMessage(user.getChatId(), messagesToDelete, telegramFoodBot);

      user.setLastMessageId(sendedMessage.getMessageId());
      userService.save(user);
    } catch (TelegramApiException e) {
      log.error("Skip sending deadline notification: {}\n {}", e.getMessage(), e);
    }
  }
}
