/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class WhoComesTomorrowAlmataCallbackHandler extends AbstractHandler implements CallbackHandler {

  @Override
  public boolean canHandle(CallbackQuery callback) {
    String[] data = callback.getData().split(":");
    if (data.length <= 0) throw new IllegalArgumentException("There is no callback");
    return CallbackState.WHO_COMES_TOMORROW_ALMATA.name().equals(data[0]);
  }

  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
    if (isUserExistAndReady(callback)) {
      User user = userService.findById(getChatId(callback).toString());
      // Кто придет завтра = текущие заказы (сделанные сегодня)
      List<Order> orders = orderService.findAllOnDate(LocalDate.now()).stream()
          .filter(o -> o.getCity() == user.getCity())
          .filter(o -> o.getStatus() == Status.READY)
          .collect(Collectors.toList());
      String names = orders.stream().map(Order::getUsername).collect(Collectors.joining(", "));
      if (names.isBlank()) {
        sendMessage(user, NOBODY_COMES, getMessageId(callback), sender);
      } else {
        sendMessage(user, String.format(WHO_COMES, orders.size(), names),
            getMessageId(callback), sender);
      }
    }
  }

  private static final String NOBODY_COMES = "Завтра никто в офис не придет( хнык хнык";
  private static final String WHO_COMES = "Завтра в офис придет [%s]:\n%s";
}
