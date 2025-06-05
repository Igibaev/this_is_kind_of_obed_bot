package kz.aday.bot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Id {
    @JsonIgnore
    String getId();
}
