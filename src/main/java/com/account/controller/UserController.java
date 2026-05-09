package com.account.controller;

import com.account.entity.User;
import com.account.service.UserService;
import com.account.util.MD5Util;
import com.account.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    @ResponseBody
    public Result login(@RequestParam String username,
                        @RequestParam String password,
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
    public Result register(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam(required = false) String nickname) {
        User existUser = userService.getByUsername(username);
        if (existUser != null) {
            return Result.error("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(MD5Util.encrypt(password));
        user.setNickname(nickname);
        boolean success = userService.register(user);
        return success ? Result.success("注册成功") : Result.error("注册失败");
    }
}
