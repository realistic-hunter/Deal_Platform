package com.example.dealplatform.controller;

import com.example.dealplatform.model.User;
import com.example.dealplatform.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ProfileController {
    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        model.addAttribute("user", userService.findById(loginUser.getId()));
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(String nickname, String realName, String phone, String email, HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        try {
            userService.updateProfile(loginUser.getId(), nickname, realName, phone, email);
            User refreshed = userService.findById(loginUser.getId());
            session.setAttribute("loginUser", refreshed);
            model.addAttribute("user", refreshed);
            model.addAttribute("message", "个人信息已保存");
        } catch (IllegalArgumentException ex) {
            model.addAttribute("user", userService.findById(loginUser.getId()));
            model.addAttribute("error", ex.getMessage());
        }
        return "profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(String oldPassword, String newPassword, String confirmPassword, HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        try {
            userService.changePassword(loginUser.getId(), oldPassword, newPassword, confirmPassword);
            model.addAttribute("message", "密码修改成功");
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        model.addAttribute("user", userService.findById(loginUser.getId()));
        return "profile";
    }
}
