package kz.aday.bot.bot.handler.commandHamndlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Slf4j
public class SetAdminCommandHandler extends AbstractHandler implements CommandHandler {
    @Override
    public boolean canHandle(String command) {
        return command.startsWith("/setadmin");
    }

    @Override
    public void handle(Update update, AbsSender sender) throws Exception {
        if (isUserExistAndReady(update)) {
            String extractedChatId = update.getMessage().getText().replace("/setadmin", "");
            if (userService.findByIdOptional(extractedChatId).filter(user -> user.getStatus() == Status.READY).isPresent()) {
                User user = userService.findById(extractedChatId);
                user.setRole(User.Role.ADMIN);
                userService.save(user);
                log.info("Set admin role to user, {}", userService.findById(extractedChatId).getPreferedName());
                sendMessage(userService.findById(getChatId(update).toString()), "Ok", sender);
            }
        }
    }
}
