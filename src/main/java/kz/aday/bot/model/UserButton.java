package kz.aday.bot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserButton {
  private String name;
  private String callback;
}
