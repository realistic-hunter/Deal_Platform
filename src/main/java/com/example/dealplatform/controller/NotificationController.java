package com.example.dealplatform.controller;

import com.example.dealplatform.model.User;
import com.example.dealplatform.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public String list(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        model.addAttribute("notifications", notificationService.list(user.getId()));
        return "notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String read(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        notificationService.markRead(id, user.getId());
        return "redirect:/notifications";
    }
}
