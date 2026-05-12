package com.account.service;

import com.account.entity.Record;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface RecordService {

    Record getById(Long id);

    List<Record> getByUserId(Long userId);

    List<Record> getByCondition(Long userId, Integer type, String category,
                                 String accountType, LocalDate startDate, LocalDate endDate);

    boolean add(Record record);

    boolean update(Record record);

    boolean delete(Long id);

    Map<String, Object> getMonthlyStatistics(Long userId, Integer year, Integer month);

    List<Map<String, Object>> getCategoryStatistics(Long userId, Integer type, Integer year, Integer month);

    List<Map<String, Object>> getMonthlyTrend(Long userId, Integer year);

}
