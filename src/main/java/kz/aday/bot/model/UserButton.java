package kz.aday.bot.model;

import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserButton {
    private String name;
    private CallbackState callback;
}
