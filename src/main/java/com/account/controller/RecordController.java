package com.account.controller;

import com.account.entity.Record;
import com.account.service.RecordService;
import com.account.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/record")
public class RecordController {

    @Autowired
    private RecordService recordService;

    @GetMapping("/list")
    public Result list(@RequestParam(required = false) Integer type,
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) String accountType,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate,
                       HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录");
        }
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;
        List<Record> records = recordService.getByCondition(userId, type, category, accountType, start, end);
        return Result.success(records);
    }

    @PostMapping("/add")
    public Result add(@RequestParam Integer type,
                     @RequestParam BigDecimal amount,
                     @RequestParam String category,
                     @RequestParam String accountType,
                     @RequestParam String recordDate,
                     @RequestParam(required = false) String remark,
                     HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录");
        }
        Record record = new Record();
        record.setUserId(userId);
        record.setType(type);
        record.setAmount(amount);
        record.setCategory(category);
        record.setAccountType(accountType);
        record.setRecordDate(LocalDate.parse(recordDate));
        record.setRemark(remark);
        boolean success = recordService.add(record);
        return success ? Result.success("保存成功") : Result.error("保存失败");
    }

    @PostMapping("/update")
    public Result update(@RequestParam Long id,
                        @RequestParam Integer type,
                        @RequestParam BigDecimal amount,
                        @RequestParam String category,
                        @RequestParam String accountType,
                        @RequestParam String recordDate,
                        @RequestParam(required = false) String remark,
                        HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录");
        }
        Record record = recordService.getById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            return Result.error("记录不存在或无权操作");
        }
        record.setType(type);
        record.setAmount(amount);
        record.setCategory(category);
        record.setAccountType(accountType);
        record.setRecordDate(LocalDate.parse(recordDate));
        record.setRemark(remark);
        boolean success = recordService.update(record);
        return success ? Result.success("更新成功") : Result.error("更新失败");
    }

    @PostMapping("/delete")
    public Result delete(@RequestParam Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录");
        }
        Record record = recordService.getById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            return Result.error("记录不存在或无权操作");
        }
        boolean success = recordService.delete(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }
}
