package kz.aday.bot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User implements Id {
    private Long chatId;
    private String preferedName;
    private Integer lastMessageId;
    private City city;
    private Role role;

    @Override
    public String getId() {
        return chatId.toString();
    }

    public enum Role {
        ADMIN, USER;
    }
}
