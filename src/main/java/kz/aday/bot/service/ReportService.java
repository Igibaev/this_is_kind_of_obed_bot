package kz.aday.bot.service;

import kz.aday.bot.model.Report;
import kz.aday.bot.repository.BaseRepository;
import kz.aday.bot.repository.Repository;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class ReportService extends BaseService<Report> {
    public ReportService() {
        super(new BaseRepository<>(new ConcurrentHashMap<>(), Report.class, "report"));
    }
}
