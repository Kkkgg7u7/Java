package com.account.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface StatisticsService {

    Map<String, Object> getOverview(Long userId);

    Map<String, Object> getMonthlySummary(Long userId, Integer year, Integer month);

    List<Map<String, Object>> getExpenseCategoryDistribution(Long userId, Integer year, Integer month);

    List<Map<String, Object>> getIncomeCategoryDistribution(Long userId, Integer year, Integer month);

    List<Map<String, Object>> getMonthlyTrend(Long userId, Integer year);

    Map<String, Object> getAccountBalanceSummary(Long userId);

}
