/* (C) 2024 Igibaev */
package kz.aday.bot.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TempOrder extends Order {
  private String orderId;

  public TempOrder(String tempOrderId) {
    this.orderId = tempOrderId;
  }

  @Override
  public String getId() {
    return orderId;
  }

  @Override
  public Long getChatId() {
    throw new UnsupportedOperationException("Операция не поддерживается у объекта TempOrder");
  }
}
