package com.account.controller;

import com.account.entity.Account;
import com.account.entity.Record;
import com.account.service.AccountService;
import com.account.service.RecordService;
import com.account.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
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

    @Autowired
    private AccountService accountService;

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
    @Transactional
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
        if (success) {
            adjustAccountBalance(userId, accountType, type, amount);
        }
        return success ? Result.success("保存成功") : Result.error("保存失败");
    }

    @PostMapping("/update")
    @Transactional
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
        Integer oldType = record.getType();
        BigDecimal oldAmount = record.getAmount();
        String oldAccountType = record.getAccountType();
        record.setType(type);
        record.setAmount(amount);
        record.setCategory(category);
        record.setAccountType(accountType);
        record.setRecordDate(LocalDate.parse(recordDate));
        record.setRemark(remark);
        boolean success = recordService.update(record);
        if (success) {
            adjustAccountBalance(userId, oldAccountType, oldType, oldAmount.negate());
            adjustAccountBalance(userId, accountType, type, amount);
        }
        return success ? Result.success("更新成功") : Result.error("更新失败");
    }

    @PostMapping("/delete")
    @Transactional
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
        if (success) {
            adjustAccountBalance(userId, record.getAccountType(), record.getType(), record.getAmount().negate());
        }
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }

    private void adjustAccountBalance(Long userId, String accountName, Integer recordType, BigDecimal amount) {
        Account account = accountService.getByUserIdAndName(userId, accountName);
        if (account == null || recordType == null || amount == null) {
            return;
        }
        BigDecimal delta = recordType == 1 ? amount : amount.negate();
        accountService.updateBalance(account.getId(), delta);
    }
}
