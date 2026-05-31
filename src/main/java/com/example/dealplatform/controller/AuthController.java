package com.example.dealplatform.controller;

import com.example.dealplatform.model.User;
import com.example.dealplatform.service.OperationLogService;
import com.example.dealplatform.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {
    private final UserService userService;
    private final OperationLogService operationLogService;

    public AuthController(UserService userService, OperationLogService operationLogService) {
        this.userService = userService;
        this.operationLogService = operationLogService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String needLogin, Model model) {
        if (needLogin != null) {
            model.addAttribute("error", "请先登录后再访问该页面");
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(String username, String password, HttpSession session, HttpServletRequest request, Model model) {
        return userService.login(username, password)
                .map(user -> afterLogin(user, session, request))
                .orElseGet(() -> {
                    model.addAttribute("error", "用户名或密码错误，或账号已被禁用");
                    return "login";
                });
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(String username, String password, String confirmPassword, String phone, String email, Model model) {
        try {
            userService.register(username, password, confirmPassword, phone, email);
            model.addAttribute("message", "注册成功，请登录");
            return "login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/forbidden")
    public String forbidden() {
        return "forbidden";
    }

    private String afterLogin(User user, HttpSession session, HttpServletRequest request) {
        session.setAttribute("loginUser", user);
        operationLogService.record(user, "LOGIN", "User login", request);
        return "admin".equals(user.getRoleCode()) ? "redirect:/admin" : "redirect:/goods";
    }
}
