/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.util.concurrent.ConcurrentHashMap;
import kz.aday.bot.model.Report;
import kz.aday.bot.repository.BaseRepository;

public class ReportService extends BaseService<Report> {
  public ReportService() {
    super(new BaseRepository<>(new ConcurrentHashMap<>(), Report.class, "report"));
  }
}
