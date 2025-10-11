/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.order;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.service.MenuRulesService;
import kz.aday.bot.util.KeyboardUtil;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.bots.AbsSender;
//
//public class CreateTempOrderStateHandler extends AbstractHandler implements StateHandler {
//
//  @Override
//  public boolean canHandle(String state) {
//    return TEMP_ORDER_FOR_USER.getDisplayName().equals(state);
//  }
//
//  @Override
//  public void handle(Update update, AbsSender sender) throws Exception {
//    if (isUserExistAndReady(update)) {
//      User user = userService.findById(getChatId(update).toString());
//      if (isMenuExist(user.getCity())) {
//        if (isMenuReady(user.getCity())) {
//          Menu menu = menuService.findById(user.getCity().toString());
//          if (user.getState() == TEMP_ORDER_FOR_USER) {
//            String tempUserOrderRaw = update.getMessage().getText();
//            String[] itemsRaw = tempUserOrderRaw.split(" ");
//            if (itemsRaw.length <= 1) {
//              String shortMenu = createShortMenuMessage(menu.getItemList());
//              sendMessage(
//                  user,
//                  "Что то неправильно выбрали. Формат такой (Иванов 5 0 1)\n\n" + shortMenu,
//                  getMessageId(update),
//                  sender);
//            } else {
//              StringBuilder orderId = new StringBuilder();
//              Set<Item> items = new HashSet<>();
//              for (String raw : itemsRaw) {
//                if (StringUtils.isNumeric(raw)) {
//                  Integer itemId = Integer.parseInt(raw);
//                  Optional<Item> optionalItem =
//                      menu.getItemList().stream()
//                          .filter(item -> item.getId().equals(itemId))
//                          .findFirst();
//                  if (optionalItem.isPresent()) {
//                    items.add(optionalItem.get());
//                  } else {
//                    String shortMenu = createShortMenuMessage(menu.getItemList());
//                    sendMessage(
//                        user,
//                        String.format(
//                            "Что то неправильно выбрали. %s такой позиции нету в меню.\n\n%s",
//                            itemId, shortMenu),
//                        getMessageId(update),
//                        sender);
//                  }
//                } else {
//                  if (orderId.toString().isBlank()) {
//                    orderId = new StringBuilder(raw);
//                  } else {
//                    orderId.append("_").append(raw);
//                  }
//                }
//              }
//              String tempOrderId = orderId.toString();
//              if (orderService.existsById(tempOrderId)) {
//                String shortMenu = createShortMenuMessage(menu.getItemList());
//                sendMessage(
//                    user,
//                    String.format(
//                        "Заказ с таким именем уже существует %s.\n\n%s", tempOrderId, shortMenu),
//                    getMessageId(update),
//                    sender);
//              } else {
//                Order tempOrder = new Order();
//                tempOrder.setChatId(tempOrderId);
//                tempOrder.setUsername(tempOrderId);
//                tempOrder.setStatus(Status.PENDING);
//                for (Item item : items) {
//                  orderService.addItemToOrder(
//                      tempOrder, item, MenuRulesService.getMenuRule(user.getCity()));
//                }
//                orderService.save(tempOrder);
//                ReplyKeyboard keyboard =
//                    KeyboardUtil.createInlineKeyboard(
//                        List.of(
//                            new UserButton(
//                                "Потвердить",
//                                CallbackState.SUBMIT_TEMP_ORDER.toString() + ":" + tempOrderId),
//                            new UserButton("Отмена", CallbackState.CANCEL.toString())));
//                sendMessageWithKeyboard(
//                    user,
//                    String.format(TEMP_ORDER_FOR_USER_READY_MESSAGE, tempOrder.getChatId(), items),
//                    keyboard,
//                    getMessageId(update),
//                    sender);
//              }
//            }
//          } else {
//            user.setState(TEMP_ORDER_FOR_USER);
//            String shortMenu = createShortMenuMessage(menu.getItemList());
//            sendMessage(
//                user, TEMP_ORDER_FOR_USER_MESSAGE + shortMenu, getMessageId(update), sender);
//          }
//        }
//      }
//    }
//  }
//
//  private String createShortMenuMessage(List<Item> itemList) {
//    StringBuilder stringBuilder = new StringBuilder();
//    stringBuilder.append("Вот что есть в меню:\n");
//    for (Item item : itemList) {
//      stringBuilder
//          .append(item.getCategory().getDisplayName())
//          .append(": ")
//          .append(item.getId())
//          .append(". ")
//          .append(item.getName())
//          .append("\n");
//    }
//    stringBuilder.append(
//        "Отправьте это сотруднику, а потом отправьте следующим сообщением, то что он выбрал и имя его. Пример: (Иванов 1 5 0)");
//    return stringBuilder.toString();
//  }
//
//  private final String TEMP_ORDER_FOR_USER_READY_MESSAGE =
//      "Создали заказ для %s. %s. Чтобы отправить заказ нажмите, потвердить.\n"
//          + "Если нужно будет удалить заказ или изменить, перейдите /templistorders.";
//
//  private final String TEMP_ORDER_FOR_USER_MESSAGE =
//      "Данный функционал нужен чтобы заказывать сотрудникам кто приезжает в город в командировку, чтобы миновать шаг добавления в чат бот.\n"
//          + "Чтобы посмотреть список заказов нажмите /templistorders.\n\n";
//}
