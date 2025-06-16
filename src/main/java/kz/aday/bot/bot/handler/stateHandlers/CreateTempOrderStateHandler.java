package kz.aday.bot.bot.handler.stateHandlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.TempOrder;
import kz.aday.bot.model.User;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static kz.aday.bot.bot.handler.stateHandlers.State.TEMP_ORDER_FOR_USER;

public class CreateTempOrderStateHandler extends AbstractHandler implements StateHandler {
    @Override
    public boolean canHandle(String state) {
        return TEMP_ORDER_FOR_USER.getDisplayName().equals(state);
    }

    @Override
    public void handle(Update update, AbsSender sender) throws Exception {
        if (isUserExistAndReady(update)) {
            User user = userService.findById(getChatId(update).toString());
            if (isMenuExist(user.getCity())) {
                if (isMenuReady(user.getCity())) {
                    Menu menu = menuService.findById(user.getCity().toString());
                    if (user.getState() == TEMP_ORDER_FOR_USER) {
                        String tempUserOrderRaw = update.getMessage().getText();
                        String[] itemsRaw = tempUserOrderRaw.split(" ");
                        if (itemsRaw.length <= 1) {
                            String shortMenu = createShortMenuMessage(menu.getItemList());
                            sendMessage(user, "Что то неправильно выбрали. Формат такой (Иванов 5 0 1)\n\n" + shortMenu, getMessageId(update), sender);
                        } else {
                            StringBuilder orderId = new StringBuilder();
                            Set<Item> items = new HashSet<>();
                            for (String raw : itemsRaw) {
                                if (StringUtils.isNumeric(raw)) {
                                    Integer itemId = Integer.parseInt(raw);
                                    Optional<Item> optionalItem = menu.getItemList().stream().filter(item -> item.getId().equals(itemId)).findFirst();
                                    if (optionalItem.isPresent()) {
                                        items.add(optionalItem.get());
                                    } else {
                                        String shortMenu = createShortMenuMessage(menu.getItemList());
                                        sendMessage(user, String.format("Что то неправильно выбрали. %s такой позиции нету в меню.\n\n%s", itemId, shortMenu), getMessageId(update), sender);
                                    }
                                } else {
                                    if (orderId.toString().isBlank()) {
                                        orderId = new StringBuilder(raw);
                                    } else {
                                        orderId.append("_").append(raw);
                                    }
                                }
                            }
                            String tempOrderId = update.getMessage().getText();
                            TempOrder tempOrder = new TempOrder(tempOrderId);
                            tempOrder.setOrderItemList(items);
                            tempOrder.setStatus(Status.READY);
                            orderService.save(tempOrder);
                            sendMessage(user, String.format(TEMP_ORDER_FOR_USER_READY_MESSAGE, tempOrder.getId(), items), getMessageId(update), sender);
                        }
                    } else {
                        user.setState(TEMP_ORDER_FOR_USER);
                        String shortMenu = createShortMenuMessage(menu.getItemList());
                        sendMessage(user, TEMP_ORDER_FOR_USER_MESSAGE + shortMenu, getMessageId(update), sender);
                    }

                }
            }
        }
    }

    private String createShortMenuMessage(List<Item> itemList) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Вот что есть в меню:\n");
        for (Item item : itemList) {
            stringBuilder.append(item.getCategory().getDisplayName()).append(": ").append(item.getId()).append(". ").append(item.getName()).append("\n");
        }
        stringBuilder.append("Отправьте это сотруднику, а потом отправьте следующим сообщением, то что он выбрал и имя его. Пример: (Иванов 1 5 0)");
        return stringBuilder.toString();
    }

    private final String TEMP_ORDER_FOR_USER_READY_MESSAGE =
            "Создали заказ для %s. %s. Если нужно будет удалить заказ или зменить, перейдите /templistorders.";


    private final String TEMP_ORDER_FOR_USER_MESSAGE =
            "Данный функционал нужен чтобы заказывать сотрудникам кто приезжает в город в командировку, чтобы миновать шаг добавления в чат бот.\n" +
                    "Чтобы посмотреть список заказов нажмите /templistorders.\n\n";
}
