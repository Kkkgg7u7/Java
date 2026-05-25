package com.account.controller;

import com.account.entity.Account;
import com.account.entity.User;
import com.account.service.AccountService;
import com.account.service.CategoryService;
import com.account.service.UserService;
import com.account.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AccountService accountService;

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/index";
        }
        return "login";
    }

    @PostMapping("/login")
    @ResponseBody
    public Result login(@RequestParam("username") String username,
                        @RequestParam("password") String password,
                        HttpSession session) {
        User user = userService.login(username, password);
        if (user == null) {
            return Result.error("用户名或密码错误");
        }
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getNickname() != null ? user.getNickname() : user.getUsername());
        return Result.success("登录成功");
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/user/login";
    }

    @PostMapping("/register")
    @ResponseBody
    public Result register(@RequestParam("username") String username,
                          @RequestParam("password") String password,
                          @RequestParam(value = "nickname", required = false) String nickname,
                          HttpSession session) {
        User existUser = userService.getByUsername(username);
        if (existUser != null) {
            return Result.error("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname(nickname);
        boolean success = userService.register(user);
        if (!success) {
            return Result.error("注册失败");
        }
        categoryService.initDefaultCategories(user.getId());
        initDefaultAccounts(user.getId());
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getNickname() != null ? user.getNickname() : user.getUsername());
        return Result.success("注册成功");
    }

    private void initDefaultAccounts(Long userId) {
        addDefaultAccount(userId, "微信钱包", "微信");
        addDefaultAccount(userId, "支付宝余额", "支付宝");
        addDefaultAccount(userId, "现金钱包", "现金");
    }

    private void addDefaultAccount(Long userId, String accountName, String accountType) {
        Account account = new Account();
        account.setUserId(userId);
        account.setAccountName(accountName);
        account.setAccountType(accountType);
        account.setBalance(BigDecimal.ZERO);
        accountService.add(account);
    }
}
