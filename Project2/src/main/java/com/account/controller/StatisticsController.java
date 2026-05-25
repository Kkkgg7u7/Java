package com.account.controller;

import com.account.service.StatisticsService;
import com.account.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/dashboard")
    public Result dashboard(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录");
        }
        
        Map<String, Object> data = statisticsService.getOverview(userId);
        return Result.success(data);
    }

    @GetMapping("/monthly")
    public Result monthly(@RequestParam(value = "year", required = false) Integer year,
                         @RequestParam(value = "month", required = false) Integer month,
                         HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录");
        }
        
        Map<String, Object> data = statisticsService.getMonthlySummary(userId, year, month);
        return Result.success(data);
    }

    @GetMapping("/summary")
    public Result summary(@RequestParam(value = "year", required = false) Integer year,
                          @RequestParam(value = "month", required = false) Integer month,
                          HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录");
        }

        Map<String, Object> data = statisticsService.getSummary(userId, year, month);
        return Result.success(data);
    }

    @GetMapping("/category/expense")
    public Result categoryExpense(@RequestParam(value = "year", required = false) Integer year,
                                  @RequestParam(value = "month", required = false) Integer month,
                                  HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录");
        }
        
        List<Map<String, Object>> data = statisticsService.getExpenseCategoryDistribution(userId, year, month);
        return Result.success(data);
    }

    @GetMapping("/category/income")
    public Result categoryIncome(@RequestParam(value = "year", required = false) Integer year,
                                 @RequestParam(value = "month", required = false) Integer month,
                                 HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录");
        }
        
        List<Map<String, Object>> data = statisticsService.getIncomeCategoryDistribution(userId, year, month);
        return Result.success(data);
    }

    @GetMapping("/trend")
    public Result trend(@RequestParam(value = "year", required = false) Integer year,
                        HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录");
        }

        List<Map<String, Object>> data = statisticsService.getMonthlyTrend(userId, year);
        return Result.success(data);
    }
}
