package kz.aday.bot.service;

import kz.aday.bot.model.TempOrder;
import kz.aday.bot.repository.BaseRepository;
import kz.aday.bot.repository.Repository;

import java.util.HashMap;

public class TempOrderService extends BaseService<TempOrder> {
    public TempOrderService() {
        super(new BaseRepository<>(new HashMap<>(), TempOrder.class, "tempOrder"));
    }
}
