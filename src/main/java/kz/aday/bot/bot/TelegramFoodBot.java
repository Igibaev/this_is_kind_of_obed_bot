package kz.aday.bot.bot;

import kz.aday.bot.bot.dispatcher.CallbackDispatcher;
import kz.aday.bot.bot.dispatcher.CommandDispatcher;
import kz.aday.bot.bot.dispatcher.StateDispatcher;
import kz.aday.bot.bot.handler.ErrorHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackHandler;
import kz.aday.bot.bot.handler.commandHamndlers.CommandHandler;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
public class TelegramFoodBot extends TelegramLongPollingBot {
  private final CallbackDispatcher callbackDispatcher;
  private final CommandDispatcher commandDispatcher;
  private final StateDispatcher stateDispatcher;
  private final ErrorHandler errorHandler;
  private final String chatBotName;

  public TelegramFoodBot(String chatBotName, String token) {
    super(token);
    this.chatBotName = chatBotName;
    this.callbackDispatcher = new CallbackDispatcher();
    this.commandDispatcher = new CommandDispatcher();
    this.stateDispatcher = new StateDispatcher();
    this.errorHandler = new ErrorHandler();
  }

  @Override
  public void onUpdateReceived(Update update) {
    try {
      if (update.hasMessage()) {
        Message msg = update.getMessage();
        String text = msg.getText();
        if (text != null) {
          if (text.startsWith("/")) {
            commandDispatcher.dispatch(update, this);
          } else {
            stateDispatcher.dispatch(update, this);
          }
        }
      } else if (update.hasCallbackQuery()) {
        callbackDispatcher.dispatch(update.getCallbackQuery(), this);
      }
    } catch (Exception e) {
      try {
        errorHandler.handle(e, update, this);
      } catch (TelegramApiException ex) {
        log.error("Critic error: ", ex);
        throw new RuntimeException(ex);
      }
    }
  }

  @Override
  public String getBotUsername() {
    return chatBotName;
  }

  public void addCallbackHandler(CallbackHandler handler) {
    callbackDispatcher.addHandler(handler);
  }

  public void addCommandHandler(CommandHandler handler) {
    commandDispatcher.addHandler(handler);
  }

  public void addStateHandler(StateHandler handler) {
    stateDispatcher.addHandler(handler);
  }
}
