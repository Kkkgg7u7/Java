package com.account.controller;

import com.account.entity.Account;
import com.account.service.AccountService;
import com.account.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @GetMapping("/list")
    public Result list(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录");
        }
        List<Account> accounts = accountService.getByUserId(userId);
        return Result.success(accounts);
    }

    @GetMapping("/totalBalance")
    public Result totalBalance(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录");
        }
        BigDecimal total = accountService.getTotalBalance(userId);
        return Result.success(total);
    }

    @PostMapping("/add")
    public Result add(@RequestParam String accountName,
                     @RequestParam(required = false) String accountType,
                     @RequestParam(required = false) BigDecimal balance,
                     HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录");
        }
        Account account = new Account();
        account.setUserId(userId);
        account.setAccountName(accountName);
        account.setAccountType(accountType);
        account.setBalance(balance != null ? balance : BigDecimal.ZERO);
        boolean success = accountService.add(account);
        return success ? Result.success("添加成功") : Result.error("添加失败");
    }

    @PostMapping("/update")
    public Result update(@RequestParam Long id,
                        @RequestParam String accountName,
                        @RequestParam(required = false) String accountType,
                        @RequestParam(required = false) BigDecimal balance,
                        HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录");
        }
        Account account = accountService.getById(id);
        if (account == null || !account.getUserId().equals(userId)) {
            return Result.error("账户不存在或无权操作");
        }
        account.setAccountName(accountName);
        account.setAccountType(accountType);
        if (balance != null) {
            account.setBalance(balance);
        }
        boolean success = accountService.update(account);
        return success ? Result.success("更新成功") : Result.error("更新失败");
    }

    @PostMapping("/delete")
    public Result delete(@RequestParam Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录");
        }
        Account account = accountService.getById(id);
        if (account == null || !account.getUserId().equals(userId)) {
            return Result.error("账户不存在或无权操作");
        }
        boolean success = accountService.delete(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }
}
