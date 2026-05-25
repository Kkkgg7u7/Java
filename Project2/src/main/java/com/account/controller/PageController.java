package com.account.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

@Controller
public class PageController {

    @GetMapping("/")
    public String index(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/user/login";
        }
        return "redirect:/index";
    }

    @GetMapping("/index")
    public String home(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/user/login";
        }
        return "index";
    }

    @GetMapping("/records")
    public String records(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/user/login";
        }
        return "records";
    }

    @GetMapping("/accounts")
    public String accounts(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/user/login";
        }
        return "accounts";
    }

    @GetMapping("/stats")
    public String stats(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/user/login";
        }
        return "stats";
    }

    @GetMapping("/bigdata")
    public String bigdata(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/user/login";
        }
        return "bigdata";
    }
}
