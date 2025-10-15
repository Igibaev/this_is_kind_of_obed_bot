package kz.aday.bot.bot.handler.stateHandlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.configuration.BotConfig;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class SendFeedbackStateHandler extends AbstractHandler implements StateHandler {
    private final String mainUserChatId;

    public SendFeedbackStateHandler() {
        this.mainUserChatId = BotConfig.getMainUserChatId();
    }

    @Override
    public boolean canHandle(String state) {
        return State.SEND_FEEDBACK.getDisplayName().equals(state);
    }

    @Override
    public void handle(Update update, AbsSender sender) throws Exception {
        if (!update.hasMessage()) return;
        Message message = update.getMessage();

        boolean hasContent = message.hasText() || message.hasPhoto() || message.hasVideo()
                || message.hasAudio() || message.hasDocument() || message.hasVoice()
                || message.hasAnimation() || message.hasSticker();

        if (!hasContent) return;

        // пересылаем оригинальное сообщение тебе
        ForwardMessage forward = new ForwardMessage();
        forward.setChatId(mainUserChatId);
        forward.setFromChatId(message.getChatId().toString());
        forward.setMessageId(message.getMessageId());
        sender.execute(forward);

        // подтверждение пользователю
        SendMessage confirm = new SendMessage();
        confirm.setChatId(message.getChatId().toString());
        confirm.setText("✅ Спасибо! Сообщение отправлено разработчику.");
        sender.execute(confirm);
    }
}
