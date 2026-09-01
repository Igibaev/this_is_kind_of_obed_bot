/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.commandHandlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.User;
import kz.aday.bot.util.Messages;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class GetAllOrdersCommandHandler extends AbstractHandler implements CommandHandler {
  @Override
  public boolean canHandle(String command) {
    return command.startsWith("/getallorders");
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    User user = userService.findById(getChatId(update).toString());
    sendMessage(
        user,
        Messages.ATTENDANCE_SHEET.getText(
            user.getCity().getValue(), orderService.getAllOrdersGropedByDate(user.getCity())),
        getMessageId(update),
        sender);
  }
}
