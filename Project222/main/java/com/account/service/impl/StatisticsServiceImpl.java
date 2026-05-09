package com.account.service.impl;

import com.account.dao.AccountMapper;
import com.account.dao.RecordMapper;
import com.account.service.StatisticsService;
import com.account.util.BigDecimalUtil;
import com.account.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private RecordMapper recordMapper;

    @Autowired
    private AccountMapper accountMapper;

    @Override
    public Map<String, Object> getOverview(Long userId) {
        if (userId == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        int year = DateUtil.getCurrentYear();
        int month = DateUtil.getCurrentMonth();
        Map<String, Object> monthlyStats = recordMapper.selectMonthlyStatistics(userId, year, month);
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        if (monthlyStats != null) {
            Object income = monthlyStats.get("totalIncome");
            Object expense = monthlyStats.get("totalExpense");
            if (income != null) {
                totalIncome = new BigDecimal(income.toString());
            }
            if (expense != null) {
                totalExpense = new BigDecimal(expense.toString());
            }
        }
        BigDecimal totalBalance = accountMapper.selectTotalBalanceByUserId(userId);
        if (totalBalance == null) {
            totalBalance = BigDecimal.ZERO;
        }
        result.put("totalBalance", BigDecimalUtil.scale(totalBalance));
        result.put("monthlyIncome", BigDecimalUtil.scale(totalIncome));
        result.put("monthlyExpense", BigDecimalUtil.scale(totalExpense));
        result.put("monthlyBalance", BigDecimalUtil.scale(BigDecimalUtil.subtract(totalIncome, totalExpense)));
        result.put("year", year);
        result.put("month", month);
        return result;
    }

    @Override
    public Map<String, Object> getMonthlySummary(Long userId, Integer year, Integer month) {
        if (userId == null) {
            return null;
        }
        if (year == null) {
            year = DateUtil.getCurrentYear();
        }
        if (month == null) {
            month = DateUtil.getCurrentMonth();
        }
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> stats = recordMapper.selectMonthlyStatistics(userId, year, month);
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        if (stats != null) {
            Object income = stats.get("totalIncome");
            Object expense = stats.get("totalExpense");
            if (income != null) {
                totalIncome = new BigDecimal(income.toString());
            }
            if (expense != null) {
                totalExpense = new BigDecimal(expense.toString());
            }
        }
        result.put("year", year);
        result.put("month", month);
        result.put("totalIncome", BigDecimalUtil.scale(totalIncome));
        result.put("totalExpense", BigDecimalUtil.scale(totalExpense));
        result.put("balance", BigDecimalUtil.scale(BigDecimalUtil.subtract(totalIncome, totalExpense)));
        return result;
    }

    @Override
    public List<Map<String, Object>> getExpenseCategoryDistribution(Long userId, Integer year, Integer month) {
        if (userId == null) {
            return null;
        }
        if (year == null) {
            year = DateUtil.getCurrentYear();
        }
        if (month == null) {
            month = DateUtil.getCurrentMonth();
        }
        return recordMapper.selectCategoryStatistics(userId, 0, year, month);
    }

    @Override
    public List<Map<String, Object>> getIncomeCategoryDistribution(Long userId, Integer year, Integer month) {
        if (userId == null) {
            return null;
        }
        if (year == null) {
            year = DateUtil.getCurrentYear();
        }
        if (month == null) {
            month = DateUtil.getCurrentMonth();
        }
        return recordMapper.selectCategoryStatistics(userId, 1, year, month);
    }

    @Override
    public List<Map<String, Object>> getMonthlyTrend(Long userId, Integer year) {
        if (userId == null) {
            return null;
        }
        if (year == null) {
            year = DateUtil.getCurrentYear();
        }
        return recordMapper.selectMonthlyTrend(userId, year);
    }

    @Override
    public Map<String, Object> getAccountBalanceSummary(Long userId) {
        if (userId == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        BigDecimal totalBalance = accountMapper.selectTotalBalanceByUserId(userId);
        if (totalBalance == null) {
            totalBalance = BigDecimal.ZERO;
        }
        result.put("totalBalance", BigDecimalUtil.scale(totalBalance));
        return result;
    }

}
